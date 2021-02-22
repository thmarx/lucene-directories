package de.marx_software.lucene.leveldb;

import de.marx_software.lucene.BaseDBFileStore;
import de.marx_software.lucene.DBFileStore;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.iq80.leveldb.CompressionType;

/**
 * Created by wens on 16-3-10.
 */
public class LeveldbFileStore extends BaseDBFileStore {

    private static final int BLOCK_SIZE = 10 * 1024;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final DB metaDb;
    private final DB dataDb;

    public LeveldbFileStore(Path path) throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
		options.compressionType(CompressionType.SNAPPY);
        File meta = new File(path.toFile(), "_meta");
        File data = new File(path.toFile(), "_data");
        if (!meta.exists()) {
            meta.mkdirs();
        }
        if (!data.exists()) {
            data.mkdirs();
        }
        metaDb = Iq80DBFactory.factory.open(meta, options);
        dataDb = Iq80DBFactory.factory.open(data, options);
    }


	@Override
    public boolean contains(String key) {
        lock.readLock().lock();
        try {
            byte[] key0 = key.getBytes();
            byte[] bytes = metaDb.get(key0);
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


                byte[] bb = dataDb.get((name + "_" + i).getBytes());

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
            byte[] key0 = key.getBytes();
            byte[] bytes = metaDb.get(key0);
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
            byte[] key0 = key.getBytes();
            long size = getSize(key);

            if (size == -1) {
                return;
            }
            int n = (int) ((size + BLOCK_SIZE - 1) / BLOCK_SIZE);
            for (int i = 0; i < n; i++) {
                dataDb.delete((key + "_" + i).getBytes());
            }
            metaDb.delete(key0);

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
            DBIterator iterator = metaDb.iterator();
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                Map.Entry<byte[], byte[]> entry = iterator.next();
                keys.add(new String(entry.getKey()).intern());
            }
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
                dataDb.put((name + "_" + i).getBytes(), bb);
                size += r;
                f += r;
                n -= r;

                if (n == 0) {
                    break;
                }
            }

            metaDb.put(name.getBytes(), longToBytes(size));

        } finally {
            lock.writeLock().unlock();
        }
    }

	@Override
    public void move(String source, String dest) {

        lock.writeLock().lock();
        try {

            long s_size = getSize(source);
            metaDb.put(dest.getBytes(), longToBytes(s_size));

            int n = (int) ((s_size + BLOCK_SIZE - 1) / BLOCK_SIZE);

            for (int i = 0; i < n; i++) {
                dataDb.put((dest + "_" + i).getBytes(), dataDb.get((source + "_" + i).getBytes()));
            }
            remove(source);

        } finally {
            lock.writeLock().unlock();
        }

    }

	@Override
    public void close() throws IOException {
        try {
            metaDb.close();
        } finally {
            dataDb.close();
        }
    }
	
	@Override
	public void sync () {
		metaDb.compactRange(null, null);
		dataDb.compactRange(null, null);
	}
}
