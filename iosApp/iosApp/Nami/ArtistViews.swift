import SwiftUI

// Pagina artista come Apple Music: foto grande che si allunga tirando giù, nome sopra la foto,
// Casuale e Radio in vetro, poi brani, album, singoli e artisti simili. Pagina dei mood.

struct ArtistPageView: View {
    let key: String
    let title: String
    let artworkURL: URL?
    @StateObject private var store: PageStore
    @State private var showsTitle = false
    @State private var expandedBio = false
    @State private var palette: ArtworkPalette?

    private let heroHeight: CGFloat = 400

    init(key: String, title: String, artworkURL: URL?) {
        self.key = key
        self.title = title
        self.artworkURL = artworkURL
        _store = StateObject(wrappedValue: PageStore(key: key, kind: .artist))
    }

    private var page: PageData? { store.page }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                hero
                if let page {
                    VStack(spacing: 12) {
                        if page.canPlay {
                            HStack(spacing: 14) {
                                PlayActionButton(title: "Casuale", systemImage: "shuffle") { store.playArtist(shuffle: true) }
                                PlayActionButton(title: "Radio", systemImage: "dot.radiowaves.left.and.right") { store.playArtist(shuffle: false) }
                            }
                        }
                        HStack {
                            SmallActionButton(
                                title: store.followed ? L("Segui già") : L("Segui"),
                                systemImage: store.followed ? "checkmark" : "plus",
                                active: store.followed,
                                action: store.toggleFollow
                            )
                            Spacer()
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, -12)
                }
                if store.loading && page == nil {
                    LoadingView()
                } else if let error = store.error, page == nil {
                    ErrorView(title: "Artista non disponibile", message: error) { store.load() }
                }
                if let page {
                    SectionsList(sections: page.sections)
                    if let bio = page.description {
                        VStack(alignment: .leading, spacing: 8) {
                            SectionTitle(title: "Informazioni", showsChevron: false)
                            Text(bio)
                                .font(.callout)
                                .foregroundStyle(.secondary)
                                .lineLimit(expandedBio ? nil : 4)
                                .padding(.horizontal, 20)
                                .onTapGesture { withAnimation(.smooth) { expandedBio.toggle() } }
                            if !expandedBio {
                                Button(L("Altro")) { withAnimation(.smooth) { expandedBio = true } }
                                    .font(.callout.weight(.semibold))
                                    .padding(.horizontal, 20)
                            }
                        }
                    }
                }
            }
            .padding(.bottom, 32)
        }
        .background(alignment: .top) {
            LinearGradient(colors: [(palette?.bottom ?? .clear).opacity(0.6), .clear], startPoint: .top, endPoint: .bottom)
                .frame(height: 900)
                .ignoresSafeArea()
                .animation(.easeInOut(duration: 0.8), value: palette)
        }
        .ignoresSafeArea(edges: .top)
        .onScrollGeometryChange(for: Bool.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top > heroHeight - 90
        } action: { _, past in
            withAnimation(.easeInOut(duration: 0.2)) { showsTitle = past }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(page?.title ?? title)
                    .font(.headline)
                    .opacity(showsTitle ? 1 : 0)
            }
        }
        .toolbarBackground(showsTitle ? .automatic : .hidden, for: .navigationBar)
        .onAppear(perform: updatePalette)
        .onChange(of: page?.artworkURL) { _, _ in updatePalette() }
    }

    private var hero: some View {
        GeometryReader { geo in
            let minY = geo.frame(in: .scrollView).minY
            let stretch = max(0, minY)
            ZStack(alignment: .bottomLeading) {
                RemoteImage(url: page?.artworkURL ?? artworkURL)
                    .frame(width: geo.size.width, height: heroHeight + stretch)
                LinearGradient(colors: [.clear, .black.opacity(0.15), .black.opacity(0.7)], startPoint: .center, endPoint: .bottom)
                VStack(alignment: .leading, spacing: 4) {
                    Text(page?.title ?? title)
                        .font(.system(size: 40, weight: .heavy))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.6)
                        .shadow(color: .black.opacity(0.3), radius: 8, y: 2)
                    if let subtitle = page?.subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.white.opacity(0.85))
                            .transition(.opacity)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 22)
            }
            .frame(width: geo.size.width, height: heroHeight + stretch)
            .clipped()
            .offset(y: -stretch)
        }
        .frame(height: heroHeight)
    }

    private func updatePalette() {
        guard let url = page?.artworkURL ?? artworkURL else { return }
        PaletteExtractor.palette(for: url) { palette = $0 }
    }
}

// MARK: - Mood e generi

struct MoodPageView: View {
    let mood: MoodItem
    @StateObject private var store: PageStore

    init(mood: MoodItem) {
        self.mood = mood
        _store = StateObject(wrappedValue: PageStore(key: mood.id, kind: .mood))
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 28) {
                if store.loading && store.page == nil {
                    LoadingView()
                } else if let error = store.error, store.page == nil {
                    ErrorView(title: "Non disponibile", message: error) { store.load() }
                }
                if let page = store.page {
                    SectionsList(sections: page.sections)
                }
            }
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .background(alignment: .top) {
            LinearGradient(colors: [mood.color.opacity(0.7), .clear], startPoint: .top, endPoint: .bottom)
                .frame(height: 420)
                .ignoresSafeArea()
        }
        .navigationTitle(mood.title)
        .navigationBarTitleDisplayMode(.large)
    }
}
