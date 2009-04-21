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


import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.DataInputStream;

/**
 * A partition work factory that always walks through each unit of work in
 * its batches.
 * <p>
 * @author Spence Koehler
 */
public class SimpleWorkFactory extends PartitionWorkFactory {

  public SimpleWorkFactory() {
    super();
  }

  public SimpleWorkFactory(String partitionPattern, String dataDirPath) throws IOException {
    super(partitionPattern, dataDirPath);
  }

  public WorkFactory createWorkFactory(File file) throws IOException {
    return new ReadingWorkFactory(new DataInputStream(new FileInputStream(file)), file.length());
  }


  //java -Xmx640m org.sd.cluster.job.SimpleWorkFactory ~/tmp/icis/batches-080706/
  public static void main(String[] args) throws IOException {
    // arg0: path to directory containing workbatch-\d+.dat files.
    final WorkFactory workFactory = new SimpleWorkFactory("^workbatch-\\d+\\.dat$", args[0]);

    UnitOfWork unitOfWork = null;
    int count = 0;
    while ((unitOfWork = workFactory.getNext()) != null) {
      ++count;
      workFactory.release(unitOfWork);
    }
    workFactory.close();
    System.out.println("count=" + count);
  }
}
