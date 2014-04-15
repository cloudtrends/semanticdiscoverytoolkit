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
using System.Collections.Generic;

namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Simple classifier for package testing.
  /// </summary>
  /// <author>Spence Koehler</author>
  public class MyTestClassifier 
    : BaseAttributeClassifier<MyTestEnum> 
  {
    private static readonly string[] MAKES = new string[] { };
    private static readonly string[] MODELS = new string[] { "type", };
    private static readonly string[] STYLES = new string[] { "type", };
    private static readonly string[] YEARS = new string[] { };

    private ISet<string> makes;
    private ISet<string> models;
    private ISet<string> styles;
    private ISet<string> years;

    public MyTestClassifier() : base(1000) {
      this.makes = load(MAKES);
      this.models = load(MODELS);
      this.styles = load(STYLES);
      this.years = load(YEARS);
    }

    private ISet<string> load(string[] terms) {
      ISet<string> result = new HashSet<string>();

      if (terms != null) {
        foreach(string term in terms) {
          if (!string.IsNullOrEmpty(term)) {
            result.Add(term.ToLower());
          }
        }
      }

      return result;
    }

    protected override MyTestEnum GetValueOf(string upperCaseType) {
      return MyTestEnum.ValueOf(upperCaseType.ToUpper());
    }

    protected override Attribute<MyTestEnum> ClassifyOtherAttribute(string type) {
      Attribute<MyTestEnum> result = null;

      type = type.ToLower();

      if (makes.Contains(type)) result = AddAtt(MyTestEnum.MAKE, result);
      if (models.Contains(type)) result = AddAtt(MyTestEnum.MODEL, result);
      if (styles.Contains(type)) result = AddAtt(MyTestEnum.STYLE, result);
      if (years.Contains(type)) result = AddAtt(MyTestEnum.YEAR, result);

      return result;
    }
  }
}
