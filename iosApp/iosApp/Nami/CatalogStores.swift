import Foundation
import SwiftUI
import Shared

// Ricerca, artisti, mood, testi, coda, download e statistiche: traducono NamiCatalog (Kotlin)
// in strutture Swift semplici per le schermate.

// MARK: - Modelli

extension HomeSection {
    init(_ s: NamiSection, index: Int) {
        self.init(id: "\(index)-\(s.title)", title: s.title, items: s.items.map { HomeItem($0) }, allSongs: s.allSongs)
    }
}

struct PageData: Equatable {
    let key: String
    let title: String
    let subtitle: String
    let artworkURL: Foundation.URL?
    let description: String?
    let sections: [HomeSection]
    let canPlay: Bool

    init(_ p: NamiPage) {
        key = p.key
        title = p.title
        subtitle = p.subtitle
        artworkURL = p.artworkUrl.flatMap { Foundation.URL(string: $0) }
        description = p.description_?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        sections = p.sections.enumerated().map { HomeSection($1, index: $0) }
        canPlay = p.canPlay
    }
}

struct MoodItem: Identifiable, Hashable {
    let id: String
    let title: String
    let color: Color

    init(_ m: NamiMood) {
        id = m.key
        title = m.title
        let argb = UInt64(bitPattern: m.color)
        color = Color(
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255
        )
    }
}

struct MoodGroup: Identifiable {
    var id: String { title }
    let title: String
    let items: [MoodItem]
}

struct LyricLine: Identifiable, Equatable {
    let id: Int
    let time: Foundation.TimeInterval
    let text: String
}

struct LyricsData: Equatable {
    let songId: String
    let synced: Bool
    let lines: [LyricLine]
    let plain: String?
}

struct QueueSnapshot: Equatable {
    var title: String?
    var tracks: [Track] = []
    var currentIndex = -1
}

struct QueueEntry: Identifiable, Equatable {
    let index: Int
    let track: Track
    var id: Int { index }
}

struct DownloadInfo: Equatable {
    /// 0 in coda, 2 in corso, 3 completato, 4 non riuscito
    let state: Int
    let progress: Double
    var completed: Bool { state == 3 }
    var running: Bool { state == 0 || state == 2 || state == 7 }
}

struct StatRow: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String
    let artworkURL: Foundation.URL?
    let time: Foundation.TimeInterval
    let plays: Int
}

struct StatsData: Equatable {
    let total: Foundation.TimeInterval
    let plays: Int
    let distinctSongs: Int
    let distinctArtists: Int
    let topSongs: [StatRow]
    let topArtists: [StatRow]
    let byHour: [Int]
    let genres: [(String, Double)]

    static func == (lhs: StatsData, rhs: StatsData) -> Bool {
        lhs.total == rhs.total && lhs.plays == rhs.plays && lhs.topSongs == rhs.topSongs && lhs.topArtists == rhs.topArtists
    }
}

extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

// MARK: - Elementi (Home, ricerca, artisti)

enum ItemActions {
    /// Il brano di un elemento, per il menu "tieni premuto".
    static func track(for item: HomeItem) -> Track? {
        guard item.kind == .song else { return nil }
        return NamiCatalog.shared.trackForItem(key: item.id).map { Track($0) }
    }
}

// MARK: - Ricerca

final class SearchStore: ObservableObject {
    static let shared = SearchStore()

    @Published private(set) var history: [String] = []
    @Published private(set) var suggestions: [String] = []
    @Published private(set) var results: [HomeSection] = []
    @Published private(set) var loading = false
    @Published private(set) var error: String?
    @Published private(set) var moods: [MoodGroup] = []
    @Published private(set) var searchedQuery = ""
    @Published var kind = 0

    private var subscription: NamiSubscription?
    private var suggestionToken = 0
    private var searchToken = 0

    static let kinds = ["Migliori risultati", "Brani", "Album", "Artisti", "Playlist", "Playlist utenti", "Video"]

    private init() {
        subscription = NamiCatalog.shared.observeSearchHistory { [weak self] list in
            self?.history = list
        }
    }

    func loadMoodsIfNeeded() {
        guard moods.isEmpty else { return }
        NamiCatalog.shared.loadMoods { [weak self] groups, _ in
            guard let groups else { return }
            self?.moods = groups.map { MoodGroup(title: $0.title, items: $0.items.map { MoodItem($0) }) }
        }
    }

    func updateSuggestions(for text: String) {
        suggestionToken += 1
        let token = suggestionToken
        let query = text.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { suggestions = []; return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) { [weak self] in
            guard let self, token == self.suggestionToken else { return }
            NamiCatalog.shared.searchSuggestions(query: query) { [weak self] list in
                guard let self, token == self.suggestionToken else { return }
                self.suggestions = list
            }
        }
    }

    func search(_ text: String) {
        let query = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }
        NamiCatalog.shared.addSearchHistory(query: query)
        searchedQuery = query
        run()
    }

    func select(kind newKind: Int) {
        guard newKind != kind else { return }
        kind = newKind
        run()
    }

    func reset() {
        searchedQuery = ""
        results = []
        error = nil
        kind = 0
    }

    func clearHistory() { NamiCatalog.shared.clearSearchHistory() }

    private func run() {
        searchToken += 1
        let token = searchToken
        loading = true
        error = nil
        NamiCatalog.shared.search(query: searchedQuery, kind: Int32(kind)) { [weak self] sections, error in
            guard let self, token == self.searchToken else { return }
            self.loading = false
            if let sections {
                withAnimation(.snappy) {
                    self.results = sections.enumerated().map { HomeSection($1, index: $0) }
                }
            } else {
                self.error = error
                self.results = []
            }
        }
    }
}

// MARK: - Pagine (artista, mood)

final class PageStore: ObservableObject {
    enum Kind { case artist, mood }

    @Published private(set) var page: PageData?
    @Published private(set) var loading = true
    @Published private(set) var error: String?
    let key: String
    let kind: Kind

    init(key: String, kind: Kind) {
        self.key = key
        self.kind = kind
        load()
    }

    func load() {
        loading = true
        error = nil
        let done: (NamiPage?, String?) -> Void = { [weak self] page, error in
            guard let self else { return }
            withAnimation(.smooth) {
                self.loading = false
                if let page { self.page = PageData(page) } else { self.error = error }
            }
        }
        switch kind {
        case .artist: NamiCatalog.shared.loadArtist(key: key, callback: done)
        case .mood: NamiCatalog.shared.loadMood(key: key, callback: done)
        }
    }

    func playArtist(shuffle: Bool) { NamiCatalog.shared.playArtist(key: key, shuffle: shuffle) }
}

// MARK: - Testi

final class LyricsStore: ObservableObject {
    static let shared = LyricsStore()

    @Published private(set) var lyrics: LyricsData?
    @Published private(set) var loading = false
    private var loadedId: String?

    func load(for songId: String?) {
        guard let songId, songId != loadedId else { return }
        loadedId = songId
        loading = true
        lyrics = nil
        NamiCatalog.shared.loadLyrics(songId: songId) { [weak self] data in
            guard let self, self.loadedId == songId else { return }
            self.loading = false
            guard let data else { self.lyrics = nil; return }
            self.lyrics = LyricsData(
                songId: data.songId,
                synced: data.synced,
                lines: data.lines.enumerated().map { LyricLine(id: $0, time: Foundation.TimeInterval($1.timeMs) / 1000, text: $1.text) },
                plain: data.plain
            )
        }
    }
}

// MARK: - Coda

final class QueueStore: ObservableObject {
    static let shared = QueueStore()

    @Published private(set) var queue = QueueSnapshot()
    private var subscription: NamiSubscription?

    private init() {
        subscription = NamiCatalog.shared.observeQueue { [weak self] q in
            let snapshot = QueueSnapshot(title: q.title, tracks: q.tracks.map { Track($0) }, currentIndex: Int(q.currentIndex))
            if snapshot != self?.queue { self?.queue = snapshot }
        }
    }

    var upNext: [QueueEntry] {
        queue.tracks.enumerated()
            .filter { $0.offset > queue.currentIndex }
            .map { QueueEntry(index: $0.offset, track: $0.element) }
    }

    func play(at index: Int) { NamiCatalog.shared.playQueueItem(index: Int32(index)) }
    func remove(at index: Int) { NamiCatalog.shared.removeFromQueue(index: Int32(index)) }
    func move(from: Int, to: Int) { NamiCatalog.shared.moveInQueue(from: Int32(from), to: Int32(to)) }
    func clearUpNext() { NamiCatalog.shared.clearUpNext() }
}

// MARK: - Download

final class DownloadsStore: ObservableObject {
    static let shared = DownloadsStore()

    @Published private(set) var states: [String: DownloadInfo] = [:]
    @Published private(set) var songs: [Track] = []
    private var subscriptions: [NamiSubscription] = []

    private init() {
        subscriptions.append(NamiCatalog.shared.observeDownloads { [weak self] list in
            var map: [String: DownloadInfo] = [:]
            for d in list { map[d.id] = DownloadInfo(state: Int(d.state), progress: Double(d.progress)) }
            if map != self?.states { self?.states = map }
        })
        subscriptions.append(NamiCatalog.shared.observeDownloadedSongs { [weak self] list in
            self?.songs = list.map { Track($0) }
        })
    }

    func info(_ song: Track) -> DownloadInfo? { states[song.id] }
    func download(_ song: Track) { NamiCatalog.shared.download(songId: song.id) }
    func downloadAll(_ songs: [Track]) { NamiCatalog.shared.downloadAll(songIds: songs.map(\.id)) }
    func remove(_ song: Track) { NamiCatalog.shared.removeDownload(songId: song.id) }

    func play(from index: Int, shuffle: Bool) {
        NamiCatalog.shared.playSongs(songIds: songs.map(\.id), title: "Scaricati", startIndex: Int32(index), shuffle: shuffle)
    }
}

// MARK: - Statistiche

final class StatsStore: ObservableObject {
    static let shared = StatsStore()

    @Published private(set) var stats: StatsData?
    @Published private(set) var loading = false
    @Published var period = 1 { didSet { if period != oldValue { load() } } }
    private var token = 0

    static let periods = ["7 giorni", "30 giorni", "Anno", "Sempre"]

    func load() {
        token += 1
        let current = token
        loading = true
        NamiCatalog.shared.loadStats(period: Int32(period)) { [weak self] s in
            guard let self, current == self.token else { return }
            self.loading = false
            guard let s else { return }
            let row: (NamiStatRow) -> StatRow = { r in
                StatRow(
                    id: r.key.isEmpty ? r.title : r.key,
                    title: r.title,
                    subtitle: r.subtitle,
                    artworkURL: r.artworkUrl.flatMap { Foundation.URL(string: $0) },
                    time: Foundation.TimeInterval(r.timeMs) / 1000,
                    plays: Int(r.plays)
                )
            }
            withAnimation(.smooth) {
                self.stats = StatsData(
                    total: Foundation.TimeInterval(s.totalMs) / 1000,
                    plays: Int(s.plays),
                    distinctSongs: Int(s.distinctSongs),
                    distinctArtists: Int(s.distinctArtists),
                    topSongs: s.topSongs.map(row),
                    topArtists: s.topArtists.map(row),
                    byHour: s.byHourMinutes.map { $0.intValue },
                    genres: s.genres.map { ($0.title, Double($0.share)) }
                )
            }
        }
    }

    /// I brani più ascoltati, da far partire.
    func playTopSongs(from index: Int) {
        guard let stats else { return }
        NamiCatalog.shared.playSongs(songIds: stats.topSongs.map(\.id), title: "I più ascoltati", startIndex: Int32(index), shuffle: false)
    }
}
