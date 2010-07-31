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
import java.util.ArrayList;
import java.util.List;
import org.sd.cio.MessageHelper;
import org.sd.io.PersistablePublishable;
import org.sd.util.InputContext;
import org.sd.util.tree.Tree;
import org.sd.xml.DomContext;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlReconstructor;

/**
 * Container for a group of ExtractionContainer instances.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionGroup extends PersistablePublishable {

  //
  //NOTE: If member variables are changed here or in subclasses, update
  //      CURRENT_VERSION and keep track of deserializing old persisted
  //      instances!
  //
  private static final int CURRENT_VERSION = 1;


  // key to the group's context
  private String _key;

  // group's extractions
  private List<ExtractionContainer> extractions;

  // group's input DomNode if available
  private DomNode _inputDomNode;

  // group's input text if available and inputDomNode is not
  private String _inputText;

  public ExtractionGroup() {
    this._key = null;
    this.extractions = new ArrayList<ExtractionContainer>();

    this._inputDomNode = null;
    this._inputText = null;
  }

  /**
   * Add the extractionContainer to this group.
   */
  public final void add(ExtractionContainer extractionContainer) {
    extractions.add(extractionContainer);
  }

  /**
   * Get this group's context key.
   */
  public final String getKey() {
    if (_key == null) {
      this._key = computeKey();
    }
    return _key;
  }

  /**
   * Get this group's extractions.
   */
  public final List<ExtractionContainer> getExtractions() {
    //todo: if we worry about consumers modifying the underlying list, return a copy.
    //todo: maybe this is where we would sort if necessary
    return extractions;
  }

  /**
   * Get the input DomNode for this group if available or null.
   */
  public DomNode getInputNode() {
    if (_inputDomNode == null) {
      _inputDomNode = computeInputDomNode();
    }
    return _inputDomNode;
  }

  /**
   * Get the input text for this group if available or null.
   */
  public String getInputText() {
    if (_inputText == null) {
      _inputText = computeInputText();
    }
    return _inputText;
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
    MessageHelper.writeString(dataOutput, _key);

    dataOutput.writeInt(extractions.size());
    for (ExtractionContainer extraction : extractions) {
      MessageHelper.writePublishable(dataOutput, extraction);
    }
    
    DomUtil.writeDomNode(dataOutput, getInputNode());
    MessageHelper.writeString(dataOutput, getInputText());
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
    this._key = MessageHelper.readString(dataInput);

    final int numExtractions = dataInput.readInt();
    for (int extNum = 0; extNum < numExtractions; ++extNum) {
      extractions.add((ExtractionContainer)MessageHelper.readPublishable(dataInput));
    }

    this._inputDomNode = DomUtil.readDomNode(dataInput);
    this._inputText = MessageHelper.readString(dataInput);
  }

  //
  // end of PersistablePublishable interface implementation
  //
  ////////


  private final String computeKey() {
    // find deepest common ancestor of all terminals in reconstructed xml tree
    Tree<XmlLite.Data> dca = null;
    for (ExtractionContainer ec : extractions) {
      final Tree<XmlLite.Data> terminal = ec.getTerminalNode();
      if (terminal != null) {
        if (dca == null) dca = terminal;
        else {
          dca = dca.getDeepestCommonAncestor(terminal);
        }
      }
    }

    // turn back into path string
    final XmlReconstructor.TagMapping tagMapping = (XmlReconstructor.TagMapping)(dca.getAttributes().get("TagMapping"));
    return tagMapping.getKey();
  }

  private final DomNode computeInputDomNode() {
    DomNode result = null;

    if (extractions.size() == 0) return result;

    final ExtractionContainer extraction = extractions.get(0);
    final InputContextContainer inputContextContainer = extraction.getInputContextContainer();
    if (inputContextContainer != null) {
      final DomContext domContext = inputContextContainer.getDomContext();
      if (domContext != null) {
        result = domContext.findDomNode(getKey());
      }
    }

    return result;
  }

  private final String computeInputText() {
    String result = null;

    final DomNode inputNode = getInputNode();
    if (inputNode != null) {
      // collect all text under inputNode
      result = DomUtil.getTrimmedNodeText(inputNode);
    }
    else {
      // decode non-xml input context string
      if (extractions.size() > 0) {
        final ExtractionContainer extraction = extractions.get(0);
        final InputContextContainer inputContextContainer = extraction.getInputContextContainer();
        if (inputContextContainer != null) {
          final InputContext inputContext = inputContextContainer.getInputContext();
          if (inputContext != null) {
            //todo: reconcile group key against inputContext to get full group text
            result = inputContext.getText();
          }
        }
      }
    }

    return result;
  }
}
