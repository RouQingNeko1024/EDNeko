package net.ccbluex.liquidbounce.utils.math;

import com.google.common.collect.Lists;
import java.util.AbstractMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ComplexMath {
    public static final double EXPANDER = Math.pow(2.0, 24.0);

    public static float[] getDataLowerThanX(float x, float[] y){
        ArrayList<Float> r = new ArrayList<>();
        for (float v : y) {
            if(v<x) r.add(v);
        }
        float[] k = new float[r.size()];
        for (int i = 0; i < r.size(); i++) {
            k[i] = r.get(i);
        }
        return k;
    }

    public static double getMax(float[] p){
        double q=-2147483647;
        for (float v : p) {
            q=Math.max(v,q);
        }
        return q;
    }
    public static double getMin(float[] p){
        double q=2147483647;
        for (float v : p) {
            q=Math.min(v,q);
        }
        return q;
    }
    public static short getMin(ArrayList<Short> p){
        short q=32767;
        for (short v : p) {
            q= (short) Math.min(v,q);
        }
        return q;
    }

    public static AbstractMap.SimpleEntry<List<Double>, List<Double>> getOutliers(final Collection<? extends Number> collection) {
        final List<Double> values = new ArrayList<>();

        for (final Number number : collection) {
            values.add(number.doubleValue());
        }

        final double q1 = getMedian(values.subList(0, values.size() / 2));
        final double q3 = getMedian(values.subList(values.size() / 2, values.size()));

        final double iqr = Math.abs(q1 - q3);
        final double lowThreshold = q1 - 1.5 * iqr, highThreshold = q3 + 1.5 * iqr;

        final AbstractMap.SimpleEntry<List<Double>, List<Double>> tuple = new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>());

        for (final Double value : values) {
            if (value < lowThreshold) {
                tuple.getKey().add(value);
            } else if (value > highThreshold) {
                tuple.getValue().add(value);
            }
        }

        return tuple;
    }

    public static float getGCD(double s) {
        float f1 = (float) ((float) s * 0.6 + 0.2);
        return f1 * f1 * f1 * 8.0F;
    }

    public static float getGCDValue(double s) {
        return getGCD(s) * 0.15F;
    }

    public static ArrayList<Float> getZScoreOutliers(final Collection<? extends Float> data, float threshold) {
        ArrayList<Float> outliers = new ArrayList<>();
        float mean = (float) getAverage(data);
        float stdDev = (float) getStandardDeviation(data);

        for (Number number : data) {
            float zScore = (number.floatValue() - mean) / stdDev;
            if (Math.abs(zScore) > threshold) {
                outliers.add((Float) number);
            }
        }

        return outliers;
    }

    public static float[] getEquidistantSequence(int length, float p){
        float[] r = new float[length];
        for (int i = 0; i < length; i++) {
            r[i]=(i+1)*p;
        }
        return r;
    }
    public static double[] linearRegression(List<? extends Number> x, List<? extends Number> y) {
        int n = x.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double xi = x.get(i).doubleValue();
            double yi = y.get(i).doubleValue();
            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    public static List<Float> getJiffDelta(List<? extends Number> data, int depth) {
        List<Float> result = new ArrayList<>();
        for (Number n : data) result.add(n.floatValue());
        for (int i = 0; i < depth; i++) {
            List<Float> calculate = new ArrayList<>();
            float old = Float.MIN_VALUE;
            for (float n : result) {
                if (old == Float.MIN_VALUE) {
                    old = n;
                    continue;
                }
                calculate.add(Math.abs(Math.abs(n) - Math.abs(old)));
                old = n;
            }
            result = new ArrayList<>(calculate);
        }
        return result;
    }
    public static ArrayList<Float> toList(float[] i){
        ArrayList<Float> result = new ArrayList<>();
        for (float v : i) {
            result.add(v);
        }
        return result;
    }
    public static double getShannonEntropy(final Collection<? extends Number> data) {
        Map<Double, Long> freqMap = data.stream()
                .collect(Collectors.groupingBy(Number::doubleValue, Collectors.counting()));

        double total = data.size();
        return -freqMap.values().stream()
                .mapToDouble(count -> (count / total) * (Math.log(count / total) / Math.log(2)))
                .sum();
    }

    public static double hypot(final double x, final double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static double magnitude(final double x, final double y, final double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static String trim(final double x) {
        return new DecimalFormat("#.##").format(x);
    }

    public static float distanceBetweenAngles(final float alpha, final float beta) {
        final float alphaX = alpha % 360.0f;
        final float betaX = beta % 360.0f;
        final float delta = Math.abs(alphaX - betaX);
        return (float)Math.abs(Math.min(360.0 - delta, delta));
    }

    public static double getVariance(final Collection<? extends Number> data) {
        int count = 0;
        double sum = 0.0;
        double variance = 0.0;
        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
        }
        final double average = sum / count;
        for (final Number number : data) {
            variance += Math.pow(number.doubleValue() - average, 2.0);
        }
        return variance;
    }

    public static double getStandardDeviation(final Collection<? extends Number> data) {
        final double variance = getVariance(data);
        return Math.sqrt(variance);
    }

    public static double getSkewness(final Collection<? extends Number> data) {
        double sum = 0.0;
        int count = 0;
        final List<Double> numbers = Lists.newArrayList();
        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
            numbers.add(number.doubleValue());
        }
        Collections.sort(numbers);
        final double mean = sum / count;
        final double median = (count % 2 != 0) ? numbers.get(count / 2) : ((numbers.get((count - 1) / 2) + numbers.get(count / 2)) / 2.0);
        final double variance = getVariance(data);
        return 3.0 * (mean - median) / variance;
    }

    public static ArrayList<Double> toArrayList(Double[] t){
        return new ArrayList<>(Arrays.asList(t));
    }
    public static double getAverageDelta(final Collection<? extends Number> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        double ls=0;
        boolean first=true;
        for (final Number number : data) {
            if(first){
                ls=number.doubleValue();
                first=false;
                continue;
            }
            sum += number.doubleValue()-ls;
            ls=number.doubleValue();
        }
        return sum / (data.size()-1);
    }

    public static float interpolate(float newValue, float oldValue, float partialTicks) {
        return oldValue + (newValue - oldValue) * partialTicks;
    }

    public static double interpolate(double newValue, double oldValue, float partialTicks) {
        return oldValue + (newValue - oldValue) * partialTicks;
    }

    public static double interpolate(double newValue, double oldValue, double partialTicks) {
        return oldValue + (newValue - oldValue) * partialTicks;
    }

    public static double getAverage(final Collection<? extends Number> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (final Number number : data) {
            sum += number.doubleValue();
        }
        return sum / data.size();
    }

    public static double getKurtosis(final Collection<? extends Number> data) {
        double sum = 0.0;
        int count = 0;
        for (final Number number : data) {
            sum += number.doubleValue();
            ++count;
        }
        if (count < 3.0) {
            return 0.0;
        }
        final double efficiencyFirst = count * (count + 1.0) / ((count - 1.0) * (count - 2.0) * (count - 3.0));
        final double efficiencySecond = 3.0 * Math.pow(count - 1.0, 2.0) / ((count - 2.0) * (count - 3.0));
        final double average = sum / count;
        double variance = 0.0;
        double varianceSquared = 0.0;
        for (final Number number2 : data) {
            variance += Math.pow(average - number2.doubleValue(), 2.0);
            varianceSquared += Math.pow(average - number2.doubleValue(), 4.0);
        }
        return efficiencyFirst * (varianceSquared / Math.pow(variance / sum, 2.0)) - efficiencySecond;
    }
    public static double round(final double value, final int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    public static int getMode(final Collection<? extends Number> array) {
        int mode = (int)array.toArray()[0];
        int maxCount = 0;
        for (final Number value : array) {
            int count = 1;
            for (final Number i : array) {
                if (i.equals(value)) {
                    ++count;
                }
                if (count > maxCount) {
                    mode = (int)value;
                    maxCount = count;
                }
            }
        }
        return mode;
    }

    private static double getMedian(final List<Double> data) {
        if (data.size() % 2 == 0) {
            return (data.get(data.size() / 2) + data.get(data.size() / 2 - 1)) / 2.0;
        }
        return data.get(data.size() / 2);
    }

    public static boolean isExponentiallySmall(final Number number) {
        return number.doubleValue() < 1.0 && Double.toString(number.doubleValue()).contains("E");
    }

    public static long getGcd(final long current, final long previous) {
        return (previous <= 16384L) ? current : getGcd(previous, current % previous);
    }
    public static double gcd(final double limit, final double a, final double b) {
        return b <= limit ? a : gcd(limit, b, a % b);
    }

    public static double getGcd(final double a, final double b) {
        if (a < b) {
            return getGcd(b, a);
        }
        if (Math.abs(b) < 0.001) {
            return a;
        }
        return getGcd(b, a - Math.floor(a / b) * b);
    }

    public static double angleOf(final double minX, final double minZ, final double maxX, final double maxZ) {
        final double deltaY = minZ - maxZ;
        final double deltaX = maxX - minX;
        final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
        return (result < 0.0) ? (360.0 + result) : result;
    }

    public static double getDistanceBetweenAngles360(final double alpha, final double beta) {
        final double abs = Math.abs(alpha % 360.0 - beta % 360.0);
        return Math.abs(Math.min(360.0 - abs, abs));
    }

    public static double getDistanceBetweenAngles360Raw(final double alpha, final double beta) {
        return Math.abs(alpha % 360.0 - beta % 360.0);
    }

    public static double getCps(final Collection<? extends Number> data) {
        return 20.0 / getAverage(data) * 50.0;
    }

    public static int getDuplicates(final Collection<? extends Number> data) {
        return (int)(data.size() - data.stream().distinct().count());
    }

    public static int getDistinct(final Collection<? extends Number> data) {
        return (int)data.stream().distinct().count();
    }

    private ComplexMath() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}