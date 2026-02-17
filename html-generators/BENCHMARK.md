# Generator Benchmarks

Performance comparison of the three ways to run the HTML generator, measured on 85 snippets across 10 categories.

## Results

| Method | Cold Start | Warm Average | Notes |
|--------|-----------|-------------|-------|
| **Fat JAR** (`java -jar`) | 1.72s | **0.46s** | Fastest warm; no JBang needed |
| **JBang** (`jbang generate.java`) | 0.96s | 0.77s | Includes JBang overhead |
| **Python** (`python3 generate.py`) | 0.17s | 1.37s | Fastest cold start; slowest warm |

- **Cold start**: First run after clearing caches / fresh process
- **Warm average**: Mean of 4 subsequent runs

## Key Takeaways

- The **fat JAR** is the fastest option for repeated builds — **~40% faster** than JBang and **~3× faster** than Python at warm steady-state
- **Python** has the fastest cold start (no JVM boot) but is the slowest overall
- **JBang** adds ~0.3s overhead vs the fat JAR due to its launcher and cache lookup
- For CI/CD, the fat JAR is ideal — no JBang installation or dependency caching required

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
