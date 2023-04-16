package dev.crater.utils.name;

import dev.crater.utils.jar.MethodWrapper;

public class InheritNode extends MapNode{
    private MethodWrapper parent;

    public InheritNode(MethodWrapper source, MethodWrapper parent) {
        super(source);
        this.parent = parent;
    }
    public MethodWrapper getParent() {
        return parent;
    }
}
