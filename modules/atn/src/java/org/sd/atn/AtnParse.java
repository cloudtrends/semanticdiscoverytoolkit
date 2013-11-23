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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sd.atn.extract.Extraction;
import org.sd.atn.extract.ExtractionFactory;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
import org.sd.token.Features;
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

      /*
    // adjust backward over immediate delims
      final String preDelim = firstCToken.getPreDelim();
      for (int idx = preDelim.length() - 1; idx >= 0; --idx) {
        final char delim = preDelim.charAt(idx);
        if (delim == ' ') break;
        --result;
      }
      */
    }

    return result;
  }

  /**
   * Get this parse's end index.
   */
  public int getEndIndex() {
    int result = -1;

    if (endState != null) {
      final Token endToken = endState.getData().getInputToken();
      result = endToken.getEndIndex();

/*
      // adjust forward over immediate delims
      final String postDelim = endToken.getPostDelim();
      final int postLen = postDelim.length();
      for (int idx = 0; idx < postLen; ++idx) {
        final char delim = postDelim.charAt(idx);
        if (delim == ' ') break;
        ++result;
      }
*/
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


  private List<ParseInterpretation> _parseInterpretations;
  /**
   * Get (possibly empty, but not null) parse interpretations for this parse.
   */
  public List<ParseInterpretation> getParseInterpretations() {
    if (_parseInterpretations == null && parseResult != null) {
      final ParseInterpreter interpreter = parseResult.getOptions().getParseInterpreter();
      if (interpreter != null) {
        _parseInterpretations = interpreter.getInterpretations(this.getParse(), parseResult.getOverrides());
      }
        
      if (_parseInterpretations == null) _parseInterpretations = new ArrayList<ParseInterpretation>();
      else {
        for (ParseInterpretation interp : _parseInterpretations) {
          if (!interp.hasSourceParse()) {
            interp.setSourceParse(this);
          }
        }
      }
    }
    return _parseInterpretations;
  }
  /**
   * Determine whether this parse has been interpreted.
   */
  public boolean wasInterpreted() {
    return _parseInterpretations != null;
  }


  Tree<AtnState> endState;
  private Tree<String> _parseTree;
  private Parse _parse;
  private Extraction _extraction;
  private Double _maxConfidence;


  AtnParse(int parseNum, Tree<AtnState> endState, AtnParseResult parseResult) {
    this.parseNum = parseNum;
    this.endState = endState;
    this.parseResult = parseResult;
    this._parseTree = null;
    this._parse = null;
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

  /**
   * Get a parse instance for this parse.
   * <p>
   * A parse instance is a serializable form of the parse information that is
   * disconnected from the AtnState objects used to generate the parse.
   */
  public Parse getParse() {
    if (_parse == null) {
      _parse = new Parse(this);
    }
    return _parse;
  }

  public AtnRule getStartRule() {
    // Get the state just under the root (which holds null) leading to the endState
    Tree<AtnState> firstStateNode = endState;
    for (Tree<AtnState> stateNode = endState.getParent(); stateNode.getParent() != null; stateNode = stateNode.getParent()) {
      if (stateNode.getParent().getData() == null) {
        firstStateNode = stateNode;
        break;
      }
    }
    return firstStateNode.getData().getRule();
  }

  public Tree<AtnState> getEndState() {
    return endState;
  }

  public Extraction getExtraction() {
    if (_extraction == null) {
      _extraction = generateExtraction(getParseTree());
    }
    return _extraction;
  }

  public String getImmediatePostParseDelims() {
    String result = null;

    if (endState != null) {
      final Token inputToken = endState.getData().getInputToken();
      result = inputToken.getTokenizer().getPostDelim(inputToken);
    }

    return result;
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
      if (interps != null) {
        for (ParseInterpretation interp : interps) {
          final double curConf = interp.getConfidence();
          if (result == null || curConf > result) {
            result = curConf;
          }
        }
      }

      _maxConfidence = result == null ? 0.0 : result;
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

    final Map<Token, TokenOffset> offsets = new HashMap<Token, TokenOffset>();
    Extraction result = doGenerateExtraction(parseTree, offsets);

    // add parse interpretations to the extraction
    if (result != null) {
      final List<ParseInterpretation> interps = getParseInterpretations();
      if (interps != null) {
        for (ParseInterpretation interp : interps) {
          final Extraction interpExtraction = buildInterpExtraction(interp);
          if (interpExtraction != null) {
            result.addField(interpExtraction);
          }
        }
      }
    }

    return result;
  }
    

  private Extraction doGenerateExtraction(Tree<String> parseTree, Map<Token, TokenOffset> offsets) {
    Extraction result = null;

    if (parseTree.hasChildren() && getInputContext() != null) {
      boolean hasLeafChild = false;
      final List<Extraction> childExtractions = new ArrayList<Extraction>();

      for (Tree<String> childTree : parseTree.getChildren()) {
        Extraction childExtraction = null;

        if (!childTree.hasChildren()) {
          // parseTree's child is terminal, so parseTree is the category of the text in childTree
          childExtraction = buildLeafExtraction(childTree, offsets);
          hasLeafChild = true;
        }
        else {
          // recurse
          childExtraction = doGenerateExtraction(childTree, offsets);
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

    // don't really want all token attributes in the following section --
    // these represent all possible categorizations for the tokens and we've
    // constrained the possibilities to that which makes sense in the context
    // of the parse.
/*
    // add token attributes if present as extraction fields
    if (result != null && parseTree.hasAttributes()) {
      final Map<String, Object> parseAttributes = parseTree.getAttributes();
      final CategorizedToken cToken = (CategorizedToken) parseAttributes.get(AtnStateUtil.TOKEN_KEY);
      if (cToken.token.hasFeatures()) {
        final Features features = cToken.token.getFeatures();
        final List<Feature> allFeatures = features.getFeatures();
        for (Feature feature : allFeatures) {
          final Object featureValue = feature.getValue();
          if (featureValue != null) {
            final Extraction featureExtraction = new Extraction(feature.getType(), featureValue.toString());
            result.addField(featureExtraction);
          }
        }
      }
    }
*/

    return result;
  }

  //
  // Recomputing token offsets in relation to the entire (parse) tree requires
  // updating tokens from prior parsing passes potentially with a subset of
  // the current input because tokens stored on the parse tree nodes are those
  // from their original context and the context changes as parses are used
  // for later passes.
  //
  // Recomputing all offsets in relation to the current tree allows for the
  // parsed tokens to be correlated with the current parse's full input
  // (through extraction instances.)
  // 

  private int getStartOffset(Tree<String> parseNode, Map<Token, TokenOffset> offsets, TokenOffset childOffset) {
    TokenOffset myOffset = null;

    // NOTE: token keyed maps depend on token instance equality, not content equality
    //       if Token defines "equals" and "hashCode" based on content, then this will
    //       break!

    if (parseNode != null) {

      final boolean isTerminal = !parseNode.hasChildren();

      if (isTerminal) parseNode = parseNode.getParent(); //token stored in penultimate (from leaf)
      final CategorizedToken cToken = AtnStateUtil.getCategorizedToken(parseNode);
      if (cToken != null && cToken.token.getSequenceNumber() >= 0) {  // ignore hardwired cached tokens

        // retrieve or create my (current parseNode's token's) tokenOffset
        myOffset = offsets.get(cToken.token);
        if (myOffset == null) {
          myOffset = new TokenOffset(cToken.token);
          offsets.put(cToken.token, myOffset);
        }
        
        // update my last child offset
        myOffset.updateLastChild(childOffset);
      }

      // walk up the tree
      if (parseNode.getParent() != null) {
        getStartOffset(parseNode.getParent(), offsets, myOffset != null ? myOffset : childOffset);
      }
    }

    return myOffset == null ? 0 : myOffset.getStart();
  }

  private static final class TokenOffset {
    public final Token token;
    private TokenOffset priorToken;
    private TokenOffset lastChild;
    private int newChildStart;

    TokenOffset(Token token) {
      this.token = token;
      this.priorToken = null;
      this.lastChild = null;
      this.newChildStart = token.getStartIndex();
    }

    public int getStart() {
      return token.getStartIndex() + getFirstChildStart();
    }

    public int getFirstChildStart() {
      return priorToken == null ? (newChildStart - token.getStartIndex()) : priorToken.getFirstChildStart();
    }

    public void updateLastChild(TokenOffset curLastChild) {
      if (curLastChild != null) {
        if (this.lastChild == null) {
          // curLastChild will be this instance's first child
          this.lastChild = curLastChild;

          // update initial "starts" for all descendants
          // note that we're assuming a depth-first tree traversal
          setInitialStart(curLastChild, token.getStartIndex());
        }
        else {
          curLastChild.updatePriorPositionWith(this.lastChild);
          this.lastChild = curLastChild;
        }
      }
    }

    /** In this instance's linear sequence, get the first. */
    private final TokenOffset getFirstToken() {
      TokenOffset result = this;
      while (result.priorToken != null) result = result.priorToken;
      return result;
    }

    /** Get this instance's first child token. */
    private final TokenOffset getFirstChild() {
      TokenOffset result = null;

      if (lastChild != null) {
        result = lastChild.getFirstToken();
      }

      return result;
    }

    private final void updatePriorPositionWith(TokenOffset newPrior) {
      // this instance's prior token will be the new prior

      // descend so that child priors are appropriately linked
      if (newPrior != null && this.lastChild != null && newPrior.lastChild != null) {
        // my first child's prior will be the new prior's last child
        final TokenOffset myFirstChild = getFirstChild();
        myFirstChild.updatePriorPositionWith(newPrior.lastChild);
      }        

      // set
      this.priorToken = newPrior;
    }

    private final void setInitialStart(TokenOffset childOffset, int newStart) {
      if (childOffset != null) {
        // back up to the first token in the child's sequence
        childOffset = childOffset.getFirstToken();

        // set the new start
        childOffset.newChildStart = newStart;

        // descend
        setInitialStart(childOffset.lastChild, newStart);
      }
    }
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

  private Extraction buildLeafExtraction(Tree<String> leafNode, Map<Token, TokenOffset> offsets) {
    final InputContext inputContext = getInputContext();

    final CategorizedToken leafCategorizedToken = AtnStateUtil.getCategorizedToken(leafNode);

    if (leafCategorizedToken == null || inputContext == null || leafNode.getParent() == null) {
      return null;
    }

    final ExtractionFactory extractionFactory = getExtractionFactory();
    if (extractionFactory == null) return null;

    final Token leafToken = leafCategorizedToken.token;
    final String leafCategory = leafCategorizedToken.category;
    final int tokenLen = leafToken.getEndIndex() - leafToken.getStartIndex();

    final int startOffset = getStartOffset(leafNode, offsets, null);

    final Extraction leafExtraction =
      extractionFactory.buildLeafExtraction(leafCategory, inputContext, startOffset, startOffset + tokenLen);

    if (leafExtraction != null && leafCategory != null) {
      final AtnParse innerParse = (AtnParse)leafToken.getFeatureValue(null, null, AtnParse.class);

      if (innerParse != null) {
        final Extraction leafFieldExtraction = innerParse.generateExtraction(innerParse.getParseTree());
        leafExtraction.addField(leafFieldExtraction);
      }
    }

    return leafExtraction;
  }

  private Extraction buildInterpExtraction(ParseInterpretation interp) {
    if (interp == null || interp.getInterpretation() == null) return null;

    final Extraction result = new Extraction("_interp_", interp.toString());

    final Extraction interpClass = new Extraction("class", interp.getInterpretation().getClass().getName());
    result.addField(interpClass);

    final Map<String, Serializable> attrs = interp.getCategory2Value();
    if (attrs != null && attrs.size() > 0) {
      final Extraction attrsExtraction = new Extraction("_attrs_", "");
      for (Map.Entry<String, Serializable> attr : attrs.entrySet()) {
        final String key = attr.getKey();
        final Serializable value = attr.getValue();
        if (value != null) {
          final Extraction attrExtr = new Extraction(key, value.toString());
          attrsExtraction.addField(attrExtr);
        }
      }
      result.addField(attrsExtraction);
    }

    return result;
  }
}
