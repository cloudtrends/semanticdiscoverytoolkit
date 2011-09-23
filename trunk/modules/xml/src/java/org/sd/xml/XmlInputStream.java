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
package org.sd.xml;


import org.sd.util.StringUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An input stream for xml (html) that attempts to find the data's charset
 * and decode accordingly.
 * <p>
 * @author Spence Koehler
 */
public class XmlInputStream {
  
  private static final boolean DEFAULT_TO_ASCII_IF_CAN = true;
  private static final boolean DEFAULT_TO_UTF8_IF_CAN = true;
  private static final int READ_LIMIT = 32 * 1024 - 100;  // 32K read limit

  /** Mapping for Windows Western character set (128-159) to Unicode */
  private static final int[] WIN2UNICODE = {
    0x20AC, 0x0000, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
    0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x0000, 0x017D, 0x0000,
    0x0000, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
    0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x0000, 0x017E, 0x0178
  };

  /** Mapping MacRoman character set to Unicode */
  private static final int[] MAC2UNICODE = {

    0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
    0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,

    0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0016, 0x0017,
    0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D, 0x001E, 0x001F,

    0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027,
    0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,

    0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
    0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,

    0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
    0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,

    0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
    0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F,

    0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
    0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,

    0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
    0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0x007F,
    /* x7F = DEL */
    0x00C4, 0x00C5, 0x00C7, 0x00C9, 0x00D1, 0x00D6, 0x00DC, 0x00E1,
    0x00E0, 0x00E2, 0x00E4, 0x00E3, 0x00E5, 0x00E7, 0x00E9, 0x00E8,

    0x00EA, 0x00EB, 0x00ED, 0x00EC, 0x00EE, 0x00EF, 0x00F1, 0x00F3,
    0x00F2, 0x00F4, 0x00F6, 0x00F5, 0x00FA, 0x00F9, 0x00FB, 0x00FC,

    0x2020, 0x00B0, 0x00A2, 0x00A3, 0x00A7, 0x2022, 0x00B6, 0x00DF,
    0x00AE, 0x00A9, 0x2122, 0x00B4, 0x00A8, 0x2260, 0x00C6, 0x00D8,

    0x221E, 0x00B1, 0x2264, 0x2265, 0x00A5, 0x00B5, 0x2202, 0x2211,
    0x220F, 0x03C0, 0x222B, 0x00AA, 0x00BA, 0x03A9, 0x00E6, 0x00F8,

    0x00BF, 0x00A1, 0x00AC, 0x221A, 0x0192, 0x2248, 0x2206, 0x00AB,
    0x00BB, 0x2026, 0x00A0, 0x00C0, 0x00C3, 0x00D5, 0x0152, 0x0153,

    0x2013, 0x2014, 0x201C, 0x201D, 0x2018, 0x2019, 0x00F7, 0x25CA,
    0x00FF, 0x0178, 0x2044, 0x20AC, 0x2039, 0x203A, 0xFB01, 0xFB02,

    0x2021, 0x00B7, 0x201A, 0x201E, 0x2030, 0x00C2, 0x00CA, 0x00C1,
    0x00CB, 0x00C8, 0x00CD, 0x00CE, 0x00CF, 0x00CC, 0x00D3, 0x00D4,
    /* xF0 = Apple Logo */
    0xF8FF, 0x00D2, 0x00DA, 0x00DB, 0x00D9, 0x0131, 0x02C6, 0x02DC,
    0x00AF, 0x02D8, 0x02D9, 0x02DA, 0x00B8, 0x02DD, 0x02DB, 0x02C7
  };


  private enum State {ASCII, ESC, ESCD, ESCDP, ESCP, NONASCII;};

  private MyBufferedInputStream inputStream;
  private Encoding encoding;
  private String explicitCharset;
  private State state;
  private InputStreamReader inputStreamReader;

  private boolean throwEncodingException;
  private boolean hitEncodingException;
  private Boolean foundXmlTag;

  /**
   * Create an xml input stream with an unknown (to be determined) encoding.
   */
  public XmlInputStream(InputStream in) throws IOException {
    this.inputStream = new MyBufferedInputStream(in, READ_LIMIT + 100);
    this.encoding = determineEncoding();
    this.state = State.ASCII;
    this.inputStreamReader = null;
    this.throwEncodingException = true;
    this.hitEncodingException = false;
    this.foundXmlTag = null;
  }

  public XmlInputStream(InputStream in, Encoding encoding) {
    this.inputStream = new MyBufferedInputStream(in, READ_LIMIT + 100);
    this.encoding = encoding;
    this.explicitCharset = null;
    this.state = State.ASCII;
    this.inputStreamReader = null;
    this.throwEncodingException = true;
    this.hitEncodingException = false;
    this.foundXmlTag = null;
  }

  public void setThrowEncodingException(boolean throwEncodingException) {
    this.throwEncodingException = throwEncodingException;
  }

  public boolean hitEncodingException(boolean reset) {
    final boolean result = hitEncodingException;
    if (reset) this.hitEncodingException = false;
    return result;
  }

  public void close() throws IOException {
    this.inputStream.close();
  }

  /**
   * Determine whether (from the current point in this stream) an xml tag is
   * found within READ_LIMIT chars. Leave the stream's read position unchanged.
   */
  public boolean foundXmlTag() throws IOException {
    if (foundXmlTag == null) {
      this.foundXmlTag = findFirstTag();
    }
    return foundXmlTag;
  }

  public Encoding getEncoding() {
    return encoding;
  }

  public void setEncoding(Encoding encoding) {
    this.encoding = encoding;
  }

  public String getExplicitCharset() {
    return explicitCharset;
  }

  /**
   * Read the next char from the stream.
   *
   * @return the next char or -1 if there are no more chars to read.
   */
  public int read() throws IOException {
    int result = read(encoding, true);

    if (result >= 0 && !Character.isValidCodePoint(result)) {
      this.hitEncodingException = true;
      final String message = "invalid codePoint " + result + " found (bytepos=" + getBytePosition() + " encoding=" + getEncoding() + ")";

      if (throwEncodingException) {
        throw new EncodingException(message);
      }
      else {
        System.err.println(message);
        result = '?';
      }
    }

    return result;
  }

  public int getBytePosition() {
    return inputStream.getPos();
  }

  /**
   * Read to (through) the given char.
   * <p>
   * NOTE: the sought char (or interruptChar) is read from the inputStream,
   *       but not appended to the result.
   *
   * @param theChar        the char to read to.
   * @param result         container to place the characters read (if non-null).
   * @param interruptChar  an alternate char to stop at if it is seen before theChar (-1 to disable)
   * 
   * @return the char that was reached (either theChar or interruptChar) or -1 if we reached the end of the stream.
   */
  public int readToChar(char theChar, StringBuilder result, int interruptChar) throws IOException {
    int codePoint = -1;

    while (true) {
      codePoint = read();
      if (codePoint == -1 || codePoint == theChar || codePoint == interruptChar) break;
      if (result != null) result.appendCodePoint(codePoint);
    }

    return codePoint;
  }

  /**
   * Read the next char according to the given encoding.
   *
   * @param encoding  The encoding of the character being read.
   * @param fixCR     True when really reading; false when searching for encoding info.
   *
   * @return the next char or -1 if there are no more chars to read.
   */
  private int read(Encoding encoding, boolean fixCR) throws IOException {
    // adapted from JTidy's org.w3c.tidy.StreamInImpl
    int c = -1;

    while (true) {
      if (fixCR) inputStream.mark(6);
      c = doRead(encoding);

      if (fixCR && c >= 0 && !Character.isValidCodePoint(c)) {
        // oops, got out of whack. just consume a single character.
        hitEncodingException = true;
        inputStream.reset();
        c = inputStream.read();
      }

      if (c < 0) break;  // end of stream.
      if (c == '\n') break;
      if (c == '\r') {
        if (fixCR) {
          inputStream.mark(6);  // unicode chars can be up to 6 bytes
          c = doRead(encoding);
          if (c != '\n') {
            inputStream.reset();
            c = '\n';
          }
        }
        break;
      }
      if (c == '\t') break;

      // strip control characters, except for ESC */
      if (c == 27) break;
      if (0 < c && c < 32) continue;

      // watch out for ISO2022
      if (encoding == Encoding.RAW || encoding == Encoding.ISO2022) break;

      if (encoding == Encoding.MACROMAN) c = MAC2UNICODE[c];

      // produced e.g. as a side-effect of smart quotes in Word
      if (127 < c && c < 160) {
        c = WIN2UNICODE[c - 128];
        if (c == 0) continue;
      }

      break;
    }

    return c;
  }

  private final int readCharFromReader(Encoding encoding) throws IOException {
    if (inputStreamReader == null) {
      inputStreamReader = new InputStreamReader(inputStream, encoding.getLabel());
    }
    return inputStreamReader.read();
  }

  // protected for JUnit access.
  protected final int doRead(Encoding encoding) throws IOException {
    if (encoding.useReader()) {
      return readCharFromReader(encoding);
    }

    int c = inputStream.read();
    if (c < 0) return c;  // end of stream

    
    // A document in ISO-2022 based encoding uses some ESC sequences
    // called "designator" to switch character sets. The designators
    // defined and used in ISO-2022-JP are:
    //
    // "ESC" + "(" + ?     for ISO646 variants
    //
    // "ESC" + "$" + ?     and
    // "ESC" + "$" + "(" + ?   for multibyte character sets
    //
    // Where ? stands for a single character used to indicate the
    // character set for multibyte characters.
    //
    // We handle this by preserving the escape sequence and
    // setting the top bit of each byte for non-ascii chars. This
    // bit is then cleared on output. The input stream keeps track
    // of the state to determine when to set/clear the bit.

    if (encoding == Encoding.ISO2022) {
      if (c == 0x1b) {  // ESC
        this.state = State.ESC;
        return c;
      }

      switch (this.state) {
        case ESC :
          if (c == '$') this.state = State.ESCD;
          else if (c == '(') this.state = State.ESCP;
          else this.state = State.ASCII;
          break;

        case ESCD :
          if (c == '(') this.state = State.ESCDP;
          else this.state = State.NONASCII;
          break;

        case ESCDP :
          this.state = State.NONASCII;
          break;

        case ESCP :
          this.state = State.ASCII;
          break;

        case NONASCII :
          c |= 0x80;
          break;
      }

      return c;
    }

    if (encoding != Encoding.UTF8) return c;

    // deal with utf-8 encoded char
    int n = -1;
    int count = 0;

    if ((c & 0xE0) == 0xC0) {  // 110X XXXX two bytes
      n = c & 31;
      count = 1;
    }
    else if ((c & 0xF0) == 0xE0) {  // 1110 XXXX three bytes
      n = c & 15;
      count = 2;
    }
    else if ((c & 0xF8) == 0xF0) {  // 1111 0XXX four bytes
      n = c & 7;
      count = 3;
    }
    else if ((c & 0xFC) == 0xF8) {  // 1111 10XX five bytes
      n = c & 3;
      count = 4;
    }
    else if ((c & 0xFE) == 0xFC) {  // 1111 110X six bytes
      n = c & 1;
      count = 5;
    }
    else {  // 0XXX XXXX one byte
      return c;
    }

    // successor bytes should have the form 10XX XXXX
    for (int i = 0; i < count; ++i) {
      c = inputStream.read();
      if (c < 0) return c;
      n = (n << 6) | (c & 0x3F);
    }

    return n;
  }

  private final Encoding determineEncoding() throws IOException {
    boolean triedAscii = false;
    boolean triedUtf8 = false;

    // search for "charset="
    final Encoding explicitResult = findExplicitCharset();

    if (explicitResult != null) {
      if (explicitResult == Encoding.UTF8) {
        if (utf8Okay()) return explicitResult;
        triedUtf8 = true;
      }
      else if (explicitResult == Encoding.ASCII) {
        if (asciiOkay()) return explicitResult;
        triedAscii = true;
      }
    }

    // try reading as utf-8
    if (!triedUtf8 && DEFAULT_TO_UTF8_IF_CAN) {
      if (utf8Okay()) {
        return Encoding.UTF8;
      }
    }

    // try reading as ascii
    else if (!triedAscii && DEFAULT_TO_ASCII_IF_CAN) {
      if (asciiOkay()) {
        return Encoding.ASCII;
      }
    }
    //todo: implement more strategies as needed.

    // default to ASCII if we still couldn't figure it out.
    return Encoding.ASCII;
  }

  private final Encoding findExplicitCharset() throws IOException {
    Encoding result = null;

    // set stream to bounce back to this point.
    inputStream.mark(READ_LIMIT + 100);

    try {
      if (readToCharsetEquals()) {
        final String charsetValue = readCharsetValue();
        this.explicitCharset = charsetValue;
        result = decodeCharset(charsetValue);
      }
    }
    finally {
      // bounce stream back to mark
      inputStream.reset();
    }

    return result;
  }

  private final boolean readToCharsetEquals() throws IOException {
    final StringBuilder builder = new StringBuilder();
    int theLen = 0;  // will be 7 for charset= or 8 for encoding=

    // look for charset= or encoding=

    while (inputStream.getPos() < READ_LIMIT) {
      int c = read(Encoding.ASCII, false);
      if (c < 0) break;
      c = Character.toLowerCase(c);
      final int len = builder.length();

      if (len == 0) {
        if (c == 'c') {  // expect charset=
          theLen = 7;
          builder.append((char)c);
        }
        else if (c == 'e') {  // expect encoding=
          theLen = 8;
          builder.append((char)c);
        }
        // else drop char on floor.
      }
      else {
        if (len == theLen) {
          if (len == 7 && c == '=' && "charset".equals(builder.toString())) {
            // found it!
            return true;
          }
          else if (len == 8 && c == '=' && "encoding".equals(builder.toString())) {
            // found it!
            return true;
          }
          else {
            builder.setLength(0);  // clear false path
          }
        }
        else {
          if (StringUtil.isWhite(c)) {
            builder.setLength(0);  // clear false path
          }
          else {
            builder.append((char)c);
          }
        }
      }
    }

    return false;
  }

  private final String readCharsetValue() throws IOException {
    final StringBuilder result = new StringBuilder();

    // assume we've just read "charset=", read charset value beyond optional double-quote
    int c = read(Encoding.ASCII, false);
    if (Character.isLetterOrDigit(c)) result.append((char)c);  // optional double-quote was skipped
    while (true) {
      c = read(Encoding.ASCII, false);
      if (Character.isLetterOrDigit(c) || c == '-') {
        result.append((char)c);
      }
      else break;
    }

    return result.toString();
  }

  private final Encoding decodeCharset(String charset) {

    charset = charset.toLowerCase();
    Encoding result = Encoding.getEncoding(charset);

    if (result == null) {  // need further unaliasing

      if ("utf-8".equals(charset)) {
        result = Encoding.UTF8;
      }
      else if ("iso-2022".equals(charset)) {
        result = Encoding.ISO2022;
      }
      else if (charset.startsWith("iso-8859")) {
        result = Encoding.LATIN1;
      }
      else if (charset.startsWith("windows")) {
        result = Encoding.WINDOWS;
      }
      else if (charset.startsWith("mac")) {
        result = Encoding.MACROMAN;
      }
    }

    return result;
  }

  private final boolean asciiOkay() throws IOException {
    boolean result = true;

    inputStream.mark(READ_LIMIT + 100);

    try {
      while (inputStream.getPos() < READ_LIMIT) {
        final int c = read(Encoding.ASCII, false);
        if (c < 0) break;  // reached end
        if (EntityConverter.isGoodAscii(c)) {
          result = false;
          break;
        }
      }
    }
    finally {
      // bounce stream back to mark
      inputStream.reset();
    }

    return result;
  }

  private final boolean utf8Okay() throws IOException {
    boolean result = true;
    boolean sawLetterOrDigit = false;

    // set stream to bounce back to this point.
    inputStream.mark(READ_LIMIT + 100);

    try {
      while (inputStream.getPos() < READ_LIMIT) {
        final int c = read(Encoding.UTF8, false);
        if (c < 0) break;  // reached end
        if (!Character.isValidCodePoint(c)) {
          result = false;
          break;
        }
        else {
          final int type = Character.getType(c);
          if (type == Character.UNASSIGNED || type == Character.PRIVATE_USE) {
            result = false;
            break;
          }
        }

        if (!sawLetterOrDigit && Character.isLetterOrDigit(c)) {
          sawLetterOrDigit = true;
        }
      }
    }
    finally {
      // bounce stream back to mark
      inputStream.reset();
    }

    return result && sawLetterOrDigit;
  }

  private final boolean findFirstTag() throws IOException {
    boolean result = false;

    // set stream to bounce back to this point.
    inputStream.mark(READ_LIMIT + 100);

    try {
      result = findChars('<', '>', 32, 127, true);
    }
    finally {
      // bounce stream back to mark
      inputStream.reset();
    }

    return result;
  }

  /**
   * Read chars until findc1 is hit, then read to (and including) findc2.
   * Abort if anything read does not fall within minc (inclusive) and maxc (exclusive)
   * between the two find chars.
   * Do not exceed READ_LIMIT chars.
   * <p>
   * NOTE: all chars and limits are currently assumed to be in ASCII range.
   * <p>
   * This is used to determine whether the stream contains xml data by looking for
   * the first xml tag.
   *
   * @return true if found both chars in sequence without leaving the range; otherwise, false.
   */
  private final boolean findChars(int findc1, int findc2, int minc, int maxc, boolean allowWhite) throws IOException {

    boolean foundc1 = false;
    boolean foundc2 = false;

    while (inputStream.getPos() < READ_LIMIT) {
      int c = read(Encoding.ASCII, false);
      if (c < 0) break;  // end of stream

      if (!foundc1) {
        if (c == findc1) {
          foundc1 = true;
        }
      }
      else if (c == findc2) {
        foundc2 = true;
        break;
      }
      else if (c == findc1) {
        // don't allow embedded findc1
        break;
      }
      else if ((c < minc || c >= maxc)) {
        if (!allowWhite || !StringUtil.isWhite(c)) {
          // illegal char between foundc1 and foundc2
//          System.out.println("c=" + c);
          break;
        }
      }
    }

    return foundc1 && foundc2;
  }


  private static final class MyBufferedInputStream extends BufferedInputStream {
    public MyBufferedInputStream(InputStream in, int size) {
      super(in, size);
    }

    public int getPos() {
      return pos;
    }
  }
}
