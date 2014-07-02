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
package org.sd.util;


import java.beans.ConstructorProperties;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * A helper class to collect statistical samples and provide summary
 * statistics at any time.
 * <p>
 * This class is designed to have a low memory footprint profile at the expense
 * of losing the samples. The effect is that summary statistics that require
 * analyzing each sample (like median) are unavailable.
 *
 * @author Spence Koehler
 */
public class StatsAccumulator implements Serializable {

  private long n;
  private double sos;
  private double sum;
  private double min;
  private double max;
  private String label;

  private static final long serialVersionUID = 42L;

  /**
   * Default constructor.
   */
  public StatsAccumulator() {
    // initialize labels to empty string rather than null so that we can safely write them to output streams in all cases
    this("");
  }

  /**
   * Construct with the given label.
   */
  public StatsAccumulator(String label) {
    clear(label);
  }

  /**
   * Construct a statsAccumulator w/the following characteristics.
   */
  @ConstructorProperties({"label", "n", "min", "max", "mean", "standardDeviation"})
  public StatsAccumulator(String label, long n, double min, double max, double mean, double stddev) {
    this.label = label;
    this.n = n;
    this.min = min;
    this.max = max;
    this.sum = mean * n;
    this.sos = stddev * stddev * (n - 1.0) + sum * sum / n;
  }

  /**
   * Copy constructor.
   */
  public StatsAccumulator(StatsAccumulator other) {
    this.label = other.label;
    this.n = other.n;
    this.min = other.min;
    this.max = other.max;
    this.sum = other.sum;
    this.sos = other.sos;
  }

  /**
   * Clear (reset/zero) this instance to begin anew.
   */
  public void clear() {
    this.n = 0;
    this.sos = 0.0;
    this.sum = 0.0;
    this.min = 0.0;
    this.max = 0.0;
  }

  /**
   * Clear this instance and reset its label.
   */
  public void clear(String label) {
    clear();
    this.label = label;
  }

  public String toString() {
    StringBuffer result = new StringBuffer();

    result.append((label == null) ? "stat" : label);
    result.append("[n=");
    result.append(n);
    result.append(",mean=");
    result.append(getMean());
    result.append(",min=");
    result.append(min);
    result.append(",max=");
    result.append(max);
    result.append(",sum=");
    result.append(sum);
    result.append(']');

    return result.toString();
  }

  /**
   * Add a sampled value to this instance.
   */
  public void add(double value) {
    if (n == 0) {
      this.min = value;
      this.max = value;
    }
    else {
      if (value < min) {
        this.min = value;
      }
      if (value > max) {
        this.max = value;
      }
    }

    ++n;
    sos += (value * value);
    sum += value;
  }

  /**
   * Get this statsAccumulator's label (possibly null).
   */
  public String getLabel() {
    return label;
  }

  /**
   * Get the number of sampled values.
   */
  public long getN() {
    return n;
  }

  /**
   * Get the sum of all sampled values.
   */
  public double getSum() {
    return sum;
  }

  /**
   * Get the sum of squares of all samples values.
   * <p>
   * Note this is sum(x*x), not sum(x)*sum(x).
   */
  public double getSumOfSquares() {
    return sos;
  }

  /**
   * Get the minimum sampled value recorded.
   */
  public double getMin() {
    return min;
  }

  /**
   * Get the maximum sampled value recorded.
   */
  public double getMax() {
    return max;
  }

  /**
   * Get the average of the recorded sampled values.
   */
  public double getMean() {
    return sum / n;
  }

  /**
   * Get the standard deviation of the recorded sampled values.
   */
  public double getStandardDeviation() {
    double variance = getVariance();
    return Math.sqrt(variance);
  }

  /**
   * Get the variance of the recorded sampled values.
   */
  public double getVariance() {
    double dn = n;
    return (1.0 / (dn - 1.0)) * (sos - (1.0 / dn) * sum * sum);
  }

  /**
   * Serializes this object to the specified DataOutput.
   *
   * @param dataOutput DataOutput to write to
   * @throws IOException
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeUTF(label == null ? "" : label);
    dataOutput.writeLong(n);
    dataOutput.writeDouble(sum);
    dataOutput.writeDouble(sos);
    dataOutput.writeDouble(min);
    dataOutput.writeDouble(max);
  }

  /**
   * Deserialize the object from the specified DataInput.
   *
   * @param dataInput dataInput
   * @return this instance
   * @throws IOException
   */
  public StatsAccumulator readFields(DataInput dataInput) throws IOException {
    label = dataInput.readUTF();
    if (label.length() == 0) {
      label = null;
    }
    n = dataInput.readLong();
    sum = dataInput.readDouble();
    sos = dataInput.readDouble();
    min = dataInput.readDouble();
    max = dataInput.readDouble();

    return this;
  }

  /**
   * Summarize the (mean) data from multiple statsAccumulators.
   *
   * @param label    The label for the new statsAccumulator (okay if null).
   * @param statsAccumulators The statsAccumulators to summarize.
   * @return a statsAccumulator that summarizes the means of the given statsAccumulators.
   */
  public static StatsAccumulator summarize(String label, StatsAccumulator[] statsAccumulators) {
    StatsAccumulator result = new StatsAccumulator(label);

    for (int i = 0; i < statsAccumulators.length; ++i) {
      result.add(statsAccumulators[i].getMean());
    }

    return result;
  }

  /**
   * Add the other statsAccumulator's data to this summary statsAccumulator.
   * <p>
   * Note that it doesn't make sense for this statsAccumulator to be aggregating data at
   * the same level as the other statsAccumulator. In other words, the result of:
   * <p>
   * c1.summarizeWith(c2)
   * <p>
   * is NOT the same as:
   * <p>
   * cs = new StatsAccumulator(); cs.summarizeWith(c1); cs.summarizeWith(c2);
   * <p>
   */
  public void summarizeWith(StatsAccumulator other) {
    add(other.getMean());
  }

  /**
   * Combine the data from multiple statsAccumulators.
   * <p>
   * This behaves as though a single statsAccumulator accumulated all of the data from
   * the given statsAccumulators in the first place. This differs from summarize in
   * that all of the min, max, and sum data is preserved instead of summarizing
   * the means of the statsAccumulators.
   *
   * @param label    The label for the new statsAccumulator (okay if null).
   * @param statsAccumulators The statsAccumulators to combine.
   * @return a statsAccumulator that combines the data of the given statsAccumulators.
   */
  public static StatsAccumulator combine(String label, StatsAccumulator[] statsAccumulators) {
    StatsAccumulator result = new StatsAccumulator(label);
    result.doCombine(statsAccumulators);
    return result;
  }

  /**
   * Combine this statsAccumulator with all the others.
   */
  private void doCombine(StatsAccumulator[] statsAccumulators) {
    for (int i = 0; i < statsAccumulators.length; ++i) {
      final StatsAccumulator curStatsAccumulator = statsAccumulators[i];
      combineWith(curStatsAccumulator);
    }
  }

  /**
   * Combine this statsAccumulator with the other.
   */
  public void combineWith(StatsAccumulator other) {
    final double curMin = other.getMin();
    final double curMax = other.getMax();
    if (n == 0) {
      min = curMin;
      max = curMax;
    }
    else {
      if (curMin < min) {
        min = curMin;
      }
      if (curMax > max) {
        max = curMax;
      }
    }

    n += other.n;
    sos += other.sos;
    sum += other.sum;
  }

  public String generateReport(boolean html) {
    return
      new Tableizer(Tableizer.LEFT_ALIGNED_TABLE).generateTable(html,
          new String[][]{
            {null, (label == null ? "" : label),
             "min", MathUtil.doubleString(min, 1),
             "mean", MathUtil.doubleString(getMean(), 1),
             "sum", MathUtil.doubleString(sum, 1), },
            {"n", Long.toString(n),
             "max", MathUtil.doubleString(max, 1),
             "stddev", MathUtil.doubleString(getStandardDeviation(), 1),
             "sos", MathUtil.doubleString(sos, 1), },
          });
  }

  /**
   * Build a view of this instance's values with the given width of the form:
   * <p>
   * "min [(mean)] max"
   * <p>
   * Where:
   * <ul>
   * <li>min -- is this instance's minimum value</li>
   * <li>"[" -- is a marker representing the minimum value on a 1D line plot</li>
   * <li>"(" -- is a marker representing this instance's mean - stddev value</li>
   * <li>mean -- is this instance's mean value</li>
   * <li>")" -- is a marker representing this instance's mean + stddev value</li>
   * <li>"]" -- is a marker representing this instance's maximum value</li>
   * <li>max -- is this instance's maximum value</li>
   * </ul>
   *
   * Note that whitespace will be used for visualizing the mean and stddev info
   * relative to the min/max.
   *
   * @param viewWidth  The width in characters of the resulting view string.
   * @param numDecimalPlaces  The number of decimal places for the min, mean, and max values.
   */
  public String buildView(int viewWidth, int numDecimalPlaces) {
    return buildView(viewWidth, numDecimalPlaces, this.getMin(), this.getMax());
  }

  /**
   * Build a view of this instance's values with the given width of the form:
   * <p>
   * "min [(mean)] max"
   * <p>
   * Where:
   * <ul>
   * <li>min -- is this instance's minimum value</li>
   * <li>"[" -- is a marker representing the minimum value on a 1D line plot</li>
   * <li>"(" -- is a marker representing this instance's mean - stddev value</li>
   * <li>mean -- is this instance's mean value</li>
   * <li>")" -- is a marker representing this instance's mean + stddev value</li>
   * <li>"]" -- is a marker representing this instance's maximum value</li>
   * <li>max -- is this instance's maximum value</li>
   * </ul>
   *
   * Note that whitespace will be used for visualizing the mean and stddev info
   * relative to the min/max.
   *
   * @param viewWidth  The width in characters of the resulting view string.
   * @param numDecimalPlaces  The number of decimal places for the min, mean, and max values.
   * @param minValue  A reference minimum value for computing the effective width of the
   *                  min, mean, and max values useful for normalizing view widths across
   *                  view strings for multiple stats.
   * @param maxValue  A reference maximum value for computing the effective width of the
   *                  min, mean, and max values useful for normalizing view widths across
   *                  view strings for multiple stats.
   */
  public String buildView(int viewWidth, int numDecimalPlaces, double minValue, double maxValue) {
    //  "minValue [(meanValue)] maxValue"
    final StringBuilder result = new StringBuilder();

    //NOTE: use maxValue (width) + numDecimalPlaces to compute (portion of view) width consumed by embedded values
    final int maxValueWidth = (int)MathUtil.log10(maxValue) + 1 + ((minValue < 0) ? 1 : 0);
    final int valWidth = maxValueWidth + numDecimalPlaces + 1; // +1 for decimal point
    final String valFormatStr = "%" + valWidth + "." + numDecimalPlaces + "f";
    final int valWidth3 = valWidth * 3;  // min, mean, max
    viewWidth = Math.max(valWidth3 + 6, viewWidth - valWidth3 + 3);  // count each val as 1

    final int scaleWidth = viewWidth - 4; // exclude "min ", " max" from scaled portion

    final double min = Math.max(this.getMin(), minValue);
    final double max = Math.min(this.getMax(), maxValue);

    // minValue + " "
    // minBracketOff = scaleValue(min, scaleWidth)
    // leftParenOff = scaleValue(Math.max(mean - stddev, min), scaleWidth)
    // meanValueOff = Math.max(scaleValue(mean, scaleWidth), 1)
    // rtParenOff = Math.max(scaleValue(Math.min(mean + stddev, max), scaleWidth), 2)
    // maxBracketOff = scaleValue(max, scaleWidth)
    // maxValueOff = scaleWidth

    // compute offsets
    int extraSpace = valWidth + 1;
    final int meanOffset = scaleValue(getMean(), minValue, maxValue, scaleWidth) + extraSpace;
    int minOffset = scaleValue(min, minValue, maxValue, scaleWidth) + extraSpace;
    if (minOffset >= meanOffset) --minOffset;
    int lpOffset = scaleValue(Math.max(this.getMean() - this.getStandardDeviation(), min), minValue, maxValue, scaleWidth) + extraSpace;
    if (lpOffset >= meanOffset) --lpOffset;
    extraSpace += (valWidth - 1);
    final int maxOffset1 = scaleValue(max, minValue, maxValue, scaleWidth) + extraSpace;
    int rpOffset = scaleValue(Math.min(this.getMean() + this.getStandardDeviation(), max), minValue, maxValue, scaleWidth) + extraSpace;
    if (rpOffset >= maxOffset1) --rpOffset;
    final int maxOffset2 = scaleWidth + extraSpace + 1;

    appendAt(result, String.format(valFormatStr, this.getMin()), 0);
    appendAt(result, "[", minOffset);
    appendAt(result, "(", lpOffset);
    appendAt(result, String.format(valFormatStr, this.getMean()), meanOffset);
    appendAt(result, ")", rpOffset);
    appendAt(result, "]", maxOffset1);
    appendAt(result, String.format(valFormatStr, this.getMax()), maxOffset2);

    return result.toString();
  }

  private final int scaleValue(double value, double minValue, double maxValue, int width) {
    return (int)(MathUtil.normalize(value, minValue, maxValue, 0, width - 1));
  }

  private final void appendAt(StringBuilder result, String toAppend, int atIdx) {
    final int curIdx = result.length();
    fillWhite(result, atIdx - curIdx);
    result.append(toAppend);
  }

  private final void fillWhite(StringBuilder result, int count) {
    for (int i = 0; i < count; ++i) {
      result.append(' ');
    }
  }
}
