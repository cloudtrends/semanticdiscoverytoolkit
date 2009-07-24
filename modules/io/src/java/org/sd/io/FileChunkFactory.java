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


/**
 * Interface for a factory to build a file chunk instance from its first file line.
 * <p>
 * @author Spence Koehler
 */
public interface FileChunkFactory <T extends FileChunk> {

  /**
   * Build a file chunk instance from its first file line.
   * <p>
   * A file chunk is a container for "chunks" from a file that is being iterated
   * over using a FileIterator.
   */
  public T buildFileChunk(String fileLine);
  
}
