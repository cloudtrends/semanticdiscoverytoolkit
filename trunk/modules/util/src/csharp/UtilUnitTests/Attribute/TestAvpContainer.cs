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
using System;
using System.Linq;
using System.Collections;
using System.Collections.Generic;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace  SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Unit tests for the AvpContainer class.
  /// </summary>
  /// <author>Spence Koehler</author>
  [TestClass]
  public class TestAvpContainer
  {
    // NOTES:
    // - Setup for this test is definition of the MyTestEnum and MyTestClassifier classes.

    [TestMethod]
    public void TestMyClassifier() 
    {
      // test no attribute
      MyTestClassifier classifier = new MyTestClassifier();
      Attribute<MyTestEnum> nothing = classifier.GetAttribute("nothing");
      Assert.IsNull(nothing);

      // test non-canonical attribute
      Attribute<MyTestEnum> other = classifier.GetAttribute("other");
      Assert.IsFalse(other.AttType.IsCanonical);
      Assert.AreEqual(MyTestEnum.OTHER, other.AttType);

      // test ambiguous attribute
      Attribute<MyTestEnum> type = classifier.GetAttribute("type");
      Assert.IsNotNull(type);
      Assert.IsTrue(type.IsAmbiguous);

      Assert.IsTrue(type.AttType.IsCanonical);
      Assert.AreEqual(MyTestEnum.MODEL, type.AttType);
      Assert.IsTrue(type.NextAmbiguity().IsAmbiguous);
      Assert.AreEqual(type, type.NextAmbiguity().FirstAmbiguity());
      Assert.AreEqual(MyTestEnum.STYLE, type.NextAmbiguity().AttType);
      Assert.IsNull(type.NextAmbiguity().NextAmbiguity());

      // test unambiguous attribute
      Attribute<MyTestEnum> make = classifier.GetAttribute("make");
      Assert.IsNotNull(make);
      Assert.IsTrue(make.AttType.IsCanonical);
      Assert.AreEqual(MyTestEnum.MAKE, make.AttType);
      Assert.IsFalse(make.IsAmbiguous);
      Assert.IsNull(make.NextAmbiguity());
    }

    [TestMethod]
    public void TestAvpContainerWithClassifier1() {
      MyTestClassifier classifier = new MyTestClassifier();
      AvpContainer<MyTestEnum, string, Object> avpContainer = 
        new AvpContainer<MyTestEnum, string, Object>(classifier);
      Assert.IsTrue(avpContainer.IsEmpty);
      Assert.AreEqual(0, avpContainer.Count);
      Assert.IsNull(avpContainer.Get((MyTestEnum)null));
      Assert.IsNull(avpContainer.Get((string)null));
      Assert.IsNull(avpContainer.Get(MyTestEnum.MAKE));
      Assert.IsNull(avpContainer.Get("make"));

      string[][] record1 = new string[][] {
        new string[] {"make", "hyundai"},
        new string[] {"model", "sonata"},
        new string[] {"style", "sedan"},
        new string[] {"year", "2010"},
      };
    
      doRecordTest(avpContainer, record1, 
                   new MyTestEnum[]{ MyTestEnum.MAKE, 
                                     MyTestEnum.MODEL, 
                                     MyTestEnum.STYLE, 
                                     MyTestEnum.YEAR });

      Assert.IsFalse(avpContainer.HasAmbiguity);
    }

    [TestMethod]
    public void TestMultipleAmbiguities() {
      MyTestClassifier classifier = new MyTestClassifier();
      AvpContainer<MyTestEnum, string, Object> avpContainer = 
        new AvpContainer<MyTestEnum, string, Object>(classifier);

      string[][] record1 = new string[][] {
        new string[] {"make", "hyundai"},
        new string[] {"model", "sonata"},
        new string[] {"style", "sedan"},
        new string[] {"year", "2010"},
      };
      addRecord(avpContainer, record1);
    
      string[][] record2 = new string[][] {
        new string[] {"make", "hyundai"},
        new string[] {"model", "sonata"},
        new string[] {"style", "sedan"},
        new string[] {"year", "2011"},
      };
      addRecord(avpContainer, record2);
    
      string[][] record3 = new string[][] {
        new string[] {"make", "hyundai"},
        new string[] {"model", "sonata"},
        new string[] {"style", "sedan"},
        new string[] {"year", "2012"},
      };
      addRecord(avpContainer, record3);
    
      Assert.IsTrue(avpContainer.HasAmbiguity);
      IList<AttValPair<MyTestEnum, string, Object>> ambiguities = avpContainer.Ambiguities;
      Assert.AreEqual(1, ambiguities.Count);
      AttValPair<MyTestEnum, string, Object> avp = ambiguities.ElementAt(0);
      Assert.IsTrue(avp.IsAmbiguous);
      Assert.AreEqual(MyTestEnum.YEAR, avp.AttType);
      Assert.AreEqual(3, avp.AmbiguityCount);
    }

    [TestMethod]
    public void TestResolveAmbiguity1() {
      MyTestClassifier classifier = new MyTestClassifier();
      AvpContainer<MyTestEnum, string, Object> avpContainer = 
        new AvpContainer<MyTestEnum, string, Object>(classifier);

      string[][] record1 = new string[][] {
        new string[] {"make", "hyundai"},
        new string[] {"type", "sonata"},
        new string[] {"style", "sedan"},
        new string[] {"year", "2010"},
      };
    
      //Note that access ignoring ambiguity gets a reasonable record
      doRecordTest(avpContainer, record1, 
                   new MyTestEnum[]{ MyTestEnum.MAKE, 
                                     MyTestEnum.MODEL, 
                                     MyTestEnum.STYLE, 
                                     MyTestEnum.YEAR });

      Assert.IsTrue(avpContainer.HasAmbiguity);

      // test disambiguate by resolve
      IList<AttValPair<MyTestEnum, string, Object>> ambiguities = avpContainer.Ambiguities;
      AttValPair<MyTestEnum, string, Object> style = ambiguities.ElementAt(0);
      Assert.AreEqual(MyTestEnum.STYLE, style.AttType);  // style is ambiguous
    
      bool resolved = false;
      while (style != null) {
        if ("sedan".Equals(style.Value)) {
          style.Resolve();
          resolved = true;
          break;
        }
        style = style.NextAmbiguity();
      }

      Assert.IsTrue(resolved);
      Assert.IsFalse(avpContainer.HasAmbiguity);
      Assert.AreEqual(MyTestEnum.STYLE, avpContainer.Get(MyTestEnum.STYLE).AttType);
    }

    [TestMethod]
    public void TestDiscardAmbiguity1() {
      MyTestClassifier classifier = new MyTestClassifier();
      AvpContainer<MyTestEnum, string, Object> avpContainer = 
        new AvpContainer<MyTestEnum, string, Object>(classifier);

      string[][] record1 = new string[][] {
        new string[] {"make", "hyundai"},
        new string[] {"model", "sonata"},
        new string[] {"type", "sedan"},
        new string[] {"year", "2010"},
      };
    
      //Note that access ignoring ambiguity gets a reasonable record
      doRecordTest(avpContainer, record1, 
                   new MyTestEnum[]{ MyTestEnum.MAKE, 
                                     MyTestEnum.MODEL, 
                                     MyTestEnum.STYLE, 
                                     MyTestEnum.YEAR });

      Assert.IsTrue(avpContainer.HasAmbiguity);

      // test disambiguate by discard
      IList<AttValPair<MyTestEnum, string, Object>> ambiguities = avpContainer.Ambiguities;
      AttValPair<MyTestEnum, string, Object> model = ambiguities.ElementAt(0);
      Assert.AreEqual(MyTestEnum.MODEL, model.AttType);  // model is ambiguous
    
      bool resolved = false;
      while (model != null) {
        if (!"sonata".Equals(model.Value)) {
          model.Discard();
          resolved = true;
          break;
        }
        model = model.NextAmbiguity();
      }

      Assert.IsTrue(resolved);
      Assert.IsFalse(avpContainer.HasAmbiguity);
    }

    private void doRecordTest(AvpContainer<MyTestEnum, string, Object> avpContainer, 
                              string[][] record,
                              MyTestEnum[] expectedEnums) 
    {
      addRecord(avpContainer, record);

      // test weak access
      foreach(string[] attVal in record) 
      {
        Assert.AreEqual(attVal[1], 
                        avpContainer.Get(attVal[0]).Value);
      }

      // test strong access
      int i = 0;
      foreach(MyTestEnum e in expectedEnums) 
      {
        Assert.AreEqual(record[i++][1], 
                        avpContainer.Get(e).Value,
                        e.ToString());
      }
    }

    private void addRecord(AvpContainer<MyTestEnum, string, Object> avpContainer, 
                           string[][] record) 
    {
      foreach(string[] attVal in record) {
        avpContainer.Add(attVal[0], attVal[1]);
      }
    }
  }
}
