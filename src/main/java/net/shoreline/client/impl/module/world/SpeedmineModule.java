package net.shoreline.client.impl.module.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.event.EventStage;
import net.shoreline.client.api.event.listener.EventListener;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.RotationModule;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.AttackBlockEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.init.Modules;
import net.shoreline.client.mixin.accessor.AccessorClientPlayerInteractionManager;
import net.shoreline.client.util.player.RotationUtil;

import java.text.DecimalFormat;

/**
 * @author OvaqRebornPlus
 * @since 1.0
 */
public class SpeedmineModule extends RotationModule {
    Config<SpeedmineMode> modeConfig = new EnumConfig<>("Mode", "The mining mode for speedmine", SpeedmineMode.PACKET, SpeedmineMode.values());
    Config<Float> mineSpeedConfig = new NumberConfig<>("Speed", "The speed to mine blocks", 0.0f, 0.7f, 0.9f, () -> modeConfig.getValue() == SpeedmineMode.DAMAGE);
    Config<Boolean> instantConfig = new BooleanConfig("Instant", "Instantly removes the mining block", false, () -> modeConfig.getValue() == SpeedmineMode.PACKET);
    Config<Float> rangeConfig = new NumberConfig<>("Range", "Range for mine", 1.0f, 4.5f, 7.0f, () -> modeConfig.getValue() == SpeedmineMode.PACKET);
    Config<Swap> swapConfig = new EnumConfig<>("AutoSwap", "Swaps to the best tool once the mining is complete", Swap.SILENT, Swap.values(), () -> modeConfig.getValue() == SpeedmineMode.PACKET);
    Config<Boolean> autoCevConfig = new BooleanConfig("AutoCev", "cev", true);
    Config<Boolean> oldCrystalConfig = new BooleanConfig("OldCrystal", "", false, () -> autoCevConfig.getValue());
    Config<Boolean> rotateConfig = new BooleanConfig("Rotate", "Rotates when mining the block", true, () -> modeConfig.getValue() == SpeedmineMode.PACKET);
    Config<Boolean> grimConfig = new BooleanConfig("Grim", "Uses grim block breaking speeds", false);
    Config<Boolean> strictConfig = new BooleanConfig("Strict", "Swaps to tool using alternative packets to bypass NCP silent swap", false, () -> swapConfig.getValue() != Swap.OFF && modeConfig.getValue() == SpeedmineMode.PACKET);
    private BlockPos mining;
    private BlockState state;
    private Direction direction;
    private boolean crystalAttack;
    private float damage;

    public SpeedmineModule() {
        super("Speedmine", "Mines faster", ModuleCategory.WORLD, 900);
    }

    @Override
    public String getModuleData() {
        DecimalFormat decimal = new DecimalFormat("0.0");
        return decimal.format(damage);
    }

    @Override
    public void onDisable() {
        mining = null;
        state = null;
        direction = null;
        crystalAttack = false;
        damage = 0.0f;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.getStage() != EventStage.PRE || modeConfig.getValue() != SpeedmineMode.DAMAGE) {
            return;
        }
        AccessorClientPlayerInteractionManager interactionManager =
                (AccessorClientPlayerInteractionManager) mc.interactionManager;
        if (interactionManager.hookGetCurrentBreakingProgress() >= mineSpeedConfig.getValue()) {
            interactionManager.hookSetCurrentBreakingProgress(2.0f);
        }
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event) {
        if (modeConfig.getValue() != SpeedmineMode.PACKET || Modules.AUTO_MINE.isEnabled() || mc.player.isCreative()) {
            return;
        }

        if (mining == null) {
            damage = 0.0f;
            return;
        }
        state = mc.world.getBlockState(mining);
        int prev = mc.player.getInventory().selectedSlot;
        int slot = Modules.AUTO_TOOL.getBestTool(state);
        double dist = mc.player.squaredDistanceTo(mining.toCenterPos());
        if (dist > ((NumberConfig<?>) rangeConfig).getValueSq()
            || state.isAir() || damage > 3.0f) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, mining, Direction.DOWN));
            mining = null;
            state = null;
            direction = null;
            damage = 0.0f;
        } else if (damage > 1.0f && !Modules.AUTO_CRYSTAL.isAttacking()
            && !Modules.AUTO_CRYSTAL.isPlacing() && !mc.player.isUsingItem()) {
            if (isRotationBlocked()) {
                return;
            }
            if (swapConfig.getValue() != Swap.OFF) {
                if (strictConfig.getValue()) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                        slot + 36, prev, SlotActionType.SWAP, mc.player);
                } else {
                    swap(slot);
                }
            }
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mining, direction));
            if (grimConfig.getValue()) {
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, mining.up(500), direction));
            }
            if (swapConfig.getValue() == Swap.SILENT) {
                if (strictConfig.getValue()) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                        slot + 36, prev, SlotActionType.SWAP, mc.player);
                } else {
                    swap(prev);
                }
            }
            damage = 0.0f;
            mining = null;
            state = null;
            direction = null;
        } else {
            float delta = calcBlockBreakingDelta(state, mc.world, mining);
            damage += delta;
            if (delta + damage > 1.0f && rotateConfig.getValue()
                && !Modules.AUTO_CRYSTAL.isAttacking()
                && !Modules.AUTO_CRYSTAL.isPlacing()) {
                float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), mining.toCenterPos());
                setRotation(rotations[0], rotations[1]);
            }
        }
    }

    /**
     * @param event
     */
    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event) {
        //
        if (event.getPacket() instanceof PlayerActionC2SPacket packet
                && packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
                && modeConfig.getValue() == SpeedmineMode.DAMAGE && grimConfig.getValue()) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().up(500), packet.getDirection()));
        }
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            damage = 0.0f;
        }
    }

    public BlockPos getBlockTarget() {
        return mining;
    }

    private void swap(int slot) {
        if (PlayerInventory.isValidHotbarIndex(slot)) {
            Managers.INVENTORY.setClientSlot(slot);
        }
    }

    @EventListener
    public void onAttackBlock(AttackBlockEvent event) {
        if (modeConfig.getValue() != SpeedmineMode.PACKET || Modules.AUTO_MINE.isEnabled()) {
            return;
        }
        if (mc.player == null || mc.world == null
                || mc.player.isCreative() || mining != null && event.getPos() == mining) {
            return;
        }
        if (mining != null) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                    mining, Direction.DOWN));
        }
        mining = event.getPos();
        direction = event.getDirection();
        damage = 0.0f;
        if (mining != null && direction != null) {
            event.cancel();
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    mining, direction));
            // Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
            //        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
            //        mining, Direction.UP));
            if (instantConfig.getValue()) {
                mc.world.removeBlock(mining, false);
            }
        }
    }

    public float calcBlockBreakingDelta(BlockState state, BlockView world,
                                        BlockPos pos) {
        if (swapConfig.getValue() == Swap.OFF) {
            return state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        }
        float f = state.getHardness(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }

    private float getBlockBreakingSpeed(BlockState block) {
        int tool = Modules.AUTO_TOOL.getBestTool(block);
        float f = mc.player.getInventory().getStack(tool).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            ItemStack stack = mc.player.getInventory().getStack(tool);
            int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }
        if (StatusEffectUtil.hasHaste(mc.player)) {
            f *= 1.0f + (float) (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float g = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            f *= g;
        }
        if (mc.player.isSubmergedIn(FluidTags.WATER)
                && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            f /= 5.0f;
        }
        if (!mc.player.isOnGround()) {
            f /= 5.0f;
        }
        return f;
    }

    private int getItemHotbar(Item item) {
        for (int i = 0; i < 9; ++i) {
            Item item2 = mc.player.getInventory().getStack(i).getItem();
            if (Item.getRawId(item2) != Item.getRawId(item)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private boolean canPlaceCrystal(BlockPos pos, boolean oldVer) {
        BlockPos boost = pos.add(0, 2, 0);
        if (mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
            return false;
        }
        BlockPos boost2 = pos.add(0, 1, 0);
        if((mc.world.getBlockState(boost).getBlock() != Blocks.AIR && mc.world.getBlockState(boost2).getBlock() != Blocks.AIR) && !oldVer) {
            return false;
        }
        for (Entity entity : mc.world.getEntitiesByClass(PlayerEntity.class, new Box(boost), entity -> true)) {
            if (entity.isAlive()) {
                if (entity instanceof EndCrystalEntity) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private PlayerEntity getPlacePlayer(BlockPos pos) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!Managers.SOCIAL.isFriend(player.getName().getString())) {
                if (player.getHealth() + player.getAbsorptionAmount() >= 0.0f || !player.isDead()) {
                    continue;
                }
                BlockPos playerPos = player.getBlockPos();
                for (Direction direction1 : Direction.Type.HORIZONTAL) {
                    if (playerPos.offset(direction1).equals(pos) || playerPos.up().offset(direction1).equals(pos)) {
                        return player;
                    }
                }
                if (playerPos.offset(Direction.UP).offset(Direction.UP).equals(pos)) {
                    return player;
                }
            }
        }
        return null;
    }

    private boolean checkCrystalPos(BlockPos pos) {
        boolean canPlace = true;
        for (Entity entity : mc.world.getEntitiesByClass(Entity.class, new Box(pos.up()), entity -> true)) {
            if (entity != null) {
                if (!entity.isAlive()) {
                    continue;
                }
                canPlace = false;
            }
        }
        return canPlace;
    }

    private boolean canHarvest(BlockState state) {
        if (state.isToolRequired()) {
            int tool = Modules.AUTO_TOOL.getBestTool(state);
            return mc.player.getInventory().getStack(tool).isSuitableFor(state);
        }
        return true;
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (mining == null || state == null || mc.player.isCreative()
                || modeConfig.getValue() != SpeedmineMode.PACKET) {
            return;
        }
        VoxelShape outlineShape = state.getOutlineShape(mc.world, mining);
        if (outlineShape.isEmpty()) {
            return;
        }
        Box render1 = outlineShape.getBoundingBox();
        Box render = new Box(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
        Vec3d center = render.getCenter();
        float scale = MathHelper.clamp(damage, 0.0f, 1.0f);
        if (scale > 1.0f) {
            scale = 1.0f;
        }
        double dx = (render1.maxX - render1.minX) / 2.0;
        double dy = (render1.maxY - render1.minY) / 2.0;
        double dz = (render1.maxZ - render1.minZ) / 2.0;
        final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
        RenderManager.renderBox(event.getMatrices(), scaled,
                damage > 0.95f ? 0x6000ff00 : 0x60ff0000);
        RenderManager.renderBoundingBox(event.getMatrices(), scaled,
                2.5f, damage > 0.95f ? 0x6000ff00 : 0x60ff0000);
    }

    public enum SpeedmineMode {
        PACKET,
        DAMAGE
    }

    public enum Swap {
        NORMAL,
        SILENT,
        OFF
    }
}
