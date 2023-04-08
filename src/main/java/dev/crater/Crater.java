package dev.crater;

import dev.crater.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Crater {
    private final static Logger logger = LogManager.getLogger("Crater");
    private Map<Object, Object> config;
    public Crater(File configFile){
        if (!configFile.exists()){
            logger.error("Config file not found");
        }
        String config = null;
        try {
            config = new String(FileUtils.readFile(configFile));
        } catch (IOException e) {
            logger.error("Read config file error",e);
            return;
        }
        this.config = new Yaml().load(config);
        System.out.println(this.config);
    }
}
