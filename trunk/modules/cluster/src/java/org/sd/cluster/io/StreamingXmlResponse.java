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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.sd.xml.XmlStringBuilder;

/**
 * XmlResponse that includes streaming file data in addition to the XML.
 * <p>
 * @author Spence Koehler
 */
public abstract class StreamingXmlResponse extends XmlResponse {
  
  /** Write streaming data portion of the response (after xml). */
  protected abstract void writeStreamingData(DataOutput dataOutput) throws IOException;

  /** Read streaming data portion of the response (after xml). */
  protected abstract void readStreamingData(DataInput dataInput) throws IOException;


  /**
   * Empty constructor for publishable reconstruction.
   */
  protected StreamingXmlResponse() {
    super();
  }

  /**
   * Construct with the given xml and outputFilePath.
   */
  protected StreamingXmlResponse(XmlStringBuilder xmlStringBuilder) {
    super(xmlStringBuilder);
  }

  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    writeStreamingData(dataOutput);
  }

  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);
    readStreamingData(dataInput);
  }
}
