package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;

import java.util.List;

/**
 * Represents a field or method to be located within a class.  By default,
 * the match is done by type signature, but this can be overridden.
 */
public abstract class MemberMapper {
    /**
     * Deobfuscated members.
     */
    protected final JavaRef[] refs;
    /**
     * Java type descriptor, e.g.,<br>
     * "[B" represents an array of bytes.<br>
     * "(I)Lnet/minecraft/src/Minecraft;" represents a method taking an int and returning a Minecraft object.
     */
    protected String descriptor;

    private int mapSuperclass;
    int mapInterface = -1;
    boolean reverse;

    private int setAccessFlags;
    private int clearAccessFlags;
    private int count;
    protected final ClassMod classMod;
    final String mapSource;

    MemberMapper(ClassMod classMod, JavaRef... refs) {
        this.classMod = classMod;
        this.refs = refs.clone();
        mapSource = ClassMap.getDefaultSource();
        for (JavaRef ref : refs) {
            if (ref != null && ref.getType() != null) {
                return;
            }
        }
        throw new RuntimeException("refs list has no descriptor");
    }

    /**
     * Specify a required access flag.
     *
     * @param flags access flags
     * @param set   if true, flags are required; if false, flags are forbidden
     * @return this
     * @see javassist.bytecode.AccessFlag
     */
    public MemberMapper accessFlag(int flags, boolean set) {
        if (set) {
            setAccessFlags |= flags;
        } else {
            clearAccessFlags |= flags;
        }
        return this;
    }

    public MemberMapper mapToSuperclass(int ancestry) {
        if (ancestry > 1) {
            throw new IllegalArgumentException("ancestry " + ancestry + " is not supported");
        }
        this.mapSuperclass = ancestry;
        return this;
    }

    /**
     * Specify that the methods/fields should be searched in reverse order, starting at the end of the class file.
     *
     * @param reverse if true, reverse the mapping order
     * @return this
     */
    public MemberMapper reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    void mapDescriptor(ClassMap classMap) {
        count = 0;
        for (JavaRef ref : refs) {
            if (ref != null && ref.getType() != null) {
                descriptor = classMap.mapTypeString(ref.getType());
                return;
            }
        }
    }

    boolean matchInfo(String descriptor, int flags) {
        return descriptor.equals(this.descriptor) &&
            (flags & setAccessFlags) == setAccessFlags &&
            (flags & clearAccessFlags) == 0;
    }

    JavaRef getRef() {
        return count < refs.length ? refs[count] : null;
    }

    String getClassName() {
        JavaRef ref = getRef();
        if (ref == null)
            return null;
        else if (ref.getClassName() == null || ref.getClassName().equals("")) {
            return classMod.getDeobfClass();
        } else {
            return ref.getClassName();
        }
    }

    String getName() {
        JavaRef ref = getRef();
        return ref == null ? null : ref.getName();
    }

    void afterMatch() {
        count++;
    }

    boolean allMatched() {
        return count >= refs.length;
    }

    abstract protected String getMapperType();

    abstract protected List getMatchingObjects(ClassFile classFile);

    abstract protected boolean match(Object o);

    abstract protected JavaRef getObfRef(String className, Object o);

    abstract protected String[] describeMatch(Object o);

    protected void updateClassMap(ClassMap classMap, ClassFile classFile, Object o) {
        JavaRef ref = getRef();
        if (ref != null) {
            String obfClassName;
            String prefix;
            if (mapSuperclass == 1) {
                obfClassName = classFile.getSuperclass();
                prefix = getClassName() + '.';
            } else if (mapInterface >= 0) {
                obfClassName = classFile.getInterfaces()[mapInterface];
                prefix = getClassName() + '.';
            } else {
                obfClassName = classFile.getName();
                prefix = "";
            }
            JavaRef obfRef = getObfRef(obfClassName, o);
            String[] s = describeMatch(o);
            Logger.log(Logger.LOG_FIELD, "%s %s matches %s%s %s", getMapperType(), ref.getName(), prefix, s[0], s[1]);
            classMap.addMap(ref, obfRef, mapSource + " " + getMapperType().substring(0, 1).toUpperCase() + getMapperType().substring(1) + "Mapper");
        }
    }
}
