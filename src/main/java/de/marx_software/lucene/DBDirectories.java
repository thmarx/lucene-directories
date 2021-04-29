/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.marx_software.lucene;

import de.marx_software.lucene.leveldb.LeveldbFileStore;
import de.marx_software.lucene.mvstore.MVStoreFileStore;
import de.marx_software.lucene.rocksdb.RocksDBFileStore;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * @author marx
 */
public class DBDirectories {
	
	
	public static DBDirectory rocket (final Path path) throws IOException {
		DBFileStore fileStore = new RocksDBFileStore(path);
		return new DBDirectory(fileStore);
	}
	public static DBDirectory leveldb (final Path path) throws IOException {
		DBFileStore fileStore = new LeveldbFileStore(path);
		return new DBDirectory(fileStore);
	}
	public static DBDirectory mvstore (final Path path) throws IOException {
		DBFileStore fileStore = new MVStoreFileStore(path);
		return new DBDirectory(fileStore);
	}
}
