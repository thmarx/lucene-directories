package de.marx_software.lucene;

import de.marx_software.lucene.rocksdb.*;
import org.apache.lucene.store.BufferedChecksum;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.Accountable;

import java.io.IOException;
import java.util.Collection;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by wens on 16-3-10.
 */
public class DBOutputStream extends IndexOutput implements Accountable {

	private int bufferSize;

	private long position;

	private byte[] currentBuffer;

	private int currentBufferIndex;

	private boolean dirty;

	private final Checksum crc;

	private final DBFileStore store;

	private final String name;

	public DBOutputStream(String name, DBFileStore store, int bufferSize, boolean checksum) {
		super("LeveldbOutputStream(name=" + name + ")", name);
		this.name = name;
		this.store = store;
		this.bufferSize = bufferSize;
		this.currentBuffer = new byte[this.bufferSize];
		this.position = 0;
		this.dirty = false;
		if (checksum) {
			crc = new BufferedChecksum(new CRC32());
		} else {
			crc = null;
		}
	}

	@Override
	public void close() throws IOException {
		if (dirty) {
			flush();
		}
		//store.close();
	}

	private void flush() throws IOException {

		store.append(name, currentBuffer, 0, currentBufferIndex);
		currentBufferIndex = 0;
		dirty = false;
	}

	@Override
	public long getFilePointer() {
		return position;
	}

	@Override
	public long getChecksum() throws IOException {
		if (crc != null) {
			return crc.getValue();
		} else {
			throw new IllegalStateException("crc is null");
		}
	}

	@Override
	public void writeByte(byte b) throws IOException {

		if (crc != null) {
			crc.update(b);
		}
		if (currentBufferIndex == bufferSize) {
			flush();
		}
		currentBuffer[currentBufferIndex++] = b;
		position++;
		dirty = true;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {

		if (crc != null) {
			crc.update(b, offset, length);
		}
		int f = offset;
		int n = length;
		while (true) {
			if (currentBufferIndex == bufferSize) {
				flush();
			}
			int r = Math.min(bufferSize - currentBufferIndex, n);
			System.arraycopy(b, f, currentBuffer, currentBufferIndex, r);
			f += r;
			currentBufferIndex += r;
			position += r;
			n -= r;
			dirty = true;

			if (n == 0) {
				break;
			}
		}
	}

	@Override
	public long ramBytesUsed() {
		return position;
	}

	@Override
	public Collection<Accountable> getChildResources() {
		return null;
	}
}
