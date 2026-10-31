package net.ccbluex.liquidbounce.utils.math;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;

public class AimAnalysisEngine {
    public static Minecraft mc = Minecraft.getMinecraft();

    public static AimAnalysisEngine aimAnalysisEngineInstance = new AimAnalysisEngine();

    public enum HeuristicsResult{
        NotConstant,
        HeuristicBypass,
        Randomization,
        RotationConsistency,
        ConstantEntropy,
        LowVariance,
        RegularSpeed,
        SuspiciousAccel,
        Straight,
        Straight2,
        MouseLength,
        UnnaturalShake,
        HistoricDuplicate
    }

    public AimAnalysisEngine(){
    }
    public static AimResult newResult(HeuristicsResult result, String description){
        return new AimResult(result, description);
    }
    public static double scale(double a, double b, double c){
        return a/b*c;
    }

    public static boolean shortTermAnalysis(Vec2f now, Vec2f last, Vec2f lastAccel) {
        float deltaYaw = now.getX();
        float lastDYaw = last.getX();
        float lastDPitch = last.getY();
        float deltaPitch = Math.abs(now.getY());
        float yawAccel = Math.abs(deltaYaw - last.getX());
        float pitchAccel = deltaPitch - last.getY();
        float DYAccel = Math.abs(lastAccel.getX() - yawAccel);
        float DPAccel = Math.abs(lastAccel.getY() - pitchAccel);
        //Is hacking?
        return (lastDYaw==deltaYaw&&deltaYaw>25f)||
                (Math.abs(deltaYaw-deltaPitch)<7.5) ||
                (lastDPitch==deltaPitch&&deltaPitch>25f)||
                (deltaYaw > 15.0F && (double)Math.abs(deltaPitch) < 0.1D) ||
                (Math.abs(deltaYaw) < 0.6 && Math.abs(deltaPitch) > 7.825f) ||
                (yawAccel > 20.0F && (double)Math.abs(pitchAccel) < 0.05D) ||
                (Math.abs(DYAccel) > 35.0F || Math.abs(DPAccel) > 35.0F) ||
                (deltaYaw > 15F && Math.abs(Math.floor(deltaYaw) - (double)deltaYaw) < 1.0E-10D && ((int)deltaYaw)%15==0) ||
                (Math.abs(deltaYaw) > 30.0F && (double)Math.abs(deltaPitch) < 0.05D);
    }
    public ArrayList<AimResult> analyze(ArrayList<float[]> angleData) {
        ArrayList<AimResult> results = new ArrayList<>();
        ArrayList<AimResult> legitResult = new ArrayList<>();
        legitResult.add(null);


        float[] yaws = new float[angleData.size()];
        float[] pitches = new float[angleData.size()];
        for (int i = 0; i < angleData.size(); i++) {
            yaws[i] = angleData.get(i)[0];
            pitches[i] = angleData.get(i)[1];
        }

        float[] yawSpeeds = getDeltaRotation(yaws);
        float[] pitchSpeeds = getDeltaRotation(pitches);
        float[] yawAccelerations = getDeltaRotation(yawSpeeds);
        float[] pitchAccelerations = getDeltaRotation(yawSpeeds);
        ArrayList<Float> pitchSpeeds2 = new ArrayList<>();
        double avgYaw=getAverageDeltaRotation(yaws);
        double avgPitch=getAverageDeltaRotation(pitches);
        for (int i = 1; i < angleData.size(); i++) {
            if((angleData.get(i)[0]-angleData.get(i-1)[0])<30&&(angleData.get(i)[0]-angleData.get(i-1)[0])>0&&(angleData.get(i)[1]-angleData.get(i-1)[1])<30&&(angleData.get(i)[1]-angleData.get(i-1)[1])>0) {
                pitchSpeeds2.add(angleData.get(i)[1] - angleData.get(i - 1)[1]);
            }
        }
        //检测随机化的第二种方式
        if(tooRandomize(yawSpeeds, pitchSpeeds)) {
            results.add(newResult(HeuristicsResult.Randomization, "The player's rotation is extremely randomized[AvgAccel=("+String.format("%.2f",ComplexMath.getAverage(ComplexMath.toList(yawAccelerations)))+","+String.format("%.2f",ComplexMath.getAverage(ComplexMath.toList(pitchAccelerations)))+")]"));
        }


        //如果玩家在尝试模拟鼠标移动
        //这个时候yaw明显偏大, pitch非常小
        if(hasFlaw(yawSpeeds, pitchSpeeds)) {
            results.add(newResult(HeuristicsResult.SuspiciousAccel, "The player‘s rotation has flaw"));
        }
        //旋转时候pitch速度接近不可能
        if(hasFlaw2(yawSpeeds, pitchSpeeds)) {
            results.add(newResult(HeuristicsResult.Straight, "The player aims straight[AvgDelta=("+String.format("%.2f",ComplexMath.getAverage(ComplexMath.toList(yawSpeeds)))+","+String.format("%.2f",ComplexMath.getAverage(ComplexMath.toList(pitchSpeeds)))+")]"));
        }
        //旋转的时候pitch速度过于精确
        if(hasFlaw3(yawSpeeds, pitchSpeeds)) {
            results.add(newResult(HeuristicsResult.Straight2, "The player tries to simulate legit aiming"));
        }
        
        //检测过于随机化, 这对于搭路有明显效果, 但我们无法用相同的方式测试Aura
        if((getSnapRotationAmount(yaws)>3&&getSnapRotationAmount(pitches)>3&&(avgYaw<3&&avgPitch<3)&&avgYaw>0.15)) {
            results.add(newResult(HeuristicsResult.HeuristicBypass, "The player tries to bypass randomization check"));
        }

        //数据统计部分必须要有足够的转头速度
        if(avgYaw<8.375)return results.isEmpty()?legitResult:results;
        double yawVariance = getVarianceRotation(yawSpeeds);
        double pitchVariance = getVarianceRotation(pitchSpeeds);

        double yawMovedDistance = 0;
        for (float velocity : yawSpeeds) {
            yawMovedDistance+=velocity;
        }
        //玩家滑动距离很长后一定会停顿一下(移动鼠标),然而机器不会这么做
        if(yawMovedDistance>480F&&ComplexMath.getDataLowerThanX(8, yawSpeeds).length<=1) {
            results.add(newResult(HeuristicsResult.MouseLength, "The player scrolls his/her mouse over "+ yawMovedDistance+"° without stopping(MinLength=+"+ComplexMath.getMin(yawSpeeds)+")"));
        }
        ArrayList<Float> yawHistory = new ArrayList<>();
        for (int i = 0; i < yawSpeeds.length-1; i++) { //yawSpeeds大小比yaws小1, 我们可以确保不会出现异常
            if(yawSpeeds[i]>30&&yawSpeeds[i+1]<10) {
                yawHistory.add(yaws[i+1]);
            }
        }
        //首先需要明确: 黑客在进行telly bridge时候平滑转回总是在快速转头以后立即停止到几乎相同的地方
        //因此如果一个玩家上一1tick数据进行了高速转头(大于30°)
        //我们将其加入受观察的历史yaws列表中
        //我们需要做的只是查看其中是否存在多个重合的Yaw值
        int maxDuplicateYaw = getDuplicateMax(yawHistory);
        if(maxDuplicateYaw>1) {
            results.add(newResult(HeuristicsResult.HistoricDuplicate, "The player's rotation is duplicated in history[Duplicate="+maxDuplicateYaw+"]"));
        }

        //如果玩家加速度全部接近平均转头速度的一半, 那么他很有可能使用了不合法的随机化
        if(ComplexMath.getAverage(ComplexMath.toList(yawAccelerations))<avgYaw/2&&Math.abs(avgYaw/2-ComplexMath.getAverage(ComplexMath.toList(yawAccelerations)))<0.1) {
            results.add(newResult(HeuristicsResult.UnnaturalShake, "The player's rotation is circular"));
        }


        //极小方差检测
        if((yawVariance<.03||pitchVariance<.03)) {
            results.add(newResult(HeuristicsResult.LowVariance, "The player's rotation is VERY smooth[Variance=("+String.format("%.2f",yawVariance)+","+String.format("%.2f",pitchVariance)+")]"));
        }


        if(angleData.size()<20)return results.isEmpty()?legitResult:results; //需要充足数据


        final int distinct = ComplexMath.getDistinct(pitchSpeeds2);
        final int duplicates = pitchSpeeds2.size() - distinct;
        // 如果玩家旋转速度中重复项过少, 则他没有一个好旋转常数
        final double average = pitchSpeeds2.stream().mapToDouble(d -> d).average().orElse(0.0);
        if((average<30f&&duplicates<=1&&pitchSpeeds2.size()>20)) {
            results.add(newResult(HeuristicsResult.NotConstant, "The player's pitch is not constant"));
        }

        //正常入转头方差应该小于360? 平均加减速18.97?
        //这不会发生!除了有XiaoSB乱转头或者他只是卡了
        //通常乱转头的玩家只会有一方面突出
        if(((yawVariance>360&&pitchVariance<0.01)||(yawVariance<0.01&&pitchVariance>360))) {
            results.add(newResult(HeuristicsResult.RotationConsistency, "The player's rotation is inconsistent[Variance=("+String.format("%.2f",yawVariance)+","+String.format("%.2f",pitchVariance)+")]"));
        }

        //过于完美的香农熵
        if((Math.abs(ComplexMath.getShannonEntropy(splitList(ComplexMath.toList(yawSpeeds)).get(0))- ComplexMath.getShannonEntropy(splitList(ComplexMath.toList(yawSpeeds)).get(1)))<1E-4&
                Math.abs(ComplexMath.getShannonEntropy(splitList(ComplexMath.toList(pitchSpeeds)).get(0))- ComplexMath.getShannonEntropy(splitList(ComplexMath.toList(pitchSpeeds)).get(1)))<1E-4
        )) {
            results.add(newResult(HeuristicsResult.ConstantEntropy, "The player's rotation entropy is constant[Average="+String.format("%.2f",ComplexMath.getAverage(ComplexMath.toList(yawSpeeds)))+"]"));
        } //在这里我把列表分成两份检查, 因为不储存上一次的AimPos信息
        //if((Math.abs(ComplexMath.getShannonEntropy(ComplexMath.toList(yawSpeeds))- ComplexMath.getShannonEntropy(ComplexMath.toList(pitchSpeeds)))<1E-4)) {
        //    results.add(newResult(HeuristicsResult.SimilarEntropy, "The player's rotation entropy is similar[Entropy=("+String.format("%.2f",ComplexMath.getShannonEntropy(ComplexMath.toList(yawSpeeds)))+","+String.format("%.2f",ComplexMath.getShannonEntropy(ComplexMath.toList(pitchSpeeds)))+")]"));
        //}

        //周期性峰值检测
        if(getPeakCount(yawSpeeds)>3) {
            results.add(newResult(HeuristicsResult.RegularSpeed, "The player's rotation speed is regular(Peak Count="+getPeakCount(yawSpeeds)+")"));
        }
        return results.isEmpty()?legitResult:results;
    }
    public static int getDuplicateMax(ArrayList<Float> list){
        int k=0;
        for (float v : list)
            if(getDuplicate(list, v)>k)k=getDuplicate(list, v);
        return k;
    }
    public static int getDuplicate(ArrayList<Float> list, float p){
        int k=0;
        for (float v : list) {
            if(Math.abs(v-p)<0.67f)k++; //magic value
        }
        return k;
    }
    public boolean tooRandomize(float[] yaw, float[] pitch){
        int a=0,b=0;
        for (int i = 1; i < yaw.length; i++) {
            float yawChange=yaw[i],pitchChange=pitch[i], yawDifference=Math.abs(yaw[i]-yaw[i-1]), pitchDifference=Math.abs(pitch[i]-pitch[i-1]);
            if(yawChange > yawDifference && yawDifference > 0.0 && yawDifference < 0.1 && pitchChange > 0.08)a++;
            if(yawChange > yawDifference && yawDifference > 0.0 && pitchChange > 0 && pitchChange < 0.02 && pitchDifference > pitchChange * 2)b++;
        }
        return a>3||b>3;
    }
    public boolean hasFlaw(float[] yaw, float[] pitch){
        for (int i = 1; i < yaw.length; i++) {
            if(yaw[i]>Math.abs(yaw[i]-yaw[i-1])&&Math.abs(yaw[i]-yaw[i-1])>0.3&&pitch[i]>0&&pitch[i] <= Math.abs(pitch[i]-pitch[i-1]) && Math.abs(pitch[i]-pitch[i-1]) < 0.1)return true;
        }
        return false;
    }
    public boolean hasFlaw2(float[] yaw, float[] pitch){
        int flag=0;
        for (int i = 1; i < yaw.length; i++) {
            if(Math.abs(yaw[i])>15 && Math.abs(pitch[i]) < 0.1)flag++;
        }
        return flag>4;
    }
    public boolean hasFlaw3(float[] yaw, float[] pitch){
        for (int i = 1; i < yaw.length; i++) {
            if(yaw[i]>1.95f&&yaw[i]<yaw[i-1]&&pitch[i] < 0.0700001F && pitch[i] > 0.0015f)return true;
        }
        return false;
    }
    public float[] getDeltaRotation(float[] angleData){
        float[] result = new float[angleData.length-1];
        for (int i = 1; i < angleData.length; i++) {
            result[i-1]=Math.abs(MathHelper.wrapAngleTo180_float(angleData[i]-angleData[i-1]));
        }
        return result;
    }
    public float getAverageDeltaRotation(float[] angleData){
        float yawSum=0;
        for (int i = 1; i < angleData.length; i++) {
            yawSum+= Math.abs(MathHelper.wrapAngleTo180_float(angleData[i]-angleData[i-1]));
        }
        return yawSum/angleData.length;
    }

    public float getVarianceRotation(float[] angleData){
        float avg= (float) ComplexMath.getAverage(ComplexMath.toList(angleData));
        float delta=0;
        for (float angle : angleData) {
            delta+=(angle-avg)*(angle-avg);
        }
        return  delta/angleData.length;
    }
    public static <T> ArrayList<ArrayList<T>> splitList(ArrayList<T> originalList) {
        ArrayList<ArrayList<T>> result = new ArrayList<>();
        int size = originalList.size();
        int half = (size + 1) / 2;  // 处理奇数长度情况

        result.add(new ArrayList<>(originalList.subList(0, half)));
        result.add(new ArrayList<>(originalList.subList(half, size)));

        return result;
    }

    public int getPeakCount(float[] angleData){
        double[] autocorrelation = new double[angleData.length/2];
        for (int lag = 1; lag < autocorrelation.length; lag++) {
            double sum = 0;
            for (int i = 0; i < angleData.length - lag; i++) {
                sum += angleData[i] * angleData[i + lag];
            }
            autocorrelation[lag] = sum / (angleData.length - lag);
        }

        //判断峰值规律性
        int peakCount = 0;
        for (int i = 2; i < autocorrelation.length - 2; i++) {
            if (autocorrelation[i] > autocorrelation[i-1] &&
                    autocorrelation[i] > autocorrelation[i-2] &&autocorrelation[i] > autocorrelation[i+2] &&
                    autocorrelation[i] > autocorrelation[i+1]) {
                peakCount++;
            }
        }
        return peakCount;
    }
    public int getSnapRotationAmount(float[] angleData){
        int c=0;
        for (int i = 0; i < angleData.length-3; i++) {
            if(isSnapRotation(angleData[i], angleData[i+1],angleData[i+2]))c++;
        }
        return c;
    }
    public boolean isSnapRotation(float y1, float y2, float y3){
        return getAverageDeltaRotation(new float[]{y1,y2,y3})>6&&Math.abs(y3-y2)>1.5*Math.abs(y2-y1)&&direction(y3-y2)!=direction(y2-y1);
    }
    public static int direction(float p){
        return p==0?0: (int) Math.floor(p / Math.abs(p));
    }
}