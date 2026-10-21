// ==============================================================================
// WHO PAYS? — Vista Principal y Lienzo Multitáctil
// Autor: Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
// Plataforma: iOS 15+ (SwiftUI Nativo a 60 FPS)
// ==============================================================================

import SwiftUI
import UIKit
import AudioToolbox

// ------------------------------------------------------------------------------
// Modelo de Punto Táctil
// Representa la coordenada y estado de cada dedo detectado en pantalla
// ------------------------------------------------------------------------------
struct TouchPoint: Identifiable {
    let id: UUID
    var location: CGPoint
    var startTime: Date
}

// ------------------------------------------------------------------------------
// Máquina de Estados Finitos del Juego
// ------------------------------------------------------------------------------
enum GameState: Equatable {
    case idle
    case tension(startDate: Date)
    case outcome(loserId: UUID)
}

struct ContentView: View {
    // Coleccion de dedos activos y estado del motor de juego
    @State private var touches: [UUID: TouchPoint] = [:]
    @State private var gameState: GameState = .idle
    @State private var showDebtSheet: Bool = false
    @State private var loserCustomName: String = ""
    @State private var billAmount: String = ""

    // Paleta de color canónica: Monochrome Titanium
    private let backgroundObsidian = Color(red: 8/255, green: 9/255, blue: 10/255)
    private let titaniumColor = Color(red: 58/255, green: 61/255, blue: 69/255)
    private let titaniumGlow = Color(red: 220/255, green: 220/255, blue: 230/255)

    var body: some View {
        ZStack {
            // Fondo oscuro base para paneles OLED
            backgroundObsidian
                .ignoresSafeArea()

            // ------------------------------------------------------------------
            // Lienzo Gráfico Continuo a 60 FPS
            // ------------------------------------------------------------------
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    let currentDate = timeline.date

                    for touch in touches.values {
                        let radius = calculateRadius(for: touch, at: currentDate)
                        let rect = CGRect(
                            x: touch.location.x - radius,
                            y: touch.location.y - radius,
                            width: radius * 2,
                            height: radius * 2
                        )
                        let circlePath = Path(ellipseIn: rect)

                        if isLoser(touch.id) {
                            // Aro del perdedor en titanio brillante de alto contraste
                            context.stroke(circlePath, with: .color(titaniumGlow), lineWidth: 5)
                            let innerRect = rect.insetBy(dx: 8, dy: 8)
                            context.stroke(Path(ellipseIn: innerRect), with: .color(Color.white.opacity(0.4)), lineWidth: 2)
                        } else {
                            // Aros regulares en titanio mate
                            let strokeColor = isSafe(touch.id) ? titaniumColor.opacity(0.3) : titaniumColor
                            context.stroke(circlePath, with: .color(strokeColor), lineWidth: 3)
                        }
                    }
                }
            }

            // Capa para etiquetas de texto sobre cada dedo
            if case .outcome(let loserId) = gameState {
                ForEach(Array(touches.values)) { touch in
                    Text(touch.id == loserId ? "TÚ PAGAS" : "A SALVO")
                        .font(.system(size: touch.id == loserId ? 18 : 13, weight: .bold, design: .monospaced))
                        .foregroundColor(touch.id == loserId ? Color.white : Color.gray.opacity(0.7))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(touch.id == loserId ? Color.black.opacity(0.8) : Color.clear)
                        .cornerRadius(6)
                        .position(x: touch.location.x, y: touch.location.y - 75)
                }
            }

            // Capa transparente para captura de eventos multitáctiles del kernel
            MultitouchOverlayView { activeTouches in
                handleTouchUpdate(activeTouches)
            }
            .ignoresSafeArea()

            // Indicador cuando la pantalla esta en reposo
            if touches.isEmpty && gameState == .idle {
                VStack(spacing: 8) {
                    Text("Pon tu dedo")
                        .font(.system(size: 17, weight: .medium, design: .default))
                        .foregroundColor(Color.gray.opacity(0.6))
                    Text("2 a 5 jugadores en pantalla")
                        .font(.system(size: 13, weight: .regular, design: .default))
                        .foregroundColor(Color.gray.opacity(0.35))
                }
            }

            // Barra inferior rapida al resolverse para registrar en el Libro de Deudas
            if case .outcome = gameState {
                VStack {
                    Spacer()
                    Button(action: { showDebtSheet = true }) {
                        HStack(spacing: 8) {
                            Image(systemName: "book.closed")
                            Text("Anotar en Libro de Deudas")
                                .font(.system(size: 14, weight: .semibold, design: .rounded))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(Color(red: 25/255, green: 26/255, blue: 30/255))
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(titaniumColor, lineWidth: 1)
                        )
                    }
                    .padding(.bottom, 36)
                }
            }
        }
        // Micro-hoja inferior (Action Sheet) para asignar nombre al perdedor
        .sheet(isPresented: $showDebtSheet) {
            DebtTaggingSheet(
                loserName: $loserCustomName,
                amount: $billAmount,
                onSave: {
                    showDebtSheet = false
                }
            )
        }
    }

    // --------------------------------------------------------------------------
    // Logica de Transicion de Estados y Protocolo Anti-Trampas
    // --------------------------------------------------------------------------
    private func handleTouchUpdate(_ activeTouches: [UUID: TouchPoint]) {
        let previousCount = self.touches.count
        self.touches = activeTouches

        if activeTouches.count >= 2 {
            switch gameState {
            case .idle:
                // Inicia la cuenta de tension
                let startDate = Date()
                gameState = .tension(startDate: startDate)
                scheduleOutcomeResolution(for: startDate)
                triggerHapticPulse(intensity: 0.5)

            case .tension:
                // Si alguien retira el dedo antes de tiempo, reinicia el reloj a 3.0s
                if activeTouches.count < previousCount {
                    let newStartDate = Date()
                    gameState = .tension(startDate: newStartDate)
                    scheduleOutcomeResolution(for: newStartDate)
                    triggerHapticAbort()
                }

            case .outcome:
                // La ronda ya concluyo; se mantiene hasta que todos suelten
                break
            }
        } else {
            // Menos de 2 dedos regresa a reposo
            if gameState != .idle {
                gameState = .idle
            }
        }
    }

    // --------------------------------------------------------------------------
    // Resolucion Criptografica de Veredicto (CSPRNG a los 3.0 segundos)
    // --------------------------------------------------------------------------
    private func scheduleOutcomeResolution(for startDate: Date) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            if case .tension(let currentStartDate) = self.gameState, currentStartDate == startDate {
                if let selectedLoserId = self.touches.keys.randomElement() {
                    self.gameState = .outcome(loserId: selectedLoserId)
                    self.triggerHapticOutcome()
                }
            }
        }
    }

    // --------------------------------------------------------------------------
    // Ecuacion de Oscilacion Armonica Amortiguada
    // --------------------------------------------------------------------------
    private func calculateRadius(for touch: TouchPoint, at currentDate: Date) -> CGFloat {
        switch gameState {
        case .idle:
            return 45.0

        case .tension(let startDate):
            let elapsed = currentDate.timeIntervalSince(startDate)
            let progress = min(elapsed / 3.0, 1.0)
            let frequency = 12.0 + (32.0 * progress)
            let oscillation = sin(elapsed * frequency) * (5.0 * (1.0 - progress * 0.4))
            return 45.0 + oscillation

        case .outcome(let loserId):
            return touch.id == loserId ? 70.0 : 38.0
        }
    }

    private func isLoser(_ id: UUID) -> Bool {
        if case .outcome(let loserId) = gameState {
            return id == loserId
        }
        return false
    }

    private func isSafe(_ id: UUID) -> Bool {
        if case .outcome(let loserId) = gameState {
            return id != loserId
        }
        return false
    }

    // --------------------------------------------------------------------------
    // Retroalimentacion Haptica Nativa (Taptic Engine)
    // --------------------------------------------------------------------------
    private func triggerHapticPulse(intensity: CGFloat) {
        let generator = UIImpactFeedbackGenerator(style: .rigid)
        generator.prepare()
        generator.impactOccurred(intensity: intensity)
    }

    private func triggerHapticAbort() {
        let generator = UIImpactFeedbackGenerator(style: .soft)
        generator.impactOccurred(intensity: 0.4)
    }

    private func triggerHapticOutcome() {
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.error)
        AudioServicesPlaySystemSound(1521)
    }
}

// ------------------------------------------------------------------------------
// Micro-hoja (Action Sheet) de Registro de Deuda
// ------------------------------------------------------------------------------
struct DebtTaggingSheet: View {
    @Binding var loserName: String
    @Binding var amount: String
    var onSave: () -> Void

    private let slateBackground = Color(red: 18/255, green: 19/255, blue: 22/255)

    var body: some View {
        ZStack {
            slateBackground
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 20) {
                Text("Registrar Perdedor")
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .foregroundColor(.white)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Nombre o Apodo")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.gray)
                    TextField("Escribe el nombre...", text: $loserName)
                        .textFieldStyle(.plain)
                        .padding(12)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(8)
                        .foregroundColor(.white)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Monto de la cuenta (Opcional)")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.gray)
                    TextField("S/ 0.00", text: $amount)
                        .textFieldStyle(.plain)
                        .keyboardType(.decimalPad)
                        .padding(12)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(8)
                        .foregroundColor(.white)
                }

                Button(action: onSave) {
                    Text("Guardar en Libro de Deudas")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color(red: 45/255, green: 48/255, blue: 55/255))
                        .cornerRadius(10)
                }
                .padding(.top, 10)

                Spacer()
            }
            .padding(24)
        }
        .presentationDetents([.fraction(0.45)])
    }
}

// ------------------------------------------------------------------------------
// Puente UIKit para Captura de Gestos Multitactiles (isMultipleTouchEnabled = true)
// ------------------------------------------------------------------------------
struct MultitouchOverlayView: UIViewRepresentable {
    var onTouchesChanged: ([UUID: TouchPoint]) -> Void

    func makeUIView(context: Context) -> MultitouchUIView {
        let view = MultitouchUIView()
        view.isMultipleTouchEnabled = true
        view.backgroundColor = .clear
        view.coordinator = context.coordinator
        return view
    }

    func updateUIView(_ uiView: MultitouchUIView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onTouchesChanged: onTouchesChanged)
    }

    class Coordinator {
        var onTouchesChanged: ([UUID: TouchPoint]) -> Void
        var touchMap: [UITouch: UUID] = [:]
        var activePoints: [UUID: TouchPoint] = [:]

        init(onTouchesChanged: @escaping ([UUID: TouchPoint]) -> Void) {
            self.onTouchesChanged = onTouchesChanged
        }

        func syncTouches(from view: UIView, event: UIEvent?) {
            guard let allTouches = event?.allTouches else { return }

            var updatedPoints: [UUID: TouchPoint] = [:]

            for touch in allTouches {
                if touch.phase == .began || touch.phase == .moved || touch.phase == .stationary {
                    let id = touchMap[touch] ?? UUID()
                    touchMap[touch] = id
                    let location = touch.location(in: view)
                    let point = TouchPoint(id: id, location: location, startTime: Date())
                    updatedPoints[id] = point
                } else if touch.phase == .ended || touch.phase == .cancelled {
                    touchMap.removeValue(forKey: touch)
                }
            }

            self.activePoints = updatedPoints
            onTouchesChanged(updatedPoints)
        }
    }
}

class MultitouchUIView: UIView {
    weak var coordinator: MultitouchOverlayView.Coordinator?

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        coordinator?.syncTouches(from: self, event: event)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        coordinator?.syncTouches(from: self, event: event)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        coordinator?.syncTouches(from: self, event: event)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        coordinator?.syncTouches(from: self, event: event)
    }
}
