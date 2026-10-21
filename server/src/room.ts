import crypto from "node:crypto";
import type { WebSocket } from "ws";
import type {
  EphemeralRoom,
  Participant,
  RoomStateEvent,
  TensionStartEvent,
  TensionCancelEvent,
  OutcomeEvent,
} from "./types.js";

export interface RoomManagerOptions {
  tensionDurationMs?: number;
  inactivityTimeoutMs?: number;
  cleanupIntervalMs?: number;
}

export class RoomManager {
  private rooms: Map<string, EphemeralRoom> = new Map();
  private readonly tensionDurationMs: number;
  private readonly inactivityTimeoutMs: number;
  private cleanupInterval: NodeJS.Timeout | null = null;

  constructor(options?: RoomManagerOptions) {
    this.tensionDurationMs = options?.tensionDurationMs ?? 3000;
    // Default 15 minutes of inactivity before auto-cleanup
    this.inactivityTimeoutMs = options?.inactivityTimeoutMs ?? 15 * 60 * 1000;
    const cleanupIntervalMs = options?.cleanupIntervalMs ?? 60 * 1000;

    this.cleanupInterval = setInterval(() => {
      this.cleanupExpiredRooms();
    }, cleanupIntervalMs);

    // Unref cleanup interval so it does not prevent node process from exiting
    if (this.cleanupInterval.unref) {
      this.cleanupInterval.unref();
    }
  }

  public getRoom(roomCode: string): EphemeralRoom | undefined {
    return this.rooms.get(roomCode.toUpperCase());
  }

  public getOrCreateRoom(roomCode: string): EphemeralRoom {
    const code = roomCode.toUpperCase();
    let room = this.rooms.get(code);

    if (!room) {
      room = {
        code,
        state: "IDLE",
        participants: new Map(),
        loserId: null,
        loserName: null,
        tensionTimer: null,
        tensionStartTime: null,
        tensionDurationMs: this.tensionDurationMs,
        createdAt: Date.now(),
        lastActivityAt: Date.now(),
      };
      this.rooms.set(code, room);
    }

    return room;
  }

  public joinRoom(
    roomCode: string,
    participantId: string,
    participantName: string,
    ws?: WebSocket
  ): EphemeralRoom {
    const room = this.getOrCreateRoom(roomCode);
    room.lastActivityAt = Date.now();

    const existing = room.participants.get(participantId);
    if (existing) {
      existing.name = participantName;
      existing.ws = ws ?? existing.ws;
    } else {
      const participant: Participant = {
        id: participantId,
        name: participantName,
        isPressing: false,
        connectedAt: Date.now(),
        ws,
      };
      room.participants.set(participantId, participant);
    }

    return room;
  }

  public touchDown(
    roomCode: string,
    participantId: string,
    callbacks?: {
      onTensionStart?: (event: TensionStartEvent) => void;
      onOutcome?: (event: OutcomeEvent) => void;
    }
  ): { room: EphemeralRoom; tensionStarted: boolean } {
    const room = this.getRoom(roomCode);
    if (!room) {
      throw new Error(`Room ${roomCode} does not exist`);
    }

    const participant = room.participants.get(participantId);
    if (!participant) {
      throw new Error(`Participant ${participantId} not found in room ${roomCode}`);
    }

    participant.isPressing = true;
    room.lastActivityAt = Date.now();

    let tensionStarted = false;

    // Check tension condition: 2 or more players pressing and currently IDLE
    const pressingParticipants = Array.from(room.participants.values()).filter(
      (p) => p.isPressing
    );

    if (pressingParticipants.length >= 2 && room.state === "IDLE") {
      room.state = "TENSION";
      room.tensionStartTime = Date.now();
      tensionStarted = true;

      const tensionEvent: TensionStartEvent = {
        type: "TENSION_START",
        roomCode: room.code,
        durationMs: room.tensionDurationMs,
        startTime: room.tensionStartTime,
      };

      callbacks?.onTensionStart?.(tensionEvent);

      room.tensionTimer = setTimeout(() => {
        this.resolveOutcome(room.code, callbacks?.onOutcome);
      }, room.tensionDurationMs);
    }

    return { room, tensionStarted };
  }

  public touchUp(
    roomCode: string,
    participantId: string,
    callbacks?: {
      onTensionCancel?: (event: TensionCancelEvent) => void;
    }
  ): { room: EphemeralRoom; tensionCancelled: boolean } {
    const room = this.getRoom(roomCode);
    if (!room) {
      throw new Error(`Room ${roomCode} does not exist`);
    }

    const participant = room.participants.get(participantId);
    if (!participant) {
      throw new Error(`Participant ${participantId} not found in room ${roomCode}`);
    }

    participant.isPressing = false;
    room.lastActivityAt = Date.now();

    let tensionCancelled = false;

    if (room.state === "TENSION") {
      const remainingPressing = Array.from(room.participants.values()).filter(
        (p) => p.isPressing
      );

      // If fewer than 2 players are pressing, tension is aborted
      if (remainingPressing.length < 2) {
        if (room.tensionTimer) {
          clearTimeout(room.tensionTimer);
          room.tensionTimer = null;
        }
        room.state = "IDLE";
        room.tensionStartTime = null;
        tensionCancelled = true;

        const cancelEvent: TensionCancelEvent = {
          type: "TENSION_CANCEL",
          roomCode: room.code,
          reason: "PLAYER_RELEASED",
        };

        callbacks?.onTensionCancel?.(cancelEvent);
      }
    }

    return { room, tensionCancelled };
  }

  public resolveOutcome(
    roomCode: string,
    onOutcome?: (event: OutcomeEvent) => void
  ): OutcomeEvent | null {
    const room = this.getRoom(roomCode);
    if (!room || room.state !== "TENSION") {
      return null;
    }

    const activePressing = Array.from(room.participants.values()).filter(
      (p) => p.isPressing
    );

    if (activePressing.length === 0) {
      room.state = "IDLE";
      room.tensionTimer = null;
      return null;
    }

    // Unbiased CSPRNG selection using Node.js crypto.randomInt
    const loserIndex = crypto.randomInt(0, activePressing.length);
    const loser = activePressing[loserIndex];

    // Cryptographic seed & SHA-256 hash for provable fairness / transparency
    const randomSeed = crypto.randomBytes(32);
    const seedHash = crypto.createHash("sha256").update(randomSeed).digest("hex");

    room.state = "OUTCOME";
    room.loserId = loser.id;
    room.loserName = loser.name;
    room.seedHash = seedHash;
    room.tensionTimer = null;
    room.lastActivityAt = Date.now();

    const outcomeEvent: OutcomeEvent = {
      type: "OUTCOME",
      roomCode: room.code,
      loserId: loser.id,
      loserName: loser.name,
      seedHash,
      timestamp: Date.now(),
    };

    onOutcome?.(outcomeEvent);
    return outcomeEvent;
  }

  public resetRound(roomCode: string): EphemeralRoom {
    const room = this.getRoom(roomCode);
    if (!room) {
      throw new Error(`Room ${roomCode} does not exist`);
    }

    if (room.tensionTimer) {
      clearTimeout(room.tensionTimer);
      room.tensionTimer = null;
    }

    room.state = "IDLE";
    room.loserId = null;
    room.loserName = null;
    room.tensionStartTime = null;
    room.seedHash = undefined;
    room.lastActivityAt = Date.now();

    // Reset touch state for all participants
    for (const participant of room.participants.values()) {
      participant.isPressing = false;
    }

    return room;
  }

  public leaveRoom(
    roomCode: string,
    participantId: string,
    callbacks?: {
      onTensionCancel?: (event: TensionCancelEvent) => void;
    }
  ): { room: EphemeralRoom | undefined; tensionCancelled: boolean } {
    const room = this.getRoom(roomCode);
    if (!room) {
      return { room: undefined, tensionCancelled: false };
    }

    const participant = room.participants.get(participantId);
    let tensionCancelled = false;

    if (participant) {
      const wasPressing = participant.isPressing;
      room.participants.delete(participantId);
      room.lastActivityAt = Date.now();

      if (room.state === "TENSION" && wasPressing) {
        const remainingPressing = Array.from(room.participants.values()).filter(
          (p) => p.isPressing
        );
        if (remainingPressing.length < 2) {
          if (room.tensionTimer) {
            clearTimeout(room.tensionTimer);
            room.tensionTimer = null;
          }
          room.state = "IDLE";
          room.tensionStartTime = null;
          tensionCancelled = true;

          const cancelEvent: TensionCancelEvent = {
            type: "TENSION_CANCEL",
            roomCode: room.code,
            reason: "PLAYER_LEFT",
          };
          callbacks?.onTensionCancel?.(cancelEvent);
        }
      }
    }

    return { room, tensionCancelled };
  }

  public getRoomStateEvent(room: EphemeralRoom): RoomStateEvent {
    return {
      type: "ROOM_STATE",
      roomCode: room.code,
      state: room.state,
      participants: Array.from(room.participants.values()).map((p) => ({
        id: p.id,
        name: p.name,
        isPressing: p.isPressing,
        connectedAt: p.connectedAt,
      })),
      loserId: room.loserId,
      loserName: room.loserName,
      lastActivityAt: room.lastActivityAt,
    };
  }

  public cleanupExpiredRooms(): number {
    const now = Date.now();
    let cleanedCount = 0;

    for (const [code, room] of this.rooms.entries()) {
      if (now - room.lastActivityAt > this.inactivityTimeoutMs) {
        if (room.tensionTimer) {
          clearTimeout(room.tensionTimer);
          room.tensionTimer = null;
        }
        this.rooms.delete(code);
        cleanedCount++;
      }
    }

    return cleanedCount;
  }

  public getAllRoomsCount(): number {
    return this.rooms.size;
  }

  public destroy(): void {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
      this.cleanupInterval = null;
    }

    for (const room of this.rooms.values()) {
      if (room.tensionTimer) {
        clearTimeout(room.tensionTimer);
        room.tensionTimer = null;
      }
    }

    this.rooms.clear();
  }
}
