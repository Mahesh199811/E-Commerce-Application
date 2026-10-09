#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
find src/main/java -name '*.java' -print > out/sources.list
javac --add-modules jdk.httpserver -d out @out/sources.list
exec java --add-modules jdk.httpserver -cp out com.fieldnote.store.presentation.StoreServer "${1:-8080}"