package net.shoreline.client.impl.module.combat;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.event.listener.EventListener;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.client.util.world.EndCrystalUtil;

import java.util.List;

/**
 * @author OvaqReborn
 * @since 1.0
 */
public final class OffHandModule extends ToggleModule {
    private static final int INVENTORY_SYNC_ID = 0;
    private static final List<Item> HOTBAR_ITEMS = List.of(
            Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);

    EnumConfig<OffhandItem> itemConfig = new EnumConfig<>("Item", "The item to wield in your offhand", OffhandItem.CRYSTAL, OffhandItem.values());
    NumberConfig<Float> healthConfig = new NumberConfig<>("Health", "The health required to fall below before swapping to a totem", 0.0f, 14.0f, 20.0f);
    BooleanConfig gappleConfig = new BooleanConfig("OffhandGapple", "If to equip a golden apple if holding down the item use button", true);
    BooleanConfig crappleConfig = new BooleanConfig("Crapple", "If to use a normal golden apple if Absorption is present", true);
    Config<Boolean> lethalConfig = new BooleanConfig("Lethal", "Calculate lethal damage sources", false);
    Config<Boolean> fastConfig = new BooleanConfig("FastSwap", "Allows you to swap using faster packets", false);
    private int lastSlot;

    public OffHandModule() {
        super("Offhand", "Automatically manages the offhand item in combat", ModuleCategory.COMBAT);
    }

    @Override
    public String getModuleData() {
        ItemStack offhandItem = mc.player.getOffHandStack();
        if (offhandItem.isEmpty()) {
            return "None";
        } else {
            return " " + offhandItem.getItem().getName().getString() + " " + offhandItem.getCount();
        }
    }

    @Override
    public void onEnable() {
        lastSlot = -1;
    }

    @EventListener
    public void onPlayerTick(final PlayerTickEvent event) {
        if (mc.currentScreen != null) {
            return;
        }

        final Item itemToWield = getItemToWield();
        if (PlayerUtil.isHolding(itemToWield, Hand.OFF_HAND)) {
            return;
        }

        final int itemSlot = getSlotFor(itemToWield);
        if (itemSlot != -1) {
            if (itemSlot < 9) {
                lastSlot = itemSlot;
            }

            if (fastConfig.getValue()) {
                mc.interactionManager.clickSlot(INVENTORY_SYNC_ID,
                        itemSlot < 9 ? itemSlot + 36 : itemSlot, 40, SlotActionType.SWAP, mc.player);
            } else {
                mc.interactionManager.clickSlot(INVENTORY_SYNC_ID, itemSlot < 9 ? itemSlot + 36 : itemSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(INVENTORY_SYNC_ID, 45, 0, SlotActionType.PICKUP, mc.player);
                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    mc.interactionManager.clickSlot(INVENTORY_SYNC_ID,
                            itemSlot < 9 ? itemSlot + 36 : itemSlot, 0, SlotActionType.PICKUP, mc.player);
                }
            }
        }
    }

    private int getSlotFor(final Item item) {
        if (lastSlot != -1 && item.equals(mc.player.getInventory().getStack(lastSlot).getItem())) {
            int slot = lastSlot;
            lastSlot = -1;
            return slot;
        }

        final int startSlot = HOTBAR_ITEMS.contains(item) ? 0 : 9;

        for (int slot = 35; slot >= startSlot; slot--) {
            final ItemStack itemStack = mc.player.getInventory().getStack(slot);
            if (!itemStack.isEmpty() && itemStack.getItem().equals(item)) {
                return slot;
            }
        }
        return -1;
    }

    private Item getItemToWield() {
        final float health = PlayerUtil.getLocalPlayerHealth();
        if (health <= healthConfig.getValue()) {
            return Items.TOTEM_OF_UNDYING;
        }

        if (PlayerUtil.computeFallDamage(mc.player.fallDistance, 1.0f) + 1.0f > mc.player.getHealth()) {
            return Items.TOTEM_OF_UNDYING;
        }

        if (lethalConfig.getValue()) {
            final List<Entity> entities = Lists.newArrayList(mc.world.getEntities());
            for (Entity e : entities) {
                if (e == null || !e.isAlive() || !(e instanceof EndCrystalEntity crystal)) {
                    continue;
                }
                if (mc.player.squaredDistanceTo(e) > 140.0) {
                    continue;
                }
                double potential = EndCrystalUtil.getDamageTo(mc.player, crystal.getPos());
                if (health + 1.5 > potential) {
                    continue;
                }
                return Items.TOTEM_OF_UNDYING;
            }
        }

        if (gappleConfig.getValue() && mc.options.useKey.isPressed() &&
                (mc.player.getMainHandStack().getItem() instanceof SwordItem
                        || mc.player.getMainHandStack().getItem() instanceof TridentItem
                        || mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
            return getGoldenAppleType();
        }

        return itemConfig.getValue().getItem();
    }

    private Item getGoldenAppleType() {
        if (crappleConfig.getValue() && mc.player.hasStatusEffect(StatusEffects.ABSORPTION)
                && InventoryUtil.hasItemInInventory(Items.GOLDEN_APPLE, true)) {
            return Items.GOLDEN_APPLE;
        }
        return Items.ENCHANTED_GOLDEN_APPLE;
    }

    private enum OffhandItem {
        TOTEM(Items.TOTEM_OF_UNDYING),
        GAPPLE(Items.ENCHANTED_GOLDEN_APPLE),
        CRYSTAL(Items.END_CRYSTAL);

        private final Item item;

        OffhandItem(Item item) {
            this.item = item;
        }

        public Item getItem() {
            return item;
        }
    }
}
