import SwiftUI
import Charts

// Statistiche (tempo d'ascolto, ore del giorno, artisti e brani più ascoltati, generi)
// e brani scaricati.

// MARK: - Statistiche

struct StatsView: View {
    @ObservedObject private var store = StatsStore.shared
    let onSettings: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 28) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(StatsStore.periods.enumerated()), id: \.offset) { index, label in
                            ChipButton(title: label, selected: store.period == index) {
                                withAnimation(.snappy) { store.period = index }
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 2)
                }
                .sensoryFeedback(.selection, trigger: store.period)

                if let stats = store.stats {
                    if stats.plays == 0 {
                        ContentUnavailableView(
                            "Ancora niente da contare",
                            systemImage: "chart.bar",
                            description: Text("Ascolta un po' di musica: qui vedrai quanto, quando e cosa ascolti.")
                        )
                    } else {
                        TotalsCard(stats: stats)
                            .padding(.horizontal, 20)
                        HoursChart(byHour: stats.byHour)
                        if !stats.topArtists.isEmpty { TopArtists(rows: stats.topArtists) }
                        if !stats.topSongs.isEmpty { TopSongs(rows: stats.topSongs) }
                        if !stats.genres.isEmpty { GenresView(genres: stats.genres) }
                    }
                } else {
                    LoadingView()
                }
            }
            .padding(.top, 4)
            .padding(.bottom, 32)
            .opacity(store.loading && store.stats != nil ? 0.55 : 1)
            .animation(.smooth, value: store.loading)
        }
        .background(alignment: .top) { AmbientGlow() }
        .navigationTitle("Statistiche")
        .toolbar { SettingsToolbar(onSettings: onSettings) }
        .refreshable { store.load() }
        .onAppear { store.load() }
    }
}

private func hoursText(_ seconds: TimeInterval) -> (String, String) {
    let minutes = Int(seconds / 60)
    if minutes < 120 { return ("\(minutes)", minutes == 1 ? "minuto" : "minuti") }
    let hours = minutes / 60
    return ("\(hours)", "ore")
}

private struct TotalsCard: View {
    let stats: StatsData

    var body: some View {
        let (value, unit) = hoursText(stats.total)
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 0) {
                Text("HAI ASCOLTATO")
                    .font(.caption.weight(.bold))
                    .tracking(1)
                    .foregroundStyle(.white.opacity(0.75))
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(value)
                        .font(.system(size: 64, weight: .heavy, design: .rounded))
                        .contentTransition(.numericText())
                    Text(unit)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.white.opacity(0.85))
                }
                .foregroundStyle(.white)
            }
            HStack(spacing: 0) {
                Metric(value: stats.plays, label: "ascolti")
                Metric(value: stats.distinctSongs, label: "brani")
                Metric(value: stats.distinctArtists, label: "artisti")
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(red: 0.98, green: 0.45, blue: 0.62), Color(red: 0.55, green: 0.25, blue: 0.75), Color(red: 0.25, green: 0.2, blue: 0.6)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ),
            in: RoundedRectangle(cornerRadius: 26, style: .continuous)
        )
        .shadow(color: Color(red: 0.55, green: 0.25, blue: 0.75).opacity(0.4), radius: 20, y: 10)
    }

    private struct Metric: View {
        let value: Int
        let label: String

        var body: some View {
            VStack(alignment: .leading, spacing: 0) {
                Text("\(value)")
                    .font(.title2.weight(.bold).monospacedDigit())
                    .contentTransition(.numericText())
                Text(label)
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.75))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct HoursChart: View {
    let byHour: [Int]

    private var peak: Int? {
        guard let max = byHour.max(), max > 0 else { return nil }
        return byHour.firstIndex(of: max)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(
                title: "Quando ascolti",
                subtitle: peak.map { "Il tuo momento: verso le \($0):00" },
                showsChevron: false
            )
            Chart {
                ForEach(Array(byHour.enumerated()), id: \.offset) { hour, minutes in
                    BarMark(x: .value("Ora", hour), y: .value("Minuti", minutes), width: .ratio(0.7))
                        .foregroundStyle(hour == peak ? Color.namiAccent : Color.namiAccent.opacity(0.35))
                        .cornerRadius(4)
                }
            }
            .chartXAxis {
                AxisMarks(values: [0, 6, 12, 18, 23]) { value in
                    AxisValueLabel { if let h = value.as(Int.self) { Text("\(h)") } }
                }
            }
            .chartYAxis(.hidden)
            .frame(height: 140)
            .padding(.horizontal, 20)
        }
    }
}

private struct TopArtists: View {
    let rows: [StatRow]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: "Artisti più ascoltati", showsChevron: false)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 16) {
                    ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                        NavigationLink {
                            ArtistPageView(key: row.id, title: row.title, artworkURL: row.artworkURL)
                        } label: {
                            VStack(spacing: 8) {
                                ZStack(alignment: .bottomLeading) {
                                    RemoteImage(url: row.artworkURL)
                                        .frame(width: 120, height: 120)
                                        .clipShape(Circle())
                                        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
                                    Text("\(index + 1)")
                                        .font(.system(size: 20, weight: .heavy, design: .rounded))
                                        .foregroundStyle(.white)
                                        .frame(width: 36, height: 36)
                                        .background(Circle().fill(Color.namiAccent.gradient))
                                        .overlay(Circle().strokeBorder(Color(.systemBackground), lineWidth: 3))
                                }
                                Text(row.title)
                                    .font(.subheadline.weight(.semibold))
                                    .lineLimit(1)
                                Text(hoursLabel(row.time))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                            .frame(width: 120)
                        }
                        .buttonStyle(.squishy)
                        .disabled(row.id == row.title)
                        .carouselEffect()
                    }
                }
                .padding(.horizontal, 20)
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
        }
    }
}

private func hoursLabel(_ seconds: TimeInterval) -> String {
    let (value, unit) = hoursText(seconds)
    return "\(value) \(unit)"
}

private struct TopSongs: View {
    let rows: [StatRow]
    @ObservedObject private var store = StatsStore.shared
    @ObservedObject private var player = PlayerStore.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(title: "Brani più ascoltati", showsChevron: false)
            VStack(spacing: 0) {
                ForEach(Array(rows.prefix(10).enumerated()), id: \.element.id) { index, row in
                    Button {
                        store.playTopSongs(from: index)
                    } label: {
                        HStack(spacing: 12) {
                            Text("\(index + 1)")
                                .font(.headline.monospacedDigit())
                                .foregroundStyle(index < 3 ? Color.namiAccent : .secondary)
                                .frame(width: 24)
                            ArtworkView(url: row.artworkURL, size: 48, cornerRadius: 6)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(row.title)
                                    .foregroundStyle(player.state.track?.id == row.id ? Color.namiAccent : .primary)
                                    .lineLimit(1)
                                Text(row.subtitle)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 8)
                            Text("\(row.plays)×")
                                .font(.subheadline.weight(.semibold).monospacedDigit())
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 6)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.squishy)
                    if index < min(rows.count, 10) - 1 {
                        Divider().padding(.leading, 96)
                    }
                }
            }
            .padding(.horizontal, 20)
        }
    }
}

private struct GenresView: View {
    let genres: [(String, Double)]
    @State private var appeared = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(title: "I tuoi generi", showsChevron: false)
            VStack(spacing: 10) {
                ForEach(Array(genres.enumerated()), id: \.offset) { index, genre in
                    HStack(spacing: 12) {
                        Text(genre.0)
                            .font(.subheadline.weight(.semibold))
                            .frame(width: 90, alignment: .leading)
                            .lineLimit(1)
                        GeometryReader { geo in
                            Capsule()
                                .fill(Color.namiAccent.opacity(1 - Double(index) * 0.09).gradient)
                                .frame(width: max(8, geo.size.width * (appeared ? genre.1 / (genres.first?.1 ?? 1) : 0)))
                        }
                        .frame(height: 14)
                        Text("\(Int((genre.1 * 100).rounded()))%")
                            .font(.footnote.monospacedDigit())
                            .foregroundStyle(.secondary)
                            .frame(width: 40, alignment: .trailing)
                    }
                }
            }
            .padding(.horizontal, 20)
        }
        .onAppear {
            withAnimation(.spring(response: 0.8, dampingFraction: 0.8).delay(0.1)) { appeared = true }
        }
    }
}

// MARK: - Download

/// Pallino accanto al brano: freccia piena se è sul telefono, anello che si riempie mentre scarica.
struct DownloadBadge: View {
    let song: Track
    @ObservedObject private var downloads = DownloadsStore.shared

    var body: some View {
        if let info = downloads.info(song) {
            if info.completed {
                Image(systemName: "arrow.down.circle.fill")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .transition(.scale.combined(with: .opacity))
            } else if info.running {
                ZStack {
                    Circle().stroke(Color.secondary.opacity(0.3), lineWidth: 2)
                    Circle()
                        .trim(from: 0, to: max(0.05, info.progress))
                        .stroke(Color.namiAccent, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .animation(.linear(duration: 0.3), value: info.progress)
                }
                .frame(width: 14, height: 14)
            }
        }
    }
}

struct DownloadsPage: View {
    @ObservedObject private var downloads = DownloadsStore.shared

    var body: some View {
        Group {
            if downloads.songs.isEmpty {
                ContentUnavailableView(
                    "Niente di scaricato",
                    systemImage: "arrow.down.circle",
                    description: Text("Tieni premuto su un brano e scegli \"Scarica\": lo ascolti anche senza connessione.")
                )
            } else {
                SongListPage(
                    title: "Scaricati",
                    subtitle: "\(downloads.songs.count) brani sul telefono",
                    artwork: { DownloadsArtwork() },
                    songs: downloads.songs,
                    play: { index, shuffle in downloads.play(from: index, shuffle: shuffle) },
                    onRemove: { index in
                        guard downloads.songs.indices.contains(index) else { return }
                        downloads.remove(downloads.songs[index])
                    }
                )
            }
        }
    }
}

private struct DownloadsArtwork: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.36, green: 0.55, blue: 0.98), Color(red: 0.2, green: 0.2, blue: 0.55)], startPoint: .topLeading, endPoint: .bottomTrailing)
            Image(systemName: "arrow.down.circle.fill")
                .font(.system(size: 80, weight: .semibold))
                .foregroundStyle(.white)
        }
        .aspectRatio(1, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}
