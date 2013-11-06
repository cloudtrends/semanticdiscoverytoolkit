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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.util.logic.LogicalExpression;
import org.sd.util.logic.LogicalOperator;
import org.sd.util.logic.LogicalResult;
import org.sd.util.logic.LogicalStatement;
import org.sd.util.logic.TruthFunction;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Classifier that allows conditional logic for successful matching.
 * <p>
 * Essentially, this wraps RoteListClassifier instances with the conditional
 * logic.
 *
 * @author Spence Koehler
 */
public class ConditionalClassifier extends AbstractAtnStateTokenClassifier {
  
  //
  // <condTerm>
  //   <condStatement />
  //   ...
  // </condTerm>
  //
  // where condTerm is one of "and" (N-ary), "or" (N-ary), "xor" (binary), or
  // "not" (unary).
  //
  // and a condStatement is either a condTerm or "classifierDef", under which a
  // classifier is defined (equivalent to classifiers under a grammar "classifiers"
  // element) like, for example, a RoteListClassifier instance.
  // 
  // RoteListClssifierNotes:
  // - Note that the logic of a RoteListClassifier is to "or" all of its terms
  //   except "test" terms ("predelim", "postdelim", and "test"), which are
  //   "and"ed.
  //
  // - Note also that unless there are only test terms, test terms are only applied
  //   upon successful classification by any of the other terms.
  //

  private ResourceManager resourceManager;
  private LogicalExpression<ClassifierInput> logicalExpression;

  public ConditionalClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement, resourceManager);
  }

  private final void init(DomElement classifierIdElement, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.logicalExpression = null;
    doSupplement(classifierIdElement);
  }

  /**
   * Supplement this classifier with the given dom node.
   */
  public void supplement(DomNode supplementNode) {
    doSupplement(supplementNode);
  }

  protected final void doSupplement(DomNode classifierIdElement) {
    this.logicalExpression = loadLogicalExpression(classifierIdElement);
    //NOTE: supplementing currently replaces instead of updates. Fix if/when needed.
  }

  /** Do the basic classification work. */
  protected boolean doClassify(Token token, AtnState atnState) {
    final TokenInput classifierInput = new TokenInput(token, atnState);
    final List<LogicalResult<ClassifierInput>> results = logicalExpression.evaluate(classifierInput);
    return results != null;
  }

  /** Do text classification over already normalized text if possible. */
  protected Map<String, String> doClassify(String normalizedText) {
    Map<String, String> result = null;

    final TextInput classifierInput = new TextInput(normalizedText);
    final List<LogicalResult<ClassifierInput>> results = logicalExpression.evaluate(classifierInput);

    if (results != null) {
      result = new HashMap<String, String>();
      for (LogicalResult<ClassifierInput> logicalResult : results) {
        if (logicalResult.isTrue()) {
          result.putAll(((TextClassifierResult)logicalResult).getFeatures());
        }
      }
    }

    return result;
  }


  private final LogicalExpression<ClassifierInput> loadLogicalExpression(DomNode classifierIdElement) {
    final Tree<LogicalStatement<ClassifierInput>> rootNode =
      new Tree<LogicalStatement<ClassifierInput>>(
        new LogicalOperator<ClassifierInput>(LogicalOperator.Type.OR));

    loadStatements(rootNode, (DomElement)classifierIdElement);

    final LogicalExpression<ClassifierInput> result =
      new LogicalExpression<ClassifierInput>(rootNode);

    return result;
  }

  private final void loadStatements(Tree<LogicalStatement<ClassifierInput>> parentNode, DomElement statementsParent) {

    // find condTerm ("and", "or", "xor", "not") or "classifierDef"
    final NodeList childNodes = statementsParent.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNodeNum = 0; childNodeNum < numChildNodes; ++childNodeNum) {
      final Node childNode = childNodes.item(childNodeNum);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

      final DomElement childElement = (DomElement)childNode;
      final String childNodeName = childNode.getLocalName();

      LogicalOperator<ClassifierInput> operator = null;

      if ("and".equalsIgnoreCase(childNodeName)) {
        operator = new LogicalOperator<ClassifierInput>(LogicalOperator.Type.AND);
      }
      else if ("or".equalsIgnoreCase(childNodeName)) {
        operator = new LogicalOperator<ClassifierInput>(LogicalOperator.Type.OR);
      }
      else if ("xor".equalsIgnoreCase(childNodeName)) {
        operator = new LogicalOperator<ClassifierInput>(LogicalOperator.Type.XOR);
      }
      else if ("not".equalsIgnoreCase(childNodeName)) {
        operator = new LogicalOperator<ClassifierInput>(LogicalOperator.Type.NOT);
      }
      else if ("classifierDef".equals(childNodeName)) {
        final AtnStateTokenClassifier classifier =
          (AtnStateTokenClassifier)resourceManager.getResource(childElement, new Object[]{getTokenClassifierHelper().getId2Normalizer()});
        final ClassifierExecutor executor = new ClassifierExecutor(classifier);
        parentNode.addChild(executor);
      }
      else {
        if (GlobalConfig.verboseLoad()) {
          System.out.println("WARNING: ConditionalClassifier.loadStatements encountered unrecognized term '" + childNodeName + "'");
        }
      }

      // operator recursion
      if (operator != null) {
        final Tree<LogicalStatement<ClassifierInput>> condNode = parentNode.addChild(operator);
        loadStatements(condNode, childElement);
      }
    }
  }


  //
  // Internal design notes
  //
  // - ClassifierInput
  //     -- holds as a single object the token + atnState or text for classification
  //     -- used as input container for evaluating the LogicalExpression
  //   - TokenInput -- holds token and atnState input for classification
  //   - TextInput -- holds text input for classification
  // - ClassifierResult - - > LogicalResult<ClassifierInput>
  //     -- holds the result of LogicalExpression evaluation over ClassifierInput
  //   - TokenClassifierResult -- holds the MatchResult from token and atnState classification
  //   - TextClassifierResult -- holds the feature map from text classification
  // - ClassifierExecutor --> TruthFunction<ClassifierInput>
  //     -- the logic TruthFunction for evaluating ClassifierInput
  //     -- essentially a container for a classifier and its execution
  //


  private static abstract class ClassifierInput {
    public TokenInput asTokenInput() { return null; }
    public TextInput asTextInput() { return null; }
  }

  private static final class TokenInput extends ClassifierInput {
    public final Token token;
    public final AtnState atnState;

    public TokenInput(Token token, AtnState atnState) {
      this.token = token;
      this.atnState = atnState;
    }

    public TokenInput asTokenInput() { return this; }
  }

  private static final class TextInput extends ClassifierInput {
    public final String text;

    public TextInput(String text) {
      this.text = text;
    }

    public TextInput asTextInput() { return this; }
  }

  private static abstract class ClassifierResult implements LogicalResult<ClassifierInput> {
    private ClassifierInput classifierInput;
    private ClassifierExecutor classifierExecutor;

    public ClassifierResult(ClassifierInput classifierInput, ClassifierExecutor classifierExecutor) {
      this.classifierInput = classifierInput;
      this.classifierExecutor = classifierExecutor;
    }

    public ClassifierInput getInput() {
      return classifierInput;
    }

    public TruthFunction<ClassifierInput> getTruthFunction() {
      return classifierExecutor;
    }

    public boolean isTrue() {
      boolean result = false;

      final TokenClassifierResult tokenClassifierResult = asTokenClassifierResult();
      if (tokenClassifierResult != null) {
        result = tokenClassifierResult.matched();
      }
      else {
        TextClassifierResult textClassifierResult = asTextClassifierResult();
        result = textClassifierResult.getFeatures() != null;
      }

      return result;
    }

    public TokenClassifierResult asTokenClassifierResult() { return null; }
    public TextClassifierResult asTextClassifierResult() { return null; }
  }

  private static final class TokenClassifierResult extends ClassifierResult {
    private MatchResult matchResult;

    public TokenClassifierResult(ClassifierInput classifierInput, ClassifierExecutor classifierExecutor, MatchResult matchResult) {
      super(classifierInput, classifierExecutor);
      this.matchResult = matchResult;
    }

    public boolean matched() {
      return matchResult != null && matchResult.matched();
    }
  }

  private static final class TextClassifierResult extends ClassifierResult {
    private Map<String, String> features;

    public TextClassifierResult(ClassifierInput classifierInput, ClassifierExecutor classifierExecutor, Map<String, String> features) {
      super(classifierInput, classifierExecutor);
      this.features = features;
    }

    public Map<String, String> getFeatures() {
      return features;
    }
  }

  private static final class ClassifierExecutor extends TruthFunction<ClassifierInput> {
    private AtnStateTokenClassifier classifier;

    public ClassifierExecutor(AtnStateTokenClassifier classifier) {
      this.classifier = classifier;
    }

    public LogicalResult<ClassifierInput> evaluateInput(ClassifierInput input) {
      LogicalResult<ClassifierInput> result = null;

      final TokenInput tokenInput = input.asTokenInput();
      if (tokenInput != null) {
        final MatchResult matchResult = classifier.classify(tokenInput.token, tokenInput.atnState);
        result = new TokenClassifierResult(input, this, matchResult);
      }
      else {
        final TextInput textInput = input.asTextInput();
        final Map<String, String> features = classifier.classify(textInput.text);
        result = new TextClassifierResult(input, this, features);
      }

      return result;
    }
  }
}
