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
package org.sd.util;


/**
 * Simple utility for handling precision and recall.
 * <p>
 * @author Spence Koehler
 */
public class PrecisionRecall {
  
  private int trueCount;
  private int generatedCount;
  private int alignedCount;

  private double precision;
  private double recall;

  public PrecisionRecall() {
    this.trueCount = 0;
    this.generatedCount = 0;
    this.alignedCount = 0;

    this.precision = -1.0;
    this.recall = -1.0;
  }

  public PrecisionRecall(int trueCount, int generatedCount, int alignedCount) {
    this.trueCount = trueCount;
    this.generatedCount = generatedCount;
    this.alignedCount = alignedCount;

    this.precision = -1.0;
    this.recall = -1.0;
  }

  public int getTrueCount() {
    return trueCount;
  }

  public void setTrueCount(int trueCount) {
    this.recall = -1.0;
    this.trueCount = trueCount;
  }

  public int getGeneratedCount() {
    return generatedCount;
  }

  public void setGeneratedCount(int generatedCount) {
    this.precision = -1.0;
    this.generatedCount = generatedCount;
  }
  
  public int getAlignedCount() {
    return alignedCount;
  }

  public void setAlignedCount(int alignedCount) {
    this.recall = -1.0;
    this.precision = -1.0;
    this.alignedCount = alignedCount;
  }

  public int[] getPrecisionRatio() {
    return new int[]{ alignedCount, generatedCount };
  }

  public int[] getRecallRatio() {
    return new int[]{ alignedCount, trueCount };
  }

  public double getPrecision() {
    if (precision < 0 && generatedCount > 0) {
      precision = ((double)alignedCount) / ((double)generatedCount);
    }
    return precision;
  }

  public double getRecall() {
    if (recall < 0 && trueCount > 0) {
      recall = ((double)alignedCount) / ((double)trueCount);
    }
    return recall;
  }

  public double getFMeasure() {
    return getFMeasure(1.0);
  }

  public double getFMeasure(double alpha) {
    return getFMeasure(getPrecision(), getRecall(), alpha);
  }

  public String getPrecisionString() {
    return getStringValue(getPrecisionRatio());
  }

  public String getPrecisionString(int numPlaces) {
    return getStringValue(getPrecisionRatio(), numPlaces);
  }

  public String getPrecisionRatioString() {
    final StringBuilder result = new StringBuilder();
    result.append(alignedCount).append('/').append(generatedCount);
    return result.toString();
  }

  public String getRecallRatioString() {
    final StringBuilder result = new StringBuilder();
    result.append(alignedCount).append('/').append(trueCount);
    return result.toString();
  }

  public String getRecallString() {
    return getStringValue(getRecallRatio());
  }

  public String getRecallString(int numPlaces) {
    return getStringValue(getRecallRatio(), numPlaces);
  }

  public String getFMeasureString() {
    return getStringValue(getFMeasure() * 100.0);
  }

  public String getFMeasureString(int numPlaces) {
    return getStringValue(getFMeasure() * 100.0, numPlaces);
  }

  public String getFMeasureString(double alpha, int numPlaces) {
    return getStringValue(getFMeasure(alpha) * 100.0, numPlaces);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    // precision   recall  f-measure   true   generated aligned
    //   ##.###    ##.###    ##.###    #,###    #,###    #,###
    result.
      append("  ").
      append(MathUtil.pad(getPrecisionString(3), 6, MathUtil.Justification.RIGHT)).
      append("    ").
      append(MathUtil.pad(getRecallString(3), 6, MathUtil.Justification.RIGHT)).
      append("    ").
      append(MathUtil.pad(getFMeasureString(3), 6, MathUtil.Justification.RIGHT)).
      append("    ").
      append(MathUtil.pad(MathUtil.addCommas(Integer.toString(trueCount)), 5, MathUtil.Justification.RIGHT)).
      append("    ").
      append(MathUtil.pad(MathUtil.addCommas(Integer.toString(generatedCount)), 5, MathUtil.Justification.RIGHT)).
      append("    ").
      append(MathUtil.pad(MathUtil.addCommas(Integer.toString(alignedCount)), 5, MathUtil.Justification.RIGHT));

    return result.toString();
  }

  /** A standard header for toString data */
  public static final String STANDARD_HEADER = "precision   recall  f-measure   true   generated aligned";

  public static double getFMeasure(int[] precisionRatio, int[] recallRatio) {
    return getFMeasure(precisionRatio, recallRatio, 1.0);
  }

  public static double getFMeasure(int[] precisionRatio, int[] recallRatio, double alpha) {
    final double precision = getDoubleValue(precisionRatio);
    final double recall = getDoubleValue(recallRatio);

    return getFMeasure(precision, recall, alpha);
  }

  public static double getFMeasure(double precision, double recall) {
    return getFMeasure(precision, recall, 1.0);
  }

  public static double getFMeasure(double precision, double recall, double alpha) {
    double result = -1.0;

    if (precision >= 0.0 && recall >= 0.0) {
      result = ((1.0 + alpha) * precision * recall) / ((alpha * precision) + recall);
    }

    return result;
  }

  public static double getDoubleValue(int[] ratio) {
    return ratio[1] == 0 ? -1.0 : ((double)ratio[0]) / ((double)ratio[1]);
  }

  public static String getStringValue(int[] ratio) {
    return getStringValue(ratio, 3);
  }

  public static String getStringValue(int[] ratio, int numPlaces) {
    final double doubleValue = getDoubleValue(ratio) * 100.0;
    return getStringValue(doubleValue, numPlaces);
  }

  public static String getStringValue(double value) {
    return getStringValue(value, 3);
  }

  public static String getStringValue(double value, int numPlaces) {
    return MathUtil.doubleString(value, numPlaces);
  }


}
