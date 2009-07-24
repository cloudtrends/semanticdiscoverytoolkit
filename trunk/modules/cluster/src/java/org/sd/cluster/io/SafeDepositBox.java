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
package org.sd.cluster.io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;
import org.sd.util.thread.UnitCounter;

/**
 * Container for SafeDepositMessage results waiting to be claimed.
 * <p>
 * @author Spence Koehler
 */
public class SafeDepositBox implements SafeDepositBoxMXBean, Shutdownable {

  public enum WithdrawalCode {NO_DEPOSIT, EXPIRED, RETRIEVED, UNRESERVED};


  private static final AtomicLong NEXT_NUM = new AtomicLong(0);
  private static final long MONITOR_DELAY = 10;  // delay of 10 seconds
  private static final TimeUnit MONITOR_UNIT = TimeUnit.SECONDS;
  private static final double MEMORY_LIMIT = 0.85;  // purge cache if free mem < 85% of max


  private final Map<Long, Drawer> claimNum2Drawer = new HashMap<Long, Drawer>();
  private final Map<String, Long> key2claimNum = new HashMap<String, Long>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private ExpirationMonitor expirationMonitor;
  private ScheduledFuture<?> expirationHandle;

  private long freeMemoryLimit;
  private AdminInfo _adminInfo;

  /**
   * Default constructor.
   * <p>
   * Construct an instance with monitorDelay of 10 seconds and to disable
   * caching when memory reaches 85% capacity.
   */
  public SafeDepositBox() {
    init(MEMORY_LIMIT);
  }

  /**
   * Construct an instance.
   * <p>
   * @param monitorDelay     the amount of time between cleaning house.
   * @param delayUnit        the units of monitorDelay
   * @param freeMemoryLimit  the percentage of memory capacity at which caching
   *                         will be (temporarily) disabled. That is, when free
   *                         memory is below the limit, all aging drawers will
   *                         be incinerated.
   */
  public SafeDepositBox(long monitorDelay, TimeUnit delayUnit, double freeMemoryLimit) {
    this.expirationMonitor = new ExpirationMonitor(this);
    this.expirationHandle =
      scheduler.scheduleWithFixedDelay(expirationMonitor, monitorDelay, monitorDelay, delayUnit);

    init(freeMemoryLimit);
  }

  private final void init(double freeMemoryLimit) {
    this.freeMemoryLimit = (long)(freeMemoryLimit * Runtime.getRuntime().totalMemory());
    this._adminInfo = null;
  }

  /**
   * Shutdown this instance.
   */
  public void shutdown(boolean now) {
    if (expirationHandle != null) expirationHandle.cancel(now);
  }

  /**
   * Reserve a drawer for a time.
   *
   * @param forMillis  the number of millis after opening (now) before the drawer expires.
   *                   if 0, then the drawer will expire when memory runs low.
   *                   if negative, then the drawer will expire this magnitude of millis
   *                   after successful withdawal. (In other words, cache the value for
   *                   repeated retrieval the given amount of time. Each time it is retrieved,
   *                   the cache clock will be reset.)
   * @param key        A key to associate with the new claim number (if non-null).
   *
   * @return the claim number to use to access the drawer.
   */
  public long reserveDrawer(long forMillis, String key, UnitCounter uc) {
    final long result = NEXT_NUM.getAndIncrement();
    claimNum2Drawer.put(result, new Drawer(forMillis, uc));

    if (key != null) {
      key2claimNum.put(key, result);
    }

//System.out.println("reserved drawer #" + result + " forMillis=" + forMillis + " key=" + key);

    this._adminInfo = null;  // this will need to be recomputed

    return result;
  }

  /**
   * Lookup the key to find its associated claim num.
   */
  public Long lookupKey(String key) {
//System.out.println("retrieving key '" + key + "' for " + key2claimNum.get(key));

    return key2claimNum.get(key);
  }

  /**
   * Close the drawer, incinerating its contents.
   */
  public void incinerate(long claimNumber) {
    claimNum2Drawer.remove(claimNumber);
    this._adminInfo = null;  // this will need to be recomputed
  }

  /**
   * Set the contents of the drawer associated with the claim number.
   *
   * @return true if the deposit succeeded; otherwise, false (i.e. if the drawer expired.)
   */
  public boolean deposit(long claimNumber, Publishable contents) {
    boolean result = false;

    final Drawer drawer = claimNum2Drawer.get(claimNumber);

    if (drawer != null) {
      drawer.setContents(contents);
      result = true;
    }

    this._adminInfo = null;  // this will need to be recomputed

    return result;
  }

  /**
   * Determine whether the claim number has been reserved.
   */
  public boolean wasReserved(long claimNumber) {
    return claimNumber >= 0 && claimNumber < NEXT_NUM.get();
  }

  /**
   * Get the UnitCounter associated with the claim number.
   *
   * @return the UnitCounter or null if not found/available/present.
   */
  public UnitCounter getUnitCounter(long claimNumber) {
    UnitCounter result = null;

    final Drawer drawer = claimNum2Drawer.get(claimNumber);
    if (drawer != null) {
      result = drawer.getUnitCounter();
    }

    return result;
  }

  /**
   * Withdraw the contents associated with the claim number, optionally
   * closing the box if the contents are retrieved. Note that when closed,
   * the drawer can no longer be accessed.
   *
   * @return the contents associated with the claim number.
   */
  public Withdrawal withdraw(long claimNumber, boolean closeBox) {
    final long claimLimit = NEXT_NUM.get();

    final Drawer drawer = claimNum2Drawer.get(claimNumber);
    WithdrawalCode withdrawalCode = null;
    UnitCounter uc = null;
    Publishable contents = null;
    long openedTime = 0;
    long depositTime = 0;
    long withdrawalTime = 0;
    long expirationTime = 0;

//System.out.println("withdrawing from drawer #" + claimNumber + " closeBox=" + closeBox);

    if (drawer == null) {
      withdrawalCode = (claimNumber < claimLimit) ? WithdrawalCode.EXPIRED : WithdrawalCode.UNRESERVED;
    }
    else {
      uc = drawer.getUnitCounter();
      contents = drawer.getContents();
      withdrawalCode = (drawer.hasDeposit()) ? WithdrawalCode.RETRIEVED : WithdrawalCode.NO_DEPOSIT;
      openedTime = drawer.getOpenedTime();
      depositTime = drawer.getDepositTime();
      withdrawalTime = drawer.getWithdrawalTime();

      if (closeBox && withdrawalCode == WithdrawalCode.RETRIEVED) {
        drawer.expire();
        incinerate(claimNumber);
      }

      expirationTime = drawer.getExpirationTime();
    }

    return new Withdrawal(claimNumber, withdrawalCode, contents,
                          openedTime, depositTime, withdrawalTime,
                          expirationTime, uc);
  }

  /**
   * Incinerate expired and aged drawers.
   * <p>
   * Also destroys cached drawers when free memory is "low".
   */
  public void cleanHouse() {
    final boolean freeMem = Runtime.getRuntime().freeMemory() < freeMemoryLimit;
    final long curTime = System.currentTimeMillis();

    for (Iterator<Map.Entry<Long, Drawer>> iter = claimNum2Drawer.entrySet().iterator(); iter.hasNext(); ) {
      final Map.Entry<Long, Drawer> drawerEntry = iter.next();
      final Drawer drawer = drawerEntry.getValue();
      
      if (drawer.shouldIncinerate(curTime, freeMem)) {
        iter.remove();

        System.out.println(new Date() + ": NOTE: SafeDepositBox incinerating a drawer w/agedTime=" +
                           drawer.agedTime + ". (freeMem=" +
                           Runtime.getRuntime().freeMemory() + ", limit=" + freeMemoryLimit + ")");
      }
    }

    this._adminInfo = null;  // this will need to be recomputed
  }

  /**
   * Get the current administration information snapshot for this instance
   */
  public AdminInfo getAdminInfo() {
    if (_adminInfo == null && NEXT_NUM.get() > 0) {
      _adminInfo = new AdminInfo(NEXT_NUM.get(), freeMemoryLimit, key2claimNum, claimNum2Drawer);
    }
    return _adminInfo;
  }

  /**
   * Get the total number of drawers (active and incinerated).
   */
  public long getTotalDrawerCount() {
    return NEXT_NUM.get();
  }

  /**
   * Get the number of incinerated drawers.
   */
  public long getNumIncineratedDrawers() {
    long result = 0;

    final AdminInfo adminInfo = getAdminInfo();
    if (adminInfo != null) {
      result = adminInfo.numIncineratedDrawers();
    }

    return result;
  }

  /**
   * Get the number of active drawers (filled or filling).
   */
  public long getNumActiveDrawers() {
    long result = 0;

    final AdminInfo adminInfo = getAdminInfo();
    if (adminInfo != null) {
      result = adminInfo.numActiveDrawers();
    }

    return result;
  }

  /**
   * Get the number of active drawers that are filled.
   */
  public long getNumFilledDrawers() {
    long result = 0;

    final AdminInfo adminInfo = getAdminInfo();
    if (adminInfo != null) {
      result = adminInfo.numFilledDrawers();
    }

    return result;
  }

  /**
   * Get the number of active drawers that are yet to be filled.
   */
  public long getNumFillingDrawers() {
    long result = 0;

    final AdminInfo adminInfo = getAdminInfo();
    if (adminInfo != null) {
      result = adminInfo.numFillingDrawers();
    }

    return result;
  }

  /**
   * Get the keys for the filled drawers.
   */
  public List<String> getFilledKeys() {
    List<String> result = null;

    final AdminInfo adminInfo = getAdminInfo();
    if (adminInfo != null) {
      result = adminInfo.getFilledKeys();
    }

    return result;
  }

  /**
   * Get the contents from the drawer with the given key.
   */
  public String getContentsString(String key) {
    String result = null;

    final Long claimNum = key2claimNum.get(key);
    if (claimNum != null) {
      final Drawer drawer = claimNum2Drawer.get(claimNum);
      if (drawer != null && drawer.hasDeposit()) {
        final Publishable contents = drawer.publishable;  // avoid side-effects of getContents
        if (contents != null) {
          result = contents.toString();
        }
      }
    }

    return result;
  }

  /**
   * Incinerate the drawer with the given key.
   */
  public void incinerate(String key) {
    final Long claimNum = key2claimNum.get(key);
    if (claimNum != null) {
      incinerate(claimNum);
    }
  }

  /**
   * Incinerate all drawers that are older than the given age (in millis).
   */
  public long incinerateOlder(long age) {
    long result = 0;

    for (Map.Entry<String, Long> k2cEntry : key2claimNum.entrySet()) {
      final String key = k2cEntry.getKey();
      final Long claimNum = k2cEntry.getValue();
      final Drawer drawer = claimNum2Drawer.get(claimNum);
      if (drawer != null && drawer.agedTime > age) {
        incinerate(claimNum);
        ++result;
      }
    }

    return result;
  }


  private static final class ExpirationMonitor implements Runnable {

    private final SafeDepositBox safeDepositBox;

    ExpirationMonitor(SafeDepositBox safeDepositBox) {
      this.safeDepositBox = safeDepositBox;
    }

    public void run() {
      safeDepositBox.cleanHouse();
    }
  }

  /**
   * Container for a withdrawal from this instance.
   */
  public static final class Withdrawal implements Publishable {
    private long claimNumber;
    private WithdrawalCode withdrawalCode;
    private Publishable contents;
    private long openedTime;
    private long depositTime;
    private long withdrawalTime;
    private long expirationTime;
    private long[] completionRatio;

    /**
     * Empty constructor for publishable reconstruction.
     */
    public Withdrawal() {
    }

    /**
     * Construct with the given params.
     */
    public Withdrawal(long claimNumber, WithdrawalCode withdrawalCode, Publishable contents,
                      long openedTime, long depositTime, long withdrawalTime, long expirationTime,
                      UnitCounter uc) {
      this.claimNumber = claimNumber;
      this.withdrawalCode = withdrawalCode;
      this.contents = contents;
      this.openedTime = openedTime;
      this.depositTime = depositTime;
      this.withdrawalTime = withdrawalTime;
      this.expirationTime = expirationTime;
      this.completionRatio = uc == null ? null : uc.getCompletionRatio();
    }

    /**
     * Get the claim number.
     */
    public long getClaimNumber() {
      return claimNumber;
    }

    /**
     * Get the withdrawal code.
     */
    public WithdrawalCode getWithdrawalCode() {
      return withdrawalCode;
    }
    
    /**
     * Get the contents.
     */
    public Publishable getContents() {
      return contents;
    }

    /**
     * Get the time the drawer was opened.
     */
    public long getOpenedTime() {
      return openedTime;
    }

    /**
     * Get the time contents were deposited.
     */
    public long getDepositTime() {
      return depositTime;
    }

    /**
     * Get the time of this withdrawal.
     */
    public long getWithdrawalTime() {
      return withdrawalTime;
    }

    /**
     * Get the time the contents will expire.
     */
    public long getExpirationTime() {
      return expirationTime;
    }

    /**
     * Get the completion ratio from the process's unit counter.
     * <ul>
     * <li>doneSoFar -- the total number of units of work done so far or -1 if
     *                  counting has not been started.</li>
     * <li>toBeDone -- the total number of units of work to be done or -1 if
     *                 unknown.</li>
     * </ul>
     *
     * @return {doneSoFar, toBeDone} or null if unavailable.
     */
    public long[] getCompletionRatio() {
      return completionRatio;
    }

    /**
     * Write this message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      dataOutput.writeLong(claimNumber);
      MessageHelper.writeString(dataOutput, withdrawalCode.name());
      MessageHelper.writePublishable(dataOutput, contents);
      dataOutput.writeLong(openedTime);
      dataOutput.writeLong(depositTime);
      dataOutput.writeLong(withdrawalTime);
      dataOutput.writeLong(expirationTime);
      if (completionRatio == null) {
        dataOutput.writeInt(-1);
      }
      else {
        dataOutput.writeInt(completionRatio.length);
        for (long value : completionRatio) {
          dataOutput.writeLong(value);
        }
      }
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
      this.claimNumber = dataInput.readLong();
      final String wcName = MessageHelper.readString(dataInput);
      if (wcName != null) {
        this.withdrawalCode = Enum.valueOf(WithdrawalCode.class, wcName);
      }
      this.contents = MessageHelper.readPublishable(dataInput);
      this.openedTime = dataInput.readLong();
      this.depositTime = dataInput.readLong();
      this.withdrawalTime = dataInput.readLong();
      this.expirationTime = dataInput.readLong();
      final int numValues = dataInput.readInt();
      if (numValues < 0) {
        this.completionRatio = null;
      }
      else {
        this.completionRatio = new long[numValues];
        for (int i = 0; i < numValues; ++i) {
          completionRatio[i] = dataInput.readLong();
        }
      }
    }
  }

  /**
   * Container for contents.
   */
  private static final class Drawer {

    private long forMillis;
    private long expirationTime;
    private long agedTime;
    private UnitCounter uc;
    private Publishable publishable;

    private long openedTime;
    private long depositTime;
    private long withdrawalTime;

    /**
     * Construct with the given expiration/aging time.
     *
     * @param forMillis  If greater than 0, this drawer will be incinerated
     *                   this number of milliseconds after creation (now).
     *                   If 0, this drawer will be incinerated when memory
     *                   gets low (indefinite caching).
     *                   If less than 0, this drawer will be incinerated
     *                   this magnitude of milliseconds after deposit or
     *                   when memory gets low (time-limited caching).
     * @param uc  The UnitCounter that is tracking process progress.
     */
    Drawer(long forMillis, UnitCounter uc) {
      this.forMillis = forMillis;
      this.openedTime = System.currentTimeMillis();

      // if <=0, don't expire on timer.
      this.expirationTime = forMillis <= 0 ? 0 : forMillis + openedTime;
      this.agedTime = 0;

      this.uc = uc;
    }

    /**
     * Determine whether it is time for this drawer to be incinerated.
     */
    boolean shouldIncinerate(long curTime, boolean freeMem) {
      boolean result = isExpired(curTime) || isAged(curTime);

      if (!result && freeMem) {
        result = (agedTime > 0);
      }

      return result;
    }

    /**
     * Determine whether this drawer is expired.
     */
    boolean isExpired() {
      return isExpired(System.currentTimeMillis());
    }

    /**
     * Determine whether this drawer is expired.
     */
    boolean isExpired(long currentTime) {
      return expirationTime == 0 ? false : expirationTime >= currentTime;
    }

    /**
     * Determine whether this drawer is aged.
     */
    boolean isAged() {
      return isAged(System.currentTimeMillis());
    }

    /**
     * Determine whether this drawer is aged.
     */
    boolean isAged(long currentTime) {
      return agedTime == 0 ? false : agedTime >= currentTime;
    }

    /**
     * Return the UnitCounter associated with this drawer.
     */
    UnitCounter getUnitCounter() {
      return uc;
    }

    /**
     * Get this drawer's contents.
     */
    Publishable getContents() {
      if (hasDeposit()) {
        this.withdrawalTime = System.currentTimeMillis();
        if (forMillis < 0) {
          this.agedTime = withdrawalTime - forMillis;
        }
      }
      return publishable;
    }

    /**
     * Set this drawer's contents (deposit).
     */
    void setContents(Publishable publishable) {
      this.depositTime = System.currentTimeMillis();
      this.withdrawalTime = 0; // for re-using this drawer.
      this.publishable = publishable;
    }

    /**
     * Determine whether contents have been deposited.
     */
    boolean hasDeposit() {
      return depositTime > 0;
    }

    /**
     * Determine whether a deposit has been made and withdrawn.
     */
    boolean depositHasBeenWithdrawn() {
      return withdrawalTime > depositTime;
    }


    /**
     * Get this drawer's expiration time.
     */
    long getExpirationTime() {
      return expirationTime;
    }

    /**
     * Expire this drawer.
     */
    void expire() {
      this.expirationTime = System.currentTimeMillis();
    }

    /**
     * Get this drawer's opened time.
     */
    long getOpenedTime() {
      return openedTime;
    }

    /**
     * Get this drawer's deposit time.
     */
    long getDepositTime() {
      return depositTime;
    }

    /**
     * Get this drawer's last withdrawal time.
     */
    long getWithdrawalTime() {
      return withdrawalTime;
    }
  }

  public static final class AdminInfo implements Publishable {

    private long totalDrawers;      // total drawers ever created
    private long activeDrawers;     // portion of total that are active
    private long freeMemoryLimit;
    private long freeMemory;
    private long currentTime;
    private List<String> filledKeys;

    private Map<String, Long> key2claimNum;
    private Map<Long, Withdrawal> claimNum2Withdrawal;

    /**
     * Default constructor for publishable reconstruction.
     */
    public AdminInfo() {
    }

    /**
     * Construct with the given info.
     */
    public AdminInfo(long totalDrawers, long freeMemoryLimit,
                     Map<String, Long> key2claimNum,
                     Map<Long, Drawer> claimNum2Drawer) {
      this.totalDrawers = totalDrawers;
      this.activeDrawers = claimNum2Drawer.size();
      this.filledKeys = getFilledKeys(key2claimNum, claimNum2Drawer);

      this.freeMemoryLimit = freeMemoryLimit;
      this.freeMemory = Runtime.getRuntime().freeMemory();
      this.currentTime = System.currentTimeMillis();

      this.key2claimNum = new TreeMap<String, Long>(key2claimNum);

      this.claimNum2Withdrawal = new TreeMap<Long, Withdrawal>();
      for (Map.Entry<Long, Drawer> entry : claimNum2Drawer.entrySet()) {
        final Long claimNum = entry.getKey();
        final Drawer drawer = entry.getValue();
        claimNum2Withdrawal.put(claimNum, buildAdminWithdrawal(claimNum, drawer));
      }
    }

    /**
     * Count the number of filled drawers.
     */
    private final List<String> getFilledKeys(Map<String, Long> key2claimNum, Map<Long, Drawer> claimNum2Drawer) {
      final List<String> result = new ArrayList<String>();

      for (Map.Entry<String, Long> k2cEntry : key2claimNum.entrySet()) {
        final String key = k2cEntry.getKey();
        final Long claimNum = k2cEntry.getValue();
        final Drawer drawer = claimNum2Drawer.get(claimNum);
        if (drawer != null && drawer.hasDeposit()) {
          result.add(key);
        }
      }

      return result;
    }

    /**
     * Get the total number of drawers allocated for this box.
     */
    public long numTotalDrawers() {
      return totalDrawers;
    }

    /**
     * Get the total number of active drawers for this box.
     */
    public long numActiveDrawers() {
      return activeDrawers;
    }

    /**
     * Get the number of active drawers that have contents.
     */
    public long numFilledDrawers() {
      return filledKeys.size();
    }

    /**
     * Get the keys for drawers with contents.
     */
    public List<String> getFilledKeys() {
      return filledKeys;
    }

    /**
     * Get the number of active drawers that have yet to be filled.
     */
    public long numFillingDrawers() {
      return activeDrawers - filledKeys.size();
    }

    /**
     * Get the total number of drawers incinerated.
     */
    public long numIncineratedDrawers() {
      return totalDrawers - activeDrawers;
    }

    /**
     * Get the amount of free memory under which the scheduled cleanup will
     * occur.
     */
    public long getFreeMemoryLimit() {
      return freeMemoryLimit;
    }

    /**
     * Get the amount of memory currently free in the box's jvm.
     */
    public long getFreeMemory() {
      return freeMemory;
    }

    /**
     * Get the current time according to this box.
     */
    public long getCurrentTime() {
      return currentTime;
    }

    /**
     * Get the map of all keys (even incinerated) to their claim numbers.
     */
    public Map<String, Long> getKey2claimNum() {
      return key2claimNum;
    }

    /**
     * Get the map of all active claim numbers to their withdrawals.
     */
    public Map<Long, Withdrawal> getClaimNum2Withdrawal() {
      return claimNum2Withdrawal;
    }

    /**
     * Write this message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      dataOutput.writeLong(totalDrawers);
      dataOutput.writeLong(activeDrawers);
      dataOutput.writeLong(freeMemoryLimit);
      dataOutput.writeLong(freeMemory);
      dataOutput.writeLong(currentTime);

      if (key2claimNum == null) {
        dataOutput.writeInt(-1);
      }
      else {
        dataOutput.writeInt(key2claimNum.size());
        for (Map.Entry<String, Long> entry : key2claimNum.entrySet()) {
          MessageHelper.writeString(dataOutput, entry.getKey());
          dataOutput.writeLong(entry.getValue());
        }
      }

      if (claimNum2Withdrawal == null) {
        dataOutput.writeInt(-1);
      }
      else {
        dataOutput.writeInt(claimNum2Withdrawal.size());
        for (Map.Entry<Long, Withdrawal> entry : claimNum2Withdrawal.entrySet()) {
          dataOutput.writeLong(entry.getKey());
          MessageHelper.writePublishable(dataOutput, entry.getValue());
        }
      }
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
      this.totalDrawers = dataInput.readLong();
      this.activeDrawers = dataInput.readLong();
      this.freeMemoryLimit = dataInput.readLong();
      this.freeMemory = dataInput.readLong();
      this.currentTime = dataInput.readLong();

      final int numkeys = dataInput.readInt();
      if (numkeys > 0) {
        this.key2claimNum = new TreeMap<String, Long>();
        for (int i = 0; i < numkeys; ++i) {
          key2claimNum.put(MessageHelper.readString(dataInput), dataInput.readLong());
        }
      }

      final int numclaims = dataInput.readInt();
      if (numclaims > 0) {
        this.claimNum2Withdrawal = new TreeMap<Long, Withdrawal>();
        for (int i = 0; i < numclaims; ++i) {
          claimNum2Withdrawal.put(dataInput.readLong(), (Withdrawal)MessageHelper.readPublishable(dataInput));
        }
      }
    }

    /**
     * Build a withdrawal with the drawer information without the side effects
     * of a normal withdrawal.
     */
    private final Withdrawal buildAdminWithdrawal(Long claimNumber, Drawer drawer) {
      final WithdrawalCode withdrawalCode = (drawer.hasDeposit()) ? WithdrawalCode.RETRIEVED : WithdrawalCode.NO_DEPOSIT;
      final UnitCounter uc = drawer.getUnitCounter();
      final Publishable contents = drawer.publishable;  // avoid side-effects of getContents
      final long openedTime = drawer.getOpenedTime();
      final long depositTime = drawer.getDepositTime();
      final long withdrawalTime = drawer.getWithdrawalTime();
      final long expirationTime = drawer.getExpirationTime();

      return new Withdrawal(claimNumber, withdrawalCode, contents,
                            openedTime, depositTime, withdrawalTime,
                            expirationTime, uc);
    }
  }
}
