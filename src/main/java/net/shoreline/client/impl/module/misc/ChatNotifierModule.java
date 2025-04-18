package net.shoreline.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.event.listener.EventListener;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.world.AddEntityEvent;
import net.shoreline.client.impl.event.world.RemoveEntityEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.impl.manager.anticheat.AntiCheatManager;

public class ChatNotifierModule extends ToggleModule {

    Config<Boolean> totemPopConfig = new BooleanConfig("TotemPop", "Notifies in chat when a player pops a totem", true);
    Config<Boolean> visualRangeConfig = new BooleanConfig("VisualRange", "Notifies in chat when player enters visual range", false);
    Config<Boolean> friendsConfig = new BooleanConfig("Friends", "Notifies for friends", false);
    Config<Boolean> grimConfig = new BooleanConfig("GrimCheck", "Notifies for GrimAC", false);

    public ChatNotifierModule() {
        super("ChatNotifier", "Notifies in chat", ModuleCategory.MISC);
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event) {
        if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING && totemPopConfig.getValue()) {
            Entity entity = packet.getEntity(mc.world);
            if (!(entity instanceof LivingEntity) || entity.getDisplayName() == null) {
                return;
            }
            int totems = Managers.TOTEM.getTotems(entity);
            String playerName = entity.getDisplayName().getString();
            boolean isFriend = Managers.SOCIAL.isFriend(playerName);
            if (isFriend && !friendsConfig.getValue() || entity == mc.player) {
                return;
            }
            ChatUtil.clientSendMessage((isFriend ? "§b " : "§s") + playerName + "§f popped §s" + totems + "§f totems");
        }
    }

    @EventListener
    public void onAddEntity(AddEntityEvent event) {
        if (!visualRangeConfig.getValue() || !(event.getEntity() instanceof PlayerEntity) || event.getEntity().getDisplayName() == null) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Managers.SOCIAL.isFriend(playerName);
        if (isFriend && !friendsConfig.getValue() || event.getEntity() == mc.player) {
            return;
        }
        ChatUtil.clientSendMessageRaw("§s[VisualRange] " + (isFriend ? "§b" + playerName : playerName) + "§f joind your visual range");
    }

    @EventListener
    public void onRemoveEntity(RemoveEntityEvent event) {
        if (!visualRangeConfig.getValue() || !(event.getEntity() instanceof PlayerEntity) || event.getEntity().getDisplayName() == null) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Managers.SOCIAL.isFriend(playerName);
        if (isFriend && !friendsConfig.getValue() || event.getEntity() == mc.player) {
            return;
        }
        ChatUtil.clientSendMessageRaw("§s[VisualRange] " + (isFriend ? "§b" + playerName : "§c" + playerName) + "§f left your visual range");
    }

    @EventListener
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getDisplayName() == null) {
            return;
        }
        int totems = Managers.TOTEM.getTotems(event.getEntity());
        if (totems == 0) {
            return;
        }
        String playerName = event.getEntity().getDisplayName().getString();
        boolean isFriend = Managers.SOCIAL.isFriend(playerName);
        if (isFriend && !friendsConfig.getValue() || event.getEntity() == mc.player) {
            return;
        }
        ChatUtil.clientSendMessage((isFriend ? "§b" : "§s") + playerName + "§f died after popping §s" + totems + "§f totems");

        if (grimConfig.getValue() && Managers.ANTICHEAT.isGrim()) {
            ChatUtil.clientSendMessage("§cServer is running GrimAC.");

        }

    }
}
