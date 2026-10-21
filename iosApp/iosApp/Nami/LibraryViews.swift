import SwiftUI

// Libreria: preferiti e playlist, pagina di una playlist con i suoi brani.

struct LibraryView: View {
    @ObservedObject private var library = LibraryStore.shared
    let onSettings: () -> Void

    private let columns = [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                NavigationLink {
                    SongListPage(
                        title: "Preferiti",
                        subtitle: "\(library.liked.count) brani",
                        artwork: { LikedArtwork() },
                        songs: library.liked,
                        play: { index, shuffle in library.playLiked(from: index, shuffle: shuffle) }
                    )
                } label: {
                    LibraryRow(systemImage: "heart.fill", title: "Preferiti", detail: "\(library.liked.count)")
                }
                .buttonStyle(.plain)
                Divider().padding(.leading, 56)

                Text("Playlist")
                    .font(.title2.weight(.bold))
                    .padding(.top, 24)
                    .padding(.bottom, 12)

                if library.playlists.isEmpty {
                    Text("Nessuna playlist")
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 24)
                } else {
                    LazyVGrid(columns: columns, alignment: .leading, spacing: 20) {
                        ForEach(library.playlists) { playlist in
                            NavigationLink {
                                PlaylistPage(playlist: playlist)
                            } label: {
                                PlaylistTile(playlist: playlist)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
        .navigationTitle("Libreria")
        .toolbar { SettingsToolbar(onSettings: onSettings) }
    }
}

private struct LibraryRow: View {
    let systemImage: String
    let title: String
    let detail: String

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(Color.namiAccent)
                .frame(width: 30)
            Text(title)
                .font(.title3)
            Spacer()
            Text(detail)
                .foregroundStyle(.secondary)
            Image(systemName: "chevron.forward")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}

private struct PlaylistTile: View {
    let playlist: PlaylistItem

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            MosaicArtwork(urls: playlist.thumbnails, cornerRadius: 10)
                .shadow(color: .black.opacity(0.12), radius: 6, y: 3)
            Text(playlist.title)
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
            Text("\(playlist.songCount) brani")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .contentShape(Rectangle())
    }
}

private struct LikedArtwork: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.98, green: 0.45, blue: 0.62), Color(red: 0.45, green: 0.2, blue: 0.55)], startPoint: .topLeading, endPoint: .bottomTrailing)
            Image(systemName: "heart.fill")
                .font(.system(size: 72, weight: .semibold))
                .foregroundStyle(.white)
        }
        .aspectRatio(1, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

// MARK: - Pagina di una playlist

struct PlaylistPage: View {
    let playlist: PlaylistItem
    @StateObject private var store: PlaylistSongsStore
    @ObservedObject private var library = LibraryStore.shared

    init(playlist: PlaylistItem) {
        self.playlist = playlist
        _store = StateObject(wrappedValue: PlaylistSongsStore(playlistId: playlist.id))
    }

    var body: some View {
        SongListPage(
            title: playlist.title,
            subtitle: "\(store.songs.count) brani",
            artwork: { MosaicArtwork(urls: playlist.thumbnails, cornerRadius: 12) },
            songs: store.songs,
            play: { index, shuffle in library.playPlaylist(playlist, from: index, shuffle: shuffle) }
        )
    }
}

/// Pagina con copertina, titolo, Riproduci/Casuale e l'elenco dei brani (come Apple Music).
struct SongListPage<Artwork: View>: View {
    let title: String
    let subtitle: String
    @ViewBuilder let artwork: () -> Artwork
    let songs: [Track]
    let play: (_ index: Int, _ shuffle: Bool) -> Void
    @ObservedObject private var player = PlayerStore.shared

    var body: some View {
        List {
            VStack(spacing: 14) {
                artwork()
                    .frame(width: 250, height: 250)
                    .shadow(color: .black.opacity(0.18), radius: 18, y: 10)
                VStack(spacing: 3) {
                    Text(title)
                        .font(.title2.weight(.bold))
                        .multilineTextAlignment(.center)
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                HStack(spacing: 14) {
                    PlayActionButton(title: "Riproduci", systemImage: "play.fill") { play(0, false) }
                    PlayActionButton(title: "Casuale", systemImage: "shuffle") { play(0, true) }
                }
                .disabled(songs.isEmpty)
                .padding(.top, 4)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)

            ForEach(Array(songs.enumerated()), id: \.offset) { index, song in
                Button {
                    play(index, false)
                } label: {
                    SongRow(song: song, playing: player.state.track?.id == song.id)
                }
                .buttonStyle(.plain)
                .listRowInsets(EdgeInsets(top: 6, leading: 20, bottom: 6, trailing: 16))
            }
        }
        .listStyle(.plain)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Riproduci / Casuale: capsule di vetro con l'accento (iOS 26), grigie prima.
private struct PlayActionButton: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            Button(action: action) {
                Label(title, systemImage: systemImage)
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 34)
            }
            .buttonStyle(.glass)
            .controlSize(.large)
            .tint(.namiAccent)
            .foregroundStyle(Color.namiAccent)
        } else {
            Button(action: action) {
                Label(title, systemImage: systemImage)
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 34)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
            .tint(.namiAccent)
        }
    }
}

struct SongRow: View {
    let song: Track
    var playing: Bool = false

    var body: some View {
        HStack(spacing: 12) {
            ArtworkView(url: song.artworkURL, size: 44, cornerRadius: 5)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(song.title)
                        .font(.body)
                        .foregroundStyle(playing ? Color.namiAccent : .primary)
                        .lineLimit(1)
                    if song.explicit {
                        Image(systemName: "e.square.fill")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                Text(song.artist)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            if playing {
                Image(systemName: "waveform")
                    .foregroundStyle(Color.namiAccent)
                    .symbolEffect(.variableColor.iterative, options: .repeating)
            }
        }
        .contentShape(Rectangle())
    }
}

// MARK: - Impostazioni

struct SettingsView: View {
    @ObservedObject private var app = NamiApp.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(isOn: Binding(get: { app.iosUI }, set: { app.setIosUI($0) })) {
                        Label("Interfaccia iOS (Liquid Glass)", systemImage: "sparkles")
                    }
                } footer: {
                    Text("Spenta, Nami torna all'interfaccia completa di sempre. Le schermate in stile iOS arrivano una alla volta: per ora ci sono player e Libreria.")
                }
            }
            .navigationTitle("Impostazioni")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Fine") { dismiss() }
                }
            }
        }
    }
}
