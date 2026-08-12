package net.ccbluex.liquidbounce.utils.pathfinder;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.List;

public final class CustomPathHelper {
    public static Minecraft mc = Minecraft.getMinecraft();


    public static List<BlockPos> fluxGetPath(Vec3 from, final Vec3 to) {
        final List<BlockPos> result = new ArrayList<BlockPos>();
        if (Double.isNaN(from.xCoord) || Double.isNaN(from.yCoord) || Double.isNaN(from.zCoord)) {
            return null;
        }
        if (!Double.isNaN(to.xCoord) && !Double.isNaN(to.yCoord) && !Double.isNaN(to.zCoord)) {
            final int var6 = MathHelper.floor_double(to.xCoord);
            final int var7 = MathHelper.floor_double(to.yCoord);
            final int var8 = MathHelper.floor_double(to.zCoord);
            int var9 = MathHelper.floor_double(from.xCoord);
            int var10 = MathHelper.floor_double(from.yCoord);
            int var11 = MathHelper.floor_double(from.zCoord);
            BlockPos var12 = new BlockPos(var9, var10, var11);
            new BlockPos(var6, var7, var8);
            final IBlockState var13 = mc.theWorld.getBlockState(var12);
            final Block var14 = var13.getBlock();
            if (( var14.getCollisionBoundingBox(mc.theWorld, var12, var13) != null) && var14.canCollideCheck(var13, true)) {
                final MovingObjectPosition var15 = var14.collisionRayTrace(mc.theWorld, var12, from, to);
                if (var15 != null) {
                    return result;
                }
            }
            int var17 = 320;
            while (var17-- >= 0) {
                if (Double.isNaN(from.xCoord) || Double.isNaN(from.yCoord) || Double.isNaN(from.zCoord)) {
                    return null;
                }
                if (var9 == var6 && var10 == var7 && var11 == var8) {
                    return result;
                }
                boolean var18 = true;
                boolean var19 = true;
                boolean var20 = true;
                double var21 = 999.0;
                double var22 = 999.0;
                double var23 = 999.0;
                if (var6 > var9) {
                    var21 = var9 + 1.0;
                }
                else if (var6 < var9) {
                    var21 = var9 + 0.0;
                }
                else {
                    var18 = false;
                }
                if (var7 > var10) {
                    var22 = var10 + 1.0;
                }
                else if (var7 < var10) {
                    var22 = var10 + 0.0;
                }
                else {
                    var19 = false;
                }
                if (var8 > var11) {
                    var23 = var11 + 1.0;
                }
                else if (var8 < var11) {
                    var23 = var11 + 0.0;
                }
                else {
                    var20 = false;
                }
                double var24 = 999.0;
                double var25 = 999.0;
                double var26 = 999.0;
                final double var27 = to.xCoord - from.xCoord;
                final double var28 = to.yCoord - from.yCoord;
                final double var29 = to.zCoord - from.zCoord;
                if (var18) {
                    var24 = (var21 - from.xCoord) / var27;
                }
                if (var19) {
                    var25 = (var22 - from.yCoord) / var28;
                }
                if (var20) {
                    var26 = (var23 - from.zCoord) / var29;
                }
                if (var24 == -0.0) {
                    var24 = -1.0E-4;
                }
                if (var25 == -0.0) {
                    var25 = -1.0E-4;
                }
                if (var26 == -0.0) {
                    var26 = -1.0E-4;
                }
                EnumFacing var30;
                if (var24 < var25 && var24 < var26) {
                    var30 = ((var6 > var9) ? EnumFacing.WEST : EnumFacing.EAST);
                    from = new Vec3(var21, from.yCoord + var28 * var24, from.zCoord + var29 * var24);
                }
                else if (var25 < var26) {
                    var30 = ((var7 > var10) ? EnumFacing.DOWN : EnumFacing.UP);
                    from = new Vec3(from.xCoord + var27 * var25, var22, from.zCoord + var29 * var25);
                }
                else {
                    var30 = ((var8 > var11) ? EnumFacing.NORTH : EnumFacing.SOUTH);
                    from = new Vec3(from.xCoord + var27 * var26, from.yCoord + var28 * var26, var23);
                }
                var9 = MathHelper.floor_double(from.xCoord) - ((var30 == EnumFacing.EAST) ? 1 : 0);
                var10 = MathHelper.floor_double(from.yCoord) - ((var30 == EnumFacing.UP) ? 1 : 0);
                var11 = MathHelper.floor_double(from.zCoord) - ((var30 == EnumFacing.SOUTH) ? 1 : 0);
                var12 = new BlockPos(var9, var10, var11);
                result.add(new BlockPos(var9, var10, var11));
                final IBlockState var31 = mc.theWorld.getBlockState(var12);
                final Block var32 = var31.getBlock();
                if (var32.getCollisionBoundingBox(mc.theWorld, var12, var31) == null) {
                    continue;
                }
                if (var32.canCollideCheck(var31, true)) {
                    final MovingObjectPosition var33 = var32.collisionRayTrace(mc.theWorld, var12, from, to);
                    if (var33 != null) {
                        return result;
                    }
                }
            }
            return result;
        }
        return null;
    }
    public static ArrayList<Vec3> findTeleportPathPlayerToPoint(double x,double y,double z,double dis){
        return findTeleportPathPointToPoint(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, x, y, z, dis, 1000, 4);
    }
    public static ArrayList<Vec3> findTeleportPathPointToPoint(
            double curX, double curY, double curZ,
            double tpX, double tpY, double tpZ,
            final double dashDistance, int loops, int depth) {

        Vec3 topFrom = new Vec3(curX, curY, curZ);
        Vec3 to = new Vec3(tpX, tpY, tpZ);

        if (!canPassThrow(new BlockPos(topFrom))) {
            topFrom = topFrom.addVector(0, 1, 0);
        }

        AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
        pathfinder.compute(loops, depth);

        ArrayList<Vec3> path = new ArrayList<>();
        ArrayList<Vec3> pathFinderPath = pathfinder.getPath();
        Vec3 lastLoc = null;
        Vec3 lastDashLoc = null;
        int pathSize = pathFinderPath.size();

        for (int i = 0; i < pathSize; i++) {
            Vec3 current = pathFinderPath.get(i);
            Vec3 adjustedCurrent = current.addVector(0.5, 0, 0.5);

            if (i == 0 || i == pathSize - 1) {
                if (lastLoc != null) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                }
                path.add(adjustedCurrent);
                lastDashLoc = current;
            }
            else {
                boolean canContinue = false;

                if (current.squareDistanceTo(lastDashLoc) <= dashDistance * dashDistance) {
                    canContinue = true;

                    int minX = (int) Math.min(lastDashLoc.xCoord, current.xCoord);
                    int minY = (int) Math.min(lastDashLoc.yCoord, current.yCoord);
                    int minZ = (int) Math.min(lastDashLoc.zCoord, current.zCoord);
                    int maxX = (int) Math.max(lastDashLoc.xCoord, current.xCoord);
                    int maxY = (int) Math.max(lastDashLoc.yCoord, current.yCoord);
                    int maxZ = (int) Math.max(lastDashLoc.zCoord, current.zCoord);

                    for (int x = minX; x <= maxX && canContinue; x++) {
                        for (int y = minY; y <= maxY && canContinue; y++) {
                            for (int z = minZ; z <= maxZ && canContinue; z++) {
                                if (!AStarCustomPathFinder.checkPositionValidity(x, y, z, false)) {
                                    canContinue = false;
                                }
                            }
                        }
                    }
                }

                if (!canContinue) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                    lastDashLoc = lastLoc;
                }
            }

            lastLoc = current;
        }

        return path;
    }
    public static ArrayList<Vec3> findTeleportPathPointToPoint(
            double curX, double curY, double curZ,
            double tpX, double tpY, double tpZ,
            final double dashDistance, int loops, int depth,  boolean legitimize) {

        Vec3 topFrom = new Vec3(curX, curY, curZ);
        Vec3 to = new Vec3(tpX, tpY, tpZ);

        if (!canPassThrow(new BlockPos(topFrom))) {
            topFrom = topFrom.addVector(0, 1, 0);
        }

        AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
        if(!legitimize)pathfinder.compute(loops, depth);
        else pathfinder.computeLegit(loops, depth);

        ArrayList<Vec3> path = new ArrayList<>();
        ArrayList<Vec3> pathFinderPath = pathfinder.getPath();
        Vec3 lastLoc = null;
        Vec3 lastDashLoc = null;
        int pathSize = pathFinderPath.size();

        for (int i = 0; i < pathSize; i++) {
            Vec3 current = pathFinderPath.get(i);
            Vec3 adjustedCurrent = current.addVector(0.5, 0, 0.5);

            if (i == 0 || i == pathSize - 1) {
                if (lastLoc != null) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                }
                path.add(adjustedCurrent);
                lastDashLoc = current;
            }
            else {
                boolean canContinue = false;

                if (current.squareDistanceTo(lastDashLoc) <= dashDistance * dashDistance) {
                    canContinue = true;

                    int minX = (int) Math.min(lastDashLoc.xCoord, current.xCoord);
                    int minY = (int) Math.min(lastDashLoc.yCoord, current.yCoord);
                    int minZ = (int) Math.min(lastDashLoc.zCoord, current.zCoord);
                    int maxX = (int) Math.max(lastDashLoc.xCoord, current.xCoord);
                    int maxY = (int) Math.max(lastDashLoc.yCoord, current.yCoord);
                    int maxZ = (int) Math.max(lastDashLoc.zCoord, current.zCoord);

                    for (int x = minX; x <= maxX && canContinue; x++) {
                        for (int y = minY; y <= maxY && canContinue; y++) {
                            for (int z = minZ; z <= maxZ && canContinue; z++) {
                                if (!AStarCustomPathFinder.checkPositionValidity(x, y, z, false)) {
                                    canContinue = false;
                                }
                            }
                        }
                    }
                }

                if (!canContinue) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                    lastDashLoc = lastLoc;
                }
            }

            lastLoc = current;
        }

        return path;
    }


    public static ArrayList<Vec3> findTeleportPathEntityToEntity(Entity current, Entity target, final double dashDistance, boolean legitimize) {
        double curX = current.posX;
        double curY = current.posY;
        double curZ = current.posZ;
        double tpX = target.posX;
        double tpY = target.posY;
        double tpZ = target.posZ;

        Vec3 topFrom = new Vec3(curX,curY,curZ);
        Vec3 to = new Vec3(tpX,tpY,tpZ);

        if (!canPassThrow(new BlockPos(topFrom))) {
            topFrom = topFrom.addVector(0, 1, 0);
        }
        AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
        if(!legitimize)pathfinder.compute();
        else pathfinder.computeLegit();

        int i = 0;
        Vec3 lastLoc = null;
        Vec3 lastDashLoc = null;
        ArrayList<Vec3> path = new ArrayList<>();
        ArrayList<Vec3> pathFinderPath = pathfinder.getPath();
        for (Vec3 pathElm : pathFinderPath) {
            if (i == 0 || i == pathFinderPath.size() - 1) {
                if (lastLoc != null) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                }
                path.add(pathElm.addVector(0.5, 0, 0.5));
                lastDashLoc = pathElm;
            } else {
                boolean canContinue = true;
                if (pathElm.squareDistanceTo(lastDashLoc) > dashDistance * dashDistance) {
                    canContinue = false;
                } else {
                    double smallX = Math.min(lastDashLoc.xCoord, pathElm.xCoord);
                    double smallY = Math.min(lastDashLoc.yCoord, pathElm.yCoord);
                    double smallZ = Math.min(lastDashLoc.zCoord, pathElm.zCoord);
                    double bigX = Math.max(lastDashLoc.xCoord, pathElm.xCoord);
                    double bigY = Math.max(lastDashLoc.yCoord, pathElm.yCoord);
                    double bigZ = Math.max(lastDashLoc.zCoord, pathElm.zCoord);
                    cordsLoop:
                    for (int x = (int) smallX; x <= bigX; x++) {
                        for (int y = (int) smallY; y <= bigY; y++) {
                            for (int z = (int) smallZ; z <= bigZ; z++) {
                                if (!AStarCustomPathFinder.checkPositionValidity(x, y, z, false)) {
                                    canContinue = false;
                                    break cordsLoop;
                                }
                            }
                        }
                    }
                }
                if (!canContinue) {
                    path.add(lastLoc.addVector(0.5, 0, 0.5));
                    lastDashLoc = lastLoc;
                }
            }
            lastLoc = pathElm;
            i++;
        }

        return path;
    }

    private static boolean canPassThrow(BlockPos pos) {
        Block block = Minecraft.getMinecraft().theWorld.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getBlock();
        return block.getMaterial() == Material.air || block.getMaterial() == Material.plants || block.getMaterial() == Material.vine || block == Blocks.ladder || block == Blocks.water || block == Blocks.flowing_water || block == Blocks.wall_sign || block == Blocks.standing_sign;
    }

    public static List<Vec3> findBlinkPath(final double tpX, final double tpY, final double tpZ) {
        final List<Vec3> positions = new ArrayList<>();

        double curX = mc.thePlayer.posX;
        double curY = mc.thePlayer.posY;
        double curZ = mc.thePlayer.posZ;
        double distance = Math.abs(curX - tpX) + Math.abs(curY - tpY) + Math.abs(curZ - tpZ);

        for (int count = 0; distance > 0.0D; count++) {
            distance = Math.abs(curX - tpX) + Math.abs(curY - tpY) + Math.abs(curZ - tpZ);

            final double diffX = curX - tpX;
            final double diffY = curY - tpY;
            final double diffZ = curZ - tpZ;
            final double offset = (count & 1) == 0 ? 0.4D : 0.1D;

            final double minX = Math.min(Math.abs(diffX), offset);
            if (diffX < 0.0D) curX += minX;
            if (diffX > 0.0D) curX -= minX;

            final double minY = Math.min(Math.abs(diffY), 0.25D);
            if (diffY < 0.0D) curY += minY;
            if (diffY > 0.0D) curY -= minY;

            double minZ = Math.min(Math.abs(diffZ), offset);
            if (diffZ < 0.0D) curZ += minZ;
            if (diffZ > 0.0D) curZ -= minZ;

            positions.add(new Vec3(curX, curY, curZ));
        }

        return positions;
    }

    public static List<Vec3> findPath(final double tpX, final double tpY, final double tpZ, final double offset) {
        final List<Vec3> positions = new ArrayList<>();
        final double steps = Math.ceil(getDistance(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, tpX, tpY, tpZ) / offset);

        final double dX = tpX - mc.thePlayer.posX;
        final double dY = tpY - mc.thePlayer.posY;
        final double dZ = tpZ - mc.thePlayer.posZ;

        for(double d = 1D; d <= steps; ++d) {
            positions.add(new Vec3(mc.thePlayer.posX + (dX * d) / steps, mc.thePlayer.posY + (dY * d) / steps, mc.thePlayer.posZ + (dZ * d) / steps));
        }

        return positions;
    }
    public static List<Vec3> findPathImmediately(final double fromX, final double fromY, final double fromZ, double tpX, final double tpY, final double tpZ, final double offset) {
        final List<Vec3> positions = new ArrayList<>();
        final double steps = Math.ceil(getDistance(fromX, fromY, fromZ, tpX, tpY, tpZ) / offset);

        for(double d = 1D; d <= steps; ++d) {
            positions.add(new Vec3(tpX, tpY, tpZ));
        }

        return positions;
    }
    public static List<Vec3> findPathDirectly(final double fromX, final double fromY, final double fromZ, double tpX, final double tpY, final double tpZ, final double offset) {
        final List<Vec3> positions = new ArrayList<>();
        final double steps = Math.ceil(getDistance(fromX, fromY, fromZ, tpX, tpY, tpZ) / offset);

        final double dX = tpX - fromX;
        final double dY = tpY - fromY;
        final double dZ = tpZ - fromZ;

        for(double d = 1D; d <= steps; ++d) {
            positions.add(new Vec3(fromX + (dX * d) / steps, fromY + (dY * d) / steps, fromZ + (dZ * d) / steps));
        }

        return positions;
    }

    private static double getDistance(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        final double xDiff = x1 - x2;
        final double yDiff = y1 - y2;
        final double zDiff = z1 - z2;
        return MathHelper.sqrt_double(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }
}
