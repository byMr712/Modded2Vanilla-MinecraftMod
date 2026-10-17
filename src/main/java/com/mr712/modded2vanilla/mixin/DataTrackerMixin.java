package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

@Mixin(SynchedEntityData.class)
public abstract class DataTrackerMixin {
    @Shadow
    @Final
    private SyncedDataHolder entity;

    @Shadow
    @Final
    private SynchedEntityData.DataItem<?>[] itemsById;

    @Inject(method = "defineId", at = @At("HEAD"))
    private static <T> void mrModded2Vanilla$onDefineId(
        Class<? extends SyncedDataHolder> entityClass, EntityDataSerializer<T> dataHandler, CallbackInfoReturnable<EntityDataAccessor<T>> cir
    ) {
        if (entityClass != null && entityClass.getName().startsWith("net.minecraft.")) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!AdjustmentTracker.isSystemClass(className)) {
                    AdjustmentTracker.recordClassName(className);
                    break;
                }
            }
        }
    }

    @Inject(method = "assignValues", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$safeAssignValues(
        List<SynchedEntityData.DataValue<?>> list, CallbackInfo ci
    ) {
        if (list == null || list.isEmpty()) {
            ci.cancel();
            return;
        }

        // In singleplayer, preserve normal vanilla and modded execution
        if (!IsolatorState.isCompensatingDataTracker()) {
            return;
        }

        ci.cancel();

        if (this.itemsById == null || this.itemsById.length == 0) {
            return;
        }

        for (final SynchedEntityData.DataValue<?> serializedEntry : list) {
            if (serializedEntry == null) {
                continue;
            }

            final int rawId = serializedEntry.id();
            final SynchedEntityData.DataItem<?> targetEntry = resolveMatchingEntry(rawId, serializedEntry);

            if (targetEntry != null) {
                if (targetEntry.getAccessor() != null && targetEntry.getAccessor().id() != rawId && this.entity != null) {
                    AdjustmentTracker.recordClass(this.entity.getClass());
                }
                try {
                    copySafely(targetEntry, serializedEntry);
                    if (this.entity != null) {
                        this.entity.onSyncedDataUpdated(targetEntry.getAccessor());
                    }
                } catch (Throwable t) {
                    MrModded2Vanilla.LOGGER.debug(
                        "[MrModded2Vanilla] Handled data update exception for entity {}: {}",
                        this.entity, t.getMessage()
                    );
                }
            } else {
                if (this.entity != null) {
                    AdjustmentTracker.recordClass(this.entity.getClass());
                }
                MrModded2Vanilla.LOGGER.debug(
                    "[MrModded2Vanilla] Suppressed unresolvable tracked entry id {} for entity {}",
                    rawId, this.entity
                );
            }
        }

        if (this.entity != null) {
            try {
                this.entity.onSyncedDataUpdated(list);
            } catch (Throwable t) {
                MrModded2Vanilla.LOGGER.debug(
                    "[MrModded2Vanilla] Handled onSyncedDataUpdated exception for entity {}: {}",
                    this.entity, t.getMessage()
                );
            }
        }
    }

    @Inject(method = "assignValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void mrModded2Vanilla$guardAssignValue(
        SynchedEntityData.DataItem<?> to, SynchedEntityData.DataValue<?> from, CallbackInfo ci
    ) {
        if (!IsolatorState.isCompensatingDataTracker()) {
            return;
        }
        if (to == null || from == null || !sameHandler(to.getAccessor(), from.serializer())) {
            if (this.entity != null) {
                AdjustmentTracker.recordClass(this.entity.getClass());
            }
            MrModded2Vanilla.LOGGER.debug(
                "[MrModded2Vanilla] Suppressed incompatible entity data update for {}: {}",
                this.entity, from
            );
            ci.cancel();
        }
    }

    private SynchedEntityData.DataItem<?> resolveMatchingEntry(int rawId, SynchedEntityData.DataValue<?> serializedEntry) {
        // 1. Direct index match
        if (isValidIndex(rawId) && sameHandler(this.itemsById[rawId].getAccessor(), serializedEntry.serializer())) {
            return this.itemsById[rawId];
        }

        // 2. Relative offset checks for common modded field shifts (+1, -1, +2, -2, +3, -3)
        final int[] offsets = { 1, -1, 2, -2, 3, -3, 4, -4 };
        for (final int offset : offsets) {
            final int candidate = rawId + offset;
            if (isValidIndex(candidate) && sameHandler(this.itemsById[candidate].getAccessor(), serializedEntry.serializer())) {
                return this.itemsById[candidate];
            }
        }

        // 3. Closest matching handler scan
        SynchedEntityData.DataItem<?> bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < this.itemsById.length; i++) {
            final SynchedEntityData.DataItem<?> entry = this.itemsById[i];
            if (entry != null && sameHandler(entry.getAccessor(), serializedEntry.serializer())) {
                int dist = Math.abs(i - rawId);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestMatch = entry;
                }
            }
        }

        return bestMatch;
    }

    @SuppressWarnings("unchecked")
    private static <T> void copySafely(SynchedEntityData.DataItem<T> to, SynchedEntityData.DataValue<?> from) {
        if (sameHandler(to.getAccessor(), from.serializer())) {
            to.setValue((T) from.value());
        }
    }

    private boolean isValidIndex(int id) {
        return id >= 0 && id < this.itemsById.length && this.itemsById[id] != null;
    }

    private static boolean sameHandler(EntityDataAccessor<?> data, EntityDataSerializer<?> handler) {
        return data != null && Objects.equals(data.serializer(), handler);
    }
}
