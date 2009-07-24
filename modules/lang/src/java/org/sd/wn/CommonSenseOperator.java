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
package org.sd.wn;


import java.util.ArrayList;
import java.util.List;

/**
 * A word sense operator to find a common (or shared) sense along the relation
 * chain of one sense while expanding another.
 * <p>
 * @author Spence Koehler
 */
public class CommonSenseOperator implements WordSenseOperator {
  
  private List<WordSense> senses;
  private WordSenseWrapper commonSense;

  public CommonSenseOperator(WordSenseWrapper sense, PointerSymbol relation) {
    this.senses = getRelationChain(sense, relation);
    this.commonSense = null;
  }

  public WordSenseWrapper getCommonSense() {
    return commonSense;
  }

  private final List<WordSense> getRelationChain(WordSenseWrapper sense, PointerSymbol relation) {
    final SenseCollector operator = new SenseCollector();
    sense.expand(operator, SingleRelationFilter.getInstance(relation));
    return operator.getSenses();
  }

  /**
   * Operate on the given child (expanded) word sense wrapper.
   * <p>
   * The UnwindOperatorException is intended as a mechanism to halt recursive
   * expansion. For example, if the target of a search is found, the exception
   * can be thrown and the handleUnwind method will be called.
   */
  public void operate(WordSenseWrapper childWrapper, WordSenseWrapper parentWrapper) throws UnwindOperatorException {
    if (senses.contains(childWrapper.getWordSense())) {
      this.commonSense = childWrapper;

      throw new UnwindOperatorException(childWrapper, parentWrapper);
    }
  }

  /**
   * Handle the unwind exception thrown by operate.
   */
  public void handleUnwind(UnwindOperatorException e) {
    //nothing to do.
  }


  private static final class SenseCollector implements WordSenseOperator {

    private List<WordSense> senses;

    public SenseCollector() {
      this.senses = new ArrayList<WordSense>();
    }

    public List<WordSense> getSenses() {
      return senses;
    }

    /**
     * Operate on the given child (expanded) word sense wrapper.
     * <p>
     * The UnwindOperatorException is intended as a mechanism to halt recursive
     * expansion. For example, if the target of a search is found, the exception
     * can be thrown and the handleUnwind method will be called.
     */
    public void operate(WordSenseWrapper childWrapper, WordSenseWrapper parentWrapper) throws UnwindOperatorException {
      senses.add(childWrapper.getWordSense());
    }

    /**
     * Handle the unwind exception thrown by operate.
     */
    public void handleUnwind(UnwindOperatorException e) {
      //nothing to do.
    }
  }
}
