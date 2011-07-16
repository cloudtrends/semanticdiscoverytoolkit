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
import com.google.protobuf.Descriptors.*;
import com.google.protobuf.Message;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.sd.xml.XmlStringBuilder;

/**
 * Utility for generically converting protobuf message instances to XML.
 * <p>
 * @author Spence Koehler
 */
public class Message2Xml {

  public static final Message2Xml DEFAULT = new Message2Xml();


  private boolean embedXml;
  private String messageTag;

  public Message2Xml() {
    this(true, "message");
  }

  public Message2Xml(boolean embedXml, String messageTag) {
    this.embedXml = embedXml;
    this.messageTag = messageTag;
  }

  public boolean getEmbedXml() {
    return embedXml;
  }

  public boolean setEmbedXml(boolean embedXml) {
    final boolean result = this.embedXml;
    this.embedXml = embedXml;
    return result;
  }

  public String getMessageTag() {
    return messageTag;
  }

  public String setMessageTag(String messageTag) {
    final String result = this.messageTag;
    this.messageTag = messageTag;
    return result;
  }


  /**
   * Convert the message to xml under the given containerTag.
   */
  public XmlStringBuilder asXml(Message message, String containerTag) {
    return asXml(message, new XmlStringBuilder(containerTag));
  }

  /**
   * Convert the message to xml as the next child tag under the given result.
   */
  public XmlStringBuilder asXml(Message message, XmlStringBuilder result) {
    return asXml(message, result, messageTag);
  }

  private final XmlStringBuilder asXml(Message message, XmlStringBuilder result, String messageTag) {
    final StringBuilder tag = new StringBuilder();
    tag.append(messageTag);

    final List<FieldData> deepFields = new ArrayList<FieldData>();

    for (Map.Entry<FieldDescriptor, Object> field : message.getAllFields().entrySet()) {
      final FieldData fieldData = new FieldData(field.getKey(), field.getValue());

      if (!fieldData.addXmlAttribute(tag)) {
        deepFields.add(fieldData);
      }
    }

    result.addTag(tag.toString());

    for (FieldData fieldData : deepFields) {
      fieldData.toXml(result);
    }

    // todo: add all unkown fields as elements, not as attributes
    // e.g. asXml(message.getUnknownFields(), result);

    result.addEndTag(tag.toString());

    return result;
  }

  // =================================================================
  // Utility functions from com.google.protobuf.TextFormat
  //

  /** Convert an unsigned 32-bit integer to a string. */
  private static String unsignedToString(final int value) {
    if (value >= 0) {
      return Integer.toString(value);
    } else {
      return Long.toString(((long) value) & 0x00000000FFFFFFFFL);
    }
  }

  /** Convert an unsigned 64-bit integer to a string. */
  private static String unsignedToString(final long value) {
    if (value >= 0) {
      return Long.toString(value);
    } else {
      // Pull off the most-significant bit so that BigInteger doesn't think
      // the number is negative, then set it again using setBit().
      return BigInteger.valueOf(value & 0x7FFFFFFFFFFFFFFFL)
                       .setBit(63).toString();
    }
  }

  /**
   * Escapes bytes in the format used in protocol buffer text format, which
   * is the same as the format used for C string literals.  All bytes
   * that are not printable 7-bit ASCII characters are escaped, as well as
   * backslash, single-quote, and double-quote characters.  Characters for
   * which no defined short-hand escape sequence is defined will be escaped
   * using 3-digit octal sequences.
   */
  private static String escapeBytes(final ByteString input) {
    final StringBuilder builder = new StringBuilder(input.size());
    for (int i = 0; i < input.size(); i++) {
      final byte b = input.byteAt(i);
      switch (b) {
        // Java does not recognize \a or \v, apparently.
        case 0x07: builder.append("\\a" ); break;
        case '\b': builder.append("\\b" ); break;
        case '\f': builder.append("\\f" ); break;
        case '\n': builder.append("\\n" ); break;
        case '\r': builder.append("\\r" ); break;
        case '\t': builder.append("\\t" ); break;
        case 0x0b: builder.append("\\v" ); break;
        case '\\': builder.append("\\\\"); break;
        case '\'': builder.append("\\\'"); break;
        case '"' : builder.append("\\\""); break;
        default:
          // Note:  Bytes with the high-order bit set should be escaped.  Since
          //   bytes are signed, such bytes will compare less than 0x20, hence
          //   the following line is correct.
          if (b >= 0x20) {
            builder.append((char) b);
          } else {
            builder.append('\\');
            builder.append((char) ('0' + ((b >>> 6) & 3)));
            builder.append((char) ('0' + ((b >>> 3) & 7)));
            builder.append((char) ('0' + (b & 7)));
          }
          break;
      }
    }
    return builder.toString();
  }

  private final class FieldData {
    public final FieldDescriptor field;
    public final Object value;

    private Map<Object, String> value2string;
    private Map<Object, Boolean> looksLikeXml;

    private FieldData(FieldDescriptor field, Object value) {
      this.field = field;
      this.value = value;
      this.value2string = new HashMap<Object, String>();
      this.looksLikeXml = new HashMap<Object, Boolean>();
    }

    private final void toXml(XmlStringBuilder result) {
      if (field.isRepeated()) {
        for (Object element : (List<?>) value) {
          asSingleFieldXml(result, element);
        }
      }
      else {
        asSingleFieldXml(result, value);
      }
    }

    private final void asSingleFieldXml(XmlStringBuilder result, Object repeatValue) {
      if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) {
        final Object theValue = (repeatValue == null) ? value : repeatValue;
        asXml((Message)theValue, result, field.getName());
      }
      else {
        String valueString = getValueString(repeatValue);

        if (valueString != null && looksLikeXml(repeatValue)) {
          result.addTag(field.getName());
          result.addXml(valueString);
          result.addEndTag(field.getName());
        }
        else {
          if (valueString == null)  valueString = "";
          else valueString = StringEscapeUtils.escapeXml(valueString);

          result.addTagAndText(field.getName(), valueString, true);
        }
      }
    }
  
    private final boolean addXmlAttribute(StringBuilder tag) {
      boolean result = false;

      if (field.isRepeated() ||
          field.getJavaType() == FieldDescriptor.JavaType.MESSAGE ||
          field.getType() == FieldDescriptor.Type.GROUP) {
        // no single value here
        return result;
      }

      final String valueString = getValueString(value);

      if (valueString == null || (embedXml && looksLikeXml(value))) {
        // embed this instead of adding as attribute
        result = false;
      }
      else {
        // embed as attribute
        tag.append(' ').append(field.getName()).append("='").append(StringEscapeUtils.escapeXml(valueString)).append('\'');

        result = true;
      }

      return result;
    }

    private final boolean looksLikeXml(Object override) {
      Boolean result = looksLikeXml.get(override);

      if (result == null) {
        result = computeLooksLikeXml(override);
        looksLikeXml.put(override, result);
      }

      return result;
    }

    private final boolean computeLooksLikeXml(Object override) {
      boolean result = false;

      final String string = getValueString(override);
      if (string == null) return result;

      final int len = string.length();

      // first non-white must be '<'
      for (int i = 0; i < len; ++i) {
        final char c = string.charAt(i);
        if (!Character.isWhitespace(c)) {
          if (c == '<') {
            result = true;
          }
          break;
        }
      }

      if (result) {
        // last non-white must be '>'
        for (int i = len - 1; i >= 0; --i) {
          final char c = string.charAt(i);
          if (!Character.isWhitespace(c)) {
            if (c != '>') {
              result = false;
            }
            break;
          }
        }
      }

      return result;
    }

    private final String getValueString(Object override) {
      String result = value2string.get(override);

      if (result == null) {
        result = computeValueString(override);
        if (result != null) {
          value2string.put(override, result);
        }
      }

      return result;
    }

    private final String computeValueString(Object value) {
      String result = null;

      switch (field.getType()) {
        case INT32:
        case SINT32:
        case SFIXED32:
          result = ((Integer)value).toString();
          break;
    
        case INT64:
        case SINT64:
        case SFIXED64:
          result = ((Long)value).toString();
          break;
    
        case BOOL:
          result = ((Boolean)value).toString();
          break;
    
        case FLOAT:
          result = ((Float)value).toString();
          break;
    
        case DOUBLE:
          result = ((Double)value).toString();
          break;
    
        case UINT32:
        case FIXED32:
          result = unsignedToString((Integer) value);
          break;
    
        case UINT64:
        case FIXED64:
          result = unsignedToString((Long)value);
          break;
    
        case STRING:
          result = (String)value;
          break;
    
        case BYTES:
          result = escapeBytes((ByteString)value);
          break;
    
        case ENUM:
          result = ((EnumValueDescriptor)value).getName();
          break;
    
        case MESSAGE:
        case GROUP:
          result = null;  // deep values not supported here
          break;
      }

      return result;
    }
  }


  /**
   * Auxiliary to iterate over the messages in log files, printing them to
   * stdout.
   * <ul>
   * <li>args[0] holds the classpath for the message instances in the log
   *     (i.e. 'mypackage.MyProtoWrapper$MyMessageType')</li>
   * <li>args[1+] hold the paths to log files to dump.</li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public static final void main(String[] args) throws Exception {
    // arg0: protobuf class
    // args1+: proto log files

    final Class<? extends Message> messageClass = (Class<? extends Message>)Class.forName(args[0]);
    for (int i = 1; i < args.length; ++i) {
      final MultiLogIterator msgIter = new MultiLogIterator(new File(args[i]), messageClass, ProtoLogStreamer.DEFAULT_INSTANCE, ProtoLogStreamer.MAX_MESSAGE_BYTES);
      while (msgIter.hasNext()) {
        final Message message = msgIter.next();
        final XmlStringBuilder xml = new XmlStringBuilder("p");
        Message2Xml.DEFAULT.asXml(message, xml);
        final StringBuilder output = new StringBuilder();
        xml.getXmlElement().asPrettyString(output, 0, 2);
        System.out.println(output.toString());
      }
    }
  }
}
