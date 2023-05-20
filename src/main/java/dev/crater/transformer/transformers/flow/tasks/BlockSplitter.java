package dev.crater.transformer.transformers.flow.tasks;

import dev.crater.Main;
import dev.crater.transformer.transformers.flow.FlowTask;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.MethodWrapper;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public class BlockSplitter extends FlowTask {
    private static double LIMIT_FACTOR = 0;
    private static int MIN_LIMIT = 0;
    public BlockSplitter(){
        LIMIT_FACTOR = (double)(Main.INSTANCE.getConfig().get("Flow.blockSizeFactor"));
        MIN_LIMIT = (int)(Main.INSTANCE.getConfig().get("Flow.minimumBlockSize"));
    }
    @Override
    public void execute(ClassWrapper classWrapper) {
        for (MethodWrapper method : classWrapper.getMethods()) {
            doSplit(method.getMethodNode(), 0, Math.max((int) (method.getMethodNode().instructions.size() * LIMIT_FACTOR),MIN_LIMIT));
        }
    }
    private static void doSplit(MethodNode methodNode, int callStackSize,int limitSize) {
        InsnList insns = methodNode.instructions;

        if (insns.size() > 10 && callStackSize < limitSize) {
            LabelNode p1 = new LabelNode();
            LabelNode p2 = new LabelNode();
            AbstractInsnNode p2Start = insns.get((int) ((insns.size() - 1) * 0.2));
            AbstractInsnNode p2End = insns.getLast();

            AbstractInsnNode p1Start = insns.getFirst();

            // We can't have trap ranges mutilated by block splitting
            if (methodNode.tryCatchBlocks.stream().anyMatch(tcbn ->
                    insns.indexOf(tcbn.end) >= insns.indexOf(p2Start)
                            && insns.indexOf(tcbn.start) <= insns.indexOf(p2Start)))
                return;


            ArrayList<AbstractInsnNode> insnNodes = new ArrayList<>();
            AbstractInsnNode currentInsn = p1Start;

            InsnList p1Block = new InsnList();

            while (currentInsn != p2Start) {
                insnNodes.add(currentInsn);

                currentInsn = currentInsn.getNext();
            }

            insnNodes.forEach(insn -> {
                insns.remove(insn);
                p1Block.add(insn);
            });

            p1Block.insert(p1);
            p1Block.add(new JumpInsnNode(GOTO, p2));

            insns.insert(p2End, p1Block);
            insns.insertBefore(p2Start, new JumpInsnNode(GOTO, p1));
            insns.insertBefore(p2Start, p2);

            // We might have messed up variable ranges when rearranging the block order.
            if (methodNode.localVariables != null)
                new ArrayList<>(methodNode.localVariables).stream().filter(lvn ->
                        insns.indexOf(lvn.end) < insns.indexOf(lvn.start)
                ).forEach(methodNode.localVariables::remove);

            doSplit(methodNode, callStackSize + 1,limitSize);
        }
    }
}
