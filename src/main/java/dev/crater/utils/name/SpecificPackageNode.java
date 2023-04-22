package dev.crater.utils.name;

import dev.crater.utils.jar.ClassWrapper;
import lombok.Getter;

public class SpecificPackageNode extends MapNode{
    @Getter
    private String target;
    public SpecificPackageNode(ClassWrapper source, String target) {
        super(source);
        this.target = target;
    }
}
