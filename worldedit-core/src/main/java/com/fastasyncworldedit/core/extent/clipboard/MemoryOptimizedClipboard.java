package com.fastasyncworldedit.core.extent.clipboard;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.jnbt.streamer.IntValueReader;
import com.fastasyncworldedit.core.math.IntTriple;
import com.fastasyncworldedit.core.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard.ClipboardEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemoryOptimizedClipboard extends LinearClipboard {

    private static final int BLOCK_SIZE = 1048576 * 2;
    private static final int BLOCK_MASK = 1048575;
    private static final int BLOCK_SHIFT = 20;

    private final byte[][] states;

    private final byte[] buffer = new byte[MainUtil.getMaxCompressedLength(BLOCK_SIZE)];
    private byte[] biomes = null;

    private final HashMap<IntTriple, CompoundTag> nbtMap;


    private int lastOrdinalsI = -1;

    private byte[] lastOrdinals;

    private boolean saveOrdinals = false;

    private final int compressionLevel;

    public MemoryOptimizedClipboard(Region region) {
        this(region, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
    }

    public MemoryOptimizedClipboard(Region region, int compressionLevel) {
        super(region.getDimensions());
        states = new byte[1 + (getVolume() >> BLOCK_SHIFT)][];
        nbtMap = new HashMap<>();
        this.compressionLevel = compressionLevel;
    }


    @Override
    public boolean hasBiomes() {
        return biomes != null;
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return setBiome(position.getX(), position.getY(), position.getZ(), biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        setBiome(getBiomeIndex(x, y, z), biome);
        return true;
    }

    @Override
    public void setBiome(int index, BiomeType biome) {
        if (biomes == null) {
            biomes = new byte[((getHeight() >> 2) + 1) * ((getLength() >> 2) + 1) * ((getWidth() >> 2) + 1)];
        }
        biomes[index] = (byte) biome.getInternalId();
    }

    @Override
    public void streamBiomes(IntValueReader task) {
        if (!hasBiomes()) {
            return;
        }
        try {
            for (int y = 0; y < getHeight(); y++) {
                for (int z = 0; z < getLength(); z++) {
                    for (int x = 0; x < getWidth(); x++) {
                        task.applyInt(getIndex(x, y, z), biomes[getBiomeIndex(x, y, z)] & 0xFF);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public BiomeType getBiome(int index) {
        if (!hasBiomes()) {
            return null;
        }
        return BiomeTypes.get(biomes[index]);
    }

    @Override
    public BiomeType getBiomeType(int x, int y, int z) {
        return getBiome(getBiomeIndex(x, y, z));
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return getBiome(getBiomeIndex(position.getX(), position.getY(), position.getZ()));
    }

    private int getOrdinal(int index) {
        int i = index >> BLOCK_SHIFT;
        int li = (index & BLOCK_MASK) << 1;
        if (i != lastOrdinalsI) {
            saveOrdinals();
            byte[] compressed = states[lastOrdinalsI = i];
            if (compressed != null) {
                lastOrdinals = MainUtil.decompress(compressed, lastOrdinals, BLOCK_SIZE, compressionLevel);
            } else {
                lastOrdinals = null;
                return 0;
            }
        }
        if (lastOrdinals == null) {
            return 0;
        }
        return (((lastOrdinals[li] & 0xFF) << 8) + (lastOrdinals[li + 1] & 0xFF));
    }

    private void saveOrdinals() {
        if (saveOrdinals && lastOrdinals != null) {
            states[lastOrdinalsI] = MainUtil.compress(lastOrdinals, buffer, compressionLevel);
        }
        saveOrdinals = false;
    }

    private int lastI;
    private int lastIMin;
    private int lastIMax;

    private int getLocalIndex(int index) {
        if (index < lastIMin || index > lastIMax) {
            lastI = index >> BLOCK_SHIFT;
            lastIMin = lastI << BLOCK_SHIFT;
            lastIMax = lastIMin + BLOCK_MASK;
        }
        return lastI;
    }

    private void setOrdinal(int index, int v) {
        int i = getLocalIndex(index);
        if (i != lastOrdinalsI) {
            saveOrdinals();
            byte[] compressed = states[lastOrdinalsI = i];
            if (compressed != null) {
                lastOrdinals = MainUtil.decompress(compressed, lastOrdinals, BLOCK_SIZE, compressionLevel);
            } else {
                lastOrdinals = null;
            }
        }
        if (lastOrdinals == null) {
            BlockType bt = BlockTypes.getFromStateOrdinal(v);
            if (bt.getMaterial().isAir()) {
                return;
            }
            lastOrdinals = new byte[BLOCK_SIZE];
        }
        int li = (index & BLOCK_MASK) << 1;
        lastOrdinals[li] = (byte) ((v >>> 8) & 0xFF);
        lastOrdinals[li + 1] = (byte) (v & 0xFF);
        saveOrdinals = true;
    }

    @Override
    public Collection<CompoundTag> getTileEntities() {
        return nbtMap.values();
    }

    public int getIndex(int x, int y, int z) {
        return x + y * getArea() + z * getWidth();
    }

    public int getBiomeIndex(int x, int y, int z) {
        return (x >> 2) + (y >> 2) * (getWidth() >> 2) * (getLength() >> 2) + (z >> 2) * (getWidth() >> 2);
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        int index = getIndex(x, y, z);
        return getFullBlock(index);
    }

    private BaseBlock toBaseBlock(BlockState state, int i) {
        if (state.getMaterial().hasContainer() && !nbtMap.isEmpty()) {
            CompoundTag nbt;
            if (nbtMap.size() < 4) {
                nbt = null;
                for (Map.Entry<IntTriple, CompoundTag> entry : nbtMap.entrySet()) {
                    IntTriple trio = entry.getKey();
                    int index = getIndex(trio.getX(), trio.getY(), trio.getZ());
                    if (index == i) {
                        nbt = entry.getValue();
                        break;
                    }
                }
            } else {
                int y = i / getArea();
                int newI = i - y * getArea();
                int z = newI / getWidth();
                int x = newI - z * getWidth();
                nbt = nbtMap.get(new IntTriple(x, y, z));
            }
            return state.toBaseBlock(nbt);
        }
        return state.toBaseBlock();
    }

    @Override
    public BaseBlock getFullBlock(int index) {
        return toBaseBlock(getBlock(index), index);
    }

    @Override
    public BlockState getBlock(int index) {
        int combinedID = getOrdinal(index);
        return BlockState.getFromOrdinal(combinedID);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getBlock(getIndex(x, y, z));
    }

    public int size() {
        saveOrdinals();
        int total = 0;
        for (byte[] array : states) {
            if (array != null) {
                total += array.length;
            }
        }
        return total;
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        final Map<String, Tag> values = new HashMap<>(tag.getValue());
        values.put("x", new IntTag(x));
        values.put("y", new IntTag(y));
        values.put("z", new IntTag(z));
        nbtMap.put(new IntTriple(x, y, z), new CompoundTag(values));
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        return setBlock(getIndex(x, y, z), block);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int index, B block) {
        int ordinal = block.getOrdinal();
        if (ordinal == 0) {
            ordinal = 1;
        }
        setOrdinal(index, ordinal);
        boolean hasNbt = block instanceof BaseBlock && block.hasNbtData();
        if (hasNbt) {
            int y = index / getArea();
            int newI = index - y * getArea();
            int z = newI / getWidth();
            int x = newI - z * getWidth();
            setTile(x, y, z, block.getNbtData());
        }
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        BlockArrayClipboard.ClipboardEntity ret = new BlockArrayClipboard.ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return new ArrayList<>(entities
                .stream()
                .filter(e -> region.contains(e.getLocation().toBlockPoint()))
                .collect(Collectors.toList()));
    }

    @Override
    public void removeEntity(Entity entity) {
        if (entity instanceof ClipboardEntity) {
            this.entities.remove(entity);
        }
    }

}
