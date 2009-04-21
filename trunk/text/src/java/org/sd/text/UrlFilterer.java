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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Given a files with url lines and a list of mtf filter def files, dump 2 files:
 * a filtered version and the discarded lines.
 * <p>
 * @author Spence Koehler
 */
public class UrlFilterer {

  private static final Map<File, UrlFilterer> file2filterer = new HashMap<File, UrlFilterer>();

  public static final UrlFilterer getInstance(File filterDefsDir) {
    UrlFilterer result = file2filterer.get(filterDefsDir);
    if (result == null) {
      result = new UrlFilterer(filterDefsDir);
      file2filterer.put(filterDefsDir, result);
    }
    return result;
  }


  private static final Pattern MTF_FILE_PATTERN = Pattern.compile("^.+\\.(.*)\\.mtf\\.?.*$");


  private MultiTermFinder[] domainMtfs;  // applied to 'domain' portion of url only
  private MultiTermFinder[] pathMtfs;    // applied to 'path' portion of url only
  private MultiTermFinder[] urlMtfs;     // applied to full url

  public UrlFilterer(File filterDefsDir) {
    // scan the filterDefDir for files "*.domain.mtf.*", "*.path.mtf.*", "*.url.mtf.*"
    try {
      init(filterDefsDir);
    }
    catch (IOException e) {
      throw new IllegalArgumentException("filterDefsDir=" + filterDefsDir, e);
    }
  }

  // scan the filterDefDir for files "*.domain.mtf.*", "*.path.mtf.*", "*.url.mtf.*"
  private final void init(File filterDefsDir) throws IOException {
    File[] mtfFiles = null;

    if (filterDefsDir.isDirectory()) {
      mtfFiles = filterDefsDir.listFiles(new FileFilter() {
          public boolean accept(File file) {
            boolean result = false;

            if (!file.isDirectory()) {
              final String name = file.getName();
              result = name.contains(".mtf.") && name.charAt(name.length() - 1) != '~';
            }

            return result;
          }
        });
    }
    else {
      mtfFiles = new File[]{filterDefsDir};
    }

    if (mtfFiles != null) {
      final List<MultiTermFinder> domains = new ArrayList<MultiTermFinder>();
      final List<MultiTermFinder> paths = new ArrayList<MultiTermFinder>();
      final List<MultiTermFinder> urls = new ArrayList<MultiTermFinder>();

      for (File mtfFile : mtfFiles) {
        final String filename = mtfFile.getName();
        final Matcher m = MTF_FILE_PATTERN.matcher(filename);
        if (m.matches()) {
          List<MultiTermFinder> bucket = null;

          final String type = m.group(1);
          if ("domain".equals(type)) {
            bucket = domains;
          }
          else if ("path".equals(type)) {
            bucket = paths;
          }
          else if ("url".equals(type)) {
            bucket = urls;
          }

          if (bucket != null) {
System.out.println("Loading mtf definitions from '" + mtfFile + "'!");
            bucket.add(MultiTermFinder.loadFromFile(mtfFile));
          }
        }
      }

      this.domainMtfs = domains.size() == 0 ? null : domains.toArray(new MultiTermFinder[domains.size()]);
      this.pathMtfs = paths.size() == 0 ? null : paths.toArray(new MultiTermFinder[paths.size()]);
      this.urlMtfs = urls.size() == 0 ? null : urls.toArray(new MultiTermFinder[urls.size()]);
    }
  }

  public boolean findMatch(String urlString) {
    return findMatch(new DetailedUrl(urlString));
  }

  public boolean findMatch(DetailedUrl dUrl) {
    boolean result = false;

    //note: replace '.'s with <space>s in domain and url before matching
    if (!result && urlMtfs != null) {
      String string = dUrl.getHost(false, false, false) + dUrl.getPath(false);
      string = string.replaceAll("\\.", " ");
      result = findMatch(urlMtfs, string);
    }

    if (!result && domainMtfs != null) {
      String string = dUrl.getHost(false, false, false);
      string = string.replaceAll("\\.", " ");
      result = findMatch(domainMtfs, string);
    }

    if (!result && pathMtfs != null) {
      String string = dUrl.getPath(false);
      result = findMatch(pathMtfs, string);
    }

    return result;
  }

  private static final boolean findMatch(MultiTermFinder[] mtfs, String string) {
    boolean result = false;

    for (MultiTermFinder mtf : mtfs) {
      if (mtf.findFirstMatch(string) != null) {
        result = true;
        break;
      }
    }

    return result;
  }

//java -Xmx640m org.sd.util.UrlFilterer /home/sbk/tmp/urls/urls.uniq2.txt.gz /home/sbk/tmp/urls/
  public static final void main(String[] args) throws IOException {
    //arg0: url file
    //arg1: dir (or file) with mtf def files
    //out: urlFile.filtered.txt.gz, urlFile.discarded.txt.gz

    final String urlFileName = args[0];
    final File urlFile = new File(urlFileName);
    final File filteredOut = new File(urlFileName + ".filtered.txt.gz");
    final File discardedOut = new File(urlFileName + ".discarded.txt.gz");

    final UrlFilterer urlFilterer = new UrlFilterer(new File(args[1]));

    final BufferedReader reader = FileUtil.getReader(urlFileName);
    final BufferedWriter filteredWriter = FileUtil.getWriter(filteredOut);
    final BufferedWriter discardedWriter = FileUtil.getWriter(discardedOut);

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0 || line.charAt(0) == '#') continue;
      boolean discard = urlFilterer.findMatch(line);
      
      final BufferedWriter writer = discard ? discardedWriter : filteredWriter;
      writer.write(line);
      writer.newLine();
    }

    discardedWriter.close();
    filteredWriter.close();
    reader.close();
  }
}
