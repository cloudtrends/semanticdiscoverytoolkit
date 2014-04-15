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

namespace SemanticDiscovery.Util.Attribute 
{
  /// <summary>
  /// Simple enumeration for package testing -- attributes describing a car.
  /// </summary>
  /// <author>Spence Koehler</author>
  public class MyTestEnum : Canonical 
  {
    public static readonly MyTestEnum MAKE = new MyTestEnum(true);
    public static readonly MyTestEnum MODEL = new MyTestEnum(true);
    public static readonly MyTestEnum STYLE = new MyTestEnum(true);
    public static readonly MyTestEnum YEAR = new MyTestEnum(true);
    public static readonly MyTestEnum OTHER = new MyTestEnum(false);

    private readonly bool m_canonical;

    public bool IsCanonical { get { return m_canonical; } }

    private MyTestEnum(bool canonical) {
      this.m_canonical = canonical;
    }

    public override string ToString() 
    {
      if(this == MAKE) return "MAKE";
      if(this == MODEL) return "MODEL";
      if(this == STYLE) return "STYLE";
      if(this == YEAR) return "YEAR";
      if(this == OTHER) return "OTHER";
      throw new ArgumentException("unexpected value");
    }

    public static MyTestEnum ValueOf(string type)
    {
      switch(type)
      {
      case "MAKE":
        return MAKE;
      case "MODEL":
        return MODEL;
      case "STYLE":
        return STYLE;
      case "YEAR":
        return YEAR;
      case "OTHER":
        return OTHER;
      default:
        throw new ArgumentException("unrecognized type: "+type);
      }
    }

  }
}
