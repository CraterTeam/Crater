package dev.crater.utils.jar;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class FieldWrapper extends IWrapper{
    @Getter
    private ClassWrapper classWrapper;
    @Getter
    @Setter
    private FieldNode fieldNode;
    @Getter
    private String originName;
    @Getter
    private String originType;
    public FieldWrapper(ClassWrapper cw, FieldNode fn){
        this.classWrapper = cw;
        this.fieldNode = fn;
        this.originName = fn.name;
        this.originType = fn.desc;
    }
    public boolean isStatic(){
        return (fieldNode.access & Opcodes.ACC_STATIC) != 0;
    }
    public static List<FieldWrapper> wrap(ClassWrapper cw){
        List<FieldWrapper> fieldWrappers = new ArrayList<>();
        for (FieldNode field : cw.getClassNode().fields) {
            fieldWrappers.add(new FieldWrapper(cw,field));
        }
        return fieldWrappers;
    }
    public boolean isSynthetic(){
        return (fieldNode.access & Opcodes.ACC_SYNTHETIC) != 0;
    }
    public int getAccess(){
        return fieldNode.access;
    }
    public void setAccess(int access){
        fieldNode.access = access;
    }
}
