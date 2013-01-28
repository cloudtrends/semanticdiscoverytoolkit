/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cio;

import java.io.File;
import java.io.IOException;
import org.sd.io.Publishable;
import org.sd.io.FileLock;

/**
 * An operation to write a publishable message to a file while
 * locking that file so that other processes don't read
 * <p>
 * @author asanderson
 */
public class LockWhileWritingOperation
  implements FileLock.LockOperation<Boolean>
{
  private Publishable publishable;
  public LockWhileWritingOperation(Publishable publishable) 
  {
    this.publishable = publishable;
  }
  
  public Boolean operate(String filename) 
    throws IOException 
  {
    boolean result = true;
    MessageHelper.dumpPublishable(new File(filename), publishable);
    return result;
  }
}
