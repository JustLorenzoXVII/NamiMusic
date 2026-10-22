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
/// spento: l'interfaccia completa di sempre, in Compose. Il passaggio è animato (InterfaceSwitcher).
struct ContentView: SwiftUI.View {
    var body: some SwiftUI.View {
        InterfaceSwitcher(
            ios: { NamiRootView() },
            compose: { ComposeView().ignoresSafeArea() }
        )
    }
}
