// ==============================================================================
// WHO PAYS? — Vista Principal y Lienzo Multitáctil
// Autor: Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
// ==============================================================================

import SwiftUI
import UIKit

// ---------------------------------------------------------------------
// Modelo de Punto Táctil
// Representa la coordenada y estado de cada dedo detectado en pantalla
// ---------------------------------------------------------------------
struct TouchPoint: Identifiable {
    let id: UUID
    var location: CGPoint
    var startTime: Date
}

// -------------------------------------
// Máquina de Estados Finitos del Juego
// -------------------------------------
enum GameState: Equatable {
    case idle
    case tension(startDate: Date)
    case outcome(loserId: UUID)
}

struct ContentView: View {
    // Estado del juego y colección de dedos activos en pantalla
    @State private var touches: [UUID: TouchPoint] = [:]
    @State private var gameState: GameState = .idle

    // Color base de fondo: Obsidiana mate (#08090A)
    private let backgroundObsidian = Color(red: 8/255, green: 9/255, blue: 10/255)
    // Color de titanio cepillado (#3A3D45)
    private let titaniumColor = Color(red: 58/255, green: 61/255, blue: 69/255)

    var body: some View {
        ZStack {
            // Fondo base oscuro
            backgroundObsidian
                .ignoresSafeArea()

            TimelineView(.animation) { timeline in
                // ---------------------------------------
                // Lienzo de Renderizado Gráfico a 60 FPS
                // ---------------------------------------
                Canvas { context, size in
                    let currentDate = timeline.date

                    for touch in touches.values {
                        // Calculamos el radio dinamico con fisica oscilatoria
                        let currentRadius = calculateRadius(for: touch, at: currentDate)

                        let rect = CGRect(
                            x: touch.location.x - currentRadius,
                            y: touch.location.y - currentRadius,
                            width: currentRadius * 2,
                            height: currentRadius * 2
                        )
                        let circlePath = Path(ellipseIn: rect)

                        // Si este dedo es el perdedor, destacamos el aro
                        let strokeColor = isLoser(touch.id) ? Color.white : titaniumColor
                        let strokeWidth: CGFloat = isLoser(touch.id) ? 6 : 3

                        context.stroke(circlePath, with: .color(strokeColor), lineWidth: strokeWidth)
                    }
                }
            }


            // Capa transparente para captura de eventos multitáctiles del kernel
            MultitouchOverlayView { activeTouches in
                handleTouchUpdate(activeTouches)
            }
            .ignoresSafeArea()

            // Texto de indicación en estado de reposo
            if touches.isEmpty && gameState == .idle {
                Text("Pon tu dedo")
                    .font(.system(size: 16, weight: .regular, design: .default))
                    .foregroundColor(Color.gray.opacity(0.6))
            }
        }
    }

    // ------------------------------------------------
    // Lógica de Transición de Estados (State Pattern)
    // ------------------------------------------------
    private func handleTouchUpdate(_ activeTouches: [UUID: TouchPoint]) {
        let previousCount = self.touches.count
        self.touches = activeTouches

        if activeTouches.count >= 2 {
            switch gameState {
            case .idle:
                // Inicia la tension con la fecha actual
                gameState = .tension(startDate: Date())

            case .tension:
                // Si un jugador retiro el dedo antes de tiempo, reiniciamos el reloj a 3.0s
                if activeTouches.count < previousCount {
                    gameState = .tension(startDate: Date())
                }

            case .outcome:
                break
            }
        } else {
            // Si queda menos de 2 dedos, volvemos a reposo
            gameState = .idle
        }
    }

    // --------------------------------
    // Calcular la Oscilación Armonica
    // --------------------------------
    private func calculateRadius(for touch: TouchPoint, at currentDate: Date) -> CGFloat {
        switch gameState {
        case .idle:
            return 45.0

        case .tension(let startDate):
            let elapsed = currentDate.timeIntervalSince(startDate)
            let progress = min(elapsed / 3.0, 1.0)

            // Frecuencia que se acelera de 10 rad/s a 40 rad/s
            let frequency = 10.0 + (30.0 * progress)
            let oscillation = sin(elapsed * frequency) * (4.0 * (1.0 - progress * 0.5))

            return 45.0 + oscillation

        case .outcome(let loserId):
            // El aro del perdedor se expande con un golpe visual
            return touch.id == loserId ? 65.0 : 40.0
        }
    }
}

// -------------------------------------------------------------------------
// Puente con UIKit para Detección Multitáctil (2 a 5 dedos simultáneos)
// SwiftUI puro solo detecta 1 dedo con DragGesture; este componente activa
// isMultipleTouchEnabled = true a nivel de hardware.
// -------------------------------------------------------------------------
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
