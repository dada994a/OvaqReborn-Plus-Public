package net.shoreline.client.api.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.impl.module.combat.SurroundModule;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author linus
 * @see SurroundModule
 * @since 1.0
 */
public class ObsidianPlacerModule extends BlockPlacerModule
{
    private static final List<Block> RESISTANT_BLOCKS = new LinkedList<>() {{
       add(Blocks.OBSIDIAN);
       add(Blocks.CRYING_OBSIDIAN);
       add(Blocks.ENDER_CHEST);
    }};

    protected Config<Boolean> strictDirectionConfig = new BooleanConfig("StrictDirection", "Places on visible sides only", false);
    protected Config<Boolean> grimConfig = new BooleanConfig("Grim", "Places using grim instant rotations", false);

    public ObsidianPlacerModule(String name, String desc, ModuleCategory category) {
        super(name, desc, category);
        register(strictDirectionConfig, grimConfig);
    }

    public ObsidianPlacerModule(String name, String desc, ModuleCategory category, int rotationPriority) {
        super(name, desc, category, rotationPriority);
        register(strictDirectionConfig, grimConfig);
    }

    protected int getResistantBlockItem() {
        for (final Block type : RESISTANT_BLOCKS) {
            final int slot = getBlockItemSlot(type);
            if (slot != -1) {
                return slot;
            }
        }
        return -1;
    }
}
