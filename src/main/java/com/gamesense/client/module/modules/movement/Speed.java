package com.gamesense.client.module.modules.movement;

import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.event.events.PlayerMoveEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.DoubleSetting;
import com.gamesense.api.setting.values.IntegerSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.api.util.misc.Timer;
import com.gamesense.api.util.world.EntityUtil;
import com.gamesense.api.util.world.MotionUtil;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import com.gamesense.client.module.ModuleManager;
import com.mojang.realmsclient.gui.ChatFormatting;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketExplosion;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Hoosiers
 * @author Doogie13
 */

@Module.Declaration(name = "Speed", category = Category.Movement)
public class Speed extends Module {

    private final Timer timer = new Timer();
    public int yl;
    ModeSetting mode = registerMode("Mode", Arrays.asList("Strafe", "GroundStrafe", "OnGround", "Fake", "YPort"), "Strafe");

    DoubleSetting speed = registerDouble("Speed", 2, 0, 10, () -> mode.getValue().equals("Strafe") || mode.getValue().equalsIgnoreCase("Beta"));
    BooleanSetting jump = registerBoolean("Jump", true, () -> mode.getValue().equals("Strafe") || mode.getValue().equalsIgnoreCase("Beta"));
    BooleanSetting boost = registerBoolean("Boost", false, () -> mode.getValue().equalsIgnoreCase("Strafe") || mode.getValue().equalsIgnoreCase("Beta"));
    DoubleSetting multiply = registerDouble("Multiply", 0.8,0.1,1, () -> boost.getValue() && boost.isVisible());
    DoubleSetting max = registerDouble("Maximum", 0.5,0,1, () -> boost.getValue() && boost.isVisible());

    DoubleSetting gSpeed = registerDouble("Ground Speed", 0.3, 0, 0.5, () -> mode.getValue().equals("GroundStrafe"));

    DoubleSetting yPortSpeed = registerDouble("Speed YPort", 0.06, 0.01, 0.15, () -> mode.getValue().equals("YPort"));

    DoubleSetting onGroundSpeed = registerDouble("Speed OnGround", 1.5, 0.01, 3, () -> mode.getValue().equalsIgnoreCase("OnGround"));
    BooleanSetting strictOG = registerBoolean("Head Block Only", false, () -> mode.getValue().equalsIgnoreCase("OnGround"));

    DoubleSetting jumpHeight = registerDouble("Jump Speed", 0.41, 0, 1, () -> mode.getValue().equalsIgnoreCase("Strafe") && jump.getValue() || mode.getValue().equalsIgnoreCase("Beta") && jump.getValue());
    IntegerSetting jumpDelay = registerInteger("Jump Delay", 300, 0, 1000, () -> mode.getValue().equalsIgnoreCase("Strafe") && jump.getValue() || mode.getValue().equalsIgnoreCase("Beta") && jump.getValue());

    BooleanSetting useTimer = registerBoolean("Timer", false);
    DoubleSetting timerVal = registerDouble("Timer Speed", 1.088, 0.8,1.2);

    private boolean slowDown;
    private double playerSpeed;
    private double velocity;
    
    Timer kbTimer = new Timer();

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PlayerMoveEvent> playerMoveEventListener = new Listener<>(event -> {
        if (mc.player.isInLava() || mc.player.isInWater() || mc.player.isOnLadder() || mc.player.isInWeb || Anchor.active) {
            return;
        }

        if (mode.getValue().equalsIgnoreCase("Strafe")) {

            double speedY = jumpHeight.getValue();

            if (mc.player.onGround && jump.getValue()) mc.player.jump();

            if (mc.player.onGround && MotionUtil.isMoving(mc.player) && timer.hasReached(jumpDelay.getValue())) {
                if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
                    speedY += (Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST)).getAmplifier() + 1) * 0.1f;
                }

                if (jump.getValue())
                    event.setY(mc.player.motionY = speedY);

                playerSpeed = MotionUtil.getBaseMoveSpeed() * (EntityUtil.isColliding(0, -0.5, 0) instanceof BlockLiquid && !EntityUtil.isInLiquid() ? 0.91 : speed.getValue());
                slowDown = true;
                timer.reset();
            } else {
                if (slowDown || mc.player.collidedHorizontally) {
                    playerSpeed -= (EntityUtil.isColliding(0, -0.8, 0) instanceof BlockLiquid && !EntityUtil.isInLiquid()) ? 0.4 : 0.7 * (playerSpeed = MotionUtil.getBaseMoveSpeed());
                    slowDown = false;
                } else {
                    playerSpeed -= playerSpeed / 159.0;
                }
            }
            playerSpeed = Math.max(playerSpeed, MotionUtil.getBaseMoveSpeed());

            if (boost.getValue() && !kbTimer.hasReached(50))
                playerSpeed += Math.min(velocity * multiply.getValue(), max.getValue());

            double[] dir = MotionUtil.forward(playerSpeed);
            event.setX(dir[0]);
            event.setZ(dir[1]);

        }
        if (mode.getValue().equalsIgnoreCase("GroundStrafe")) {

            playerSpeed = gSpeed.getValue();

            playerSpeed *= MotionUtil.getBaseMoveSpeed() / 0.2873; // get speed

            if (mc.player.onGround) {
                double[] dir = MotionUtil.forward(playerSpeed);
                event.setX(dir[0]);
                event.setZ(dir[1]);
            }

        } else if (mode.getValue().equalsIgnoreCase("OnGround")) {

            if (mc.player.collidedHorizontally) {
                return;
            }

            double offset = 0.4;

            if (mc.world.collidesWithAnyBlock(mc.player.boundingBox.offset(0,0.4,0)))
                offset = 2 - mc.player.boundingBox.maxY;
            else if (strictOG.getValue())
                return;

                mc.player.posY -= offset;
            mc.player.motionY = -1000.0;
            mc.player.cameraPitch = 0.3f;
            mc.player.distanceWalkedModified = 44.0f;
            if (mc.player.onGround) {
                mc.player.posY += offset;
                mc.player.motionY = offset;
                mc.player.distanceWalkedOnStepModified = 44.0f;
                mc.player.motionX *= onGroundSpeed.getValue();
                mc.player.motionZ *= onGroundSpeed.getValue();
                mc.player.cameraPitch = 0.0f;
            }

        }

    });

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PacketEvent.Receive> receiveListener = new Listener<>(event -> {

        if (event.getPacket() instanceof SPacketExplosion) {
            velocity = Math.abs(((SPacketExplosion) event.getPacket()).motionX) + Math.abs(((SPacketExplosion) event.getPacket()).motionZ);
            kbTimer.reset();
        }

        if (event.getPacket() instanceof SPacketEntityVelocity) {
            if (((SPacketEntityVelocity) event.getPacket()).getEntityID() != mc.player.getEntityId())
                return;
            if (velocity < Math.abs(((SPacketEntityVelocity) event.getPacket()).motionX) + Math.abs(((SPacketEntityVelocity) event.getPacket()).motionZ)) {
                velocity = Math.abs(((SPacketEntityVelocity) event.getPacket()).motionX) + Math.abs(((SPacketEntityVelocity) event.getPacket()).motionZ);
                kbTimer.reset();
            }
        }

    });

    public void onEnable() {
        playerSpeed = MotionUtil.getBaseMoveSpeed();
        yl = ((int) mc.player.posY);
    }

    public void onDisable() {
        timer.reset();
    }

    public void onUpdate() {
        if (mc.player == null || mc.world == null) {
            disable();
            return;
        }

        if (Anchor.active)
            return;

        if (mode.getValue().equalsIgnoreCase("YPort")) {
            handleYPortSpeed();
        }

        if (!ModuleManager.isModuleEnabled(com.gamesense.client.module.modules.movement.Timer.class) && useTimer.getValue())
            mc.timer.tickLength = 50 / timerVal.getValue().floatValue();

    }

    private void handleYPortSpeed() {
        if (!MotionUtil.isMoving(mc.player) || mc.player.isInWater() && mc.player.isInLava() || mc.player.collidedHorizontally) {
            return;
        }

        if (mc.player.onGround) {
            mc.player.jump(); // motion = 0.42
            MotionUtil.setSpeed(mc.player, MotionUtil.getBaseMoveSpeed() + yPortSpeed.getValue()); // set speed
        } else {
            mc.player.motionY = -1; // return to ground instantly
        }
    }

    public String getHudInfo() {
        return "[" + ChatFormatting.WHITE + mode.getValue() + ChatFormatting.GRAY + "]";
    }

}