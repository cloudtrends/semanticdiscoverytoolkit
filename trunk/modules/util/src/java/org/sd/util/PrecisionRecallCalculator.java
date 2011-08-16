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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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
public class PrecisionRecallCalculator {

  private int tp;  // true positives
  private int fp;  // false positives
  private int tn;  // true negatives
  private int fn;  // false negatives
  private String label;

  /**
   * Default constructor.
   */
  public PrecisionRecallCalculator() {
    // initialize labels to empty string rather than null so that we can safely write them to output streams in all cases
    this("");
  }

  /**
   * Construct with the given label.
   */
  public PrecisionRecallCalculator(String label) {
    clear(label);
  }

  /**
   * Construct a statsAccumulator w/the following characteristics.
   */
  public PrecisionRecallCalculator(String label, int tp, int fp, int tn, int fn) {
    this.label = label;
    this.tp = tp;
    this.fp = fp;
    this.tn = tn;
    this.fn = fn;
  }

  /**
   * Clear (reset/zero) this instance to begin anew.
   */
  public final void clear() {
    this.tp = 0;
    this.fp = 0;
    this.tn = 0;
    this.fn = 0;
  }

  /**
   * Clear this instance and reset its label.
   */
  public final void clear(String label) {
    clear();
    this.label = label;
  }

  public String toString() {
    StringBuffer result = new StringBuffer();

    result.
      append((label == null) ? "P/R" : label + " P/R").
      append(" : ").
      append(MathUtil.doubleString(getPrecision() * 100.0, 2)).
      append("/").
      append(MathUtil.doubleString(getRecall() * 100.0, 2));

    return result.toString();
  }

  public void incTruePositive() {
    this.incTruePositive(1);
  }

  public void incTruePositive(int n) {
    tp += n;
  }

  public void incFalsePositive() {
    this.incFalsePositive(1);
  }

  public void incFalsePositive(int n) {
    fp += n;
  }

  public void incTrueNegative() {
    this.incTrueNegative(1);
  }

  public void incTrueNegative(int n) {
    tn += n;
  }

  public void incFalseNegative() {
    this.incFalseNegative(1);
  }

  public void incFalseNegative(int n) {
    fn += n;
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
  public int getTotal() {
    return tp + fp + tn + fn;
  }

  /**
   * Get the precision.
   */
  public double getPrecision() {
    final int d = tp + fp;
    return d == 0 ? 0.0 : (double)tp / (double)d;
  }

  /**
   * Get the recall.
   */
  public double getRecall() {
    final int d = tp + fn;
    return d == 0 ? 0.0 : (double)tp / (double)d;
  }

  /**
   * Serializes this object to the specified DataOutput.
   *
   * @param dataOutput DataOutput to write to
   * @throws IOException
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeUTF(label == null ? "" : label);
    dataOutput.writeInt(tp);
    dataOutput.writeInt(fp);
    dataOutput.writeInt(tn);
    dataOutput.writeInt(fn);
  }

  /**
   * Deserialize the object from the specified DataInput.
   *
   * @param dataInput dataInput
   * @return this instance
   * @throws IOException
   */
  public PrecisionRecallCalculator readFields(DataInput dataInput) throws IOException {
    label = dataInput.readUTF();
    if (label.length() == 0) {
      label = null;
    }
    tp = dataInput.readInt();
    fp = dataInput.readInt();
    tn = dataInput.readInt();
    fn = dataInput.readInt();

    return this;
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
  public static PrecisionRecallCalculator combine(String label, PrecisionRecallCalculator[] statsAccumulators) {
    PrecisionRecallCalculator result = new PrecisionRecallCalculator(label);
    result.doCombine(statsAccumulators);
    return result;
  }

  /**
   * Combine this statsAccumulator with all the others.
   */
  private void doCombine(PrecisionRecallCalculator[] statsAccumulators) {
    for (int i = 0; i < statsAccumulators.length; ++i) {
      final PrecisionRecallCalculator curPrecisionRecallCalculator = statsAccumulators[i];
      combineWith(curPrecisionRecallCalculator);
    }
  }

  /**
   * Combine this statsAccumulator with the other.
   */
  public void combineWith(PrecisionRecallCalculator other) {
    tp += other.tp;
    fp += other.fp;
    tn += other.tn;
    fn += other.fn;
  }

  public String generateReport(boolean html) {
    return
      new Tableizer(Tableizer.LEFT_ALIGNED_TABLE).generateTable(html,
          new String[][]{
            {label == null ? "" : label,
             "n", Integer.toString(getTotal()),
            },
            {"P", MathUtil.doubleString(getPrecision(), 1),
             "R", MathUtil.doubleString(getRecall(), 1),
            }
          });
  }
}
