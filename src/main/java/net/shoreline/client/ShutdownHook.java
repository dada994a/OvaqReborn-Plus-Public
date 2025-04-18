package net.shoreline.client;

import net.shoreline.client.api.file.ClientConfiguration;
import net.shoreline.client.impl.module.client.IRCModule;
import net.shoreline.client.init.Modules;

/**
 * @author OvaqRebornPlus
 * @since 1.0
 */
public class ShutdownHook extends Thread {
    /**
     *
     */
    public ShutdownHook() {
        setName("OvaqRebornPlus-ShutdownHook");
    }

    /**
     * This runs when the game is shutdown and saves the
     * {@link ClientConfiguration} files.
     *
     * @see ClientConfiguration#saveClient()
     */
    @Override
    public void run() {
        OvaqRebornPlus.info("Config Saveing…");
        OvaqRebornPlus.CONFIG.saveClient();
        OvaqRebornPlus.info("PRC stoping…");
        OvaqRebornPlus.RPC.stopRPC();

        if (Modules.IRC.isEnabled() && IRCModule.chat.isConnected()) {
            IRCModule.chat.disconnect();
        }
    }
}
