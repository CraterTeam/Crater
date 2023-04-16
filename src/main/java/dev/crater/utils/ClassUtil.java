package dev.crater.utils;

import dev.crater.Main;
import dev.crater.utils.jar.ClassWrapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class ClassUtil {
    public static String byInternalName(String internalName){
        return internalName.replace("/",".");
    }
    @Deprecated
    public static boolean hasClass(String className){
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    public static boolean hasClass(String className,ClassLoader classLoader){
        try {
            Class.forName(className,true,classLoader);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Overwrite ClassWriter.getCommonSuperClass
     * @param type1
     * @param type2
     * @return
     */
    public static String getCommonSuperClass(final String type1, final String type2){
        List<ClassWrapper> libraries = Main.INSTANCE.getLibrariesClasses();
        List<ClassWrapper> filtered = Main.INSTANCE.getFilteredClasses();
        List<ClassWrapper> classes = Main.INSTANCE.getClasses();
        List<ClassWrapper> merge = new ArrayList<>();
        merge.addAll(libraries);
        merge.addAll(filtered);
        merge.addAll(classes);
        Object type1Class = null;
        Object type2Class = null;
        ClassLoader classLoader = ClassUtil.class.getClassLoader();
        try {
            type1Class = Class.forName(type1.replace('/', '.'), false, classLoader);
        } catch (ClassNotFoundException e) {
            type1Class = matchClass(type1,merge);
            if (type1Class == null){
                throw new TypeNotPresentException(type1, e);
            }
        }
        try {
            type2Class = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (ClassNotFoundException e) {
            type2Class = matchClass(type2,merge);
            if (type2Class == null){
                throw new TypeNotPresentException(type2, e);
            }
        }
        if (isAssignableFrom(type1Class,type2Class,merge)) {
            return type1;
        }
        if (isAssignableFrom(type2Class,type1Class,merge)) {
            return type2;
        }
        if (isInterface(type1Class) || isInterface(type2Class)) {
            return "java/lang/Object";
        } else {
            do {
                type1Class = getSuperclass(type1Class,merge);
            } while (!isAssignableFrom(type1Class,type2Class,merge));
            if (type1Class instanceof Class<?>){
                return Type.getInternalName((Class<?>) type1Class);
            }else {
                return ((ClassWrapper)type1Class).getClassInternalName();
            }
        }
    }
    private static Object getSuperclass(Object o,List<ClassWrapper> classWrappers){
        if (o instanceof Class<?>){
            return ((Class<?>) o).getSuperclass();
        }else if (o instanceof ClassWrapper){
            try {
                Class clazz = Class.forName(((ClassWrapper) o).getClassNode().superName.replace("/","."));
                return clazz;
            } catch (ClassNotFoundException e) {
                return matchClass(((ClassWrapper) o).getClassNode().superName,classWrappers);
            }
        }
        return null;
    }
    private static boolean isInterface(Object o){
        if (o instanceof Class<?>){
            return ((Class<?>) o).isInterface();
        }else if (o instanceof ClassWrapper){
            return (((ClassWrapper)o).getClassNode().access & Opcodes.ACC_INTERFACE) != 0;
        }
        return false;
    }
    private static ClassWrapper matchClass(String type,List<ClassWrapper> classWrappers){
        for (ClassWrapper classWrapper : classWrappers) {
            if (classWrapper.getClassInternalName().equals(type)){
                return classWrapper;
            }
        }
        return null;
    }
    private static boolean isAssignableFrom(Object o1,Object o2,List<ClassWrapper> classWrappers){
        if (o1 instanceof Class<?> && o2 instanceof Class<?>){
            return ((Class<?>)o1).isAssignableFrom(((Class<?>)o2));
        }else if (o1 instanceof ClassWrapper && o2 instanceof ClassWrapper){
            return getInheritTree((ClassWrapper) o2,classWrappers).contains(((ClassWrapper) o1).getClassInternalName());
        }else if (o1 instanceof Class<?> && o2 instanceof ClassWrapper){
            return getInheritTree((ClassWrapper) o2,classWrappers).contains(Type.getInternalName((Class<?>) o1));
        }else if (o1 instanceof ClassWrapper && o2 instanceof Class<?>){
            return getInheritTree((Class<?>) o2).contains(((ClassWrapper) o1).getClassInternalName());
        }
        return false;
    }
    public static List<String> getInheritTree(Class<?> clazz){
        List<String> inherit = new ArrayList<>();
        inherit.add(Type.getInternalName(clazz));
        for (Class<?> anInterface : clazz.getInterfaces()) {
            inherit.add(Type.getInternalName(anInterface));
        }
        String parent = Type.getInternalName(clazz);
        while (!parent.equals("java/lang/Object")){
            try {
                Class clazz2 = Class.forName(parent.replace("/","."));
                Class superClass = clazz2.getSuperclass();
                parent = Type.getInternalName(superClass);
                inherit.add(parent);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return inherit;
    }
    public static List<String> getInheritTree(ClassWrapper cw,List<ClassWrapper> classWrappers){
        List<String> inherit = new ArrayList<>();
        inherit.add(cw.getClassInternalName());
        for (String anInterface : cw.getClassNode().interfaces) {
            inherit.add(anInterface);
        }
        String parent = cw.getClassNode().superName;
        while (!parent.equals("java/lang/Object")){
            ClassWrapper cw2 = matchClass(parent,classWrappers);
            if (cw2 != null){
                parent = cw2.getClassNode().superName;
                inherit.add(parent);
                continue;
            }
            try {
                Class clazz = Class.forName(parent.replace("/","."));
                parent = Type.getInternalName(clazz.getSuperclass());
                inherit.add(parent);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return inherit;
    }
}
