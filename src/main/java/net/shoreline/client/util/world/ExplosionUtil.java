package net.shoreline.client.util.world;

import com.google.common.collect.Multimap;
import java.util.function.BiFunction;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.shoreline.client.util.Globals;
//最高級Util
public class ExplosionUtil implements Globals {
  /*

    public static double getDamageTo(Entity entity, Vec3d explosion) {
        return ExplosionUtil.getDamageTo(entity, explosion, false);
    }

    public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain) {
        return ExplosionUtil.getDamageTo(entity, explosion, ignoreTerrain, 12.0f);
    }

    public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, float power) {
        double d = Math.sqrt(entity.squaredDistanceTo(explosion));
        double ab = ExplosionUtil.getExposure(explosion, entity, ignoreTerrain);
        double w = d / (double)power;
        double ac = (1.0 - w) * ab;
        double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
        dmg = ExplosionUtil.getReduction(entity, ExplosionUtil.mc.world.getDamageSources().explosion(null), dmg);
        return Math.max(0.0, dmg);
    }

    public static double getDamageToPos(Vec3d pos, Entity entity, Vec3d explosion, boolean ignoreTerrain) {
        Box bb = entity.getBoundingBox();
        double dx = pos.getX() - bb.minX;
        double dy = pos.getY() - bb.minY;
        double dz = pos.getZ() - bb.minZ;
        Box box = bb.offset(dx, dy, dz);
        double ab = ExplosionUtil.getExposure(explosion, box, ignoreTerrain);
        double w = Math.sqrt(pos.squaredDistanceTo(explosion)) / 12.0;
        double ac = (1.0 - w) * ab;
        double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
        dmg = ExplosionUtil.getReduction(entity, ExplosionUtil.mc.world.getDamageSources().explosion(null), dmg);
        return Math.max(0.0, dmg);
    }

    private static double getReduction(Entity entity, DamageSource damageSource, double damage) {
        if (damageSource.isScaledWithDifficulty()) {
            switch (ExplosionUtil.mc.world.getDifficulty()) {
                case EASY: {
                    damage = Math.min(damage / 2.0 + 1.0, damage);
                    break;
                }
                case HARD: {
                    damage *= 1.5;
                }
            }
        }
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            damage = DamageUtil.getDamageLeft((float)damage, ExplosionUtil.getArmor(livingEntity), (float)ExplosionUtil.getAttributeValue(livingEntity, EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
            damage = ExplosionUtil.getProtectionReduction(entity, damage, damageSource);
        }
        return Math.max(damage, 0.0);
    }

    private static float getArmor(LivingEntity entity) {
        return (float)Math.floor(ExplosionUtil.getAttributeValue(entity, EntityAttributes.GENERIC_ARMOR));
    }

    private static float getProtectionReduction(Entity player, double damage, DamageSource source) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), source);
        return DamageUtil.getInflictedDamage((float)damage, protLevel);
    }

    public static double getAttributeValue(LivingEntity entity, EntityAttribute attribute) {
        return ExplosionUtil.getAttributeInstance(entity, attribute).getValue();
    }

    public static EntityAttributeInstance getAttributeInstance(LivingEntity entity, EntityAttribute attribute) {
        double baseValue = ExplosionUtil.getDefaultForEntity(entity).getBaseValue(attribute);
        EntityAttributeInstance attributeInstance = new EntityAttributeInstance(attribute, o1 -> {});
        attributeInstance.setBaseValue(baseValue);
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack stack = entity.getEquippedStack(equipmentSlot);
            Multimap modifiers = stack.getAttributeModifiers(equipmentSlot);
            for (EntityAttributeModifier modifier : modifiers.get(attribute)) {// FUCKYOU
                attributeInstance.addTemporaryModifier(modifier);
            }
        }
        return attributeInstance;
    }

    private static <T extends LivingEntity> DefaultAttributeContainer getDefaultForEntity(T entity) {
        return DefaultAttributeRegistry.get(entity.getType());//FUCKYOU
    }

    private static float getExposure(Vec3d source, Entity entity, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();
        return ExplosionUtil.getExposure(source, box, ignoreTerrain);
    }

    private static float getExposure(Vec3d source, Box box, boolean ignoreTerrain) {
        RaycastFactory raycastFactory = ExplosionUtil.getRaycastFactory(ignoreTerrain);
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;
        double xStep = 1.0 / (xDiff * 2.0 + 1.0);
        double yStep = 1.0 / (yDiff * 2.0 + 1.0);
        double zStep = 1.0 / (zDiff * 2.0 + 1.0);
        if (xStep > 0.0 && yStep > 0.0 && zStep > 0.0) {
            int misses = 0;
            int hits = 0;
            double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
            double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
            xStep *= xDiff;
            yStep *= yDiff;
            zStep *= zDiff;
            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;
            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3d position = new Vec3d(x, y, z);
                        if (ExplosionUtil.raycast(new ExposureRaycastContext(position, source), raycastFactory) == null) {
                            ++misses;
                        }
                        ++hits;
                    }
                }
            }
            return (float)misses / (float)hits;
        }
        return 0.0f;
    }

    private static RaycastFactory getRaycastFactory(boolean ignoreTerrain) {
        if (ignoreTerrain) {
            return (context, blockPos) -> {
                BlockState blockState = ExplosionUtil.mc.world.getBlockState((BlockPos)blockPos);
                if (blockState.getBlock().getBlastResistance() < 600.0f) {
                    return null;
                }
                return blockState.getCollisionShape(ExplosionUtil.mc.world, (BlockPos)blockPos).raycast(context.start(), context.end(), (BlockPos)blockPos);
            };
        }
        return (context, blockPos) -> {
            BlockState blockState = ExplosionUtil.mc.world.getBlockState((BlockPos)blockPos);
            return blockState.getCollisionShape(ExplosionUtil.mc.world, (BlockPos)blockPos).raycast(context.start(), context.end(), (BlockPos)blockPos);
        };
    }

    private static BlockHitResult raycast(ExposureRaycastContext context, RaycastFactory raycastFactory) {
        return (BlockHitResult)BlockView.raycast(context.start, context.end, context, raycastFactory, ctx -> null);
    }

    @FunctionalInterface
    public static interface RaycastFactory
            extends BiFunction<ExposureRaycastContext, BlockPos, BlockHitResult> {
    }

    public record ExposureRaycastContext(Vec3d start, Vec3d end) {
    }*/
}

