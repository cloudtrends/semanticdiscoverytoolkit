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
  /// Container for potentially ambiguous canonical attributes as generated
  /// through an AttributeClassifier.
  /// 
  /// Templated for the canonical attribute enumeration, E.
  /// </summary>
  /// <author>Spence Koehler</author>
  public class Attribute<E> : AbstractAmbiguousEntity<Attribute<E>>
  {
    /// The attribute type wrapped by this instance.
    public E AttType { get; private set; }

    #region "Implement AmbiguousEntity interface(properties)"
    /// Safely and efficiently typecast this to an Attribute.
    public override Attribute<E> Entity { get { return this; } }

    /// Simple typecasting helper auxiliary for getting the first ambiguity.
    public Attribute<E> FirstAmbiguity() { 
      return (Attribute<E>)GetFirstAmbiguity();
    }

    /// Simple typecasting helper auxiliary for getting the next ambiguity.
    public Attribute<E> NextAmbiguity() { 
      return (Attribute<E>)GetNextAmbiguity();
    }
    #endregion

    public Attribute(E attType) {
      this.AttType = attType;
    }

    #region "Implement AmbiguousEntity interface"
    /// Determine whether this ambiguous entity matches (is a duplicate of) the other
    public override bool Matches(AmbiguousEntity<Attribute<E>> other) {
      bool result = (this == other);
      if (!result && other != null) {
        result = object.ReferenceEquals(this.AttType, other.Entity.AttType);
      }
      return result;
    }
    #endregion
  }
}
