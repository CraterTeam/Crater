package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.Main;
import dev.crater.transformer.Transformer;
import dev.crater.utils.FileUtils;
import dev.crater.utils.dictionary.Dictionary;
import dev.crater.utils.dictionary.WordType;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.FieldWrapper;
import dev.crater.utils.jar.MethodWrapper;
import dev.crater.utils.jar.ResourceWrapper;
import dev.crater.utils.name.InheritNode;
import dev.crater.utils.name.MapNode;
import dev.crater.utils.name.NoChangeNode;
import dev.crater.utils.name.NoNameNode;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

//ToDo: Support for inner classes
public class NameTransformer extends Transformer {
    private final static Logger logger = LogManager.getLogger("NameTransformer");
    private Map<String,List<String>> filterRules = new HashMap<>();
    public NameTransformer() {
        super("Name");
    }

    @Override
    public void transform(List<ClassWrapper> cw) {
        if (Main.INSTANCE.getConfig().containsKey("Name.filterRule")){
            try {
                String ruleFile = new String(FileUtils.readFile(new File((String) Main.INSTANCE.getConfig().get("Name.filterRule"))));
                List<String> currentRule = new ArrayList<>();
                String currentClass = "";
                ruleFile = ruleFile.replace("\r","");
                for (String s : ruleFile.split("\n")) {
                    if (s.split(" ").length == 2){
                        if (s.startsWith("class ")){
                            if (!currentClass.equals("")){
                                filterRules.put(currentClass,currentRule);
                            }
                            currentClass = s.split(" ")[1];
                            currentRule = new ArrayList<>();
                        }
                    }else {
                        currentRule.add(s.trim());
                    }
                }
                if (!currentClass.equals("")){
                    filterRules.put(currentClass,currentRule);
                }
            } catch (IOException e) {
                logger.info("Failed to load filter rule file");
                throw new RuntimeException(e);
            }
        }
        List<MapNode> preparse = new ArrayList<>();
        for (int i = 0; i < cw.size(); i++) {
            ClassWrapper clazz = cw.get(i);
            if (filtered(clazz)){
                preparse.add(new NoChangeNode(clazz));
            }else {
                preparse.add(new NoNameNode(clazz));
            }
            {
                for (int x = 0; x < clazz.getClassNode().fields.size(); x++) {
                    FieldNode field = clazz.getClassNode().fields.get(x);
                    if (filtered(field)){
                        preparse.add(new NoChangeNode(new FieldWrapper(clazz,field)));
                    }else {
                        preparse.add(new NoNameNode(new FieldWrapper(clazz,field)));
                    }
                }
                for (int x = 0; x < clazz.getClassNode().methods.size(); x++) {
                    MethodNode method = clazz.getClassNode().methods.get(x);
                    if (filtered(method)){
                        preparse.add(new NoChangeNode(new MethodWrapper(clazz,method)));
                    }else {
                        List<String> inherit = new ArrayList<>();
                        inherit.add(clazz.getClassNode().superName);
                        clazz.getClassNode().interfaces.forEach(inherit::add);
                        Crater crater = Main.INSTANCE;
                        boolean isInherited = false;
                        for (String parent : inherit) {
                            if (parent != null) {
                                //find in class list
                                for (int y = 0; y < cw.size(); y++) {
                                    ClassWrapper testClass = cw.get(y);
                                    if (testClass.getClassInternalName().equals(parent)){
                                        for (int z = 0; z < testClass.getClassNode().methods.size(); z++) {
                                            MethodNode testMethod = testClass.getClassNode().methods.get(z);
                                            if (testMethod.name.equals(method.name) && testMethod.desc.equals(method.desc)){
                                                preparse.add(new InheritNode(new MethodWrapper(clazz,method),new MethodWrapper(testClass,testMethod)));
                                                isInherited = true;
                                            }
                                        }
                                    }
                                }
                                if (isInherited) break;
                                //find in filtered class list
                                for (int y = 0; y < crater.getFilteredClasses().size(); y++) {
                                    ClassWrapper testClass = crater.getFilteredClasses().get(y);
                                    if (testClass.getClassInternalName().equals(parent)){
                                        for (int z = 0; z < testClass.getClassNode().methods.size(); z++) {
                                            MethodNode testMethod = new MethodNode();
                                            if (testMethod.name.equals(method.name) && testMethod.desc.equals(method.desc)){
                                                preparse.add(new NoChangeNode(new MethodWrapper(clazz,method)));
                                                isInherited = true;
                                            }
                                        }
                                    }
                                }
                                if (isInherited) break;
                                //find in library class list
                                for (int y = 0; y < crater.getLibrariesClasses().size(); y++) {
                                    ClassWrapper testClass = crater.getLibrariesClasses().get(y);
                                    if (testClass.getClassInternalName().equals(parent)){
                                        for (int z = 0; z < testClass.getClassNode().methods.size(); z++) {
                                            MethodNode testMethod = new MethodNode();
                                            if (testMethod.name.equals(method.name) && testMethod.desc.equals(method.desc)){
                                                preparse.add(new NoChangeNode(new MethodWrapper(clazz,method)));
                                                isInherited = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!isInherited){
                            preparse.add(new NoNameNode(new MethodWrapper(clazz,method)));
                        }
                    }
                }
            }
        }
        Map<String,String> map = new HashMap<>();
        Dictionary dictionary = null;
        for (Dictionary dictionary1 : Dictionary.getDictionaries()) {
            if (dictionary1.getName().equals((String) (Main.INSTANCE.getConfig().get("Name.dictionary.name")))) dictionary = dictionary1;
        }
        if (dictionary == null){
            throw new RuntimeException("Dictionary not found!");
        }
        String packageName = (String) Main.INSTANCE.getConfig().get("Name.newPackageName");
        if (packageName == null){
            packageName = "";
        }else {
            packageName = packageName.replace(".","/");
            if (!packageName.endsWith("/")) packageName = packageName + "/";
        }
        //Generate simple map
        try (ProgressBar pb = new ProgressBar("Generate Map", preparse.size())){
            while (preparse.size() != 0){
                pb.stepTo(pb.getMax() - preparse.size());
                MapNode node = preparse.get(0);
                if (node instanceof NoChangeNode){
                    if (node.getSource() instanceof ClassWrapper){
                        map.put(((ClassWrapper) node.getSource()).getClassInternalName(),((ClassWrapper) node.getSource()).getClassInternalName());
                    }else if (node.getSource() instanceof FieldWrapper){
                        map.put(((FieldWrapper) node.getSource()).getClassWrapper().getClassInternalName()
                                +"."+((FieldWrapper) node.getSource()).getFieldNode().name,((FieldWrapper) node.getSource()).getFieldNode().name);
                    }else if (node.getSource() instanceof MethodWrapper){
                        map.put(((MethodWrapper) node.getSource()).getClassWrapper().getClassInternalName()
                                +"."+((MethodWrapper) node.getSource()).getMethodNode().name+((MethodWrapper) node.getSource()).getMethodNode().desc,((MethodWrapper) node.getSource()).getMethodNode().name);
                    }
                    preparse.remove(0);
                    continue;
                }
                if (node instanceof NoNameNode){
                    if (node.getSource() instanceof ClassWrapper){
                        map.put(((ClassWrapper) node.getSource()).getClassInternalName(),packageName + dictionary.getWord(WordType.TypeName));
                    }else if (node.getSource() instanceof FieldWrapper){
                        map.put(((FieldWrapper) node.getSource()).getClassWrapper().getClassInternalName()
                                +"."+((FieldWrapper) node.getSource()).getFieldNode().name,dictionary.getWord(WordType.FieldName));
                    }else if (node.getSource() instanceof MethodWrapper){
                        map.put(((MethodWrapper) node.getSource()).getClassWrapper().getClassInternalName()
                                +"."+((MethodWrapper) node.getSource()).getMethodNode().name+((MethodWrapper) node.getSource()).getMethodNode().desc,dictionary.getWord(WordType.MethodName));
                    }
                    preparse.remove(0);
                    continue;
                }
                if (node instanceof InheritNode){
                    boolean isFound = false;
                    for (int i = 0; i < new ArrayList<>(map.entrySet()).size(); i++) {
                        Map.Entry<String, String> entry = new ArrayList<>(map.entrySet()).get(i);
                        if (entry.getKey().equals(((InheritNode) node).getParent().getClassWrapper().getClassInternalName()
                                +"."+((InheritNode) node).getParent().getMethodNode().name+((InheritNode) node).getParent().getMethodNode().desc)){
                            map.put(((MethodWrapper)((InheritNode) node).getSource()).getClassWrapper().getClassInternalName()
                                    +"."+((MethodWrapper)((InheritNode) node).getSource()).getMethodNode().name+((MethodWrapper)((InheritNode) node).getSource()).getMethodNode().desc,entry.getValue());
                            preparse.remove(0);
                            isFound = true;
                            break;
                        }
                    }
                    if (!isFound){
                        preparse.remove(0);
                        preparse.add(node);
                    }
                }
            }
        }
        {
            //Fix Entry point
            ResourceWrapper rw = null;
            for (ResourceWrapper resourceWrapper : Main.INSTANCE.getResources()) {
                if (resourceWrapper.getOriginEntryName().equals("META-INF/MANIFEST.MF")){
                    rw = resourceWrapper;
                    break;
                }
            }
            if (rw != null){
                Manifest manifest = new Manifest();
                try {
                    manifest.read(new ByteArrayInputStream(rw.getBytes()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Attributes attributes = manifest.getMainAttributes();
                String mainClass = attributes.getValue("Main-Class");
                if (mainClass != null){
                    logger.info("Found Main-Class: " + mainClass);
                    String newMainClass = map.get(mainClass.replace(".","/")).replace("/",".");
                    logger.info("Remap Main-Class to: " + newMainClass);
                    attributes.putValue("Main-Class",newMainClass);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        manifest.write(baos);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    rw.setBytes(baos.toByteArray());
                    //Keep main class
                    map.replace(mainClass.replace(".","/")+".main([Ljava/lang/String;)V","main");
                }else {
                    logger.info("Main-Class not found! Skip entry-point fix");
                }
            }else {
                logger.info("Manifest not found! Skip entry-point fix");
            }
        }
        SimpleRemapper remapper = new SimpleRemapper(map);
        for (ClassWrapper classWrapper : ProgressBar.wrap(cw,"Remap classes")) {
            ClassNode newClassNode = new ClassNode();
            ClassRemapper classRemapper = new ClassRemapper(newClassNode,remapper);
            classWrapper.getClassNode().accept(classRemapper);
            classWrapper.setClassNode(newClassNode);
        }
        if (Main.INSTANCE.getConfig().containsKey("Name.export")) {
            logger.info("Write map to file");
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
            }
            try {
                FileUtils.writeFile(new File((String) Main.INSTANCE.getConfig().get("Name.export")), sb.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public boolean filtered(ClassWrapper clazz){
        if(filterRules.containsKey(clazz.getClassInternalName().replace("/","."))) {
            for (String rule : filterRules.get(clazz.getClassInternalName().replace("/","."))){
                if (rule.equalsIgnoreCase("keepClassName")){
                    return true;
                }
            }
        }
        return false;
    }
    public boolean filtered(FieldNode field){
        return false;
    }
    public boolean filtered(MethodNode method){
        if (method.name.equals("<init>") || method.name.equals("<clinit>")){
            return true;
        }
        return false;
    }
}
