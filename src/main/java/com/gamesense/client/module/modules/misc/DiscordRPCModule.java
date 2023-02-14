package com.gamesense.client.module.modules.misc;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.IntegerSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.api.setting.values.StringSetting;
import com.gamesense.api.util.player.PlayerUtil;
import com.gamesense.client.manager.managers.TotemPopManager;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import com.gamesense.client.GameSense;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

@Module.Declaration(name = "DiscordRPC", category = Category.Misc, drawn = false, enabled = false)
public class DiscordRPCModule extends Module {


    ArrayList<String> options = new ArrayList<String>() {
        {
            add("None");
            add("Dimension");
            add("Health");
            add("Kill Counter");
            add("Name");
            add("Pop Counter");
            add("Server");
            add("Speed");
            add("Status");
            add("Version");
            add("Ping");
            add("Fps");
        }
    };

    String getVersion() {
        return GameSense.MODID + " " + GameSense.MODVER;
    }

    String getDimension() {
        return mc.world == null
                ? "In the main menu"
            : (mc.world.provider.getDimension() == 0
                ? "Overworld"
            : mc.world.provider.getDimension() == 1 ?
                "End" : "Nether");
    }

    String getFps() {
        return String.valueOf(mc.fpsCounter);
    }

    String getHealth() {
        return (int) PlayerUtil.getHealth() + "hp";
    }

    String getStatus() {
        return mc.player.inventory.armorItemInSlot(2).getItem().equals(Items.DIAMOND_CHESTPLATE) ? "Fighting" : "Chilling around";
    }

    String getKill() {
        return TotemPopManager.INSTANCE.getKills() + "kills";
    }

    String getPops() {
        return TotemPopManager.INSTANCE.getPops() + "pops";
    }

    String getServer() {
        return mc.getCurrentServerData() == null ? "Singleplayer" : mc.getCurrentServerData().serverIP;
    }

    String getPing() {
        String p;
        if (mc.player == null || mc.getConnection() == null || mc.getConnection().getPlayerInfo(mc.player.getName()) == null) {
            p = "-1";
        } else {
            p = String.valueOf(Objects.requireNonNull(mc.getConnection().getPlayerInfo(mc.player.getName())).getResponseTime());
        }
        p = p + "ping";
        return p;
    }

    String getMcName() {
        return mc.player.getName();
    }

    static final String discordID = "840996509880680479";
    static final DiscordRichPresence discordRichPresence = new DiscordRichPresence();
    static final DiscordRPC discordRPC = DiscordRPC.INSTANCE;

    int curImg = -1;
    ModeSetting imgType = registerMode("Image", Arrays.asList("gs++", "insigna", "luk", "aether"), "gs++");
    ModeSetting lowImg = registerMode("Low Img", Arrays.asList("none", "nocatsnolife", "sable__", "phantom826", "EightTwoSix", "doogie13", "soulbond", "anonymousplayer", "hoosier", "toxicaven", "0b00101010"), "none");
    BooleanSetting animateGs = registerBoolean("Animated gs++", true);
    IntegerSetting msChange = registerInteger("Image Change", 2000, 250, 5000);
    ModeSetting timeDisplay = registerMode("Display Time", Arrays.asList("Linear", "Reverse", "None"), "Linear");
    static final String MPS = "m/s";
    static final String KMH = "km/h";
    static final String MPH = "mph";
    ModeSetting speedUnit = registerMode("Unit", Arrays.asList(MPS, KMH, MPH), KMH);
    BooleanSetting firstLine = registerBoolean("First Line", true);
    StringSetting formatFirst = registerString("1Format", "%1 %2 %3");
    ModeSetting Line1Option1 = registerMode("Opt 1: ", Arrays.asList(options.toArray(new String[0])), "Version");
    ModeSetting Line1Option2 = registerMode("Opt 2: ", Arrays.asList(options.toArray(new String[0])), "Server");
    ModeSetting Line1Option3 = registerMode("Opt 3: ", Arrays.asList(options.toArray(new String[0])), "None");
    BooleanSetting secondLine = registerBoolean("Second Line", true);
    StringSetting formatSecond = registerString("2Format", "%1 %2 %3");
    ModeSetting Line2Option1 = registerMode("Opt 1; ", Arrays.asList(options.toArray(new String[0])), "Status");
    ModeSetting Line2Option2 = registerMode("Opt 2; ", Arrays.asList(options.toArray(new String[0])), "Health");
    ModeSetting Line2Option3 = registerMode("Opt 3; ", Arrays.asList(options.toArray(new String[0])), "Speed");

    String getSpeed() {
        return (int) calcSpeed(mc.player, speedUnit.getValue()) + speedUnit.getValue();
    }

    private double calcSpeed(EntityPlayerSP player, String unit) {
        double tps = 1000.0 / mc.timer.tickLength;
        double xDiff = player.posX - player.prevPosX;
        double zDiff = player.posZ - player.prevPosZ;

        double speed = Math.hypot(xDiff, zDiff) * tps;

        // Fast memory address comparison
        switch (unit) {
            case KMH:
                speed *= 3.6;
                break;
            case MPH:
                speed *= 2.237;
                break;
            default:
                break;
        }

        return speed;
    }


    public void onEnable() {
        //Discord.startRPC();
        DiscordEventHandlers eventHandlers = new DiscordEventHandlers();
        eventHandlers.disconnected = ((var1, var2) -> System.out.println("Discord RPC disconnected, var1: " + var1 + ", var2: " + var2));
        discordRPC.Discord_Initialize(discordID, eventHandlers, true, null);
        prevTimeImg = System.currentTimeMillis();

        // Set how we are going to display the time
        switch (timeDisplay.getValue()) {
            case "Linear":
                discordRichPresence.startTimestamp = System.currentTimeMillis() / 1000L;
                break;
            case "Reverse":
                discordRichPresence.endTimestamp = discordRichPresence.startTimestamp + 100;
                break;
            case "None":
                break;
        }
        //

        // Update rpc
        updateRpc();
    }

    public void onUpdate() {

        updateRpc();

    }

    public void onDisable() {
        //Discord.stopRPC();
        discordRPC.Discord_Shutdown();
        discordRPC.Discord_ClearPresence();
    }

    void updateRpc() {

        updatePicture();

        updateStatus();

        discordRPC.Discord_UpdatePresence(discordRichPresence);
    }

    void updateStatus() {

        // First line
        if (firstLine.getValue()) {
            discordRichPresence.details =
                    formatFirst.getText()
                    .replace("%1", getValues(Line1Option1.getValue()))
                    .replace("%2", getValues(Line1Option2.getValue()))
                    .replace("%3", getValues(Line1Option3.getValue()));
        }

        // Second line
        if (secondLine.getValue()) {
            discordRichPresence.state =
                    formatSecond.getText()
                            .replace("%1", getValues(Line2Option1.getValue()))
                            .replace("%2", getValues(Line2Option2.getValue()))
                            .replace("%3", getValues(Line2Option3.getValue()));
        }
    }

    String getValues(String values) {
        switch (values) {
            case "Dimension":
                return getDimension();
            case "Health":
                return getHealth();
            case "Kill Counter":
                return getKill();
            case "Pop Counter":
                return getPops();
            case "Name":
                return getMcName();
            case "Server":
                return getServer();
            case "Speed":
                return getSpeed();
            case "Status":
                return getStatus();
            case "Version":
                return getVersion();
            case "Ping":
                return getPing();
            case "Fps":
                return getFps();
        }
        return "";
    }

    void updatePicture() {
        /// Large IMG
        // Output
        String imgNow, description;
        // If gs++
        if ("gs++".equals(imgType.getValue())) {
            // If animated
            if (animateGs.getValue()) {
                // If we have to change
                if (prevTimeImg + msChange.getValue() < System.currentTimeMillis()) {
                    // Get where we are
                    int maxImg = 4;
                    curImg = curImg >= maxImg ? 0 : curImg + 1;
                    // Update time
                    prevTimeImg = System.currentTimeMillis();
                }
            // Set 0 in case of normal gs++
            } else curImg = 0;
            // Set imgNow
            imgNow = "gs" + curImg;
            description = "gs++ engine";
        } else {
            // Default
            imgNow = imgType.getValue();
            description = imgNow + " powered by gs++";
        }

        // Picture + text
        discordRichPresence.largeImageKey = imgNow;
        discordRichPresence.largeImageText = description;

        /// Small IMG
        if (!lowImg.getValue().equals("none")) {
            // Get img
            discordRichPresence.smallImageKey = lowImg.getValue();
            // Get text
            discordRichPresence.smallImageText = lowImg.getValue().equalsIgnoreCase(getMcName()) ? "Confirmed user" : "Not identified user";
        }
        else discordRichPresence.smallImageKey = null;

    }

    private long prevTimeImg;

}
