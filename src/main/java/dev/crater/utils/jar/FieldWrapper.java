package dev.crater.utils.jar;

import lombok.Getter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class FieldWrapper extends IWrapper{
    @Getter
    private ClassWrapper classWrapper;
    @Getter
    private FieldNode fieldNode;
    public FieldWrapper(ClassWrapper cw, FieldNode fn){
        this.classWrapper = cw;
        this.fieldNode = fn;
    }
}
