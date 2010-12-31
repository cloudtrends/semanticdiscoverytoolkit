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


import java.util.List;
import org.sd.util.tree.Tree;

/**
 * Interface to generate text for reverse parsing, analysis, and testing.
 * <p>
 * @author Spence Koehler
 */
public interface TextGenerator {
  
  /**
   * Hook called as the new top rule is started.
   */
  public void startRule(AtnGrammarAnalyzer grammarAnalyzer, AtnRule rule);

  /**
   * Generate (possibly null) text for the step.
   */
  public String getText(AtnGrammarAnalyzer grammarAnalyzer, AtnRuleStep step);

  /**
   * Hook called as the top rule is ended.
   */
  public void endRule(AtnGrammarAnalyzer grammarAnalyzer, AtnRule rule, List<Tree<String>> trees);

  /**
   * Convert the tree to a string of text.
   * <p>
   * This gives implementations an opportunity to do more than tree.getLeafText()
   * in order to 'clean up' the generated text once built.
   */
  public String getText(AtnGrammarAnalyzer grammarAnalyzer, Tree<String> tree);

}
