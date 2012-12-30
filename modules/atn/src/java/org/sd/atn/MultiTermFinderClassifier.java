/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.text.MultiTermFinder;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * A classifier based on a MultiTermFinder.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.AbstractAtnStateTokenClassifier implementation\n" +
       "that uses an org.sd.text.MultiTermFinder loaded from the\n" +
       "specified 'textfile' referenced on initialization."
  )
public class MultiTermFinderClassifier extends AbstractAtnStateTokenClassifier {
  
  private ResourceManager resourceManager;
  private MultiTermFinder mtf;

  public MultiTermFinderClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);

    getTokenClassifierHelper().setMaxWordCount(0);
    this.resourceManager = resourceManager;
    this.mtf = null;

    init(classifierIdElement);
  }

  private final void init(DomElement classifierIdElement) {

    final DomElement textfileNode = (DomElement)classifierIdElement.selectSingleNode("textfile");
    if (textfileNode != null) {
      final File textfile = resourceManager.getWorkingFile(textfileNode);

      try {
        this.mtf = MultiTermFinder.loadFromFile(textfile);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public boolean isEmpty() {
    return mtf == null || mtf.getExpressions() == null || mtf.getExpressions().length == 0;
  }

  public void supplement(DomNode supplementNode) {
    init(supplementNode.asDomElement());
  }

  public boolean doClassify(Token token, AtnState atnState) {
    return doClassification(token.getTextWithDelims());
  }

  protected Map<String, String> doClassify(String text) {
    return doClassification(text) ? EMPTY_MAP : null;
  }

  private final boolean doClassification(String text) {
    boolean result = false;

    if (mtf != null) {
      result = mtf.findFirstMatch(text) != null;
    }

    return result;
  }
}
