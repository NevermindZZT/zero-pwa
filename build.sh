#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$DIR/.android}"
export XDG_DATA_HOME="${XDG_DATA_HOME:-$DIR/.xdg-data}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$DIR/.gradle-home}"
export PATH="${JAVA_HOME}/bin:$PATH"

echo "=== Zero PWA 构建环境 ==="
echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Gradle: $(./gradlew --version 2>&1 | grep 'Gradle ')"

TASK="${1:-assembleDebug}"
echo "Executing task: $TASK ..."
./gradlew "$TASK" -Dandroid.user.home="$DIR/.android" --no-daemon
