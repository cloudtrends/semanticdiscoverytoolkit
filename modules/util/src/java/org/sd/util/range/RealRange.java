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
public class RealRange extends AbstractNumericRange {

  public RealRange(Collection<String> values) {
    super();
    super.parseValues(values);
  }

  public RealRange(String[] values) {
    super();
    super.parseValues(values);
  }

  public RealRange(String value) {
    super();
    super.parseValue(value);
  }

  public RealRange(double number) {
    super();
    super.addRange(new SimpleRealRange(number), true);
  }

  public RealRange(double left, boolean leftInclusive, double right, boolean rightInclusive) {
    super();
    super.addRange(new SimpleRealRange(left, leftInclusive, right, rightInclusive), true);
  }

  public RealRange(double base, double tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    super();
    super.addRange(new SimpleRealRange(base, tolerance, inclusiveStart, inclusiveEnd), true);
  }

  protected SimpleRange buildRange(String number) {
    return new SimpleRealRange(number);
  }

  protected SimpleRange buildRange(String left, boolean leftInclusive, String right, boolean rightInclusive) {
    return new SimpleRealRange(left, leftInclusive, right, rightInclusive);
  }

  protected SimpleRange buildToleranceRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
    return new SimpleRealRange(base, tolerance, inclusiveStart, inclusiveEnd);
  }

  private final class SimpleRealRange implements SimpleRange {
    private double low;
    private double high;
    private boolean includeLow;
    private boolean includeHigh;
    private String string;

    SimpleRealRange(String number) {
      this.low = "".equals(number) ? Double.NEGATIVE_INFINITY : Double.parseDouble(number);
      this.high = "".equals(number) ? Double.POSITIVE_INFINITY : this.low;
      this.includeLow = true;
      this.includeHigh = true;
      this.string = number;
    }

    SimpleRealRange(String left, boolean leftInclusive, String right, boolean rightInclusive) {
      init("".equals(left) ? Double.NEGATIVE_INFINITY : Double.parseDouble(left),
           "".equals(left) ? true : leftInclusive,
           "".equals(right) ? Double.POSITIVE_INFINITY : Double.parseDouble(right),
           "".equals(right) ? true : rightInclusive);
    }

    SimpleRealRange(String base, String tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
      final double baseValue = Double.parseDouble(base);
      final double toleranceValue = "".equals(tolerance) ? 0.0 : Double.parseDouble(tolerance);

      init(baseValue - toleranceValue, inclusiveStart, baseValue + toleranceValue, inclusiveEnd);
    }

    SimpleRealRange(double number) {
      init(number, true, number, true);
    }

    SimpleRealRange(double left, boolean leftInclusive, double right, boolean rightInclusive) {
      init(left, leftInclusive, right, rightInclusive);
    }

    SimpleRealRange(double base, double tolerance, boolean inclusiveStart, boolean inclusiveEnd) {
      init(base - tolerance, inclusiveStart, base + tolerance, inclusiveEnd);
    }

    private final void init(double low, boolean includeLow, double high, boolean includeHigh) {
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
        final double lowValue = this.low;
        this.low = this.high;
        this.high = lowValue;

        final boolean includeLowValue = this.includeLow;
        this.includeLow = this.includeHigh;
        this.includeHigh = includeLowValue;
      }
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
        result = includes(Double.parseDouble(value));
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

      if (this.includes(other.getLowAsDouble()) || other.includes(this.getHighAsDouble()) ||
        other.includes(this.getLowAsDouble()) || this.includes(other.getHighAsDouble())) {
        // range extends from min low to max high
        mergeIn(other);
        result = true;
      }

      if (result) string = null;  // need to recompute.

      return result;
    }

    private final void mergeIn(SimpleRange other) {
      final double otherLow = other.getLowAsDouble();
      final double otherHigh = other.getHighAsDouble();

      if (otherLow < this.low) {
        this.low = otherLow;
        this.includeLow = other.includesLow();
      }
      else if (otherLow == this.low && other.includesLow()) {
        this.includeLow = true;
      }
      if (otherHigh > this.high) {
        this.high = otherHigh;
        this.includeHigh = other.includesHigh();
      }
      else if (otherHigh == this.high && other.includesHigh()) {
        this.includeHigh = true;
      }
    }

    public int compareTo(SimpleRange other) {
      int result = low < other.getLowAsDouble() ? -1 : low > other.getLowAsDouble() ? 1 : 0;
      if (result == 0) {
        result = high < other.getHighAsDouble() ? -1 : high > other.getHighAsDouble() ? 1 : 0;
      }
      return result;
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
      String result = Double.toString(low);
      if (result.endsWith(".0")) result = result.substring(0, result.length() - 2);
      return result;
    }

    /**
     * Get this range's high value (as a string).
     */
    public String getHigh() {
      String result = Double.toString(high);
      if (result.endsWith(".0")) result = result.substring(0, result.length() - 2);
      return result;
    }

    /**
     * Get the number of integers included in this range.
     *
     * @return null if the range is infinite; otherwise the size.
     */
    public Integer size() {
      if (low == Double.NEGATIVE_INFINITY || high == Double.POSITIVE_INFINITY) return null;

      int ilow = (int)Math.ceil(low);
      int ihigh = (int)Math.floor(high);

      int result = ihigh - ilow - 1;
      if (includes(ilow)) ++result;
      if (includes(ihigh)) ++result;

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

          // handle unbound ranges
          if (low == Double.NEGATIVE_INFINITY || high == Double.POSITIVE_INFINITY) {
            if (low == Double.NEGATIVE_INFINITY && high == Double.POSITIVE_INFINITY) {
              result.append('-');
            }
            else if (low == Double.NEGATIVE_INFINITY) {
              result.
                append("-(").
                append(high).
                append(includeHigh ? ']' : ')');
            }
            else {  // high == Double.POSITIVE_INFINITY
              if (!defaultInclusion) result.append(includeLow ? '[' : '(');
              result.
                append(low).
                append('-');
            }
          }
          else {
            if (!defaultInclusion) result.append(includeLow ? '[' : '(');

            if (high - low < 2.0) {
              final double base = low + (high - low) / 2.0;
              final double tol = base - low;
            
              // emit tolerance form
              result.
                append(base).
                append('^').
                append(tol);
            }
            else {
              // emit range form
              result.
                append(low).
                append('-').
                append(high);
            }

            if (!defaultInclusion) result.append(includeHigh ? ']' : ')');
          }

          string = result.toString().replaceAll("\\.0\\b", "");
        }
      }
      return string;
    }

    /**
     * Get the low value as an int.
     */
    public int getLowAsInt(boolean round) {
      return round ? (int)Math.ceil(low) : (int)low;
    }

    /**
     * Get the low value as an long.
     */
    public long getLowAsLong(boolean round) {
      return round ? (long)Math.ceil(low) : (long)low;
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
      return round ? (int)Math.floor(high) : (int)high;
    }

    /**
     * Get the high value as an long.
     */
    public long getHighAsLong(boolean round) {
      return round ? (long)Math.floor(high) : (long)high;
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
      this.low += value;
      this.high += value;
    }
  }
}
