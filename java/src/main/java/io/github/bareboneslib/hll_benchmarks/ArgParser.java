package io.github.bareboneslib.hll_benchmarks;


public class ArgParser {
    static class Config {
        int TEST_ITERATIONS;
        int LGK;
        int[] SKETCH_SIZES;
        Config(int testIters, int lgk, int[] sketchSizes) {
            this.TEST_ITERATIONS = testIters;
            this.LGK = lgk;
            this.SKETCH_SIZES = sketchSizes;
        }
    }

    static Config parseArguments(String[] args) {
        int testIterations = 0;
        int lgK = 0;
        int[] sketchSizes = new int[0];
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--iterations":
                case "-i":
                    if (i + 1 < args.length) {
                        testIterations = Integer.parseInt(args[++i]);
                    }
                    break;

                case "--sizes":
                case "-s":
                    if (i + 1 < args.length) {
                        String[] sizesStr = args[++i].split(",");
                        sketchSizes = new int[sizesStr.length];
                        for (int j = 0; j < sizesStr.length; j++) {
                            sketchSizes[j] = Integer.parseInt(sizesStr[j].trim());
                        }
                    }
                    break;

                case "--lgk":
                case "-k":
                    if (i + 1 < args.length) {
                        lgK = Integer.parseInt(args[++i]);
                    }
                    break;

                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;

                default:
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }
        return new Config(testIterations, lgK, sketchSizes);
    }

    private static void printUsage() {
        System.out.println("Usage: java SketchPerformanceTest [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -i, --iterations <num>    Number of test iterations (recommended: 10000)");
        System.out.println("  -s, --sizes <sizes>       Comma-separated sketch sizes");
        System.out.println("  -k, --lgk <num>           Log2 of K value");
        System.out.println("  -h, --help                Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java SketchPerformanceTest --iterations 50000 --sizes 1,10,100,1000 --lg-k 10");
        System.out.println("  java SketchPerformanceTest -i 200000 -s 100,1000,10000 -k 14");
    }
}

