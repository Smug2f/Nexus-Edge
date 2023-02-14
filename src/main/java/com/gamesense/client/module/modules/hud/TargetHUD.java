package com.gamesense.client.module.modules.hud;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.Objects;

import com.gamesense.api.setting.values.ColorSetting;
import com.gamesense.api.setting.values.IntegerSetting;
import com.gamesense.api.util.player.social.SocialManager;
import com.gamesense.api.util.render.GSColor;
import com.gamesense.api.util.world.EntityUtil;
import com.gamesense.client.clickgui.GameSenseGUI;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.HUDModule;
import com.gamesense.client.module.Module;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.gui.ColorMain;
import com.lukflug.panelstudio.base.Context;
import com.lukflug.panelstudio.base.IInterface;
import com.lukflug.panelstudio.hud.HUDComponent;
import com.lukflug.panelstudio.setting.Labeled;
import com.lukflug.panelstudio.theme.ITheme;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;

/**
 * @Author Hoosiers on 10/19/2020,
 * update by linustouchtips on 10/27/2020,
 * rewritten by lukflug on 21.11.2020
 */

@Module.Declaration(name = "TargetHUD", category = Category.HUD)
@HUDModule.Declaration(posX = 0, posZ = 70)
public class TargetHUD extends HUDModule {

    IntegerSetting range = registerInteger("Range", 100, 10, 260);
    ColorSetting outline = registerColor("Outline", new GSColor(255, 0, 0, 255));
    ColorSetting background = registerColor("Background", new GSColor(0, 0, 0, 255));

    private static EntityPlayer targetPlayer;

    @Override
    public void populate(ITheme theme) {
        component = new TargetHUDComponent(theme);
    }

    private static Color getNameColor(String playerName) {
        if (SocialManager.isFriend(playerName)) {
            return new GSColor(ModuleManager.getModule(ColorMain.class).getFriendGSColor(), 255);
        } else if (SocialManager.isEnemy(playerName)) {
            return new GSColor(ModuleManager.getModule(ColorMain.class).getEnemyGSColor(), 255);
        } else {
            return new GSColor(255, 255, 255, 255);
        }
    }

    private static GSColor getHealthColor(int health) {
        if (health > 36) {
            health = 36;
        }
        if (health < 0) {
            health = 0;
        }

        int red = (int) (255 - (health * 7.0833));
        int green = 255 - red;

        return new GSColor(red, green, 0, 255);
    }

    private static boolean isValidEntity(Entity e) {
        if (!(e instanceof EntityPlayer) || e.getName().length() == 0) return false;
        else return e != mc.player;
    }

    private static float getPing(EntityPlayer player) {
        float ping = 0;
        try {
            ping = EntityUtil.clamp(Objects.requireNonNull(mc.getConnection()).getPlayerInfo(player.getUniqueID()).getResponseTime(), 1, 300.0f);
        } catch (NullPointerException ignored) {
        }
        return ping;
    }

    public static boolean isRenderingEntity(EntityPlayer entityPlayer) {
        return targetPlayer == entityPlayer;
    }

    private class TargetHUDComponent extends HUDComponent {

        public TargetHUDComponent(ITheme theme) {
        	super(new Labeled(getName(),null,()->true), TargetHUD.this.position, getName());
        }

        @Override
        public void render(Context context) {
            super.render(context);
            // Render content
            if (mc.world != null && mc.player.ticksExisted >= 10) {
                EntityPlayer entityPlayer = (EntityPlayer) mc.world.loadedEntityList.stream()
                    .filter(TargetHUD::isValidEntity)
                    .map(entity -> (EntityLivingBase) entity)
                    .min(Comparator.comparing(c -> mc.player.getDistance(c)))
                    .orElse(null);
                if (entityPlayer != null && entityPlayer.getDistance(mc.player) <= range.getValue()) {
                    // Render background
                    Color bgcolor = new GSColor(background.getValue(), 100);
                    context.getInterface().fillRect(context.getRect(), bgcolor, bgcolor, bgcolor, bgcolor);
                    // Render outline
                    Color color = outline.getValue();
                    context.getInterface().fillRect(new Rectangle(context.getPos(), new Dimension(context.getSize().width, 1)), color, color, color, color);
                    context.getInterface().fillRect(new Rectangle(context.getPos(), new Dimension(1, context.getSize().height)), color, color, color, color);
                    context.getInterface().fillRect(new Rectangle(new Point(context.getPos().x + context.getSize().width - 1, context.getPos().y), new Dimension(1, context.getSize().height)), color, color, color, color);
                    context.getInterface().fillRect(new Rectangle(new Point(context.getPos().x, context.getPos().y + context.getSize().height - 1), new Dimension(context.getSize().width, 1)), color, color, color, color);
                    // Render player
                    targetPlayer = entityPlayer;
                    GameSenseGUI.renderEntity(entityPlayer, new Point(context.getPos().x + 35, context.getPos().y + 87 - (entityPlayer.isSneaking() ? 10 : 0)), 43);
                    targetPlayer = null;
                    // Render name
                    String playerName = entityPlayer.getName();
                    Color nameColor = getNameColor(playerName);
                    context.getInterface().drawString(new Point(context.getPos().x + 71, context.getPos().y + 11), GameSenseGUI.FONT_HEIGHT, TextFormatting.BOLD + playerName, nameColor);
                    // Render health
                    int playerHealth = (int) (entityPlayer.getHealth() + entityPlayer.getAbsorptionAmount());
                    Color healthColor = getHealthColor(playerHealth);
                    context.getInterface().drawString(new Point(context.getPos().x + 71, context.getPos().y + 23), GameSenseGUI.FONT_HEIGHT, TextFormatting.WHITE + "Health: " + TextFormatting.RESET + playerHealth, healthColor);
                    // Render distance
                    context.getInterface().drawString(new Point(context.getPos().x + 71, context.getPos().y + 33), GameSenseGUI.FONT_HEIGHT, "Distance: " + ((int) entityPlayer.getDistance(mc.player)), new Color(255, 255, 255));
                    // Render ping and info
                    String info;
                    if (entityPlayer.inventory.armorItemInSlot(2).getItem().equals(Items.ELYTRA)) {
                        info = TextFormatting.LIGHT_PURPLE + "Wasp";
                    } else if (entityPlayer.inventory.armorItemInSlot(2).getItem().equals(Items.DIAMOND_CHESTPLATE)) {
                        info = TextFormatting.RED + "Threat";
                    } else if (entityPlayer.inventory.armorItemInSlot(3).getItem().equals(Items.AIR)) {
                        info = TextFormatting.GREEN + "NewFag";
                    } else {
                        info = TextFormatting.WHITE + "None";
                    }
                    context.getInterface().drawString(new Point(context.getPos().x + 71, context.getPos().y + 43), GameSenseGUI.FONT_HEIGHT, info + TextFormatting.WHITE + " | " + getPing(entityPlayer) + " ms", new Color(255, 255, 255));
                    // Render status effects
                    String status = null;
                    Color statusColor = null;
                    for (PotionEffect effect : entityPlayer.getActivePotionEffects()) {
                        if (effect.getPotion() == MobEffects.WEAKNESS) {
                            status = "Weakness!";
                            statusColor = new Color(135, 0, 25);
                        } else if (effect.getPotion() == MobEffects.INVISIBILITY) {
                            status = "Invisible!";
                            statusColor = new Color(90, 90, 90);
                        } else if (effect.getPotion() == MobEffects.STRENGTH) {
                            status = "Strength!";
                            statusColor = new Color(185, 65, 185);
                        }
                    }
                    if (status != null)
                        context.getInterface().drawString(new Point(context.getPos().x + 71, context.getPos().y + 55), GameSenseGUI.FONT_HEIGHT, TextFormatting.WHITE + "Status: " + TextFormatting.RESET + status, statusColor);
                    // Render items
                    int xPos = context.getPos().x + 150;
                    for (ItemStack itemStack : entityPlayer.getArmorInventoryList()) {
                        xPos -= 20;
                        GameSenseGUI.renderItem(itemStack, new Point(xPos, context.getPos().y + 73));
                    }
                }
            }
        }

        @Override
		public Dimension getSize(IInterface inter) {
			return new Dimension(162,94);
		}
    }
}