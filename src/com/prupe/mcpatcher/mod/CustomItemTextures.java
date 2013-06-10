package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomItemTextures extends Mod {
    private static final String GLINT_PNG = MCPatcherUtils.TEXTURE_PACK_BLUR + MCPatcherUtils.TEXTURE_PACK_PREFIX + "misc/glint.png";
    private static final FieldRef itemsList = new FieldRef("Item", "itemsList", "[LItem;");
    private static final MethodRef glDepthFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthFunc", "(I)V");
    private static final MethodRef getEntityItem = new MethodRef("EntityItem", "getEntityItem", "()LItemStack;");
    private static final MethodRef hasEffect = new MethodRef("ItemStack", "hasEffect", "()Z");
    private static final MethodRef getIconFromDamageForRenderPass = new MethodRef("Item", "getIconFromDamageForRenderPass", "(II)LIcon;");
    private static final MethodRef getItem = new MethodRef("ItemStack", "getItem", "()LItem;");
    private static final MethodRef getCITIcon = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getIcon", "(LIcon;LItemStack;I)LIcon;");

    public CustomItemTextures() {
        name = MCPatcherUtils.CUSTOM_ITEM_TEXTURES;
        author = "MCPatcher";
        description = "Enables support for custom item textures, enchantments, and armor.";
        version = "0.4";
        configPanel = new ConfigPanel();

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);
        addDependency(MCPatcherUtils.BASE_TILESHEET_MOD);

        addClassMod(new BaseMod.TessellatorMod(this));
        addClassMod(new BaseMod.NBTTagCompoundMod(this).mapGetTagList());
        addClassMod(new BaseMod.NBTTagListMod(this));
        addClassMod(new BaseMod.IconMod(this));
        addClassMod(new ItemMod());
        addClassMod(new ItemStackMod());
        addClassMod(new EntityItemMod());
        addClassMod(new ItemRendererMod());
        addClassMod(new RenderItemMod());
        addClassMod(new RenderLivingMod());
        addClassMod(new RenderBipedMod());
        addClassMod(new RenderArmorMod());
        addClassMod(new RenderSnowballMod());
        addClassMod(new EntityLivingMod());
        addClassMod(new EntityLivingSubMod());
        addClassMod(new EntityPlayerMod());
        addClassMod(new PotionMod());
        addClassMod(new PotionHelperMod());

        addClassFile(MCPatcherUtils.CIT_UTILS_CLASS);
        addClassFile(MCPatcherUtils.CIT_UTILS_CLASS + "$1");
        addClassFile(MCPatcherUtils.OVERRIDE_BASE_CLASS);
        addClassFile(MCPatcherUtils.ITEM_OVERRIDE_CLASS);
        addClassFile(MCPatcherUtils.ENCHANTMENT_CLASS);
        addClassFile(MCPatcherUtils.ENCHANTMENT_LIST_CLASS);
        addClassFile(MCPatcherUtils.ENCHANTMENT_LIST_CLASS + "$Layer");
        addClassFile(MCPatcherUtils.ENCHANTMENT_LIST_CLASS + "$LayerMethod");
        addClassFile(MCPatcherUtils.ENCHANTMENT_LIST_CLASS + "$Average");
        addClassFile(MCPatcherUtils.ENCHANTMENT_LIST_CLASS + "$Layered");
        addClassFile(MCPatcherUtils.ENCHANTMENT_LIST_CLASS + "$Cycle");
        addClassFile(MCPatcherUtils.ARMOR_OVERRIDE_CLASS);
        addClassFile(MCPatcherUtils.POTION_REPLACER_CLASS);

        BaseTexturePackMod.earlyInitialize(2, MCPatcherUtils.CIT_UTILS_CLASS, "init");
    }

    private class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox itemsCheckBox;
        private JCheckBox enchantmentCheckBox;
        private JCheckBox armorCheckBox;

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            itemsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", itemsCheckBox.isSelected());
                }
            });

            enchantmentCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", enchantmentCheckBox.isSelected());
                }
            });

            armorCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", armorCheckBox.isSelected());
                }
            });
        }

        @Override
        public void save() {
            itemsCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", true));
            enchantmentCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", true));
            armorCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", true));
        }
    }

    private class ItemMod extends BaseMod.ItemMod {
        ItemMod() {
            super(CustomItemTextures.this);

            final MethodRef getIconIndex = new MethodRef(getDeobfClass(), "getIconIndex", "(LItemStack;)LIcon;");

            addMemberMapper(new MethodMapper(getIconIndex));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override item texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ARETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        push(0),
                        reference(INVOKESTATIC, getCITIcon)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(getIconIndex)
            );

            addMemberMapper(new MethodMapper(getIconFromDamageForRenderPass));
        }
    }

    private class ItemStackMod extends ClassMod {
        ItemStackMod() {
            final FieldRef stackSize = new FieldRef(getDeobfClass(), "stackSize", "I");
            final FieldRef itemID = new FieldRef(getDeobfClass(), "itemID", "I");
            final FieldRef itemDamage = new FieldRef(getDeobfClass(), "itemDamage", "I");
            final FieldRef stackTagCompound = new FieldRef(getDeobfClass(), "stackTagCompound", "LNBTTagCompound;");
            final MethodRef getItemDamage = new MethodRef(getDeobfClass(), "getItemDamage", "()I");

            addClassSignature(new ConstSignature("id"));
            addClassSignature(new ConstSignature("Count"));
            addClassSignature(new ConstSignature("Damage"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        ILOAD_2,
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        ILOAD_3,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .setMethod(new MethodRef(getDeobfClass(), "<init>", "(III)V"))
                .addXref(1, itemID)
                .addXref(2, stackSize)
                .addXref(3, itemDamage)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IRETURN,
                        end()
                    );
                }
            }
                .setMethod(getItemDamage)
                .addXref(1, itemDamage)
            );

            addMemberMapper(new FieldMapper(stackTagCompound));
            addMemberMapper(new MethodMapper(getItem));
        }
    }

    private class EntityItemMod extends ClassMod {
        EntityItemMod() {
            addClassSignature(new ConstSignature("Health"));
            addClassSignature(new ConstSignature("Age"));
            addClassSignature(new ConstSignature("Item"));

            addMemberMapper(new MethodMapper(getEntityItem));
        }
    }

    private class GlintSignature extends BytecodeSignature {
        GlintSignature(ClassMod classMod, MethodRef method) {
            super(classMod);
            setMethod(method);
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                push(GLINT_PNG)
            );
        }
    }

    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLiving;LItemStack;I)V");
            final MethodRef renderItemIn2D = new MethodRef(getDeobfClass(), "renderItemIn2D", "(LTessellator;FFFFIIF)V");

            addClassSignature(new GlintSignature(this, renderItem));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() && renderPass == 0)
                        optional(build(
                            ALOAD_2,
                            IFNULL, any(2)
                        )),
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2),
                        ILOAD_3,
                        IFNE, any(2)
                    );
                }
            }
                .setMethod(renderItem)
                .addXref(1, hasEffect)
            );

            addMemberMapper(new MethodMapper(renderItemIn2D).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item enchantment (held)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() && renderPass == 0)
                        optional(build(
                            ALOAD_2,
                            IFNULL, any(2)
                        )),
                        ALOAD_2,
                        reference(INVOKEVIRTUAL, hasEffect),
                        IFEQ, any(2),
                        ILOAD_3,
                        IFNE, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderEnchantmentHeld(itemStack, renderPass)) {
                        ALOAD_2,
                        ILOAD_3,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentHeld", "(LItemStack;I)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItem));
        }
    }

    private class RenderItemMod extends ClassMod {
        RenderItemMod() {
            final FieldRef zLevel = new FieldRef(getDeobfClass(), "zLevel", "F");
            final MethodRef renderDroppedItem = new MethodRef(getDeobfClass(), "renderDroppedItem", "(LEntityItem;LIcon;IFFFF)V");
            final MethodRef renderItemAndEffectIntoGUI = new MethodRef(getDeobfClass(), "renderItemAndEffectIntoGUI", "(LFontRenderer;LTextureManager;LItemStack;II)V");

            addClassSignature(new GlintSignature(this, renderDroppedItem));
            addClassSignature(new GlintSignature(this, renderItemAndEffectIntoGUI));

            addMemberMapper(new FieldMapper(zLevel).accessFlag(AccessFlag.STATIC, false));

            addPatch(new BytecodePatch() {
                private int itemStackRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                anyALOAD,
                                reference(INVOKEVIRTUAL, getEntityItem),
                                capture(anyASTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            itemStackRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "render item enchantment (dropped)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() ...)
                        ALOAD, itemStackRegister,
                        IFNULL, any(2),
                        ALOAD, itemStackRegister,
                        reference(INVOKEVIRTUAL, hasEffect),
                        IFEQ, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderEnchantmentDropped(itemStack)) {
                        ALOAD, itemStackRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentDropped", "(LItemStack;)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderDroppedItem));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item enchantment (gui)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack.hasEffect())
                        ALOAD_3,
                        reference(INVOKEVIRTUAL, hasEffect),
                        IFEQ, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderEnchantmentGUI(itemStack, x, y, this.zLevel)) {
                        ALOAD_3,
                        ILOAD, 4,
                        ILOAD, 5,
                        ALOAD_0,
                        reference(GETFIELD, zLevel),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentGUI", "(LItemStack;IIF)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItemAndEffectIntoGUI));

            addPatch(new ItemStackRenderPassPatch(this, "other"));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "handle items with multiple render passes (gui)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // Item.itemsList[itemID].getIconFromDamageForRenderPass(itemDamage, renderPass)
                        reference(GETSTATIC, itemsList),
                        anyILOAD,
                        AALOAD,
                        anyILOAD,
                        capture(anyILOAD),
                        reference(INVOKEVIRTUAL, getIconFromDamageForRenderPass)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils.getIcon(..., itemStack, renderPass)
                        ALOAD_3,
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getCITIcon)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            final MethodRef doRenderLiving = new MethodRef(getDeobfClass(), "doRenderLiving", "(LEntityLiving;DDDFF)V");

            addClassSignature(new GlintSignature(this, doRenderLiving));
            addClassSignature(new ConstSignature("deadmau5"));

            addPatch(new BytecodePatch() {
                private int passRegister;
                private byte[] renderModelCode;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // flags = this.shouldRenderPass(entityLiving, pass, partialTick);
                                ALOAD_0,
                                ALOAD_1,
                                capture(anyILOAD),
                                anyFLOAD,
                                anyReference(INVOKEVIRTUAL),
                                anyISTORE
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            passRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // this.renderPassModel.render(entityLiving, swing, prevSwing, rotation, yawHead - yawOffset, pitch, oneSixteenth);
                                ALOAD_0,
                                anyReference(GETFIELD),
                                ALOAD_1,
                                anyFLOAD,
                                anyFLOAD,
                                anyFLOAD,
                                anyFLOAD,
                                anyFLOAD,
                                FSUB,
                                anyFLOAD,
                                anyFLOAD,
                                anyReference(INVOKEVIRTUAL)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            renderModelCode = getMatch();
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "render item enchantment (armor)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if ((flags & 15) == 15) {
                        anyILOAD,
                        push(15),
                        IAND,
                        push(15),
                        IF_ICMPNE, any(2),

                        // ...
                        nonGreedy(any(0, 400)),

                        // }
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (CITUtils.setupArmorEnchantments(entityLiving, pass)) {
                        ALOAD_1,
                        ILOAD, passRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "setupArmorEnchantments", "(LEntityLiving;I)Z")),
                        IFEQ, branch("A"),

                        // while (CITUtils.preRenderArmorEnchantment()) {
                        label("B"),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "preRenderArmorEnchantment", "()Z")),
                        IFEQ, branch("C"),

                        // this.renderPassModel.render(...);
                        // CITUtils.postRenderArmorEnchantment();
                        renderModelCode,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "postRenderArmorEnchantment", "()V")),
                        GOTO, branch("B"),

                        // }

                        // } else {
                        label("A"),

                        // ...
                        getMatch(),

                        // }
                        label("C")
                    );
                }
            }.targetMethod(doRenderLiving));
        }
    }

    private class RenderBipedMod extends ClassMod {
        RenderBipedMod() {
            final MethodRef loadTextureForPass = new MethodRef(getDeobfClass(), "loadTextureForPass", "(LEntityLivingSub;IF)V");
            final MethodRef getCurrentArmor = new MethodRef("EntityLivingSub", "getCurrentArmor", "(I)LItemStack;");

            addClassSignature(new ConstSignature(MCPatcherUtils.TEXTURE_PACK_PREFIX + "armor/"));
            addClassSignature(new ConstSignature("_"));
            addClassSignature(new ConstSignature("_b.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        push(3),
                        ILOAD_2,
                        ISUB,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(loadTextureForPass)
                .addXref(1, getCurrentArmor)
            );
        }
    }

    private class RenderArmorMod extends ClassMod {
        RenderArmorMod() {
            setParentClass("RenderLiving");
            setMultipleMatchesAllowed(true);

            final MethodRef getArmorTexture = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getArmorTexture", "(Ljava/lang/String;LEntityLiving;LItemStack;)Ljava/lang/String;");
            final ClassRef sbClass = new ClassRef("java/lang/StringBuilder");
            final MethodRef sbInit0 = new MethodRef("java/lang/StringBuilder", "<init>", "()V");
            final MethodRef sbInit1 = new MethodRef("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
            final MethodRef sbToString = new MethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

            addClassSignature(new ConstSignature(MCPatcherUtils.TEXTURE_PACK_PREFIX + "armor/"));
            addClassSignature(new ConstSignature("_"));
            addClassSignature(new ConstSignature("_b.png"));

            addPatch(new BytecodePatch() {
                private int armorRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                begin(),
                                // itemStack = entityLiving.getCurrentArmor(3 - slot);
                                // - or -
                                // itemStack = entityPlayer.inventory.armorItemInSlot(3 - slot);
                                ALOAD_1,
                                optional(build(anyReference(GETFIELD))),
                                push(3),
                                ILOAD_2,
                                ISUB,
                                anyReference(INVOKEVIRTUAL),
                                capture(anyASTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            armorRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override armor texture";
                }

                @Override
                public boolean filterMethod() {
                    return getMethodInfo().getDescriptor().matches("\\(L[a-z]+;IF\\)[IV]");
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, sbClass),
                        DUP,
                        or(
                            build(
                                // new StringBuilder("armor/")
                                push(MCPatcherUtils.TEXTURE_PACK_PREFIX + "armor/"),
                                reference(INVOKESPECIAL, sbInit1)
                            ),
                            build(
                                // new StringBuilder().append("armor/")
                                reference(INVOKESPECIAL, sbInit0),
                                push(MCPatcherUtils.TEXTURE_PACK_PREFIX + "armor/")
                            )
                        ),
                        nonGreedy(any(0, 100)),
                        reference(INVOKEVIRTUAL, sbToString)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        ALOAD, armorRegister,
                        reference(INVOKESTATIC, getArmorTexture)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private class RenderSnowballMod extends ClassMod {
        RenderSnowballMod() {
            setParentClass("Render");

            final MethodRef doRender = new MethodRef(getDeobfClass(), "doRender", "(LEntity;DDDFF)V");

            addClassSignature(new ConstSignature(MCPatcherUtils.TEXTURE_PACK_PREFIX + "gui/items.png"));
            addClassSignature(new ConstSignature("potion_splash"));
            addClassSignature(new ConstSignature("potion_contents"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        captureReference(CHECKCAST),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(doRender)
                .addXref(1, new ClassRef("EntityPotion"))
                .addXref(2, new MethodRef("EntityPotion", "getPotionDamage", "()I"))
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion entity texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // icon = ...;
                        begin(),
                        capture(build(
                            ALOAD_0,
                            nonGreedy(any(0, 20)))
                        ),
                        capture(anyASTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // icon = CITUtils.getEntityIcon(entity, ..., 0);
                        ALOAD_1,
                        getCaptureGroup(1),
                        push(0),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getEntityIcon", "(LEntity;LIcon;I)LIcon;")),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(doRender));
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            setParentClass("Entity");

            addClassSignature(new ConstSignature(MCPatcherUtils.TEXTURE_PACK_PREFIX + "mob/char.png"));
            addClassSignature(new ConstSignature("Health"));
            addClassSignature(new ConstSignature("HurtTime"));
        }
    }

    private class EntityLivingSubMod extends ClassMod {
        EntityLivingSubMod() {
            setParentClass("EntityLiving");

            addClassSignature(new ConstSignature("explode"));
            addClassSignature(new ConstSignature("CanPickUpLoot"));
            addClassSignature(new ConstSignature("PersistenceRequired"));
            addClassSignature(new ConstSignature("Equipment"));
        }
    }

    private class EntityPlayerMod extends ClassMod {
        EntityPlayerMod() {
            final MethodRef getCurrentArmor = new MethodRef(getDeobfClass(), "getCurrentArmor", "(I)LItemStack;");

            setParentClass("EntityLiving");

            addClassSignature(new ConstSignature(MCPatcherUtils.TEXTURE_PACK_PREFIX + "mob/char.png"));
            addClassSignature(new ConstSignature("random.eat"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ILOAD_1,
                        anyReference(INVOKEVIRTUAL),
                        ARETURN,
                        end()
                    );
                }
            }.setMethod(getCurrentArmor));

            addPatch(new ItemStackRenderPassPatch(this, "player held"));
        }
    }

    private class ItemStackRenderPassPatch extends BytecodePatch {
        private final String desc;

        ItemStackRenderPassPatch(ClassMod classMod, String desc) {
            super(classMod);
            this.desc = desc;
            setInsertAfter(true);
        }

        @Override
        public String getDescription() {
            return "handle items with multiple render passes (" + desc + ")";
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                // itemStack.getItem().getIconFromDamageForRenderPass(itemStack.getItemDamage(), renderPass)
                capture(anyALOAD),
                reference(INVOKEVIRTUAL, getItem),
                backReference(1),
                anyReference(INVOKEVIRTUAL),
                capture(anyILOAD),
                reference(INVOKEVIRTUAL, getIconFromDamageForRenderPass)
            );
        }

        @Override
        public byte[] getReplacementBytes() {
            return buildCode(
                // CITUtils.getIcon(..., itemStack, renderPass)
                getCaptureGroup(1),
                getCaptureGroup(2),
                reference(INVOKESTATIC, getCITIcon)
            );
        }
    }

    private class PotionMod extends ClassMod {
        PotionMod() {
            final FieldRef potionTypes = new FieldRef(getDeobfClass(), "potionTypes", "[LPotion;");
            final MethodRef getName = new MethodRef(getDeobfClass(), "getName", "()Ljava/lang/String;");

            addClassSignature(new ConstSignature("potion.moveSpeed"));
            addClassSignature(new ConstSignature("potion.moveSlowdown"));

            addMemberMapper(new FieldMapper(potionTypes)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );

            addMemberMapper(new MethodMapper(getName)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class PotionHelperMod extends ClassMod {
        PotionHelperMod() {
            final MethodRef getMundaneName = new MethodRef(getDeobfClass(), "getMundaneName", "(I)Ljava/lang/String;");

            addClassSignature(new ConstSignature("potion.prefix.mundane"));
            addClassSignature(new ConstSignature("potion.prefix.uninteresting"));

            addMemberMapper(new MethodMapper(getMundaneName)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }
}
