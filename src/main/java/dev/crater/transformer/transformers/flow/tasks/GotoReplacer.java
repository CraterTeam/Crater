package dev.crater.transformer.transformers.flow.tasks;

import dev.crater.transformer.transformers.flow.FlowTask;
import dev.crater.utils.dictionary.Dictionary;
import dev.crater.utils.jar.ClassWrapper;
import org.objectweb.asm.tree.*;

public class GotoReplacer extends FlowTask {
    @Override
    public void execute(ClassWrapper classWrapper) {
        FieldNode predicate = new FieldNode(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, Dictionary.randomString("abc".toCharArray(),20), "Z", null, null);
        classWrapper.getClassNode().fields.add(predicate);
        classWrapper.getMethods().stream().filter(mw -> mw.hasInstructions()).forEach(mw -> {
            InsnList insns = mw.getMethodNode().instructions;

            int varIndex = mw.getMethodNode().maxLocals;
            mw.getMethodNode().maxLocals++; // Prevents breaking of other transformers which rely on this field.

            for (AbstractInsnNode insn : insns.toArray()) {

                if (insn.getOpcode() == GOTO) {
                    insns.insertBefore(insn, new VarInsnNode(ILOAD, varIndex));
                    insns.insertBefore(insn, new JumpInsnNode(IFEQ, ((JumpInsnNode) insn).label));
                    insns.insert(insn, new InsnNode(ATHROW));
                    insns.insert(insn, new InsnNode(ACONST_NULL));
                    insns.remove(insn);
                }
            }
            insns.insert(new VarInsnNode(ISTORE, varIndex));
            insns.insert(new FieldInsnNode(GETSTATIC, classWrapper.getClassInternalName(), predicate.name, "Z"));
        });
    }
}
