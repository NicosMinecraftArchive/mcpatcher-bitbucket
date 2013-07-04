package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ItemStack;

import java.util.*;

final class EnchantmentList {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static final float PI = (float) Math.PI;

    private static LayerMethod applyMethod;
    private static int limit;
    private static float fade;

    private final List<Layer> layers = new ArrayList<Layer>();

    static void setProperties(Properties properties) {
        applyMethod = new Average();
        limit = 99;
        fade = 0.5f;
        if (properties != null) {
            String value = MCPatcherUtils.getStringProperty(properties, "method", "average").toLowerCase();
            if (value.equals("layered")) {
                applyMethod = new Layered();
            } else if (value.equals("cycle")) {
                applyMethod = new Cycle();
            } else if (!value.equals("average")) {
                logger.warning("%s: unknown enchantment layering method '%s'", CITUtils.CIT_PROPERTIES, value);
            }
            limit = Math.max(MCPatcherUtils.getIntProperty(properties, "cap", limit), 0);
            fade = Math.max(MCPatcherUtils.getFloatProperty(properties, "fade", fade), 0.0f);
        }
    }

    EnchantmentList(Enchantment[][] enchantments, ItemStack itemStack) {
        BitSet layersPresent = new BitSet();
        Map<Integer, Layer> tmpLayers = new HashMap<Integer, Layer>();
        int itemID = itemStack.itemID;
        int[] enchantmentLevels = CITUtils.getEnchantmentLevels(itemID, itemStack.stackTagCompound);
        boolean hasEffect = itemStack.hasEffect();
        if (itemID >= 0 && itemID < enchantments.length && enchantments[itemID] != null) {
            for (Enchantment enchantment : enchantments[itemID]) {
                if (enchantment.match(itemStack, enchantmentLevels, hasEffect)) {
                    int level = Math.max(enchantment.lastEnchantmentLevel, 1);
                    int layer = enchantment.layer;
                    if (!layersPresent.get(layer)) {
                        Layer newLayer = new Layer(enchantment, level);
                        tmpLayers.put(layer, newLayer);
                        layersPresent.set(layer);
                    }
                }
            }
        }
        if (layersPresent.isEmpty()) {
            return;
        }
        while (layersPresent.cardinality() > limit) {
            int layer = layersPresent.nextSetBit(0);
            layersPresent.clear(layer);
            tmpLayers.remove(layer);
        }
        for (int i = layersPresent.nextSetBit(0); i >= 0; i = layersPresent.nextSetBit(i + 1)) {
            layers.add(tmpLayers.get(i));
        }
        applyMethod.computeIntensities(this);
    }

    boolean isEmpty() {
        return layers.isEmpty();
    }

    int size() {
        return layers.size();
    }

    Enchantment getEnchantment(int index) {
        return layers.get(index).enchantment;
    }

    float getIntensity(int index) {
        return layers.get(index).intensity;
    }

    private static final class Layer {
        final Enchantment enchantment;
        final int level;
        float intensity;

        Layer(Enchantment enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }

        float getEffectiveDuration() {
            return enchantment.duration + 2.0f * fade;
        }
    }

    abstract private static class LayerMethod {
        abstract void computeIntensities(EnchantmentList enchantments);

        protected void scaleIntensities(EnchantmentList enchantments, int denominator) {
            if (denominator > 0) {
                for (Layer layer : enchantments.layers) {
                    if (layer.enchantment.blendMethod.canFade()) {
                        layer.intensity = (float) layer.level / (float) denominator;
                    } else {
                        layer.intensity = layer.level > 0 ? 1.0f : 0.0f;
                    }
                }
            } else {
                for (Layer layer : enchantments.layers) {
                    layer.intensity = layer.level > 0 ? 1.0f : 0.0f;
                }
            }
        }
    }

    private static final class Average extends LayerMethod {
        @Override
        void computeIntensities(EnchantmentList enchantments) {
            int total = 0;
            for (Layer layer : enchantments.layers) {
                if (layer.enchantment.blendMethod.canFade()) {
                    total += layer.level;
                }
            }
            scaleIntensities(enchantments, total);
        }
    }

    private static final class Layered extends LayerMethod {
        @Override
        void computeIntensities(EnchantmentList enchantments) {
            int max = 0;
            for (Layer layer : enchantments.layers) {
                if (layer.enchantment.blendMethod.canFade()) {
                    Math.max(max, layer.level);
                }
            }
            scaleIntensities(enchantments, max);
        }
    }

    private static final class Cycle extends LayerMethod {
        @Override
        void computeIntensities(EnchantmentList enchantments) {
            float total = 0.0f;
            for (Layer layer : enchantments.layers) {
                if (layer.enchantment.blendMethod.canFade()) {
                    total += layer.getEffectiveDuration();
                }
            }
            float timestamp = (float) ((System.currentTimeMillis() / 1000.0) % total);
            for (Layer layer : enchantments.layers) {
                if (!layer.enchantment.blendMethod.canFade()) {
                    continue;
                }
                if (timestamp <= 0.0f) {
                    break;
                }
                float duration = layer.getEffectiveDuration();
                if (timestamp < duration) {
                    float denominator = (float) Math.sin(PI * fade / duration);
                    layer.intensity = (float) (Math.sin(PI * timestamp / duration) / (denominator == 0.0f ? 1.0f : denominator));
                }
                timestamp -= duration;
            }
        }
    }
}
