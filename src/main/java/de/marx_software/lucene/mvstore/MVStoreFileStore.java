package de.marx_software.lucene.mvstore;

import de.marx_software.lucene.BaseDBFileStore;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.iq80.leveldb.CompressionType;

/**
 * Created by wens on 16-3-10.
 */
public class MVStoreFileStore extends BaseDBFileStore {

	private static final int BLOCK_SIZE = 10 * 1024;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final MVMap<String, byte[]> metaDb;
	private final MVMap<String, byte[]> dataDb;

	MVStore store;

	public MVStoreFileStore(Path path) throws IOException {

		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}
		store = new MVStore.Builder().
				fileName(path.resolve("directory.db").toString()).
				compress().
				open();

		metaDb = store.openMap("_meta");
		dataDb = store.openMap("_data");
	}

	@Override
	public boolean contains(String key) {
		lock.readLock().lock();
		try {
			byte[] bytes = metaDb.get(key);
			return bytes != null;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int load(String name, long position, byte[] buf, int offset, int len) {

		lock.readLock().lock();
		try {
			long size = getSize(name);

			if (position >= size) {
				return -1;
			}

			if (buf.length < offset + len) {
				throw new IllegalArgumentException("len is too long");
			}

			long p = position;
			int f = offset;
			int n = len;

			while (true) {
				int m = (int) (p % (long) BLOCK_SIZE);
				int r = Math.min(BLOCK_SIZE - m, n);
				int i = (int) (p / (long) BLOCK_SIZE);

				byte[] bb = dataDb.get((name + "_" + i));

				System.arraycopy(bb, m, buf, f, r);

				p += r;
				f += r;
				n -= r;

				if (n == 0 || p >= size) {
					break;
				}
			}

			return (int) (p - position);

		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @param key
	 * @return not exist return -1
	 */
	@Override
	public long getSize(String key) {
		lock.readLock().lock();
		try {
			byte[] bytes = metaDb.get(key);
			if (bytes != null) {
				return readLong(bytes);
			}
		} finally {
			lock.readLock().unlock();
		}
		return -1;
	}

	@Override
	public void remove(String key) {
		lock.writeLock().lock();
		try {
			long size = getSize(key);

			if (size == -1) {
				return;
			}
			int n = (int) ((size + BLOCK_SIZE - 1) / BLOCK_SIZE);
			for (int i = 0; i < n; i++) {
				dataDb.remove((key + "_" + i));
			}
			metaDb.remove(key);

		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void clear() {

		lock.writeLock().lock();
		try {
			Set<String> keySet = listKey();
			for (String key : keySet) {
				remove(key);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Set<String> listKey() {
		Set<String> keys = new HashSet<>();
		lock.readLock().lock();
		try {

			metaDb.keySet().forEach((key) -> {
				keys.add(new String(key).intern());
			});
		} finally {
			lock.readLock().unlock();
		}

		return keys;
	}

	@Override
	public void append(String name, byte[] buf, int offset, int len) {

		lock.writeLock().lock();
		try {

			long size = getSize(name);
			if (size == -1) {
				size = 0;
			}

			int f = offset;
			int n = len;

			while (true) {

				int m = (int) (size % (long) BLOCK_SIZE);
				int r = Math.min(BLOCK_SIZE - m, n);

				byte[] bb;

				int i = (int) ((size) / (long) BLOCK_SIZE);
				if (m == 0) {
					bb = new byte[BLOCK_SIZE];
				} else {
					bb = dataDb.get((name + "_" + i).getBytes());
				}

				System.arraycopy(buf, f, bb, m, r);
				dataDb.put((name + "_" + i), bb);
				size += r;
				f += r;
				n -= r;

				if (n == 0) {
					break;
				}
			}

			metaDb.put(name, longToBytes(size));

		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void move(String source, String dest) {

		lock.writeLock().lock();
		try {

			long s_size = getSize(source);
			metaDb.put(dest, longToBytes(s_size));

			int n = (int) ((s_size + BLOCK_SIZE - 1) / BLOCK_SIZE);

			for (int i = 0; i < n; i++) {
				dataDb.put((dest + "_" + i), dataDb.get((source + "_" + i)));
			}
			remove(source);

		} finally {
			lock.writeLock().unlock();
		}

	}

	@Override
	public void close() throws IOException {
		store.close();
	}

	@Override
	public void sync() {
		store.compactMoveChunks();
	}
}
