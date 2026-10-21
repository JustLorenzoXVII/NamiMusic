import SwiftUI

// L'app Nami in SwiftUI: schede native di iOS 26 (Liquid Glass), mini player come accessorio
// della barra e player a schermo intero che si apre con lo zoom di sistema (come Apple Music).

enum NamiTab: Hashable {
    case home, stats, library, search
}

struct NamiRootView: View {
    @StateObject private var player = PlayerStore.shared
    @State private var tab: NamiTab = .home
    @State private var showPlayer = false
    @State private var showSettings = false
    @Namespace private var playerNamespace

    var body: some View {
        Group {
            if #available(iOS 26.0, *) {
                modernTabs
            } else {
                legacyTabs
            }
        }
        .tint(.namiAccent)
        .fullScreenCover(isPresented: $showPlayer) {
            PlayerView()
                .modifier(ZoomFromMiniPlayer(namespace: playerNamespace))
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }

    // MARK: iOS 26

    @available(iOS 26.0, *)
    private var modernTabs: some View {
        TabView(selection: $tab) {
            Tab("Home", systemImage: "house.fill", value: NamiTab.home) {
                NavigationStack { HomeView(onSettings: { showSettings = true }) }
            }
            Tab("Statistiche", systemImage: "chart.bar.fill", value: NamiTab.stats) {
                NavigationStack { StatsView(onSettings: { showSettings = true }) }
            }
            Tab("Libreria", systemImage: "square.stack.fill", value: NamiTab.library) {
                NavigationStack { LibraryView(onSettings: { showSettings = true }) }
            }
            Tab(value: NamiTab.search, role: .search) {
                NavigationStack { SearchView() }
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
        .tabViewBottomAccessory {
            MiniPlayerView(onOpen: { showPlayer = true })
                .matchedTransitionSource(id: "player", in: playerNamespace)
        }
    }

    // MARK: iOS 18

    private var legacyTabs: some View {
        TabView(selection: $tab) {
            NavigationStack { HomeView(onSettings: { showSettings = true }) }
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(NamiTab.home)
            NavigationStack { StatsView(onSettings: { showSettings = true }) }
                .tabItem { Label("Statistiche", systemImage: "chart.bar.fill") }
                .tag(NamiTab.stats)
            NavigationStack { LibraryView(onSettings: { showSettings = true }) }
                .tabItem { Label("Libreria", systemImage: "square.stack.fill") }
                .tag(NamiTab.library)
            NavigationStack { SearchView() }
                .tabItem { Label("Cerca", systemImage: "magnifyingglass") }
                .tag(NamiTab.search)
        }
        .safeAreaInset(edge: .bottom) {
            if player.state.track != nil {
                MiniPlayerView(onOpen: { showPlayer = true })
                    .frame(height: 56)
                    .background(.ultraThinMaterial, in: Capsule())
                    .padding(.horizontal, 12)
                    .padding(.bottom, 54)
                    .matchedTransitionSource(id: "player", in: playerNamespace)
            }
        }
    }
}

/// Il player si apre "zoomando" fuori dal mini player e si richiude dentro di lui.
private struct ZoomFromMiniPlayer: ViewModifier {
    let namespace: Namespace.ID

    func body(content: Content) -> some View {
        content.navigationTransition(.zoom(sourceID: "player", in: namespace))
    }
}

// MARK: - Mini player

struct MiniPlayerView: View {
    @ObservedObject private var player = PlayerStore.shared
    let onOpen: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            ArtworkView(url: player.state.track?.artworkURL, size: 32, cornerRadius: 6)
            VStack(alignment: .leading, spacing: 0) {
                Text(player.state.track?.title ?? "Niente in riproduzione")
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                if let artist = player.state.track?.artist, !artist.isEmpty {
                    Text(artist)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: player.togglePlayPause) {
                Image(systemName: player.state.isPlaying ? "pause.fill" : "play.fill")
                    .font(.title3)
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .disabled(player.state.track == nil)
            Button(action: player.next) {
                Image(systemName: "forward.fill")
                    .font(.title3)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .disabled(player.state.track == nil)
        }
        .buttonStyle(.plain)
        .foregroundStyle(.primary)
        .padding(.leading, 8)
        .padding(.trailing, 10)
        .contentShape(Rectangle())
        .onTapGesture { if player.state.track != nil { onOpen() } }
    }
}

// MARK: - Schede ancora in arrivo

/// Pulsante impostazioni in alto a destra (vetro su iOS 26).
struct SettingsToolbar: ToolbarContent {
    let onSettings: () -> Void

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button(action: onSettings) {
                Image(systemName: "gearshape")
            }
            .accessibilityLabel("Impostazioni")
        }
    }
}

struct ComingSoonView: View {
    let title: String
    let systemImage: String
    let text: String
    @ObservedObject private var app = NamiApp.shared

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            Text(text)
        } actions: {
            Button("Usa l'interfaccia completa") { app.setIosUI(false) }
                .buttonStyle(.borderedProminent)
        }
    }
}

struct HomeView: View {
    let onSettings: () -> Void

    var body: some View {
        ComingSoonView(
            title: "Home in arrivo",
            systemImage: "house",
            text: "La Home in SwiftUI arriva nella prossima versione. Intanto trovi preferiti e playlist in Libreria."
        )
        .navigationTitle("Home")
        .toolbar { SettingsToolbar(onSettings: onSettings) }
    }
}

struct StatsView: View {
    let onSettings: () -> Void

    var body: some View {
        ComingSoonView(
            title: "Statistiche in arrivo",
            systemImage: "chart.bar",
            text: "Le statistiche in SwiftUI arrivano in una delle prossime versioni."
        )
        .navigationTitle("Statistiche")
        .toolbar { SettingsToolbar(onSettings: onSettings) }
    }
}

struct SearchView: View {
    @State private var query = ""

    var body: some View {
        ComingSoonView(
            title: "Ricerca in arrivo",
            systemImage: "magnifyingglass",
            text: "La ricerca in SwiftUI arriva in una delle prossime versioni."
        )
        .navigationTitle("Cerca")
        .searchable(text: $query, prompt: "Artisti, brani, playlist")
    }
}
