import SwiftUI

/// Il passaggio tra le due è un'onda di vetro colorato: l'interfaccia vecchia si rimpicciolisce
/// e sfuma sotto il vetro, quella nuova emerge da dietro ingrandendosi appena.
struct InterfaceSwitcher<Ios: View, Compose: View>: View {
    @ViewBuilder let ios: () -> Ios
    @ViewBuilder let compose: () -> Compose
    @StateObject private var app = NamiApp.shared
    @State private var showingIos = NamiApp.shared.iosUI
    @State private var wave: CGFloat = 0
    @State private var waveVisible = false

    var body: some View {
        ZStack {
            Group {
                if showingIos {
                    ios()
                        .id(app.language)
                        .transition(.asymmetric(
                            insertion: .scale(scale: 1.06).combined(with: .opacity),
                            removal: .scale(scale: 0.94).combined(with: .opacity)
                        ))
                } else {
                    compose()
                        .transition(.asymmetric(
                            insertion: .scale(scale: 1.06).combined(with: .opacity),
                            removal: .scale(scale: 0.94).combined(with: .opacity)
                        ))
                }
            }
            if waveVisible {
                SwitchWave(progress: wave, towardsIos: app.iosUI)
                    .ignoresSafeArea()
                    .allowsHitTesting(false)
            }
        }
        .background(Color.black)
        .onChange(of: app.iosUI) { _, newValue in
            switchInterface(to: newValue)
        }
    }

    private func switchInterface(to ios: Bool) {
        waveVisible = true
        wave = 0
        // 1) il vetro sale e copre l'interfaccia di prima
        withAnimation(.easeIn(duration: 0.35)) { wave = 1 }
        // 2) sotto il vetro si cambia interfaccia
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.32) {
            withAnimation(.spring(response: 0.55, dampingFraction: 0.85)) { showingIos = ios }
            // 3) il vetro si dissolve e scopre quella nuova
            withAnimation(.easeOut(duration: 0.55).delay(0.12)) { wave = 2 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.75) { waveVisible = false }
        }
    }
}

/// L'onda: un cerchio di vetro colorato che si allarga dal basso fino a coprire lo schermo
/// (0 → 1) e poi diventa trasparente (1 → 2).
private struct SwitchWave: View, Animatable {
    var progress: CGFloat
    let towardsIos: Bool

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    var body: some View {
        GeometryReader { geo in
            let diagonal = sqrt(geo.size.width * geo.size.width + geo.size.height * geo.size.height)
            let grow = min(progress, 1)
            let fade = max(0, progress - 1)
            ZStack {
                Rectangle().fill(.ultraThinMaterial)
                LinearGradient(
                    colors: towardsIos
                        ? [Color(red: 0.55, green: 0.56, blue: 1.0).opacity(0.55), Color(red: 0.95, green: 0.5, blue: 0.75).opacity(0.45)]
                        : [Color(red: 0.4, green: 0.8, blue: 0.7).opacity(0.5), Color(red: 0.3, green: 0.4, blue: 0.95).opacity(0.5)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Image(systemName: towardsIos ? "sparkles" : "square.grid.2x2.fill")
                    .font(.system(size: 54, weight: .semibold))
                    .foregroundStyle(.white)
                    .scaleEffect(0.6 + 0.4 * grow)
                    .opacity(Double(grow) * (1 - Double(fade) * 1.6))
            }
            .mask {
                Circle()
                    .frame(width: diagonal * 2 * grow, height: diagonal * 2 * grow)
                    .position(x: geo.size.width / 2, y: geo.size.height)
            }
            .opacity(Double(1 - fade))
        }
    }
}
