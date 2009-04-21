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
package org.sd.xml.record;


import java.util.List;

/**
 * A divide strategy that takes another divide strategy and filters the records.
 * <p>
 * @author Spence Koehler
 */
public abstract class FilterStrategy implements DivideStrategy {

  /**
   * Filter the list of records.
   */
  protected abstract List<Record> filter(List<Record> records);


  private DivideStrategy divideStrategy;

  protected FilterStrategy(DivideStrategy divideStrategy) {
    this.divideStrategy = divideStrategy;
  }

  /**
   * Divide the given record into sub-records.
   * 
   * @return the sub-records or null if the record cannot be divided by
   *         this strategy.
   */
  public List<Record> divide(Record record) {
    List<Record> result = null;

    final List<Record> divisions = divideStrategy.divide(record);
    if (divisions != null) {
      result = filter(divisions);
    }

    return result;
  }
}
