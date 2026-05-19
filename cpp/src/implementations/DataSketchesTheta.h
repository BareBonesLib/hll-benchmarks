#pragma once

#include "../SketchBase.h"
#include <vector>
#include <string>
#include <cstdint>
#include <cmath>

#include <xxhash.h>
#include <DataSketches/theta_sketch.hpp>
#include <DataSketches/theta_union.hpp>

using namespace datasketches;

class DataSketchesTheta : public SketchBase {
    int lg_k;
    // Theta has two types: update_theta_sketch (mutable) and compact_theta_sketch (after ops)
    // We store as update_theta_sketch for normal use
    update_theta_sketch sketch;

    static uint64_t hash(const std::string& s) {
        return XXH64(s.data(), s.size(), 0);
    }

public:
    explicit DataSketchesTheta(int lg_k) : lg_k(lg_k), sketch(update_theta_sketch(update_theta_sketch::builder().set_lg_k(lg_k).build())) {};

    ~DataSketchesTheta() override = default;

    DataSketchesTheta(const DataSketchesTheta&) = delete;
    DataSketchesTheta& operator=(const DataSketchesTheta&) = delete;

    void update(const std::string& value) override {
        sketch.update(hash(value));
    }

    void merge(SketchBase& other) override {
        const auto& o = dynamic_cast<const DataSketchesTheta&>(other);
        theta_union u = theta_union::builder().set_lg_k(lg_k).build();
        u.update(this->sketch);
        u.update(o.sketch);
        compact_theta_sketch result = u.get_result();

        // Rebuild as update sketch from compact result via serialize/deserialize
        auto bytes = result.serialize();
        sketch = update_theta_sketch(
            update_theta_sketch::builder().set_lg_k(lg_k).build()
        );
        // Re-insert all retained entries from the compact sketch
        for (const auto& entry : result) {
            sketch.update(entry);
        }
    }

    std::vector<uint8_t> serialize() override {
        return sketch.compact().serialize();
    }

    void deserialize(const std::vector<uint8_t>& bytes) override {
        auto compact = compact_theta_sketch::deserialize(bytes.data(), bytes.size());
        sketch = update_theta_sketch(
            update_theta_sketch::builder().set_lg_k(lg_k).build()
        );
        for (const auto& entry : compact) {
            sketch.update(entry);
        }
    }

    uint64_t cardinality() override {
        return std::round(sketch.get_estimate());
    }
};