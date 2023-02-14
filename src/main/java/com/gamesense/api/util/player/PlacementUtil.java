package com.gamesense.api.util.player;

import com.gamesense.api.util.world.BlockUtil;
import com.gamesense.client.module.ModuleManager;
import com.gamesense.client.module.modules.combat.AutoCrystalRewrite;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class PlacementUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static int placementConnections = 0;
    private static boolean isSneaking = false;

    public static void onEnable() {
        placementConnections++;
    }

    public static void stopSneaking() {
        if (isSneaking) {
            isSneaking = false;
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }
    }

    public static void onDisable() {
        placementConnections--;
        if (placementConnections == 0) {
            if (isSneaking) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
                isSneaking = false;
            }
        }
    }

    public static boolean placeBlock(BlockPos blockPos, EnumHand hand, boolean rotate, Class<? extends Block> blockToPlace) {
        int oldSlot = mc.player.inventory.currentItem;
        int newSlot = InventoryUtil.findFirstBlockSlot(blockToPlace, 0, 8);

        if (newSlot == -1) {
            return false;
        }

        mc.player.inventory.currentItem = newSlot;
        boolean output = place(blockPos, hand, rotate);
        mc.player.inventory.currentItem = oldSlot;

        return output;
    }

    public static boolean placeBlockSilent(BlockPos blockPos, EnumHand hand, boolean rotate, Class<? extends Block> blockToPlace) {
        int oldSlot = mc.player.inventory.currentItem;
        int newSlot = InventoryUtil.findFirstBlockSlot(blockToPlace, 0, 8);

        if (newSlot == -1) {
            return false;
        }

        mc.player.connection.sendPacket(new CPacketHeldItemChange(newSlot));
        boolean output = place(blockPos, hand, rotate);
        mc.player.connection.sendPacket(new CPacketHeldItemChange(oldSlot));

        return output;
    }

    public static boolean placeItem(BlockPos blockPos, EnumHand hand, boolean rotate, Class<? extends Item> itemToPlace) {
        int oldSlot = mc.player.inventory.currentItem;
        int newSlot = InventoryUtil.findFirstItemSlot(itemToPlace, 0, 8);

        if (newSlot == -1) {
            return false;
        }

        mc.player.inventory.currentItem = newSlot;
        boolean output = place(blockPos, hand, rotate);
        mc.player.inventory.currentItem = oldSlot;

        return output;
    }

    public static boolean place(BlockPos blockPos, EnumHand hand, boolean rotate) {
        return placeBlock(blockPos, hand, rotate, true, null, true);
    }

    public static boolean place(BlockPos blockPos, EnumHand hand, boolean rotate, ArrayList<EnumFacing> forceSide) {
        return placeBlock(blockPos, hand, rotate, true, forceSide, true);
    }

    public static boolean place(BlockPos blockPos, EnumHand hand, boolean rotate, boolean checkAction) {
        return placeBlock(blockPos, hand, rotate, checkAction, null, true);
    }
    public static boolean place(BlockPos blockPos, EnumHand hand, boolean rotate, boolean checkAction, boolean swingArm) {
        return placeBlock(blockPos, hand, rotate, checkAction, null, swingArm);
    }

    public static boolean placeBlock(BlockPos blockPos, EnumHand hand, boolean rotate, boolean checkAction, ArrayList<EnumFacing> forceSide, boolean swingArm) {
        EntityPlayerSP player = mc.player;
        WorldClient world = mc.world;
        PlayerControllerMP playerController = mc.playerController;

        if (player == null || world == null || playerController == null) return false;

        if (!world.getBlockState(blockPos).getMaterial().isReplaceable()) {
            return false;
        }

        EnumFacing side = forceSide != null ? BlockUtil.getPlaceableSideExlude(blockPos, forceSide) : BlockUtil.getPlaceableSide(blockPos);

        if (side == null) {
            return false;
        }

        BlockPos neighbour = blockPos.offset(side);
        EnumFacing opposite = side.getOpposite();

        if (!BlockUtil.canBeClicked(neighbour)) {
            return false;
        }

        Vec3d hitVec = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(new Vec3d(opposite.getDirectionVec()).scale(0.5));
        Block neighbourBlock = world.getBlockState(neighbour).getBlock();

        if (!isSneaking && BlockUtil.blackList.contains(neighbourBlock) || BlockUtil.shulkerList.contains(neighbourBlock)) {
            player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING));
            isSneaking = true;
        }

        boolean stoppedAC = false;

        if (ModuleManager.isModuleEnabled(AutoCrystalRewrite.class)) {
            AutoCrystalRewrite.stopAC = true;
            stoppedAC = true;
        }

        if (rotate) {
            BlockUtil.faceVectorPacketInstant(hitVec, true);
        }

        EnumActionResult action = playerController.processRightClickBlock(player, world, neighbour, opposite, hitVec, hand);
        if (!checkAction || action == EnumActionResult.SUCCESS) {
            if (swingArm) {
                player.swingArm(hand);
                mc.rightClickDelayTimer = 4;
            }
            else {
                player.connection.sendPacket(new CPacketAnimation(hand));
            }
        }

        if (stoppedAC) {
            AutoCrystalRewrite.stopAC = false;
        }

        return action == EnumActionResult.SUCCESS;
    }

    public static CPacketPlayer.Rotation placeBlockGetRotate(BlockPos blockPos, EnumHand hand, boolean checkAction, ArrayList<EnumFacing> forceSide, boolean swingArm) {
        EntityPlayerSP player = mc.player;
        WorldClient world = mc.world;
        PlayerControllerMP playerController = mc.playerController;

        if (player == null || world == null || playerController == null) return null;

        if (!world.getBlockState(blockPos).getMaterial().isReplaceable()) {
            return null;
        }

        EnumFacing side = forceSide != null ? BlockUtil.getPlaceableSideExlude(blockPos, forceSide) : BlockUtil.getPlaceableSide(blockPos);

        if (side == null) {
            return null;
        }

        BlockPos neighbour = blockPos.offset(side);
        EnumFacing opposite = side.getOpposite();

        if (!BlockUtil.canBeClicked(neighbour)) {
            return null;
        }

        Vec3d hitVec = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(new Vec3d(opposite.getDirectionVec()).scale(0.5));
        Block neighbourBlock = world.getBlockState(neighbour).getBlock();

        if (!isSneaking && BlockUtil.blackList.contains(neighbourBlock) || BlockUtil.shulkerList.contains(neighbourBlock)) {
            player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING));
            isSneaking = true;
        }

        boolean stoppedAC = false;

        if (ModuleManager.isModuleEnabled(AutoCrystalRewrite.class)) {
            AutoCrystalRewrite.stopAC = true;
            stoppedAC = true;
        }

        EnumActionResult action = playerController.processRightClickBlock(player, world, neighbour, opposite, hitVec, hand);
        if (!checkAction || action == EnumActionResult.SUCCESS) {
            if (swingArm) {
                player.swingArm(hand);
                mc.rightClickDelayTimer = 4;
            }
            else {
                player.connection.sendPacket(new CPacketAnimation(hand));
            }
        }

        if (stoppedAC) {
            AutoCrystalRewrite.stopAC = false;
        }

        return BlockUtil.getFaceVectorPacket(hitVec, true);
    }

    public static boolean placePrecise(BlockPos blockPos, EnumHand hand, boolean rotate, Vec3d precise, EnumFacing forceSide, boolean onlyRotation, boolean support) {
        EntityPlayerSP player = mc.player;
        WorldClient world = mc.world;
        PlayerControllerMP playerController = mc.playerController;

        if (player == null || world == null || playerController == null) return false;

        if (!world.getBlockState(blockPos).getMaterial().isReplaceable()) {
            return false;
        }

        EnumFacing side = forceSide == null ? BlockUtil.getPlaceableSide(blockPos) : forceSide;

        if (side == null) {
            return false;
        }

        BlockPos neighbour = blockPos.offset(side);
        EnumFacing opposite = side.getOpposite();

        if (!BlockUtil.canBeClicked(neighbour)) {
            return false;
        }

        Vec3d hitVec = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(new Vec3d(opposite.getDirectionVec()).scale(0.5));
        Block neighbourBlock = world.getBlockState(neighbour).getBlock();

        if (!isSneaking && BlockUtil.blackList.contains(neighbourBlock) || BlockUtil.shulkerList.contains(neighbourBlock)) {
            player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING));
            isSneaking = true;
        }

        boolean stoppedAC = false;

        if (ModuleManager.isModuleEnabled(AutoCrystalRewrite.class)) {
            AutoCrystalRewrite.stopAC = true;
            stoppedAC = true;
        }

        if (rotate && !support) {
            BlockUtil.faceVectorPacketInstant(precise == null ? hitVec : precise, true);
        }

        if (!onlyRotation) {
            EnumActionResult action = playerController.processRightClickBlock(player, world, neighbour, opposite, precise == null ? hitVec : precise, hand);
            if (action == EnumActionResult.SUCCESS) {
                player.swingArm(hand);
                mc.rightClickDelayTimer = 4;
            }

            if (stoppedAC) {
                AutoCrystalRewrite.stopAC = false;
            }

            return action == EnumActionResult.SUCCESS;
        }

        return true;
    }
}