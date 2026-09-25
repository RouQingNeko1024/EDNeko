package net.ccbluex.liquidbounce.injection.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Mixin configuration plugin that loads the refmap from classpath using getResourceAsStream,
 * bypassing URL resolution issues with non-ASCII paths.
 * <p>
 * Mixin's default refmap loading uses new URL(configURL, refmapName) to construct the refmap URL.
 * On some systems with non-ASCII characters in the path (e.g., Chinese characters in Windows username),
 * this URL resolution fails and the refmap is not loaded, causing "Shadow field was not located" errors.
 * <p>
 * This plugin loads the refmap content through ClassLoader.getResourceAsStream() which handles
 * non-ASCII paths correctly, and injects the mappings into the MixinConfig's ReferenceMapper
 * in the preApply callback (before any mixin transformations).
 * <p>
 * We cannot inject in onLoad() because Mixins.getConfigs() doesn't include this config yet
 * when onLoad() is called. preApply() is called after all configs are registered and before
 * any mixin transformation, making it the ideal injection point.
 */
public class MixinRefmapPlugin implements IMixinConfigPlugin {

    private static final Map<String, Map<String, String>> refmapData = new HashMap<>();
    private static volatile boolean dataLoaded = false;
    private static volatile boolean injectionDone = false;

    static {
        loadRefmapData();
    }

    /**
     * Load refmap data from classpath using getResourceAsStream.
     * This avoids URL encoding issues with non-ASCII paths that occur when Mixin
     * tries to resolve the refmap URL relative to the config URL.
     */
    private static void loadRefmapData() {
        if (dataLoaded) return;

        InputStream is = null;
        try {
            // Try multiple classloaders to find the refmap resource
            is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("liquidbounce.mixins.refmap.json");

            if (is == null) {
                is = MixinRefmapPlugin.class.getResourceAsStream("/liquidbounce.mixins.refmap.json");
            }

            if (is == null) {
                is = ClassLoader.getSystemResourceAsStream("liquidbounce.mixins.refmap.json");
            }

            if (is != null) {
                try (Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
                    String content = scanner.hasNext() ? scanner.next() : "";
                    parseRefmapJson(content);
                    System.out.println("[LiquidBounce] Refmap loaded from classpath stream: "
                            + refmapData.size() + " classes mapped");
                }
            } else {
                System.err.println("[LiquidBounce] WARNING: Refmap not found via any classpath resource method!");
            }
        } catch (Exception e) {
            System.err.println("[LiquidBounce] WARNING: Failed to load refmap data: " + e.getMessage());
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
            dataLoaded = true;
        }
    }

    private static void parseRefmapJson(String jsonContent) {
        try {
            JsonObject root = new JsonParser().parse(jsonContent).getAsJsonObject();
            JsonObject mappings = root.getAsJsonObject("mappings");
            if (mappings == null) {
                System.err.println("[LiquidBounce] Refmap JSON has no 'mappings' field");
                return;
            }

            for (Map.Entry<String, JsonElement> classEntry : mappings.entrySet()) {
                String className = classEntry.getKey();
                JsonObject classMappings = classEntry.getValue().getAsJsonObject();
                Map<String, String> memberMappings = new HashMap<>();
                for (Map.Entry<String, JsonElement> memberEntry : classMappings.entrySet()) {
                    memberMappings.put(memberEntry.getKey(), memberEntry.getValue().getAsString());
                }
                refmapData.put(className, memberMappings);
            }
        } catch (Exception e) {
            System.err.println("[LiquidBounce] Failed to parse refmap JSON: " + e.getMessage());
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        // Note: We cannot access Mixins.getConfigs() here because the config hasn't been
        // added to the list yet. Config is added AFTER onLoad() returns.
        // We defer refmap injection to preApply() where the config is available.
        if (refmapData.isEmpty()) {
            System.err.println("[LiquidBounce] WARNING: No refmap data loaded - mixin shadow fields may fail!");
        } else {
            System.out.println("[LiquidBounce] Refmap data ready (" + refmapData.size()
                    + " classes) - will inject in preApply");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // preApply is called before the mixin transformation is applied.
        // By this time, all configs are registered and we can safely inject the refmap data.
        if (injectionDone || refmapData.isEmpty()) return;

        System.out.println("[LiquidBounce] Injecting refmap data (triggered by preApply for " + mixinClassName + ")...");

        try {
            // Get the MixinConfig from the mixin info - this is our config since the plugin
            // is registered on our mixin config
            IMixinConfig mixinConfig = mixinInfo.getConfig();
            if (mixinConfig == null) {
                System.err.println("[LiquidBounce] mixinConfig is null!");
                return;
            }

            // Use reflection to access the private refMapper field
            Field refMapperField = mixinConfig.getClass().getDeclaredField("refMapper");
            refMapperField.setAccessible(true);
            Object refMapper = refMapperField.get(mixinConfig);

            if (refMapper == null) {
                System.err.println("[LiquidBounce] refMapper is null for MixinConfig!");
                return;
            }

            // Access the private mappings field in ReferenceMapper
            Field mappingsField = refMapper.getClass().getDeclaredField("mappings");
            mappingsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> existingMappings =
                    (Map<String, Map<String, String>>) mappingsField.get(refMapper);

            if (existingMappings == null) {
                System.err.println("[LiquidBounce] mappings field is null!");
                return;
            }

            // Skip if already populated (e.g., someone else already loaded the refmap)
            if (existingMappings.size() > 0) {
                System.out.println("[LiquidBounce] Refmap already has "
                        + existingMappings.size() + " class mappings, skipping injection");
                injectionDone = true;
                return;
            }

            // Inject our refmap data into the ReferenceMapper's mappings
            int injectedCount = 0;
            for (Map.Entry<String, Map<String, String>> classEntry : refmapData.entrySet()) {
                String className = classEntry.getKey();
                if (!existingMappings.containsKey(className)) {
                    existingMappings.put(className, new HashMap<>(classEntry.getValue()));
                    injectedCount++;
                }
            }

            injectionDone = true;
            System.out.println("[LiquidBounce] Refmap injected successfully: "
                    + injectedCount + " class mappings added (total: " + existingMappings.size() + ")");
        } catch (NoSuchFieldException e) {
            System.err.println("[LiquidBounce] Refmap injection failed - field not found: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[LiquidBounce] Refmap injection failed: " + e.getMessage());
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // If injection didn't happen in preApply (unlikely), try here
        if (!injectionDone && !refmapData.isEmpty()) {
            preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }
}