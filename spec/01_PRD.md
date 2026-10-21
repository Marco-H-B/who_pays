# 📄 Product Requirement Document (PRD) — WHO PAYS?

> **Documento:** 01 de 06 (Estándar Canónico de Planificación)
> **Producto:** WHO PAYS? (El Selector Social Táctil & Motor de Decisión P2P)
> **Ubicación:** `E:\development\business\mobile\social\who_pays\spec\01_PRD.md`
> **Autor:** Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
> **Fecha:** 21 de Octubre de 2026
> **Versión:** 1.0.0-STABLE

---

## 1. 🎯 Visión General & Resumen Ejecutivo

**WHO PAYS?** es una aplicación móvil y web ultrarrápida, táctil y de estética minimalista diseñada para resolver en segundos uno de los dilemas sociales más universales de la humanidad:

> _"¿Quién paga la cuenta del almuerzo / quién va a comprar las gaseosas / a quién le toca exponer primero / quién lava los platos?"_

A diferencia de las ruletas de casino infantiles, las aplicaciones saturadas de publicidad molesta o los métodos manuales (sacar dedos, monedas), **WHO PAYS?** ofrece una experiencia de alta ingeniería con **cero anuncios**, física de oscilación armónica, diseño sobrio _Monochrome Titanium_ (inspirado en Apple Human Interface Guidelines) y respuesta háptica que genera una tensión física real en las manos de los participantes.

El producto opera bajo dos modalidades complementarias:

1. **Modo Pantalla Única (2 a 5 personas):** Todos ponen un dedo en el mismo teléfono. 100% gratuito de por vida, sin registro ni anuncios.
2. **Modo Mesa Sincronizada QR (6 a 30+ personas):** El anfitrión genera un código QR. Los invitados escanean con la cámara y entran en 2 segundos desde su navegador web móvil (PWA) sin descargar nada. El anfitrión desbloquea la ronda con un pase único de **\$1.99 USD** o mediante la suscripción **WHO PAYS? PRO** de **\$9.99 USD/mes**.

---

## 2. 🚨 Problemas que Resuelve & Oportunidad de Mercado

1. **La Fricción Social del Grupo:**
    - En reuniones, chifas, bares, pichangas o trabajos universitarios, decidir quién asume un costo o tarea genera momentos incómodos o discusiones triviales. WHO PAYS? convierte esa fricción en un ritual lúdico, tenso y divertido.
2. **El Cáncer de los Anuncios en Apps de Utilidad:**
    - Las apps actuales de toma de decisiones bombardean con anuncios de video de 30 segundos con sonido no deseado en medio de la mesa de un restaurante. WHO PAYS? erradica los anuncios por completo.
3. **El Límite Físico de la Pantalla en Grupos Grandes:**
    - Cuando hay más de 5 personas, poner 10 o 15 dedos en una pantalla de 6 pulgadas es incómodo, amontonado y arruina la estética visual de los aros. El Modo Mesa QR descentraliza la interacción a los propios teléfonos de cada usuario mediante WebSockets de sub-20ms.

---

## 3. 👥 Arquetipos de Usuario (User Personas)

1. **El Anfitrión Social / Universitario (The Host):**
    - Estudiante o joven profesional que suele reunirse con amigos a almorzar, estudiar o salir.
    - _Comportamiento:_ Saca el teléfono en la mesa, abre la app y reta a los demás a poner el dedo. Es quien adquiere el pase de \$1.99 o la suscripción Pro de \$9.99/mes para liderar las salidas grandes.
2. **El Invitado Casual (The Guest):**
    - Amigo del grupo que solo quiere participar sin trabas técnicas.
    - _Comportamiento:_ Escanea el QR con su cámara, no instala ninguna app, mantiene pulsado su botón en la pantalla y siente la tensión háptica en vivo. Al ver la experiencia, pregunta el nombre de la app y la descarga para sus propias reuniones.

---

## 4. 🧬 Fundamentación Curricular UNI (`doc.pdf`)

Cada aspecto técnico de WHO PAYS? está respaldado por las asignaturas del plan de estudios de Ciencias de la Computación de la UNI:

```
+---------------------------------------------------------------------------------------------------------+
|                                    MATRIZ DE ASIGNATURAS UNI — WHO PAYS?                                |
+---------------------------------------------------------------------------------------------------------+
| ASIGNATURA UNI            | CÓDIGO  | ROL CRÍTICO EN EL SISTEMA WHO PAYS?                               |
+---------------------------+---------+-------------------------------------------------------------------+
| Redacción y Comunicación  | BRC01   | Microcopy atómico sin rodeos ("Pon tu dedo", "Tú pagas").         |
| Ética y Filosofía Política| BEF01   | Aleatoriedad verificable (Verificación de semilla criptográfica). |
| Inglés I                  | CL002   | Microcopy bilingüe nativo (Who Pays?, Safe, Debt Ledger).         |
| Cálculo Diferencial/Avanz.| BMA/CM  | Ecuaciones diferenciales de oscilación armónica amortiguada.       |
| Probabilidad y Estadística| CM1H/2H | CSPRNG uniforme discreto U{1, N} con pruebas Chi-Cuadrado (χ²).   |
| Álgebra Lineal            | BMA03/C2| Detección euclidiana de toques en R^2 y matrices de dispersión.   |
| Matemática Discreta       | CM2H1   | Partición de conjuntos (k-subsets) para división de equipos.      |
| Física I, II y Electromag.| BFI/CF/CQ Frecuencia resonante del motor háptico (de 30 Hz a 180 Hz).       |
| Arquitectura de Computad. | CC221   | Canvas optimizado a 60 FPS (16.6ms frame budget en iPhone 15) / 120 FPS ProMotion. |
| Economía General          | BEG01   | Modelo de pricing escalonado ($1.99 pase único vs $9.99/mes Pro). |
| POO                       | CC211   | Clean Architecture y State Pattern (Idle -> Tension -> Selected).  |
| Sistemas Operativos       | CC222   | Manejo de eventos multitáctiles del kernel y vibración nativa.    |
| Base de Datos             | CC202   | SQLite local con transacciones ACID para el Libro de Deudas.      |
| Algoritmos y Estructuras  | CC232   | Estructura de toques en O(1) y simplificación de deudas (Grafos). |
| Redes de Computadoras     | CC311   | WebSockets dúplex de sub-20ms para sincronización de la Mesa QR.  |
| Ingeniería de Software    | CC341   | Metodología de 6 Documentos y suite de pruebas bajo TDD.          |
+---------------------------------------------------------------------------------------------------------+
```

---

## 5. ⚙️ Requisitos Funcionales del Sistema

### RF-01: Selector Táctil Local de 2 a 5 Jugadores (Gratis)

- Detección simultánea de hasta 5 dedos sobre la pantalla táctil mediante eventos `PointerDown` / `PointerUp`.
- Generación de aros concéntricos de titanio alrededor de cada dedo con física de oscilación amortiguada.
- **Temporizador de tensión de 3.0 segundos:**
    - Segundo 0 a 2.5: Aceleración progresiva de pulsos hápticos (30 Hz a 180 Hz) y sonido de tensión creciente.
    - Segundo 3.0: Selección pseudoaleatoria criptográfica (CSPRNG) de un único perdedor.
    - El aro perdedor vibra con golpe seco sostenido y la pantalla muestra: **"TÚ PAGAS"**. Los demás aros se desvanecen suavemente.
- **Manejo de Retiro Prematuro de Dedo (Anti-Trampas):**
    - Si un jugador levanta el dedo antes de los 3.0 segundos:
        1. *Feedback Acústico:* El aro del desertor colapsa con un sonido de destensión elástica (*pitch bend* descendente de 250 Hz a 60 Hz) y su aro se disuelve en humo grafito (`#6B7280`). La vibración de tensión cesa en seco.
        2. *Reinicio Equitativo de Tensión:* Si quedan 2 o más dedos en pantalla, el temporizador de 3.0 segundos se reinicia a cero para los dedos restantes, garantizando justicia e impidiendo que alguien retire el dedo a los 2.9s para salvarse con ventaja.
        3. Si queda menos de 2 dedos, el estado regresa a espera limpia (*Idle*).

### RF-02: Modo Mesa Sincronizada QR para 6 a 30+ Jugadores

- El anfitrión selecciona el modo **"Mesa QR"** (despejado de redundancias en la barra superior: botones canónicos **"1 Celular"** y **"Mesa QR"**).
- **Pasarela Previa al Despliegue:** Si el anfitrión no posee membresía PRO activa ni un pase vigente, la app despliega de forma inmediata la **Pasarela de Monetización Escalonada (Paywall Modal)** antes de generar o exponer la sala QR.
- **Experiencia Invitado:** Tras desbloquear la mesa, los demás escanean con su cámara. Se abre una PWA ultraligera en su navegador móvil en menos de 3 segundos, sin login forzoso ni descargas.
- **Sincronización WebSockets (CC311):** Todos mantienen pulsado su botón en pantalla. El servidor WebSockets envía la señal de tensión al unísono a todos los teléfonos conectados.
- Al resolverse:
    - El teléfono del perdedor vibra fuertemente y muestra **"TÚ PAGAS"** sobre fondo titanio oscuro.
    - Los demás teléfonos vibran con un pulso suave y muestran **"A SALVO"** sobre fondo blanco perla.

### RF-03: Pasarela de Monetización Escalonada (Paywall con TTL Estricto)

- **Opciones de Pago Disponibles:**
    - **Pase de Mesa (3 Horas):** **S/ 3.50 PEN** (~$0.99 USD) para la salida de hoy.
    - **WHO PAYS? PRO:** **S/ 9.90 PEN/mes** (~$2.99 USD) o $19.99 USD vitalicio con salas QR ilimitadas y cero anuncios.
- **Ventana de Pago Estricta (TTL 3 Minutos) con Indicador Cromático Dinámico:**
    - Al abrirse el modal de la pasarela, se inicia una cuenta regresiva de **máximo 3:00 minutos (180 segundos)** visible con tipografía monoespaciada tabular `SF Mono`.
    - **Gradiente Dinámico de Urgencia (Apple HIG):**
        - **3:00 a 1:30 (180s - 90s):** Verde Esmeralda (`#30D158`) — Estado tranquilo / tiempo suficiente.
        - **1:30 a 0:30 (90s - 30s):** Amarillo Ámbar (`#FFD60A`) — Advertencia de tiempo medio.
        - **0:30 a 0:00 (30s - 0s):** Rojo Carmesí (`#FF453A`) con micro-pulsos — Urgencia crítica de expiración.
    - **Cuenta Regresiva Sonora (Últimos 10 Segundos):**
        - Del segundo 10 al 4: Pulso acústico audible rítmico (800 Hz) a cada segundo exacto.
        - Segundos 3, 2 y 1: Beep de alta frecuencia (1200 Hz).
        - Segundo 0: Tono descendente sordo (150 Hz) de expiración definitiva.
    - **Regla Estricta de Auto-Cierre por Expiración:**
        - Al llegar el contador a **00:00**, la pasarela se **cierra automáticamente**, cancela la reserva efímera, devuelve la interfaz a modo local ("1 Celular") y emite una notificación táctil/visual de sesión expirada.
- **Integraciones de Pago:**
    - Pasarelas Rápidas: Yape / Plin, Tarjetas de Débito/Crédito y Apple Pay / Google Pay.

### RF-04: El Libro de Deudas Social (Función Pro)

- **Mecanismo de Identificación del Perdedor:**
    - **Modo Local (1 solo teléfono, 2 a 5 dedos):**
        - Los sensores capacitivos de pantalla detectan coordenadas táctiles (`PointerId`), pero no la identidad física de la persona.
        - Tras declararse el veredicto con **"TÚ PAGAS"**, la app despliega fluidamente una micro-hoja inferior (Action Sheet estilo Apple HIG):
            1. **Primera Vez de Uso (Cold Start sin amigos guardados):**
                - Si la base de datos local SQLite (`CC202`) está vacía, el campo de texto se activa con foco automático: `[ Escribe su nombre o apodo... ]`.
                - Ofrece chips de sugerencia rápida: `[ Jugador 1 ]`, `[ Jugador 2 ]` si no desean escribir nombres reales.
                - Al ingresar un nombre (ej. _"Lucas"_), este se almacena automáticamente en el SQLite local.
            2. **Uso Recurrente (1 Solo Tap):**
                - Para todas las rondas posteriores, la hoja muestra directamente chips táctiles de un solo toque con los amigos recientes (`[ Lucas ]`, `[ Marco ]`, `[ Renato ]`). El perdedor solo toca su nombre en menos de 0.5 segundos.
            3. **Monto (Opcional):** Input numérico rápido para ingresar la cuenta (ej. `S/ 45.00` o `$12.00`).
            4. Si no desean registrarlo, un toque fuera de la tarjeta o deslizar hacia abajo descarta la anotación instantáneamente sin fricción.
    - **Modo Mesa Sincronizada QR (6 a 30+ jugadores):**
        - Al escanear el QR o entrar al enlace desde su propio teléfono, cada jugador ingresa su **Nombre o Alias** en 1 campo antes de mantener pulsado su botón en pantalla.
        - El WebSocket mapea internamente `socket.id -> playerName`. Al resolverse la ronda mediante el algoritmo CSPRNG, el servidor conoce directamente el nombre del perdedor y lo registra de forma 100% automática en el Libro de Deudas del anfitrión.
- **Simplificación de Deudas Cruzadas (Teoría de Grafos - `CC232`):**
    - Aplica un algoritmo de reducción de flujo en grafos dirigidos para minimizar el número de transferencias necesarias:
        - Si Juan le debe S/ 20 a Pedro y Pedro le debe S/ 20 a Marco, el grafo se simplifica a: _"Juan le paga S/ 20 a Marco"_.

---

## 6. 🎨 Requisitos No Funcionales (Estética, Rendimiento & Privacidad)

1. **Estética "Monochrome Titanium" (Apple HIG):**
    - Fondo: `#08090A` (Negro obsidiana mate).
    - Aros y Botones: Gradientes de titanio cepillado (`#26282E` a `#3A3D45`).
    - Tipografía: _San Francisco Pro_ y _SF Mono_ con cifras tabulares.
    - Cero colores neón, cero degradados estridentes, cero ilustraciones infantiles.
2. **Latencia Sub-20ms en Red:**
    - La emisión del veredicto a los clientes WebSockets debe propagarse en menos de 20 milisegundos para que todos los teléfonos reaccionen en el mismo instante perceptible.
3. **Privacidad Total (Zero Tracking):**
    - No se solicitan correos ni contraseñas a los invitados. Las sesiones de mesa son efímeras y se destruyen en RAM tras completarse.

---

## 7. 💰 Proyecciones de Negocio (Unit Economics)

- **Conversión de Alto Valor:** En una salida de 8 personas donde la cuenta es de \$60 a \$120 USD, pagar **\$1.99 USD** por el anfitrión representa menos del 2% del gasto total de la mesa.
- **Suscripción de \$9.99 USD/mes:** Altamente rentable para anfitriones recurrentes (con solo 500 suscriptores activos se generan \$5,000 USD mensuales recurrentes con costos de infraestructura inferiores a \$50 USD en Cloudflare Workers / WebSockets).

---

## 8. 📈 Métricas Clave de Éxito (KPIs)

- **Tasa de Viralidad K-Factor:** Número de invitados que posteriormente crean su propia mesa (Meta: $K > 1.8$).
- **Ratio de Conversión a Pase (\$1.99):** Porcentaje de mesas de 6+ personas que compran el ticket de partida única.
- **Ratio de Conversión a Pro (\$9.99/mes):** Porcentaje de anfitriones que migran a suscripción mensual tras su tercer pase único.
