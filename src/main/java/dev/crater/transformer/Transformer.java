package dev.crater.transformer;

import dev.crater.Crater;
import dev.crater.utils.jar.ClassWrapper;
import lombok.Getter;
import org.objectweb.asm.Opcodes;

import java.util.List;

public abstract class Transformer implements Opcodes {
    @Getter
    private String name;
    public Transformer(String name) {
        this.name = name;
    }
    public abstract void transform(List<ClassWrapper> cw, Crater crater);
}
