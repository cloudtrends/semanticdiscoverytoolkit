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
package org.sd.text.segment;


import java.util.LinkedList;

/**
 * Container for an ordered sequence of segments.
 * <p>
 * @author Spence Koehler
 */
public class SegmentSequence {
  
  private LinkedList<Segment> segments;

  /**
   * Construct an empty sequence.
   */
  public SegmentSequence() {
    this.segments = new LinkedList<Segment>();
  }

  /**
   * Add a segment to the end of this sequence.
   */
  public SegmentSequence add(Segment segment) {
    segments.add(segment);
    return this;
  }

  /**
   * Add a new (non-delim) segment with the given text.
   * <p>
   * Note that the segment will have no FeatureBag.
   *
   * @return the new Segment.
   */
  public Segment add(String text) {
    return add(text, false);
  }

  /**
   * Add a new segment with the given text.
   * <p>
   * Note that the segment will have no FeatureBag.
   *
   * @return the new Segment.
   */
  public Segment add(String text, boolean isDelim) {
    final Segment result = new Segment(text);
    if (isDelim) result.setManualFeature(CommonFeatures.DELIM, isDelim);
    add(result);
    return result;
  }

  /**
   * Add the other sequence to the end of this sequence.
   */
  public SegmentSequence add(SegmentSequence other) {
    segments.addAll(other.segments);
    return this;
  }

  /**
   * Get the size of this sequence.
   */
  public int size() {
    return segments.size();
  }

  /**
   * Get the first segment or null.
   */
  public Segment getFirstSegment() {
    return getSegment(0);
  }

  /**
   * Get the last segment or null.
   */
  public Segment getLastSegment() {
    return getSegment(segments.size() - 1);
  }

  /**
   * Get the segment at the given position.
   */
  public Segment getSegment(int position) {
    Segment result = null;

    if (position >= 0 && position < segments.size()) {
      result = segments.get(position);
    }

    return result;
  }

  /**
   * Get the position of the segment in this sequence.
   */
  public int getSegmentPosition(Segment segment) {
    return segments.indexOf(segment);
  }

  /**
   * Get the segment that follows the given segment in this sequence.
   */
  public Segment getNextSegment(Segment segment) {
    Segment result = null;

    final int position = segments.indexOf(segment);
    if (position + 1 < segments.size()) {
      result = segments.get(position + 1);
    }

    return result;
  }

  /**
   * Join the segment with its following segment, returning the now removed
   * following segment.
   */
  public Segment joinSegmentWithNext(Segment segment) {
    Segment result = null;

    final int position = segments.indexOf(segment);
    if (position + 1 < segments.size()) {
      result = segments.get(position + 1);
      segments.remove(result);
      segment.append(result);
    }

    return result;
  }

  /**
   * Get the segment that precedes the given segment in this sequence.
   * <p>
   * If at the first segment, return null.
   * <p>
   * If the given segment is null, get the last segment.
   */
  public Segment getPreviousSegment(Segment segment) {
    Segment result = null;

    if (segment == null) {
      if (segments.size() > 0) {
        result = segments.get(segments.size() - 1);
      }
    }
    else {
      final int position = segments.indexOf(segment);
      if (position - 1 >= 0) {
        result = segments.get(position - 1);
      }
    }

    return result;
  }

  /**
   * Get the segments.
   */
  public LinkedList<Segment> getSegments() {
    return segments;
  }

  /**
   * Set the current segment to be the first segment of the sequence.
   * <p>
   * Note that if the curSegment is null (at the end of the sequence),
   * the sequence will be cleared!
   */
  public void setStart(Segment segment) {
    if (segment == null) {
      segments.clear();
    }
    else {
      int position = segments.indexOf(segment);
      while (position > 0) {
        segments.remove();
        --position;
      }
    }
  }

  /**
   * Set the current segment to be the last segment of the sequence.
   * <p>
   * Note that if the curSegment is null (at the end of the sequence),
   * nothing will happen. If the curSegment is at the beginning of
   * the sequence, it will be the only remaining segment!
   */
  public void setLast(Segment segment) {
    if (segment != null) {
      int position = segments.indexOf(segment);
      final int len = segments.size();

      for (int i = position + 1; i < len; ++i) {
        segments.removeLast();
      }
    }
  }

  /**
   * Set the current segment to be the end of the sequence, removing it
   * and all following segments.
   * <p>
   * Note that if the curSegment is null (at the end of the sequence),
   * nothing will happen. If the curSegment is at the beginning of
   * the sequence, the sequence will be empty!
   */
  public void setEnd(Segment segment) {
    if (segment != null) {
      int position = segments.indexOf(segment);
      final int len = segments.size();

      for (int i = position; i < len; ++i) {
        segments.removeLast();
      }
    }
  }

  /**
   * Split the segment using its feature function, replacing the segment with the results
   * if non-null.
   */
  public SegmentSequence splitSegment(Segment segment, String feature) {
    final SegmentSequence result = segment.split(feature);

    replaceSegment(segment, result);

    return result;
  }

  /**
   * Split the segment using the split function, replacing the segment with the results
   * if non-null.
   */
  public SegmentSequence splitSegment(Segment segment, SplitFunction splitFunction) {
    SegmentSequence result = null;

    if (splitFunction.shouldSplit(segment)) {
      result = splitFunction.split(segment.getText());
    
      if (result != null) {
        result.injectFeatureBag(segment.getFeatureBag());
        replaceSegment(segment, result);
      }
    }

    return result;
  }

  /**
   * Replace the segment with the sequence's segments.
   */
  public void replaceSegment(Segment segment, SegmentSequence sequence) {
    if (sequence != null) {
      int pos = segments.indexOf(segment);
      segments.remove(pos);

      for (Segment splitSegment : sequence.getSegments()) {
        segments.add(pos++, splitSegment);
      }
    }
  }

  /**
   * Inject the feature bag into this sequence's segments.
   */
  public SegmentSequence injectFeatureBag(FeatureBag featureBag) {
    for (Segment segment : segments) {
      segment.setFeatureBag(featureBag);
    }
    return this;
  }

  /**
   * Replace the segment with multiple segments (all having the same
   * feature bag, but no set features) an item of text. 
   */
  public void splitSegment(Segment segment, String[] text) {
    int pos = segments.indexOf(segment);
    segments.remove(pos);

    for (String nextText : text) {
      segments.add(pos++, new Segment(segment.getFeatureBag(), nextText));
    }
  }

  /**
   * Find the first segment with the given feature.
   */
  public Segment findFirstWithFeature(String feature, Boolean value) {
    return findFirstWithFeature(feature, value, 0);
  }

  /**
   * Find the first segment with the given feature at or after at the given
   * position.
   */
  public Segment findFirstWithFeature(String feature, Boolean value, int startPos) {
    return findFirstWithFeature(feature, value, startPos, segments.size());
  }

  /**
   * Find the first segment with the given feature at or after at the start
   * position, but before the end position.
   */
  public Segment findFirstWithFeature(String feature, Boolean value, int startPos, int endPos) {
    Segment result = null;

    for (int i = startPos; i < endPos; ++i) {
      final Segment segment = segments.get(i);
      if (segment.hasFeature(feature, value)) {
        result = segment;
        break;
      }
    }

    return result;
  }

  /**
   * Find the first segment with the given feature at or after at the start
   * segment, but before the end segment (where null indicates the end of the sequence).
   */
  public Segment findFirstWithFeature(String feature, Boolean value, Segment startSegment, Segment endSegment) {
    return findFirstWithFeature(feature, value, segments.indexOf(startSegment),
                                endSegment == null ? segments.size() : segments.indexOf(endSegment));
  }

  /**
   * Find the last segment with the given feature.
   */
  public Segment findLastWithFeature(String feature, Boolean value) {
    return findLastWithFeature(feature, value, 0, segments.size());
  }

  /**
   * Find the last segment with the given feature, at or before the given
   * position.
   */
  public Segment findLastWithFeature(String feature, Boolean value, int endPos) {
    return findLastWithFeature(feature, value, 0, endPos);
  }    

  /**
   * Find the last segment with the given feature, at or after the start
   * position (where 0 indicates the beginning of the sequence), but before
   * the end position.
   */
  public Segment findLastWithFeature(String feature, Boolean value, int startPos, int endPos) {
    Segment result = null;

    for (int i = endPos - 1; i >= startPos; --i) {
      final Segment segment = segments.get(i);
      if (segment.hasFeature(feature, value)) {
        result = segment;
        break;
      }
    }

    return result;
  }

  /**
   * Find the last segment with the given feature at or after the start
   * segment (where null or the first segment indicates the beginning of the
   * sequence), but before the end segment (where null indicates the end of
   * the sequence).
   */
  public Segment findLastWithFeature(String feature, Boolean value, Segment startSegment, Segment endSegment) {
    return findLastWithFeature(feature, value,
                               startSegment == null ? 0 : segments.indexOf(startSegment),
                               endSegment == null ? segments.size() : segments.indexOf(endSegment));
  }


  public String toString() {
    return segments.toString();
  }
}
