package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.transformer.Transformer;
import dev.crater.utils.RandomUtils;
import dev.crater.utils.jar.ClassWrapper;

import java.util.List;

public class SourceRenamer extends Transformer {
    public SourceRenamer() {
        super("Source");
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        for (ClassWrapper classWrapper : cw) {
            classWrapper.getClassNode().sourceDebug = null;
        }
        if (!crater.getConfig().containsKey("Source.newSource")){
            for (ClassWrapper classWrapper : cw) {
                classWrapper.getClassNode().sourceFile = null;
            }
        }else {
            if (crater.getConfig().get("Source.newSource") instanceof List){
                List<String> newSource = (List<String>) crater.getConfig().get("Source.newSource");
                for (ClassWrapper classWrapper : cw) {
                    classWrapper.getClassNode().sourceFile = newSource.get(RandomUtils.randomInt(newSource.size()));
                }
            }else {
                String newSource = crater.getConfig().get("Source.newSource").toString();
                for (ClassWrapper classWrapper : cw) {
                    classWrapper.getClassNode().sourceFile = newSource;
                }
            }
        }
    }
}
