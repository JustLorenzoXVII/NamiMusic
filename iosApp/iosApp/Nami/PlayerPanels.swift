import SwiftUI

// Dentro il player: testo sincronizzato (riga attiva grande e luminosa, le altre sfocate,
// scorre da solo; tocca una riga per saltarci) e coda "Successivi" da riordinare.

struct LyricsPanel: View {
    let onSeek: (TimeInterval) -> Void
    @ObservedObject private var player = PlayerStore.shared
    @ObservedObject private var store = LyricsStore.shared
    @State private var userScrolling = false

    private var activeIndex: Int? {
        guard let lines = store.lyrics?.lines, store.lyrics?.synced == true else { return nil }
        // Un attimo d'anticipo, così la riga si accende insieme alla voce
        let time = player.position + 0.25
        return lines.lastIndex { $0.time <= time }
    }

    var body: some View {
        Group {
            if store.loading {
                ProgressView()
                    .tint(.white)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let lyrics = store.lyrics {
                if lyrics.synced {
                    synced(lyrics.lines)
                } else {
                    ScrollView {
                        Text(lyrics.plain ?? "")
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(.white.opacity(0.85))
                            .lineSpacing(8)
                            .padding(.horizontal, 28)
                            .padding(.vertical, 24)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .scrollIndicators(.hidden)
                }
            } else {
                VStack(spacing: 10) {
                    Image(systemName: "quote.bubble")
                        .font(.system(size: 40, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.5))
                    Text(L("Testo non disponibile"))
                        .font(.headline)
                        .foregroundStyle(.white.opacity(0.7))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .mask(
            LinearGradient(stops: [
                .init(color: .clear, location: 0),
                .init(color: .black, location: 0.08),
                .init(color: .black, location: 0.88),
                .init(color: .clear, location: 1),
            ], startPoint: .top, endPoint: .bottom)
        )
        .onAppear { store.load(for: player.state.track?.id) }
        .onChange(of: player.state.track?.id) { _, id in store.load(for: id) }
    }

    private func synced(_ lines: [LyricLine]) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    Color.clear.frame(height: 60)
                    ForEach(lines) { line in
                        let active = line.id == activeIndex
                        let distance = abs(line.id - (activeIndex ?? 0))
                        Text(line.text)
                            .font(.system(size: 30, weight: .bold))
                            .foregroundStyle(.white.opacity(active ? 1 : 0.32))
                            .blur(radius: active || userScrolling ? 0 : min(Double(distance) * 0.6, 2.2))
                            .scaleEffect(active ? 1 : 0.96, anchor: .leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .id(line.id)
                            .contentShape(Rectangle())
                            .onTapGesture { onSeek(line.time) }
                            .animation(.spring(response: 0.55, dampingFraction: 0.8), value: active)
                    }
                    Color.clear.frame(height: 220)
                }
                .padding(.horizontal, 28)
            }
            .scrollIndicators(.hidden)
            .onScrollPhaseChange { _, phase in
                if phase == .interacting { userScrolling = true }
                if phase == .idle {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) { userScrolling = false }
                }
            }
            .onChange(of: activeIndex) { _, index in
                guard let index, !userScrolling else { return }
                withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) {
                    proxy.scrollTo(index, anchor: UnitPoint(x: 0, y: 0.3))
                }
            }
            .onAppear {
                if let index = activeIndex { proxy.scrollTo(index, anchor: UnitPoint(x: 0, y: 0.3)) }
            }
        }
    }
}

// MARK: - Coda

struct QueuePanel: View {
    @ObservedObject private var store = QueueStore.shared
    @ObservedObject private var player = PlayerStore.shared

    var body: some View {
        let upNext = store.upNext
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L("Successivi"))
                        .font(.title3.weight(.bold))
                    if let title = store.queue.title, !title.isEmpty {
                        Text(LF("Da {0}", title))
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.6))
                            .lineLimit(1)
                    }
                }
                Spacer()
                ToggleChip(systemName: "shuffle", active: player.state.shuffle, action: player.toggleShuffle)
                ToggleChip(systemName: player.state.repeatMode == 1 ? "repeat.1" : "repeat", active: player.state.repeatMode != 0, action: player.cycleRepeat)
                ToggleChip(systemName: "infinity", active: store.autoplay, action: store.toggleAutoplay)
                    .accessibilityLabel(L("Automatica"))
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)

            if store.autoplay {
                Text(L("Alla fine della coda continua con brani simili."))
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.6))
                    .padding(.horizontal, 24)
                    .transition(.opacity)
            }
            if upNext.isEmpty {
                Text(L("Non c'è altro in coda"))
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.6))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(upNext) { entry in
                        Button {
                            store.play(at: entry.index)
                        } label: {
                            HStack(spacing: 12) {
                                ArtworkView(url: entry.track.artworkURL, size: 44, cornerRadius: 6)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(entry.track.title)
                                        .font(.body)
                                        .lineLimit(1)
                                    Text(entry.track.artist)
                                        .font(.subheadline)
                                        .foregroundStyle(.white.opacity(0.6))
                                        .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .listRowBackground(Color.clear)
                        .listRowSeparatorTint(.white.opacity(0.12))
                        .listRowInsets(EdgeInsets(top: 6, leading: 24, bottom: 6, trailing: 16))
                        .contextMenu {
                            Button(role: .destructive) {
                                store.remove(at: entry.index)
                            } label: {
                                Label(L("Rimuovi dalla coda"), systemImage: "minus.circle")
                            }
                        }
                    }
                    .onMove { source, destination in
                        guard let from = source.first else { return }
                        let base = store.queue.currentIndex + 1
                        let to = destination > from ? destination - 1 : destination
                        store.move(from: base + from, to: base + to)
                    }
                    .deleteDisabled(true)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .environment(\.editMode, .constant(.active))
                .animation(.snappy, value: upNext.map(\.track.id))
            }
        }
    }
}

private struct ToggleChip: View {
    let systemName: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(active ? Color.black.opacity(0.8) : .white)
                .contentTransition(.symbolEffect(.replace))
                .frame(width: 38, height: 32)
                .background(Capsule().fill(.white.opacity(active ? 0.9 : 0.14)))
        }
        .buttonStyle(.squishy)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: active)
    }
}
