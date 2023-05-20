package dev.crater.transformer;

import dev.crater.transformer.transformers.*;
import dev.crater.transformer.transformers.Number;
import dev.crater.transformer.transformers.flow.Flow;
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
        loadTransformer(new Number());
        loadTransformer(new Flow());
        loadTransformer(new Renamer());
        loadTransformer(new SourceRenamer());
        loadTransformer(new Mutations());
        loadTransformer(new Collapse());
        loadTransformer(new Hider());
    }
    public void loadTransformer(Transformer transformer){
        transformers.add(transformer);
    }
}
