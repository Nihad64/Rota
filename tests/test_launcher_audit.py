"""Verify lossless release PNG optimization cannot hide an icon change."""
import importlib.util
from pathlib import Path
import struct
import unittest
import zlib

spec = importlib.util.spec_from_file_location('launcher_audit', Path(__file__).parents[1]/'scripts/audit_launcher.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def chunk(kind, data):
    return struct.pack('>I', len(data))+kind+data+struct.pack('>I', zlib.crc32(kind+data))


def png(color, row, palette=b'', alpha=b''):
    header = struct.pack('>IIBBBBB', 2, 1, 8, color, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR', header)
            +(chunk(b'PLTE', palette) if palette else b'')
            +(chunk(b'tRNS', alpha) if alpha else b'')
            +chunk(b'IDAT', zlib.compress(b'\0'+row))+chunk(b'IEND', b''))


class LauncherPixelTests(unittest.TestCase):
    def test_palette_optimization_preserves_the_displayed_mask(self):
        source = png(6, bytes([255,255,255,255, 127,77,22,0]))
        optimized = png(3, bytes([0,1]), bytes([255,255,255, 0,0,0]), bytes([255,0]))
        self.assertNotEqual(source, optimized)
        self.assertEqual(audit.png_pixels(source), audit.png_pixels(optimized))

    def test_a_visible_cream_pixel_cannot_pass_as_white(self):
        white = png(6, bytes([255,255,255,255])*2)
        cream = png(3, bytes([0,0]), bytes([248,238,217]))
        self.assertNotEqual(audit.png_pixels(white), audit.png_pixels(cream))

    def test_alpha_changes_are_not_ignored(self):
        white = png(6, bytes([255,255,255,255])*2)
        translucent = png(6, bytes([255,255,255,254])*2)
        self.assertNotEqual(audit.png_pixels(white), audit.png_pixels(translucent))

    def test_unsupported_png_is_rejected(self):
        with self.assertRaises(AssertionError):
            audit.png_pixels(png(2, b'\0'*6))


if __name__ == '__main__': unittest.main()
