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
package org.sd.atn.extract;


import org.sd.xml.DomContext;
import org.sd.xml.DomNode;

/**
 * An extraction for holding data based on an xml context.
 *
 * This class is abstract because extending classes must deal with setting
 * the Extraction's Text.
 * <p>
 * @author Spence Koehler
 */
public abstract class XmlExtraction extends Extraction {
  
  private DomContext context;
  /**
   * DomContext fully encompassing this extraction's text.
   */
  public DomContext getContext() {
    return context;
  }
  protected final void setContext(DomContext context) {
    this.context = context;
  }
  protected final void setNode(DomNode node) {
    this.context = node.getDomContext();
  }
  public final DomNode getNode() {
    return context.getDomNode();
  }


  /**
   * Construct with the given type, expecting to set the Text and Context later.
   */
  protected XmlExtraction(String type) {
    super(type);
  }

  /**
   * Construct with the given type and context, expecting to set the Text later.
   */
  protected XmlExtraction(String type, DomContext domContext) {
    super(type);
    this.context = domContext;
  }
}
