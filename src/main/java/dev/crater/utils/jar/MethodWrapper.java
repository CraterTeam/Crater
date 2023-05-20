package dev.crater.utils.jar;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class MethodWrapper extends IWrapper{
    @Getter
    private ClassWrapper classWrapper;
    @Getter
    @Setter
    private MethodNode methodNode;
    @Getter
    private String originName;
    @Getter
    private String originDescriptor;
    public MethodWrapper(ClassWrapper cw, MethodNode mn){
        this.classWrapper = cw;
        this.methodNode = mn;
        this.originName = mn.name;
        this.originDescriptor = mn.desc;
    }
    public boolean isStatic(){
        return (methodNode.access & Opcodes.ACC_STATIC) != 0;
    }
    public boolean isNative(){
        return (methodNode.access & Opcodes.ACC_NATIVE) != 0;
    }
    public boolean isSynthetic(){
        return (methodNode.access & Opcodes.ACC_SYNTHETIC) != 0;
    }
    public boolean isBridge(){
        return (methodNode.access & Opcodes.ACC_BRIDGE) != 0;
    }
    public static List<MethodWrapper> wrap(ClassWrapper cw){
        List<MethodWrapper> methodWrappers = new ArrayList<>();
        for (MethodNode method : cw.getClassNode().methods) {
            methodWrappers.add(new MethodWrapper(cw,method));
        }
        return methodWrappers;
    }
    public int getAccess(){
        return methodNode.access;
    }
    public boolean hasInstructions() {
        return methodNode.instructions != null && methodNode.instructions.size() > 0;
    }
    public void setAccess(int access){
        methodNode.access = access;
    }
}
