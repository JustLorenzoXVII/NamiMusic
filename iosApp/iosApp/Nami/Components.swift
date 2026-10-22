import SwiftUI

// Pezzi comuni a Home, ricerca, artisti e mood: titoli di sezione, copertine con titolo,
// caroselli, "Scelte rapide", righe di elenco, transizioni a zoom e pressione dei pulsanti.

// MARK: - Router (fogli aperti da qualsiasi schermata)

enum NamiRoute: Hashable {
    case artist(key: String, title: String)
    case album(id: String, title: String)
}

final class NamiRouter: ObservableObject {
    static let shared = NamiRouter()
    /// Brano da aggiungere a una playlist: apre il foglio "Aggiungi a una playlist".
    @Published var addingToPlaylist: Track?
    /// Pagina da aprire nella scheda visibile (da un menu, anche dal player).
    @Published var pending: NamiRoute?
    var activeTab: NamiTab = .home

    func open(_ route: NamiRoute) { pending = route }
}

extension HomeItem {
    /// Elemento costruito a mano (per esempio "Vai all'album"): la chiave "1:<id>" apre l'album.
    init(key: String, kind: Kind, title: String, subtitle: String = "", artworkURL: URL? = nil) {
        self.id = key
        self.kind = kind
        self.title = title
        self.subtitle = subtitle
        self.artworkURL = artworkURL
        self.explicit = false
    }
}

@ViewBuilder
func routeDestination(for route: NamiRoute) -> some View {
    switch route {
    case let .artist(key, title):
        ArtistPageView(key: key, title: title, artworkURL: nil)
    case let .album(id, title):
        CollectionPage(item: HomeItem(key: "1:" + id, kind: .album, title: title))
    }
}

// MARK: - Transizione a zoom (iOS 18): la copertina si allarga nella pagina che apre

private struct ZoomNamespaceKey: EnvironmentKey {
    static let defaultValue: Namespace.ID? = nil
}

extension EnvironmentValues {
    var zoomNamespace: Namespace.ID? {
        get { self[ZoomNamespaceKey.self] }
        set { self[ZoomNamespaceKey.self] = newValue }
    }
}

private struct ZoomSource: ViewModifier {
    let id: String
    @Environment(\.zoomNamespace) private var namespace

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), let namespace {
            content.matchedTransitionSource(id: id, in: namespace) { source in
                source.clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        } else {
            content
        }
    }
}

private struct ZoomDestination: ViewModifier {
    let id: String
    @Environment(\.zoomNamespace) private var namespace

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), let namespace {
            content.navigationTransition(.zoom(sourceID: id, in: namespace))
        } else {
            content
        }
    }
}

extension View {
    func zoomSource(_ id: String) -> some View { modifier(ZoomSource(id: id)) }
    func zoomDestination(_ id: String) -> some View { modifier(ZoomDestination(id: id)) }
}

/// Ogni scheda ha il suo NavigationStack con il suo spazio per lo zoom.
struct ZoomingStack<Content: View>: View {
    let tab: NamiTab
    @ViewBuilder let content: () -> Content
    @Namespace private var namespace
    @State private var path = NavigationPath()
    @ObservedObject private var router = NamiRouter.shared

    var body: some View {
        NavigationStack(path: $path) {
            content()
                .navigationDestination(for: NamiRoute.self) { route in
                    routeDestination(for: route)
                }
        }
        .environment(\.zoomNamespace, namespace)
        .onChange(of: router.pending) { _, route in
            guard let route, router.activeTab == tab else { return }
            path.append(route)
            router.pending = nil
        }
    }
}

// MARK: - Bordo morbido sotto la barra in alto (iOS 26)

extension View {
    @ViewBuilder func softScrollEdges() -> some View {
        if #available(iOS 26.0, *) {
            scrollEdgeEffectStyle(.soft, for: .top)
        } else {
            self
        }
    }
}

// MARK: - Pressione con molla e vibrazione leggera

struct SquishyButtonStyle: ButtonStyle {
    var scale: CGFloat = 0.95

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .brightness(configuration.isPressed ? -0.04 : 0)
            .animation(.spring(response: 0.28, dampingFraction: 0.62), value: configuration.isPressed)
            .sensoryFeedback(.impact(weight: .light, intensity: 0.6), trigger: configuration.isPressed) { _, pressed in pressed }
    }
}

extension ButtonStyle where Self == SquishyButtonStyle {
    static var squishy: SquishyButtonStyle { SquishyButtonStyle() }
}

/// Le card si rimpiccioliscono un po' e sfumano quando escono dal bordo del carosello.
extension View {
    func carouselEffect() -> some View {
        scrollTransition(.interactive, axis: .horizontal) { content, phase in
            content
                .scaleEffect(phase.isIdentity ? 1 : 0.94)
                .opacity(phase.isIdentity ? 1 : 0.7)
        }
    }
}

// MARK: - Titoli

struct SectionTitle: View {
    let title: String
    var subtitle: String? = nil
    var showsChevron = true

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.title2.weight(.bold))
                    .lineLimit(1)
                if showsChevron {
                    Image(systemName: "chevron.forward")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.tint)
                }
            }
            if let subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Copertina quadrata con titolo e sottotitolo sotto.
struct CardLabel: View {
    let artworkURL: URL?
    let title: String
    let subtitle: String
    var explicit: Bool = false
    var size: CGFloat = 160

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ArtworkView(url: artworkURL, size: size, cornerRadius: 12)
                .shadow(color: .black.opacity(0.14), radius: 8, y: 4)
            HStack(spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)
                if explicit { ExplicitBadge() }
            }
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .frame(width: size, alignment: .leading)
        .contentShape(Rectangle())
    }
}

struct ExplicitBadge: View {
    var body: some View {
        Image(systemName: "e.square.fill")
            .font(.caption2)
            .foregroundStyle(.secondary)
    }
}

// MARK: - Sezioni

struct SectionsList: View {
    let sections: [HomeSection]
    /// La prima sezione di copertine diventa un carosello grande (come su Android)
    var featureFirstCarousel = false

    private var featuredID: String? {
        guard featureFirstCarousel else { return nil }
        return sections.first { !$0.allSongs && $0.items.count >= 3 }?.id
    }

    var body: some View {
        let featured = featuredID
        ForEach(sections) { section in
            if section.id == featured {
                FeaturedCarousel(section: section)
            } else {
                SectionView(section: section)
            }
        }
    }
}

/// Carosello grande: copertine quasi a tutta larghezza con il titolo sopra, che si fermano una alla volta.
struct FeaturedCarousel: View {
    let section: HomeSection

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            NavigationLink {
                SectionGridPage(section: section)
            } label: {
                SectionTitle(title: section.title)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 12) {
                    ForEach(section.items) { item in
                        ItemButton(item: item) {
                            FeaturedCard(item: item)
                        }
                        .containerRelativeFrame(.horizontal) { width, _ in width * 0.8 }
                        .scrollTransition(.interactive, axis: .horizontal) { content, phase in
                            content
                                .scaleEffect(phase.isIdentity ? 1 : 0.9)
                                .opacity(phase.isIdentity ? 1 : 0.6)
                                .rotation3DEffect(.degrees(phase.value * -8), axis: (x: 0, y: 1, z: 0))
                        }
                    }
                }
                .scrollTargetLayout()
            }
            .contentMargins(.horizontal, 20, for: .scrollContent)
            .scrollTargetBehavior(.viewAligned)
        }
    }
}

private struct FeaturedCard: View {
    let item: HomeItem

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Group {
                if item.kind == .artist {
                    RemoteImage(url: item.artworkURL)
                } else {
                    RemoteImage(url: item.artworkURL)
                }
            }
            .aspectRatio(1, contentMode: .fit)
            LinearGradient(colors: [.clear, .clear, .black.opacity(0.75)], startPoint: .top, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.title3.weight(.heavy))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                if !item.subtitle.isEmpty {
                    Text(item.subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.8))
                        .lineLimit(1)
                }
            }
            .padding(16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 12, y: 6)
        .contentShape(Rectangle())
    }
}

struct SectionView: View {
    let section: HomeSection

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !section.title.isEmpty {
                NavigationLink {
                    SectionGridPage(section: section)
                } label: {
                    SectionTitle(title: section.title)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            if section.allSongs {
                QuickPicks(items: section.items)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: 14) {
                        ForEach(section.items) { item in
                            ItemCard(item: item)
                                .carouselEffect()
                        }
                    }
                    .padding(.horizontal, 20)
                    .scrollTargetLayout()
                }
                .scrollTargetBehavior(.viewAligned)
            }
        }
    }
}

/// Brani in colonne da 4 che scorrono di lato (come "Scelte rapide" di Apple Music).
struct QuickPicks: View {
    let items: [HomeItem]
    @ObservedObject private var player = PlayerStore.shared

    private let rows = Array(repeating: GridItem(.fixed(56), spacing: 6), count: 4)

    var body: some View {
        let rowCount = min(4, max(1, items.count))
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHGrid(rows: Array(rows.prefix(rowCount)), spacing: 16) {
                ForEach(items) { item in
                    ItemButton(item: item) {
                        ItemRowLabel(item: item, playing: isPlaying(item))
                            .frame(width: 300, alignment: .leading)
                    }
                }
            }
            .padding(.horizontal, 20)
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(.viewAligned)
        .frame(height: CGFloat(rowCount) * 56 + CGFloat(rowCount - 1) * 6)
    }

    private func isPlaying(_ item: HomeItem) -> Bool {
        guard let id = player.state.track?.id else { return false }
        return item.id.hasSuffix(":" + id)
    }
}

/// Riga: copertina piccola (tonda per gli artisti), titolo e sottotitolo.
struct ItemRowLabel: View {
    let item: HomeItem
    var playing = false
    var showsType = false

    private var typeLabel: String {
        switch item.kind {
        case .song: return "Brano"
        case .album: return "Album"
        case .playlist: return "Playlist"
        case .artist: return "Artista"
        case .other: return "Video"
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            if item.kind == .artist {
                RemoteImage(url: item.artworkURL)
                    .frame(width: 50, height: 50)
                    .clipShape(Circle())
            } else {
                ArtworkView(url: item.artworkURL, size: 50, cornerRadius: 6)
            }
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(item.title)
                        .font(.body)
                        .foregroundStyle(playing ? AnyShapeStyle(.tint) : AnyShapeStyle(.primary))
                        .lineLimit(1)
                    if item.explicit { ExplicitBadge() }
                }
                let subtitle = showsType ? [typeLabel, item.subtitle].filter { !$0.isEmpty }.joined(separator: " · ") : item.subtitle
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
            if playing {
                Image(systemName: "waveform")
                    .foregroundStyle(.tint)
                    .symbolEffect(.variableColor.iterative, options: .repeating)
            } else if item.kind != .song && item.kind != .other {
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .contentShape(Rectangle())
    }
}

/// Card di un elemento nei caroselli.
struct ItemCard: View {
    let item: HomeItem

    var body: some View {
        ItemButton(item: item) {
            if item.kind == .artist {
                VStack(spacing: 8) {
                    RemoteImage(url: item.artworkURL)
                        .frame(width: 140, height: 140)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.14), radius: 8, y: 4)
                    Text(item.title)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                        .frame(width: 140)
                }
                .contentShape(Rectangle())
            } else {
                CardLabel(artworkURL: item.artworkURL, title: item.title, subtitle: item.subtitle, explicit: item.explicit)
            }
        }
    }
}

/// Tocco su un elemento: brani e video partono, album e playlist si aprono (con lo zoom),
/// gli artisti aprono la loro pagina. Tenendo premuto su un brano c'è il suo menu.
struct ItemButton<Content: View>: View {
    let item: HomeItem
    @ViewBuilder let label: () -> Content

    var body: some View {
        switch item.kind {
        case .album, .playlist:
            NavigationLink {
                CollectionPage(item: item)
                    .zoomDestination(item.id)
            } label: {
                label().zoomSource(item.id)
            }
            .buttonStyle(.squishy)
        case .artist:
            NavigationLink {
                ArtistPageView(key: item.id, title: item.title, artworkURL: item.artworkURL)
            } label: {
                label()
            }
            .buttonStyle(.squishy)
        case .song, .other:
            Button {
                HomeStore.shared.play(item)
            } label: {
                label()
            }
            .buttonStyle(.squishy)
            .contextMenu {
                if item.kind == .song, let track = ItemActions.track(for: item) {
                    SongMenuItems(song: track, onAddToPlaylist: { NamiRouter.shared.addingToPlaylist = track })
                }
            }
        }
    }
}

/// "Vedi tutto" di una sezione: griglia di copertine, o elenco se sono solo brani.
struct SectionGridPage: View {
    let section: HomeSection

    private let columns = [GridItem(.flexible(), spacing: 14, alignment: .top), GridItem(.flexible(), spacing: 14, alignment: .top)]

    var body: some View {
        ScrollView {
            if section.allSongs {
                LazyVStack(spacing: 0) {
                    ForEach(section.items) { item in
                        ItemButton(item: item) {
                            ItemRowLabel(item: item)
                                .padding(.vertical, 6)
                        }
                        Divider().padding(.leading, 62)
                    }
                }
                .padding(.horizontal, 20)
            } else {
                LazyVGrid(columns: columns, spacing: 20) {
                    ForEach(section.items) { item in
                        GridCard(item: item)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
        .contentMargins(.vertical, 12, for: .scrollContent)
        .navigationTitle(section.title)
        .navigationBarTitleDisplayMode(.large)
    }
}

/// Card che si allarga alla colonna della griglia.
private struct GridCard: View {
    let item: HomeItem

    var body: some View {
        ItemButton(item: item) {
            VStack(alignment: item.kind == .artist ? .center : .leading, spacing: 6) {
                Group {
                    if item.kind == .artist {
                        RemoteImage(url: item.artworkURL)
                            .aspectRatio(1, contentMode: .fit)
                            .clipShape(Circle())
                    } else {
                        ArtworkView(url: item.artworkURL, cornerRadius: 12)
                    }
                }
                .shadow(color: .black.opacity(0.14), radius: 8, y: 4)
                HStack(spacing: 4) {
                    Text(item.title)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                    if item.explicit { ExplicitBadge() }
                }
                if !item.subtitle.isEmpty && item.kind != .artist {
                    Text(item.subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: item.kind == .artist ? .center : .leading)
            .contentShape(Rectangle())
        }
    }
}

// MARK: - Stati di caricamento

struct LoadingView: View {
    var body: some View {
        ProgressView()
            .controlSize(.large)
            .frame(maxWidth: .infinity, minHeight: 200)
    }
}

struct ErrorView: View {
    let title: String
    let message: String
    let retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "wifi.exclamationmark")
        } description: {
            Text(message)
        } actions: {
            Button("Riprova", action: retry)
                .buttonStyle(.borderedProminent)
        }
    }
}
