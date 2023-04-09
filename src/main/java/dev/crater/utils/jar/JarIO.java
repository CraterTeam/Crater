package dev.crater.utils.jar;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarIO {
    public static Map<String,byte[]> readJar(File file){
        Map<String,byte[]> map = new HashMap<>();
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null){
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while (zis.available() != 0){
                    bos.write(zis.read());
                }
                map.put(entry.getName(),bos.toByteArray());
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
