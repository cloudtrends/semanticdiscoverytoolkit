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
package org.sd.token;

/**
 * Implementation of segment pointer finder factory with a flag to 
 * parameterize the prevention of entity containing caps change
 * <p>
 * @author asanderson
 */
public class NamedEntitySegmentFinderFactory 
  implements SegmentPointerFinderFactory
{
  private boolean ignoreCapsChange;
  public NamedEntitySegmentFinderFactory(boolean ignoreCapsChange) {
    this.ignoreCapsChange = ignoreCapsChange;
  }
    
  public SegmentPointerFinder getSegmentPointerFinder(String input) {
    return new NamedEntitySegmentFinder(input, ignoreCapsChange);
  }
}
