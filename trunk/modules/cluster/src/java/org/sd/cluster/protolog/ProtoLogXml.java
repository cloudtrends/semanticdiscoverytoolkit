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


import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringEscapeUtils;
import org.sd.cluster.protolog.codegen.ProtoLogProtos.*;
import org.sd.util.Base64;
import org.sd.util.PropertiesParser;
import org.sd.xml.DomElement;
import org.sd.xml.XmlStringBuilder;
import org.w3c.dom.NodeList;

/**
 * Xml helper class for protolog EventEntry instances.
 * <p>
 * @author Spence Koehler
 */
public class ProtoLogXml {

  public static final String DEFAULT_ATTRIBUTE_DELIM = "|";
  public static final String DEFAULT_UNTYPED_ATTRIBUTE_STRING = "_A_";


  private String attributeDelim = DEFAULT_ATTRIBUTE_DELIM;
  private String untypedAttributeKey = DEFAULT_UNTYPED_ATTRIBUTE_STRING;

  /**
   * Default constructor.
   * <p>
   * attributeDelim is "|" by default.
   */
  public ProtoLogXml() {
  }

  /**
   * Set the attributeDelim.
   */
  public void setAttributeDelim(String attributeDelim) {
    this.attributeDelim = attributeDelim;
  }

  /**
   * Get the attributeDelim.
   */
  public String getAttributeDelim() {
    return attributeDelim;
  }

  /**
   * Set the untypedAttributeKey.
   */
  public void setUntypedAttributeKey(String untypedAttributeKey) {
    this.untypedAttributeKey = untypedAttributeKey;
  }

  /**
   * Get the untypedAttributeKey.
   */
  public String getUntypedAttributeKey() {
    return untypedAttributeKey;
  }

  /**
   * Convert the eventEntry to XML.
   * <p>
   * &lt;event _id='`id`' _time='`timestamp`' _type='`type`' _who='`who`' _what='`what`' `attribute.type`='`attribute.*data`'&rt;
   *   &lt;message&rt;`message`&lt;/message&rt;
   * &lt;/event&rt;
   */
  public XmlStringBuilder asXml(EventEntry eventEntry) {
    return asXml(null, eventEntry, this.attributeDelim, this.untypedAttributeKey);
  }

  /**
   * Convert the eventEntry to XML.
   * <p>
   * &lt;event _id='`id`' _time='`timestamp`' _type='`type`' _who='`who`' _what='`what`' `attribute.type`='`attribute.*data`'&rt;
   *   &lt;message&rt;`message`&lt;/message&rt;
   * &lt;/event&rt;
   */
  public XmlStringBuilder asXml(XmlStringBuilder result, EventEntry eventEntry) {
    return asXml(result, eventEntry, this.attributeDelim, this.untypedAttributeKey);
  }

  /**
   * Convert the eventEntry to XML, adding the 'event' tag to the given builder.
   * <p>
   * &lt;event _id='`id`' _time='`timestamp`' _type='`type`' _who='`who`' _what='`what`' `attribute.type`='`attribute.*data`'&rt;
   *   &lt;message&rt;`message`&lt;/message&rt;
   * &lt;/event&rt;
   */
  public static XmlStringBuilder asXml(XmlStringBuilder result, EventEntry eventEntry, String attributeDelim, String untypedAttributeKey) {
    if (result == null) result = new XmlStringBuilder();

    // <event _id='`id`' _time='`timestamp`' _type='`type`' _who='`who`' _what='`what`' `attribute.type`='`attribute.*data`'>
    //   <message>`message`</message>
    // </event>
    
    final StringBuilder tag = new StringBuilder();

    final EventId eventId = eventEntry.getId();

    tag.
      append("event _id=\"").
      append(eventId.getId()).
      append("\" _time=\"").
      append(eventEntry.getTimestamp()).
      append("\" _type=\"").
      append(eventEntry.getType().name()).
      append("\" _who=\"").
      append(eventId.getWho()).
      append("\" _what=\"").
      append(eventId.getWhat()).
      append('"');

    final int numAtts = eventEntry.getAttributeCount();
    for (int attIdx = 0; attIdx < numAtts; ++attIdx) {
      final Attribute attribute = eventEntry.getAttribute(attIdx);

      // build value string (using attributeDelim)
      final String attributeString = asString(attribute, attributeDelim);

      if (attribute.hasType()) {
        tag.append(' ').append(attributeString);
      }
      else {
        tag.
          append(' ').
          append(untypedAttributeKey).
          append("=\"").
          append(attributeString).append('"');
      }
    }


    final String eventTag = tag.toString();
    result.addTag(eventTag);

    // add messages
    final int numMessages = eventEntry.getMessageCount();
    for (int msgIdx = 0; msgIdx < numMessages; ++msgIdx) {
      final String msgString = eventEntry.getMessage(msgIdx);
      result.addTagAndText("message", msgString);
    }

    result.addEndTag(eventTag);

    return result;
  }

  /**
   * Convert the XML to an eventEntry instance using default values.
   * <p>
   * &lt;event _id='`id`' _time='`timestamp`' _type='`type`' _who='`who`' _what='`what`' `attribute.type`='`attribute.*data`'&rt;
   *   &lt;message&rt;`message`&lt;/message&rt;
   * &lt;/event&rt;
   */
  public EventEntry fromXml(String xmlString) {
    return fromXml(new XmlStringBuilder(xmlString).getXmlElement());
  }

  /**
   * Convert the XML to an eventEntry instance using default values.
   * <p>
   * &lt;event _id='`id`' _time='`timestamp`' _type='`type`' _who='`who`' _what='`what`' `attribute.type`='`attribute.*data`'&rt;
   *   &lt;message&rt;`message`&lt;/message&rt;
   * &lt;/event&rt;
   */
  public EventEntry fromXml(DomElement eventEntryXml) {
    return fromXml(eventEntryXml, this.attributeDelim, this.untypedAttributeKey);
  }

  public static EventEntry fromXml(DomElement eventEntryXml, String attributeDelim, String untypedAttributeKey) {
    EventEntry.Builder result = null;

    long eventId = 0L;
    long when = 0L;
    EventEntry.EventType eventType = null;
    String who = null;
    String what = null;
    final List<Attribute.Builder> attributes = new ArrayList<Attribute.Builder>();

    // parse attributes
    if (eventEntryXml.hasAttributes()) {
      for (Map.Entry<String, String> attr : eventEntryXml.getDomAttributes().getAttributes().entrySet()) {
        final String key = attr.getKey();
        final String val = attr.getValue();

        if ("_id".equals(key)) {
          eventId = Long.parseLong(val);
        }
        else if ("_time".equals(key)) {
          when = Long.parseLong(val);
        }
        else if ("_type".equals(key)) {
          eventType = EventEntry.EventType.valueOf(val);
        }
        else if ("_who".equals(key)) {
          who = val;
        }
        else if ("_what".equals(key)) {
          what = val;
        }
        else {
          final Attribute.Builder attribute = Attribute.newBuilder();
          if (!untypedAttributeKey.equals(key)) {
            attribute.setType(key);
          }
          parseDataString(attribute, val, attributeDelim);
          attributes.add(attribute);
        }
      }
    }

    if (eventType != null) {
      result = EventLogger.createEventEntry(eventId, when, who, what, eventType);

      // add attributes
      for (Attribute.Builder attribute : attributes) {
        result.addAttribute(attribute);
      }

      // add messages
      final NodeList messageNodes = eventEntryXml.selectNodes("message");
      if (messageNodes != null) {
        final int numMsgNodes = messageNodes.getLength();
        for (int msgNodeIdx = 0; msgNodeIdx < numMsgNodes; ++msgNodeIdx) {
          result.addMessage(messageNodes.item(msgNodeIdx).getTextContent());
        }
      }
    }

    return result == null ? null : result.build();
  }

  /**
   * Get the attribute as a string of the form:
   * <p>
   * [type="]valueText["].
   * <p>
   * Note that multiple values will be delimited by attributeDelim
   * and valueText (including delims) will be xml escaped. If there
   * is no type, then the values will not be quoted. Note that each
   * value is marked by type and converted to string forms. See
   * getDataString below.
   */
  public String asString(Attribute attribute) {
    return asString(attribute, this.attributeDelim);
  }

  /**
   * Get the attribute as a string of the form:
   * <p>
   * [type="]valueText["].
   * <p>
   * Note that multiple values will be delimited by attributeDelim
   * and valueText (including delims) will be xml escaped. If there
   * is no type, then the values will not be quoted. Note that each
   * value is marked by type and converted to string forms. See
   * getDataString below.
   */
  public static String asString(Attribute attribute, String attributeDelim) {

    final StringBuilder result = new StringBuilder();
    final String attrs = getDataString(attribute, attributeDelim);

    if (attribute.hasType()) {
      result.
        append(attribute.getType()).
        append("=\"");
    }

    result.append(StringEscapeUtils.escapeXml(attrs.toString()));

    if (attribute.hasType()) {
      result.append('"');
    }

    return result.toString();
  }

  /**
   * Get the attribute data as a string, delimited by attributeDelim.
   * <p>
   * Each value appears in the form "x_" + valueString:
   * <ul>
   * <li>"s_" + stringData (string characters)</li>
   * <li>"i_" + intData (ascii digits)</li>
   * <li>"l_" + longData (ascii digits)</li>
   * <li>"b_" + boolData (0=false, 1=true)</li>
   * <li>"B_" + byteData (Base64 encoded)</li>
   * </ul>
   */
  public String getDataString(Attribute attribute) {
    return getDataString(attribute, this.attributeDelim);
  }

  /**
   * Get the attribute data as a string, delimited by attributeDelim.
   * <p>
   * Each value appears in the form "x_" + valueString:
   * <ul>
   * <li>"s_" + stringData (string characters)</li>
   * <li>"i_" + intData (ascii digits)</li>
   * <li>"l_" + longData (ascii digits)</li>
   * <li>"b_" + boolData (0=false, 1=true)</li>
   * <li>"B_" + byteData (Base64 encoded)</li>
   * </ul>
   */
  public static String getDataString(Attribute attribute, String attributeDelim) {
    final StringBuilder result = new StringBuilder();

    final int numStrings = attribute.getStringDataCount();
    for (int strNum = 0; strNum < numStrings; ++strNum) {
      addValue(result, "s_", attribute.getStringData(strNum), attributeDelim);
    }
    final int numInts = attribute.getIntDataCount();
    for (int intNum = 0; intNum < numInts; ++intNum) {
      addValue(result, "i_", Integer.toString(attribute.getIntData(intNum)), attributeDelim);
    }
    final int numLongs = attribute.getLongDataCount();
    for (int longNum = 0; longNum < numLongs; ++longNum) {
      addValue(result, "l_", Long.toString(attribute.getLongData(longNum)), attributeDelim);
    }
    final int numBools = attribute.getBoolDataCount();
    for (int boolNum = 0; boolNum < numBools; ++boolNum) {
      final boolean boolData = attribute.getBoolData(boolNum);
      addValue(result, "b_", boolData ? "1" : "0", attributeDelim);
    }
    final int numBytes = attribute.getBytesDataCount();
    for (int byteNum = 0; byteNum < numBytes; ++byteNum) {
      addValue(result, "B_", Base64.encodeBytes(attribute.getBytesData(byteNum).toByteArray()), attributeDelim);
    }

    return result.toString();
  }

  /**
   * Parse the attributeData string's (see getDataString) data into the given
   * attribute builder.
   */
  public void parseDataString(Attribute.Builder attribute, String attributeData) {
    parseDataString(attribute, attributeData, this.attributeDelim);
  }

  /**
   * Parse the attributeData string's (see getDataString) data into the given
   * attribute builder.
   */
  public static void parseDataString(Attribute.Builder attribute, String attributeData, String attributeDelim) {
    final List<String> dataPieces = split(attributeData, attributeDelim);

    for (String dataPiece : dataPieces) {
      final int slashPos = dataPiece.indexOf('_');
      final String type = (slashPos < 0) ? "" : dataPiece.substring(0, slashPos);
      final String data = dataPiece.substring(slashPos + 1);

      try {
        if ("i".equals(type)) {
          attribute.addIntData(Integer.parseInt(data));
        }
        else if ("l".equals(type)) {
          attribute.addLongData(Long.parseLong(data));
        }
        else if ("b".equals(type)) {
          attribute.addBoolData(!"0".equals(data));
        }
        else if ("B".equals(type)) {
          attribute.addBytesData(ByteString.copyFrom(Base64.decode(data)));
        }
        else {
          attribute.addStringData(data);
        }
      }
      catch (Exception e) {
        // eat exception. TODO: log it.
      }
    }
  }

  private static final void addValue(StringBuilder result, String marker, String value, String attributeDelim) {
    if (result.length() > 0) result.append(attributeDelim);
    result.append(marker).append(value);
  }

  private static final List<String> split(String attributeData, String attributeDelim) {
    final List<String> result = new ArrayList<String>();

    final int adlen = attributeDelim.length();
    int cursor = 0;

    for (int delimIdx = attributeData.indexOf(attributeDelim);
         delimIdx >= 0;
         delimIdx = attributeData.indexOf(attributeDelim, cursor)) {

      result.add(attributeData.substring(cursor, delimIdx));
      cursor = delimIdx + adlen;
    }

    if (cursor < attributeData.length()) {
      result.add(attributeData.substring(cursor));
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //  id -- (optional, default=1) event id
    //  when -- (optional, default=now) timestamp (in millis)
    //  type -- (optional, default=START) event type (START, SUCCEED, ERROR, KILL, TIMEOUT)
    //  who -- (optional, default="me")
    //  what -- (optional, default="test")
    //  attributeDelim -- (optional, default="|")
    //  untypedAttributeKey -- (optional, default="_A_")
    //
    //  args -- (optional) attributes of form "data" or "type:data" where data in form of getDataString
    //

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final ProtoLogXml protoLogXml = new ProtoLogXml();

    final String attributeDelim = properties.getProperty("attributeDelim");
    if (attributeDelim != null) protoLogXml.setAttributeDelim(attributeDelim);

    final String untypedAttributeKey = properties.getProperty("untypedAttributeKey");
    if (untypedAttributeKey != null) protoLogXml.setUntypedAttributeKey(untypedAttributeKey);

    final String idString = properties.getProperty("id");
    final long id = (idString == null) ? 1L : Long.parseLong(idString);

    final String whenString = properties.getProperty("when");
    final long when = (whenString == null) ? System.currentTimeMillis() : Long.parseLong(whenString);

    final String typeString = properties.getProperty("type");
    final EventEntry.EventType type = (typeString == null) ? EventEntry.EventType.START : EventEntry.EventType.valueOf(typeString.toUpperCase());

    final String whoString = properties.getProperty("who");
    final String who = (whoString == null) ? "me" : whoString;

    final String whatString = properties.getProperty("what");
    final String what = (whatString == null) ? "test" : whatString;

    final EventEntry.Builder eventEntry = EventLogger.createEventEntry(id, when, who, what, type);

    for (String arg : args) {
      final Attribute.Builder attribute = Attribute.newBuilder();

      final int eqPos = arg.indexOf(':');
      if (eqPos >= 0) {
        final String attrType = arg.substring(0, eqPos);
        attribute.setType(attrType);
      }
      protoLogXml.parseDataString(attribute, arg.substring(eqPos + 1));

      eventEntry.addAttribute(attribute);
    }

    final XmlStringBuilder xml = protoLogXml.asXml(eventEntry.build());

    System.out.println(xml.getXmlElement().toString()); // pretty
  }
}
