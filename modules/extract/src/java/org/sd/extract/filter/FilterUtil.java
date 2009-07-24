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
package org.sd.extract.filter;


import org.sd.text.MungedWordFinder;
import org.sd.text.MultiTermFinder;

import java.util.List;

/**
 * Utility class for applying common filter operations.
 * <p>
 * @author Spence Koehler
 */
public class FilterUtil {
  
  /**
   * Utility method for applying a MultiTermFinder to a MungedWordFinder's
   * breakdown of a string.
   *
   * @return true if a term is found in any breakdown of the string;
   *         otherwise, false.
   */
  public static final boolean findHitsInAny(String string, MultiTermFinder multiTermFinder, String mtfExpressionString, MungedWordFinder mungedWordFinder) {
    boolean result = false;

    final List<MungedWordFinder.WordSequence> seqs = mungedWordFinder.getBestSplits(string);
    for (MungedWordFinder.WordSequence seq : seqs) {
      if (hasHit(seq, multiTermFinder, mtfExpressionString)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Utility method for applying a MultiTermFinder to a MungedWordFinder's
   * breakdown of a string.
   *
   * @return true if a term is found in all breakdowns of the string;
   *         otherwise, false.
   */
  public static final boolean findHitsInAll(String string, MultiTermFinder multiTermFinder, String mtfExpressionString, MungedWordFinder mungedWordFinder) {
    boolean result = true;

    final List<MungedWordFinder.WordSequence> seqs = mungedWordFinder.getBestSplits(string);
    for (MungedWordFinder.WordSequence seq : seqs) {
      if (!hasHit(seq, multiTermFinder, mtfExpressionString)) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Utility method for applying a verifying MultiTermFinder to a 
   * MungedWordFinder's breakdown of a string after prequalifying
   * the string for a potential match.
   *
   * @return true if a verifier term is found in any breakdowns of the string;
   *         otherwise, false.
   */
  public static final boolean findHitsInAny(String string, MultiTermFinder preQualifier, String pqExpressionString, MultiTermFinder verifier, String vExpressionString, MungedWordFinder mungedWordFinder) {
    boolean result = false;

    if (hasHit(string, preQualifier, pqExpressionString)) {
      result = findHitsInAny(string, verifier, vExpressionString, mungedWordFinder);
    }

    return result;
  }

  /**
   * Utility method for applying a verifying MultiTermFinder to a 
   * MungedWordFinder's breakdown of a string after prequalifying
   * the string for a potential match.
   *
   * @return true if a verifier term is found in all breakdowns of the string;
   *         otherwise, false.
   */
  public static final boolean findHitsInAll(String string, MultiTermFinder preQualifier, String pqExpressionString, MultiTermFinder verifier, String vExpressionString, MungedWordFinder mungedWordFinder) {
    boolean result = false;

    if (hasHit(string, preQualifier, pqExpressionString)) {
      result = findHitsInAll(string, verifier, vExpressionString, mungedWordFinder);
    }

    return result;
  }


  /**
   * Determine whether the word sequence has a multi-term finder hit.
   */
  public static final boolean hasHit(MungedWordFinder.WordSequence seq, MultiTermFinder multiTermFinder, String mtfExpressionString) {
    return hasHit(seq, multiTermFinder, mtfExpressionString);
  }

  /**
   * Determine whether the string has a multi-term finder hit.
   */
  public static final boolean hasHit(String string, MultiTermFinder multiTermFinder, String mtfExpressionString) {
    return multiTermFinder.evaluateLogicalExpression(mtfExpressionString, string) != null;
  }

  /**
   * Determine whether any of the strings has a multi-term finder hit.
   *
   * @return true if a term is found in any of the strings; otherwise, false.
   */
  public static final boolean findHitsInAny(String[] strings, MultiTermFinder multiTermFinder, String mtfExpressionString) {
    boolean result = false;

    for (String string : strings) {
      if (multiTermFinder.evaluateLogicalExpression(mtfExpressionString, string) != null) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether all of the strings has a multi-term finder hit.
   *
   * @return true if a term is found in all of the strings; otherwise, false.
   */
  public static final boolean findHitsInAll(String[] strings, MultiTermFinder multiTermFinder, String mtfExpressionString) {
    boolean result = true;

    for (String string : strings) {
      if (multiTermFinder.evaluateLogicalExpression(mtfExpressionString, string) == null) {
        result = false;
        break;
      }
    }

    return result;
  }
}
