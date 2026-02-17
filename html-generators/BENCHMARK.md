# Generator Benchmarks

Performance comparison of the four ways to run the HTML generator, measured on 85 snippets across 10 categories.

## Results

| Method | Cold Start | Warm Average | Notes |
|--------|-----------|-------------|-------|
| **Fat JAR + AOT** (`java -XX:AOTCache`) | 0.27s | **0.27s** | Fastest; requires one-time cache build |
| **Fat JAR** (`java -jar`) | 1.72s | 0.46s | No setup needed |
| **JBang** (`jbang generate.java`) | 0.96s | 0.77s | Includes JBang overhead |
| **Python** (`python3 generate.py`) | 0.17s | 1.37s | Fastest cold start; slowest warm |

- **Cold start**: First run after clearing caches / fresh process
- **Warm average**: Mean of 4 subsequent runs (AOT: mean of 5, no cold penalty)

## Key Takeaways

- The **fat JAR + AOT cache** is the fastest option — **~40% faster** than the plain fat JAR and **~5× faster** than Python
- The AOT cache eliminates the JVM cold start penalty entirely (0.27s cold = 0.27s warm)
- **Python** has the fastest cold start (no JVM boot) but is the slowest overall
- **JBang** adds ~0.3s overhead vs the fat JAR due to its launcher and cache lookup
- For CI/CD, the plain fat JAR is ideal — no AOT build step needed, and still very fast

## AOT Cache Setup

```bash
# One-time: build the cache (~21 MB, platform-specific)
./html-generators/build-cds.sh

# Use it
java -XX:AOTCache=html-generators/generate.aot -jar html-generators/generate.jar
```

The AOT cache uses Java 25 CDS (JEP 514/515) to pre-load ~3,300 classes from a training run. It is platform-specific (CPU arch + JDK version) and not committed to git.

## Environment

| | |
|---|---|
| **CPU** | Apple M1 Max |
| **RAM** | 32 GB |
| **Java** | OpenJDK 25.0.1 (Temurin) |
| **JBang** | 0.136.0 |
| **Python** | 3.14.3 |
| **OS** | macOS (Darwin) |

## Methodology

Each method was warmed up once, then timed 5 times using `/usr/bin/time -p`. The first run is reported as "cold start" and the remaining 4 runs are averaged for "warm average". Between each run, `site/index.html` was reset via `git checkout` to ensure the `{{snippetCount}}` patch runs each time.
