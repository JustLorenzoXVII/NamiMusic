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

struct GlassChromeView: SwiftUI.View {
    @ObservedObject var model: GlassChromeModel
    @SwiftUI.Namespace private var tabNamespace

    var body: some SwiftUI.View {
        SwiftUI.Group {
            if let state = model.state {
                chrome(state)
                    .allowsHitTesting(state.visible)
                    .environment(\.colorScheme, state.dark ? .dark : .light)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .ignoresSafeArea()
    }

    private func chrome(_ state: GlassChromeState) -> some SwiftUI.View {
        let accent = SwiftUI.Color(argb: state.accent)
        return GlassGroup {
            SwiftUI.VStack(spacing: 10) {
                if state.hasMini {
                    MiniAccessory(state: state)
                        .frame(height: 50)
                        .contentShape(SwiftUI.Capsule())
                        .onTapGesture { GlassChromeBridge.shared.openPlayer() }
                        .glassCapsule()
                        .background(MiniFrameReporter())
                        // Mentre il player è aperto il mini player non c'è: sparisce e ricompare di colpo,
                        // perché il player si apre e si richiude esattamente nel suo riquadro
                        .opacity(state.visible ? 1 : 0)
                        .animation(nil, value: state.visible)
                        // Quando parte il primo brano sale da sotto
                        .transition(.asymmetric(insertion: .opacity.combined(with: .offset(y: 28)), removal: .opacity))
                }
                SwiftUI.HStack(spacing: 12) {
                    TabCapsule(state: state, accent: accent, namespace: tabNamespace)
                    SearchCircle(state: state, accent: accent)
                }
                .frame(height: 62)
                // Le schede scivolano giù sotto il player che si apre, e risalgono quando si chiude
                .offset(y: state.visible ? 0 : 110)
                .opacity(state.visible ? 1 : 0)
                .animation(.spring(response: 0.42, dampingFraction: 0.88), value: state.visible)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, CGFloat(state.bottomPadding))
            .animation(.spring(response: 0.45, dampingFraction: 0.85), value: state.hasMini)
        }
    }
}

// MARK: - Schede

private struct GlassTabItem: Identifiable {
    let id: Int32
    let symbol: String
    let label: String
}

private struct TabCapsule: SwiftUI.View {
    let state: GlassChromeState
    let accent: SwiftUI.Color
    let namespace: SwiftUI.Namespace.ID

    // Stesso ordine delle schede dell'app: 0 Home, 1 Cerca, 2 Statistiche, 3 Libreria
    private var items: [GlassTabItem] {
        [
            GlassTabItem(id: 0, symbol: "house.fill", label: state.homeLabel),
            GlassTabItem(id: 2, symbol: "chart.bar.fill", label: state.statsLabel),
            GlassTabItem(id: 3, symbol: "square.stack.fill", label: state.libraryLabel),
        ]
    }

    var body: some SwiftUI.View {
        SwiftUI.HStack(spacing: 0) {
            SwiftUI.ForEach(items) { item in
                let selected = state.selectedTab == item.id
                SwiftUI.Button {
                    GlassChromeBridge.shared.selectTab(index: item.id)
                } label: {
                    SwiftUI.VStack(spacing: 2) {
                        SwiftUI.Image(systemName: item.symbol)
                            .font(.system(size: 19, weight: .semibold))
                            .frame(height: 25)
                        SwiftUI.Text(item.label)
                            .font(.system(size: 10, weight: .semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    .foregroundStyle(selected ? accent : SwiftUI.Color.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background {
                        if selected {
                            SwiftUI.Capsule()
                                .fill(SwiftUI.Color.primary.opacity(0.1))
                                .matchedGeometryEffect(id: "selezione", in: namespace)
                        }
                    }
                    .contentShape(SwiftUI.Capsule())
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

private struct SearchCircle: SwiftUI.View {
    let state: GlassChromeState
    let accent: SwiftUI.Color

    var body: some SwiftUI.View {
        let selected = state.selectedTab == 1
        SwiftUI.Button {
            GlassChromeBridge.shared.selectTab(index: 1)
        } label: {
            SwiftUI.Image(systemName: "magnifyingglass")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(selected ? accent : SwiftUI.Color.primary)
                .frame(width: 62, height: 62)
                .contentShape(SwiftUI.Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(state.searchLabel)
        .glassCircle()
    }
}

// MARK: - Mini player

private struct MiniAccessory: SwiftUI.View {
    let state: GlassChromeState

    var body: some SwiftUI.View {
        SwiftUI.HStack(spacing: 10) {
            GlassArtwork(url: state.artworkUrl)
                .frame(width: 38, height: 38)
                .clipShape(SwiftUI.RoundedRectangle(cornerRadius: 8, style: .continuous))
            SwiftUI.VStack(alignment: .leading, spacing: 1) {
                SwiftUI.Text(state.title)
                    .font(.system(size: 14, weight: .semibold))
                    .lineLimit(1)
                if !state.artist.isEmpty {
                    SwiftUI.Text(state.artist)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            SwiftUI.Button {
                GlassChromeBridge.shared.playPause()
            } label: {
                SwiftUI.Image(systemName: state.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 38, height: 44)
                    .contentShape(SwiftUI.Rectangle())
            }
            .buttonStyle(.plain)
            SwiftUI.Button {
                GlassChromeBridge.shared.next()
            } label: {
                SwiftUI.Image(systemName: "forward.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .frame(width: 38, height: 44)
                    .contentShape(SwiftUI.Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.leading, 6)
        .padding(.trailing, 10)
        .foregroundStyle(SwiftUI.Color.primary)
    }
}

/// Copertina del mini player. Al cambio brano resta quella vecchia finché la nuova non è pronta
/// (niente lampo grigio), e le copertine già viste si riprendono dalla memoria.
private struct GlassArtwork: SwiftUI.View {
    let url: String?
    @StateObject private var loader = ArtworkLoader()

    var body: some SwiftUI.View {
        SwiftUI.ZStack {
            SwiftUI.Color.gray.opacity(0.3)
            if let image = loader.image {
                SwiftUI.Image(uiImage: image).resizable().scaledToFill()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: loader.image)
        .onAppear { loader.load(url) }
        .onChange(of: url) { _, newUrl in loader.load(newUrl) }
    }
}

private final class ArtworkLoader: ObservableObject {
    @Published var image: UIImage?
    private static let cache = NSCache<NSString, UIImage>()
    private var current: String?
    private var task: URLSessionDataTask?

    func load(_ url: String?) {
        guard let url, url != current else { return }
        current = url
        if let cached = Self.cache.object(forKey: url as NSString) {
            image = cached
            return
        }
        task?.cancel()
        guard let address = URL(string: url) else { return }
        task = URLSession.shared.dataTask(with: address) { [weak self] data, _, _ in
            guard let data, let loaded = UIImage(data: data) else { return }
            Self.cache.setObject(loaded, forKey: url as NSString)
            DispatchQueue.main.async {
                guard let self, self.current == url else { return }
                self.image = loaded
            }
        }
        task?.resume()
    }
}

/// Dice all'app dove si trova il mini player: il player si apre da lì e lì si richiude.
private struct MiniFrameReporter: SwiftUI.View {
    var body: some SwiftUI.View {
        SwiftUI.GeometryReader { geometry in
            let frame = geometry.frame(in: .global)
            SwiftUI.Color.clear
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

/// Su iOS 26 le forme di vetro nello stesso contenitore si deformano insieme. La distanza è bassa:
/// mini player, schede e ricerca restano separati e non si fondono a riposo.
private struct GlassGroup<Content: SwiftUI.View>: SwiftUI.View {
    @ViewBuilder var content: Content

    var body: some SwiftUI.View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: 4) { content }
        } else {
            content
        }
    }
}

private extension SwiftUI.View {
    @ViewBuilder
    func glassCapsule() -> some SwiftUI.View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular.interactive(), in: SwiftUI.Capsule())
        } else {
            self
                .background(.ultraThinMaterial, in: SwiftUI.Capsule())
                .overlay(SwiftUI.Capsule().strokeBorder(SwiftUI.Color.white.opacity(0.18), lineWidth: 0.5))
                .shadow(color: .black.opacity(0.18), radius: 12, y: 6)
        }
    }

    @ViewBuilder
    func glassCircle() -> some SwiftUI.View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular.interactive(), in: SwiftUI.Circle())
        } else {
            self
                .background(.ultraThinMaterial, in: SwiftUI.Circle())
                .overlay(SwiftUI.Circle().strokeBorder(SwiftUI.Color.white.opacity(0.18), lineWidth: 0.5))
                .shadow(color: .black.opacity(0.18), radius: 12, y: 6)
        }
    }
}

private extension SwiftUI.Color {
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
