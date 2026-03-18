//package io.github.bareboneslib.hll_benchmarks.BareBones;
//
//public class HLL3 {
//    // below variables need to be serialized
//    int p;
//    int r;
//    long[] registers;
//
//    // below variables are derived
//    int m;
//    int maxRegisterValue;
//    int regPerDatatype;
//    int totalRegisters;
//    int PAD;
//    long MASK;
//
//    // below are constants
//    static final double POW_2_32 = Math.pow(2, 32);
//    static double[] PRE_POW_2_K = new double[256];
//    static final int DT_WIDTH = 64;
//    static {
//        for(int i = 0; i< 256; i++) {
//            PRE_POW_2_K[i] = Math.pow(2, -i);
//        }
//    }
//
//    public HLL3(int p, int r) {
//        assert(p >= 5 && p <= 30);
//        assert(r >= 4 && r <= 8);
//
//        this.p = p;
//        this.r = r;
//        this.regPerDatatype = DT_WIDTH / r;
//        this.totalRegisters = (1 << p);
//        this.m = totalRegisters / regPerDatatype + (totalRegisters % regPerDatatype == 0 ? 0 : 1);
//        this.maxRegisterValue = ((1 << r) - 1);
//        this.PAD = DT_WIDTH % r;
//        this.MASK = (1L << r) - 1;
//
//        this.registers = new long[m];
//    }
//
//    private HLL3(byte[] array) {
//        this.p = array[array.length - 2];
//        this.r = array[array.length - 1];
//        this.regPerDatatype = DT_WIDTH / r;
//        this.totalRegisters = (1 << p);
//        this.m = totalRegisters / regPerDatatype + (totalRegisters % regPerDatatype == 0 ? 0 : 1);
//        this.maxRegisterValue = ((1 << r) - 1);
//        this.PAD = DT_WIDTH % r;
//        this.MASK = (1L << r) - 1;
//
//        this.registers = new long[m];
//        for(int i=0; i<m; i++) {
//            this.registers[i] = (
//                    ((array[i * 8] & 0xFFL) << 56) |
//                            ((array[i * 8 + 1] & 0xFFL) << 48) |
//                            ((array[i * 8 + 2] & 0xFFL) << 40) |
//                            ((array[i * 8 + 3] & 0xFFL) << 32) |
//                            ((array[i * 8 + 4] & 0xFFL) << 24) |
//                            ((array[i * 8 + 5] & 0xFFL) << 16) |
//                            ((array[i * 8 + 6] & 0xFFL) << 8) |
//                            (array[i * 8 + 7] & 0xFFL)
//            );
//        }
//    }
//
//    // read r bits of the registers from a specified bit location and return it as a byte.
//    public byte readRegister(int index) {
//        int registerIndex = index / regPerDatatype;
//        int registerEndOffset = (DT_WIDTH - 1) - (PAD + ((index % regPerDatatype) * r) + (r - 1));
//        return (byte) ((this.registers[registerIndex] >>> registerEndOffset) & MASK);
//    }
//
//    // write the rightmost r bits of given byte value to a specified bit location in the registers.
//    public void writeRegister(byte value, int index) {
//        int registerIndex = index / regPerDatatype;
//        int registerEndOffset = (DT_WIDTH - 1) - (PAD + ((index % regPerDatatype) * r) + (r - 1));
//        this.registers[registerIndex] &= ~(MASK << registerEndOffset);
//        this.registers[registerIndex] |= ((value & 0xFFL) << registerEndOffset);
//    }
//
//    public void add(long value) {
//        int bucket = (int) (value >>> (64 - p));
//        value = value | 1L << (64 - p);
//        int cnt = Long.numberOfTrailingZeros(value) + 1;
//
//        if(cnt > maxRegisterValue)
//            cnt = maxRegisterValue;
//
//        int prev = readRegister(bucket) & 0xFF;
//        if(prev < cnt)
//            writeRegister((byte) cnt, bucket);
//    }
//
//    public void merge(HLL3 other) {
////        assert(this.m == other.m);
//
//        for(int i = 0; i < m; ++i) {
//            long word = 0;
//
//            for(int j = 0; j < regPerDatatype; ++j) {
//                long mask = MASK << (r * j);
//                long thisVal = this.registers[i] & mask;
//                long thatVal = other.registers[i] & mask;
//                word |= (thisVal < thatVal) ? thatVal : thisVal;
////                word |= ((thisVal ^ Long.MIN_VALUE) >= (thatVal ^ Long.MIN_VALUE)) ? thisVal : thatVal;
//            }
//
//            this.registers[i] = word;
//        }
//    }
//
//    public long estimate() {
//        double M = totalRegisters;
//        double sum = 0;
//        double zeroRegisters = 0;
//        int tmp = 0;
//
//        for(int i=0; i<this.registers.length; i++) {
//            for(int j=regPerDatatype -1; j>=0; j--) {
//                if((tmp) >= totalRegisters)
//                    break;
//
//                long mask = MASK << (r * j);
//                int k = (int) ((this.registers[i] & mask) >>> (r * j));
//                zeroRegisters += ((k == 0) ? 1 : 0);
//                sum = sum + PRE_POW_2_K[k];
//                tmp++;
//            }
//        }
//        double alphaM = getAlphaM((int) M);
//        double rawEstimate = alphaM * M * M * (1 / sum);
////        System.err.println(rawEstimate);
//
//        if(rawEstimate <= (5.0 * M / 2.0) && zeroRegisters > 0) {
//            rawEstimate = M * Math.log(M / zeroRegisters);
//        } else if(rawEstimate > ((1.0 / 30.0) * POW_2_32)) {
//            rawEstimate = -POW_2_32 * Math.log(1 - rawEstimate / POW_2_32);
//        }
//
//        return (long) rawEstimate;
//    }
//
//    private double getAlphaM(int M) {
//        switch (M) {
//            case 16:
//                return 0.673;
//            case 32:
//                return 0.697;
//            case 64:
//                return 0.709;
//            default:
//                return 0.7213 / (1 + (1.079 / M));
//        }
//    }
//
//    public byte[] serialize() {
//        int size = m * 8 + 2;
//
//        byte[] array = new byte[size];
//        int j = 0;
//        for(int i=0; i<m; i++) {
//            array[j++] = (byte) (registers[i] >>> 56 & 0xFF);
//            array[j++] = (byte) (registers[i] >>> 48 & 0xFF);
//            array[j++] = (byte) (registers[i] >>> 40 & 0xFF);
//            array[j++] = (byte) (registers[i] >>> 32 & 0xFF);
//            array[j++] = (byte) (registers[i] >>> 24 & 0xFF);
//            array[j++] = (byte) (registers[i] >>> 16 & 0xFF);
//            array[j++] = (byte) (registers[i] >>> 8 & 0xFF);
//            array[j++] = (byte) (registers[i] & 0xFF);
//        }
//
//        array[m * 8] = (byte) p;
//        array[m * 8 + 1] = (byte) r;
//        return array;
//    }
//
//    public static HLL3 deserialize(byte[] array) {
//        return new HLL3(array);
//    }
//
//    void debugInfo() {
//        System.err.println("p=" + p + " r=" + r + " bytes=" + m + " M=" + totalRegisters);
//        int max = (1 << r) - 1;
//        int[] hist = new int[max + 1];
//        for (int i = 0; i < totalRegisters; i++) {
//            int v = readRegister(i) & 0xFF;
//            hist[v]++;
//        }
//        for (int i = 0; i <= max; i++)
//            System.err.printf("reg %2d: %6d%n", i, hist[i]);
//        double zero = hist[0];
//        System.err.println("zeroRegs=" + zero);
//    }
//}