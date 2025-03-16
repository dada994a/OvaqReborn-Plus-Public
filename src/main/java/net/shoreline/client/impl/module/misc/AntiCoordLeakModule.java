package net.shoreline.client.impl.module.misc;

import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.event.listener.EventListener;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.command.BaritoneCommand;
import net.shoreline.client.impl.event.gui.chat.ChatMessageEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;

/**
 * @author OvaqRebornPlus
 * @since 1.0
 */
public class AntiCoordLeakModule extends ConcurrentModule {
    private static final String WARNING_MESSAGE = "はいお前座標貼ろうとしたね。戦犯";
    private static final String SERVER_ALERT_MESSAGE = "このメッセージはアホが座標を流出させようとしたことを示すメッセージです。 ｜ ᴼᵛᵃᵠᴿᵉᵇᵒʳⁿ⁺ ᴮᵉᵗᵃ";

    private final Config<Boolean> alert = new BooleanConfig("Alert", "For Tikuwa", true);

    public AntiCoordLeakModule() {
        super("AntiCoordLeak", "For Tikuwa", ModuleCategory.MISC);
    }

    @EventListener
    public void onChatMessage(ChatMessageEvent.Client event) {
        String message = event.getMessage().toLowerCase();

        if (tikuwamoment(message)) {
            event.cancel();
            ChatUtil.clientSendMessage(WARNING_MESSAGE);

            if (alert.getValue()) {
                ChatUtil.serverSendMessage(SERVER_ALERT_MESSAGE);
            }
        }
    }

    private boolean tikuwamoment(String message) {
        String commandPrefix = Managers.COMMAND.getPrefix();
        if (message.contains("/") || message.contains(commandPrefix) || message.contains("#")) {
            return false;
        }
        return message.matches(".*(?:-?\\d{1,6}\\s*[,\\s]\\s*){2}-?\\d{1,6}.*");
    }
}
