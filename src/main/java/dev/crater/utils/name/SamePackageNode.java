package dev.crater.utils.name;

import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.IWrapper;
import lombok.Getter;

public class SamePackageNode extends MapNode{
    @Getter
    private ClassWrapper target;
    public SamePackageNode(ClassWrapper source, ClassWrapper target) {
        super(source);
        this.target = target;
    }
}
