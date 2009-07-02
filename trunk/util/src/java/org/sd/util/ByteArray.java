/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.util;


import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * A dynamic random access array of bytes.
 * <p>
 * @author Spence Koehler
 */
public class ByteArray {

	private LinkedList<Byte> bytes;

	/**
	 * Construct an empty instance.
	 */
	public ByteArray() {
		this.bytes = new LinkedList<Byte>();
	}

	/**
	 * Construct an instance with the given bytes.
	 */
	public ByteArray(byte[] bytes) {
		this.bytes = new LinkedList<Byte>();
		for (byte b : bytes) this.bytes.add(b);
	}

	/**
	 * Get the number of bytes stored in this array.
	 */
	public int size() {
		return bytes.size();
	}

	/**
	 * Get all of this array's bytes.
	 */
	public byte[] getBytes() {
		return toArray(bytes);
	}

	/**
	 * Get 'length' bytes starting from startIndex.
	 */
	public byte[] getBytes(int startIndex, int length) {
		final List<Byte> subList = bytes.subList(startIndex, startIndex + length);
		return toArray(subList);
	}

	/**
	 * Read the bytes at the given index as an int.
	 */
	public int getInt(int index) {
		final List<Byte> subList = bytes.subList(index, index + 4);
		return BitUtil.getInteger(toArray(subList), 0);
	}

	/**
	 * Read the bytes at the given index as an long.
	 */
	public long getLong(int index) {
		final List<Byte> subList = bytes.subList(index, index + 8);
		return BitUtil.getLong(toArray(subList), 0);
	}

	/**
	 * Get a data input stream over 'length' bytes from 'startIndex'.
	 */
	public DataInputStream getDataInputStream(int startIndex, int length) {
		return new DataInputStream(new ByteArrayInputStream(getBytes(startIndex, length)));
	}

	/**
	 * Add the given byte to the end of this instance.
	 */
	public void addByte(byte b) {
		bytes.add(b);
	}

	/**
	 * Add the given bytes to the end of this instance.
	 */
	public void addBytes(byte[] bytes) {
		for (byte b : bytes) this.bytes.add(b);
	}

	/**
	 * Set this array's bytes from the startIndex with the given bytes,
	 * growing if necessary.
	 */
	public void setBytes(int startIndex, byte[] bytes) {
		setBytes(startIndex, bytes, bytes.length);
	}

	/**
	 * Set this array's bytes from the startIndex with length of the bytes from
	 * the given array.
	 */
	public void setBytes(int startIndex, byte[] bytes, int length) {
		if (startIndex >= this.bytes.size()) {  // all bytes are being set beyond current length
			for (int i = this.bytes.size(); i < startIndex; ++i) addByte((byte)0);  // pad up until startIndex
			addBytes(bytes);  // add the bytes at the startIndex
		}
		else {  // there is overlap
			int endIndex = startIndex + length;
			int bytesEnd = length;
			if (endIndex > this.bytes.size()) {  // some bytes are being set beyond current length
				final int numExtra = endIndex - this.bytes.size();
				bytesEnd = length - numExtra;
				// add the bytes after endIndex
				for (int i = bytesEnd; i < length; ++i) addByte(bytes[i]);
				endIndex = this.bytes.size();
			}		
			final List<Byte> subList = this.bytes.subList(startIndex, endIndex);
			for (int i = 0; i < bytesEnd; ++i) subList.set(i, bytes[i]);
		}
	}

	/**
	 * Remove the numBytes bytes starting from startIndex, shifting remaining
	 * bytes into their position.
	 */
	public void removeBytes(int startIndex, int numBytes) {
		this.bytes.subList(startIndex, startIndex + numBytes).clear();
	}

	/**
	 * Convert the list of Bytes into a byte array.
	 */
	private final byte[] toArray(List<Byte> bytes) {
		final byte[] result = new byte[bytes.size()];
		int index = 0;
		for (Byte b : bytes) {
			result[index++] = b;
		}
		return result;
	}

	/**
	 * Write this instance's bytes to the data output.
	 */
	public void write(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(bytes.size());
		for (Byte b : bytes) {
			dataOutput.writeByte(b);
		}
	}

	/**
	 * Read the (integer) count and count bytes from the data input, adding to
	 * the end of this instance.
	 * <p>
	 * If there will not be enough memory to read the bytes, then they will not
	 * be read and the return value will be the negative of the count.
	 *
	 * @return the number of bytes read.
	 */
	public int read(DataInput dataInput) throws IOException {
		int count = dataInput.readInt();

		final long availableBytes = Runtime.getRuntime().freeMemory();
		if (availableBytes > count) {
			for (int i = 0; i < count; ++i) {
				addByte(dataInput.readByte());
			}
		}
		else {
			// not enough memory. report the magnitude of the bytes.
			count = -count;
		}

		return count;
	}
}
