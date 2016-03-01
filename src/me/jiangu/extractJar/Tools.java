package me.jiangu.extractJar;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * jiangyukun on 2016/3/1.
 */
public class Tools {


    public static void makeSupDir(String outFileName) {
        Pattern p = Pattern.compile("[/\\" + File.separator + "]");
        Matcher m = p.matcher(outFileName);
        while (m.find()) {
            int index = m.start();
            String subDir = outFileName.substring(0, index);
            File subDirFile = new File(subDir);
            if (!subDirFile.exists()) {
                subDirFile.mkdir();
            }
        }
    }

    public static void writeFile(File inputFile, File outputFile) throws IOException {
        writeFile(new FileInputStream(inputFile), outputFile);
    }

    public static void writeFile(InputStream ips, File outputFile) throws IOException {
        OutputStream ops = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            byte[] buffer = new byte[1024];
            int nBytes = 0;
            while ((nBytes = ips.read(buffer)) > 0) {
                ops.write(buffer, 0, nBytes);
            }
        } finally {
            try {
                ops.flush();
                ops.close();
            } finally {
                if (null != ips) {
                    ips.close();
                }
            }
        }
    }
}
