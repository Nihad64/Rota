"""Install only the Android build tools in a persistent, ignored project directory.

No emulator, system image, Android Studio, Homebrew or system-wide Java changes.
Downloads are checksum verified and reused. Run without --install for a status check.
"""
import argparse
import hashlib
from pathlib import Path
import subprocess
import tarfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.toolchain'
DOWNLOADS = TOOLS / 'downloads'
JDK_URL = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.21_9.tar.gz'
JDK_SHA = '78b08c7f5a0598242d5719c022e19479cb4a3aa45c37c19726a757a469ed1c15'
SDK_URL = 'https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip'
SDK_SHA1 = 'c3e06a1959762e89167d1cbaa988605f6f7c1d24'


def download(url, name, algorithm, expected):
    DOWNLOADS.mkdir(parents=True, exist_ok=True)
    path = DOWNLOADS / name
    def valid(p):
        if not p.is_file(): return False
        digest = hashlib.new(algorithm)
        with p.open('rb') as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b''): digest.update(block)
        return digest.hexdigest() == expected
    if valid(path): return path
    partial = path.with_suffix(path.suffix + '.partial')
    subprocess.run(['curl', '--fail', '--location', '--retry', '2', '--connect-timeout', '20', '--output', str(partial), url], check=True)
    if not valid(partial): raise RuntimeError(f'Checksum mismatch: {name}')
    partial.replace(path)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', action='store_true')
    args = parser.parse_args()
    java = TOOLS / 'jdk/Contents/Home/bin/java'
    manager = TOOLS / 'android-sdk/cmdline-tools/19.0/bin/sdkmanager'
    if not args.install:
        for name, path in [('Java 17', java), ('SDK manager', manager), ('ADB', TOOLS/'android-sdk/platform-tools/adb'), ('SDK 35', TOOLS/'android-sdk/platforms/android-35/android.jar')]:
            print(f'{name}: {"available" if path.is_file() else "missing"}')
        return
    if subprocess.check_output(['uname', '-sm'], text=True).strip() != 'Darwin arm64':
        raise RuntimeError('This bootstrap is pinned to Apple Silicon macOS.')
    if not java.exists():
        archive = download(JDK_URL, 'temurin-17.0.21-mac-arm64.tar.gz', 'sha256', JDK_SHA)
        staging = TOOLS / 'jdk-unpack'
        staging.mkdir(exist_ok=True)
        with tarfile.open(archive) as source: source.extractall(staging, filter='data')
        directory = next(staging.glob('jdk-*'))
        directory.rename(TOOLS/'jdk')
        staging.rmdir()
    if not manager.exists():
        archive = download(SDK_URL, 'android-commandline-tools-19.zip', 'sha1', SDK_SHA1)
        destination = TOOLS/'android-sdk/cmdline-tools'
        destination.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(archive) as source: source.extractall(destination)
        (destination/'cmdline-tools').rename(destination/'19.0')
        for path in (destination/'19.0/bin').iterdir(): path.chmod(0o755)
    import os
    env = dict(os.environ, JAVA_HOME=str(java.parents[1]), ANDROID_HOME=str(TOOLS/'android-sdk'))
    logs = TOOLS/'logs'
    logs.mkdir(exist_ok=True)
    required = {
        'platform-tools': TOOLS/'android-sdk/platform-tools/adb',
        'platforms;android-35': TOOLS/'android-sdk/platforms/android-35/android.jar',
        'build-tools;35.0.0': TOOLS/'android-sdk/build-tools/35.0.0/aapt',
    }
    missing = [name for name, path in required.items() if not path.is_file()]
    if missing:
        with (logs/'sdk-licenses.log').open('w') as log:
            subprocess.run([str(manager), f'--sdk_root={TOOLS/"android-sdk"}', '--licenses'], input='y\n'*100, text=True, stdout=log, stderr=subprocess.STDOUT, env=env, check=True)
        with (logs/'sdk-install.log').open('w') as log:
            subprocess.run([str(manager), f'--sdk_root={TOOLS/"android-sdk"}', *missing], stdout=log, stderr=subprocess.STDOUT, env=env, check=True)
    # Preserve signing-related local settings; change only this machine's SDK path.
    local = ROOT/'local.properties'
    lines = local.read_text().splitlines() if local.exists() else []
    lines = [line for line in lines if not line.startswith('sdk.dir=')]
    local.write_text('\n'.join(lines+[f'sdk.dir={TOOLS/"android-sdk"}'])+'\n')
    print('Persistent Android build tools are ready. Use scripts/with-android.sh ./gradlew ...')


if __name__ == '__main__': main()
