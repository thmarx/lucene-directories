package de.marx_software.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import java.io.*;

/**
 * Created by wens on 16-3-10.
 */
public class TestUtils {


    public static String readTextForFile(File file) throws IOException {

        InputStream inputStream = new FileInputStream(file);


        ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());

        try {
            byte[] buf = new byte[1024];
            while (true) {
                int read = inputStream.read(buf);
                if (read != -1) {
                    out.write(buf, 0, read);
                }
                break;
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (out != null) {
                out.close();
            }
        }

        return out.toString("utf-8");
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static void indexTextFile(IndexWriter writer, File resourceDir) throws IOException {

        for (File file : resourceDir.listFiles()) {
            if (file.isFile()) {
                String fileName = file.getName();
                String content = TestUtils.readTextForFile(file);
                Document doc = new Document();
                doc.add(new StringField("fileName", fileName, Field.Store.YES));
                doc.add(new TextField("content", content, Field.Store.YES));
                writer.addDocument(doc);
            } else {
                indexTextFile(writer, file);
            }
        }
    }

}
