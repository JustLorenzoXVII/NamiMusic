import Foundation
import SwiftUI
import Shared

// L'unico file dell'interfaccia SwiftUI che vede le classi Kotlin (import Shared).
// Le traduce in strutture Swift semplici, così le schermate non hanno conflitti di nomi
// (il framework Kotlin esporta anche classi come "View" o "Color").

// MARK: - Modelli

struct Track: Identifiable, Equatable, Hashable {
    let id: String
    let title: String
    let artist: String
    let artworkURL: Foundation.URL?
    let largeArtworkURL: Foundation.URL?
    let duration: Foundation.TimeInterval
    let explicit: Bool

    init(_ t: NamiTrack) {
        id = t.id
        title = t.title
        artist = t.artist
        artworkURL = t.artworkUrl.flatMap { Foundation.URL(string: $0) }
        largeArtworkURL = t.largeArtworkUrl.flatMap { Foundation.URL(string: $0) }
        duration = Foundation.TimeInterval(t.durationMs) / 1000
        explicit = t.isExplicit
    }
}

struct PlayerSnapshot: Equatable {
    var track: Track?
    var isPlaying = false
    var liked = false
    var shuffle = false
    /// 0 = niente, 1 = ripeti brano, 2 = ripeti tutto
    var repeatMode = 0
    var queueTitle: String?
    var canSkipPrevious = true
    var canSkipNext = true

    static let empty = PlayerSnapshot()

    init() {}

    init(_ s: NamiPlayerState) {
        track = s.track.map { Track($0) }
        isPlaying = s.isPlaying
        liked = s.liked
        shuffle = s.shuffle
        repeatMode = Int(s.repeatMode)
        queueTitle = s.queueTitle
        canSkipPrevious = s.canSkipPrevious
        canSkipNext = s.canSkipNext
    }
}

struct PlaylistItem: Identifiable, Equatable, Hashable {
    let id: String
    let title: String
    let songCount: Int
    let thumbnails: [Foundation.URL]

    init(_ p: NamiPlaylist) {
        id = p.id
        title = p.title
        songCount = Int(p.songCount)
        thumbnails = p.thumbnails.compactMap { Foundation.URL(string: $0) }
    }
}

// MARK: - App

/// Avvio del motore Kotlin e scelta dell'interfaccia (SwiftUI o quella di sempre).
final class NamiApp: ObservableObject {
    static let shared = NamiApp()

    /// true = interfaccia SwiftUI (interruttore "Tema iOS" acceso)
    @Published private(set) var iosUI: Bool
    private var subscription: NamiSubscription?

    private init() {
        NamiBridge.shared.start()
        iosUI = NamiBridge.shared.isIosUiEnabled()
        subscription = NamiBridge.shared.observeIosUi { [weak self] value in
            let enabled = value.boolValue
            if self?.iosUI != enabled {
                withAnimation(.easeInOut(duration: 0.25)) { self?.iosUI = enabled }
            }
        }
    }

    func setIosUI(_ enabled: Bool) {
        NamiBridge.shared.setIosUiEnabled(enabled: enabled)
    }
}

// MARK: - Player

final class PlayerStore: ObservableObject {
    static let shared = PlayerStore()

    @Published private(set) var state = PlayerSnapshot.empty
    @Published private(set) var position: Foundation.TimeInterval = 0
    @Published private(set) var duration: Foundation.TimeInterval = 0
    /// Mentre si trascina la barra la posizione non si aggiorna da sola
    var scrubbing = false

    private var subscription: NamiSubscription?
    private var timer: Foundation.Timer?

    private init() {
        subscription = NamiBridge.shared.observePlayer { [weak self] newState in
            guard let self else { return }
            let snapshot = PlayerSnapshot(newState)
            if snapshot != self.state { self.state = snapshot }
            self.tick()
        }
        let timer = Foundation.Timer(timeInterval: 0.25, repeats: true) { [weak self] _ in self?.tick() }
        Foundation.RunLoop.main.add(timer, forMode: .common)
        self.timer = timer
    }

    private func tick() {
        guard !scrubbing else { return }
        let pos = Foundation.TimeInterval(NamiBridge.shared.positionMs()) / 1000
        if abs(pos - position) > 0.05 { position = pos }
        let d = Foundation.TimeInterval(NamiBridge.shared.durationMs()) / 1000
        let total = d > 0 ? d : (state.track?.duration ?? 0)
        if total != duration { duration = total }
    }

    func togglePlayPause() { NamiBridge.shared.togglePlayPause() }
    func next() { NamiBridge.shared.next() }
    func previous() { NamiBridge.shared.previous() }
    func toggleLike() { NamiBridge.shared.toggleLike() }
    func toggleShuffle() { NamiBridge.shared.toggleShuffle() }
    func cycleRepeat() { NamiBridge.shared.cycleRepeat() }

    func seek(to seconds: Foundation.TimeInterval) {
        position = seconds
        NamiBridge.shared.seekTo(positionMs: Int64(seconds * 1000))
    }
}

// MARK: - Libreria

final class LibraryStore: ObservableObject {
    static let shared = LibraryStore()

    @Published private(set) var liked: [Track] = []
    @Published private(set) var playlists: [PlaylistItem] = []
    private var subscriptions: [NamiSubscription] = []

    private init() {
        subscriptions.append(NamiBridge.shared.observeLikedSongs { [weak self] songs in
            self?.liked = songs.map { Track($0) }
        })
        subscriptions.append(NamiBridge.shared.observePlaylists { [weak self] list in
            self?.playlists = list.map { PlaylistItem($0) }
        })
    }

    func playLiked(from index: Int = 0, shuffle: Bool = false) {
        NamiBridge.shared.playLikedSongs(startIndex: Int32(index), shuffle: shuffle)
    }

    func playPlaylist(_ playlist: PlaylistItem, from index: Int = 0, shuffle: Bool = false) {
        NamiBridge.shared.playPlaylist(playlistId: playlist.id, title: playlist.title, startIndex: Int32(index), shuffle: shuffle)
    }
}

/// I brani di una playlist, osservati finché la pagina è aperta.
final class PlaylistSongsStore: ObservableObject {
    @Published private(set) var songs: [Track] = []
    @Published private(set) var loaded = false
    private var subscription: NamiSubscription?

    init(playlistId: String) {
        subscription = NamiBridge.shared.observePlaylistSongs(playlistId: playlistId) { [weak self] list in
            self?.songs = list.map { Track($0) }
            self?.loaded = true
        }
    }

    deinit { subscription?.cancel() }
}

// MARK: - Home

struct HomeItem: Identifiable, Equatable, Hashable {
    enum Kind: Int { case song = 0, album, playlist, artist, other }

    let id: String
    let kind: Kind
    let title: String
    let subtitle: String
    let artworkURL: Foundation.URL?
    let explicit: Bool

    init(_ i: NamiItem) {
        id = i.key
        kind = Kind(rawValue: Int(i.kind)) ?? .other
        title = i.title
        subtitle = i.subtitle
        artworkURL = i.artworkUrl.flatMap { Foundation.URL(string: $0) }
        explicit = i.isExplicit
    }
}

struct HomeSection: Identifiable, Equatable {
    let id: String
    let title: String
    let items: [HomeItem]
    let allSongs: Bool
}

struct HomeChip: Identifiable, Equatable, Hashable {
    var id: String { title }
    let title: String
    let params: String?
}

struct MixItem: Identifiable, Equatable, Hashable {
    let id: String
    let title: String
    let subtitle: String
    let covers: [Foundation.URL]
    let songCount: Int
    let kind: Int

    init(_ m: NamiMix) {
        id = m.id
        title = m.title
        subtitle = m.subtitle
        covers = m.covers.compactMap { Foundation.URL(string: $0) }
        songCount = Int(m.songCount)
        kind = Int(m.kind)
    }
}

final class HomeStore: ObservableObject {
    static let shared = HomeStore()

    @Published private(set) var chips: [HomeChip] = []
    @Published private(set) var sections: [HomeSection] = []
    @Published private(set) var selectedChip: String?
    @Published private(set) var loading = false
    @Published private(set) var error: String?
    @Published private(set) var recent: [Track] = []
    @Published private(set) var mixes: [MixItem] = []
    private var subscriptions: [NamiSubscription] = []
    private var loadedOnce = false

    private init() {
        subscriptions.append(NamiBridge.shared.observeRecent { [weak self] songs in
            self?.recent = songs.map { Track($0) }
        })
        subscriptions.append(NamiBridge.shared.observeMixes { [weak self] list in
            self?.mixes = list.map { MixItem($0) }
        })
    }

    func loadIfNeeded() {
        guard !loadedOnce else { return }
        loadedOnce = true
        load()
    }

    func select(chip: HomeChip) {
        selectedChip = selectedChip == chip.params ? nil : chip.params
        load()
    }

    func load(completion: (() -> Void)? = nil) {
        loading = true
        NamiBridge.shared.loadHome(params: selectedChip) { [weak self] home, error in
            guard let self else { return }
            self.loading = false
            if let home {
                self.error = nil
                if !home.chips.isEmpty || self.chips.isEmpty {
                    self.chips = home.chips.map { HomeChip(title: $0.title, params: $0.params) }
                }
                self.sections = home.sections.enumerated().map { index, section in
                    HomeSection(id: "\(index)-\(section.title)", title: section.title, items: section.items.map { HomeItem($0) }, allSongs: section.allSongs)
                }
            } else {
                self.error = error
            }
            completion?()
        }
    }

    func reload() async {
        await withCheckedContinuation { continuation in
            load { continuation.resume() }
        }
    }

    func play(_ item: HomeItem) { NamiBridge.shared.playItem(key: item.id) }
    func playRecent(at index: Int) { NamiBridge.shared.playRecent(index: Int32(index)) }
    func refreshMixes() { NamiBridge.shared.refreshMixes() }
    func mixTracks(_ mix: MixItem) -> [Track] { NamiBridge.shared.mixTracks(id: mix.id).map { Track($0) } }
    func playMix(_ mix: MixItem, from index: Int = 0, shuffle: Bool = false) {
        NamiBridge.shared.playMix(id: mix.id, startIndex: Int32(index), shuffle: shuffle)
    }
}

/// Album o playlist di YouTube Music aperti dalla Home.
final class CollectionStore: ObservableObject {
    @Published private(set) var title = ""
    @Published private(set) var subtitle = ""
    @Published private(set) var artworkURL: Foundation.URL?
    @Published private(set) var tracks: [Track] = []
    @Published private(set) var loading = true
    @Published private(set) var error: String?
    let key: String

    init(key: String) {
        self.key = key
        load()
    }

    func load() {
        loading = true
        error = nil
        NamiBridge.shared.loadCollection(key: key) { [weak self] collection, error in
            guard let self else { return }
            self.loading = false
            if let collection {
                self.title = collection.title
                self.subtitle = collection.subtitle
                self.artworkURL = collection.artworkUrl.flatMap { Foundation.URL(string: $0) }
                self.tracks = collection.tracks.map { Track($0) }
            } else {
                self.error = error
            }
        }
    }

    func play(from index: Int = 0, shuffle: Bool = false) {
        NamiBridge.shared.playCollection(key: key, startIndex: Int32(index), shuffle: shuffle)
    }
}
