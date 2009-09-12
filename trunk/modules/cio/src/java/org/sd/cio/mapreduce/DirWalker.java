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
package org.sd.cio.mapreduce;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.sd.io.DirectorySelector;
import org.sd.io.FileOperator;
import org.sd.io.FileRecordIterator;
import org.sd.io.FileRecordIteratorFactory;
import org.sd.io.FileSelector;
import org.sd.io.MultiFileIterator;
import org.sd.io.MultiFileIteratorFactory;
import org.sd.io.MultiPartRecord;
import org.sd.io.MultiPartRecordFactory;
import org.sd.io.MultiRecordIterator;
import org.sd.io.RecordOperator;
import org.sd.io.RecordMerger;
import org.sd.util.thread.Governable;
import org.sd.util.thread.GovernableThread;
import org.sd.util.thread.UnitCounter;

/**
 * Base class for walking over directories and files (optionally) in a
 * governable thread, applying a fileOperation to each selected file
 * and/or directory.
 * <p>
 * To run an instance within a governable thread, do (for example):
 * <p>
 * GovernableThread.newGovernableThread(dirWalker, true);
 * 
 * @author Spence Koehler
 */
public class DirWalker implements Governable {
  
  /**
   * Utility method to iterate over selected files.
   * <p>
   * NOTE: Be aware that the iterator spawns a new thread that runs a DirWalker
   *       instance.
   *
   * @param rootDir  The directory (or file) at which to start walking.
   * @param dirSelector  A directory selector strategy (always DESCENDs if null).
   * @param fileSelector  A file selector strategy (always selects if null).
   *
   * @return the iterator over selected files.
   */
  public static final Iterator<File> iterator(File rootDir, DirectorySelector dirSelector, FileSelector fileSelector) {
    return new WalkerIterator(rootDir, dirSelector, fileSelector);
  }

  /**
   * Utility method to count the number of selected files.
   *
   * @param rootDir  The directory (or file) at which to start walking.
   * @param dirSelector  A directory selector strategy (always DESCENDs if null).
   * @param fileSelector  A file selector strategy (always selects if null).
   */
  public static final int count(File rootDir, DirectorySelector dirSelector, FileSelector fileSelector) {
    int result = 0;

    for (Iterator<File> iter = iterator(rootDir, dirSelector, fileSelector); iter.hasNext(); ) {
      ++result;
    }

    return result;
  }

  /**
   * Utility method to collect selected files.
   *
   * @param rootDir  The directory (or file) at which to start walking.
   * @param dirSelector  A directory selector strategy (always DESCENDs if null).
   * @param fileSelector  A file selector strategy (always selects if null).
   */
  public static final List<File> collect(File rootDir, DirectorySelector dirSelector, FileSelector fileSelector) {
    List<File> result = new ArrayList<File>();

    for (Iterator<File> iter = iterator(rootDir, dirSelector, fileSelector); iter.hasNext(); ) {
      result.add(iter.next());
    }

    return result;
  }

  /**
   * Factory method for a DirWalker for mapping each record (of type T) within
   * selected files, halting if/when recordOperator returns false.
   *
   */
  public static final <T> DirWalker mapper(boolean verbose, UnitCounter uc, File rootDir,
                                           DirectorySelector dirSelector,
                                           FileSelector fileSelector,
                                           final FileRecordIteratorFactory<T> recordIteratorFactory,
                                           final RecordOperator<T, File> recordOperator) {

    final boolean recordVerbose = false;  // note: usually, it's a bad idea to be verbose at the record level
    final UcOperationPerformer<T, File> performer = new UcOperationPerformer<T, File>(recordVerbose, uc.registerSubsidiary()) {
      protected boolean performOperation(T record, File context) {
        return recordOperator.operate(record, context);
      }
    };

    return
      new DirWalker(verbose, uc, rootDir, dirSelector, fileSelector,
                    new FileOperator() {
                      private File prevFile;
                      public boolean initializeHook() {return true;}
                      public boolean operate(File file) {
                        boolean result = true;

                        FileRecordIterator<T> recordIterator = null;
                        try {
                          recordIterator = recordIteratorFactory.getFileRecordIterator(file);
                          if (recordIterator != null) {
                            while (result && recordIterator.hasNext()) {
                              final T record = recordIterator.next();
                              result = performer.operate(record, file);
                            }
                          }
                        }
                        catch (IOException e) {
                          throw new IllegalStateException(e);
                        }
                        finally {
                          if (recordIterator != null) {
                            try {
                              recordIterator.close();
                            }
                            catch (IOException e) {
                              throw new IllegalStateException(e);
                            }
                          }
                        }

                        prevFile = file;
                        return result;
                      }
                      public void finalizeHook() {}
                    }
        );
  }

  /**
   * Factory method for a DirWalker for reducing mapped records (of type T)
   * from within selected files, halting if/when mergeOperator returns false.
   *
   */
  public static final <T> DirWalker reducer(boolean verbose, UnitCounter uc, File rootDir,
                                            DirectorySelector dirSelector,
                                            FileSelector fileSelector,
                                            final MultiPartRecordFactory<File,List<File>> fileCollector, // collect files to co-iterate
                                            final MultiFileIteratorFactory<T> multiFileIteratorFactory,  // create co-file-iterator
                                            final Comparator<T> recordComparer,                          // sort, find equal records
                                            final RecordMerger<T, List<File>> recordMerger,              // merge equal records
                                            final OutputFinalizer<List<File>> outputFinalizer,           // flush output
                                            final AtomicBoolean needsReduce) {                           // output flag

    final boolean recordVerbose = false;  // note: usually, it's a bad idea to be verbose at the record level
    final UcOperationPerformer<List<T>, List<File>> performer = new UcOperationPerformer<List<T>, List<File>>(recordVerbose, uc.registerSubsidiary()) {
      protected boolean performOperation(List<T> records, List<File> context) {
        return recordMerger.merge(records, context);
      }
    };

    return
      new DirWalker(verbose, uc, rootDir, dirSelector, fileSelector,
                    new FileOperator() {
                      private MultiPartRecord<File, List<File>> groupedFiles = fileCollector.newMultiPartRecord();

                      public boolean initializeHook() {return true;}

                      public boolean operate(File file) {
                        boolean result = true;

                        final boolean added = groupedFiles.addPiece(file);
                        if (!added) {
                          // time to flush current group
                          result = flush();

                          // start a new group with the file that couldn't be added
                          groupedFiles = fileCollector.newMultiPartRecord();
                          result = groupedFiles.addPiece(file);
                        }
                        // else, still collecting files

                        return result;
                      }

                      public void finalizeHook() {
                        // flush last collected input files
                        flush();
                      }

                      private final boolean flush() {
                        boolean result = true;
                        final List<File> files = groupedFiles.getRecord();  // increments
                        if (files != null && files.size() > 0) {

                          // if !groupedFiles.isComplete(), then output will need to be input to another reduce phase;
                          // otherwise, output is input suitable for a map phase.
                          if (!groupedFiles.isComplete()) needsReduce.set(true);

                          MultiFileIterator<T> multiFileIterator = null;
                          try {
                            multiFileIterator = multiFileIteratorFactory.getMultiFileIterator(files, recordComparer);
                            if (multiFileIterator != null) {
                              while (result && multiFileIterator.hasNext()) {
                                final List<T> records = multiFileIterator.next();
                                result = performer.operate(records, files);
                              }
                            }
                          }
                          catch (IOException e) {
                            throw new IllegalStateException(e);
                          }
                          finally {
                            if (multiFileIterator != null) {
                              try {
                                multiFileIterator.close();

                                // flush/finalize the output
                                if (outputFinalizer != null) {
                                  try {
                                    outputFinalizer.finalize(files);
                                  }
                                  catch (IOException e) {
                                    throw new IllegalStateException(e);
                                  }
                                }
                              }
                              catch (IOException e) {
                                throw new IllegalStateException(e);
                              }
                            }
                          }
                        }
                        return result;
                      }
                    }
        );
  }



  private boolean verbose;
  private UnitCounter uc;
  private File rootDir;
  private DirectorySelector dirSelector;
  private FileSelector fileSelector;
  private FileOperator fileOperator;   // operator to apply to each file
  private UcOperationPerformer<File, DirWalker> performer;
  private final AtomicReference<File> currentFile = new AtomicReference<File>();

//todo: add resume functionality?

  /**
   * Construct non-verbose non-governed instance with the given params.
   *
   * @param rootDir  path to root directory or file to process.
   * @param dirSelector  directory selector (always DESCENDs if null).
   * @param fileSelector  file selector (always selects if null).
   * @param fileOperator  file operator to apply to each selected file.
   */
  public DirWalker(File rootDir,
                   DirectorySelector dirSelector,
                   FileSelector fileSelector,
                   FileOperator fileOperator) {
    this(false, null, rootDir, dirSelector, fileSelector, fileOperator);
  }

  /**
   * Construct with the given params.
   *
   * @param verbose  Indicates whether to show progress.
   * @param uc  Unit counter to monitor/control progress (possibly null).
   * @param rootDir  path to root directory or file to process.
   * @param dirSelector  directory selector (always DESCENDs if null).
   * @param fileSelector  file selector (always selects if null).
   * @param fileOperator  file operator to aply to each selected file.
   */
  public DirWalker(boolean verbose, UnitCounter uc, File rootDir,
                   DirectorySelector dirSelector,
                   FileSelector fileSelector,
                   final FileOperator fileOperator) {
    this.verbose = verbose;
    this.uc = uc;
    this.rootDir = rootDir;

    this.dirSelector = dirSelector;
    this.fileSelector = fileSelector;
    this.fileOperator = fileOperator;

    this.performer = new UcOperationPerformer<File, DirWalker>(verbose, uc) {
      protected boolean performOperation(File curFile, DirWalker context) {
        return fileOperator.operate(curFile);
      }
    };
  }

  /**
   * Get the current file or directory being visited.
   */
  public File getCurrentFile() {
    return currentFile.get();
  }

  /**
   * Set the verbose flag for this instance.
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Get the verbose flag for this instance.
   */
  public boolean getVerbose() {
    return verbose;
  }

  /**
   * Get the unit counter.
   */
  public UnitCounter getUnitCounter() {
    return uc;
  }

  /**
   * Get the root directory.
   */
  public File getRootDir() {
    return rootDir;
  }

  /**
   * Set (or reset) the root directory.
   */
  public void setRootDir(File rootDir) {
    this.rootDir = rootDir;
  }

  /**
   * Get the directory selector.
   */
  public DirectorySelector getDirSelector() {
    return dirSelector;
  }

  /**
   * Set the directory selector.
   */
  public void getDirSelector(DirectorySelector dirSelector) {
    this.dirSelector = dirSelector;
  }

  /**
   * Get the file selector.
   */
  public FileSelector getFileSelector() {
    return fileSelector;
  }

  /**
   * Set the file selector.
   */
  public void getFileSelector(FileSelector fileSelector) {
    this.fileSelector = fileSelector;
  }

  /**
   * Get the file operator.
   */
  public FileOperator getFileOperator() {
    return fileOperator;
  }

  /**
   * Set the file operator.
   */
  public void getFileOperator(FileOperator fileOperator) {
    this.fileOperator = fileOperator;
  }


  /**
   * Run this walker.
   */
  public void run() {

    if (uc != null) uc.markStartNow();

    if (verbose) {
      System.out.println(new Date() + ": Walking over '" + rootDir + "'");
    }

    if (fileOperator.initializeHook()) {
      try {
        execute(rootDir);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
      fileOperator.finalizeHook();
    }
    else {
      if (verbose) {
        System.out.println("fileOperator initializeHook over '" + rootDir + "' failed!");
      }
    }

    if (uc != null) uc.markEndNow();
  }

  /**
   * Execute over the current file.
   */
  private final boolean execute(File curFile) throws IOException {
    boolean keepGoing = true;

    // set currentFile reference
    currentFile.set(curFile);

    if (curFile.isDirectory()) {
      DirectorySelector.Action dirAction = DirectorySelector.Action.DESCEND; // default
      if (dirSelector != null) dirAction = dirSelector.select(curFile);

      switch (dirAction) {
        case DESCEND :
          keepGoing = descendDir(curFile); break;
        case SELECT :
          keepGoing = operate(curFile); break;
        case IGNORE :
          keepGoing = true; break;
        case STOP :
          keepGoing = false; break;
      }
    }
    else {  // isa file
      if (fileSelector == null || fileSelector.select(curFile)) {
        keepGoing = operate(curFile);
      }
    }

    return keepGoing;
  }

  /**
   * Descend into the given dir.
   */
  private final boolean descendDir(File curDir) throws IOException {
    boolean keepGoing = true;

    final File[] curFiles = curDir.listFiles();
    Arrays.sort(curFiles);  // for deterministic ordering

    for (File curFile : curFiles) {
      if (uc != null && uc.isTimeToQuit()) {
        //todo: collect resume info

        if (verbose) {
          System.out.println(new Date() + ": Quitting at dir '" + curFile + "'");
        }

        keepGoing = false;
        break;
      }

      keepGoing = execute(curFile);
    }

    return keepGoing;
  }

  /**
   * Operate on the given selected file (or dir).
   */
  private final boolean operate(File curFile) throws IOException {
    return performer.operate(curFile, this);
  }


  /**
   * Encapsulation of logic to perform a monitored counted operation.
   */
  private static abstract class UcOperationPerformer<X, C> {
    protected abstract boolean performOperation(X x, C context);

    private boolean verbose;
    private UnitCounter uc;

    UcOperationPerformer(boolean verbose, UnitCounter uc) {
      this.verbose = verbose;
      this.uc = uc;
    }

    public final boolean operate(X curx, C context) {
      boolean result = true;

      if (verbose) {
        System.out.println(new Date() + ": visiting uc #" + (uc == null ? '?' : uc.doneSoFar()) + " '" + curx + "'");
      }

      if (uc != null) {
        if (uc.isPaused()) {
          //todo: collect resume info


          if (verbose) {
            System.out.println(new Date() + ": Pausing before '" + curx + "'");
          }
        }

        if (uc.isTimeToQuit()) {
          result = false;
        }
      }

      if (result) {
        result = performOperation(curx, context);

        if (uc != null && !uc.inc()) {
          //todo: collect resume info

          if (verbose) {
            System.out.println(new Date() + ": Quitting after '" + curx + "'");
          }

          result = false;
        }
      }

      return result;
    }
  }


  /**
   * Helper class for iterator behavior.
   */
  private static final class WalkerIterator implements Iterator<File> {

    private final UnitCounter uc;
    private final AtomicReference<File> opFile = new AtomicReference<File>(null);
    private File next;

    WalkerIterator(File rootDir, DirectorySelector dirSelector, FileSelector fileSelector) {
      this.uc = new UnitCounter();
      final DirWalker walker = new DirWalker(false, uc, rootDir, dirSelector, fileSelector, new FileOperator() {
          public boolean initializeHook() {return true;}
          public boolean operate(File file) {
            uc.pause(0L);
            opFile.set(file);
            return true;
          }
          public void finalizeHook() {}
        });
      final GovernableThread thread = GovernableThread.newGovernableThread(walker, true);
      this.next = getNext();
    }

    public boolean hasNext() {
      return next != null;
    }

    public File next() {
      File result = next;
      next = getNext();
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException("Not supported!");
    }

    private final File getNext() {
      while (opFile.get() == null && !uc.hasEnded()) {}  // wait 
      File result = opFile.getAndSet(null);
      uc.resume();
      return result;
    }
  }


  /**
   * Example usage of iterator lists all files/dirs.
   */
  public static final void main(String[] args) {
    for (String arg : args) {
      final File file = new File(arg);
      System.out.println(new Date() + ": Starting walkerIterator(" + file + ")");
      for (Iterator<File> iter = iterator(file, null, null); iter.hasNext(); ) {
        System.out.println(new Date() + ": " + iter.next().getAbsolutePath());
      }
    }
  }
}
