# ⚙️ Technical Requirement Document (TRD) — WHO PAYS?

> **Documento:** 02 de 06 (Estándar Canónico de Planificación)
> **Producto:** WHO PAYS? (El Selector Social Táctil & Motor de Decisión P2P)
> **Ubicación:** `E:\development\business\mobile\social\who_pays\docs\02_TRD.md`
> **Autor:** Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
> **Versión:** 1.0.0-STABLE

---

## 1. 🏛️ Arquitectura General del Sistema

El sistema sigue los principios de **Clean Architecture Hexagonal** (`CC341` / `CC211`), dividiendo el software en dos clientes ligeros y un servicio de señalización WebSockets:

```
+-----------------------------------------------------------------------------------+
|                           ARQUITECTURA SISTÉMICA WHO PAYS?                        |
+-----------------------------------------------------------------------------------+
|  [Cliente Móvil Nativo]      Android (Kotlin + Jetpack Compose)                   |
|                              iOS (Swift + SwiftUI / CoreHaptics)                  |
|                              Motor Háptico de Precisión & Canvas Nativo a 60/120 FPS|
+-----------------------------------------------------------------------------------+
|  [Cliente Web Invitado QR]   PWA Vanilla TS + HTML5 Canvas Ultraligero (< 50KB)   |
|                              Web Audio API (Osciladores armónicos en memoria)     |
+-----------------------------------------------------------------------------------+
|  [Capa de Red (CC311)]       WebSockets Dúplex de Baja Latencia (< 20ms)          |
|                              Protocolo binario/JSON ligero con heartbeat de 5s    |
+-----------------------------------------------------------------------------------+
|  [Backend de Salas P2P]      Node.js / Hono en Cloudflare Workers (Edge Runtime)  |
|                              Almacenamiento en memoria RAM de salas efímeras      |
+-----------------------------------------------------------------------------------+
|  [Pasarelas de Cobro]        Lemon Squeezy (Merchant of Record Global)            |
|                              Google Play Billing (Android) / StoreKit 2 (iOS)     |
|                              Yape / Plin / Lemon Cash (Mercado Local)             |
+-----------------------------------------------------------------------------------+
```

---

## 2. 🧮 Modelado Matemático & Físico

### 2.1. Física de Oscilación Armónica Amortiguada (`BMA01` / `CM2A1` / `BFI01`)

Los aros táctiles responden a una ecuación diferencial de segundo orden:
$$m \frac{d^2x}{dt^2} + c \frac{dx}{dt} + kx = 0$$

- $m = 1.0$ (Masa virtual del aro).
- $c = 14.0$ (Coeficiente de amortiguamiento crítico para evitar rebote excesivo).
- $k = 180.0$ (Constante elástica que simula la tensión del titanio al expandirse).

### 2.2. Generador Pseudoaleatorio Criptográficamente Seguro (CSPRNG) (`CM1H2`)

- Se prohíbe el uso de `Math.random()` convencional (predecible por congruencia lineal).
- Se utiliza la API criptográfica nativa `crypto.getRandomValues()` en web o `java.security.SecureRandom` en Android para generar una semilla no sesgada:
  $$S \sim \mathcal{U}\{0, 2^{32}-1\}$$
  $$\text{Índice Perdedor} = S \pmod N$$
- Pruebas automatizadas de bondad de ajuste **Chi-Cuadrado ($\chi^2$)** con $p$-value $> 0.05$ para validar la distribución uniforme en 10,000 ejecuciones sintéticas bajo TDD.

### 2.3. Principio UX: "The Labor Illusion" (La Ilusión del Trabajo)

- Si el sistema resolviera al perdedor en 0.001 segundos, el usuario percibiría el resultado como vacío, abrupto o trucado.
- Siguiendo el estudio de Harvard (_Buell & Norton, 2011_), se implementa deliberadamente una **ventana de tensión de 3.0 segundos**:
    - Incremento exponencial de la frecuencia de vibración háptica ($30 \text{ Hz} \to 180 \text{ Hz}$).
    - Contracción y aceleración de pulsos visuales.
    - Esto maximiza la **anticipación dopamínica** y otorga valor psicológico y solemnidad al resultado final.

### 2.4. Síntesis Sonora Acoplada a Física & Háptica (Audio-Visual Sync)

Para atrapar sensorialmente al usuario, el audio no utiliza grabaciones genéricas en MP3/WAV pesadas, sino **síntesis procedural en tiempo real** acoplada matemáticamente a cada milisegundo de animación:

```
[0.0s] Touch Down     -> Pop percusivo metálico en 100 Hz (Aro se expande).
[0.0s - 2.95s] Ramp   -> Drone armónico FM con aceleración de 60 a 300 BPM
                         y glissando de 110 Hz (A2) a 440 Hz (A4).
[Abandono < 3.0s]     -> Destensión elástica: pitch bend descendente (250 Hz -> 60 Hz)
                         en 80ms, aro se esfuma en gris y reloj de dedos restantes se reinicia.
[2.95s - 3.0s] Vacuum -> Silencio absoluto de 50ms ("Caída al vacío").
[3.0s] Veredicto      -> Perdedor: "Titanium Thud" en 60 Hz + golpe háptico seco.
                         A Salvo: Acorde mayor etéreo con desvanecimiento alfa.
[10s a 0s Checkout]   -> Ticks rítmicos de sonar en 800 Hz (10s-4s), beeps dobles 
                         en 1200 Hz (3s-1s) y tono de cancelación sordo en 150 Hz (0s).
```

- **Osciladores Nativos:**
    - **iOS:** `AVAudioEngine` con nodos de oscilación armónica procedural y envelope ADSR (Attack, Decay, Sustain, Release) acoplado a `CoreHaptics`.
    - **Android:** `AudioTrack` nativo en modo low-latency PCM con búfer de 256 muestras acoplado a `VibratorManager`.
    - **Web PWA:** `Web Audio API` con `OscillatorNode` (`sine` + `triangle`) y `GainNode` sincronizados con `requestAnimationFrame`.

---

## 3. 🌐 Especificación de Red & WebSockets (`CC311`)

- **Protocolo:** WebSocket sobre TLS (`wss://`).
- **Estructura de Trama JSON Ligera:**
    ```json
    {
        "t": "PULSE",
        "room": "7F2A",
        "pId": "usr_9b2",
        "ts": 1729478400123
    }
    ```
- **Latencia Objetivo:** Sub-20ms en red local o Edge CDN.
- **Tolerancia a Desconexiones:** Si un cliente suelta el botón durante la fase de tensión, el servidor emite una notificación de abandono inmediato (`FORFEIT`).

---

## 4. 💳 Integración de Pagos & Webhooks

1. **Lemon Squeezy (Web / PWA):**
    - Configuración de Checkout Overlay directo sin redirección externa.
    - Webhook seguro firmado con HMAC-SHA256 (`x-signature`) para validar la activación inmediata del ticket de \$1.99 o la suscripción Pro de \$9.99/mes.
2. **Google Play Billing & Apple StoreKit 2 (Apps Nativas):**
    - Consumibles para el Pase de 1 solo uso (\$1.99).
    - Auto-renewable Subscription para el plan mensual (\$9.99).
3. **Control de Ventana Temporal de Pago (TTL Estricto de 3 Minutos):**
    - **Regla de Expiración:** Todo intento de pago (orden/checkout/intento) tiene una vigencia máxima inmutable de **3:00 minutos (180 segundos)**: `expires_at = created_at + 180s`.
    - **Validación Criptográfica & Anti-Replay:** El webhook verifica que `now() <= expires_at`. Si un webhook llega después de la ventana de 3 minutos, el backend lo rechaza con código `410 Gone` o inicia un reembolso automático, evitando que se active una sala que los jugadores ya abandonaron.
    - **Sincronización en Sala:** Un temporizador regresivo de 3 minutos en el cliente del anfitrión muestra los segundos restantes con gradiente de color dinámico (Verde `#30D158` -> Amarillo `#FFD60A` -> Rojo `#FF453A`). Si expira, los WebSockets emiten el evento `PAYMENT_TIMEOUT` a todos los clientes conectados y liberan la sala efímera de la memoria RAM (`CC222`).

---

## 5. 🛡️ Blindaje de Ciberseguridad & Cumplimiento Legal

### 5.1. Ciberseguridad & Anti-Fraude

1. **Almacenamiento Local Cero-Conocimiento (Zero-Knowledge SQLite):**
    - El historial de deudas y nombres de amigos se almacena en el dispositivo del usuario en una base SQLite local protegida por el sandbox del sistema operativo (Android Keystore / iOS Keychain). Ningún dato de amigos ni cuentas viaja a servidores remotos.
2. **Esquema de Compromiso Verificable (CSPRNG Commitment Scheme):**
    - En la Mesa QR, el servidor genera la semilla pseudoaleatoria criptográfica antes de la ronda y distribuye su hash `SHA-256(seed)` a todos los clientes.
    - Tras el veredicto, el servidor revela la semilla original. Cualquier cliente puede verificar matemáticamente en su navegador que el resultado no fue manipulado a favor de nadie.
3. **Protección Anti-Replay en Webhooks:**
    - Todo webhook entrante debe incluir cabecera de firma HMAC-SHA256 y marca de tiempo (`x-timestamp`). Si $|t_{\text{servidor}} - t_{\text{petición}}| > 180 \text{ s}$, la petición es descartada de inmediato.
4. **Protección DoS & Rate Limiting en Edge (Cloudflare Workers):**
    - Máximo 5 creaciones de sala por IP por minuto.
    - Tamaño máximo de payload WebSocket restringido a 256 bytes para evitar ataques de desbordamiento de búfer.

### 5.2. Cumplimiento Legal & Normativo

1. **Clasificación Legal: Herramienta de Utilidad Social (NO Juego de Azar / NO Apuestas):**
    - **Términos de Servicio (ToS):** Se establece explícitamente que WHO PAYS? es una herramienta de utilidad social para la toma de decisiones y resolución de turnos. No custodia fondos ni organiza apuestas de dinero real, cumpliendo con la **Ley N° 31557 de Perú** y las directrices de **Apple Store (Guideline 5.3)** y **Google Play**.
2. **Privacidad de Cero Registro (No-PII / Ley N° 29733 & GDPR):**
    - Los invitados de la Mesa QR no proporcionan correos, contraseñas ni números telefónicos. Sus alias viven temporalmente en la memoria RAM del WebSocket y se autodestruyen al cerrar la sala.
3. **Operación Fiscal Internacional (Merchant of Record):**
    - Lemon Squeezy actúa como MoR oficial para transacciones con tarjeta y Apple Pay/Google Pay a nivel global, gestionando automáticamente el IVA/Sales Tax internacional y la emisión de comprobantes digitales.
