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
package org.sd.xml.record;


import org.sd.util.Histogram;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility to use a histogram of paths to identify common paths.
 * <p>
 * @author Spence Koehler
 */
public class PathHistogram {
  
  private Record.View view;
  private Record record;
  private List<Record.Field> fields;
  private Histogram<Path> h;
  private Histogram<Integer> f;
  private boolean excludeText;

  public PathHistogram(Record record) {
    this(record, new Record.View(false, Record.DEFAULT_HTML_VIEW_STRINGS), true);
  }

  public PathHistogram(Record record, Record.View view, boolean excludeText) {
    this.view = view;
    this.record = record;
    this.excludeText = excludeText;
    this.fields = record.getFields(view);
    this.h = loadHistogram(fields);
    this.f = buildFreqHistogram(h);
  }

  /**
   * Get an iterator over the record's path groups from the group having
   * the most paths down to that having the least.
   * <p>
   * From a path group, the consecutive groups of fields matching the paths
   * can be iterated over.
   */
  public Iterator<PathGroup> iterator() {
    return new PathGroupIterator();
  }

  /**
   * Build a histogram of the (field) paths according to the view of the record.
   */
  private final Histogram<Path> loadHistogram(List<Record.Field> fields) {
    final Histogram<Path> result = new Histogram<Path>();

    if (fields != null) {
      for (Record.Field field : fields) {
        result.add(new Path(field, excludeText));
      }
    }    

    return result;
  }

  /**
   * Build a histogram of the frequencies of paths in the path histogram.
   * <p>
   * This gives us the number of paths that share a common frequency in
   * its frequencies.
   */
  private final Histogram<Integer> buildFreqHistogram(Histogram<Path> h) {
    final Histogram<Integer> result = new Histogram<Integer>();

    final long numRanks = h.getNumRanks();
    for (long rank = 0; rank < numRanks; ++rank) {
      final long rankFreq = h.getFrequencyCount(rank);
      if (rankFreq > 1) {  // only add in for path that occur more than once
        result.add((int)rankFreq);
      }
    }

    return result;
  }

  /**
   * Get the paths that occur with the given frequency.
   */
  private final List<Path> getPathsWithFrequency(int freq) {
    final List<Path> result = new ArrayList<Path>();

    final long numRanks = h.getNumRanks();
    for (long rank = 0; rank < numRanks; ++rank) {
      final long rankFreq = h.getFrequencyCount(rank);
      if (rankFreq == freq) {
        result.add(h.getElement(rank));
      }
    }

    return result;
  }

  
  /**
   * Container for a view of a record field suitable for use in a histogram.
   * <p>
   * Note that use in the histogram requires a meaningful equals and hashCode
   * to be defined.
   */
  public static final class Path {
    public final Record.Field field;
    public final boolean excludeText;
    public final String fieldString;

    public Path(Record.Field field, boolean excludeText) {
      this.field = field;
      this.excludeText = excludeText;
      this.fieldString = excludeText ? field.attribute : field.toString();
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Path) {
        final Path other = (Path)o;
        result = fieldString.equals(other.fieldString);
      }

      return result;
    }

    public int hashCode() {
      return fieldString.hashCode();
    }

    public String toString() {
      return fieldString;
    }
  }

  public final class FieldGroup {
    public final PathGroup pathGroup;        // the pathGroup used to select fields.
    public final List<Record.Field> fields;  // the fields matching the pathGroup
    private Record _record;

    public FieldGroup(List<Record.Field> fields, PathGroup pathGroup) {
      this.fields = fields;
      this.pathGroup = pathGroup;
    }

    /**
     * Determine whether a field was found for each path.
     */
    public boolean isComplete() {
      return fields.size() == pathGroup.size();
    }

    /**
     * Get a record encapsulating all of this group's fields.
     */
    public Record getRecord() {
      if (_record == null) {
        _record = Record.buildRecord(fields);
      }
      return _record;
    }
  }

  private final class FieldGroupIterator implements Iterator<FieldGroup> {

    private PathGroup pathGroup;
    private Iterator<Record.Field> iter;
    private Record.Field nextField;
    private FieldGroup nextGroup;

    FieldGroupIterator(PathGroup pathGroup) {
      this.pathGroup = pathGroup;
      this.iter = fields.iterator();
      this.nextField = iter.hasNext() ? iter.next() : null;
      this.nextGroup = loadNextGroup();
    }

    public boolean hasNext() {
      return this.nextGroup != null;
    }

    public FieldGroup next() {
      final FieldGroup result = nextGroup;

      if (nextGroup != null) {
        nextGroup = loadNextGroup();
      }

      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }

    private final FieldGroup loadNextGroup() {
      FieldGroup result = null;

      // collect record fields until we've seen one and only one example
      // from every path in the path group
      final List<Record.Field> collectedFields = new ArrayList<Record.Field>();
      final Set<Path> foundPaths = new HashSet<Path>();

      boolean done = false;
      while (nextField != null && !done) {
        final Path curPath = new Path(nextField, excludeText);

        if (pathGroup.contains(curPath)) {
          if (!foundPaths.add(curPath)) {
            // already saw this path and haven't seen all of the others yet
            break;  // break out without incrementing so next pass picks this up.
          }
          collectedFields.add(curPath.field);
          
          if (collectedFields.size() == pathGroup.size()) {
            // we've collected one of each path
            done = true;  // don't break out so we will increment.
          }
        }

        // increment
        nextField = iter.hasNext() ? iter.next() : null;
      }

      // build a FieldGroup from the collected information
      if (collectedFields.size() > 0) {
        result = new FieldGroup(collectedFields, pathGroup);
      }

      return result;
    }
  }

  public static class FieldGroupCollector {
    private List<FieldGroup> completeFieldGroups;
    private List<FieldGroup> incompleteFieldGroups;
    private List<Record> _recordsWithIncomplete;
    private List<Record> _recordsWithoutIncomplete;

    public FieldGroupCollector() {
      this.completeFieldGroups = new ArrayList<FieldGroup>();
      this.incompleteFieldGroups = new ArrayList<FieldGroup>();
      this._recordsWithIncomplete = null;
      this._recordsWithoutIncomplete = null;
    }

    /**
     * Add the field group to this collector.
     * 
     * @return true if the added field group was "complete"; otherwise, false.
     */
    public boolean add(FieldGroup fieldGroup) {
      boolean result = false;

      if (fieldGroup.isComplete()) {
        completeFieldGroups.add(fieldGroup);
        result = true;
      }
      else {
        incompleteFieldGroups.add(fieldGroup);
      }

      // flag records for recompute
      this._recordsWithIncomplete = null;
      this._recordsWithoutIncomplete = null;

      return result;
    }

    /**
     * Add all field groups from the path group's iterator.
     *
     * @return true if all groups were "complete"; otherwise, false.
     */
    public boolean addAll(PathGroup pathGroup) {
      boolean result = true;

      for (Iterator<FieldGroup> iter = pathGroup.iterator(); iter.hasNext(); ) {
        final FieldGroup fieldGroup = iter.next();
        result &= add(fieldGroup);
      }

      return result;
    }

    /**
     * Get the extrapolated records from the fields groups.
     * 
     * @param includeIncomplete  If true, then incomplete records will be used
     *                           to compute all records. Note that incomplete
     *                           records may be in the result set regardless of
     *                           whether they are used to compute the records
     *                           due to extrapolation.
     *
     * @return the extrapolated records.
     */
    public List<Record> getRecords(boolean includeIncomplete) {
      List<Record> result = null;

      // find the deepest common parent of all of the field group records
      // and build a record from each of that node's children.

      if (includeIncomplete) {
        if (_recordsWithIncomplete == null) {
          _recordsWithIncomplete = buildRecords(includeIncomplete);
        }
        result = _recordsWithIncomplete;
      }
      else {
        if (_recordsWithoutIncomplete == null) {
          _recordsWithoutIncomplete = buildRecords(includeIncomplete);
        }
        result = _recordsWithoutIncomplete;
      }

      return result;
    }

    /**
     * Empty out this instance.
     */
    public void clear() {
      completeFieldGroups.clear();
      incompleteFieldGroups.clear();
      this._recordsWithIncomplete = null;
      this._recordsWithoutIncomplete = null;
    }

    /**
     * Find the deepest common parent of all of the field group records
     * and build a record form each of that node's children.
     */
    private final List<Record> buildRecords(boolean includeIncomplete) {
      final List<Record> result = new ArrayList<Record>();

      final int[] count = new int[]{0};
      Tree<XmlLite.Data> root = getRoot(completeFieldGroups, null, count);
      if (includeIncomplete) {
        root = getRoot(incompleteFieldGroups, root, count);
      }

      if (root != null) {
        if (root.numChildren() <= 1) {
          // walk up the resulting tree while it has no siblings
          while (root.getNumSiblings() == 1 && root.getParent() != null) {
            root = root.getParent();
          }
          
          result.add(Record.getRecord(root));
        }
        else {
          // add one record for each child.
          for (Tree<XmlLite.Data> child : root.getChildren()) {
            result.add(Record.getRecord(child));
          }
        }
      }

      return result;
    }

    /**
     * Get the deepest common parent among records from the field groups,
     * but only as long as it doesn't lose any distinct records.
     */
    private final Tree<XmlLite.Data> getRoot(List<FieldGroup> fieldGroups, Tree<XmlLite.Data> root, int[] count) {
      for (FieldGroup fieldGroup : fieldGroups) {
        final Record record = fieldGroup.getRecord();
        final Tree<XmlLite.Data> curTree = record.asTree();

        if (root == null) {
          root = curTree;
        }
        else {
          final Tree<XmlLite.Data> ancestor = curTree.getDeepestCommonAncestor(root);
          if (ancestor != null) {
            if (ancestor.numChildren() >= count[0] + 1) {
              root = ancestor;
              ++count[0];
            }
            // else, don't move up to this ancestor.
          }
        }
        if (root == null) throw new IllegalStateException();
      }
      return root;
    }
  }

  /**
   * Container for a group of paths.
   */
  public final class PathGroup {
    public final List<Path> paths;
    public final int freq;
    public final int rank;

    public PathGroup(List<Path> paths, int freq, int rank) {
      this.paths = paths;
      this.freq = freq;
      this.rank = rank;
    }

    /**
     * Get an iterator over the field groups for this path group.
     * <p>
     * Specifically, iterate over the groups of record fields that match the
     * paths in this group.
     */
    public Iterator<FieldGroup> iterator() {
      return new FieldGroupIterator(this);
    }

    /**
     * Return the number of paths in this group.
     */
    public int size() {
      return paths.size();
    }

    /**
     * Determine whether the given path is present in this group.
     */
    public boolean contains(Path path) {
      return paths.contains(path);
    }
  }

  /**
   * Iterator over path groups.
   */
  private final class PathGroupIterator implements Iterator<PathGroup> {

    private int curRank;

    PathGroupIterator() {
      this.curRank = 0;
    }

    public boolean hasNext() {
      return this.curRank < f.getNumRanks();
    }

    public PathGroup next() {
      PathGroup result = null;

      if (hasNext()) {
        final int freq = f.getElement(curRank);
        final List<Path> paths = getPathsWithFrequency(freq);
        result = new PathGroup(paths, freq, curRank);
        ++curRank;
      }

      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }
  }
}
