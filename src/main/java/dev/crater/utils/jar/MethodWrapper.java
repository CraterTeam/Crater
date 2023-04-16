package dev.crater.utils.jar;

import lombok.Getter;
import org.objectweb.asm.tree.MethodNode;

public class MethodWrapper extends IWrapper{
    @Getter
    private ClassWrapper classWrapper;
    @Getter
    private MethodNode methodNode;
    public MethodWrapper(ClassWrapper cw, MethodNode mn){
        this.classWrapper = cw;
        this.methodNode = mn;
    }
}
