package net.ccbluex.liquidbounce.utils.rotation;

/**
 * Universal PRNG interface - replaces java.util.random.RandomGenerator (Java 17+).
 */
public interface Prng {
    long nextLong();

    default int nextInt() {
        return (int) (nextLong() >>> 32);
    }

    default int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        long r = nextLong() >>> 33;
        long m = r * bound;
        return (int) (m >>> 32);
    }

    default double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    default float nextFloat() {
        return (float) ((nextLong() >>> 40) * 0x1.0p-24f);
    }

    default boolean nextBoolean() {
        return nextLong() < 0;
    }

    default long nextLong(long bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        long r = nextLong() >>> 1;
        long m = bound - 1;
        if ((bound & m) == 0) {
            return r & m;
        }
        for (long u = r; u - (r = u % bound) + m < 0; u = nextLong() >>> 1)
            ;
        return r;
    }

    // Box-Muller Gaussian
    default double nextGaussian() {
        double u = nextDouble();
        double v = nextDouble();
        return Math.sqrt(-2 * Math.log(u + 1e-300)) * Math.cos(6.283185307179586 * v);
    }
}