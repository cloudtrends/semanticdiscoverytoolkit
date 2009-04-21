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
package org.sd.classifier;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * Utility to cut attributes or instances from an arff.
 * <p>
 * @author Spence Koehler
 */
public class ArffCutter {

  public static final void main(String[] args) throws IOException {
    // cut out the attributes and/or instances, leaving the rest.
    // -f number-range-of-attributes-to-cut
    // -i number-range-of-instances-to-cut
    // -v reverse logic to keep instead of cut

    //todo: refactor/abstract ArffPruner, ArffMerger to re-use common functionality.
  }
}
