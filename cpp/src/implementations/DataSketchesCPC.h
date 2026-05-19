#pragma once

#include "../SketchBase.h"
#include <vector>
#include <string>
#include <cstdint>
#include <cmath>

#include <xxhash.h>
#include <DataSketches/cpc_sketch.hpp>
#include <DataSketches/cpc_union.hpp>

using namespace datasketches;

class DataSketchesCPC : public SketchBase {
    int lg_k;
    cpc_sketch sketch;

    static uint64_t hash(const std::string& s) {
        return XXH64(s.data(), s.size(), 0);
    }

public:
    explicit DataSketchesCPC(int lg_k) : lg_k(lg_k) {
        sketch = cpc_sketch(lg_k);
    }

    ~DataSketchesCPC() override = default;

    DataSketchesCPC(const DataSketchesCPC&) = delete;
    DataSketchesCPC& operator=(const DataSketchesCPC&) = delete;

    void update(const std::string& value) override {
        sketch.update(hash(value));
    }

    void merge(SketchBase& other) override {
        const auto& o = dynamic_cast<const DataSketchesCPC&>(other);
        cpc_union u(lg_k);
        u.update(this->sketch);
        u.update(o.sketch);
        auto result = u.get_result();
        sketch = cpc_sketch(std::move(result));
    }

    std::vector<uint8_t> serialize() override {
        return sketch.serialize();
    }

    void deserialize(const std::vector<uint8_t>& bytes) override {
        sketch = cpc_sketch(cpc_sketch::deserialize(bytes.data(), bytes.size()));
    }

    uint64_t cardinality() override {
        return std::round(sketch.get_estimate());
    }
};