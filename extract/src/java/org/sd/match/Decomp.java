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
package org.sd.match;


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class for the decomposition type enumeration.
 * <p>
 * @author Spence Koehler
 */
public class Decomp {

  /**
   * An enumeration of decomposition types, one of which will apply for each
   * concept term.
   */
  public enum Type {

    MISCELLANEOUS(null, null, "Miscellaneous", false, 1, true),

    BASE("Base(Vocabulary Term)", "Base(Term ID)", "Base", false, 8, true),
    ADD_TYPE1("Add_Type1(Vocabulary Term)", "Add_Type1(Term ID)", "Add_Type1", false, 2, false),
    ADD_TYPE2("Add_Type2(Vocabulary Term)", "Add_Type2(Term ID)", "Add_Type2", false, 2, false),
    ADD_TYPE3("Add_Type3(Vocabulary Term)", "Add_Type3(Term ID)", "Add_Type3", false, 2, false),
    ADD_TYPE4("Add_Type4(Vocabulary Term)", "Add_Type4(Term ID)", "Add_Type4", false, 2, false),
    MATERIAL("Material(Vocabulary Term)", "Material(Term ID)", "Material", false, 2, false),
    STANDARD("Standard(Vocabulary Term)", "Standard(Term ID)", "Standard", false, 2, false),
    FORM("Form(Vocabulary Term)", "Form(Term ID)", "Form", false, 2, false),
    APP_TO_PRODUCT("AppToProduct(Vocabulary Term)", "AppToProduct(Term ID)", "AppToProduct", false, 2, false),
    APP_TO_INDUSTRY("AppToIndustry(Vocabulary Term)", "AppToIndustry(Term ID)", "AppToIndustry", false, 2, false),
    APP_TO_ACTIVITY("AppToActivity(Vocabulary Term)", "AppToActivity(Term ID)", "AppToActivity", false, 2, false),
    APP_TO_LOCATION("AppToLocation(Vocabulary Term)", "AppToLocation(Term ID)", "AppToLocation", false, 2, false),
    ADD_FEATURE1("AddFeature1(Vocabulary Term)", "AddFeature1(Term ID)", "AddFeature1", false, 2, false),
    ADD_FEATURE2("AddFeature2(Vocabulary Term)", "AddFeature2(Term ID)", "AddFeature2", false, 2, false),
    GROUP_NAME("Group_Name(Vocabulary Term)", "Group_Name(Term ID)", "Group_Name", true, 1, false),

    //
    // NOTE: Always add more types before this. (Used for testing/integrity purposes.)
    //
    //   *** REMEMBER to always add new types to the set below!
    //
    _FINAL_(null, null, null, false, 0, false);

    private String fieldName;   // field name for vocabulary term in the master ontology file
    private String fieldId;     // field name for term ID in the master ontology file
    private String text;
    private boolean optional;  // flag for whether text from this term must match input
    private int weightMultiplier;
    private boolean isPrimary;  // true if this is a primary field that must match
    
    Type(String fieldName, String fieldId, String text, boolean optional, int weightMultiplier, boolean isPrimary) {
      this.fieldName = fieldName;
      this.fieldId = fieldId;
      this.text = text;
      this.optional = optional;
      this.weightMultiplier = weightMultiplier;
      this.isPrimary = isPrimary;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String getFieldId() {
      return fieldId;
    }

    public String getText() {
      return text;
    }

    public int getWeightMultiplier() {
      return weightMultiplier;
    }

    public boolean isPrimary() {
      return isPrimary;
    }
  };

  private static final Set<Type> types = new LinkedHashSet<Type>();
  private static final Map<Integer, Type> ordinal2type = new LinkedHashMap<Integer, Type>();
  private static final Map<String, Type> name2type = new LinkedHashMap<String, Type>();
  private static final Map<String, Type> fieldName2type = new LinkedHashMap<String, Type>();
  private static final Map<String, Type> text2type = new LinkedHashMap<String, Type>();

  static {
    types.add(Type.MISCELLANEOUS);

    types.add(Type.BASE);
    types.add(Type.ADD_TYPE1);
    types.add(Type.ADD_TYPE2);
    types.add(Type.ADD_TYPE3);
    types.add(Type.ADD_TYPE4);
    types.add(Type.MATERIAL);
    types.add(Type.STANDARD);
    types.add(Type.FORM);
    types.add(Type.APP_TO_PRODUCT);
    types.add(Type.APP_TO_INDUSTRY);
    types.add(Type.APP_TO_ACTIVITY);
    types.add(Type.APP_TO_LOCATION);
    types.add(Type.ADD_FEATURE1);
    types.add(Type.ADD_FEATURE2);
    types.add(Type.GROUP_NAME);

    for (Type type : types) {
      ordinal2type.put(type.ordinal(), type);
      name2type.put(type.name(), type);
      fieldName2type.put(type.getFieldName(), type);
      text2type.put(type.getText(), type);
    }
  }

  public static final Set<Type> getTypes() {
    return types;
  }

  public static final Type getType(int ordinal) {
    return ordinal2type.get(ordinal);
  }

  public static final Type getType(String name) {
    return name2type.get(name);
  }

  public static final Type getTypeByFieldName(String fieldName) {
    return fieldName2type.get(fieldName);
  }

  public static final Type getTypeByText(String text) {
    return text2type.get(text);
  }
}
