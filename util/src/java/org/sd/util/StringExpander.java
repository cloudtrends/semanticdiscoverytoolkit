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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to split, expand, and combine a string into multiple versions
 * according to a series of split/expand functions.
 * <p>
 * @author Spence Koehler
 */
public class StringExpander {
  
  public static interface SplitExpandFunction {
    public Collection<String> split(String string, Context context);  // split string into pieces
    public Collection<String> expand(String piece, Context context);  // expand each split piece
    public SplitExpandFunction nextFunction();       // get next split/expand function to apply on each expansion
    public String getDelimString();                  // to paste things back together
  }

  public static interface ExpandStrategy {
    public Collection<String> expand(String piece, Context context);  // expand each split piece
  }

  public static interface SplitStrategy {
    public Collection<String> split(String string, Context context);  // split string into pieces
  }

  public static class BaseSplitExpandFunction implements SplitExpandFunction {

    private String delimString;
    private SplitStrategy splitStrategy;
    private ExpandStrategy expandStrategy;
    private SplitExpandFunction next;
    private boolean onlyExpandOnce;

    public BaseSplitExpandFunction(String delimString) {
      this(delimString, null, null, true);
    }

    public BaseSplitExpandFunction(String delimString, ExpandStrategy expandStrategy) {
      this(delimString, null, expandStrategy, true);
    }

    public BaseSplitExpandFunction(String delimString, SplitStrategy splitStrategy) {
      this(delimString, splitStrategy, null, true);
    }

    public BaseSplitExpandFunction(String delimString, SplitStrategy splitStrategy, ExpandStrategy expandStrategy, boolean onlyExpandOnce) {
      this.delimString = delimString;
      this.splitStrategy = splitStrategy;
      this.expandStrategy = expandStrategy;
      this.onlyExpandOnce = onlyExpandOnce;
    }

    public void setSplitStrategy(SplitStrategy splitStrategy) {
      this.splitStrategy = splitStrategy;
    }

    public void setExpandStrategy(ExpandStrategy expandStrategy) {
      this.expandStrategy = expandStrategy;
    }

    /**
     * Default split will return null.
     */
    public final Collection<String> split(String string, Context context) {  // split string into pieces
      Collection<String> result = null;

      if (splitStrategy != null) {
        result = splitStrategy.split(string, context);

        //NOTE: for the following logic to be properly executed, this method is final!
        if (result != null && onlyExpandOnce && context != null &&
            context.hasWordInBag(string.toLowerCase()/*, true*/)) {
          // to preserve that the splits came from the string, and therefore,
          // to curtain further expansion (i.e. synonym expansion) on the
          // pieces, we add them to the bag here.
          for (String resultString : result) {
            context.addWordToBag(resultString.toLowerCase());
          }
        }
      }

      return result;
    }

    /**
     * Default expansion is to not expand (return piece in a collection.)
     */
    public Collection<String> expand(String piece, Context context) {  // expand each split piece
      Collection<String> result = null;

      if (expandStrategy == null) {
        result = new ArrayList<String>();
        result.add(piece);
      }
      else {
        result = expandStrategy.expand(piece, context);
      }

      return result;
    }

    public void setNextFunction(SplitExpandFunction next) {
      this.next = next;
    }

    public SplitExpandFunction nextFunction() {
      return next;
    }

    public void setDelimString(String delimString) {
      this.delimString = delimString;
    }

    public String getDelimString() {
      return delimString;
    }
  }

  public static class RegexSplitExpandFunction extends BaseSplitExpandFunction {

    private String regex;
    private boolean dedup;

    /**
     * Construct with a regex and delimString
     *
     * @param regex        The regular expression to use for splitting.
     * @param delimString  The delim string to paste things back together.
     * @param dedup        Whether to dedup pieces split using the regex.
     */
    public RegexSplitExpandFunction(String regex, String delimString, boolean dedup) {
      this(null, regex, delimString, dedup);
    }

    public RegexSplitExpandFunction(final ExpandStrategy expandStrategy, final String regex, final String delimString, final boolean dedup) {
      super(delimString,
            new SplitStrategy() {
              public Collection<String> split(String string, Context context) {
                return StringExpander.split(string, regex, dedup);
              }
            },
            expandStrategy,
            true);

      this.regex = regex;
      this.dedup = dedup;
    }
  }

  public static Collection<String> split(String string, String splitPattern, boolean dedup) {
    Collection<String> result = dedup ? new LinkedHashSet<String>() : new ArrayList<String>();

    final String[] pieces = string.split(splitPattern);
    for (String piece : pieces) {
      result.add(piece);
    }

    return result;
  }

  private SplitExpandFunction splitExpandFunction;

  public StringExpander(SplitExpandFunction splitExpandFunction) {
    this.splitExpandFunction = splitExpandFunction;
  }

  /**
   * Get the strings generated from splitting, expanding, and combining the
   * input string according to this instance's split and expand functions.
   */
  public Collection<String> getStrings(String string) {
    final PiecesContainer container = new PiecesContainer(string, splitExpandFunction, null);
    return container.getAlternateStrings();
  }

  public static interface Context {
    public Context getParent();

    public Set<String> getWordBag();
    public boolean hasWordInBag(String word);
    public void addWordToBag(String word);

    public Object getProperty(String propertyName);
    public void setProperty(String propertyName, Object propertyValue);
    public Set<String> getPropertyNames();
  }

  public static abstract class AbstractContext implements Context {
    private Context parent;
    private Set<String> wordBag;
    private Map<String, Object> properties;

    protected AbstractContext(Context parent) {
      this.parent = parent;
      this.wordBag = parent == null ? new HashSet<String>() : parent.getWordBag();
      this.properties = null;
    }

    public Context getParent() {
      return parent;
    }

    public Set<String> getWordBag() {
      return wordBag;
    }

    public boolean hasWordInBag(String word) {
      boolean result = false;

      if (wordBag != null) {
        result = wordBag.contains(word);
      }
      return result;
    }

    public void addWordToBag(String word) {
      wordBag.add(word);
    }

    public Object getProperty(String propertyName) {
      Object result = null;

      if (properties != null) {
        result = properties.get(propertyName);
      }

      return result;
    }

    public void setProperty(String propertyName, Object propertyValue) {
      if (properties == null) {
        this.properties = new LinkedHashMap<String, Object>();
      }
      properties.put(propertyName, propertyValue);
    }

    public Set<String> getPropertyNames() {
      return properties.keySet();
    }
  }

  private static final class PiecesContainer extends AbstractContext {
    private String string;
    private String delimString;
    private List<AlternatesContainer> pieces;  // each piece has alternates
    private Collection<String> _alternateStrings;

    PiecesContainer(String string, SplitExpandFunction splitExpandFunction, Context parent) {
      super(parent);

      this.string = string;
      this.delimString = null;
      this.pieces = null;
      this._alternateStrings = null;

      if (splitExpandFunction != null) {
        this.delimString = splitExpandFunction.getDelimString();
        final Collection<String> nextStrings = splitExpandFunction.split(string, this);
        if (nextStrings != null) {
          this.pieces = new ArrayList<AlternatesContainer>();
          for (String nextString : nextStrings) {
            this.pieces.add(new AlternatesContainer(nextString, splitExpandFunction, this));
          }
        }
      }
    }

    public Collection<String> getAlternateStrings() {
      if (_alternateStrings == null) {
        _alternateStrings = computeAlternateStrings();
      }
      return _alternateStrings;
    }

    // combinate alternates across pieces
    public Collection<String> computeAlternateStrings() {
      Set<String> result = new LinkedHashSet<String>();

      if (pieces == null) {
        result.add(string);
      }
      else {
        final List<Collection<String>> preCombos = new ArrayList<Collection<String>>();
        for (AlternatesContainer piece : pieces) {
          preCombos.add(piece.getAlternateStrings());
        }
        final List<Collection<String>> combos = GeneralUtil.combine(preCombos);

        for (Collection<String> combo : combos) {
          final String alternateString = concatenate(combo, delimString);
          result.add(alternateString);
        }
      }

      return result;
    }

    private String concatenate(Collection<String> strings, String delimString) {
      final StringBuilder result = new StringBuilder();

      for (Iterator<String> iter = strings.iterator(); iter.hasNext(); ) {
        final String string = iter.next();
        result.append(string);
        if (iter.hasNext()) result.append(delimString);
      }

      return result.toString();
    }
  }

  private static final class AlternatesContainer extends AbstractContext {
    private String string;
    private List<PiecesContainer> alternates;  // each alternate has pieces
    private Collection<String> _alternateStrings;

    AlternatesContainer(String string, SplitExpandFunction splitExpandFunction, Context parent) {
      super(parent);

      this.string = string;
      this.alternates = null;
      this._alternateStrings = null;

      final Collection<String> nextStrings = splitExpandFunction.expand(string, this);
      if (nextStrings != null) {
        this.alternates = new ArrayList<PiecesContainer>();
        for (String nextString : nextStrings) {
          this.alternates.add(new PiecesContainer(nextString, splitExpandFunction.nextFunction(), this));
        }
      }
    }

    public Collection<String> getAlternateStrings() {
      if (_alternateStrings == null) {
        _alternateStrings = computeAlternateStrings();
      }
      return _alternateStrings;
    }

    // collect alternates across alternates
    public Collection<String> computeAlternateStrings() {
      Set<String> result = new LinkedHashSet<String>();

      if (alternates == null) {
        result.add(string);
      }
      else {
        // get the combine the alternate pieces
        for (PiecesContainer alternate : alternates) {
          final Collection<String> pieceAlternates = alternate.getAlternateStrings();
          result.addAll(pieceAlternates);
        }
      }

      return result;
    }
  }

}
