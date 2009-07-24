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
package org.sd.lang.english;


import org.sd.io.FileUtil;
import org.sd.lang.WordFinder;

import java.io.File;
import java.io.IOException;

/**
 * Factory for english word finders.
 * <p>
 * @author Spence Koehler
 */
public class EnglishWordFinderFactory {

  public static final String PREP_RESOURCE = "resources/EnglishSentence.preps.txt";
  public static final String DET_RESOURCE = "resources/EnglishSentence.dets.txt";
  public static final String CONJ_RESOURCE = "resources/EnglishSentence.conjs.txt";

  private static WordFinder prepFinder;
  private static WordFinder detFinder;
  private static WordFinder conjFinder;

  private static WordFinder prepFinderCS;
  private static WordFinder detFinderCS;
  private static WordFinder conjFinderCS;

  /**
   * Get a word finder for English prepositions.
   */
  public static final WordFinder getPrepFinder(boolean caseSensitive) {
    WordFinder result = null;

    if (caseSensitive) {
      if (prepFinderCS == null) {
        prepFinderCS = buildWordFinder("EnglishPrep", FileUtil.getFile(EnglishWordFinderFactory.class, PREP_RESOURCE), caseSensitive);
      }
      result = prepFinderCS;
    }
    else {
      if (prepFinder == null) {
        prepFinder = buildWordFinder("EnglishPrep", FileUtil.getFile(EnglishWordFinderFactory.class, PREP_RESOURCE), caseSensitive);
      }
      result = prepFinder;
    }
    return result;
  }

  /**
   * Get a word finder for English determiners.
   */
  public static final WordFinder getDetFinder(boolean caseSensitive) {
    WordFinder result = null;

    if (caseSensitive) {
      if (detFinderCS == null) {
        detFinderCS = buildWordFinder("EnglishDet", FileUtil.getFile(EnglishWordFinderFactory.class, DET_RESOURCE), caseSensitive);
      }
      result = detFinderCS;
    }
    else {
      if (detFinder == null) {
        detFinder = buildWordFinder("EnglishDet", FileUtil.getFile(EnglishWordFinderFactory.class, DET_RESOURCE), caseSensitive);
      }
      result = detFinder;
    }
    return result;
  }

  /**
   * Get a word finder for English conjunctions.
   */
  public static final WordFinder getConjFinder(boolean caseSensitive) {
    WordFinder result = null;

    if (caseSensitive) {
      if (conjFinderCS == null) {
        conjFinderCS = buildWordFinder("EnglishConj", FileUtil.getFile(EnglishWordFinderFactory.class, CONJ_RESOURCE), caseSensitive);
      }
      result = conjFinderCS;
    }
    else {
      if (conjFinder == null) {
        conjFinder = buildWordFinder("EnglishConj", FileUtil.getFile(EnglishWordFinderFactory.class, CONJ_RESOURCE), caseSensitive);
      }
      result = conjFinder;
    }
    return result;
  }

  private static final WordFinder buildWordFinder(String label, File file, boolean caseSensitive) {
    WordFinder result = null;

    try {
      result = new WordFinder(label, file, caseSensitive);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }
}
