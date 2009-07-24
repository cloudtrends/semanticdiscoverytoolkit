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
import org.sd.io.FileUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for a batch of work.
 * <p>
 * A batch is constructed by collecting new units of work or by restoring
 * itself from a persisted batch for continued processing.
 *
 * @author Spence Koehler
 */
public class WorkBatch {
  
  private String batchFileName;
  private BatchMaker batchMaker;
  private List<UnitOfWork> _workUnits;

  /**
   * Construct a work batch from its persisted data.
   *
   * @param batchFileName  name of file where batch is or will be persisted.
   */
  public WorkBatch(String batchFileName) {
    this(batchFileName, null);
  }

  /**
   * Construct a work batch either from its persisted data or using the
   * given batch maker.
   *
   * @param batchFileName  name of file where batch is or will be persisted.
   * @param batchMaker     a maker to create the batch if the batchFileName
   *                       doesn't exist.
   */
  public WorkBatch(String batchFileName, BatchMaker batchMaker) {
    this.batchFileName = batchFileName;
    this.batchMaker = batchMaker;
    this._workUnits = null;
  }

  /**
   * Get the number of work units for this batch, not keeping units in memory
   * if they haven't been loaded yet.
   */
  public int getNumWorkUnits() {
    int result = 0;
    if (_workUnits == null) {
      _workUnits = loadWorkUnits();
      result = _workUnits.size();
      _workUnits = null;
    }
    else {
      result =_workUnits.size();
    }
    return result;
  }

  public List<UnitOfWork> getWorkUnits() {
    if (_workUnits == null) {
      _workUnits = loadWorkUnits();
    }
    return _workUnits;
  }

  /**
   * special accessor for manually creating a work batch. not intended for
   * normal use.
   */
  public void addWorkUnit(UnitOfWork workUnit) {
    if (_workUnits == null) {
      _workUnits = new LinkedList<UnitOfWork>();
    }
    _workUnits.add(workUnit);
  }

  public void save() throws IOException {
//System.out.println("Writing " + getWorkUnits().size() + " workUnits to '" + batchFileName + "'");
    writeWorkFile(batchFileName, getWorkUnits());
  }

  public Map<WorkStatus, List<UnitOfWork>> getCurrentStatus() {
    final Map<WorkStatus, List<UnitOfWork>> result = new HashMap<WorkStatus, List<UnitOfWork>>();
    for (UnitOfWork workUnit : getWorkUnits()) {
      final WorkStatus status = workUnit.getWorkStatus();
      List<UnitOfWork> curUnits = result.get(status);
      if (curUnits == null) {
        curUnits = new ArrayList<UnitOfWork>();
        result.put(status, curUnits);
      }
      curUnits.add(workUnit);
    }
    return result;
  }

  /**
   * Mark stopped work units as initialized for re-running.
   */
  public void reclaimStopped() {
    for (UnitOfWork workUnit : getWorkUnits()) {
      workUnit.compareAndSetWorkStatus(WorkStatus.STOPPED, WorkStatus.INITIALIZED);
    }
  }

  private final List<UnitOfWork> loadWorkUnits() {
    List<UnitOfWork> result = null;
    final File workFile = new File(batchFileName);
    try {
      if (batchMaker == null || workFile.exists()) {
        result = loadWorkFile(workFile);
      }
      else {
        result = batchMaker.createWorkUnits();
      }
    }
    catch (IOException e) {
      //todo: handle this exception...
      System.err.println("Unable to load work units! batchFileName=" + batchFileName +
                         " (exists=" + workFile.exists() + ")");
      e.printStackTrace(System.err);
    }
    return result;
  }

  public static final void writeWorkFile(String batchFileName, List<UnitOfWork> workUnits) throws IOException {
    FileUtil.mkdirs(batchFileName);
    final DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(batchFileName));
    for (UnitOfWork workUnit : workUnits) {
      MessageHelper.writePublishable(dataOutput, workUnit);
    }
    dataOutput.close();
  }

  private final List<UnitOfWork> loadWorkFile(File workFile) throws IOException {
    List<UnitOfWork> result = new LinkedList<UnitOfWork>();
    final DataInputStream dataInput = new DataInputStream(new FileInputStream(workFile));
    while (true) {
      UnitOfWork workUnit = null;
      try {
        workUnit = (UnitOfWork)MessageHelper.readPublishable(dataInput);
      }
      catch (EOFException e) {
        // reached end. ok
        break;
      }
      final WorkStatus workStatus = workUnit.getWorkStatus();
      if (workStatus == WorkStatus.PROCESSING) workUnit.setWorkStatus(WorkStatus.STOPPED);
      result.add(workUnit);
    }
    dataInput.close();

    return result;
  }

  private void showStatus(PrintStream out) throws IOException {
    final Map<WorkStatus, List<UnitOfWork>> status = getCurrentStatus();

    for (Map.Entry<WorkStatus, List<UnitOfWork>> entry : status.entrySet()) {
      final WorkStatus workStatus = entry.getKey();
      final int numItems = entry.getValue().size();

      out.println(workStatus + ": " + numItems);
    }

    out.println("\ttotal: " + getWorkUnits().size());
  }

  public UnitOfWork findWorkUnit(WorkUnitComparer comparer) {
    final List<UnitOfWork> workUnits = getWorkUnits();
    int index = 0;
    for (UnitOfWork workUnit : workUnits) {
      if (comparer.matches(workUnit)) {
        System.out.println("index=" + index);
        return workUnit;
      }
      ++index;
    }
    return null;
  }

  public static interface WorkUnitComparer {
    public boolean matches(UnitOfWork workUnit);
  }

  public static final class EndsWithComparer implements WorkUnitComparer {
    private String ending;

    public EndsWithComparer(String ending) {
      this.ending = (ending.charAt(0) == '/') ? ending : "/" + ending;
    }

    public boolean matches(UnitOfWork workUnit) {
      return ((StringUnitOfWork)workUnit).getString().endsWith(ending);
    }
  }

  public static final class EndsWithAnyComparer implements WorkUnitComparer {
    private Set<String> endings;

    public EndsWithAnyComparer(String[] endings, int startPos) {
      this.endings = new HashSet<String>();

      for (int i = startPos; i < endings.length; ++i) {
        String ending = endings[i];
        if (ending.charAt(0) != '/') ending = "/" + ending;
        this.endings.add(ending);
      }
    }

    public boolean matches(UnitOfWork workUnit) {
      String string = ((StringUnitOfWork)workUnit).getString();
      final int lastSlashPos = string.lastIndexOf('/');
      string = string.substring(lastSlashPos);
      return endings.contains(string);
    }
  }

  // get stats on the work batch and show the work units that match (end with) the given strings.
  // ./run org.sd.cluster.job.WorkBatch ~/cluster/jvm-0/data/job/CompanyFinderJob.main/german.061206/batchfile.dat www.pool-zentrum.de
  public static void main(String[] args) throws IOException {
    // arg0: path to workkBatch file
    // arg1: (optional) -q  (don't show totals, just work units)
    // args1+: domains to find in the batch
    //
    final WorkBatch workBatch = new WorkBatch(args[0]);
    final boolean quiet = (args.length > 1) && "-q".equals(args[1]);
    final int startIndex = quiet ? 2 : 1;

    if (!quiet) {
      workBatch.showStatus(System.out);
    }

    if (args.length == 1 || quiet) {
      List<UnitOfWork> workUnits = workBatch.getWorkUnits();
      int count = 0;
      for (UnitOfWork workUnit : workUnits) {
        final String value = ((StringUnitOfWork)workUnit).getString();
        System.out.println(value);
        ++count;

//        if (count > 10) break;
      }
    }

    if (!quiet) {
      for (int i = startIndex; i < args.length; ++i) {
        final WorkUnitComparer comparer = new EndsWithComparer(args[i]);
        final UnitOfWork found = workBatch.findWorkUnit(comparer);
        System.out.println(args[i] + ": " + found);
      }
    }
  }
}
