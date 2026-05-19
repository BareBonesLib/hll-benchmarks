package io.github.bareboneslib.hll_benchmarks;

import java.util.Random;

public class RandomFactory {

    private final Random random;

    /**
     * Creates a RandomFactory with seed based on current time
     */
    public RandomFactory() {
//        this(System.currentTimeMillis());
        this(999L);
    }

    /**
     * Creates a RandomFactory with specified seed
     *
     * @param seed the seed for random number generation
     */
    public RandomFactory(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Returns the next random double value between 0.0 (inclusive) and 1.0 (exclusive)
     *
     * @return random double value [0.0, 1.0)
     */
    public double nextDouble() {
        return random.nextDouble();
    }

    /**
     * Returns the next random float value between 0.0 (inclusive) and 1.0 (exclusive)
     *
     * @return random float value [0.0, 1.0)
     */
    public float nextFloat() {
        return random.nextFloat();
    }

    /**
     * Returns a batch of random double values between 0.0 (inclusive) and 1.0 (exclusive)
     *
     * @param size the number of random values to generate
     * @return array of random double values
     */
    public double[] batchDouble(int size) {
        double[] batch = new double[size];
        for (int i = 0; i < size; i++) {
            batch[i] = random.nextDouble();
        }
        return batch;
    }

    /**
     * Returns a batch of random float values between 0.0 (inclusive) and 1.0 (exclusive)
     *
     * @param size the number of random values to generate
     * @return array of random float values
     */
    public float[] batchFloat(int size) {
        float[] batch = new float[size];
        for (int i = 0; i < size; i++) {
            batch[i] = random.nextFloat();
        }
        return batch;
    }

    /**
     * Fills an existing array with random double values between 0.0 and 1.0
     *
     * @param array the array to fill
     */
    public void fillDouble(double[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextDouble();
        }
    }

    /**
     * Fills an existing array with random float values between 0.0 and 1.0
     *
     * @param array the array to fill
     */
    public void fillFloat(float[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextFloat();
        }
    }
}
