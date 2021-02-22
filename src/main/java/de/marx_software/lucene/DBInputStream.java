package de.marx_software.lucene;

import de.marx_software.lucene.rocksdb.*;
import org.apache.lucene.store.IndexInput;

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by wens on 16-3-10.
 */
public class DBInputStream extends IndexInput {

	private final int bufferSize;

	private long position;

	private final long length;

	private byte[] currentBuffer;

	private int currentBufferIndex;

	private final DBFileStore store;

	private final String name;

	public DBInputStream(String name, DBFileStore store, int bufferSize) throws IOException {
		this(name, store, bufferSize, store.getSize(name));
	}

	public DBInputStream(String name, DBFileStore store, int bufferSize, long length) {
		super("DBInputStream(name=" + name + ")");
		this.name = name;
		this.store = store;
		this.bufferSize = bufferSize;
		this.currentBuffer = new byte[this.bufferSize];
		this.currentBufferIndex = bufferSize;
		this.position = 0;
		this.length = length;

	}

	@Override
	public void close() throws IOException {
		//store.close();
	}

	@Override
	public long getFilePointer() {
		return position;
	}

	@Override
	public void seek(long pos) throws IOException {
		if (pos < 0 || pos > length) {
			throw new IllegalArgumentException("pos must be between 0 and " + length);
		}
		position = pos;
		currentBufferIndex = this.bufferSize;
	}

	@Override
	public long length() {
		return this.length;
	}

	@Override
	public IndexInput slice(String sliceDescription, final long offset, final long length) throws IOException {

		if (offset < 0 || length < 0 || offset + length > this.length) {
			throw new IllegalArgumentException("slice() " + sliceDescription + " out of bounds: " + this);
		}

		return new DBInputStream(name, store, bufferSize, offset + length) {
			{
				seek(0L);
			}

			@Override
			public void seek(long pos) throws IOException {
				if (pos < 0L) {
					throw new IllegalArgumentException("Seeking to negative position: " + this);
				}

				super.seek(pos + offset);
			}

			@Override
			public long getFilePointer() {
				return super.getFilePointer() - offset;
			}

			@Override
			public long length() {
				return super.length() - offset;
			}

			@Override
			public IndexInput slice(String sliceDescription, long ofs, long len) throws IOException {
				return super.slice(sliceDescription, offset + ofs, len);
			}
		};
	}

	@Override
	public byte readByte() throws IOException {

		if (position >= length) {
			throw new EOFException("Read end");
		}
		loadBufferIfNeed();
		byte b = currentBuffer[currentBufferIndex++];
		position++;
		return b;
	}

	protected void loadBufferIfNeed() throws IOException {
		if (this.currentBufferIndex == this.bufferSize) {
			int n = store.load(name, position, currentBuffer, 0, bufferSize);
			if (n == -1) {
				throw new EOFException("Read end");
			}
			this.currentBufferIndex = 0;
		}
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {

		if (position >= length) {
			throw new EOFException("Read end");
		}

		int f = offset;
		int n = Math.min((int) (length - position), len);
		while (true) {
			loadBufferIfNeed();

			int r = Math.min(bufferSize - currentBufferIndex, n);

			System.arraycopy(currentBuffer, currentBufferIndex, b, f, r);

			f += r;
			position += r;
			currentBufferIndex += r;
			n -= r;
			if (n == 0) {
				break;
			}

		}
	}

	@Override
	public IndexInput clone() {
		DBInputStream in = (DBInputStream) super.clone();
		in.currentBuffer = new byte[bufferSize];
		System.arraycopy(this.currentBuffer, 0, in.currentBuffer, 0, bufferSize);
		return in;
	}
}
