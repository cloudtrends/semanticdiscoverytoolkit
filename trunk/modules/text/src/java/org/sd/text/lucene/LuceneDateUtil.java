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
package org.sd.text.lucene;


import java.util.Date;

import org.apache.lucene.document.DateTools;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class LuceneDateUtil {
  
  public static final String asDay(Date date) {
    return DateTools.dateToString(date, DateTools.Resolution.DAY);
  }

  public static final String asYear(Date date) {
    return DateTools.dateToString(date, DateTools.Resolution.YEAR);
  }

  public static void main(String[] args) {
    final Date date = new Date();
    System.out.println("date=" + date + "; dateString=" + DateTools.dateToString(date, DateTools.Resolution.DAY));
    System.out.println("date=" + date + "; dateString=" + DateTools.dateToString(date, DateTools.Resolution.MILLISECOND));
  }
}
