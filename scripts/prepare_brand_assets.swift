// Deterministic packaging of the user's original logo. No redrawing/generation.
import AppKit
import CryptoKit
let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let source = root.appendingPathComponent("docs/reference/rotam-approved-logo.png")
let image = NSImage(contentsOf: source)!
let normalized = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 1024, pixelsHigh: 1024,
 bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
 colorSpaceName: .deviceRGB, bytesPerRow: 4096, bitsPerPixel: 32)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: normalized)
image.draw(in: NSRect(x: 0, y: 0, width: 1024, height: 1024))
NSGraphicsContext.restoreGraphicsState()
let input = normalized
let size = 1024
func render(_ monochrome: Bool) -> NSBitmapImageRep {
    let out = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size,
        bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
        colorSpaceName: .deviceRGB, bytesPerRow: size * 4, bitsPerPixel: 32)!
    for y in 0..<size { for x in 0..<size {
        let c = input.colorAt(x: x * input.pixelsWide / size, y: y * input.pixelsHigh / size)!.usingColorSpace(.deviceRGB)!
        if monochrome {
            // Black is background; ivory R and orange diamond retain their exact silhouette.
            let a = min(1, max(0, (max(c.redComponent, c.greenComponent, c.blueComponent) - 0.1) / 0.65))
            out.setColor(NSColor(deviceRed: 1, green: 1, blue: 1, alpha: a), atX: x, y: y)
        } else { out.setColor(c, atX: x, y: y) }
    }}
    return out
}
for (name, mono) in [("rotam_launcher_art", false), ("rotam_launcher_mask", true)] {
    try render(mono).representation(using: .png, properties: [:])!.write(to: root.appendingPathComponent("app/src/main/res/drawable-nodpi/\(name).png"))
}
// iOS source asset, ready for a future native target. No iOS implementation is implied.
let ios = root.appendingPathComponent("ios/Brand.xcassets/AppIcon.appiconset")
try FileManager.default.createDirectory(at: ios, withIntermediateDirectories: true)
try render(false).representation(using: .png, properties: [:])!.write(to: ios.appendingPathComponent("Rotam-1024.png"))

// Tight, transparent crop of the same supplied artwork for in-app headers and widgets.
let mark = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 485, pixelsHigh: 520,
 bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
 colorSpaceName: .deviceRGB, bytesPerRow: 485 * 4, bitsPerPixel: 32)!
for y in 0..<520 { for x in 0..<485 {
 let c = input.colorAt(x: x + 280, y: y + 250)!.usingColorSpace(.deviceRGB)!
 let alpha = min(1, max(0, (max(c.redComponent, c.greenComponent, c.blueComponent) - 0.1) / 0.65))
 mark.setColor(NSColor(deviceRed: c.redComponent, green: c.greenComponent, blue: c.blueComponent, alpha: alpha), atX: x, y: y)
}}
try mark.representation(using: .png, properties: [:])!.write(to: root.appendingPathComponent("app/src/main/res/drawable-nodpi/rotam_mark.png"))
