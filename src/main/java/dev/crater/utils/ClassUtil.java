package dev.crater.utils;

public class ClassUtil {
    public static String byInternalName(String internalName){
        return internalName.replace("/",".");
    }
    @Deprecated
    public static boolean hasClass(String className){
        System.out.println(className);
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
}
