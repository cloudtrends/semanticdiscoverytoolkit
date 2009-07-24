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
package org.sd.crawl;


import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A container for the results of a page processor.
 * <p>
 * This container provides access to a String representation for its value,
 * but the value need not (and usually should not) be represented as a string
 * in the underlying implementation.
 *
 * @author Spence Koehler
 */
public abstract class ProcessorResult implements Publishable {

  /**
   * Get this result's information with html markup.
   */
  public abstract String toHtml(int maxRows);

  /**
   * Get this result's value as a string.
   */
  protected abstract String createValueAsString();

  /**
   * Do the work of combining other instance's value into this instance.
   */
  protected abstract void doCombineWithOthers(ProcessorResult[] others);

//   /**
//    * Do the work of combining this instance's value with the given string
//    * forms of its value.
//    */
//   protected abstract void doCombineWithOthers(String[] value);


  private String simpleName;
  private String name;

  private boolean dirty;  // used to recalculate the string value.
  private String _value;  // cached built string form of the value

  /**
   * Default constructor for publishable reconstruction only.
   */
  protected ProcessorResult() {
  }

  /**
   * Construct with the given name.
   */
  protected ProcessorResult(String name) {
    this.simpleName = name;
    this.name = this.getClass().getName() + ":" + name;  // provide unique names by class
    this.dirty = true;
    this._value = null;
  }

  /**
   * Get this processor result's simple name.
   */
  public String getSimpleName() {
    return simpleName;
  }

  /**
   * Get this processor result's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Set this instance's dirty flag to indicate that the string form of the
   * value will need to be rebuilt.
   */
  protected final void setDirtyFlag() {
    this.dirty = true;
  }

  /**
   * Combine the other instances' data into this.
   */
  public final void combineWithOthers(ProcessorResult[] others) {
    setDirtyFlag();
    doCombineWithOthers(others);
  }

  /**
   * Convenience method to combine the other instance's data into this.
   */
  public final void combineWithOther(ProcessorResult other) {
    setDirtyFlag();
    combineWithOthers(new ProcessorResult[]{other});
  }

  /**
   * Get this instance's value as a string.
   */
  public final String getValue() {
    if (_value == null || dirty) {
      this._value = createValueAsString();
    }
    return _value;
  }

//   /**
//    * Combine this instance's value with the other values.
//    */
//   public final void combineWithOthers(String[] values) {
//     setDirtyFlag();
//     doCombineWithOthers(values);
//   }

//   /**
//    * Convenience method to combine this instance's value with another's.
//    */
//   public final void combineWithOther(String value) {
//     doCombineWithOthers(new String[]{value});
//   }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, simpleName);
    MessageHelper.writeString(dataOutput, name);
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
    this.simpleName = MessageHelper.readString(dataInput);
    this.name = MessageHelper.readString(dataInput);
  }
}
