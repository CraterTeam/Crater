package dev.crater.utils.jar;

import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ClassWrapper {
    @Getter
    private String originEntryName;
    @Getter
    private ClassNode classNode;
    @Getter
    private String originName;
    public ClassWrapper(String originEntryName, byte[] bytes) {
        this.originEntryName = originEntryName;
        ClassReader cr = new ClassReader(bytes);
        classNode = new ClassNode();
        cr.accept(classNode,0);
        originName = classNode.name;
    }
    public String getClassName(){
        return classNode.name.replace("/",".");
    }
}
