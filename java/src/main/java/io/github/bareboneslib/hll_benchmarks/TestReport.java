package io.github.bareboneslib.hll_benchmarks;

import java.util.List;


// Result classes
class SketchTestResult {
    public String sketchType;
    public int sketchSize;
    public int numSketches;
    public int lgK;
    public long timeTakenNs;
    public OpResult updateResult;
    public OpResult mergeResult;
    public OpResult serializeResult;
    public OpResult deserializeResult;
    public SizeResult sizeResult;
    public CardinalityResult cardinalityResult;
    public List<MemorySnapshot> memorySnapshots;
    public List<EventTags> eventTags;

    public SketchTestResult(String sketchType, int sketchSize, int numSketches, int lgK, long timeTakenNs,
                        OpResult updateResult, OpResult mergeResult, OpResult serializeResult,
                        OpResult deserializeResult, SizeResult sizeResult, CardinalityResult cardinalityResult,
                        List<MemorySnapshot> memorySnapshots, List<EventTags> eventTags) {
        this.sketchType = sketchType;
        this.sketchSize = sketchSize;
        this.numSketches = numSketches;
        this.lgK = lgK;
        this.timeTakenNs = timeTakenNs;
        this.updateResult = updateResult;
        this.mergeResult = mergeResult;
        this.serializeResult = serializeResult;
        this.deserializeResult = deserializeResult;
        this.cardinalityResult = cardinalityResult;
        this.sizeResult = sizeResult;
        this.memorySnapshots = memorySnapshots;
        this.eventTags = eventTags;
    }
}

class OpResult {
    long startTimeNs;
    long endTimeNs;
    long avgOpTimeNs;

    OpResult(long startTimeNs, long endTimeNs, long avgOpTimeNs) {
        this.startTimeNs = startTimeNs;
        this.endTimeNs = endTimeNs;
        this.avgOpTimeNs = avgOpTimeNs;
    }
}

class SizeResult {
    long startTimeNs;
    long endTimeNs;
    long minSize;
    long maxSize;

    SizeResult(long startTimeNs, long endTimeNs, long minSize, long maxSize) {
        this.startTimeNs = startTimeNs;
        this.endTimeNs = endTimeNs;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }
}

class CardinalityResult {
    long time;
    double errorPercentage;
    long estimatedCardinality;
    long actualCardinality;

    CardinalityResult(long time, double errorPercentage, long estimated, long actual) {
        this.time = time;
        this.errorPercentage = errorPercentage;
        this.estimatedCardinality = estimated;
        this.actualCardinality = actual;
    }
}


class EventTags {
    long startTimeNs;
    long endTimeNs;
    String markTag;
    boolean isRegion;

    EventTags(String markTag, boolean isRegion, long startTimeNs, long endTimeNs) {
        this.markTag = markTag;
        this.isRegion = isRegion;
        this.startTimeNs = startTimeNs;
        if(!isRegion)
            this.endTimeNs = startTimeNs;
        else
            this.endTimeNs = endTimeNs;
    }
}