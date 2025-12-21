package com.hnp_arda.castlerush;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class SpawnGenerator extends ChunkGenerator {

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        if (chunkX != 0 || chunkZ != 0) return;

        for (int x = 1; x <= 16; x++)
            for (int z = 1; z <= 16; z++)
                chunkData.setBlock(x, 79, z, Material.STONE);


        chunkData.setBlock(8, 79, 8, Material.WHITE_CONCRETE);

        //Time wall    x=14   y=80-83   z=5-11
        for (int z = 5; z <= 11; z++)
            for (int y = 80; y <= 83; y++)
                chunkData.setBlock(14, y, z, Material.OAK_PLANKS);

        //Game wall    x=5-11   y=80-83   z=14
        for (int x = 5; x <= 11; x++)
            for (int y = 80; y <= 83; y++)
                chunkData.setBlock(x, y, 14, Material.OAK_PLANKS);

    }
}
