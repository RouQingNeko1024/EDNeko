/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.client;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class WeakCapeImageBuffer implements IImageBuffer {

    private static Method parseCapeMethod;
    private static Method setLocationOfCapeMethod;

    private final WeakReference<AbstractClientPlayer> playerReference;
    private final ResourceLocation resourceLocation;

    public WeakCapeImageBuffer(AbstractClientPlayer player, ResourceLocation resourceLocation) {
        playerReference = new WeakReference<>(player);
        this.resourceLocation = resourceLocation;
    }

    @Override
    public BufferedImage parseUserSkin(BufferedImage image) {
        try {
            return (BufferedImage) getParseCapeMethod().invoke(null, image);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return image;
        }
    }

    @Override
    public void skinAvailable() {
        AbstractClientPlayer player = playerReference.get();

        if (player != null) {
            try {
                getSetLocationOfCapeMethod().invoke(player, resourceLocation);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
    }

    private static Method getParseCapeMethod() throws ClassNotFoundException, NoSuchMethodException {
        if (parseCapeMethod == null) {
            parseCapeMethod = Class.forName("CapeUtils").getDeclaredMethod("parseCape", BufferedImage.class);
            parseCapeMethod.setAccessible(true);
        }

        return parseCapeMethod;
    }

    private static Method getSetLocationOfCapeMethod() throws NoSuchMethodException {
        if (setLocationOfCapeMethod == null) {
            setLocationOfCapeMethod = AbstractClientPlayer.class.getDeclaredMethod("setLocationOfCape", ResourceLocation.class);
            setLocationOfCapeMethod.setAccessible(true);
        }

        return setLocationOfCapeMethod;
    }
}
