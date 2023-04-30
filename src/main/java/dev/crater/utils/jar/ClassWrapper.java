package dev.crater.utils.jar;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClassWrapper extends IWrapper{
    @Getter
    private final String originEntryName;
    @Getter
    @Setter
    private ClassNode classNode;
    @Getter
    private final byte[] originBytes;
    @Getter
    private String originName;
    @Getter
    private List<String> inherits = new ArrayList<>();
    @Getter
    private final List<ClassWrapper> children = new ArrayList<>();
    @Getter
    private final List<ClassWrapper> parents = new ArrayList<>();
    @Getter
    private List<MethodWrapper> methods = null;
    @Getter
    private List<FieldWrapper> fields = null;
    @Getter
    private boolean library = false;
    public ClassWrapper(String originEntryName, byte[] bytes) {
        this.originEntryName = originEntryName;
        originBytes = bytes;
        ClassReader cr = new ClassReader(bytes);
        classNode = new ClassNode();
        cr.accept(classNode,0);
        originName = classNode.name;
        methods = MethodWrapper.wrap(this);
        fields = FieldWrapper.wrap(this);
    }
    public ClassWrapper(String originEntryName, byte[] bytes,boolean library){
        this(originEntryName,bytes);
        this.library = library;
    }
    public String getClassName(){
        return classNode.name.replace("/",".");
    }
    public String getClassInternalName(){
        return classNode.name;
    }
    public String getSuperName() {
        return classNode.superName;
    }
    public List<String> getInterfaces(){
        return classNode.interfaces;
    }
    public byte[] getClassBytes(boolean verify){
        ClassWriter cw = new ClassWriter(verify ? ClassWriter.COMPUTE_FRAMES : 0);
        classNode.accept(cw);
        return cw.toByteArray();
    }
    public boolean equals(Object o){
        if (o instanceof ClassWrapper){
            ClassWrapper cw = (ClassWrapper) o;
            return cw.getClassName().equals(getClassName());
        }
        return super.equals(o);
    }
    public Stream<MethodWrapper> methodStream() {
        return getMethods().stream();
    }
    public Stream<FieldWrapper> fieldStream() {
        return getFields().stream();
    }
    public boolean isEnum(){
        return (classNode.access & Opcodes.ACC_ENUM) != 0;
    }
}
