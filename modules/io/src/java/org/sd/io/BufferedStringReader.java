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
package org.sd.io;


import java.io.IOException;
import java.io.Reader;

/**
 * Utility to read strings from a reader.
 * <p>
 * @author Spence Koehler
 */
public class BufferedStringReader {

  static final int IO_BUFFER_SIZE = 1024;

  private final char[] ioBuffer;
  private final StringBuilder stringBuilder;
  private int ioBufferOffset;
  private int ioBufferEnd;
  private int charOffset;

  public BufferedStringReader() {
    this(IO_BUFFER_SIZE);
  }

  public BufferedStringReader(int ioBufferSize) {
    this.ioBuffer = new char[ioBufferSize];
    this.stringBuilder = new StringBuilder();

    init();
  }

  /**
   * Get the offset of the next character to be read.
   */
  public int getCharOffset() {
    return charOffset;
  }

  /**
   * Read all data from the reader as a string.
   */
  public String readFully(Reader reader) throws IOException {
    int numLoaded = -1;

    grabChars();  // grab any pending characters

    while ((numLoaded = loadChars(reader)) > 0) {
      if (!grabChars()) {
        break;
      }
    }

    final String result = (numLoaded < 0 && stringBuilder.length() == 0) ? null : stringBuilder.toString();
    stringBuilder.setLength(0);  // reset

    return result;
  }

  /**
   * Read all characters until (and not including) the given character is
   * encountered or the end of the reader is reached.
   *
   * @return the read characters up to, but not including 'c' (possibly empty)
   *         or null if the stream has no more characters.
   */
  public String readUntil(Reader reader, char c) throws IOException {
    int numLoaded = -1;

    if (!grabUntil(c)) {
      while ((numLoaded = loadChars(reader)) > 0) {
        if (grabUntil(c)) {
          break;
        }
      }
    }

    final String result = (numLoaded < 0 && stringBuilder.length() == 0) ? null : stringBuilder.toString();
    stringBuilder.setLength(0);  // reset

    return result;
  }

  /**
   * Read while the characters encountered are 'c'.
   *
   * @return the read characters, possibly empty.
   */
  public String readWhile(Reader reader, char c) throws IOException {
    int numLoaded = -1;

    if (grabWhile(c)) {
      while ((numLoaded = loadChars(reader)) > 0) {
        if (!grabWhile(c)) {
          break;
        }
      }
    }

    final String result = (numLoaded < 0 && stringBuilder.length() == 0) ? null : stringBuilder.toString();
    stringBuilder.setLength(0);  // reset

    return result;
  }

  /**
   * Reset this instance.
   */
  public void reset() {
    stringBuilder.setLength(0);
    init();
  }

  private final void init() {
    ioBufferOffset = 0;
    ioBufferEnd = 0;
    charOffset = 0;
  }

  /**
   * Load characters into the buffer.
   */
  private final int loadChars(Reader reader) throws IOException {
    final int numRead = reader.read(ioBuffer, ioBufferEnd, ioBuffer.length - ioBufferEnd);

    if (numRead >= 0) {
      ioBufferEnd += numRead;
    }

    return numRead;
  }

  /**
   * Grab all chars currently in the io buffer.
   */
  private final boolean grabChars() {
    boolean result = false;

    if (ioBufferEnd > ioBufferOffset) {
      final int diff = ioBufferEnd - ioBufferOffset;

      // capture any pending characters
      stringBuilder.append(ioBuffer, ioBufferOffset, diff);

      // reset the io buffer
      charOffset += diff;
      ioBufferOffset = 0;
      ioBufferEnd = 0;

      result = true;
    }

    return result;
  }

  /**
   * Grab all chars currently in the io buffer until the character is reached.
   *
   * @return true if the character was reached.
   */
  private final boolean grabUntil(char c) {
    boolean result = false;

    for (; ioBufferOffset < ioBufferEnd; ++ioBufferOffset) {
      final char curc = ioBuffer[ioBufferOffset];
      if (curc == c) {
        result = true;
        break;
      }
      stringBuilder.append(curc);
      ++charOffset;
    }

    // reset the io buffer
    if (ioBufferOffset == ioBufferEnd) {
      ioBufferOffset = 0;
      ioBufferEnd = 0;
    }

    return result;
  }

  /**
   * Grab all chars currently in the io buffer while the character occurs.
   *
   * @return true if the character may still occur.
   */
  private final boolean grabWhile(char c) {
    boolean result = true;

    for (; ioBufferOffset < ioBufferEnd; ++ioBufferOffset) {
      final char curc = ioBuffer[ioBufferOffset];
      if (curc != c) {
        result = false;
        break;
      }
      stringBuilder.append(curc);
      ++charOffset;
    }

    // reset the io buffer
    if (ioBufferOffset == ioBufferEnd) {
      ioBufferOffset = 0;
      ioBufferEnd = 0;
    }

    return result;
  }
}
