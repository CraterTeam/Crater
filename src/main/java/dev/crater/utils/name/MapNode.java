package dev.crater.utils.name;

import dev.crater.utils.dictionary.WordType;
import dev.crater.utils.jar.IWrapper;
import lombok.Getter;

public class MapNode {
    @Getter
    private IWrapper source;
    public MapNode(IWrapper source){
        this.source = source;
    }
}
