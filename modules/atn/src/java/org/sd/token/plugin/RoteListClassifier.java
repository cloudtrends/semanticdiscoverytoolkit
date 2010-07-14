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
package org.sd.token.plugin;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sd.io.FileUtil;
import org.sd.token.AbstractTokenClassifier;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class RoteListClassifier extends AbstractTokenClassifier {
  
  private String roteListType;
  public String getRoteListType() {
    return roteListType;
  }

  private Map<String, Map<String, String>> term2attributes;
  private boolean caseSensitive;

  public RoteListClassifier(DomElement classifierIdElement, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement);
  }

  private final void init(DomElement classifierIdElement) {
    this.roteListType = classifierIdElement.getLocalName();
    this.term2attributes = new HashMap<String, Map<String, String>>();
    this.caseSensitive = false;

    final DomElement termsNode = (DomElement)classifierIdElement.selectSingleNode("terms");

    if (termsNode != null) {
      this.caseSensitive = termsNode.getAttributeBoolean("caseSensitive", false);

      Map<String, String> termAttributes = null;
      final NodeList termNodes = termsNode.selectNodes("term");
      for (int i = 0; i < termNodes.getLength(); ++i) {
        final Node curNode = termNodes.item(i);
        if (curNode.getNodeType() != Node.ELEMENT_NODE) continue;
        final DomElement termElement = (DomElement)curNode;

        String termText = termElement.getTextContent();
        if (!caseSensitive) termText = termText.toLowerCase();

        if (termElement.hasAttributes()) {
          termAttributes = termElement.getDomAttributes().getAttributes();
        }
        else termAttributes = null;

        term2attributes.put(termText, termAttributes);
      }
    }

    final DomElement textfileNode = (DomElement)classifierIdElement.selectSingleNode("textfile");

    if (textfileNode != null) {
      this.caseSensitive = textfileNode.getAttributeBoolean("caseSensitive", false);

      String textfile = textfileNode.getTextContent();
      if (textfileNode.getDataProperties() != null) {
        textfile = textfileNode.getDataProperties().replaceVariables(textfile);
      }
      try {
        final BufferedReader reader = FileUtil.getReader(textfile);

        String line = null;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (!caseSensitive) line = line.toLowerCase();
          term2attributes.put(line, null);
        }

        reader.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }


  public boolean doClassify(Token token) {
    boolean result = false;

    String key = caseSensitive ? token.getText() : token.getText().toLowerCase();

    if (term2attributes.containsKey(key)) {
      result = true;

      final Map<String, String> attributes = term2attributes.get(key);

      if (attributes != null) {
        for (Map.Entry<String, String> kvPair : attributes.entrySet()) {
          token.setFeature(kvPair.getKey(), kvPair.getValue(), this);
        }
      }
    }

    return result;
  }
}
