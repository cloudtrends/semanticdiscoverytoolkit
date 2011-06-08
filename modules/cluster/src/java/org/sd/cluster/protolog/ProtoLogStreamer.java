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


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper class for streaming multiple protolog messages.
 * <p>
 * @author Spence Koehler
 */
public class ProtoLogStreamer {
  
	/**
	 * The default maximum length of a message to be logged, currently set to 16K.
	 * <p>
	 * If a message to be logged exceeds this size, a MessageTooLongException
	 * will be thrown. Log-building tools such as the RollingOutputStreamStrategy
	 * will catch this exception and maintain a count of its occurrence so that
	 * the value's effect can be monitored and appropriately adjusted if/when
	 * needed.
	 */
	public static final int MAX_MESSAGE_BYTES = 16 * 1024;	 // don't accept messages longer than 16K

  /**
   * A marker to precede the length. Currently 0xFFFFFFFFFF (5 bytes of 1's).
   * <p>
   * Note that we use an odd number of bytes to shift normal byte alignments,
   * rendering the matching of the patterns in random data less likely.
   */
  public static final byte[] PRE_MARKER = new byte[] {
    (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
  };

  /**
   * A marker to follow the length. Currently 0x0000000000 (5 bytes of 0's).
   * <p>
   * Note that we use an odd number of bytes to shift normal byte alignments,
   * rendering the matching of the patterns in random data less likely.
   */
  public static final byte[] POST_MARKER = new byte[] {
    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
  };


  /**
   * A default instance using default values.
   */
  public static final ProtoLogStreamer DEFAULT_INSTANCE = new ProtoLogStreamer();


  private byte[] preMarker;
  private byte[] postMarker;
  private int markwidth;

  /**
   * A variable that can be queried (e.g. through jmx) to determine how many
   * times we erroneously encountered false-positive marker patterns.
   */
  public final AtomicLong falsePositiveCount = new AtomicLong(0);

  /**
   * Construct with default values.
   */
  public ProtoLogStreamer() {
    this(null, null);
  }

  /**
   * Construct with the given values.
   * <p>
   * Note that the serializer/deserializer for a message stream must share the
   * same parameters.
   */
  public ProtoLogStreamer(byte[] preMarker, byte[] postMarker) {
    this.preMarker = preMarker == null ? PRE_MARKER : preMarker;
    this.postMarker = postMarker == null ? POST_MARKER : postMarker;
    this.markwidth = this.preMarker.length + this.postMarker.length + 2;
  }

  /**
   * Auxiliary to serialize a single message for the stream of a sequence of
   * messages.
   * <p>
   * It is assumed that the messageBytes have already been checked for length.
   * Conforming to a maximum length (in bytes) is important for proper
   * deserialization of the written bytes.
   * <p>
   * Since protobuf instances don't serialize in a way that recognizes instance
   * boundaries, we need this wrapper when writing multiple messages to a
   * stream in order to reliably decode the messages read from the stream.
   * <p>
   * Here, we write the size of the message bytes followed by the bytes to the
   * output stream.
   * <p>
   * Further, in order to identify record boundaries from an arbitrary position
   * in the stream, we delimit the byte count with pre- and post- marker bytes.
   * <p>
   * Lastly, we will re-write the number of bytes in the message after the
   * message bytes as a checksum to validate the presence of a message in the
   * stream.
   * <p>
   * So, a full "record" has the following format:
   * <table border="1">
   * <tr><td>pre-marker (e.g. 5 bytes of 1's)</td></tr>
   * <tr><td>numBytes (4 byte integer, x, where 0 &lt; x &lt; e.g. 16K)</td></tr>
   * <tr><td>post-marker (e.g. 5 bytes of 0's)</td></tr>
   * <tr><td>serialized message bytes (limited to e.g. 16K bytes)</td></tr>
   * <tr><td>checksum (4 byte integer equals numBytes)</td></tr>
   * </table>
   *
   * @param message   The message to write (must be non-null).
   * @param out   The stream to write the message to (okay if null, message bytes will still be returned).
   *
   * NOTE: This assumes that no single message will have more than Integer.MAX_VALUE bytes.
   *
   * @return the message's bytes.
   */
  public final void writeTo(byte[] messageBytes, DataOutputStream out) throws IOException {
    if (out != null && messageBytes != null) {
      final int len = messageBytes.length;

      synchronized (out) {
        writeLength(out, len);    // write length and markers
        out.write(messageBytes);  // write the bytes
        out.writeInt(len);        // write the checksum
        out.flush();
      }
    }
  }

  /**
   * Read the next message, which has the given message class, from the input
   * stream.
   * <p>
   * The message to be read needs to be preceded by its byte size consistent
   * with how 'writeTo' writes the message.
   * <p>
   * When not at a pre-marker byte, the method will scan forward until the
   * appropriate pattern is found. If the message deserialization fails, then
   * the method will advance to the next marker pattern and try again.
   *
   * @param in  The stream from which to read the message (preceded by its size
   *            in bytes).
   * @param messageClass  The class of the message to reconstruct.
   * @param maxMessageBytes  The maximum number of bytes allowed for each
   *                         message (not including markers, etc.)
   *
   * NOTE: This assumes that no single message will have more than
   *       Integer.MAX_VALUE bytes.
   *
   * @return the next message.
   * @throws EOFException if the end of the stream is reached.
   */
  public final Message readMessageFrom(DataInputStream in, Class<? extends Message> messageClass, int maxMessageBytes) throws IOException {
    final Method parseMethod = ProtoLogUtil.getParseMethod(messageClass);
    return readMessageFrom(in, parseMethod, maxMessageBytes);
  }

  /**
   * Read the next message from the input stream using the given
   * Message.parseTo(byte[]) method.
   *
   * @param in  The stream from which to read the message (preceded by its size
   *            in bytes).
   * @param parseMethod   The parse method to reconstruct the message.
   * @param maxMessageBytes  The maximum number of bytes allowed for each
   *                         message (not including markers, etc.)
   *
   * NOTE: This assumes that no single message will have more than
   *       Integer.MAX_VALUE bytes.
   *
   * @return the next message.
   * @throws EOFException if the end of the stream is reached.
   */
  public final Message readMessageFrom(DataInputStream in, Method parseMethod, int maxMessageBytes) throws IOException {
    Message result = null;

    while (result == null) {
      final int numBytes = readLength(in, maxMessageBytes);  // does own marking/resetting to find length

      if (numBytes > 0) {
        in.mark(maxMessageBytes + 4);  // mark current stream position for reading the message plus the checksum

        final byte[] bytes = readMessageBytes(numBytes, in);
        final int checksum = in.readInt();

        if (checksum != numBytes) {
          in.reset();   // reset back to the start of the message (after the byte length)
          continue;     // and try again
        }

        try {
          result = ProtoLogUtil.parseMessage(bytes, parseMethod);
        }
        catch (InvalidProtocolBufferException e) {
          // assume we found a bad marker match and try again
          in.reset();   // reset back to the start of the message (after the byte length)
          //NOTE: we assume that the correct record boundary will start AFTER and never
          //      within the boundary that looked like the pre-marker, int, post-marker
          //      that got us here.

          // increment the false positive count
          falsePositiveCount.incrementAndGet();
        }
      }
    }

    return result;
  }

  /**
   * Read the next message's bytes from the input stream.
   *
   * @param numBytes  The number of bytes to read.
   * @param in  The stream from which to read the message.
   *
   * NOTE: This assumes that no single message will have more than
   *       Integer.MAX_VALUE bytes.
   *
   * @return the next message's bytes.
   * @throws EOFException if the end of the stream is reached.
   */
  private final byte[] readMessageBytes(int numBytes, DataInputStream in) throws IOException {
    final byte[] result = new byte[numBytes];
    in.readFully(result);
    return result;
  }

  /**
   * Write the length of the message with the pre- and post- marker bytes.
   */
  private final void writeLength(DataOutputStream out, int length) throws IOException {
    out.write(preMarker);
    out.writeInt(length);
    out.write(postMarker);
  }

  /**
   * Read the length of the message at the current position.
   * <p>
   * If the proper pattern is not found, then scan forward until it is found.
   *
   * @return the length of the message or -1 if no length can be found or read.
   */
  private final int readLength(DataInputStream in, int maxMessageBytes) throws IOException {
    int result = -1;

    while (result <= 0 || result > maxMessageBytes) {
      in.mark(markwidth);  // mark current stream position for reading the next N bytes

      if (readBytes(in, preMarker)) {
        result = in.readInt();
        if (!readBytes(in, postMarker)) {  // post-marker invalid
          result = -1;  // reset result
        }
      }

      if (result <= 0) {  // didn't find a valid length
        in.reset();   // pop back to the beginning
        in.skipBytes(1);  // move forward a byte in search of the start
      }
    }

    return result;
  }

  /**
   * Read bytes from the input stream that match 'verify'.
   *
   * @return true if all verify bytes were read from the stream; otherwise, false.
   */
  private final boolean readBytes(DataInputStream in, byte[] verify) throws IOException {
    boolean result = true;

    for (byte expect : verify) {
      final byte current = in.readByte();
      if (current != expect) {
        result = false;
        break;
      }
    }

    return result;
  }
}
