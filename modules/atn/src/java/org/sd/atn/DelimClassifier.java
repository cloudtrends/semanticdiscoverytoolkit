/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.List;
import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * Classifies a token based on its pre- or post- delims.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "This is a classifier wrapper for a org.sd.atn.DelimTest test."
  )
public class DelimClassifier extends AbstractAtnStateTokenClassifier {
  
  private DelimTest delimTest;

  //
  // <classifier-id ...>
  //   <jclass>org.sd.atn.DelimClassifier</jclass>
  //   <delim pre="false">
  //     <disallowall />
  //     <require type="substr">...</require>
  //   </delim>
  // </classifier-id>
  //

  public DelimClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);

    this.delimTest = null;
    final DomNode delimNode = classifierIdElement.selectSingleNode("delim");

    if (delimNode != null) {
      this.delimTest = new DelimTest(delimNode, resourceManager);
    }
  }

  public boolean isEmpty() {
    return delimTest == null;
  }

  public boolean doClassify(Token token, AtnState atnState) {
    boolean result = false;
 
    if (delimTest != null) {
      result = delimTest.accept(token, atnState).accept();
    }

    return result;
  }

  protected Map<String, String> doClassify(String text) {
    return EMPTY_MAP;
  }
}
