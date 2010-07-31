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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sd.cio.MessageHelper;
import org.sd.io.PersistablePublishable;
import org.sd.util.tree.Tree;
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


  // all extractions mapped (in document order) by key
  private Map<String, List<ExtractionContainer>> key2extractions;

  // reconstructor for the tree of all extraction keys
  private XmlReconstructor xmlReconstructor;

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
    this.xmlReconstructor = new XmlReconstructor();
    this.key2extractions = createKey2Extractions(output);
    this._groups = null;
  }

  /**
   * Get all extractions mapped (in document order) by key.
   */
  public Map<String, List<ExtractionContainer>> getKey2Extractions() {
    return key2extractions;
  }

  /**
   * Remove the extraction from this instance.
   */
  public boolean removeExtractionContainer(ExtractionContainer extraction) {
    boolean result = false;

    final String key = extraction.getKey();
    final List<ExtractionContainer> extractions = key2extractions.get(key);
    if (extractions != null) {
      extractions.remove(extraction);
      result = true;
    }

    if (extractions == null || extractions.size() == 0) {
      key2extractions.remove(key);
    }

    final Tree<XmlLite.Data> terminalNode = extraction.getTerminalNode();
    if (terminalNode != null) {
      xmlReconstructor.removeTagPath(terminalNode);
    }

    if (result) this._groups = null;  // reset for recompute

    return result;
  }

  /**
   * Get all partitioned groups of extractions.
   */
  public List<ExtractionGroup> getExtractionGroups() {
    if (_groups == null) {
      _groups = partitionExtractions(key2extractions);
    }
    return _groups;
  }

  public int getNumGroups() {
    final List<ExtractionGroup> groups = getExtractionGroups();
    return groups.size();
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

    dataOutput.writeInt(key2extractions.size());
    for (Map.Entry<String, List<ExtractionContainer>> entry : key2extractions.entrySet()) {
      final String key = entry.getKey();
      final List<ExtractionContainer> extractions = entry.getValue();

      MessageHelper.writeString(dataOutput, key);
      dataOutput.writeInt(extractions.size());
      for (ExtractionContainer extraction : extractions) {
        MessageHelper.writePublishable(dataOutput, extraction);
      }
    }

    final List<ExtractionGroup> groups = getExtractionGroups();
    dataOutput.writeInt(groups.size());
    for (ExtractionGroup group : groups) {
      MessageHelper.writePublishable(dataOutput, group);
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

    this.xmlReconstructor = new XmlReconstructor();

    final int numKeys = dataInput.readInt();
    this.key2extractions = new LinkedHashMap<String, List<ExtractionContainer>>();
    for (int keyNum = 0; keyNum < numKeys; ++keyNum) {
      final String key = MessageHelper.readString(dataInput);
      final int numExtractions = dataInput.readInt();

      final List<ExtractionContainer> extractions = new ArrayList<ExtractionContainer>();
      key2extractions.put(key, extractions);

      for (int extNum = 0; extNum < numExtractions; ++extNum) {
        final ExtractionContainer extraction = (ExtractionContainer)MessageHelper.readPublishable(dataInput);
        extractions.add(extraction);

        final Tree<XmlLite.Data> terminalNode = xmlReconstructor.addTagPath(key, extraction.getLocalText());
        extraction.setTerminalNode(terminalNode);
      }
    }

    this._groups = new ArrayList<ExtractionGroup>();
    final int numGroups = dataInput.readInt();
    for (int groupNum = 0; groupNum < numGroups; ++groupNum) {
      _groups.add((ExtractionGroup)MessageHelper.readPublishable(dataInput));
    }
  }

  //
  // end of PersistablePublishable interface implementation
  //
  ////////


  /**
   * Partition the extractions into groups (overridable).
   */
  protected List<ExtractionGroup> partitionExtractions(Map<String, List<ExtractionContainer>> key2extractions) {
    List<ExtractionGroup> result = new ArrayList<ExtractionGroup>();

    int numExts = key2extractions.size();
    if (numExts == 0) return result;

    final List<ExtractionContainer> all = new ArrayList<ExtractionContainer>();
    for (List<ExtractionContainer> extractions : key2extractions.values()) {
      for (ExtractionContainer ec : extractions) {
        if (ec != null) all.add(ec);
        else { System.err.println("found null extractionContainer!"); }
      }
    }
    numExts = all.size();
    final ExtractionContainer[] extractionContainers = all.toArray(new ExtractionContainer[numExts]);

    final Tree<XmlLite.Data> keyTree = xmlReconstructor.getXmlTree();

    // Algorithm:
    // - Put first extraction into first group, set as "prior"
    // - Loop (while next != null)
    //   - Consider (next extraction with prior extraction = np) and (next extraction with following extraction = nf)
    //     - if f == null, then add to current group
    //       - note that this means that exactly 2 extractions will always be grouped together
    //       - ?maybe include a "maximum distance" constraint measured either in characters and/or in tree traversal distance?
    //     - if np.dca (deepest common ancestor) != nf.dca
    //       - if np.dca is ancestor of nf.dca, then next extraction starts a new group
    //       - if np.dca is descendant of nf.dca, then next extraction goes into current group as its last member
    //   - increment: prior = next, next = following

    ExtractionContainer prior = extractionContainers[0];

    ExtractionContainer priorE = extractionContainers[0];
    ExtractionGroup curGroup = new ExtractionGroup();
    result.add(curGroup);
    curGroup.add(priorE);

    for (int ecIdx = 1; ecIdx < numExts; ++ecIdx) {
      final ExtractionContainer nextE = extractionContainers[ecIdx];
      final ExtractionContainer followingE = (ecIdx + 1 < numExts) ? extractionContainers[ecIdx + 1] : null;

      if (followingE == null) {
        // add to current group
        if (curGroup == null) {
          curGroup = new ExtractionGroup();
          result.add(curGroup);
        }
        curGroup.add(nextE);
      }
      else {
        if (nextE.getTerminalNode() == null) {
          System.out.println("no terminalNode! ecIdx=" + ecIdx);
        }

        // next/prior deepest common ancestor node
        final Tree<XmlLite.Data> npNode = nextE.getTerminalNode().getDeepestCommonAncestor(priorE.getTerminalNode());

        // next/following deepest common ancestor node
        final Tree<XmlLite.Data> nfNode = nextE.getTerminalNode().getDeepestCommonAncestor(followingE.getTerminalNode());

        if (npNode == nfNode) {
          // keep in the same group
          if (curGroup == null) {
            curGroup = new ExtractionGroup();
            result.add(curGroup);
          }
          curGroup.add(nextE);
        }
        else {
          if (npNode.isAncestor(nfNode, true)) {
            // start a new group
            curGroup = new ExtractionGroup();
            result.add(curGroup);
            curGroup.add(nextE);
          }
          else {  // nfNode.isAncestor(npNode)
            // add as last member of current group
            if (curGroup == null) {
              curGroup = new ExtractionGroup();
              result.add(curGroup);
            }
            curGroup.add(nextE);
            curGroup = null;
          }
        }
      }

      priorE = nextE;
    }

    return result;
  }

  /**
   * Extract parses from the output as ExtractionContainers.
   */
  private final Map<String, List<ExtractionContainer>> createKey2Extractions(ParseOutputCollector output) {
    final Map<String, List<ExtractionContainer>> result = new LinkedHashMap<String, List<ExtractionContainer>>();

    final List<AtnParseResult> parseResults = output.getParseResults();
    for (AtnParseResult parseResult : parseResults) {
      final ExtractionContainer extraction = ExtractionContainer.createExtractionContainer(parseResult);
      if (extraction != null) {
        final String key = extraction.getKey();
        List<ExtractionContainer> extractions = result.get(key);
        if (extractions == null) {
          extractions = new ArrayList<ExtractionContainer>();
          result.put(key, extractions);
        }
        extractions.add(extraction);
        Collections.sort(extractions);

        final Tree<XmlLite.Data> terminalNode = xmlReconstructor.addTagPath(key, extraction.getLocalText());
        extraction.setTerminalNode(terminalNode);
      }
    }

    return result;
  }
}
