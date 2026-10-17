package com.mr712.modded2vanilla.snapshot;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class VanillaBlockStateSnapshot {

    private static BlockState[] VANILLA_STATES = null;
    private static Reference2IntOpenHashMap<BlockState> STATE_TO_RAW_ID = null;
    private static volatile boolean captured = false;

    private VanillaBlockStateSnapshot() {
    }

    public static synchronized void capture() {
        if (captured) {
            return;
        }
        captured = true;

        int size = Block.BLOCK_STATE_REGISTRY.size();
        VANILLA_STATES = new BlockState[size];
        STATE_TO_RAW_ID = new Reference2IntOpenHashMap<>(size);
        STATE_TO_RAW_ID.defaultReturnValue(-1);

        for (int i = 0; i < size; i++) {
            BlockState state = Block.BLOCK_STATE_REGISTRY.byId(i);
            VANILLA_STATES[i] = state;
            if (state != null) {
                STATE_TO_RAW_ID.put(state, i);
            }
        }
        MrModded2Vanilla.LOGGER.info("Captured pure vanilla block state snapshot ({} canonical states).", VANILLA_STATES.length);
    }

    public static BlockState resolveState(int rawId) {
        if (!IsolatorState.isIsolating() || VANILLA_STATES == null) {
            BlockState normal = Block.BLOCK_STATE_REGISTRY.byId(rawId);
            return normal != null ? normal : Blocks.AIR.defaultBlockState();
        }

        if (rawId >= 0 && rawId < VANILLA_STATES.length) {
            BlockState canonical = VANILLA_STATES[rawId];
            if (canonical != null) {
                return canonical;
            }
        }

        BlockState fallback = Block.BLOCK_STATE_REGISTRY.byId(rawId);
        return fallback != null ? fallback : Blocks.AIR.defaultBlockState();
    }

    public static int getRawId(BlockState state) {
        if (!IsolatorState.isIsolating() || STATE_TO_RAW_ID == null || state == null) {
            return Block.BLOCK_STATE_REGISTRY.getId(state);
        }

        int canonicalId = STATE_TO_RAW_ID.getInt(state);
        if (canonicalId != -1) {
            return canonicalId;
        }

        return Block.BLOCK_STATE_REGISTRY.getId(state);
    }

    public static boolean isCaptured() {
        return captured;
    }
}
