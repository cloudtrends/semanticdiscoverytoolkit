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

import java.io.File;
import java.io.IOException;

/**
 * Utility to aggregate the results of multiple histograms
 * <p>
 * @author Abe Sanderson
 */
public class XmlHistogramAggregator
{
  public static void main(String[] args) 
    throws IOException 
  {
    final XmlHistogram result = new XmlHistogram();
    final File outFile = new File(args[0]);

    //arg(0): output histogram file
    //args(1+): histogram files
    if (args.length > 1) {
      for (int i = 1; i < args.length; ++i) 
      {
        final File xmlFile = new File(args[i]);
        final XmlHistogram xmlHisto = new XmlHistogram(xmlFile);
        result.add(xmlHisto);
      }
    }

    result.dumpXml(outFile);
  }
}
