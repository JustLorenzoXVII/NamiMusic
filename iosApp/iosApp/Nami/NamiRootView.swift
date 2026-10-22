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
    @ObservedObject private var router = NamiRouter.shared
    @Namespace private var playerNamespace

    var body: some View {
        Group {
            if #available(iOS 26.0, *) {
                modernTabs
            } else {
                legacyTabs
            }
        }
        .namiTint()
        .onChange(of: tab) { _, newTab in router.activeTab = newTab }
        .onChange(of: router.pending) { _, route in
            // Dal player: prima si chiude, poi si apre la pagina
            if route != nil, showPlayer { showPlayer = false }
        }
        .onAppear { AccentStore.shared.update(for: player.state.track?.artworkURL) }
        .onChange(of: player.state.track?.artworkURL) { _, url in AccentStore.shared.update(for: url) }
        .fullScreenCover(isPresented: $showPlayer) {
            PlayerView()
                .modifier(ZoomFromMiniPlayer(namespace: playerNamespace))
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
                .namiTint()
        }
        .sheet(item: $router.addingToPlaylist) { song in
            AddToPlaylistSheet(song: song)
                .namiTint()
        }
    }

    // MARK: iOS 26

    @available(iOS 26.0, *)
    private var modernTabs: some View {
        TabView(selection: $tab) {
            Tab("Home", systemImage: "house.fill", value: NamiTab.home) {
                ZoomingStack(tab: .home) { HomeView(onSettings: { showSettings = true }) }
            }
            Tab("Statistiche", systemImage: "chart.bar.fill", value: NamiTab.stats) {
                ZoomingStack(tab: .stats) { StatsView(onSettings: { showSettings = true }) }
            }
            Tab("Libreria", systemImage: "square.stack.fill", value: NamiTab.library) {
                ZoomingStack(tab: .library) { LibraryView(onSettings: { showSettings = true }) }
            }
            Tab(value: NamiTab.search, role: .search) {
                ZoomingStack(tab: .search) { SearchView() }
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
            ZoomingStack(tab: .home) { HomeView(onSettings: { showSettings = true }) }
                .tabItem { Label("Home", systemImage: "house.fill") }
                .tag(NamiTab.home)
            ZoomingStack(tab: .stats) { StatsView(onSettings: { showSettings = true }) }
                .tabItem { Label("Statistiche", systemImage: "chart.bar.fill") }
                .tag(NamiTab.stats)
            ZoomingStack(tab: .library) { LibraryView(onSettings: { showSettings = true }) }
                .tabItem { Label("Libreria", systemImage: "square.stack.fill") }
                .tag(NamiTab.library)
            ZoomingStack(tab: .search) { SearchView() }
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
        HStack(spacing: 12) {
            ArtworkView(url: player.state.track?.artworkURL, size: 34, cornerRadius: 7)
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
        .padding(.leading, 14)
        .padding(.trailing, 12)
        .contentShape(Rectangle())
        .onTapGesture { if player.state.track != nil { onOpen() } }
    }
}

// MARK: - Schede ancora in arrivo

/// In alto a destra: il tuo account (iniziale colorata) che apre account e impostazioni,
/// come il profilo di Apple Music.
struct SettingsToolbar: ToolbarContent {
    let onSettings: () -> Void

    private var avatarButton: some View {
        Button(action: onSettings) {
            AccountAvatar(size: 36)
                .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
        }
        .buttonStyle(.squishy)
        .accessibilityLabel("Account e impostazioni")
    }

    var body: some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarItem(placement: .topBarTrailing) {
                avatarButton
            }
            .sharedBackgroundVisibility(.hidden)
        } else {
            ToolbarItem(placement: .topBarTrailing) {
                avatarButton
            }
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

