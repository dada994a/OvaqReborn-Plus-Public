package net.shoreline.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric {@link ModInitializer}.
 *
 * @author OvaqRebornPlus
 * @since 1.0
 */
public class OvaqRebornMod implements ClientModInitializer {
    public static final String MOD_NAME = "OvaqReborn Plus";
    public static final String MOD_BUILD_NUMBER = BuildConfig.BUILD_IDENTIFIER;
    public static final String MOD_VER = BuildConfig.VERSION;

    /**
     * This code runs as soon as Minecraft is in a mod-load-ready state.
     * However, some things (like resources) may still be uninitialized.
     * Proceed with mild caution.
     */
    @Override
    public void onInitializeClient() {
        OvaqRebornPlus.init();
    }

    public static boolean isBaritonePresent() {
        return FabricLoader.getInstance().getModContainer("baritone").isPresent();
    }
}
