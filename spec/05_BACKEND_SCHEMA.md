# 🗄️ Esquema Backend & Modelado de Datos — WHO PAYS?

> **Documento:** 05 de 06 (Estándar Canónico de Planificación)
> **Producto:** WHO PAYS? (El Selector Social Táctil & Motor de Decisión P2P)
> **Ubicación:** `E:\development\business\mobile\social\who_pays\docs\05_BACKEND_SCHEMA.md`
> **Autor:** Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
> **Versión:** 1.0.0-STABLE

---

## 1. ⚡ Arquitectura Híbrida: Memoria Efímera en Edge + SQLite Local

Para cumplir con la premisa de **costo de servidor $0 al inicio y latencia sub-20ms**:

- Las salas de juego no tocan la base de datos de disco en cada frame.
- Viven en **memoria RAM** dentro de Cloudflare Workers (Durable Objects / In-Memory KV) durante 15 minutos y se autodestruyen al terminar el juego.
- La base de datos persistente almacena exclusivamente pagos (Lemon Squeezy), licencias Pro y el Libro de Deudas local en SQLite (`CC202`).

---

## 2. 📋 Esquema Relacional de Base de Datos (PostgreSQL / SQLite)

```sql
-- 1. Tabla de Usuarios y Anfitriones
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    device_id TEXT UNIQUE NOT NULL,
    is_pro BOOLEAN DEFAULT FALSE,
    pro_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Registro Inmutable de Pagos (Auditoría Financiera & Timeout de 3 Minutos)
CREATE TABLE payments (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id),
    provider TEXT NOT NULL, -- 'lemon_squeezy', 'google_play', 'apple_pay'
    order_id TEXT UNIQUE NOT NULL,
    amount_cents INTEGER NOT NULL, -- 199 para $1.99, 999 para $9.99
    currency TEXT DEFAULT 'USD',
    status TEXT NOT NULL, -- 'pending', 'completed', 'expired', 'refunded'
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- created_at + INTERVAL '3 minutes'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Libro de Deudas Sociales (Función Pro)
CREATE TABLE debts_ledger (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    debtor_name TEXT NOT NULL,    -- Quién perdió
    creditor_name TEXT NOT NULL,  -- Quién pagó la cuenta
    amount_cents INTEGER NOT NULL,
    settled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. 🌐 Estructura de Sala Efímera en RAM (Durable Objects / WebSockets)

```typescript
interface EphemeralRoom {
    code: string; // ej: "7F2A"
    hostDeviceId: string;
    isPaid: boolean; // true si host es Pro o pagó ticket de $1.99
    paymentExpiresAt?: number; // Timestamp epoch (ms) del timeout estricto de 3 minutos (180s)
    participants: {
        id: string;
        name: string; // Nombre/alias ingresado al escanear QR (identificación del perdedor)
        isPressing: boolean;
        connectedAt: number;
    }[];
    state: "LOBBY" | "PAYMENT_PENDING" | "COUNTDOWN" | "RESOLVED" | "EXPIRED";
    seedHash: string; // SHA-256 de la semilla para transparencia
}
```
