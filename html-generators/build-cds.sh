#!/usr/bin/env bash
# Build an App CDS / AOT cache for the generator fat JAR.
# The cache is platform-specific and must be regenerated when
# the JAR or JDK version changes.
#
# Usage:
#   ./html-generators/build-cds.sh          # creates generate.aot
#   java -XX:AOTCache=html-generators/generate.aot -jar html-generators/generate.jar

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

JAR="html-generators/generate.jar"
AOT="html-generators/generate.aot"

echo "Training run + AOT cache generation..."
java -XX:AOTCacheOutput="$AOT" -jar "$JAR"
echo "Done: $AOT ($(du -h "$AOT" | cut -f1))"
echo ""
echo "Run with:  java -XX:AOTCache=$AOT -jar $JAR"
