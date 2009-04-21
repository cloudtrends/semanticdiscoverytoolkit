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
package org.sd.classifier;


import org.sd.util.MathUtil;
import org.sd.util.PropertiesParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.sd.io.FileUtil;

/**
 * A class to keep track of all feature attributes added to feature
 * vectors for a training set.
 * <p>
 * A FeatureDictionary that is "unlocked" collects features updated from
 * FeatureVector.setValue (called during feature extraction) while collecting
 * training data.
 * <p>
 * A "locked" FeatureDictionary is loaded with the features used during
 * training and stored in a trained classifier's ARFF file. When update requests
 * are received on a locked dictionary, the new features are logged for feedback
 * on a classifier's relevance to current input and should be monitored closely
 * for indications that retraining is necessary.
 * 
 * @author Spence Koehler, Dave Barney
 */
public class FeatureDictionary {

  /** Exact start of line containing classificaiton attribute name in input ARFF */
  public static final String CLASSIFICATION_NAME_COMMENT = "%CLASSIFICATION=";
  
  /** Construction property to specify the input arff path for building a locked feature dictionary */
  public static final String INPUT_ARFF_PROPERTY = "inputArff";

  /** Required construction properties for building a locked dictionary */
  public static final String[] LOCKED_PROPERTY_NAMES = new String[] {
    INPUT_ARFF_PROPERTY,
  };

  /** Construction property to specify the name of a dataset */
  public static final String DATASET_NAME_PROPERTY = "dataSet";

  /** Construction property to specify the classification attribute constraints */
  public static final String CLASSIFICATION_CONSTRAINTS_PROPERTY = "classificationAttribute";

  /** Required construction properties for building an unlocked dictionary */
  public static final String[] UNLOCKED_PROPERTY_NAMES = new String[] {
    DATASET_NAME_PROPERTY,
    CLASSIFICATION_CONSTRAINTS_PROPERTY,
  };


  /** Flag if dictionary is locked or not */
  private boolean locked;
  
  /** Relation (data set) name */
  private String name;

  /** Attribute contraints instance that identifies the classification to predict */
  private AttributeConstraints classificationConstraints;

  /** Map of all {@link IntegerFeatureAttribute}s */
  private Map<String, IntegerFeatureAttribute> intAttrMap;

  /** Map of all {@link DoubleFeatureAttribute}s */
  private Map<String, RealFeatureAttribute> realAttrMap;

  /** Map of all {@link IntegerFeatureAttribute}s */
  private Map<String, NominalFeatureAttribute> nominalAttrMap;
  
  /** Map of all nominal values */
  private Map<NominalFeatureAttribute, Set<String>> nominalValuesMap;
  
  /** Ordered list of feature attributes (lazily created) */
  private List<FeatureAttribute> _orderedFeatureAttributeList;
  
  /**
   * Constructs a Feature Dictionary, which will be loaded from the given ARFF file.  The ARFF
   * file must contains a comment that specifies which attribute contains the classification.
   * This comment line must appear BEFORE the <code>@ATTRIBUTE</code> section in the ARFF file.
   * This comment line must also be in the following format:
   * <pre>
   * %CLASSIFICATION=attribute_name
   * </pre>
   * or
   * <pre>
   * %CLASSIFICATION=attribute_name|constraintsString
   * </pre>
   * The type of the classification attribute is defined by the "@ATTRIBUTE" arff line later
   * in the file as are nominal values. A constraintsString can define the numeric constraints
   * on the attribute if any. If the classification attribute is numeric without constraints,
   * then its values are unbounded.
   * 
   * @param arffFile ARFF file to load
   */
  public FeatureDictionary(File arffFile) throws IOException {
    init(arffFile);
  }
  
  public FeatureDictionary(File arffFile, FeatureDictionary masterFeatureDictionary) throws IOException {
    init(arffFile, masterFeatureDictionary);
  }
  
  /**
   * Constructs an empty Feature Dictionary with the given name.
   *
   * @param name name of the relation / data-set
   * @param classificationConstraints string for construction of classification attribute
   */
  public FeatureDictionary(String name, String classificationConstraints) {
    init();
    
    this.name = name;
    initClassificationConstraints(new AttributeConstraints(classificationConstraints));
  }

  public FeatureDictionary(Properties properties) throws IOException {
    init();
    
    final String missing = PropertiesParser.getMissingProperties(properties, new String[][] {
      LOCKED_PROPERTY_NAMES,
      UNLOCKED_PROPERTY_NAMES,
    });

    if (missing != null) {
      throw new IllegalArgumentException("Missing required properties: " + missing + "!");
    }

    final String filename = properties.getProperty(INPUT_ARFF_PROPERTY);
    if (filename != null) {
      // creating a locked instance.
      final File arffFile = new File(filename);
      init(arffFile);
    }
    else {
      // creating an unlocked instance.
      this.name = properties.getProperty(DATASET_NAME_PROPERTY);
      initClassificationConstraints(new AttributeConstraints(properties.getProperty(CLASSIFICATION_CONSTRAINTS_PROPERTY)));

      // Initialize values if nominal attribute.
      final Set<String> nominalValues = classificationConstraints.getNominalValues();
      if (nominalValues != null) {
        final String name = classificationConstraints.getAttributeName();
        for (String value : nominalValues) {
          getNominalFeatureAttribute(name, value);
        }
      }
    }
  }

  /**
   * Place for general initializations for all constructors.
   */
  private final void init() {
    this.intAttrMap = new TreeMap<String, IntegerFeatureAttribute>();
    this.realAttrMap = new TreeMap<String, RealFeatureAttribute>();
    this.nominalAttrMap = new TreeMap<String, NominalFeatureAttribute>();
    this.nominalValuesMap = new HashMap<NominalFeatureAttribute, Set<String>>();
    this.locked = false;
  }

  /**
   * Does the actual reading and loading from the ARFF file.
   * 
   * @param arffFile arff file to load
   * @throws IOException
   */
  private final void init(File arffFile) throws IOException {
    init(arffFile, null);
  }
  
  /**
   * Does the actual reading and loading from the ARFF file.
   * 
   * @param arffFile arff file to load
   * @param append   option that this arff is being appended to an already existing {@link FeatureDictionary}
   * @throws IOException
   */
  private final void init(File arffFile, FeatureDictionary masterFeatureDictionary) throws IOException {
    init();
    
    final List<FeatureAttribute> orderedAttributes = new ArrayList<FeatureAttribute>();

    String classificationAttributeName = null;
    AttributeType classificationAttributeType = null;
    String classificationValues = null;

    final BufferedReader reader = FileUtil.getReader(arffFile);
    String line;
    // save having to check each loop by having a separate loop for this
    while ((line = reader.readLine()) != null) {
        if (line.startsWith("@RELATION")) {
          this.name = line.split("\\s+")[1];
          break;
        }
    }
    while ((line = reader.readLine()) != null) {
      // special case comment line to specify the classification attribute name
      if (line.startsWith(CLASSIFICATION_NAME_COMMENT)) {
        final String specString = line.substring(CLASSIFICATION_NAME_COMMENT.length()).trim();
        final String[] specPieces = specString.split("\\s*\\|\\s*");
        classificationAttributeName = specPieces[0];
        if (specPieces.length > 1) classificationValues = specPieces[1];
        
      // load and interpret attributes
      } else if (line.startsWith("@ATTRIBUTE")) {
        FeatureAttribute theFeatureAttribute = null;
        
        if (classificationAttributeName == null) {
          throw new IllegalArgumentException("Classification attribute never set in ARFF file: " + arffFile);
        }

        final String[] parts = line.split("\\s+");
        final String name = parts[1];
        String type = parts[2];
        
        if (name.equals(classificationAttributeName)) {
          if (type.startsWith("{")) {
            classificationAttributeType = AttributeType.NOMINAL;
            classificationValues = type.substring(1, type.length() - 1);  // override regardless of spec
          }
          else if (type.equalsIgnoreCase("integer")) {
            classificationAttributeType = AttributeType.INTEGER;
            if (classificationValues == null) classificationValues = "-";  // set to unbounded if not in spec
          }
          else if (type.equalsIgnoreCase("real") || type.equalsIgnoreCase("numeric")) {
            classificationAttributeType = AttributeType.REAL;
            if (classificationValues == null) classificationValues = "-";  // set to unbounded if not in spec
          }
        }

        if (type.startsWith("{")) {
          type = type.substring(1, type.length()-1);
          final String[] values =  type.split(",");
            
          for (String value: values) {
            final NominalFeatureAttribute attr = getNominalFeatureAttribute(name, value, masterFeatureDictionary);
            theFeatureAttribute = attr;
            if (attr.getBagOfWords() != null) attr.setDefaultValue(0d);
          }
          
          // Handle integer, double, and numeric types
        } else {
          if (type.equalsIgnoreCase("integer")) {
            final IntegerFeatureAttribute attr = getIntegerFeatureAttribute(name, masterFeatureDictionary);
            theFeatureAttribute = attr;
            if (attr.getBagOfWords() != null) attr.setDefaultValue(0d);
            
            // treat generic "numeric" attributes as real
          } else if (type.equalsIgnoreCase("real") || type.equalsIgnoreCase("numeric")) {
            final RealFeatureAttribute attr = getRealFeatureAttribute(name, masterFeatureDictionary);
            theFeatureAttribute = attr;
            if (attr.getBagOfWords() != null) attr.setDefaultValue(0d);
            
          } else {
            throw new IllegalArgumentException("Unsupported attribute type: " + type + " in ARFF file: " + arffFile);
          }
        }
        
        if (theFeatureAttribute != null) {
          orderedAttributes.add(theFeatureAttribute);
        }

      // once @DATA section is reached, header is read in and we can exit
      } else if (line.startsWith("@DATA")) {
        _orderedFeatureAttributeList = orderedAttributes;
        this.locked = true;
        break;
      }
    }
    reader.close();
    
    if (this.name == null) throw new IllegalArgumentException("RELATION Name never set in ARFF file: " + arffFile);
    if (classificationAttributeType == null || classificationValues == null) {
      throw new IllegalArgumentException("ATTRIBUTE " + classificationAttributeName + " never set in ARFF file: " + arffFile);
    }
    else {
      initClassificationConstraints(new AttributeConstraints(classificationAttributeName, classificationAttributeType, classificationValues, false));
    }
  }
  
  private final void initClassificationConstraints(AttributeConstraints attributeConstraints) {
    this.classificationConstraints = attributeConstraints;

    // for consistency, make sure this dictionary knows all of the classification's nominal values.
    // otherwise, only "seen" values will be recorded.
    final Set<String> nominalValues = attributeConstraints.getNominalValues();
    NominalFeatureAttribute nominalFeatureAttribute = null;
    if (nominalValues != null) {
      for (String value : nominalValues) {
        nominalFeatureAttribute = setNominalFeatureAttribute(attributeConstraints.getAttributeName(), value, nominalFeatureAttribute);
      }
    }
  }

  /**
   * Will append the defintion of an arff file to the existing feature
   * dicionary.  This is used when merging arff generated from different
   * data, but belonging to the same data-set.
   * 
   * @param arffFile
   * @throws IOException
   */
  public FeatureDictionary appendArffFile(File arffFile) throws IOException {
    boolean lockCache = locked;
    
    locked = false;
    FeatureDictionary newFeatureDictionary = new FeatureDictionary(arffFile, this);
    locked = lockCache;
    
    return newFeatureDictionary;
  }

  public IntegerFeatureAttribute getIntegerFeatureAttribute(String name) {
    return getIntegerFeatureAttribute(name, null);
  }
  
  private IntegerFeatureAttribute getIntegerFeatureAttribute(String name, FeatureDictionary masterDict) {
    IntegerFeatureAttribute result = masterDict != null ? masterDict.getIntegerFeatureAttribute(name) : intAttrMap.get(name);
    return setIntegerFeatureAttribute(name, result);
  }

  private IntegerFeatureAttribute setIntegerFeatureAttribute(String name, IntegerFeatureAttribute result) {
    if (!locked) {
       if (result == null) {
         result = new IntegerFeatureAttribute(this, name);
       }
       intAttrMap.put(name, result);

      clearFeatureAttributeList();  // clear cached list to recompute.
    }
    
    return result;
  }
  
  public RealFeatureAttribute getRealFeatureAttribute(String name) {
    return getRealFeatureAttribute(name, null);
  }
  
  private RealFeatureAttribute getRealFeatureAttribute(String name, FeatureDictionary masterDict) {
    RealFeatureAttribute result = masterDict != null ? masterDict.getRealFeatureAttribute(name) : realAttrMap.get(name);
    return setRealFeatureAttribute(name, result);
  }

  private RealFeatureAttribute setRealFeatureAttribute(String name, RealFeatureAttribute result) {
    if (!locked) {
      if (result == null) {
        result = new RealFeatureAttribute(this, name);
      }
      realAttrMap.put(name, result);

      clearFeatureAttributeList();  // clear cached list to recompute.
    }
    
    return result;
  }
  
  public NominalFeatureAttribute getNominalFeatureAttribute(String name, String value) {
    return getNominalFeatureAttribute(name, value, null);
  }

  /**
   * Called when appending arffs.  The masterDict acts as a reference for all {@link FeatureAttribute}s.
   * 
   * @param name
   * @param value
   * @param masterDict reference dictionary
   * @return
   */
  private NominalFeatureAttribute getNominalFeatureAttribute(String name, String value, FeatureDictionary masterDict) {
    NominalFeatureAttribute nominalFeatureAttribute = masterDict != null ? masterDict.getNominalFeatureAttribute(name, value) : nominalAttrMap.get(name);
    return setNominalFeatureAttribute(name, value, nominalFeatureAttribute);
  }

  private NominalFeatureAttribute setNominalFeatureAttribute(String name, String value, NominalFeatureAttribute nominalFeatureAttribute) {
    // if not locked, then always add
    if (!locked) {
      if (nominalFeatureAttribute == null) {
        nominalFeatureAttribute = new NominalFeatureAttribute(this, name);
      }
      nominalAttrMap.put(name, nominalFeatureAttribute);
      Set<String> nominalValues = nominalValuesMap.get(nominalFeatureAttribute);
      if (nominalValues == null) {
        nominalValues = new TreeSet<String>();
        nominalValuesMap.put(nominalFeatureAttribute, nominalValues);
      }
      nominalValues.add(value);

      clearFeatureAttributeList();  // clear cached list to recompute.
    }
    
    if (nominalFeatureAttribute == null) return null;
    return nominalValuesMap.get(nominalFeatureAttribute).contains(value) ? nominalFeatureAttribute : null;
  }

  public String[] getNominalFeatureValues(NominalFeatureAttribute nominalFeatureAttribute) {
    Set<String> nominalValues = nominalValuesMap.get(nominalFeatureAttribute);
    String[] retVal = new String[nominalValues.size()];
    return nominalValues.toArray(retVal);
  }
  
  public boolean containsNominalValue(NominalFeatureAttribute nominalFeatureAttribute, String value) {
    Set<String> nominalValues = nominalValuesMap.get(nominalFeatureAttribute);
    return nominalValues.contains(value);
  }

  /**
   * getter for the name of this data-set.
   */
  public String getName() {
    return name;
  }
  
  /**
   * Getter for the classification attribute constraints.
   */
  public final AttributeConstraints getClassificationConstraints() {
    return classificationConstraints;
  }

  /**
   * Getter for the name of the classification attribute.
   */
  public String getClassificationAttributeName() {
    return classificationConstraints.getAttributeName();
  }
  
  /**
   * Get the feature attribute instance for the classification attribute.
   */
  public FeatureAttribute getClassificationAttribute(String value) {
    return classificationConstraints.getIfValid(value, this);
  }

  /**
   * Set the classification attribute to the value on the feature vector.
   *
   * @return true if the value was set on the featureVector; otherwise, false.
   */
  public boolean setClassificationAttribute(String value, FeatureVector featureVector) {
    return classificationConstraints.setIfValid(value, this, featureVector);
  }

  /**
   * Get the index of the value within its nominals.
   *
   * @return the valid index or -1.
   */
  public int getNominalValueIndex(NominalFeatureAttribute nominalFeatureAttribute, String value) {
    int result = -1;

    final Set<String> values = nominalValuesMap.get(nominalFeatureAttribute);
    int index = 0;
    for (String v : values) {
      if (value.equals(v)) {
        result = index;
        break;
      }
      ++index;
    }

    return result;
  }

  /**
   * Test whether this dictionary is locked.
   */
  public boolean isLocked() {
    return locked;
  }
  
  /**
   * This will lock the Feature Dictionary. There is no way to unlock a Feature Diciontary, so only call once
   * all inteneded inputs are read in.
   */
  public void lock() {
    this.locked = true;
  }
  
  /**
   * Unlock this dictionary, allowing for feature attribute modifications.
   * <p>
   * Note that any feature vectors associated with this dictionary may no longer be
   * properly associated with attributes if this dictionary is modified!
   */
  public void unlock() {
    this.locked = false;
  }

  /**
   * Formats into an SD-Enhanced ARFF header string.
   * 
   * @return SD-Ehanced ARFF header
   */
  public String getArffString() {
    StringBuilder buf = new StringBuilder();
    
    buf.append("% SD-Enhanced ARFF File.  Edit with caution as this is an auto-generated ARFF.\n");
    buf.append("@RELATION " + name + "\n");
    buf.append("\n");
    buf.append(CLASSIFICATION_NAME_COMMENT + getClassificationAttributeName() + "\n");

    // make sure these lists are created
    final List<FeatureAttribute> orderedFeatureAttributeList = getFeatureAttributeList();
    
    for (FeatureAttribute fa : orderedFeatureAttributeList) {
      final String name = fa.getName();
      
      buf.append("@ATTRIBUTE " + name + " ");

      if (fa.isNumeric()) {
        if (fa.asNumeric().isInteger()) buf.append("integer\n");
        else if (fa.asNumeric().isReal()) buf.append("real\n");
        else throw new IllegalStateException("Invalid FeatureAttribute: " + fa);
        
      } else if (fa.isNominal()) {
        String[] values = fa.asNominal().getNominalValues();
        buf.append("{");
        for (int j=0; j < values.length; j++) {
          if (j > 0) buf.append(",");
          buf.append(values[j]);
        }
        buf.append("}\n");
        
      } else {
        throw new IllegalStateException("Invalid FeatureAttribute: " + fa);
      }
    }
    buf.append("\n\n@DATA\n");
    
    return buf.toString();
  }

  /**
   * Lazily creates and caches the ordered list of {@link FeatureAttribute}s for use
   * in {@link ExtractedFeatures} and other classes.
   *
   * @return ordered list of {@link FeatureAttribute}s
   */
  public List<FeatureAttribute> getFeatureAttributeList() {
    if (_orderedFeatureAttributeList != null) return _orderedFeatureAttributeList;

    List<FeatureAttribute> result = new ArrayList<FeatureAttribute>();

    // todo: this is a clunky way of getting to the classification FeatureAttribute - refactor
    final String classificationAttributeName = getClassificationAttributeName();
    FeatureAttribute classificationAttribute = null;
    switch (classificationConstraints.getAttributeType()) {
      case INTEGER:
        classificationAttribute = getIntegerFeatureAttribute(classificationAttributeName);
        break;
      case REAL:
        classificationAttribute = getRealFeatureAttribute(classificationAttributeName);
        break;
      case NOMINAL:
        classificationAttribute = nominalAttrMap.get(classificationAttributeName);
//        classificationAttribute = getNominalFeatureAttribute(classificationAttributeName, classificationConstraints.getNominalValues().iterator().next());
        break;
    }
    result.add(classificationAttribute);
    
    // IntegerFeatureAttributes
    final Collection<IntegerFeatureAttribute> intValues = intAttrMap.values();
    for (Iterator<IntegerFeatureAttribute> iter = intValues.iterator(); iter.hasNext(); ) {
      FeatureAttribute curr = iter.next();
      if (curr.equals(classificationAttribute)) continue;
      result.add(curr);
    }
    
    // RealFeatureAttributes
    final Collection<RealFeatureAttribute> realValues = realAttrMap.values();
    for (Iterator<RealFeatureAttribute> iter = realValues.iterator(); iter.hasNext(); ) {
      FeatureAttribute curr = iter.next();
      if (curr.equals(classificationAttribute)) continue;
      result.add(curr);
    }
    
    // NominalFeatureAttributes
    final Collection<NominalFeatureAttribute> nominalValues = nominalAttrMap.values();
    for (Iterator<NominalFeatureAttribute> iter = nominalValues.iterator(); iter.hasNext(); ) {
      FeatureAttribute curr = iter.next();
      if (curr.equals(classificationAttribute)) continue;
      result.add(curr);
    }
    
    _orderedFeatureAttributeList = result;  // cache result.
    
    return result;
  }

  private final void clearFeatureAttributeList() {
	  _orderedFeatureAttributeList = null;
  }

  /**
   * Given a value, get the String reprsentation of this value
   * for the classification label.
   * 
   * @see #toString(FeatureAttribute, Double)
   * @param value
   * @return String representation of the input value
   */
  public String getClassificationLabel(double value) {
    final List<FeatureAttribute> orderedFeatureAttributeList = getFeatureAttributeList();
    return toString(orderedFeatureAttributeList.get(0), value);
  }
  
  public double[] getFeatureVectorAsDouble(FeatureVector featureVector) {
    final List<FeatureAttribute> orderedFeatureAttributeList = getFeatureAttributeList();
    double[] values = new double[orderedFeatureAttributeList.size()];
    
    for (int i=0; i < orderedFeatureAttributeList.size(); i++) {
      Double value = featureVector.getValue(orderedFeatureAttributeList.get(i));
      if (value != null) values[i] = value;
    }
    
    return values;
  }
  
  public boolean removeFeatureAttribute(FeatureAttribute toRemove) {
    if (locked) {
      throw new IllegalStateException("Can't modify a locked dictionary!");
    }

	  final NominalFeatureAttribute nominalFeatureAttribute = toRemove.asNominal();
	  
	  if (nominalFeatureAttribute == null) {
		  final NumericFeatureAttribute numericFeatureAttribute = toRemove.asNumeric();
		  
		  if (numericFeatureAttribute == null) return false;
		  final RealFeatureAttribute realFeatureAttribute = numericFeatureAttribute.asReal();
		  
		  if (realFeatureAttribute == null) {
			  final IntegerFeatureAttribute integerFeatureAttribute = numericFeatureAttribute.asInteger();
			  
			  if (integerFeatureAttribute == null) {
				  return false;
			  } else {
				  intAttrMap.remove(integerFeatureAttribute.name);
			  }
		  } else {
			  realAttrMap.remove(realFeatureAttribute.name);
		  }
	  } else {
		  nominalAttrMap.remove(nominalFeatureAttribute.name);
	  }
	  
	  
	  // reset sticky bit to recompute these guys next time they are called
    clearFeatureAttributeList();
	  
	  return true;
  }
  
  /**
   * Converts from a double value into the string representation of the {@link FeatureAttribute}.
   * 
   * @param featureAttribute 
   * @param value 
   */
  public String toString(FeatureAttribute featureAttribute, Double value) {
    String result = null;

    // get the nominal string value
    final NominalFeatureAttribute nominalFA = featureAttribute.asNominal();

    if (nominalFA != null) {
      String[] values = getNominalFeatureValues(nominalFA);
      result = values[MathUtil.toInt(value)];
    }

    else {
      final NumericFeatureAttribute numericFA = featureAttribute.asNumeric();
      final IntegerFeatureAttribute integerFA = numericFA.asInteger();

      if (integerFA != null) {
        // round double to nearest int
        result = MathUtil.toInt(value) + "";
      }
      else {  // is a double anyway
        result = value.toString();
      }
    }

    return result;
  }

  public String toString() {
    return "FeatureDictionary [" + name + "]";
  }
}
