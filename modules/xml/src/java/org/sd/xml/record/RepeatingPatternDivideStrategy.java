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
package org.sd.xml.record;


import org.sd.util.tree.Tree;
import org.sd.util.fsm.FsmBuilder;
import org.sd.util.fsm.FsmBuilder1;
import org.sd.util.fsm.FsmSequence;
import org.sd.xml.XmlLite;

import java.util.List;


/**
 * A top-down divide strategy where nodes containing a repeating pattern of
 * tags are split.
 * <p>
 * @author Spence Koehler
 */
public class RepeatingPatternDivideStrategy extends TopDownDivideStrategy {
  
  public RepeatingPatternDivideStrategy() {
    super();
  }

  public RepeatingPatternDivideStrategy(Record.View view) {
    super(view);
  }

  /**
   * Given a "record" xmlNode, determine whether its a final (divided) record.
   *
   * @return true the node is considered as a record; otherwise, false.
   */
  protected boolean nodeIsRecord(Tree<XmlLite.Data> xmlNode) {

    // root is never chosen as a record
    if (xmlNode.getParent() == null) return false;

    boolean result = nodeIsCandidateRecord(xmlNode, null);

    return result;
  }

  private final boolean nodeIsCandidateRecord(Tree<XmlLite.Data> xmlNode, PatternContainer apc) {

    // get/build the patterns
    final PatternContainer pc = getPatternContainer(xmlNode);

    //
    // A node is a record if
    //
    // - it is a candidate record and
    // - no descendants are "better" candidate records
    //

    //
    // A node is a candidate record if:
    // 
    // - it has repeating record children or
    // - it has multiple "record" siblings (with text)
    //
    // - where multiple record nodes are those where sibling names have a
    //   repeating pattern
    //

    boolean result = pc.patternRepeats();

    final String nodeName = getName(xmlNode);

    if (!result) {
      // check for parent pattern as long as we cover more than one path
      final Record record = Record.getRecord(xmlNode);
      if (record != null && record.maxPathIndex() - record.minPathIndex() > 0) {
        final PatternContainer ppc = getPatternContainer(xmlNode.getParent());
        result = ppc.patternRepeats();
      }
    }

    if (result && apc != null) {
      // check whether the current pc is better than (ancestor) apc
      if (pc.getNumPatterns() == 1 && pc.patternRepeats(0) && pc.getNumTokens(0) == 1) {
        // isn't better
        result = false;
      }
    }

    if (result) {
      if (xmlNode.hasChildren()) {
        for (Tree<XmlLite.Data> childNode : xmlNode.getChildren()) {
          if (nodeIsCandidateRecord(childNode, apc == null ? pc : apc)) {
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  private final PatternContainer getPatternContainer(Tree<XmlLite.Data> xmlNode) {
    PatternContainer result = null;

    final Object pc = xmlNode.getData().getProperty("RepeatingPatternDivideStrategy:PatternContainer");
    if (pc != null) {
      result = (PatternContainer)pc;
    }
    else {
      result = buildPatternContainer(xmlNode);
      xmlNode.getData().setProperty("RepeatingPatternDivideStrategy:PatternContainer", result);
    }

    return result;
  }

  private final PatternContainer buildPatternContainer(Tree<XmlLite.Data> xmlNode) {
    // build the patterns
    final FsmBuilder<String> fsmBuilder = new FsmBuilder1<String>(".");
    int numChildren = 0;

    if (xmlNode.hasChildren()) {
      for (Tree<XmlLite.Data> childNode : xmlNode.getChildren()) {
        if (Record.isNonRecord(childNode)) continue;  // ignore non-record nodes.
        final String childName = getName(childNode);
        fsmBuilder.add(childName);
        ++numChildren;
      }
    }

    return new PatternContainer(fsmBuilder, numChildren);
  }

  /**
   * Container for child name patterns.
   */
  private static final class PatternContainer {

    public final FsmBuilder<String> fsmBuilder;
    public final int numChildren;
    private List<FsmSequence<String>> _sequences;

    PatternContainer(FsmBuilder<String> fsmBuilder, int numChildren) {
      this.fsmBuilder = fsmBuilder;
      this.numChildren = numChildren;
      this._sequences = null;
    }

    public List<FsmSequence<String>> getSequences() {
      if (_sequences == null) {
        this._sequences = fsmBuilder.getSequences(false/*unique*/);
      }
      return _sequences;
    }

    public final int getNumPatterns() {
      int result = 0;

      if (numChildren > 0) {
        result = getSequences().size();
      }

      return result;
    }

    public final FsmSequence<String> getPattern(int patternIndex) {
      FsmSequence<String> result = null;

      if (numChildren > 0) {
        final List<FsmSequence<String>> sequences = getSequences();
        if (sequences.size() > patternIndex) {
          result = sequences.get(patternIndex);
        }
      }

      return result;
    }

    public final int getNumTokens(int patternIndex) {
      final FsmSequence<String> seq = getPattern(patternIndex);
      return (seq == null) ? 0 : seq.size();
    }

    public boolean patternRepeats(int patternIndex) {
      final FsmSequence<String> seq = getPattern(patternIndex);
      return seq == null ? false : seq.getTotalRepeat() > 1;
    }

    public boolean patternRepeats() {
      boolean result = false;

      for (FsmSequence<String> seq : getSequences()) {
        if (seq.getTotalRepeat() > 1) {
          result = true;
          break;
        }
      }

      return result;
    }
  }
}
