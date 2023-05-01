package dev.crater.utils.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigTree extends HashMap<String,Object> {
    public static ConfigTree toTree(Map<Object,Object> map){
        ConfigTree tree = new ConfigTree();
        tree.putAll(convert(map,null));
        return tree;
    }
    private static Map<String,Object> convert(Map<Object,Object> map,String parentKey){
        Map<String,Object> newMap = new HashMap<>();
        for (Map.Entry<Object,Object> entry : map.entrySet()){
            if (entry.getKey() instanceof String){
                if (entry.getValue() instanceof Map){
                    newMap.putAll(convert((Map<Object, Object>) entry.getValue(),parentKey == null ? (String) entry.getKey() : parentKey+"."+entry.getKey()));
                }
                else{
                    newMap.put(parentKey == null ? (String) entry.getKey() : parentKey+"."+entry.getKey(),entry.getValue());
                }
            }
        }
        return newMap;
    }
    public boolean getBoolean(String key){
        return containsKey(key) && (boolean) get(key);
    }
}
