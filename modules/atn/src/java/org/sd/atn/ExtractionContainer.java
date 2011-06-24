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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.atn.extract.Extraction;
import org.sd.cio.MessageHelper;
import org.sd.io.PersistablePublishable;
import org.sd.util.AlignmentVector;
import org.sd.util.InputContext;
import org.sd.util.tree.Tree;
import org.sd.xml.DomContext;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlLite;

/**
 * A container for an extraction with its structural and semantic ambiguities.
 * <p>
 * An ExtractionContainer contains one or more ExtractionContainer.ExtractionData
 * instances, each of which contains an Extraction with its InputContext and
 * ParseInterpretations.
 * <p>
 * Multiple ExtractionContainer.ExtractionData instances marks structural
 * ambiguity. Multiple ParseInterpretations represent semantic ambiguity.
 *
 * @author Spence Koehler
 */
public class ExtractionContainer extends PersistablePublishable implements Comparable<ExtractionContainer> {

  /**
   * Factory method to create and return a non-empty extraction container.
   */
  public static final ExtractionContainer createExtractionContainer(AtnParseResult parseResult) {
    final ExtractionContainer result = new ExtractionContainer(parseResult);
    return result.isEmpty() ? null : result;
  }


  //
  //NOTE: If member variables are changed here or in subclasses, update
  //      CURRENT_VERSION and keep track of deserializing old persisted
  //      instances!
  //
  private static final int CURRENT_VERSION = 1;


  
  private List<ExtractionData> extractions;
  private InputContextContainer inputContext;
  private ParseInterpretation _theInterpretation;
  private boolean setTheInterpretation;
  private ExtractionData _theExtraction;
  private boolean setTheExtraction;

  private int localStartPosition;
  private int globalStartPosition;
  private String localText;  // text of longest parsed text

  private Set<ParseInterpretation> _allInterpretations;
  private Set<String> _parsedTexts;
  private Set<String> _extractionTypes;
  private Set<String> _interpretationClassifications;

  // terminal (path reconstruction tree) node for this extraction container
  private transient Tree<XmlLite.Data> terminalNode;

  /**
   * Empty constructor for publishable reconstruction.
   */
  public ExtractionContainer() {
  }

  /**
   * Construct a potentially empty container from the given parse result.
   */
  public ExtractionContainer(AtnParseResult parseResult) {
    this.extractions = new ArrayList<ExtractionData>();
    this.inputContext = new InputContextContainer(parseResult.getInputContext());
    this._theInterpretation = null;
    this.setTheInterpretation = false;
    this._theExtraction = null;
    this.setTheExtraction = false;

    this.localStartPosition = parseResult.getFirstToken().getStartIndex();

    final int[] startPosition = new int[]{0};
    final InputContext ic = inputContext.getInputContext();
    final InputContext rootContext = ic.getContextRoot();
    if (rootContext != null) rootContext.getPosition(ic, startPosition);
    this.globalStartPosition = startPosition[0] + localStartPosition;


    final int numParses = parseResult.getNumParses();
    for (int i = 0; i < numParses; ++i) {
      final AtnParse parse = parseResult.getParse(i);

      if (!parse.getSelected()) continue;

      final List<ParseInterpretation> interpretations = parse.getParseInterpretations();
      if (interpretations == null || interpretations.size() == 0) continue;

      // only add selected extractions with interpretations
      extractions.add(new ExtractionData(inputContext, parse, interpretations));

      final String curParsedText = parse.getParsedText();
      if (localText == null || localText.length() < curParsedText.length()) {
        localText = curParsedText;
      }
    }
  }

  /**
   * Container is empty if it has no selected extractions with an interpretation.
   */
  public boolean isEmpty() {
    return extractions.size() == 0;
  }

  /**
   * Get this extraction's key.
   */
  public String getKey() {
    return inputContext.getKey();
  }

  /**
   * Get this extraction's text's local start position.
   */
  public int getLocalStartPosition() {
    return localStartPosition;
  }

  /**
   * Get this extraction's text's global start position.
   */
  public int getGlobalStartPosition() {
    return globalStartPosition;
  }

  /**
   * Get this extraction's (longest) local (parsed) text.
   */
  public String getLocalText() {
    return localText;
  }
  

  /**
   * Get all of this extraction's parses.
   */
  public List<ExtractionData> getAllExtractions() {
    return extractions;
  }

  /**
   * Get "approved" extractions.
   * <p>
   * If 'theExtraction' is set, then it will be returned as a single element
   * in the result; otherwise, all extractions will be returned.
   */
  public List<ExtractionData> getExtractions() {
    List<ExtractionData> result = null;

    if (_theExtraction != null) {
      result = new ArrayList<ExtractionData>();
      result.add(_theExtraction);
    }
    else {
      result = extractions;
    }

    return result;
  }

  /**
   * Get this extracton's input context container.
   */
  public InputContextContainer getInputContextContainer() {
    return inputContext;
  }

  /**
   * Set the terminal (path reconstruction tree) node for this extraction
   * container.
   */
  public void setTerminalNode(Tree<XmlLite.Data> terminalNode) {
    this.terminalNode = terminalNode;
  }

  /**
   * Get this instance's terminal (path reconstruction tree) node or null.
   */
  public Tree<XmlLite.Data> getTerminalNode() {
    return terminalNode;
  }

  /**
   * Remove an extraction, presumably because the structural ambiguity
   * has been resolved enough to disqualify the extraction. Note that
   * this will have no effect on "the" interpretation if it has been set.
   */
  public void removeExtraction(ExtractionData extractionData) {
    if (!setTheInterpretation) _theInterpretation = null;
    if (setTheExtraction && extractionData.equals(_theExtraction)) _theExtraction = null;
    extractions.remove(extractionData);
  }

  /**
   * Set the extraction as the definitive extraction for this container,
   * presumably because the structural ambiguity has been resolved.
   * Note that this will have no effect on "the" interpretation if it has
   * been set.
   */
  public void setTheExtraction(ExtractionData extractionData) {
    if (!setTheInterpretation) _theInterpretation = null;
    _theExtraction = extractionData;
    setTheExtraction = true;
    this.extractions.clear();
    this.extractions.add(extractionData);
  }

  /**
   * There is structural ambiguity if there are multiple extractions.
   */
  public boolean hasStructuralAmbiguity() {
    return extractions.size() > 1;
  }

  /**
   * There is semantic ambiguity if there is more than one interpretation.
   */
  public boolean hasSemanticAmbiguity() {
    return getTheInterpretation() == null;
  }


  /**
   * Get the 'definitive' extraction or null.
   */
  public ExtractionData getTheExtraction() {
    return _theExtraction;
  }

  /**
   * Get the single interpretation for this extraction if it exists; otherwise,
   * null.
   */
  public ParseInterpretation getTheInterpretation() {
    if (_theInterpretation == null) {
      final Set<ParseInterpretation> allInterpretations = getAllInterpretations();
      if (allInterpretations.size() == 1) {
        _theInterpretation = allInterpretations.iterator().next();
        _theExtraction = extractions.get(0);
      }
    }
    return _theInterpretation;
  }

  /**
   * Set or override the interpretation for this extraction.
   * <p>
   * If set to null, then allow for automatic interpretation selection.
   */
  public void setTheInterpretation(ParseInterpretation theInterpretation) {
    this._theInterpretation = theInterpretation;

    if (theInterpretation != null) {
      this.setTheInterpretation = true;

      for (ExtractionData extraction : extractions) {
        if (extraction.interpretations.contains(theInterpretation)) {
          this._theExtraction = extraction;
          break;
        }
      }
    }
    else {
      this.setTheInterpretation = false;
      this._theExtraction = null;
      this.setTheExtraction = false;
    }
  }

  /**
   * Get all unique parsed texts for this extraction.
   */
  public Set<String> getParsedTexts() {
    if (_parsedTexts == null) {
      _parsedTexts = new HashSet<String>();

      for (ExtractionData extraction : extractions) {
        _parsedTexts.add(extraction.parsedText);
      }
    }

    return _parsedTexts;
  }

  public Set<String> getExtractionTypes() {
    if (_extractionTypes == null) {
      _extractionTypes = new HashSet<String>();

      for (ExtractionData extraction : extractions) {
        if (extraction.extraction != null) {
          _extractionTypes.add(extraction.extraction.getType());
        }
      }
    }
    return _extractionTypes;
  }

  public Set<String> getInterpretationClassifications() {
    if (_interpretationClassifications == null) {
      _interpretationClassifications = new HashSet<String>();

      for (ExtractionData extraction : extractions) {
        if (extraction.interpretations != null) {
          for (ParseInterpretation interpretation : extraction.interpretations) {
            _interpretationClassifications.add(interpretation.getClassification());
          }
        }
      }
    }
    return _interpretationClassifications;
  }

  public int getNumStructuralParses() {
    return extractions.size();
  }

  public Set<ParseInterpretation> getParseInterpretations() {
    Set<ParseInterpretation> result = null;

    final ParseInterpretation theInterpretation = getTheInterpretation();
    if (theInterpretation != null) {
      result = new HashSet<ParseInterpretation>();
      result.add(theInterpretation);
    }
    else {
      result = getAllInterpretations();
    }

    return result;
  }

  public int getNumInterpretations() {
    return getParseInterpretations().size();
  }

  /**
   * Compare this extraction container to another under the same root context.
   * <p>
   * Note that comparisons across contexts will all appear equal.
   */
  public int compareTo(ExtractionContainer other) {
    return this.globalStartPosition - other.globalStartPosition;
  }

  private final Set<ParseInterpretation> getAllInterpretations() {
    if (_allInterpretations == null) {
      _allInterpretations = new HashSet<ParseInterpretation>();
      for (ExtractionData eData : extractions) {
        for (ParseInterpretation parseInterpretation : eData.interpretations) {
          _allInterpretations.add(parseInterpretation);
        }
      }
    }
    return _allInterpretations;
  }


  ////////
  //
  // PersistablePublishable interface implementation

  /**
   * Get the current version.
   * <p>
   * Note that changes to subclasses as well as to this class will require
   * this value to change and proper handling in the write/read methods.
   */
  protected final int getCurrentVersion() {
    return CURRENT_VERSION;
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  protected void writeCurrentVersion(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(extractions.size());
    for (ExtractionData extractionData : extractions) {
      MessageHelper.writePublishable(dataOutput, extractionData);
    }
    MessageHelper.writePublishable(dataOutput, inputContext);
    MessageHelper.writeSerializable(dataOutput, _theInterpretation);
    MessageHelper.writePublishable(dataOutput, _theExtraction);
    dataOutput.writeInt(globalStartPosition);
    dataOutput.writeInt(localStartPosition);
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
  protected void readVersion(int version, DataInput dataInput) throws IOException {
    if (version == 1) {
      readVersion1(dataInput);
    }
    else {
      badVersion(version);
    }
  }

  private final void readVersion1(DataInput dataInput) throws IOException {
    this.localText = null;
    final int numExtractionDatas = dataInput.readInt();
    this.extractions = new ArrayList<ExtractionData>();
    for (int i = 0; i < numExtractionDatas; ++i) {
      final ExtractionData ed = (ExtractionData)MessageHelper.readPublishable(dataInput);
      extractions.add(ed);

      if (localText == null || localText.length() < ed.getParsedText().length()) {
        localText = ed.getParsedText();
      }
    }
    this.inputContext = (InputContextContainer)MessageHelper.readPublishable(dataInput);
    this._theInterpretation = (ParseInterpretation)MessageHelper.readSerializable(dataInput);
    this.setTheInterpretation = _theInterpretation != null;
    this._theExtraction = (ExtractionData)MessageHelper.readPublishable(dataInput);
    this.setTheExtraction = _theExtraction != null;
    this.globalStartPosition = dataInput.readInt();
    this.localStartPosition = dataInput.readInt();
  }

  //
  // end of PersistablePublishable interface implementation
  //
  ////////


  /**
   * Container for an extraction with its context and interpretations.
   */
  public static final class ExtractionData extends PersistablePublishable implements Comparable<ExtractionData> {

    //
    //NOTE: If member variables are changed here or in subclasses, update
    //      CURRENT_VERSION and keep track of deserializing old persisted
    //      instances!
    //
    private static final int CURRENT_VERSION = 1;


    private InputContextContainer context;
    private String parsedText;
    private int startPos;
    private int endPos;
    private int length;
    private Extraction extraction;
    private Tree<String> parseTree;
    private List<ParseInterpretation> interpretations;
    private int parseNum;

    private AlignmentVector _alignmentVector;

    /**
     * Empty constructor for publishable reconstruction.
     */
    public ExtractionData() {
    }

    public ExtractionData(InputContextContainer context, AtnParse parse, List<ParseInterpretation> interpretations) {
      this.context = context;
      this.parsedText = parse.getParsedText();
      this.startPos = parse.getStartIndex();
      this.endPos = parse.getEndIndex();
      this.length = parse.getFullTextLength();
      this.extraction = null;
      try {
        this.extraction = parse.getExtraction();
      }
      catch (Exception e) {
        System.err.println("***NOTE: Unable to build 'Extraction' instance for parse. (ExtractionContainer)\n\t" +
                           e.toString());
        //e.printStackTrace(System.err);
      }
      this.parseTree = parse.getParseTree();
      this.interpretations = interpretations;
      this.parseNum = parse.getParseNum();
    }

    public InputContextContainer getContext() {
      return context;
    }

    public String getParsedText() {
      return parsedText;
    }

    public int getStartPos() {
      return startPos;
    }

    public int getEndPos() {
      return endPos;
    }

    public int getLength() {
      return length;
    }

    public Extraction getExtraction() {
      return extraction;
    }

    public Tree<String> getParseTree() {
      return parseTree;
    }

    public List<ParseInterpretation> getInterpretations() {
      return interpretations;
    }

    public int getParseNum() {
      return parseNum;
    }

    /**
     * Get this instance's alignment vector.
     * <p>
     * Alignment is based on:
     * <ul>
     * <li>0: context key</li>
     * <li>1: startPos</li>
     * <li>2: parsed text</li>
     * <li>3: extraction type</li>
     * </ul>
     */
    public AlignmentVector getAlignmentVector() {
      if (_alignmentVector == null) {
        _alignmentVector = new AlignmentVector();

        _alignmentVector.add(context.getKey());
        _alignmentVector.add(startPos);
        _alignmentVector.add(parsedText);
        _alignmentVector.add(extraction.getType());
      }
      return _alignmentVector;
    }

    /**
     * Get the match between this and the other extraction data as a mask with bits:
     * <ul>
     * <li>0: context key matches</li>
     * <li>1: startPos matches</li>
     * <li>2: parsed text matches</li>
     * <li>3: extraction type matches</li>
     * </ul>
     */
    public BitSet getMatchMask(ExtractionData other) {
      return this.getAlignmentVector().alignWith(other.getAlignmentVector());
    }

    /**
     * Compare this instance to another based on their alignment vectors.
     */
    public int compareTo(ExtractionData other) {
      return this.getAlignmentVector().compareTo(other.getAlignmentVector());
    }


    ////////
    //
    // PersistablePublishable interface implementation

    /**
     * Get the current version.
     * <p>
     * Note that changes to subclasses as well as to this class will require
     * this value to change and proper handling in the write/read methods.
     */
    protected final int getCurrentVersion() {
      return CURRENT_VERSION;
    }

    /**
     * Write this message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    protected void writeCurrentVersion(DataOutput dataOutput) throws IOException {
      MessageHelper.writePublishable(dataOutput, context);
      MessageHelper.writeString(dataOutput, parsedText);
      dataOutput.writeInt(startPos);
      dataOutput.writeInt(endPos);
      dataOutput.writeInt(length);
      MessageHelper.writePublishable(dataOutput, extraction);
      MessageHelper.writeStringTree(dataOutput, parseTree);
      dataOutput.writeInt(interpretations.size());
      for (ParseInterpretation interpretation : interpretations) {
        MessageHelper.writeSerializable(dataOutput, interpretation);
      }
      dataOutput.writeInt(parseNum);
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
    protected void readVersion(int version, DataInput dataInput) throws IOException {
      if (version == 1) {
        readVersion1(dataInput);
      }
      else {
        badVersion(version);
      }
    }

    private final void readVersion1(DataInput dataInput) throws IOException {
      this.context = (InputContextContainer)MessageHelper.readPublishable(dataInput);
      this.parsedText = MessageHelper.readString(dataInput);
      this.startPos = dataInput.readInt();
      this.endPos = dataInput.readInt();
      this.length = dataInput.readInt();
      this.extraction = (Extraction)MessageHelper.readPublishable(dataInput);
      this.parseTree = MessageHelper.readStringTree(dataInput);
      final int numInterpretations = dataInput.readInt();
      this.interpretations = new ArrayList<ParseInterpretation>();
      for (int i = 0; i < numInterpretations; ++i) {
        interpretations.add((ParseInterpretation)MessageHelper.readSerializable(dataInput));
      }
      this.parseNum = dataInput.readInt();
    }

    //
    // end of PersistablePublishable interface implementation
    //
    ////////
  }
}
