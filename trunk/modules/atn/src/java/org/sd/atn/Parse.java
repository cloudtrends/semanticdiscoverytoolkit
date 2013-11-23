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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.sd.cio.MessageHelper;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
import org.sd.token.Features;
import org.sd.token.LiteralTokenizer;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.util.Usage;
import org.sd.io.Publishable;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;

/**
 * Container for the persistable distillation of a parse.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "Container for the persistable distillation of a parse.")
public class Parse implements Publishable, Serializable {
  
  // (Literal)Tokenizer containing just the portion of the string that was parsed
  private LiteralTokenizer tokenizer;

  // Parse tree including attributes and categorized tokens with token features
  private Tree<String> parseTree;

  // rule used to generate the parse
  private String ruleId;
  
  // text remaining from original input
  private String remainingText;

  // delims remaining from original input
  private String immediatePostParseDelims;

  private List<CategorizedToken> _cTokens;
  private DomElement _domElement;

  private transient AtnParse _atnParse = null;

  private static final long serialVersionUID = 42L;

  private static IdentityParseInterpreter _ipi = null;
  private static final IdentityParseInterpreter getIdentityParseInterpreter() {
    if (_ipi == null) {
      _ipi = new IdentityParseInterpreter(false);
    }
    return _ipi;
  }

  /**
   * Empty constructor for publishable reconstruction.
   */
  public Parse() {
  }

  /**
   * Hardwired constructor (for testing and analysis).
   */
  public Parse(String ruleId, Tree<String> parseTree) {
    this(ruleId, parseTree, null);
  }

  /**
   * Hardwired constructor (for testing and analysis).
   */
  public Parse(String ruleId, Tree<String> parseTree, String parsedText) {
    if (parsedText == null) parsedText = parseTree.getLeafText();    
    this.tokenizer = new LiteralTokenizer(parsedText);
    this.parseTree = parseTree;
    this.ruleId = ruleId;
    this.remainingText = null;
    this.immediatePostParseDelims = null;
    this._cTokens = null;
    this._domElement = null;
  }

  /**
   * Construct with the given atn parse.
   * <p>
   * Package protected to provide access to AtnParse to build while it
   * maintains its own parse instance to avoid unnecessary duplication
   * of effort.
   */
  Parse(AtnParse parse) {
    final int zeroIndex = parse.getStartIndex();

    final List<Token> tokens = new ArrayList<Token>();
    for (CategorizedToken cToken : parse.getTokens()) {
      tokens.add(cToken.token);
    }
    this.tokenizer = new LiteralTokenizer(parse.getParsedText(), tokens);

    int[] offset = new int[]{0};
    this.parseTree = buildParseTree(parse.getParseTree(), tokenizer, zeroIndex, offset);

    // NOTE: ruleIDs are placed on parse tree nodes while built in AtnStateUtil
    this.ruleId = parse.getStartRule().getRuleId();

    this.remainingText = parse.getRemainingText();
    this.immediatePostParseDelims = parse.getImmediatePostParseDelims();

    this._atnParse = parse;
  }

  public LiteralTokenizer getTokenizer() {
    return tokenizer;
  }

  public Tree<String> getParseTree() {
    return parseTree;
  }

  public String getRuleId() {
    return ruleId;
  }

  public String getParsedText() {
    return tokenizer.getText();
  }

  /**
   * Get the text after the parse if available or null.
   */
  public String getRemainingText() {
    return remainingText;
  }

  /**
   * Set the remaining text for this parse.
   */
  public void setRemainingText(String remainingText) {
    this.remainingText = remainingText;
  }

  /**
   * Get the delims after the parse if available or null.
   */
  public String getImmediatePostParseDelims() {
    return immediatePostParseDelims;
  }

  /**
   * Set the immediate post parse delims for this parse.
   */
  public void setImmediatePostParseDelims(String immediatePostParseDelims) {
    this.immediatePostParseDelims = immediatePostParseDelims;
  }

  public List<CategorizedToken> getTokens() {
    if (_cTokens == null) {
      _cTokens = buildCategorizedTokens();
    }
    return _cTokens;
  }

  private final List<CategorizedToken> buildCategorizedTokens() {
    final List<CategorizedToken> result = new ArrayList<CategorizedToken>();

    for (Tree<String> leaf : parseTree.gatherLeaves()) {
      final CategorizedToken cToken = ParseInterpretationUtil.getCategorizedToken(leaf);
      result.add(cToken);
    }

    return result;
  }

  public CategorizedToken getFirstCategorizedToken() {
    CategorizedToken result = null;
    final List<CategorizedToken> cTokens = getTokens();
    if (cTokens != null && cTokens.size() > 0) {
      result = cTokens.get(0);
    }
    return result;
  }

  public CategorizedToken getLastCategorizedToken() {
    CategorizedToken result = null;
    final List<CategorizedToken> cTokens = getTokens();
    if (cTokens != null && cTokens.size() > 0) {
      result = cTokens.get(cTokens.size() - 1);
    }
    return result;
  }

  public boolean hasAtnParse() {
    return _atnParse != null;
  }

  public AtnParse getAtnParse() {
    return _atnParse;
  }

  /**
   * Get this parse's tree as xml.
   */
  public DomElement asXml() {
    if (_domElement == null) {
      final IdentityParseInterpreter ipi = getIdentityParseInterpreter();
      final List<ParseInterpretation> identityInterps = ipi.getInterpretations(this, null);
      if (identityInterps != null && identityInterps.size() > 0) {
        final ParseInterpretation identityInterp = identityInterps.get(0);
        _domElement = (DomElement)identityInterp.getInterpTree().getData().asDomNode();

        if (ruleId != null && !"".equals(ruleId) &&
            (!_domElement.hasAttributes() || _domElement.getAttributeValue("_ruleID", null) == null)) {
          _domElement.setAttribute("_ruleID", ruleId);
        }
      }
    }
    return _domElement;
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, ruleId);
    MessageHelper.writeString(dataOutput, remainingText);
    MessageHelper.writeString(dataOutput, immediatePostParseDelims);
    MessageHelper.writePublishable(dataOutput, tokenizer);
    writeTree(dataOutput, parseTree);
  }

  private final void writeTree(DataOutput dataOutput, Tree<String> parseTree) throws IOException {
    final boolean halt = writeNode(dataOutput, parseTree);

    if (halt || !parseTree.hasChildren()) {
      dataOutput.writeInt(0);
    }
    else {
      dataOutput.writeInt(parseTree.numChildren());
      for (Tree<String> child : parseTree.getChildren()) {
        writeTree(dataOutput, child);
      }
    }
  }

  /**
   * Write the node data, preserving all serializable attribute values.
   */
  private final boolean writeNode(DataOutput dataOutput, Tree<String> parseTreeNode) throws IOException {
    boolean halt = false;

    MessageHelper.writeString(dataOutput, parseTreeNode.getData());
    if (!parseTreeNode.hasAttributes()) {
      dataOutput.writeInt(0);
    }
    else {
      final Map<String, Object> attributes = parseTreeNode.getAttributes();
      dataOutput.writeInt(attributes.size());
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        final String key = entry.getKey();
        final Object value = entry.getValue();

        if (AtnStateUtil.TOKEN_KEY.equals(key)) {
          dataOutput.writeByte(2);

          final CategorizedToken cToken = (CategorizedToken)value;
          dataOutput.writeInt(cToken.token.getStartIndex());

          // preserve token features
          if (cToken.token.hasFeatures()) {
            final List<Feature> features = cToken.token.getFeatures().getFeatures();
            dataOutput.writeInt(features.size());
            for (Feature feature : features) {
              MessageHelper.writePublishable(dataOutput, feature);

              // ParseInterpretation Note:
              //
              // When we see a token key as a node's attribute and the token (value)
              // has a ParseInterpretation value for a feature, it means that the
              // node holds another parse covering the token (value). The full parse
              // content for this node is contained in the ParseInterpretation, so
              // we can stop serializing at this point.
              //

              if (feature.getValue() instanceof ParseInterpretation) {
                final ParseInterpretation interp = (ParseInterpretation)feature.getValue();
                final Parse interpParse = interp.getParse();
                if (interpParse != null) {
                  final Tree<String> interpParseTree = interpParse.getParseTree();
                  if (interpParseTree != null) {
                    halt = true;
                  }
                }
              }
            }
          }
          else {
            dataOutput.writeInt(0);
          }
        }
        else if (value instanceof Serializable) {
          dataOutput.writeByte(1);
          MessageHelper.writeString(dataOutput, key);
          MessageHelper.writeSerializable(dataOutput, (Serializable)value);
        }
        else {
          dataOutput.writeByte(0);
        }
      }
    }

    return halt;
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.ruleId = MessageHelper.readString(dataInput);
    this.remainingText = MessageHelper.readString(dataInput);
    this.immediatePostParseDelims = MessageHelper.readString(dataInput);
    this.tokenizer = (LiteralTokenizer)MessageHelper.readPublishable(dataInput);
    this.parseTree = readTree(dataInput, tokenizer);
  }

  private final Tree<String> readTree(DataInput dataInput, LiteralTokenizer tokenizer) throws IOException {
    final Tree<String> result = readNode(dataInput, tokenizer);

    final int numChildren = dataInput.readInt();
    for (int childNum = 0; childNum < numChildren; ++childNum) {
      final Tree<String> child = readTree(dataInput, tokenizer);
      result.addChild(child);
    }

    return result;
  }

  private final Tree<String> readNode(DataInput dataInput, LiteralTokenizer tokenizer) throws IOException {
    final String category = MessageHelper.readString(dataInput);
    final Tree<String> result = new Tree<String>(category);
    final int numAttributes = dataInput.readInt();
    if (numAttributes > 0) {
      final Map<String, Object> attributes = result.getAttributes();
      for (int attrNum = 0; attrNum < numAttributes; ++attrNum) {
        final byte attrFlag = dataInput.readByte();
        if (attrFlag != 0) {
          if (attrFlag == 2) {
            final int tokenStart = dataInput.readInt();
            final Token token = tokenizer.getToken(tokenStart);
            final CategorizedToken cToken = new CategorizedToken(token, category);
            attributes.put(AtnStateUtil.TOKEN_KEY, cToken);

            // restore token features
            final int numFeatures = dataInput.readInt();
            if (numFeatures > 0) {
              final Features features = new Features();
              for (int fNum = 0; fNum < numFeatures; ++fNum) {
                final Feature feature = (Feature)MessageHelper.readPublishable(dataInput);
                features.add(feature);

                // restore interpretation parse tree (see ParseInterpretation Note above)
                if (feature.getValue() instanceof ParseInterpretation) {
                  final ParseInterpretation interp = (ParseInterpretation)feature.getValue();
                  final Parse interpParse = interp.getParse();
                  if (interpParse != null) {
                    final Tree<String> interpParseTree = interpParse.getParseTree();
                    if (interpParseTree != null) {
                      result.addChild(interpParseTree);
                    }
                  }
                }
              }
              token.setFeatures(features);
            }
          }
          else if (attrFlag == 1) {
            final String key = MessageHelper.readString(dataInput);
            final Object value = MessageHelper.readSerializable(dataInput);
            attributes.put(key, value);
          }
        }
      }
    }
    return result;
  }


  private final Tree<String> buildParseTree(Tree<String> parseTree, LiteralTokenizer tokenizer, int zeroIndex, int[] offset) {
    int[] theOffset = offset;
    int offsetValue = 0;

    // NOTE: when a parseTree has multiple children due to ambiguities, the token offset needs to be repeated
    final boolean ambiguous = parseTree.hasAttributes() && "true".equals(parseTree.getAttributes().get("ambiguous"));
    if (ambiguous) {
      theOffset = new int[]{offset[0]};
    }

    final Tree<String> result = buildNode(parseTree, tokenizer, zeroIndex, theOffset);

    if (ambiguous) {
      offsetValue = theOffset[0];
    }

    if (parseTree.hasChildren()) {
      for (Tree<String> child : parseTree.getChildren()) {
        if (ambiguous) {
          theOffset = new int[]{offset[0]};
        }
        result.addChild(buildParseTree(child, tokenizer, zeroIndex, theOffset));
      }
    }

    if (ambiguous) {
      offset[0] = offsetValue;
    }

    return result;
  }

  private final Tree<String> buildNode(Tree<String> parseTreeNode, LiteralTokenizer tokenizer, int zeroIndex, int[] offset) {
    final String category = parseTreeNode.getData();
    final Tree<String> result = new Tree<String>(category);

    // copy over attributes, converting tokens to be from this instance's tokenizer
    if (parseTreeNode.hasAttributes()) {
      final Map<String, Object> newAttributes = result.getAttributes();

      for (Map.Entry<String, Object> attr : parseTreeNode.getAttributes().entrySet()) {
        final String key = attr.getKey();
        final Object value = attr.getValue();

        if (AtnStateUtil.TOKEN_KEY.equals(key)) {
          final CategorizedToken cToken = (CategorizedToken)value;
          if (cToken != null && cToken.token != null) {
            final int startPos = cToken.token.getStartIndex() - zeroIndex;
            final int endPos = cToken.token.getEndIndex() - zeroIndex;

            int adjustment = startPos < offset[0] ? (offset[0] + zeroIndex) : 0;
            Token lToken = tokenizer.buildToken(startPos + adjustment, endPos + adjustment);

            if (lToken == null) {
              System.err.println(new Date() +
                                 ": Note -- Parse.buildNode cToken.token=" + cToken.token +
                                 " zeroIndex=" + zeroIndex +
                                 " startPos=" + startPos +
                                 " endPos=" + endPos +
                                 " adjustment=" + adjustment +
                                 " cToken.input=" + cToken.token.getTokenizer().getText());

              //todo: this is a hack for a special case that fails. find/fix the underlying problem!
              adjustment = 0;
              lToken = tokenizer.buildToken(startPos, endPos);

              if (lToken == null) {
                System.err.println("\tParse.buildNode fallback failed!");
              }
            }

            if (lToken != null) {
              if (adjustment == 0) offset[0] = startPos;

              if (cToken.token.hasFeatures()) {
                if (!lToken.hasFeatures()) {
                  lToken.setFeatures(cToken.token.getFeatures());
                }
                else {
                  final Features lFeatures = lToken.getFeatures();
                  for (Feature feature : cToken.token.getFeatures().getFeatures()) {
                    lFeatures.add(feature);
                  }
                }
              }
              newAttributes.put(key, new CategorizedToken(lToken, category));
            }
          }
        }
        else {
          newAttributes.put(key, value);
        }
      }
    }

    return result;
  }


  private void writeObject(ObjectOutputStream out) throws IOException {
    write(out);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    read(in);
  }
}
