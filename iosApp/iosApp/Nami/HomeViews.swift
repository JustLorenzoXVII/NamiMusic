import SwiftUI

// Home: saluto in base all'ora, bagliore colorato dalla copertina in riproduzione, card
// "In riproduzione", mood, mix "Fatte per te", ascoltati di recente e le sezioni di YouTube Music.

struct HomeView: View {
    @ObservedObject private var home = HomeStore.shared
    @ObservedObject private var player = PlayerStore.shared
    let onSettings: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 30) {
                Text(Self.dateLine)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 20)
                    .padding(.bottom, -18)

                if player.state.track != nil {
                    NowPlayingCard()
                        .padding(.horizontal, 20)
                        .transition(.asymmetric(insertion: .scale(scale: 0.92).combined(with: .opacity), removal: .opacity))
                }
                if !home.chips.isEmpty {
                    ChipsRow(chips: home.chips, selected: home.selectedChip, onSelect: { chip in
                        withAnimation(.snappy) { home.select(chip: chip) }
                    })
                }
                if !home.mixes.isEmpty {
                    MixesSection(mixes: home.mixes)
                }
                if !home.recent.isEmpty {
                    RecentSection(songs: home.recent)
                }
                if home.sections.isEmpty {
                    if home.loading {
                        LoadingView()
                    } else if let error = home.error {
                        ErrorView(title: "Home non disponibile", message: error) { home.load() }
                    }
                }
                SectionsList(sections: home.sections)
                    .opacity(home.loading && !home.sections.isEmpty ? 0.45 : 1)
            }
            .padding(.top, 4)
            .padding(.bottom, 32)
            .animation(.smooth(duration: 0.4), value: home.sections.map(\.id))
            .animation(.spring(response: 0.45, dampingFraction: 0.8), value: player.state.track?.id)
        }
        .background(alignment: .top) { AmbientGlow() }
        .navigationTitle(Self.greeting)
        .toolbar { SettingsToolbar(onSettings: onSettings) }
        .refreshable { await home.reload() }
        .onAppear { home.loadIfNeeded() }
    }

    static var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<13: return "Buongiorno"
        case 13..<18: return "Buon pomeriggio"
        default: return "Buonasera"
        }
    }

    static var dateLine: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "it_IT")
        formatter.dateFormat = "EEEE d MMMM"
        return formatter.string(from: Date()).capitalized(with: Locale(identifier: "it_IT"))
    }
}

// MARK: - Bagliore dalla copertina

/// Una macchia di colore sfumata dietro l'intestazione, presa dalla copertina che sta suonando
/// (l'accento di Nami se non suona niente). Cambia colore con un'animazione lenta.
struct AmbientGlow: View {
    @ObservedObject private var player = PlayerStore.shared
    @State private var palette: ArtworkPalette?
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let first = palette?.top ?? Color.namiAccent
        let second = palette?.bottom ?? Color.namiAccent.opacity(0.4)
        ZStack {
            RadialGradient(colors: [first.opacity(scheme == .dark ? 0.75 : 0.45), .clear], center: .topLeading, startRadius: 10, endRadius: 380)
            RadialGradient(colors: [second.opacity(scheme == .dark ? 0.6 : 0.3), .clear], center: .topTrailing, startRadius: 10, endRadius: 320)
        }
        .frame(height: 460)
        .mask(LinearGradient(colors: [.black, .black.opacity(0.6), .clear], startPoint: .top, endPoint: .bottom))
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .animation(.easeInOut(duration: 1.2), value: palette)
        .onAppear(perform: update)
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
                Text(player.state.isPlaying ? "IN RIPRODUZIONE" : "IN PAUSA")
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
                ProgressView(value: player.duration > 0 ? min(player.position / player.duration, 1) : 0)
                    .tint(.white)
                    .scaleEffect(x: 1, y: 0.7, anchor: .center)
                    .padding(.top, 4)
            }
            Spacer(minLength: 4)
            Button(action: player.togglePlayPause) {
                Image(systemName: player.state.isPlaying ? "pause.fill" : "play.fill")
                    .font(.title2)
                    .foregroundStyle(.white)
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 52, height: 52)
            }
            .buttonStyle(.squishy)
            .glassCircle()
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
    /// Cerchio di vetro (iOS 26), materiale sfocato prima.
    @ViewBuilder func glassCircle() -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular.interactive(), in: Circle())
        } else {
            background(.ultraThinMaterial, in: Circle())
        }
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
                    ChipButton(title: chip.title, selected: chip.params != nil && chip.params == selected) {
                        onSelect(chip)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 4)
        }
        .sensoryFeedback(.selection, trigger: selected)
    }
}

struct ChipButton: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            if selected {
                Button(title, action: action)
                    .buttonStyle(.glassProminent)
                    .tint(.namiAccent)
            } else {
                Button(title, action: action)
                    .buttonStyle(.glass)
            }
        } else {
            if selected {
                Button(title, action: action)
                    .buttonStyle(.borderedProminent)
                    .tint(.namiAccent)
            } else {
                Button(title, action: action)
                    .buttonStyle(.bordered)
            }
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
                SectionTitle(title: "Fatte per te", subtitle: "Dai tuoi ascolti, ogni settimana")
                Button {
                    spins += 1
                    home.refreshMixes()
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.headline)
                        .foregroundStyle(Color.namiAccent)
                        .rotationEffect(.degrees(Double(spins) * 360))
                        .animation(.spring(response: 0.6, dampingFraction: 0.7), value: spins)
                        .frame(width: 36, height: 36)
                }
                .padding(.trailing, 14)
                .accessibilityLabel("Rifai i mix")
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
            .frame(width: 180, height: 180)
            .zoomSource("mix:" + mix.id)
            .shadow(color: colors[1].opacity(0.35), radius: 12, y: 6)
            Text(mix.subtitle)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .lineLimit(2)
                .frame(width: 180, alignment: .leading)
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

// MARK: - Ascoltati di recente

private struct RecentSection: View {
    let songs: [Track]
    @ObservedObject private var home = HomeStore.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: "Ascoltati di recente")
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
                    subtitle: store.loading ? item.subtitle : (store.subtitle.isEmpty ? "\(store.tracks.count) brani" : "\(store.subtitle) · \(store.tracks.count) brani"),
                    artwork: { ArtworkView(url: store.artworkURL ?? item.artworkURL, cornerRadius: 12) },
                    songs: store.tracks,
                    play: { index, shuffle in store.play(from: index, shuffle: shuffle) },
                    loading: store.loading,
                    tintURL: store.artworkURL ?? item.artworkURL
                )
            }
        }
    }
}
