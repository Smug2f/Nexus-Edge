package com.gamesense.mixin.mixins;

import com.gamesense.api.util.player.social.SocialManager;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.gui.ColorMain;
import com.gamesense.client.module.modules.misc.ExtraTab;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    ExtraTab extraTab = ModuleManager.getModule(ExtraTab.class);

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Ljava/util/List;subList(II)Ljava/util/List;", remap = false))
    public List<NetworkPlayerInfo> subListHook(final List<NetworkPlayerInfo> list, final int fromIndex, final int toIndex) {
        return list.subList(fromIndex, extraTab.isEnabled() ? Math.min(extraTab.players.getValue(), list.size()) : toIndex);
    }

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerNameHead(NetworkPlayerInfo networkPlayerInfoIn, CallbackInfoReturnable<String> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(getPlayerNameGS(networkPlayerInfoIn));
    }

    private String getPlayerNameGS(NetworkPlayerInfo networkPlayerInfoIn) {
        String displayName = networkPlayerInfoIn.getDisplayName() != null ?
            networkPlayerInfoIn.getDisplayName().getFormattedText() :
            ScorePlayerTeam.formatPlayerName(networkPlayerInfoIn.getPlayerTeam(), networkPlayerInfoIn.getGameProfile().getName());

        if (SocialManager.isFriend(displayName)) {
            return ModuleManager.getModule(ColorMain.class).getFriendColor() + displayName;
        } else if (SocialManager.isEnemy(displayName)) {
            return ModuleManager.getModule(ColorMain.class).getEnemyColor() + displayName;
        } else {
            return displayName;
        }
    }
}