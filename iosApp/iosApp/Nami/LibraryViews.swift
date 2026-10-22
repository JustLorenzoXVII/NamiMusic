import SwiftUI

// Libreria: preferiti e playlist (crea, rinomina, elimina), pagina di una playlist con i suoi brani
// (togli, riordina), menu dei brani e scelta della playlist a cui aggiungere un brano.

struct LibraryView: View {
    @ObservedObject private var library = LibraryStore.shared
    @ObservedObject private var downloads = DownloadsStore.shared
    @ObservedObject private var saved = SavedStore.shared
    let onSettings: () -> Void

    @State private var creating = false
    @State private var renaming: PlaylistItem?
    @State private var newName = ""
    @State private var deleting: PlaylistItem?

    private let columns = [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 14) {
                    NavigationLink {
                        SongListPage(
                            title: "Preferiti",
                            subtitle: "\(library.liked.count) brani",
                            artwork: { LikedArtwork() },
                            songs: library.liked,
                            play: { index, shuffle in library.playLiked(from: index, shuffle: shuffle) },
                            sortable: true
                        )
                        .zoomDestination("library:liked")
                    } label: {
                        LibraryTile(
                            title: "Preferiti",
                            detail: "\(library.liked.count) brani",
                            systemImage: "heart.fill",
                            colors: [Color(red: 0.98, green: 0.45, blue: 0.62), Color(red: 0.45, green: 0.2, blue: 0.55)]
                        )
                        .zoomSource("library:liked")
                    }
                    .buttonStyle(.squishy)
                    NavigationLink {
                        DownloadsPage()
                            .zoomDestination("library:downloads")
                    } label: {
                        LibraryTile(
                            title: "Scaricati",
                            detail: "\(downloads.songs.count) brani",
                            systemImage: "arrow.down.circle.fill",
                            colors: [Color(red: 0.36, green: 0.55, blue: 0.98), Color(red: 0.2, green: 0.2, blue: 0.55)]
                        )
                        .zoomSource("library:downloads")
                    }
                    .buttonStyle(.squishy)
                }
                .padding(.top, 8)

                HStack {
                    Text("Playlist")
                        .font(.title2.weight(.bold))
                    Spacer()
                    Button {
                        creating = true
                    } label: {
                        Label("Nuova", systemImage: "plus")
                            .font(.subheadline.weight(.semibold))
                    }
                }
                .padding(.top, 24)
                .padding(.bottom, 12)

                if library.playlists.isEmpty {
                    Button {
                        creating = true
                    } label: {
                        VStack(spacing: 10) {
                            Image(systemName: "music.note.list")
                                .font(.system(size: 40))
                                .foregroundStyle(.tint)
                            Text("Crea la tua prima playlist")
                                .font(.headline)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                    }
                    .buttonStyle(.plain)
                } else {
                    LazyVGrid(columns: columns, alignment: .leading, spacing: 20) {
                        ForEach(library.playlists) { playlist in
                            NavigationLink {
                                PlaylistPage(playlist: playlist)
                                    .zoomDestination("playlist:" + playlist.id)
                            } label: {
                                PlaylistTile(playlist: playlist)
                            }
                            .buttonStyle(.squishy)
                            .contextMenu {
                                Button {
                                    library.playPlaylist(playlist)
                                } label: {
                                    Label("Riproduci", systemImage: "play.fill")
                                }
                                Button {
                                    library.playPlaylist(playlist, shuffle: true)
                                } label: {
                                    Label("Casuale", systemImage: "shuffle")
                                }
                                if playlist.isEditable {
                                    Button {
                                        newName = playlist.title
                                        renaming = playlist
                                    } label: {
                                        Label("Rinomina", systemImage: "pencil")
                                    }
                                }
                                Button(role: .destructive) {
                                    deleting = playlist
                                } label: {
                                    Label(playlist.isEditable ? "Elimina" : "Togli dalla Libreria", systemImage: "trash")
                                }
                            }
                        }
                    }
                    .animation(.snappy, value: library.playlists)
                }

                if !saved.albums.isEmpty {
                    SectionView(section: HomeSection(id: "saved-albums", title: "Album", items: saved.albums, allSongs: false))
                        .padding(.horizontal, -20)
                        .padding(.top, 30)
                }
                if !saved.artists.isEmpty {
                    SectionView(section: HomeSection(id: "saved-artists", title: "Artisti", items: saved.artists, allSongs: false))
                        .padding(.horizontal, -20)
                        .padding(.top, 30)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
        .navigationTitle("Libreria")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    creating = true
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Nuova playlist")
            }
            SettingsToolbar(onSettings: onSettings)
        }
        .sheet(isPresented: $creating) {
            NewPlaylistSheet()
                .namiTint()
        }
        .alert("Rinomina playlist", isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField("Nome", text: $newName)
            Button("Annulla", role: .cancel) { renaming = nil }
            Button("Salva") {
                let name = newName.trimmingCharacters(in: .whitespacesAndNewlines)
                if let playlist = renaming, !name.isEmpty { library.rename(playlist, to: name) }
                renaming = nil
            }
        }
        .confirmationDialog(
            deleting.map { "Eliminare \"\($0.title)\"?" } ?? "",
            isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }),
            titleVisibility: .visible
        ) {
            Button(deleting?.isEditable == false ? "Togli dalla Libreria" : "Elimina playlist", role: .destructive) {
                if let playlist = deleting { library.delete(playlist) }
                deleting = nil
            }
        } message: {
            if deleting?.isEditable == true {
                Text("Se è anche su YouTube Music, viene eliminata anche lì.")
            }
        }
    }
}

/// Tessera colorata in cima alla Libreria (Preferiti, Scaricati).
private struct LibraryTile: View {
    let title: String
    let detail: String
    let systemImage: String
    let colors: [Color]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Image(systemName: systemImage)
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.white)
                .symbolEffect(.bounce, value: detail)
            Spacer(minLength: 18)
            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
            Text(detail)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.8))
                .contentTransition(.numericText())
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 120, alignment: .leading)
        .background(
            LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing),
            in: RoundedRectangle(cornerRadius: 20, style: .continuous)
        )
        .shadow(color: colors[1].opacity(0.35), radius: 12, y: 6)
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
                .foregroundStyle(.tint)
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
            MosaicArtwork(urls: playlist.thumbnails, cornerRadius: 12)
                .aspectRatio(1, contentMode: .fit)
                .zoomSource("playlist:" + playlist.id)
                .shadow(color: .black.opacity(0.14), radius: 8, y: 4)
            Text(playlist.title)
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
            Text("\(playlist.songCount) brani")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .contentShape(.contextMenuPreview, RoundedRectangle(cornerRadius: 10, style: .continuous))
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
    @Environment(\.dismiss) private var dismiss

    @State private var renaming = false
    @State private var newName = ""
    @State private var deleting = false

    init(playlist: PlaylistItem) {
        self.playlist = playlist
        _store = StateObject(wrappedValue: PlaylistSongsStore(playlistId: playlist.id))
    }

    /// Il nome aggiornato (dopo una rinomina) e se si può modificare.
    private var current: PlaylistItem {
        library.playlists.first { $0.id == playlist.id } ?? playlist
    }

    var body: some View {
        SongListPage(
            title: current.title,
            subtitle: "\(store.songs.count) brani",
            artwork: { MosaicArtwork(urls: current.thumbnails, cornerRadius: 12) },
            songs: store.songs,
            play: { index, shuffle in library.playPlaylist(current, from: index, shuffle: shuffle) },
            onRemove: current.isEditable ? { index in library.remove(at: index, from: current) } : nil,
            onMove: current.isEditable ? { from, to in library.move(in: current, from: from, to: to) } : nil,
            tintURL: current.thumbnails.first,
            sortable: true
        )
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    if current.isEditable {
                        Button {
                            newName = current.title
                            renaming = true
                        } label: {
                            Label("Rinomina", systemImage: "pencil")
                        }
                    }
                    Button(role: .destructive) {
                        deleting = true
                    } label: {
                        Label(current.isEditable ? "Elimina playlist" : "Togli dalla Libreria", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                }
                .accessibilityLabel("Altro")
            }
        }
        .alert("Rinomina playlist", isPresented: $renaming) {
            TextField("Nome", text: $newName)
            Button("Annulla", role: .cancel) {}
            Button("Salva") {
                let name = newName.trimmingCharacters(in: .whitespacesAndNewlines)
                if !name.isEmpty { library.rename(current, to: name) }
            }
        }
        .confirmationDialog("Eliminare \"\(current.title)\"?", isPresented: $deleting, titleVisibility: .visible) {
            Button(current.isEditable ? "Elimina playlist" : "Togli dalla Libreria", role: .destructive) {
                library.delete(current)
                dismiss()
            }
        } message: {
            if current.isEditable {
                Text("Se è anche su YouTube Music, viene eliminata anche lì.")
            }
        }
    }
}

/// Ordine dei brani in una pagina (come su Android).
enum SongSort: Int, CaseIterable {
    case custom, title, artist, album, duration

    var label: String {
        switch self {
        case .custom: return "Personalizzato"
        case .title: return "Titolo"
        case .artist: return "Artista"
        case .album: return "Album"
        case .duration: return "Durata"
        }
    }
}

/// Pagina con copertina, titolo, Riproduci/Casuale e l'elenco dei brani (come Apple Music).
/// Sotto i pulsanti: Salva (album e playlist di YouTube Music), Scarica tutto e Ordina.
/// Con [onRemove] e [onMove] i brani si tolgono scorrendo e si riordinano con "Modifica".
struct SongListPage<Artwork: View>: View {
    let title: String
    let subtitle: String
    @ViewBuilder let artwork: () -> Artwork
    let songs: [Track]
    let play: (_ index: Int, _ shuffle: Bool) -> Void
    var onRemove: ((_ index: Int) -> Void)? = nil
    var onMove: ((_ from: Int, _ to: Int) -> Void)? = nil
    var loading: Bool = false
    /// Copertina da cui prendere i colori del bagliore in alto
    var tintURL: URL? = nil
    /// Salva in Libreria (album e playlist di YouTube Music)
    var saved: Bool? = nil
    var onToggleSave: (() -> Void)? = nil
    /// Mostra il pulsante "Ordina"
    var sortable: Bool = false

    @ObservedObject private var player = PlayerStore.shared
    @ObservedObject private var downloads = DownloadsStore.shared
    @State private var rows: [Track] = []
    @State private var palette: ArtworkPalette?
    @State private var appeared = false
    @AppStorage("nami.sort") private var sortRaw = 0
    @AppStorage("nami.sortDescending") private var descending = false
    @Environment(\.colorScheme) private var scheme

    private var sort: SongSort { sortable ? (SongSort(rawValue: sortRaw) ?? .custom) : .custom }
    private var sorted: Bool { sort != .custom || (sortable && descending) }

    /// Brani nell'ordine mostrato, con la loro posizione originale.
    private var displayed: [(index: Int, track: Track)] {
        let base = Array(rows.enumerated()).map { (index: $0.offset, track: $0.element) }
        let ordered: [(index: Int, track: Track)]
        switch sort {
        case .custom: ordered = base
        case .title: ordered = base.sorted { $0.track.title.localizedCaseInsensitiveCompare($1.track.title) == .orderedAscending }
        case .artist: ordered = base.sorted { $0.track.artist.localizedCaseInsensitiveCompare($1.track.artist) == .orderedAscending }
        case .album: ordered = base.sorted { ($0.track.albumTitle ?? "").localizedCaseInsensitiveCompare($1.track.albumTitle ?? "") == .orderedAscending }
        case .duration: ordered = base.sorted { $0.track.duration < $1.track.duration }
        }
        return sortable && descending ? ordered.reversed() : ordered
    }

    private var allDownloaded: Bool {
        !rows.isEmpty && rows.allSatisfy { downloads.info($0)?.completed == true }
    }

    private func start(_ position: Int, shuffle: Bool) {
        if sorted {
            NamiPlayTracks(displayed.map(\.track), title: title, from: position, shuffle: shuffle)
        } else {
            play(position, shuffle)
        }
    }

    var body: some View {
        let list = displayed
        List {
            VStack(spacing: 14) {
                artwork()
                    .frame(width: 250, height: 250)
                    .shadow(color: (palette?.bottom ?? .black).opacity(0.35), radius: 22, y: 12)
                    .scaleEffect(appeared ? 1 : 0.94)
                    .animation(.spring(response: 0.5, dampingFraction: 0.75), value: appeared)
                VStack(spacing: 3) {
                    Text(title)
                        .font(.title2.weight(.bold))
                        .multilineTextAlignment(.center)
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .contentTransition(.numericText())
                }
                HStack(spacing: 14) {
                    PlayActionButton(title: "Riproduci", systemImage: "play.fill") { start(0, shuffle: false) }
                    PlayActionButton(title: "Casuale", systemImage: "shuffle") { start(0, shuffle: true) }
                }
                .disabled(rows.isEmpty)
                .padding(.top, 4)
                HStack(spacing: 10) {
                    if let saved, let onToggleSave {
                        SmallActionButton(
                            title: saved ? "Salvato" : "Salva",
                            systemImage: saved ? "checkmark" : "plus",
                            active: saved,
                            action: onToggleSave
                        )
                    }
                    SmallActionButton(
                        title: allDownloaded ? "Scaricato" : "Scarica",
                        systemImage: allDownloaded ? "arrow.down.circle.fill" : "arrow.down.circle",
                        active: allDownloaded
                    ) {
                        if !allDownloaded { downloads.downloadAll(rows) }
                    }
                    .disabled(rows.isEmpty)
                    if sortable {
                        Menu {
                            Picker("Ordina per", selection: $sortRaw) {
                                ForEach(SongSort.allCases, id: \.rawValue) { option in
                                    Text(option.label).tag(option.rawValue)
                                }
                            }
                            Divider()
                            Toggle("Ordine inverso", systemImage: "arrow.up.arrow.down", isOn: $descending)
                        } label: {
                            SmallActionLabel(title: sort == .custom ? "Ordina" : sort.label, systemImage: "arrow.up.arrow.down", active: sorted)
                        }
                    }
                }
                .padding(.top, 2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .moveDisabled(true)
            .deleteDisabled(true)

            if loading && rows.isEmpty {
                LoadingView()
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
            }

            ForEach(Array(list.enumerated()), id: \.element.index) { position, entry in
                let song = entry.track
                Button {
                    start(sorted ? position : entry.index, shuffle: false)
                } label: {
                    SongRow(song: song, playing: player.state.track?.id == song.id)
                }
                .buttonStyle(.plain)
                .listRowInsets(EdgeInsets(top: 6, leading: 20, bottom: 6, trailing: 16))
                .listRowBackground(Color.clear)
                .contextMenu {
                    SongMenuItems(song: song, onAddToPlaylist: { NamiRouter.shared.addingToPlaylist = song })
                    if let onRemove {
                        Divider()
                        Button(role: .destructive) {
                            removeRow(at: entry.index, onRemove)
                        } label: {
                            Label("Togli dalla playlist", systemImage: "minus.circle")
                        }
                    }
                }
                .swipeActions(edge: .leading) {
                    Button {
                        LibraryStore.shared.playNext(song)
                    } label: {
                        Label("Dopo", systemImage: "text.line.first.and.arrowtriangle.forward")
                    }
                    .tint(.indigo)
                }
            }
            .onDelete(perform: deleteAction(list))
            .onMove(perform: sorted ? nil : moveAction)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background {
            ZStack(alignment: .top) {
                Color(.systemBackground)
                LinearGradient(
                    colors: [(palette?.top ?? .clear).opacity(scheme == .dark ? 0.8 : 0.4), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 520)
                .animation(.easeInOut(duration: 0.8), value: palette)
            }
            .ignoresSafeArea()
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if onMove != nil, !rows.isEmpty, !sorted {
                ToolbarItem(placement: .topBarTrailing) {
                    EditButton()
                }
            }
        }
        .animation(.snappy, value: sortRaw)
        .animation(.snappy, value: descending)
        .sensoryFeedback(.selection, trigger: sortRaw)
        .onAppear {
            rows = songs
            appeared = true
            updatePalette()
        }
        .onChange(of: songs) { _, newValue in
            withAnimation(.snappy) { rows = newValue }
        }
        .onChange(of: tintURL) { _, _ in updatePalette() }
    }

    private func updatePalette() {
        guard let url = tintURL ?? songs.first?.artworkURL else { return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }

    private func deleteAction(_ list: [(index: Int, track: Track)]) -> ((IndexSet) -> Void)? {
        guard let onRemove else { return nil }
        return { offsets in
            let originals = offsets.compactMap { list.indices.contains($0) ? list[$0].index : nil }
            for index in originals.sorted(by: >) { removeRow(at: index, onRemove) }
        }
    }

    private var moveAction: ((IndexSet, Int) -> Void)? {
        guard let onMove else { return nil }
        return { source, destination in
            guard let from = source.first else { return }
            let to = destination > from ? destination - 1 : destination
            rows.move(fromOffsets: source, toOffset: destination)
            onMove(from, to)
        }
    }

    private func removeRow(at index: Int, _ remove: (Int) -> Void) {
        guard rows.indices.contains(index) else { return }
        _ = withAnimation(.snappy) { rows.remove(at: index) }
        remove(index)
    }
}

/// Pulsantino a capsula sotto Riproduci/Casuale (Salva, Scarica, Ordina).
struct SmallActionButton: View {
    let title: String
    let systemImage: String
    var active = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SmallActionLabel(title: title, systemImage: systemImage, active: active)
        }
        .buttonStyle(.squishy)
        .sensoryFeedback(.success, trigger: active) { old, new in !old && new }
    }
}

struct SmallActionLabel: View {
    let title: String
    let systemImage: String
    var active = false

    var body: some View {
        Label(title, systemImage: systemImage)
            .font(.subheadline.weight(.semibold))
            .contentTransition(.symbolEffect(.replace))
            .foregroundStyle(active ? AnyShapeStyle(.white) : AnyShapeStyle(.tint))
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background {
                Capsule().fill(active ? AnyShapeStyle(.tint) : AnyShapeStyle(.tint.opacity(0.14)))
            }
            .animation(.snappy, value: active)
    }
}

/// Le voci del menu di un brano (tenendo premuto).
struct SongMenuItems: View {
    let song: Track
    let onAddToPlaylist: () -> Void
    @ObservedObject private var library = LibraryStore.shared

    var body: some View {
        Button {
            NamiCatalog_startRadio(song)
        } label: {
            Label("Avvia radio del brano", systemImage: "dot.radiowaves.left.and.right")
        }
        Button {
            library.playNext(song)
        } label: {
            Label("Riproduci dopo", systemImage: "text.line.first.and.arrowtriangle.forward")
        }
        Button {
            library.addToQueue(song)
        } label: {
            Label("Aggiungi in coda", systemImage: "text.line.last.and.arrowtriangle.forward")
        }
        Divider()
        Button(action: onAddToPlaylist) {
            Label("Aggiungi a una playlist", systemImage: "text.badge.plus")
        }
        Button {
            library.toggleLike(song)
        } label: {
            if library.isLiked(song) {
                Label("Togli dai preferiti", systemImage: "heart.slash")
            } else {
                Label("Aggiungi ai preferiti", systemImage: "heart")
            }
        }
        Divider()
        if let artistId = song.artistId {
            Button {
                NamiRouter.shared.open(.artist(key: artistId, title: song.artist))
            } label: {
                Label("Vai all'artista", systemImage: "music.microphone")
            }
        }
        if let albumId = song.albumId {
            Button {
                NamiRouter.shared.open(.album(id: albumId, title: song.albumTitle ?? "Album"))
            } label: {
                Label("Vai all'album", systemImage: "square.stack")
            }
        }
        if let url = URL(string: "https://music.youtube.com/watch?v=" + song.id) {
            ShareLink(item: url, subject: Text(song.title), message: Text("\(song.title) – \(song.artist)")) {
                Label("Condividi", systemImage: "square.and.arrow.up")
            }
        }
        let download = DownloadsStore.shared.info(song)
        if download?.completed == true || download?.running == true {
            Button(role: .destructive) {
                DownloadsStore.shared.remove(song)
            } label: {
                Label(download?.completed == true ? "Rimuovi download" : "Annulla download", systemImage: "xmark.circle")
            }
        } else {
            Button {
                DownloadsStore.shared.download(song)
            } label: {
                Label("Scarica", systemImage: "arrow.down.circle")
            }
        }
    }
}

/// Riproduci / Casuale: capsule di vetro con l'accento (iOS 26), grigie prima.
struct PlayActionButton: View {
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
            .foregroundStyle(.tint)
        } else {
            Button(action: action) {
                Label(title, systemImage: systemImage)
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 34)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
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
                        .foregroundStyle(playing ? AnyShapeStyle(.tint) : AnyShapeStyle(.primary))
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
            DownloadBadge(song: song)
            if playing {
                Image(systemName: "waveform")
                    .foregroundStyle(.tint)
                    .symbolEffect(.variableColor.iterative, options: .repeating)
            }
        }
        .contentShape(Rectangle())
    }
}

// MARK: - Nuova playlist e "Aggiungi a una playlist"

struct NewPlaylistSheet: View {
    var firstSong: Track? = nil
    var onCreated: (() -> Void)? = nil
    @ObservedObject private var library = LibraryStore.shared
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var onYouTube = true
    @FocusState private var focused: Bool

    private var trimmed: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Nome della playlist", text: $name)
                        .focused($focused)
                        .submitLabel(.done)
                        .onSubmit(create)
                }
                if library.isLoggedIn {
                    Section {
                        Toggle("Crea anche su YouTube Music", isOn: $onYouTube)
                    } footer: {
                        Text("Così la trovi anche nelle altre app di YouTube Music.")
                    }
                }
                if let firstSong {
                    Section("Primo brano") {
                        SongRow(song: firstSong)
                    }
                }
            }
            .navigationTitle("Nuova playlist")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annulla") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Crea", action: create)
                        .disabled(trimmed.isEmpty)
                }
            }
            .onAppear { focused = true }
        }
        .presentationDetents([.medium, .large])
    }

    private func create() {
        guard !trimmed.isEmpty else { return }
        library.createPlaylist(name: trimmed, onYouTube: onYouTube, firstSong: firstSong)
        dismiss()
        onCreated?()
    }
}

struct AddToPlaylistSheet: View {
    let song: Track
    @ObservedObject private var library = LibraryStore.shared
    @Environment(\.dismiss) private var dismiss
    @State private var creating = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    SongRow(song: song)
                }
                Section {
                    Button {
                        creating = true
                    } label: {
                        Label("Nuova playlist…", systemImage: "plus")
                            .foregroundStyle(.tint)
                    }
                    ForEach(library.editablePlaylists) { playlist in
                        Button {
                            library.add(song, to: playlist)
                            dismiss()
                        } label: {
                            HStack(spacing: 12) {
                                MosaicArtwork(urls: playlist.thumbnails, cornerRadius: 6)
                                    .frame(width: 44, height: 44)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(playlist.title)
                                        .lineLimit(1)
                                    Text("\(playlist.songCount) brani")
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                } header: {
                    Text("Le tue playlist")
                }
            }
            .navigationTitle("Aggiungi a una playlist")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annulla") { dismiss() }
                }
            }
            .sheet(isPresented: $creating) {
                NewPlaylistSheet(firstSong: song, onCreated: { dismiss() })
            }
        }
        .presentationDetents([.medium, .large])
    }
}
