import SwiftUI
import UIKit

/// Passaggio tra l'interfaccia Material (Compose) e quella Liquid Glass (SwiftUI).
///
/// Durante il passaggio le due interfacce sono vive una sopra l'altra: quella che se ne va sta
/// sopra e si anima, quella nuova è già pronta sotto. Niente foto dello schermo (con Compose, che
/// disegna con Metal, la foto usciva grigia).
/// - verso Liquid Glass: Material si stacca come una card con gli angoli dello schermo, si
///   copre di vetro, poi scivola giù rimpicciolendosi e sparisce; Liquid Glass si assesta da 1.05.
/// - verso Material: il vetro si "scioglie" (si sfoca e si allarga) e sfuma, scoprendo Material.
struct InterfaceSwitcher<Ios: View, Compose: View>: View {
    @ViewBuilder let ios: () -> Ios
    @ViewBuilder let compose: () -> Compose
    @StateObject private var app = NamiApp.shared

    private enum Side { case ios, compose }

    /// Interfaccia mostrata (quella "sotto" durante il passaggio)
    @State private var current: Side = NamiApp.shared.iosUI ? .ios : .compose
    /// Interfaccia che se ne va (sopra), finché l'animazione non finisce
    @State private var leaving: Side?
    /// 0 ferma, 1 staccata e coperta di vetro, 2 andata via
    @State private var phase = 0
    @State private var arriving = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if current == .ios || leaving == .ios {
                layer(.ios) { ios().id(app.language) }
            }
            if current == .compose || leaving == .compose {
                layer(.compose) { compose() }
            }
        }
        .onChange(of: app.iosUI) { _, enabled in
            switchTo(enabled ? .ios : .compose)
        }
        .onChange(of: app.language) { _, _ in
            InterfaceTransition.crossfade()
        }
    }

    @ViewBuilder
    private func layer<Content: View>(_ side: Side, @ViewBuilder content: () -> Content) -> some View {
        let isLeaving = leaving == side
        let towardsIos = leaving == .compose
        content()
            .scaleEffect(scale(isLeaving: isLeaving, towardsIos: towardsIos))
            .offset(y: isLeaving && towardsIos && phase == 2 ? 140 : 0)
            .overlay {
                if isLeaving {
                    GlassVeil(towardsIos: towardsIos, strength: phase == 0 ? 0 : 1)
                        .ignoresSafeArea()
                        .allowsHitTesting(false)
                }
            }
            // La maschera copre tutto lo schermo (anche sotto l'ora): a riposo non taglia niente
            .mask {
                RoundedRectangle(cornerRadius: isLeaving && phase > 0 ? 52 : 0, style: .continuous)
                    .ignoresSafeArea()
            }
            .opacity(isLeaving && phase == 2 ? 0 : 1)
            .shadow(color: .black.opacity(isLeaving && phase == 1 ? 0.35 : 0), radius: 30, y: 16)
            .zIndex(isLeaving ? 1 : 0)
            .allowsHitTesting(!isLeaving)
    }

    private func scale(isLeaving: Bool, towardsIos: Bool) -> CGFloat {
        if isLeaving {
            switch phase {
            case 1: return towardsIos ? 0.88 : 1.04
            case 2: return towardsIos ? 0.72 : 1.16
            default: return 1
            }
        }
        return arriving ? 1.05 : 1
    }

    private func switchTo(_ target: Side) {
        guard target != current, leaving == nil else { return }
        // Chiude i fogli aperti (impostazioni) prima di partire
        InterfaceTransition.dismissPresented()
        var instant = Transaction()
        instant.disablesAnimations = true
        withTransaction(instant) {
            leaving = current
            current = target
            phase = 0
            arriving = true
        }
        // 1) la vecchia interfaccia si stacca e diventa vetro
        withAnimation(.spring(response: 0.42, dampingFraction: 0.9)) { phase = 1 }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.42) {
            // 2) se ne va, la nuova si assesta
            withAnimation(.spring(response: 0.55, dampingFraction: 0.92)) {
                phase = 2
                arriving = false
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                withTransaction(instant) {
                    leaving = nil
                    phase = 0
                }
            }
        }
    }
}

/// Il velo di vetro colorato sopra l'interfaccia che se ne va.
private struct GlassVeil: View {
    let towardsIos: Bool
    let strength: Double

    var body: some View {
        ZStack {
            Rectangle().fill(towardsIos ? .ultraThinMaterial : .thickMaterial)
            LinearGradient(
                colors: towardsIos
                    ? [Color(red: 0.55, green: 0.56, blue: 1.0).opacity(0.35), Color(red: 0.95, green: 0.5, blue: 0.75).opacity(0.25)]
                    : [Color(red: 0.35, green: 0.8, blue: 0.7).opacity(0.35), Color(red: 0.3, green: 0.4, blue: 0.95).opacity(0.3)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Image(systemName: towardsIos ? "sparkles" : "square.grid.2x2.fill")
                .font(.system(size: 54, weight: .semibold))
                .foregroundStyle(.white)
                .scaleEffect(0.7 + 0.3 * strength)
        }
        .opacity(strength)
    }
}

enum InterfaceTransition {
    static func dismissPresented() {
        keyWindow?.rootViewController?.presentedViewController?.dismiss(animated: false)
    }

    /// Cambio lingua: la foto dello schermo sfuma sulla nuova interfaccia (solo SwiftUI, quindi la foto viene bene).
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
