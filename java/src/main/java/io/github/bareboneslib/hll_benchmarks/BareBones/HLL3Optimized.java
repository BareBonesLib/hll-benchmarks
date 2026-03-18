//package io.github.bareboneslib.hll_benchmarks.BareBones;
//
//public class HLL3Optimized {
//    int p;
//    int r;
//    long[] registers;
//
//    int m;
//    int maxRegisterValue;
//    int regPerDatatype;
//    int totalRegisters;
//    int PAD;
//    long MASK;
//
//    // Cache these for add() hot path
//    private int pShift;
//    private long pBitMask;
//
//    static final double POW_2_32 = 4294967296.0;
//    static final double[] PRE_POW_2_K;
//    static final int DT_WIDTH = 64;
//
//    static {
//        PRE_POW_2_K = new double[256];
//        for(int i = 0; i < 256; i++) {
//            PRE_POW_2_K[i] = Math.pow(2, -i);
//        }
//    }
//
//    public HLL3Optimized(int p, int r) {
//        this.p = p;
//        this.r = r;
//        this.regPerDatatype = DT_WIDTH / r;
//        this.totalRegisters = (1 << p);
//        this.m = totalRegisters / regPerDatatype + (totalRegisters % regPerDatatype == 0 ? 0 : 1);
//        this.maxRegisterValue = ((1 << r) - 1);
//        this.PAD = DT_WIDTH % r;
//        this.MASK = (1L << r) - 1;
//        this.pShift = 64 - p;
//        this.pBitMask = 1L << pShift;
//        this.registers = new long[m];
//    }
//
//    private HLL3Optimized(byte[] array) {
//        this.p = array[array.length - 2];
//        this.r = array[array.length - 1];
//        this.regPerDatatype = DT_WIDTH / r;
//        this.totalRegisters = (1 << p);
//        this.m = totalRegisters / regPerDatatype + (totalRegisters % regPerDatatype == 0 ? 0 : 1);
//        this.maxRegisterValue = ((1 << r) - 1);
//        this.PAD = DT_WIDTH % r;
//        this.MASK = (1L << r) - 1;
//        this.pShift = 64 - p;
//        this.pBitMask = 1L << pShift;
//
//        this.registers = new long[m];
//        for(int i = 0; i < m; i++) {
//            int idx = i << 3; // i * 8
//            this.registers[i] =
//                    ((long)(array[idx] & 0xFF) << 56) |
//                            ((long)(array[idx + 1] & 0xFF) << 48) |
//                            ((long)(array[idx + 2] & 0xFF) << 40) |
//                            ((long)(array[idx + 3] & 0xFF) << 32) |
//                            ((long)(array[idx + 4] & 0xFF) << 24) |
//                            ((long)(array[idx + 5] & 0xFF) << 16) |
//                            ((long)(array[idx + 6] & 0xFF) << 8) |
//                            (long)(array[idx + 7] & 0xFF);
//        }
//    }
//
//    // Inline candidate - keep simple
//    private int getRegisterOffset(int index) {
//        return (DT_WIDTH - 1) - (PAD + ((index % regPerDatatype) * r) + (r - 1));
//    }
//
//    public byte readRegister(int index) {
//        int registerIndex = index / regPerDatatype;
//        int offset = getRegisterOffset(index);
//        return (byte) ((registers[registerIndex] >>> offset) & MASK);
//    }
//
//    public void writeRegister(byte value, int index) {
//        int registerIndex = index / regPerDatatype;
//        int offset = getRegisterOffset(index);
//        long valueLong = value & 0xFFL;
//        registers[registerIndex] = (registers[registerIndex] & ~(MASK << offset)) | (valueLong << offset);
//    }
//
//    // Fully inline the hot path - avoid method calls
//    public void add(long value) {
//        // Extract bucket
//        int bucket = (int) (value >>> pShift);
//
//        // Set bit and count trailing zeros
//        int cnt = Long.numberOfTrailingZeros(value | pBitMask) + 1;
//
//        // Clamp to max
//        cnt = Math.min(cnt, maxRegisterValue);
//
//        // Inline register read/write to avoid method call overhead
//        int regIndex = bucket / regPerDatatype;
//        int offset = (DT_WIDTH - 1) - (PAD + ((bucket % regPerDatatype) * r) + (r - 1));
//
//        long reg = registers[regIndex];
//        int prev = (int)((reg >>> offset) & MASK);
//
//        if(prev < cnt) {
//            reg = (reg & ~(MASK << offset)) | ((long)cnt << offset);
//            registers[regIndex] = reg;
//        }
//    }
//
//    public void merge(HLL3Optimized other) {
//        // Try to help with loop unrolling
//        int i = 0;
//        for(; i < m; i++) {
//            long thisReg = registers[i];
//            long otherReg = other.registers[i];
//            long result = 0;
//
//            for(int j = 0; j < regPerDatatype; j++) {
//                long shift = r * j;
//                long mask = MASK << shift;
//                long thisVal = thisReg & mask;
//                long thatVal = otherReg & mask;
//                // Use unsigned comparison
//                result |= ((thisVal ^ Long.MIN_VALUE) >= (thatVal ^ Long.MIN_VALUE)) ? thisVal : thatVal;
//            }
//
//            registers[i] = result;
//        }
//    }
//
//    public long estimate() {
//        double M = totalRegisters;
//        double sum = 0;
//        int zeroRegisters = 0;
//        int tmp = 0;
//
//        // Calculate exact number of complete longs we need to process
//        int completeRegs = totalRegisters / regPerDatatype;
//        int remainingRegs = totalRegisters % regPerDatatype;
//
//        // Process complete longs without boundary check
//        for(int i = 0; i < completeRegs; i++) {
//            long reg = registers[i];
//
//            for(int j = regPerDatatype - 1; j >= 0; j--) {
//                int k = (int)((reg >>> (r * j)) & MASK);
//                zeroRegisters += (k == 0) ? 1 : 0;
//                sum += PRE_POW_2_K[k];
//            }
//        }
//
//        // Process remaining registers in the last long
//        if(remainingRegs > 0) {
//            long reg = registers[completeRegs];
//            for(int j = regPerDatatype - 1; j >= regPerDatatype - remainingRegs; j--) {
//                int k = (int)((reg >>> (r * j)) & MASK);
//                zeroRegisters += (k == 0) ? 1 : 0;
//                sum += PRE_POW_2_K[k];
//            }
//        }
//
//        double alphaM;
//        if(((int) M) == 16)
//            alphaM = 0.673;
//        else if (((int) M) == 32)
//            alphaM = 0.697;
//        else if (((int) M) == 64)
//            alphaM = 0.709;
//        else
//            alphaM = 0.7213 / (1 + 1.079 / M);
////        switch ((int)M) {
////            case 16: alphaM = 0.673; break;
////            case 32: alphaM = 0.697; break;
////            case 64: alphaM = 0.709; break;
////            default: alphaM = 0.7213 / (1 + 1.079 / M);
////        }
//
//        double rawEstimate = alphaM * M * M / sum;
//
//        if(rawEstimate <= 2.5 * M && zeroRegisters > 0) {
//            return (long)(M * Math.log(M / zeroRegisters));
//        } else if(rawEstimate > POW_2_32 / 30.0) {
//            return (long)(-POW_2_32 * Math.log(1 - rawEstimate / POW_2_32));
//        }
//
//        return (long) rawEstimate;
//    }
//
//    public byte[] serialize() {
//        int size = m * 8 + 2;
//        byte[] array = new byte[size];
//
//        for(int i = 0, j = 0; i < m; i++) {
//            long reg = registers[i];
//            array[j++] = (byte)(reg >>> 56);
//            array[j++] = (byte)(reg >>> 48);
//            array[j++] = (byte)(reg >>> 40);
//            array[j++] = (byte)(reg >>> 32);
//            array[j++] = (byte)(reg >>> 24);
//            array[j++] = (byte)(reg >>> 16);
//            array[j++] = (byte)(reg >>> 8);
//            array[j++] = (byte)reg;
//        }
//
//        array[size - 2] = (byte) p;
//        array[size - 1] = (byte) r;
//        return array;
//    }
//
//    public static HLL3Optimized deserialize(byte[] array) {
//        return new HLL3Optimized(array);
//    }
//
//    void debugInfo() {
//        System.err.println("p=" + p + " r=" + r + " bytes=" + m + " M=" + totalRegisters);
//        int max = maxRegisterValue;
//        int[] hist = new int[max + 1];
//        for (int i = 0; i < totalRegisters; i++) {
//            int v = readRegister(i) & 0xFF;
//            hist[v]++;
//        }
//        for (int i = 0; i <= max; i++)
//            System.err.printf("reg %2d: %6d%n", i, hist[i]);
//        System.err.println("zeroRegs=" + hist[0]);
//    }
//}