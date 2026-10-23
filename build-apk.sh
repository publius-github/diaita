#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

BUILD_TYPE="release"
GRADLE_TASK="assembleRelease"
APK_DIR="app/build/outputs/apk/release"
APK_NAME=""

# Where built APKs are collected (kept out of the repo root, gitignored)
DIST_DIR="dist"
# Optional extra copy destination (e.g. a synced folder); set APK_COPY_DIR to enable
COPY_DIR="${APK_COPY_DIR:-}"

for arg in "$@"; do
    case "$arg" in
        ""|release)
            ;;
        debug)
            BUILD_TYPE="debug"
            GRADLE_TASK="assembleDebug"
            APK_DIR="app/build/outputs/apk/debug"
            APK_NAME="app-debug.apk"
            ;;
        -h|--help)
            echo "Usage: ./build-apk.sh [debug|release]"
            echo "Default: release; set APK_COPY_DIR to also copy the APK to that folder"
            exit 0
            ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Usage: ./build-apk.sh [debug|release]" >&2
            exit 2
            ;;
    esac
done

if [[ -x "./gradlew" ]]; then
    GRADLE=("./gradlew")
elif [[ -f "./gradlew" ]]; then
    GRADLE=("bash" "./gradlew")
elif [[ -f "./gradlew.bat" ]] && command -v cmd.exe >/dev/null 2>&1; then
    GRADLE=("cmd.exe" "/c" "gradlew.bat")
else
    echo "Gradle wrapper not found. Run this script from the project root." >&2
    exit 1
fi

echo "Building $BUILD_TYPE APK..."
"${GRADLE[@]}" "$GRADLE_TASK"

if [[ -n "$APK_NAME" ]]; then
    APK_PATH="$APK_DIR/$APK_NAME"
else
    APK_PATH="$(find "$APK_DIR" -maxdepth 1 -name '*.apk' -print -quit 2>/dev/null || true)"
fi

if [[ -n "$APK_PATH" && -f "$APK_PATH" ]]; then
    echo
    echo "APK built successfully: $APK_PATH"
    
    # Extract version from build.gradle.kts
    VERSION=$(grep -oP 'val semVer = "\K[^"]+' app/build.gradle.kts 2>/dev/null || echo "unknown")
    
    # Determine output filename
    if [[ "$BUILD_TYPE" == "release" ]]; then
        if [[ "$APK_PATH" =~ unsigned ]]; then
            OUTPUT_NAME="diaita-${VERSION}-release-unsigned.apk"
        else
            OUTPUT_NAME="diaita-${VERSION}-release.apk"
        fi
    else
        OUTPUT_NAME="diaita-${VERSION}-debug.apk"
    fi
    
    # Copy to the dist subfolder (kept out of the repo root)
    mkdir -p "$ROOT_DIR/$DIST_DIR"
    cp "$APK_PATH" "$ROOT_DIR/$DIST_DIR/$OUTPUT_NAME"
    echo "Copied to: $DIST_DIR/$OUTPUT_NAME"

    # Also copy to APK_COPY_DIR when set
    if [[ -n "$COPY_DIR" ]]; then
        if mkdir -p "$COPY_DIR" 2>/dev/null; then
            cp "$APK_PATH" "$COPY_DIR/$OUTPUT_NAME"
            echo "Copied to: $COPY_DIR/$OUTPUT_NAME"
        else
            echo "Warning: could not access copy dir: $COPY_DIR (skipped)" >&2
        fi
    fi
else
    echo
    echo "Build finished, but no APK was found in: $APK_DIR" >&2
    exit 1
fi