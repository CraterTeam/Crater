package dev.crater.utils.name;

import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.FieldWrapper;
import dev.crater.utils.jar.IWrapper;
import lombok.Getter;

public class ProtectedNode extends MapNode{
    @Getter
    private IWrapper target;
    public ProtectedNode(ClassWrapper source, IWrapper target) {
        super(source);
        this.target = target;
    }
}
