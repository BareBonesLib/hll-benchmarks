#pragma once

#include "../SketchBase.h"
#include <vector>
#include <string>
#include <cstdint>

#include <xxhash.h> 
#include <DataSketches/hll.hpp>

using namespace datasketches;

class DataSketchesHLL : public SketchBase {
    int p;
    hll_sketch sketch;

    static uint64_t hash(const std::string& s) {
        return XXH64(s.data(), s.size(), 0);
    }

    static datasketches::target_hll_type to_type(int r) {
        if (r == 4) return HLL_4;
        if (r == 6) return HLL_6;
        return HLL_8;
    }

public:
    DataSketchesHLL(int p, int r) : p(p), sketch(p, to_type(r)) {};
    
    // Add destructor
    ~DataSketchesHLL() override = default;

    // Prevent copying (or implement deep copy)
    DataSketchesHLL(const DataSketchesHLL&) = delete;
    DataSketchesHLL& operator=(const DataSketchesHLL&) = delete;

    void update(const std::string& value) override {
        sketch.update(hash(value));
    }

    void merge(SketchBase& other) override {
        const auto& o = dynamic_cast<const DataSketchesHLL&>(other);
        hll_union merged(p);
        merged.update((this->sketch));
        merged.update((o.sketch));
        this->sketch = merged.get_result();
    }

    std::vector<uint8_t> serialize() override {
        return sketch.serialize_compact();
    }

    void deserialize(const std::vector<uint8_t>& bytes) override {
        this->sketch = hll_sketch(hll_sketch::deserialize(bytes.data(), bytes.size()));
    }

    uint64_t cardinality() override {
        return std::round(sketch.get_estimate());
    }
};

class DataSketchesHLL4 : public DataSketchesHLL {
public:
    explicit DataSketchesHLL4(int p) : DataSketchesHLL(p, 4) {}
};

class DataSketchesHLL6 : public DataSketchesHLL {
public:
    explicit DataSketchesHLL6(int p) : DataSketchesHLL(p, 6) {}
};

class DataSketchesHLL8 : public DataSketchesHLL {
public:
    explicit DataSketchesHLL8(int p) : DataSketchesHLL(p, 8) {}
};
