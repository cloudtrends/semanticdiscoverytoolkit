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
package org.sd.xml;


import org.sd.util.tree.Tree;

import java.util.Iterator;

/**
 * Interface for ripping tags and text from xml.
 * <p>
 * @author Spence Koehler
 */
public interface XmlRipper extends Iterator<Tree<XmlLite.Data>> {
  
  /**
   * Get the current (last next element's) tag stack.
   */
  public TagStack getTagStack();

  /**
   * Get the index of the last node returned from next.
   */
  public int getIndex();

  /**
   * Close this ripper, cleanly disposing of open references, etc.
   */
  public void close();

  /**
   * Determine whether the stream has been read to its end.
   */
  public boolean finishedStream();

}
