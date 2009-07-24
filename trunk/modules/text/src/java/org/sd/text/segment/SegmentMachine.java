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


/**
 * Utility to traverse segment sequences.
 * <p>
 * @author Spence Koehler
 */
public class SegmentMachine {

  private SegmentSequence segmentSequence;
  private StringBuilder curText;
  private Segment curSegment;
  private int numSkippedSegments;
  private Segment markedSegment;

  public SegmentMachine(String text) {
    this.segmentSequence = new SegmentSequence();
    segmentSequence.add(text);
    init();
  }

  public SegmentMachine(String text, FeatureBag featureBag) {
    this.segmentSequence = new SegmentSequence().add(new Segment(featureBag, text));
    init();
  }

  public SegmentMachine(SegmentSequence segmentSequence) {
    this.segmentSequence = segmentSequence;
    init();
  }

  private final void init() {
    this.curText = new StringBuilder();
    this.curSegment = segmentSequence.getFirstSegment();
    this.numSkippedSegments = 0;
    this.markedSegment = null;
  }

  public SegmentSequence getSegmentSequence() {
    return segmentSequence;
  }

  /**
   * Set the current segment to the end of the sequence,
   * clearing the current text if indicated.
   */
  public SegmentMachine setToEnd(boolean reset) {
    curSegment = null;
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  /**
   * Set the current segment to the beginning of the sequence,
   * clearing the current text if indicated.
   */
  public SegmentMachine setToBeginning(boolean reset) {
    curSegment = segmentSequence.getFirstSegment();
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  public SegmentMachine mark(boolean reset) {
    this.markedSegment = curSegment;
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  public SegmentMachine setToMark(boolean reset) {
    curSegment = markedSegment;
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  /**
   * Set the current segment to be the first segment of the sequence,
   * clearing the current text if indicated.
   * <p>
   * Note that if the curSegment is null (at the end of the sequence),
   * the sequence will be cleared!
   */
  public SegmentMachine setStart(boolean reset) {
    segmentSequence.setStart(curSegment);
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  /**
   * Set the current segment to be the last segment of the sequence,
   * clearing the current text if indicated.
   * <p>
   * Note that if the curSegment is null (at the end of the sequence),
   * the sequence will not be changed!
   */
  public SegmentMachine setLast(boolean reset) {
    segmentSequence.setLast(curSegment);
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  /**
   * Clip the current and following segments from the segment sequence,
   * leaving the machine at the (new) end. Optionally reset curText.
   */
  public SegmentMachine setEnd(boolean reset) {
    segmentSequence.setEnd(curSegment);
    if (reset) {
      curText.setLength(0);
    }
    return this;
  }

  /**
   * Set the current segment to be the last, resetting its text to be the given
   * text. If the text to set is empty, then remove the current segment
   * (and all following). If the current segment is beyond the last segment,
   * add a segment with the non-empty text (or do nothing if text is empty).
   * <p>
   * As a side effect curText will be cleared and the machine will be positioned
   * after the last segment.
   */
  public SegmentMachine resetLast(String lastString) {
    if (lastString == null) lastString = "";
    if (curSegment == null) {
      if (!"".equals(lastString)) {
        segmentSequence.add(lastString);  // add a segment
      }
    }
    else {
      segmentSequence.setLast(curSegment);
      if (!lastString.equals(curSegment.getText())) {
        final String[] newText = ("".equals(lastString)) ? new String[0] : new String[]{lastString};
        segmentSequence.splitSegment(curSegment, newText);  // note: if newText is empty, curSegment is removed.
      }
    }

    return setToEnd(true);
  }

//   public void setCurSegment(Segment segment) {
// //todo: throw exception if segment isn't in the segment sequence?
//     this.curSegment = segment;
//   }

  public Segment getCurSegment() {
    return curSegment;
  }

  /**
   * Get non-null prior segment's text (skipping/ignoring white and possibly empty).
   */
  public String getPriorSegmentText() {
    final StringBuilder result = new StringBuilder();

    for (Segment priorSegment = segmentSequence.getPreviousSegment(curSegment); priorSegment != null;
         priorSegment = segmentSequence.getPreviousSegment(priorSegment)) {

      result.insert(0, priorSegment.getText().trim());
      if (!SegmentUtils.isDelimOrWhitespace(priorSegment)) {
        break;
      }
    }

    return result.toString();
  }

  public Segment getPreviousSegment() {
    return segmentSequence.getPreviousSegment(curSegment);
  }

  public Segment getNextSegment() {
    return segmentSequence.getNextSegment(curSegment);
  }

  /**
   * Get the current text (without fixing). If reset, then empty the text buffer.
   */
  public String getCurText(boolean reset) {
    return getCurText(false, reset);
  }

  /**
   * Get the current text. If fix, then remove extra whitespace and delims
   * from ends. If reset, then empty the text buffer.
   */
  public String getCurText(boolean fix, boolean reset) {
    String result = curText.toString();

    if (fix) {
      result = fix(result);
    }

    if (reset) {
      curText.setLength(0);
    }

    return result;
  }

  /**
   * Fix the text by removing extra whitespace and delims from the ends.
   */
  public static final String fix(String text) {
    String result = "";

    final int len = text.length();
    int startPos = 0;
    for (; startPos < len && !isStartChar(text.charAt(startPos)); ++startPos);

    int endPos = len;
    for (; endPos > 0 && !isEndChar(text.charAt(endPos - 1)); --endPos);
    
    if (endPos > startPos) {
      result = text.substring(startPos, endPos);
    }

    return result;
  }

  public static final String fixFront(String text) {
    String result = "";

    final int len = text.length();
    int startPos = 0;
    for (; startPos < len && !isStartChar(text.charAt(startPos)); ++startPos);

    int endPos = len;
    
    if (endPos > startPos) {
      result = text.substring(startPos, endPos);
    }

    return result;
  }

  public static final String fixBack(String text) {
    String result = "";

    final int len = text.length();
    int startPos = 0;

    int endPos = len;
    for (; endPos > 0 && !isEndChar(text.charAt(endPos - 1)); --endPos);
    
    if (endPos > startPos) {
      result = text.substring(startPos, endPos);
    }

    return result;
  }

  private static final boolean isStartChar(char c) {
    return c == '(' || Character.isLetterOrDigit(c);
  }

  private static final boolean isEndChar(char c) {
    return c == '.' || c == ')' || Character.isLetterOrDigit(c);
  }

  public void resetCurText() {
    curText.setLength(0);
  }

  public int getNumSkippedSegments() {
    return numSkippedSegments;
  }

  public boolean hasFeature(String feature, Boolean value) {
    boolean result = false;

    if (curSegment != null) {
      result = curSegment.hasFeature(feature, value);
    }

    return result;
  }

  /**
   * Check the prior non-white segment for the given feature.
   */
  public boolean priorHasFeature(String feature, Boolean value) {
    boolean result = false;

    for (Segment priorSegment = segmentSequence.getPreviousSegment(curSegment);
         priorSegment != null;
         priorSegment = segmentSequence.getPreviousSegment(priorSegment)) {

      if (SegmentUtils.isDelimOrWhitespace(priorSegment)) continue;

      result = priorSegment.hasFeature(feature, value);
      break;
    }

    return result;
  }

  /**
   * Check the next non-white segment for the given feature.
   */
  public boolean nextHasFeature(String feature, Boolean value) {
    boolean result = false;

    for (Segment nextSegment = segmentSequence.getNextSegment(curSegment);
         nextSegment != null;
         nextSegment = segmentSequence.getNextSegment(nextSegment)) {

      if (SegmentUtils.isDelimOrWhitespace(nextSegment)) continue;

      result = nextSegment.hasFeature(feature, value);
      break;
    }

    return result;
  }

  /**
   * Go forward from the current segment the given number of segments or
   * until hitting the end of the sequence. Ignore delims and whitespace
   * for counting purposes.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean forward(int numSegments) {
    return forward(numSegments, true);
  }

  /**
   * Go forward from the current segment the given number of segments or
   * until hitting the end of the sequence.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean forward(int numSegments, boolean ignoreDelimsAndWhitespace) {
    boolean result = false;
    numSkippedSegments = 0;

    for (int i = 0; i < numSegments; ++i) {
      if (curSegment == null) break;
      curText.append(curSegment.getText());
      curSegment = segmentSequence.getNextSegment(curSegment);
      ++numSkippedSegments;

      if (ignoreDelimsAndWhitespace) {
        if (SegmentUtils.isDelimOrWhitespace(curSegment)) {
          --i;
          --numSkippedSegments;
        }
      }

      result = true;
    }

    return result;
  }

  /**
   * Go forward from the current segment until hitting a segment with the
   * given value for the feature.
   * <p>
   * If the feature/value combination is never found, the state will not change.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the feature matched (even if the state hasn't changed);
   *         otherwise, false.
   */
  public boolean forwardUntil(String feature, Boolean value) {
    boolean result = false;

    final String backupText = curText.toString();
    final Segment backupSegment = curSegment;
    numSkippedSegments = 0;

    for (; curSegment != null ;
         curSegment = segmentSequence.getNextSegment(curSegment)) {
      if (curSegment.hasFeature(feature, value)) {
        result = true;
        break;
      }
      curText.append(curSegment.getText());
      if (!SegmentUtils.isDelimOrWhitespace(curSegment)) {
        ++numSkippedSegments;
      }
    }

    if (curSegment == null && !result) {
      // never found it! revert.
      curSegment = backupSegment;
      curText.setLength(0);
      curText.append(backupText);
      numSkippedSegments = 0;
    }

    return result;
  }

  /**
   * Go forward from the current segment until hitting a segment with the
   * given value for the feature.  Continue moving forward through segments
   * until reaching a segment for which the feature does NOT have the value.
   * Ignore delims and whitespace.
   * <p>
   * If the feature/value combination is never found, the state will not change.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the feature matched (even if the state hasn't changed);
   *         otherwise, false.
   */
  public boolean forwardThrough(String feature, Boolean value) {
    return forwardThrough(feature, value, true);
  }

  /**
   * Go forward from the current segment until hitting a segment with the
   * given value for the feature.  Continue moving forward through segments
   * until reaching a segment for which the feature does NOT have the value.
   * <p>
   * If the feature/value combination is never found, the state will not change.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the feature matched (even if the state hasn't changed);
   *         otherwise, false.
   */
  public boolean forwardThrough(String feature, Boolean value, boolean ignoreDelimsAndWhitespace) {
    boolean result = forwardUntil(feature, value);
    numSkippedSegments = 0;

    if (result) {
      for (; curSegment != null &&
             (curSegment.hasFeature(feature, value) ||
              (ignoreDelimsAndWhitespace && SegmentUtils.isDelimOrWhitespace(curSegment))) ;

           curSegment = segmentSequence.getNextSegment(curSegment)) {

        curText.append(curSegment.getText());
        ++numSkippedSegments;
      }

      // if stopped on whitespace or delims, move back to text
      if (ignoreDelimsAndWhitespace && SegmentUtils.isDelimOrWhitespace(curSegment)) {
        backwardUntil(feature, value);
      }
    }

    return result;
  }

  /**
   * Go forward from the current segment until hitting the end.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean forwardToEnd() {
    boolean result = false;
    numSkippedSegments = 0;
    for (; curSegment != null; curSegment = segmentSequence.getNextSegment(curSegment)) {
      curText.append(curSegment.getText());
      if (!SegmentUtils.isDelimOrWhitespace(curSegment)) {
        ++numSkippedSegments;
      }
      result = true;
    }
    return result;
  }

  /**
   * Go backward from the current segment the given number of segments or
   * until hitting the first segment. Ignore delims and whitespace for
   * counting purposes.
   * <p>
   * Insert text from the current segment's predecessor up to and including
   * the final segment reached.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean backward(int numSegments) {
    return backward(numSegments, true);
  }

  /**
   * Go backward from the current segment the given number of segments or
   * until hitting the first segment.
   * <p>
   * Insert text from the current segment's predecessor up to and including
   * the final segment reached.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean backward(int numSegments, boolean ignoreDelimsAndWhitespace) {
    boolean result = false;
    numSkippedSegments = 0;

    Segment theSegment = curSegment;

    for (int i = 0; i < numSegments; ++i) {
      curSegment = segmentSequence.getPreviousSegment(curSegment);
      ++numSkippedSegments;
      if (curSegment != null) {
        result = true;
        curText.insert(0, curSegment.getText());

        if (ignoreDelimsAndWhitespace) {
          if (SegmentUtils.isDelimOrWhitespace(curSegment)) {
            --i;
            --numSkippedSegments;
          }
        }
        theSegment = curSegment;
      }
      else {
        curSegment = theSegment;  // put back onto the first segment.
        break;
      }
    }

    return result;
  }

  /**
   * Go backward from the current segment until the prior segment has
   * the given value for the feature.
   * <p>
   * If the feature/value combination is never found, the state will not change.
   * <p>
   * Insert text from the current segment's predecessor up to and including
   * the final segment reached.
   *
   * @return true if the feature matched (even if the state hasn't changed);
   *         otherwise, false.
   */
  public boolean backwardUntil(String feature, Boolean value) {
    boolean result = false;

    final String backupText = curText.toString();
    final Segment backupSegment = curSegment;
    numSkippedSegments = 0;

    Segment priorSegment = segmentSequence.getPreviousSegment(curSegment);
    while (priorSegment != null) {
      if (priorSegment.hasFeature(feature, value)) {
        result = true;
        break;
      }
      curSegment = priorSegment;
      curText.insert(0, curSegment.getText());
      if (!SegmentUtils.isDelimOrWhitespace(priorSegment)) {
        ++numSkippedSegments;
      }
      priorSegment = segmentSequence.getPreviousSegment(curSegment);
    }

    if (priorSegment == null && !result) {
      // never found it! revert.
      curSegment = backupSegment;
      curText.setLength(0);
      curText.append(backupText);
      numSkippedSegments = 0;
    }

    return result;
  }

  /**
   * Go backward from the current segment until the prior segment has
   * the given value. Continue moving backward through segments until
   * the prior segment does NOT have the value. Ignore delims and
   * whitespace.
   * <p>
   * If the feature/value combination is never found, the state will not change.
   * <p>
   * Insert text from the current segment's predecessor up to and including
   * the final segment reached.
   *
   * @return true if the feature matched (even if the state hasn't changed);
   *         otherwise, false.
   */
  public boolean backwardThrough(String feature, Boolean value) {
    return backwardThrough(feature, value, true);
  }

  /**
   * Go backward from the current segment until the prior segment has
   * the given value. Continue moving backward through segments until
   * the prior segment does NOT have the value.
   * <p>
   * If the feature/value combination is never found, the state will not change.
   * <p>
   * Insert text from the current segment's predecessor up to and including
   * the final segment reached.
   *
   * @return true if the feature matched (even if the state hasn't changed);
   *         otherwise, false.
   */
  public boolean backwardThrough(String feature, Boolean value, boolean ignoreDelimsAndWhitespace) {

    boolean result = backwardUntil(feature, value);
    numSkippedSegments = 0;

    if (result) {
      Segment priorSegment = segmentSequence.getPreviousSegment(curSegment);
      while (priorSegment != null &&
             (priorSegment.hasFeature(feature, value) ||
               (ignoreDelimsAndWhitespace && SegmentUtils.isDelimOrWhitespace(priorSegment)))) {
        curSegment = priorSegment;
        curText.insert(0, curSegment.getText());
        priorSegment = segmentSequence.getPreviousSegment(curSegment);
        ++numSkippedSegments;
      }

      // if stopped on whitespace or delims, move forward to text
      if (ignoreDelimsAndWhitespace && SegmentUtils.isDelimOrWhitespace(curSegment)) {
        forwardUntil(feature, value);
      }
    }

    return result;
  }

  /**
   * Go forward from the current segment until hitting the end.
   * <p>
   * Collect text up to, but not including, the final segment reached.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean backwardToBeginning() {
    boolean result = false;
    numSkippedSegments = 0;

    Segment priorSegment = segmentSequence.getPreviousSegment(curSegment);
    while (priorSegment != null) {
      curSegment = priorSegment;
      if (!SegmentUtils.isDelimOrWhitespace(curSegment)) {
        ++numSkippedSegments;
      }
      curText.insert(0, curSegment.getText());
      priorSegment = segmentSequence.getPreviousSegment(curSegment);
      result = true;
    }

    return result;
  }

  /**
   * Split the current segment with the feature function.
   * <p>
   * If successful, set the current segment to be the first new split;
   * otherwise, remain at the current segment.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean splitAndPositionAtFirst(String feature) {
    boolean result = false;

    if (curSegment != null) {
      final SegmentSequence splitSequence = segmentSequence.splitSegment(curSegment, feature);
      if (splitSequence != null) {
        curSegment = splitSequence.getSegment(0);
        result = true;
      }
    }

    return result;
  }

  /**
   * Split the current segment with the feature function.
   * <p>
   * If successful, set the current segment to be the segment after the
   * new split; otherwise, leave the current segment at its location.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean splitAndPositionAfter(String feature) {
    boolean result = false;

    if (curSegment != null) {
      final Segment nextSegment = segmentSequence.getNextSegment(curSegment);
      final SegmentSequence splitSequence = segmentSequence.splitSegment(curSegment, feature);
      if (splitSequence != null) {
        curSegment = nextSegment;
        result = true;
      }
    }

    return result;
  }

  /**
   * Split the current segment with the split function.
   * <p>
   * If successful, set the current segment to be the first new split;
   * otherwise, remain at the current segment.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean splitAndPositionAtFirst(SplitFunction splitFunction) {
    boolean result = false;

    if (curSegment != null) {
      final SegmentSequence splitSequence = segmentSequence.splitSegment(curSegment, splitFunction);
      if (splitSequence != null) {
        curSegment = splitSequence.getSegment(0);
        result = true;
      }
    }

    return result;
  }

  /**
   * Split the current segment with the split function.
   * <p>
   * <p>
   * If successful, set the current segment to be the segment after the
   * new split; otherwise, leave the current segment at its location.
   *
   * @return true if the state has changed; otherwise, false.
   */
  public boolean splitAndPositionAfter(SplitFunction splitFunction) {
    boolean result = false;

    if (curSegment != null) {
      final Segment nextSegment = segmentSequence.getNextSegment(curSegment);
      final SegmentSequence splitSequence = segmentSequence.splitSegment(curSegment, splitFunction);
      if (splitSequence != null) {
        curSegment = nextSegment;
        result = true;
      }
    }

    return result;
  }

  /**
   * Join the current segment with the next segment(s).
   * <p>
   * The current text will be reset to be the newly joined segment's text.
   * The machine will be positioned at the newly joined segment, unless positionAfter is true;
   * in which case, the machine will be positioned after the joined segment.
   */
  public boolean join(int numSegments, boolean ignoreDelimsAndWhitespace, boolean positionAfter) {
    boolean result = false;
    numSkippedSegments = 0;

    for (int i = 0; i < numSegments; ++i) {
      if (curSegment == null) break;
      final Segment removed = segmentSequence.joinSegmentWithNext(curSegment);
      ++numSkippedSegments;

      if (ignoreDelimsAndWhitespace) {
        if (SegmentUtils.isDelimOrWhitespace(removed)) {
          --i;
          --numSkippedSegments;
        }
      }

      result = true;
    }

    resetCurText();

    if (curSegment != null) {
      if (positionAfter) {
        forward(1, false);
      }
      else {
        curText.append(curSegment.getText());
      }
    }

    return result;
  }
}
