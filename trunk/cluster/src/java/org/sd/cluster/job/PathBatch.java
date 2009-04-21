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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility to track a batch of domain paths in memory for processing.
 * <p>
 * @author Spence Koehler
 */
public class PathBatch {

  private ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> machine2paths;
  private Map<String, String> short2longPaths;
  private Set<String> dataCaches;
  private AtomicLong numPaths;
  private Random random;
  private boolean onlyOwn;
  private boolean acceptHelp;

  public PathBatch(File pathsFile, boolean onlyOwn, boolean acceptHelp) throws IOException {
    this(onlyOwn, acceptHelp);
    load(pathsFile);
  }
  
  public PathBatch(boolean onlyOwn, boolean acceptHelp) {
    this.machine2paths = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();
    this.short2longPaths = new HashMap<String, String>();
    this.dataCaches = new HashSet<String>();
    this.numPaths = new AtomicLong(0);
    this.random = new Random();
    this.onlyOwn = onlyOwn;
    this.acceptHelp = acceptHelp;
  }
  
  private final void load(File pathsFile) throws IOException {
    final BufferedReader reader = FileUtil.getReader(pathsFile);

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("java ")) continue;  // ignore echoed java line

      final String[] pieces = line.split("\\s*\\|\\s*");
      addPath(pieces[0], line);
    }

    reader.close();
  }

  public final void addPath(String shortAndLongPath) {
    addPath(shortAndLongPath, shortAndLongPath);
  }

  public final void addPath(String shortPath, String longPath) {
    short2longPaths.put(shortPath, longPath);

    final String machine = getMachineName(shortPath);

    if (machine == null) {
      System.err.println("PathBatch.addPath ***WARNING: unknown machine in path '" + shortPath + "' ...ignoring!");
    }
    else {
      ConcurrentLinkedQueue<String> paths = machine2paths.get(machine);
      if (paths == null) {
        paths = new ConcurrentLinkedQueue<String>();
        machine2paths.put(machine, paths);
      }

      paths.offer(longPath);
      numPaths.incrementAndGet();
    }
  }

  // given shortPath, remove longPath
  public final boolean removePath(String shortPath) {
    boolean result = false;

    final String machine = getMachineName(shortPath);

    if (machine == null) {
      System.err.println("PathBatch.removePath ***WARNING: unknown machine in path '" + shortPath + "' ...ignoring!");
    }
    else {
      final ConcurrentLinkedQueue<String> paths = machine2paths.get(machine);
      final String longPath = short2longPaths.get(shortPath);
      if (longPath == null) {
        System.err.println("PathBatch.removePath ***WARNING: no long path for short path '" + shortPath + "'!");
      }
      else {
        result = paths.remove(longPath);
        if (result) {
          numPaths.decrementAndGet();

          if (paths.isEmpty()) {
            machine2paths.remove(machine);
          }
        }
        else {
          System.err.println("*** WARNING: couldn't remove path '" + longPath + "'! machine=" + machine);
        }
      }
    }

    return result;
  }

  private final String getMachineName(String path) {
    String result = null;

    final int cPos = path.indexOf(':');
    if (cPos > 0) {
      result = path.substring(0, cPos).toLowerCase();
    }

    else if (path.startsWith("/mnt/")) {
      final int slashPos = path.indexOf('/', 5);
      if (slashPos > 5) {
        result = path.substring(5, slashPos).toLowerCase();
      }
    }

    // if machine is of the form a^b, then return a (a=newMachine, b=origMachine)
    if (result != null) {
      final int caretPos = result.indexOf('^');
      if (caretPos >= 0) {
        result = result.substring(0, caretPos).toLowerCase();

        // keep track of these machines -- they are data caches.
        dataCaches.add(result);
      }
    }

    return result;
  }

  /**
   * Given a file with pipe-delimited lines whose first field is a domain string,
   * remove all domains from this batch that exist in the file.
   */
  public int removeFinishedWork(File outputFile) throws IOException {
    int result = 0;

    final BufferedReader reader = FileUtil.getReader(outputFile);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final String[] pieces = line.split("\\s*\\|\\s*");
      if (removePath(pieces[0])) ++result;
    }

    reader.close();

    return result;
  }

  public String getNext(String machine) {
    if (machine2paths.isEmpty()) return null;

    String result = null;

    ConcurrentLinkedQueue<String> paths = (machine == null) ? null : machine2paths.get(machine);

    // this allows us to add a machine that hadn't previously cached data
    // to help out with the work
    boolean onlyDoOwn = (onlyOwn && machine != null && dataCaches.contains(machine));

    if (onlyDoOwn) {

      // only give a machine its own data to process. intended for use when
      // re-using a cache --  not for when trying to process a crawler's data
      // on itself!

      result = getNextSpecific(machine, paths);

      // no more work for the specific machine. try to take on new work
      // (without stealing from other designated machines)
      if (result == null) {
        result = getNextRandomized(machine, paths);
      }

      // no more 'own' or 'randomized' work for machine and we're allowed to
      // 'steal' or 'help' with other machines' work
      if (result == null && acceptHelp) {
//todo: fix this. it isn't working.
        result = getNextRandomized(machine, null);
      }
    }
    else {
      result = getNextRandomized(machine, paths);
    }

    if (result == null && (paths == null || paths.size() == 0)) {
      result = WorkServer.WORK_IS_DONE_RESPONSE;
    }

    return result;
  }

  private final String getNextSpecific(String machine, ConcurrentLinkedQueue<String> paths) {
    String result = null;

    if (paths != null) {
      if (paths.isEmpty()) {
        machine2paths.remove(machine);
      }
      else {
        result = paths.poll();
        numPaths.decrementAndGet();

        // somebody else got it.
        if (result == null) {
          machine2paths.remove(machine);
        }
      }
    }

    return result;
  }

  private final String getNextRandomized(String machine, ConcurrentLinkedQueue<String> paths) {
    String result = null;
	
    if (paths == null) {
      machine = chooseMachine();
      paths = (machine == null) ? null : machine2paths.get(machine);
      if (paths != null) {
        result = getNextRandomized(machine, paths);
      }
    }
    else if (paths.isEmpty()) {
      machine2paths.remove(machine);
      machine = chooseMachine();
      paths = (machine == null) ? null : machine2paths.get(machine);
      if (paths != null) {
        result = getNextRandomized(machine, paths);
      }
    }
    else {
      result = paths.poll();
      numPaths.decrementAndGet();
 	
      // somebody else got it.
      if (result == null) {
        machine2paths.remove(machine);
        result = getNextRandomized(null, null);
      }
    }
 	
    return result;
  }

  private final String chooseMachine() {
    final Set<String> machines = getAvailableMachines();

    if (machines.size() == 0) return null;

    final int num = random.nextInt(machines.size());

    int count = 0;
    String machine = null;

    for (String m : machines) {
      if (count == num) {
        machine = m;
        break;
      }
      ++count;
    }

    if (machine == null) {
      machine = machines.iterator().next();
    }

    return machine;
  }

  private final Set<String> getAvailableMachines() {
    Set<String> result = null;
    final Set<String> machines = machine2paths.keySet();

    if (onlyOwn) {
      // only choose non-dataCache machines.
      result = new HashSet<String>();
      for (String m : machines) {
        if (!dataCaches.contains(m)) {
          result.add(m);
        }
      }
    }
    else {
      // choose from all machines.
      result = machines;
    }

    return result;
  }

  public boolean isComplete() {
    return machine2paths.isEmpty();
  }

  public int getNumMachinePaths() {
    return machine2paths.size();
  }

  public long getRemainingEstimate() {
    return numPaths.get();
  }

  public void close() throws IOException {
//todo: dump remaining paths?
  }

  public String dumpDetails() {
    final StringBuilder result = new StringBuilder();

    result.append(machine2paths.size()).append(" machine paths remain.");
    for (Map.Entry<String, ConcurrentLinkedQueue<String>> entry : machine2paths.entrySet()) {
      result.append("\t").append(entry.getKey()).append(": ").append(entry.getValue().size()).append("\n");
    }

    return result.toString();
  }


  //java -Xmx640m org.sd.cluster.job.PathBatch /home/sbk/tmp/uk_products/input_batch/uk_domains_041007.txt.gz /home/sbk/tmp/uk_products/output_batch/uk_domains_041007.txt.gz.out
  //java -Xmx640m org.sd.cluster.job.PathBatch /home/sbk/tmp/dlv_index/input_batch/global.011007.minus-UK.cached.batch.txt.gz /home/sbk/tmp/dlv_index/output_batch/global.011007.minus-UK.cached.batch.txt.gz.out
  public static final void main(String[] args) throws IOException {
    //arg0: inputFile
    //arg1: outputFile [optional]

    final File inputFile = new File(args[0]);
    final File outputFile = args.length > 1 ? new File(args[1]) : null;

    final PathBatch pathBatch = new PathBatch(inputFile, true, false);

    long numPaths = pathBatch.getRemainingEstimate();
    System.out.println("PathBatch(" + inputFile + ") has " + numPaths + " paths.");

    if (outputFile != null) {
      final int numRemoved = pathBatch.removeFinishedWork(outputFile);
      System.out.println("Removed " + numRemoved + " paths using outputFile '" + outputFile + "'");
      numPaths = pathBatch.getRemainingEstimate();
      System.out.println("PathBatch(" + inputFile + ") has " + numPaths + " paths.");
    }
  }
}
