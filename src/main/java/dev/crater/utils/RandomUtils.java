package dev.crater.utils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {
    private static ThreadLocalRandom instance() {
        return ThreadLocalRandom.current();
    }

    // --------
    // Integers
    // --------

    public static int randomInt(int origin, int bound) {
        return instance().nextInt(origin, bound);
    }

    public static int randomInt(int bound) {
        return instance().nextInt(bound);
    }

    public static int randomInt() {
        return instance().nextInt();
    }

    // -----
    // Longs
    // -----

    public static long randomLong(long origin, long bound) {
        return instance().nextLong(origin, bound);
    }

    public static long randomLong(long bound) {
        return instance().nextLong(bound);
    }

    public static long randomLong() {
        return instance().nextLong();
    }

    // ------
    // Floats
    // ------

    public static float randomFloat(float origin, float bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }

        float r = (float)((randomInt()) >>> 8) * 5.9604645E-8F;
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound) {
                r = Float.intBitsToFloat(Float.floatToIntBits(bound) - 1);
            }
        }

        return r;
    }

    public static float randomFloat(float bound) {
        if (bound <= 0.0F) {
            throw new IllegalArgumentException("bound must be positive");
        }

        float r = (float)((randomInt()) >>> 8) * 5.9604645E-8F;
        return r < bound ? r : Float.intBitsToFloat(Float.floatToIntBits(bound) - 1);
    }

    public static float randomFloat() {
        return instance().nextFloat();
    }

    // --------
    // Doubles
    // --------

    public static double randomDouble(double origin, double bound) {
        return instance().nextDouble(origin, bound);
    }

    public static double randomDouble(double bound) {
        return instance().nextDouble(bound);
    }

    public static double randomDouble() {
        return instance().nextDouble();
    }

    // -----
    // Misc.
    // -----

    public static boolean randomBoolean() {
        return instance().nextBoolean();
    }

    public static byte[] randomBytes(int length) {
        byte[] arr = new byte[length];
        instance().nextBytes(arr);
        return arr;
    }

    public static byte[] randomBytes() {
        byte[] arr = new byte[randomInt(0xFFFF)];
        instance().nextBytes(arr);
        return arr;
    }
}
