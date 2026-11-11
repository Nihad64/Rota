"""Audit the compiled APK's launcher reference graph, not just image presence.

Read-only. Requires the Android SDK's aapt. Does not clear caches, change package
identity, enable/disable components, or access the user's application data.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def xml_nodes(text):
    root = None
    stack = []
    for line in text.splitlines():
        match = re.match(r'(\s*)E: ([^ ]+)', line)
        if match:
            indent = len(match[1])
            node = {'tag': match[2], 'attrs': {}, 'children': []}
            while stack and stack[-1][0] >= indent: stack.pop()
            if stack: stack[-1][1]['children'].append(node)
            else: root = node
            stack.append((indent, node))
        elif stack:
            match = re.match(r'\s*A: (?:android:)?([^ (=]+)(?:\([^)]*\))?=(.*)', line)
            if match:
                value = match[2]
                if value.startswith('"'): value = value.split('"')[1]
                stack[-1][1]['attrs'][match[1]] = value
    if root is None: raise ValueError('No compiled XML root')
    return root


def audit(apk, aapt):
    def dump(*args):
        return subprocess.check_output([str(aapt), 'dump', *args], text=True)
    table = dump('--values', 'resources', str(apk))
    paths = {}
    current = None
    for line in table.splitlines():
        entry = re.match(r'\s*resource (0x[0-9a-f]+) [^:]+:([^: ]+):', line)
        if entry:
            current = entry[1]
            paths.setdefault(current, {'name': entry[2], 'files': []})
        value = re.match(r'\s*\(string(?:8|16)\) "(res/[^\"]+)"', line)
        if value and current: paths[current]['files'].append(value[1])
    def node(path): return xml_nodes(dump('xmltree', str(apk), path))
    manifest = node('AndroidManifest.xml')
    app = next(n for n in manifest['children'] if n['tag'] == 'application')
    launchers = []
    for component in app['children']:
        if component['tag'] not in ('activity', 'activity-alias'): continue
        for intent in component['children']:
            if intent['tag'] != 'intent-filter': continue
            actions = [n['attrs'].get('name') for n in intent['children'] if n['tag'] == 'action']
            categories = [n['attrs'].get('name') for n in intent['children'] if n['tag'] == 'category']
            if 'android.intent.action.MAIN' in actions and 'android.intent.category.LAUNCHER' in categories:
                launchers.append(component)
    assert manifest['attrs']['package'] == 'com.rota.app', 'Upgrade package identity changed'
    assert len(launchers) == 1, f'Expected one launcher, found {len(launchers)}'
    assert launchers[0]['attrs']['name'] == 'com.rota.app.MainActivity', 'Legacy launcher identity missing'
    expected = {n: hashlib.sha256((ROOT/'app/src/main/res/drawable-nodpi'/n).read_bytes()).hexdigest()
                for n in ['rotam_launcher_art.png', 'rotam_launcher_mask.png']}
    checks = []
    with zipfile.ZipFile(apk) as archive:
        def files(reference):
            assert reference and reference.startswith('@0x'), f'Invalid resource reference: {reference}'
            entry = paths.get(reference[1:])
            assert entry and entry['files'], f'No resource files for {reference}'
            return sorted(set(entry['files']))
        def approved(reference, name):
            for path in files(reference):
                if path.endswith('.xml'):
                    bitmap = node(path)
                    if bitmap['tag']=='vector' and name=='rotam_launcher_art.png':
                        source=ET.parse(ROOT/'app/src/main/res/drawable/ic_launcher_foreground.xml').getroot()
                        ns='{http://schemas.android.com/apk/res/android}'
                        expected_paths=[p for p in source if p.tag=='path']
                        actual_paths=[p for p in bitmap['children'] if p['tag']=='path']
                        assert len(expected_paths)==len(actual_paths)==2, 'R and diamond paths required'
                        for expected_path,actual_path,color in zip(expected_paths,actual_paths,['0xffffffff','0xffff803f']):
                            assert actual_path['attrs'].get('pathData')==expected_path.get(ns+'pathData'), 'Stale native logo silhouette'
                            assert actual_path['attrs'].get('fillColor','').endswith(color), 'Wrong native logo color'
                    else:
                        assert bitmap['tag'] == 'bitmap', f'Unexpected drawable: {path}'
                        approved(bitmap['attrs'].get('src'), name)
                else:
                    actual = hashlib.sha256(archive.read(path)).hexdigest()
                    assert actual == expected[name], f'Stale artwork: {path}'
        roots = {'application': app['attrs'].get('icon'), 'round': app['attrs'].get('roundIcon'),
                 'launcher': launchers[0]['attrs'].get('icon', app['attrs'].get('icon'))}
        for role, reference in roots.items():
            for path in files(reference):
                adaptive = node(path)
                assert adaptive['tag'] == 'adaptive-icon', f'Unexpected {role} icon: {path}'
                layers = {n['tag']: n['attrs'].get('drawable') for n in adaptive['children']}
                approved(layers.get('foreground'), 'rotam_launcher_art.png')
                approved(layers.get('monochrome'), 'rotam_launcher_mask.png')
                checks.append({'role': role, 'resource': path, 'foreground': 'approved', 'monochrome': 'approved'})
    return {'apk': str(apk), 'package': manifest['attrs']['package'], 'version': manifest['attrs'].get('versionName'),
            'launcher': launchers[0]['attrs']['name'], 'checks': checks,
            'limit': 'APK references verified; Samsung theme/cache and the installed device APK are not inspected.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--aapt', type=Path)
    args = parser.parse_args()
    sdk = Path(os.environ.get('ANDROID_HOME', ROOT/'.toolchain/android-sdk'))
    candidates = sorted(sdk.glob('build-tools/*/aapt'))
    aapt = args.aapt or shutil.which('aapt') or (candidates[-1] if candidates else None)
    if not aapt: parser.error('aapt unavailable; use scripts/with-android.sh or --aapt')
    print(json.dumps(audit(args.apk, aapt), ensure_ascii=False, indent=2))


if __name__ == '__main__': main()
