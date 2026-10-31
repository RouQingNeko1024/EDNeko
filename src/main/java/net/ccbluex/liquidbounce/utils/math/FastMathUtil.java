package net.ccbluex.liquidbounce.utils.math;

public class FastMathUtil {
    private static final float[] ATAN_TABLE = new float[65536];
    private static final float[] ASIN_TABLE = new float[65536];
    public static final float PI = roundToFloat(Math.PI);
    private static final float radToIndex = roundToFloat(651.8986469044033D);
    public static final float deg2Rad = roundToFloat(0.017453292519943295D);
    private static final float BF_SIN_TO_COS;
    private static final int BF_SIN_BITS, BF_SIN_MASK, BF_SIN_COUNT;
    private static final float BF_radFull, BF_radToIndex;
    private static final float[] BF_sinFull;

    static {
        for (int i = 0; i < ATAN_TABLE.length; i++) {
            float x = (float) i / (ATAN_TABLE[i] - 1);
            ATAN_TABLE[i] = (float) Math.atan(x);
        }
        for (int i = 0; i < 65536; ++i) {
            ASIN_TABLE[i] = (float) Math.asin((double) i / 32767.5D - 1.0D);
        }

        for (int j = -1; j < 2; ++j) {
            ASIN_TABLE[(int) (((double) j + 1.0D) * 32767.5D) & 65535] = (float) Math.asin(j);
        }

        BF_SIN_TO_COS = (float) (Math.PI * 0.5f);

        BF_SIN_BITS = 12;
        BF_SIN_MASK = ~(-1 << BF_SIN_BITS);
        BF_SIN_COUNT = BF_SIN_MASK + 1;

        BF_radFull = (float) (Math.PI * 2.0);
        BF_radToIndex = BF_SIN_COUNT / BF_radFull;

        BF_sinFull = new float[BF_SIN_COUNT];
        for (int i = 0; i < BF_SIN_COUNT; i++) {
            BF_sinFull[i] = (float) Math.sin((i + Math.min(1, i % (BF_SIN_COUNT / 4)) * 0.5) / BF_SIN_COUNT * BF_radFull);
        }
    }

    public static float roundToFloat(double d) {
        return (float) ((double) Math.round(d * 1.0E8D) / 1.0E8D);
    }

    public static float sin(float rad) {
        return BF_sinFull[(int) (rad * BF_radToIndex) & BF_SIN_MASK];
    }

    public static float cos(float rad) {
        return sin(rad + BF_SIN_TO_COS);
    }

    public static float arcSin(float value) {
        return ASIN_TABLE[(int) ((double) (value + 1.0F) * 32767.5D) & 65535];
    }

    public static float arcCos(float value) {
        return ((float) Math.PI / 2F) - ASIN_TABLE[(int) ((double) (value + 1.0F) * 32767.5D) & 65535];
    }

    public static float toAngleDegree(float angle) {
        return angle * radToIndex;
    }

    public static float toAngleRadian(float angle) {
        return angle * deg2Rad;
    }

    public static float tan(float angle) {
        return sin(angle) / cos(angle);
    }

    public static float atan(double x) {
        if (x < 0) return -atan(-x);
        int index = (int)(x * (ATAN_TABLE.length - 1));
        if (index >= ATAN_TABLE.length - 1) return ATAN_TABLE[ATAN_TABLE.length - 1];
        double y0 = ATAN_TABLE[index];
        return (float) (y0 + ((x * (ATAN_TABLE.length - 1)) - index) * (ATAN_TABLE[index + 1] - y0));
    }

    public static float atan2(double y, double x) {
        if (x == 0) return y > 0 ? PI / 2 : -PI / 2;
        if (y == 0) return x > 0 ? 0 : PI;
        return atan(y / x);
    }

    public static float sqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f375a86 - (i >> 1);  // what the fuck?
        x = Float.intBitsToFloat(i);

        for (int j = 0; j < 16; j++) {  // newton iteration
            x = x * (1.5f - xhalf * x * x);
        }

        return x;
    }

}
