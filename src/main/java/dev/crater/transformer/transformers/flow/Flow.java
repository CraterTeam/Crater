package dev.crater.transformer.transformers.flow;

import dev.crater.Crater;
import dev.crater.transformer.Transformer;
import dev.crater.transformer.transformers.flow.tasks.BlockSplitter;
import dev.crater.transformer.transformers.flow.tasks.GotoReplacer;
import dev.crater.utils.jar.ClassWrapper;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Flow extends Transformer {
    private static final Logger logger = LogManager.getLogger("Flow");
    private final List<FlowTask> tasks = new ArrayList<>();
    public Flow() {
        super("Flow");
        tasks.add(new BlockSplitter());
        tasks.add(new GotoReplacer());
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        for (ClassWrapper classWrapper : ProgressBar.wrap(cw,"Flow")) {
            tasks.forEach(task -> task.execute(classWrapper));
        }
    }
}
