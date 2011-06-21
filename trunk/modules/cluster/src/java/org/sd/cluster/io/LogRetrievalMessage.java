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
package org.sd.cluster.io;


import java.io.IOException;
import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.ClusterNode;
import org.sd.cluster.util.LogManager;
import org.sd.io.FileUtil;
import org.sd.xml.DomElement;
import org.sd.xml.XmlStringBuilder;

/**
 * An xml message to retrieve the cluster stdout, stderr logs.
 * <p>
 * @author Spence Koehler
 */
public class LogRetrievalMessage extends XmlMessage {
  
  public enum Select { BOTH, ERR, OUT };

  //
  // <request select='both|err|out' controllerGroup='' workerGroup='' groupTimeout='' />
  // <response>
  //   <log file='' node='' starttime='' curtime='' [readError='true']>
  //     ...log text or readError details...
  //   </log>
  //   ...
  // </response>
  //
  // NOTE: new lines are preserved as "&#xA;" which is "double escaped" in
  //       the xml. Once the text value is read (and unescaped) from the xml
  //       the "&#xA;" strings can be replaced with "\n" to re-instate newlines.
  //
  // future?:
  // <request type='list|retrieve' filter='latest|all' select='both|err|out'/>
  //

  /**
   * Factory method for creating an instance.
   */
  public static final LogRetrievalMessage makeInstance(Select select, String controllerGroup, String workerGroup, Integer groupTimeout) {
    if (select == null) select = Select.BOTH;

    final StringBuilder xml = new StringBuilder();

    xml.
      append("<request select='").
      append(select.name()).
      append("'");

    if (controllerGroup != null) {
      xml.append(" controllerGroup='").append(controllerGroup).append("'");
    }

    if (workerGroup != null) {
      xml.append(" workerGroup='").append(workerGroup).append("'");
    }

    if (groupTimeout != null) {
      xml.append(" groupTimeout='").append(groupTimeout).append("'");
    }

    xml.append("/>");

    return new LogRetrievalMessage(xml.toString());
  }

  /**
   * Empty constructor for publishable reconstruction.
   */
  public LogRetrievalMessage() {
    super();
  }

  /**
   * Initialize (for sending) with the given xmlRequestString.
   */
  private LogRetrievalMessage(String xmlRequestString) {
    super(xmlRequestString);
  }

  public void handle(Context serverContext, ConnectionContext connectionContext) {
    // nothing to do
  }

  public Message getResponse(Context serverContext, ConnectionContext connectionContext) {

    final XmlStringBuilder xml = new XmlStringBuilder("response");
    final Message result = new XmlResponse(xml);

    if (serverContext instanceof ClusterNode) {
      final ClusterNode clusterContext = (ClusterNode)serverContext;

      final DomElement requestElement = getXmlElement();
      final String controllerGroup = requestElement.getAttributeValue("controllerGroup", null);
      final String workerGroup = requestElement.getAttributeValue("workerGroup", ClusterDefinition.ALL_NODES_GROUP);

      if (controllerGroup != null && clusterContext.hasGroup(controllerGroup) && clusterContext.hasGroup(workerGroup)) {
        final int timeout = requestElement.getAttributeInt("groupTimeout", 30000);
        processControllerRequest(xml, clusterContext, workerGroup, timeout);
      }
      else {  // assume worker group
        processWorkRequest(xml, clusterContext);
      }
    }

    return result;
  }

  private void processControllerRequest(XmlStringBuilder xml, ClusterNode clusterNode, String workerGroup, int timeout) {
  }

  private void processWorkRequest(XmlStringBuilder xml, ClusterNode clusterNode) {
    final DomElement requestElement = getXmlElement();

    final Select select = Select.valueOf(requestElement.getAttributeValue("select", "SELECT"));

    final LogManager.LogInfo errorLog = clusterNode.getErrorLog();
    final LogManager.LogInfo outputLog = clusterNode.getOutputLog();
    final String nodeName = clusterNode.getConfig().getNodeName();

    switch (select) {
      case BOTH :
        populateResponseXml(xml, errorLog, nodeName);
        populateResponseXml(xml, outputLog, nodeName);
        break;
      case ERR :
        populateResponseXml(xml, errorLog, nodeName);
        break;
      case OUT :
        populateResponseXml(xml, outputLog, nodeName);
        break;
    }
  }

  private final void populateResponseXml(XmlStringBuilder xml, LogManager.LogInfo log, String nodeName) {
    //   <log file='' node='' starttime='' curtime='' readError='true'>...log text...</log>
    final StringBuilder tag = new StringBuilder();
    final StringBuilder text = new StringBuilder();
    text.append('\n');

    tag.
      append("log file='").
      append(log.logFile).
      append("' node='").
      append(nodeName).
      append("' starttime='").
      append(log.timeMillis).
      append("' curtime='").
      append(System.currentTimeMillis()).
      append("'");

    try {
      text.append(FileUtil.getTextFileAsString(log.logFile));
    }
    catch (IOException e) {
      tag.append(" readError='true'");
      text.
        append(e.toString()).
        append('\n').
        append(FileUtil.getStackTrace(e)).
        append('\n');
    }

    xml.addTagAndText(tag.toString(), text.toString().replaceAll("\n", "&#xA;"));
  }
}
