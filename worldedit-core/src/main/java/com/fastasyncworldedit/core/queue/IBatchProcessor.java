package com.fastasyncworldedit.core.queue;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.processor.EmptyBatchProcessor;
import com.fastasyncworldedit.core.extent.processor.MultiBatchProcessor;
import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.apache.logging.log4j.LogManager.getLogger;

public interface IBatchProcessor {

    /**
     * Process a chunk that has been set.
     */
    IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set);

    Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set);

    default boolean processGet(int chunkX, int chunkZ) {
        return true;
    }

    /**
     * Convert this processor into an Extent based processor instead of a queue batch based on.
     */
    @Nullable
    Extent construct(Extent child);

    /**
     * Utility method to trim a chunk based on min and max Y.
     *
     * @return false if chunk is empty of blocks
     */
    default boolean trimY(IChunkSet set, int minY, int maxY) {
        int minLayer = (minY - 1) >> 4;
        for (int layer = 0; layer <= minLayer; layer++) {
            if (set.hasSection(layer)) {
                if (layer == minLayer) {
                    char[] arr = set.load(layer);
                    int index = (minY & 15) << 8;
                    for (int i = 0; i < index; i++) {
                        arr[i] = 0;
                    }
                    set.setBlocks(layer, arr);
                } else {
                    set.setBlocks(layer, null);
                }
            }
        }
        int maxLayer = (maxY + 1) >> 4;
        for (int layer = maxLayer; layer < FaweCache.IMP.CHUNK_LAYERS; layer++) {
            if (set.hasSection(layer)) {
                if (layer == minLayer) {
                    char[] arr = set.load(layer);
                    int index = ((maxY + 1) & 15) << 8;
                    for (int i = index; i < arr.length; i++) {
                        arr[i] = 0;
                    }
                    set.setBlocks(layer, arr);
                } else {
                    set.setBlocks(layer, null);
                }
            }
        }
        try {
            int layer = (minY - 15) >> 4;
            while (layer < (maxY + 15) >> 4) {
                if (layer > -1) {
                    if (set.hasSection(layer)) {
                        return true;
                    }
                }
                layer++;
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            getLogger(IBatchProcessor.class).error("minY = {} , layer = {}", minY, ((minY - 15) >> 4), exception);
        }
        return false;
    }

    /**
     * Utility method to trim entity and blocks with a provided contains function.
     *
     * @return false if chunk is empty of NBT
     */
    default boolean trimNBT(IChunkSet set, Function<BlockVector3, Boolean> contains) {
        Set<CompoundTag> ents = set.getEntities();
        if (!ents.isEmpty()) {
            ents.removeIf(ent -> !contains.apply(ent.getEntityPosition().toBlockPoint()));
        }
        Map<BlockVector3, CompoundTag> tiles = set.getTiles();
        if (!tiles.isEmpty()) {
            tiles.entrySet().removeIf(blockVector3CompoundTagEntry -> !contains
                    .apply(blockVector3CompoundTagEntry.getKey()));
        }
        return !tiles.isEmpty() || !ents.isEmpty();
    }

    /**
     * Join two processors and return the result.
     */
    default IBatchProcessor join(IBatchProcessor other) {
        return MultiBatchProcessor.of(this, other);
    }

    default IBatchProcessor joinPost(IBatchProcessor other) {
        return MultiBatchProcessor.of(this, other);
    }

    default void flush() {
    }

    /**
     * Return a new processor after removing all are instances of a specified class.
     */
    default <T extends IBatchProcessor> IBatchProcessor remove(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            return EmptyBatchProcessor.getInstance();
        }
        return this;
    }

    /**
     * Default to CUSTOM ProcessorScope as we want custom processors people add to be before we write history, but after FAWE does it's own stuff.
     */
    default ProcessorScope getScope() {
        return ProcessorScope.CUSTOM;
    }

}
