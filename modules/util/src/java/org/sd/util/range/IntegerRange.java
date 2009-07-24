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


import java.util.Collection;

/**
 * Class to represent a set of integers.
 * <p>
 * @author Spence Koehler
 */
public class IntegerRange extends AbstractNumericRange {

  public IntegerRange(Collection<String> values) {
    super();
    super.parseValues(values);
  }

  public IntegerRange(String[] values) {
    super();
    super.parseValues(values);
  }

  public IntegerRange(String value) {
    super();
    super.parseValue(value);
  }

  public IntegerRange(int number) {
    super();
    super.addRange(new SimpleIntegerRange(number));
  }

  public IntegerRange(int left, boolean leftInclusive, int right, boolean rightInclusive) {
    super();
    super.addRange(new SimpleIntegerRange(left, leftInclusive, right, rightInclusive));
  }

  public IntegerRange(int base, int tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    super();
    super.addRange(new SimpleIntegerRange(base, tolerance, inclusiveStart, inclusiveEnd));
  }

  public int getLow() {
    boolean didFirst = false;
    int result = 0;

    for (SimpleRange simpleRange : getSimpleRanges()) {
      if (!didFirst || simpleRange.getLowAsInt(true) < result) {
        result = simpleRange.getLowAsInt(true);
        didFirst = true;
      }
    }

    return result;
  }

  public int getHigh() {
    boolean didFirst = false;
    int result = 0;

    for (SimpleRange simpleRange : getSimpleRanges()) {
      if (!didFirst || simpleRange.getHighAsInt(true) > result) {
        result = simpleRange.getHighAsInt(true);
        didFirst = true;
      }
    }

    return result;
  }

  protected SimpleRange buildRange(String number) {
    return new SimpleIntegerRange(number);
  }

  protected SimpleRange buildRange(String left, boolean leftInclusive, String right, boolean rightInclusive) {
    return new SimpleIntegerRange(left, leftInclusive, right, rightInclusive);
  }

  protected SimpleRange buildToleranceRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    return new SimpleIntegerRange(base, tolerance, inclusiveStart, inclusiveEnd);
  }

  private final class SimpleIntegerRange implements SimpleRange {
    private int low;
    private int high;
    private boolean includeLow;
    private boolean includeHigh;
    private String string;

    SimpleIntegerRange(String number) {
      this.low = "".equals(number) ? Integer.MIN_VALUE : Integer.parseInt(number);
      this.high = "".equals(number) ? Integer.MAX_VALUE : this.low;
      this.includeLow = true;
      this.includeHigh = true;
      this.string = number;
    }

    SimpleIntegerRange(String left, boolean leftInclusive, String right, boolean rightInclusive) {
      init("".equals(left) ? Integer.MIN_VALUE : Integer.parseInt(left),
           "".equals(left) ? true : leftInclusive,
           "".equals(right) ? Integer.MAX_VALUE : Integer.parseInt(right),
           "".equals(right) ? true : rightInclusive);
    }

    SimpleIntegerRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
      final int baseValue = Integer.parseInt(base);
      final int toleranceValue = "".equals(tolerance) ? 0 : Integer.parseInt(tolerance);

      init(baseValue - toleranceValue, inclusiveStart, baseValue + toleranceValue, inclusiveEnd);
    }

    SimpleIntegerRange(int number) {
      init(number, true, number, true);
    }

    SimpleIntegerRange(int left, boolean leftInclusive, int right, boolean rightInclusive) {
      init(left, leftInclusive, right, rightInclusive);
    }

    SimpleIntegerRange(int base, int tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
      init(base - tolerance, inclusiveStart, base + tolerance, inclusiveEnd);
    }

    private final void init(int low, boolean includeLow, int high, boolean includeHigh) {
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
        final int lowValue = this.low;
        this.low = this.high;
        this.high = lowValue;

        final boolean includeLowValue = this.includeLow;
        this.includeLow = this.includeHigh;
        this.includeHigh = includeLowValue;
      }
    }

    public int compareTo(SimpleRange other) {
      int result = low - other.getLowAsInt(true);
      if (result == 0) {
        result = high - other.getHighAsInt(false);
      }
      return result;
    }

    /**
     * Determine whether the integer is in this numeric range.
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
        result = includes(Integer.parseInt(value));
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

      final int otherLowAdj = other.includesLow() ? 1 : 0;
      final int thisLowAdj = this.includesLow() ? 1 : 0;
      final int otherHighAdj = other.includesHigh() ? 1 : 0;
      final int thisHighAdj = this.includesHigh() ? 1 : 0;

      if (this.includes(other.getLowAsInt(false) - otherLowAdj) || other.includes(this.getHighAsInt(false) + thisHighAdj) ||
          other.includes(this.getLowAsInt(false) - thisLowAdj) || this.includes(other.getHighAsInt(false) + otherHighAdj)) {

        // range extends from min low to max high
        mergeIn(other);
        result = true;
      }

      if (result) string = null;  // need to recompute.

      return result;
    }

    private final void mergeIn(SimpleRange other) {
      final int otherLowR = other.getLowAsInt(true);
      final int otherHighR = other.getHighAsInt(true);
      final int otherLowT = other.getLowAsInt(false);
      final int otherHighT = other.getHighAsInt(false);

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
      return Integer.toString(low);
    }

    /**
     * Get this range's high value (as a string).
     */
    public String getHigh() {
      return Integer.toString(high);
    }

    /**
     * Get the number of integers included in this range.
     *
     * @return null if the range is infinite; otherwise the size.
     */
    public Integer size() {
      if (low == Integer.MIN_VALUE || high == Integer.MAX_VALUE) return null;

      int result = high - low - 1;
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
          if (low == Integer.MIN_VALUE || high == Integer.MAX_VALUE) {
            if (low == Integer.MIN_VALUE && high == Integer.MAX_VALUE) {
              result.append('-');
            }
            else if (low == Integer.MIN_VALUE) {
              result.
                append("-(").
                append(high).
                append(includeHigh ? ']' : ')');
            }
            else {  // high == Integer.MAX_VALUE
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
      return low;
    }

    /**
     * Get the low value as a double.
     */
    public double getLowAsDouble() {
      return low;
    }

    /**
     * Get the high value as an int.
     */
    public int getHighAsInt(boolean round) {
      return high;
    }

    /**
     * Get the high value as a double.
     */
    public double getHighAsDouble() {
      return high;
    }
  }
}
