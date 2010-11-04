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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;
import org.sd.xml.XPath;
import org.sd.xml.XPathApplicator;
import org.w3c.dom.NodeList;

/**
 * A generic parse interpreter.
 *
 * @author Spence Koehler
 */
public class GenericParseInterpreter implements AtnParseInterpreter {
  
  private String[] classifications;
  private FieldsContainer fields;

  /**
   * Construct with a domNode of the form:
   * <pre>
   * &lt;...&gt;
   *   &lt;jclass&gt;...&lt;/jclass&gt;
   *   &lt;classifications&gt;
   *     &lt;classification&gt;classification&lt;/classification&gt;
   *     ...
   *   &lt;/classifications&gt;
   *   &lt;fields&gt;
   *     &lt;field|fields ...field-options...&gt;fieldspec&lt;/field|fields&gt;
   *     ...
   *   &lt;/fields&gt;
   * </pre>
   */
  public GenericParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    this.classifications = loadClassifications(domNode, resourceManager);
    this.fields = new FieldsContainer(domNode, resourceManager);
  }

  /**
   * Get classifications offered by this interpreter.
   * <p>
   * Note that classifications are applied to parse-based tokens.
   */
  public String[] getClassifications() {
    return classifications;
  }

  /**
   * Get the interpretations for the parse or null.
   */
  public List<ParseInterpretation> getInterpretations(AtnParse parse) {
    List<ParseInterpretation> result = null;

    final List<Tree<XmlLite.Data>> interpTrees = fields.getInterpretationTrees(parse.getParseTree());
    if (interpTrees != null) {
      result = new ArrayList<ParseInterpretation>();
      for (Tree<XmlLite.Data> interpTree : interpTrees) {
        result.add(new ParseInterpretation(interpTree));
      }
    }

    return result;
  }


  private static final String getXmlString(DomNode domNode) {
    String xmlString = null;
    try {
      xmlString = XmlLite.asXml(domNode.asTree(), false);
    }
    catch (java.io.IOException e) {
      xmlString = domNode.getNodeName();
    }
    return xmlString;
  }

  private final String[] loadClassifications(DomNode domNode, ResourceManager resourceManager) {
    final DomElement classificationsElement = (DomElement)domNode.selectSingleNode("classifications");
    if (classificationsElement == null) {
      final String xmlString = getXmlString(domNode);
      throw new IllegalArgumentException("parseInterpreter missing 'classifications' element! " + xmlString);
    }
    final NodeList classificationNodesList = classificationsElement.selectNodes("classification");
    final List<String> cList = new ArrayList<String>();
    for (int nodeNum = 0; nodeNum < classificationNodesList.getLength(); ++nodeNum) {
      final DomElement classificationNode = (DomElement)classificationNodesList.item(nodeNum);
      cList.add(classificationNode.getTextContent().trim());
    }
    return cList.toArray(new String[cList.size()]);
  }


  /**
   * Given an expression string of the form "command(arg1, arg2, ...)", return
   * the command (in result[0]) and all args (in result[1+]).
   */
  private static final String[] parseExpression(String expr) {
    String[] result = null;

    final int lpPos = expr.indexOf('(');
    if (lpPos >= 0) {
      final int rpPos = expr.lastIndexOf(')');
      final String argString = expr.substring(lpPos + 1, rpPos).trim();
      final String[] args = argString.split("\\s*,\\s*");

      result = new String[args.length + 1];
      result[0] = expr.substring(0, lpPos).trim();
      for (int argNum = 0; argNum < args.length; ++argNum) {
        result[argNum + 1] = args[argNum];
      }
    }
    else {
      // no args, just the command
      result = new String[]{expr.trim()};
    }

    return result;
  }

  private static final List<Field> loadFields(DomElement parentElement, ResourceManager resourceManager, XPathApplicator xpathApplicator) {
    final List<Field> result = new ArrayList<Field>();

    final NodeList childNodeList = parentElement.getChildNodes();
    for (int nodeNum = 0; nodeNum < childNodeList.getLength(); ++nodeNum) {
      final DomNode childNode = (DomNode)childNodeList.item(nodeNum);
      if (childNode.getNodeType() != DomNode.ELEMENT_NODE) continue;
      final String nodeName = childNode.getNodeName();
      if (!("field".equals(nodeName) || "fields".equals(nodeName))) continue;
      final DomElement fieldElement = (DomElement)childNode;
      result.add(new Field(fieldElement, resourceManager, xpathApplicator));
    }

    return result;
  }

  private static final IsOptionalFunction ALWAYS_OPTIONAL = new IsOptionalFunction() {
      public boolean isOptional(Tree<XmlLite.Data> interpretationTree) { return true; }
      public String toString() { return "optional(false)"; }
    };
  private static final IsOptionalFunction NEVER_OPTIONAL = new IsOptionalFunction() {
      public boolean isOptional(Tree<XmlLite.Data> interpretationTree) { return false; }
      public String toString() { return "optional(false)"; }
    };

  private static final IsOptionalFunction buildIsOptionalFunction(String spec, XPathApplicator xpathApplicator) {
    // IsOptionalFunction
    //
    //  false
    //  true
    //  exists(xpath)  <-- note: applied to emerging parse interpretation tree, not to input parse tree!

    IsOptionalFunction result = null;

    final String normalizedSpec = spec.trim().toLowerCase();

    if ("false".equals(normalizedSpec)) {
      result = NEVER_OPTIONAL;
    }
    else if ("true".equals(normalizedSpec)) {
      result = ALWAYS_OPTIONAL;
    }
    else {
      final String[] commandArgs = parseExpression(spec);

      if ("exists".equalsIgnoreCase(commandArgs[0])) {
        if (commandArgs.length == 2) {
          result = new ExistsOptionalFunction(commandArgs[1], xpathApplicator);
        }
        else {
          throw new IllegalArgumentException("Bad expression format '" + spec + "'!");
        }
      }
    }

    if (result == null) {
      // unrecognized optional function
      throw new IllegalArgumentException("Unrecognized 'optional' function spec '" + spec + "'!");
    }

    return result;
  }

  private static final ValueFunction buildValueFunction(String spec) {
    // ValueFunction
    //
    //  nodeText([nodePath])
    //  parseInterpretation([nodePath, ]feature)  <- tokenFeature's parseInterp's interpTree
    //  tokenFeature([nodePath, ]feature)
    //  tokenText([nodePath])
    //  nodeAttribute([nodePath, ]attr)

    ValueFunction result = null;

    final String[] commandArgs = parseExpression(spec);
    final String command = commandArgs[0];

    if ("nodeText".equals(command)) {
      result = new NodeTextValueFunction(commandArgs);
    }
    else if ("parseInterpretation".equals(command)) {
      result = new ParseInterpretationValueFunction(commandArgs);
    }
    else if ("tokenFeature".equals(command)) {
      result = new TokenFeatureValueFunction(commandArgs);
    }
    else if ("tokenText".equals(command)) {
      result = new TokenTextValueFunction(commandArgs);
    }
    else if ("nodeAttribute".equals(command)) {
      result = new NodeAttributeValueFunction(commandArgs);
    }

    if (result == null) {
      // unrecognized value function
      throw new IllegalArgumentException("Unrecognized 'value' function spec '" + spec + "'!");
    }

    return result;
  }

  private static final VerificationFunction buildVerificationFunction(String spec) {
    // VerificationFunction
    //
    //  nodeAttribute(attr, val)

    VerificationFunction result = null;

    if (spec != null) {
      final String[] commandArgs = parseExpression(spec);
      final String command = commandArgs[0];

      if ("nodeAttribute".equals(command)) {
        result = new NodeAttributeVerificationFunction(commandArgs);
      }
    }

    return result;
  }


  public interface IsOptionalFunction {

    /**
     * Determine whether the current field is optional based on the full
     * interpretation tree.
     */
    public boolean isOptional(Tree<XmlLite.Data> interpretationTree);

  }

  public static class ExistsOptionalFunction implements IsOptionalFunction {
    private String xpath;
    private XPathApplicator xpathApplicator;

    public ExistsOptionalFunction(String xpath, XPathApplicator xpathApplicator) {
      this.xpath = xpath;
      this.xpathApplicator = xpathApplicator;
    }

    /**
     * Determine whether the current field is optional based on the full
     * interpretation tree.
     */
    public boolean isOptional(Tree<XmlLite.Data> interpretationTree) {
      // if a node exists in the tree for the pattern, then isOptional is true
      return xpathApplicator.getFirstNode(xpath, interpretationTree) != null;
    }

    public String toString() {
      return "exists(" + xpath + ")";
    }
  }

  public interface ValueFunction {

    /**
     * Get the interpretation value for the given parse tree node to insert
     * into the emerging interpretation tree.
     */
    public Tree<XmlLite.Data> getInterpretationValue(Tree<String> parseTreeNode);

  }

  static abstract class AbstractValueFunction implements ValueFunction {

    protected abstract Tree<XmlLite.Data> doGetInterpretationValue(Tree<String> parseTreeNode);

    private NodePath<String> nodePath;

    protected AbstractValueFunction(String nodePath) {
      this.nodePath = nodePath == null ? null : new NodePath<String>(nodePath);
    }

    public final Tree<XmlLite.Data> getInterpretationValue(Tree<String> parseTreeNode) {
      Tree<XmlLite.Data> result = null;

      Tree<String> selectedParseTreeNode = parseTreeNode;

      if (nodePath != null) {
        final List<Tree<String>> selectedNodes = nodePath.apply(parseTreeNode);
        if (selectedNodes != null && selectedNodes.size() > 0) {
          selectedParseTreeNode = selectedNodes.get(0);
        }
        else {
          selectedParseTreeNode = null;
        }
      }

      if (selectedParseTreeNode != null) {
        result = doGetInterpretationValue(selectedParseTreeNode);
      }

      return result;
    }
  }

  static abstract class TokenValueFunction extends AbstractValueFunction {

    protected abstract Tree<XmlLite.Data> doGetInterpretationValue(CategorizedToken cToken);

    protected TokenValueFunction(String nodePath) {
      super(nodePath);
    }

    protected final Tree<XmlLite.Data> doGetInterpretationValue(Tree<String> parseTreeNode) {
      Tree<XmlLite.Data> result = null;

      final CategorizedToken cToken = ParseInterpretationUtil.getCategorizedToken(parseTreeNode);
      if (cToken != null) {
        result = doGetInterpretationValue(cToken);
      }

      return result;
    }
  }

  static abstract class AbstractTokenFeatureValueFunction extends TokenValueFunction {

    protected abstract Tree<XmlLite.Data> doGetInterpretationValue(Feature feature);

    private String featureKey;
    private Class featureClass;

    protected AbstractTokenFeatureValueFunction(String nodePath, String featureKey, Class featureClass) {
      super(nodePath);

      this.featureKey = featureKey;
      this.featureClass = featureClass;
    }

    protected final Tree<XmlLite.Data> doGetInterpretationValue(CategorizedToken cToken) {
      Tree<XmlLite.Data> result = null;

      if (cToken.token.hasFeatures()) {
        //NOTE: only getting the *first* matching feature!
        final Feature feature = cToken.token.getFeature(featureKey, null, featureClass);
        if (feature != null) {
          result = doGetInterpretationValue(feature);
        }
      }

      return result;
    }
  }

  static class NodeTextValueFunction extends AbstractValueFunction {

    NodeTextValueFunction(String[] commandArgs) {
      super(commandArgs.length > 1 ? commandArgs[1] : null);
    }

    protected Tree<XmlLite.Data> doGetInterpretationValue(Tree<String> parseTreeNode) {
      return XmlLite.createTextNode(parseTreeNode.getLeafText());
    }
  }

  static class ParseInterpretationValueFunction extends AbstractTokenFeatureValueFunction {

    ParseInterpretationValueFunction(String[] commandArgs) {
      super(
        commandArgs.length > 2 ? commandArgs[1] : null,           // nodePath
        commandArgs.length > 2 ? commandArgs[2] : commandArgs[1], // feature
        ParseInterpretation.class);                               // featureClass
    }

    protected Tree<XmlLite.Data> doGetInterpretationValue(Feature feature) {
      return ((ParseInterpretation)feature.getValue()).getInterpTree();
    }
  }

  static class TokenFeatureValueFunction extends AbstractTokenFeatureValueFunction {

    TokenFeatureValueFunction(String[] commandArgs) {
      super(
        commandArgs.length > 2 ? commandArgs[1] : null,           // nodePath
        commandArgs.length > 2 ? commandArgs[2] : commandArgs[1], // feature
        null);                                                    // featureClass
    }

    public Tree<XmlLite.Data> doGetInterpretationValue(Feature feature) {
      Tree<XmlLite.Data> result = null;

      final Object value = feature.getValue();
      if (value != null) {
        result = XmlLite.createTextNode(value.toString());
      }

      return result;
    }
  }

  static class TokenTextValueFunction extends TokenValueFunction {

    TokenTextValueFunction(String[] commandArgs) {
      super(commandArgs.length > 1 ? commandArgs[1] : null);
    }

    protected Tree<XmlLite.Data> doGetInterpretationValue(CategorizedToken cToken) {
      Tree<XmlLite.Data> result = null;

      //NOTE: only getting the *first* matching feature!
      result = XmlLite.createTextNode(cToken.token.getText());

      return result;
    }
  }

  static class NodeAttributeValueFunction extends AbstractValueFunction {

    private String attr;

    NodeAttributeValueFunction(String[] commandArgs) {
      super(commandArgs.length > 2 ? commandArgs[1] : null);
      this.attr = (commandArgs.length > 2 ? commandArgs[2] : commandArgs[1]);
    }

    protected Tree<XmlLite.Data> doGetInterpretationValue(Tree<String> parseTreeNode) {
      Tree<XmlLite.Data> result = null;

      if (parseTreeNode.hasAttributes()) {
        final Map<String, Object> parseAttributes = parseTreeNode.getAttributes();
        final Object value = parseAttributes.get(attr);
        if (value != null) {
          result = XmlLite.createTextNode(value.toString());
        }
      }

      return result;
    }
  }


  public interface VerificationFunction {

    /**
     * Verify a parseTree node for interpretation applicability.
     */
    public boolean isApplicable(Tree<String> selectedParseTreeNode);
  }

  static final class NodeAttributeVerificationFunction implements VerificationFunction {

    final String attr;
    final String val;

    NodeAttributeVerificationFunction(String[] commandArgs) {
      this.attr = commandArgs[1];
      this.val = commandArgs.length > 2 ? commandArgs[2] : null;
    }

    public boolean isApplicable(Tree<String> selectedParseTreeNode) {
      boolean result = false;

      if (selectedParseTreeNode.hasAttributes()) {
        final Map<String, Object> parseAttributes = selectedParseTreeNode.getAttributes();
        final Object value = parseAttributes.get(attr);

        if (this.val == null) {
          // one argument spec means just test for existence of a non-null value
          result = value != null;
        }
        else {
          // check that the value's toString equals the spec val
          if (value != null) {
            result = this.val.equals(value.toString());
          }
        }
      }

      return result;
    }
  }


  static final class FieldsContainer {

    private List<Fields> fields;
    private XPathApplicator xpathApplicator;

    FieldsContainer(DomNode parseInterpreterNode, ResourceManager resourceManager) {
      this.fields = new ArrayList<Fields>();
      this.xpathApplicator = new XPathApplicator();

      final NodeList childNodeList = parseInterpreterNode.getChildNodes();
      for (int nodeNum = 0; nodeNum < childNodeList.getLength(); ++nodeNum) {
        final DomNode childNode = (DomNode)childNodeList.item(nodeNum);
        if (childNode.getNodeType() != DomNode.ELEMENT_NODE || !"fields".equals(childNode.getNodeName())) continue;
        final DomElement fieldsElement = (DomElement)childNode;
        fields.add(new Fields(fieldsElement, resourceManager, xpathApplicator));
      }
    }

    List<Tree<XmlLite.Data>> getInterpretationTrees(Tree<String> parseTree) {
      List<Tree<XmlLite.Data>> result = null;

      for (Fields fieldsInstance : fields) {
        final Tree<XmlLite.Data> interp = fieldsInstance.getInterpretationTree(parseTree);
        if (interp != null) {
          if (result == null) result = new ArrayList<Tree<XmlLite.Data>>();
          result.add(interp);
        }
      }

      return result;
    }
  }

  static final class Fields {

    // each "fields" instance corresponds to a potential interpretation for a parseTree

    private String select;
    private String name;
    private NodePath<String> nodePath;
    private List<Field> fields;

    Fields(DomElement fieldsElement, ResourceManager resourceManager, XPathApplicator xpathApplicator) {
      this.select = fieldsElement.getAttributeValue("select");
      this.name = fieldsElement.getAttributeValue("name", select);

      if (select == null && name == null) {
        final String xmlString = getXmlString(fieldsElement);
        throw new IllegalArgumentException("fieldsElement missing 'select' and 'name' attrs! " + xmlString);
      }

      this.nodePath = new NodePath<String>(select);
      this.fields = loadFields(fieldsElement, resourceManager, xpathApplicator);
    }

    Tree<XmlLite.Data> getInterpretationTree(Tree<String> parseTree) {
      Tree<XmlLite.Data> result = null;

      final List<Tree<String>> parseTreeNodes = nodePath.apply(parseTree);
      if (parseTreeNodes != null && parseTreeNodes.size() > 0) {
        final Tree<String> parseTreeNode = parseTreeNodes.get(0);

        // collect all IsOptionalFunctions for fields that fail to match (not selected);
        // apply to final interpretation tree; fail/reject interpretation if any are not optional
        final List<IsOptionalFunction> optionalFunctions = new ArrayList<IsOptionalFunction>();

        final Tree<XmlLite.Data> interp = XmlLite.createTagNode(name);

        final boolean[] gotOne = new boolean[]{false};
        for (Field field : fields) {
          final Tree<XmlLite.Data> fieldTree = field.buildInterpretationTree(parseTreeNode, interp, optionalFunctions, gotOne);
        }

        if (gotOne[0] && interp != null) {
          result = interp;

          for (IsOptionalFunction isOptionalFunction : optionalFunctions) {
            if (!isOptionalFunction.isOptional(result)) {
              // failed optionality test
              result = null;
              break;
            }
          }
        }
      }

      return result;
    }
  }

  static final class Field {

    // each "field" instance builds a parse interpretation node (and its children)

    private String select;
    private String name;
    private NodePath<String> nodePath;
    private IsOptionalFunction optional;
    private VerificationFunction verificationFunction;
    private ValueFunction valueFunction;  // non-null when "terminal"
    private List<Field> children;         // non-null when not "terminal"


    Field(DomElement fieldElement, ResourceManager resourceManager, XPathApplicator xpathApplicator) {
      final String eltName = fieldElement.getNodeName();

      this.select = fieldElement.getAttributeValue("select", null);
      this.name = fieldElement.getAttributeValue("name", select);
      this.nodePath = select != null ? new NodePath<String>(select) : null;

      final String optionalString = fieldElement.getAttributeValue("optional", "false");
      this.optional = buildIsOptionalFunction(optionalString, xpathApplicator);

      final String verifyString = fieldElement.getAttributeValue("verify", null);
      this.verificationFunction = buildVerificationFunction(verifyString);

      if ("field".equals(eltName)) {
        // build value function
        final String valueString = fieldElement.getAttributeValue("value", "nodeText");
        if (valueString == null) {
          final String xmlString = getXmlString(fieldElement);
          throw new IllegalArgumentException("fieldElement missing 'value' attr! " + xmlString);
        }
        this.valueFunction = buildValueFunction(valueString);
      }
      else if ("fields".equals(eltName)) {
        // recurse
        this.children = loadFields(fieldElement, resourceManager, xpathApplicator);
      }
    }

    /**
     * Recursive auxiliary for building an interpretation tree.
     */
    private final Tree<XmlLite.Data> buildInterpretationTree(Tree<String> parseTreeNode, Tree<XmlLite.Data> interpNode,
                                                             List<IsOptionalFunction> optionalFunctions, boolean[] gotOne) {
      Tree<XmlLite.Data> result = null;

      final List<Tree<String>> selectedParseTreeNodes = getSelectedParseTreeNodes(parseTreeNode);
      if (selectedParseTreeNodes != null) {
        result = interpNode;

        for (Tree<String> selectedParseTreeNode : selectedParseTreeNodes) {
          if (verificationFunction != null) {
            if (!verificationFunction.isApplicable(selectedParseTreeNode)) {
              continue;
            }
          }

          if (valueFunction != null) {
            // add interp value
            final Tree<XmlLite.Data> interpValueNode = valueFunction.getInterpretationValue(selectedParseTreeNode);
            if (interpValueNode != null) {
              final Tree<XmlLite.Data> interpNodeChild = select == null ? interpNode : interpNode.addChild(XmlLite.createTagNode(name));
              interpNodeChild.addChild(interpValueNode);
              gotOne[0] = true;
            }
          }
          else {
            final Tree<XmlLite.Data> interpNodeChild = select == null ? interpNode : XmlLite.createTagNode(name);

            // recurse for nested fields
            for (Field field : children) {
              field.buildInterpretationTree(selectedParseTreeNode, interpNodeChild, optionalFunctions, gotOne);
            }

            if (gotOne[0] && select != null) interpNode.addChild(interpNodeChild);
          }
        }
      }
      else {
        // selected field node isn't available
        optionalFunctions.add(optional);
      }

      return result;
    }

    private final List<Tree<String>> getSelectedParseTreeNodes(Tree<String> parseTreeNode) {
      List<Tree<String>> result = null;

      if (nodePath != null) {
        result = nodePath.apply(parseTreeNode);
      }
      else {
        result = new ArrayList<Tree<String>>();
        result.add(parseTreeNode);
      }

      return result;
    }
  }
}
