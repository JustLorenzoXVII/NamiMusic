import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

/// Interruttore "Tema iOS" acceso: la nuova interfaccia in SwiftUI (Nami/…);
/// spento: l'interfaccia completa di sempre, in Compose.
struct ContentView: SwiftUI.View {
    @StateObject private var app = NamiApp.shared

    var body: some SwiftUI.View {
        if app.iosUI {
            NamiRootView()
                .transition(.opacity)
        } else {
            ComposeView()
                .ignoresSafeArea()
                .transition(.opacity)
        }
    }
}
