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

import org.sd.io.DataHelper;
import org.sd.io.Publishable;
import org.sd.util.LineBuilder;
import org.sd.util.tree.Tree;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HtmlLink implements Publishable {

  private String _xmlText;
  private Tree<XmlLite.Data> _xmlNode;

  private String _hrefText;
  private String _hyperText;

  /**
   * Default constructor for publishable reconstruction.
   */
  public HtmlLink() {
    this._xmlText = null;
    this._xmlNode = null;

    this._hrefText = null;
    this._hyperText = null;
  }
  
  /**
   * Construct HtmlLink from a string of XML
   */
  public HtmlLink(String xmlText) {
    this._xmlText = xmlText;
    this._xmlNode =  null;
  }
  
  /**
   * Construct HtmlLink from a Tree of xml data
   */
  public HtmlLink(Tree<XmlLite.Data> xmlNode){
    this._xmlNode = xmlNode;
    this._xmlText = null;
  }
  
  public String getXmlText() {
    if (_xmlText == null) {
      try {
        _xmlText = XmlLite.asXml(_xmlNode, false);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return _xmlText;
  }

  public Tree<XmlLite.Data> getXmlNode() {
    if (_xmlNode == null) {
      try {
        _xmlNode = XmlFactory.buildXmlTree(_xmlText, true, true);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return _xmlNode;
  }

  public String getHrefText() {
    if (_hrefText == null) {
      _hrefText = XmlTreeHelper.getAttribute(getXmlNode(), "href");
    }
    return _hrefText;
  }

  public String getHyperText() {
    if (_hyperText == null) {
      _hyperText = XmlTreeHelper.getAllText(getXmlNode());
    }
    return _hyperText;
  }


  public String toString(){
    return getXmlText();
  }

  public String asString(){
    final LineBuilder result = new LineBuilder();
    
    result.
      append(getXmlText()).
      append(getHrefText()).
      append(getHyperText());

    return result.toString();
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    DataHelper.writeString(dataOutput, getXmlText());
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
    this._xmlText = DataHelper.readString(dataInput);
  }
}
