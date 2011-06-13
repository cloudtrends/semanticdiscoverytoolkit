/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cluster.protolog;


import com.google.protobuf.Message;
import java.io.File;
import org.sd.cluster.config.ClusterNode;
import org.sd.cluster.config.Config;
import org.sd.cluster.io.ConnectionContext;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.XmlMessage;
import org.sd.cluster.io.XmlResponse;
import org.sd.cluster.protolog.codegen.ProtoLogProtos.*;
import org.sd.xml.DomElement;
import org.sd.xml.XmlStringBuilder;

/**
 * Utility to retrieve protolog data as xml.
 * <p>
 * @author Spence Koehler
 */
public class PLogRetrievalMessage extends XmlMessage {

  public static final PLogRetrievalMessage makeInstance(String jobIdString, String dataDirName, long minTime, long maxTime) {
    final StringBuilder xml = new StringBuilder();

    // <request jobIdString='' dataDirName='' minTime='' maxTime='' />

    //todo: add parameters (e.g. for custom streamer, maxMessageBytes, filename constraints, etc.)
    xml.
      append("<request jobIdString='").
      append(jobIdString).
      append("' dataDirName='").
      append(dataDirName).
      append("'");

    if (minTime > 0L) {
      xml.append(" minTime='").append(minTime).append("'");
    }

    if (maxTime > 0L) {
      xml.append(" maxTime='").append(maxTime).append("'");
    }

    xml.append("/>");

    return new PLogRetrievalMessage(xml.toString());
  }

  /**
   * Empty constructor for publishable reconstruction.
   */
  public PLogRetrievalMessage() {
    super();
  }

  /**
   * Initialize (for sending) with the given xmlRequestString.
   */
  private PLogRetrievalMessage(String xmlRequestString) {
    super(xmlRequestString);
  }

  public void handle(Context serverContext, ConnectionContext connectionContext) {
    // nothing to do
  }

  public org.sd.cluster.io.Message getResponse(Context serverContext, ConnectionContext connectionContext) {
    final XmlStringBuilder xml = new XmlStringBuilder("response");
    final org.sd.cluster.io.Message result = new XmlResponse(xml);

    final DomElement requestElement = getXmlElement();

    //todo: parse/handle parameters (e.g. for custom streamer, maxMessageBytes, filename constraints, etc.)
    final String jobIdString = requestElement.getAttributeValue("jobIdString");
    final String dataDirName = requestElement.getAttributeValue("dataDirName");
    final long minTime = requestElement.getAttributeLong("minTime", 0L);
    final long maxTime = requestElement.getAttributeLong("maxTime", 0L);

    final ClusterNode clusterNode = (ClusterNode)serverContext;
    final Config config = clusterNode.getConfig();
    final File plogdir = new File(config.getOutputDataPath(jobIdString, dataDirName));

    if (plogdir.exists()) {
      final ProtoLogXml protoLogXml = new ProtoLogXml();

      MultiLogIterator iter = null;

      try {
        iter =
          new MultiLogIterator(plogdir, EventEntry.class,
                               ProtoLogStreamer.DEFAULT_INSTANCE,
                               ProtoLogStreamer.MAX_MESSAGE_BYTES,
                               null, null,
                               (minTime <= 0) ? null : minTime,
                               (maxTime <= 0) ? null : maxTime);
        
        while (iter.hasNext()) {
          final Message message = iter.next();
          if (message instanceof EventEntry) {
            final EventEntry eventEntry = (EventEntry)message;
            protoLogXml.asXml(xml, eventEntry);
          }
        }
      }
      finally {
        if (iter != null) iter.close();
      }
    }

    return result;
  }
}
