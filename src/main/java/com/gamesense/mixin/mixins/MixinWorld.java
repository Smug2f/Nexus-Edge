/*
    CheckLightFor from lambda (https://github.com/lambda-client/lambda/blob/master/src/main/java/com/lambda/client/mixin/client/world/MixinWorld.java)
 */
package com.gamesense.mixin.mixins;

import com.gamesense.api.event.events.BlockUpdateEvent;
import com.gamesense.client.GameSense;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.render.NoRender;
import com.gamesense.client.module.modules.render.noGlitchBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "checkLightFor", at = @At("HEAD"), cancellable = true)
    private void updateLightmapHook(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        NoRender noRender = ModuleManager.getModule(NoRender.class);

        if (noRender.isEnabled() && noRender.noSkylight.getValue()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", at = @At("HEAD"), cancellable = true)
    private void setBlockState(BlockPos pos, IBlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
        noGlitchBlock noGlitchBlock = ModuleManager.getModule(noGlitchBlock.class);
        if ( noGlitchBlock.isEnabled() && noGlitchBlock.placeBlock.getValue() && flags != 3) {
            cir.cancel();
            cir.setReturnValue(false);
        }

    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    private void getThunderStrengthHead(float delta, CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.getModule(NoRender.class).noWeather.getValue() && !ModuleManager.getModule(NoRender.class).weather.getValue().equals("Thunder")) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    private void getRainStrengthHead(float delta, CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.getModule(NoRender.class).noWeather.getValue() && ModuleManager.getModule(NoRender.class).weather.getValue().equals("Clear")) {
            cir.setReturnValue(0.0f);
        }
    }
}