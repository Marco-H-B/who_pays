import { describe, it, before, after } from "node:test";
import assert from "node:assert/strict";
import { WebSocket } from "ws";
import { WhoPaysServer } from "../src/server.js";
import { RoomManager } from "../src/room.js";
import type { ServerEvent, RoomStateEvent, TensionStartEvent, TensionCancelEvent, OutcomeEvent } from "../src/types.js";

class TestSocketClient {
  public ws: WebSocket;
  public messages: ServerEvent[] = [];
  private waiters: Array<{
    predicate: (msg: ServerEvent) => boolean;
    resolve: (msg: ServerEvent) => void;
    reject: (err: Error) => void;
    timer: NodeJS.Timeout;
  }> = [];

  constructor(url: string) {
    this.ws = new WebSocket(url);
    this.ws.on("message", (raw) => {
      try {
        const parsed = JSON.parse(raw.toString()) as ServerEvent;
        this.messages.push(parsed);

        // Check if any waiter is satisfied
        for (let i = this.waiters.length - 1; i >= 0; i--) {
          const waiter = this.waiters[i];
          if (waiter.predicate(parsed)) {
            clearTimeout(waiter.timer);
            this.waiters.splice(i, 1);
            waiter.resolve(parsed);
          }
        }
      } catch (err) {
        console.error("TestSocketClient JSON parse error:", err);
      }
    });
  }

  public async waitOpen(timeoutMs = 3000): Promise<void> {
    if (this.ws.readyState === WebSocket.OPEN) return;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        reject(new Error(`WebSocket did not open in ${timeoutMs}ms`));
      }, timeoutMs);

      this.ws.on("open", () => {
        clearTimeout(timer);
        resolve();
      });

      this.ws.on("error", (err) => {
        clearTimeout(timer);
        reject(err);
      });
    });
  }

  public send(payload: unknown): void {
    this.ws.send(JSON.stringify(payload));
  }

  public waitFor<T extends ServerEvent>(
    predicate: (msg: ServerEvent) => boolean,
    timeoutMs = 4000
  ): Promise<T> {
    // Check if message already received
    const existing = this.messages.find(predicate);
    if (existing) {
      return Promise.resolve(existing as T);
    }

    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        const idx = this.waiters.findIndex((w) => w.timer === timer);
        if (idx !== -1) this.waiters.splice(idx, 1);
        reject(
          new Error(
            `Timeout waiting for message matching predicate (${timeoutMs}ms). Received: ${JSON.stringify(
              this.messages
            )}`
          )
        );
      }, timeoutMs);

      this.waiters.push({
        predicate,
        resolve: (msg) => resolve(msg as T),
        reject,
        timer,
      });
    });
  }

  public async close(): Promise<void> {
    for (const w of this.waiters) {
      clearTimeout(w.timer);
    }
    this.waiters = [];

    if (this.ws.readyState === WebSocket.CLOSED) return;

    return new Promise((resolve) => {
      this.ws.once("close", () => resolve());
      this.ws.terminate();
    });
  }
}

describe("WHO PAYS? - Servidor WebSockets de Alta Fidelidad", () => {
  let server: WhoPaysServer;
  let port: number;
  let serverUrl: string;

  before(async () => {
    // Port 0 binds to a free OS port
    server = new WhoPaysServer({ port: 0 });
    port = await server.start();
    serverUrl = `ws://127.0.0.1:${port}`;
  });

  after(async () => {
    await server.stop();
  });

  it("1. Conexión y creación/unión a sala efímera", async () => {
    const client1 = new TestSocketClient(serverUrl);
    const client2 = new TestSocketClient(serverUrl);

    await client1.waitOpen();
    await client2.waitOpen();

    const roomCode = "SALA1";

    // Player 1 joins
    client1.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p1",
      participantName: "Marco",
    });

    const state1 = await client1.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.participants.length === 1
    );

    assert.equal(state1.roomCode, roomCode);
    assert.equal(state1.state, "IDLE");
    assert.equal(state1.participants[0].id, "p1");
    assert.equal(state1.participants[0].name, "Marco");

    // Player 2 joins
    client2.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p2",
      participantName: "Elena",
    });

    const state2 = await client1.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.participants.length === 2
    );
    const state2ForClient2 = await client2.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.participants.length === 2
    );

    assert.equal(state2.participants.length, 2);
    assert.equal(state2ForClient2.participants.length, 2);
    assert.equal(state2.state, "IDLE");

    await client1.close();
    await client2.close();
  });

  it("2. Tensión iniciada cuando 2 o más jugadores presionan al unísono (3000ms)", async () => {
    const client1 = new TestSocketClient(serverUrl);
    const client2 = new TestSocketClient(serverUrl);

    await client1.waitOpen();
    await client2.waitOpen();

    const roomCode = "SALA2";

    client1.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p1",
      participantName: "Marco",
    });
    client2.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p2",
      participantName: "Elena",
    });

    await client2.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.participants.length === 2
    );

    // Player 1 presses down: only 1 pressing -> still IDLE, no TENSION_START
    client1.send({ type: "TOUCH_DOWN", roomCode, participantId: "p1" });

    await client1.waitFor<RoomStateEvent>(
      (m) =>
        m.type === "ROOM_STATE" &&
        m.participants.find((p) => p.id === "p1")?.isPressing === true &&
        m.state === "IDLE"
    );

    // Player 2 presses down: now 2 are pressing -> TENSION_START emitted
    client2.send({ type: "TOUCH_DOWN", roomCode, participantId: "p2" });

    const tensionEvent1 = await client1.waitFor<TensionStartEvent>(
      (m) => m.type === "TENSION_START"
    );
    const tensionEvent2 = await client2.waitFor<TensionStartEvent>(
      (m) => m.type === "TENSION_START"
    );

    assert.equal(tensionEvent1.roomCode, roomCode);
    assert.equal(tensionEvent1.durationMs, 3000);
    assert.equal(tensionEvent2.durationMs, 3000);
    assert.ok(typeof tensionEvent1.startTime === "number");

    // Room state updates to TENSION
    const roomStateTension = await client1.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.state === "TENSION"
    );
    assert.equal(roomStateTension.state, "TENSION");

    await client1.close();
    await client2.close();
  });

  it("3. Cancelación de tensión si un jugador suelta antes de 3.0s", async () => {
    const client1 = new TestSocketClient(serverUrl);
    const client2 = new TestSocketClient(serverUrl);

    await client1.waitOpen();
    await client2.waitOpen();

    const roomCode = "SALA3";

    client1.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p1",
      participantName: "Marco",
    });
    client2.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p2",
      participantName: "Elena",
    });

    await client2.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.participants.length === 2
    );

    // Both touch down
    client1.send({ type: "TOUCH_DOWN", roomCode, participantId: "p1" });
    client2.send({ type: "TOUCH_DOWN", roomCode, participantId: "p2" });

    await client1.waitFor<TensionStartEvent>((m) => m.type === "TENSION_START");

    // Player 1 releases immediately before 3000ms
    client1.send({ type: "TOUCH_UP", roomCode, participantId: "p1" });

    const cancel1 = await client1.waitFor<TensionCancelEvent>(
      (m) => m.type === "TENSION_CANCEL"
    );
    const cancel2 = await client2.waitFor<TensionCancelEvent>(
      (m) => m.type === "TENSION_CANCEL"
    );

    assert.equal(cancel1.roomCode, roomCode);
    assert.equal(cancel1.reason, "PLAYER_RELEASED");
    assert.equal(cancel2.reason, "PLAYER_RELEASED");

    // State returns to IDLE
    const stateBackIdle = await client1.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.state === "IDLE"
    );
    assert.equal(stateBackIdle.state, "IDLE");

    await client1.close();
    await client2.close();
  });

  it("4. Resolución y veredicto con selección uniforme de perdedor al cumplir 3.0s", async () => {
    const client1 = new TestSocketClient(serverUrl);
    const client2 = new TestSocketClient(serverUrl);

    await client1.waitOpen();
    await client2.waitOpen();

    const roomCode = "SALA4";

    client1.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p1",
      participantName: "Marco",
    });
    client2.send({
      type: "JOIN_ROOM",
      roomCode,
      participantId: "p2",
      participantName: "Elena",
    });

    await client2.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.participants.length === 2
    );

    // Both touch down
    client1.send({ type: "TOUCH_DOWN", roomCode, participantId: "p1" });
    client2.send({ type: "TOUCH_DOWN", roomCode, participantId: "p2" });

    await client1.waitFor<TensionStartEvent>((m) => m.type === "TENSION_START");

    // Wait for the 3.0s tension window to complete
    const outcome1 = await client1.waitFor<OutcomeEvent>(
      (m) => m.type === "OUTCOME",
      4500
    );
    const outcome2 = await client2.waitFor<OutcomeEvent>(
      (m) => m.type === "OUTCOME",
      4500
    );

    assert.equal(outcome1.roomCode, roomCode);
    assert.equal(outcome1.loserId, outcome2.loserId);
    assert.ok(outcome1.loserId === "p1" || outcome1.loserId === "p2");
    assert.ok(outcome1.loserName === "Marco" || outcome1.loserName === "Elena");

    // Seed hash must be a valid 64-character SHA-256 hex string
    assert.match(outcome1.seedHash, /^[a-f0-9]{64}$/);
    assert.equal(outcome1.seedHash, outcome2.seedHash);

    // Room state must reflect OUTCOME
    const outcomeState = await client1.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.state === "OUTCOME"
    );
    assert.equal(outcomeState.state, "OUTCOME");
    assert.equal(outcomeState.loserId, outcome1.loserId);

    // Verify RESET_ROUND resets back to IDLE
    client1.send({ type: "RESET_ROUND", roomCode, participantId: "p1" });

    const resetState = await client1.waitFor<RoomStateEvent>(
      (m) => m.type === "ROOM_STATE" && m.state === "IDLE"
    );
    assert.equal(resetState.state, "IDLE");
    assert.equal(resetState.loserId, null);

    await client1.close();
    await client2.close();
  });

  it("5. Auto-limpieza en RAM de salas inactivas (15 minutos)", () => {
    // Create an isolated RoomManager with 100ms inactivity timeout for testing
    const manager = new RoomManager({
      inactivityTimeoutMs: 100,
      cleanupIntervalMs: 50,
    });

    const room = manager.getOrCreateRoom("EXPIRE_TEST");
    assert.equal(manager.getAllRoomsCount(), 1);

    // Set lastActivityAt to 200ms in the past
    room.lastActivityAt = Date.now() - 200;

    const cleaned = manager.cleanupExpiredRooms();
    assert.equal(cleaned, 1);
    assert.equal(manager.getAllRoomsCount(), 0);

    manager.destroy();
  });

  it("6. Distribución uniforme del CSPRNG crypto.randomInt (1000 iteraciones)", () => {
    const manager = new RoomManager({ tensionDurationMs: 10 });
    const roomCode = "CSPRNG_TEST";

    manager.joinRoom(roomCode, "p1", "Marco");
    manager.joinRoom(roomCode, "p2", "Elena");
    manager.joinRoom(roomCode, "p3", "Mateo");

    const counts: Record<string, number> = { p1: 0, p2: 0, p3: 0 };
    const iterations = 1200;

    for (let i = 0; i < iterations; i++) {
      const room = manager.getOrCreateRoom(roomCode);
      room.state = "TENSION";
      for (const p of room.participants.values()) {
        p.isPressing = true;
      }
      const outcome = manager.resolveOutcome(roomCode);
      assert.ok(outcome);
      counts[outcome.loserId]++;
    }

    // Expected ~400 each out of 1200. Each should be within reasonable bounds [300, 500]
    assert.ok(counts.p1 > 300 && counts.p1 < 500, `p1 count was ${counts.p1}`);
    assert.ok(counts.p2 > 300 && counts.p2 < 500, `p2 count was ${counts.p2}`);
    assert.ok(counts.p3 > 300 && counts.p3 < 500, `p3 count was ${counts.p3}`);

    manager.destroy();
  });
});
