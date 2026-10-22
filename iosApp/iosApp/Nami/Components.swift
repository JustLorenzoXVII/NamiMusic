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

// MARK: - Intestazione delle schede

/// Titolo grande della scheda (largo e pesante, con una riga colorata sopra) e il tuo avatar.
/// Scorre via insieme al contenuto: in alto non resta nessuna barra.
struct PageHeader<Trailing: View>: View {
    let overline: String
    let title: String
    var horizontalPadding: CGFloat = 20
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(alignment: .bottom, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(overline.uppercased())
                    .font(.caption.weight(.heavy))
                    .tracking(1.6)
                    .foregroundStyle(.tint)
                Text(title)
                    .font(.system(size: 40, weight: .black).width(.expanded))
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
            }
            Spacer(minLength: 0)
            trailing()
                .padding(.bottom, 6)
        }
        .padding(.horizontal, horizontalPadding)
        .padding(.top, 6)
    }
}

/// Sotto l'ora e la batteria: una sfumatura di vetro, così il contenuto che scorre sotto
/// non si sovrappone ai simboli in alto.
struct StatusBarScrim: View {
    var body: some View {
        GeometryReader { geo in
            Rectangle()
                .fill(.ultraThinMaterial)
                .mask(LinearGradient(colors: [.black, .black.opacity(0.7), .clear], startPoint: .top, endPoint: .bottom))
                .frame(height: geo.safeAreaInsets.top + 18)
                .ignoresSafeArea(edges: .top)
        }
        .allowsHitTesting(false)
    }
}

extension View {
    /// Scheda senza barra in alto: titolo dentro al contenuto e sfumatura sotto la barra di stato.
    func barlessPage(title: String) -> some View {
        self
            .navigationTitle(title)
            .toolbar(.hidden, for: .navigationBar)
            .overlay(alignment: .top) { StatusBarScrim() }
    }
}

/// Avatar che apre account e impostazioni (dentro l'intestazione delle schede).
struct AvatarButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            AccountAvatar(size: 40)
                .shadow(color: .black.opacity(0.2), radius: 6, y: 3)
        }
        .buttonStyle(.squishy)
        .accessibilityLabel(L("Account e impostazioni"))
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
                Text(L(title))
                    .font(.title3.weight(.heavy).width(.expanded))
                    .lineLimit(1)
                if showsChevron {
                    Image(systemName: "chevron.forward")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.tint)
                }
            }
            if let subtitle {
                Text(L(subtitle))
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
        .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
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
        case .song: return L("Brano")
        case .album: return L("Album")
        case .playlist: return L("Playlist")
        case .artist: return L("Artista")
        case .other: return L("Video")
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
            Label(L(title), systemImage: "wifi.exclamationmark")
        } description: {
            Text(message)
        } actions: {
            Button(L("Riprova"), action: retry)
                .buttonStyle(.borderedProminent)
        }
    }
}

// MARK: - Avvisi ("Aggiunto a…")

struct Toast: Identifiable, Equatable {
    let id = UUID()
    let title: String
    let subtitle: String?
    let systemImage: String
    let artwork: URL?
}

/// Avviso che scende dall'alto per un paio di secondi: conferma che un'azione è avvenuta
/// (aggiunto a una playlist, in coda, ai preferiti, download iniziato…).
final class ToastCenter: ObservableObject {
    static let shared = ToastCenter()
    @Published private(set) var toast: Toast?
    private var hideWork: DispatchWorkItem?

    func show(_ title: String, systemImage: String = "checkmark.circle.fill", subtitle: String? = nil, artwork: URL? = nil) {
        let new = Toast(title: title, subtitle: subtitle, systemImage: systemImage, artwork: artwork)
        withAnimation(.spring(response: 0.45, dampingFraction: 0.78)) { toast = new }
        hideWork?.cancel()
        let work = DispatchWorkItem { [weak self] in
            withAnimation(.spring(response: 0.4, dampingFraction: 0.9)) { self?.toast = nil }
        }
        hideWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.4, execute: work)
    }

    func dismiss() {
        hideWork?.cancel()
        withAnimation(.spring(response: 0.4, dampingFraction: 0.9)) { toast = nil }
    }
}

private struct ToastView: View {
    let toast: Toast

    var body: some View {
        HStack(spacing: 12) {
            if let artwork = toast.artwork {
                ArtworkView(url: artwork, size: 40, cornerRadius: 8)
                    .overlay(alignment: .bottomTrailing) {
                        Image(systemName: toast.systemImage)
                            .font(.system(size: 16, weight: .bold))
                            .symbolRenderingMode(.palette)
                            .foregroundStyle(.white, .tint)
                            .background(Circle().fill(.background).padding(2))
                            .offset(x: 6, y: 6)
                            .symbolEffect(.bounce, value: toast.id)
                    }
            } else {
                Image(systemName: toast.systemImage)
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(.tint)
                    .symbolEffect(.bounce, value: toast.id)
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(toast.title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                if let subtitle = toast.subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, 12)
        .padding(.trailing, 18)
        .padding(.vertical, 10)
        .frame(maxWidth: 360)
        .toastGlass()
        .shadow(color: .black.opacity(0.18), radius: 16, y: 8)
        .padding(.horizontal, 16)
    }
}

private extension View {
    @ViewBuilder func toastGlass() -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular, in: Capsule())
        } else {
            background(.regularMaterial, in: Capsule())
        }
    }
}

private struct ToastHost: ViewModifier {
    @ObservedObject private var center = ToastCenter.shared

    func body(content: Content) -> some View {
        content.overlay(alignment: .top) {
            if let toast = center.toast {
                ToastView(toast: toast)
                    .id(toast.id)
                    .padding(.top, 6)
                    .transition(.asymmetric(
                        insertion: .move(edge: .top).combined(with: .scale(scale: 0.85, anchor: .top)).combined(with: .opacity),
                        removal: .move(edge: .top).combined(with: .opacity)
                    ))
                    .onTapGesture { center.dismiss() }
                    .gesture(DragGesture(minimumDistance: 10).onEnded { g in if g.translation.height < 0 { center.dismiss() } })
                    .sensoryFeedback(.success, trigger: toast.id)
            }
        }
    }
}

extension View {
    func toastHost() -> some View { modifier(ToastHost()) }
}
