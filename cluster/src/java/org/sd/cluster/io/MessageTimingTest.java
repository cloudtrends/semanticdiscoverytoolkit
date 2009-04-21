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
package org.sd.cluster.io;


import org.sd.cluster.config.BooleanResponse;
import org.sd.cluster.config.ClusterRunner;
import org.sd.cluster.config.Console;
import org.sd.util.PropertiesParser;
import org.sd.util.TimingMetrics;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class MessageTimingTest {
  

  public static final class SimpleMessage implements Message {
    public SimpleMessage() {
    }

    public Message getResponse(Context serverContext) {
      return new BooleanResponse(serverContext, true);
    }

    public void handle(Context serverContext) {
    }

    public void write(DataOutput dataOutput) throws IOException {
    }

    public void read(DataInput dataInput) throws IOException {
    }
  }


  // ./deploy sbk 1m2n.2 "sd" 500 sd
  // pushd ~/cluster/bin
  // ./startClusterNode 0;./startClusterNode 1
  // 
  // java -Xmx640m org.sd.cluster.io.MessageTimingTest groupName=sd-0
  //   2008-11-11 ended with: 780.329 msgs/s (N=100000, min= d, max=134 ms, t=02:08.151 min)
  // java -Xmx640m org.sd.cluster.io.MessageTimingTest groupName=all  (round-tripping to 2 nodes)
  //   2008-11-11 ended with: 461.794 msgs/s (N=100000, min=1 ms, max=146 ms, t=03:36.547 min)
  public static final void main(String[] args) throws Exception {
    // properties:
    //  groupName -- name of group to send messages to.

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    final Console console = new ClusterRunner(properties).getConsole();
    final String groupName = properties.getProperty("groupName");
    final Message message = new SimpleMessage();

    final TimingMetrics timingMetrics = new TimingMetrics("msgs");

    while (true) {
      final Response[] responses = console.sendMessageToNodes(message, groupName, 5000, true);
      timingMetrics.mark();

      if ((timingMetrics.getTotalMarks() % 1000) == 0) {
        System.out.println(timingMetrics);
      }

      if (timingMetrics.getTotalMarks() >= 100000) break;
    }

    console.shutdown();
  }
}
