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
    let artworkURL: URL?
    let largeArtworkURL: URL?
    let duration: TimeInterval
    let explicit: Bool

    init(_ t: NamiTrack) {
        id = t.id
        title = t.title
        artist = t.artist
        artworkURL = t.artworkUrl.flatMap { URL(string: $0) }
        largeArtworkURL = t.largeArtworkUrl.flatMap { URL(string: $0) }
        duration = TimeInterval(t.durationMs) / 1000
        explicit = t.explicit
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
    let thumbnails: [URL]

    init(_ p: NamiPlaylist) {
        id = p.id
        title = p.title
        songCount = Int(p.songCount)
        thumbnails = p.thumbnails.compactMap { URL(string: $0) }
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
    @Published private(set) var position: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    /// Mentre si trascina la barra la posizione non si aggiorna da sola
    var scrubbing = false

    private var subscription: NamiSubscription?
    private var timer: Timer?

    private init() {
        subscription = NamiBridge.shared.observePlayer { [weak self] newState in
            guard let self else { return }
            let snapshot = PlayerSnapshot(newState)
            if snapshot != self.state { self.state = snapshot }
            self.tick()
        }
        let timer = Timer(timeInterval: 0.25, repeats: true) { [weak self] _ in self?.tick() }
        RunLoop.main.add(timer, forMode: .common)
        self.timer = timer
    }

    private func tick() {
        guard !scrubbing else { return }
        let pos = TimeInterval(NamiBridge.shared.positionMs()) / 1000
        if abs(pos - position) > 0.05 { position = pos }
        let d = TimeInterval(NamiBridge.shared.durationMs()) / 1000
        let total = d > 0 ? d : (state.track?.duration ?? 0)
        if total != duration { duration = total }
    }

    func togglePlayPause() { NamiBridge.shared.togglePlayPause() }
    func next() { NamiBridge.shared.next() }
    func previous() { NamiBridge.shared.previous() }
    func toggleLike() { NamiBridge.shared.toggleLike() }
    func toggleShuffle() { NamiBridge.shared.toggleShuffle() }
    func cycleRepeat() { NamiBridge.shared.cycleRepeat() }

    func seek(to seconds: TimeInterval) {
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
