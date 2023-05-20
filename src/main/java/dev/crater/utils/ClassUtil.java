package dev.crater.utils;

import dev.crater.Crater;
import dev.crater.Main;
import dev.crater.utils.jar.ClassWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ClassUtil {
    public static String byInternalName(String internalName){
        return internalName.replace("/",".");
    }
    @Deprecated
    public static boolean hasClass(String className){
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    public static boolean hasClass(String className,ClassLoader classLoader){
        try {
            Class.forName(className,true,classLoader);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static boolean hasAnnotations(ClassNode classNode) {
        return (classNode.visibleAnnotations != null && !classNode.visibleAnnotations.isEmpty())
                || (classNode.invisibleAnnotations != null && !classNode.invisibleAnnotations.isEmpty());
    }

    public static boolean hasAnnotations(MethodNode methodNode) {
        return (methodNode.visibleAnnotations != null && !methodNode.visibleAnnotations.isEmpty())
                || (methodNode.invisibleAnnotations != null && !methodNode.invisibleAnnotations.isEmpty());
    }

    public static boolean hasAnnotations(FieldNode fieldNode) {
        return (fieldNode.visibleAnnotations != null && !fieldNode.visibleAnnotations.isEmpty())
                || (fieldNode.invisibleAnnotations != null && !fieldNode.invisibleAnnotations.isEmpty());
    }
    public static boolean isIntInsn(AbstractInsnNode insn) {
        if (insn == null) {
            return false;
        }
        int opcode = insn.getOpcode();
        return ((opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5)
                || opcode == Opcodes.BIPUSH
                || opcode == Opcodes.SIPUSH
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer));
    }

    public static boolean isLongInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode == Opcodes.LCONST_0
                || opcode == Opcodes.LCONST_1
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long));
    }

    public static boolean isFloatInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2)
                || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Float);
    }

    public static boolean isDoubleInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.DCONST_0 && opcode <= Opcodes.DCONST_1)
                || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Double);
    }

    public static AbstractInsnNode getNumberInsn(int number) {
        if (number >= -1 && number <= 5)
            return new InsnNode(number + 3);
        else if (number >= -128 && number <= 127)
            return new IntInsnNode(Opcodes.BIPUSH, number);
        else if (number >= -32768 && number <= 32767)
            return new IntInsnNode(Opcodes.SIPUSH, number);
        else
            return new LdcInsnNode(number);
    }

    public static AbstractInsnNode getNumberInsn(long number) {
        if (number == 0 || number == 1)
            return new InsnNode((int) (number + 9));
        else
            return new LdcInsnNode(number);
    }

    public static AbstractInsnNode getNumberInsn(float number) {
        if (number >= 0 && number <= 2) {
            return new InsnNode((int) (number + 11));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(double number) {
        if (number >= 0 && number <= 1)
            return new InsnNode((int) (number + 14));
        else
            return new LdcInsnNode(number);
    }
    public static int getIntegerFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            return opcode - 3;
        } else if (insn instanceof IntInsnNode
                && insn.getOpcode() != Opcodes.NEWARRAY) {
            return ((IntInsnNode) insn).operand;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer) {
            return (Integer) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static long getLongFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.LCONST_0 && opcode <= Opcodes.LCONST_1) {
            return opcode - 9;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long) {
            return (Long) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static float getFloatFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2) {
            return opcode - 11;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Float) {
            return (Float) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static double getDoubleFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.DCONST_0 && opcode <= Opcodes.DCONST_1) {
            return opcode - 14;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Double) {
            return (Double) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

}
