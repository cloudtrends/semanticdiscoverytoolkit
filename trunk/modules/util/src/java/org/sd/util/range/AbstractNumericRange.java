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
package org.sd.util.range;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for numeric ranges.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractNumericRange implements NumericRange {

  private static final long serialVersionUID = 42L;


  protected abstract SimpleRange buildRange(String number);
  protected abstract SimpleRange buildRange(String left, boolean leftInclusive, String right, boolean rightInclusive);
  protected abstract SimpleRange buildToleranceRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd);


  private List<SimpleRange> ranges;
  private String string;

  protected AbstractNumericRange() {
    this.ranges = new ArrayList<SimpleRange>();
    this.string = null;
  }

  /**
   * Get the (sorted) list of simple ranges.
   */
  protected final List<SimpleRange> getSimpleRanges() {
    Collections.sort(ranges);
    return ranges;
  }

  /**
   * Determine whether the integer is in this numeric range.
   */
  public boolean includes(int value) {
    boolean result = false;

    for (SimpleRange range : ranges) {
      if (range.includes(value)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether the long is in this numeric range.
   */
  public boolean includes(long value) {
    boolean result = false;

    for (SimpleRange range : ranges) {
      if (range.includes(value)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether the double is in this numeric range.
   */
  public boolean includes(double value) {
    boolean result = false;

    for (SimpleRange range : ranges) {
      if (range.includes(value)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether the value is in this numeric range.
   */
  public boolean includes(String value) {
    boolean result = false;

    for (SimpleRange range : ranges) {
      if (range.includes(value)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Incorporate the value into this range.
   * <p>
   * See AbstractNumericRange.parseValue's javadoc for the format of value.
   */
  public void include(String value) {
    parseValue(value);
  }

  /**
   * Shift all range end points by the given value.
   */
  public void shift(double value) {
    for (SimpleRange range : ranges) {
      range.shift(value);
    }
  }

  /**
   * Get the number of integers included in this range.
   *
   * @return null if the range is infinite; -1 if the number of integers 
   * exceeds the maximum integer value, otherwise the size.
   */
  public Integer size() {
    int size = 0;

    for (SimpleRange range : ranges) {
      final Integer curSize = range.size();
      if (curSize == null) {
        size = 0;
        break;
      }
      else if (curSize == -1) {
        size = -1;
        break;
      }
      size += curSize;
    }

    return (size == 0) ? null : size;
  }

  /**
   * Get this range fully represented as a string.
   */
  public String asString() {
    if (string == null) {
      final StringBuilder result = new StringBuilder();
      final List<SimpleRange> ranges = getSimpleRanges();  // so they're sorted
      for (SimpleRange range : ranges) {
        if (result.length() > 0) result.append(",");
        result.append(range.asString());
      }
      string = result.toString();
    }
    return string;
  }

  public String toString() {
    return asString();
  }

  /**
   * Parse a collection of values. See parseValue.
   */
  protected final void parseValues(Collection<String> values) {
    for (String value : values) {
      parseValue(value);
    }
  }

  /**
   * Parse a collection of values. See parseValue.
   */
  protected final void parseValues(String[] values) {
    for (String value : values) {
      parseValue(value);
    }
  }

  /**
   * Parse a value of the form: (for example)
   * <p>
   * "(a-b],c^d,[e-f)"
   * <p>
   * Where a, b, c, d, e, and f are integers or reals (depending on attributeType) and
   * represent the range of values from a (exclusive) to b (inclusive), c plus or minus d
   * (exclusive), and the values from e (inclusive) to f (exclusive).
   * <p>
   * There can be any number of terms separated by commas.
   * <p>
   * Unbounded ranges are supported such that: (where "a" is non-negative)
   * <p>
   * "a-" means all numbers from a to Integer.MAX_VALUE or Double.POSITIVE_INFINITY.
   * <p>
   * "-a-" means all numbers from negative a to Integer.MAX_VALUE or Double.POSITIVE_INFINITY.
   * <p>
   * "-(a)" means all numbers from Integer.MIN_VALUE or Double.NEGATIVE_INFINITY to (positive) a.
   * <p>
   * "-(-a)" or "--a" means all numbers from Integer.MIN_VALUE or Double.NEGATIVE_INFINITY to negative a.
   * <p>
   * "-" means all numbers from Integer.MIN_VALUE or Double.NEGATIVE_INFINITY to Integer.MAX_VALUE or Double.POSITIVE_INFINITY.
   * <p>
   * Note that:
   * <p>
   * "-a" means just the (negative) number "-a" is in the range.
   * <p>
   * An unbounded low or high always results in includesLow or includesHigh being true
   * regardless of other annotations in the term.
   * <p>
   * A single value sets the low and high to that number and includesLow and includesHigh
   * will be set to true regardless of other annotations in the term.
   */
  protected final void parseValue(String value) {
    final String[] pieces = value.replaceAll("\\s+", "").split(",");

    for (String piece : pieces) {
      parsePiece(piece);
    }
  }

  /**
   * Parse a piece of the form:
   * <p>
   * optional '(' or '['  (default is '[')
   * optional number
   * optional '-' or '^'
   * optional number
   * optional ')' or ']'  (default is ']')
   */
  private final void parsePiece(String piece) {
    int len = piece.length();

    if (len == 0) return;

    // check for inclusive end
    final char lastc = piece.charAt(len - 1);
    final boolean inclusiveEnd = (lastc != ')');  // default to inclusive if not explicitly exclusive
    if (lastc == ')' || lastc == ']') --len;

    // check for inclusive start
    int index = 0;
    final char firstc = piece.charAt(index);
    final boolean inclusiveStart = (firstc != '(');  // default to inclusive if not explicitly exclusive
    if (firstc == '(' || firstc == '[') ++index;

    int markerIndex = -1;
    char marker = (char)0;

    if (firstc == '-') {
      marker = '-';

      if (len == 1) {
        // range is negative to positive infinity!
        markerIndex = 0;
      }
      else {  // len > 1
        // check for "-(" which indicates a range from negative infinity to a number.
        final char secondc = piece.charAt(1);
        if (secondc == '(' || secondc == '[') {
          markerIndex = 1;
          ++index;
        }
        else if (secondc == '-') {
          // have "--a", which is negative infinity to negative a.
          markerIndex = 0;
          // leave index at 0.
        }
        else {
          markerIndex = findMarker(piece, 1);
          marker = (markerIndex > index) ? piece.charAt(markerIndex) : (char)0;
        }
      }
    }
    else {
      // check for range ('-') or tolerance ('^') marker
      markerIndex = findMarker(piece, index);
      if (markerIndex > index) marker = piece.charAt(markerIndex);
    }

    SimpleRange newRange = null;

    if (markerIndex < 0) {
      // no marker, just a number.
      final String number = piece.substring(index, len);
      newRange = buildRange(number);
    }
    else {
      final String preMarkerString = piece.substring(index, markerIndex);
      final String postMarkerString = piece.substring(markerIndex + 1, len);

      if (marker == '-') {
        newRange = buildRange(preMarkerString, inclusiveStart, postMarkerString, inclusiveEnd);
      }
      else if (marker == '^') {
        newRange = buildToleranceRange(preMarkerString, postMarkerString, inclusiveStart, inclusiveEnd);
      }
    }

    if (newRange != null) {
      incorporateRange(newRange);
    }
  }

  /**
   * Find the index of a marker ('-' or '^') after index.
   */
  private final int findMarker(String piece, int index) {
    int result = -1;

    ++index;  // look AFTER the index

    int dashPos = piece.indexOf('-', index);
    int caretPos = piece.indexOf('^', index);

    if (dashPos < 0) {
      result = caretPos;
    }
    else if (caretPos < 0) {
      result = dashPos;
    }
    else {  // have both so only the caret could be meaningful
      result = caretPos;
    }

    return result;
  }

  protected final void addRange(SimpleRange newRange, boolean combineIfContiguous) {
    if (combineIfContiguous) {
      incorporateRange(newRange);
    }
    else {
      addNonDuplicate(newRange);
    }
  }

  private final void incorporateRange(SimpleRange newRange) {
    SimpleRange combined = null;
    for (SimpleRange range : ranges) {
      if (range.getUnionIfContinuous(newRange)) {
        combined = range;
        break;
      }
    }

    if (combined == null) {
      ranges.add(newRange);
    }
    else {
      // combining ranges may have brought two ranges together now. try to collapse.
      collapse(combined);
    }
  }

  private final void addNonDuplicate(SimpleRange newRange) {
    boolean isDup = false;
    for (SimpleRange range : ranges) {
      if (range.compareTo(newRange) == 0) {
        isDup = true;
      }
    }
    if (!isDup) {
      ranges.add(newRange);
    }
  }

  private final void collapse(SimpleRange existingRange) {
    ranges.remove(existingRange);
    incorporateRange(existingRange);
  }


  protected interface SimpleRange extends NumericRange, Comparable<SimpleRange> {
    /**
     * Incorporate the other range's values into this range if this range
     * combined with the other range is continuous.
     */
    public boolean getUnionIfContinuous(SimpleRange other);

    /**
     * Test whether this range includes its low value.
     */
    public boolean includesLow();

    /**
     * Test whether this range includes its high value.
     */
    public boolean includesHigh();

    /**
     * Get this range's low value (as a string).
     */
    public String getLow();

    /**
     * Get this range's high value (as a string).
     */
    public String getHigh();

    /**
     * Get the low value as an int.
     */
    public int getLowAsInt(boolean round);

    /**
     * Get the low value as an long.
     */
    public long getLowAsLong(boolean round);

    /**
     * Get the low value as a double.
     */
    public double getLowAsDouble();

    /**
     * Get the high value as an int.
     */
    public int getHighAsInt(boolean round);

    /**
     * Get the high value as an long.
     */
    public long getHighAsLong(boolean round);

    /**
     * Get the high value as a double.
     */
    public double getHighAsDouble();

    /**
     * Shift this range's bounds by the given value.
     */
    public void shift(double value);
  }
}
