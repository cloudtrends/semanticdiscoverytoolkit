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
package org.sd.token;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.xml.DataProperties;

/**
 * A StandardTokenizer augmented with SegmentPointer information.
 * <p>
 * @author Spence Koehler
 */
public class SegmentTokenizer extends StandardTokenizer {
  
  private DataProperties dataProperties;
  private Map<String, List<SegmentPointer>> label2ptrs;
  private Map<Integer, SegmentPointer> pos2ptr;
  
  private Set<String> hardBoundaryLabels;
  private Set<String> unbreakableLabels;

  //
  // properties:
  //   segmentHardBoundaries -- comma-delimited list of segment labels that identify hard-boundary segments
  //   segmentUnbreakables -- comma-delimited list of segment labels that have no internal breaking

  public SegmentTokenizer(SegmentPointerFinder ptrFinder, StandardTokenizerOptions tokenizerOptions, DataProperties dataProperties) {
    super(ptrFinder.getInput(), tokenizerOptions);
    this.dataProperties = dataProperties;
    this.label2ptrs = new HashMap<String, List<SegmentPointer>>();
    this.pos2ptr = new HashMap<Integer, SegmentPointer>();


    this.hardBoundaryLabels = null;
    this.unbreakableLabels = null;

    if (dataProperties != null) {
      final String[] segmentHardBoundaries = dataProperties.getString("segmentHardBoundaries", "").split(",");
      for (String s : segmentHardBoundaries) {
        if (!"".equals(s)) {
          if (hardBoundaryLabels == null) hardBoundaryLabels = new HashSet<String>();
          hardBoundaryLabels.add(s);
        }
      }

      final String[] segmentUnbreakables = dataProperties.getString("segmentUnbreakables", "").split(",");
      for (String s : segmentUnbreakables) {
        if (!"".equals(s)) {
          if (unbreakableLabels == null) unbreakableLabels = new HashSet<String>();
          unbreakableLabels.add(s);
        }
      }
    }


    int seqNum = 0;
    for (SegmentPointerIterator iter = new SegmentPointerIterator(ptrFinder); iter.hasNext(); ) {
      final SegmentPointer ptr = iter.next();
      ptr.setSeqNum(seqNum++);
      final String label = ptr.getLabel();

      List<SegmentPointer> ptrs = label2ptrs.get(label);
      if (ptrs == null) {
        ptrs = new ArrayList<SegmentPointer>();
        label2ptrs.put(label, ptrs);
      }
      ptrs.add(ptr);

      if (hardBoundaryLabels.contains(ptr.getLabel())) {
        // NOTE: only hard boundary labels get a token feature
        pos2ptr.put(ptr.getStartPtr(), ptr);
      }
    }
  }

  protected Map<Integer, Break> createBreaks() {
    final Map<Integer, Break> result = super.createBreaks();

    // mark hard boundaries and unbreakable segments
    if (hardBoundaryLabels != null) {
      for (String hardBoundaryLabel : hardBoundaryLabels) {
        final List<SegmentPointer> ptrs = label2ptrs.get(hardBoundaryLabel);
        if (ptrs != null) {
          for (SegmentPointer ptr : ptrs) {
            // Set LHS break as Hard
            setBreak(result, ptr.getStartPtr(), true, true);

            // Set RHS break as Hard
            setBreak(result, ptr.getEndPtr(), false, true);
          }
        }
      }
    }

    if (unbreakableLabels != null) {
      for (String unbreakableLabel : unbreakableLabels) {
        final List<SegmentPointer> ptrs = label2ptrs.get(unbreakableLabel);
        if (ptrs != null) {
          for (SegmentPointer ptr : ptrs) {
            // clear breaks within segment
            if (!ptr.hasInnerSegments()) {
              clearBreaks(result, ptr.getStartPtr() + 1, ptr.getEndPtr());
            }
            else {
              for (SegmentPointer.InnerSegment innerSegment : ptr.getInnerSegments()) {
                // Set LHS, RHS breaks as Hard
                setBreak(result, innerSegment.getStartPtr(), true, true);
                setBreak(result, innerSegment.getEndPtr(), false, true);
                clearBreaks(result, innerSegment.getStartPtr() + 1, innerSegment.getEndPtr());
              }
            }
          }
        }
      }
    }

    return result;
  }

  protected void addTokenFeatures(Token token) {
    if (token != null) {
      final SegmentPointer ptr = pos2ptr.get(token.getStartIndex());
      if (ptr != null && ptr.getEndPtr() >= token.getEndIndex()) {
        token.setFeature(ptr.getLabel(), ptr, this);
      }
    }
  }

  /**
   * A feature constraint for locating segment pointer features on tokens
   * <p>
   * Note that values of features found through this constraint will be
   * SegmentPointer instances.
   */
  public static final FeatureConstraint createSegmentPointerFeatureConstraint(String label) {
    final FeatureConstraint result = new FeatureConstraint();
    result.setType(label);
    result.setClassType(SegmentTokenizer.class);
    result.setFeatureValueType(SegmentPointer.class);
    return result;
  }
}
