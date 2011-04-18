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


import java.util.ArrayList;
import java.util.List;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * A base rule step test that uses RoteList- and Regex- Classifiers.
 * classifiers to determine pass/fail.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseClassifierTest implements AtnRuleStepTest {
  
  protected String id;
  protected RoteListClassifier roteListClassifier;
  protected RegexClassifier regexClassifier;
  protected boolean verbose;

  private static int nextAutoId = 0;

  public BaseClassifierTest(DomNode testNode, ResourceManager resourceManager) {
    this.id = testNode.getAttributeValue("id", Integer.toString(nextAutoId++));

    this.roteListClassifier = new RoteListClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer());
    this.regexClassifier = new RegexClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer());

    if (roteListClassifier.isEmpty()) roteListClassifier = null;
    if (regexClassifier.isEmpty()) regexClassifier = null;

    this.verbose = testNode.getAttributeBoolean("verbose", false);

    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    //
    // <test reverse='true|false'>
    //   <jclass>org.sd.atn.*Test</jclass>
    //   <terms caseSensitive='true|false'>
    //     <term>...</term>
    //     ...
    //   </terms>
    //   <regexes>
    //     <regex type='...' groupN='...'>...</regex>
    //   </regexes>
    // </test>

  }
			
  // extenders of this abstract class  must implement 'accept':
  // public boolean accept(Token token, AtnState curState) {

}
