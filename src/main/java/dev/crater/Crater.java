package dev.crater;

import dev.crater.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Crater {
    private final static Logger logger = LogManager.getLogger("Crater");
    private Map<String, Object> config;
    public Crater(File configFile){
        if(!parseConfig(configFile)){
            logger.error("An error occurred while parsing the configuration file");
            return;
        }
    }
    private boolean parseConfig(File configFile){
        if (!configFile.exists()){
            logger.error("Config file not found");
        }
        String config = null;
        try {
            config = new String(FileUtils.readFile(configFile));
        } catch (IOException e) {
            logger.error("Read config file error",e);
            return false;
        }
        Map<Object,Object> configMap = new Yaml().load(config);
        if (configMap == null){
            logger.error("Config file is empty");
            return false;
        }
        this.config = convert(configMap,null);
        for (Map.Entry<String, Object> stringObjectEntry : this.config.entrySet()) {
            System.out.println(stringObjectEntry.getKey()+" = "+stringObjectEntry.getValue());
        }
        return true;
    }
    private Map<String,Object> convert(Map<Object,Object> map,String parentKey){
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
}
