package io.github.bareboneslib.hll_benchmarks;

import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.Union;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Random;
import java.util.UUID;

public class DataSketchesPerformanceTest {

    private static final int NUM_IDS = 1_000_000;
    private static final int NUM_SKETCHES = 1_000_000_0;
    private static final int MAX_USERS_PER_SKETCH = 1_000;
    private static final int LG_K = 12; // accuracy vs memory tradeoff

    public static void main(String[] args) {
        System.out.println("===== Apache DataSketches Performance Test =====\n");

        testUpdatePerformance();
        testUnionPerformance();
    }

    private static void testUpdatePerformance() {
        System.out.println("=== Test 1: Update 1M UUIDs ===");
        Random random = new Random(13);

        HllSketch sketch = new HllSketch(LG_K);
        long start = System.nanoTime();
        long x = 10;
        for (int i = 0; i < NUM_IDS; i++) {
            if(random.nextInt() == 10213) {
                x = x + random.nextInt(5);
            }
            sketch.update(UUID.randomUUID().toString());
        }
        System.out.println(x);

        long end = System.nanoTime();
        double seconds = (end - start) / 1e9;

        System.out.printf("Time to update %,d UUIDs: %.3f seconds%n", NUM_IDS, seconds);
        System.out.printf("Estimated unique count: %.0f%n%n", sketch.getEstimate());
    }

    private static void testUnionPerformance() {
        System.out.println("=== Test 2: Union 1M Sketches ===");

        Random random = new Random(42);
        HllSketch[] sketches = new HllSketch[NUM_SKETCHES];

        // Create 1M sketches with random UUIDs
        System.out.println("Creating sketches...");
        long createStart = System.nanoTime();

        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        for (int i = 0; i < NUM_SKETCHES; i++) {
            HllSketch s = new HllSketch(LG_K);
            int users = 1 + random.nextInt(MAX_USERS_PER_SKETCH);
            for (int j = 0; j < users; j++) {
                s.update(UUID.randomUUID().toString());
            }
            sketches[i] = s;
            if(random.nextDouble() < 0.0001) {
                double totalMem = (double) runtime.totalMemory() / 1024 / 1024;
                double freeMem = (double) runtime.freeMemory() / 1024 / 1024;
                double usageMem = totalMem - freeMem;
                MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
                MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
                System.out.println("creating sketch %" + ((double) i / (double) NUM_SKETCHES) * 100);
                System.out.println("RT Max         : " + totalMem + " MB");
                System.out.println("RT free        : " + freeMem + " MB");
                System.out.println("RT usage       : " + usageMem + " MB");
                System.out.println("Heap Max       : " + heapMemory.getMax()/1024/1024 + " MB");
                System.out.println("Heap Used      : " + heapMemory.getUsed()/1024/1024 + " MB");
                System.out.println("Heap Committed : " + heapMemory.getCommitted()/1024/1024 + " MB");
                System.out.println("NonHeap Max    : " + nonHeapMemory.getMax()/1024/1024 + " MB");
                System.out.println("NonHeap Used   : " + nonHeapMemory.getUsed()/1024/1024 + " MB");
                System.out.println("NonHeap comt   : " + nonHeapMemory.getCommitted()/1024/1024 + " MB");
            }
        }

        long createEnd = System.nanoTime();
        double creationTime = (createEnd - createStart) / 1e9;
        System.out.printf("Time to create %,d sketches: %.3f seconds%n", NUM_SKETCHES, creationTime);

        // Union all sketches
        System.out.println("Unioning all sketches...");
        Union union = new Union(LG_K);
        long unionStart = System.nanoTime();

        long r = 0;
        for (HllSketch s : sketches) {
            long x = random.nextInt();
            if(x == 10012312)
                r = r + (x + random.nextInt(5));
            union.update(s);
            r = r - 1;
        }
        System.out.println(r);

        long unionEnd = System.nanoTime();
        double unionTime = (unionEnd - unionStart) / 1e9;

        System.out.printf("Time to union %,d sketches: %.3f seconds%n", NUM_SKETCHES, unionTime);
        System.out.printf("Final union estimate: %.0f%n", union.getEstimate());
    }
}
