package dev.crater.utils.jar;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarIO {
    public static Map<String,byte[]> readJar(File file){
        Map<String,byte[]> map = new HashMap<>();
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null){
                if (entry.isDirectory()) continue;
                int len;
                byte[] buffer = new byte[2048];
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while ((len = zis.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
                System.out.println(entry.getName()+" "+bos.toByteArray().length);
                map.put(entry.getName(),bos.toByteArray());
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
