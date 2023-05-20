package dev.crater.transformer.transformers;

import dev.crater.Crater;
import dev.crater.Main;
import dev.crater.transformer.Transformer;
import dev.crater.utils.CraterRemapper;
import dev.crater.utils.FileUtils;
import dev.crater.utils.RandomUtils;
import dev.crater.utils.dictionary.Dictionary;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.jar.FieldWrapper;
import dev.crater.utils.jar.MethodWrapper;
import dev.crater.utils.jar.ResourceWrapper;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.IntStream;

public class Renamer extends Transformer {
    private final static Logger logger = LogManager.getLogger("Renamer");

    private final Map<String, String> mappings = new HashMap<>();
    private Dictionary methodNameDictionary = null;
    private Dictionary fieldNameDictionary = null;
    private Dictionary classNameDictionary = null;
    private Map<String,List<String>> filterRules = new HashMap<>();
    public Renamer() {
        super("Name");
        methodNameDictionary = Dictionary.getDictionary("method");
        fieldNameDictionary = Dictionary.getDictionary("field");
        classNameDictionary = Dictionary.getDictionary("class");
    }

    @Override
    public void transform(List<ClassWrapper> cw, Crater crater) {
        logger.info("Building inheritance hierarchy graph");
        long current = System.currentTimeMillis();
        crater.buildHierarchyGraph();
        logger.info(String.format("Finished building inheritance graph [%dms]", (System.currentTimeMillis() - current)));

        logger.info("Generating mappings");
        current = System.currentTimeMillis();
        loadFilterRules(crater);
        generateMappings(cw,crater);
        logger.info(String.format("Finished generating mappings [%dms]", (System.currentTimeMillis() - current)));

        logger.info("Applying mappings");
        current = System.currentTimeMillis();
        applyMappings(cw);
        logger.info(String.format("Finished applying mappings [%dms]", (System.currentTimeMillis() - current)));

        logger.info("Adapting resources");
        current = System.currentTimeMillis();
        fixEntryPoint();
        adaptResources(crater.getResources());
        logger.info(String.format("Finished adapting resources [%dms]", (System.currentTimeMillis() - current)));

        if (crater.getConfig().containsKey("Name.export")) {
            logger.info("Dumping mappings");
            dumpMappings(crater);
        }
    }
    private void generateMappings(List<ClassWrapper> classes,Crater crater) {
        classes.forEach(classWrapper -> {
            classWrapper.methodStream().filter(methodWrapped -> !cannotRenameMethod(classWrapper, methodWrapped, new HashSet<>())).forEach(methodWrapper -> {
                String newName;
                do {
                    newName = methodNameDictionary.next();
                } while (mappings.containsKey(classWrapper.getOriginName() + '.' + methodWrapper.getOriginName() + methodWrapper.getOriginDescriptor())
                        && mappings.get(classWrapper.getOriginName() + '.' + methodWrapper.getOriginName() + methodWrapper.getOriginDescriptor()).equals(newName));


                generateMethodMappings(classWrapper, methodWrapper, newName);
                for (String mapping : mappings.keySet()) {
                    if (mapping.startsWith("java/util/List.size") && mappings.get(mapping).contains("Cx")) {
                        System.out.println("Working on " + classWrapper.getClassInternalName() + '.' + methodWrapper.getOriginName() + methodWrapper.getOriginDescriptor() + ": " + mapping + " -> " + mappings.get(mapping));
                        break;
                    }
                }
            });
            classWrapper.fieldStream().filter(fieldWrapper -> !cannotRenameField(classWrapper, fieldWrapper)).forEach(fieldWrapper -> {
                String newName;
                do {
                    newName = fieldNameDictionary.next();
                } while (mappings.containsKey(classWrapper.getOriginName() + '.' + fieldWrapper.getOriginName() + ' ' + fieldWrapper.getOriginType())
                        && mappings.get(classWrapper.getOriginName() + '.' + fieldWrapper.getOriginName() + ' ' + fieldWrapper.getOriginType()).equals(newName));


                generateFieldMappings(classWrapper, fieldWrapper, newName);
            });

            if (!filtered(classWrapper)) {
                String repackagingPrefix = (String) crater.getConfig().get("Name.newPackageName");
                if (repackagingPrefix == null) {
                    repackagingPrefix = classNameDictionary.copy().randomStr(RandomUtils.randomInt(0xF));
                }
                repackagingPrefix = repackagingPrefix.replace(".", "/");
                String newName = repackagingPrefix;
                if (!newName.isEmpty()) {
                    newName += '/';
                }
                String temp;
                do {
                    temp = newName + classNameDictionary.next();
                } while (getClasspathWrapper(crater,temp) != null); // Important to check classpath instead of input classes
                newName = temp;
                mappings.put(classWrapper.getOriginName(), newName);
            }
        });
    }
    public ClassWrapper getClasspathWrapper(Crater crater,String name) {
        for (ClassWrapper aClass : crater.getClasses()) {
            if (aClass.getClassInternalName().equals(name)) {
                return aClass;
            }
        }
        for (ClassWrapper librariesClass : crater.getLibrariesClasses()) {
            if (librariesClass.getClassInternalName().equals(name)) {
                return librariesClass;
            }
        }
        return null;
    }
    private boolean cannotRenameMethod(ClassWrapper classWrapper, MethodWrapper wrapper, Set<ClassWrapper> visited) {
        // Already visited so don't check
        if (!visited.add(classWrapper)) {
            return false;
        }

        // If excluded, we don't want to rename.
        // If we already mapped the tree, we don't want to waste time doing it again.
        if (filtered(classWrapper,wrapper.getMethodNode()) || mappings.containsKey(classWrapper.getOriginName() + '.' + wrapper.getOriginName() + wrapper.getOriginDescriptor())) {
            return true;
        }

        // Native and main/premain methods should not be renamed
        if (wrapper.isNative()){
            if (Main.INSTANCE.getConfig().containsKey("Name.renameNative")){
                return (boolean) Main.INSTANCE.getConfig().get("Name.renameNative");
            }
            return true;
        }
        // Init and clinit methods should also not be renamed (otherwise the jvm will get mad)
        if ((wrapper.getOriginName().equals("main") && wrapper.getOriginDescriptor().equals("([Ljava/lang/String;)V")) || wrapper.getOriginName().equals("premain") || wrapper.getOriginName().startsWith("<")) {
            return true;
        }

        // Static methods are never inherited
        if (wrapper.isStatic()) {
            // Renaming these particular enum methods will cause problems
            return classWrapper.isEnum()
                    && (wrapper.getOriginName().equals("valueOf") || wrapper.getOriginName().equals("values"));
        } else {
            if (classWrapper.getOriginName().startsWith("java/util")) {
                if (Crater.isDebug())
                    System.out.println("Checking " + classWrapper.getOriginName() + '.' + wrapper.getOriginName() + wrapper.getOriginDescriptor());
            }
            // Methods which override or inherit from external libs cannot be renamed
            if (classWrapper != wrapper.getClassWrapper() && classWrapper.isLibrary()
                    && classWrapper.methodStream().anyMatch(other -> other.getOriginName().equals(wrapper.getOriginName())
                    && other.getOriginDescriptor().equals(wrapper.getOriginDescriptor()))) {
                System.out.println("Cannot rename " + classWrapper.getOriginName() + '.' + wrapper.getOriginName() + wrapper.getOriginDescriptor() + " because it overrides a library method");
                return true;
            }

            // Children are checked for exclusions
            // Parents are checked for exclusions and if they are library nodes
            return classWrapper.getParents().stream().anyMatch(parent -> cannotRenameMethod(parent, wrapper, visited))
                    || classWrapper.getChildren().stream().anyMatch(child -> cannotRenameMethod(child, wrapper, visited));
        }
    }

    private void generateMethodMappings(ClassWrapper owner, MethodWrapper wrapper, String newName) {
        if (owner.isLibrary())
            return;
        String key = owner.getOriginName() + '.' + wrapper.getOriginName() + wrapper.getOriginDescriptor();

        // This (supposedly) will prevent an infinite recursion because the tree was already renamed
        if (mappings.containsKey(key)) {
            return;
        }
        mappings.put(key, newName);

        if (!wrapper.isStatic()) { //  Static methods cannot be overriden
            owner.getParents().forEach(parent -> generateMethodMappings(parent, wrapper, newName));
            owner.getChildren().forEach(child -> generateMethodMappings(child, wrapper, newName));
        }
    }

    private boolean cannotRenameField(ClassWrapper classWrapper, FieldWrapper wrapper) {
        if (filtered(classWrapper,wrapper.getFieldNode()) || mappings.containsKey(classWrapper.getOriginName() + '.' + wrapper.getOriginName() + ' ' + wrapper.getOriginType())) {
            return true;
        }

        //return classWrapper.isEnum(); // Todo: enums are a pain to handle
        return false;
    }

    private void generateFieldMappings(ClassWrapper owner, FieldWrapper wrapper, String newName) {
        String key = owner.getOriginName() + '.' + wrapper.getOriginName() + ' ' + wrapper.getOriginType();

        // This (supposedly) will prevent an infinite recursion because the tree was already renamed
        if (mappings.containsKey(key)) {
            return;
        }
        mappings.put(key, newName);

        if (!wrapper.isStatic()) { //  Static fields cannot be inherited
            owner.getParents().forEach(parent -> generateFieldMappings(parent, wrapper, newName));
            owner.getChildren().forEach(child -> generateFieldMappings(child, wrapper, newName));
        }
    }
    private void applyMappings(List<ClassWrapper> classes) {
        try(ProgressBar pb = new ProgressBar("Remap classes",classes.size())){
            CraterRemapper remapper = new CraterRemapper(mappings);
            classes.stream().parallel().forEach(cw -> {
                ClassNode newClassNode = new ClassNode();
                ClassRemapper classRemapper = new ClassRemapper(newClassNode,remapper);
                cw.getClassNode().accept(classRemapper);
                cw.setClassNode(newClassNode);
                cw.wrap();
                pb.step();
            });
        }
    }
    private void fixEntryPoint(){
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
                String newMainClass = mappings.get(mainClass.replace(".","/")).replace("/",".");
                logger.info("Remap Main-Class to: " + newMainClass);
                attributes.putValue("Main-Class",newMainClass);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    manifest.write(baos);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                rw.setBytes(baos.toByteArray());
            }else {
                logger.info("Main-Class not found! Skip entry-point fix");
            }
        }else {
            logger.info("Manifest not found! Skip entry-point fix");
        }
    }
    private void adaptResources(List<ResourceWrapper> resources) {
        for (ResourceWrapper resource : resources) {
            byte[] resourceBytes = resource.getBytes();
            if (resourceBytes == null) {
                logger.warn("Attempted to adapt nonexistent resource: " + resource.getOriginEntryName());
            }

            String stringVer = new String(resourceBytes, StandardCharsets.UTF_8);
            for (String original : mappings.keySet()) {
                if (stringVer.contains(original.replace("/", "."))) {
                    if (resource.getOriginEntryName().equals("META-INF/MANIFEST.MF")
                            || resource.getOriginEntryName().equals("plugin.yml")
                            || resource.getOriginEntryName().equals("bungee.yml")) {
                        stringVer = stringVer.replaceAll("(?<=[: ])" + original.replace("/", "."), mappings.get(original)).replace("/", ".");
                    } else {
                        stringVer = stringVer.replace(original.replace("/", "."), mappings.get(original)).replace("/", ".");
                    }
                }
            }
            resource.setEntryName(resource.getOriginEntryName());
        }
    }

    private void dumpMappings(Crater crater) {
        File file = new File((String) crater.getConfig().get("Name.export"));
        if (file.exists()) {
            FileUtils.renameExistingFile(file);
        }

        try {
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            mappings.forEach((oldName, newName) -> {
                try {
                    bw.append(oldName).append(" -> ").append(newName).append("\n");
                } catch (IOException ioe) {
                    logger.warn(String.format("Caught IOException while attempting to write line \"%s -> %s\"", oldName, newName));
                    if (Crater.isDebug()) {
                        ioe.printStackTrace(System.out);
                    }
                }
            });
        } catch (Throwable t) {
            logger.warn("Captured throwable upon attempting to generate mappings file: " + t.getMessage());
            if (Crater.isDebug()) {
                t.printStackTrace(System.out);
            }
        }
    }
    public void loadFilterRules(Crater crater){
        if (crater.getConfig().containsKey("Name.filterRule")){
            try {
                String rules = new String(FileUtils.readFile(new File((String) crater.getConfig().get("Name.filterRule")))).replace("\r","");
                String currentClass = null;
                List<String> filterRule = new ArrayList<>();
                for (String rule : rules.split("\n")) {
                    if (rule.startsWith("class")){
                        if (currentClass != null){
                            filterRules.put(currentClass,filterRule);
                        }
                        currentClass = rule.split(" ")[1];
                        filterRule.clear();
                    }else {
                        filterRule.add(rule.trim());
                    }
                }
                if (currentClass != null){
                    filterRules.put(currentClass,filterRule);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public boolean filtered(ClassWrapper clazz){
        if(filterRules.containsKey(clazz.getClassInternalName().replace("/","."))) {
            for (String rule : filterRules.get(clazz.getClassInternalName().replace("/","."))){
                if (rule.equalsIgnoreCase("<keepClassName>")){
                    return true;
                }
            }
        }
        return false;
    }
    public boolean filtered(ClassWrapper cw, FieldNode field){
        if(filterRules.containsKey(cw.getClassInternalName().replace("/","."))) {
            for (String rule : filterRules.get(cw.getClassInternalName().replace("/","."))){
                if (rule.equalsIgnoreCase(field.name)){
                    return true;
                }
                if (rule.equalsIgnoreCase("<anyField>")){
                    return true;
                }
            }
        }
        return false;
    }
    public boolean filtered(ClassWrapper cw, MethodNode method){
        if (method.name.equals("<init>") || method.name.equals("<clinit>")){
            return true;
        }
        if(filterRules.containsKey(cw.getClassInternalName().replace("/","."))) {
            for (String rule : filterRules.get(cw.getClassInternalName().replace("/","."))){
                if (rule.equalsIgnoreCase(method.name+method.desc)){
                    return true;
                }
                if (rule.equalsIgnoreCase("<anyMethod>")){
                    return true;
                }
            }
        }
        return false;
    }
}
