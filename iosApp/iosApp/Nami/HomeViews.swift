import SwiftUI

// Home: saluto in base all'ora, bagliore colorato dalla copertina in riproduzione, card
// "In riproduzione", mood, mix "Fatte per te", ascoltati di recente e le sezioni di YouTube Music.

struct HomeView: View {
    @ObservedObject private var home = HomeStore.shared
    @ObservedObject private var player = PlayerStore.shared
    let onSettings: () -> Void

    /// Con un filtro attivo la Home mostra solo i risultati del filtro, dall'inizio.
    private var filtering: Bool { home.selectedChip != nil }

    /// "Per te adesso": il primo brano consigliato, come su Android.
    private var heroSong: HomeItem? {
        home.sections.lazy.flatMap(\.items).first { $0.kind == .song }
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 30) {
                    PageHeader(overline: Self.dateLine, title: Self.greeting) {
                        AvatarButton(action: onSettings)
                    }
                    .padding(.bottom, -10)
                    .id("top")

                    // I filtri subito sotto il titolo: toccandone uno la Home mostra solo quello
                    if !home.chips.isEmpty {
                        ChipsRow(chips: home.chips, selected: home.selectedChip, onSelect: { chip in
                            withAnimation(.snappy) { home.select(chip: chip) }
                        })
                        .padding(.bottom, -8)
                    }
                    if !filtering, player.state.track != nil {
                        NowPlayingCard()
                            .padding(.horizontal, 20)
                            .transition(.asymmetric(insertion: .scale(scale: 0.92).combined(with: .opacity), removal: .opacity))
                    }
                    if !filtering {
                        if let hero = heroSong {
                            ForYouHero(item: hero)
                                .padding(.horizontal, 20)
                                .transition(.opacity.combined(with: .scale(scale: 0.96)))
                        }
                        if !home.mixes.isEmpty {
                            MixesSection(mixes: home.mixes)
                                .transition(.opacity.combined(with: .move(edge: .leading)))
                        }
                        if !home.recent.isEmpty {
                            RecentSection(songs: home.recent)
                                .transition(.opacity.combined(with: .move(edge: .leading)))
                        }
                    }
                    if home.loading && (home.sections.isEmpty || filtering) {
                        LoadingView()
                            .frame(minHeight: 120)
                    } else if home.sections.isEmpty, let error = home.error {
                        ErrorView(title: "Home non disponibile", message: error) { home.load() }
                    }
                    SectionsList(sections: home.sections, featureFirstCarousel: true)
                        .opacity(home.loading && !home.sections.isEmpty ? 0.35 : 1)
                }
                .padding(.top, 4)
                .padding(.bottom, 32)
                .animation(.smooth(duration: 0.4), value: home.sections.map(\.id))
                .animation(.spring(response: 0.45, dampingFraction: 0.85), value: filtering)
                .animation(.spring(response: 0.45, dampingFraction: 0.8), value: player.state.track?.id)
            }
            .onChange(of: home.selectedChip) { _, _ in
                withAnimation(.spring(response: 0.5, dampingFraction: 0.9)) {
                    proxy.scrollTo("top", anchor: .top)
                }
            }
        }
        .background(alignment: .top) { AmbientGlow() }
        .barlessPage(title: L("Home"))
        .refreshable { await home.reload() }
        .onAppear { home.loadIfNeeded() }
    }

    static var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<13: return L("Buongiorno")
        case 13..<18: return L("Buon pomeriggio")
        default: return L("Buonasera")
        }
    }

    static var dateLine: String {
        let formatter = DateFormatter()
        let locale = Locale(identifier: L("Buongiorno") == "Buongiorno" ? "it_IT" : "en_US")
        formatter.locale = locale
        formatter.setLocalizedDateFormatFromTemplate("EEEEdMMMM")
        return formatter.string(from: Date())
    }
}

// MARK: - Bagliore dalla copertina

/// Una macchia di colore sfumata dietro l'intestazione, presa dalla copertina che sta suonando
/// (l'accento di Nami se non suona niente). Cambia colore con un'animazione lenta.
struct AmbientGlow: View {
    @ObservedObject private var player = PlayerStore.shared
    @ObservedObject private var accent = AccentStore.shared
    @State private var palette: ArtworkPalette?
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject private var presence = PlayerPresence.shared
    @State private var visible = false

    /// Si muove solo se si vede davvero: niente animazione sotto il player, in un'altra scheda o a schermo spento.
    private var paused: Bool { !visible || presence.open || scenePhase != .active }

    var body: some View {
        let first = palette?.top ?? accent.color
        let second = palette?.bottom ?? accent.color.opacity(0.5)
        let strength = scheme == .dark ? 0.85 : 0.5
        // Una "lava" di colore che si muove piano: punti della griglia che oscillano nel tempo
        TimelineView(.animation(minimumInterval: 1.0 / 15.0, paused: paused)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            let wobble = { (speed: Double, amount: Double) -> Float in Float(sin(t * speed) * amount) }
            MeshGradient(
                width: 3,
                height: 3,
                points: [
                    [0, 0], [0.5 + wobble(0.31, 0.12), 0], [1, 0],
                    [0, 0.45 + wobble(0.23, 0.1)], [0.5 + wobble(0.27, 0.15), 0.5 + wobble(0.19, 0.12)], [1, 0.5 + wobble(0.21, 0.1)],
                    [0, 1], [0.5 + wobble(0.17, 0.12), 1], [1, 1],
                ],
                colors: [
                    first.opacity(strength), accent.color.opacity(strength * 0.7), second.opacity(strength),
                    second.opacity(strength * 0.6), first.opacity(strength * 0.5), accent.color.opacity(strength * 0.4),
                    .clear, .clear, .clear,
                ],
                smoothsColors: true
            )
        }
        .frame(height: 520)
        .mask(LinearGradient(colors: [.black, .black.opacity(0.7), .clear], startPoint: .top, endPoint: .bottom))
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .animation(.easeInOut(duration: 1.2), value: palette)
        .onAppear { visible = true; update() }
        .onDisappear { visible = false }
        .onChange(of: player.state.track?.artworkURL) { _, _ in update() }
    }

    private func update() {
        guard let url = player.state.track?.artworkURL else { palette = nil; return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }
}

// MARK: - In riproduzione

/// Card grande col brano che sta suonando: colori della copertina, Liquid Glass sul pulsante.
private struct NowPlayingCard: View {
    @ObservedObject private var player = PlayerStore.shared
    @State private var palette = ArtworkPalette.fallback

    var body: some View {
        let track = player.state.track
        HStack(spacing: 14) {
            ArtworkView(url: track?.artworkURL, size: 72, cornerRadius: 10)
                .shadow(color: .black.opacity(0.35), radius: 10, y: 5)
                .scaleEffect(player.state.isPlaying ? 1 : 0.9)
                .animation(.spring(response: 0.45, dampingFraction: 0.65), value: player.state.isPlaying)
            VStack(alignment: .leading, spacing: 3) {
                Text(player.state.isPlaying ? L("IN RIPRODUZIONE") : L("IN PAUSA"))
                    .font(.caption2.weight(.bold))
                    .tracking(0.8)
                    .foregroundStyle(.white.opacity(0.7))
                    .contentTransition(.opacity)
                Text(track?.title ?? "")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text(track?.artist ?? "")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.75))
                    .lineLimit(1)
                WithClock {
                    ProgressView(value: player.duration > 0 ? min(player.position / player.duration, 1) : 0)
                }
                    .tint(.white)
                    .scaleEffect(x: 1, y: 0.7, anchor: .center)
                    .padding(.top, 4)
            }
            Spacer(minLength: 4)
            GlassPlayButton(isPlaying: player.state.isPlaying, action: player.togglePlayPause)
                .sensoryFeedback(.impact(weight: .medium), trigger: player.state.isPlaying)
        }
        .padding(14)
        .background {
            ZStack {
                LinearGradient(colors: [palette.top, palette.bottom], startPoint: .topLeading, endPoint: .bottomTrailing)
                RemoteImage(url: track?.artworkURL, placeholder: .clear)
                    .blur(radius: 40, opaque: true)
                    .opacity(0.35)
            }
            .animation(.easeInOut(duration: 0.8), value: palette)
        }
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: palette.bottom.opacity(0.45), radius: 18, y: 10)
        .onAppear(perform: update)
        .onChange(of: track?.artworkURL) { _, _ in update() }
    }

    private func update() {
        guard let url = player.state.track?.artworkURL else { return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }
}

private extension View {
    /// Capsula di vetro (iOS 26), materiale sfocato prima.
    @ViewBuilder func glassCapsule() -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular, in: Capsule())
        } else {
            background(.ultraThinMaterial, in: Capsule())
        }
    }

    /// Cerchio di vetro (iOS 26), materiale sfocato prima.
    @ViewBuilder func glassCircle() -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular, in: Circle())
        } else {
            background(.ultraThinMaterial, in: Circle())
        }
    }
}

/// Play/pausa tondo di vetro: su iOS 26 è un vero pulsante di vetro di sistema (il tocco
/// vale solo dentro il cerchio).
private struct GlassPlayButton: View {
    let isPlaying: Bool
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            Button(action: action) {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.title2)
                    .foregroundStyle(.white)
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.glass)
            .buttonBorderShape(.circle)
            .controlSize(.large)
        } else {
            Button(action: action) {
                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                    .font(.title2)
                    .foregroundStyle(.white)
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 52, height: 52)
                    .background(.ultraThinMaterial, in: Circle())
            }
            .buttonStyle(.squishy)
        }
    }
}

// MARK: - Per te adesso

/// Il primo brano consigliato, grande: copertina sfocata come sfondo, copertina nitida a destra,
/// colori presi dall'immagine e un pulsante di vetro per farlo partire (con la radio dopo).
private struct ForYouHero: View {
    let item: HomeItem
    @State private var palette = ArtworkPalette.fallback

    var body: some View {
        Button {
            HomeStore.shared.play(item)
        } label: {
            ZStack(alignment: .leading) {
                LinearGradient(colors: [palette.top, palette.bottom], startPoint: .topLeading, endPoint: .bottomTrailing)
                RemoteImage(url: item.artworkURL, placeholder: .clear)
                    .blur(radius: 50, opaque: true)
                    .opacity(0.4)
                HStack(spacing: 0) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L("PER TE ADESSO"))
                            .font(.caption.weight(.heavy))
                            .tracking(1.2)
                            .foregroundStyle(.white.opacity(0.75))
                        Text(item.title)
                            .font(.title2.weight(.heavy))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .minimumScaleFactor(0.8)
                        Text(item.subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.8))
                            .lineLimit(1)
                        Spacer(minLength: 10)
                        HStack(spacing: 6) {
                            Image(systemName: "play.fill")
                            Text(L("Ascolta"))
                        }
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .glassCapsule()
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    ArtworkView(url: item.artworkURL, size: 132, cornerRadius: 14)
                        .shadow(color: .black.opacity(0.4), radius: 14, y: 8)
                        .rotationEffect(.degrees(4))
                        .padding(.trailing, 18)
                }
            }
            .frame(height: 190)
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: palette.bottom.opacity(0.45), radius: 18, y: 10)
            .animation(.easeInOut(duration: 0.6), value: palette)
        }
        .buttonStyle(.squishy)
        .contextMenu {
            if let track = ItemActions.track(for: item) {
                SongMenuItems(song: track, onAddToPlaylist: { NamiRouter.shared.addingToPlaylist = track })
            }
        }
        .onAppear(perform: update)
        .onChange(of: item.artworkURL) { _, _ in update() }
    }

    private func update() {
        guard let url = item.artworkURL else { return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }
}

// MARK: - Mood

private struct ChipsRow: View {
    let chips: [HomeChip]
    let selected: String?
    let onSelect: (HomeChip) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(chips) { chip in
                    if chip.browseId != nil {
                        // "Podcast" e simili: si apre la loro pagina
                        NavigationLink {
                            MoodPageView(mood: MoodItem(chip: chip, color: AccentStore.shared.color))
                        } label: {
                            ChipLabel(title: chip.title, selected: false)
                        }
                        .buttonStyle(.squishy)
                    } else {
                        ChipButton(title: chip.title, selected: chip.params != nil && chip.params == selected) {
                            onSelect(chip)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 4)
        }
        .sensoryFeedback(.selection, trigger: selected)
    }
}

/// Filtro a capsula di vetro. Non è un Button di sistema: il tocco vale solo dentro la capsula
/// (con i pulsanti di vetro in una riga che scorre, a volte il tocco finiva sull'elemento sotto).
struct ChipButton: View {
    let title: String
    let selected: Bool
    let action: () -> Void
    var body: some View {
        ChipLabel(title: title, selected: selected)
            .contentShape(Capsule())
            .onTapGesture(perform: action)
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { action() }
    }
}

struct ChipLabel: View {
    let title: String
    let selected: Bool

    var body: some View {
        Text(L(title))
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(selected ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
            .padding(.horizontal, 16)
            .padding(.vertical, 9)
            .background {
                if selected {
                    Capsule().fill(.tint)
                }
            }
            .chipGlass(selected: selected)
            .animation(.snappy, value: selected)
    }
}

private extension View {
    @ViewBuilder func chipGlass(selected: Bool) -> some View {
        if selected {
            self
        } else if #available(iOS 26.0, *) {
            glassEffect(.regular, in: Capsule())
        } else {
            background(.thinMaterial, in: Capsule())
        }
    }
}

// MARK: - Fatte per te

private struct MixesSection: View {
    let mixes: [MixItem]
    @ObservedObject private var home = HomeStore.shared
    @State private var spins = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                NavigationLink {
                    MixesPage()
                } label: {
                    SectionTitle(title: "Fatte per te", subtitle: "Dai tuoi ascolti, ogni settimana")
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                Button {
                    spins += 1
                    home.refreshMixes()
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.headline)
                        .foregroundStyle(.tint)
                        .rotationEffect(.degrees(Double(spins) * 360))
                        .animation(.spring(response: 0.6, dampingFraction: 0.7), value: spins)
                        .frame(width: 36, height: 36)
                }
                .padding(.trailing, 14)
                .accessibilityLabel(L("Rifai i mix"))
                .sensoryFeedback(.impact(weight: .light), trigger: spins)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 14) {
                    ForEach(mixes) { mix in
                        NavigationLink {
                            MixPage(mix: mix)
                                .zoomDestination("mix:" + mix.id)
                        } label: {
                            MixCard(mix: mix)
                        }
                        .buttonStyle(.squishy)
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

private struct MixCard: View {
    let mix: MixItem
    var zoomID: String? = nil

    private var colors: [Color] {
        switch mix.kind {
        case 1: return [Color(red: 0.25, green: 0.55, blue: 0.95), Color(red: 0.15, green: 0.2, blue: 0.6)]
        case 2: return [Color(red: 0.98, green: 0.62, blue: 0.3), Color(red: 0.7, green: 0.25, blue: 0.2)]
        case 3: return [Color(red: 0.35, green: 0.75, blue: 0.55), Color(red: 0.1, green: 0.35, blue: 0.3)]
        default: return [Color(red: 0.98, green: 0.45, blue: 0.62), Color(red: 0.45, green: 0.2, blue: 0.55)]
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .bottomLeading) {
                MosaicArtwork(urls: mix.covers, cornerRadius: 16)
                LinearGradient(colors: [colors[0].opacity(0.25), colors[1].opacity(0.95)], startPoint: .top, endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Image(systemName: "sparkles")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white.opacity(0.85))
                    Text(mix.title)
                        .font(.title3.weight(.heavy))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8)
                }
                .padding(14)
            }
            .frame(width: zoomID == nil ? 180 : nil, height: zoomID == nil ? 180 : nil)
            .aspectRatio(1, contentMode: .fit)
            .zoomSource(zoomID ?? "mix:" + mix.id)
            .shadow(color: colors[1].opacity(0.35), radius: 12, y: 6)
            Text(mix.subtitle)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .lineLimit(2)
                .frame(width: zoomID == nil ? 180 : nil, alignment: .leading)
                .frame(maxWidth: zoomID == nil ? nil : .infinity, alignment: .leading)
        }
        .contentShape(Rectangle())
    }
}

struct MixPage: View {
    let mix: MixItem
    @ObservedObject private var home = HomeStore.shared

    var body: some View {
        SongListPage(
            title: mix.title,
            subtitle: mix.subtitle,
            artwork: { MosaicArtwork(urls: mix.covers, cornerRadius: 12) },
            songs: home.mixTracks(mix),
            play: { index, shuffle in home.playMix(mix, from: index, shuffle: shuffle) },
            tintURL: mix.covers.first
        )
    }
}

/// Tutti i mix "Fatte per te" in griglia.
private struct MixesPage: View {
    @ObservedObject private var home = HomeStore.shared
    private let columns = [GridItem(.adaptive(minimum: 160), spacing: 14, alignment: .top)]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 20) {
                ForEach(home.mixes) { mix in
                    NavigationLink {
                        MixPage(mix: mix)
                            .zoomDestination("mixgrid:" + mix.id)
                    } label: {
                        MixCard(mix: mix, zoomID: "mixgrid:" + mix.id)
                    }
                    .buttonStyle(.squishy)
                }
            }
            .padding(.horizontal, 20)
        }
        .contentMargins(.vertical, 12, for: .scrollContent)
        .navigationTitle(L("Fatte per te"))
    }
}

/// Tutti gli ascoltati di recente.
private struct RecentPage: View {
    @ObservedObject private var home = HomeStore.shared
    @ObservedObject private var player = PlayerStore.shared

    var body: some View {
        List {
            ForEach(Array(home.recent.enumerated()), id: \.offset) { index, song in
                Button {
                    home.playRecent(at: index)
                } label: {
                    SongRow(song: song, playing: player.state.track?.id == song.id)
                }
                .buttonStyle(.plain)
                .contextMenu {
                    SongMenuItems(song: song, onAddToPlaylist: { NamiRouter.shared.addingToPlaylist = song })
                }
            }
        }
        .listStyle(.plain)
        .navigationTitle(L("Ascoltati di recente"))
    }
}

// MARK: - Ascoltati di recente

private struct RecentSection: View {
    let songs: [Track]
    @ObservedObject private var home = HomeStore.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            NavigationLink {
                RecentPage()
            } label: {
                SectionTitle(title: "Ascoltati di recente")
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 14) {
                    ForEach(Array(songs.enumerated()), id: \.offset) { index, song in
                        Button {
                            home.playRecent(at: index)
                        } label: {
                            CardLabel(artworkURL: song.artworkURL, title: song.title, subtitle: song.artist, explicit: song.explicit, size: 140)
                        }
                        .buttonStyle(.squishy)
                        .contextMenu {
                            SongMenuItems(song: song, onAddToPlaylist: { NamiRouter.shared.addingToPlaylist = song })
                        }
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

// MARK: - Album e playlist di YouTube Music

struct CollectionPage: View {
    let item: HomeItem
    @StateObject private var store: CollectionStore

    init(item: HomeItem) {
        self.item = item
        _store = StateObject(wrappedValue: CollectionStore(key: item.id))
    }

    var body: some View {
        Group {
            if let error = store.error {
                ErrorView(title: "Non disponibile", message: error) { store.load() }
            } else {
                SongListPage(
                    title: store.title.isEmpty ? item.title : store.title,
                    subtitle: store.loading ? item.subtitle : (store.subtitle.isEmpty ? LF("{0} brani", store.tracks.count) : store.subtitle + " · " + LF("{0} brani", store.tracks.count)),
                    artwork: { ArtworkView(url: store.artworkURL ?? item.artworkURL, cornerRadius: 12) },
                    songs: store.tracks,
                    play: { index, shuffle in store.play(from: index, shuffle: shuffle) },
                    loading: store.loading,
                    tintURL: store.artworkURL ?? item.artworkURL,
                    saved: store.loading ? nil : store.saved,
                    onToggleSave: store.toggleSave,
                    sortable: item.kind == .playlist
                )
            }
        }
    }
}
