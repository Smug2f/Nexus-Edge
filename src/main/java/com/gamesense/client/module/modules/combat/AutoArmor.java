package com.gamesense.client.module.modules.combat;

import com.gamesense.api.event.events.PlayerMoveEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.api.util.player.InventoryUtil;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.movement.ElytraFly;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Module.Declaration(name = "AutoArmor", category = Category.Combat)
public class AutoArmor extends Module {

    ModeSetting allowElytra = registerMode("Elytra", Arrays.asList("Allow", "Only on ElytraFly", "Disallow"), "Only on ElytraFly");
    BooleanSetting noThorns = registerBoolean("No Thorns", false);
    BooleanSetting lastResortThorns = registerBoolean("No Other Thorns", false);
    BooleanSetting oneAtTime = registerBoolean("One At Time", false);
    BooleanSetting strict = registerBoolean("Strict", false);

    public boolean dontMove;

    @EventHandler
    private final Listener<PlayerMoveEvent> playerMoveEventListener = new Listener<>(event -> {

        if (dontMove) {

            event.setX(0);
            event.setZ(0);

            dontMove = false;

        }

    });

    public void onUpdate() {
        if (mc.player.ticksExisted % 2 == 0) return;
        // check screen
        if (mc.currentScreen instanceof GuiContainer
                && !(mc.currentScreen instanceof InventoryEffectRenderer))
            return;

        List<ItemStack> armorInventory = mc.player.inventory.armorInventory;
        List<ItemStack> inventory = mc.player.inventory.mainInventory;

        // store slots and values of best armor pieces
        int[] bestArmorSlots = {-1, -1, -1, -1};
        int[] bestArmorValues = {-1, -1, -1, -1};

        // initialize with currently equipped armour
        for (int i = 0; i < 4; i++) {
            ItemStack oldArmour = armorInventory.get(i);
            if (oldArmour.getItem() instanceof ItemArmor) {
                bestArmorValues[i] = ((ItemArmor) oldArmour.getItem()).damageReduceAmount;
            }

            List<Integer> slots = InventoryUtil.findAllItemSlots(ItemArmor.class);
            HashMap<Integer, ItemStack> armour = new HashMap<>();
            HashMap<Integer, ItemStack> thorns = new HashMap<>();

            for (Integer slot : slots) {
                ItemStack item = inventory.get(slot);
                // 7 is the id for thorns
                if (noThorns.getValue() && EnchantmentHelper.getEnchantments(item).containsKey(Enchantment.getEnchantmentByID(7))) {
                    thorns.put(slot, item);
                } else {
                    armour.put(slot, item);
                }
            }

            armour.forEach(((integer, itemStack) -> {
                ItemArmor itemArmor = (ItemArmor) itemStack.getItem();
                int armorType = itemArmor.armorType.ordinal() - 2;

                if (armorType == 2 && mc.player.inventory.armorItemInSlot(armorType).getItem().equals(Items.ELYTRA)) {
                    return;
                }

                int armorValue = itemArmor.damageReduceAmount;

                if (armorValue > bestArmorValues[armorType]) {
                    bestArmorSlots[armorType] = integer;
                    bestArmorValues[armorType] = armorValue;
                }
            }));

            if (noThorns.getValue() && lastResortThorns.getValue()) {
                thorns.forEach(((integer, itemStack) -> {
                    ItemArmor itemArmor = (ItemArmor) itemStack.getItem();
                    int armorType = itemArmor.armorType.ordinal() - 2;

                    // Thorns is only put in when all other is lost
                    if (!(armorInventory.get(armorType) == ItemStack.EMPTY && bestArmorSlots[armorType] == -1)) {
                        return;
                    }

                    if (armorType == 2 && mc.player.inventory.armorItemInSlot(armorType).getItem().equals(Items.ELYTRA) && (allowElytra.getValue().equalsIgnoreCase("Allow") || allowElytra.getValue().equalsIgnoreCase("Only on ElytraFly") && ModuleManager.isModuleEnabled(ElytraFly.class))) {
                        return;
                    }

                    int armorValue = itemArmor.damageReduceAmount;

                    if (armorValue > bestArmorValues[armorType]) {
                        bestArmorSlots[armorType] = integer;
                        bestArmorValues[armorType] = armorValue;
                    }
                }));
            }

            // equip better armor
            for (int j = 0; j < 4; j++) {
                // check if better armor was found
                int slot = bestArmorSlots[j];
                if (slot == -1) {
                    continue;
                }
                // hotbar fix
                if (slot < 9) {
                    slot += 36;
                }

                if (strict.getValue()) {
                    try {
                        dontMove = true;
                        mc.playerController.onStoppedUsingItem(mc.player);
                    } catch (Exception ignored) {
                        dontMove = true;
                    }
                }

                // pick up inventory slot
                mc.playerController.windowClick(0, slot, 0, ClickType.PICKUP, mc.player);
                // click on armour slot
                mc.playerController.windowClick(0, 8 - j, 0, ClickType.PICKUP, mc.player);
                // put back inventory slot
                mc.playerController.windowClick(0, slot, 0, ClickType.PICKUP, mc.player);

                if (oneAtTime.getValue())
                    break;
            }
        }
    }
}