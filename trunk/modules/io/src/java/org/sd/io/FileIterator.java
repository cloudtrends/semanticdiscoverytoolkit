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


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.util.Iterator;

/**
 * Utility for iterating over a file in consecutive "file chunks".
 * <p>
 * @author Spence Koehler
 */
public class FileIterator <T extends FileChunk> implements Iterator<T> {

  public static final class FileChunkImpl implements FileChunk {
    private String line;

    public FileChunkImpl(String line) {
      this.line = line;
    }
    public boolean addLine(String fileLine) {
      return false;  // each line is in its own chunk.
    }
    public String getLine() {
      return line;
    }
  }
  
  public static final FileChunkFactory<FileChunkImpl> LINE_CHUNK_FACTORY = new FileChunkFactory<FileChunkImpl>() {
      public FileChunkImpl buildFileChunk(String fileLine) {
        return new FileChunkImpl(fileLine);
      }
    };

  public static final FileIterator<FileChunkImpl> getLineIterator(File file) throws IOException {
    return new FileIterator<FileChunkImpl>(file, LINE_CHUNK_FACTORY);
  }

  public static final FileIterator<FileChunkImpl> getLineIterator(InputStream inputStream) throws IOException {
    return new FileIterator<FileChunkImpl>(inputStream, LINE_CHUNK_FACTORY);
  }


  private BufferedReader reader;
  private FileChunkFactory<T> fileChunkFactory;

  private boolean hitEnd;
  private String nextLine;
  private T nextChunk;
  
  public FileIterator(File file, FileChunkFactory<T> fileChunkFactory) throws IOException {
    this(FileUtil.getReader(file), fileChunkFactory);
  }
  
  public FileIterator(InputStream inputStream, FileChunkFactory<T> fileChunkFactory) throws IOException {
    this(FileUtil.getReader(inputStream), fileChunkFactory);
  }
  
  public FileIterator(BufferedReader reader, FileChunkFactory<T> fileChunkFactory) throws IOException {
    this.reader = reader;
    this.fileChunkFactory = fileChunkFactory;
    this.nextChunk = getNextChunk();
    this.hitEnd = false;
  }

  public boolean hasNext() {
    return (this.nextChunk != null);
  }

  public void close() {
    if (!hitEnd) {
      this.hitEnd = true;
      try {
        reader.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public T next() {
    final T result = nextChunk;

    if (result != null) {
      this.nextChunk = getNextChunk();
      if (nextChunk == null) close();
    }

    return result;
  }

  public void remove() {
    //do nothing.
  }

  private final T getNextChunk() {
    if (hitEnd) return null;

    T result = null;
    try {
      result = readNextChunk();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    // see if we hit the end.
    if (result == null) {
      // close reader
      close();
    }

    return result;
  }

  private final T readNextChunk() throws IOException {
    T curChunk = null;
    String curLine = null;

    if (nextLine != null) {
      curChunk = fileChunkFactory.buildFileChunk(nextLine);
    }

    while ((curLine = reader.readLine()) != null) {
      if (curChunk == null) {
        curChunk = fileChunkFactory.buildFileChunk(curLine);
      }
      else if (!curChunk.addLine(curLine)) {
        break;
      }
    }

    nextLine = curLine;

    return curChunk;
  }
}
