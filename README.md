# Lucene directory implementations

This project started as a fork of [lucene-leveldb](https://github.com/wenzuojing/lucene-leveldb). 
Now it's heavily refactored and offers different lucene directory implementations.

## MVStore
Lucene directory that uses MVStore as storage.

[MVStore](https://www.h2database.com/html/mvstore.html)

## RocksDB
Lucene directory that uses Facebooks RocksDB as storage.

[RocksDB](https://github.com/facebook/rocksdb)

## lucene-leveldb
Lucene directory that uses Googles Leveldb as storage.

[Leveldb 1.2](https://github.com/pcmind/leveldb)

Requirements：

* Java 11+
* Lucene 8.0+

## Example


```java
        Path path = Paths.get("db-data");

        File indexDir = path.toFile();

        Directory directory = DBDirectories.leveldb(path);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writer =
                new IndexWriter(directory, indexWriterConfig);

        directory.close();

```
