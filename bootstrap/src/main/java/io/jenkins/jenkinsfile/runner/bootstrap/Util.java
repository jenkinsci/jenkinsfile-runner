package io.jenkins.jenkinsfile.runner.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Util {

    public static File explodeWar(String jarPath) throws IOException {
        JarFile jarfile = new java.util.jar.JarFile(new java.io.File(jarPath));
        Enumeration<java.util.jar.JarEntry> enu= jarfile.entries();

        //Creating working directory
        String currentDirectory = System.getProperty("user.dir");
        File destDir = new File(currentDirectory + "/work");
        //Creating the directory
        if(destDir.mkdir()) {
            System.out.println("Work directory has been created successfully at" + destDir.getAbsolutePath());
        } else {
            System.out.println("Failed to created directory");
        }

        while(enu.hasMoreElements()) {
            JarEntry je = enu.nextElement();
            File file = new File(destDir, je.getName());
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                file = new File(destDir, je.getName());
            }
            if(je.isDirectory()) {
                continue;
            }
            InputStream is = jarfile.getInputStream(je);
            FileOutputStream fo = new FileOutputStream(file);
            while(is.available()>0) {
                fo.write(is.read());
            }
            fo.close();
            is.close();
        }

        return destDir;
    }
}
