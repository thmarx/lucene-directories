package de.marx_software.lucene.rocksdb;

import de.marx_software.lucene.BaseDBFileStore;
import de.marx_software.lucene.DBFileStore;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import org.rocksdb.*;

/**
 * Created by wens on 16-3-10.
 */
public class RocksDBFileStore extends BaseDBFileStore {

	private static final int BLOCK_SIZE = 10 * 1024;

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final RocksDB storeDb;
	private ColumnFamilyHandle dataHandle;
	private ColumnFamilyHandle metaHandle;

	
	public RocksDBFileStore(Path path) throws IOException {
		Options options = new Options();
		options.setCreateIfMissing(true);
		options.setCompressionType(CompressionType.SNAPPY_COMPRESSION);
		File data = new File(path.toFile(), "_data");
		
		if (!data.exists()) {
			data.mkdirs();
		}
		try {
			storeDb = RocksDB.open(options, data.getAbsolutePath());
			
			ColumnFamilyDescriptor metaCol = new ColumnFamilyDescriptor("meta".getBytes());
			ColumnFamilyDescriptor dataCol = new ColumnFamilyDescriptor("data".getBytes());
			
			dataHandle = storeDb.createColumnFamily(dataCol);
			metaHandle = storeDb.createColumnFamily(metaCol);
			
		} catch (RocksDBException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public boolean contains(String key) throws IOException {
		lock.readLock().lock();
		try {
			byte[] key0 = key.getBytes();
			byte[] bytes = storeDb.get(metaHandle, key0);
			return bytes != null;
		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);

			throw new IOException(ex);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public int load(String name, long position, byte[] buf, int offset, int len) throws IOException {

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

				byte[] bb = storeDb.get(dataHandle, (name + "_" + i).getBytes());

				System.arraycopy(bb, m, buf, f, r);

				p += r;
				f += r;
				n -= r;

				if (n == 0 || p >= size) {
					break;
				}
			}

			return (int) (p - position);

		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);

			throw new IOException(ex);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @param key
	 * @return not exist return -1
	 */
	@Override
	public long getSize(String key) throws IOException {
		lock.readLock().lock();
		try {
			byte[] key0 = key.getBytes();
			byte[] bytes = storeDb.get(metaHandle, key0);
			if (bytes != null) {
				return readLong(bytes);
			}
		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);

			throw new IOException(ex);
		} finally {
			lock.readLock().unlock();
		}
		return -1;
	}

	

	@Override
	public void remove(String key) throws IOException {
		lock.writeLock().lock();
		try {
			byte[] key0 = key.getBytes();
			long size = getSize(key);

			if (size == -1) {
				return;
			}
			int n = (int) ((size + BLOCK_SIZE - 1) / BLOCK_SIZE);
			for (int i = 0; i < n; i++) {
				storeDb.delete(dataHandle, (key + "_" + i).getBytes());
			}
			storeDb.delete(metaHandle, key0);

		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);
			throw new IOException(ex);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void clear() throws IOException {

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
			try ( RocksIterator iterator = storeDb.newIterator(metaHandle);) {
				iterator.seekToFirst();
				while (iterator.isValid()) {
					keys.add(new String(iterator.key()).intern());
					iterator.next();
				}
			}
		} finally {
			lock.readLock().unlock();
		}

		return keys;
	}

	@Override
	public void append(String name, byte[] buf, int offset, int len) throws IOException {

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
					bb = storeDb.get(dataHandle, (name + "_" + i).getBytes());
				}

				System.arraycopy(buf, f, bb, m, r);
				storeDb.put(dataHandle, (name + "_" + i).getBytes(), bb);
				size += r;
				f += r;
				n -= r;

				if (n == 0) {
					break;
				}
			}

			storeDb.put(metaHandle, name.getBytes(), longToBytes(size));

		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);

			throw new IOException(ex);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void move(String source, String dest) throws IOException {

		lock.writeLock().lock();
		try {

			long s_size = getSize(source);
			storeDb.put(metaHandle, dest.getBytes(), longToBytes(s_size));

			int n = (int) ((s_size + BLOCK_SIZE - 1) / BLOCK_SIZE);

			for (int i = 0; i < n; i++) {
				storeDb.put(dataHandle, (dest + "_" + i).getBytes(), storeDb.get(dataHandle, (source + "_" + i).getBytes()));
			}
			remove(source);

		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);
			throw new IOException(ex);
		} finally {
			lock.writeLock().unlock();
		}

	}

	@Override
	public void close() throws IOException {
		try {
			storeDb.close();
		} finally {
		}
	}

	@Override
	public void sync() throws IOException {
		try {
			storeDb.compactRange();
		} catch (RocksDBException ex) {
			java.util.logging.Logger.getLogger(RocksDBFileStore.class.getName()).log(Level.SEVERE, null, ex);
			
			throw new IOException(ex);
		}
	}
}
