package org.sd.atn;


import java.util.List;
import org.sd.util.tree.Tree;
import org.sd.xml.DomNode;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class IdentityTextGenerator implements TextGenerator {
  
  public IdentityTextGenerator(DomNode domNode, ResourceManager resourceManager) {
  }

  /**
   * Hook called as the new top rule is started.
   */
  public void startRule(AtnGrammarAnalyzer grammarAnalyzer, AtnRule rule) {
    // no-op
  }

  /**
   * Generate (possibly null) text for the step.
   */
  public String getText(AtnGrammarAnalyzer grammarAnalyzer, AtnRuleStep step) {
    final String stepCategory = step.getCategory();
    return stepCategory;
  }

  /**
   * Hook called as the top rule is ended.
   */
  public void endRule(AtnGrammarAnalyzer grammarAnalyzer, AtnRule rule, List<Tree<String>> trees) {
    // no-op
  }

  /**
   * Convert the tree to a string of text.
   * <p>
   * This gives implementations an opportunity to do more than tree.getLeafText()
   * in order to 'clean up' the generated text once built.
   */
  public String getText(AtnGrammarAnalyzer grammarAnalyzer, Tree<String> tree) {
    return tree.getLeafText();
  }
}
