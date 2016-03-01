package me.jiangu.extractJar;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * jiangyukun on 2016/3/1.
 */
public class ToolsTest {
    @Test
    public void testMakeSupDir() {
        String dirPath = Extractor.SOURCE_ROOT_DIR + "test/";
        File newDir = new File(dirPath);
        if (newDir.exists()) {
            newDir.delete();
        }
        Tools.makeSupDir(dirPath);

        assertTrue(newDir.exists());
        newDir.delete();
    }

    @Test
    public void testDoExtract() throws Exception {

    }

    @Test
    public void testExtractSingleJar() throws Exception {

    }

    @Test
    public void testUnZipFile() throws Exception {
        Extractor.unZipFile(Extractor.SOURCE_ROOT_DIR + "com.ibm.icu.source_54.1.1.v201501272100.jar");
    }

    @Test
    public void testWriteFile() throws Exception {

    }

    @Test
    public void testWriteFile1() throws Exception {

    }
}