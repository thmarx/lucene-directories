package de.marx_software.lucene;


import de.marx_software.lucene.leveldb.LeveldbFileStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by wens on 16-3-10.
 */
public class LeveldbFileStoreTest {

    private byte[] bb = "abcefghijklmnopqrstuvwxyz1234567890".getBytes();
    private LeveldbFileStore store;

    private File tmpFile;

    @Before
    public void setUp() throws IOException {
        tmpFile = new File("target/test-store");
        if (tmpFile.exists()) {
            TestUtils.deleteDir(tmpFile);
        }
        store = new LeveldbFileStore(tmpFile.toPath());
    }

    @After
    public void after() throws IOException {

        store.clear();
        store.close();
        if (tmpFile.exists()) {
            TestUtils.deleteDir(tmpFile);
        }


    }

    @org.junit.Test
    public void testAppend() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            store.append("test-1", bb, 0, bb.length);
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    @org.junit.Test
    public void testLoad() {

        for (int i = 0; i < 1000; i++) {
            store.append("test-2", bb, 0, bb.length);
        }

        long p = 0;
        byte[] b = new byte[bb.length];
        while (true) {

            int n = store.load("test-2", p, b, 0, bb.length);

            if (n == -1) {
                break;
            }

            for (int i = 0; i < bb.length; i++) {
                Assert.assertEquals(bb[i], b[i]);
            }

            p += n;
        }
    }


    @Test
    public void testListKey() {
        for (int i = 0; i < 1000; i++) {
            store.append("test-tt-" + i, bb, 0, bb.length);
        }

        Set<String> strings = store.listKey();
        for (int i = 0; i < 1000; i++) {
            Assert.assertTrue(strings.contains("test-tt-" + i));
        }
    }

    @Test
    public void testMove() {

        for (int i = 0; i < 10; i++) {
            store.append("test-3", bb, 0, bb.length);
        }

        store.move("test-3", "test-3-b");

        Assert.assertFalse(store.contains("test-3"));

    }

    @Test
    public void test_1() {

    }
}
