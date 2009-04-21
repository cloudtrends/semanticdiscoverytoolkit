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
package org.sd.text;


import org.sd.io.FileUtil;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility to find a term from a (relatively small) set of terms in any
 * arbitrarily-sized string.
 * <p>
 * @author Spence Koehler
 */
public class TermFinder extends AbstractPatternFinder {
  
  public static final List<String> MATCH_FLAGS = Arrays.asList(new String[] {
      "sub",      // ACCEPT_PARTIAL (0)
      "prefix",   // BEGIN_WORD (1)
      "suffix",   // END_WORD (2)
      "full",     // FULL_WORD (3)
    });

  /**
   * Convert a string from "sub", "prefix", "suffix", or "full" to its
   * corresponding match (acceptPattern) flag.
   */
  public static final int getMatchFlag(String matchFlag) {
    return MATCH_FLAGS.indexOf(matchFlag);
  }


  private Set<String> terms;
  private RobinKarpStringSearch _rkSearch;

  /**
   * Construct with a general normalizer.
   */
  public TermFinder(String type, boolean caseSensitive) {
    this(type, caseSensitive ? GeneralNormalizer.getCaseSensitiveInstance() : GeneralNormalizer.getCaseInsensitiveInstance());
  }

  /**
   * Construct with a general normalizer and given terms.
   */
  public TermFinder(String type, boolean caseSensitive, String[] terms) {
    this(type, caseSensitive ? GeneralNormalizer.getCaseSensitiveInstance() : GeneralNormalizer.getCaseInsensitiveInstance(),
         terms);
  }

  /**
   * Construct with the given normalizer.
   */
  public TermFinder(String type, Normalizer normalizer) {
    this(type, normalizer, null);
  }

  /**
   * Construct with the given normalizer and terms.
   */
  public TermFinder(String type, Normalizer normalizer, String[] terms) {
    super(type, normalizer);

    this.terms = new HashSet<String>();
    this._rkSearch = null;

    if (terms != null) {
      loadTerms(terms);
    }
  }

  /**
   * Load (add) terms to find from the resource file, one term per line.
   * <p>
   * Empty lines and comment lines (starting with '#') are ignored.
   * <p>
   * Each term will be normalized while loading.
   */
  public void loadTerms(File resourceFile) throws IOException {
    _rkSearch = null;

    final BufferedReader reader = FileUtil.getReader(resourceFile);
    String line = null;

    while ((line = reader.readLine()) != null) {
      if (line.length() == 0 || line.charAt(0) == '#') continue;

      // term
      terms.add(normalize(line).getNormalized());
    }

    reader.close();
  }

  public void loadTerms(File resourceFile, File persistedFile) throws IOException {
    loadTerms(resourceFile);

    final DataInputStream dataIn = new DataInputStream(FileUtil.getInputStream(persistedFile));
    this._rkSearch = new RobinKarpStringSearch();
    this._rkSearch.read(dataIn);

    dataIn.close();
  }

  public void persistTo(File persistedFile) throws IOException {
    if (_rkSearch != null) {
      final DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(persistedFile, false));
      _rkSearch.write(dataOut);
      dataOut.close();
    }    
  }

  /**
   * Load (add) the given terms.
   * <p>
   * Each term will be normalized while loading.
   */
  public void loadTerms(String[] terms) {
    _rkSearch = null;
    
    for (String term : terms) {
//      this.terms.add(normalize(term).getNormalized());
      final NormalizedString normalizedString = normalize(term);
      if (normalizedString != null) {
        final String curTerm = normalizedString.getNormalized();
        if (curTerm != null && !"".equals(curTerm)) {
          this.terms.add(curTerm);
        }
      }
    }
  }

  /**
   * Find the position of a term within the (normalized) input.
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(NormalizedString input, int acceptPartial) {
    if (input == null || input.getNormalizedLength() == 0) return null;
    final RobinKarpStringSearch rkSearch = getRobinKarpSearch();
    return rkSearch.search(input, acceptPartial);
  }

  /**
   * Find a term between the fromPos (inclusive normalized position) and the
   * toPos (exclusive normalized position).
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(NormalizedString input, int fromPos, int toPos, int acceptPartial) {
    if (input == null || input.getNormalizedLength() == 0) return null;
    final RobinKarpStringSearch rkSearch = getRobinKarpSearch();
    return rkSearch.search(input, fromPos, toPos, acceptPartial);
  }

  /**
   * Determine whether this finder find the given term, normalizing as appropriate.
   */
  public boolean hasTerm(String term) {
    return hasTerm(normalize(term));
  }

  /**
   * Determine whether this finder find the given normalized term.
   */
  public boolean hasTerm(NormalizedString term) {
    return this.terms.contains(term.getNormalized());
  }

  /**
   * Get this finder's terms.
   */
  public Set<String> getTerms() {
    return terms;
  }

  private final RobinKarpStringSearch getRobinKarpSearch() {
    if (_rkSearch == null) {
      final String[] searchTerms = terms.toArray(new String[terms.size()]);
      _rkSearch = new RobinKarpStringSearch(17, searchTerms);
    }
    return _rkSearch;
  }
}
