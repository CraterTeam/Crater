package dev.crater.utils.jar;

import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class ClassWrapper {
    @Getter
    private final String originEntryName;
    @Getter
    private ClassNode classNode;
    @Getter
    private final byte[] originBytes;
    @Getter
    private String originName;
    public ClassWrapper(String originEntryName, byte[] bytes) {
        this.originEntryName = originEntryName;
        originBytes = bytes;
        ClassReader cr = new ClassReader(bytes);
        classNode = new ClassNode();
        cr.accept(classNode,0);
        originName = classNode.name;
    }
    public String getClassName(){
        return classNode.name.replace("/",".");
    }
    public String getClassInternalName(){
        return classNode.name;
    }
    public byte[] getClassBytes(boolean verify){
        ClassWriter cw = new ClassWriter(verify ? ClassWriter.COMPUTE_FRAMES : 0);
        classNode.accept(cw);
        return cw.toByteArray();
    }
}
