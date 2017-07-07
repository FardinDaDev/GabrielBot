package gabrielbot.utils;

import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings("all")
public class Randoms {
    public static final SecureRandom RANDOM, NATIVE_RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstance("SHA1PRNG");
            NATIVE_RANDOM = SecureRandom.getInstance("NativePRNG");
            Executors.newSingleThreadScheduledExecutor(r->{
                Thread t = new Thread(r, "Random Seeder");
                t.setDaemon(true);
                return t;
            }).scheduleAtFixedRate(()->RANDOM.setSeed(NATIVE_RANDOM.generateSeed(8)), 1, 1, TimeUnit.MINUTES);
        } catch(Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static int nextInt() {
        return RANDOM.nextInt();
    }

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static long nextLong() {
        return RANDOM.nextLong();
    }

    public static boolean nextBoolean() {
        return RANDOM.nextBoolean();
    }

    public static float nextFloat() {
        return RANDOM.nextFloat();
    }

    public static double nextDouble() {
        return RANDOM.nextDouble();
    }

    public static double nextGaussian() {
        return RANDOM.nextGaussian();
    }

    public static IntStream ints(long streamSize) {
        return RANDOM.ints(streamSize);
    }

    public static IntStream ints() {
        return RANDOM.ints();
    }

    public static IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        return RANDOM.ints(streamSize, randomNumberOrigin, randomNumberBound);
    }

    public static IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        return RANDOM.ints(randomNumberOrigin, randomNumberBound);
    }

    public static LongStream longs(long streamSize) {
        return RANDOM.longs(streamSize);
    }

    public static LongStream longs() {
        return RANDOM.longs();
    }

    public static LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        return RANDOM.longs(streamSize, randomNumberOrigin, randomNumberBound);
    }

    public static LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        return RANDOM.longs(randomNumberOrigin, randomNumberBound);
    }

    public static DoubleStream doubles(long streamSize) {
        return RANDOM.doubles(streamSize);
    }

    public static DoubleStream doubles() {
        return RANDOM.doubles();
    }

    public static DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        return RANDOM.doubles(streamSize, randomNumberOrigin, randomNumberBound);
    }

    public static DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        return RANDOM.doubles(randomNumberOrigin, randomNumberBound);
    }
}
