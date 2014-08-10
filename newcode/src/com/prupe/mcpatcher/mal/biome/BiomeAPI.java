package com.prupe.mcpatcher.mal.biome;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Minecraft;
import net.minecraft.src.Position;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Properties;

abstract public class BiomeAPI {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);
    private static final BiomeAPI instance = MAL.newInstance(BiomeAPI.class, "biome");

    public static final int WORLD_MAX_HEIGHT = 255;
    public static final boolean isColorHeightDependent = instance.isColorHeightDependent();

    private static boolean biomesLogged;

    private static Method getWaterColorMultiplier;
    private static BiomeGenBase lastBiome;
    private static int lastI;
    private static int lastK;

    static {
        try {
            getWaterColorMultiplier = BiomeGenBase.class.getDeclaredMethod("getWaterColorMultiplier");
            getWaterColorMultiplier.setAccessible(true);
            logger.config("forge getWaterColorMultiplier detected");
        } catch (NoSuchMethodException e) {
        }
    }

    public static void parseBiomeList(String list, BitSet bits) {
        logBiomes();
        if (MCPatcherUtils.isNullOrEmpty(list)) {
            return;
        }
        for (String s : list.split(list.contains(",") ? "\\s*,\\s*" : "\\s+")) {
            BiomeGenBase biome = findBiomeByName(s);
            if (biome != null) {
                bits.set(biome.biomeID);
            }
        }
    }

    public static BitSet getHeightListProperty(PropertiesFile properties, String suffix) {
        int minHeight = Math.max(properties.getInt("minHeight" + suffix, 0), 0);
        int maxHeight = Math.min(properties.getInt("maxHeight" + suffix, WORLD_MAX_HEIGHT), WORLD_MAX_HEIGHT);
        String heightStr = properties.getString("heights" + suffix, "");
        if (minHeight == 0 && maxHeight == WORLD_MAX_HEIGHT && heightStr.length() == 0) {
            return null;
        } else {
            BitSet heightBits = new BitSet(WORLD_MAX_HEIGHT + 1);
            if (heightStr.length() == 0) {
                heightStr = String.valueOf(minHeight) + "-" + String.valueOf(maxHeight);
            }
            for (int i : MCPatcherUtils.parseIntegerList(heightStr, 0, WORLD_MAX_HEIGHT)) {
                heightBits.set(i);
            }
            return heightBits;
        }
    }

    public static BiomeGenBase findBiomeByName(String name) {
        logBiomes();
        if (name == null) {
            return null;
        }
        name = name.replace(" ", "");
        if (name.isEmpty()) {
            return null;
        }
        for (BiomeGenBase biome : BiomeGenBase.biomeList) {
            if (biome == null || biome.biomeName == null) {
                continue;
            }
            if (name.equalsIgnoreCase(biome.biomeName) || name.equalsIgnoreCase(biome.biomeName.replace(" ", ""))) {
                if (biome.biomeID >= 0 && biome.biomeID < BiomeGenBase.biomeList.length) {
                    return biome;
                }
            }
        }
        return null;
    }

    public static IBlockAccess getWorld() {
        return Minecraft.getInstance().theWorld;
    }

    public static int getBiomeIDAt(IBlockAccess blockAccess, int i, int j, int k) {
        BiomeGenBase biome = getBiomeGenAt(blockAccess, i, j, k);
        return biome == null ? BiomeGenBase.biomeList.length : biome.biomeID;
    }

    public static BiomeGenBase getBiomeGenAt(IBlockAccess blockAccess, int i, int j, int k) {
        if (lastBiome == null || i != lastI || k != lastK) {
            lastI = i;
            lastK = k;
            lastBiome = instance.getBiomeGenAt_Impl(blockAccess, i, j, k);
        }
        return lastBiome;
    }

    public static float getTemperature(BiomeGenBase biome, int i, int j, int k) {
        return instance.getTemperaturef_Impl(biome, i, j, k);
    }

    public static float getTemperature(IBlockAccess blockAccess, int i, int j, int k) {
        return getTemperature(getBiomeGenAt(blockAccess, i, j, k), i, j, k);
    }

    public static float getRainfall(BiomeGenBase biome, int i, int j, int k) {
        return biome.getRainfallf();
    }

    public static float getRainfall(IBlockAccess blockAccess, int i, int j, int k) {
        return getRainfall(getBiomeGenAt(blockAccess, i, j, k), i, j, k);
    }

    public static int getGrassColor(BiomeGenBase biome, int i, int j, int k) {
        return instance.getGrassColor_Impl(biome, i, j, k);
    }

    public static int getFoliageColor(BiomeGenBase biome, int i, int j, int k) {
        return instance.getFoliageColor_Impl(biome, i, j, k);
    }

    public static int getWaterColorMultiplier(BiomeGenBase biome) {
        if (getWaterColorMultiplier != null) {
            try {
                return (Integer) getWaterColorMultiplier.invoke(biome);
            } catch (Throwable e) {
                e.printStackTrace();
                getWaterColorMultiplier = null;
            }
        }
        return biome == null ? 0xffffff : biome.waterColorMultiplier;
    }

    private static void logBiomes() {
        if (!biomesLogged) {
            biomesLogged = true;
            for (int i = 0; i < BiomeGenBase.biomeList.length; i++) {
                BiomeGenBase biome = BiomeGenBase.biomeList[i];
                if (biome != null) {
                    int x = (int) (255.0f * (1.0f - biome.temperature));
                    int y = (int) (255.0f * (1.0f - biome.temperature * biome.rainfall));
                    logger.config("setupBiome #%d id=%d \"%s\" %06x (%d,%d)", i, biome.biomeID, biome.biomeName, biome.waterColorMultiplier, x, y);
                }
            }
        }
    }

    abstract protected BiomeGenBase getBiomeGenAt_Impl(IBlockAccess blockAccess, int i, int j, int k);

    abstract protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k);

    abstract protected int getGrassColor_Impl(BiomeGenBase biome, int i, int j, int k);

    abstract protected int getFoliageColor_Impl(BiomeGenBase biome, int i, int j, int k);

    abstract protected boolean isColorHeightDependent();

    BiomeAPI() {
    }

    final private static class V1 extends BiomeAPI {
        @Override
        protected BiomeGenBase getBiomeGenAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBiomeGenAt(i, k);
        }

        @Override
        protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getTemperaturef();
        }

        @Override
        protected int getGrassColor_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getGrassColor();
        }

        @Override
        protected int getFoliageColor_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getFoliageColor();
        }

        @Override
        protected boolean isColorHeightDependent() {
            return false;
        }
    }

    private static class V2 extends BiomeAPI {
        @Override
        protected BiomeGenBase getBiomeGenAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBiomeGenAt(i, k);
        }

        @Override
        protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getTemperaturef(i, j, k);
        }

        @Override
        protected int getGrassColor_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getGrassColor(i, j, k);
        }

        @Override
        protected int getFoliageColor_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getFoliageColor(i, j, k);
        }

        @Override
        protected boolean isColorHeightDependent() {
            return true;
        }
    }

    private static class V3 extends BiomeAPI {
        @Override
        protected BiomeGenBase getBiomeGenAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBiomeGenAt(new Position(i, j, k));
        }

        @Override
        protected float getTemperaturef_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getTemperaturef(new Position(i, j, k));
        }

        @Override
        protected int getGrassColor_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getGrassColor(new Position(i, j, k));
        }

        @Override
        protected int getFoliageColor_Impl(BiomeGenBase biome, int i, int j, int k) {
            return biome.getFoliageColor(new Position(i, j, k));
        }

        @Override
        protected boolean isColorHeightDependent() {
            return true;
        }
    }
}
