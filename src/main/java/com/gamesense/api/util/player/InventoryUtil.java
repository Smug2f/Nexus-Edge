package com.gamesense.api.util.player;

import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.combat.OffHand;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void swap(int InvSlot, int newSlot) {

        mc.playerController.windowClick(0, InvSlot, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(0, newSlot, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(0, InvSlot, 0, ClickType.PICKUP, mc.player);

        mc.playerController.updateController();

    }

    public static int findObsidianSlot(boolean offHandActived, boolean activeBefore) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        if (offHandActived && ModuleManager.isModuleEnabled(OffHand.class)) {
            if (!activeBefore) {
                OffHand.requestItems(0);
            }
            return 9;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }

            Block block = ((ItemBlock) stack.getItem()).getBlock();
            if (block instanceof BlockObsidian) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    public static int findCrystalBlockSlot(boolean offHandActived, boolean activeBefore) {

        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        if (offHandActived && ModuleManager.isModuleEnabled(OffHand.class)) {
            if (!activeBefore) {
                OffHand.requestItems(0);
            }
            return 9;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }

            Block block = ((ItemBlock) stack.getItem()).getBlock();
            if (block.getBlockState().getBlock().blockHardness > 6) {
                slot = i;
                break;
            }
        }
        return slot;

    }

    public static int findSkullSlot(boolean offHandActived, boolean activeBefore) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        if (offHandActived) {
            if (!activeBefore)
                OffHand.requestItems(1);
            return 9;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack != ItemStack.EMPTY && stack.getItem() instanceof ItemSkull)
                return i;
        }
        return slot;
    }

    public static int findTotemSlot(int lower, int upper) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;
        for (int i = lower; i <= upper; i++) {
            ItemStack stack = mainInventory.get(i);
            if (stack == ItemStack.EMPTY || stack.getItem() != Items.TOTEM_OF_UNDYING)
                continue;

            slot = i;
            break;
        }
        return slot;
    }

    public static int findFirstShulker(int lower, int upper) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;
        for (int i = lower; i <= upper; i++) {
            ItemStack stack = mainInventory.get(i);
            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemBlock))
                continue;

            if (((ItemBlock) stack.getItem()).getBlock() instanceof BlockShulkerBox) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    public static int findFirstItemSlot(Class<? extends Item> itemToFind, int lower, int upper) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        for (int i = lower; i <= upper; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(itemToFind.isInstance(stack.getItem()))) {
                continue;
            }

            if (itemToFind.isInstance(stack.getItem())) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    public static int findFirstBlockSlot(Class<? extends Block> blockToFind, int lower, int upper) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        for (int i = lower; i <= upper; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }

            if (blockToFind.isInstance(((ItemBlock) stack.getItem()).getBlock())) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    public static int findAnyBlockSlot(int lower, int upper) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        for (int i = lower; i <= upper; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }
            slot = i;
        }
        return slot;
    }

    public static List<Integer> findAllItemSlots(Class<? extends Item> itemToFind) {
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(itemToFind.isInstance(stack.getItem()))) {
                continue;
            }

            slots.add(i);
        }
        return slots;
    }

    public static List<Integer> findAllBlockSlots(Class<? extends Block> blockToFind) {
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mainInventory.get(i);

            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }

            if (blockToFind.isInstance(((ItemBlock) stack.getItem()).getBlock())) {
                slots.add(i);
            }
        }
        return slots;
    }

    public static int findToolForBlockState(IBlockState iBlockState, int lower, int upper) {
        int slot = -1;
        List<ItemStack> mainInventory = mc.player.inventory.mainInventory;

        double foundMaxSpeed = 0;
        for (int i = lower; i <= upper; i++) {
            ItemStack itemStack = mainInventory.get(i);

            if (itemStack == ItemStack.EMPTY) continue;

            float breakSpeed = itemStack.getDestroySpeed(iBlockState);

            int efficiencySpeed = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, itemStack);

            if (breakSpeed > 1.0F) {
                breakSpeed += efficiencySpeed > 0 ? Math.pow(efficiencySpeed, 2) + 1 : 0;

                if (breakSpeed > foundMaxSpeed) {
                    foundMaxSpeed = breakSpeed;
                    slot = i;
                }
            }
        }

        return slot;
    }
}