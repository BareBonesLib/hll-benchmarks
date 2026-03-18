package io.github.bareboneslib.hll_benchmarks;


import java.util.UUID;

// UUID Factory
public class UUIDFactory {

    private static final int BATCH_SIZE = 100_000;
    private String[] batch;
    private int index;

    // 128-bit counter
    private long high;
    private long low;

    public UUIDFactory() {
        UUID base = new UUID(100, 100);// UUID.randomUUID();
        high = base.getMostSignificantBits();
        low = base.getLeastSignificantBits();
        generateBatch();
    }

    private void generateBatch() {
        batch = new String[BATCH_SIZE];
        for (int i = 0; i < BATCH_SIZE; i++) {
//            batch[i] = new UUID(high, low).toString();
            batch[i] = high + " " + low;
            low++;
            if (low == 0) {   // overflow
                high++;
            }
//            increment();
        }
        index = 0;
    }

    private void increment() {
        low++;
        if (low == 0) {   // overflow
            high++;
        }
    }

    public String next() {
        if (index >= BATCH_SIZE) generateBatch();
        return batch[index++];
    }

    public String[] getBatch(int size) {
        String[] result = new String[size];
        for (int i = 0; i < size; i++) result[i] = next();
        return result;
    }
}
