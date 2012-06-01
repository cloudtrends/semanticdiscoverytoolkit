/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn.interp;


import java.util.List;

/**
 * Utility to traverse an interp container('s contents) while applying an
 * InterpContainerVisitor to each container.
 * <p>
 * @author Spence Koehler
 */
public class InterpContainerWalker {

  public static final InterpContainerWalker INSTANCE = new InterpContainerWalker();

  private InterpContainerWalker() {
  }

  public void traverse(InterpContainer container, InterpContainerVisitor visitor) {
    visitor.visit(container);

    if (container.hasFields()) {
      for (List<InterpContainer> fieldValues : container.getFields().values()) {
        for (InterpContainer fieldValue : fieldValues) {
          traverse(fieldValue, visitor);
        }
      }
    }
  }
}
