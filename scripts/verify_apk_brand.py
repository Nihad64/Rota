"""Ensure the APK ships the exact approved-logo derivatives, not stale vectors."""
import hashlib
from pathlib import Path
import sys
import zipfile
apk = Path(sys.argv[1] if len(sys.argv)>1 else 'app/build/outputs/apk/debug/app-debug.apk')
with zipfile.ZipFile(apk) as archive:
    for name in ('rotam_launcher_art.png', 'rotam_launcher_mask.png'):
        matches = [p for p in archive.namelist() if p.startswith('res/drawable-nodpi') and p.endswith('/'+name)]
        assert len(matches)==1, f'Missing or ambiguous launcher asset: {name}'
        expected = Path('app/src/main/res/drawable-nodpi', name).read_bytes()
        assert hashlib.sha256(archive.read(matches[0])).digest()==hashlib.sha256(expected).digest(), f'Stale icon: {name}'
    for name in archive.namelist():
        if name.endswith('.dex'):
            assert b'AIzaSy' not in archive.read(name), 'Possible Google secret found in DEX; review before shipping'
print('Approved raster reference assets preserved; audit_launcher.py verifies the active native foreground. No Gemini key signature found in DEX.')
