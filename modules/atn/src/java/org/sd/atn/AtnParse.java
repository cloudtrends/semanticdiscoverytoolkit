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
import java.util.Iterator;
import java.util.List;
import org.sd.atn.extract.Extraction;
import org.sd.atn.extract.ExtractionFactory;
import org.sd.token.CategorizedToken;
import org.sd.token.Token;
import org.sd.util.InputContext;
import org.sd.util.tree.Tree;

/**
 * Container for a single parse as returned through a ParseResult instance.
 * <p>
 * @author Spence Koehler
 */
public class AtnParse {
  
  private int parseNum;
  /**
   * This parse's parse number at the time of creation.
   */
  public int getParseNum() {
    return parseNum;
  }

  private AtnParseResult parseResult;
  /**
   * Backpointer to this parse's parse result.
   */
  public AtnParseResult getParseResult() {
    return parseResult;
  }

  private List<AtnParse> encompassingParses;
  public List<AtnParse> getEncompassingParses() {
    return encompassingParses;
  }
  public boolean hasEncompassingParses() {
    return encompassingParses != null && encompassingParses.size() > 0;
  }
  public void addEncompassingParse(AtnParse encompassingParse) {
    if (encompassingParses == null) encompassingParses = new ArrayList<AtnParse>();
    encompassingParses.add(encompassingParse);
  }

  private List<CategorizedToken> _tokens;
  /**
   * This parse's categorized tokens.
   */
  public List<CategorizedToken> getTokens() {
    if (_tokens == null) {
      _tokens = AtnState.computeTokens(endState);
    }
    return _tokens;
  }


  /**
   * This parse's first categorized token.
   */
  public CategorizedToken getFirstCategorizedToken() {
    CategorizedToken result = null;

    final List<CategorizedToken> tokens = getTokens();

    if (tokens != null && tokens.size() > 0) {
      result = tokens.get(0);
    }

    return result;
  }

  /**
   * This parse's last categorized token.
   */
  public CategorizedToken getLastCategorizedToken() {
    CategorizedToken result = null;

    final List<CategorizedToken> tokens = getTokens();

    if (tokens != null && tokens.size() > 0) {
      result = tokens.get(tokens.size() - 1);
    }

    return result;
  }

  /**
   * Get this parse's start index.
   */
  public int getStartIndex() {
    int result = -1;

    final CategorizedToken firstCToken = getFirstCategorizedToken();
    if (firstCToken != null) {
      result = firstCToken.token.getStartIndex();
    }

    return result;
  }

  /**
   * Get this parse's end index.
   */
  public int getEndIndex() {
    int result = -1;

    if (endState != null) {
      result = endState.getData().getInputToken().getEndIndex();
    }

    return result;
  }

  /**
   * Get the next token after this parse.
   */
  public Token getNextToken() {
    Token result = null;

    final CategorizedToken lastCToken = getLastCategorizedToken();
    if (lastCToken != null) {
      result = lastCToken.token.getNextToken();
    }

    return result;
  }


  private String _category;
  /**
   * This parse's category.
   */
  public String getCategory() {
    if (_category == null) {
      Tree<String> parseTree = getParseTree();
      _category = parseTree.getData();
    }
    return _category;
  }


  private boolean selected;
  /**
   * Flag on this parse indicating whether it has been selected or not.
   */
  public boolean getSelected() {
    return selected;
  }
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  private List<String> notes;
  public boolean hasNotes() {
    return notes != null && notes.size() > 0;
  }
  public List<String> getNotes() {
    return notes;
  }
  public void addNote(String note) {
    if (note != null && !"".equals(note)) {
      if (notes == null) notes = new ArrayList<String>();
      notes.add(note);
    }
  }
  public String getNotesString(String delim) {
    if (notes == null) return "";

    final StringBuilder result = new StringBuilder();

    for (String note : notes) {
      if (result.length() > 0) result.append(delim);
      result.append(note);
    }

    return result.toString();
  }


  private String _parsedText;
  /**
   * The original text used to generate this parse.
   */
  public String getParsedText() {
    if (_parsedText == null && endState != null) {
      String fullText = endState.getData().getInputToken().getTokenizer().getText();
      _parsedText = fullText.substring(getStartIndex(), getEndIndex());
    }
    return _parsedText;
  }

  /**
   * The full original text submitted for parsing.
   */
  public String getFullText() {
    String result = null;
    if (endState != null) {
      result = endState.getData().getInputToken().getTokenizer().getText();
    }
    return result;
  }

  /**
   * The length of the full original text submitted for parsing.
   */
  public int getFullTextLength() {
    final String fullText = getFullText();
    return fullText == null ? 0 : fullText.length();
  }

  /**
   * Get the input context associated with this parse's input or null.
   */
  public InputContext getInputContext() {
    InputContext result = null;

    final CategorizedToken firstCToken = getFirstCategorizedToken();
    if (firstCToken != null) {
      result = firstCToken.token.getTokenizer().getInputContext();
    }

    return result;
  }


  private ExtractionFactory _extractionFactory;
  public ExtractionFactory getExtractionFactory() {
    if (_extractionFactory == null && getInputContext() != null) {
      _extractionFactory = ExtractionFactory.getFactory(getInputContext());
    }
    return _extractionFactory;
  }


  private Object parseInterpretationsLock = new Object();
  private List<ParseInterpretation> _parseInterpretations;
  /**
   * Get (possibly empty, but not null) parse interpretations for this parse.
   */
  public List<ParseInterpretation> getParseInterpretations() {
    synchronized (parseInterpretationsLock) {
      if (_parseInterpretations == null && parseResult != null) {
        final AtnParseInterpreter interpreter = parseResult.getOptions().getParseInterpreter();
        if (interpreter != null) {
          _parseInterpretations = interpreter.getInterpretations(this);
        }
        
        if (_parseInterpretations == null) _parseInterpretations = new ArrayList<ParseInterpretation>();
      }
      return _parseInterpretations;
    }
  }


  Tree<AtnState> endState;
  private Tree<String> _parseTree;
  private Extraction _extraction;
  private Double _maxConfidence;


  AtnParse(int parseNum, Tree<AtnState> endState, AtnParseResult parseResult) {
    this.parseNum = parseNum;
    this.endState = endState;
    this.parseResult = parseResult;
    this._parseTree = null;
    this._extraction = null;
    this.selected = true;
    this._parseInterpretations = null;
    this._maxConfidence = null;
  }

  public Tree<String> getParseTree() {
    if (_parseTree == null && endState != null) {
      _parseTree = AtnStateUtil.convertToTree(endState);
    }
    return _parseTree;
  }

  public Extraction getExtraction() {
    if (_extraction == null) {
      _extraction = generateExtraction(getParseTree());
    }
    return _extraction;
  }

  public String getRemainingText() {
    String result = null;

    if (endState != null) {
      final Token inputToken = endState.getData().getInputToken();
      result = inputToken.getTokenizer().getNextText(inputToken);
    }

    return result;
  }

  /**
   * Get all of the categorized tokens under the givennode from this parse's
   * parseTree.
   */
  public List<CategorizedToken> getCategorizedTokens(Tree<String> parseTreeNode) {
    final List<CategorizedToken> result = new ArrayList<CategorizedToken>();

    final List<Tree<String>> leaves = parseTreeNode.gatherLeaves();
    for (Tree<String> leaf : leaves) {
      final CategorizedToken cToken = AtnStateUtil.getCategorizedToken(leaf);
      if (cToken != null) {
        result.add(cToken);
      }
    }

    return result;
  }

  /**
   * Return (highest) constituents from this tree of the given category or null.
   */
  public List<Tree<String>> getConstituents(String category) {
    return getParseTree().findNodes(category, Tree.Traversal.DEPTH_FIRST);
  }

  public Double getMaxConfidence() {
    if (_maxConfidence == null) {
      Double result = null;
      final List<ParseInterpretation> interps = getParseInterpretations();
      for (ParseInterpretation interp : interps) {
        final double curConf = interp.getConfidence();
        if (result == null || curConf > result) {
          result = curConf;
        }
      }
      _maxConfidence = result;
    }

    return _maxConfidence;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final Tree<String> parseTree = getParseTree();
    if (parseTree == null) {
      result.append("<NULL>");
    }
    else {
      result.append(parseTree.toString());
    }

    return result.toString();
  }

  private Extraction generateExtraction(Tree<String> parseTree) {
    Extraction result = null;

    if (parseTree.hasChildren() && getInputContext() != null) {
      boolean hasLeafChild = false;
      final List<Extraction> childExtractions = new ArrayList<Extraction>();

      for (Tree<String> childTree : parseTree.getChildren()) {
        Extraction childExtraction = null;

        if (!childTree.hasChildren()) {
          // parseTree's child is terminal, so parseTree is the category of the text in childTree
          childExtraction = buildLeafExtraction(childTree);
          hasLeafChild = true;
        }
        else {
          // recurse
          childExtraction = generateExtraction(childTree);
        }

        if (childExtraction != null) childExtractions.add(childExtraction);
      }

      if (childExtractions.size() > 0) {
        final Extraction firstChildExtraction = childExtractions.get(0);
        if (hasLeafChild && parseTree.getChildren().size() == 1) {
          result = firstChildExtraction;
        }
        else {
          final int lastEltIndex = childExtractions.size() - 1;
          final Extraction lastChildExtraction = childExtractions.get(lastEltIndex);

          result = buildIntermediateExtraction(parseTree, firstChildExtraction, lastChildExtraction);

          for (Extraction childExtraction : childExtractions) {
            result.addField(childExtraction);
          }
        }
      }
    }

    return result;
  }

  private Extraction buildIntermediateExtraction(Tree<String> parseTree, Extraction firstChildExtraction, Extraction lastChildExtraction) {
    final InputContext inputContext = getInputContext();

    if (inputContext == null) return null;

    Extraction result = null;

    final ExtractionFactory extractionFactory = getExtractionFactory();
    if (extractionFactory != null) {
      result = extractionFactory.buildParentExtraction(parseTree.getData(), firstChildExtraction, lastChildExtraction);
    }

    return result;
  }

  private Extraction buildLeafExtraction(Tree<String> leafNode) {
    final InputContext inputContext = getInputContext();

    final CategorizedToken leafCategorizedToken = AtnStateUtil.getCategorizedToken(leafNode);

    if (leafCategorizedToken == null || inputContext == null || leafNode.getParent() == null) {
      return null;
    }

    final ExtractionFactory extractionFactory = getExtractionFactory();
    if (extractionFactory == null) return null;

    final Token leafToken = leafCategorizedToken.token;
    final String leafCategory = leafCategorizedToken.category;

    final Extraction leafExtraction =
      extractionFactory.buildLeafExtraction(leafCategory, inputContext, leafToken.getStartIndex(), leafToken.getEndIndex());

    if (leafExtraction != null && leafCategory != null) {
      final AtnParse innerParse = (AtnParse)leafToken.getFeatureValue(null, null, AtnParse.class);

      if (innerParse != null) {
        final Extraction leafFieldExtraction = innerParse.generateExtraction(innerParse.getParseTree());
        leafExtraction.addField(leafFieldExtraction);
      }
    }

    return leafExtraction;
  }
}
