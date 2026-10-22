import SwiftUI

// Ricerca come Apple Music: campo di ricerca nella barra (iOS 26), ricerche recenti e mood
// colorati quando è vuota, suggerimenti mentre scrivi, risultati con i filtri in vetro.

struct SearchView: View {
    @ObservedObject private var store = SearchStore.shared
    @State private var query = ""

    private var showingResults: Bool { !store.searchedQuery.isEmpty && !query.isEmpty }

    var body: some View {
        ScrollView {
            Group {
                if showingResults {
                    SearchResultsView()
                        .transition(.opacity.combined(with: .move(edge: .bottom)))
                } else {
                    SearchHomeView(onSearch: submit)
                        .transition(.opacity)
                }
            }
            .animation(.smooth(duration: 0.35), value: showingResults)
            .padding(.bottom, 32)
        }
        .navigationTitle(L("Cerca"))
        .searchable(text: $query, prompt: Text(L("Artisti, brani, album e altro")))
        .searchSuggestions {
            if !showingResults || query != store.searchedQuery {
                ForEach(store.suggestions, id: \.self) { suggestion in
                    Button {
                        submit(suggestion)
                    } label: {
                        Label(suggestion, systemImage: "magnifyingglass")
                    }
                    .tint(.primary)
                }
            }
        }
        .onSubmit(of: .search) { submit(query) }
        .onChange(of: query) { _, text in
            store.updateSuggestions(for: text)
            if text.isEmpty { store.reset() }
        }
        .onAppear { store.loadMoodsIfNeeded() }
    }

    private func submit(_ text: String) {
        let q = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return }
        query = q
        store.search(q)
    }
}

// MARK: - Ricerca vuota: recenti e mood

private struct SearchHomeView: View {
    @ObservedObject private var store = SearchStore.shared
    let onSearch: (String) -> Void

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 26) {
            if !store.history.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        SectionTitle(title: "Ricerche recenti", showsChevron: false)
                        Button(L("Cancella")) {
                            withAnimation(.snappy) { store.clearHistory() }
                        }
                        .font(.subheadline.weight(.semibold))
                        .padding(.trailing, 20)
                    }
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(store.history, id: \.self) { entry in
                                ChipButton(title: entry, selected: false) { onSearch(entry) }
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 2)
                    }
                }
            }
            if store.moods.isEmpty {
                LoadingView()
            }
            ForEach(store.moods) { group in
                VStack(alignment: .leading, spacing: 12) {
                    SectionTitle(title: group.title, showsChevron: false)
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(group.items) { mood in
                            NavigationLink {
                                MoodPageView(mood: mood)
                                    .zoomDestination(mood.id)
                            } label: {
                                MoodTile(mood: mood)
                                    .zoomSource(mood.id)
                            }
                            .buttonStyle(.squishy)
                        }
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
        .padding(.top, 8)
    }
}

/// Tessera di un mood: il suo colore in sfumatura, titolo grande e una nota inclinata.
private struct MoodTile: View {
    let mood: MoodItem

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(colors: [mood.color, mood.color.opacity(0.55)], startPoint: .topLeading, endPoint: .bottomTrailing)
            Image(systemName: "music.note")
                .font(.system(size: 54, weight: .black))
                .foregroundStyle(.white.opacity(0.18))
                .rotationEffect(.degrees(18))
                .offset(x: 18, y: 8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            Text(mood.title)
                .font(.headline.weight(.bold))
                .foregroundStyle(.white)
                .shadow(color: .black.opacity(0.25), radius: 4, y: 1)
                .lineLimit(2)
                .padding(12)
        }
        .frame(height: 96)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

// MARK: - Risultati

private struct SearchResultsView: View {
    @ObservedObject private var store = SearchStore.shared

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 22) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(SearchStore.kinds.enumerated()), id: \.offset) { index, label in
                        ChipButton(title: label, selected: store.kind == index) {
                            store.select(kind: index)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 2)
            }
            .sensoryFeedback(.selection, trigger: store.kind)

            if store.loading && store.results.isEmpty {
                LoadingView()
            } else if let error = store.error {
                ErrorView(title: "Ricerca non riuscita", message: error) { store.search(store.searchedQuery) }
            } else if store.results.isEmpty {
                ContentUnavailableView.search(text: store.searchedQuery)
            }

            ForEach(Array(store.results.enumerated()), id: \.element.id) { index, section in
                VStack(alignment: .leading, spacing: 10) {
                    if !section.title.isEmpty {
                        SectionTitle(title: section.title, showsChevron: false)
                    }
                    if store.kind == 0 && index == 0, section.items.count == 1, let top = section.items.first {
                        TopResultCard(item: top)
                            .padding(.horizontal, 20)
                    } else {
                        VStack(spacing: 0) {
                            ForEach(section.items) { item in
                                ItemButton(item: item) {
                                    ItemRowLabel(item: item, showsType: store.kind == 0)
                                        .padding(.vertical, 6)
                                }
                                if item.id != section.items.last?.id {
                                    Divider().padding(.leading, 62)
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                }
            }
            .opacity(store.loading ? 0.5 : 1)
        }
        .padding(.top, 8)
    }
}

/// "Risultato migliore": card grande con copertina, tipo e un pulsante di vetro per ascoltare.
private struct TopResultCard: View {
    let item: HomeItem
    @State private var palette = ArtworkPalette.fallback

    var body: some View {
        ItemButton(item: item) {
            HStack(spacing: 16) {
                Group {
                    if item.kind == .artist {
                        RemoteImage(url: item.artworkURL)
                            .frame(width: 96, height: 96)
                            .clipShape(Circle())
                    } else {
                        ArtworkView(url: item.artworkURL, size: 96, cornerRadius: 12)
                    }
                }
                .shadow(color: .black.opacity(0.35), radius: 10, y: 5)
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                    Text(item.subtitle.isEmpty ? kindLabel : "\(kindLabel) · \(item.subtitle)")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.75))
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                Image(systemName: item.kind == .song || item.kind == .other ? "play.fill" : "chevron.forward")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
            }
            .padding(16)
            .background {
                LinearGradient(colors: [palette.top, palette.bottom], startPoint: .topLeading, endPoint: .bottomTrailing)
                    .animation(.easeInOut(duration: 0.6), value: palette)
            }
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: palette.bottom.opacity(0.4), radius: 14, y: 8)
        }
        .onAppear {
            guard let url = item.artworkURL else { return }
            PaletteExtractor.palette(for: url) { palette = $0 }
        }
    }

    private var kindLabel: String {
        switch item.kind {
        case .song: return L("Brano")
        case .album: return L("Album")
        case .playlist: return L("Playlist")
        case .artist: return L("Artista")
        case .other: return L("Video")
        }
    }
}
