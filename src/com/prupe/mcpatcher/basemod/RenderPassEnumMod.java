package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static javassist.bytecode.Opcode.*;

public class RenderPassEnumMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef SOLID = new FieldRef("RenderPassEnum", "SOLID", "LRenderPassEnum;");
    public static final FieldRef CUTOUT_MIPPED = new FieldRef("RenderPassEnum", "CUTOUT_MIPPED", "LRenderPassEnum;");
    public static final FieldRef CUTOUT = new FieldRef("RenderPassEnum", "CUTOUT", "LRenderPassEnum;");
    public static final FieldRef TRANSLUCENT = new FieldRef("RenderPassEnum", "TRANSLUCENT", "LRenderPassEnum;");
    public static final FieldRef values = new FieldRef("RenderPassEnum", "values", "[LRenderPassEnum;");

    public static boolean haveRenderPassEnum() {
        return Mod.getMinecraftVersion().compareTo("14w03a") >= 0;
    }

    public static String getDescriptor() {
        return haveRenderPassEnum() ? "LRenderPassEnum;" : "I";
    }

    public static int getLoadOpcode() {
        return haveRenderPassEnum() ? ALOAD : ILOAD;
    }

    public static int getStoreOpcode() {
        return haveRenderPassEnum() ? ASTORE : ISTORE;
    }

    public static String getOrdinalExpr() {
        return haveRenderPassEnum() ? anyReference(INVOKEVIRTUAL) : "";
    }

    public RenderPassEnumMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("SOLID"));
        addClassSignature(new ConstSignature("CUTOUT_MIPPED"));
        addClassSignature(new ConstSignature("CUTOUT"));
        addClassSignature(new ConstSignature("TRANSLUCENT"));

        addMemberMapper(new FieldMapper(SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, true)
        );

        addMemberMapper(new FieldMapper(values)
            .accessFlag(AccessFlag.STATIC, true)
        );
    }
}
