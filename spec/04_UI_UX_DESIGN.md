# 🎨 UI/UX Design Brief — WHO PAYS?

> **Documento:** 04 de 06 (Estándar Canónico de Planificación)
> **Producto:** WHO PAYS? (El Selector Social Táctil & Motor de Decisión P2P)
> **Ubicación:** `E:\development\business\mobile\social\who_pays\docs\04_UI_UX_DESIGN.md`
> **Autor:** Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
> **Filosofía:** Apple Human Interface Guidelines | Monochrome Titanium | The Labor Illusion

---

## 1. 🖤 Paleta Cromática: "Monochrome Titanium" (Cero Neón)

| Elemento                    | Token de Color      | Valor Hex | Propósito Visual                                             |
| :-------------------------- | :------------------ | :-------- | :----------------------------------------------------------- |
| **Fondo Base**              | `--bg-obsidian`     | `#08090A` | Negro carbón mate para pantallas OLED (ahorro de batería).   |
| **Superficie de Paneles**   | `--surface-slate`   | `#121316` | Gris grafito mate con elevación de 1dp.                      |
| **Bordes Milimétricos**     | `--border-titanium` | `#1E2025` | Trazos finos de 1px sólido para delimitación de componentes. |
| **Texto de Alta Jerarquía** | `--text-primary`    | `#F4F4F6` | Blanco perla puro para veredictos y números clave.           |
| **Texto Secundario**        | `--text-muted`      | `#6B7280` | Gris neutro para instrucciones discretas.                    |
| **Aro del Perdedor**        | `--accent-titanium` | `#3A3D45` | Titanio oscuro cepillado de alto contraste.                  |
| **Aros a Salvo**            | `--accent-safe`     | `#FFFFFF` | Blanco puro con desvanecimiento alfa gradual a 0%.           |

---

## 2. 🔤 Tipografía y Escala de Texto

- **Fuente Principal:** _San Francisco Pro Display_ (iOS / macOS) y _Inter_ (Web / Android).
- **Fuente Tabular / Datos:** _SF Mono_ con cifras tabulares fijas (`font-variant-numeric: tabular-nums`) para cronómetros y cuentas de jugadores.
- **Escala:**
    - Veredicto Principal: `32px` Bold / Tracking `-0.02em`.
    - Microcopy Central: `15px` Regular / Tracking `0`.
    - Leyendas de Sala: `13px` Monoespaciado / Uppercase.

---

## 3. ⏳ Psicología de Interacción: "The Labor Illusion" (Buell & Norton, Harvard)

Un pilar maestro que rige el diseño de WHO PAYS? es **La Ilusión del Trabajo**:

1. **La Paradoja de la Inmediatez:**
    - Si la app determinara al perdedor en 1 milisegundo, el cerebro humano interpretaría el resultado como superficial, artificial o poco fidedigno.
2. **El Valor del Esfuerzo Percibido:**
    - La ventana deliberada de **3.0 segundos de tensión** no es una pantalla de carga vacía: es un ritual cinemático donde el usuario _ve y siente el cálculo_:
        - Los aros se expanden y contraen al compás de un oscilador armónico.
        - La respuesta háptica del motor de vibración acelera su pulso progresivamente simulando taquicardia.
        - En el modo QR, se muestra un micro-indicador: _"Sincronizando 10 nodos P2P... Verificando semilla..."_.
    - Al experimentar la fricción y el proceso, los usuarios aceptan el veredicto con absoluta solemnidad y diversión.

---

## 4. 🪟 Componentes Clave de Interfaz Nativa (Apple HIG)

### 4.1. Action Sheet de Etiquetado Rápido del Perdedor (Modo Local)

- **Aparición:** Emerge suavemente tras el veredicto "TÚ PAGAS" usando física de resorte (`spring()`).
- **Estado Inicial (Primera Vez / Cold Start):**
    - Si no existen amigos registrados en el SQLite local, la hoja despliega un campo de texto con foco automático y teclado oscuro: `[ Escribe su nombre o apodo... ]`.
    - Presenta botones de rescate rápido `[ Jugador 1 ]`, `[ Jugador 2 ]`.
    - Al presionar Enter o "Guardar", el nombre se almacena permanentemente y a partir de ese momento se convierte en un chip de 1 solo toque.
- **Estado Recurrente (1 Tap):** Botones redondeados con borde de titanio de 1px con los nombres de la lista de amigos/grupo activo (ej. `[Marco]` `[Lucas]` `[Renato]`).
- **Campo de Monto:** Input numérico discreto con tipografía tabular `SF Mono` para asentar la cuenta del grupo.
- **Cero Fricción:** Tocar cualquier punto fuera de la hoja la colapsa de inmediato sin forzar el registro.

### 4.2. Modal Paywall de Activación QR con Temporizador de 3 Minutos & Color Dinámico

- **Temporizador Circular:** Cuenta regresiva `03:00` en tipografía tabular monospaced (`SF Mono`) con indicador visual de urgencia cromática continua:
    - **180s a 90s (03:00 - 01:30):** Verde Esmeralda (`#30D158`) — Tono relajado.
    - **90s a 30s (01:30 - 00:30):** Amarillo Ámbar (`#FFD60A`) — Tono de advertencia.
    - **30s a 0s (00:30 - 00:00):** Rojo Carmesí (`#FF453A`) — Pulso sutil de advertencia final en los últimos 15 segundos.
    - **Cuenta Sonora en Últimos 10 Segundos:** Pulsos rítmicos audibles (800 Hz) del segundo 10 al 4, beeps dobles en 1200 Hz del segundo 3 al 1, y beep de cancelación en 150 Hz al llegar a 00:00.
- **Botones de Pago:** Integración nativa de Apple Pay (en iOS vía StoreKit) y Google Pay / Yape en 1 solo toque.
- **Expiración Fluida:** Si el cronómetro llega a `00:00`, el modal transiciona elegantemente a un estado de sesión expirada con opción de reintentar.

### 4.3. Renderizado Nativo Multi-Frecuencia (60 Hz iPhone 15 / 90 Hz / 120 Hz ProMotion)

- **Hardware Principal de Referencia (iPhone 15):** Pantalla Super Retina XDR a 60 Hz nativos (presupuesto de 16.6ms por cuadro). Cero pausas de renderizado y control háptico milimétrico con el Taptic Engine (`CoreHaptics`).
- **Soporte Multi-Frecuencia Desacoplado ($\Delta t$):**
  - Dispositivos 60 Hz (iPhone 15, Android estándar): Renderizado fluido a 60 FPS.
  - Dispositivos 90 Hz (Android gama media): Renderizado a 90 FPS.
  - Dispositivos 120 Hz (iPhone Pro / iPads / Android LTPO): Renderizado dinámico ProMotion a 120 FPS.
  - **Invarianza Temporal:** Todas las animaciones, aros y física utilizan el tiempo continuo ($\Delta t$), garantizando que la experiencia dure exactamente 3.0 segundos en cualquier pantalla sin acelerarse ni ralentizarse.
- **iOS:** Implementado en **SwiftUI** con `TimelineView(.animation)` o `Canvas` acelerado por Metal.
- **Android:** Implementado en **Jetpack Compose** con `Canvas` y `withFrameNanos`.

---

## 5. 🔊 Arquitectura de Audio & Acoplamiento Háptico-Visual

El diseño sonoro se fusiona con la física de la animación en 4 compases cinemáticos:

1. **Compás 1: Contacto Inicial (0.0s):**
    - _Visual:_ El aro nace del dedo y se expande con rebote armónico.
    - _Audio:_ Micro-pop percusivo metálico en 100 Hz con decaimiento rápido de 40ms.
    - _Háptico:_ Micro-tap sutil (tipo teclado de cristal).
2. **Compás 2: Rampa de Suspenso "The Labor Illusion" (0.0s a 2.95s):**
    - _Visual:_ Los aros oscilan a frecuencia creciente contrayéndose hacia el centro.
    - _Audio:_ Drone subgrave con síntesis FM que experimenta una rampa de aceleración de 60 a 300 BPM y un glissando de tono desde 110 Hz ($A_2$) hasta 440 Hz ($A_4$).
    - _Háptico:_ Vibración continua con modulación de frecuencia que sube de 30 Hz a 180 Hz en sincronía con el tono de audio.
3. **Compás 3: Caída al Vacío / El Silencio Táctil (2.95s a 3.0s):**
    - _Visual:_ Los aros se congelan por 50 milisegundos.
    - _Audio:_ **Corte súbito de sonido (silencio absoluto de 50ms)**. La ausencia repentina de estímulo genera una descarga inmediata de adrenalina en los jugadores.
    - _Háptico:_ Cese instantáneo de vibración.
4. **Compás 4: Veredicto Resolutivo (3.0s):**
    - _Perdedor:_ Golpe seco y profundo ("Titanium Thud" a 60 Hz con caída abrupta) + pulso háptico sostenido de 180ms + texto "TÚ PAGAS".
    - _A Salvo:_ Acorde mayor etéreo en tríada brillante con decaimiento suave de 1.2 segundos + pulso háptico de desvanecimiento suave.
5. **Compás Anómalo: Retiro de Dedo Prematuro (< 3.0s):**
    - _Visual:_ El aro del desertor colapsa con desvanecimiento gris grafito (`#6B7280`). Si quedan $\ge 2$ dedos, sus aros parpadean brevemente y reinician la oscilación.
    - _Audio:_ Efecto de destensión elástica (*pitch bend* descendente de 250 Hz a 60 Hz en 80ms) simulando una cuerda que pierde tensión de golpe. El drone se resetea a su frecuencia base (110 Hz).
    - _Háptico:_ Cese seco inmediato de vibración (0ms de inercia).
