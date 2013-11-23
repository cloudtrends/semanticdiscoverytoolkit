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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
import org.sd.token.KeyLabel;
import org.sd.util.Usage;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;
import org.w3c.dom.NodeList;

/**
 * An ParseInterpreter that uses the parse itself as the interpretation.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.ParseInterpreter implementation that transforms\n" +
       "a parse tree directly into an interpretation."
  )
public class IdentityParseInterpreter implements ParseInterpreter {
  
  private boolean compress;
  protected boolean verbose;
  protected boolean keepInterpObjects;
  private Map<String, String> featureMap;

  public IdentityParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    this.compress = domNode.getAttributeBoolean("compress", false);
    this.verbose = domNode.getAttributeBoolean("verbose", false);
    this.keepInterpObjects = domNode.getAttributeBoolean("keepInterpObjects", true);
    this.featureMap = loadFeatureMap(domNode);
  }

  public IdentityParseInterpreter(boolean compress) {
    this.compress = compress;
  }

  /**
   * Get classifications offered by this interpreter.
   * 
   * Note that classifications are applied to parse-based tokens. Access
   * to the potential classifications is intended to help with monitoring,
   * introspection, and other high-level tools for building grammars and
   * parsers.
   */
  public String[] getClassifications() {
    return new String[]{"identity"};
  }

  /**
   * Get the interpretations for the parse or null.
   */
  public List<ParseInterpretation> getInterpretations(Parse parse, DataProperties overrides) {
    final List<ParseInterpretation> result = new ArrayList<ParseInterpretation>();
    final Context context = buildContextHook(parse, overrides);
    final Tree<XmlLite.Data> interpTree = asInterpTree(context, parse.getParseTree(), null);
    final ParseInterpretation parseInterp = buildParseInterpretationHook(context, interpTree);
    parseInterp.setParse(parse);
    if (parse.hasAtnParse()) parseInterp.setSourceParse(parse.getAtnParse());
    result.add(parseInterp);
    return result;
  }

  /**
   * Supplement this interpreter according to the given domElement.
   */
  public void supplement(DomElement domElement) {
    // nothing to do.
  }

  public Map<String, String> getFeatureMap() {
    return featureMap;
  }

  public void setFeatureMap(Map<String, String> featureMap) {
    this.featureMap = featureMap;
  }

  public String getMappedFeature(String cat) {
    String result = null;

    if (featureMap != null) {
      result = featureMap.get(cat);
    }

    return result;
  }


  private final Map<String, String> loadFeatureMap(DomNode domNode) {
    Map<String, String> result = null;

    final NodeList featureMappingsNodes = domNode.selectNodes("featureMappings");
    if (featureMappingsNodes != null) {
      result = new HashMap<String, String>();
      for (int fmidx = 0; fmidx < featureMappingsNodes.getLength(); ++fmidx) {
        final DomElement fmElt = (DomElement)featureMappingsNodes.item(fmidx);
        final NodeList mapNodes = fmElt.selectNodes("map");
        if (mapNodes != null) {
          for (int midx = 0; midx < mapNodes.getLength(); ++midx) {
            final DomElement mapElt = (DomElement)mapNodes.item(midx);
            final String keyString = mapElt.getAttributeValue("key", null);
            final String valueString = mapElt.getAttributeValue("value", null);
            if (keyString != null && valueString != null) {
              final String[] keys = keyString.split("\\s*,\\s*");
              final String[] values = valueString.split("\\s*,\\s*");
              for (String key : keys) {
                for (String value : values) {
                  result.put(key, value);
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  private final Tree<XmlLite.Data> asInterpTree(Context context, Tree<String> parseTree, Tree<XmlLite.Data> parent) {
    Set<Tree<String>> compressNodes = null;

    if (compress) {
      compressNodes = getCompressNodes(parseTree);
      context.setCompressNodes(compressNodes);
    }

    final Tree<XmlLite.Data> result = convertTree(context, parseTree, parent);
    return result;
  }

  private final Tree<XmlLite.Data> convertTree(Context context, Tree<String> parseTree, Tree<XmlLite.Data> parent) {

    final NodeContainer nodeContainer = buildNodeContainerHook(context, parseTree);
    context.setCurNodeContainer(nodeContainer);

    if (nodeContainer.isTag() && context.hasCompressNode(parseTree)) {
      nodeContainer.setTerminateFlag(true);
    }

    // Collect interp object
    if (nodeContainer.hasParseInterp()) {
      final ParseInterpretation interp = nodeContainer.getParseInterp();
      if (interp.hasInterpretationObject()) {
        final Serializable interpObject = (Serializable)interp.getInterpretation();
        if (interpObject != null) {
          context.addInterpObject(interp.getClassification(), interpObject);
        }
      }
    }

    // echo location when verbose
    if (verbose) {
      System.out.print(this.getClass().getName() + ": Visiting '" + parseTree.getData() + "'(" + nodeContainer.getDepth() + ")");
      if (nodeContainer.hasParseInterp()) {
        System.out.print("*");
      }
      if (nodeContainer.isAmbiguous()) {
        System.out.print("@");
      }
      System.out.println();
    }

    // Hook for extenders to modify context and/or nodeContainer
    context.updateHook(nodeContainer);

    // get final interp node
    final Tree<XmlLite.Data> curInterpNode = nodeContainer.getFinalInterpNode();

    // construct tree
    if (parent != null && curInterpNode != null) {
      parent.addChild(curInterpNode);
    }

    // recurse
    if (nodeContainer.getRecurse() && curInterpNode != null) {
      // add children
      for (Tree<String> childTree : parseTree.getChildren()) {
        convertTree(context, childTree, curInterpNode);
      }
    }

    return curInterpNode;
  }

  /**
   * Hook for extenders to modify Context construction.
   */
  protected Context buildContextHook(Parse parse, DataProperties overrides) {
    return new Context(parse, overrides, verbose);
  }

  /**
   * Hook for extenders to modify NodeContainer construction.
   */
  protected NodeContainer buildNodeContainerHook(Context context, Tree<String> parseTree) {
    return new NodeContainer(this, parseTree, context.nextSeqNum());
  }

  /**
   * Hook for extenders to modify final ParseInterpretation construction.
   */
  protected ParseInterpretation buildParseInterpretationHook(Context context, Tree<XmlLite.Data> interpTree) {
    final ParseInterpretation result = new ParseInterpretation(interpTree);
    if (keepInterpObjects) {
      context.setInterpObject(result);
    }
    return result;
  }

  private final Set<Tree<String>> getCompressNodes(Tree<String> parseTree) {
    final Set<Tree<String>> result = new HashSet<Tree<String>>();
    final Set<Tree<String>> gpNodes = new HashSet<Tree<String>>();

    // collect nodes 2 above the leaves
    final List<Tree<String>> leaves = parseTree.gatherLeaves();
    for (Tree<String> leaf : leaves) {
      final Tree<String> parent = leaf.getParent();
      if (parent != null) {
        final Tree<String> gparent = parent.getParent();
        if (gparent != null) {
          gpNodes.add(gparent);
        }
      }
    }

    // ascend nodes
    for (Tree<String> gpNode : gpNodes) {
      if (gpNode.equidepth()) {
        result.add(gpNode.ascend());
      }
    }

    return result;
  }


  protected static class Context {
    private Parse parse;
    private DataProperties overrides;
    protected boolean verbose;
    private Set<Tree<String>> compressNodes;
    private int seqNum;
    private Map<String, List<Serializable>> interpObjects;

    private NodeContainer curNodeContainer;
    private NodeContainer prevNodeContainer;

    public Context(Parse parse, DataProperties overrides, boolean verbose) {
      this.parse = parse;
      this.overrides = overrides;
      this.verbose = verbose;
      this.compressNodes = null;
      this.curNodeContainer = null;
      this.prevNodeContainer = null;
      this.seqNum = -1;
      this.interpObjects = null;

      if (verbose) {
        System.out.println(this.getClass().getName() + ": Analyzing " + parse.getParseTree());
      }
    }

    private void setCurNodeContainer(NodeContainer curNodeContainer) {
      this.prevNodeContainer = this.curNodeContainer;
      this.curNodeContainer = curNodeContainer;
    }

    /**
     * Get the previously visited nodeContainer (traversal order).
     */
    public NodeContainer getPrevNodeContainer() {
      return prevNodeContainer;
    }

    /**
     * Hook for extenders to modify context and/or nodeContainer.
     */
    protected void updateHook(NodeContainer nodeContainer) {
      // nothing to do here.
    }

    public Parse getParse() {
      return parse;
    }

    public Tree<String> getParseTree() {
      return parse.getParseTree();
    }

    public DataProperties getOverrides() {
      return overrides;
    }

    public boolean hasCompressNode(Tree<String> parseTree) {
      boolean result = false;

      if (hasCompressNodes()) {
        result = compressNodes.contains(parseTree);
      }

      return result;
    }

    public boolean hasCompressNodes() {
      return compressNodes != null && compressNodes.size() > 0;
    }

    public Set<Tree<String>> getCompressNodes() {
      return compressNodes;
    }

    public void setCompressNodes(Set<Tree<String>> compressNodes) {
      this.compressNodes = compressNodes;
    }

    public int nextSeqNum() {
      return ++seqNum;
    }

    public int getSeqNum() {
      return seqNum;
    }

    public void addInterpObject(String type, Serializable interpObject) {
      if (interpObjects == null) interpObjects = new HashMap<String, List<Serializable>>();
      List<Serializable> objects = interpObjects.get(type);
      if (objects == null) {
        objects = new ArrayList<Serializable>();
        interpObjects.put(type, objects);
      }
      objects.add(interpObject);
    }

    public boolean hasInterpObjects() {
      return interpObjects != null && interpObjects.size() > 0;
    }

    /**
     * If there is only one interp object, return it; otherwise (0 or 2+) return null.
     */
    public Serializable getSingleInterpObject() {
      Serializable result = null;

      if (interpObjects != null && interpObjects.size() == 1) {
        for (List<Serializable> objects : interpObjects.values()) {
          if (objects.size() == 1) {
            result = objects.get(0);
          }
        }
      }

      return result;
    }

    public Map<String, List<Serializable>> getInterpObjects() {
      return interpObjects;
    }

    /**
     * This method sets an interpretation object on the interp based on this
     * instance's interpObjects using the following rules:
     * <ul>
     * <li>If there are no interpObjects in this context, nothing is done</li>
     * <li>If there is exactly one interpretation object, that object is set
     *     as the given interp's interpretation object.</li>
     * <li>Otherwise, (more than one interp object), the map from type to
     *     list of interp objects is set as the given interp's interpretation.</li>
     * </ul>
     * @return true if an interpretation object was set on the given interp.
     */
    public boolean setInterpObject(ParseInterpretation interp) {
      boolean result = false;

      if (hasInterpObjects()) {
        final Serializable single = getSingleInterpObject();
        if (single != null) {
          interp.setInterpretation(single);
          result = true;
        }
        else {
          interp.setInterpretation((Serializable)interpObjects);
          result = true;
        }
      }

      return result;
    }
  }

  protected static final class NodeContainer {
    private IdentityParseInterpreter interpreter;
    private Tree<String> parseTree;  // immutable
    private int seqNum;
    private boolean isTag;           // when false, adds parseTree's token's text or parseTree's data to tree (!isTerminal)
    private boolean recurse;         // when false, stops descending tree
    private Tree<XmlLite.Data> curInterpNode;
    private boolean terminate;       // terminate this branch with this node
    private ParseInterpretation parseInterp;
    private boolean ambiguous;
    private Integer _depth;

    public NodeContainer(IdentityParseInterpreter interpreter, Tree<String> parseTree, int seqNum) {
      this.interpreter = interpreter;
      this.parseTree = parseTree;
      this.seqNum = seqNum;
      this.isTag = parseTree.hasChildren();
      this.recurse = isTag;
      this.curInterpNode = null;
      this.terminate = false;
      this.parseInterp = null;
      this.ambiguous = false;
      this._depth = null;

      init();
    }

    private final void init() {
      if (isTag) {
        String nodeText = parseTree.getData();
        if ("?".equals(nodeText)) nodeText = "_UNK_";
        this.curInterpNode = XmlLite.createTagNode(nodeText);

        // add attributes
        if (parseTree.hasAttributes()) {
          for (Map.Entry<String, Object> entry : parseTree.getAttributes().entrySet()) {
            final String attr = entry.getKey();
            final Object val = entry.getValue();

            if (val != null) {
              final XmlLite.Tag tag = curInterpNode.getData().asTag();
              if (val instanceof CategorizedToken) {
                final CategorizedToken cToken = (CategorizedToken)val;

                //NOTE: escaping of attribute values done within XmlLite.Tag

                // tokPreDelim, tokPostDelim, tokKeyLabels
                final String preDelim = cToken.token.getPreDelim();
                final String postDelim = cToken.token.getPostDelim();
                final String keyLabels = KeyLabel.asString(cToken.token.getKeyLabels());

                if (!"".equals(preDelim)) {
                  tag.attributes.put("_tokPreDelim", preDelim);
                }
                tag.attributes.put("_tokText", cToken.token.getText());
                if (!"".equals(postDelim)) {
                  tag.attributes.put("_tokPostDelim", postDelim);
                }
                if (!"".equals(keyLabels)) {
                  tag.attributes.put("_tokKeyLabels", keyLabels);
                }

                // add-in token features
                if (cToken.token.hasFeatures()) {
                  ParseInterpretation primaryInterp = null;
                  ParseInterpretation altInterp = null;

                  for (Feature feature : cToken.token.getFeatures().getFeatures()) {
                    final Object featureValue = feature.getValue();
                    if (featureValue != null) {
                      final String className = featureValue.getClass().getName();

                      // just include "primitive" feature values
                      if (className.startsWith("java.lang")) {
                        tag.attributes.put(feature.getType(), featureValue.toString());
                      }
//                      else if (featureValue instanceof ParseInterpretation && feature.getType().equals(parseTree.getData())) {
                      else if (featureValue instanceof ParseInterpretation) {
                        final String featureType = feature.getType();
                        final String parseType = parseTree.getData();
                        if (featureType.equals(parseType)) {
                          primaryInterp = (ParseInterpretation)featureValue;
                        }
                        else {
                          final String mappedType = interpreter.getMappedFeature(parseType);
                          if (mappedType != null && featureType.equals(mappedType)) {
                            altInterp = (ParseInterpretation)featureValue;
                          }
                        }
                      }
                    }
                  }

                  if (primaryInterp != null) {
                    this.parseInterp = primaryInterp;
                  }
                  else if (altInterp != null) {
                    this.parseInterp = altInterp;
                  }
                }
              }
              else if ("ambiguous".equals(attr)) {
                this.ambiguous = "true".equals(val.toString());
              }
              else if (tag != null) {
                tag.attributes.put(attr, val.toString());
              }
            }
          }
        }

        if (this.parseInterp != null) {
          this.curInterpNode = this.parseInterp.getInterpTree();
          this.recurse = false;
        }
      }
    }

    /**
     * Get this container's parseTree node.
     */
    public Tree<String> getParseTree() {
      return parseTree;
    }

    /**
     * Get this instance's sequence number;
     */
    public int getSeqNum() {
      return seqNum;
    }

    /**
     * Determine whether the current parseTree is terminal (or has been set to appear so).
     */
    public boolean isTerminal() {
      return terminate ? true : !isTag;
    }

    /**
     * Set node to appear terminal (or not, in which case "terminate" may be "un"-set).
     */
    public void setIsTerminal(boolean isTerminal) {
      this.isTag = !isTerminal;

      if (!isTerminal) this.terminate = false;
    }

    /**
     * Determine whether the current parseTree is not terminal (or as been set to appear so).
     */
    public boolean isTag() {
      return terminate ? false : isTag;
    }

    /**
     * Set node to appear non-terminal ("un"-setting "terminate" if necessary) or terminal.
     */
    public void setIsTag(boolean isTag) {
      this.isTag = isTag;

      if (isTag) this.terminate = false;
    }

    /**
     * Sets this node as terminal and allows for "fixing" the final curInterpNode.
     */
    public void setAsTerminal() {
      this.isTag = false;
      this.recurse = false;
    }

    public boolean getTerminateFlag() {
      return terminate;
    }

    /**
     * Sets to terminate without "fixing" the final curInterpNode.
     */
    public void setTerminateFlag(boolean terminate) {
      this.terminate = terminate;
    }

    public boolean getRecurse() {
      return terminate ? false : recurse;
    }

    public Tree<XmlLite.Data> getCurInterpNode() {
      return curInterpNode;
    }

    public void setCurInterpNode(Tree<XmlLite.Data> curInterpNode) {
      this.curInterpNode = curInterpNode;
    }

    public Tree<XmlLite.Data> getFinalInterpNode() {
      Tree<XmlLite.Data> result = curInterpNode;

      if (!isTag && !terminate) {
        final CategorizedToken cToken = ParseInterpretationUtil.getCategorizedToken(parseTree);

        String text = null;
        if (cToken != null) {
          text = cToken.token.getTextWithDelims();
        }
        else {
          text = parseTree.getData();
        }

        result = XmlLite.createTextNode(text);
      }

      return result;
    }

    public boolean hasParseInterp() {
      return parseInterp != null;
    }

    public ParseInterpretation getParseInterp() {
      return parseInterp;
    }

    public boolean isAmbiguous() {
      return ambiguous;
    }

    public int getDepth() {
      if (_depth == null) {
        _depth = parseTree.depth();
      }
      return _depth;
    }
  }
}
