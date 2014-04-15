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
using System.Text;
using System.Collections.Generic;

namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Container for associating an attribute with a value (or values).
  /// 
  /// Templated for the canonical attribute enumeration, E, and the value object, V.
  /// 
  /// Note that Un-canonized (or free) attributes use the "otherType" to express
  /// the attribute.
  /// </summary>
  /// <author>Spence Koehler</author>
  public class AttValPair<E,V,M> 
    : AbstractAmbiguousEntity<AttValPair<E,V,M>>, Canonical, MultipleValues<V> 
    where E : Canonical
  {
    // AttValPair fields
    private MultipleValuesContainer<V> values;
    private AvpContainer<E, V, M> container;

    /// The (canonical) attribute type.
    public E AttType { get; set; }

    /// The meta-data associated with this instance.
    // todo: in callers, find where old metadata value returned
    public M MetaData { get; set; }

    /// Determine whether this instance holds meta-data.
    public bool HasMetaData { get { return MetaData != null; } }

    /// Get the other (non-canonical) type.
    public string OtherType { get; private set; }

    /// Determine whether this instance has a non-empty other type.
    public bool HasOtherType {
      get { return !string.IsNullOrEmpty(OtherType); }
    }

    #region "Implement AmbiguousEntity interface(properties)"
    /// Safely and efficiently typecast this to an AttValPair.
    public override AttValPair<E,V,M> Entity { get { return this; } }

    /// Simple typecasting helper auxiliary for getting the first ambiguity.
    public AttValPair<E, V, M> FirstAmbiguity() {
      return (AttValPair<E, V, M>)GetFirstAmbiguity();
    }

    /// Simple typecasting helper auxiliary for getting the next ambiguity.
    public AttValPair<E, V, M> NextAmbiguity() {
      return (AttValPair<E, V, M>)GetNextAmbiguity();
    }
    #endregion

    #region "Implement MultipleValues interface(properties)"
    /// Get the value.
    public V Value {
      get {
        return values == null ? default(V) : values.Value;
      }
      set {
        if (this.values == null) {
          this.values = new MultipleValuesContainer<V>(value);
        }
        else {
          this.values.Value = value;
        }
      }
    }

    /// Determine whether the current instance has multiple values.
    public bool HasMultipleValues {
      get {
        return values != null && values.HasMultipleValues;
      }
    }

    /// Get all of this instance's values.
    public List<V> Values {
      get {
        return values == null ? null : values.Values;
      }
    }

    /// Get the number of values.
    public int ValuesCount {
      get {
        return values == null ? 0 : values.ValuesCount;
      }
    }
    #endregion

    #region "Implement Canonical interface(properties)"
    /// Determine whether this holds a canonical attribute.
    public bool IsCanonical {
      get { return (AttType == null) ? false : AttType.IsCanonical; }
    }
    #endregion


    /// Create with a canonical attribute.
    public AttValPair(E attType, V value) {
      init();
      this.AttType = attType;
      this.OtherType = (attType == null) ? null : attType.ToString();
      this.values = new MultipleValuesContainer<V>(value);
      this.container = null;
    }

    /// Create with a free (non-canonical) attribute.
    public AttValPair(string otherType, V value) {
      init();
      this.AttType = default(E);
      this.OtherType = otherType;
      this.values = new MultipleValuesContainer<V>(value);
      this.container = null;
    }

    /// Create with given field values.
    public AttValPair(E attType, string otherType, V value) {
      init();
      this.AttType = attType;
      this.OtherType = otherType;
      this.values = new MultipleValuesContainer<V>(value);
      this.container = null;
    }

    /// Copy constructor (shallow).
    /// 
    /// Note that this creates a copy of just the attribute and not its multi-
    /// value or  ambiguity chains.
    public AttValPair(AttValPair<E,V,M> other) {
      init();
      this.AttType = other.AttType;
      this.OtherType = other.OtherType;
      this.MetaData = other.MetaData;
      this.values = new MultipleValuesContainer<V>(other.values);
      this.container = other.container;
    }

    /// Set this instance's container. Maintained thru AvpContainer.
    internal void SetContainer(AvpContainer<E,V,M> container) {
      this.container = container;
    }

    internal void ClearAllContainers() {
      for (AttValPair<E,V,M> avp = this; avp != null; avp = avp.NextAmbiguity()) {
        avp.container = null;
      }
    }

    public string ToString() {
      StringBuilder result = new StringBuilder();

      if (AttType != null) {
        result.Append(AttType);
      }
      else if (!string.IsNullOrEmpty(OtherType)) {
        result.Append('%').Append(OtherType);
      }
      else {
        result.Append('?');
      }

      if (values != null) {
        result.Append('=').Append(values.ToString());
      }

      if (MetaData != null) {
        // add a marker indicating the presence of metadata
        result.Append('+');
      }

      AttValPair<E,V,M> nextAmbiguity = NextAmbiguity();
      if (nextAmbiguity != null) {
        result.Append('|').Append(nextAmbiguity.ToString());
      }

      return result.ToString();
    }

    /// Initialize all instance fields
    private void init() {
      this.AttType = default(E);
      this.OtherType = null;
      this.MetaData = default(M);
      this.values = null;
    }

    /// Create a copy of this instance with its ambiguity chain, adding as
    /// additional ambiguities to result if result is non-null.
    public AttValPair<E,V,M> CopyAmbiguityChain(AttValPair<E,V,M> result) 
    {
      AttValPair<E,V,M> avp = this;

      if (result == null) {
        result = new AttValPair<E,V,M>(this);
        avp = avp.NextAmbiguity();
      }
    
      while (avp != null) {
        result.AddAmbiguity(new AttValPair<E,V,M>(avp));
        avp = avp.NextAmbiguity();
      }

      return result;
    }


    #region "Implement AmbiguousEntity interface"
    /// Override to update this instance's container.
    public override void Discard() {
      if (container != null) {
        container.Remove(this);
      }
      base.Discard();
    }

    /// Override to update this instance's container.
    public override void Resolve() {
      AvpContainer<E,V,M> theContainer = this.container;
      if (container != null) {
        container.RemoveAll(this);  //note: this unsets container for this instance
      }
      base.Resolve();
      if (theContainer != null) {
        theContainer.Add(this);  // restores container
      }
    }

    /// Determine whether this ambiguous entity matches (is a duplicate of) the other
    public override bool Matches(AmbiguousEntity<AttValPair<E,V,M>> other) {
      bool result = (this == other);
      if (!result && other != null) {
        // types must match
        AttValPair<E,V,M> otherAVP = other.Entity;
        if (this.AttType == null && otherAVP.AttType == null) {
          // rely on otherType
          result = ((this.OtherType == otherAVP.OtherType) ||
                    (this.OtherType != null && this.OtherType.Equals(otherAVP.OtherType)));
        }
        else {
          // rely on attType
          result = (object.ReferenceEquals(this.AttType, otherAVP.AttType));
        }

        // and values must match
        if (result) {
          result = this.values.Equals(otherAVP.values);
        }
      }

      return result;
    }
    #endregion

    #region "Implement MultipleValues interface"
    /// Safely and efficiently typecast this to an MultipleValues.
    public MultipleValues<V> AsMultipleValues() {
      return this;
    }

    /// Add another value, returning true if added (unique).
    public bool AddValue(V value) {
      bool result = false;

      if (value != null) {
        if (values == null) {
          values = new MultipleValuesContainer<V>(value);
          result = true;
        }
        else {
          result = values.AddValue(value);
        }
      }

      return result;
    }

    /// Remove the given value, returning true if removed (existed).
    public bool RemoveValue(V value) {
      return (values == null) ? false : values.RemoveValue(value);
    }
    #endregion
  }
}
