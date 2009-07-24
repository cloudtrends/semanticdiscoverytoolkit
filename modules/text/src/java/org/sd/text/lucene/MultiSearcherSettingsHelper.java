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


import org.sd.xml.record.Record;
import org.sd.xml.record.Settings;
import org.sd.util.SdnUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to access multi-searcher settings.
 * <p>
 * @author Spence Koehler
 */
public class MultiSearcherSettingsHelper {
  
  // constants to cross-reference with the "settings" ids
  public static final String POSTS_ID = "posts";
  public static final String SITEGRAPH_ID = "sitegraph";
  public static final String KEYWORD_ID = "keyword";
  public static final String BENCHMARK_ID = "benchmark";

  private static final MultiSearcherSettingsHelper INSTANCE = new MultiSearcherSettingsHelper();

  public static final MultiSearcherSettingsHelper getInstance() {
    return INSTANCE;
  }


  //
  // Settings for an ID are of the form:
  //
  // <id>
  //
  //   <container type="CONSTANT/TRANSIENT">
  //     <path>path</path>
  //     <pattern>pattern</pattern>
  //   </container>
  //
  //   <dir type="CONSTANT/TRANSIENT">luceneDir</dir>
  //
  // </id>


  private Properties properties;
  private Settings settings;

  private MultiSearcherSettingsHelper() {
    this.properties = new Properties();
    this.settings = new Settings(properties, new File(SdnUtil.getSdnResourcePath("settings/multisearcher_settings.xml")));
  }

  public void setProperty(String property, String value) {
    this.properties.setProperty(property, value);
  }

  /**
   * Determine whether new searchers may be available.
   */
  public boolean hasNewSearchers() {
    return settings.hasNewSettings();
  }

  /**
   * Get (rebuild) the searchers from the settings.
   */
  public List<Searcher> getSearchers(String luceneId) {
    final List<Searcher> result = new ArrayList<Searcher>();

    final Record record = settings.getSettingsRecord(luceneId);
    if (record != null) {
      // get the "container" records
      final List<Record> containerRecords = settings.getAllSettingRecords("container", record);
      if (containerRecords != null) {
        for (Record containerRecord : containerRecords) {
          // get the "type" of searcher (CONSTANT, TRANSIENT)
          final SearcherType searcherType = getSearcherType(containerRecord);

          // build searchers from the container specs
          buildSearchers(result, containerRecord, searcherType);
        }
      }

      // get the "dir" values
      final List<Record> dirRecords = settings.getAllSettingRecords("dir", record);
      if (dirRecords != null) {
        for (Record dirRecord : dirRecords) {
          // get the "type" of searcher (CONSTANT, TRANSIENT)
          final SearcherType searcherType = getSearcherType(dirRecord);
          final String dir = dirRecord.getText(properties, null);

          // build searchers from the dirs
          buildSearcher(result, dir, searcherType);
        }
      }
    }

    return result == null || result.size() == 0 ? null : result;
  }

  private final SearcherType getSearcherType(Record record) {
    final String type = record.getAttribute("type");
    return SearcherFactory.getSearcherType(type);
  }

  private final void buildSearchers(List<Searcher> result, Record containerRecord, SearcherType searcherType) {

    // get the path to the container directory
    final String path = settings.getSettingsValue(containerRecord, "path");

    if (path != null) {
      // get the name pattern for indexes in the container
      final String pattern = settings.getSettingsValue(containerRecord, "pattern");

      // build the searcher
      buildSearchers(result, path, pattern, searcherType);
    }

  }

  private final void buildSearchers(List<Searcher> result, String path, String pattern, SearcherType searcherType) {

    final File pathDir = new File(path);
    if (pathDir.exists()) {
      final Pattern p = (pattern == null || "".equals(pattern)) ? null : Pattern.compile(pattern);
      final File[] files = pathDir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            boolean result = true;

            if (p != null) {
              final Matcher m = p.matcher(name);
              result = m.matches();
            }

            return result;
          }
        });

      if (files != null && files.length > 0) {
        for (File file : files) {
          buildSearcher(result, file, searcherType);
        }
      }
    }
  }

  private final void buildSearcher(List<Searcher> result, String dir, SearcherType searcherType) {
    if (dir != null) {
      final File file = new File(dir);
      buildSearcher(result, file, searcherType);
    }
  }

  private final void buildSearcher(List<Searcher> result, File file, SearcherType searcherType) {
    if (file.exists()) {
      final Searcher searcher = SearcherFactory.getSearcher(file, searcherType);
      if (searcher != null) {
        result.add(searcher);
      }
    }
  }


  public static final void main(String[] args) {
    //
    // Validates settings by loading searchers for each settingID
    //
    final MultiSearcherSettingsHelper mssh = MultiSearcherSettingsHelper.getInstance();

    final String[] ids = new String[] {
      POSTS_ID, SITEGRAPH_ID, KEYWORD_ID, BENCHMARK_ID,
    };

    for (String id : ids) {
      System.out.println("\nLoading id=" + id);

      final List<Searcher> searchers = mssh.getSearchers(id);

      System.out.println("\tfound " + ((searchers == null) ? "NO" : searchers.size()) + " indexes.");
      if (searchers != null) {
        int index = 0;
        for (Searcher searcher : searchers) {
          System.out.println("    " + index + ": " + searcher.getClass().getName() + "(" +
                             searcher.getDirPath() + ")");
          ++index;
        }
      }
    }
  }
}
