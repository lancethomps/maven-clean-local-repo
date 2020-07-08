#!/usr/bin/env bash
if [[ "$TRAVIS_OS_NAME" == "osx" ]]; then DOWNLOAD_OS_NAME="darwin"; fi
if [[ "$TRAVIS_OS_NAME" == "linux" ]]; then DOWNLOAD_OS_NAME="linux"; fi

GRAALVM_BASE_DIR="$HOME/graalvm"
GRAALVM_DIR="$GRAALVM_BASE_DIR/graalvm-ce-java11-$GRAALVM_VERSION"

if ! test -e "$GRAALVM_DIR"; then
  mkdir -pv "$GRAALVM_BASE_DIR"
  curl -LJ "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$GRAALVM_VERSION/graalvm-ce-java11-$DOWNLOAD_OS_NAME-amd64-$GRAALVM_VERSION.tar.gz" --output "$GRAALVM_BASE_DIR/graalvm.tar.gz"
  cd "$GRAALVM_BASE_DIR"
  tar -xzf graalvm.tar.gz
  "$HOME/graalvm/graalvm-ce-java11-$GRAALVM_VERSION/bin/gu" install native-image
else
  true
fi
