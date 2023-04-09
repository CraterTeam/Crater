package dev.crater.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class HTTPUtils {
    public static String parseMavenURL(String mvnRepo,String mvnPackage){
        String[] split = mvnPackage.split(":");
        String group = split[0];
        String artifact = split[1];
        String version = split[2];
        String url = null;
        if (mvnRepo.endsWith("/")){
            url = mvnRepo+group.replace(".","/")+"/"+artifact+"/"+version+"/"+artifact+"-"+version+".jar";
        }else {
            url = mvnRepo+"/"+group.replace(".","/")+"/"+artifact+"/"+version+"/"+artifact+"-"+version+".jar";
        }
        return url;
    }
    public static byte[] downloadFile(String urlString, Consumer<Long> progress, Consumer<Long> max) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        long completeFileSize = httpConnection.getContentLength();
        max.accept(completeFileSize);
        BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        long downloadedFileSize = 0;
        int x = 0;
        while ((x = in.read(data, 0, 1024)) >= 0) {
            downloadedFileSize += x;
            // update progress bar
            progress.accept(downloadedFileSize);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            bos.write(data, 0, x);
        }
        bos.close();
        in.close();
        return bos.toByteArray();
    }
    public static byte[] downloadFile(String urlString, Consumer<Integer> progress) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        long completeFileSize = httpConnection.getContentLength();

        BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        long downloadedFileSize = 0;
        int x = 0;
        while ((x = in.read(data, 0, 1024)) >= 0) {
            downloadedFileSize += x;

            // calculate progress
            final int currentProgress = (int) ((((double)downloadedFileSize) / ((double)completeFileSize)) * 100000d);

            // update progress bar
            progress.accept(currentProgress);
            bos.write(data, 0, x);
        }
        bos.close();
        in.close();
        return bos.toByteArray();
    }
}
