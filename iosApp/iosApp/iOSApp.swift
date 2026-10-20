import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Barra di vetro del tema iOS: va registrata prima che parta l'interfaccia
        GlassChrome.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
