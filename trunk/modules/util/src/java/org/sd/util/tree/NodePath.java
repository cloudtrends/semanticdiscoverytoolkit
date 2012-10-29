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
package org.sd.util.tree;


import org.sd.util.MathUtil;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a path of nodes through a tree.
 * <p>
 * @author Spence Koehler
 */
public class NodePath<T> {
  
  private List<PathElement<T>> path;

  private final DataMatcherMaker<T> defaultDataMatcherMaker = new DataMatcherMaker<T>() {
    public DataMatcher<T> makeDataMatcher(final String dataString) {
      return new DataMatcher<T>() {
        public boolean matches(Tree<T> node) {
          if ("*".equals(dataString)) return true;
          final T dataInNode = node.getData();
          boolean result = dataInNode == dataString;
          if (!result && dataInNode != null) {
            final String string = dataInNode.toString();
            result = string.equals(dataString);
          }
          return result;
        }
      };
    }
  };

  private final PatternSplitter defaultPatternSplitter = new PatternSplitter() {
      public String[] split(String patternString) {
        return patternString.split("\\.");
      }
    };

  /**
   * Default constructor. Build the path using the add methods.
   */
  public NodePath() {
    this.path = new ArrayList<PathElement<T>>();
  }

  /**
   * Path pattern constructor.
   * <p>
   * Constructs a path from a string of the form p1.p2. ... .pN
   * <p>
   * where pi is of the form:
   * <ul>
   * <li>"x" -- matches when x.equals(node.getData().toString()) or </li>
   * <li>"x[subscript]" -- matches as for "x" AND the 0-based local sibling
   *                       index is a member of the comma-delimited list of
   *                       'subscript' integers and/or hyphenated ranges or </li>
   * <li>"**" -- matches any "x" down any number of nodes along the path.</li>
   * <li>"*" -- mathces any "x" for the current path node.</li>
   * </ul>
   */
  public NodePath(String patternString) {
    this();
    buildNodePath(patternString, defaultDataMatcherMaker, null);
  }

  /**
   * Path pattern constructor.
   * <p>
   * Constructs a path from a string of the form p1.p2. ... .pN
   * <p>
   * where pi is of the form:
   * <ul>
   * <li>"x" -- matches when the dataMatcher created with x matches the node's data or </li>
   * <li>"x[subscript]" -- matches as for "x" AND the 0-based local sibling
   *                       index is a member of the comma-delimited list of
   *                       'subscript' integers and/or hyphenated ranges or </li>
   * <li>"**" -- matches any "x" down any number of nodes along the path.</li>
   * <li>"*" -- mathces any "x" for the current path node.</li>
   * </ul>
   */
  public NodePath(String patternString, DataMatcherMaker<T> dataMatcherMaker) {
    this();
    buildNodePath(patternString, dataMatcherMaker, null);
  }

  /**
   * Path pattern constructor.
   * <p>
   * Constructs a path from a string of the form p1.p2. ... .pN
   * <p>
   * where pi is of the form:
   * <ul>
   * <li>"x" -- matches when the dataMatcher created with x matches the node's data or </li>
   * <li>"x[subscript]" -- matches as for "x" AND the 0-based local sibling
   *                       index is a member of the comma-delimited list of
   *                       'subscript' integers and/or hyphenated ranges or </li>
   * <li>"**" -- matches any "x" down any number of nodes along the path.</li>
   * <li>"*" -- mathces any "x" for the current path node.</li>
   * </ul>
   */
  public NodePath(String patternString, DataMatcherMaker<T> dataMatcherMaker, PatternSplitter patternSplitter) {
    this();
    buildNodePath(patternString, dataMatcherMaker, patternSplitter);
  }

  private final void buildNodePath(String patternString, DataMatcherMaker<T> dataMatcherMaker, PatternSplitter patternSplitter) {
    if (patternSplitter == null) patternSplitter = defaultPatternSplitter;
    final String[] pieces = patternSplitter.split(patternString);
    for (String piece : pieces) {
      if ("**".equals(piece)) {
        // add "skip" path element
        add();
      }
      else {
        final int lsbPos = piece.indexOf('[');  // left square bracket position
        int[] indeces = null;
        if (lsbPos >= 0) {
          // add with subscripts
          final int rsbPos = piece.indexOf(']', lsbPos + 1);
          final String indecesString = piece.substring(lsbPos + 1, rsbPos);
          try {
            indeces = MathUtil.parseIntegers(indecesString);
          }
          catch (NumberFormatException e) {
            //indeces not integers, leave as null
          }
          piece = piece.substring(0, lsbPos);
        }

        // add match
        final DataMatcher<T> dataMatcher = dataMatcherMaker.makeDataMatcher(piece);
        add(dataMatcher, indeces);
      }
    }
  }

  /**
   * Get the length of this NodePath.
   */
  public int length() {
    return path.size();
  }

  /**
   * Add an element that matches when the given data equals the data in a node.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   *
   * @param dataToMatch  data to match when .equals a node's data (ok if null to match null data in a node).
   * @param indeces      indeces for siblings matching dataToMatch to extract (match all indeces if null).
   *
   * @return the element that is added to the path.
   */
  public PathElement<T> add(T dataToMatch, int[] indeces) {
    final PathElement<T> result = new PathElement<T>();
    result.setDataMatcher(dataToMatch);
    setSubscripts(result, indeces);
    this.path.add(result);
    return result;
  }

  /**
   * Add an element that matches when the given dataMatcher matches the data in a node.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   *
   * @param dataMatcher  data matcher applied to a node's data (ok if null to match null data in a node).
   * @param indeces      indeces for siblings matching dataToMatch to extract (match all indeces if null).
   *
   * @return the element that is added to the path.
   */
  public PathElement<T> add(DataMatcher<T> dataMatcher, int[] indeces) {
    final PathElement<T> result = new PathElement<T>();
    result.setDataMatcher(dataMatcher);
    setSubscripts(result, indeces);
    this.path.add(result);
    return result;
  }

  /**
   * Add an element that matches when the given pattern matches the data.toString() in a node.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   *
   * @param pattern  pattern to match against a node's data.toString(); matches any (including null)
   *                 node data if pattern is null; doesn't match null data when pattern is non-null.
   * @param indeces  indeces for siblings matching dataToMatch to extract (match all indeces if null).
   *
   * @return the element that is added to the path.
   */
  public PathElement<T> add(Pattern pattern, int[] indeces) {
    final PathElement<T> result = new PathElement<T>();
    result.setDataMatcher(pattern);
    setSubscripts(result, indeces);
    this.path.add(result);
    return result;
  }

  /**
   * Add an element that matches when the given string equals the data.toString() in a node.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   *
   * @param dataString  string to .equals against a node's data.toString() (ok if null to match null data in a node)
   * @param indeces     indeces for siblings matching dataToMatch to extract (match all indeces if null).
   *
   * @return the element that is added to the path.
   */
  public PathElement<T> add(int[] indeces, String dataString) {
    final PathElement<T> result = new PathElement<T>();
    result.setDataMatcher(dataString);
    setSubscripts(result, indeces);
    this.path.add(result);
    return result;
  }

  /**
   * Add an element that designates to skip 0 or more levels in the tree until
   * the next path element matches. If there are no more path elements, this
   * designates to gather all leaves for the nodes that match to this element
   * in the path.
   */
  public void add() {

    // check for repeat ... doesn't make sense to add this twice in a row.
    final int pathLen = path.size();
    if (pathLen > 0) {
      final PathElement<T> element = path.get(pathLen - 1);
      if (element == null) return;
    }

    this.path.add(null);  // 'null' flags skipping 0 or more levels
  }

  /**
   * Applying this node path to a node extracts the end nodes that match this path.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   */
  public List<Tree<T>> apply(Tree<T> node) {
    return apply(node, path.size());
  }

  /**
   * Apply only the first numPathElements elements of this path to the given node
   * to extract from the given node.
   */
  public List<Tree<T>> apply(Tree<T> node, int numPathElements) {
    final List<Tree<T>> result = new ArrayList<Tree<T>>();
    apply(node, 0, 0, numPathElements, result);
    return result.size() > 0 ? result : null;
  }

  /**
   * Convenience method to get just the first matching node, or null.
   */
  public Tree<T> getFirst(Tree<T> node) {
    Tree<T> result = null;

    final List<Tree<T>> matches = apply(node);
    if (matches != null && matches.size() > 0) {
      result = matches.get(0);
    }

    return result;
  }

  public DataMatcher<T> getDataMatcher(int pathElementNum) {
    final PathElement<T> pathElement = path.get(pathElementNum);
    return pathElement.getDataMatcher();
  }

  /**
   * Recursive auxiliary to apply.
   *
   * @param node          the node to test.
   * @param pathIndex     the index of the element in the path to match.
   * @param siblingIndex  the sibling index of the node.
   * @param result        accumulator for results.
   *
   * @return nodes that match this path.
   */
  private final void apply(Tree<T> node, int pathIndex, int siblingIndex, int pathLen, List<Tree<T>> result) {
    if (pathIndex >= pathLen) return;

    PathElement<T> element = path.get(pathIndex);
    boolean recurse = false;
    boolean multilevelMatch = false;

    if (element == null) {
      multilevelMatch = true;
      if ((pathIndex + 1) < pathLen) {
        element = path.get(pathIndex + 1);
      }
      else {
        // multilevel match at the end of the path means to collect all leaf nodes at or under this node
        result.addAll(node.gatherLeaves());
        return;
      }
    }

    if (element.matches(node, siblingIndex)) {  // found a match
      final int increment = multilevelMatch ? 2 : 1;
      if ((pathIndex + increment) == pathLen) {  // found one!
        if (result == null) result = new ArrayList<Tree<T>>();
        result.add(node);
      }
      else {  // increment pathIndex and recurse
        pathIndex = pathIndex + increment;
        recurse = true;
      }
    }
    else {  // didn't match
      // if multilevelMatch, recurse without incrementing pathIndex
      if (multilevelMatch) recurse = true;
    }

    if (recurse && pathIndex < pathLen) {
      final List<Tree<T>> children = node.getChildren();
      if (children != null) {
        int index = 0;
        for (Tree<T> child : children) {
          apply(child, pathIndex, index++, pathLen, result);
        }
      }
    }
  }

  private final void setSubscripts(PathElement<T> element, int[] indeces) {
    // note: subscripts are ignored for the first element in the path.
    element.setSubscripts(path.size() == 0 ? null : indeces);
  }

  public static interface DataMatcherMaker<T> {
    public DataMatcher<T> makeDataMatcher(String dataString);
  }

  public static interface PatternSplitter {
    public String[] split(String patternString);
  }

  /**
   * Class to represent an element in the path.
   * <p>
   * A node path element defines when a node's data is a match.
   *
   * @author Spence Koehler
   */
  public class PathElement<T> {
    private DataMatcher<T> dataMatcher; // if null, any data matches
    private BitSet subscripts;          // if null, get all siblings; else get nth MATCHING sibling(s) [0-based]

    public PathElement() {  // match anything
      dataMatcher = null;
      subscripts = null;
    }

    protected DataMatcher<T> getDataMatcher() {
      return dataMatcher;
    }

    public void setDataMatcher(final String dataString) {
      this.dataMatcher = new DataMatcher<T>() {
        public boolean matches(Tree<T> node) {
          final T dataInNode = node.getData();
          boolean result = dataInNode == dataString;
          if (!result && dataInNode != null) {
            final String string = dataInNode.toString();
            result = string.equals(dataString);
          }
          return result;
        }
      };
    }

    public void setDataMatcher(final Pattern pattern) {
      if (pattern == null) this.dataMatcher = null;
      else {
        this.dataMatcher = new DataMatcher<T>() {
          public boolean matches(Tree<T> node) {
            final T dataInNode = node.getData();
            boolean result = false;
            if (dataInNode != null) {
              final String string = dataInNode.toString();
              final Matcher matcher = pattern.matcher(string);
              result = matcher.matches();
            }
            return result;
          }
        };
      }
    }

    public void setDataMatcher(final T dataToMatch) {  // set to match (equals) the given data
      this.dataMatcher = new DataMatcher<T>() {
        public boolean matches(Tree<T> node) {
          final T dataInNode = node.getData();
          return (dataToMatch == dataInNode) || (dataToMatch != null && dataToMatch.equals(dataInNode));
        }
      };
    }

    public void setDataMatcher(DataMatcher<T> dataMatcher) {  // set this element's data matcher
      this.dataMatcher = dataMatcher;
    }

    /**
     * Set the subscripts that match this element.
     *
     * @param indeces specifies which subscript indeces match; if null, all indeces are accepted.
     */
    public void setSubscripts(int[] indeces) {  // set to match the given subscripts
      if (indeces != null) {
        for (int index : indeces) {
          addSubscript(index);
        }
      }
      else {
        this.subscripts = null;
      }
    }

    public void addSubscript(int subscript) {  // add the given subscript among those included for this element
      if (subscripts == null) subscripts = new BitSet();
      subscripts.set(subscript);
    }

    public boolean matches(Tree<T> node, int siblingIndex) {
      return subscriptMatches(siblingIndex) && dataMatches(node);
    }

    public boolean dataMatches(Tree<T> node) {
      return (dataMatcher == null) || dataMatcher.matches(node);
    }

    public boolean subscriptMatches(int index) {
      return (subscripts == null) || subscripts.get(index);
    }
  }
  
  /**
   * Interface for matching data in a node.
   * <p>
   * @author Spence Koehler
   */
  public static interface DataMatcher<T> {
    public boolean matches(Tree<T> data);
  }

  /**
   * Interface for matching a node's subscript.
   * <p>
   * Note: this allows a function to be the critereon for matching a subscript.
   *       for example, match all even or odd subscripts, etc.
   *
   * @author Spence Koehler
   */
  public static interface SubscriptMatcher {
    public boolean matches(int subscript);
  }
}
