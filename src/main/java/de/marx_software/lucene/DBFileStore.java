/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.marx_software.lucene;

import java.io.IOException;
import java.util.Set;

/**
 *
 * @author marx
 */
public interface DBFileStore {

	void append(String name, byte[] buf, int offset, int len) throws IOException;

	void clear() throws IOException;

	void close() throws IOException;

	boolean contains(String key) throws IOException;

	/**
	 * @param key
	 * @return not exist return -1
	 */
	long getSize(String key) throws IOException;

	Set<String> listKey();

	int load(String name, long position, byte[] buf, int offset, int len) throws IOException;

	void move(String source, String dest) throws IOException;

	void remove(String key) throws IOException;

	void sync() throws IOException;
	
}
