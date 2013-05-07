/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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

import org.apache.commons.lang.StringEscapeUtils;

/**
 * utilities to help process xml files
 * <p>
 * @author Abe Sanderson
 */
public class XmlUtil
{
  public static String escape(String text)
  {
    String str = StringEscapeUtils.escapeXml(text);
    if(str == null) return null;

    StringBuilder builder = new StringBuilder();
    for(char ch : str.toCharArray())
    {
      if(ch < 0x20 && ch != 0x09)
        builder.append(String.format("&#x%02X;", (byte)ch));
      else
        builder.append(ch);
    }
    return builder.toString();
  }
}
