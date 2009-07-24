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
package org.sd.util.thread;


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convenience/reference implementation of the governable interface.
 * <p>
 * Governance can be added to a class in the following ways:
 * <ul>
 * <li>Have the class extend this class.</li>
 * <li>Use a strategy pattern where this class is extended by the strategy class.</li>
 * </ul>
 * <p>
 * In either case, the (Base)Governable class would be used as follows:
 * <pre>
 * final Governable myGovernable = asStrategy ? new MyGovernable(this, ...) : new MyGovernable(...);
 * final GovernableThread thread = new GovernableThread(myGovernable);
 * thread.start();
 * ...
 * // control/monitor the process through myGovernable.getUnitCounter()
 * //   or from thread.getUnitCounter() [same thing].
 * // pause or kill through thread.pauseFor or thread.kill
 * </pre>
 * An example to perform an operation for a number of times or a certain length
 * of time, whichever comes first, would be:
 * <pre>
 * GovernableThread.newGovernableThread(new BaseGovernable(numTimes) {
 *     protected boolean doOperation(long workUnit, AtomicBoolean die) {
 *       //...do operation number workUnit here...
 *     }
 *   }, true).runFor(duration, waitMillis, interrupt);
 * </pre>
 * For the strategy pattern, the operations that need to be formed would be
 * wrapped in the strategy class. Note that with this pattern, a class that
 * uses the strategy usually passes itself into the strategy class on
 * construction.
 *
 * @author Spence Koehler
 */
public abstract class BaseGovernable implements Governable {

  /**
   * Do the operation for workUnit (by number).
   *
   * @param workUnit  the (0-based) number of the work unit to perform.
   * @param die  the die flag to monitor for halting the process
   *
   * @return true unless we wish to end the iteration (without incrementing
   *         the UnitCounter or calling 'quit'). The other way to end iteration
   *         (to increment and call quit) would be to return false here, but
   *         set die to true.
   */
  protected abstract boolean doOperation(long workUnit, AtomicBoolean die);

  /**
   * Clean up hook for quitting the iteration before or after operating
   * on workUnit. This is called if unitCounter.isTimeToQuit() returns
   * true at the beginning and again at the end of the iteration loop.
   * <p>
   * The default implementation does nothing. Extenders should override.
   *
   * @param workUnit  the (0-based) number of the work unit to perform.
   * @param operated  if false, then workUnit has NOT been operated on.
   */
  protected void quit(long workUnit, boolean operated) {
    // no-op
  }


  private UnitCounter uc;

  /**
   * Initialize without knowing how many work units are to be done.
   */
  protected BaseGovernable() {
    this(-1L);
  }

  /**
   * Initialize with the given number of work units to be done.
   */
  protected BaseGovernable(long toBeDone) {
    this.uc = new UnitCounter(toBeDone);
  }

  /**
   * Set the number of work units to be done.
   */
  protected void setToBeDone(long toBeDone) {
    uc.setToBeDone(toBeDone);
  }

  /**
   * Get this instance's UnitCounter for progress and flow control
   * communications.
   *
   * @return a non-null unit counter instance that is updated and monitored
   *         within this class's run method (directly or indirectly).
   */
  public UnitCounter getUnitCounter() {
    return uc;
  }

  /**
   * Run processing.
   */
  public void run() {
    uc.markStartNow();

    final boolean unknownLimit = uc.toBeDone() < 0;
    for (long workUnit = 0; workUnit < uc.toBeDone() || unknownLimit; ++workUnit) {
      if (uc.isTimeToQuit()) {  // checks for die and waits for pause to resume
        quit(workUnit, false);  // clean up for clean quit before processing workUnit
        break;
      }

      if (!doOperation(workUnit, uc.die())) {
        // do unit processing; sending die down into stack for checking
        break;
      }

      if (!uc.inc()) {  // inc will count, but will return false if time to die
        quit(workUnit, true);
        break;
      }
    }

    uc.markEndNow();
  }
}
