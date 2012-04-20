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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;

/**
 * Simple text generator that always maps a step's category to the same text.
 * <p>
 * @author Spence Koehler
 */
public class StaticTextGenerator implements TextGenerator {
  
  protected Map<String, String> cat2text;

  public StaticTextGenerator() {
    this.cat2text = new HashMap<String, String>();
  }

  /**
   * Construct with mappings from the file of the form:
   * <p>
   * stepCategory \t text
   * <p>
   * Ignore blank lines and those beginning with a '#'.
   */
  public StaticTextGenerator(File file) throws IOException {
    this();
    load(file);
  }

  public final void load(File file) throws IOException {

    if (file != null) {
      final BufferedReader reader = FileUtil.getReader(file);
      String line = null;
      while ((line = reader.readLine()) != null) {
        if ("".equals(line) || line.charAt(0) == '#') continue;
        final String[] pieces = line.split("\t");
        if (pieces.length == 2 && !"".equals(pieces[0])) {
          cat2text.put(pieces[0], pieces[1]);
        }
      }
      reader.close();
    }
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
    return cat2text.get(stepCategory);
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
