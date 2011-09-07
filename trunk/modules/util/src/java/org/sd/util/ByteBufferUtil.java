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
package org.sd.util;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.UUID;

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
    if(bytes.remaining() < 2)
      throw new IOException("not enough space in buffer to write unsigned short");

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
    if((pos + 2) > bytes.limit())
      throw new IOException("not enough space in buffer to write unsigned short");

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
    if(bytes.remaining() < 2)
      throw new IOException("too few bytes specified for unsigned short value!");

    byte b1 = bytes.get();
    byte b2 = bytes.get();
    
    int value = (int)((0xff & b1) << 8 | (0xff & b2));
    return value;
  }

  public static int getUnsignedShort(ByteBuffer bytes, int pos)
    throws IOException
  {
    if((pos + 2) > bytes.limit())
      throw new IOException("too few bytes specified for unsigned short value!");

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

    if(bytes.remaining() < (numBytes + 2))
      throw new IOException("not enough space in buffer to write string value");

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

    if((pos + numBytes + 2) > bytes.limit())
      throw new IOException("not enough space in buffer to write string value");
    
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
    
    if(bytes.remaining() < 2)
      throw new IOException("too few bytes specified for string value!");
    int numBytes = getUnsignedShort(bytes);
    if(bytes.remaining() < numBytes)
      throw new IOException("too few bytes specified for string value!");
    for(int i = 0; i < numBytes; i++)
      builder.append((char)bytes.get());
    
    return builder.toString();
  }

  public static String getAscii(ByteBuffer bytes, int pos)
    throws IOException
  {
    StringBuilder builder = new StringBuilder();
    
    if((pos + 2) > bytes.limit())
      throw new IOException("too few bytes specified for string value!");
    int numBytes = getUnsignedShort(bytes, pos);
    pos += 2;
    if((pos + numBytes) > bytes.limit())
      throw new IOException("too few bytes specified for string value!");
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
    {
      if(bytes.remaining() < 4)
        throw new IOException("not enough space in buffer to write truncated int value");
      return bytes.putInt(value);
    }
      
    if(bytes.remaining() < numBytes)
      throw new IOException("not enough space in buffer to write truncated int value");

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
    {
      if((pos + 4) > bytes.limit())
        throw new IOException("not enough space in buffer to write truncated int value");
      return bytes.putInt(pos, value);
    }
      
    if((pos + numBytes) > bytes.limit())
      throw new IOException("not enough space in buffer to write truncated int value");

    int maxValue = 0xff;
    for(int i = 1; i < numBytes; i++)
      maxValue = (maxValue << 8) | 0xff;

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
    {
      if(bytes.remaining() < 4)
        throw new IOException("too few bytes specified for truncated int value!");
      return bytes.getInt();
    }

    if(bytes.remaining() < numBytes)
      throw new IOException("too few bytes specified for truncated int value!");

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
    {
      if((pos + 4) > bytes.limit())
        throw new IOException("too few bytes specified for truncated int value!");
      return bytes.getInt(pos);
    }

    if((pos + numBytes) > bytes.limit())
      throw new IOException("too few bytes specified for truncated int value!");

    int value = 0;
    for(int i = (numBytes-1); i >= 0; i--)
      value |= ((bytes.get(pos++) & 0xff) << (8*i));
    return value;
  }

  public static ByteBuffer putUUID(ByteBuffer bytes, UUID uuid)
    throws IOException
  {
    if(bytes.remaining() < 16)
      throw new IOException("not enough space in buffer to write UUID value!");
      
    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    
    bytes.putLong(msb);
    bytes.putLong(lsb);
    return bytes;
  }

  public static ByteBuffer putUUID(ByteBuffer bytes, int pos, UUID uuid)
    throws IOException
  {
    if((pos + 16) > bytes.limit())
      throw new IOException("not enough space in buffer to write UUID value!");

    long msb = uuid.getMostSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    
    bytes.putLong(pos, msb);
    bytes.putLong(pos+8, lsb);
    return bytes;
  }

  public static UUID getUUID(ByteBuffer bytes)
    throws IOException
  {
    if(bytes.remaining() < 16)
      throw new IOException("too few bytes specified for UUID value!");
      
    long msb = bytes.getLong();
    long lsb = bytes.getLong();
    
    UUID value = new UUID(msb, lsb);
    return value;
  }

  public static UUID getUUID(ByteBuffer bytes, int pos)
    throws IOException
  {
    if((pos + 16) > bytes.limit())
      throw new IOException("too few bytes specified for UUID value!");

    long msb = bytes.getLong(pos);
    long lsb = bytes.getLong(pos+8);
    
    UUID value = new UUID(msb, lsb);
    return value;
  }
}
