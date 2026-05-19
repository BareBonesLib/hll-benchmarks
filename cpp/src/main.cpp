#include <iostream>
#include <vector>

#include "ArgParser.h"
#include "SketchType.h"
#include "SketchTestRunner.h"
#include "Results.h"
#include "ResultsJson.h"

// Sketch implementations
#include "implementations/BareBonesHLL.h"
#include "implementations/DataSketchesHLL.h"
#include "implementations/DataSketchesCPC.h"
#include "implementations/DataSketchesTheta.h"

// JSON
#include <nlohmann/json.hpp>

using json = nlohmann::json;

int main(int argc, char** argv) {
    // Parse CLI args
    Config cfg = parseArguments(argc, argv);

    std::cerr << "Starting sketch performance tests...\n";
    std::cerr << "Test iterations (sketch array size): "
              << cfg.TEST_ITERATIONS << "\n\n";

    // Register sketches
    REGISTER_SKETCH("barebones-hll-4-cpp", BareBonesHLL4);
    REGISTER_SKETCH("barebones-hll-5-cpp", BareBonesHLL5);
    REGISTER_SKETCH("barebones-hll-6-cpp", BareBonesHLL6);
    REGISTER_SKETCH("datasketch-hll-4-cpp", DataSketchesHLL4);
    REGISTER_SKETCH("datasketch-hll-6-cpp", DataSketchesHLL6);
    REGISTER_SKETCH("datasketch-hll-8-cpp", DataSketchesHLL8);
    REGISTER_SKETCH("datasketch-cpc", DataSketchesCPC);
    REGISTER_SKETCH("datasketch-theta", DataSketchesTheta);

    std::vector<SketchTestResult> results;

    for (const auto& type : sketchRegistry()) {
        std::cerr << "Testing: " << type.name << "\n";

        for (int size : cfg.SKETCH_SIZES) {
            std::cerr << "  Building "
                      << cfg.TEST_ITERATIONS
                      << " sketches, each with "
                      << size << " elements...\n";

            SketchTestRunner runner(cfg, type, size);
            SketchTestResult result = runner.runTest();

            results.push_back(std::move(result));

            std::cerr << "    Done.\n";
        }
        std::cerr << "\n";
    }

    // Serialize results to JSON (stdout)
    json j = results;
    std::cout << j.dump() << std::endl;

    return 0;
}