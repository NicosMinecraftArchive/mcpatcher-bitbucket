package com.prupe.mcpatcher.cc;

import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ResourceLocation;

import java.util.Collection;

interface IColorMap {
    boolean isHeightDependent();

    int getColorMultiplier();

    int getColorMultiplier(IBlockAccess blockAccess, int i, int j, int k);

    float[] getColorMultiplierF(IBlockAccess blockAccess, int i, int j, int k);

    void claimResources(Collection<ResourceLocation> resources);

    IColorMap copy();
}
