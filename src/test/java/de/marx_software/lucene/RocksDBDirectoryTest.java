package de.marx_software.lucene;

import de.marx_software.lucene.DBDirectories;
import de.marx_software.lucene.DBDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.rocksdb.RocksDBException;

/**
 * Created by wens on 16-3-11.
 */
public class RocksDBDirectoryTest {

    public static void main(String[] args) throws IOException, ParseException, RocksDBException {

        Path path = Paths.get("rocksdb-data");

        File indexDir = path.toFile();

        if (indexDir.exists()) {
            TestUtils.deleteDir(indexDir);
        }

        Directory directory = DBDirectories.rocket(path);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writer =
                new IndexWriter(directory, indexWriterConfig);


        File resourceDir = new File(TestUtils.class.getResource("/test-data-set").getPath());

        Long startTime = System.currentTimeMillis();
        TestUtils.indexTextFile(writer, resourceDir);
        writer.close();
        System.out.println("Index speed time : " + (System.currentTimeMillis() - startTime));

        DirectoryReader index = DirectoryReader.open(directory);

        IndexSearcher searcher = new IndexSearcher(index);

        Query query = new QueryParser("content", analyzer).parse("good");

        TopScoreDocCollector collector = TopScoreDocCollector.create(100, 100);

        startTime = System.currentTimeMillis();
        searcher.search(query, collector);
        System.out.println("Search speed time : " + (System.currentTimeMillis() - startTime));
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            //System.out.println((i + 1) + ". " + d.get("fileName") + " score=" + hits[i].score);
        }

        directory.close();


    }
}
