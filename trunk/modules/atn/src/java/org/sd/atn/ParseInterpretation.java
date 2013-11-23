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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;
import org.sd.util.Usage;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

/**
 * Container for an interpretation of a parse.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "Container for an interpretation of a parse.")
public class ParseInterpretation implements Publishable, Serializable {
  
  private static final long serialVersionUID = 42L;

  private String classification;
  private Serializable interpretation;
  private double confidence;
  private String toStringOverride;
  private Map<String, Serializable> category2Value;

  //todo: phase out above fields add confidence as attr on tree nodes; add xml accessors
  private transient Tree<XmlLite.Data> _interpTree;
  private String _interpXml;

  private transient AtnParse _sourceParse;
  private Parse _parse;
  private String _inputText;

  public ParseInterpretation() {
    init(null);
  }

  public ParseInterpretation(String classification) {
    init(classification);
  }

  public ParseInterpretation(Tree<XmlLite.Data> interpTree) {
    this._interpTree = interpTree;

    init(interpTree.getData().asTag().name);
  }

  private final void init(String classification) {
    if (this._interpTree == null && classification != null) {
      this._interpTree = XmlLite.createTagNode(classification);
    }

    this.classification = classification;
    this.interpretation = null;
    this.confidence = 1.0;
    this.toStringOverride = null;
    this.category2Value = null;
  }

  /**
   * Determine whether this interp has a source parse.
   */
  public boolean hasSourceParse() {
    return _sourceParse != null;
  }

  /**
   * Set the source parse for this interpretation.
   */
  public void setSourceParse(AtnParse sourceParse) {
    this._sourceParse = sourceParse;
  }

  /**
   * Get the (atn) source parse for this interpretation, if available.
   * <p>
   * Note that this will be unavailable in instances built through
   * deserialization.
   */
  public AtnParse getSourceParse() {
    return _sourceParse;
  }

  /**
   * Set the Parse associated with this instance.
   */
  public void setParse(Parse parse) {
    this._parse = parse;
  }

  /**
   * Get the free-standing parse information connected with this instance, if
   * available.
   */
  public Parse getParse() {
    if (_parse == null && _sourceParse != null) {
      _parse = _sourceParse.getParse();
    }
    return _parse;
  }

  /**
   * Get the input text that yielded this interpretation (if available).
   */
  public String getInputText() {
    if (_inputText == null) {
      final Parse parse = getParse();
      if (parse != null) {
        _inputText = parse.getParsedText();
      }
    }
    return _inputText;
  }

  public Tree<XmlLite.Data> getInterpTree() {
    if (_interpTree == null && _interpXml != null) {
      try {
        _interpTree = XmlFactory.buildXmlTree(_interpXml, true, false);
      }
      catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return this._interpTree;
  }


  /**
   * Get the interpretation text under the given named node (or full text if topNode is null).
   */
  public String getInterpText(String topNode) {
    final Tree<XmlLite.Data> interpTree = getInterpTree();
    return ParseInterpretationUtil.getInterpText(interpTree, topNode);
  }

  public String getInterpAttribute(String fromNode, String attribute) {
    final Tree<XmlLite.Data> interpTree = getInterpTree();
    return ParseInterpretationUtil.getInterpAttribute(interpTree, fromNode, attribute);
  }

  public boolean hasInterpNode(String nodeName) {
    return hasInterpAttribute(nodeName, null);
  }

  /**
   * Collect all (highest) interp nodes having the given name.
   * <p>
   * @return nodes w/name or null.
   */
  public List<Tree<XmlLite.Data>> getInterpNodes(String nodeName) {
    final Tree<XmlLite.Data> interpTree = getInterpTree();
    return ParseInterpretationUtil.getInterpNodes(interpTree, nodeName);
  }

  public boolean hasInterpAttribute(String nodeName, String attribute) {
    final Tree<XmlLite.Data> interpTree = getInterpTree();
    return ParseInterpretationUtil.hasInterpAttribute(interpTree, nodeName, attribute);
  }


  public String getInterpXml() {
    if (_interpXml == null && _interpTree != null) {
      final StringBuilder builder = new StringBuilder();
      final DomElement domElement = (DomElement)(_interpTree.getData().asDomNode());
      domElement.asFlatString(builder);
      _interpXml = builder.toString();
    }

    return _interpXml;
  }

  /**
   * The classification of this interpretation for future parsing.
   */
  public String getClassification() {
    return classification;
  }

  /**
   * The classification of this interpretation for future parsing.
   */
  public void setClassification(String classification) {
    this.classification = classification;
  }

  /**
   * Determine whether this instance has an explicitly set (serializable) interp object.
   */
  public boolean hasInterpretationObject() {
    return interpretation != null;
  }

  /**
   * An object representing the detailed interpretation when non-null.
   */
  public Object getInterpretation() {
    Object result = interpretation;

    if (result == null) {
      final Tree<XmlLite.Data> interpTree = getInterpTree();
      if (interpTree != null) {
        result = interpTree;
      }
      else {
        result = classification;
      }
    }

    return result;
  }

  /**
   * An object representing the detailed interpretation when non-null.
   * <p>
   * NOTE: If this interpretation is to be persisted, the Object must be serializable.
   */
  public void setInterpretation(Serializable interpretation) {
    this.interpretation = interpretation;
  }

  /**
   * A confidence associated with this interpretation, defaults to 1.0 if not
   * explicitly set.
   */
  public double getConfidence() {
    return confidence;
  }

  /**
   * A confidence associated with this interpretation, defaults to 1.0 if not
   * explicitly set.
   */
  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  /**
   * A string to override this instance's ToString when non-null.
   */
  public String getToStringOverride() {
    return toStringOverride;
  }

  /**
   * A string to override this instance's ToString when non-null.
   */
  public void setToStringOverride(String toStringOverride) {
    this.toStringOverride = toStringOverride;
  }

  /**
   * Mappings from categories to their interpreted values for components
   * of the interpretation.
   */
  public Map<String, Serializable> getCategory2Value() {
    return category2Value;
  }

  /**
   * Get the category mapping or null.
   */
  public Object get(String category) {
    Object result = null;

    if (category2Value != null) {
      result = category2Value.get(category);
    }

    return result;
  }

  /**
   * Add a mapping to this instance.
   * <p>
   * NOTE: If this interpretation is to be persisted, the Object must be serializable.
   */
  public void add(String category, Serializable value) {
    if (category2Value == null) category2Value = new HashMap<String, Serializable>();
    category2Value.put(category, value);
  }


  public String toString() {
    String result = toStringOverride;

    if (result == null) {
      if (interpretation != null) {
        result = interpretation.toString();
      }
      else {
        final Tree<XmlLite.Data> interpTree = getInterpTree();
        if (interpTree != null) {
          result = interpTree.getData().asDomNode().toString();
        }
      }
    }

    if (result == null) {
      result = super.toString();
    }

    return result;
  }

  public boolean equals(Object o) {
    boolean result = this == o;

    if (!result && o instanceof ParseInterpretation) {
      final ParseInterpretation other = (ParseInterpretation)o;

      result =
        (this.classification == other.getClassification() ||
         (this.classification != null && this.classification.equals(other.getClassification()))) &&
        (this.interpretation == other.getInterpretation() ||
         (this.interpretation != null && this.interpretation.equals(other.getInterpretation())));
    }

    return result;
  }

  public int hashCode() {
    int result = 13;

    result = 17 * result + this.classification.hashCode();
    if (this.interpretation != null) {
      result = 17 * result + this.interpretation.hashCode();
    }

    return result;
  }
  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, classification);
    MessageHelper.writeSerializable(dataOutput, interpretation);
    dataOutput.writeDouble(confidence);
    MessageHelper.writeString(dataOutput, toStringOverride);

    if (category2Value == null) {
      dataOutput.writeInt(0);
    }
    else {
      dataOutput.writeInt(category2Value.size());
      for (Map.Entry<String, Serializable> entry : category2Value.entrySet()) {
        MessageHelper.writeString(dataOutput, entry.getKey());
        MessageHelper.writeSerializable(dataOutput, entry.getValue());
      }
    }

    MessageHelper.writeString(dataOutput, getInterpXml());
    MessageHelper.writePublishable(dataOutput, getParse());
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
    this.classification = MessageHelper.readString(dataInput);
    this.interpretation = MessageHelper.readSerializable(dataInput);
    this.confidence = dataInput.readDouble();
    this.toStringOverride = MessageHelper.readString(dataInput);

    final int numC2Vs = dataInput.readInt();
    if (numC2Vs > 0) {
      this.category2Value = new HashMap<String, Serializable>();
      for (int c2vNum = 0; c2vNum < numC2Vs; ++c2vNum) {
        category2Value.put(MessageHelper.readString(dataInput),
                           MessageHelper.readSerializable(dataInput));
      }
    }

    this._interpXml = MessageHelper.readString(dataInput);
    this._parse = (Parse)MessageHelper.readPublishable(dataInput);
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    write(out);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    read(in);
  }
}
