package org.sd.util;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * Utilities for manipulating ByteBuffers
 * <p>
 * @author 
 */
public class ByteBufferUtil
{
  public static ByteBuffer putUnsignedShort(ByteBuffer bytes, int value)
    throws IOException
  {
    if(value > 65535)
      throw new IOException("int value exceeds maximum unsigned short value of 65535!");

    byte b1 = (byte)(0xff & (value >> 8));
    byte b2 = (byte)(0xff & value);

    bytes.put(b1);
    bytes.put(b2);
    
    return bytes;
  }

  public static ByteBuffer putUnsignedShort(ByteBuffer bytes, int pos, int value)
    throws IOException
  {
    if(value > 65535)
      throw new IOException("int value exceeds maximum unsigned short value of 65535!");

    byte b1 = (byte)(0xff & (value >> 8));
    byte b2 = (byte)(0xff & value);

    bytes.put(pos, b1);
    bytes.put(pos+1, b2);
    
    return bytes;
  }

  public static int getUnsignedShort(ByteBuffer bytes)
    throws IOException
  {
    byte b1 = bytes.get();
    byte b2 = bytes.get();
    
    int value = (int)((0xff & b1) << 8 | (0xff & b2));
    return value;
  }

  public static int getUnsignedShort(ByteBuffer bytes, int pos)
    throws IOException
  {
    byte b1 = bytes.get(pos);
    byte b2 = bytes.get(pos+1);

    int value = (int)((0xff & b1) << 8 | (0xff & b2));
    return value;
  }

  
  public static ByteBuffer putAscii(ByteBuffer bytes, String value)
    throws IOException
  {
    byte[] asciiBytes = value.getBytes("US-ASCII");
    int numBytes = asciiBytes.length;
    if(numBytes > 65535)
      throw new IOException("ASCII string exceeds maximum size of 65535 bytes!");

    putUnsignedShort(bytes, numBytes);
    for(int i = 0; i < numBytes; i++)
      bytes.put(asciiBytes[i]);

    return bytes;
  }

  public static ByteBuffer putAscii(ByteBuffer bytes, int pos, String value)
    throws IOException
  {
    byte[] asciiBytes = value.getBytes("US-ASCII");
    int numBytes = asciiBytes.length;
    if(numBytes > 65535)
      throw new IOException("ASCII string exceeds maximum size of 65535 bytes!");
    
    putUnsignedShort(bytes, pos, numBytes);
    pos += 2;
    for(int i = 0; i < numBytes; i++)
      bytes.put(pos+i, asciiBytes[i]);

    return bytes;
  }

  public static String getAscii(ByteBuffer bytes)
    throws IOException
  {
    StringBuilder builder = new StringBuilder();
    
    int numBytes = getUnsignedShort(bytes);
    for(int i = 0; i < numBytes; i++)
      builder.append((char)bytes.get());
    
    return builder.toString();
  }

  public static String getAscii(ByteBuffer bytes, int pos)
    throws IOException
  {
    StringBuilder builder = new StringBuilder();
    
    int numBytes = getUnsignedShort(bytes, pos);
    pos += 2;
    for(int i = 0; i < numBytes; i++)
      builder.append((char)bytes.get(pos+i));

    return builder.toString();
  }

  public static ByteBuffer putTruncatedInt(ByteBuffer bytes, int value, int numBytes)
    throws IOException
  {
    if(numBytes <= 0)
      throw new IOException("too few bytes specified for truncated int value!");
    else if(numBytes >= 4)
      return bytes.putInt(value);
      
    int maxValue = 0xff;
    for(int i = 1; i < numBytes; i++)
      maxValue = (maxValue << 8) | 0xff;

    if(value > maxValue)
      throw new IOException("int value exceeds maximum truncated int value of "+maxValue+"!");

    for(int i = (numBytes-1); i >= 0; i--)
      bytes.put((byte)(0xff & (value >> (8*i))));
    
    return bytes;
  }

  public static ByteBuffer putTruncatedInt(ByteBuffer bytes, int pos, int value, int numBytes)
    throws IOException
  {
    if(numBytes <= 0)
      throw new IOException("too few bytes specified for truncated int value!");
    else if(numBytes >= 4)
      return bytes.putInt(pos, value);
      
    int maxValue = 0x00000000;
    for(int i = 0; i < numBytes; i++)
      maxValue = (maxValue << (8*i)) | 0xff;

    if(value > maxValue)
      throw new IOException("int value exceeds maximum truncated int value of "+maxValue+"!");

    for(int i = (numBytes-1); i >= 0; i--)
      bytes.put(pos++,(byte)(0xff & (value >> (8*i))));
    
    return bytes;
  }

  public static int getTruncatedInt(ByteBuffer bytes, int numBytes)
    throws IOException
  {
    if(numBytes <= 0)
      throw new IOException("too few bytes specified for truncated int value!");
    else if(numBytes >= 4)
      return bytes.getInt();

    int value = 0;
    for(int i = (numBytes-1); i >= 0; i--)
      value |= ((bytes.get() & 0xff) << (8*i));
    return value;
  }

  public static int getTruncatedInt(ByteBuffer bytes, int pos, int numBytes)
    throws IOException
  {
    if(numBytes <= 0)
      throw new IOException("too few bytes specified for truncated int value!");
    else if(numBytes >= 4)
      return bytes.getInt(pos);

    int value = 0;
    for(int i = (numBytes-1); i >= 0; i--)
      value |= ((bytes.get(pos++) & 0xff) << (8*i));
    return value;
  }
}
