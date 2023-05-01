package dev.crater.transformer;

import dev.crater.transformer.transformers.*;
import dev.crater.transformer.transformers.Number;
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
        loadTransformer(new Hider());
        loadTransformer(new Number());
        loadTransformer(new Mutations());
    }
    public void loadTransformer(Transformer transformer){
        transformers.add(transformer);
    }
}
