#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ -n "${DB_URL:-}" ]; then
	if ! command -v mvn >/dev/null 2>&1; then
		echo "DB_URL is set, but Maven is required for the MySQL driver. Use Docker Compose or install Maven."
		exit 1
	fi
	mvn -q package
	exec java --add-modules jdk.httpserver -jar target/store-app.jar "${1:-8080}"
fi
mkdir -p out
find src/main/java -name '*.java' -print > out/sources.list
javac --add-modules jdk.httpserver -d out @out/sources.list
exec java --add-modules jdk.httpserver -cp out com.fieldnote.store.presentation.StoreServer "${1:-8080}"