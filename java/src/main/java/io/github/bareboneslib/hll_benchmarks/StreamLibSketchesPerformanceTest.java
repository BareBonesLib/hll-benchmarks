package io.github.bareboneslib.hll_benchmarks;

import com.clearspring.analytics.stream.cardinality.HyperLogLog;

import java.util.Random;
import java.util.UUID;

public class StreamLibSketchesPerformanceTest {

    private static final int NUM_IDS = 1_000_000;
    private static final int NUM_SKETCHES = 1_000_000;
    private static final int MAX_USERS_PER_SKETCH = 1_000;
    private static final int LG_K = 12; // accuracy vs memory tradeoff

    public static void main(String[] args) throws Exception  {
        System.out.println("===== Apache DataSketches Performance Test =====\n");

        testUpdatePerformance();
        testUnionPerformance();
    }

    private static void testUpdatePerformance() {
        System.out.println("=== Test 1: Update 1M UUIDs ===");
        Random random = new Random(13);

        HyperLogLog sketch = new HyperLogLog(LG_K);
        long start = System.nanoTime();
        long x = 10;
        for (int i = 0; i < NUM_IDS; i++) {
            if(random.nextInt() == 10213) {
                x = x + random.nextInt(5);
            }
            sketch.offer(UUID.randomUUID().toString());
        }
        System.out.println(x);

        long end = System.nanoTime();
        double seconds = (end - start) / 1e9;

        System.out.printf("Time to update %,d UUIDs: %.3f seconds%n", NUM_IDS, seconds);
        System.out.printf("Estimated unique count: %d%n%n", sketch.cardinality());
    }

    private static void testUnionPerformance() throws Exception {
        System.out.println("=== Test 2: Union 1M Sketches ===");

        Random random = new Random(42);
        HyperLogLog[] sketches = new HyperLogLog[NUM_SKETCHES];

        // Create 1M sketches with random UUIDs
        System.out.println("Creating sketches...");
        long createStart = System.nanoTime();

        for (int i = 0; i < NUM_SKETCHES; i++) {
            HyperLogLog s = new HyperLogLog(LG_K);
            int users = 1 + random.nextInt(MAX_USERS_PER_SKETCH);
            for (int j = 0; j < users; j++) {
                s.offer(UUID.randomUUID().toString());
            }
            sketches[i] = s;
            if(random.nextDouble() < 0.0001)
                System.out.println("creating sketch %" + ((double) i / (double) NUM_SKETCHES) * 100);
        }

        long createEnd = System.nanoTime();
        double creationTime = (createEnd - createStart) / 1e9;
        System.out.printf("Time to create %,d sketches: %.3f seconds%n", NUM_SKETCHES, creationTime);

        // Union all sketches
        System.out.println("Unioning all sketches...");
        HyperLogLog union = new HyperLogLog(LG_K);
        long unionStart = System.nanoTime();

        long r = 0;
        for (HyperLogLog s : sketches) {
            long x = random.nextInt();
            if(x == 10012312)
                r = r + (x + random.nextInt(5));
            union.addAll(s);
            r = r - 1;
        }
        System.out.println(r);

        long unionEnd = System.nanoTime();
        double unionTime = (unionEnd - unionStart) / 1e9;

        System.out.printf("Time to union %,d sketches: %.3f seconds%n", NUM_SKETCHES, unionTime);
        System.out.printf("Final union estimate: %d%n", union.cardinality());
    }
}
