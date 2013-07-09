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


/**
 * Interface for representing, building, and tearing down a stack of xml tags.
 * <p>
 * @author Spence Koehler
 */
public abstract class MutableTagStack extends BaseTagStack {
  
  protected MutableTagStack() {
    this(false);
  }
  protected MutableTagStack(boolean useTagEquivalents) {
    super(useTagEquivalents);
  }

  /**
   * Push the given tag onto this stack.
   */
  public abstract void pushTag(XmlLite.Tag tag);

  /**
   * Pop the tag with the given name from this stack.
   */
  public abstract XmlLite.Tag popTag(String tagName);

  /**
   * Pop the last tag pushed onto the stack.
   *
   * @return the popped tag or null if the stack is empty.
   */
  public abstract XmlLite.Tag popTag();

  /**
   * Reset this tag stack for reuse.
   */
  public abstract void reset();

}
