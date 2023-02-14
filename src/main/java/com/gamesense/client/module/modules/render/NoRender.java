package com.gamesense.client.module.modules.render;

import com.gamesense.api.event.events.BossbarEvent;
import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.event.events.TotemPopEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.IntegerSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.client.GameSense;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Arrays;

@Module.Declaration(name = "NoRender", category = Category.Render)
public class NoRender extends Module {

    public BooleanSetting armor = registerBoolean("Armor", false);
    BooleanSetting fire = registerBoolean("Fire", false);
    BooleanSetting blind = registerBoolean("Blind", false);
    BooleanSetting nausea = registerBoolean("Nausea", false);
    public BooleanSetting hurtCam = registerBoolean("HurtCam", false);
    public BooleanSetting noSkylight = registerBoolean("Skylight", false);
    public BooleanSetting noOverlay = registerBoolean("No Overlay", false);
    BooleanSetting noBossBar = registerBoolean("No Boss Bar", false);
    public BooleanSetting noWeather = registerBoolean("No Weather", false);
    public ModeSetting weather = registerMode("Allowed Weather", Arrays.asList("Clear", "Rain", "Thunder"), "Clear");
    public BooleanSetting noCluster = registerBoolean("No Cluster", false);
    IntegerSetting maxNoClusterRender = registerInteger("No Cluster Max", 5, 1, 25);
    BooleanSetting noTotem = registerBoolean("No Totem", false);

    @EventHandler
    private final Listener<PacketEvent.Receive> packetEventListener = new Listener<>(event -> {
        if (mc.player == null || mc.world == null || !noTotem.getValue()) return;

        if (event.getPacket() instanceof SPacketEntityStatus) {
            SPacketEntityStatus packet = (SPacketEntityStatus) event.getPacket();
            if (packet.getOpCode() == 35) {
                event.cancel();
            }
        }
    });

    public int currentClusterAmount = 0;

    public void onUpdate() {
        if (blind.getValue() && mc.player.isPotionActive(MobEffects.BLINDNESS))
            mc.player.removePotionEffect(MobEffects.BLINDNESS);
        if (nausea.getValue() && mc.player.isPotionActive(MobEffects.NAUSEA))
            mc.player.removePotionEffect(MobEffects.NAUSEA);
    }

    public void onRender() {
        currentClusterAmount = 0;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public Listener<RenderBlockOverlayEvent> blockOverlayEventListener = new Listener<>(event -> {
        if (fire.getValue() && event.getOverlayType() == RenderBlockOverlayEvent.OverlayType.FIRE)
            event.setCanceled(true);
        if (noOverlay.getValue() && event.getOverlayType() == RenderBlockOverlayEvent.OverlayType.WATER)
            event.setCanceled(true);
        if (noOverlay.getValue() && event.getOverlayType() == RenderBlockOverlayEvent.OverlayType.BLOCK)
            event.setCanceled(true);
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<EntityViewRenderEvent.FogDensity> fogDensityListener = new Listener<>(event -> {
        if (noOverlay.getValue()) {
            if (event.getState().getMaterial().equals(Material.WATER)
                || event.getState().getMaterial().equals(Material.LAVA)) {
                event.setDensity(0);
                event.setCanceled(true);
            }
        }
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<RenderBlockOverlayEvent> renderBlockOverlayEventListener = new Listener<>(event -> {
        if (noOverlay.getValue()) event.setCanceled(true);
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<RenderGameOverlayEvent> renderGameOverlayEventListener = new Listener<>(event -> {
        if (noOverlay.getValue()) {
            if (event.getType().equals(RenderGameOverlayEvent.ElementType.HELMET)) {
                event.setCanceled(true);
            }
            if (event.getType().equals(RenderGameOverlayEvent.ElementType.PORTAL)) {
                event.setCanceled(true);
            }
        }
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<BossbarEvent> bossbarEventListener = new Listener<>(event -> {
        if (noBossBar.getValue()) {
            event.cancel();
        }
    });

    public boolean incrementNoClusterRender() {
        ++currentClusterAmount;
        return currentClusterAmount <= maxNoClusterRender.getValue();
    }

    public boolean getNoClusterRender() {
        return currentClusterAmount <= maxNoClusterRender.getValue();
    }
}