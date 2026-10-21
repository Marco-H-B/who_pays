# 🚀 Plan de Implementación Paso a Paso — WHO PAYS?

> **Documento:** 06 de 06 (Estándar Canónico de Planificación)
> **Producto:** WHO PAYS? (El Selector Social Táctil & Motor de Decisión P2P)
> **Ubicación:** `E:\development\business\mobile\social\who_pays\docs\06_IMPLEMENTATION_PLAN.md`
> **Autor:** Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
> **Metodología:** Spec-Driven Development & Test-Driven Development (TDD)

---

## 📅 Hitos de Desarrollo Cronológico

### Fase 1: Motor Táctil Nativo & Física de Aros Local (Días 1 a 2)

- [ ] **Hito 1.1:** Setup del lienzo Canvas Nativo a 60 FPS fijos (iPhone 15) / 120 FPS adaptativo (SwiftUI Canvas en iOS / Compose en Android).
- [ ] **Hito 1.2:** Detección multitáctil nativa del kernel (hasta 5 dedos simultáneos) con tracking en $O(1)$.
- [ ] **Hito 1.3:** Implementación de la ecuación de oscilador armónico amortiguado para los aros.
- [ ] **Hito 1.4:** Pruebas TDD del generador CSPRNG con verificación estadística $\chi^2$.

### Fase 2: Motor Háptico & Tensión Sonora (Día 3)

- [ ] **Hito 2.1:** Modulación de frecuencia de vibración nativa (30 Hz a 180 Hz) en la ventana de 3.0s ("The Labor Illusion") vía `CoreHaptics` en iOS y `VibrationEffect` en Android.
- [ ] **Hito 2.2:** Síntesis de sonido de tensión acústica con osciladores en memoria (cero assets pesados).
- [ ] **Hito 2.3:** Animación de veredicto con colapso de aros, revelación de _"TÚ PAGAS"_ y Action Sheet de 1 tap para registro del perdedor en el Libro de Deudas.

### Fase 3: Modo Mesa QR & Señalización WebSockets (Días 4 a 5)

- [ ] **Hito 3.1:** Servidor de señalización WebSockets ligero en Cloudflare Workers / Node.js.
- [ ] **Hito 3.2:** Generación y renderizado de código QR en pantalla de anfitrión.
- [ ] **Hito 3.3:** Cliente web ultraligero para invitados (Vanilla TS + HTML5 Canvas con carga en $< 1.0$ segundo y campo de nombre/alias).
- [ ] **Hito 3.4:** Sincronización de pulsos y veredicto simultáneo en 10+ dispositivos con registro automático del perdedor.

### Fase 4: Integración de Pagos, Timeout de 3 Minutos & Despliegue (Días 6 a 7)

- [ ] **Hito 4.1:** Integración de checkout overlay de Lemon Squeezy ($1.99 pase único y $9.99/mes Pro) y webhook firmado HMAC-SHA256 con TTL estricto de 3 minutos e indicador dinámico de color.
- [ ] **Hito 4.2:** Soporte de compras integradas nativas (Google Play Billing / Apple StoreKit 2).
- [ ] **Hito 4.3:** Auditoría del Checklist de Seguridad de 20 Puntos y despliegue a producción.
