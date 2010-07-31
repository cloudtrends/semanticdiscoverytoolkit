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
package org.sd.atn;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.sd.cio.MessageHelper;
import org.sd.io.PersistablePublishable;
import org.sd.util.InputContext;
import org.sd.util.StringInputContext;
import org.sd.xml.DomContext;

/**
 * Container around an input context for generating a key.
 * <p>
 * Note that when an instance is published, the original larger context will be
 * lost. All necessary pieces of the original context should be preserved in
 * the instance variables.
 *
 * @author Spence Koehler
 */
public class InputContextContainer extends PersistablePublishable {

  //
  //NOTE: If member variables are changed here or in subclasses, update
  //      CURRENT_VERSION and keep track of deserializing old persisted
  //      instances!
  //
  private static final int CURRENT_VERSION = 1;


  private InputContext inputContext;
  private DomContext _domContext;
  private String _xpath;
  private String _key;

  /**
   * Empty constructor for publishable reconstruction.
   */
  public InputContextContainer() {
  }

  public InputContextContainer(InputContext inputContext) {
    this.inputContext = inputContext;
    this._domContext = null;
    this._xpath = null;
    this._key = null;
  }

  public InputContext getInputContext() {
    return inputContext;
  }

  public DomContext getDomContext() {
    if (_domContext == null && inputContext instanceof DomContext) {
      _domContext = (DomContext)inputContext;
    }
    return _domContext;
  }

  /**
   * Get the xpath to this extraction if available.
   * <p>
   * Note that this can be used as a key to similar extractions across html pages.
   */
  public String getXPath() {
    if (_xpath == null) {
      final DomContext domContext = getDomContext();
      if (domContext != null) {
        _xpath = domContext.getIndexedPathString();
      }
    }
    return _xpath;
  }

  public String getKey() {
    if (_key == null) {
      _key = buildKey();
    }
    return _key;
  }

  private final String buildKey() {
    final StringBuilder result = new StringBuilder();

    final String xpath = getXPath();
    if (xpath != null) {
      // the xpath alone is a good key for DomContexts
      result.append(xpath);
    }
    else {
      // for other contexts, add colon-separated inputContext.getId() up to,
      // but not including the ultimate root (unless the curContext IS the root).
      InputContext prevContext = null;
      for (InputContext curInputContext = inputContext; curInputContext != null; curInputContext = curInputContext.getContextRoot()) {
        final InputContext rootContext = curInputContext.getContextRoot();
        if (rootContext == null || rootContext == prevContext) break;
        prevContext = curInputContext;

        if (result.length() > 0) result.insert(0, '/');
        result.insert(0, curInputContext.getId());
      }
    }

    return result.toString();
  }


  ////////
  //
  // PersistablePublishable interface implementation

  /**
   * Get the current version.
   * <p>
   * Note that changes to subclasses as well as to this class will require
   * this value to change and proper handling in the write/read methods.
   */
  protected final int getCurrentVersion() {
    return CURRENT_VERSION;
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  protected void writeCurrentVersion(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(inputContext.getId());
    MessageHelper.writeString(dataOutput, inputContext.getText());
    MessageHelper.writeString(dataOutput, getXPath());
    MessageHelper.writeString(dataOutput, getKey());
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
  protected void readVersion(int version, DataInput dataInput) throws IOException {
    if (version == 1) {
      readVersion1(dataInput);
    }
    else {
      badVersion(version);
    }
  }

  private final void readVersion1(DataInput dataInput) throws IOException {
    final int contextId = dataInput.readInt();
    final String contextText = MessageHelper.readString(dataInput);
    this.inputContext = new StringInputContext(contextText, contextId);
    this._xpath = MessageHelper.readString(dataInput);
    this._key = MessageHelper.readString(dataInput);
  }

  //
  // end of PersistablePublishable interface implementation
  //
  ////////

}
