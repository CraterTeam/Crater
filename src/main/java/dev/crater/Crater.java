package dev.crater;

import dev.crater.transformer.TransformManager;
import dev.crater.transformer.Transformer;
import dev.crater.utils.ClassTree;
import dev.crater.utils.HTTPUtils;
import dev.crater.utils.jar.ClassWrapper;
import dev.crater.utils.ClassUtil;
import dev.crater.utils.FileUtils;
import dev.crater.utils.jar.JarIO;
import dev.crater.utils.config.ConfigTree;
import dev.crater.utils.jar.ResourceWrapper;
import lombok.Getter;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.yaml.snakeyaml.Yaml;
import sun.misc.Launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Crater {
    private final static Logger logger = LogManager.getLogger("Crater");
    @Getter
    private ConfigTree config;
    @Getter
    private final List<ClassWrapper> classes = new ArrayList<>();
    @Getter
    private final List<ResourceWrapper> resources = new ArrayList<>();
    @Getter
    private final List<ClassWrapper> filteredClasses = new ArrayList<>();
    @Getter
    private final List<ClassWrapper> librariesClasses = new ArrayList<>();
    private Map<String, ClassTree> hierarchy = new HashMap<>();
    private long originJarSize = 0;
    private TransformManager transformManager;
    @Getter
    private static boolean debug = false;
    public Crater(File configFile){
        if(!parseConfig(configFile) ||
                !hasImportantConfig()){
            logger.error("An error occurred while parsing the configuration file");
            throw new RuntimeException();
        }
        if (config.containsKey("debug")){
            debug = (boolean) config.get("debug");
            if (debug){
                logger.info("Debug mode enabled");
            }
        }
        logger.info("Loading library");
        if (!loadLibs()){
            logger.error("An error occurred while loading the library");
            throw new RuntimeException();
        }
        logger.info("Loading classes");
        if (!loadClasses()){
            logger.error("An error occurred while loading the classes");
            throw new RuntimeException();
        }
        logger.info(String.format("Loaded %d classes",classes.size()) +" "+filteredClasses.size()+" filtered");
        if (!checkClasses()){
            logger.error("An error occurred while checking the classes");
            throw new RuntimeException();
        }
        logger.info("Classes checked");
    }
    private void buildHierarchy(ClassWrapper wrapper, ClassWrapper sub, Set<String> visited) {
        if (visited.add(wrapper.getClassInternalName())) {
            if (wrapper.getSuperName() != null) {
                ClassWrapper superParent = getClasspathWrapper(wrapper.getSuperName());
                wrapper.getParents().add(superParent);
                buildHierarchy(superParent, wrapper, visited);
            }
            if (wrapper.getInterfaces() != null) {
                wrapper.getInterfaces().forEach(interfaceName -> {
                    ClassWrapper interfaceParent = getClasspathWrapper(interfaceName);
                    wrapper.getParents().add(interfaceParent);
                    buildHierarchy(interfaceParent, wrapper, visited);
                });
            }
        }
        if (sub != null) {
            wrapper.getChildren().add(sub);
        }
    }
    public ClassWrapper getClasspathWrapper(String name) {
        if (name == null) return null;
        for (ClassWrapper aClass : classes) {
            if (aClass.getClassInternalName().equals(name)) {
                return aClass;
            }
        }
        for (ClassWrapper librariesClass : librariesClasses) {
            if (librariesClass.getClassInternalName().equals(name)) {
                return librariesClass;
            }
        }
        throw new RuntimeException("Missing library class: " + name);
    }
    public void buildHierarchyGraph() {
        HashSet<String> visited = new HashSet<>();
        classes.forEach(wrapper -> buildHierarchy(wrapper, null, visited));
    }

    public void doObfuscate(){
        transformManager = new TransformManager();
        logger.info("Obfuscating");
        for (Transformer transformer : ProgressBar.wrap(transformManager.getTransformers(),"Obfuscating")) {
            if (!config.containsKey(transformer.getName() + ".enable") || !((boolean) (config.get(transformer.getName() + ".enable")))){
                logger.info("Skip {}",transformer.getName());
                continue;
            }
            logger.info("Executing transformer: {}",transformer.getName());
            transformer.transform(classes,this);
        }
        logger.info("Obfuscate finished");
    }
    public void saveJar(){
        boolean verify = true;
        if (config.containsKey("verifyClasses")){
            verify = (boolean)config.get("verifyClasses");
        }else {
            verify = true;
            logger.info("Verify classes is not set, default to true");
        }
        new File((String)config.get("output")).delete();
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File((String)config.get("output"))));
            for (ClassWrapper classWrapper : ProgressBar.wrap(classes,"Save classes")) {
                ZipEntry entry = new ZipEntry(classWrapper.getClassInternalName() + ".class");
                zos.putNextEntry(entry);
                zos.write(classWrapper.getClassBytes(verify));
            }
            for (ClassWrapper filteredClass : ProgressBar.wrap(filteredClasses,"Save filtered classes")) {
                ZipEntry entry = new ZipEntry(filteredClass.getOriginEntryName());
                zos.putNextEntry(entry);
                zos.write(filteredClass.getOriginBytes());
            }
            for (ResourceWrapper resource : ProgressBar.wrap(resources,"Save resources")) {
                ZipEntry entry = new ZipEntry(resource.getEntryName());
                zos.putNextEntry(entry);
                zos.write(resource.getBytes());
            }
            zos.close();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("An error occurred while saving the jar");
            return;
        }
        long newJarSize = new File((String) config.get("output")).length();
        logger.info("Jar saved {} bytes({})",newJarSize,newJarSize < originJarSize ? "-"+(originJarSize-newJarSize) : "+"+(newJarSize-originJarSize));
    }

    private boolean checkClasses(){
        if ((boolean)config.get("skipCheck")){
            logger.info("Classes check skipped");
            return true;
        }
        List<String> missing = new ArrayList<>();
        for (ClassWrapper cw : ProgressBar.wrap(classes,"Checking")) {
            List<String> relatedClasses = getRelatedClasses(cw);
            relatedClasses.stream().filter(s -> s != null)
                    .map(ClassUtil::byInternalName)
                    .filter(s -> !(classes.stream().map(ClassWrapper::getClassName).collect(Collectors.toList()).contains(s)))
                    .filter(s -> !(filteredClasses.stream().map(ClassWrapper::getClassName).collect(Collectors.toList()).contains(s)))
                    .filter(s -> !(librariesClasses.stream().map(ClassWrapper::getClassName).collect(Collectors.toList()).contains(s)))
                    .filter(s -> !(ClassUtil.hasClass(s)))
                    .forEach(missing::add);
        }
        missing = missing.stream().distinct().collect(Collectors.toList());
        if (missing.size() > 0){
            logger.error("Missing classes:");
            for (String s : missing) {
                logger.error(s);
            }
            logger.error("Missing classes count: " + missing.size());
            logger.info("If you want to skip check please use \"skipCheck: false\" in config file");
            return false;
        }
        return true;
    }
    private List<String> getRelatedClasses(ClassWrapper cw){
        List<String> relatedClasses = new ArrayList<>();
        for (MethodNode method : cw.getClassNode().methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode){
                    relatedClasses.add(((MethodInsnNode) instruction).owner);
                }
                if (instruction instanceof FieldInsnNode){
                    relatedClasses.add(((FieldInsnNode) instruction).owner);
                }
            }
        }
        return relatedClasses.stream().map(s -> s.contains("[L") ? s.replace("[L", "").replace(";", "") : s).distinct().collect(Collectors.toList());
    }
    private boolean loadClasses(){
        if (!new File((String) config.get("input")).exists()){
            logger.error("Input file not found");
            return false;
        }
        originJarSize = new File((String) config.get("input")).length();
        List<String> filters = new ArrayList<>();
        if (config.containsKey("filter")){
            filters.addAll((Collection<? extends String>) config.get("filter"));
        }
        Map<String,byte[]> files = JarIO.readJar(new File((String) config.get("input")));
        Map<String,byte[]> classes = new HashMap<>();
        Map<String,byte[]> resources = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            if (entry.getKey().endsWith(".class")){
                boolean skip = false;
                for (String filter : filters) {
                    if (entry.getKey().replace("/",".").contains(filter)){
                        filteredClasses.add(new ClassWrapper(entry.getKey(),entry.getValue()));
                        skip = true;
                    }
                }
                if (skip)
                    continue;
                classes.put(entry.getKey(),entry.getValue());
            }else {
                resources.put(entry.getKey(),entry.getValue());
            }
        }
        if (classes.size() == 0){
            logger.error("Classes read error");
            return false;
        }
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            this.classes.add(new ClassWrapper(entry.getKey(),entry.getValue()));
        }
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            this.resources.add(new ResourceWrapper(entry.getKey(),entry.getValue()));
        }
        return true;
    }
    private boolean loadLibs(){
        List<File> libFiles = new ArrayList<>();
        logger.info("");
        URL[] urLs = Launcher.getBootstrapClassPath().getURLs();
        for (URL urL : urLs) {
            try {
                if (new File(urL.toURI()).isFile()){
                    libFiles.add(new File(urL.toURI()));
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        List<URL> urls = new ArrayList<>();
        if (config.containsKey("maven")){
            logger.info("Loading maven libraries");
            List<String> mavenLibs;
            if (((mavenLibs = loadMavenLibs()) == null)){
                logger.error("An error occurred while loading maven libraries");
                return false;
            }
            for (String mavenLib : mavenLibs) {
                libFiles.add(new File(mavenLib));
            }
        }
        if (config.containsKey("library")){
            List<String> libs = (List<String>) config.get("library");
            if (libs == null){
                logger.error("Libs config error");
                return false;
            }
            for (String lib : libs) {
                File libFile = new File(lib);
                if (!libFile.exists()){
                    logger.error("Lib file not found: "+lib);
                    return false;
                }
                libFiles.add(libFile);
            }
        }
        for (File libFile : ProgressBar.wrap(libFiles,"Loading libs")) {
            loadLib(libFile);
        }
        return true;
    }
    private void loadLib(File file){
        JarIO.readJar(file).forEach((s, bytes) -> {
            if (s.endsWith(".class")){
                librariesClasses.add(new ClassWrapper(s,bytes,true));
            }
        });
    }
    private List<String> loadMavenLibs(){
        List<String> mavenLibs = (List<String>) config.get("maven");
        List<String> repos = (List<String>) config.get("repo");
        if (repos == null){
            logger.warn("No repository found, using default repository \"https://repo1.maven.org/maven2/\"");
            repos = new ArrayList<>();
            repos.add("https://repo1.maven.org/maven2/");
        }
        List<String> libFiles = new ArrayList<>();
        new File("maven").mkdir();
        for (String mavenLib : ProgressBar.wrap(mavenLibs,"Load maven libraries")) {
            File lib = new File("maven\\"+FileUtils.parseMavenFile(mavenLib));
            if (!lib.exists()){
                byte[] bytes = null;
                new File(FileUtils.getPath("maven\\"+FileUtils.parseMavenFile(mavenLib))).mkdirs();
                for (String repo : repos) {
                    String url = HTTPUtils.parseMavenURL(repo,mavenLib);
                    try {
                        try (ProgressBar pb = new ProgressBar("Download", -1)){
                            pb.setExtraMessage(mavenLib);
                            bytes = HTTPUtils.downloadFile(url,L -> {
                                pb.stepTo(L);
                            },L -> {
                                pb.maxHint(L);
                            });
                            FileUtils.writeFile(lib,bytes);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (bytes != null){
                        break;
                    }
                }
                if (bytes == null){
                    logger.error("Download maven library error: "+mavenLib);
                    continue;
                }
            }
            libFiles.add(lib.getAbsolutePath());
        }
        return libFiles;
    }
    private boolean hasImportantConfig(){
        String[] importantConfig = new String[]{"input","output"};
        for (String s : importantConfig) {
            if (!config.containsKey(s)){
                logger.error("Config file missing important config: "+s);
                return false;
            }
        }
        return true;
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
        config = new String(config.getBytes(Charset.forName("GBK")), StandardCharsets.UTF_8);
        Map<Object,Object> configMap = new Yaml().load(config);
        if (configMap == null){
            logger.error("Config file is empty");
            return false;
        }
        this.config = ConfigTree.toTree(configMap);
        return true;
    }
    public ClassTree getTree(String ref) {
        if (!hierarchy.containsKey(ref)) {
            ClassWrapper wrapper = getClasspathWrapper(ref);
            buildHierarchy(wrapper, null);
        }

        return hierarchy.get(ref);
    }

    private void buildHierarchy(ClassWrapper wrapper, ClassWrapper sub) {
        if (hierarchy.get(wrapper.getClassInternalName()) == null) {
            ClassTree tree = new ClassTree(wrapper);

            if (wrapper.getSuperName() != null) {
                tree.getParentClasses().add(wrapper.getSuperName());

                buildHierarchy(getClasspathWrapper(wrapper.getSuperName()), wrapper);
            }
            if (wrapper.getInterfaces() != null)
                wrapper.getInterfaces().forEach(s -> {
                    tree.getParentClasses().add(s);

                    buildHierarchy(getClasspathWrapper(s), wrapper);
                });

            hierarchy.put(wrapper.getClassInternalName(), tree);
        }

        if (sub != null)
            hierarchy.get(wrapper.getClassInternalName()).getSubClasses().add(sub.getClassInternalName());
    }
    public boolean isAssignableFrom(String type1, String type2) throws Exception {
        if ("java/lang/Object".equals(type1))
            return true;
        if (type1.equals(type2))
            return true;

        getClasspathWrapper(type1);
        getClasspathWrapper(type2);

        ClassTree firstTree = getTree(type1);
        if (firstTree == null)
            throw new Exception("Could not find " + type1 + " in the built class hierarchy");

        Set<String> allChildren = new HashSet<>();
        Deque<String> toProcess = new ArrayDeque<>(firstTree.getSubClasses());
        while (!toProcess.isEmpty()) {
            String s = toProcess.poll();

            if (allChildren.add(s)) {
                getClasspathWrapper(s);
                ClassTree tempTree = getTree(s);
                toProcess.addAll(tempTree.getSubClasses());
            }
        }
        return allChildren.contains(type2);
    }

}
