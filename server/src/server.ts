import { WebSocketServer, WebSocket } from "ws";
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { RoomManager, type RoomManagerOptions } from "./room.js";
import type { ClientMessage, ServerEvent } from "./types.js";

const MIME_TYPES: Record<string, string> = {
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".jpeg": "image/jpeg",
  ".jpg": "image/jpeg",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
};

export interface ServerOptions extends RoomManagerOptions {
  port?: number;
  host?: string;
  staticDir?: string;
}

export class WhoPaysServer {
  public readonly roomManager: RoomManager;
  private httpServer: http.Server | null = null;
  private wss: WebSocketServer | null = null;
  private wsToParticipant: Map<WebSocket, { roomCode: string; participantId: string }> =
    new Map();
  private readonly defaultPort: number;
  private readonly host: string;
  private readonly staticDir: string | null;
  private boundPort: number | null = null;

  constructor(options?: ServerOptions) {
    this.defaultPort =
      options?.port ??
      (process.env.PORT ? parseInt(process.env.PORT, 10) : 3000);
    this.host = options?.host ?? "0.0.0.0";
    this.staticDir =
      options?.staticDir ??
      path.resolve(fileURLToPath(import.meta.url), "../../web");
    this.roomManager = new RoomManager(options);
  }

  public getPort(): number {
    if (this.boundPort === null) {
      throw new Error("Server is not running");
    }
    return this.boundPort;
  }

  public async start(): Promise<number> {
    return new Promise((resolve, reject) => {
      try {
        this.httpServer = http.createServer((req, res) => {
          this.handleHttpRequest(req, res);
        });

        this.wss = new WebSocketServer({
          server: this.httpServer,
        });

        this.wss.on("connection", (ws: WebSocket) => {
          this.handleConnection(ws);
        });

        this.httpServer.on("error", (err) => {
          reject(err);
        });

        this.httpServer.listen(this.defaultPort, this.host, () => {
          const address = this.httpServer?.address();
          if (typeof address === "object" && address !== null) {
            this.boundPort = address.port;
          } else {
            this.boundPort = this.defaultPort;
          }
          resolve(this.boundPort);
        });
      } catch (error) {
        reject(error);
      }
    });
  }

  public async stop(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.roomManager.destroy();

      if (this.wss) {
        for (const client of this.wss.clients) {
          if (client.readyState === WebSocket.OPEN) {
            client.terminate();
          }
        }
      }

      this.wsToParticipant.clear();

      if (this.wss) {
        this.wss.close();
        this.wss = null;
      }

      if (this.httpServer) {
        this.httpServer.close((err) => {
          this.httpServer = null;
          this.boundPort = null;
          if (err) reject(err);
          else resolve();
        });
      } else {
        this.boundPort = null;
        resolve();
      }
    });
  }

  private handleHttpRequest(req: http.IncomingMessage, res: http.ServerResponse): void {
    if (!this.staticDir || !fs.existsSync(this.staticDir)) {
      res.writeHead(200, { "Content-Type": "text/plain" });
      res.end("WHO PAYS? WebSocket Server Active");
      return;
    }

    const parsedUrl = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
    let reqPath = decodeURIComponent(parsedUrl.pathname);
    if (reqPath === "/") reqPath = "/index.html";

    const safePath = path.normalize(reqPath).replace(/^(\.\.[/\\])+/, "");
    const fullPath = path.join(this.staticDir, safePath);

    if (!fs.existsSync(fullPath) || fs.statSync(fullPath).isDirectory()) {
      res.writeHead(404, { "Content-Type": "text/plain" });
      res.end("Not Found");
      return;
    }

    const ext = path.extname(fullPath).toLowerCase();
    const contentType = MIME_TYPES[ext] || "application/octet-stream";

    res.writeHead(200, {
      "Content-Type": contentType,
      "Cache-Control": "no-cache",
    });
    fs.createReadStream(fullPath).pipe(res);
  }

  private handleConnection(ws: WebSocket): void {
    ws.on("message", (raw: string | Buffer) => {
      try {
        const text = raw.toString();
        const message = JSON.parse(text) as ClientMessage;
        this.handleMessage(ws, message);
      } catch (err: unknown) {
        this.sendToSocket(ws, {
          type: "ERROR",
          message:
            err instanceof Error ? err.message : "Invalid JSON payload received",
        });
      }
    });

    ws.on("close", () => {
      this.handleDisconnect(ws);
    });

    ws.on("error", () => {
      this.handleDisconnect(ws);
    });
  }

  private handleMessage(ws: WebSocket, message: ClientMessage): void {
    if (!message || typeof message.type !== "string") {
      this.sendToSocket(ws, {
        type: "ERROR",
        message: "Message must contain a valid 'type' property",
      });
      return;
    }

    const roomCode = message.roomCode?.toUpperCase();
    if (!roomCode) {
      this.sendToSocket(ws, {
        type: "ERROR",
        message: "Missing or invalid 'roomCode'",
      });
      return;
    }

    switch (message.type) {
      case "JOIN_ROOM": {
        if (!message.participantId || !message.participantName) {
          this.sendToSocket(ws, {
            type: "ERROR",
            message: "Missing 'participantId' or 'participantName'",
          });
          return;
        }

        const room = this.roomManager.joinRoom(
          roomCode,
          message.participantId,
          message.participantName,
          ws
        );

        this.wsToParticipant.set(ws, {
          roomCode,
          participantId: message.participantId,
        });

        // Broadcast updated state to all members of the room
        this.broadcastToRoom(roomCode, this.roomManager.getRoomStateEvent(room));
        break;
      }

      case "TOUCH_DOWN": {
        try {
          const { room } = this.roomManager.touchDown(
            roomCode,
            message.participantId,
            {
              onTensionStart: (event) => {
                this.broadcastToRoom(roomCode, event);
              },
              onOutcome: (event) => {
                this.broadcastToRoom(roomCode, event);
                const updatedRoom = this.roomManager.getRoom(roomCode);
                if (updatedRoom) {
                  this.broadcastToRoom(
                    roomCode,
                    this.roomManager.getRoomStateEvent(updatedRoom)
                  );
                }
              },
            }
          );

          this.broadcastToRoom(
            roomCode,
            this.roomManager.getRoomStateEvent(room)
          );
        } catch (err: unknown) {
          this.sendToSocket(ws, {
            type: "ERROR",
            message: err instanceof Error ? err.message : "Error on TOUCH_DOWN",
          });
        }
        break;
      }

      case "TOUCH_UP": {
        try {
          const { room } = this.roomManager.touchUp(
            roomCode,
            message.participantId,
            {
              onTensionCancel: (event) => {
                this.broadcastToRoom(roomCode, event);
              },
            }
          );

          this.broadcastToRoom(
            roomCode,
            this.roomManager.getRoomStateEvent(room)
          );
        } catch (err: unknown) {
          this.sendToSocket(ws, {
            type: "ERROR",
            message: err instanceof Error ? err.message : "Error on TOUCH_UP",
          });
        }
        break;
      }

      case "RESET_ROUND": {
        try {
          const room = this.roomManager.resetRound(roomCode);
          this.broadcastToRoom(
            roomCode,
            this.roomManager.getRoomStateEvent(room)
          );
        } catch (err: unknown) {
          this.sendToSocket(ws, {
            type: "ERROR",
            message: err instanceof Error ? err.message : "Error on RESET_ROUND",
          });
        }
        break;
      }

      case "LEAVE_ROOM": {
        const { room } = this.roomManager.leaveRoom(
          roomCode,
          message.participantId,
          {
            onTensionCancel: (event) => {
              this.broadcastToRoom(roomCode, event);
            },
          }
        );

        this.wsToParticipant.delete(ws);

        if (room) {
          this.broadcastToRoom(
            roomCode,
            this.roomManager.getRoomStateEvent(room)
          );
        }
        break;
      }

      default: {
        this.sendToSocket(ws, {
          type: "ERROR",
          message: `Unknown message type: ${(message as { type: string }).type}`,
        });
      }
    }
  }

  private handleDisconnect(ws: WebSocket): void {
    const session = this.wsToParticipant.get(ws);
    if (!session) return;

    this.wsToParticipant.delete(ws);

    const { room } = this.roomManager.leaveRoom(
      session.roomCode,
      session.participantId,
      {
        onTensionCancel: (event) => {
          this.broadcastToRoom(session.roomCode, event);
        },
      }
    );

    if (room) {
      this.broadcastToRoom(
        session.roomCode,
        this.roomManager.getRoomStateEvent(room)
      );
    }
  }

  public broadcastToRoom(roomCode: string, event: ServerEvent): void {
    const room = this.roomManager.getRoom(roomCode);
    if (!room) return;

    const payload = JSON.stringify(event);

    for (const participant of room.participants.values()) {
      if (participant.ws && participant.ws.readyState === WebSocket.OPEN) {
        participant.ws.send(payload);
      }
    }
  }

  private sendToSocket(ws: WebSocket, event: ServerEvent): void {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(event));
    }
  }
}

// Standalone entrypoint check
const isMain = (): boolean => {
  if (!process.argv[1]) return false;
  try {
    const currentFile = fileURLToPath(import.meta.url);
    const invokedFile = path.resolve(process.argv[1]);
    return currentFile.toLowerCase() === invokedFile.toLowerCase();
  } catch {
    return false;
  }
};

if (isMain()) {
  const server = new WhoPaysServer();
  server
    .start()
    .then((port) => {
      console.log(`\n==================================================`);
      console.log(`👆 WHO PAYS? High-Fidelity WebSocket Server`);
      console.log(`⚡ Puerto Activo: ${port}`);
      console.log(`⏱️  Auto-Limpieza RAM: 15 minutos de inactividad`);
      console.log(`🎯 Tensión Háptica: 3000ms`);
      console.log(`🎲 CSPRNG: crypto.randomInt + SHA-256 seedHash`);
      console.log(`==================================================\n`);
    })
    .catch((err) => {
      console.error("❌ Error iniciando el servidor WHO PAYS?:", err);
      process.exit(1);
    });
}
