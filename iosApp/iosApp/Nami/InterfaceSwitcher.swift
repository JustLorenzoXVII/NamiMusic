import SwiftUI
import UIKit

/// Passaggio tra l'interfaccia Material (Compose) e quella Liquid Glass (SwiftUI).
///
/// Si fotografa lo schermo così com'è e la foto resta sopra a tutto mentre, sotto, si cambia
/// interfaccia senza animazioni (niente salti a metà). Poi la foto si anima da sola con UIKit,
/// che è fluido anche quando sotto c'è l'interfaccia Compose:
/// - verso Liquid Glass: la vecchia interfaccia si stacca come una card, si sfoca nel vetro,
///   si rimpicciolisce e sparisce; quella nuova arriva ingrandendosi appena.
/// - verso Material: il vetro si "scioglie": la foto si sfoca, si ingrandisce e sfuma via.
struct InterfaceSwitcher<Ios: View, Compose: View>: View {
    @ViewBuilder let ios: () -> Ios
    @ViewBuilder let compose: () -> Compose
    @StateObject private var app = NamiApp.shared
    @State private var showingIos = NamiApp.shared.iosUI
    @State private var arriving = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            Group {
                if showingIos {
                    ios()
                        .id(app.language)
                } else {
                    compose()
                }
            }
            .scaleEffect(arriving ? 1.05 : 1)
            .opacity(arriving ? 0.6 : 1)
        }
        .onChange(of: app.language) { _, _ in
            InterfaceTransition.crossfade()
        }
        .onChange(of: app.iosUI) { _, newValue in
            guard newValue != showingIos else { return }
            InterfaceTransition.run(towardsIos: newValue) {
                // Sotto la foto: cambio secco, senza animazioni
                var transaction = Transaction()
                transaction.disablesAnimations = true
                withTransaction(transaction) {
                    showingIos = newValue
                    arriving = true
                }
            } reveal: {
                withAnimation(.spring(response: 0.7, dampingFraction: 0.88)) { arriving = false }
            }
        }
    }
}

enum InterfaceTransition {
    private static var running = false

    static func run(towardsIos: Bool, swap: @escaping () -> Void, reveal: @escaping () -> Void) {
        guard !running, let window = keyWindow, let snapshot = window.snapshotView(afterScreenUpdates: false) else {
            swap()
            reveal()
            return
        }
        running = true

        // La foto dello schermo, con un velo di vetro sopra che all'inizio è trasparente
        let container = UIView(frame: window.bounds)
        container.isUserInteractionEnabled = true
        container.backgroundColor = .clear
        snapshot.frame = container.bounds
        snapshot.layer.cornerCurve = .continuous
        snapshot.clipsToBounds = true
        container.addSubview(snapshot)
        let blur = UIVisualEffectView(effect: nil)
        blur.frame = container.bounds
        blur.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        snapshot.addSubview(blur)
        let tint = UIView(frame: container.bounds)
        tint.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        tint.backgroundColor = towardsIos
            ? UIColor(red: 0.55, green: 0.56, blue: 1.0, alpha: 1)
            : UIColor(red: 0.35, green: 0.75, blue: 0.7, alpha: 1)
        tint.alpha = 0
        snapshot.addSubview(tint)
        window.addSubview(container)

        // I fogli aperti (impostazioni) si chiudono sotto la foto
        window.rootViewController?.presentedViewController?.dismiss(animated: false)

        // Sotto la foto si cambia interfaccia
        swap()

        let screenRadius: CGFloat = 52
        if towardsIos {
            // 1) la vecchia interfaccia si stacca come una card e diventa vetro
            snapshot.layer.cornerRadius = screenRadius
            let lift = UIViewPropertyAnimator(duration: 0.45, dampingRatio: 0.9) {
                snapshot.transform = CGAffineTransform(scaleX: 0.88, y: 0.88)
                blur.effect = UIBlurEffect(style: .systemUltraThinMaterial)
                tint.alpha = 0.18
            }
            lift.addCompletion { _ in
                // 2) la card scivola giù e sparisce, la nuova interfaccia arriva
                reveal()
                let away = UIViewPropertyAnimator(duration: 0.55, dampingRatio: 0.95) {
                    snapshot.transform = CGAffineTransform(translationX: 0, y: window.bounds.height * 0.18).scaledBy(x: 0.7, y: 0.7)
                    snapshot.alpha = 0
                }
                away.addCompletion { _ in
                    container.removeFromSuperview()
                    running = false
                }
                away.startAnimation()
            }
            lift.startAnimation()
        } else {
            // 1) il vetro si "scioglie": sfocatura e colore
            let melt = UIViewPropertyAnimator(duration: 0.4, curve: .easeIn) {
                blur.effect = UIBlurEffect(style: .systemThickMaterial)
                tint.alpha = 0.22
                snapshot.transform = CGAffineTransform(scaleX: 1.04, y: 1.04)
            }
            melt.addCompletion { _ in
                // 2) si allarga e sfuma, scoprendo l'interfaccia Material
                reveal()
                let fade = UIViewPropertyAnimator(duration: 0.55, curve: .easeOut) {
                    snapshot.transform = CGAffineTransform(scaleX: 1.15, y: 1.15)
                    snapshot.alpha = 0
                }
                fade.addCompletion { _ in
                    container.removeFromSuperview()
                    running = false
                }
                fade.startAnimation()
            }
            melt.startAnimation()
        }
    }

    /// Cambio lingua: la foto dello schermo sfuma sulla nuova interfaccia.
    static func crossfade() {
        guard let window = keyWindow, let snapshot = window.snapshotView(afterScreenUpdates: false) else { return }
        snapshot.frame = window.bounds
        window.addSubview(snapshot)
        let fade = UIViewPropertyAnimator(duration: 0.45, curve: .easeInOut) { snapshot.alpha = 0 }
        fade.addCompletion { _ in snapshot.removeFromSuperview() }
        fade.startAnimation(afterDelay: 0.05)
    }

    private static var keyWindow: UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
    }
}
