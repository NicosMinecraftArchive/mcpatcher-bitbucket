package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TileLoader;
import com.prupe.mcpatcher.WeightedIndex;
import net.minecraft.src.Icon;
import net.minecraft.src.ResourceLocation;

import java.util.Properties;

class TileOverrideImpl {
    final static class CTM extends TileOverride {
        // Index into this array is formed from these bit values:
        // 128 64  32
        // 1   *   16
        // 2   4   8
        private static final int[] neighborMap = new int[]{
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
        };

        CTM(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "ctm";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() >= 47) {
                return null;
            } else {
                return "requires at least 47 tiles";
            }
        }

        @Override
        boolean requiresFace() {
            return true;
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            int neighborBits = 0;
            for (int bit = 0; bit < 8; bit++) {
                if (shouldConnect(blockOrientation, origIcon, bit)) {
                    neighborBits |= (1 << bit);
                }
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[0];
        }
    }

    static class Horizontal extends TileOverride {
        // Index into this array is formed from these bit values:
        // 1   *   2
        private static final int[] neighborMap = new int[]{
            3, 2, 0, 1,
        };

        Horizontal(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "horizontal";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 4) {
                return null;
            } else {
                return "requires exactly 4 tiles";
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            int face = blockOrientation.getFaceForHV();
            if (face < 0) {
                return null;
            }
            int neighborBits = 0;
            if (shouldConnect(blockOrientation, origIcon, REL_L)) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_R)) {
                neighborBits |= 2;
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[3];
        }
    }

    final static class HorizontalVertical extends Horizontal {
        // Index into this array is formed from these bit values:
        // 32  16  8
        //     *
        // 1   2   4
        private static final int[] neighborMap = new int[]{
            3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
            4, 4, 5, 4, 4, 4, 4, 4, 3, 3, 6, 3, 3, 3, 3, 3,
            3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
            3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 6, 3, 3, 3, 3, 3,
        };

        HorizontalVertical(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "horizontal+vertical";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 7) {
                return null;
            } else {
                return "requires exactly 7 tiles";
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            Icon icon = super.getTileWorld_Impl(blockOrientation, origIcon);
            if (icon != icons[3]) {
                return icon;
            }
            int neighborBits = 0;
            if (shouldConnect(blockOrientation, origIcon, REL_DL)) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_D)) {
                neighborBits |= 2;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_DR)) {
                neighborBits |= 4;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_UR)) {
                neighborBits |= 8;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_U)) {
                neighborBits |= 16;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_UL)) {
                neighborBits |= 32;
            }
            return icons[neighborMap[neighborBits]];
        }
    }

    static class Vertical extends TileOverride {
        // Index into this array is formed from these bit values:
        // 2
        // *
        // 1
        private static final int[] neighborMap = new int[]{
            3, 2, 0, 1,
        };

        Vertical(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "vertical";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 4) {
                return null;
            } else {
                return "requires exactly 4 tiles";
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            int face = blockOrientation.getFaceForHV();
            if (face < 0) {
                return null;
            }
            int neighborBits = 0;
            if (shouldConnect(blockOrientation, origIcon, REL_D)) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_U)) {
                neighborBits |= 2;
            }
            return icons[neighborMap[neighborBits]];
        }

        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[3];
        }
    }

    final static class VerticalHorizontal extends Vertical {
        // Index into this array is formed from these bit values:
        // 32     16
        // 1   *   8
        // 2       4
        private static final int[] neighborMap = new int[]{
            3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3,
            3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        };

        VerticalHorizontal(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "vertical+horizontal";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 7) {
                return null;
            } else {
                return "requires exactly 7 tiles";
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            Icon icon = super.getTileWorld_Impl(blockOrientation, origIcon);
            if (icon != icons[3]) {
                return icon;
            }
            int neighborBits = 0;
            if (shouldConnect(blockOrientation, origIcon, REL_L)) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_DL)) {
                neighborBits |= 2;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_DR)) {
                neighborBits |= 4;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_R)) {
                neighborBits |= 8;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_UR)) {
                neighborBits |= 16;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_UL)) {
                neighborBits |= 32;
            }
            return icons[neighborMap[neighborBits]];
        }
    }

    final static class Top extends TileOverride {
        Top(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "top";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 1) {
                return null;
            } else {
                return "requires exactly 1 tile";
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            int face = blockOrientation.blockFace;
            if (face < 0) {
                face = NORTH_FACE;
            } else if (face <= TOP_FACE) {
                return null;
            }
            if (shouldConnect(blockOrientation, origIcon, REL_U)) {
                return icons[0];
            }
            return null;
        }

        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return null;
        }
    }

    final static class Random1 extends TileOverride {
        private final int symmetry;
        private final boolean linked;
        private final WeightedIndex chooser;

        Random1(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);

            String sym = properties.getProperty("symmetry", "none");
            if (sym.equals("all")) {
                symmetry = 6;
            } else if (sym.equals("opposite")) {
                symmetry = 2;
            } else {
                symmetry = 1;
            }

            linked = MCPatcherUtils.getBooleanProperty(properties, "linked", false);

            chooser = WeightedIndex.create(getNumberOfTiles(), properties.getProperty("weights", ""));
            if (chooser == null) {
                error("invalid weights");
            }
        }

        @Override
        String getMethod() {
            return "random";
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            int face = blockOrientation.blockFace;
            if (face < 0) {
                face = 0;
            }
            int i = blockOrientation.i;
            int j = blockOrientation.j;
            int k = blockOrientation.k;
            if (linked && blockOrientation.setCoordOffsetsForRenderType()) {
                i += blockOrientation.di;
                j += blockOrientation.dj;
                k += blockOrientation.dk;
            }
            long hash = WeightedIndex.hash128To64(i, j, k, face / symmetry);
            int index = chooser.choose(hash);
            return icons[index];
        }

        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[0];
        }
    }

    final static class Repeat extends TileOverride {
        private final int width;
        private final int height;
        private final int symmetry;

        Repeat(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
            width = MCPatcherUtils.getIntProperty(properties, "width", 0);
            height = MCPatcherUtils.getIntProperty(properties, "height", 0);
            if (width <= 0 || height <= 0) {
                error("invalid width and height (%dx%d)", width, height);
            }

            String sym = properties.getProperty("symmetry", "none");
            if (sym.equals("opposite")) {
                symmetry = ~1;
            } else {
                symmetry = -1;
            }
        }

        @Override
        String getMethod() {
            return "repeat";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == width * height) {
                return null;
            } else {
                return String.format("requires exactly %dx%d tiles", width, height);
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            int face = blockOrientation.blockFace;
            if (face < 0) {
                face = 0;
            }
            face &= symmetry;
            int x;
            int y;
            switch (face) {
                case TOP_FACE:
                case BOTTOM_FACE:
                    if (blockOrientation.rotateTop) {
                        x = blockOrientation.k;
                        y = blockOrientation.i;
                    } else {
                        x = blockOrientation.i;
                        y = blockOrientation.k;
                    }
                    break;

                case NORTH_FACE:
                    x = -blockOrientation.i - 1;
                    y = -blockOrientation.j;
                    break;

                case SOUTH_FACE:
                    x = blockOrientation.i;
                    y = -blockOrientation.j;
                    break;

                case WEST_FACE:
                    x = blockOrientation.k;
                    y = -blockOrientation.j;
                    break;

                case EAST_FACE:
                    x = -blockOrientation.k - 1;
                    y = -blockOrientation.j;
                    break;

                default:
                    return null;
            }
            x %= width;
            if (x < 0) {
                x += width;
            }
            y %= height;
            if (y < 0) {
                y += height;
            }
            return icons[width * y + x];
        }

        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[0];
        }
    }

    final static class Fixed extends TileOverride {
        Fixed(ResourceLocation filePrefix, Properties properties, TileLoader tileLoader) {
            super(filePrefix, properties, tileLoader);
        }

        @Override
        String getMethod() {
            return "fixed";
        }

        @Override
        String checkTileMap() {
            if (getNumberOfTiles() == 1) {
                return null;
            } else {
                return "requires exactly 1 tile";
            }
        }

        @Override
        Icon getTileWorld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[0];
        }


        @Override
        Icon getTileHeld_Impl(BlockOrientation blockOrientation, Icon origIcon) {
            return icons[0];
        }
    }
}
