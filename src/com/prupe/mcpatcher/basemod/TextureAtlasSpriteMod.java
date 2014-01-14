package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.build;
import static com.prupe.mcpatcher.BinaryRegex.repeat;
import static com.prupe.mcpatcher.BytecodeMatcher.anyFSTORE;
import static com.prupe.mcpatcher.BytecodeMatcher.anyILOAD;
import static javassist.bytecode.Opcode.*;

/**
 * Maps TextureAtlasSprite class.
 */
public class TextureAtlasSpriteMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef textureName = new FieldRef(getDeobfClass(), "textureName", "Ljava/lang/String;");

    public TextureAtlasSpriteMod(Mod mod) {
        super(mod);
        setInterfaces("Icon");

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(repeat(build(
                    push(0.009999999776482582),
                    anyILOAD,
                    I2D,
                    DDIV,
                    D2F,
                    anyFSTORE
                ), 2));
            }
        });

        addMemberMapper(new FieldMapper(textureName));
    }
}
