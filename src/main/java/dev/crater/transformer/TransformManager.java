package dev.crater.transformer;

import dev.crater.transformer.transformers.Collapse;
import dev.crater.transformer.transformers.Renamer;
import dev.crater.transformer.transformers.SourceRenamer;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TransformManager {
    private final static Logger logger = LogManager.getLogger("TransformManager");
    @Getter
    private final List<Transformer> transformers = new ArrayList<>();
    public TransformManager(){
        logger.info("TransformManager init");
        loadTransformer(new Renamer());
        loadTransformer(new SourceRenamer());
        loadTransformer(new Collapse());
    }
    public void loadTransformer(Transformer transformer){
        transformers.add(transformer);
    }
}
