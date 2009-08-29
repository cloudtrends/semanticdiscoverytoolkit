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
 * Interface for building a multi-part record of type R, one piece of type P at
 * a time.
 * <p>
 * @author Spence Koehler
 */
public interface MultiPartRecord<P,R> {

  /**
   * Add a piece of the record to this instance.
   *
   * @return true if the piece was successfully added as a part of this record;
   *         false if the piece does not belong in this record.
   */
  public boolean addPiece(P piece);

  /**
   * Get the record with all of its parts.
   */
  public R getRecord();
}
