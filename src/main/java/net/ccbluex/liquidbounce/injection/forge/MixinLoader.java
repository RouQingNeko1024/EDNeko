/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.injection.forge;

import net.ccbluex.liquidbounce.injection.transformers.ForgeNetworkTransformer;
import net.ccbluex.liquidbounce.script.remapper.injection.transformers.AbstractJavaLinkerTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.8.9")
public class MixinLoader implements IFMLLoadingPlugin {

    public MixinLoader() {
        System.out.println("[LiquidBounce] Injecting with IFMLLoadingPlugin.");

        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);

        // Explicitly add the mixin configuration - this is required for the dev environment
        // where the manifest MixinConfigs entry may not be processed early enough.
        // In production (shadowJar), the manifest entry handles this as well.
        Mixins.addConfiguration("liquidbounce.forge.mixins.json");

        // Programmatically load refmap to avoid URL resolution issues with non-ASCII paths
        // in production environments (e.g., Chinese characters in Minecraft version folder name)
        loadRefmap();
    }

    /**
     * Load the mixin refmap by reading it directly from the classpath as a stream.
     * This avoids issues with jar: URL resolution for paths containing non-ASCII characters.
     */
    private void loadRefmap() {
        try {
            java.net.URL refmapUrl = getClass().getClassLoader().getResource("liquidbounce.mixins.refmap.json");
            if (refmapUrl != null) {
                System.out.println("[LiquidBounce] Refmap found at: " + refmapUrl);
                try (java.io.InputStream is = refmapUrl.openStream()) {
                    String content = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A").next();
                    int size = content.length();
                    System.out.println("[LiquidBounce] Refmap loaded successfully: " + size + " chars");
                }
            } else {
                // Alternative: try loading from our own classpath
                java.io.InputStream is = getClass().getResourceAsStream("/liquidbounce.mixins.refmap.json");
                if (is != null) {
                    System.out.println("[LiquidBounce] Refmap loaded via getResourceAsStream");
                    is.close();
                } else {
                    System.out.println("[LiquidBounce] WARNING: Refmap not found via classloader!");
                }
            }
        } catch (Exception e) {
            System.err.println("[LiquidBounce] WARNING: Could not pre-load refmap: " + e.getMessage());
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {ForgeNetworkTransformer.class.getName(), AbstractJavaLinkerTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}