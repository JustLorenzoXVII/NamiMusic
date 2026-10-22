import SwiftUI
import AVKit
import MediaPlayer

// Il player a schermo intero, in tre stili (scelti nelle impostazioni):
// - Classico: come Apple Music, copertina grande e comandi sotto.
// - Moduli: comandi a moduli di vetro come il Centro di Controllo, volume verticale.
// - Edizione: chiaro, "da rivista", con il titolo con le grazie e il colore della carta dalla copertina.
// Con testo o coda aperti la copertina (una sola, che scivola) va in alto e i comandi stanno
// in un pannello di vetro in basso; nello stile Classico restano quelli di sempre.

enum PlayerPanel: Equatable {
    case none, lyrics, queue
}

enum PlayerStyle: String, CaseIterable, Identifiable {
    case classic, modules, edition
    var id: String { rawValue }

    var label: String {
        switch self {
        case .classic: return L("Classico")
        case .modules: return L("Moduli")
        case .edition: return L("Edizione")
        }
    }

    var detail: String {
        switch self {
        case .classic: return L("Copertina grande e comandi sotto, come Apple Music.")
        case .modules: return L("Comandi a moduli di vetro come il Centro di Controllo, testo protagonista.")
        case .edition: return L("Chiaro, da rivista: carta colorata dalla copertina e titolo con le grazie.")
        }
    }
}

// MARK: - Colori del player (bianco sul vetro scuro, inchiostro sulla carta)

private struct PlayerInkKey: EnvironmentKey { static let defaultValue: Color = .white }
private struct PlayerPaperKey: EnvironmentKey { static let defaultValue: Color = Color.black.opacity(0.8) }

extension EnvironmentValues {
    var playerInk: Color {
        get { self[PlayerInkKey.self] }
        set { self[PlayerInkKey.self] = newValue }
    }
    var playerPaper: Color {
        get { self[PlayerPaperKey.self] }
        set { self[PlayerPaperKey.self] = newValue }
    }
}

struct PlayerView: View {
    @ObservedObject private var player = PlayerStore.shared
    @ObservedObject private var queue = QueueStore.shared
    @Environment(\.dismiss) private var dismiss
    @AppStorage("nami.playerStyle") private var styleRaw = PlayerStyle.classic.rawValue
    @State private var palette = ArtworkPalette.fallback
    @State private var scrubValue: TimeInterval?
    @State private var addingToPlaylist: Track?
    @State private var panel: PlayerPanel = .none
    /// Dove sta la copertina grande e dove quella piccola (testo/coda): la copertina è una sola
    /// e scivola da un posto all'altro, così l'animazione è continua
    @State private var bigRect: CGRect = .zero
    @State private var smallRect: CGRect = .zero

    private var track: Track? { player.state.track }
    private var style: PlayerStyle { PlayerStyle(rawValue: styleRaw) ?? .classic }
    private var ink: Color { style == .edition ? palette.ink : .white }
    private var paper: Color { style == .edition ? palette.paper : Color.black.opacity(0.8) }

    var body: some View {
        GeometryReader { geo in
            ZStack {
                background
                Group {
                    if panel != .none {
                        panelLayout(geo)
                            .transition(.asymmetric(
                                insertion: .opacity.combined(with: .offset(y: 24)),
                                removal: .opacity.combined(with: .offset(y: 24))
                            ))
                    } else {
                        Group {
                            switch style {
                            case .classic: classicLayout(geo)
                            case .modules: modulesLayout(geo)
                            case .edition: editionLayout(geo)
                            }
                        }
                        .transition(.asymmetric(
                            insertion: .opacity.combined(with: .scale(scale: 0.97, anchor: .bottom)),
                            removal: .opacity.combined(with: .scale(scale: 0.97, anchor: .bottom))
                        ))
                    }
                }
                .padding(.top, geo.safeAreaInsets.top)
                floatingArtwork
            }
            .coordinateSpace(name: "player")
            .ignoresSafeArea()
        }
        .foregroundStyle(ink)
        .environment(\.playerInk, ink)
        .environment(\.playerPaper, paper)
        .environment(\.colorScheme, style == .edition ? .light : .dark)
        .animation(.easeInOut(duration: 0.5), value: styleRaw)
        .onAppear { updatePalette() }
        .onChange(of: track?.largeArtworkURL) { _, _ in updatePalette() }
        .statusBarHidden(false)
        .sheet(item: $addingToPlaylist) { song in
            AddToPlaylistSheet(song: song)
                .namiTint()
        }
        .sensoryFeedback(.selection, trigger: panel)
        .toastHost()
    }

    // MARK: Copertina unica che scivola

    /// Prima misura senza animazione (niente copertina che vola dall'angolo in alto).
    private func set(_ rect: Binding<CGRect>, _ value: CGRect) {
        guard rect.wrappedValue != value else { return }
        if rect.wrappedValue == .zero {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { rect.wrappedValue = value }
        } else {
            rect.wrappedValue = value
        }
    }

    /// Posto della copertina grande nel layout (la copertina vera la disegna floatingArtwork).
    private func artworkSlot(width: CGFloat, height: CGFloat) -> some View {
        Color.clear
            .frame(width: width, height: height)
            .onGeometryChange(for: CGRect.self) { $0.frame(in: .named("player")) } action: { set($bigRect, $0) }
    }

    private var floatingArtwork: some View {
        let compact = panel != .none
        let rect = compact && smallRect != .zero ? smallRect : bigRect
        let paused = !compact && !player.state.isPlaying
        let radius: CGFloat = compact ? 8 : (style == .modules ? 22 : style == .edition ? 6 : 12)
        let pausedScale: CGFloat = style == .classic ? 0.82 : style == .modules ? 0.9 : 0.95
        return ArtworkView(url: track?.largeArtworkURL, cornerRadius: radius)
            .frame(width: rect.width, height: rect.height)
            .shadow(color: (style == .edition ? palette.ink : .black).opacity(compact ? 0.25 : 0.4), radius: compact ? 10 : 26, y: compact ? 5 : 16)
            .scaleEffect(paused ? pausedScale : 1)
            .contentShape(Rectangle())
            .onTapGesture { if compact { toggle(panel) } }
            .position(x: rect.midX, y: rect.midY)
            .opacity(rect == .zero ? 0 : 1)
            .animation(.spring(response: 0.5, dampingFraction: 0.86), value: rect)
            .animation(.spring(response: 0.5, dampingFraction: 0.68), value: paused)
            .animation(.spring(response: 0.5, dampingFraction: 0.86), value: radius)
            .allowsHitTesting(compact)
    }

    private func toggle(_ target: PlayerPanel) {
        withAnimation(.spring(response: 0.5, dampingFraction: 0.86)) {
            panel = panel == target ? .none : target
        }
    }

    // MARK: Sfondo

    private var background: some View {
        ZStack {
            if style == .edition {
                palette.paper
                RadialGradient(colors: [palette.accent.opacity(0.28), .clear], center: .topTrailing, startRadius: 10, endRadius: 420)
                RadialGradient(colors: [palette.top.opacity(0.18), .clear], center: .bottomLeading, startRadius: 10, endRadius: 380)
            } else {
                LinearGradient(colors: [palette.top, palette.bottom], startPoint: .top, endPoint: .bottom)
                RemoteImage(url: track?.largeArtworkURL, placeholder: .clear)
                    .blur(radius: style == .modules ? 50 : 80, opaque: true)
                    .opacity(style == .modules ? 0.75 : 0.45)
                    .scaleEffect(1.4)
                LinearGradient(colors: [.clear, .black.opacity(style == .modules ? 0.2 : 0.35)], startPoint: .center, endPoint: .bottom)
            }
        }
        .animation(.easeInOut(duration: 0.6), value: palette)
        .ignoresSafeArea()
    }

    private var grabber: some View {
        Capsule()
            .fill(ink.opacity(0.45))
            .frame(width: 38, height: 5)
            .padding(.top, 10)
            .padding(.bottom, 4)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
            .onTapGesture { dismiss() }
            .accessibilityLabel(L("Chiudi"))
    }

    // MARK: Stile Classico

    private func classicLayout(_ geo: GeometryProxy) -> some View {
        let artSide = min(geo.size.width - 56, geo.size.height * 0.42)
        return VStack(spacing: 0) {
            grabber
            Spacer(minLength: 12)
            artworkSlot(width: artSide, height: artSide)
                .frame(maxWidth: .infinity)
            Spacer(minLength: 20)
            VStack(spacing: 22) {
                titleRow
                progress
                classicControls
                VolumeSlider()
                    .frame(height: 34)
                bottomRow
            }
            .padding(.horizontal, 28)
            .padding(.bottom, max(geo.safeAreaInsets.bottom, 16))
        }
    }

    private var titleRow: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(track?.title ?? "")
                        .font(.title3.weight(.semibold))
                        .lineLimit(1)
                    if track?.explicit == true {
                        Image(systemName: "e.square.fill")
                            .font(.footnote)
                            .foregroundStyle(ink.opacity(0.6))
                    }
                }
                Text(track?.artist ?? "")
                    .font(.title3)
                    .foregroundStyle(ink.opacity(0.62))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            heartButton
            moreMenu
        }
    }

    private var heartButton: some View {
        GlassIconButton(systemName: player.state.liked ? "heart.fill" : "heart", action: player.toggleLike)
            .symbolEffect(.bounce, value: player.state.liked)
            .sensoryFeedback(.impact(weight: .medium), trigger: player.state.liked)
    }

    private var classicControls: some View {
        HStack {
            Spacer()
            ControlButton(systemName: "backward.fill", size: 34, action: player.previous)
            Spacer()
            ControlButton(systemName: player.state.isPlaying ? "pause.fill" : "play.fill", size: 50, action: player.togglePlayPause)
            Spacer()
            ControlButton(systemName: "forward.fill", size: 34, action: player.next)
            Spacer()
        }
    }

    private var bottomRow: some View {
        HStack {
            PanelToggle(systemName: "quote.bubble", active: panel == .lyrics) { toggle(.lyrics) }
            Spacer()
            AirPlayGlassButton(tint: .white)
            Spacer()
            PanelToggle(systemName: "list.bullet", active: panel == .queue) { toggle(.queue) }
        }
        .padding(.horizontal, 24)
    }

    // MARK: Stile Moduli

    private func modulesLayout(_ geo: GeometryProxy) -> some View {
        let width = geo.size.width - 48
        let third = (width - 24) / 3
        let artSide = min(width - 80, geo.size.height * 0.29)
        return VStack(spacing: 0) {
            grabber
            Spacer(minLength: 8)
            artworkSlot(width: artSide, height: artSide)
            Spacer(minLength: 18)
            VStack(spacing: 12) {
                infoModule
                HStack(alignment: .top, spacing: 12) {
                    VStack(spacing: 12) {
                        controlsModule
                            .frame(height: 96)
                        HStack(spacing: 12) {
                            heartModule
                            lyricsModule
                        }
                        .frame(height: 96)
                    }
                    .frame(width: third * 2 + 12)
                    VolumeModule()
                        .frame(width: third, height: 204)
                }
                HStack(spacing: 12) {
                    Button { toggle(.queue) } label: {
                        Label(queueLabel, systemImage: "list.bullet")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: PlayerButtonMetrics.icon)
                    }
                    .playerGlassCapsule()
                    AirPlayGlassButton(tint: .white)
                    moreMenu
                }
            }
            .frame(width: width)
            .padding(.bottom, max(geo.safeAreaInsets.bottom, 16))
        }
        .frame(maxWidth: .infinity)
    }

    private var queueLabel: String {
        let remaining = max(0, queue.queue.tracks.count - queue.queue.currentIndex - 1)
        return remaining > 0 ? LF("Coda · {0}", remaining) : L("Coda")
    }

    private var infoModule: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 6) {
                        Text(track?.title ?? "")
                            .font(.title3.weight(.bold))
                            .lineLimit(1)
                        if track?.explicit == true {
                            Image(systemName: "e.square.fill")
                                .font(.footnote)
                                .foregroundStyle(ink.opacity(0.6))
                        }
                    }
                    Text(track?.artist ?? "")
                        .font(.subheadline)
                        .foregroundStyle(ink.opacity(0.78))
                        .lineLimit(1)
                }
                Spacer(minLength: 4)
                if queue.queue.tracks.count > 1, queue.queue.currentIndex >= 0 {
                    Text("\(queue.queue.currentIndex + 1) / \(queue.queue.tracks.count)")
                        .font(.caption2.weight(.bold).monospacedDigit())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(ink.opacity(0.2)))
                        .contentTransition(.numericText())
                }
            }
            progress
                .padding(.top, 6)
        }
        .padding(.horizontal, 18)
        .padding(.top, 14)
        .padding(.bottom, 8)
        .glassModule(radius: 30)
    }

    private var controlsModule: some View {
        HStack {
            ControlButton(systemName: "backward.fill", size: 28, action: player.previous)
            Spacer(minLength: 0)
            ControlButton(systemName: player.state.isPlaying ? "pause.fill" : "play.fill", size: 38, action: player.togglePlayPause)
            Spacer(minLength: 0)
            ControlButton(systemName: "forward.fill", size: 28, action: player.next)
        }
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .glassModule(radius: 30)
    }

    private var heartModule: some View {
        Button(action: player.toggleLike) {
            VStack(spacing: 6) {
                Image(systemName: player.state.liked ? "heart.fill" : "heart")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(player.state.liked ? AnyShapeStyle(Color.pink) : AnyShapeStyle(ink))
                    .contentTransition(.symbolEffect(.replace))
                    .symbolEffect(.bounce, value: player.state.liked)
                Text(L("Preferito"))
                    .font(.caption.weight(.semibold))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.squishy)
        .glassModule(radius: 30)
        .sensoryFeedback(.impact(weight: .medium), trigger: player.state.liked)
    }

    private var lyricsModule: some View {
        Button { toggle(.lyrics) } label: {
            VStack(spacing: 6) {
                Image(systemName: "quote.bubble.fill")
                    .font(.system(size: 24, weight: .semibold))
                Text(L("Testo"))
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle(paper)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(RoundedRectangle(cornerRadius: 30, style: .continuous).fill(ink.opacity(0.92)))
            .contentShape(Rectangle())
        }
        .buttonStyle(.squishy)
    }

    // MARK: Stile Edizione

    private func editionLayout(_ geo: GeometryProxy) -> some View {
        let width = geo.size.width - 48
        let artSide = min(width - 30, geo.size.height * 0.4)
        return VStack(alignment: .leading, spacing: 0) {
            grabber
            HStack(spacing: 8) {
                if queue.queue.tracks.count > 1, queue.queue.currentIndex >= 0 {
                    Text(LF("TRACCIA {0} / {1}", String(format: "%02d", queue.queue.currentIndex + 1), String(format: "%02d", queue.queue.tracks.count)))
                        .font(.caption.weight(.bold))
                        .tracking(1.6)
                        .foregroundStyle(palette.accent)
                        .contentTransition(.numericText())
                }
                Spacer()
                AirPlayGlassButton(tint: UIColor(palette.ink))
                moreMenu
            }
            .padding(.top, 14)
            HStack(alignment: .center, spacing: 10) {
                Text(track?.albumTitle ?? queue.queue.title ?? "")
                    .font(.system(size: 14, design: .serif).italic())
                    .tracking(1)
                    .foregroundStyle(palette.accent)
                    .lineLimit(1)
                    .frame(width: artSide, alignment: .trailing)
                    .rotationEffect(.degrees(-90))
                    .frame(width: 20, height: artSide)
                artworkSlot(width: artSide, height: artSide)
            }
            .padding(.top, 16)
            VStack(alignment: .leading, spacing: 6) {
                Text(track?.title ?? "")
                    .font(.system(size: 34, weight: .bold, design: .serif))
                    .lineLimit(2)
                    .minimumScaleFactor(0.7)
                HStack {
                    Text(track?.artist ?? "")
                        .font(.headline)
                        .foregroundStyle(palette.accent)
                        .lineLimit(1)
                    Spacer()
                    Button(action: player.toggleLike) {
                        Image(systemName: player.state.liked ? "heart.fill" : "heart")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(player.state.liked ? AnyShapeStyle(Color.red) : AnyShapeStyle(ink))
                            .contentTransition(.symbolEffect(.replace))
                            .symbolEffect(.bounce, value: player.state.liked)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.squishy)
                    .sensoryFeedback(.impact(weight: .medium), trigger: player.state.liked)
                }
            }
            .padding(.top, 22)
            WithClock {
                EditionScrub(
                    value: (scrubValue ?? player.position) / max(player.duration, 1),
                    onChanged: { fraction in
                        player.scrubbing = true
                        scrubValue = fraction * max(player.duration, 1)
                    },
                    onEnded: { fraction in
                        player.seek(to: fraction * max(player.duration, 1))
                        scrubValue = nil
                        player.scrubbing = false
                    }
                )
            }
            .padding(.top, 10)
            timesRow
            Spacer(minLength: 12)
            glassControlBar(showsProgress: false)
                .padding(.bottom, max(geo.safeAreaInsets.bottom, 16))
        }
        .padding(.horizontal, 24)
    }

    // MARK: Testo e coda aperti

    private func panelLayout(_ geo: GeometryProxy) -> some View {
        VStack(spacing: 0) {
            grabber
            compactHeader
                .padding(.horizontal, 24)
                .padding(.top, 8)
            Group {
                if panel == .lyrics {
                    LyricsPanel(onSeek: { player.seek(to: $0) })
                } else {
                    QueuePanel()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .transition(.asymmetric(insertion: .opacity.combined(with: .offset(y: 30)), removal: .opacity))
            .id(panel)
            Group {
                if style == .classic {
                    VStack(spacing: 16) {
                        progress
                        classicControls
                        bottomRow
                    }
                    .padding(.horizontal, 28)
                } else {
                    glassControlBar(showsProgress: true)
                        .padding(.horizontal, 20)
                }
            }
            .padding(.bottom, max(geo.safeAreaInsets.bottom, 16))
        }
    }

    /// Copertina piccola con titolo, in alto quando sono aperti testo o coda.
    private var compactHeader: some View {
        HStack(spacing: 14) {
            Color.clear
                .frame(width: 64, height: 64)
                .onGeometryChange(for: CGRect.self) { $0.frame(in: .named("player")) } action: { set($smallRect, $0) }
            VStack(alignment: .leading, spacing: 2) {
                Text(track?.title ?? "")
                    .font(.headline)
                    .lineLimit(1)
                Text(track?.artist ?? "")
                    .font(.subheadline)
                    .foregroundStyle(ink.opacity(0.62))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            heartButton
            moreMenu
        }
    }

    /// Un solo pannello di vetro: avanzamento sopra, testo · indietro · play · avanti · coda.
    private func glassControlBar(showsProgress: Bool) -> some View {
        VStack(spacing: 6) {
            if showsProgress {
                WithClock {
                    HStack(spacing: 10) {
                        Text(formatTime(scrubValue ?? player.position))
                        ScrubBar(
                            value: (scrubValue ?? player.position) / max(player.duration, 1),
                            onChanged: { fraction in
                                player.scrubbing = true
                                scrubValue = fraction * max(player.duration, 1)
                            },
                            onEnded: { fraction in
                                player.seek(to: fraction * max(player.duration, 1))
                                scrubValue = nil
                                player.scrubbing = false
                            }
                        )
                        Text("-" + formatTime(max(0, player.duration - (scrubValue ?? player.position))))
                    }
                    .font(.caption2.weight(.semibold).monospacedDigit())
                    .foregroundStyle(ink.opacity(0.7))
                }
            }
            HStack {
                PanelToggle(systemName: "quote.bubble", active: panel == .lyrics) { toggle(.lyrics) }
                Spacer(minLength: 0)
                ControlButton(systemName: "backward.fill", size: 26, action: player.previous)
                Spacer(minLength: 0)
                ControlButton(systemName: player.state.isPlaying ? "pause.fill" : "play.fill", size: 34, action: player.togglePlayPause)
                Spacer(minLength: 0)
                ControlButton(systemName: "forward.fill", size: 26, action: player.next)
                Spacer(minLength: 0)
                PanelToggle(systemName: "list.bullet", active: panel == .queue) { toggle(.queue) }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, showsProgress ? 12 : 6)
        .glassModule(radius: 34)
    }

    // MARK: Pezzi comuni

    private var moreMenu: some View {
        Menu {
            Button {
                player.toggleShuffle()
            } label: {
                Label(player.state.shuffle ? L("Disattiva casuale") : L("Casuale"), systemImage: "shuffle")
            }
            Button {
                player.cycleRepeat()
            } label: {
                Label(repeatLabel, systemImage: player.state.repeatMode == 1 ? "repeat.1" : "repeat")
            }
            if let track {
                Divider()
                Button {
                    addingToPlaylist = track
                } label: {
                    Label(L("Aggiungi a una playlist"), systemImage: "text.badge.plus")
                }
                if DownloadsStore.shared.info(track)?.completed != true {
                    Button {
                        DownloadsStore.shared.download(track)
                    } label: {
                        Label(L("Scarica"), systemImage: "arrow.down.circle")
                    }
                }
                Divider()
                if let artistId = track.artistId {
                    Button {
                        NamiRouter.shared.open(.artist(key: artistId, title: track.artist))
                    } label: {
                        Label(L("Vai all'artista"), systemImage: "music.microphone")
                    }
                }
                if let albumId = track.albumId {
                    Button {
                        NamiRouter.shared.open(.album(id: albumId, title: track.albumTitle ?? "Album"))
                    } label: {
                        Label(L("Vai all'album"), systemImage: "square.stack")
                    }
                }
                if let url = URL(string: "https://music.youtube.com/watch?v=" + track.id) {
                    ShareLink(item: url) {
                        Label(L("Condividi"), systemImage: "square.and.arrow.up")
                    }
                }
            }
        } label: {
            GlassMenuLabel(systemName: "ellipsis")
        }
        .menuGlassStyle()
    }

    private var repeatLabel: String {
        switch player.state.repeatMode {
        case 2: return L("Ripeti il brano")
        case 1: return L("Non ripetere")
        default: return L("Ripeti tutto")
        }
    }

    private var progress: some View {
        WithClock { progressContent }
    }

    private var progressContent: some View {
        let total = max(player.duration, 1)
        let current = scrubValue ?? player.position
        return VStack(spacing: 8) {
            ScrubBar(
                value: current / total,
                onChanged: { fraction in
                    player.scrubbing = true
                    scrubValue = fraction * total
                },
                onEnded: { fraction in
                    player.seek(to: fraction * total)
                    scrubValue = nil
                    player.scrubbing = false
                }
            )
            timesRow
        }
    }

    private var timesRow: some View {
        WithClock { timesRowContent }
    }

    private var timesRowContent: some View {
        let total = max(player.duration, 1)
        let current = scrubValue ?? player.position
        return HStack {
            Text(formatTime(current))
            Spacer()
            Text("-" + formatTime(max(0, total - current)))
        }
        .font(.caption.weight(.medium).monospacedDigit())
        .foregroundStyle(ink.opacity(0.55))
    }

    private func updatePalette() {
        guard let url = track?.largeArtworkURL else { return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }
}

// MARK: - Moduli di vetro

extension View {
    /// Modulo di vetro (Liquid Glass su iOS 26), con angoli continui.
    @ViewBuilder func glassModule(radius: CGFloat) -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
        } else {
            background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
        }
    }
}

/// Volume verticale come nel Centro di Controllo: si riempie trascinando su e giù.
private struct VolumeModule: View {
    @ObservedObject private var volume = VolumeController.shared
    @Environment(\.playerInk) private var ink
    @Environment(\.playerPaper) private var paper
    @State private var dragStart: Float?

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .bottom) {
                Rectangle()
                    .fill(ink.opacity(0.92))
                    .frame(height: geo.size.height * CGFloat(volume.volume))
                Image(systemName: volume.volume == 0 ? "speaker.slash.fill" : volume.volume < 0.5 ? "speaker.wave.1.fill" : "speaker.wave.2.fill")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(volume.volume > 0.12 ? paper : ink)
                    .contentTransition(.symbolEffect(.replace))
                    .padding(.bottom, 18)
            }
            .frame(width: geo.size.width, height: geo.size.height, alignment: .bottom)
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { g in
                        let start = dragStart ?? volume.volume
                        if dragStart == nil { dragStart = start }
                        let delta = Float(-g.translation.height / geo.size.height)
                        volume.set(min(max(start + delta, 0), 1))
                    }
                    .onEnded { _ in dragStart = nil }
            )
            .animation(.spring(response: 0.25, dampingFraction: 0.9), value: volume.volume)
        }
        .glassModule(radius: 30)
        .background(HiddenVolumeView().frame(width: 1, height: 1).opacity(0.01))
        .accessibilityElement()
        .accessibilityLabel(L("Volume"))
        .accessibilityValue("\(Int(volume.volume * 100))%")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: volume.set(min(volume.volume + 0.1, 1))
            case .decrement: volume.set(max(volume.volume - 0.1, 0))
            @unknown default: break
            }
        }
    }
}

/// Volume di sistema: lo legge da AVAudioSession e lo cambia con lo slider nascosto di MPVolumeView
/// (l'unico modo permesso da iOS).
final class VolumeController: ObservableObject {
    static let shared = VolumeController()

    @Published private(set) var volume: Float
    let volumeView = MPVolumeView(frame: CGRect(x: -200, y: -200, width: 10, height: 10))
    private var observation: NSKeyValueObservation?

    private init() {
        let session = AVAudioSession.sharedInstance()
        volume = session.outputVolume
        observation = session.observe(\.outputVolume, options: [.new]) { [weak self] session, _ in
            DispatchQueue.main.async { self?.volume = session.outputVolume }
        }
    }

    func set(_ value: Float) {
        volume = value
        guard let slider = volumeView.subviews.compactMap({ $0 as? UISlider }).first else { return }
        slider.value = value
        slider.sendActions(for: .valueChanged)
    }
}

private struct HiddenVolumeView: UIViewRepresentable {
    func makeUIView(context: Context) -> MPVolumeView { VolumeController.shared.volumeView }
    func updateUIView(_ uiView: MPVolumeView, context: Context) {}
}

/// Avanzamento "da rivista": una riga sottile con una tacca, che si ingrossa trascinando.
private struct EditionScrub: View {
    let value: Double
    let onChanged: (Double) -> Void
    let onEnded: (Double) -> Void
    @Environment(\.playerInk) private var ink
    @State private var dragging = false

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let fraction = min(max(value, 0), 1)
            ZStack(alignment: .leading) {
                Rectangle().fill(ink.opacity(0.18)).frame(height: dragging ? 4 : 2)
                Rectangle().fill(ink).frame(width: width * fraction, height: dragging ? 4 : 2)
                RoundedRectangle(cornerRadius: 1.5)
                    .fill(ink)
                    .frame(width: 3, height: dragging ? 22 : 14)
                    .offset(x: width * fraction - 1.5)
            }
            .frame(maxHeight: .infinity)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { g in
                        if !dragging { dragging = true }
                        onChanged(min(max(g.location.x / width, 0), 1))
                    }
                    .onEnded { g in
                        dragging = false
                        onEnded(min(max(g.location.x / width, 0), 1))
                    }
            )
            .animation(.spring(response: 0.3, dampingFraction: 0.75), value: dragging)
        }
        .frame(height: 28)
    }
}

// MARK: - Pezzi del player

/// Comando grande senza sfondo: premuto si rimpicciolisce e compare un alone, come in Apple Music.
struct ControlButton: View {
    let systemName: String
    let size: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: size, weight: .regular))
                .contentTransition(.symbolEffect(.replace))
                .frame(width: size + 34, height: size + 34)
                .contentShape(Circle())
        }
        .buttonStyle(PressShrinkStyle())
        .sensoryFeedback(.impact(weight: .light), trigger: systemName)
    }
}

/// Testo / coda: pieno quando è attivo (come Apple Music), di vetro quando no.
struct PanelToggle: View {
    @Environment(\.playerInk) private var ink
    @Environment(\.playerPaper) private var paper
    let systemName: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: active && systemName == "quote.bubble" ? "quote.bubble.fill" : systemName)
                .font(.system(size: PlayerButtonMetrics.symbol, weight: .semibold))
                .foregroundStyle(active ? paper : ink)
                .contentTransition(.symbolEffect(.replace))
                .frame(width: PlayerButtonMetrics.icon, height: PlayerButtonMetrics.icon)
        }
        .playerGlassCircle(prominent: active, tint: ink)
        .animation(.spring(response: 0.35, dampingFraction: 0.7), value: active)
    }
}

/// Tutti i pulsanti tondi del player hanno la stessa misura: icona 17 pt in un cerchio di vetro
/// di sistema (su iOS 26 il vetro aggiunge lo stesso margine a tutti).
enum PlayerButtonMetrics {
    static let symbol: CGFloat = 17
    static let icon: CGFloat = 28
}

extension View {
    @ViewBuilder func playerGlassCircle(prominent: Bool = false, tint: Color = .white) -> some View {
        if #available(iOS 26.0, *) {
            if prominent {
                buttonStyle(.glassProminent).buttonBorderShape(.circle).controlSize(.large).tint(tint)
            } else {
                buttonStyle(.glass).buttonBorderShape(.circle).controlSize(.large)
            }
        } else {
            buttonStyle(.bordered).buttonBorderShape(.circle).controlSize(.large).tint(prominent ? tint : .white)
        }
    }

    @ViewBuilder func playerGlassCapsule() -> some View {
        if #available(iOS 26.0, *) {
            buttonStyle(.glass).buttonBorderShape(.capsule).controlSize(.large)
        } else {
            buttonStyle(.bordered).buttonBorderShape(.capsule).controlSize(.large)
        }
    }
}

/// AirPlay nello stesso cerchio di vetro degli altri pulsanti (il selettore di sistema sta dentro).
struct AirPlayGlassButton: View {
    var tint: UIColor = .white

    var body: some View {
        Button {} label: {
            AirPlayButton(tint: tint)
                .frame(width: PlayerButtonMetrics.icon, height: PlayerButtonMetrics.icon)
        }
        .playerGlassCircle()
        .accessibilityLabel("AirPlay")
    }
}

private struct PressShrinkStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(Circle().fill(.primary.opacity(configuration.isPressed ? 0.14 : 0)))
            .scaleEffect(configuration.isPressed ? 0.86 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

/// Barra di avanzamento di Apple Music: sottile, si ingrossa mentre la trascini.
struct ScrubBar: View {
    @Environment(\.playerInk) private var ink
    let value: Double
    let onChanged: (Double) -> Void
    let onEnded: (Double) -> Void
    @State private var dragging = false

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let fraction = min(max(value, 0), 1)
            ZStack(alignment: .leading) {
                Capsule().fill(ink.opacity(0.22))
                Capsule()
                    .fill(ink.opacity(dragging ? 1 : 0.8))
                    .frame(width: width * fraction)
            }
            .frame(height: dragging ? 14 : 7)
            .frame(maxHeight: .infinity)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { g in
                        if !dragging { dragging = true }
                        onChanged(min(max(g.location.x / width, 0), 1))
                    }
                    .onEnded { g in
                        dragging = false
                        onEnded(min(max(g.location.x / width, 0), 1))
                    }
            )
            .animation(.spring(response: 0.3, dampingFraction: 0.75), value: dragging)
        }
        .frame(height: 24)
    }
}

/// Lo slider del volume di sistema (quello vero di iOS).
struct VolumeSlider: UIViewRepresentable {
    func makeUIView(context: Context) -> MPVolumeView {
        let view = MPVolumeView(frame: .zero)
        view.showsVolumeSlider = true
        view.tintColor = .white
        return view
    }

    func updateUIView(_ uiView: MPVolumeView, context: Context) {}
}

/// Il pulsante AirPlay di sistema.
struct AirPlayButton: UIViewRepresentable {
    var tint: UIColor = .white
    func makeUIView(context: Context) -> AVRoutePickerView {
        let view = AVRoutePickerView()
        view.tintColor = tint
        view.activeTintColor = tint
        view.prioritizesVideoDevices = false
        return view
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {}
}

// MARK: - Vetro

/// Pulsante tondo di vetro (Liquid Glass su iOS 26, materiale prima).
struct GlassIconButton: View {
    let systemName: String
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            Button(action: action) {
                Image(systemName: systemName)
                    .font(.system(size: PlayerButtonMetrics.symbol, weight: .semibold))
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: PlayerButtonMetrics.icon, height: PlayerButtonMetrics.icon)
            }
            .playerGlassCircle()
        } else {
            Button(action: action) {
                GlassIconLabel(systemName: systemName)
            }
            .buttonStyle(.plain)
        }
    }
}

/// Etichetta del menu ⋯: su iOS 26 è un pulsante di vetro di sistema, così il menu si apre
/// e si richiude "trasformandosi" dal cerchio senza angoli strani.
struct GlassMenuLabel: View {
    let systemName: String

    var body: some View {
        if #available(iOS 26.0, *) {
            Image(systemName: systemName)
                .font(.system(size: PlayerButtonMetrics.symbol, weight: .semibold))
                .frame(width: PlayerButtonMetrics.icon, height: PlayerButtonMetrics.icon)
        } else {
            GlassIconLabel(systemName: systemName)
        }
    }
}

extension View {
    @ViewBuilder func menuGlassStyle() -> some View {
        if #available(iOS 26.0, *) {
            buttonStyle(.glass).buttonBorderShape(.circle).controlSize(.large)
        } else {
            self
        }
    }
}

struct GlassIconLabel: View {
    let systemName: String

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: 17, weight: .semibold))
            .contentTransition(.symbolEffect(.replace))
            .frame(width: 44, height: 44)
            .modifier(GlassCircle())
    }
}

struct GlassCircle: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular.interactive(), in: Circle())
        } else {
            content.background(.ultraThinMaterial, in: Circle())
        }
    }
}
