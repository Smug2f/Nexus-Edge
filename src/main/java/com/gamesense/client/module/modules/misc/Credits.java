package com.gamesense.client.module.modules.misc;

import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.client.GameSense;
import com.gamesense.client.command.CommandManager;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import com.gamesense.client.module.modules.combat.PistonCrystal;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.network.play.client.CPacketChatMessage;

import java.util.Arrays;

@Module.Declaration(name = "Credits", category = Category.Misc, enabled = false)
public class Credits extends Module {


    @Override
    public void onUpdate() {
        if (mc.world == null && mc.player == null)
            return;

        PistonCrystal.printDebug(" latest Nexus check.\n" +
                "\n" +
                "    Skill issue\n" +
                "    Piston Push (Alice)\n", false);


        disable();
    }
}
