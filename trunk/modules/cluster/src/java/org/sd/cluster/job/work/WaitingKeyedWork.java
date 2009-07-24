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
package org.sd.cluster.job.work;

import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

/**
 * KeyedWork for holding values for a waiting response signal from a server.
 * @author Abe Sanderson
 */
public class WaitingKeyedWork extends KeyedWork {
  private int retries;
  private long sleepTime;

  public WaitingKeyedWork(){
  }
  
  public WaitingKeyedWork(int retries, long sleepTime){
    this.retries = retries;
    this.sleepTime = (sleepTime < 0 ? 0 : sleepTime);
  }

  public int getRetries(){
    return this.retries;
  }

  public long getSleepTime(){
    return this.sleepTime;
  }

  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    dataOutput.writeInt(retries);
    dataOutput.writeLong(sleepTime);
  }

  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);
    this.retries = dataInput.readInt();
    this.sleepTime = dataInput.readLong();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append('[').append(super.toString()).append(',').append(retries).append(',').append(sleepTime).append(']');

    return result.toString();
  }
}
