/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.client;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public final class ResourcePackIconScaler {

    private static final int PACK_ICON_SIZE = 64;

    private ResourcePackIconScaler() {
    }

    public static BufferedImage scale(BufferedImage image) {
        if (image == null || image.getWidth() == PACK_ICON_SIZE && image.getHeight() == PACK_ICON_SIZE) {
            return image;
        }

        BufferedImage scaledImage = new BufferedImage(PACK_ICON_SIZE, PACK_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = scaledImage.getGraphics();
        graphics.drawImage(image, 0, 0, PACK_ICON_SIZE, PACK_ICON_SIZE, null);
        graphics.dispose();
        return scaledImage;
    }
}
