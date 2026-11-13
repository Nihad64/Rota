"""Read installed Rotam APK and launcher metadata over an authorized ADB connection.

No uninstall, app-data access, cache clearing or launcher/settings changes.
The pulled APK and report remain in ignored .toolchain/device-audit/.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
from audit_launcher import audit, ROOT


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True, help='Explicit ADB device serial; never choose an arbitrary device')
    args = parser.parse_args()
    sdk = Path(os.environ.get('ANDROID_HOME', ROOT/'.toolchain/android-sdk'))
    adb = str(sdk/'platform-tools/adb')
    aapt = sdk/'build-tools/35.0.0/aapt'
    def read(*command): return subprocess.check_output([adb, '-s', args.serial, *command], text=True).strip()
    if read('get-state') != 'device': raise SystemExit('Authorize the USB debugging connection on the phone first.')
    destination = ROOT/'.toolchain/device-audit'
    destination.mkdir(parents=True, exist_ok=True)
    report = {'model': read('shell', 'getprop', 'ro.product.model'),
              'android': read('shell', 'getprop', 'ro.build.version.release'),
              'samsung_icon_theme': read('shell', 'settings', 'get', 'system', 'current_sec_appicon_theme_package'),
              'home': read('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME'),
              'rotam_launcher': read('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER', 'com.rota.app')}
    locations = read('shell', 'pm', 'path', 'com.rota.app').splitlines()
    if not locations: raise SystemExit('com.rota.app is not installed. Nothing was changed.')
    base = next((v.removeprefix('package:') for v in locations if v.endswith('/base.apk')), None)
    if base is None: raise SystemExit('No base APK found. Nothing was changed.')
    apk = destination/'installed-rotam.apk'
    subprocess.run([adb, '-s', args.serial, 'pull', base, str(apk)], check=True)
    badging = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
    report['package_metadata'] = next(line for line in badging.splitlines() if line.startswith('package:'))
    try:
        report['artwork_audit'] = audit(apk, aapt)
        report['artwork_audit']['limit'] = 'Installed APK verified. On-screen launcher rendering still needs visual confirmation.'
    except (AssertionError, ValueError, subprocess.CalledProcessError) as error:
        report['artwork_audit_error'] = str(error)
    output = destination/'report.json'
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(output)
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__': main()
