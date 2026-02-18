# Generator Benchmarks

Performance comparison of the four ways to run the HTML generator, measured on 95 snippets across 10 categories.

## Results

| Method | Cold Start | Warm Average | Notes |
|--------|-----------|-------------|-------|
| **Fat JAR + AOT** (`java -XX:AOTCache`) | 0.47s | **0.43s** | Fastest overall; requires one-time cache build |
| **Fat JAR** (`java -jar`) | 0.43s | 0.55s | No setup needed |
| **JBang** (`jbang generate.java`) | 0.93s | 0.98s | Includes JBang overhead |
| **Python** (`python3 generate.py`) | 0.37s | 1.60s | Fast cold start; slowest warm |

- **Cold start**: First run after clearing caches / fresh process
- **Warm average**: Mean of 5 subsequent runs

## AOT Cache Setup

```bash
# One-time: build the cache (~21 MB, platform-specific)
java -XX:AOTCacheOutput=html-generators/generate.aot -jar html-generators/generate.jar

# Use it
java -XX:AOTCache=html-generators/generate.aot -jar html-generators/generate.jar
```

The AOT cache uses Java 25 CDS (JEP 483) to pre-load classes from a training run. It is platform-specific (CPU arch + JDK version).

## Environment

| | |
|---|---|
| **CPU** | Apple M1 Max |
| **RAM** | 32 GB |
| **Java** | OpenJDK 25.0.1 (Temurin) |
| **JBang** | 0.136.0 |
| **Python** | 3.14.3 |
| **OS** | Darwin |

## Methodology

Each method was timed 6 times using `/usr/bin/time -p`. The first run is reported as "cold start" and the remaining 5 runs are averaged for "warm average". Between each run, `site/index.html` was reset via `git checkout` to ensure the generator runs fully each time.

## Reproduce

```bash
./html-generators/benchmark/run.sh            # print results to stdout
./html-generators/benchmark/run.sh --update    # also update this file
```
