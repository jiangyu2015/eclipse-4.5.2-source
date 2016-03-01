package me.jiangu.extractJar;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * jiangyukun on 2016/3/1.
 * 将eclipse源码从多个jar文件中合并
 */
public class Extractor {
    public static final String SOURCE_ROOT_DIR = "C:\\Users\\jiangyukun\\Desktop\\eclipse-source-jar\\";
//    public static final String SOURCE_ROOT_DIR = "C:\\Users\\jiangyukun\\Desktop\\zz\\";


    public static void main(String[] args) throws IOException {
        File rootDir = new File(SOURCE_ROOT_DIR);
        new Extractor().doExtract(rootDir.listFiles());
    }

    public void doExtract(File[] jarList) throws IOException {
        for (File jar : jarList) {
            if (jar.exists() && jar.getAbsolutePath().endsWith(".jar")) {
                extractSingleJar(jar);
            }
        }
    }

    public void extractSingleJar(File jar) throws IOException {
        String jarPath = jar.getAbsolutePath();


//        String dirPath = jar.getParent();
        /*String rarCommand = "rar x " + jarPath + " " + dirPath;
        System.out.println(rarCommand);
        Runtime.getRuntime().exec(rarCommand);*/

        unZipFile(jarPath);
    }

    public static void unZipFile(String jarPath) throws IOException {
        String rootPath = new File(jarPath).getAbsolutePath();
        String jarDir = rootPath.substring(0, rootPath.length() - 4) + File.separator;

        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> jarEntrys = jarFile.entries();
        while (jarEntrys.hasMoreElements()) {
            JarEntry jarEntry = jarEntrys.nextElement();
            jarEntry.getName();
            String outFileName = jarDir + jarEntry.getName();
            File f = new File(outFileName);
            Tools.makeSupDir(outFileName);
            if (jarEntry.isDirectory()) {
                continue;
            }
            Tools.writeFile(jarFile.getInputStream(jarEntry), f);
        }
    }

}
