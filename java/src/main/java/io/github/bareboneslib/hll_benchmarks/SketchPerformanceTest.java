package io.github.bareboneslib.hll_benchmarks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;


public class SketchPerformanceTest {
//    private static final int TEST_ITERATIONS = 100_000;
////    private static final int[] SKETCH_SIZES = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};
//    private static final int[] SKETCH_SIZES = {1, 10, 100, 1_000};
//    private static final int LG_K = 12; // 4096 nominal entries
    static ArgParser.Config config = null;

    public static void main(String[] args) throws Exception {
//        Thread.sleep(10000);
        config = ArgParser.parseArguments(args);

        System.err.println("Starting sketch performance tests...");
        System.err.println("Test iterations (sketch array size): " + config.TEST_ITERATIONS);
        System.err.println();

        List<SketchTestResult> results = new ArrayList<>();

        for (SketchType sketchType : SketchType.values()) {
            System.err.println("Testing: " + sketchType.getName());

            for (int size : config.SKETCH_SIZES) {
                System.err.println("  Building " + config.TEST_ITERATIONS + " sketches, each with " + size + " elements...");
                SketchTestRunner runner = new SketchTestRunner(config, sketchType, size);
                SketchTestResult result = runner.runTest();
                results.add(result);
                System.err.println("    Done.");
            }

            System.err.println();
        }

        // Output JSON to stdout
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(results);
        System.out.println(json);
    }
}