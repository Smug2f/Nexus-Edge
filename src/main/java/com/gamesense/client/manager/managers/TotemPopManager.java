package com.gamesense.client.manager.managers;

import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.event.events.TotemPopEvent;
import com.gamesense.api.util.misc.MessageBus;
import com.gamesense.client.GameSense;
import com.gamesense.client.manager.Manager;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.misc.PvPInfo;
import com.mojang.realmsclient.gui.ChatFormatting;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;

/**
 * @author Darki for popcounter
 * Originally made for PvPInfo
 * @author Hoosiers
 * @src https://github.com/DarkiBoi/CliNet/blob/master/src/main/java/me/zeroeightsix/kami/module/modules/combat/TotemPopCounter.java
 **/

public enum TotemPopManager implements Manager {

    INSTANCE;

    public boolean sendMsgs = false;
    public boolean sendCountPops = false;
    public boolean sendCountKills = false;
    public boolean popCount = false;
    public ChatFormatting chatFormatting = ChatFormatting.WHITE;
    private final HashMap<String, Integer> playerPopCount = new HashMap<>();
    private int countPops = 0;
    private int countKills = 0;
    PvPInfo pvp = ModuleManager.getModule(PvPInfo.class);

    public int getPops() {
        return countPops;
    }

    public int getKills() {
        return countKills;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<TickEvent.ClientTickEvent> clientTickEventListener = new Listener<>(event -> {
        if (event.phase != TickEvent.Phase.START) return;

        if (getPlayer() == null || getWorld() == null) {
            playerPopCount.clear();
            return;
        }

        for (EntityPlayer entityPlayer : getWorld().playerEntities) {
            if (entityPlayer.getHealth() <= 0 && playerPopCount.containsKey(entityPlayer.getName()) && entityPlayer != Minecraft.getMinecraft().player) {
                if (sendMsgs && pvp.isEnabled()) {
                    MessageBus.sendClientPrefixMessage(chatFormatting + entityPlayer.getName() + " died after popping " + ChatFormatting.GREEN + getPlayerPopCount(entityPlayer.getName()) + chatFormatting + " totems!");
                }
                ++countKills;
                if (sendCountKills && pvp.isEnabled())
                    MessageBus.sendClientPrefixMessage(chatFormatting + "You have seen " + ChatFormatting.GREEN + countKills + chatFormatting + " people killed!");
                playerPopCount.remove(entityPlayer.getName());
            }
        }
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PacketEvent.Receive> packetEventListener = new Listener<>(event -> {
        if (getPlayer() == null || getWorld() == null) return;

        if (event.getPacket() instanceof SPacketEntityStatus) {
            SPacketEntityStatus packet = (SPacketEntityStatus) event.getPacket();
            Entity entity = packet.getEntity(getWorld());

            if (packet.getOpCode() == 35) {
                GameSense.EVENT_BUS.post(new TotemPopEvent(entity));
            }
        }
    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<TotemPopEvent> totemPopEventListener = new Listener<>(event -> {
        if (getPlayer() == null || getWorld() == null) return;

        if (event.getEntity() == null) return;

        String entityName = event.getEntity().getName();

        if (getMinecraft().player.gameProfile.getName().equals(entityName))
            return;
        ++countPops;

        if ( sendMsgs && popCount && pvp.isEnabled())
            MessageBus.sendClientPrefixMessage(chatFormatting + "You have seen " + ChatFormatting.GREEN + countPops + chatFormatting + " people popped!");

        if (playerPopCount.get(entityName) == null) {
            playerPopCount.put(entityName, 1);
            if (sendMsgs && sendCountPops && pvp.isEnabled())
                MessageBus.sendClientPrefixMessage(chatFormatting + entityName + " popped " + ChatFormatting.RED + 1 + chatFormatting + " totem!");
        } else {
            int popCounter = playerPopCount.get(entityName) + 1;

            playerPopCount.put(entityName, popCounter);
            if (sendMsgs && sendCountPops && pvp.isEnabled())
                MessageBus.sendClientPrefixMessage(chatFormatting + entityName + " popped " + ChatFormatting.RED + popCounter + chatFormatting + " totems!");
        }
    });

    public int getPlayerPopCount(String name) {
        if (playerPopCount.containsKey(name)) {
            return playerPopCount.get(name);
        }

        return 0;
    }
}