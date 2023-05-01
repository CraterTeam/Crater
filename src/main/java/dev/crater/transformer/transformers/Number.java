package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.transformer.Transformer;
import dev.crater.utils.ClassUtil;
import dev.crater.utils.RandomUtils;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.MethodWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.List;

public class Number extends Transformer {
    private static final Logger logger = LogManager.getLogger("Number");
    public Number() {
        super("Number");
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        logger.info("Preprocessing classes");
        for (ClassWrapper classWrapper : cw) {
            for (MethodWrapper method : classWrapper.getMethods()) {
                //Remove const values
                InsnList newInsn = new InsnList();
                for (AbstractInsnNode instruction : method.getMethodNode().instructions) {
                    switch (instruction.getOpcode()){
                        case ICONST_M1:
                            newInsn.add(new LdcInsnNode(-1));
                            break;
                        case ICONST_0:
                            newInsn.add(new LdcInsnNode(0));
                            break;
                        case ICONST_1:
                            newInsn.add(new LdcInsnNode(1));
                            break;
                        case ICONST_2:
                            newInsn.add(new LdcInsnNode(2));
                            break;
                        case ICONST_3:
                            newInsn.add(new LdcInsnNode(3));
                            break;
                        case ICONST_4:
                            newInsn.add(new LdcInsnNode(4));
                            break;
                        case ICONST_5:
                            newInsn.add(new LdcInsnNode(5));
                            break;
                        case LCONST_0:
                            newInsn.add(new LdcInsnNode(0L));
                            break;
                        case LCONST_1:
                            newInsn.add(new LdcInsnNode(1L));
                            break;
                        case FCONST_0:
                            newInsn.add(new LdcInsnNode(0F));
                            break;
                        case FCONST_1:
                            newInsn.add(new LdcInsnNode(1F));
                            break;
                        case FCONST_2:
                            newInsn.add(new LdcInsnNode(2F));
                            break;
                        case DCONST_0:
                            newInsn.add(new LdcInsnNode(0D));
                            break;
                        case DCONST_1:
                            newInsn.add(new LdcInsnNode(1D));
                            break;
                        default:
                            newInsn.add(instruction);
                            break;
                    }
                }
                method.getMethodNode().instructions = newInsn;
            }
        }
        logger.info("processing classes");
        for (ClassWrapper classWrapper : cw) {
            for (MethodWrapper method : classWrapper.getMethods()) {
                //Remove const values
                InsnList newInsn = new InsnList();
                for (AbstractInsnNode instruction : method.getMethodNode().instructions) {
                    if (ClassUtil.isIntInsn(instruction)) {
                        InsnList insns = obfuscateNumber(ClassUtil.getIntegerFromInsn(instruction));

                        newInsn.add(insns);
                        continue;
                    }
                    if (ClassUtil.isLongInsn(instruction)) {
                        InsnList insns = obfuscateNumber(ClassUtil.getLongFromInsn(instruction));

                        newInsn.add(insns);
                        continue;
                    }
                    if (ClassUtil.isDoubleInsn(instruction)) {
                        InsnList insns = obfuscateNumber(ClassUtil.getDoubleFromInsn(instruction));

                        newInsn.add(insns);
                        continue;
                    }
                    if (ClassUtil.isFloatInsn(instruction)) {
                        InsnList insns = obfuscateNumber(ClassUtil.getFloatFromInsn(instruction));

                        newInsn.add(insns);
                        continue;
                    }
                    newInsn.add(instruction);
                }
                method.getMethodNode().instructions = newInsn;
            }
        }
    }
    private InsnList obfuscateNumber(int originalNum) {
        int current = 0;
        if (RandomUtils.randomBoolean()){
            current = randomInt(originalNum);
        }else {
            current = RandomUtils.randomInt();
        }

        InsnList insns = new InsnList();
        insns.add(ClassUtil.getNumberInsn(current));

        for (int i = 0; i < RandomUtils.randomInt(2, 6); i++) {
            int operand;

            switch (RandomUtils.randomInt(12)) {
                case 0:
                    operand = randomInt(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IADD));

                    current += operand;
                    break;
                case 1:
                    operand = randomInt(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(ISUB));

                    current -= operand;
                    break;
                case 2:
                    operand = RandomUtils.randomInt(1, 255);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IMUL));

                    current *= operand;
                    break;
                case 3:
                    operand = RandomUtils.randomInt(1, 255);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IDIV));

                    current /= operand;
                    break;
                case 4:
                    operand = RandomUtils.randomInt(1, 5);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(ISHR));

                    current = current >> operand;
                    break;
                case 5:
                    operand = RandomUtils.randomInt(1, 5);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IUSHR));

                    current = current >>> operand;
                    break;
                case 6:
                    operand = RandomUtils.randomInt(1, 5);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(ISHL));

                    current = current << operand;
                    break;
                case 7:
                    insns.add(ClassUtil.getNumberInsn(-1));
                    insns.add(new InsnNode(IXOR));

                    current = ~current;
                    break;
                case 8:
                    operand = randomInt(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IXOR));

                    current = current ^ operand;
                    break;
                case 9:
                    operand = randomInt(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IOR));

                    current |= operand;
                    break;
                case 10:
                    operand = randomInt(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IAND));

                    current &= operand;
                    break;
                default:
                    operand = RandomUtils.randomInt(1, 255);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(IREM));

                    current %= operand;
                    break;
            }
        }
        if (RandomUtils.randomBoolean()){
            int correctionOperand = originalNum ^ current;
            insns.add(ClassUtil.getNumberInsn(correctionOperand));
            insns.add(new InsnNode(IXOR));
        }else {
            int correctionOperand = originalNum - current;
            insns.add(ClassUtil.getNumberInsn(correctionOperand));
            insns.add(new InsnNode(IADD));
        }

        return insns;
    }
    private InsnList obfuscateNumber(long originalNum) {
        long current;
        if (RandomUtils.randomBoolean()){
            current = randomLong(originalNum);
        }else {
            current = RandomUtils.randomLong();
        }
        InsnList insns = new InsnList();
        insns.add(ClassUtil.getNumberInsn(current));

        for (int i = 0; i < RandomUtils.randomInt(2, 6); i++) {
            long operand;

            switch (RandomUtils.randomInt(12)) {
                case 0:
                    operand = randomLong(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LADD));

                    current += operand;
                    break;
                case 1:
                    operand = randomLong(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LSUB));

                    current -= operand;
                    break;
                case 2:
                    operand = RandomUtils.randomInt(1, 65535);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LMUL));

                    current *= operand;
                    break;
                case 3:
                    operand = RandomUtils.randomInt(1, 65535);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LDIV));

                    current /= operand;
                    break;
                case 4:
                    operand = randomLong(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LAND));

                    current &= operand;
                    break;
                case 5:
                    operand = randomLong(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LOR));

                    current |= operand;
                    break;
                case 6:
                    operand = randomLong(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LXOR));

                    current ^= operand;
                    break;
                case 7:
                    operand = RandomUtils.randomInt(1, 32);

                    insns.add(ClassUtil.getNumberInsn((int) operand));
                    insns.add(new InsnNode(LSHL));

                    current <<= operand;
                    break;
                case 8:
                    operand = RandomUtils.randomInt(1, 32);

                    insns.add(ClassUtil.getNumberInsn((int) operand));
                    insns.add(new InsnNode(LSHR));

                    current >>= operand;
                    break;
                case 9:
                    operand = RandomUtils.randomInt(1, 32);

                    insns.add(ClassUtil.getNumberInsn((int) operand));
                    insns.add(new InsnNode(LUSHR));

                    current >>>= operand;
                    break;
                case 10:
                    insns.add(ClassUtil.getNumberInsn((long) -1));
                    insns.add(new InsnNode(LXOR));

                    current = ~current;
                    break;
                default:
                    operand = RandomUtils.randomInt(1, 255);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(LREM));

                    current %= operand;
                    break;
            }
        }
        if (RandomUtils.randomBoolean()){
            long correctionOperand = originalNum - current;
            insns.add(ClassUtil.getNumberInsn(correctionOperand));
            insns.add(new InsnNode(LADD));
        }else {
            long correctionOperand = originalNum ^ current;
            insns.add(ClassUtil.getNumberInsn(correctionOperand));
            insns.add(new InsnNode(LXOR));
        }

        return insns;
    }
    private InsnList obfuscateNumber(double originalNum) {
        double current = randomDouble(originalNum);

        InsnList insns = new InsnList();
        insns.add(ClassUtil.getNumberInsn(current));

        for (int i = 0; i < RandomUtils.randomInt(2, 6); i++) {
            double operand;

            switch (RandomUtils.randomInt(6)) {
                case 0:
                    operand = randomDouble(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(DADD));

                    current += operand;
                    break;
                case 1:
                    operand = randomDouble(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(DSUB));

                    current -= operand;
                    break;
                case 2:
                    operand = RandomUtils.randomInt(1, 65535);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(DMUL));

                    current *= operand;
                    break;
                case 3:
                    operand = RandomUtils.randomInt(1, 65535);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(DDIV));

                    current /= operand;
                    break;
                case 4:
                default:
                    operand = RandomUtils.randomInt(1, 255);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(DREM));

                    current %= operand;
                    break;
            }
        }

        double correctionOperand = originalNum - current;
        insns.add(ClassUtil.getNumberInsn(correctionOperand));
        insns.add(new InsnNode(DADD));

        return insns;
    }
    private InsnList obfuscateNumber(float originalNum) {
        float current = randomFloat(originalNum);

        InsnList insns = new InsnList();
        insns.add(ClassUtil.getNumberInsn(current));

        for (int i = 0; i < RandomUtils.randomInt(2, 6); i++) {
            float operand;

            switch (RandomUtils.randomInt(6)) {
                case 0:
                    operand = randomFloat(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(FADD));

                    current += operand;
                    break;
                case 1:
                    operand = randomFloat(current);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(FSUB));

                    current -= operand;
                    break;
                case 2:
                    operand = RandomUtils.randomInt(1, 65535);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(FMUL));

                    current *= operand;
                    break;
                case 3:
                    operand = RandomUtils.randomInt(1, 65535);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(FDIV));

                    current /= operand;
                    break;
                case 4:
                default:
                    operand = RandomUtils.randomInt(1, 255);

                    insns.add(ClassUtil.getNumberInsn(operand));
                    insns.add(new InsnNode(FREM));

                    current %= operand;
                    break;
            }
        }

        float correctionOperand = originalNum - current;
        insns.add(ClassUtil.getNumberInsn(correctionOperand));
        insns.add(new InsnNode(FADD));

        return insns;
    }

    protected static int randomInt(int bounds) {
        if (bounds <= 0)
            return RandomUtils.randomInt(Integer.MAX_VALUE);

        return RandomUtils.randomInt(bounds);
    }

    protected static long randomLong(long bounds) {
        if (bounds <= 0)
            return RandomUtils.randomLong(Long.MAX_VALUE);

        return RandomUtils.randomLong(bounds);
    }

    protected static float randomFloat(float bounds) {
        if (bounds <= 0)
            return RandomUtils.randomFloat(Float.MAX_VALUE);

        return RandomUtils.randomFloat(bounds);
    }

    protected static double randomDouble(double bounds) {
        if (bounds <= 0)
            return RandomUtils.randomDouble(Double.MAX_VALUE);

        return RandomUtils.randomDouble(bounds);
    }

}
