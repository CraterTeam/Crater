package dev.crater.utils;

import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;

public class CraterRemapper extends SimpleRemapper {
    public CraterRemapper(Map<String, String> mappings) {
        super(mappings);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String remappedName = map(owner + '.' + name + ' ' + descriptor);
        return (remappedName != null) ? remappedName : name;
    }
}
