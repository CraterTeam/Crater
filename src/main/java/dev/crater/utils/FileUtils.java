package dev.crater.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class FileUtils {
    public static byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[fis.available()];
        fis.read(bytes);
        fis.close();
        return bytes;
    }
    public static byte[] writeFile(File file, byte[] bytes) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
        return bytes;
    }
    public static String parseMavenFile(String mvnPackage){
        String[] split = mvnPackage.split(":");
        String group = split[0];
        String artifact = split[1];
        String version = split[2];
        String url = null;
        url = group.replace(".","/")+"/"+artifact+"/"+version+"/"+artifact+"-"+version+".jar";
        return url;
    }
    public static String renameExistingFile(File existing) {
        try {
            int i = 0;
            while (true) {
                i++;
                String newName = existing.getAbsolutePath() + ".BACKUP-" + i;
                File backUpName = new File(newName);
                if (!backUpName.exists()) {
                    existing.renameTo(backUpName);
                    return newName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException(String.format("Could not backup \"%s\"", existing.getAbsolutePath()));
        }
    }
    public static String getPath(String pathWithFileName){
        String[] split = pathWithFileName.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            sb.append(split[i]).append("/");
        }
        return sb.toString();
    }
    public static String parseMavenPath(){
        return null;
    }
}
