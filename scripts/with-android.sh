#!/bin/sh
# Keep build caches and tools out of /tmp and the user's system settings.
set -eu
ROTAM_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
export JAVA_HOME="$ROTAM_ROOT/.toolchain/jdk/Contents/Home"
export ANDROID_HOME="$ROTAM_ROOT/.toolchain/android-sdk"
export GRADLE_USER_HOME="$ROTAM_ROOT/.toolchain/gradle"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo 'Android tools missing. Run python3 scripts/setup_android.py --install first.' >&2
  exit 1
fi
exec "$@"
