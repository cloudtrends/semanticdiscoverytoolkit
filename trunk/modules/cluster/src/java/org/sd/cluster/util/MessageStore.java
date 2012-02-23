package org.sd.cluster.util;

import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.cio.MessageHelper;
import org.sd.util.RollingStore;
import org.sd.cluster.config.Config;
import org.sd.cluster.config.ClusterContext;
import java.io.OutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for holding stores of publishable messages
 * <p>
 * @author Abe Sanderson
 */
public class MessageStore
  extends RollingStore<Publishable>
{
  private File storeDir;
  private String storeName;
  private String storeNamePrefix;
  private String storeNameSuffix;
  private Pattern namePattern;
  private List<File> storeFiles;

  public MessageStore(Config config, String jobIdString, 
                      String dataDirName, String storeFileName) 
  {
    super(null, true, false, true);

    final File firstStoreFile = getFirstStoreFile(config, jobIdString, dataDirName, storeFileName);
    this.storeDir = firstStoreFile.getParentFile();
    this.storeName = storeName;

    final int storeLen = storeName.length();
    int lastDotPos = storeName.lastIndexOf('.');
    if (lastDotPos < 0) lastDotPos = storeLen;

    this.storeNamePrefix = 
      lastDotPos >= 0 ? storeName.substring(0, lastDotPos) : storeName;
    this.storeNameSuffix = 
      lastDotPos < 0 || lastDotPos >= storeLen ? "" : storeName.substring(lastDotPos);
    this.namePattern = Pattern.compile("^" + storeNamePrefix + "-(\\d+)" + storeNameSuffix + "(.*)$");
    this.storeFiles = getStoreFiles(storeDir, storeNamePrefix, storeNameSuffix, namePattern);

    initialize(null);
  }

  private final File getFirstStoreFile(Config config, String jobIdString,  
                                       String dataDirName, String storeName) 
  {
    String filename = config.getOutputDataPath(jobIdString, dataDirName);
    return new File(filename + "/" + storeName);
  }

  /**
   * Get the last existing store file, or null if none exist.
   */
  private final File getLastStoreFile() 
  {
    File result = null;

    if (this.storeFiles.size() > 0) {
      result = storeFiles.get(storeFiles.size() - 1);
    }

    return result;
  }

  private final List<File> getStoreFiles(File storeDir, 
                                       String storeNamePrefix, String storeNameSuffix, 
                                       final Pattern p) 
  {
    final List<File> result = new ArrayList<File>();

    if (!storeDir.exists()) {
      storeDir.mkdirs();
    }
    else {
      final File[] files = storeDir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return p.matcher(name).matches();
          }
        });
      Arrays.sort(files, new Comparator<File>() {
          public int compare(File f1, File f2) {
            final int i1 = getStoreNumber(f1, p);
            final int i2 = getStoreNumber(f2, p);
            return i1 - i2;
          }
          public boolean equals(Object o) {
            return this == o;
          }
        });
      for (File file : files) result.add(file);
    }

    return result;
  }

  /**
   * Get the store number for the file (if it has one) or -1.
   */
  private final int getStoreNumber(final File storeFile, final Pattern p) 
  {
    int result = -1;

    final Matcher m = p.matcher(storeFile.getName());
    if (m.matches()) {
      result = Integer.parseInt(m.group(1));
    }

    return result;
  }

  /**
   * Get the root location of this store's elements
   */
  public File getStoreRoot() {
    return storeDir;
  }

  /**
   * Build an instance of a store at the given location.
   */
  protected Store<Publishable> buildStore(File storeFile) 
  {
    return new PublishableStore(storeFile);
  }

  /**
   * Get the next available store file as the last element of the list (and all
   * existing store files as prior elements in the list).
   */
  protected List<File> nextAvailableFile() 
  {
    File result = null;

    final File lastStoreFile = getLastStoreFile();
    int nextNum = 0;

    if (lastStoreFile != null) {
      nextNum = getStoreNumber(lastStoreFile, namePattern) + 1;
    }

    result = new File(storeDir, storeNamePrefix + "-" + nextNum + storeNameSuffix);
    storeFiles.add(result);

    return storeFiles;
  }

  /**
   * Hooked called while (after) opening.
   */
  protected void afterOpenHook(boolean firstTime, 
                               List<File> nextAvailable, 
                               File justOpenedStore) 
  {
    // nothing to do here just yet
  }

  public static final class PublishableStore 
    implements Store<Publishable> 
  {
    private long createDate = System.currentTimeMillis();
    private File outputFile;
    private OutputStream outputStore;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    public PublishableStore(File outputFile) {
      this.outputFile = outputFile;
    }

    public void open() 
      throws IOException 
    {
      this.outputStore = FileUtil.getOutputStream(outputFile, true);  // always append
    }

    public File getDirPath() {
      return outputFile;
    }

    public void close(boolean closingInThread) 
      throws IOException 
    {
      if (closed.compareAndSet(false, true)) 
      {
        if (outputStore != null) 
        {
          outputStore.flush();
          outputStore.close();
        }
      }
    }

    public boolean isClosed() 
    {
      return closed.get();
    }

    public boolean addElement(Publishable message)
      throws IOException 
    {
      MessageHelper.dumpPublishable(outputStore, message);
      outputStore.flush();
      return false;
    }

    // always start a new store on restarts
    public boolean shouldResume() 
    {
      return true;
    }

    // roll on an interval(time)
    public boolean shouldRoll() 
    {
      long interval = System.currentTimeMillis() - createDate;
      return (interval > 3600000L); // 1hr intervals
    }
  }
}
