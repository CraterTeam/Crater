package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.transformer.Transformer;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.FieldWrapper;
import dev.crater.utils.jar.MethodWrapper;

import java.util.List;

/**
 * Some decompilers don't show synthetic methods and fields, so we can hide them
 */
public class Hider extends Transformer {
    public Hider() {
        super("Hider");
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        if (crater.getConfig().getBoolean("Hider.method")){
            for (ClassWrapper classWrapper : cw) {
                for (MethodWrapper method : classWrapper.getMethods()) {
                    if (!method.isSynthetic()) {
                        method.setAccess(method.getAccess() | ACC_SYNTHETIC);
                    }
                    if (!method.getOriginName().startsWith("<") && !method.isBridge()) {
                        method.setAccess(method.getAccess() | ACC_BRIDGE);
                    }
                }
            }
        }
        if (crater.getConfig().getBoolean("Hider.field")){
            for (ClassWrapper classWrapper : cw) {
                for (FieldWrapper field : classWrapper.getFields()) {
                    if (!field.isSynthetic()) {
                        field.setAccess(field.getAccess() | ACC_SYNTHETIC);
                    }
                }
            }
        }
    }
}
