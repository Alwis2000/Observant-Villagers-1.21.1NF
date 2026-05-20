package com.stereowalker.obville.world;

import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.stereowalker.obville.dat.VillageData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.AABB;

public class PlacedBlocks extends SavedData
{
    public static final String KEY =  "obville_placed_blocks";

    public HashMap<BlockPos, Integer> blockChanges = new HashMap<>();

    public HashMap<Integer, VillageData> villages = new HashMap<>();
    private Gson gson;

    private PlacedBlocks()
    {
        super(/*KEY*/);
        gson = new Gson();
        villages.put(0, new VillageData());
    }
    
    public void brokeGeneratedBlock(BlockPos pos) {
    	blockChanges.put(pos, 2);
		setDirty();
    }
    
    public void playerPlacedBlock(BlockPos pos) {
    	blockChanges.put(pos, 1);
		setDirty();
    }
    
    public boolean didPlayerPlaceBlock(BlockPos pos) {
    	return blockChanges.getOrDefault(pos, 2) == 1;
    }
    
    public int isVillageOrRegisterVillage(ServerLevel level, BlockPos pos) {
    	int last = 0;
    	for (Integer village : villages.keySet()) {
    		if (village == 0) continue;
    		if (village >= last) last = village + 1;
    		AABB bounds = new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(villages.get(village).bounds.getLeft()), net.minecraft.world.phys.Vec3.atLowerCornerOf(villages.get(village).bounds.getRight()));
    		if (bounds.contains(pos.getX(), pos.getY(), pos.getZ())) {
    			return village;
    		} else if (bounds.inflate(10).contains(pos.getX(), pos.getY(), pos.getZ())) {
    			// Player is near this village, expand its bounds to include the new position
    			AABB newBounds = bounds.minmax(new AABB(pos));
    			VillageData dat = villages.get(village);
    			dat.bounds = Pair.of(BlockPos.containing(newBounds.minX, newBounds.minY, newBounds.minZ), 
    	    		BlockPos.containing(newBounds.maxX, newBounds.maxY, newBounds.maxZ));
    			setDirty();
    			return village;
    		}
    	}
    	if (last == 0) last = 1;
    	
    	BlockPos yPlus = pos.above();
    	while(level.isCloseToVillage(yPlus, 2)) yPlus = yPlus.above();
    	
    	BlockPos yMinus = pos.below();
    	while(level.isCloseToVillage(yMinus, 2)) yMinus = yMinus.below();
    	
    	BlockPos zPlus = pos.south();
    	while(level.isCloseToVillage(zPlus, 2)) zPlus = zPlus.south();
    	
    	BlockPos zMinus = pos.north();
    	while(level.isCloseToVillage(zMinus, 2)) zMinus = zMinus.north();
    	
    	BlockPos xPlus = pos.east();
    	while(level.isCloseToVillage(xPlus, 2)) xPlus = xPlus.east();
    	
    	BlockPos xMinus = pos.west();
    	while(level.isCloseToVillage(xMinus, 2)) xMinus = xMinus.west();
    	
    	VillageData dat = new VillageData();
		dat.bounds = Pair.of(new BlockPos(xPlus.getX(), yPlus.getY(), zPlus.getZ()), 
    			new BlockPos(xMinus.getX(), yMinus.getY(), zMinus.getZ()));
		villages.put(last, dat);
		setDirty();
    	return last;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries)
    {
    	CompoundTag blockChangesTag = new CompoundTag();
        for (BlockPos entry : blockChanges.keySet())
        {
            int age = blockChanges.get(entry);
            blockChangesTag.putInt(gson.toJson(entry), age);
        }
        tag.put("BlockChanges", blockChangesTag);

    	CompoundTag tag2 = new CompoundTag();
        for (Integer entry : villages.keySet())
        {
            tag2.put(entry.toString(), villages.get(entry).write(registries));
        }
        tag.put("Villages", tag2);
        
        return tag;
    }

    public static PlacedBlocks read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries)
    {
    	PlacedBlocks map = new PlacedBlocks();
        map.blockChanges.clear();

        CompoundTag blockChangesTag = tag.getCompound("BlockChanges");
        for (String entry : blockChangesTag.getAllKeys())
        {
            int age = blockChangesTag.getInt(entry);
            BlockPos pos = map.gson.fromJson(entry, BlockPos.class);

            map.blockChanges.put(pos, age);
        }
        
        CompoundTag tag2 = tag.getCompound("Villages");
        for (String entry : tag2.getAllKeys())
        {
            map.villages.put(Integer.parseInt(entry), VillageData.read(tag2.getCompound(entry), registries));
        }
        return map;

    }

    public static PlacedBlocks getInstance(ServerLevel level)
    {
    	DimensionDataStorage manager = Objects.requireNonNull(level).getDataStorage();
        return manager.computeIfAbsent(new SavedData.Factory<>(PlacedBlocks::new, PlacedBlocks::read, null), KEY);
    }

    public static PlacedBlocks getInstance(MinecraftServer server, ResourceKey<Level> dimension)
    {
    	DimensionDataStorage manager = Objects.requireNonNull(server.getLevel(dimension)).getDataStorage();
        return manager.computeIfAbsent(new SavedData.Factory<>(PlacedBlocks::new, PlacedBlocks::read, null), KEY);
    }
}
