import SwiftUI

// Home come Apple Music: mood in alto, mix "Fatte per te", ascoltati di recente e le sezioni
// di YouTube Music. Album e playlist si aprono nella loro pagina, i brani partono subito.

struct HomeView: View {
    @ObservedObject private var home = HomeStore.shared
    let onSettings: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 30) {
                if !home.chips.isEmpty {
                    ChipsRow(chips: home.chips, selected: home.selectedChip, onSelect: home.select(chip:))
                }
                if !home.mixes.isEmpty {
                    MixesSection(mixes: home.mixes)
                }
                if !home.recent.isEmpty {
                    RecentSection(songs: home.recent)
                }
                if home.sections.isEmpty {
                    if home.loading {
                        ProgressView()
                            .controlSize(.large)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 40)
                    } else if let error = home.error {
                        ContentUnavailableView {
                            Label("Home non disponibile", systemImage: "wifi.exclamationmark")
                        } description: {
                            Text(error)
                        } actions: {
                            Button("Riprova") { home.load() }
                                .buttonStyle(.borderedProminent)
                        }
                    }
                }
                ForEach(home.sections) { section in
                    HomeSectionView(section: section)
                }
            }
            .padding(.top, 4)
            .padding(.bottom, 32)
        }
        .navigationTitle("Home")
        .toolbar { SettingsToolbar(onSettings: onSettings) }
        .refreshable { await home.reload() }
        .onAppear { home.loadIfNeeded() }
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
    }
}

private struct ChipButton: View {
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

// MARK: - Intestazioni

struct SectionTitle: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.title2.weight(.bold))
                Image(systemName: "chevron.forward")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.secondary)
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

// MARK: - Fatte per te

private struct MixesSection: View {
    let mixes: [MixItem]
    @ObservedObject private var home = HomeStore.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                SectionTitle(title: "Fatte per te", subtitle: "Dai tuoi ascolti, ogni settimana")
                Button {
                    home.refreshMixes()
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.headline)
                }
                .padding(.trailing, 20)
                .accessibilityLabel("Rifai i mix")
            }
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 14) {
                    ForEach(mixes) { mix in
                        NavigationLink {
                            MixPage(mix: mix)
                        } label: {
                            MixCard(mix: mix)
                        }
                        .buttonStyle(.plain)
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
        VStack(alignment: .leading, spacing: 6) {
            ZStack(alignment: .bottomLeading) {
                MosaicArtwork(urls: mix.covers, cornerRadius: 12)
                LinearGradient(colors: [colors[0].opacity(0.35), colors[1].opacity(0.9)], startPoint: .top, endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                Text(mix.title)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .padding(12)
            }
            .frame(width: 170, height: 170)
            .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
            Text(mix.subtitle)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .lineLimit(2)
                .frame(width: 170, alignment: .leading)
        }
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
            play: { index, shuffle in home.playMix(mix, from: index, shuffle: shuffle) }
        )
    }
}

// MARK: - Ascoltati di recente

private struct RecentSection: View {
    let songs: [Track]
    @ObservedObject private var home = HomeStore.shared
    @State private var addingToPlaylist: Track?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: "Ascoltati di recente")
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 14) {
                    ForEach(Array(songs.enumerated()), id: \.offset) { index, song in
                        Button {
                            home.playRecent(at: index)
                        } label: {
                            CardLabel(artworkURL: song.artworkURL, title: song.title, subtitle: song.artist, explicit: song.explicit)
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            SongMenuItems(song: song, onAddToPlaylist: { addingToPlaylist = song })
                        }
                    }
                }
                .padding(.horizontal, 20)
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
        }
        .sheet(item: $addingToPlaylist) { song in
            AddToPlaylistSheet(song: song)
        }
    }
}

// MARK: - Sezioni di YouTube Music

private struct HomeSectionView: View {
    let section: HomeSection

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: section.title)
            if section.allSongs {
                QuickPicks(items: section.items)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: 14) {
                        ForEach(section.items) { item in
                            HomeItemCard(item: item)
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
private struct QuickPicks: View {
    let items: [HomeItem]
    @ObservedObject private var home = HomeStore.shared
    @ObservedObject private var player = PlayerStore.shared

    private let rows = Array(repeating: GridItem(.fixed(56), spacing: 6), count: 4)

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHGrid(rows: rows, spacing: 16) {
                ForEach(items) { item in
                    Button {
                        home.play(item)
                    } label: {
                        HStack(spacing: 12) {
                            ArtworkView(url: item.artworkURL, size: 50, cornerRadius: 5)
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 4) {
                                    Text(item.title)
                                        .font(.body)
                                        .lineLimit(1)
                                    if item.explicit {
                                        Image(systemName: "e.square.fill")
                                            .font(.caption2)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                                Text(item.subtitle)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 0)
                        }
                        .frame(width: 300, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(.viewAligned)
        .frame(height: 4 * 56 + 3 * 6)
    }
}

private struct HomeItemCard: View {
    let item: HomeItem
    @ObservedObject private var home = HomeStore.shared

    var body: some View {
        switch item.kind {
        case .album, .playlist:
            NavigationLink {
                CollectionPage(item: item)
            } label: {
                CardLabel(artworkURL: item.artworkURL, title: item.title, subtitle: item.subtitle, explicit: item.explicit)
            }
            .buttonStyle(.plain)
        case .artist:
            NavigationLink {
                ComingSoonView(title: "Artisti in arrivo", systemImage: "music.microphone", text: "Le pagine degli artisti in SwiftUI arrivano in una delle prossime versioni.")
                    .navigationTitle(item.title)
            } label: {
                VStack(spacing: 8) {
                    RemoteImage(url: item.artworkURL)
                        .frame(width: 140, height: 140)
                        .clipShape(Circle())
                    Text(item.title)
                        .font(.subheadline.weight(.medium))
                        .lineLimit(1)
                        .frame(width: 140)
                }
            }
            .buttonStyle(.plain)
        default:
            Button {
                home.play(item)
            } label: {
                CardLabel(artworkURL: item.artworkURL, title: item.title, subtitle: item.subtitle, explicit: item.explicit)
            }
            .buttonStyle(.plain)
        }
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
            ArtworkView(url: artworkURL, size: size, cornerRadius: 10)
                .shadow(color: .black.opacity(0.12), radius: 6, y: 3)
            HStack(spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)
                if explicit {
                    Image(systemName: "e.square.fill")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
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
            if store.loading {
                ProgressView()
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = store.error {
                ContentUnavailableView {
                    Label("Non disponibile", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(error)
                } actions: {
                    Button("Riprova") { store.load() }
                        .buttonStyle(.borderedProminent)
                }
            } else {
                SongListPage(
                    title: store.title.isEmpty ? item.title : store.title,
                    subtitle: store.subtitle.isEmpty ? "\(store.tracks.count) brani" : "\(store.subtitle) · \(store.tracks.count) brani",
                    artwork: { ArtworkView(url: store.artworkURL ?? item.artworkURL, cornerRadius: 12) },
                    songs: store.tracks,
                    play: { index, shuffle in store.play(from: index, shuffle: shuffle) }
                )
            }
        }
    }
}
