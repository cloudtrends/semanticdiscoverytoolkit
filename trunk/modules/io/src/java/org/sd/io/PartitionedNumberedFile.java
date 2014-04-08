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

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to work with numbered files.
 * <p>
 * @author Abe Sanderson
 */
public class PartitionedNumberedFile 
  extends BaseNumberedFile
{
  private static final int PARTITION_DEPTH = 2;
  private static final int PARTITION_SIZE = 1024;
  
  public PartitionedNumberedFile(File dir, 
                                 String namePrefix, 
                                 String nameSuffix) 
  {
    this(dir, namePrefix, nameSuffix, false);
  }
  public PartitionedNumberedFile(File dir, 
                                 String namePrefix, 
                                 String nameSuffix,
                                 boolean useFileLocks) 
  {
    super(dir, namePrefix, nameSuffix, useFileLocks);
  }

  protected File[] findFiles() 
  {
    return FileUtil.findSubFiles(this.dir, this.namePattern);
  }
  protected File newFile(int number) 
  {
    int[] splits = partitionNumber(number, PARTITION_DEPTH, PARTITION_SIZE);
    File partitionDir = new File(this.dir.getAbsolutePath());
    for(int split : splits)
      partitionDir = new File(partitionDir, String.valueOf(split));
    partitionDir.mkdirs();
    return new File(partitionDir, this.namePrefix + number + this.nameSuffix);
  }

  private int[] partitionNumber(int x, int depth, int partitionMax) 
	{
    int[] splits = new int[depth];
    for(int i = depth; i > 0; i--)
    {
      int max = (int)Math.pow(partitionMax, i);
      splits[depth-i] = (x/max) * max;
    }
    return splits;
  }
}
