#!/bin/sh

set -u

if ! command -v java >/dev/null 2>&1; then
    printf '%s\n' \
        '{"error":"java not found on PATH; install Java 8 or newer and configure PATH"}' >&2
    exit 127
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
java -jar "$script_dir/detect-java-version.jar" "$@"
