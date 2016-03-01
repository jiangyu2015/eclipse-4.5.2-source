package me.jiangu.extractJar;

import java.io.File;
import java.io.IOException;

/**
 * jiangyukun on 2016/3/1.
 * 合并Java工具类
 */
public class MergeJavaClass {
    public static final String JAVA_ROOT_DIR = "C:/Users/jiangyukun/Desktop/dist/";
    public static final String DIST_DIR = "D:/2016/2016Projects/company/eclipse-4.5.2-source/src/";

    public static void main(String[] args) throws IOException {
        File root = new File(JAVA_ROOT_DIR);
        new MergeJavaClass().traverseDir(root);
    }

    private void traverseDir(File file) throws IOException {
        if (file.isFile()) {
            copyJavaFile(file);
        } else if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                for (File sub : subFiles) {
                    traverseDir(sub);
                }
            }
        }
    }

    public void copyJavaFile(File file) throws IOException {
        String sourcePath = file.getAbsolutePath();
        if (!sourcePath.contains(".java")) {
            return;
        }
        String dist = getFileDistPath(sourcePath);
        if (dist.equals("")) {
//            System.out.println(sourcePath);
            return;
        }
        String distPath = DIST_DIR + dist;
        System.out.println(sourcePath);
        System.out.println(" -- " + distPath);
        Tools.makeSupDir(distPath);
        Tools.writeFile(file, new File(distPath));
    }

    private String getFileDistPath(String sourcePath) {
        String separator = File.separator;
//        int index = sourcePath.indexOf(separator + "com" + separator + "w3c" + separator);
        int index = sourcePath.indexOf(separator + "com" + separator);
        if (index != -1) {
            return sourcePath.substring(index);
        }
        return "";
    }
}
