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
import java.util.Collection;
import java.util.TreeSet;

/**
 * Class to represent a set of longs.
 * <p>
 * @author Spence Koehler
 */
public class LongRange extends AbstractNumericRange implements Serializable {

  private static final long serialVersionUID = 42L;

  /**
   * Construct an empty instance.
   */
  public LongRange() {
    super();
  }

  public LongRange(Collection<String> values) {
    super();
    super.parseValues(values);
  }

  public LongRange(String[] values) {
    super();
    super.parseValues(values);
  }

  public LongRange(String value) {
    super();
    super.parseValue(value);
  }

  public LongRange(long number) {
    super();
    add(number);
  }

  public LongRange(long left, boolean leftInclusive, long right, boolean rightInclusive) {
    super();
    add(left, leftInclusive, right, rightInclusive);
  }

  public LongRange(long lowInclusive, long highInclusive) {
    super();
    add(lowInclusive, highInclusive);
  }

  public LongRange(long base, long tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    super();
    add(base, tolerance, inclusiveStart, inclusiveEnd);
  }

  public long getLow() {
    boolean didFirst = false;
    long result = 0;

    for (SimpleRange simpleRange : getSimpleRanges()) {
      if (!didFirst || simpleRange.getLowAsLong(true) < result) {
        result = simpleRange.getLowAsLong(true);
        didFirst = true;
      }
    }

    return result;
  }

  public long getHigh() {
    boolean didFirst = false;
    long result = 0;

    for (SimpleRange simpleRange : getSimpleRanges()) {
      if (!didFirst || simpleRange.getHighAsLong(true) > result) {
        result = simpleRange.getHighAsLong(true);
        didFirst = true;
      }
    }

    return result;
  }

  /**
   * Determine whether the other long range is included in this long range.
   */
  public boolean includes(LongRange other) {
    boolean result = false;

    final TreeSet<Long> myValues = this.getValues();
    final TreeSet<Long> otherValues = other.getValues();

    if (myValues == null) {
      result = true;
    }
    else if (otherValues != null) {
      if (myValues.size() >= otherValues.size()) {
        for (Long otherValue : otherValues) {
          if (!myValues.contains(otherValue)) {
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Compare this long range to the other with return values:
   * <ul>
   * <li>-1: If this range's low is less than the other range's low or this range's high is less than the other range's high</li>
   * <li> 0: If this range is the same as or includes all of the other's values, including if both are infinite</li>
   * <li> 1: If the other range's low is less than this range's low or the other range's high is less than this range's high</li>
   * </ul>
   */
  public int compareTo(LongRange other) {
    int result = 0;

    if (this.getLow() < other.getLow() && this.getHigh() < other.getHigh()) {
      result = -1;
    }
    else if (other.getLow() < this.getLow() && other.getHigh() < this.getHigh()) {
      result = 1;
    }

    return result;
  }

  /**
   * Get each of this range's values, unless it is infinite.
   *
   * @return the discrete values or null.
   */
  public TreeSet<Long> getValues() {
    TreeSet<Long> result = new TreeSet<Long>();

    for (SimpleRange range : getSimpleRanges()) {
      final Integer curSize = range.size();
      if (curSize == null) {
        result = null;
        break;
      }
      else {
        long low = range.getLowAsLong(false);
        if (!range.includesLow()) ++low;
        long high = range.getHighAsLong(false);
        if (!range.includesHigh()) --high;
        for (long i = low; i <= high; ++i) {
          result.add(i);
        }
      }
    }

    return result;
  }

  public final LongRange add(long singleValue) {
    return add(singleValue, true);
  }

  public final LongRange add(long singleValue, boolean combineIfContiguous) {
    super.addRange(new SimpleLongRange(singleValue), combineIfContiguous);
    return this;
  }

  public final LongRange add(long lowInclusive, long highInclusive) {
    return add(lowInclusive, highInclusive, true);
  }

  public final LongRange add(long lowInclusive, long highInclusive, boolean combineIfContiguous) {
    super.addRange(new SimpleLongRange(lowInclusive, true, highInclusive, true), combineIfContiguous);
    return this;
  }

  public final LongRange add(long left, boolean leftInclusive, long right, boolean rightInclusive) {
    return add(left, leftInclusive, right, rightInclusive, true);
  }

  public final LongRange add(long left, boolean leftInclusive, long right, boolean rightInclusive, boolean combineIfContiguous) {
    super.addRange(new SimpleLongRange(left, leftInclusive, right, rightInclusive), combineIfContiguous);
    return this;
  }

  public final LongRange add(long base, long tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    return add(base, tolerance, inclusiveStart, inclusiveEnd, true);
  }

  public final LongRange add(long base, long tolerance, boolean inclusiveStart, boolean inclusiveEnd, boolean combineIfContiguous) {
    super.addRange(new SimpleLongRange(base, tolerance, inclusiveStart, inclusiveEnd), combineIfContiguous);
    return this;
  }

  protected SimpleRange buildRange(String number) {
    return new SimpleLongRange(number);
  }

  protected SimpleRange buildRange(String left, boolean leftInclusive, String right, boolean rightInclusive) {
    return new SimpleLongRange(left, leftInclusive, right, rightInclusive);
  }

  protected SimpleRange buildToleranceRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    return new SimpleLongRange(base, tolerance, inclusiveStart, inclusiveEnd);
  }

  private final class SimpleLongRange implements SimpleRange, Serializable  {

    private static final long serialVersionUID = 42L;

    private long low;
    private long high;
    private boolean includeLow;
    private boolean includeHigh;
    private String string;

    SimpleLongRange(String number) {
      this.low = "".equals(number) ? Long.MIN_VALUE : Long.parseLong(number);
      this.high = "".equals(number) ? Long.MAX_VALUE : this.low;
      this.includeLow = true;
      this.includeHigh = true;
      this.string = number;
    }

    SimpleLongRange(String left, boolean leftInclusive, String right, boolean rightInclusive) {
      init("".equals(left) ? Long.MIN_VALUE : Long.parseLong(left),
           "".equals(left) ? true : leftInclusive,
           "".equals(right) ? Long.MAX_VALUE : Long.parseLong(right),
           "".equals(right) ? true : rightInclusive);
    }

    SimpleLongRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
      final long baseValue = Long.parseLong(base);
      final long toleranceValue = "".equals(tolerance) ? 0 : Long.parseLong(tolerance);

      init(baseValue - toleranceValue, inclusiveStart, baseValue + toleranceValue, inclusiveEnd);
    }

    SimpleLongRange(long number) {
      init(number, true, number, true);
    }

    SimpleLongRange(long left, boolean leftInclusive, long right, boolean rightInclusive) {
      init(left, leftInclusive, right, rightInclusive);
    }

    SimpleLongRange(long base, long tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
      init(base - tolerance, inclusiveStart, base + tolerance, inclusiveEnd);
    }

    private final void init(long low, boolean includeLow, long high, boolean includeHigh) {
      this.low = low;
      this.includeLow = includeLow;
      this.high = high;
      this.includeHigh = includeHigh;
      this.string = null;  // lazy load

      if (low == high) {
        this.includeLow = true;
        this.includeHigh = true;
      }
      else if (low > high) {
        // swap left/right
        final long lowValue = this.low;
        this.low = this.high;
        this.high = lowValue;

        final boolean includeLowValue = this.includeLow;
        this.includeLow = this.includeHigh;
        this.includeHigh = includeLowValue;
      }
    }

    public int compareTo(SimpleRange other) {
      long result = low - other.getLowAsLong(true);
      if (result == 0) {
        result = high - other.getHighAsLong(false);
      }

      if(result < Integer.MIN_VALUE)
        return Integer.MIN_VALUE;
      else if(result > Integer.MAX_VALUE)
        return Integer.MAX_VALUE;
      else
        return (int)result;
    }

    /**
     * Determine whether the int is in this numeric range.
     */
    public boolean includes(int value) {
      boolean result = (value > low && value < high);

      if (!result) {
        if (includeLow && value == low) result = true;
        else if (includeHigh && value == high) result = true;
      }

      return result;
    }

    /**
     * Determine whether the long is in this numeric range.
     */
    public boolean includes(long value) {
      boolean result = (value > low && value < high);

      if (!result) {
        if (includeLow && value == low) result = true;
        else if (includeHigh && value == high) result = true;
      }

      return result;
    }

    /**
     * Determine whether the double is in this numeric range.
     */
    public boolean includes(double value) {
      boolean result = (value > low && value < high);

      if (!result) {
        if (includeLow && value == low) result = true;
        else if (includeHigh && value == high) result = true;
      }

      return result;
    }

    /**
     * Determine whether the value is in this numeric range.
     */
    public boolean includes(String value) {
      boolean result = false;
      try {
        result = includes(Long.parseLong(value));
      }
      catch (NumberFormatException e) {
        // non-numeric isn't in range. result is already false.
      }
      return result;
    }

    /**
     * Incorporate the value into this range.
     * <p>
     * See AbstractNumericRange.parseValue's javadoc for the format of value.
     */
    public void include(String value) {
      throw new UnsupportedOperationException("If this is needed, implement it.");
    }

    /**
     * Incorporate the other range's values into this range if this range
     * combined with the other range is continuous.
     */
    public boolean getUnionIfContinuous(SimpleRange other) {
      boolean result = false;

      final long otherLowAdj = other.includesLow() ? 1 : 0;
      final long thisLowAdj = this.includesLow() ? 1 : 0;
      final long otherHighAdj = other.includesHigh() ? 1 : 0;
      final long thisHighAdj = this.includesHigh() ? 1 : 0;

      if (this.includes(other.getLowAsLong(false) - otherLowAdj) || other.includes(this.getHighAsLong(false) + thisHighAdj) ||
          other.includes(this.getLowAsLong(false) - thisLowAdj) || this.includes(other.getHighAsLong(false) + otherHighAdj)) {

        // range extends from min low to max high
        mergeIn(other);
        result = true;
      }

      if (result) string = null;  // need to recompute.

      return result;
    }

    private final void mergeIn(SimpleRange other) {
      final long otherLowR = other.getLowAsLong(true);
      final long otherHighR = other.getHighAsLong(true);
      final long otherLowT = other.getLowAsLong(false);
      final long otherHighT = other.getHighAsLong(false);

      if (otherLowR < this.low) {
        this.low = otherLowR;
        this.includeLow = other.includesLow();
      }
      else if (otherLowR == this.low && otherLowT == this.low && other.includesLow()) {
        this.includeLow = true;
      }
      if (otherHighT > this.high) {
        this.high = otherHighT;
        this.includeHigh = other.includesHigh();
      }
      else if (otherHighT == this.high && otherHighR == this.high && other.includesHigh()) {
        this.includeHigh = true;
      }
    }

    /**
     * Test whether this range includes its low value.
     */
    public boolean includesLow() {
      return includeLow;
    }

    /**
     * Test whether this range includes its high value.
     */
    public boolean includesHigh() {
      return includeHigh;
    }

    /**
     * Get this range's low value (as a string).
     */
    public String getLow() {
      return Long.toString(low);
    }

    /**
     * Get this range's high value (as a string).
     */
    public String getHigh() {
      return Long.toString(high);
    }

    /**
     * Get the number of integers included in this range.
     *
     * @return null if the range is infinite; -1 if the number of integers 
     * exceeds the maximum integer value, otherwise the size.
     */
    public Integer size() {
      if (low == Long.MIN_VALUE || high == Long.MAX_VALUE) return null;

      long result = high - low - 1;
      if (includeLow) ++result;
      if (includeHigh) ++result;

      if(result > Integer.MAX_VALUE)
        return new Integer(-1);
      else
        return new Integer((int)result);
    }

    /**
     * Get the number of longs included in this range.
     *
     * @return null if the range is infinite; otherwise the size.
     */
    public Long longSize() {
      if (low == Long.MIN_VALUE || high == Long.MAX_VALUE) return null;

      long result = high - low - 1;
      if (includeLow) ++result;
      if (includeHigh) ++result;

      return result;
    }

    /**
     * Get this range fully represented as a string.
     */
    public String asString() {
      if (string == null) {
        if (low == high) {
          string = getLow();
        }
        else {
          final StringBuilder result = new StringBuilder();

          final boolean defaultInclusion = includeLow && includeHigh;

          // handle unbounded ranges
          if (low == Long.MIN_VALUE || high == Long.MAX_VALUE) {
            if (low == Long.MIN_VALUE && high == Long.MAX_VALUE) {
              result.append('-');
            }
            else if (low == Long.MIN_VALUE) {
              result.
                append("-(").
                append(high).
                append(includeHigh ? ']' : ')');
            }
            else {  // high == Long.MAX_VALUE
              if (!defaultInclusion) result.append(includeLow ? '[' : '(');
              result.
                append(low).
                append('-');
            }
          }
          else {
            if (!defaultInclusion) result.append(includeLow ? '[' : '(');
            result.
              append(low).
              append('-').
              append(high);
            if (!defaultInclusion) result.append(includeHigh ? ']' : ')');
          }

          string = result.toString();
        }
      }
      return string;
    }

    public String toString() {
      return asString();
    }

    /**
     * Get the low value as an int.
     */
    public int getLowAsInt(boolean round) {
      if(low < Integer.MIN_VALUE)
        throw new IllegalArgumentException("Unable to convert long("+low+") to int");
      else 
        return (int)low;
    }

    /**
     * Get the low value as an long.
     */
    public long getLowAsLong(boolean round) {
      return low;
    }

    /**
     * Get the low value as a double.
     */
    public double getLowAsDouble() {
      return low;
    }

    /**
     * Get the low value as an int.
     */
    public int getHighAsInt(boolean round) {
      if(high > Integer.MAX_VALUE)
        throw new IllegalArgumentException("Unable to convert long("+high+") to int");
      else 
        return (int)high;
    }

    /**
     * Get the high value as an long.
     */
    public long getHighAsLong(boolean round) {
      return high;
    }

    /**
     * Get the high value as a double.
     */
    public double getHighAsDouble() {
      return high;
    }

    /**
     * Shift this range's bounds by the given value.
     */
    public void shift(double value) {
      string = null;  // need to recompute
      this.low += (long)value;
      this.high += (long)value;
    }
  }
}
