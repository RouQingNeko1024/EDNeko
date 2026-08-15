package net.ccbluex.liquidbounce.utils.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.security.SecureRandom;

import static net.ccbluex.liquidbounce.utils.extensions.PlayerExtensionKt.getNearestPointBB;
import static org.apache.commons.lang3.RandomUtils.nextFloat;

public class AimAssistRotationUtil {
    public static Minecraft mc = Minecraft.getMinecraft();

    public static Rotation getRotations(Vec3 from_, Vec3 to){
        double x = to.xCoord - from_.xCoord;
        double y = to.yCoord - from_.yCoord;
        double z = to.zCoord - from_.zCoord;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (MathHelper.atan2(z, x) * 180.0 / Math.PI - 90);
        float pitch = (float) -(MathHelper.atan2(y, dist) * 180.0 / Math.PI);
        return new Rotation(yaw, pitch);
    }

    public static double [] heuristics( Entity entity, double [] xyz) {
        double boxSize = 0.2;
        float f11 = entity.getCollisionBorderSize();
        double minX = MathHelper.clamp_double(
                xyz[0] - boxSize, entity.getEntityBoundingBox().minX - (double)f11, entity.getEntityBoundingBox().maxX + (double)f11
        );
        double minY = MathHelper.clamp_double(
                xyz[1] - boxSize, entity.getEntityBoundingBox().minY - (double)f11, entity.getEntityBoundingBox().maxY + (double)f11
        );
        double minZ = MathHelper.clamp_double(
                xyz[2] - boxSize, entity.getEntityBoundingBox().minZ - (double)f11, entity.getEntityBoundingBox().maxZ + (double)f11
        );
        double maxX = MathHelper.clamp_double(
                xyz[0] + boxSize, entity.getEntityBoundingBox().minX - (double)f11, entity.getEntityBoundingBox().maxX + (double)f11
        );
        double maxY = MathHelper.clamp_double(
                xyz[1] + boxSize, entity.getEntityBoundingBox().minY - (double)f11, entity.getEntityBoundingBox().maxY + (double)f11
        );
        double maxZ = MathHelper.clamp_double(
                xyz[2] + boxSize, entity.getEntityBoundingBox().minZ - (double)f11, entity.getEntityBoundingBox().maxZ + (double)f11
        );
        xyz[0] = MathHelper.clamp_double(xyz[0] + randomSin(), minX, maxX);
        xyz[1] = MathHelper.clamp_double(xyz[1] + randomSin(), minY, maxY);
        xyz[2] = MathHelper.clamp_double(xyz[2] + randomSin(), minZ, maxZ);
        return xyz;
    }
    public static float randomSin(){
        return MathHelper.sin(nextFloat(0F, (float) (2F * Math.PI)));
    }


    public static Vec3 getNearestHitVec(final Entity entity, boolean heuristics){
        final Vec3 positionEyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 result= getNearestPointBB(positionEyes, entity.getEntityBoundingBox());
        if(!heuristics)return result;
        else{
            double[] pq = heuristics(entity, new double[]{result.xCoord,result.yCoord,result.zCoord});
            return new Vec3(pq[0],pq[1],pq[2]);
        }
    }

    public static  float[] face(
            Vec3 pointToFace,
            float yawSpeed,
            float pitchSpeed,
            float currentYaw,
            float currentPitch,
            float randomizeStrength
    ) {
        Vec3 point = pointToFace;
        if(Math.random()<=0.08){ //别转了。
            yawSpeed*=0.04F;pitchSpeed*= 0.03F;
        }
        float calcYaw,calcPitch;
        float[] current = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        Rotation origin = RotationUtils.INSTANCE.getRotations(point.xCoord, point.yCoord, point.zCoord);
        float[] target = new float[]{origin.getYaw(), origin.getPitch()};
        float[] finalR = smoothRotation(target, current, yawSpeed, pitchSpeed, point);
        calcYaw=finalR[0];
        calcPitch=finalR[1];
        calcYaw += randomizeStrength * (getInRange(-1, 1) + getInRange(-1, 1) * getInRange(0, 1) + getInRange(-1, 1) * getInRange(0, 1) * getInRange(0, 1) + getInRange(-1, 1) * getInRange(0, 1) * getInRange(0, 1) * getInRange(0, 1) + getInRange(-1, 1) * getInRange(0, 1) * getInRange(0, 1) * getInRange(0, 1) * getInRange(0, 1));
        calcPitch +=randomizeStrength* 0.96981317007977318F*(getInRange(-1, 1)+getInRange(-1, 1)*getInRange(0, 1)+getInRange(-1, 1)*getInRange(0, 1)*getInRange(0, 1)+getInRange(-1, 1)*getInRange(0, 1)*getInRange(0, 1)*getInRange(0, 1)+getInRange(-1, 1)*getInRange(0, 1)*getInRange(0, 1)*getInRange(0, 1)*getInRange(0, 1));
        float fixed = (float)((double)calcPitch + 1.356526526*Math.sin(0.06981317007977318 * (double)(updateRotation(currentYaw, calcYaw, 180.0F) - calcYaw)) * 8.0);
        if(!Float.isNaN(fixed)) calcPitch=fixed;
        if ((double)mc.gameSettings.mouseSensitivity == 0.5) {
            mc.gameSettings.mouseSensitivity = 0.47887325F;
        }

        float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f2 = f1 * f1 * f1 * 8.0F;
        int deltaX = (int)((6.667 * (double)calcYaw - 6.667 * (double)currentYaw) / (double)f2);
        int deltaY = (int)((6.667 * (double)calcPitch - 6.667 * (double)currentPitch) / (double)f2) * -1;
        float f5 = (float)deltaX * f2;
        float f3 = (float)deltaY * f2;
        calcYaw = (float)((double)currentYaw + (double)f5 * 0.15);
        float f4 = (float)((double)currentPitch - (double)f3 * 0.15);
        calcPitch = MathHelper.clamp_float(f4, -90.0F, 90.0F);


        return new float[]{calcYaw, calcPitch};
    }
    public static  float[] face(
            EntityLivingBase entity,
            float yawSpeed,
            float pitchSpeed,
            float currentYaw,
            float currentPitch,
            boolean heuristics,
            boolean shortStop,
            float P1Max,
            float P1Min,
            float entropyFactor,
            float randomizeStrength
    ) {
        if(shortStop&&(Math.random()<=0.08)){ //别转了。
            yawSpeed*=0.04F;pitchSpeed*= 0.03F;
        }
        float calcYaw,calcPitch;
        Vec3 targetPos = getNearestHitVec(entity, heuristics);
        float[] current = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        Rotation origin = RotationUtils.INSTANCE.getRotations(targetPos.xCoord, targetPos.yCoord, targetPos.zCoord);
        float[] target = new float[]{origin.getYaw(), origin.getPitch()};
        float[] finalR = smoothRotation(target, current, yawSpeed, pitchSpeed, entity, P1Max, P1Min, entropyFactor);
        calcYaw=finalR[0];
        calcPitch=finalR[1];
        calcYaw += randomizeStrength * (getInRange(-1, 1) + getInRange(-1, 1) * getInRange(0, 1) + getInRange(-1, 1) * getInRange(0, 1) * getInRange(0, 1) + getInRange(-1, 1) * getInRange(0, 1) * getInRange(0, 1) * getInRange(0, 1) + getInRange(-1, 1) * getInRange(0, 1) * getInRange(0, 1) * getInRange(0, 1) * getInRange(0, 1));
        calcPitch +=randomizeStrength* 0.96981317007977318F*(getInRange(-1, 1)+getInRange(-1, 1)*getInRange(0, 1)+getInRange(-1, 1)*getInRange(0, 1)*getInRange(0, 1)+getInRange(-1, 1)*getInRange(0, 1)*getInRange(0, 1)*getInRange(0, 1)+getInRange(-1, 1)*getInRange(0, 1)*getInRange(0, 1)*getInRange(0, 1)*getInRange(0, 1));
        float fixed = (float)((double)calcPitch + 1.356526526*Math.sin(0.06981317007977318 * (double)(updateRotation(currentYaw, calcYaw, 180.0F) - calcYaw)) * 8.0);
        if(!Float.isNaN(fixed)) calcPitch=fixed;
        if ((double)mc.gameSettings.mouseSensitivity == 0.5) {
            mc.gameSettings.mouseSensitivity = 0.47887325F;
        }

        float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f2 = f1 * f1 * f1 * 8.0F;
        int deltaX = (int)((6.667 * (double)calcYaw - 6.667 * (double)currentYaw) / (double)f2);
        int deltaY = (int)((6.667 * (double)calcPitch - 6.667 * (double)currentPitch) / (double)f2) * -1;
        float f5 = (float)deltaX * f2;
        float f3 = (float)deltaY * f2;
        calcYaw = (float)((double)currentYaw + (double)f5 * 0.15);
        float f4 = (float)((double)currentPitch - (double)f3 * 0.15);
        calcPitch = MathHelper.clamp_float(f4, -90.0F, 90.0F);


        return new float[]{calcYaw, calcPitch};
    }
    public static float getInRange(float min, float max){
        return min + (float) (Math.random() * (max - min));
    }
    public static float updateRotation(final float current, final float calc, final float maxDelta) {
        float f = MathHelper.wrapAngleTo180_float(calc - current);
        if (f > maxDelta) {
            f = maxDelta;
        }
        if (f < -maxDelta) {
            f = -maxDelta;
        }
        return current + f;
    }

    public static SecureRandom random = new SecureRandom();

    public static float[] smoothRotation(float[] currentRotations, float[] targetRotations, float deltaYaw, float deltaPitch, Vec3 point) {
        float smoothFactor = MathHelper.clamp_float((float) (1.0 - mc.thePlayer.getDistance(point.xCoord, point.yCoord, point.zCoord) / 15.0), 0.3F, 0.9F);


        targetRotations = new float[]{targetRotations[0], targetRotations[1]};

        float deltaYawDiff = MathHelper.wrapAngleTo180_float(targetRotations[0] - currentRotations[0]);
        float deltaPitchDiff = targetRotations[1] - currentRotations[1];
        float controlYaw1 = currentRotations[0] + deltaYawDiff * 0.25F;
        float controlYaw2 = currentRotations[0] + deltaYawDiff * 0.75F;
        float controlPitch1 = currentRotations[1] + deltaPitchDiff * 0.25F;
        float controlPitch2 = currentRotations[1] + deltaPitchDiff * 0.75F;

        float invT = 1.0F - smoothFactor;
        float yaw = invT * invT * invT * currentRotations[0] +
                3 * invT * invT * smoothFactor * controlYaw1 +
                3 * invT * smoothFactor * smoothFactor * controlYaw2 +
                smoothFactor * smoothFactor * smoothFactor * targetRotations[0];
        float pitch = invT * invT * invT * currentRotations[1] +
                3 * invT * invT * smoothFactor * controlPitch1 +
                3 * invT * smoothFactor * smoothFactor * controlPitch2 +
                smoothFactor * smoothFactor * smoothFactor * targetRotations[1];

        yaw = currentRotations[0] + MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(yaw - currentRotations[0]), -deltaYaw, deltaYaw);
        pitch = currentRotations[1] + MathHelper.clamp_float(pitch - currentRotations[1], -deltaPitch, deltaPitch);

        return new float[]{yaw, pitch};
    }

    public static float[] smoothRotation(float[] currentRotations, float[] targetRotations, float deltaYaw, float deltaPitch, EntityLivingBase target, float maxP, float minP, float enF) {
        double speed = Math.sqrt(target.motionX * target.motionX + target.motionY * target.motionY + target.motionZ * target.motionZ);
        float smoothFactor = MathHelper.clamp_float((float) (1.0 - mc.thePlayer.getDistanceToEntity(target) / 15.0 + speed * 0.3), 0.3F, 0.9F);

        double perturbationYaw = (random.nextDouble() * (maxP-minP) + minP) * enF;
        double perturbationPitch = (random.nextDouble() * (maxP-minP) + minP) * enF;
        perturbationYaw *= random.nextBoolean() ? 1 : -1;
        perturbationPitch *= random.nextBoolean() ? 1 : -1;

        targetRotations = new float[]{targetRotations[0] + (float) perturbationYaw, targetRotations[1] + (float) perturbationPitch};

        float deltaYawDiff = MathHelper.wrapAngleTo180_float(targetRotations[0] - currentRotations[0]);
        float deltaPitchDiff = targetRotations[1] - currentRotations[1];
        float controlYaw1 = currentRotations[0] + deltaYawDiff * 0.25F;
        float controlYaw2 = currentRotations[0] + deltaYawDiff * 0.75F;
        float controlPitch1 = currentRotations[1] + deltaPitchDiff * 0.25F;
        float controlPitch2 = currentRotations[1] + deltaPitchDiff * 0.75F;

        float invT = 1.0F - smoothFactor;
        float yaw = invT * invT * invT * currentRotations[0] +
                3 * invT * invT * smoothFactor * controlYaw1 +
                3 * invT * smoothFactor * smoothFactor * controlYaw2 +
                smoothFactor * smoothFactor * smoothFactor * targetRotations[0];
        float pitch = invT * invT * invT * currentRotations[1] +
                3 * invT * invT * smoothFactor * controlPitch1 +
                3 * invT * smoothFactor * smoothFactor * controlPitch2 +
                smoothFactor * smoothFactor * smoothFactor * targetRotations[1];

        yaw = currentRotations[0] + MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(yaw - currentRotations[0]), -deltaYaw, deltaYaw);
        pitch = currentRotations[1] + MathHelper.clamp_float(pitch - currentRotations[1], -deltaPitch, deltaPitch);

        return new float[]{yaw, pitch};
    }

}
