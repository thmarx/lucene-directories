/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.marx_software.lucene;

/**
 *
 * @author marx
 */
public abstract class BaseDBFileStore implements DBFileStore {

	protected long readLong(byte[] bytes) {
		return ((long) bytes[0] << 56)
				+ ((long) (bytes[1] & 255) << 48)
				+ ((long) (bytes[2] & 255) << 40)
				+ ((long) (bytes[3] & 255) << 32)
				+ ((long) (bytes[4] & 255) << 24)
				+ ((bytes[5] & 255) << 16)
				+ ((bytes[6] & 255) << 8)
				+ ((bytes[7] & 255) << 0);
	}

	protected byte[] longToBytes(long size) {
		return new byte[]{
			(byte) (size >>> 56),
			(byte) (size >>> 48),
			(byte) (size >>> 40),
			(byte) (size >>> 32),
			(byte) (size >>> 24),
			(byte) (size >>> 16),
			(byte) (size >>> 8),
			(byte) (size >>> 0)

		};
	}
}
