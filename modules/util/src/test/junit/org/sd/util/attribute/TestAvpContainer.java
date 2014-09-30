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
package org.sd.util.attribute;


import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the AvpContainer class.
 * <p>
 * @author Spence Koehler
 */
public class TestAvpContainer extends TestCase {

  public TestAvpContainer(String name) {
    super(name);
  }
  
  // NOTES:
  // - Setup for this test is definition of the classes:
  //   - MyTestEnum and MyTestClassifier
  //   - FamEnum and FamClassifier

  public void testClassifyStrongThenWeakType() {
    final MyTestClassifier classifier = new MyTestClassifier();
    final AvpContainer<MyTestEnum, String, Object> avpContainer = new AvpContainer<MyTestEnum, String, Object>(classifier);

    AttValPair<MyTestEnum, String, Object> avp = 
      new AttValPair<MyTestEnum, String, Object>(MyTestEnum.MAKE, "foo");
    avpContainer.add(avp);
    String value = avpContainer.get("make").getValue();

    assertEquals("foo", value);
  }

  public void testMyClassifier() {
    // test no attribute
    final MyTestClassifier classifier = new MyTestClassifier();
    final Attribute<MyTestEnum> nothing = classifier.getAttribute("nothing");
    assertNull(nothing);

    // test non-canonical attribute
    final Attribute<MyTestEnum> other = classifier.getAttribute("other");
    assertFalse(other.getAttType().isCanonical());
    assertEquals(MyTestEnum.OTHER, other.getAttType());

    // test ambiguous attribute
    final Attribute<MyTestEnum> type = classifier.getAttribute("type");
    assertNotNull(type);
    assertTrue(type.isAmbiguous());

    assertTrue(type.getAttType().isCanonical());
    assertEquals(MyTestEnum.MODEL, type.getAttType());
    assertTrue(type.nextAmbiguity().isAmbiguous());
    assertEquals(type, type.nextAmbiguity().firstAmbiguity());
    assertEquals(MyTestEnum.STYLE, type.nextAmbiguity().getAttType());
    assertNull(type.nextAmbiguity().nextAmbiguity());

    // test unambiguous attribute
    final Attribute<MyTestEnum> make = classifier.getAttribute("make");
    assertNotNull(make);
    assertTrue(make.getAttType().isCanonical());
    assertEquals(MyTestEnum.MAKE, make.getAttType());
    assertFalse(make.isAmbiguous());
    assertNull(make.nextAmbiguity());
  }

  public void testAvpContainerWithClassifier1() {
    final MyTestClassifier classifier = new MyTestClassifier();
    final AvpContainer<MyTestEnum, String, Object> avpContainer = new AvpContainer<MyTestEnum, String, Object>(classifier);
    assertTrue(avpContainer.isEmpty());
    assertEquals(0, avpContainer.size());
    assertNull(avpContainer.get((MyTestEnum)null));
    assertNull(avpContainer.get((String)null));
    assertNull(avpContainer.get(MyTestEnum.MAKE));
    assertNull(avpContainer.get("make"));

    final String[][] record1 = new String[][] {
      {"make", "hyundai"},
      {"model", "sonata"},
      {"style", "sedan"},
      {"year", "2010"},
    };
    
    doRecordTest(avpContainer, record1, new MyTestEnum[]{MyTestEnum.MAKE, MyTestEnum.MODEL, MyTestEnum.STYLE, MyTestEnum.YEAR});

    assertFalse(avpContainer.hasAmbiguity());
  }

  public void testMultipleAmbiguities() {
    final MyTestClassifier classifier = new MyTestClassifier();
    final AvpContainer<MyTestEnum, String, Object> avpContainer = new AvpContainer<MyTestEnum, String, Object>(classifier);

    final String[][] record1 = new String[][] {
      {"make", "hyundai"},
      {"model", "sonata"},
      {"style", "sedan"},
      {"year", "2010"},
    };
    addRecord(avpContainer, record1);
    
    final String[][] record2 = new String[][] {
      {"make", "hyundai"},
      {"model", "sonata"},
      {"style", "sedan"},
      {"year", "2011"},
    };
    addRecord(avpContainer, record2);
    
    final String[][] record3 = new String[][] {
      {"make", "hyundai"},
      {"model", "sonata"},
      {"style", "sedan"},
      {"year", "2012"},
    };
    addRecord(avpContainer, record3);
    
    assertTrue(avpContainer.hasAmbiguity());
    final List<AttValPair<MyTestEnum, String, Object>> ambiguities = avpContainer.getAmbiguities();
    assertEquals(1, ambiguities.size());
    final AttValPair<MyTestEnum, String, Object> avp = ambiguities.get(0);
    assertTrue(avp.isAmbiguous());
    assertEquals(MyTestEnum.YEAR, avp.getAttType());
    assertEquals(3, avp.getAmbiguityCount());
  }

  public void testResolveAmbiguity1() {
    final MyTestClassifier classifier = new MyTestClassifier();
    final AvpContainer<MyTestEnum, String, Object> avpContainer = new AvpContainer<MyTestEnum, String, Object>(classifier);

    final String[][] record1 = new String[][] {
      {"make", "hyundai"},
      {"type", "sonata"},
      {"style", "sedan"},
      {"year", "2010"},
    };
    
    //Note that access ignoring ambiguity gets a reasonable record
    doRecordTest(avpContainer, record1, new MyTestEnum[]{MyTestEnum.MAKE, MyTestEnum.MODEL, MyTestEnum.STYLE, MyTestEnum.YEAR});

    assertTrue(avpContainer.hasAmbiguity());

    // test disambiguate by resolve
    final List<AttValPair<MyTestEnum, String, Object>> ambiguities = avpContainer.getAmbiguities();
    AttValPair<MyTestEnum, String, Object> style = ambiguities.get(0);
    assertEquals(MyTestEnum.STYLE, style.getAttType());  // style is ambiguous
    
    boolean resolved = false;
    while (style != null) {
      if ("sedan".equals(style.getValue())) {
        style.resolve();
        resolved = true;
        break;
      }
      style = style.nextAmbiguity();
    }

    assertTrue(resolved);
    assertFalse(avpContainer.hasAmbiguity());
    assertEquals(MyTestEnum.STYLE, avpContainer.get(MyTestEnum.STYLE).getAttType());
  }

  public void testDiscardAmbiguity1() {
    final MyTestClassifier classifier = new MyTestClassifier();
    final AvpContainer<MyTestEnum, String, Object> avpContainer = new AvpContainer<MyTestEnum, String, Object>(classifier);

    final String[][] record1 = new String[][] {
      {"make", "hyundai"},
      {"model", "sonata"},
      {"type", "sedan"},
      {"year", "2010"},
    };
    
    //Note that access ignoring ambiguity gets a reasonable record
    doRecordTest(avpContainer, record1, new MyTestEnum[]{MyTestEnum.MAKE, MyTestEnum.MODEL, MyTestEnum.STYLE, MyTestEnum.YEAR});

    assertTrue(avpContainer.hasAmbiguity());

    // test disambiguate by discard
    final List<AttValPair<MyTestEnum, String, Object>> ambiguities = avpContainer.getAmbiguities();
    AttValPair<MyTestEnum, String, Object> model = ambiguities.get(0);
    assertEquals(MyTestEnum.MODEL, model.getAttType());  // model is ambiguous
    
    boolean resolved = false;
    while (model != null) {
      if (!"sonata".equals(model.getValue())) {
        model.discard();
        resolved = true;
        break;
      }
      model = model.nextAmbiguity();
    }

    assertTrue(resolved);
    assertFalse(avpContainer.hasAmbiguity());
  }

  private final void doRecordTest(AvpContainer<MyTestEnum, String, Object> avpContainer, String[][] record,
                                  MyTestEnum[] expectedEnums) {
    addRecord(avpContainer, record);

    // test weak access
    doWeakAccessTest(avpContainer, record);

    // test strong access
    doStrongAccessTest(avpContainer, record, expectedEnums);
  }

  private final <T extends Canonical> void addRecord(AvpContainer<T, String, Object> avpContainer, String[][] record) {
    for (String[] attVal : record) {
      avpContainer.add(attVal[0], attVal[1]);
    }
  }

  private final <T extends Canonical> void doWeakAccessTest(AvpContainer<T, String, Object> avpContainer, String[][] record) {
    // test weak access
    for (String[] attVal : record) {
      final AttValPair<T, String, Object> avp = avpContainer.get(attVal[0]);
      assertNotNull(attVal[1], avp);
      assertEquals(attVal[1], avp.getValue());
      assertEquals(attVal[1], avpContainer, avp.getContainer());
    }
  }

  private final <T extends Canonical> void doStrongAccessTest(AvpContainer<T, String, Object> avpContainer, String[][] record,
                                                              T[] expectedEnums) {
    int i = 0;
    for (T e : expectedEnums) {
      final AttValPair<T, String, Object> avp = avpContainer.get(e);
      assertNotNull(e.toString(), avp);
      assertEquals(e.toString(), record[i++][1], avp.getValue());
      assertEquals(e.toString(), avpContainer, avp.getContainer());
    }
  }


  public void testAmbiguousStrongType1() {
    // FamEnum.CHILD <-- "child", "children"
    // add("child")
    // ensure get("child") exists
    // ensure get(FamEnum.CHILD) exists
    // ensure get("children") exists
    // delete("child")
    // ensure get("child") is empty
    // ensure get(FamEnum.CHILD) is empty
    // ensure get("children") is empty

    final FamClassifier classifier = new FamClassifier();
    final AvpContainer<FamEnum, String, Object> avpContainer = new AvpContainer<FamEnum, String, Object>(classifier);
    assertTrue(avpContainer.isEmpty());
    assertEquals(0, avpContainer.size());
    assertNull(avpContainer.get((FamEnum)null));
    assertNull(avpContainer.get((String)null));
    assertNull(avpContainer.get(FamEnum.CHILD));

    final String[][] record1 = new String[][] {
      {"father", "tommy sr"},
      {"child", "tommy jr"},
    };
    final String[][] record2 = new String[][] {
      {"father", "tommy sr"},
      {"children", "tommy jr"},
    };
    final FamEnum[] expectedEnums = new FamEnum[] {
      FamEnum.FATHER,
      FamEnum.CHILD,
    };

    addRecord(avpContainer, record1);

    doStrongAccessTest(avpContainer, record1, expectedEnums);  // get(FamEnum.CHILD)
    doWeakAccessTest(avpContainer, record1);                   // get("child")
    doWeakAccessTest(avpContainer, record2);                   // get("children")
    
    final AttValPair<FamEnum, String, Object> childAvp = avpContainer.get("child");
    assertNotNull(childAvp);
    childAvp.discard();  // NOTE: leads to avpContainer.remove(childAvp)

    // strongs and classifieds for CHILD should all be gone now
    assertEquals(1, avpContainer.size());
    assertNull(avpContainer.get(FamEnum.CHILD));

    assertNull(avpContainer.get("child"));
    assertNull(avpContainer.get("children"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestAvpContainer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
