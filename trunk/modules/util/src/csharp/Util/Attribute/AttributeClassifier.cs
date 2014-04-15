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
namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Interface for classifying free text as canonized attribute.
  /// 
  /// Templated for the canonical attribute enumeration, E.
  /// </summary>
  /// <author>Spence Koehler</author>
  public interface AttributeClassifier<E>
  {
    /// Classify the string as a (potentially ambiguous) canonical attribute, if
    /// possible, or null.
    Attribute<E> GetAttribute(string type);
  }
}
