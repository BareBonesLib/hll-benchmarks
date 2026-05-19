package io.github.bareboneslib.hll_benchmarks;

import java.util.ArrayList;
import java.util.List;

public class SketchTestRunner {
    ArgParser.Config config = null;
    List<EventTags> eventTags = null;
    SketchType sketchType = null;
    int size = 0;

    SketchTestRunner(ArgParser.Config config, SketchType sketchType, int size) {
        this.config = config;
        this.eventTags = new ArrayList<>();
        this.sketchType = sketchType;
        this.size = size;
    }

    SketchTestResult runTest() throws Exception {
        long startTimeNs = System.nanoTime();

        System.gc();

        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        // Build array of sketches
        System.err.println("    Creating sketch array...");
        SketchBase[] sketches = createSketchArray(sketchType, size);

        // Test Avg Size
        System.err.println("    Testing update...");
        SizeResult sizeResult = testMinMaxSerializedSize(sketches);

        // Test operations
        System.err.println("    Testing update...");
        OpResult updateResult = testUpdate(sketches);

        System.err.println("    Testing merge...");
        OpResult mergeResult = testMerge(sketchType, sketches);

        System.err.println("    Testing serialize...");
        OpResult serResult = testSerialize(sketches);

        System.err.println("    Testing deserialize...");
        OpResult deserResult = testDeserialize(sketchType, sketches);

        System.err.println("    Testing cardinality...");
        CardinalityResult cardResult = testCardinality(sketches, size);

        // Shutdown and print report
        monitor.shutdown();
        try {
            monitor.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<MemorySnapshot> snapshots = monitor.getSnapshots();

        long endTimeNs = System.nanoTime();

        return new SketchTestResult(
                sketchType.getName(),
                size,
                config.TEST_ITERATIONS,
                config.LGK,
                endTimeNs - startTimeNs,
                updateResult,
                mergeResult,
                serResult,
                deserResult,
                sizeResult,
                cardResult,
                snapshots,
                eventTags
        );
    }

    /**
     * Creates an array of TEST_ITERATIONS sketches, each populated with 'size' unique elements
     */
    private SketchBase[] createSketchArray(SketchType sketchType, int size) throws Exception {
        SketchBase[] sketches = new SketchBase[config.TEST_ITERATIONS];
        UUIDFactory factory = new UUIDFactory();
        RandomFactory randomFactory = new RandomFactory();
        double[] randoms = randomFactory.batchDouble(config.TEST_ITERATIONS);

        long startTimeNs = System.nanoTime();
        for (int i = 0; i < config.TEST_ITERATIONS; i++) {
            sketches[i] = sketchType.createInstance(config.LGK);

            // Pre-generate batch of UUIDs for this sketch
            String[] uuids = factory.getBatch(size);

            // Add 'size' unique elements to each sketch
            for (int j = 0; j < size; j++) {
                sketches[i].update(uuids[j]);
            }

            // Progress indicator for large arrays
            if (i > 0 && randoms[i] <= ((double) 20 / config.TEST_ITERATIONS)) {
                double progress = ((double) i / config.TEST_ITERATIONS) * 100.0;
                System.err.printf("      Created %.2f%% sketches...%n", progress);
            }
        }

        eventTags.add(new EventTags("SketchCreation", true, startTimeNs, System.nanoTime()));
        return sketches;
    }

    /**
     * Test Avg Size: Find the average serialized sketch size (compact if possible) in bytes
     */
    private static SizeResult testMinMaxSerializedSize(SketchBase[] sketches) {
        // Pre-generate batch of UUIDs

        // Measure: update each sketch once
        long start = System.nanoTime();
        long min = sketches[0].serialize().length;
        long max = sketches[0].serialize().length;
        for (int i = 0; i < sketches.length; i++) {
            long size = sketches[i].serialize().length;
            min = Math.min(min, size);
            max = Math.max(max, size);
        }
        long end = System.nanoTime();

        // Average time per update operation
        return new SizeResult(start, end, min, max);
    }

    /**
     * Test update: Add one element to each sketch in the array
     */
    private static OpResult testUpdate(SketchBase[] sketches) {
        // Pre-generate batch of UUIDs
        UUIDFactory factory = new UUIDFactory();
        String[] uuids = factory.getBatch(sketches.length);

        // Measure: update each sketch once
        long start = System.nanoTime();
        for (int i = 0; i < sketches.length; i++) {
            sketches[i].update(uuids[i]);
        }
        long end = System.nanoTime();

        // Average time per update operation
        return new OpResult(start, end, (end - start) / sketches.length);
    }


    /**
     * Test merge: Merge all sketches in the array into a single sketch
     */
    private OpResult testMerge(SketchType sketchType, SketchBase[] sketches) throws Exception {
        // Create a fresh sketch for merging
        SketchBase merged = sketchType.createInstance(config.LGK);

        // Measure: merge all sketches
        long start = System.nanoTime();
        for (SketchBase sketch : sketches) {
            merged.merge(sketch);
        }
        long end = System.nanoTime();

        // Average time per merge operation
        return new OpResult(start, end, (end - start) / sketches.length);
    }

    /**
     * Test serialize: Serialize entire array of sketches
     */
    private static OpResult testSerialize(SketchBase[] sketches) {
        // Measure: serialize each sketch
        byte[][] serialized = new byte[sketches.length][];

        long start = System.nanoTime();
        for (int i = 0; i < sketches.length; i++) {
            serialized[i] = sketches[i].serialize();
        }
        long end = System.nanoTime();

        // Average time per serialize operation
        return new OpResult(start, end, (end - start) / sketches.length);
    }

    /**
     * Test deserialize: Deserialize array of bytes back to sketches
     */
    private OpResult testDeserialize(SketchType sketchType, SketchBase[] sketches) throws Exception {
        // First serialize all sketches
        byte[][] serialized = new byte[sketches.length][];
        for (int i = 0; i < sketches.length; i++) {
            serialized[i] = sketches[i].serialize();
        }

        // Measure: deserialize each byte array
        long start = System.nanoTime();
        for (int i = 0; i < serialized.length; i++) {
            SketchBase temp = sketchType.createInstance(config.LGK);
            temp.deserialize(serialized[i]);
        }
        long end = System.nanoTime();

        // Average time per deserialize operation
        return new OpResult(start, end, (end - start) / serialized.length);
    }

    /**
     * Test cardinality: Get cardinality from each sketch and measure precision
     */
    private CardinalityResult testCardinality(SketchBase[] sketches, int expectedSize) {
        // Measure: get cardinality from each sketch
        long start = System.nanoTime();
        long totalEstimate = 0;
        for (SketchBase sketch : sketches) {
            totalEstimate += sketch.cardinality();
        }
        long end = System.nanoTime();
        eventTags.add(new EventTags("CardinalityEst", true, start, end));

        long avgTime = (end - start) / sketches.length;
        long avgEstimate = totalEstimate / sketches.length;

        // Calculate precision (relative error as percentage)
        // We added one more element during update test, so actual is expectedSize + 1
        long actualCardinality = expectedSize + 1;
        double precision = Math.abs(avgEstimate - actualCardinality) / (double) actualCardinality * 100.0;

        return new CardinalityResult(avgTime, precision, avgEstimate, actualCardinality);
    }
}