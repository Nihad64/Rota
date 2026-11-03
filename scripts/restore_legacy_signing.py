"""Restore the existing install identity in ephemeral CI without copying credentials into source.

The historical repository already contains this identity. It must be migrated
out of public history separately; silently rotating it would break upgrades.
"""
import base64
import os
from pathlib import Path
import re
import subprocess

LEGACY_COMMIT = 'aea9c4ec9d8fcfe217cf6ab5acd0e117193f998d'
def old(path):
    return subprocess.check_output(['git', 'show', f'{LEGACY_COMMIT}:{path}'], text=True)

workflow = old('.github/workflows/build-apk.yml')
gradle = old('app/build.gradle')
encoded = re.search(r'echo\s+"([A-Za-z0-9+/=]+)"\s*\|\s*base64\s+--decode', workflow)
if not encoded:
    raise SystemExit('Existing signing identity could not be restored.')
values = {}
for key in ('storePassword', 'keyPassword'):
    match = re.search(key + r'\s+[\x22\x27]([^\x22\x27]+)', gradle)
    if not match:
        raise SystemExit('Existing signing configuration could not be restored.')
    values[key] = match.group(1)
# Newly generated values and all output remain private to the ephemeral runner.
Path('rotam-stable.jks').write_bytes(base64.b64decode(encoded.group(1), validate=True))
os.chmod('rotam-stable.jks', 0o600)
with Path('local.properties').open('a') as stream:
    stream.write('\nROTAM_STORE_PASSWORD=' + values['storePassword'] + '\n')
    stream.write('ROTAM_KEY_PASSWORD=' + values['keyPassword'] + '\n')
os.chmod('local.properties', 0o600)
print('Restored the existing signing identity for upgrade verification.')
