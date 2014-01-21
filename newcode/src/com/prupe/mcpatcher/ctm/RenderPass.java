package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class RenderPass {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.BETTER_GLASS);

    private static final ResourceLocation RENDERPASS_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("renderpass.properties");

    private static final Map<Block, Integer> baseRenderPass = new IdentityHashMap<Block, Integer>();
    private static final Map<Block, Integer> extraRenderPass = new IdentityHashMap<Block, Integer>();
    private static final Map<Block, Integer> renderPassBits = new IdentityHashMap<Block, Integer>();
    private static final Set<Block> customRenderPassBlocks = new HashSet<Block>();

    private static BlendMethod blendMethod;
    private static ResourceLocation blendBlankResource;
    private static boolean enableLightmap;
    private static boolean enableColormap;
    private static boolean backfaceCulling;

    private static int currentRenderPass = -1;
    private static int maxRenderPass = 1;
    private static boolean ambientOcclusion;

    public static boolean canRenderInThisPass;

    static {
        RenderPassAPI.instance = new RenderPassAPI() {
            @Override
            public boolean skipDefaultRendering(Block block) {
                return currentRenderPass > MAX_BASE_RENDER_PASS;
            }

            @Override
            public boolean skipThisRenderPass(Block block, int pass) {
                if (pass < 0) {
                    pass = RenderPassMap.instance.getDefaultRenderPass(block);
                }
                return pass != currentRenderPass;
            }

            @Override
            public boolean useColorMultiplierThisPass(Block block) {
                return currentRenderPass != OVERLAY_RENDER_PASS || enableColormap;
            }

            @Override
            public boolean useLightmapThisPass() {
                return currentRenderPass != OVERLAY_RENDER_PASS || enableLightmap;
            }

            @Override
            public void clear() {
                canRenderInThisPass = false;
                maxRenderPass = MAX_BASE_RENDER_PASS - 1;
                baseRenderPass.clear();
                extraRenderPass.clear();
                renderPassBits.clear();
                customRenderPassBlocks.clear();

                for (Block block : BlockAPI.getAllBlocks()) {
                    baseRenderPass.put(block, RenderPassMap.instance.getDefaultRenderPass(block));
                }
            }

            @Override
            public void setRenderPassForBlock(Block block, int pass) {
                if (block == null || pass < 0) {
                    return;
                }
                String name;
                if (pass <= MAX_BASE_RENDER_PASS) {
                    baseRenderPass.put(block, pass);
                    name = "base";
                } else {
                    extraRenderPass.put(block, pass);
                    name = "extra";
                }
                logger.fine("%s %s render pass -> %s",
                    BlockAPI.getBlockName(block), name, RenderPassAPI.instance.getRenderPassName(pass)
                );
                customRenderPassBlocks.add(block);
                maxRenderPass = Math.max(maxRenderPass, pass);
            }

            @Override
            public void finish() {
                RenderPass.finish();
            }

            @Override
            public ResourceLocation getBlankResource(int pass) {
                return pass == OVERLAY_RENDER_PASS ? blendBlankResource : super.getBlankResource(pass);
            }

            @Override
            public ResourceLocation getBlankResource() {
                return getBlankResource(currentRenderPass);
            }
        };

        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.BETTER_GLASS, 4) {
            @Override
            public void beforeChange() {
                blendMethod = BlendMethod.ALPHA;
                enableLightmap = true;
                enableColormap = false;
                backfaceCulling = true;
            }

            @Override
            public void afterChange() {
                Properties properties = TexturePackAPI.getProperties(RENDERPASS_PROPERTIES);
                if (properties != null) {
                    String method = properties.getProperty("blend.3", "alpha").trim().toLowerCase();
                    blendMethod = BlendMethod.parse(method);
                    if (blendMethod == null) {
                        logger.error("%s: unknown blend method '%s'", RENDERPASS_PROPERTIES, method);
                        blendMethod = BlendMethod.ALPHA;
                    }
                    blendBlankResource = blendMethod.getBlankResource();
                    if (blendBlankResource == null) {
                        blendBlankResource = BlendMethod.ALPHA.getBlankResource();
                    }
                    enableLightmap = MCPatcherUtils.getBooleanProperty(properties, "enableLightmap.3", !blendMethod.isColorBased());
                    enableColormap = MCPatcherUtils.getBooleanProperty(properties, "enableColormap.3", false);
                    backfaceCulling = MCPatcherUtils.getBooleanProperty(properties, "backfaceCulling.3", true);
                }
                for (Block block : BlockAPI.getAllBlocks()) {
                    int bits = 0;
                    Integer i = baseRenderPass.get(block);
                    if (i != null && i >= 0) {
                        bits |= (1 << i);
                    }
                    i = extraRenderPass.get(block);
                    if (i != null && i >= 0) {
                        bits |= (1 << i);
                    }
                    renderPassBits.put(block, bits);
                }
            }
        });
    }

    public static void start(int pass) {
        finish();
        currentRenderPass = RenderPassMap.instance.vanillaToMCPatcher(pass);
        CTMUtils.setBlankResource();
    }

    public static void finish() {
        currentRenderPass = -1;
    }

    public static boolean skipAllRenderPasses(boolean[] skipRenderPass) {
        return skipRenderPass[0] && skipRenderPass[1] && skipRenderPass[2] && skipRenderPass[3];
    }

    public static boolean checkRenderPasses(Block block, boolean moreRenderPasses) {
        int bits = renderPassBits.get(block) >>> currentRenderPass;
        canRenderInThisPass = (bits & 1) != 0;
        return moreRenderPasses || (bits >>> 1) != 0;
    }

    public static boolean canRenderInPass(Block block, int pass, boolean renderThis) {
        if (customRenderPassBlocks.contains(block)) {
            checkRenderPasses(block, true);
            return canRenderInThisPass;
        } else {
            return renderThis;
        }
    }

    // pre-14w02a
    public static boolean shouldSideBeRendered(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        if (BlockAPI.shouldSideBeRendered(block, blockAccess, i, j, k, face)) {
            return true;
        } else if (!extraRenderPass.containsKey(block)) {
            Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
            return extraRenderPass.containsKey(neighbor);
        } else {
            return false;
        }
    }

    // 14w02a+
    public static boolean shouldSideBeRendered(Block block, IBlockAccess blockAccess, Position position, Direction direction) {
        if (block.shouldSideBeRendered(blockAccess, position, direction)) {
            return true;
        } else if (!extraRenderPass.containsKey(block)) {
            Block neighbor = blockAccess.getBlock(position);
            return extraRenderPass.containsKey(neighbor);
        } else {
            return false;
        }
    }

    public static boolean setAmbientOcclusion(boolean ambientOcclusion) {
        RenderPass.ambientOcclusion = ambientOcclusion;
        return ambientOcclusion;
    }

    public static float getAOBaseMultiplier(float multiplier) {
        return RenderPassAPI.instance.useLightmapThisPass() ? multiplier : 1.0f;
    }

    public static boolean preRenderPass(int pass) {
        currentRenderPass = pass;
        if (pass > maxRenderPass) {
            return false;
        }
        switch (pass) {
            case RenderPassAPI.BACKFACE_RENDER_PASS:
                GL11.glDisable(GL11.GL_CULL_FACE);
                break;

            case RenderPassAPI.OVERLAY_RENDER_PASS:
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glPolygonOffset(-2.0f, -2.0f);
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                if (backfaceCulling) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                } else {
                    GL11.glDisable(GL11.GL_CULL_FACE);
                }
                if (ambientOcclusion) {
                    GL11.glShadeModel(GL11.GL_SMOOTH);
                }
                blendMethod.applyBlending();
                break;

            default:
                break;
        }
        return true;
    }

    public static int postRenderPass(int value) {
        switch (currentRenderPass) {
            case RenderPassAPI.BACKFACE_RENDER_PASS:
                GL11.glEnable(GL11.GL_CULL_FACE);
                break;

            case RenderPassAPI.OVERLAY_RENDER_PASS:
                GL11.glPolygonOffset(0.0f, 0.0f);
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                if (!backfaceCulling) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                }
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glShadeModel(GL11.GL_FLAT);
                break;

            default:
                break;
        }
        currentRenderPass = -1;
        return value;
    }

    public static void enableDisableLightmap(EntityRenderer renderer, double partialTick) {
        if (RenderPassAPI.instance.useLightmapThisPass()) {
            renderer.enableLightmap(partialTick);
        } else {
            renderer.disableLightmap(partialTick);
        }
    }
}
