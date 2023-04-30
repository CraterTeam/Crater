package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.transformer.Transformer;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.MethodWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.List;

public class Collapse extends Transformer {
    private final static Logger logger = LogManager.getLogger("Collapse");
    public Collapse() {
        super("Collapse");
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        logger.info("Removing linenumber");
        for (ClassWrapper classWrapper : cw) {
            for (MethodWrapper method : classWrapper.getMethods()) {
                InsnList newInsnList = new InsnList();
                for (AbstractInsnNode instruction : method.getMethodNode().instructions) {
                    if (instruction instanceof LineNumberNode)
                        continue;
                    newInsnList.add(instruction);
                }
            }
        }
        logger.info("Removing localVariables");
        for (ClassWrapper classWrapper : cw) {
            for (MethodWrapper method : classWrapper.getMethods()) {
                if (method.getMethodNode().localVariables != null)
                    method.getMethodNode().localVariables.clear();
            }
        }
    }
}
