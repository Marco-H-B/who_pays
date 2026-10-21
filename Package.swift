// swift-tools-version: 5.9
// ==============================================================================
// 👆 WHO PAYS? — Swift Package Manifest (xtool iOS Native)
// Autor: Marco Antonio Huamani Bonifacio (UNI - Ciencia de la Computación)
// Propósito: Definir la configuración de compilación nativa para iPhone 15
// ==============================================================================

import PackageDescription

let package = Package(
    name: "WhoPays",

    // --------------------------------------------
    // Plataformas soportadas desde 15 o superior.
    // --------------------------------------------
    platforms: [
        .iOS(.v15)
    ],

    // -------------------------------
    // Productos generados (Products)
    // -------------------------------
    products: [
        .executable(
            name: "WhoPays",
            targets: [
                "WhoPays"
            ]
        )
    ],

    // -----------------------
    // Targets de Compilación
    // -----------------------
    targets: [
        .executableTarget(
            name: "WhoPays",
            dependencies: [],
            path: "Sources/WhoPays"
        )
    ]
)
