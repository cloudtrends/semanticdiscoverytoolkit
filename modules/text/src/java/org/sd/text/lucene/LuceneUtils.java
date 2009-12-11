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
package org.sd.text.lucene;


import org.sd.util.ReflectUtil;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * General lucene utilities.
 * <p>
 * @author Spence Koehler
 */
public class LuceneUtils {

  /**
   * A minimal set of english stopwords.
   */
  public static final Set<String> DEFAULT_STOPWORDS = new HashSet<String>();
  static {
    for (Object stopword : StopAnalyzer.ENGLISH_STOP_WORDS_SET) {
      if (stopword instanceof String) {
        DEFAULT_STOPWORDS.add((String)stopword);
      }
    }
  }

  /**
   * A minimal set of english stopwords.
   */
  public static final String[] DEFAULT_STOPWORDS_ARRAY = DEFAULT_STOPWORDS.toArray(new String[DEFAULT_STOPWORDS.size()]);

  /**
   * Split the string into tokens using the LuceneStore's default analyzer.
   */
  public static final List<String> getTokenTexts(String string) {
    return getTokenTexts(LuceneStore.getDefaultAnalyzer(), null, string);
  }

  /**
   * Split the string into tokens using the given analyzer.
   */
  public static final List<String> getTokenTexts(Analyzer analyzer, String fieldName, String string) {
    if (string == null) return null;

    final List<String> result = new ArrayList<String>();

    if (analyzer != null) {
      final TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(string));

      try {
        while (tokenStream.incrementToken()) {
          if (tokenStream.hasAttribute(TermAttribute.class)) {
            final TermAttribute termAttribute = (TermAttribute)tokenStream.getAttribute(TermAttribute.class);
            result.add(termAttribute.term());
          }
        }
        tokenStream.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    else {
      result.add(string);
    }

    return result;
  }

  public static final List<List<String>> getPhraseTexts(Analyzer analyzer, String fieldName, String string) {
    if (string == null) return null;

    final List<List<String>> result = new LinkedList<List<String>>();
    List<String> curPhrase = new ArrayList<String>();
    result.add(curPhrase);

    if (analyzer != null) {
      final TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(string));
      int lastEndOffset = 0;

      try {
        while (tokenStream.incrementToken()) {
          boolean incPhrase = true;
          if (tokenStream.hasAttribute(OffsetAttribute.class)) {
            final OffsetAttribute offsetAttribute = (OffsetAttribute)tokenStream.getAttribute(OffsetAttribute.class);
            if (offsetAttribute.startOffset() == lastEndOffset) {
              incPhrase = false;
            }
            lastEndOffset = offsetAttribute.endOffset();
          }

          if (tokenStream.hasAttribute(TermAttribute.class)) {
            final TermAttribute termAttribute = (TermAttribute)tokenStream.getAttribute(TermAttribute.class);
            if (incPhrase && curPhrase.size() > 0) {
              curPhrase = new ArrayList<String>();
              result.add(curPhrase);
            }

            curPhrase.add(termAttribute.term());
          }
        }
        tokenStream.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    else {
      curPhrase.add(string);
    }

    return result;
  }

  /**
   * Build a phrase query from the tokens in the given string using LuceneStore's
   * default analyzer.
   */
  public static final Query toQuery(String fieldName, String string, Collection<String> termCollector) {
    return toQuery(LuceneStore.getDefaultAnalyzer(), fieldName, string, termCollector);
  }

  /**
   * Create a phrase query from all of the tokens in all of the strings using
   * the given analyzer.
   */
  public static final Query toQuery(Analyzer analyzer, String[] fieldNames, String string, Collection<String> termCollector) {
    BooleanQuery result = new BooleanQuery();
    for (String fieldName : fieldNames) {
      result.add(toQuery(analyzer, fieldName, string, termCollector), BooleanClause.Occur.SHOULD);
    }
    return result;
  }

  /**
   * Build a phrase query from the tokens in the given string using the given
   * analyzer.
   */
  public static final Query toQuery(Analyzer analyzer, String fieldName, String string, Collection<String> termCollector) {
    return toQuery(analyzer, fieldName, string, termCollector, BooleanClause.Occur.SHOULD);
  }

  /**
   * Build a phrase query from the tokens in the given string using the given
   * analyzer.
   * <p>
   * Use a BooleanClause.Occur.MUST for exact matches and BooleanClause.Occur.SHOULD
   * for fuzzy matches.
   */
  public static final Query toQuery(Analyzer analyzer, String fieldName, String string, Collection<String> termCollector, BooleanClause.Occur occur) {
    Query result = null;

    if (analyzer != null) {
      final TokenStream tokenStream = analyzer.tokenStream(fieldName, new StringReader(string));
      
      BooleanQuery booleanQuery = null;
      PhraseQuery phraseQuery = null;
      int lastEndOffset = 0;

      try {
        while (tokenStream.incrementToken()) {
          if (tokenStream.hasAttribute(TermAttribute.class)) {
            final TermAttribute termAttribute = (TermAttribute)tokenStream.getAttribute(TermAttribute.class);
            final String term = termAttribute.term();

            // check offset attribute
            if (tokenStream.hasAttribute(OffsetAttribute.class)) {
              final OffsetAttribute offsetAttribute = (OffsetAttribute)tokenStream.getAttribute(OffsetAttribute.class);
              if (offsetAttribute.startOffset() != lastEndOffset) {
                // time to increment phrase
                if (phraseQuery != null) {
                  if (booleanQuery == null) booleanQuery = new BooleanQuery();
                  booleanQuery.add(phraseQuery, occur);
                  phraseQuery = null;
                }
              }
              lastEndOffset = offsetAttribute.endOffset();            
            }

            if (phraseQuery == null) phraseQuery = new PhraseQuery();
            phraseQuery.add(new Term(fieldName, term));
            if (termCollector != null) termCollector.add(term);
          }
        }
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }

      if (phraseQuery != null) {
        if (booleanQuery == null) booleanQuery = new BooleanQuery();
        booleanQuery.add(phraseQuery, BooleanClause.Occur.SHOULD);
      }
      result = booleanQuery;
    }

    if (result == null) {
      result = new TermQuery(new Term(fieldName, string));
      if (termCollector != null) termCollector.add(string);
    }

    return result;
  }

  /**
   * Create a phrase query from all of the tokens in all of the strings using
   * LuceneStore's default analyzer.
   */
  public static final Query toQuery(String fieldName, String[] strings, Collection<String> termCollector) {
    return toQuery(LuceneStore.getDefaultAnalyzer(), fieldName, strings, termCollector);
  }

  /**
   * Create a phrase query from all of the tokens in all of the strings using
   * the given analyzer.
   */
  public static final Query toQuery(Analyzer analyzer, String[] fieldNames, String[] strings, Collection<String> termCollector) {
    BooleanQuery result = new BooleanQuery();
    for (String fieldName : fieldNames) {
      result.add(toQuery(analyzer, fieldName, strings, termCollector), BooleanClause.Occur.SHOULD);
    }
    return result;
  }
  
  /**
   * Create a phrase query from all of the tokens in all of the strings using
   * the given analyzer.
   */
  public static final Query toQuery(Analyzer analyzer, String fieldName, String[] strings, Collection<String> termCollector) {
    return toQuery(analyzer, fieldName, strings, termCollector, BooleanClause.Occur.SHOULD);
  }

  /**
   * Create a phrase query from all of the tokens in all of the strings using
   * the given analyzer.
   */
  public static final Query toQuery(Analyzer analyzer, String fieldName, String[] strings, Collection<String> termCollector, BooleanClause.Occur occur) {
    Query result = null;

    if (strings.length == 1) {
      result = toQuery(analyzer, fieldName, strings[0], termCollector, occur);
    }
    else if (strings.length > 1) {
      final PhraseQuery phraseQuery = new PhraseQuery();
      for (String string : strings) {
        final List<String> terms = getTokenTexts(analyzer, fieldName, string);
        for (String term : terms) {
          phraseQuery.add(new Term(fieldName, term));
          if (termCollector != null) termCollector.add(term);
        }
      }
      result = phraseQuery;
    }

    return result;
  }

  public static final Query tokensToQuery(String label, String[] tokens, Collection<String> termCollector) {
    Query result = null;

    if (tokens != null && tokens.length > 0) {
      if (tokens.length == 1) {
        result = new TermQuery(new Term(label, tokens[0]));
        if (termCollector != null) termCollector.add(tokens[0]);
      }
      else {
        PhraseQuery phraseQuery = new PhraseQuery();
        for (String token : tokens) {
          phraseQuery.add(new Term(label, token));
          if (termCollector != null) termCollector.add(token);
        }
        result = phraseQuery;
      }
    }

    return result;
  }

  /**
   * Turn the string into a format suitable for building a query using LucenStore's
   * default analyzer.
   */
  public static final String toQueryString(String fieldName, String string, BooleanClause.Occur occur) {
    return toQueryString(LuceneStore.getDefaultAnalyzer(), fieldName, string, occur);
  }

  /**
   * Turn the string into a format suitable for building a query using the given
   * analyzer.
   */
  public static final String toQueryString(Analyzer analyzer, String fieldName, String string, BooleanClause.Occur occur) {
    final StringBuilder result = new StringBuilder();

    if (occur == BooleanClause.Occur.MUST) {
      result.append('+');
    }
    else if (occur == BooleanClause.Occur.MUST_NOT) {
      result.append('^');
    }

    result.append(fieldName).append(':');

    final List<String> terms = getTokenTexts(analyzer, fieldName, string);
    for (Iterator<String> iter = terms.iterator(); iter.hasNext(); ) {
      final String term = iter.next();
      result.append(term);
      if (iter.hasNext()) result.append('_');
    }

    return result.toString();
  }

  /**
   * Turn the string into a format suitable for building a query using the given
   * analyzer.
   */
  public static final String toQueryString(LuceneFieldId fieldId, String string, BooleanClause.Occur occur) {
    final StringBuilder result = new StringBuilder();

    if (occur == BooleanClause.Occur.MUST) {
      result.append('+');
    }
    else if (occur == BooleanClause.Occur.MUST_NOT) {
      result.append('^');
    }

    result.append(fieldId.getLabel()).append(':');

    final String[] terms = fieldId.tokenize(string);
    for (int i = 0; i < terms.length; ++i) {
      if (i > 0) result.append('_');
      result.append(terms[i]);
    }

    return result.toString();
  }

  /**
   * Build a query from a possibly nested formatted string.
   * <p>
   * Syntax is FIELD_ID_CLASSPATH/SIMPLE_QUERIES
   * <p>
   * Where FIELD_ID_CLASSPATH/ is optional. When present it indicates the
   * class that extends LuceneFieldId from which to lookup field id instances
   * to use the proper analyzer. When absent, the default analyzer is always
   * used for every field.
   * <p>
   * each SIMPLE_QUERY is of the form M(QUERY), which is a marker followed by
   * a simple query in parenthesis as described in buildSimpleQuery below.
   * <p>
   * M is a marker that is empty (for 'OR'), '+' (for 'AND'), or '^' (for 'NOT')
   * <p>
   * If termCollector is non-null, collect query terms in it, discarding "not" terms.
   * <p>
   * If fieldNames is non-null, collect the names of fields being queried.
   */
  public static Query buildQuery(String queryString, Collection<String> termCollector, Collection<String> fieldNames) {
    Query result = null;

    // split off "fieldIdClass/"
    final FieldIdSplit fieldIdSplit = new FieldIdSplit(queryString);
    queryString = fieldIdSplit.getRemainder();

    final String[] queries = splitSimpleQueries(queryString);

    if (queries == null) {  //note: splitting returns null if there are no parens.
      result = buildSimpleQuery(queryString, fieldIdSplit, termCollector, fieldNames);
    }
    else {
      BooleanQuery booleanQuery = new BooleanQuery();

      for (String query : queries) {
        final OccurSplit occurSplit = new OccurSplit(query);

        final BooleanClause.Occur occur = occurSplit.getOccur();
        final Query curQuery = buildSimpleQuery(occurSplit.getRemainder(), fieldIdSplit, termCollector, fieldNames);

        booleanQuery.add(curQuery, occur);
        result = booleanQuery;
      }
    }

    return result;
  }

  /**
   * Build a query from a non-nested formatted string.
   * <p>
   * Syntax is FIELD_ID_CLASSPATH/FIELD_TERMS
   * <p>
   * Where FIELD_ID_CLASSPATH/ is optional. When present it indicates the
   * class that extends LuceneFieldId from which to lookup field id instances
   * to use the proper analyzer. When absent, the default analyzer is always
   * used for every field.
   * <p>
   * FIELD_TERMS is a list of field terms of the form: "MfieldName:value",
   * <p>
   * where each element in the list is separated by whitespace (' '), comma(','),
   * or semicolon(';'), and multiple phrase terms in the value are separated by
   * an underscore ('_').
   * <p>
   * M is a marker that is empty (for 'OR'), '+' (for 'AND'), or '^' (for 'NOT')
   * <p>
   * If termCollector is non-null, collect query terms in it, discarding "not" terms.
   * <p>
   * If fieldNames is non-null, collect the names of fields being queried.
   */
  public static Query buildSimpleQuery(String queryString, Collection<String> termCollector, Collection<String> fieldNames) {
    return buildSimpleQuery(queryString, null, termCollector, fieldNames);
  }

  private static Query buildSimpleQuery(String queryString, FieldIdSplit fieldIdSplit, Collection<String> termCollector, Collection<String> fieldNames) {
// todo: validate fields against the luceneStore when it is opened
    final BooleanQuery result = new BooleanQuery();

    if (fieldIdSplit == null) {
      fieldIdSplit = new FieldIdSplit(queryString);
      queryString = fieldIdSplit.getRemainder();
    }

    final Class fieldIdClass = fieldIdSplit.getFieldIdClass();

    //final String[] terms = queryString.split("[^\\w\\d:+^-]+");
    final String[] terms = splitTerms(queryString);

    for (String term : terms) {
      final String[] pieces = term.split(":");
      if (pieces.length != 2) {
        System.err.println("Illegal term: '" + term + "' IGNORED!");
      }
      else {
        final String termString = pieces[1];
        final OccurSplit occurSplit = new OccurSplit(pieces[0]);
        final BooleanClause.Occur occur = occurSplit.getOccur();
        final String fieldNameString  = occurSplit.getRemainder();
        if (fieldNames != null) fieldNames.add(fieldNameString);

        final String[] phraseTerms = termString.split("_");

        if (fieldIdClass == null) {
          result.add(LuceneUtils.toQuery(fieldNameString, phraseTerms, occur == BooleanClause.Occur.MUST_NOT ? null : termCollector), occur);
        }
        else {
          final Query curQuery = toQuery(fieldIdClass, fieldNameString, phraseTerms, occur == BooleanClause.Occur.MUST_NOT ? null : termCollector);
          if (curQuery != null) {
            result.add(curQuery, occur);
          }
        }
      }
    }

    return result;
  }

  private static final String[] splitTerms(String queryString) {
    // essentially "queryString.split("[^\\w\\d:+^-]+");", which doesn't work
    // when there are asian chars.
    final List<String> result = new ArrayList<String>();
    final StringBuilder builder = new StringBuilder();
    final int len = queryString.length();
    for (int i = 0; i < len; ++i) {
      final int cp = queryString.codePointAt(i);
      if (!Character.isLetterOrDigit(cp) && cp != ':' && cp != '+' && cp != '^' && cp != '-') {
        if (builder.length() > 0) {
          result.add(builder.toString());
          builder.setLength(0);
        }
      }
      else {
        builder.appendCodePoint(cp);
      }
    }
    if (builder.length() > 0) {
      result.add(builder.toString());
    }
    return result.toArray(new String[result.size()]);
  }

  public static final Query toQuery(Class fieldIdClass, String fieldNameString, String[] phraseTerms, Collection<String> termCollector) {
    Query result = null;

    final LuceneFieldId fieldId = getFieldId(fieldIdClass, fieldNameString);

    if (fieldId == null) {
      System.err.println(new Date() + ": ERROR! : LuceneUtils.toQuery : can't build fieldId for class=" + fieldIdClass + " name=" + fieldNameString + "!");
    }
    else {
      try {
        result = fieldId.getQuery(phraseTerms, termCollector);
      }
      catch (NumberFormatException nfe){
        System.err.println(new Date() + ": ERROR! : LuceneUtils.toQuery : expected numeric value for class=" + fieldIdClass + " name=" + fieldNameString + "!: " + nfe.getMessage());
      }
    }

    return result;
  }

  public static final Query toQuery(Class fieldIdClass, String[] fieldNameStrings, String[] phraseTerms, BooleanClause.Occur occur, Collection<String> termCollector) {
    final BooleanQuery result = new BooleanQuery();

    // NOTE: overriding occur is giving expected behavior (in SearchParser)! test and fix.
    occur = BooleanClause.Occur.SHOULD;

    for (String fieldNameString : fieldNameStrings) {
      Query curQuery = toQuery(fieldIdClass, fieldNameString, phraseTerms, occur == BooleanClause.Occur.MUST_NOT ? null : termCollector);
      if (curQuery != null) {
        result.add(curQuery, occur);
      }
    }

    return result;
  }

  /**
   * Split out terms of form "M(QUERY)", or return null if there are no
   * such parentheses-delimited terms.
   */
  protected static final String[] splitSimpleQueries(String queryString) {
    List<String> pieces = null;

    for (int lpPos = queryString.indexOf('('); lpPos >= 0; lpPos = queryString.indexOf('(')) {

      // move back over marker if present
      if (lpPos > 0) {
        final char c = queryString.charAt(lpPos - 1);
        if (c == '+' || c == '^') --lpPos;
      }

      int rpPos = queryString.indexOf(')') + 1;  // rightParen
      if (rpPos == 0) rpPos = queryString.length();

      final String simpleQuery = queryString.substring(lpPos, rpPos);
      if (pieces == null) pieces = new ArrayList<String>();
      pieces.add(simpleQuery);

      queryString = queryString.substring(rpPos);
    }

    return pieces == null ? null : pieces.toArray(new String[pieces.size()]);
  }


  public static final LuceneFieldId getFieldId(Class fieldIdClass, String fieldNameString) {
    return (LuceneFieldId)ReflectUtil.getInstance(fieldIdClass, "getLuceneFieldId", fieldNameString);
  }


  /**
   * Container for a fieldId class name and the remaining string.
   */
  protected static final class FieldIdSplit {

    private String fieldIdName;
    private Class fieldIdClass;
    private String remainder;

    /**
     * Split "fieldIdClass/remainder" from the string.
     */
    public FieldIdSplit(String string) {
      this.fieldIdName = null;
      this.fieldIdClass = null;
      this.remainder = string;

      final int slashPos = string.indexOf('/');
      if (slashPos > 0) {
        this.fieldIdName = string.substring(0, slashPos);
        this.fieldIdClass = ReflectUtil.getClass(fieldIdName);
        this.remainder = string.substring(slashPos + 1);
      }
    }

    public String getFieldIdName() {
      return fieldIdName;
    }

    public Class getFieldIdClass() {
      return fieldIdClass;
    }

    public String getRemainder() {
      return remainder;
    }
  }

  /**
   * Container for an occur and the remaining string.
   */
  protected static final class OccurSplit {
    private BooleanClause.Occur occur;
    private String remainder;

    /**
     * Split the boolean clause occur type corresponding to the marker
     * string's character at position 0 from the remainder of the string.
     * <p>
     * Where '+' means MUST, '^' means MUST_NOT, and anything else
     * means SHOULD.
     */
    public OccurSplit(String string) {
      this.occur = BooleanClause.Occur.SHOULD;
      this.remainder = string;

      int endPos = string.length();
      if (endPos > 0) {
        int charPos = 0;

        char firstChar = string.charAt(charPos);
        if (firstChar == '+') {
          ++charPos;
          occur = BooleanClause.Occur.MUST;
        }
        else if (firstChar == '^') {
          ++charPos;
          occur = BooleanClause.Occur.MUST_NOT;
        }

        if (endPos > charPos) {
          firstChar = string.charAt(charPos);
          if (firstChar == '(') {
            ++charPos;
            endPos = string.indexOf(')');
            if (endPos == 0) endPos = string.length();
          }
        }

        this.remainder = string.substring(charPos, endPos);
      }
    }

    public BooleanClause.Occur getOccur() {
      return occur;
    }

    public String getRemainder() {
      return remainder;
    }
  }
}
