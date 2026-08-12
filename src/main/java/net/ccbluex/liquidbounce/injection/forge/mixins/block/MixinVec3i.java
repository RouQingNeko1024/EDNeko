/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.minecraft.util.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Kotlin 2 compiles {@code vec3i.x} to {@code getX()}, but MC 1.8.9 only exposes coordinate
 * fields. Adding these accessors fixes {@code NoSuchMethodError} for {@link Vec3i} and
 * subclasses such as {@link net.minecraft.util.BlockPos}.
 */
@Mixin(Vec3i.class)
public class MixinVec3i {

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int z;

    @Unique
    public int getX() {
        return x;
    }

    @Unique
    public int getY() {
        return y;
    }

    @Unique
    public int getZ() {
        return z;
    }
}
