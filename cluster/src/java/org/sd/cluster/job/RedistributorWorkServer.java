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
package org.sd.cluster.job;


import org.sd.cio.MessageHelper;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Config;
import org.sd.io.FileUtil;
import org.sd.util.PropertiesParser;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

/**
 * A work server for redistributing work evenly across machines once
 * work has already been assigned (unevenly). This job should only be
 * necessary if/when the original WorkServer didn't have the "limit"
 * property set to "true".
 * <p>
 * NOTE: The 'batch' file for this job is NOT a batch input file like
 *       those required for the BatchWorkServer; rather, it is a batch
 *       OUTPUT (or log) file.
 *
 * @author Spence Koehler
 */
public class RedistributorWorkServer extends BatchWorkServer {

  private String[] newWorkers;
  private String[] removeWorkers;

  public RedistributorWorkServer() {
    super();
  }

  public RedistributorWorkServer(Properties properties) {
    super(properties);

    // add new workers of the form "Bashir-0,Bashir-1,Bashir-2,..."
    final String newWorkersString = properties.getProperty("newWorkers");
    if (newWorkersString != null) {
      this.newWorkers = newWorkersString.split("\\s*\\,\\s*");
    }

    // add new workers of the form "Bashir-0,Bashir-1,Bashir-2,..."
    final String removeWorkersString = properties.getProperty("removeWorkers");
    if (removeWorkersString != null) {
      this.removeWorkers = removeWorkersString.split("\\s*\\,\\s*");
    }
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writeStrings(dataOutput, newWorkers);
    MessageHelper.writeStrings(dataOutput, removeWorkers);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);
    this.newWorkers = MessageHelper.readStrings(dataInput);
    this.removeWorkers = MessageHelper.readStrings(dataInput);
  }

  protected PathBatch getPathBatch() {
    final Config config = getConfig();

    // jvmRootDir/data/job/WorkServer/<workServerId>/<batchFileName>
    final String batchFileName = getBatchFilename();
    final File workDir = new File(config.getJobDataPath(getName(), getJobId()));
    final File pathBatchFile = new File(workDir, batchFileName);
    PathBatch result = null;

    if (!pathBatchFile.exists()) {
      System.err.println("*** Can't find path batch file at '" + pathBatchFile.getAbsolutePath() + "'!");
    }
    else {
      System.out.println(new Date() + ": Opening pathBatch at '" + pathBatchFile.getAbsolutePath() + "'. " +
                         getDescription());

      final Map<String, LinkedList<String>> cache2paths = new HashMap<String, LinkedList<String>>();
      long totalPaths = 0L;
      try {
        totalPaths = loadCache2Paths(pathBatchFile, cache2paths);
      }
      catch (IOException e) {
        System.err.println("*** Can't load path batch file at '" + pathBatchFile.getAbsolutePath() + "'!");
        e.printStackTrace(System.err);
      }

      // add in extra machines here by mapping the new node Id's (like "Bashir-0") to an empty list.
      if (newWorkers != null) {
        for (String newWorker : newWorkers) {
          if (!cache2paths.containsKey(newWorker)) {
            cache2paths.put(newWorker, new LinkedList<String>());
          }
        }
      }

      result = redistributePaths(totalPaths, cache2paths);
    }
    return result;
  }

  private final long loadCache2Paths(File pathBatchFile, Map<String, LinkedList<String>> cache2paths) throws IOException {
    long result = 0L;

    final BufferedReader reader = FileUtil.getReader(pathBatchFile);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final String[] pieces = line.split("\\s*\\|\\s*");
      if (pieces.length != 4) {
        throw new IllegalStateException("Bad batch file! Need 'output' format with 4 fields! " + pathBatchFile);
      }

      final String cache = pieces[2];  // i.e. "Tandaran-0"
      String path = pieces[0];         // i.e. "tandaran:/data/crawl/sd833.us/00"
      final int cPos = path.indexOf("^");
      if (cPos >= 0) {
        path = path.substring(cPos + 1);  // change "a^b:/data/crawl/..." to "b:/data/crawl/..."
      }
      LinkedList<String> paths = cache2paths.get(cache);
      if (paths == null) {
        paths = new LinkedList<String>();
        cache2paths.put(cache, paths);
      }
      paths.add(path);
      ++result;
    }
    reader.close();

    return result;
  }

  private final PathBatch redistributePaths(long totalPaths, Map<String, LinkedList<String>> cache2paths) {
    final PathBatch result = new PathBatch(true, false);  // onlyOwn, don't acceptHelp

    int numWorkers = cache2paths.size();
    if (removeWorkers != null) {
      numWorkers -= removeWorkers.length;
    }

    // find how many should be in each bucket
    final long pathsPerBucket = totalPaths / numWorkers;

    final LinkedList<String> extraPaths = new LinkedList<String>();

    // remove all from workers to be removed
    if (removeWorkers != null) {
      for (String removeWorker : removeWorkers) {
        final LinkedList<String> paths = cache2paths.get(removeWorker);
        if (paths != null) {
          extraPaths.addAll(paths);
        }
        cache2paths.remove(removeWorker);
      }
    }

    // for those caches with too many paths, pull off the extras
    for (LinkedList<String> cachePaths : cache2paths.values()) {
      while (cachePaths.size() > pathsPerBucket + 1) {
        extraPaths.add(cachePaths.removeLast());
      }
    }

    // walk through those that have too few and add an extra until extras are gone
    while (extraPaths.size() > 0) {
      for (LinkedList<String> cachePaths : cache2paths.values()) {
        if (cachePaths.size() <= pathsPerBucket) {
          cachePaths.add(extraPaths.removeLast());
          if (extraPaths.size() == 0) break;
        }
      }
    }

    // now load up the path batch
    for (Map.Entry<String, LinkedList<String>> entry : cache2paths.entrySet()) {
      final String cache = entry.getKey();
      final LinkedList<String> paths = entry.getValue();

      // change i.e. "Tandaran-0" to "tandaran"
      final String fixedCache = fix(cache);

      for (String path : paths) {
        result.addPath(fixedCache + "^" + path);
      }
    }

    return result;
  }

  private final String fix(String cacheName) {
    // change i.e. "Tandaran-0" to "tandaran"
    final int dPos = cacheName.indexOf('-');
    if (dPos >= 0) cacheName = cacheName.substring(0, dPos);
    return cacheName.toLowerCase();
  }

  public static void main(String[] args) throws IOException, ClusterException {
    // start the work server on a node
    //
    //properties: [user], [defName], [machines], jobId(=dataDirName), groupName(=workServerId), batch, [onlyOwn] [acceptHelp] [limit] [newWorkers]
    final PropertiesParser pp = new PropertiesParser(args);
    final WorkServer workServer = new RedistributorWorkServer(pp.getProperties());
    run(pp, workServer);
  }
}
