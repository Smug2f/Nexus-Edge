package com.gamesense.client.module.modules.movement;

import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.event.events.PlayerMoveEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.DoubleSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.api.util.misc.Timer;
import com.gamesense.api.util.player.InventoryUtil;
import com.gamesense.api.util.player.PlayerUtil;
import com.gamesense.api.util.player.RotationUtil;
import com.gamesense.api.util.world.MotionUtil;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.Vec2f;

import java.util.Arrays;

@Module.Declaration(name = "ElytraFly", category = Category.Movement)
public class ElytraFly extends Module {

    public BooleanSetting sound = registerBoolean("Sounds", true);
    ModeSetting mode = registerMode("Mode", Arrays.asList("Control", "Packet", "Boost"), "Boost");
    BooleanSetting replace = registerBoolean("Replace", false);
    ModeSetting toMode = registerMode("Takeoff", Arrays.asList("PacketFly", "Timer", "Freeze", "Fast", "None"), "PacketFly");
    ModeSetting upMode = registerMode("Up Mode", Arrays.asList("Jump", "Aim"), "Jump", () -> !mode.getValue().equals("Boost"));
    DoubleSetting speed = registerDouble("Speed", 2.5, 0, 10, () -> mode.getValue().equalsIgnoreCase("Control"));
    DoubleSetting ySpeed = registerDouble("Y Speed", 0, 1, 10, () -> mode.getValue().equalsIgnoreCase("Control"));
    DoubleSetting glideSpeed = registerDouble("Glide Speed", 0, 0, 3, () -> mode.getValue().equalsIgnoreCase("Control"));
    BooleanSetting yawLock = registerBoolean("Yaw Lock", false, () -> mode.getValue().equalsIgnoreCase("Control"));
    BooleanSetting pursue = registerBoolean("Pursue", false, () -> mode.getValue().equalsIgnoreCase("Control") && upMode.getValue().equalsIgnoreCase("Jump"));
    BooleanSetting build = registerBoolean("Build Height", false, () -> pursue.getValue() && pursue.isVisible());

    int ticks;
    boolean setAng,
            shouldEflyPacket;
    @EventHandler
    private final Listener<PacketEvent.Send> packetSendListener = new Listener<>(event -> {

        if (event.getPacket() instanceof CPacketPlayer && setAng && !mode.getValue().equalsIgnoreCase("Boost")) {

            ((CPacketPlayer) event.getPacket()).pitch = 0f; // spoof pitch

        }

    });
    Timer upTimer = new Timer();
    @EventHandler
    private final Listener<PlayerMoveEvent> playerMoveEventListener = new Listener<>(event -> {

        if (mc.player.isElytraFlying()) {

            mc.timer.tickLength = 50f;

            if (mode.getValue().equals("Boost")) {
                if (mc.gameSettings.keyBindJump.isKeyDown() || mc.gameSettings.keyBindForward.isKeyDown()) {
                    float yaw = (float) Math.toRadians(mc.player.rotationYaw);
                    mc.player.motionX -= Math.sin(yaw) * 0.05f;
                    mc.player.motionZ += Math.cos(yaw) * 0.05f;
                }

            } else if (mode.getValue().equals("Control")) {

                if (upMode.getValue().equalsIgnoreCase("Jump")) {

                    if (mc.gameSettings.keyBindJump.isKeyDown()) {

                        event.setY(ySpeed.getValue());

                    } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {

                        event.setY(-ySpeed.getValue());

                    } else {

                        event.setY(-0.000001 - glideSpeed.getValue());

                    }

                    if (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {

                        double[] dir;

                        if (!yawLock.getValue()) {

                            dir = MotionUtil.forward(speed.getValue());

                        } else {

                            final int angle = 360 / 8;
                            float yaw = mc.player.rotationYaw;
                            yaw = (float) (Math.round(yaw / angle) * angle);

                            dir = MotionUtil.forward(speed.getValue(), yaw);

                        }

                        mc.player.motionX = dir[0];
                        mc.player.motionZ = dir[1];

                        event.setX(dir[0]);
                        event.setZ(dir[1]);

                    } else {

                        if (!pursue.getValue()) {

                            event.setX(0);
                            event.setZ(0);

                        } else {

                            if (mc.player.posY > 256 || !build.getValue()) {
                                Entity target = PlayerUtil.findClosestTarget(696969, null);

                                if (target != null) {
                                    Vec2f rot = RotationUtil.getRotationTo(target.getPositionVector());

                                    double[] dir = MotionUtil.forward(Math.min(speed.getValue(), mc.player.getDistance(target)), rot.x);

                                    mc.player.setVelocity(dir[0], mc.player.motionY, dir[1]);

                                    if (mc.player.posY > target.posY)
                                        event.setY(-ySpeed.getValue());
                                    else if (mc.player.posY < target.posY)
                                        event.setY(Math.min(ySpeed.getValue(),target.posY));
                                    else
                                        event.setY(0);
                                } else {

                                    event.setX(0);
                                    event.setZ(0);

                                }
                            }

                        }

                    }

                } else if (upMode.getValue().equalsIgnoreCase("Aim")) {

                    if (mc.player.rotationPitch > 0 || (upTimer.getTimePassed() >= 1500)) {

                        upTimer.reset();

                        if (mc.gameSettings.keyBindSneak.isKeyDown()) {

                            event.setY(-ySpeed.getValue());

                        } else {

                            event.setY(-glideSpeed.getValue());

                        }

                        if (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {

                            double[] dir;

                            if (!yawLock.getValue()) {

                                dir = MotionUtil.forward(speed.getValue());

                            } else {

                                final int angle = 360 / 8;
                                float yaw = mc.player.rotationYaw;
                                yaw = (float) (Math.round(yaw / angle) * angle);

                                dir = MotionUtil.forward(speed.getValue(), yaw);

                            }

                            mc.player.motionX = dir[0];
                            mc.player.motionZ = dir[1];

                            event.setX(dir[0]);
                            event.setZ(dir[1]);

                        } else {

                            event.setX(0);
                            event.setZ(0);

                        }

                        setAng = true;

                        ticks++;

                    } else {

                        setAng = false;

                    }
                }
            } else if (mode.getValue().equalsIgnoreCase("Packet")) {

                shouldEflyPacket = !mc.player.onGround;

                if (shouldEflyPacket) {

                    setAng = true;

                    event.setY(-0.000001 - glideSpeed.getValue());

                    mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));

                    if (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {

                        double[] dir;

                        if (!yawLock.getValue()) {

                            dir = MotionUtil.forward(speed.getValue());

                        } else {

                            final int angle = 360 / 8;
                            float yaw = mc.player.rotationYaw;
                            yaw = (float) (Math.round(yaw / angle) * angle);

                            dir = MotionUtil.forward(speed.getValue(), yaw);

                        }

                        event.setX(dir[0]);
                        event.setZ(dir[1]);

                    } else {

                        event.setX(0);
                        event.setZ(0);

                    }
                }

            }
        } else {

            if (mc.gameSettings.keyBindJump.isKeyDown() && mc.player.inventory.armorInventory.get(2).getItem().equals(Items.ELYTRA) && !shouldEflyPacket) {
                switch (toMode.getValue()) {

                    case "PacketFly": {

                        if (mc.player.onGround) {

                            mc.player.jump();

                        } else if (mc.player.motionY < 0) {

                            mc.player.connection.sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX + mc.player.motionX, mc.player.posY - 0.0025, mc.player.posZ + mc.player.motionZ, mc.player.rotationYaw, mc.player.rotationPitch, false));
                            mc.player.connection.sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX + 55, mc.player.posY, mc.player.posZ + 55, mc.player.rotationYaw, mc.player.rotationPitch, false));
                            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));

                        }

                        break;

                    }
                    case "Timer": {

                        if (mc.player.onGround) {
                            mc.player.jump();
                        } else if (mc.player.motionY < 0) {
                            mc.timer.tickLength = 300f;
                            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                        }

                        break;

                    }
                    case "Fast": {

                        if (mc.player.onGround) {
                            mc.player.jump();
                        } else if (mc.player.motionY < 0) {

                            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));

                        }

                        break;

                    }
                    case "Freeze": {

                        if (mc.player.onGround) {
                            mc.player.jump();
                        } else if (mc.player.motionY < 0) {
                            event.setY(-0.00001);
                            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                        }

                        break;

                    }
                }
            }


        }

    });

    @Override
    protected void onEnable() {
        if (replace.getValue())
            if (!mc.player.inventory.armorItemInSlot(2).getItem().equals(Items.ELYTRA) && InventoryUtil.findFirstItemSlot(Items.ELYTRA.getClass(), 0, 35) != -1)
                InventoryUtil.swap(InventoryUtil.findFirstItemSlot(Items.ELYTRA.getClass(), 0, 35), 6);
    }

    @Override
    protected void onDisable() {
        mc.timer.tickLength = 50;

        // take off again if possible
        if (replace.getValue())
            if (mc.player.inventory.armorItemInSlot(2).getItem().equals(Items.ELYTRA) && InventoryUtil.findFirstItemSlot(Items.AIR.getClass(), 0, 35) != -1)
                InventoryUtil.swap(6, InventoryUtil.findFirstItemSlot(Items.AIR.getClass(), 0, 35));
    }
}
