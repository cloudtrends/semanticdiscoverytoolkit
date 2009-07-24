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
package org.sd.cluster.job;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A partition work factory that tracks units of work that have been dealt and
 * released through a StreamingWorkFactory.
 * <p>
 * @author Spence Koehler
 */
public class TrackingWorkFactory extends PartitionWorkFactory {
  
  public TrackingWorkFactory() {
    super();
  }

  public TrackingWorkFactory(String partitionPattern, String dataDirPath) throws IOException {
    super(partitionPattern, dataDirPath);
  }

  public WorkFactory createWorkFactory(File file) throws IOException {
    WorkFactory result = null;

    final DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
    final String outputPath = file.getAbsolutePath() + ".out";
    final File outputFile = new File(outputPath);
    final DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFile, true));
    if (outputFile.exists()) {
      final DataInputStream priorStream = new DataInputStream(new FileInputStream(outputFile));
//todo: parameterize for 'priorStatusToReprocess' Set<WorkStatus> arg...
      result = new StreamingWorkFactory(inputStream, outputStream, priorStream, null);
    }
    else {
      result = new StreamingWorkFactory(inputStream, outputStream);
    }
    return result;
  }
}
