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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import org.sd.util.StringUtil;

/**
 * Base class to handle the parsing and interpretation of human queries and converting them to lucene queries.
 * 
 * @author Dave Barney
 */
public abstract class SearchParser {
  private HashMap<String,String> humanFieldToLuceneFieldMap;
  private String[] defaultSearchFieldNames;
  private Class fieldIdClass;
  
  public SearchParser(HashMap<String, String> humanFieldToLuceneFieldMap, String[] defaultFieldNames, Class fieldIdClass) {
    this.humanFieldToLuceneFieldMap = humanFieldToLuceneFieldMap;
    
    this.defaultSearchFieldNames = new String[defaultFieldNames.length];
    for (int i=0; i < defaultFieldNames.length; i++) {
      this.defaultSearchFieldNames[i] = defaultFieldNames[i];
    }

    this.fieldIdClass = fieldIdClass;
  }
  
  /**
   * Parse the human query into a lucene query.
   * <p>
   * If termCollector is non-null, collect query terms in it, discarding "not" terms.
   */
  public Query parseQuery(String humanQuery, Collection<String> termCollector, Collection<String> fieldCollector) {
    return parseQuery(StringUtil.trim(humanQuery), null, null, termCollector, fieldCollector);
  }
  
  private Query parseQuery(String humanQuery, BooleanClause.Occur occur, String[] fieldNames, Collection<String> termCollector, Collection<String> fieldCollector) {
    QueryHolder result = new QueryHolder();

    // default behavior is "MUST" (AND)
    if (occur == null) occur = BooleanClause.Occur.MUST;
    
    //default fieldName comes from construction
    if (fieldNames == null) fieldNames = defaultSearchFieldNames;
    
    while (humanQuery.length() > 0) {
      final char c1 = humanQuery.charAt(0);
      boolean normal = false;
  
      switch (c1) {
        case '+':
          humanQuery = humanQuery.substring(1);
          if (humanQuery.length() == 0) return result.toQuery();
          humanQuery = handleNestedTerm(humanQuery, BooleanClause.Occur.MUST, fieldNames, result, termCollector, fieldCollector);
          break;
          
        case '-':
          humanQuery = humanQuery.substring(1);
          if (humanQuery.length() == 0) return result.toQuery();
          // NOTE: termCollector is null in following so we don't collect 'not' terms.
          humanQuery = handleNestedTerm(humanQuery, BooleanClause.Occur.MUST_NOT, fieldNames, result, null/*termCollector*/, null/*fieldCollector*/);
          break;
          
        case '(':
        case '[':
          final int matchingParen = findMatchingParen(humanQuery);
          if (matchingParen > 1) {
            final String parenSearch = humanQuery.substring(1, matchingParen);
            final boolean isNot = occur == BooleanClause.Occur.MUST_NOT;
            result.add(parseQuery(parenSearch, null, fieldNames, isNot ? null : termCollector, isNot ? null : fieldCollector), occur);
            
            if (matchingParen < humanQuery.length()-1) humanQuery = humanQuery.substring(matchingParen+2);
            else humanQuery = "";
          } else {
            humanQuery = humanQuery.substring(1);
            normal = true;
          }
          break;
          
        case '"':
        case '\'':
          final int secondQuote = humanQuery.indexOf(c1, 1);
          if (secondQuote > 0) {
            final String[] phrase = humanQuery.substring(1, secondQuote).split(" ");
            result.add(LuceneUtils.toQuery(fieldIdClass, fieldNames, phrase, occur, termCollector), occur);
            collectFieldNames(fieldCollector, fieldNames);
          } else {
            humanQuery = humanQuery.substring(1);
            normal = true;
          }
          
          if (secondQuote == humanQuery.length()-1) humanQuery = "";
          else humanQuery = humanQuery.substring(secondQuote+2);
          break;
          
        case 'O':
          if (humanQuery.length() > 3 && humanQuery.charAt(1) == 'R' && humanQuery.charAt(2) == ' ') {
            humanQuery = humanQuery.substring(3);
            result.setNextQueryAsOr();
          } else {
            normal = true;
          }
          break;
  
        default:
          normal = true;
          break;
      }
      
      if (normal) {
        final int colonIndex = humanQuery.indexOf(':');
        final int spaceIndex = humanQuery.indexOf(' ');
        if (colonIndex > 0 && (colonIndex < spaceIndex || spaceIndex < 0)) {
          String field = humanQuery.substring(0, colonIndex).toLowerCase();
          String text = humanQuery.substring(colonIndex+1);
          
          final String currFieldName = lookupField(field);
          if (currFieldName == null) {
            throw new IllegalStateException("Invalid field-type '" + field + "' in query: " + humanQuery);
          }
          final String[] currFieldNames = new String[] { currFieldName };
          if (fieldCollector != null) fieldCollector.add(currFieldName);
         
          humanQuery = handleNestedTerm(text, occur, currFieldNames, result, termCollector, fieldCollector);
        } else {
          int i = 0;
          while (i < humanQuery.length() && humanQuery.charAt(i) != ' ') i++;
          result.add(LuceneUtils.toQuery(fieldIdClass, fieldNames, new String[]{humanQuery.substring(0, i)}, occur, termCollector), occur);
          collectFieldNames(fieldCollector, fieldNames);
          
          if (i < humanQuery.length()) {
            humanQuery = humanQuery.substring(i+1);
          } else {
            humanQuery = "";
          }
        }
      }
      
      occur = BooleanClause.Occur.MUST;
    }
    
    return result.toQuery();
  }

  private final void collectFieldNames(Collection<String> fieldCollector, String[] fieldNames) {
    if (fieldCollector != null) {
      for (String fieldName : fieldNames) {
        fieldCollector.add(fieldName);
      }
    }
  }

  private final String lookupField(String field) {
    String result = humanFieldToLuceneFieldMap.get(field);
    return result == null ? field : result;
  }

  private String handleNestedTerm(String humanQuery, BooleanClause.Occur occur, String[] fieldNames, QueryHolder result, Collection<String> termCollector, Collection<String> fieldCollector) {
    final char char1 = humanQuery.charAt(0);
    switch (char1) {
      case '\'':
      case '"':
        final int secondQuote = humanQuery.indexOf(char1, 1);
        if (secondQuote > 0) {
          final String[] phrase = humanQuery.substring(1, secondQuote).split(" ");
          result.add(LuceneUtils.toQuery(fieldIdClass, fieldNames, phrase, occur, termCollector), occur);
          collectFieldNames(fieldCollector, fieldNames);
        } else {
          throw new IllegalStateException("Invalid query.  Quotes are not matching: " + humanQuery);
        }
        
        if (secondQuote == humanQuery.length()-1) humanQuery = "";
        else humanQuery = humanQuery.substring(secondQuote+2);
        break;

      case '(':
      case '[':
        final int matchingParen = findMatchingParen(humanQuery);
        if (matchingParen > 0) {
          result.add(recursivelyBuild(humanQuery.substring(1, matchingParen), occur, fieldNames, termCollector, fieldCollector), occur);
        } else {
          throw new IllegalStateException("Invalid query.  Parenthesis are not matching: " + humanQuery);
        }
        
        if (matchingParen == humanQuery.length()-1) humanQuery = "";
        else humanQuery = humanQuery.substring(matchingParen+2);
        break;
        
      default:
        final int nextSpace = humanQuery.indexOf(' ');
        if (nextSpace < 0) {
          result.add(recursivelyBuild(humanQuery, occur, fieldNames, termCollector, fieldCollector), occur);
          humanQuery = "";
        } else {
          result.add(recursivelyBuild(humanQuery.substring(0, nextSpace), occur, fieldNames, termCollector, fieldCollector), occur);
          humanQuery = humanQuery.substring(nextSpace+1);
        }
        break;
    }

    return humanQuery;
  }
  
  private final Query recursivelyBuild(String humanQuery, BooleanClause.Occur occur, String[] fieldNames, Collection<String> termCollector, Collection<String> fieldCollector) {
    final boolean isNot = occur == BooleanClause.Occur.MUST_NOT;
    return parseQuery(humanQuery, null, fieldNames, isNot ? null : termCollector, isNot ? null : fieldCollector);
  }

  public static int findMatchingParen(String s) {
    final char c1 = s.charAt(0);
    char c2;
    
    switch (c1) {
      case '[':
        c2 = ']';
        break;
      case '(':
        c2 = ')';
        break;
      default:
        return -1;
    }
    
    int open = 1;
    int i=1;
    for (; i < s.length() && open > 0; i++) {
      if (s.charAt(i) == c1) open++;
      else if (s.charAt(i) == c2) open--;
    }
    i--;
    
    if (open == 0) return i;
    else return -1;
  }
  
  class QueryHolder {
    private ArrayList<Query> queries;
    private ArrayList<BooleanClause.Occur> occurs;
    private boolean nextAsOr;
    
    public QueryHolder() {
      this.queries = new ArrayList<Query>();
      this.occurs = new ArrayList<BooleanClause.Occur>();
      this.nextAsOr = false;;
    }
    
    public void add(Query query, BooleanClause.Occur occur) {
      if (query.toString().length() == 0) return;

      if (nextAsOr && queries.size() > 0) {
        BooleanQuery orQuery = new BooleanQuery();
        final Query orLeft = queries.get(queries.size()-1);
        orQuery.add(orLeft, BooleanClause.Occur.SHOULD);
        orQuery.add(query, BooleanClause.Occur.SHOULD);
        queries.set(queries.size()-1, orQuery);
        nextAsOr = false;
      } else {
        this.queries.add(query);
        this.occurs.add(occur);
      }
    }
    
    public Query toQuery() {
      Query result = null;

      if (queries.size() == 1) {
        final BooleanClause.Occur occur = occurs.get(0);
        if (occur != BooleanClause.Occur.MUST_NOT) {
          result = queries.get(0);
        }
      }

      if (result == null) {
        final BooleanQuery query = new BooleanQuery();
        for (int i=0; i < queries.size(); i++) {
          query.add(queries.get(i), occurs.get(i));
        }
        result = query;
      }

      return result;
    }

    public void print() {
      for (int i=0; i < queries.size(); i++) {
        System.out.println("Query: " + queries.get(i).toString());
      }
    }

    public void setNextQueryAsOr() {
      nextAsOr = true;
    }
  }
}
