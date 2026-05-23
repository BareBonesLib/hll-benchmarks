# hll-benchmarks

A comprehensive, reproducible benchmark suite comparing HyperLogLog and related cardinality sketch implementations across Java and native C++.

**[→ View Live Results Dashboard](https://bareboneslib.github.io/hll-benchmarks/)**

---

## Why This Exists

Choosing a cardinality sketch for production use is harder than it should be. Published comparisons tend to be single-library, test only one or two dimensions (usually just accuracy), or are tied to benchmark harnesses that are difficult to reproduce. This project attempts something more complete: a neutral, side-by-side comparison of the major sketch implementations available in the Java ecosystem and in native C++, measured across all the operations that actually matter in distributed pipelines.

---

## What's Benchmarked

### Java

Run under Java 1.8 (HotSpot 64-bit Server VM).

| Library | Sketch | Version | Variant |
|---|---|---|---|
| [BareBonesHLL](https://github.com/BareBonesLib/barebones-hll) | HyperLogLog++ | `0.5.0` | precision 4, 5, 6 |
| [BareBonesHLL](https://github.com/BareBonesLib/barebones-hll) | HyperLogLog (classic) | `0.5.0` | precision 4, 5, 6 |
| [Apache DataSketches](https://datasketches.apache.org/) | HyperLogLog | `5.0.0` | lgK 4, 6, 8 |
| [Apache DataSketches](https://datasketches.apache.org/) | CPC Sketch | `5.0.0` | — |
| [Apache DataSketches](https://datasketches.apache.org/) | Theta Sketch | `5.0.0` | — |
| [AGKN (net.agkn)](https://github.com/aggregateknowledge/java-hll) | HyperLogLog | `1.6.0` | register width 4, 6, 8 |
| [stream-lib (clearspring)](https://github.com/addthis/stream-lib) | HyperLogLog | `2.9.8` | — |
| [stream-lib (clearspring)](https://github.com/addthis/stream-lib) | HyperLogLog++ | `2.9.8` | sparse precision 12, 14, 16, 18, 20, 22, 24 |
| [LiveRamp HyperMinHash](https://github.com/LiveRamp/HyperMinHash-java) | HyperMinHash | `0.2` | register width 4, 6, 8 |
| [LiveRamp HyperMinHash](https://github.com/LiveRamp/HyperMinHash-java) | BetaMinHash | `0.2` | — |
| [Google ZetaSketch](https://github.com/google/zetasketch) | ZetaSketch (HLL++) | `0.1.0` | — |

> **Note:** `hash4j` (Dynatrace UltraLogLog and HyperLogLog) is not available on Java 1.8 and is excluded from this benchmark run. The wrapper code is present in the repo for future runs on Java 11+.

### C++ (Native)

Compiled with `clang++ 16 -std=c++17 -O3 -march=native` on Apple Silicon.

| Library | Sketch | Version | Variant |
|---|---|---|---|
| [BareBonesHLL](https://github.com/BareBonesLib/barebones-hll) | HyperLogLog++ | `0.5.0` | precision 4, 5, 6 |
| [Apache DataSketches C++](https://github.com/apache/datasketches-cpp) | HyperLogLog | `5.3.20251113` | lgK 4, 6, 8 |
| [Apache DataSketches C++](https://github.com/apache/datasketches-cpp) | CPC Sketch | `5.3.20251113` | — |
| [Apache DataSketches C++](https://github.com/apache/datasketches-cpp) | Theta Sketch | `5.3.20251113` | — |

> In BareBonesHLL, all variants — whether labeled `hll` or `hllpp` — are HyperLogLog++ implementations. There is no separate classic HLL implementation in library.

---

## Methodology

Each sketch is tested by building an array of `N` independent sketch instances, each pre-loaded with a fixed number of distinct UUID elements. The element counts are swept in powers of two — e.g. `1, 2, 4, ..., 1,048,576` — to observe how each metric scales with cardinality.

The five operations measured, reported as average nanoseconds per operation:

- **Update** — insert one element into a pre-populated sketch
- **Merge** — merge one sketch into another (union)
- **Serialize** — produce a compact byte representation
- **Deserialize** — reconstruct a sketch from bytes
- **Cardinality** — read the current estimate

In addition to timing, the benchmark measures:

- **Error rate** — relative error between the sketch's estimate and true cardinality: `|estimate − actual| / actual × 100%`
- **Serialized sketch size** — min and max byte size of a sketch at each cardinality level
- **JVM heap usage** (Java only) — memory snapshots taken at regular intervals throughout the test via a background monitor thread

### Distributed Environment Projection (Formula Model)

Raw per-operation latencies can be misleading without context. The dashboard includes an interactive chart that projects the total time a sketch would consume in a distributed job (e.g. Spark), using this formula:

```
Total = R×avg(update) + K×(R/numTasks)×avg(merge) + SW×avg(serialize) + SW×avg(deserialize) + SW×avg(merge) + W×avg(cardinality)
```

Where:
- **R** — total number of records (inputs) across the job
- **numTasks** — number of map-side parallel tasks
- **K** — average number of sketches merged per task (≈ records/tasks, default: 3.0197 for the preset run)
- **SW** — number of shuffle-write operations (serialize + send + deserialize + merge at reduce side)
- **W** — number of cardinality reads (write-side final estimation)

This decomposes total cost into six stacked components — map-side updates, map-side partial merges, shuffle serialization, shuffle deserialization, reduce-side merges, and final cardinality reads — so you can see which operation dominates for a given workload shape. All parameters are adjustable in the dashboard.

---

## Benchmark Results

All results are published on the [live dashboard](https://bareboneslib.github.io/hll-benchmarks/) with interactive charts. Two preset runs are included:

| Run | Platform | Runtime |
|---|---|---|
| Benchmark #1 | Apple M3 Pro, 18 GB LPDDR5, macOS 14.8.2 | Java HotSpot 1.8.0_421, `-Xmx8g` |
| Benchmark #2 | Apple M3 Pro, 18 GB LPDDR5, macOS 14.8.2 | Apple clang++ 16.0.0, `-O3 -march=native` |

### Summary

The BareBonesHLL library consistently outperforms competing implementations across most of the five operations at every cardinality level, while maintaining comparable or better accuracy. The distributed projection chart makes this clearest: at realistic Spark-scale workloads involving billions of records and terabytes of total data, the per-operation cost differences compound significantly, and sketch choice has a measurable impact on total job time.

---

## Repository Structure

```
.
├── cpp/                    # Native C++ benchmark harness
│   ├── src/
│   │   ├── implementations/    # Sketch wrappers (BareBonesHLL, DataSketches, CPC, Theta)
│   │   ├── SketchTestRunner.*  # Core benchmark logic
│   │   └── main.cpp
│   └── makefile
├── java/                   # JVM benchmark harness
│   ├── src/main/java/.../
│   │   ├── SketchBase.java         # All sketch wrappers + SketchType enum
│   │   ├── SketchTestRunner.java   # Core benchmark logic
│   │   ├── SketchPerformanceTest.java  # Entry point
│   │   └── MemoryMonitor.java      # Background heap sampler
│   └── pom.xml
└── docs/                   # GitHub Pages dashboard
    ├── index.html
    └── benchmarks/
        ├── metadata.json
        └── json/           # Benchmark result files (1.json, 2.json, ...)
```

---

## Running Locally

### Java

**Requirements:** Java 8, Maven 3.x

```bash
cd java
mvn package -q

# Run with settings (10k iterations, sizes 1–131K, lgK=12), higher sizes takes more time
java -Xmx8g -jar target/hll_benchmarks-1.0.0-SNAPSHOT.jar \
  --iterations 10000 \
  --sizes 1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072 \
  --lgk 12 \
  > results.json
```

### C++

**Requirements:** `g++` or `clang++` with C++17, [BareBonesHLL C++](https://github.com/BareBonesLib/barebones-hll) and [Apache DataSketches C++](https://github.com/apache/datasketches-cpp) installed (e.g. via Homebrew), `xxhash`

```bash
cd cpp
make

./build/benchmark_runner \
  --iterations 10000 \
  --sizes 1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072 \
  --lgk 12 \
  > results.json
```

### Viewing Results in the Dashboard

The dashboard accepts results in three ways — preset runs, paste JSON, or load a local file. To view your own run, open `docs/index.html` in a browser or use the live link, select **Load File**, and point it at your `results.json`.

---

## Contributing Reference Benchmarks

Benchmark numbers are only meaningful in the context of the hardware they were collected on. If you run this suite on your own machine, consider submitting your results so others with similar hardware can use them as a reference without having to run the benchmarks themselves.

### How to Submit

1. Run the benchmark (Java and/or C++) following the instructions above and save the JSON output.
2. Add your result file as `docs/benchmarks/json/<next_id>.json`.
3. Add a corresponding entry to `docs/benchmarks/metadata.json`:

```json
{
  "id": 3,
  "name": "Benchmark Run #3 - <short description>",
  "file": "3.json",
  "date": "YYYY-MM-DD",
  "isNative": false,
  "cpu": "<CPU model and core count>",
  "memory": "<RAM size and type>",
  "os": "<OS name and version>",
  "jvm": "<JVM version string>",
  "jvmFlags": "<flags used, e.g. -Xmx8g>",
  "libraries": [
    {
        "name": "<library name>",
        "url": "<repository link>",
        "sketch": "<sketch name>",
        "version": "<library version>"
    }, ...
  ]
  "notes": "<anything else relevant>"
}
```

For C++ runs, replace `jvm`/`jvmFlags` with `compiler` and `compilerOptions` (see run #2 in `metadata.json` as an example).

4. Open a pull request with the two files. No code changes required.

### What Makes a Good Reference Run

- The hardware should be distinct from existing runs — a different CPU microarchitecture, a server-class machine, a cloud instance type, or a non-Apple-Silicon laptop are all valuable additions.
- Run with the same default parameters (`--iterations 10000 (or higher)`, the full power-of-two size sweep (as high as possible), `--lgk 12`) so results are directly comparable across entries in the dashboard.
- Make sure the machine is otherwise idle during the run to minimize noise.

---

## Adding a New Sketch

**Java:** Add a new subclass of `SketchBase` in `SketchBase.java` implementing `update`, `merge`, `serialize`, `deserialize`, and `cardinality`. Register it as a new entry in the `SketchType` enum.

**C++:** Add a new header under `cpp/src/implementations/` implementing the `SketchBase` interface, then register it with `REGISTER_SKETCH("your-sketch-name", YourSketchClass)` in `main.cpp`.

---

## License

See [LICENSE](LICENSE).