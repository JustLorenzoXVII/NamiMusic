import SwiftUI
import UIKit
import Shared

// Barra in basso del tema iOS (Liquid Glass): mini player, schede e ricerca.
// L'app (Kotlin) le passa lo stato con GlassChromeBridge e riceve i tocchi dallo stesso ponte.
// Su iOS 26 il vetro è quello vero di sistema; prima di iOS 26 si usa il materiale sfocato.

/// Lo stato della barra, aggiornato dall'app.
final class GlassChromeModel: ObservableObject {
    @Published var state: GlassChromeState?

    init() {
        state = GlassChromeBridge.shared.current()
        GlassChromeBridge.shared.listener = { [weak self] newState in
            if Thread.isMainThread {
                self?.state = newState
            } else {
                DispatchQueue.main.async { self?.state = newState }
            }
        }
    }
}

enum GlassChrome {
    static let model = GlassChromeModel()

    /// Da chiamare all'avvio, prima che parta l'interfaccia dell'app.
    static func install() {
        GlassChromeBridge.shared.factory = {
            let controller = UIHostingController(rootView: GlassChromeView(model: model))
            controller.view.backgroundColor = .clear
            controller.safeAreaRegions = []
            return controller
        }
    }
}

struct GlassChromeView: View {
    @ObservedObject var model: GlassChromeModel
    @Namespace private var tabNamespace

    var body: some View {
        Group {
            if let state = model.state {
                chrome(state)
                    .opacity(state.visible ? 1 : 0)
                    .allowsHitTesting(state.visible)
                    .animation(.easeInOut(duration: 0.22), value: state.visible)
                    .environment(\.colorScheme, state.dark ? .dark : .light)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .ignoresSafeArea()
    }

    private func chrome(_ state: GlassChromeState) -> some View {
        let accent = Color(argb: state.accent)
        return GlassGroup {
            VStack(spacing: 10) {
                if state.hasMini {
                    MiniAccessory(state: state)
                        .frame(height: 50)
                        .contentShape(Capsule())
                        .onTapGesture { GlassChromeBridge.shared.openPlayer() }
                        .glassCapsule()
                        .background(MiniFrameReporter())
                        .transition(.opacity.combined(with: .scale(scale: 0.9, anchor: .bottom)))
                }
                HStack(spacing: 12) {
                    TabCapsule(state: state, accent: accent, namespace: tabNamespace)
                    SearchCircle(state: state, accent: accent)
                }
                .frame(height: 62)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, CGFloat(state.bottomPadding))
            .animation(.smooth(duration: 0.3), value: state.hasMini)
        }
    }
}

// MARK: - Schede

private struct TabItem: Identifiable {
    let id: Int32
    let symbol: String
    let label: String
}

private struct TabCapsule: View {
    let state: GlassChromeState
    let accent: Color
    let namespace: Namespace.ID

    // Stesso ordine delle schede dell'app: 0 Home, 1 Cerca, 2 Statistiche, 3 Libreria
    private var items: [TabItem] {
        [
            TabItem(id: 0, symbol: "house.fill", label: state.homeLabel),
            TabItem(id: 2, symbol: "chart.bar.fill", label: state.statsLabel),
            TabItem(id: 3, symbol: "square.stack.fill", label: state.libraryLabel),
        ]
    }

    var body: some View {
        HStack(spacing: 0) {
            ForEach(items) { item in
                let selected = state.selectedTab == item.id
                Button {
                    GlassChromeBridge.shared.selectTab(index: item.id)
                } label: {
                    VStack(spacing: 2) {
                        Image(systemName: item.symbol)
                            .font(.system(size: 19, weight: .semibold))
                            .frame(height: 25)
                        Text(item.label)
                            .font(.system(size: 10, weight: .semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    .foregroundStyle(selected ? accent : Color.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background {
                        if selected {
                            Capsule()
                                .fill(Color.primary.opacity(0.1))
                                .matchedGeometryEffect(id: "selezione", in: namespace)
                        }
                    }
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.label)
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
        .padding(4)
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: state.selectedTab)
        .glassCapsule()
    }
}

private struct SearchCircle: View {
    let state: GlassChromeState
    let accent: Color

    var body: some View {
        let selected = state.selectedTab == 1
        Button {
            GlassChromeBridge.shared.selectTab(index: 1)
        } label: {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(selected ? accent : Color.primary)
                .frame(width: 62, height: 62)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(state.searchLabel)
        .glassCircle()
    }
}

// MARK: - Mini player

private struct MiniAccessory: View {
    let state: GlassChromeState

    var body: some View {
        HStack(spacing: 10) {
            Artwork(url: state.artworkUrl)
                .frame(width: 38, height: 38)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            VStack(alignment: .leading, spacing: 1) {
                Text(state.title)
                    .font(.system(size: 14, weight: .semibold))
                    .lineLimit(1)
                if !state.artist.isEmpty {
                    Text(state.artist)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                GlassChromeBridge.shared.playPause()
            } label: {
                Image(systemName: state.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 38, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Button {
                GlassChromeBridge.shared.next()
            } label: {
                Image(systemName: "forward.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .frame(width: 38, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.leading, 6)
        .padding(.trailing, 10)
        .foregroundStyle(Color.primary)
    }
}

private struct Artwork: View {
    let url: String?

    var body: some View {
        AsyncImage(url: url.flatMap { URL(string: $0) }) { phase in
            if let image = phase.image {
                image.resizable().scaledToFill()
            } else {
                Color.gray.opacity(0.3)
            }
        }
    }
}

/// Dice all'app dove si trova il mini player: il player si apre da lì e lì si richiude.
private struct MiniFrameReporter: View {
    var body: some View {
        GeometryReader { geometry in
            let frame = geometry.frame(in: .global)
            Color.clear
                .onAppear { report(frame) }
                .onChange(of: frame) { _, newFrame in report(newFrame) }
        }
    }

    private func report(_ frame: CGRect) {
        GlassChromeBridge.shared.reportMiniFrame(
            x: Double(frame.minX),
            y: Double(frame.minY),
            width: Double(frame.width),
            height: Double(frame.height)
        )
    }
}

// MARK: - Vetro

/// Su iOS 26 le forme di vetro vicine si fondono tra loro, come nelle app di Apple.
private struct GlassGroup<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: 12) { content }
        } else {
            content
        }
    }
}

private extension View {
    @ViewBuilder
    func glassCapsule() -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular.interactive(), in: Capsule())
        } else {
            self
                .background(.ultraThinMaterial, in: Capsule())
                .overlay(Capsule().strokeBorder(Color.white.opacity(0.18), lineWidth: 0.5))
                .shadow(color: .black.opacity(0.18), radius: 12, y: 6)
        }
    }

    @ViewBuilder
    func glassCircle() -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular.interactive(), in: Circle())
        } else {
            self
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().strokeBorder(Color.white.opacity(0.18), lineWidth: 0.5))
                .shadow(color: .black.opacity(0.18), radius: 12, y: 6)
        }
    }
}

private extension Color {
    /// Colore da un intero ARGB (come lo passa l'app).
    init(argb: Int64) {
        let value = UInt32(truncatingIfNeeded: argb)
        self.init(
            .sRGB,
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255,
            opacity: Double((value >> 24) & 0xFF) / 255
        )
    }
}
