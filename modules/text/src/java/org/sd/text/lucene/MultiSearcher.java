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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A wrapper around multiple lucene searchers for query.
 * <p>
 * @author Spence Koehler
 */
public class MultiSearcher {

  private String luceneId;
  private ArrayList<Searcher> searchers;
  private ArrayList<Searcher> clusterSearchers;
  private List<Searcher> extraSearchers;
  private SearcherType searcherType;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);

  private static final MultiSearcherSettingsHelper MSSH = MultiSearcherSettingsHelper.getInstance();

  public MultiSearcher(String luceneId, SearcherType searcherType) {
    this(luceneId, null, searcherType);
  }

  public MultiSearcher(String luceneId, Searcher[] clusterSearchers, SearcherType searcherType) {
    this.luceneId = luceneId;
    this.searchers = new ArrayList<Searcher>();

    this.clusterSearchers = new ArrayList<Searcher>();
    this.extraSearchers = MSSH.getSearchers(luceneId);

    this.searcherType = searcherType;

    if (clusterSearchers != null) {
      for (Searcher searcher : clusterSearchers) {
        if (searcher != null) {
          this.clusterSearchers.add(searcher);
        }
      }
    }

    this.searchers.addAll(this.clusterSearchers);
    if (extraSearchers != null) this.searchers.addAll(this.extraSearchers);
  }

  public void setSearcherType(SearcherType searcherType) {
    this.searcherType = searcherType;
  }

  public SearcherType getSearcherType() {
    return searcherType;
  }

  public String getLuceneId() {
    return luceneId;
  }

  public int size() {
    if (MSSH.hasNewSearchers()) {
      this.extraSearchers = MSSH.getSearchers(luceneId);

      this.searchers.clear();
      this.searchers.addAll(clusterSearchers);
      if (extraSearchers != null) this.searchers.addAll(extraSearchers);
    }

    return searchers.size();
  }

  /**
   * Get the searcher at the given position.
   * <p>
   * If the searcher's backing directory no longer exists, then close and disable
   * it. A null entry will then exist at the given index.
   */
  public Searcher get(int index) {
    Searcher result = index < size() ? searchers.get(index) : null;

    if (result != null) {
      if (!result.getDirPath().exists()) {

        // disable index that has disappeared.
        searchers.set(index, null);

        if (index < clusterSearchers.size()) clusterSearchers.set(index, null);
        else extraSearchers.set(index - clusterSearchers.size(), null);

        try {
          result.close();
        }
        catch (IOException e) {
          System.err.println(new Date() + ": WARNING : MultiSearcher couldn't close disappeared index at '" +
                             result.getDirPath() + "'!");
          e.printStackTrace(System.err);
        }
        result = null;
      }
    }

    return result;
  }

  public void addSearcher(File luceneDir) {
    if (isClosed.get()) return;

    final Searcher searcher = SearcherFactory.getSearcher(luceneDir, searcherType);

    try {
      searcher.open();
      this.clusterSearchers.add(searcher);

      this.searchers.clear();
      this.searchers.addAll(clusterSearchers);
      if (extraSearchers != null) this.searchers.addAll(extraSearchers);
    }
    catch (FileNotFoundException fnfe) {
      // this means we didn't add anything to an index, which happens i.e. if we didn't have any asian (or non-asian) posts in a batch.
      // so what we'll do is log it and ignore this index.
      System.err.println(new Date() + ": WARNING : Ignoring empty index at '" + luceneDir + "'!");
    }
    catch (IOException e) {
      System.err.println(new Date() + ": *** ERROR! " + luceneId + " can't open lucene searcher at '" + luceneDir + "'");
      e.printStackTrace(System.err);
    }
  }

  public void addSearcher(Searcher searcher) {
    if (isClosed.get()) return;

    this.clusterSearchers.add(searcher);
    this.searchers.clear();
    this.searchers.addAll(clusterSearchers);
    if (extraSearchers != null) this.searchers.addAll(extraSearchers);
  }

  public void removeSearcher(Searcher searcher) {
    this.clusterSearchers.remove(searcher);
    this.extraSearchers.remove(searcher);
    this.searchers.remove(searcher);
  }

  public void close() {
    if (isClosed.compareAndSet(false, true)) {
      for (Searcher searcher : searchers) {
        if (searcher != null) {
          try {
            searcher.close();
          }
          catch (IOException e) {
            System.err.println(new Date() + ": *** WARNING: " + luceneId + " unable to close searcher '" + searcher.getDirPath() + "'!");
          }
        }
      }
    }
  }
}
