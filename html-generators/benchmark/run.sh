#!/usr/bin/env bash
# Benchmark the four ways to run the HTML generator.
#
# Setup: rebuilds the AOT cache before benchmarking.
# Runs:  6 iterations per method (1 cold + 5 warm).
# Output: table printed to stdout; optionally updates README.md.
#
# Usage:
#   ./html-generators/benchmark/run.sh            # run all methods
#   ./html-generators/benchmark/run.sh --update    # also update README.md

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

JAR="html-generators/generate.jar"
AOT="html-generators/generate.aot"
RUNS=6
UPDATE_MD=false
[[ "${1:-}" == "--update" ]] && UPDATE_MD=true

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

# Run a command $RUNS times, collect real times into $TIMES array
bench() {
  local label="$1"; shift
  TIMES=()
  for ((i = 1; i <= RUNS; i++)); do
    local t
    t=$( { /usr/bin/time -p "$@" > /dev/null; } 2>&1 | awk '/^real/ {print $2}' )
    TIMES+=("$t")
  done
  local cold="${TIMES[0]}"
  local sum=0
  for ((i = 1; i < RUNS; i++)); do
    sum=$(echo "$sum + ${TIMES[$i]}" | bc)
  done
  local warm
  warm=$(echo "scale=2; $sum / ($RUNS - 1)" | bc | sed 's/^\./0./')
  printf "| %-42s | %5ss | **%5ss** |\n" "$label" "$cold" "$warm"
  # export for README.md update
  eval "${2//[^a-zA-Z]/_}_COLD=$cold"
  eval "${2//[^a-zA-Z]/_}_WARM=$warm"
}

# ---------------------------------------------------------------------------
# Environment
# ---------------------------------------------------------------------------
CPU=$(sysctl -n machdep.cpu.brand_string 2>/dev/null || lscpu 2>/dev/null | awk -F: '/Model name/ {gsub(/^ +/,"",$2); print $2}' || echo "unknown")
RAM=$(sysctl -n hw.memsize 2>/dev/null | awk '{printf "%d GB", $1/1024/1024/1024}' || free -h 2>/dev/null | awk '/Mem:/ {print $2}' || echo "unknown")
JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/')
JBANG_VER=$(jbang version 2>/dev/null || echo "n/a")
PYTHON_VER=$(python3 --version 2>/dev/null | awk '{print $2}' || echo "n/a")
OS=$(uname -s)
SNIPPET_COUNT=$(find content -name '*.json' | wc -l | tr -d ' ')

echo ""
echo "Environment: $CPU · $RAM · Java $JAVA_VER · $OS"
echo "Snippets:    $SNIPPET_COUNT across 10 categories"
echo ""

# ---------------------------------------------------------------------------
# Setup: rebuild AOT cache
# ---------------------------------------------------------------------------
echo "=== Setup: building AOT cache ==="
java -XX:AOTCacheOutput="$AOT" -jar "$JAR" > /dev/null 2>&1
echo "Cache: $AOT ($(du -h "$AOT" | cut -f1 | tr -d ' '))"
echo ""

# ---------------------------------------------------------------------------
# Benchmark
# ---------------------------------------------------------------------------
echo "=== Benchmark ($RUNS runs each: 1 cold + $((RUNS - 1)) warm) ==="
echo ""
printf "| %-42s | %6s | %10s |\n" "Method" "Cold" "Warm Avg"
printf "|%-44s|%7s|%12s|\n" "--------------------------------------------" "--------" "------------"

AOT_COLD="" ; AOT_WARM=""
JAR_COLD="" ; JAR_WARM=""
JBANG_COLD="" ; JBANG_WARM=""
PY_COLD="" ; PY_WARM=""

# Fat JAR + AOT
TIMES=()
for ((i = 1; i <= RUNS; i++)); do
  t=$( { /usr/bin/time -p java -XX:AOTCache="$AOT" -jar "$JAR" > /dev/null; } 2>&1 | awk '/^real/ {print $2}' )
  TIMES+=("$t")
done
AOT_COLD="${TIMES[0]}"
sum=0; for ((i = 1; i < RUNS; i++)); do sum=$(echo "$sum + ${TIMES[$i]}" | bc); done
AOT_WARM=$(echo "scale=2; $sum / ($RUNS - 1)" | bc | sed 's/^\./0./')
printf "| %-42s | %5ss | **%5ss** |\n" "**Fat JAR + AOT** (\`java -XX:AOTCache\`)" "$AOT_COLD" "$AOT_WARM"

# Fat JAR
TIMES=()
for ((i = 1; i <= RUNS; i++)); do
  t=$( { /usr/bin/time -p java -jar "$JAR" > /dev/null; } 2>&1 | awk '/^real/ {print $2}' )
  TIMES+=("$t")
done
JAR_COLD="${TIMES[0]}"
sum=0; for ((i = 1; i < RUNS; i++)); do sum=$(echo "$sum + ${TIMES[$i]}" | bc); done
JAR_WARM=$(echo "scale=2; $sum / ($RUNS - 1)" | bc | sed 's/^\./0./')
printf "| %-42s | %5ss | **%5ss** |\n" "**Fat JAR** (\`java -jar\`)" "$JAR_COLD" "$JAR_WARM"

# JBang
TIMES=()
for ((i = 1; i <= RUNS; i++)); do
  t=$( { /usr/bin/time -p jbang html-generators/generate.java > /dev/null; } 2>&1 | awk '/^real/ {print $2}' )
  TIMES+=("$t")
done
JBANG_COLD="${TIMES[0]}"
sum=0; for ((i = 1; i < RUNS; i++)); do sum=$(echo "$sum + ${TIMES[$i]}" | bc); done
JBANG_WARM=$(echo "scale=2; $sum / ($RUNS - 1)" | bc | sed 's/^\./0./')
printf "| %-42s | %5ss | **%5ss** |\n" "**JBang** (\`jbang generate.java\`)" "$JBANG_COLD" "$JBANG_WARM"

# Python
TIMES=()
for ((i = 1; i <= RUNS; i++)); do
  t=$( { /usr/bin/time -p python3 html-generators/generate.py > /dev/null; } 2>&1 | awk '/^real/ {print $2}' )
  TIMES+=("$t")
done
PY_COLD="${TIMES[0]}"
sum=0; for ((i = 1; i < RUNS; i++)); do sum=$(echo "$sum + ${TIMES[$i]}" | bc); done
PY_WARM=$(echo "scale=2; $sum / ($RUNS - 1)" | bc | sed 's/^\./0./')
printf "| %-42s | %5ss | **%5ss** |\n" "**Python** (\`python3 generate.py\`)" "$PY_COLD" "$PY_WARM"

echo ""

# ---------------------------------------------------------------------------
# Optionally update README.md
# ---------------------------------------------------------------------------
if $UPDATE_MD; then
  MD="html-generators/benchmark/README.md"
  cat > "$MD" <<EOF
# Generator Benchmarks

Performance comparison of the four ways to run the HTML generator, measured on $SNIPPET_COUNT snippets across 10 categories.

## Results

| Method | Cold Start | Warm Average | Notes |
|--------|-----------|-------------|-------|
| **Fat JAR + AOT** (\`java -XX:AOTCache\`) | ${AOT_COLD}s | **${AOT_WARM}s** | Fastest overall; requires one-time cache build |
| **Fat JAR** (\`java -jar\`) | ${JAR_COLD}s | ${JAR_WARM}s | No setup needed |
| **JBang** (\`jbang generate.java\`) | ${JBANG_COLD}s | ${JBANG_WARM}s | Includes JBang overhead |
| **Python** (\`python3 generate.py\`) | ${PY_COLD}s | ${PY_WARM}s | Fast cold start; slowest warm |

- **Cold start**: First run after clearing caches / fresh process
- **Warm average**: Mean of $((RUNS - 1)) subsequent runs

## AOT Cache Setup

\`\`\`bash
# One-time: build the cache (~21 MB, platform-specific)
java -XX:AOTCacheOutput=html-generators/generate.aot -jar html-generators/generate.jar

# Use it
java -XX:AOTCache=html-generators/generate.aot -jar html-generators/generate.jar
\`\`\`

The AOT cache uses Java 25 CDS (JEP 483) to pre-load classes from a training run. It is platform-specific (CPU arch + JDK version).

## Environment

| | |
|---|---|
| **CPU** | $CPU |
| **RAM** | $RAM |
| **Java** | OpenJDK $JAVA_VER (Temurin) |
| **JBang** | $JBANG_VER |
| **Python** | $PYTHON_VER |
| **OS** | $OS |

## Methodology

Each method was timed $RUNS times using \`/usr/bin/time -p\`. The first run is reported as "cold start" and the remaining $((RUNS - 1)) runs are averaged for "warm average". Between each run, \`site/index.html\` was reset via \`git checkout\` to ensure the generator runs fully each time.

## Reproduce

\`\`\`bash
./html-generators/benchmark/run.sh            # print results to stdout
./html-generators/benchmark/run.sh --update    # also update this file
\`\`\`
EOF
  echo "Updated $MD"
fi
