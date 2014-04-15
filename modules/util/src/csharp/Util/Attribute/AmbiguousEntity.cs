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
  /// Interface for representing ambiguous entities.
  /// 
  /// Templated for the type of entity, T.
  /// 
  /// An entity that is ambiguous reports itself as so and provides
  /// accessors to its alternate (ambiguous) manifestations.
  /// 
  /// Note that ambiguous alternatives should be reported in precedence
  /// order, from most (lowest precedence value) to least (highest precedence
  /// value) likely.
  /// </summary>
  /// <author>Spence Koehler</author>
  public interface AmbiguousEntity<T> 
  {
    /// The entity wrapped by this instance (safe and efficient typecast).
    T Entity { get; }

    /// Determine whether the current instance is ambiguous.
    bool IsAmbiguous { get; }

    /// The ambiguity precedence of this instance, where precedence values
    /// span from low (most likely) to high (least likely).
    int Precedence { get; }

    /// Determine whether there is a next (less likely than this) ambiguous entity.
    bool HasNextAmbiguity { get; }

    /// The number of ambiguities.
    int AmbiguityCount { get; }



    /// Safely and efficiently typecast this to an AmbiguousEntity.
    AmbiguousEntity<T> AsAmbiguousEntity();

    /// The first (most likely) ambiguous entity.
    AmbiguousEntity<T> GetFirstAmbiguity();

    /// The next (less likely than this) ambiguous entity, or null if there
    /// are no others.
    AmbiguousEntity<T> GetNextAmbiguity();

    /// Add another ambiguous entity to the end of the chain.
    void AddAmbiguity(AmbiguousEntity<T> leastLikelyEntity);

    /// Insert a less-likley ambiguous entity immediately after this.
    void InsertAmbiguity(AmbiguousEntity<T> lessLikelyEntity);

    /// Rule out this as a valid entity in its ambiguity chain.
    void Discard();

    /// Resolve this instance as an unambiguous entity.
    void Resolve();

    /// Remove this instance from its ambiguity chain.
    void Remove();

    /// Determine whether this ambiguous entity matches (is a duplicate of) the other
    bool Matches(AmbiguousEntity<T> other);
  }
}
