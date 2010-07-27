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
package org.sd.atn;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import org.sd.io.DataHelper;
import org.sd.io.Publishable;

/**
 * Container for parse source information.
 * <p>
 * @author Spence Koehler
 */
public class ParseSourceInfo implements Publishable {

  private String inputString;
  private boolean inputIsXml;
  private boolean inputIsHtml;
  private boolean inputIsFile;
  private String diffString;
  private String url;
  private String diffUrl;
  
  public ParseSourceInfo() {
    this.inputString = null;
    this.inputIsXml = false;
    this.inputIsHtml = false;
    this.inputIsFile = false;
    this.diffString = null;
    this.url = null;
    this.diffUrl = null;
  }

  public ParseSourceInfo(File file, boolean isXml, boolean isHtml) {
    this(file.getAbsolutePath(), isXml, isHtml, true, null, null, null);
  }

  public ParseSourceInfo(String inputString, boolean inputIsXml, boolean inputIsHtml, boolean inputIsFile, String diffString, String url, String diffUrl) {
    this.inputString = inputString;
    this.inputIsXml = inputIsXml || inputIsHtml;
    this.inputIsHtml = inputIsHtml;
    this.inputIsFile = inputIsFile;
    this.diffString = diffString;
    this.url = url;
    this.diffUrl = diffUrl;
  }

  public String getInputString() {
    return inputString;
  }
  public void setInputString(String inputString) {
    this.inputString = inputString;
  }

  public boolean inputIsXml() {
    return inputIsXml;
  }
  public void setInputIsXml(boolean inputIsXml) {
    this.inputIsXml = inputIsXml;
  }

  public boolean inputIsHtml() {
    return inputIsHtml;
  }
  public void setInputIsHtml(boolean inputIsHtml) {
    if (inputIsHtml) this.inputIsXml = true;
    this.inputIsHtml = inputIsHtml;
  }

  public boolean inputIsFile() {
    return inputIsFile;
  }
  public void setInputIsFile(boolean inputIsFile) {
    this.inputIsFile = inputIsFile;
  }

  public String getDiffString() {
    return diffString;
  }
  public void setDiffString(String diffString) {
    this.diffString = diffString;
  }

  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  public String getDiffUrl() {
    return diffUrl;
  }
  public void setDiffUrl(String diffUrl) {
    this.diffUrl = diffUrl;
  }

  /**
   * source types: xmlfile, htmlfile, flatfile, xml, string
   */
  public String getSourceType() {
    String result = "";

    if (inputIsFile) {
      result = "file";
    }

    if (inputIsXml) {
      if (inputIsHtml) {
        result = "html" + result;
      }
      else {
        result = "xml" + result;
      }
    }
    else {
      if (inputIsFile) {
        result = "flat" + result;
      }
      else {
        result = "string" + result;
      }
    }

    return result;
  }

  /**
   * diff types: <null>, difffile, diffxml, diff
   */
  public String getSourceDiffType() {
    String result = null;

    if (diffString != null) {
      result = "diff";

      if (inputIsFile) {
        result = result + "file";
      }
      else if (inputIsXml) {
        if (inputIsHtml) {
          result = result + "html";
        }
        else {
          result = result + "xml";
        }
      }
    }

    return result;
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    DataHelper.writeString(dataOutput, inputString);
    dataOutput.writeBoolean(inputIsXml);
    dataOutput.writeBoolean(inputIsHtml);
    dataOutput.writeBoolean(inputIsFile);
    DataHelper.writeString(dataOutput, diffString);
    DataHelper.writeString(dataOutput, url);
    DataHelper.writeString(dataOutput, diffUrl);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.inputString = DataHelper.readString(dataInput);
    this.inputIsXml = dataInput.readBoolean();
    this.inputIsHtml = dataInput.readBoolean();
    this.inputIsFile = dataInput.readBoolean();
    this.diffString = DataHelper.readString(dataInput);
    this.url = DataHelper.readString(dataInput);
    this.diffUrl = DataHelper.readString(dataInput);
  }
}
