package dev.crater.transformer.transformers.flow;

import dev.crater.utils.jar.ClassWrapper;
import org.objectweb.asm.Opcodes;

public abstract class FlowTask implements Opcodes {
    public abstract void execute(ClassWrapper classWrapper);
}
