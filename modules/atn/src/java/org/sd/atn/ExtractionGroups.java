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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.cio.MessageHelper;
import org.sd.io.PersistablePublishable;
import org.sd.util.MathUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlReconstructor;

/**
 * Container and organizer for related ExtractionContainer instances (e.g. all
 * from the same underlying source page.)
 * <p>
 * @author Spence Koehler
 */
public class ExtractionGroups extends PersistablePublishable {
  
  //
  //NOTE: If member variables are changed here or in subclasses, update
  //      CURRENT_VERSION and keep track of deserializing old persisted
  //      instances!
  //
  private static final int CURRENT_VERSION = 1;


  /**
   * Interface for visiting each extraction in the group.
   */
  public interface ExtractionVisitor {
    /**
     * Visit a group on the way to visiting its interpretations
     * 
     * @return true to continue on to the interpretations; otherwise, false.
     */
    public boolean visitExtractionGroup(String source, int groupNum, String groupKey, ExtractionGroup group);

    /** Visit a specific interpretation within an extraction */
    public void visitInterpretation(String source, int groupNum, String groupKey, ExtractionContainer.ExtractionData theExtraction,
                                    int interpNum, ParseInterpretation theInterpretation, String extractionKey);
  }


  // all extractions in document order.
  private List<ExtractionContainer> extractions;

  // all extractions mapped (in document order) by key
  private Map<String, List<ExtractionContainer>> _key2extractions;

  // reconstructor for the tree of all extraction keys
  private XmlReconstructor _xmlReconstructor;

  // all partitioned groups of extractions
  private List<ExtractionGroup> _groups;


  /**
   * Empty constructor for publishable reconstruction.
   */
  public ExtractionGroups() {
  }

  /**
   * Construct with the parses in the given parse output collector.
   */
  public ExtractionGroups(ParseOutputCollector output) {
    this.extractions = loadExtractions(output);
    this._key2extractions = null;
    this._xmlReconstructor = null;
    this._groups = null;
  }

  /**
   * Return all extractions in order.
   */
  public List<ExtractionContainer> getExtractions() {
    return extractions;
  }

  /**
   * Get all partitioned groups of extractions.
   */
  public List<ExtractionGroup> getExtractionGroups() {
    if (_groups == null) {
      _groups = partitionExtractions();
    }
    return _groups;
  }

  /**
   * Get the the number of extraction groups.
   */
  public int getNumGroups() {
    final List<ExtractionGroup> groups = getExtractionGroups();
    return groups.size();
  }

  /**
   * Collect the unique, non-overlapping input nodes across all groups.
   */
  public List<DomNode> getInputNodes() {
    final LinkedList<DomNode> result = new LinkedList<DomNode>();

    for (ExtractionGroup extractionGroup : getExtractionGroups()) {
      final DomNode groupInputNode = extractionGroup.getInputNode();
      if (groupInputNode != null) {
        boolean keeper = true;
        for (Iterator<DomNode> iter = result.iterator(); iter.hasNext(); ) {
          final DomNode collectedNode = iter.next();
          if (groupInputNode == collectedNode) {
            keeper = false;
            break;
          }
          else if (groupInputNode.isAncestor(collectedNode, false)) {
            keeper = true;
            iter.remove();
          }
          else if (collectedNode.isAncestor(groupInputNode, false)) {
            keeper = false;
            break;
          }
        }

        if (keeper) result.add(groupInputNode);
      }
    }

    return result;
  }

  /**
   * Get all extractions mapped (in document order) by key.
   */
  public Map<String, List<ExtractionContainer>> getKey2Extractions() {
    if (_key2extractions == null) {
      createKey2Extractions();
    }
    return _key2extractions;
  }

  /**
   * Get the xmlReconstructor for all extractions' paths.
   */
  public XmlReconstructor getXmlReconstructor() {
    if (_xmlReconstructor == null) {
      createKey2Extractions();
    }
    return _xmlReconstructor;
  }

  /**
   * Remove the extraction from this instance.
   */
  public boolean removeExtractionContainer(ExtractionContainer extraction) {

    final boolean result = this.extractions.remove(extraction);

    if (result) {
      if (_key2extractions != null) {
        final String key = extraction.getKey();
        final List<ExtractionContainer> extractions = _key2extractions.get(key);
        if (extractions != null) {
          extractions.remove(extraction);
        }

        if (extractions == null || extractions.size() == 0) {
          _key2extractions.remove(key);
        }
      }

      if (_xmlReconstructor != null) {
        final Tree<XmlLite.Data> terminalNode = extraction.getTerminalNode();
        if (terminalNode != null) {
          _xmlReconstructor.removeTagPath(terminalNode);
        }
      }

      this._groups = null;  // reset for recompute
    }

    return result;
  }

  /**
   * Collect tab-delimited fielded strings with the fields:
   * <ul>
   * <li>source (if non-null)</li>
   * <li>groupKey (numeric if numberKeys, otherwise, xpath)</li>
   * <li>parsedText</li>
   * <li>interpNum</li>
   * <li>interpClassification</li>
   * <li>interpConfidence</li>
   * <li>extractionKey (numeric if numberKeys, otherwise, xpath)</li>
   * </ul>
   */
  public final List<String> collectBriefExtractions(String source, boolean numberKeys) {
    final List<String> result = new ArrayList<String>();

    visitExtractions(
      source, numberKeys,
      new ExtractionVisitor() {
        public boolean visitExtractionGroup(String source, int groupNum, String groupKey, ExtractionGroup group) {
          return true;
        }
        public void visitInterpretation(String source, int groupNum, String groupKey, ExtractionContainer.ExtractionData theExtraction,
                                        int interpNum, ParseInterpretation theInterpretation, String extractionKey) {
          showBriefExtraction(source, groupKey, theExtraction, interpNum, theInterpretation, extractionKey, result);
        }
      });

    return result;
  }

  public final void showExtractionGroups(boolean briefResults, String source) {
    showExtractionGroups(briefResults, source, false);
  }

  public final void showExtractionGroups(final boolean briefResults, final String source, final boolean numberKeys) {
    visitExtractions(
      source, numberKeys,
      new ExtractionVisitor() {
        public boolean visitExtractionGroup(String source, int groupNum, String groupKey, ExtractionGroup group) {
          if (!briefResults) {
            System.out.println("   Group #" + (groupNum++) + ": " + groupKey + " w/" +
                               group.getExtractions().size() + " extractions");

            String contextString = null;
            String contextType = "text";
            final DomNode inputNode = group.getInputNode();
            if (inputNode != null) {
              final String inputXml = DomUtil.getSubtreeXml(inputNode);
              if (inputXml != null) {
                contextType = "xml";
              }
            }
            if (contextString == null) {
              contextString = group.getInputText();
            }

            System.out.println("    Context: (" + contextType + "):\n" + contextString);
          }

          return true;
        }
        public void visitInterpretation(String source, int groupNum, String groupKey, ExtractionContainer.ExtractionData theExtraction,
                                        int interpNum, ParseInterpretation theInterpretation, String extractionKey) {
          showBriefExtraction(source, groupKey, theExtraction, interpNum, theInterpretation, extractionKey, null);
        }
      });
  }

  public final void visitExtractions(String source, boolean numberKeys, ExtractionVisitor visitor) {

    String theLastGroupKey = null;
    String theLastExtractionKey = null;

    int groupKeyNum = -1;
    String curGroupKey = null;
    int extractionKeyNum = -1;
    String curExtractionKey = null;

    final Set<Tree<String>> seenParseTrees = new HashSet<Tree<String>>();

    int groupNum = 0;
    for (ExtractionGroup group : getExtractionGroups()) {

      final String groupKey = group.getKey();

      curGroupKey = groupKey;
      if (numberKeys) {
        if (!curGroupKey.equals(theLastGroupKey)) {
          ++groupKeyNum;
        }
        curGroupKey = MathUtil.integerString(groupKeyNum, 3, '0');
        theLastGroupKey = groupKey;
      }

      if (visitor.visitExtractionGroup(source, groupNum, groupKey, group)) {

        for (ExtractionContainer extraction : group.getExtractions()) {

          final String extractionKey = extraction.getKey();

          curExtractionKey = extractionKey;
          if (numberKeys) {
            if (!curExtractionKey.equals(theLastExtractionKey)) {
              ++extractionKeyNum;
            }
            curExtractionKey = MathUtil.integerString(extractionKeyNum, 3, '0');
            theLastExtractionKey = extractionKey;
          }

          final ParseInterpretation theInterpretation = extraction.getTheInterpretation();
          final ExtractionContainer.ExtractionData theExtraction = extraction.getTheExtraction();

          if (theInterpretation != null) {
            final Tree<String> extractionTree = theExtraction.getParseTree();
            if (seenParseTrees.contains(extractionTree)) continue;
            seenParseTrees.add(extractionTree);

            visitor.visitInterpretation(source, groupNum, curGroupKey, theExtraction, 0, theInterpretation, curExtractionKey);
          }
          else {
            for (ExtractionContainer.ExtractionData anExtraction : extraction.getExtractions()) {
              if (anExtraction != null && anExtraction.getInterpretations() != null) {
                final Tree<String> extractionTree = anExtraction.getParseTree();
                if (seenParseTrees.contains(extractionTree)) continue;
                seenParseTrees.add(extractionTree);

                int interpNum = 0;
                for (ParseInterpretation interpretation : anExtraction.getInterpretations()) {
                  visitor.visitInterpretation(source, groupNum, curGroupKey, anExtraction, interpNum++, interpretation, curExtractionKey);
                }
              }
            }
          }
        }
      }
      ++groupNum;
    }
  }

  // source.url group.key context.string interpNum interpretation.classification interpretation.confidence context.key(xpath)
  private final void showBriefExtraction(String source, String groupKey,
                                         ExtractionContainer.ExtractionData theExtraction,
                                         int interpNum, ParseInterpretation theInterpretation,
                                         String extractionKey, List<String> briefResultCollector) {

    final StringBuilder briefExtraction = new StringBuilder();

    final String parsedText = theExtraction == null ? "???" : theExtraction.getParsedText();
    if (theInterpretation != null) {
      if (source != null) briefExtraction.append(source).append('\t');
      briefExtraction.
        append(groupKey).append('\t').
        append(parsedText).append('\t').
        append(interpNum).append('\t').
        append(theInterpretation.getClassification()).append('\t').
        append(MathUtil.doubleString(theInterpretation.getConfidence(), 6)).append('\t').
        append(extractionKey);
      
      if (briefResultCollector != null) {
        briefResultCollector.add(briefExtraction.toString());
      }
      else {
        System.out.println(briefExtraction.toString());
      }
    }
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
    for (ExtractionContainer extraction : extractions) {
      MessageHelper.writePublishable(dataOutput, extraction);
    }
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
    this.extractions = new ArrayList<ExtractionContainer>();

    final int numExtractions = dataInput.readInt();
    for (int extNum = 0; extNum < numExtractions; ++extNum) {
      final ExtractionContainer extraction = (ExtractionContainer)MessageHelper.readPublishable(dataInput);
      extractions.add(extraction);
    }
  }

  //
  // end of PersistablePublishable interface implementation
  //
  ////////


  /**
   * Partition the extractions into groups (overridable).
   */
  protected List<ExtractionGroup> partitionExtractions() {
    List<ExtractionGroup> result = new ArrayList<ExtractionGroup>();

    if (extractions == null || extractions.size() == 0) return result;

    // Get xmlReconstructor so we can use xml structure here.
    // NOTE: side-effect places reconstructed nodes into extraction container instances.
    final XmlReconstructor xmlReconstructor = getXmlReconstructor();

    final Tree<XmlLite.Data> keyTree = xmlReconstructor.getXmlTree();

    // Algorithm:
    // - Put first extraction into first group, set as "prior"
    // - Loop (while next != null)
    //   - Consider (next extraction with prior extraction = np) and (next extraction with following extraction = nf)
    //     - if f == null, then add to current group
    //       - note that this means that exactly 2 extractions will always be grouped together
    //       - ?maybe include a "maximum distance" constraint measured either in characters and/or in tree traversal distance?
    //     - if np.dca (deepest common ancestor) == nf.dca, then add next extraction to the current group
    //     - else, np.dca != nf.dca
    //       - if np.dca is ancestor of nf.dca, then next extraction starts a new group
    //       - if np.dca is descendant of nf.dca, then next extraction goes into current group as its last member
    //   - increment: prior = next, next = following

    ExtractionContainer prior = null;
    ExtractionGroup curGroup = null;
    ExtractionContainer next = null;

    for (ExtractionContainer following : extractions) {
      if (next == null) {
        // still walking up to where we've got prior, next, and following
        if (prior == null) {
          // this is the first extraction
          prior = following;

          curGroup = new ExtractionGroup();
          result.add(curGroup);
          curGroup.add(prior);

          // don't want next = following = prior.
          following = null;
        }
        else {
          // else this is the second extraction and'll become the first 'next' (unless there's ambiguity)

          final boolean ambiguity = prior.getGlobalStartPosition() == following.getGlobalStartPosition();
          if (ambiguity) {
            curGroup.add(following);
            following = null;
          }
        }
      }
      else {
        // have prior, next, and following
        if (curGroup == null) {
          // prior group has been closed, so 'next' goes into a new group
          curGroup = new ExtractionGroup();
          result.add(curGroup);
          curGroup.add(next);
        }
        else {
          final boolean ambiguity = next.getGlobalStartPosition() == following.getGlobalStartPosition();
          if (ambiguity) {
            curGroup.add(next);
            next = prior;  // don't change 'prior' on increment
          }
          else {
            final Tree<XmlLite.Data> nextNode = next.getTerminalNode();

            // next/prior deepest common ancestor node
            final Tree<XmlLite.Data> npNode = nextNode.getDeepestCommonAncestor(prior.getTerminalNode());

            // next/following deepest common ancestor node
            final Tree<XmlLite.Data> nfNode = nextNode.getDeepestCommonAncestor(following.getTerminalNode());

            if (npNode == nfNode) {
              // keep in the same group
              curGroup.add(next);
            }
            else {
              if (npNode.isAncestor(nfNode, true)) {
                // start a new group
                curGroup = new ExtractionGroup();
                result.add(curGroup);
                curGroup.add(next);
              }
              else {  // nfNode.isAncestor(npNode)
                // add as last member of current group
                curGroup.add(next);
                if (!ambiguity) curGroup = null;  // close group
              }
            }
          }
        }
        
        // increment prior
        prior = next;
      }

      // set 'next' before incrementing 'following'
      next = following;
    }

    // Handle last 'next' (has no 'following')
    if (next != null) {
      if (curGroup == null) {
        curGroup = new ExtractionGroup();
        result.add(curGroup);
      }
      curGroup.add(next);
    }

    return result;
  }

  private final List<ExtractionContainer> loadExtractions(ParseOutputCollector output) {
    final List<ExtractionContainer> result = new ArrayList<ExtractionContainer>();

    final List<AtnParseResult> parseResults = output.getParseResults();
    if (parseResults != null) {
      for (AtnParseResult parseResult : parseResults) {
        final ExtractionContainer extraction = ExtractionContainer.createExtractionContainer(parseResult);
        if (extraction != null) {
          result.add(extraction);
        }
      }
    }

    return result;
  }

  /**
   * Extract parses from the output as ExtractionContainers.
   */
  private final void createKey2Extractions() {
    this._key2extractions = new LinkedHashMap<String, List<ExtractionContainer>>();
    this._xmlReconstructor = new XmlReconstructor();

    for (ExtractionContainer extraction : this.extractions) {
      final String key = extraction.getKey();
      List<ExtractionContainer> extractions = _key2extractions.get(key);
      if (extractions == null) {
        extractions = new ArrayList<ExtractionContainer>();
        _key2extractions.put(key, extractions);
      }
      extractions.add(extraction);
      Collections.sort(extractions);

      final Tree<XmlLite.Data> terminalNode = _xmlReconstructor.addTagPath(key, extraction.getLocalText());
      extraction.setTerminalNode(terminalNode);
    }
  }
}
