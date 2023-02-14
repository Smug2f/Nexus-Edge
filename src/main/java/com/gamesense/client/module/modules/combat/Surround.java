package com.gamesense.client.module.modules.combat;

import com.gamesense.api.event.events.OnUpdateWalkingPlayerEvent;
import com.gamesense.api.event.events.PacketEvent;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.IntegerSetting;
import com.gamesense.api.util.misc.Timer;
import com.gamesense.api.util.player.InventoryUtil;
import com.gamesense.api.util.player.PlacementUtil;
import com.gamesense.api.util.player.PlayerPacket;
import com.gamesense.api.util.player.PlayerUtil;
import com.gamesense.api.util.world.BlockUtil;
import com.gamesense.client.manager.managers.PlayerPacketManager;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;


@Module.Declaration(name = "Surround", category = Category.Combat, priority = 101)
public class Surround extends Module {

    private final Timer delayTimer = new Timer();
    BooleanSetting allowNon1x1 = registerBoolean("Allow non 1x1", true);
    BooleanSetting centre = registerBoolean("Centre", false, () -> !allowNon1x1.getValue());
    IntegerSetting delayTicks = registerInteger("Tick Delay", 3, 0, 10);
    IntegerSetting blocksPerTick = registerInteger("Blocks Per Tick", 4, 1, 20);
    BooleanSetting onlyOnStop = registerBoolean("OnStop", false);
    BooleanSetting disableOnJump = registerBoolean("Disable On Jump", false);
    BooleanSetting onlyOnSneak = registerBoolean("Only on Sneak", false);
    BooleanSetting rotate = registerBoolean("Rotate", false);
    IntegerSetting afterRotate = registerInteger("After Rotate", 3, 0, 5, () -> rotate.getValue());
    BooleanSetting destroyCrystal = registerBoolean("Destroy Stuck Crystal", false);
    BooleanSetting destroyAboveCrystal = registerBoolean("Destroy Above Crystal", false);
    BooleanSetting alertPlayerClip = registerBoolean("Alert Player Clip", true);
    BooleanSetting stopAC = registerBoolean("Stop AC", false);
    IntegerSetting tickStopAC = registerInteger("Tick Stop AC", 3, 0, 5, () -> stopAC.getValue());
    ArrayList<BlockPos> blockChanged = new ArrayList<>();

    int y;

    @EventHandler
    private final Listener<PacketEvent.Receive> listener2 = new Listener<>(event -> {
        /*
        if (event.getPacket() instanceof SPacketBlockChange && this.predict.getValue()) {
            SPacketBlockChange packet = (SPacketBlockChange) event.getPacket();
            if (!blockChanged.contains(packet.getBlockPosition())) {
                for (BlockPos pos : this.getOffsets()) {
                    if (!pos.equals(packet.getBlockPosition()) || packet.getBlockState().getBlock() != Blocks.AIR)
                        continue;
                    int blockSlot = this.getSlot();
                    if (blockSlot == -1) {
                        return;
                    }
                    if (blockSlot != mc.player.inventory.currentItem)
                        mc.player.connection.sendPacket(new CPacketHeldItemChange(blockSlot));
                    PlacementUtil.place(pos, EnumHand.MAIN_HAND, false, false);
                    if (blockSlot != mc.player.inventory.currentItem) {
                        mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem));
                        mc.playerController.updateController();
                    }
                    if (stopAC.getValue()) {
                        tickAC = tickStopAC.getValue();
                        AutoCrystalRewrite.stopAC = true;
                    }
                    PistonCrystal.printDebug("Predict Place", false);
                    blockChanged.add(pos);
                    break;
                }
            }
        }*/
    });
    Timer alertDelay = new Timer();
    boolean hasPlaced;
    int lookDown = -1;
    @EventHandler
    private final Listener<OnUpdateWalkingPlayerEvent> onUpdateWalkingPlayerEventListener = new Listener<>(event -> {
        if (mc.player == null || mc.world == null || lookDown == -1)
            return;

        PlayerPacketManager.INSTANCE.addPacket(new PlayerPacket(this, new Vec2f(0, 90)));
        lookDown--;

    });

    int getSlot() {
        int slot = InventoryUtil.findFirstBlockSlot(Blocks.OBSIDIAN.getClass(), 0, 8);
        if (slot == -1) {
            slot = InventoryUtil.findFirstBlockSlot(Blocks.ENDER_CHEST.getClass(), 0, 8);
        }
        return slot;
    }
    int tickAC = -1;
    @Override
    protected void onEnable() {
        alertDelay.setTimer(0);
        y = (int) Math.floor(mc.player.posY);
    }

    public void onUpdate() {

        if (mc.player == null || mc.world == null)
            return;

        if (onlyOnStop.getValue() && (mc.player.motionX != 0 || mc.player.motionY != 0 || mc.player.motionZ != 0))
            return;

        if (onlyOnSneak.getValue() && !mc.gameSettings.keyBindSneak.isPressed())
            return;

        if (disableOnJump.getValue() && Math.abs(Math.abs(y) - Math.abs(mc.player.posY)) >= 0.3) { // enough for jump to cause unless under a block
            disable();
            return;
        }

        if (tickAC > -1) {
            if (--tickAC == 0)
                AutoCrystalRewrite.stopAC = false;
        }


        if (delayTimer.getTimePassed() / 50L >= delayTicks.getValue()) {
            delayTimer.reset();

            int blocksPlaced = 0;

            hasPlaced = false;

            List<BlockPos> offsetPattern = this.getOffsets();
            int maxSteps = offsetPattern.size();
            boolean hasSilentSwitched = false;
            int blockSlot = this.getSlot();
            if (blockSlot == -1)
                return;

            int offsetSteps = 0;
            if (centre.getValue() && !allowNon1x1.getValue()) {
                PlayerUtil.centerPlayer(mc.player.getPositionVector());
            }
            while (blocksPlaced <= blocksPerTick.getValue()) {

                if (offsetSteps >= maxSteps) {
                    break;
                }

                BlockPos targetPos = offsetPattern.get(offsetSteps++);

                if (blockChanged.contains(targetPos))
                    continue;

                mc.world.getEntitiesInAABBexcluding(null, new AxisAlignedBB(targetPos), null);
                boolean foundSomeone = false;
                for (Entity entity : mc.world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(targetPos))) {
                    if (entity instanceof EntityPlayer) {
                        foundSomeone = true;
                        if (alertPlayerClip.getValue() && entity != mc.player) {
                            if (alertDelay.hasReached(1000)) {
                                PistonCrystal.printDebug("Player " + entity.getName() + " is clipping in your surround", false);
                                alertDelay.reset();
                            }
                        }
                        break;
                    }
                    if (entity instanceof EntityEnderCrystal && destroyCrystal.getValue()) {
                        if (rotate.getValue()) {
                            BlockUtil.faceVectorPacketInstant(new Vec3d(entity.getPosition()).add(0.5, 0, 0.5), true);
                        }
                        mc.player.connection.sendPacket(new CPacketUseEntity(entity));
                        mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
                    }
                }

                if (destroyAboveCrystal.getValue()) {
                    for (Entity entity : new ArrayList<>(mc.world.loadedEntityList)) {
                        if (entity instanceof EntityEnderCrystal) {
                            if (sameBlockPos(entity.getPosition(), targetPos)) {
                                if (rotate.getValue()) {
                                    BlockUtil.faceVectorPacketInstant(new Vec3d(entity.getPosition()).add(0.5, 0, 0.5), true);
                                }
                                mc.player.connection.sendPacket(new CPacketUseEntity(entity));
                                mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
                            }
                        }
                    }
                }


                if (foundSomeone)
                    continue;

                if (!mc.world.getBlockState(targetPos).getMaterial().isReplaceable())
                    continue;

                if (!hasSilentSwitched) {
                    if (blockSlot != mc.player.inventory.currentItem) {
                        mc.player.connection.sendPacket(new CPacketHeldItemChange(blockSlot));
                        hasSilentSwitched = true;
                    }
                }

                if (PlacementUtil.place(targetPos, EnumHand.MAIN_HAND, rotate.getValue(), false)) {

                    if (centre.getValue())
                        PlayerUtil.centerPlayer(mc.player.getPositionVector());

                    y = (int) Math.floor(mc.player.posY);

                    if (stopAC.getValue()) {
                        tickAC = tickStopAC.getValue();
                        AutoCrystalRewrite.stopAC = true;
                    }

                    PistonCrystal.printDebug("Normal Place", false);

                    blocksPlaced++;
                    if (rotate.getValue())
                        if (afterRotate.getValue() != 0)
                            lookDown = afterRotate.getValue();
                }


            }


            if (hasSilentSwitched) {
                mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem));
                mc.playerController.updateController();
            }

        }

        PlacementUtil.stopSneaking();
        blockChanged.clear();


    }

    // Say if two blockPos are the same
    boolean sameBlockPos(BlockPos first, BlockPos second) {
        if (first == null || second == null)
            return false;
        return first.getX() == second.getX() && first.getY() == second.getY() + 2 && first.getZ() == second.getZ();
    }


    List<BlockPos> getOffsets() {
        BlockPos playerPos = this.getPlayerPos();
        ArrayList<BlockPos> offsets = new ArrayList<BlockPos>();
        if (this.allowNon1x1.getValue()) {
            int z;
            int x;
            double decimalX = Math.abs(mc.player.posX) - Math.floor(Math.abs(mc.player.posX));
            double decimalZ = Math.abs(mc.player.posZ) - Math.floor(Math.abs(mc.player.posZ));
            int lengthXPos = this.calcLength(decimalX, false);
            int lengthXNeg = this.calcLength(decimalX, true);
            int lengthZPos = this.calcLength(decimalZ, false);
            int lengthZNeg = this.calcLength(decimalZ, true);
            ArrayList<BlockPos> tempOffsets = new ArrayList<BlockPos>();
            offsets.addAll(this.getOverlapPos());
            for (x = 1; x < lengthXPos + 1; ++x) {
                tempOffsets.add(this.addToPlayer(playerPos, x, 0.0, 1 + lengthZPos));
                tempOffsets.add(this.addToPlayer(playerPos, x, 0.0, -(1 + lengthZNeg)));
            }
            for (x = 0; x <= lengthXNeg; ++x) {
                tempOffsets.add(this.addToPlayer(playerPos, -x, 0.0, 1 + lengthZPos));
                tempOffsets.add(this.addToPlayer(playerPos, -x, 0.0, -(1 + lengthZNeg)));
            }
            for (z = 1; z < lengthZPos + 1; ++z) {
                tempOffsets.add(this.addToPlayer(playerPos, 1 + lengthXPos, 0.0, z));
                tempOffsets.add(this.addToPlayer(playerPos, -(1 + lengthXNeg), 0.0, z));
            }
            for (z = 0; z <= lengthZNeg; ++z) {
                tempOffsets.add(this.addToPlayer(playerPos, 1 + lengthXPos, 0.0, -z));
                tempOffsets.add(this.addToPlayer(playerPos, -(1 + lengthXNeg), 0.0, -z));
            }
            for (BlockPos pos : tempOffsets) {
                if (getDown(pos)) {
                    offsets.add(pos.add(0, -1, 0));
                }
                offsets.add(pos);
            }
        } else {
            offsets.add(playerPos.add(0, -1, 0));
            for (int[] surround : new int[][]{
                    {1, 0},
                    {0, 1},
                    {-1, 0},
                    {0, -1}
            }) {
                if (getDown(playerPos.add(surround[0], 0, surround[1])))
                    offsets.add(playerPos.add(surround[0], -1, surround[1]));

                offsets.add(playerPos.add(surround[0], 0, surround[1]));
            }
        }
        return offsets;
    }

    public static boolean getDown(BlockPos pos) {

        for (EnumFacing e : EnumFacing.values())
            if (!mc.world.isAirBlock(pos.add(e.getDirectionVec())))
                return false;

        return true;

    }

    int calcOffset(double dec) {
        return dec >= 0.7 ? 1 : (dec <= 0.3 ? -1 : 0);
    }

    BlockPos addToPlayer(BlockPos playerPos, double x, double y, double z) {
        if (playerPos.getX() < 0) {
            x = -x;
        }
        if (playerPos.getY() < 0) {
            y = -y;
        }
        if (playerPos.getZ() < 0) {
            z = -z;
        }
        return playerPos.add(x, y, z);
    }

    List<BlockPos> getOverlapPos() {
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        double decimalX = mc.player.posX - Math.floor(mc.player.posX);
        double decimalZ = mc.player.posZ - Math.floor(mc.player.posZ);
        int offX = this.calcOffset(decimalX);
        int offZ = this.calcOffset(decimalZ);
        positions.add(this.getPlayerPos());
        for (int x = 0; x <= Math.abs(offX); ++x) {
            for (int z = 0; z <= Math.abs(offZ); ++z) {
                int properX = x * offX;
                int properZ = z * offZ;
                positions.add(this.getPlayerPos().add(properX, -1, properZ));
            }
        }
        return positions;
    }


    int calcLength(double decimal, boolean negative) {
        if (negative) {
            return decimal <= 0.3 ? 1 : 0;
        }
        return decimal >= 0.7 ? 1 : 0;
    }

    BlockPos getPlayerPos() {
        double decimalPoint = mc.player.posY - Math.floor(mc.player.posY);
        return new BlockPos(mc.player.posX, decimalPoint > 0.8 ? Math.floor(mc.player.posY) + 1.0 : Math.floor(mc.player.posY), mc.player.posZ);
    }


}