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
package org.sd.atn;


import java.util.ArrayList;
import java.util.List;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A container for rule step test wrappers.
 * <p>
 * @author Spence Koehler
 */
public class StateTestContainer {
  
  public enum Directive { CONTINUE, HALT, SUCCEED, FAIL };

  private List<StateTestWrapper> scanTestWrappers;
  private List<StateTestWrapper> finalTestWrappers;
  private boolean empty;

  public StateTestContainer() {
    this.scanTestWrappers = null;
    this.finalTestWrappers = null;
    this.empty = true;
  }

  public StateTestContainer(DomElement stepElement, ResourceManager resourceManager) {
    this.scanTestWrappers = null;
    this.finalTestWrappers = null;

    final NodeList childNodes = stepElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childIdx = 0; childIdx < numChildNodes; ++childIdx) {
      final Node childNode = childNodes.item(childIdx);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;
      final DomElement childElt = (DomElement)childNode;

      final StateTestWrapper testWrapper = new StateTestWrapper(childElt, resourceManager);
      if (!testWrapper.isEmpty()) {
        List<StateTestWrapper> testWrappers = null;
        if (testWrapper.isScanType()) {
          if (this.scanTestWrappers == null) this.scanTestWrappers = new ArrayList<StateTestWrapper>();
          testWrappers = scanTestWrappers;
        }
        else {
          if (this.finalTestWrappers == null) this.finalTestWrappers = new ArrayList<StateTestWrapper>();
          testWrappers = finalTestWrappers;
        }
        testWrappers.add(testWrapper);
      }
    }

    this.empty = (scanTestWrappers == null && finalTestWrappers == null);
  }

  public final synchronized boolean addTestWrapper(StateTestWrapper testWrapper) {
    boolean result = false;

    if (testWrapper != null && !testWrapper.isEmpty()) {
      List<StateTestWrapper> testWrappers = null;
      if (testWrapper.isScanType()) {
        if (this.scanTestWrappers == null) this.scanTestWrappers = new ArrayList<StateTestWrapper>();
        testWrappers = scanTestWrappers;
      }
      else {
        if (this.finalTestWrappers == null) this.finalTestWrappers = new ArrayList<StateTestWrapper>();
        testWrappers = finalTestWrappers;
      }
      testWrappers.add(testWrapper);
      result = true;
      this.empty = false;
    }

    return result;
  }

  /**
   * Determine whether this container is empty.
   */
  public boolean isEmpty() {
    return empty;
  }

  /**
   * Verify the scan test constraints.
   */
  public Directive verifyScanTests(Token token, AtnState curState) {
    if (empty) return Directive.SUCCEED;
    return verifyTests(scanTestWrappers, token, curState);
  }

  /**
   * Verify the final test constraints.
   */
  public Directive verifyFinalTests(Token token, AtnState curState) {
    if (empty) return Directive.SUCCEED;
    return verifyTests(finalTestWrappers, token, curState);
  }

  /**
   * Verify the test constraints.
   */
  private final Directive verifyTests(List<StateTestWrapper> testWrappers, Token token, AtnState curState) {
    if (testWrappers == null) return Directive.SUCCEED;

    Directive result = Directive.CONTINUE;

    for (StateTestWrapper testWrapper : testWrappers) {
      result = testWrapper.verify(token, curState);
      if (result != Directive.CONTINUE) break;
    }
  
    return result;
  }

  public boolean hasScanTests() {
    return scanTestWrappers != null;
  }

  public List<StateTestWrapper> getScanTestWrappers() {
    return scanTestWrappers;
  }

  public boolean hasFinalTests() {
    return finalTestWrappers != null;
  }

  public List<StateTestWrapper> getFinalTestWrappers() {
    return finalTestWrappers;
  }
}
