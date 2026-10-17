package com.mr712.modded2vanilla.isolator;

import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Universal movement and physics isolator for multiplayer servers.
 * Enforces pure vanilla movement kinematics, onGround invariants, ladder climbing limits, and step height bounds.
 */
public final class MovementIsolator {

    public static final float VANILLA_MAX_STEP_HEIGHT = 0.6F;
    public static final double VANILLA_MAX_CLIMB_UP_SPEED = 0.2D;
    public static final double VANILLA_MAX_CLIMB_DOWN_SPEED = -0.15000000596046448D;

    private MovementIsolator() {
    }

    public static Vec3 sanitizeClimbingSpeed(LocalPlayer player, Vec3 motion) {
        player.resetFallDistance();
        double x = Mth.clamp(motion.x, VANILLA_MAX_CLIMB_DOWN_SPEED, -VANILLA_MAX_CLIMB_DOWN_SPEED);
        double z = Mth.clamp(motion.z, VANILLA_MAX_CLIMB_DOWN_SPEED, -VANILLA_MAX_CLIMB_DOWN_SPEED);
        double y = Math.max(motion.y, VANILLA_MAX_CLIMB_DOWN_SPEED);
        if (y < 0.0D && !player.getInBlockState().is(Blocks.SCAFFOLDING) && player.isSuppressingSlidingDownLadder() && player instanceof Player) {
            y = 0.0D;
        }
        return new Vec3(x, y, z);
    }

    public static void detectAndRecordCallerMod() {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : traces) {
            String className = element.getClassName();
            if (!AdjustmentTracker.isSystemClass(className)) {
                AdjustmentTracker.recordClassName(className);
                return;
            }
        }
    }
}
