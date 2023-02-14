package com.gamesense.client.module.modules.misc;

import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockObsidian;
import net.minecraft.item.*;

/**
 * @author 0b00101010
 * @since 25/01/21
 */

@Module.Declaration(name = "NoEntityTrace", category = Category.Misc)
public class NoEntityTrace extends Module {

    BooleanSetting pickaxe = registerBoolean("Pickaxe", true);
    BooleanSetting gap = registerBoolean("Food Stuffs", false); // for miningtrace
    BooleanSetting obsidian = registerBoolean("Obsidian", false);
    BooleanSetting eChest = registerBoolean("EnderChest", false);
    BooleanSetting block = registerBoolean("Blocks", false);
    BooleanSetting all = registerBoolean("All", false);

    boolean isHoldingPickaxe = false;
    boolean isHoldingObsidian = false;
    boolean isHoldingEChest = false;
    boolean isHoldingBlock = false;
    boolean isHoldingGap = false;

    public void onUpdate() {
        Item item = mc.player.getHeldItemMainhand().getItem();
        isHoldingPickaxe = item instanceof ItemPickaxe;
        isHoldingBlock = item instanceof ItemBlock;
        isHoldingGap = item instanceof ItemFood || item instanceof ItemPotion;

        if (isHoldingBlock) {
            isHoldingObsidian = ((ItemBlock) item).getBlock() instanceof BlockObsidian;
            isHoldingEChest = ((ItemBlock) item).getBlock() instanceof BlockEnderChest;
        } else {
            isHoldingObsidian = false;
            isHoldingEChest = false;
        }
    }

    public boolean noTrace() {
        if (pickaxe.getValue() && isHoldingPickaxe) return isEnabled();
        if (obsidian.getValue() && isHoldingObsidian) return isEnabled();
        if (eChest.getValue() && isHoldingEChest) return isEnabled();
        if (block.getValue() && isHoldingBlock) return isEnabled();
        if (gap.getValue() && isHoldingGap) return isEnabled();
        if (all.getValue()) return isEnabled();
        return false;
    }
}