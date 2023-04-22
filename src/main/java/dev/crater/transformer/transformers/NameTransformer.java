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
import dev.crater.utils.name.*;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

//ToDo: Support for inner classes
//ToDo: Support for no-modifier {@see https://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html}
public class NameTransformer extends Transformer {
    private final static Logger logger = LogManager.getLogger("NameTransformer");
    private Map<String,List<String>> filterRules = new HashMap<>();
    public NameTransformer() {
        super("Name");
    }

    @Override
    public void transform(List<ClassWrapper> cw,Crater crater) {
        loadFilterRules(crater);
        List<MapNode> preparse = new ArrayList<>();
        logger.info("Preparing");
        for (ClassWrapper classWrapper : cw) {
            generateClassMap(classWrapper,preparse,cw,crater);
        }
        logger.info("Generating names");
        /*for (int i = 0; i < cw.size(); i++) {
            ClassWrapper clazz = cw.get(i);
            if (filtered(clazz)){
                preparse.add(new NoChangeNode(clazz));
            }else {
                preparse.add(new NoNameNode(clazz));
            }
            {
                for (int x = 0; x < clazz.getClassNode().fields.size(); x++) {
                    FieldNode field = clazz.getClassNode().fields.get(x);
                    boolean skip = false;
                    if (filtered(clazz,field)){
                        preparse.add(new NoChangeNode(new FieldWrapper(clazz,field)));
                    }else {
                        preparse.add(new NoNameNode(new FieldWrapper(clazz,field)));
                        //Support for protected fields
                        if ((field.access & Opcodes.ACC_PROTECTED) != 0){
                            List<ClassWrapper> child = new ArrayList<>();
                            for (int y = 0; y < cw.size(); y++) {
                                ClassWrapper current = cw.get(y);
                                ClassWrapper parent = current;
                                boolean isChild = false;
                                while (parent != null){
                                    ClassWrapper find = parent;
                                    parent = null;
                                    for (int z = 0; z < cw.size(); z++) {
                                        if (find.getClassInternalName().equals(clazz.getClassInternalName())){
                                            isChild = true;
                                            break;
                                        }
                                        if (cw.get(z).getClassInternalName().equals(find.getClassNode().superName)){
                                            parent = cw.get(z);
                                            break;
                                        }
                                    }
                                }
                                if (isChild){
                                    child.add(current);
                                }
                            }
                            for (ClassWrapper classWrapper : child) {
                                preparse.add(new ProtectedNode(classWrapper,new FieldWrapper(clazz,field)));
                            }
                        }
                    }
                }
                for (int x = 0; x < clazz.getClassNode().methods.size(); x++) {
                    MethodNode method = clazz.getClassNode().methods.get(x);
                    if (filtered(clazz,method)){
                        preparse.add(new NoChangeNode(new MethodWrapper(clazz,method)));
                    }else {
                        List<String> inherit = new ArrayList<>();
                        inherit.add(clazz.getClassNode().superName);
                        clazz.getClassNode().interfaces.forEach(inherit::add);
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
                                if (isInherited) break;
                                //find in jvm
                                {
                                    String current = parent;
                                    while (!current.equals("java/lang/Object")){
                                        try {
                                            Class cls = Class.forName(current.replace("/","."));
                                            for (Method method1 : cls.getDeclaredMethods()) {
                                                if (method1.getName().equals(method.name) && Type.getMethodDescriptor(method1).equals(method.desc)){
                                                    preparse.add(new NoChangeNode(new MethodWrapper(clazz,method)));
                                                    isInherited = true;
                                                }
                                            }
                                            if (cls.getSuperclass() == null)
                                                break;
                                            current = Type.getInternalName(cls.getSuperclass());
                                        } catch (ClassNotFoundException e) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (!isInherited){
                            preparse.add(new NoNameNode(new MethodWrapper(clazz,method)));
                        }
                        if ((method.access & Opcodes.ACC_PROTECTED) != 0){
                            List<ClassWrapper> child = new ArrayList<>();
                            for (int y = 0; y < cw.size(); y++) {
                                ClassWrapper current = cw.get(y);
                                ClassWrapper parent = current;
                                boolean isChild = false;
                                while (parent != null){
                                    ClassWrapper find = parent;
                                    parent = null;
                                    for (int z = 0; z < cw.size(); z++) {
                                        if (find.getClassInternalName().equals(clazz.getClassInternalName())){
                                            isChild = true;
                                            break;
                                        }
                                        if (cw.get(z).getClassInternalName().equals(find.getClassNode().superName)){
                                            parent = cw.get(z);
                                            break;
                                        }
                                    }
                                }
                                if (isChild){
                                    child.add(current);
                                }
                            }
                            for (ClassWrapper classWrapper : child) {
                                preparse.add(new ProtectedNode(classWrapper,new MethodWrapper(clazz,method)));
                            }
                        }
                    }
                }
            }
        }

         */
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
        List<MapNode> sorted = new ArrayList<>();
        for (MapNode mapNode : preparse) {
            if (mapNode instanceof NoNameNode || mapNode instanceof NoChangeNode || mapNode instanceof InheritNode){
                sorted.add(0,mapNode);
            }else {
                sorted.add(mapNode);
            }
        }
        preparse = sorted;
        boolean pass = false;
        while (!pass){
            pass = true;
            SamePackageNode currentNode = null;
            for (int i = 0; i < preparse.size(); i++) {
                if (preparse.get(i) instanceof SamePackageNode){
                    currentNode = (SamePackageNode) preparse.get(i);
                    if (isLooped(currentNode,preparse)){
                        if (crater.isDebug())
                            System.out.println("Looped: " + currentNode);
                        preparse.remove(i);
                        preparse.add(new SpecificPackageNode((ClassWrapper) currentNode.getSource(),getPackage(packageName)));
                        pass = false;
                        break;
                    }
                }
            }
        }
        //Generate simple map
        try (ProgressBar pb = new ProgressBar("Generate Map", preparse.size())){
            while (preparse.size() != 0){
                pb.stepTo(pb.getMax() - preparse.size());
                MapNode node = preparse.get(0);
                if (node instanceof NoChangeNode){
                    if (node.getSource() instanceof ClassWrapper){
                        ClassWrapper classWrapper = (ClassWrapper) node.getSource();
                        map.put(classWrapper.getClassInternalName(),classWrapper.getClassInternalName());
                    }else if (node.getSource() instanceof FieldWrapper){
                        FieldWrapper fieldWrapper = (FieldWrapper) node.getSource();
                        map.put(fieldWrapper.getClassWrapper().getClassInternalName()
                                +"."+fieldWrapper.getFieldNode().name,fieldWrapper.getFieldNode().name);
                    }else if (node.getSource() instanceof MethodWrapper){
                        MethodWrapper methodWrapper = (MethodWrapper) node.getSource();
                        map.put(methodWrapper.getClassWrapper().getClassInternalName()
                                +"."+methodWrapper.getMethodNode().name+methodWrapper.getMethodNode().desc,methodWrapper.getMethodNode().name);
                    }
                    preparse.remove(0);
                    continue;
                }
                if (node instanceof NoNameNode){
                    if (node.getSource() instanceof ClassWrapper){
                        ClassWrapper classWrapper = (ClassWrapper) node.getSource();
                        map.put(classWrapper.getClassInternalName(),packageName + dictionary.getWord(WordType.TypeName));
                    }else if (node.getSource() instanceof FieldWrapper){
                        FieldWrapper fieldWrapper = (FieldWrapper) node.getSource();
                        map.put(fieldWrapper.getClassWrapper().getClassInternalName()
                                +"."+fieldWrapper.getFieldNode().name,dictionary.getWord(WordType.FieldName));
                    }else if (node.getSource() instanceof MethodWrapper){
                        MethodWrapper methodWrapper = (MethodWrapper) node.getSource();
                        map.put(methodWrapper.getClassWrapper().getClassInternalName()
                                +"."+methodWrapper.getMethodNode().name+methodWrapper.getMethodNode().desc,dictionary.getWord(WordType.MethodName));
                    }
                    preparse.remove(0);
                    continue;
                }
                if (node instanceof InheritNode){
                    InheritNode inheritNode = (InheritNode) node;
                    boolean isFound = false;
                    for (int i = 0; i < new ArrayList<>(map.entrySet()).size(); i++) {
                        Map.Entry<String, String> entry = new ArrayList<>(map.entrySet()).get(i);
                        if (entry.getKey().equals(inheritNode.getParent().getClassWrapper().getClassInternalName()
                                +"."+inheritNode.getParent().getMethodNode().name+inheritNode.getParent().getMethodNode().desc)){
                            MethodWrapper methodWrapper = (MethodWrapper) node.getSource();
                            map.put(methodWrapper.getClassWrapper().getClassInternalName()
                                    +"."+methodWrapper.getMethodNode().name+methodWrapper.getMethodNode().desc,entry.getValue());
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
                if (node instanceof ProtectedNode){
                    ProtectedNode protectedNode = (ProtectedNode) node;
                    boolean isFound = false;
                    for (int i = 0; i < new ArrayList<>(map.entrySet()).size(); i++) {
                        Map.Entry<String, String> entry = new ArrayList<>(map.entrySet()).get(i);
                        if (protectedNode.getTarget() instanceof FieldWrapper){
                            FieldWrapper fieldWrapper = (FieldWrapper) protectedNode.getTarget();
                            if (entry.getKey().equals(fieldWrapper.getClassWrapper().getClassInternalName()
                                    +"."+fieldWrapper.getFieldNode().name)){
                                ClassWrapper classWrapper = (ClassWrapper) node.getSource();
                                map.put(classWrapper.getClassInternalName()+"."+fieldWrapper.getFieldNode().name,
                                        entry.getValue());
                                preparse.remove(0);
                                isFound = true;
                                break;
                            }
                        }else if (protectedNode.getTarget() instanceof MethodWrapper){
                            MethodWrapper methodWrapper = (MethodWrapper) protectedNode.getTarget();
                            if (entry.getKey().equals(methodWrapper.getClassWrapper().getClassInternalName()
                                    +"."+methodWrapper.getMethodNode().name+methodWrapper.getMethodNode().desc)){
                                ClassWrapper classWrapper = (ClassWrapper) node.getSource();
                                map.put(classWrapper.getClassInternalName()+"."+methodWrapper.getMethodNode().name+methodWrapper.getMethodNode().desc,
                                        entry.getValue());
                                preparse.remove(0);
                                isFound = true;
                                break;
                            }
                        }
                    }
                    if (!isFound){
                        preparse.remove(0);
                        preparse.add(node);
                    }
                }
                if (node instanceof SamePackageNode){
                    ClassWrapper classWrapper = (ClassWrapper) node.getSource();
                    ClassWrapper newClassWrapper = ((SamePackageNode) node).getTarget();
                    boolean isFound = false;
                    for (int i = 0; i < new ArrayList<>(map.entrySet()).size(); i++) {
                        Map.Entry<String, String> entry = new ArrayList<>(map.entrySet()).get(i);
                        if (entry.getKey().equals(newClassWrapper.getClassInternalName())){
                            map.put(classWrapper.getClassInternalName(),getPackage(entry.getValue())+"/"+ dictionary.getWord(WordType.TypeName));
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
                if (node instanceof SpecificPackageNode){
                    ClassWrapper classWrapper = (ClassWrapper) node.getSource();
                    map.put(classWrapper.getClassInternalName(),((SpecificPackageNode) node).getTarget()+"/"+ dictionary.getWord(WordType.TypeName));
                    preparse.remove(0);
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
    private boolean isLooped(SamePackageNode samePackageNode,List<MapNode> mapNodes){
        List<MapNode> copied = new ArrayList<>(mapNodes);
        SamePackageNode current = samePackageNode;
        int count = 0;
        while ((current = findTarget(current,copied)) != null){
            count++;
            if (((ClassWrapper)current.getSource()).getClassInternalName().equals(((ClassWrapper)samePackageNode.getSource()).getClassInternalName())){
                return true;
            }
            if (count > 100)//I guess it is a loop
                return true;
        }
        return false;
    }
    private SamePackageNode findTarget(SamePackageNode target,List<MapNode> mapNodes){
        for (MapNode mapNode : mapNodes) {
            if (mapNode instanceof SamePackageNode){
                SamePackageNode samePackageNode = (SamePackageNode) mapNode;
                if (samePackageNode.getSource().equals(target.getTarget())){
                    return samePackageNode;
                }
            }
        }
        return null;
    }
    private void generateClassMap(ClassWrapper cw,List<MapNode> maps,List<ClassWrapper> classes,Crater crater){
        if (filtered(cw)){
            maps.add(new NoChangeNode(cw));
        }else {
            if (!hasPackageOnly(cw,classes,maps,crater)){
                maps.add(new NoNameNode(cw));
            }else {
                if (crater.isDebug())
                    logger.debug("Class " + cw.getClassInternalName() + " has package only access");
            }
        }
        for (MethodNode methodNode : cw.getClassNode().methods) {
            if (!filtered(cw,methodNode)){
                generateMethodMap(new MethodWrapper(cw,methodNode),maps,classes,crater);
            }else {
                maps.add(new NoChangeNode(new MethodWrapper(cw,methodNode)));
            }
        }
    }
    private void generateMethodMap(MethodWrapper mw,List<MapNode> maps,List<ClassWrapper> classes,Crater crater){
        Object superClass = mw.getClassWrapper();
        while ((superClass = getSuper(superClass,crater)) != null){
            if (superClass instanceof ClassWrapper){
                ClassWrapper classWrapper = (ClassWrapper) superClass;
                for (int i = 0; i < classWrapper.getClassNode().methods.size(); i++) {
                    MethodNode methodNode = classWrapper.getClassNode().methods.get(i);
                    if (methodNode.name.equals(mw.getMethodNode().name) && methodNode.desc.equals(mw.getMethodNode().desc)){
                        if (isModifiable(classWrapper.getClassInternalName(),classes)){
                            maps.add(new InheritNode(mw,new MethodWrapper(classWrapper,methodNode)));
                        }else {
                            maps.add(new NoChangeNode(mw));
                        }
                        return;
                    }
                }
            }
            if (superClass instanceof Class<?>){
                Class<?> clazz = (Class<?>) superClass;
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(mw.getMethodNode().name) && Type.getMethodDescriptor(method).equals(mw.getMethodNode().desc)){
                        maps.add(new NoChangeNode(mw));
                        return;
                    }
                }
            }
        }
        maps.add(new NoNameNode(mw));
    }
    private Object getSuper(Object child,Crater crater){
        String superName = null;
        if (child instanceof ClassWrapper){
            ClassWrapper cw = (ClassWrapper) child;
            superName = cw.getClassNode().superName;
        }else if (child instanceof Class){
            if (child == Object.class){
                return null;
            }
            Class<?> clazz = (Class<?>) child;
            superName = Type.getInternalName(clazz.getSuperclass());
        }
        List<ClassWrapper> merged = new ArrayList<>();
        merged.addAll(crater.getLibrariesClasses());
        merged.addAll(crater.getFilteredClasses());
        merged.addAll(crater.getClasses());
        for (ClassWrapper classWrapper : merged) {
            if (classWrapper.getClassInternalName().equals(superName)){
                return classWrapper;
            }
        }
        try {
            return Class.forName(superName.replace("/","."));
        } catch (ClassNotFoundException e) {
            if (crater.isDebug())
                logger.debug("Super class " + superName + " not found");
            return null;
        }
    }
    private String getClassName(Object clazz){
        if (clazz instanceof ClassWrapper){
            return ((ClassWrapper) clazz).getClassInternalName();
        }else if (clazz instanceof Class){
            return Type.getInternalName((Class<?>) clazz);
        }
        return null;
    }
    private boolean hasPackageOnly(ClassWrapper cw,List<ClassWrapper> classes,List<MapNode> maps,Crater crater){
        for (MethodNode method : cw.getClassNode().methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode){
                    MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                    if (!methodInsnNode.owner.equals(cw.getClassInternalName())){
                        if (getPackage(methodInsnNode.owner).equals(getPackage(cw.getClassInternalName()))){
                            if (isPackageOnlyAccess(methodInsnNode.owner,methodInsnNode.name,methodInsnNode.desc,crater)){
                                if (isModifiable(methodInsnNode.owner,classes)){
                                    maps.add(new SamePackageNode(cw,findClass(methodInsnNode.owner,classes)));
                                    return true;
                                }else {
                                    maps.add(new SpecificPackageNode(cw,getPackage(methodInsnNode.owner)));
                                    return true;
                                }
                            }
                        }
                    }
                }else if (instruction instanceof FieldInsnNode){
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                    if (!fieldInsnNode.owner.equals(cw.getClassInternalName())){
                        if (getPackage(fieldInsnNode.owner).equals(getPackage(cw.getClassInternalName()))){
                            if (isPackageOnlyAccess(fieldInsnNode.owner,fieldInsnNode.name,crater)){
                                if (isModifiable(fieldInsnNode.owner,classes)){
                                    maps.add(new SamePackageNode(cw,findClass(fieldInsnNode.owner,classes)));
                                    return true;
                                }else {
                                    maps.add(new SpecificPackageNode(cw,getPackage(fieldInsnNode.owner)));
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    private boolean isPackageOnlyAccess(String classInternalName,String fieldName,Crater crater){
        List<ClassWrapper> merged = new ArrayList<>();
        merged.addAll(crater.getLibrariesClasses());
        merged.addAll(crater.getFilteredClasses());
        merged.addAll(crater.getClasses());
        if (findClass(classInternalName,merged) != null){
            ClassWrapper cw = findClass(classInternalName,merged);
            for (int i = 0; i < cw.getClassNode().fields.size(); i++) {
                FieldNode fieldNode = cw.getClassNode().fields.get(i);
                if (fieldNode.name.equals(fieldName)){
                    if (fieldNode.access == 0){
                        return true;
                    }
                }
            }
        }
        try {
            Class<?> clazz = Class.forName(classInternalName.replace("/","."));
            Field field = null;
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.getName().equals(fieldName)){
                    field = declaredField;
                    break;
                }
            }
            if (field == null){
                if (crater.isDebug()){
                    logger.debug("Cannot find field: " + fieldName +" in class: " + classInternalName + "");
                }
                return false;
            }
            if (field.getModifiers() == 0){
                return true;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
    private boolean isPackageOnlyAccess(String classInternalName,String methodName,String methodDesc,Crater crater){
        if (methodName.equals("<init>") || methodName.equals("<clinit>")){
            return false;
        }
        List<ClassWrapper> merged = new ArrayList<>();
        merged.addAll(crater.getLibrariesClasses());
        merged.addAll(crater.getFilteredClasses());
        merged.addAll(crater.getClasses());
        if (findClass(classInternalName,merged) != null){
            ClassWrapper cw = findClass(classInternalName,merged);
            for (int i = 0; i < cw.getClassNode().methods.size(); i++) {
                MethodNode methodNode = cw.getClassNode().methods.get(i);
                if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodDesc)){
                    if (methodNode.access == 0){
                        return true;
                    }
                }
            }
        }
        try {
            Class<?> clazz = Class.forName(classInternalName.replace("/","."));
            Method method = null;
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (declaredMethod.getName().equals(methodName) && Type.getMethodDescriptor(declaredMethod).equals(methodDesc)){
                    method = declaredMethod;
                    break;
                }
            }
            if (method == null){
                if (crater.isDebug()){
                    logger.debug("Cannot find method: " + methodName + methodDesc + " in class: " + classInternalName + "");
                }
                return false;
            }
            if (method.getModifiers() == 0){
                return true;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
    private ClassWrapper findClass(String name,List<ClassWrapper> classes){
        name = name.replace(".","/");
        for (ClassWrapper classWrapper : classes) {
            if (classWrapper.getClassInternalName().equals(name)){
                return classWrapper;
            }
        }
        return null;
    }
    private boolean isModifiable(String name,List<ClassWrapper> classes){
        name = name.replace(".","/");
        for (ClassWrapper classWrapper : classes) {
            if (classWrapper.getClassInternalName().equals(name)){
                return true;
            }
        }
        return false;
    }
    private String getPackage(String name){
        if (name.contains("/")){
            return name.substring(0,name.lastIndexOf("/"));
        }else {
            return "";
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
    public boolean filtered(ClassWrapper cw,FieldNode field){
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
    public boolean filtered(ClassWrapper cw,MethodNode method){
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
