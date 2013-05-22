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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.sd.atn.extract.Extraction;
import org.sd.atn.extract.ExtractionFactory;
import org.sd.token.Tokenizer;
import org.sd.util.InputContext;
import org.sd.util.InputContextComparison;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TraversalIterator;
import org.sd.xml.DataProperties;
import org.sd.xml.DomContext;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlLite;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class ParseOutputCollector {
  
  private LinkedHashSet<DomDocument> domDocuments;
  public LinkedHashSet<DomDocument> getDomDocuments() {
    return domDocuments;
  }

  private List<AtnParseResult> parseResults;
  public List<AtnParseResult> getParseResults() {
    return parseResults;
  }
  public boolean hasParseResults() {
    return parseResults != null && parseResults.size() > 0;
  }

  private LinkedList<ParseInfo> topParseInfos;

  private String defaultStyle;
  private String interpretedStyle;
  private boolean hideUninterpreted;
  private InputContext inputContext;
  private Tokenizer outputTokenizer;

  private ParseSourceInfo parseSourceInfo;

  public ParseOutputCollector(DomElement outputsElement) {
    this.domDocuments = null;
    this.parseResults = null;
    this.topParseInfos = new LinkedList<ParseInfo>();
    this.inputContext = null;
    this.outputTokenizer = null;

    final DataProperties config = outputsElement == null ? new DataProperties() : new DataProperties(outputsElement);
    init(config);
  }

  public ParseOutputCollector(DataProperties config) {
    init(config);
  }

  private final void init(DataProperties config) {

    // expected format:
    //
    // <outputs>
    //   <markup>
    //     <style>border: 3px solid; border-color: green; background-color: yellow;</style>
    //     <style type='interpreted'>border: 3px solid; border-color: blue; background-color: green;</style>
    //     <hideUninterpreted>false</hideUninterpreted>
    //   </markup>
    //   <stdout/>
    //   <file type='dom'>path-to-dom-file.dom</file>
    // </outputs>

    final DomElement domElement = config.getDomElement();

    if (domElement != null) {
      final NodeList styleNodes = domElement.selectNodes("markup/style");
      if (styleNodes != null) {
        for (int i = 0; i < styleNodes.getLength(); ++i) {
          final Node styleNode = styleNodes.item(i);
          if (styleNode.getNodeType() != DomElement.ELEMENT_NODE) continue;
          final DomElement styleElement = (DomElement)styleNode;

          final String style = styleElement.getTextContent();
          final String styleType = styleElement.getAttributeValue("type", null);
          if (styleType != null && "interpreted".equalsIgnoreCase(styleType)) {
            this.interpretedStyle = style;
          }
          else {
            this.defaultStyle = style;
            if (this.interpretedStyle == null) this.interpretedStyle = style;
          }
        }
      }
    }

    this.hideUninterpreted = config.getBoolean("markup/hideUninterpreted", false);
  }


  /**
   * Determine whether the input context is present.
   */
  public boolean hasInputContext() {
    return inputContext != null;
  }

  /**
   * Set the context used as input for this instance.
   */
  public void setInputContext(InputContext inputContext) {
    this.inputContext = inputContext;
  }

  /**
   * Get the context used as input for this instance.
   */
  public InputContext getInputContext() {
    return inputContext;
  }

  /**
   * Determine whether the output tokenizer is present.
   */
  public boolean hasOutputTokenizer() {
    return outputTokenizer != null;
  }

  /**
   * Set the tokenizer used as output for this instance.
   */
  public void setOutputTokenizer(Tokenizer outputTokenizer) {
    this.outputTokenizer = outputTokenizer;
  }

  /**
   * Get the tokenizer used as output for this instance.
   */
  public Tokenizer getOutputTokenizer() {
    return outputTokenizer;
  }

  /**
   * Get source information, empty if unset.
   */
  public ParseSourceInfo getParseSourceInfo() {
    if (parseSourceInfo == null) {
      parseSourceInfo = new ParseSourceInfo();
    }
    return parseSourceInfo;
  }

  /**
   * Set source information.
   */
  public void setParseSourceInfo(ParseSourceInfo parseSourceInfo) {
    this.parseSourceInfo = parseSourceInfo;
  }



  public String asXml(boolean includeXmlHeader, boolean onlySelected, boolean onlyInterpreted, int indentLevel, int indentSpaces) {
    final XmlParseOutput xmlOutput = new XmlParseOutput(parseSourceInfo, indentLevel, indentSpaces);

    if (parseResults != null) {
      for (AtnParseResult parseResult : parseResults) {
        xmlOutput.addParseResult(parseResult, onlySelected, onlyInterpreted);
      }
    }

    return xmlOutput.getOutput(includeXmlHeader);
  }


  /**
   * Get the first dom document (result) if it exists or null.
   */
  public DomDocument getFirstDomDocument() {
    return getFirstDomDocument(false);
  }

  public DomDocument getFirstDomDocument(boolean prune) {
    DomDocument result = null;

    if (domDocuments != null && domDocuments.size() > 0) {
      result = domDocuments.iterator().next();

      if (prune) {
        pruneNodes(result);
      }
    }

    return result;
  }

  public static final void pruneNodes(DomDocument domDocument) {
    final Tree<XmlLite.Data> tree = domDocument.getDocumentDomElement().asTree();
    for (TraversalIterator<XmlLite.Data> it = tree.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = it.next();
      final XmlLite.Tag tag = curNode.getData().asTag();
      if (tag != null) {
        boolean removeTag = ("iframe".equals(tag.name) || "script".equals(tag.name));

        if (!removeTag && !curNode.hasChildren() && "style".equals(tag.name) && tag.attributes.size() == 0) {
          removeTag = true;
        }

        if (removeTag) {
          it.remove();
          it.skip();
        }
      }
    }
  }

  public void showParseResults(boolean onlyInterpreted) {
    final List<AtnParse> interpretedParses = getTopInterpretedParses(onlyInterpreted);

    if (interpretedParses.size() == 0) {
      System.out.println("No Interpreted Parses.");
    }
    else {
      System.out.println(interpretedParses.size() + " Interpreted Parses:");

      int parseNum = 1;
      for (AtnParse parse : interpretedParses) {
        final List<ParseInterpretation> interpretations = parse.getParseInterpretations();
        final Extraction extraction = parse.getExtraction();

        System.out.println(" IParse #" + parseNum + " has " + interpretations.size() + " interpretations.");
        System.out.println("        Text: " + parse.getParsedText());
        System.out.println("     Context: " + parse.getInputContext().toString());
        System.out.println("   ParseTree: " + parse.getParseTree().toString());
        System.out.println("  Extraction:\n" + (extraction == null ? "<NONE>" : extraction.toString()));

        int interpNum = 1;
        for (ParseInterpretation interpretation : interpretations) {
          final String interpXml = interpretation.getInterpXml();

          System.out.println("  Interpretation #" + interpNum + " = " + interpretation.toString());
          if (interpXml != null) System.out.println("\tinterpXml=" + interpXml);
          ++interpNum;
        }

        System.out.println();

        ++parseNum;
      }
    }
  }

  /**
   * Add the parse result.
   */
  public void add(AtnParseResult parseResult) {
    addParseResult(parseResult);

    boolean addedDomDocument = false;
    for (int parseNum = 0; parseNum < parseResult.getNumParses(); ++parseNum) {
      final AtnParse parse = parseResult.getParse(parseNum);

      final ParseInfo parseInfo = new ParseInfo(parse, hideUninterpreted, defaultStyle, interpretedStyle);
      addParseInfo(parseInfo);

      if (!addedDomDocument) {
        // all parses in a result will have the same owner document
        addedDomDocument = addDomDocument(parseInfo);
      }
    }
  }

  public List<AtnParse> getTopInterpretedParses(boolean onlyInterpreted) {
    final List<AtnParse> interpretedParses = new ArrayList<AtnParse>();
    final Map<DomNode, MarkupContainer> textNode2Markup = new HashMap<DomNode, MarkupContainer>();

    // call parseInfo.AddMarkup() on final parseInfo's
    for (ParseInfo parseInfo : topParseInfos) {
      if (onlyInterpreted && !parseInfo.wasInterpreted()) continue;

      if (parseInfo.addMarkup(textNode2Markup)) {
        parseInfo.setHasMarkup(true);
      }

      interpretedParses.add(parseInfo.getParse());
    }

    for (MarkupContainer markup : textNode2Markup.values()) {
      markup.applyMarkup();
    }

    return interpretedParses;
  }

  public List<ParseInterpretation> getParseInterpretations() {
    return getParseInterpretations((ParseInterpretationSelector)null);
  }

  public List<ParseInterpretation> getParseInterpretations(final String classification) {
    return getParseInterpretations(new ParseInterpretationSelector() {
        public boolean select(ParseInterpretation interp) {
          return classification.equals(interp.getClassification());
        }
      });
  }

  public List<Parse> getParses() {
    final List<Parse> result = new ArrayList<Parse>();

    if (parseResults != null) {
      for (AtnParseResult parseResult : parseResults) {
        final int numParses = parseResult.getNumParses();
        for (int parseNum = 0; parseNum < numParses; ++parseNum) {
          final AtnParse parse = parseResult.getParse(parseNum);
          if (parse.getSelected()) {
            result.add(parse.getParse());
          }
        }
      }
    }

    return result;
  }

  private List<ParseInterpretation> getParseInterpretations(ParseInterpretationSelector selector) {
    final List<ParseInterpretation> result = new ArrayList<ParseInterpretation>();

    if (parseResults != null) {
      for (AtnParseResult parseResult : parseResults) {
        final int numParses = parseResult.getNumParses();
        for (int parseNum = 0; parseNum < numParses; ++parseNum) {
          final AtnParse parse = parseResult.getParse(parseNum);
          if (parse.getSelected()) {
            final List<ParseInterpretation> interps = parse.getParseInterpretations();
            if (interps != null && interps.size() > 0) {
              for (ParseInterpretation interp : interps) {
                if (selector == null || selector.select(interp)) {
                  result.add(interp);
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Collect all selected parse interpretations from all parse results.
   */
  public List<ParseInterpretation> collectSelectedInterps() {
    final List<ParseInterpretation> result = new ArrayList<ParseInterpretation>();

    if (parseResults != null) {
      for (AtnParseResult parseResult : parseResults) {
        result.addAll(parseResult.getSelectedInterps());
      }
    }

    return result;
  }


  private void addParseResult(AtnParseResult parseResult) {
    if (parseResult != null) {
      if (parseResults == null) parseResults = new ArrayList<AtnParseResult>();
      parseResults.add(parseResult);
    }
  }

  private void addParseInfo(ParseInfo parseInfo) {
    boolean isDuplicate = false;

    // remove parseInfos that are encompassed by this parseInfo
    for (Iterator<ParseInfo> iter = topParseInfos.iterator(); iter.hasNext(); ) {
      final ParseInfo curParseInfo = iter.next();

      if (parseInfo.isDuplicate(curParseInfo)) {
        isDuplicate = true;
        break;
      }
      else {
        if (parseInfo.encompasses(curParseInfo)) {
          // new parse encompasses an existing, so get rid of existing in favor of new
          iter.remove();
        }
        else if (curParseInfo.encompasses(parseInfo)) {
          // old parse encompasses the new, so mark the new as a subset of the old
          parseInfo.getParse().addEncompassingParse(curParseInfo.getParse());
        }
      }
    }

    // add the parseInfo
    if (!isDuplicate) topParseInfos.addLast(parseInfo);
  }

  /**
   * Add the parse's document if available.
   */
  private boolean addDomDocument(ParseInfo parseInfo) {
    boolean result = false;

    final InputContext ic = parseInfo.getInputContext();
    if (ic != null && ic instanceof DomContext) {
      final DomContext dc = (DomContext)ic;
      final DomNode domNode = dc.getDomNode();

      if (domNode != null) {
        if (domDocuments == null) domDocuments = new LinkedHashSet<DomDocument>();
        domDocuments.add((DomDocument)domNode.getOwnerDocument());
        result = true;
      }
    }

    return result;
  }


  private class MarkupContainer {
    private DomNode textNode;
    private String interpretation;
    private String style;
    private int iterationId;
    private boolean wasInterpreted;

    MarkupContainer(DomNode textNode, String interpretation, String style, int iterationId, boolean wasInterpreted) {
      this.textNode = textNode;
      this.interpretation = interpretation;
      this.style = style;
      this.iterationId = iterationId;
      this.wasInterpreted = wasInterpreted;
    }

    void update(String nextInterpretation, boolean nextWasInterpreted, String nextStyle) {
      if (nextWasInterpreted && !this.wasInterpreted) {
        this.interpretation = nextInterpretation;
        this.wasInterpreted = nextWasInterpreted;
        this.style = nextStyle;
      }
      else if (nextWasInterpreted == this.wasInterpreted) {
        if (nextInterpretation != null && interpretation != null && nextInterpretation.length() > interpretation.length()) {
          this.interpretation = nextInterpretation;
        }
      }
    }

    void applyMarkup() {
      if (interpretation == null) return;

      final DomElement newChild = DomUtil.createElement(textNode, "div", false);

      // add style
      if (style != null && !"".equals(style)) DomUtil.addAttribute(newChild, "style", style);

      interpretation = "(" + iterationId + ")" + ": " + interpretation;

      // add hover
      final DomElement hoverChild = DomUtil.createElement(newChild, "a", true);
      DomUtil.addAttribute(hoverChild, "href", "#");
      DomUtil.addAttribute(hoverChild, "title", interpretation);
      DomUtil.addAttribute(hoverChild, "onclick", "return false");

      textNode.getParentNode().replaceChild(newChild, textNode);
      hoverChild.appendChild(textNode);
    }
  }


  private class ParseInfo {
    private boolean hasMarkup;
    boolean hasMarkup() {
      return hasMarkup;
    }
    void setHasMarkup(boolean hasMarkup) {
      this.hasMarkup = hasMarkup;
    }

    private AtnParse parse;
    AtnParse getParse() {
      return parse;
    }

    private boolean hideUninterpreted;
    boolean hideUninterpreted() {
      return hideUninterpreted;
    }

    private String defaultStyle;
    String getDefaultStyle() {
      return defaultStyle;
    }

    private String interpretedStyle;
    String getInterpretedStyle() {
      return interpretedStyle;
    }

    private boolean wasInterpreted;
    boolean wasInterpreted() {
      return wasInterpreted;
    }

    private String _interpretation;
    String getInterpretation() {
      if (_interpretation == null && parse != null && parse.getSelected()) {
        _interpretation = getInterpretationString(parse);
      }
      return _interpretation;
    }

    public InputContext getInputContext() {
      return parse.getInputContext();
    }

    private DomNode _textNode;
    DomNode getTextNode() {
      if (_textNode == null) {
        final InputContext ic = getInputContext();
        if (ic != null && ic instanceof DomContext) {
          final DomContext dc = (DomContext)ic;
          final DomNode parseNode = dc.getDomNode();

          if (parseNode != null) {
            int startPos = parse.getFirstCategorizedToken().token.getStartIndex();
            int endPos = parse.getLastCategorizedToken().token.getEndIndex();
            this._textNode = DomUtil.getDeepestNode(parseNode, startPos, endPos);
          }
        }
      }
      return _textNode;
    }

    boolean hasInterpretation() {
      return getInterpretation() != null;
    }

    ParseInfo(AtnParse parse, boolean hideUninterpreted, String defaultStyle, String interpretedStyle) {
      this.parse = parse;
      this.hideUninterpreted = hideUninterpreted;
      this.defaultStyle = defaultStyle;
      this.interpretedStyle = interpretedStyle;

      this.wasInterpreted = false;
      this._interpretation = null;
    }


    boolean isDuplicate(ParseInfo other) {
      final Tree<String> myParseTree = parse.getParseTree();
      final Tree<String> otherParseTree = other.getParse().getParseTree();
      return myParseTree.equals(otherParseTree);
    }

    boolean encompasses(ParseInfo other) {
      boolean result = false;

      final ExtractionFactory extractionFactory = parse.getExtractionFactory();
      if (extractionFactory != null && extractionFactory == other.getParse().getExtractionFactory()) {
        final Extraction thisExtraction = parse.getExtraction();
        final Extraction otherExtraction = other.getParse().getExtraction();

        if (thisExtraction != null && otherExtraction != null) {
          final int[] thisStartPos = new int[]{0};
          final int[] thisEndPos = new int[]{0};
          final InputContext thisInputContext = extractionFactory.getInputContext(thisExtraction, thisStartPos, thisEndPos);

          final int[] otherStartPos = new int[]{0};
          final int[] otherEndPos = new int[]{0};
          final InputContext otherInputContext = extractionFactory.getInputContext(otherExtraction, otherStartPos, otherEndPos);

          if (thisInputContext != null && otherInputContext != null) {
            final InputContextComparison icc = new InputContextComparison(thisInputContext, thisStartPos[0], thisEndPos[0],
                                                                          otherInputContext, otherStartPos[0], otherEndPos[0]);
            result = icc.isComparable() && icc.encompasses();
          }
        }
      }

      return result;
    }

    boolean addMarkup(Map<DomNode, MarkupContainer> textNode2Markup) {
      boolean result = false;

      final String interpretation = getInterpretation();

      final DomNode textNode = getTextNode();
      if (this.wasInterpreted() && textNode != null && (defaultStyle != null || interpretedStyle != null)) {
        final String style = wasInterpreted() ? interpretedStyle : defaultStyle;

        // for now, just use the given node even though this will likely hold more nodes and text than just the parse's text
        if (textNode.getParentNode() != null) {
          result = true;

          MarkupContainer markup = textNode2Markup.get(textNode);
          if (markup != null) {
            markup.update(interpretation, wasInterpreted(), style);
          }
          else {
            
            markup = new MarkupContainer(textNode, interpretation, style, parse.getInputContext().getId(), wasInterpreted());
            textNode2Markup.put(textNode, markup);
          }
        }
      }

      return result;
    }

    private String getInterpretationString(AtnParse parse) {
      String result = null;

      final List<ParseInterpretation> interpretations = parse.getParseInterpretations();
      if (interpretations != null && interpretations.size() > 0) {
        this.wasInterpreted = parse.getSelected();
        result = interpretations.get(0).toString();
        //todo: get a String representation of multiple interpretations if they exist.
      }


      if (result == null && !hideUninterpreted) {
        result = parse.getParseTree().toString();
      }

      return result;
    }
  }
}
