package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.Main;
import dev.crater.transformer.Transformer;
import dev.crater.utils.jar.ClassWrapper;

import java.util.List;

public class Mutations extends Transformer {
    public Mutations() {
        super("Mutations");
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        System.out.println("Mutations not implemented yet");
    }
    private boolean canMutate(){
        return percentage((Double) Main.INSTANCE.getConfig().get("Mutations.percent"));
    }
    private boolean percentage(double percentage){
        return Math.random() <= percentage;
    }
}
