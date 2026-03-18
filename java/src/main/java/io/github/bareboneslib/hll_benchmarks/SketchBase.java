package io.github.bareboneslib.hll_benchmarks;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import io.github.bareboneslib.bareboneshll.HLLPlusPlus;
import net.openhft.hashing.LongHashFunction;
import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.TgtHllType;
import org.apache.datasketches.hll.Union;
import net.agkn.hll.HLL;
import net.jpountz.xxhash.XXHashFactory;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import org.apache.datasketches.cpc.CpcSketch;
import org.apache.datasketches.cpc.CpcUnion;
import org.apache.datasketches.theta.Sketch;
import org.apache.datasketches.theta.UpdateSketch;
import org.apache.datasketches.memory.Memory;

// Hash4j imports
//import com.dynatrace.hash4j.distinctcount.UltraLogLog;

// LiveRamp HyperMinHash imports
import com.liveramp.hyperminhash.HyperMinHash;
import com.liveramp.hyperminhash.BetaMinHash;
import com.liveramp.hyperminhash.HyperMinHashCombiner;
import com.liveramp.hyperminhash.BetaMinHashCombiner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

enum SketchType {
//    THETA("theta", ThetaSketch.class),
//    CPC("cpc", CPCSketch.class),
//    D_HLL_4("datasketches-hll-4", DataSketchHLL4.class),
//    D_HLL_6("datasketches-hll-6", DataSketchHLL6.class),
//    D_HLL_8("datasketches-hll-8", DataSketchHLL8.class),
//    AGKN_4("agkn-4", AGKNSketch4.class),
//    AGKN_6("agkn-6", AGKNSketch6.class),
//    AGKN_8("agkn-8", AGKNSketch8.class),
//    STRMLIB("strmlib-hll", StreamLibSketchesHLL.class),
//    STRMLIB_SP12("strmlib-hll_sp12", StreamLibSketchesHLLPlus12.class),
//    STRMLIB_SP14("strmlib-hll_sp14", StreamLibSketchesHLLPlus14.class),
//    STRMLIB_SP16("strmlib-hll_sp16", StreamLibSketchesHLLPlus16.class),
//    STRMLIB_SP18("strmlib-hll_sp18", StreamLibSketchesHLLPlus18.class),
//    STRMLIB_SP20("strmlib-hll_sp20", StreamLibSketchesHLLPlus20.class),
//    STRMLIB_SP22("strmlib-hll_sp22", StreamLibSketchesHLLPlus22.class),
//    STRMLIB_SP24("strmlib-hll_sp24", StreamLibSketchesHLLPlus24.class),
//    HASH4J_ULL("hash4j-ull", Hash4jUltraLogLog.class),
//    HASH4J_HLL("hash4j-hll", Hash4jHyperLogLog.class),
//    LIVERAMP_HMH_4("liveramp-hmh-4", LiveRampHyperMinHash4.class),
//    LIVERAMP_HMH_6("liveramp-hmh-6", LiveRampHyperMinHash6.class),
//    LIVERAMP_HMH_8("liveramp-hmh-8", LiveRampHyperMinHash8.class),
//    LIVERAMP_BMH("liveramp-bmh", LiveRampBetaMinHash.class),
//    BAREBONES_HLL_4("barebones-hll-4", BareBonesHLL4.class),
//    BAREBONES_HLL_5("barebones-hll-5", BareBonesHLL5.class),
//    BAREBONES_HLL_6("barebones-hll-6", BareBonesHLL6.class),
    BAREBONES_HLLPP_4("barebones-hllpp-4", BareBonesHLLPlusPlus4.class),
    BAREBONES_HLLPP_5("barebones-hllpp-5", BareBonesHLLPlusPlus5.class),
    BAREBONES_HLLPP_6("barebones-hllpp-6", BareBonesHLLPlusPlus6.class);

    private final String name;
    private final Class<? extends SketchBase> sketchClass;

    SketchType(String name, Class<? extends SketchBase> sketchClass) {
        this.name = name;
        this.sketchClass = sketchClass;
    }

    public String getName() {
        return name;
    }

    public Class<? extends SketchBase> getSketchClass() {
        return sketchClass;
    }

    // Get enum by string
    public static SketchType fromString(String name) {
        for (SketchType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sketch type: " + name);
    }

    // Create instance with default constructor
    public SketchBase createInstance() throws Exception {
        return sketchClass.getDeclaredConstructor().newInstance();
    }

    // Create instance with parameters
    public SketchBase createInstance(int lgK) throws Exception {
        return sketchClass.getDeclaredConstructor(int.class).newInstance(lgK);
    }
}

abstract class SketchBase {
    abstract void update(String val);
    abstract void merge(SketchBase sketch);
    abstract byte[] serialize();
    abstract void deserialize(byte[] bytes);
    abstract long cardinality();
}

// Base class for DataSketches HLL to reduce code duplication
abstract class DataSketchHLLBase extends SketchBase {
    HllSketch sketch = null;
    Union union = null;
    int lgK;

    protected void init(int lgK, TgtHllType hllType) {
        this.lgK = lgK;
        sketch = new HllSketch(lgK, hllType);
        union = new Union(lgK);
    }

    @Override
    void update(String val) {
        sketch.update(val);
    }

    @Override
    void merge(SketchBase sketch) {
        union.reset();
        union.update(this.sketch);
        union.update(((DataSketchHLLBase) sketch).sketch);
        this.sketch = union.getResult();
    }

    @Override
    byte[] serialize() {
        return sketch.toCompactByteArray();
    }

    @Override
    void deserialize(byte[] bytes) {
        sketch = HllSketch.heapify(bytes);
    }

    @Override
    long cardinality() {
        return (long) this.sketch.getEstimate();
    }
}

class DataSketchHLL4 extends DataSketchHLLBase {
    DataSketchHLL4(int lgK) {
        init(lgK, TgtHllType.HLL_4);
    }
}

class DataSketchHLL6 extends DataSketchHLLBase {
    DataSketchHLL6(int lgK) {
        init(lgK, TgtHllType.HLL_6);
    }
}

class DataSketchHLL8 extends DataSketchHLLBase {
    DataSketchHLL8(int lgK) {
        init(lgK, TgtHllType.HLL_8);
    }
}

// Base class for AGKN HLL to reduce code duplication
abstract class AGKNSketchBase extends SketchBase {
    HLL sketch = null;
    XXHashFactory factory = XXHashFactory.fastestInstance();
    long seed = 0;

    protected void init(int lgK, int registerWidth) {
        sketch = new HLL(lgK, registerWidth);
    }

    @Override
    void update(String val) {
        byte[] data = val.getBytes(StandardCharsets.UTF_8);
        long hash64 = factory.hash64().hash(data, 0, data.length, seed);
        sketch.addRaw(hash64);
    }

    @Override
    void merge(SketchBase sketch) {
        this.sketch.union(((AGKNSketchBase) sketch).sketch);
    }

    @Override
    byte[] serialize() {
        return sketch.toBytes();
    }

    @Override
    void deserialize(byte[] bytes) {
        sketch = HLL.fromBytes(bytes);
    }

    @Override
    long cardinality() {
        return this.sketch.cardinality();
    }
}

class AGKNSketch4 extends AGKNSketchBase {
    AGKNSketch4(int lgK) {
        init(lgK, 4);
    }
}

class AGKNSketch6 extends AGKNSketchBase {
    AGKNSketch6(int lgK) {
        init(lgK, 6);
    }
}

class AGKNSketch8 extends AGKNSketchBase {
    AGKNSketch8(int lgK) {
        init(lgK, 8);
    }
}

// Stream-lib HyperLogLog (fixed 5-bit registers)
class StreamLibSketchesHLL extends SketchBase {
    HyperLogLog sketch = null;

    StreamLibSketchesHLL(int lgK) {
        // Stream-lib uses fixed 5-bit registers (REGISTER_SIZE = 5 is hardcoded)
        sketch = new HyperLogLog(lgK);
    }

    @Override
    void update(String val) {
        sketch.offer(val);
    }

    @Override
    void merge(SketchBase sketch) {
        try {
            this.sketch.addAll(((StreamLibSketchesHLL) sketch).sketch);
        } catch (CardinalityMergeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    byte[] serialize() {
        try {
            return sketch.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void deserialize(byte[] bytes) {
        try {
            sketch = HyperLogLog.Builder.build(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    long cardinality() {
        return this.sketch.cardinality();
    }
}

class StreamLibSketchesHLLPlusBase extends SketchBase {
    HyperLogLogPlus sketch = null;

    void init(int lgK, int sp) {
        // Stream-lib uses fixed 5-bit registers (REGISTER_SIZE = 5 is hardcoded)
        sketch = new HyperLogLogPlus(lgK, sp);
    }

    @Override
    void update(String val) {
        sketch.offer(val);
    }

    @Override
    void merge(SketchBase sketch) {
        try {
            this.sketch.addAll(((StreamLibSketchesHLLPlusBase) sketch).sketch);
        } catch (CardinalityMergeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    byte[] serialize() {
        try {
            return sketch.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void deserialize(byte[] bytes) {
        try {
            sketch = HyperLogLogPlus.Builder.build(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    long cardinality() {
        return this.sketch.cardinality();
    }
}

class StreamLibSketchesHLLPlus08 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus08(int p) {
        init(p, 8);
    }
}

class StreamLibSketchesHLLPlus10 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus10(int p) {
        init(p, 10);
    }
}

class StreamLibSketchesHLLPlus12 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus12(int p) {
        init(p, 12);
    }
}

class StreamLibSketchesHLLPlus14 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus14(int p) {
        init(p, 14);
    }
}

class StreamLibSketchesHLLPlus16 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus16(int p) {
        init(p, 16);
    }
}

class StreamLibSketchesHLLPlus18 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus18(int p) {
        init(p, 18);
    }
}

class StreamLibSketchesHLLPlus20 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus20(int p) {
        init(p, 20);
    }
}

class StreamLibSketchesHLLPlus22 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus22(int p) {
        init(p, 22);
    }
}

class StreamLibSketchesHLLPlus24 extends StreamLibSketchesHLLPlusBase {
    StreamLibSketchesHLLPlus24(int p) {
        init(p, 24);
    }
}

class CPCSketch extends SketchBase {
    CpcSketch sketch = null;
    int lgK;

    CPCSketch(int lgK) {
        this.lgK = lgK;
        sketch = new CpcSketch(lgK);
    }

    @Override
    void update(String val) {
        sketch.update(val);
    }

    @Override
    void merge(SketchBase sketch) {
        CpcUnion union = new CpcUnion(lgK);
        union.update(this.sketch);
        union.update(((CPCSketch) sketch).sketch);
        this.sketch = union.getResult();
    }

    @Override
    byte[] serialize() {
        return sketch.toByteArray();
    }

    @Override
    void deserialize(byte[] bytes) {
        this.sketch = CpcSketch.heapify(bytes);
    }

    @Override
    long cardinality() {
        return (long) this.sketch.getEstimate();
    }
}

class ThetaSketch extends SketchBase {
    Sketch sketch = null;
    int lgK;
    org.apache.datasketches.theta.Union union = null;

    ThetaSketch(int lgK) {
        this.lgK = lgK;
        sketch = UpdateSketch.builder().setLogNominalEntries(lgK).build();
        union = org.apache.datasketches.theta.Union.builder().setLogNominalEntries(lgK).buildUnion();
    }

    @Override
    void update(String val) {
        ((UpdateSketch) sketch).update(val);
    }

    @Override
    void merge(SketchBase sketch) {
        union.reset();
        union.union(this.sketch);
        union.union(((ThetaSketch) sketch).sketch);
        this.sketch = union.getResult();
    }

    @Override
    byte[] serialize() {
        return this.sketch.compact().toByteArray();
    }

    @Override
    void deserialize(byte[] bytes) {
        this.sketch = Sketch.heapify(Memory.wrap(bytes));
    }

    @Override
    long cardinality() {
        return (long) this.sketch.getEstimate();
    }
}

/*
// Hash4j UltraLogLog implementation
class Hash4jUltraLogLog extends SketchBase {
    UltraLogLog sketch = null;
    int lgK;

    Hash4jUltraLogLog(int lgK) {
        this.lgK = lgK;
        sketch = UltraLogLog.create(lgK);
    }

    @Override
    void update(String val) {
        long hash = val.hashCode(); // Using simple hash, can be replaced with better hash
        sketch.add(hash);
    }

    @Override
    void merge(SketchBase sketch) {
        UltraLogLog merged = UltraLogLog.create(lgK);
        merged.add(this.sketch);
        merged.add(((Hash4jUltraLogLog) sketch).sketch);
        this.sketch = merged;
    }

    @Override
    byte[] serialize() {
        return sketch.getState();
    }

    @Override
    void deserialize(byte[] bytes) {
        sketch = UltraLogLog.wrap(bytes);
    }

    @Override
    long cardinality() {
        return (long) this.sketch.getDistinctCountEstimate();
    }
}

// Hash4j HyperLogLog implementation (HLL++)
class Hash4jHyperLogLog extends SketchBase {
    com.dynatrace.hash4j.distinctcount.HyperLogLog sketch = null;
    int lgK;

    Hash4jHyperLogLog(int lgK) {
        this.lgK = lgK;
        sketch = com.dynatrace.hash4j.distinctcount.HyperLogLog.create(lgK);
    }

    @Override
    void update(String val) {
        long hash = val.hashCode(); // Using simple hash, can be replaced with better hash
        sketch.add(hash);
    }

    @Override
    void merge(SketchBase sketch) {
        com.dynatrace.hash4j.distinctcount.HyperLogLog merged = com.dynatrace.hash4j.distinctcount.HyperLogLog.create(lgK);
        merged.add(this.sketch);
        merged.add(((Hash4jHyperLogLog) sketch).sketch);
        this.sketch = merged;
    }

    @Override
    byte[] serialize() {
        return sketch.getState();
    }

    @Override
    void deserialize(byte[] bytes) {
        sketch = com.dynatrace.hash4j.distinctcount.HyperLogLog.wrap(bytes);
    }

    @Override
    long cardinality() {
        return (long) this.sketch.getDistinctCountEstimate();
    }
}
*/

// LiveRamp HyperMinHash implementation
class LiveRampHyperMinHashBase extends SketchBase {
    HyperMinHash sketch = null;
    int p;
    int r;

    void init(int p, int r) {
        this.p = p;
        this.r = r;
        sketch = new HyperMinHash(p, r);
    }

    @Override
    void update(String val) {
        sketch.offer(val.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    void merge(SketchBase sketch) {
        HyperMinHashCombiner combiner = HyperMinHashCombiner.getInstance();
        java.util.List<HyperMinHash> sketches = new java.util.ArrayList<>();
        sketches.add(this.sketch);
        sketches.add(((LiveRampHyperMinHashBase) sketch).sketch);
        this.sketch = combiner.union(sketches);
    }

    @Override
    byte[] serialize() {
        com.liveramp.hyperminhash.HyperMinHashSerDe serde = new com.liveramp.hyperminhash.HyperMinHashSerDe();
        return serde.toBytes(sketch);
    }

    @Override
    void deserialize(byte[] bytes) {
        com.liveramp.hyperminhash.HyperMinHashSerDe serde = new com.liveramp.hyperminhash.HyperMinHashSerDe();
        sketch = serde.fromBytes(bytes);
    }

    @Override
    long cardinality() {
        try {
            return sketch.cardinality();
        } catch (Exception e) {
            // small p for spline cardinality correction
            return 0L;
        }
    }
}

class LiveRampHyperMinHash4 extends LiveRampHyperMinHashBase {
    LiveRampHyperMinHash4(int lgK) {
        init(lgK, 4);
    }
}

class LiveRampHyperMinHash6 extends LiveRampHyperMinHashBase {
    LiveRampHyperMinHash6(int lgK) {
        init(lgK, 6);
    }
}
class LiveRampHyperMinHash8 extends LiveRampHyperMinHashBase {
    LiveRampHyperMinHash8(int lgK) {
        init(lgK, 8);
    }
}

// LiveRamp BetaMinHash implementation (optimized for small cardinalities)
class LiveRampBetaMinHash extends SketchBase {
    BetaMinHash sketch = null;

    LiveRampBetaMinHash() {
        sketch = new BetaMinHash(); // Fixed precision p=14
    }

    LiveRampBetaMinHash(int p) {
        // BetaMinHash only supports p=14, ignore parameter
        this();
    }

    @Override
    void update(String val) {
        sketch.offer(val.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    void merge(SketchBase sketch) {
        BetaMinHashCombiner combiner = BetaMinHashCombiner.getInstance();
        java.util.List<BetaMinHash> sketches = new java.util.ArrayList<>();
        sketches.add(this.sketch);
        sketches.add(((LiveRampBetaMinHash) sketch).sketch);
        this.sketch = combiner.union(sketches);
    }

    @Override
    byte[] serialize() {
        com.liveramp.hyperminhash.BetaMinHashSerde serde = new com.liveramp.hyperminhash.BetaMinHashSerde();
        return serde.toBytes(sketch);
    }

    @Override
    void deserialize(byte[] bytes) {
        com.liveramp.hyperminhash.BetaMinHashSerde serde = new com.liveramp.hyperminhash.BetaMinHashSerde();
        sketch = serde.fromBytes(bytes);
    }

    @Override
    long cardinality() {
        return sketch.cardinality();
    }
}


class BareBonesHLLBase extends SketchBase {
    io.github.bareboneslib.bareboneshll.HLL sketch = null;
    static net.openhft.hashing.LongHashFunction hash = LongHashFunction.xx();

    void init(int p, int r) {
        this.sketch = new io.github.bareboneslib.bareboneshll.HLL(p, r);
    }

    @Override
    void update(String val) {
        sketch.add(hash.hashBytes(val.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    void merge(SketchBase sketch) {
        this.sketch.merge(((BareBonesHLLBase) sketch).sketch);
    }

    @Override
    byte[] serialize() {
        return this.sketch.serialize();
    }

    @Override
    void deserialize(byte[] bytes) {
        this.sketch = io.github.bareboneslib.bareboneshll.HLL.deserialize(bytes);
    }

    @Override
    long cardinality() {
        return sketch.estimate();
    }
}

class BareBonesHLL4 extends BareBonesHLLBase {
    BareBonesHLL4(int p) {
        init(p, 4);
    }
}

class BareBonesHLL5 extends BareBonesHLLBase {
    BareBonesHLL5(int p) {
        init(p, 5);
    }
}

class BareBonesHLL6 extends BareBonesHLLBase {
    BareBonesHLL6(int p) {
        init(p, 6);
    }
}



class BareBonesHLLPlusPlusBase extends SketchBase {
    HLLPlusPlus sketch = null;
    static net.openhft.hashing.LongHashFunction hash = LongHashFunction.xx();

    void init(int p, int r) {
        this.sketch = new HLLPlusPlus(p, r);
    }

    @Override
    void update(String val) {
        sketch.add(hash.hashBytes(val.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    void merge(SketchBase sketch) {
        this.sketch.merge(((BareBonesHLLPlusPlusBase) sketch).sketch);
    }

    @Override
    byte[] serialize() {
        return this.sketch.serialize();
    }

    @Override
    void deserialize(byte[] bytes) {
        this.sketch = HLLPlusPlus.deserialize(bytes);
    }

    @Override
    long cardinality() {
        return sketch.estimate();
    }
}

class BareBonesHLLPlusPlus4 extends BareBonesHLLPlusPlusBase {
    BareBonesHLLPlusPlus4(int p) {
        init(p, 4);
    }
}

class BareBonesHLLPlusPlus5 extends BareBonesHLLPlusPlusBase {
    BareBonesHLLPlusPlus5(int p) {
        init(p, 5);
    }
}

class BareBonesHLLPlusPlus6 extends BareBonesHLLPlusPlusBase {
    BareBonesHLLPlusPlus6(int p) {
        init(p, 6);
    }
}
