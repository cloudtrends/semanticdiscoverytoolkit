/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cluster.util;

import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.cio.MessageHelper;
import org.sd.util.RollingStore;
import org.sd.cluster.config.Config;
import org.sd.cluster.config.ClusterContext;
import java.io.DataOutputStream;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Utility class for holding stores of publishable messages
 * <p>
 * @author Abe Sanderson
 */
public class MessageStore
  extends RollingStore<Publishable>
{
  public static final String DEFAULT_JOBID = "message";
  public static final String DEFAULT_PREFIX = "message";
  public static final String DEFAULT_SUFFIX = "out";
  private static final DateFormat s_format = new SimpleDateFormat("yyyy-MM-dd-kk:mm:ss");
  private static final int ROLL_INTERVAL = 3600;  // number of seconds between rolls

  private File storeDir;
  private String storeNamePrefix;
  private String storeNameSuffix;
  private Pattern namePattern;
  private List<File> storeFiles;

  public MessageStore(Config config, String dataDirName) 
  {
    this(config, DEFAULT_JOBID, dataDirName, 
         new Date(), DEFAULT_PREFIX, DEFAULT_SUFFIX);
  }

  public MessageStore(Config config, String jobIdString, String dataDirName, 
                      Date startDate, String storeNamePrefix, String storeNameSuffix) 
  {
    super(null, true, false, true);

    this.storeNamePrefix = storeNamePrefix;
    this.storeNameSuffix = storeNameSuffix;
    this.storeDir = new File(config.getOutputDataPath(jobIdString, dataDirName));
    this.namePattern = Pattern.compile("^" + storeNamePrefix + "-(.*)\\."+storeNameSuffix+"$");
    this.storeFiles = getStoreFiles();

    initialize(null);
  }

  private final Comparator<File> s_storeComparator = 
    new Comparator<File>() 
    {
      public int compare(File f1, File f2) 
      {
        final Date d1 = getStoreDate(f1, namePattern);
        final Date d2 = getStoreDate(f2, namePattern);
        return d1.compareTo(d2);
      }
      public boolean equals(Object o) 
      {
        return this == o;
      }
    };

  private final List<File> getStoreFiles() 
  {
    final List<File> result = new ArrayList<File>();

    if (!storeDir.exists()) {
      storeDir.mkdirs();
    }
    else {
      final File[] files = storeDir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return namePattern.matcher(name).matches();
          }
        });
      Arrays.sort(files, s_storeComparator);
      for (File file : files) result.add(file);
    }

    return result;
  }

  public List<File> getStoreFiles(Date startDate, Date endDate) 
  {
    List<File> result = new ArrayList<File>();
    for(File storeFile : getStoreFiles())
    {
      if((startDate == null || getStoreDate(storeFile, namePattern).compareTo(startDate) >= 0) &&
         (endDate == null || getStoreDate(storeFile, namePattern).compareTo(endDate) <= 0))
        result.add(storeFile);
    }
    return result;
  }

  /**
   * Get the store number for the file (if it has one) or -1.
   */
  private static final Date getStoreDate(final File storeFile, Pattern p) 
  {
    Date result = null;
    final Matcher m = p.matcher(storeFile.getName());
    if (m.matches()) 
    {
      try
      {
        result = s_format.parse(m.group(1));
      }
      catch(ParseException parseex)
      {
        System.err.println("Error while attempting to parse date from file("+storeFile.getName()+"):"+
                           parseex.getMessage());
      }
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
    return new PublishableStore(storeFile, namePattern);
  }

  /**
   * Get the next available store file as the last element of the list (and all
   * existing store files as prior elements in the list).
   */
  protected List<File> nextAvailableFile() 
  {
    File result = new File(storeDir, getStoreFilename(new Date()));
    storeFiles.add(result);
    return storeFiles;
  }

  private String getStoreFilename(Date date)
  {
    long ts = date.getTime();
    long tsMod = (ts/(ROLL_INTERVAL * 1000L)) * (ROLL_INTERVAL * 1000L);

    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append(storeNamePrefix);
    nameBuilder.append("-");
    nameBuilder.append(s_format.format(new Date(tsMod)));
    nameBuilder.append(".");
    nameBuilder.append(storeNameSuffix);
    return nameBuilder.toString();
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

  public void writeMessage(Publishable message)
    throws IOException
  {
    PublishableStore store = (PublishableStore)getStore();
    if (store != null) 
    {
      boolean rollover = store.shouldRoll();
      if (rollover)
      {
        roll(); // not rolling with add, since we will add after roll
        store = (PublishableStore)getStore();
      }

      synchronized (store) {
        store.addElement(message);
      }
    }
  }

  public static final class PublishableStore 
    implements Store<Publishable> 
  {
    private long createDate;
    private File outputFile;
    private OutputStream outputStore;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    public PublishableStore(File outputFile, Pattern p) {
      this.outputFile = outputFile;
      this.createDate = getStoreDate(outputFile, p).getTime();
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
      DataOutputStream out = new DataOutputStream(outputStore);
      MessageHelper.writePublishable(out, message);
      out.flush();
      return false;
    }

    // always append to the existing store
    // (rely on the interval to roll the store upon resume)
    public boolean shouldResume() 
    {
      return true;
    }

    // roll on an interval(time)
    public boolean shouldRoll() 
    {
      long interval = System.currentTimeMillis() - createDate;
      return (interval > (ROLL_INTERVAL * 1000L));
    }
  }
}
