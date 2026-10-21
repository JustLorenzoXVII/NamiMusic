import SwiftUI
import AVKit
import MediaPlayer

// Il player a schermo intero, come Apple Music: sfondo con i colori della copertina,
// copertina grande che si rimpicciolisce in pausa, barra che si ingrossa quando la trascini,
// volume e AirPlay di sistema, pulsanti di vetro.

enum PlayerPanel: Equatable {
    case none, lyrics, queue
}

struct PlayerView: View {
    @ObservedObject private var player = PlayerStore.shared
    @Environment(\.dismiss) private var dismiss
    @State private var palette = ArtworkPalette.fallback
    @State private var scrubValue: TimeInterval?
    @State private var addingToPlaylist: Track?
    @State private var panel: PlayerPanel = .none
    @Namespace private var artNamespace

    private var track: Track? { player.state.track }

    var body: some View {
        GeometryReader { geo in
            let artSide = min(geo.size.width - 56, geo.size.height * 0.42)
            ZStack {
                background
                VStack(spacing: 0) {
                    grabber
                    if panel == .none {
                        Spacer(minLength: 12)
                        artwork(side: artSide)
                        Spacer(minLength: 20)
                    } else {
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
                    }
                    VStack(spacing: panel == .none ? 22 : 16) {
                        if panel == .none {
                            titleRow
                                .transition(.opacity)
                        }
                        progress
                        controls
                        if panel == .none {
                            VolumeSlider()
                                .frame(height: 34)
                                .transition(.opacity)
                        }
                        bottomRow
                    }
                    .padding(.horizontal, 28)
                    .padding(.bottom, max(geo.safeAreaInsets.bottom, 16))
                }
                .padding(.top, geo.safeAreaInsets.top)
            }
            .ignoresSafeArea()
        }
        .foregroundStyle(.white)
        .environment(\.colorScheme, .dark)
        .onAppear { updatePalette() }
        .onChange(of: track?.largeArtworkURL) { _, _ in updatePalette() }
        .statusBarHidden(false)
        .sheet(item: $addingToPlaylist) { song in
            AddToPlaylistSheet(song: song)
        }
        .sensoryFeedback(.selection, trigger: panel)
    }

    private func toggle(_ target: PlayerPanel) {
        withAnimation(.spring(response: 0.5, dampingFraction: 0.85)) {
            panel = panel == target ? .none : target
        }
    }

    /// Copertina piccola con titolo, in alto quando sono aperti testo o coda.
    private var compactHeader: some View {
        HStack(spacing: 14) {
            ArtworkView(url: track?.largeArtworkURL, size: 64, cornerRadius: 8)
                .matchedGeometryEffect(id: "artwork", in: artNamespace)
                .shadow(color: .black.opacity(0.35), radius: 10, y: 5)
                .onTapGesture { toggle(panel) }
            VStack(alignment: .leading, spacing: 2) {
                Text(track?.title ?? "")
                    .font(.headline)
                    .lineLimit(1)
                Text(track?.artist ?? "")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.62))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            GlassIconButton(systemName: player.state.liked ? "heart.fill" : "heart", action: player.toggleLike)
                .symbolEffect(.bounce, value: player.state.liked)
            moreMenu
        }
    }

    // MARK: Parti

    private var background: some View {
        ZStack {
            LinearGradient(colors: [palette.top, palette.bottom], startPoint: .top, endPoint: .bottom)
            RemoteImage(url: track?.largeArtworkURL, placeholder: .clear)
                .blur(radius: 80, opaque: true)
                .opacity(0.45)
                .scaleEffect(1.4)
            LinearGradient(colors: [.clear, .black.opacity(0.35)], startPoint: .center, endPoint: .bottom)
        }
        .animation(.easeInOut(duration: 0.6), value: palette)
        .ignoresSafeArea()
    }

    private var grabber: some View {
        Capsule()
            .fill(.white.opacity(0.45))
            .frame(width: 38, height: 5)
            .padding(.top, 10)
            .padding(.bottom, 4)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
            .onTapGesture { dismiss() }
            .accessibilityLabel("Chiudi")
    }

    private func artwork(side: CGFloat) -> some View {
        ArtworkView(url: track?.largeArtworkURL, size: side, cornerRadius: 12)
            .matchedGeometryEffect(id: "artwork", in: artNamespace)
            .shadow(color: .black.opacity(0.4), radius: 24, y: 14)
            .scaleEffect(player.state.isPlaying ? 1 : 0.82)
            .animation(.spring(response: 0.5, dampingFraction: 0.68), value: player.state.isPlaying)
            .frame(maxWidth: .infinity)
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
                            .foregroundStyle(.white.opacity(0.6))
                    }
                }
                Text(track?.artist ?? "")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.62))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            GlassIconButton(systemName: player.state.liked ? "heart.fill" : "heart", action: player.toggleLike)
                .symbolEffect(.bounce, value: player.state.liked)
            moreMenu
        }
    }

    private var moreMenu: some View {
        Menu {
            Button {
                player.toggleShuffle()
            } label: {
                Label(player.state.shuffle ? "Disattiva casuale" : "Casuale", systemImage: "shuffle")
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
                    Label("Aggiungi a una playlist", systemImage: "text.badge.plus")
                }
                if DownloadsStore.shared.info(track)?.completed != true {
                    Button {
                        DownloadsStore.shared.download(track)
                    } label: {
                        Label("Scarica", systemImage: "arrow.down.circle")
                    }
                }
            }
        } label: {
            GlassIconLabel(systemName: "ellipsis")
        }
    }

    private var repeatLabel: String {
        switch player.state.repeatMode {
        case 2: return "Ripeti il brano"
        case 1: return "Non ripetere"
        default: return "Ripeti tutto"
        }
    }

    private var progress: some View {
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
            HStack {
                Text(formatTime(current))
                Spacer()
                Text("-" + formatTime(max(0, total - current)))
            }
            .font(.caption.weight(.medium).monospacedDigit())
            .foregroundStyle(.white.opacity(0.55))
        }
    }

    private var controls: some View {
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
            AirPlayButton()
                .frame(width: 44, height: 44)
                .modifier(GlassCircle())
            Spacer()
            PanelToggle(systemName: "list.bullet", active: panel == .queue) { toggle(.queue) }
        }
        .padding(.horizontal, 24)
    }

    private func updatePalette() {
        guard let url = track?.largeArtworkURL else { return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }
}

// MARK: - Pezzi del player

/// Comando grande senza sfondo: premuto si rimpicciolisce e compare un alone, come in Apple Music.
private struct ControlButton: View {
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
private struct PanelToggle: View {
    let systemName: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: active && systemName == "quote.bubble" ? "quote.bubble.fill" : systemName)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(active ? Color.black.opacity(0.8) : .white)
                .contentTransition(.symbolEffect(.replace))
                .frame(width: 44, height: 44)
                .background(Circle().fill(.white.opacity(active ? 0.9 : 0)))
                .modifier(GlassCircle())
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.35, dampingFraction: 0.7), value: active)
    }
}

private struct PressShrinkStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(Circle().fill(.white.opacity(configuration.isPressed ? 0.14 : 0)))
            .scaleEffect(configuration.isPressed ? 0.86 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

/// Barra di avanzamento di Apple Music: sottile, si ingrossa mentre la trascini.
private struct ScrubBar: View {
    let value: Double
    let onChanged: (Double) -> Void
    let onEnded: (Double) -> Void
    @State private var dragging = false

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let fraction = min(max(value, 0), 1)
            ZStack(alignment: .leading) {
                Capsule().fill(.white.opacity(0.22))
                Capsule()
                    .fill(.white.opacity(dragging ? 1 : 0.8))
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
private struct VolumeSlider: UIViewRepresentable {
    func makeUIView(context: Context) -> MPVolumeView {
        let view = MPVolumeView(frame: .zero)
        view.showsVolumeSlider = true
        view.tintColor = .white
        return view
    }

    func updateUIView(_ uiView: MPVolumeView, context: Context) {}
}

/// Il pulsante AirPlay di sistema.
private struct AirPlayButton: UIViewRepresentable {
    func makeUIView(context: Context) -> AVRoutePickerView {
        let view = AVRoutePickerView()
        view.tintColor = .white
        view.activeTintColor = .white
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
        Button(action: action) {
            GlassIconLabel(systemName: systemName)
        }
        .buttonStyle(.plain)
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
