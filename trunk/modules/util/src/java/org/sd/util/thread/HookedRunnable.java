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


/**
 * A runnable with before and after execution hooks.
 * <p>
 * NOTE: this is the type of runnable executed in a BlockingThreadPool.
 *
 * @author Spence Koehler
 */
public interface HookedRunnable extends Killable {

  /**
   * Hook to be executed just before the 'run' method when a thread handles
   * this runnable.
   */
  public void preRunHook();

  /**
   * Hook to be executed just after the 'run' method when a thread handles
   * this runnable.
   */
  public void postRunHook();

  /**
   * Hook to be executed if there is an Exception thrown while running.
   * <p>
   * This hook is called if there is an exception thrown during preRunHook,
   * run, or postRunHook.
   */
  public void exceptionHook(Throwable t);
}
