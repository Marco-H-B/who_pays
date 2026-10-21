import type { WebSocket } from "ws";

export type RoomState = "IDLE" | "TENSION" | "OUTCOME";

export interface Participant {
  id: string;
  name: string;
  isPressing: boolean;
  connectedAt: number;
  ws?: WebSocket;
}

export interface EphemeralRoom {
  code: string;
  state: RoomState;
  participants: Map<string, Participant>;
  loserId: string | null;
  loserName: string | null;
  tensionTimer: NodeJS.Timeout | null;
  tensionStartTime: number | null;
  tensionDurationMs: number;
  createdAt: number;
  lastActivityAt: number;
  seedHash?: string;
}

// Client -> Server Messages
export type ClientMessage =
  | {
      type: "JOIN_ROOM";
      roomCode: string;
      participantId: string;
      participantName: string;
    }
  | {
      type: "TOUCH_DOWN";
      roomCode: string;
      participantId: string;
    }
  | {
      type: "TOUCH_UP";
      roomCode: string;
      participantId: string;
    }
  | {
      type: "RESET_ROUND";
      roomCode: string;
      participantId: string;
    }
  | {
      type: "LEAVE_ROOM";
      roomCode: string;
      participantId: string;
    };

// Server -> Client Events
export interface RoomStateEvent {
  type: "ROOM_STATE";
  roomCode: string;
  state: RoomState;
  participants: Array<{
    id: string;
    name: string;
    isPressing: boolean;
    connectedAt: number;
  }>;
  loserId: string | null;
  loserName: string | null;
  lastActivityAt: number;
}

export interface TensionStartEvent {
  type: "TENSION_START";
  roomCode: string;
  durationMs: number;
  startTime: number;
}

export interface TensionCancelEvent {
  type: "TENSION_CANCEL";
  roomCode: string;
  reason: string;
}

export interface OutcomeEvent {
  type: "OUTCOME";
  roomCode: string;
  loserId: string;
  loserName: string;
  seedHash: string;
  timestamp: number;
}

export interface ErrorEvent {
  type: "ERROR";
  message: string;
}

export type ServerEvent =
  | RoomStateEvent
  | TensionStartEvent
  | TensionCancelEvent
  | OutcomeEvent
  | ErrorEvent;
