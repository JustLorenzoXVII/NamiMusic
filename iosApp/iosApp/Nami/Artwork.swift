import SwiftUI
import UIKit
import CoreImage

// Copertine: caricate una volta e tenute in memoria; da ognuna si ricavano i colori
// per lo sfondo del player (come Apple Music).

final class ImageCache {
    static let shared = ImageCache()
    private let cache = NSCache<NSURL, UIImage>()
    private var running: [URL: [(UIImage?) -> Void]] = [:]
    private let lock = NSLock()

    init() { cache.countLimit = 300 }

    func cached(_ url: URL) -> UIImage? { cache.object(forKey: url as NSURL) }

    func load(_ url: URL, completion: @escaping (UIImage?) -> Void) {
        if let image = cached(url) { completion(image); return }
        lock.lock()
        if running[url] != nil {
            running[url]?.append(completion)
            lock.unlock()
            return
        }
        running[url] = [completion]
        lock.unlock()
        URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            let image = data.flatMap { UIImage(data: $0) }
            if let image { self?.cache.setObject(image, forKey: url as NSURL) }
            self?.lock.lock()
            let waiting = self?.running.removeValue(forKey: url) ?? []
            self?.lock.unlock()
            DispatchQueue.main.async { waiting.forEach { $0(image) } }
        }.resume()
    }
}

/// Immagine da internet: resta quella vecchia finché la nuova non è pronta (niente lampi grigi).
struct RemoteImage: View {
    let url: URL?
    var placeholder: Color = Color(.tertiarySystemFill)
    @State private var image: UIImage?
    @State private var shownURL: URL?

    var body: some View {
        ZStack {
            placeholder
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .transition(.opacity)
            }
        }
        .clipped()
        .onAppear { load() }
        .onChange(of: url) { _, _ in load() }
    }

    private func load() {
        guard let url, url != shownURL else { return }
        if let cached = ImageCache.shared.cached(url) {
            image = cached
            shownURL = url
            return
        }
        ImageCache.shared.load(url) { loaded in
            guard self.url == url, let loaded else { return }
            withAnimation(.easeInOut(duration: 0.2)) { image = loaded }
            shownURL = url
        }
    }
}

/// Copertina quadrata con angoli continui.
struct ArtworkView: View {
    let url: URL?
    var size: CGFloat? = nil
    var cornerRadius: CGFloat = 6

    var body: some View {
        RemoteImage(url: url)
            .aspectRatio(1, contentMode: .fit)
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
            )
    }
}

/// Mosaico di 4 copertine (playlist), o una sola se ce n'è una.
struct MosaicArtwork: View {
    let urls: [URL]
    var cornerRadius: CGFloat = 8

    var body: some View {
        GeometryReader { geo in
            let side = geo.size.width
            Group {
                if urls.count >= 4 {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            RemoteImage(url: urls[0]).frame(width: side / 2, height: side / 2)
                            RemoteImage(url: urls[1]).frame(width: side / 2, height: side / 2)
                        }
                        HStack(spacing: 0) {
                            RemoteImage(url: urls[2]).frame(width: side / 2, height: side / 2)
                            RemoteImage(url: urls[3]).frame(width: side / 2, height: side / 2)
                        }
                    }
                } else if let first = urls.first {
                    RemoteImage(url: first).frame(width: side, height: side)
                } else {
                    ZStack {
                        Color(.tertiarySystemFill)
                        Image(systemName: "music.note.list")
                            .font(.system(size: side * 0.3, weight: .medium))
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .frame(width: side, height: side)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

// MARK: - Colori dalla copertina

struct ArtworkPalette: Equatable {
    var top: Color
    var bottom: Color

    static let fallback = ArtworkPalette(top: Color(white: 0.25), bottom: Color(white: 0.08))
}

enum PaletteExtractor {
    private static let context = CIContext(options: [.workingColorSpace: NSNull()])
    private static var cache: [URL: ArtworkPalette] = [:]

    /// Colore medio della metà alta e di quella bassa, scuriti per tenere leggibili i testi bianchi.
    static func palette(for url: URL, completion: @escaping (ArtworkPalette) -> Void) {
        if let cached = cache[url] { completion(cached); return }
        ImageCache.shared.load(url) { image in
            guard let image, let ci = CIImage(image: image) else { return }
            DispatchQueue.global(qos: .userInitiated).async {
                let extent = ci.extent
                let topRect = CGRect(x: extent.minX, y: extent.midY, width: extent.width, height: extent.height / 2)
                let bottomRect = CGRect(x: extent.minX, y: extent.minY, width: extent.width, height: extent.height / 2)
                let top = average(ci, in: topRect) ?? UIColor(white: 0.3, alpha: 1)
                let bottom = average(ci, in: bottomRect) ?? UIColor(white: 0.1, alpha: 1)
                let result = ArtworkPalette(top: Color(darkened(top, to: 0.42)), bottom: Color(darkened(bottom, to: 0.22)))
                DispatchQueue.main.async {
                    cache[url] = result
                    completion(result)
                }
            }
        }
    }

    private static func average(_ image: CIImage, in rect: CGRect) -> UIColor? {
        guard let filter = CIFilter(name: "CIAreaAverage", parameters: [kCIInputImageKey: image, kCIInputExtentKey: CIVector(cgRect: rect)]),
              let output = filter.outputImage else { return nil }
        var pixel = [UInt8](repeating: 0, count: 4)
        context.render(output, toBitmap: &pixel, rowBytes: 4, bounds: CGRect(x: 0, y: 0, width: 1, height: 1), format: .RGBA8, colorSpace: nil)
        return UIColor(red: CGFloat(pixel[0]) / 255, green: CGFloat(pixel[1]) / 255, blue: CGFloat(pixel[2]) / 255, alpha: 1)
    }

    /// Stessa tinta, luminosità al massimo [to] e saturazione un po' più alta.
    private static func darkened(_ color: UIColor, to maxBrightness: CGFloat) -> UIColor {
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getHue(&h, saturation: &s, brightness: &b, alpha: &a)
        return UIColor(hue: h, saturation: min(1, s * 1.25 + 0.05), brightness: min(b, maxBrightness), alpha: 1)
    }
}

// MARK: - Colori di Nami

extension Color {
    /// Accento dell'icona: rosa del fiore nel tema scuro, prugna nel tema chiaro.
    static let namiAccent = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 1.0, green: 0.71, blue: 0.81, alpha: 1)
            : UIColor(red: 0.42, green: 0.18, blue: 0.39, alpha: 1)
    })
}

/// "3:07" da secondi.
func formatTime(_ seconds: TimeInterval) -> String {
    guard seconds.isFinite, seconds > 0 else { return "0:00" }
    let total = Int(seconds.rounded())
    return String(format: "%d:%02d", total / 60, total % 60)
}
