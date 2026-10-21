// ==============================================================================
// WHO PAYS? — Vista Principal y Lienzo Multitáctil
// Autor: Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
// ==============================================================================

import SwiftUI
import UIKit

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

            // ------------------------------------------------------------------
            // Lienzo de Renderizado Gráfico a 60 FPS
            // ------------------------------------------------------------------
            Canvas { context, size in
                for touch in touches.values {
                    let rect = CGRect(
                        x: touch.location.x - 45,
                        y: touch.location.y - 45,
                        width: 90,
                        height: 90
                    )
                    let circlePath = Path(ellipseIn: rect)
                    context.stroke(circlePath, with: .color(titaniumColor), lineWidth: 3)
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

    // --------------------------------------------------------------------------
    // Lógica de Transición de Estados (State Pattern)
    // --------------------------------------------------------------------------
    private func handleTouchUpdate(_ activeTouches: [UUID: TouchPoint]) {
        self.touches = activeTouches

        if activeTouches.count >= 2 {
            if gameState == .idle {
                gameState = .tension(startDate: Date())
            }
        } else {
            gameState = .idle
        }
    }
}

// ------------------------------------------------------------------------------
// Puente con UIKit para Detección Multitáctil (2 a 5 dedos simultáneos)
// SwiftUI puro solo detecta 1 dedo con DragGesture; este componente activa
// isMultipleTouchEnabled = true a nivel de hardware.
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
