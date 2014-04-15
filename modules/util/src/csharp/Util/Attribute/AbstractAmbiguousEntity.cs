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
  /// Abstract implementation of the AmbiguousEntity interface.
  /// 
  /// Extenders (e.g., "MyClass") need to implement getEntity, addAmbiguity, and
  /// insertAmbiguity similarly to:
  /// <pre>
  ///  public MyClass getEntity() {
  ///    return this;
  ///  }
  /// </pre>
  /// 
  /// See AttValPair and Attribute for implementation examples.
  /// </summary>
  /// <author>Spence Koehler</author>
  public abstract class AbstractAmbiguousEntity<T> : AmbiguousEntity<T> 
  {
    private AmbiguityChain<T> ambiguityChain;
  
    /// The entity wrapped by this instance (safe and efficient typecast).
    public abstract T Entity { get; }

    /// Determine whether the current instance is ambiguous.
    public bool IsAmbiguous
    {
      get {
        bool result = false;
        
        if (ambiguityChain != null) {
          int count = 0;
          
          AmbiguousEntity<T> entity = GetFirstAmbiguity(); 
          while(entity != null) {
            ++count;
            if (count > 1) {
              result = true;
              break;
            }
            entity = entity.GetNextAmbiguity();
          }
        }
        
        return result;
      }
    }

    /// The ambiguity precedence of this instance, where precedence values
    /// span from low (most likely) to high (least likely).
    public int Precedence {
      get {
        return ambiguityChain.GetPrecedence(this);
      }
    }

    /// Determine whether there is a next (less likely than this) ambiguous entity. */
    public bool HasNextAmbiguity {
      get {
        return ambiguityChain != null && ambiguityChain.HasNext(this);
      }
    }

    /// The number of ambiguities.
    public int AmbiguityCount {
      get {
        int result = 0;
        
        if (ambiguityChain != null) {
          AmbiguousEntity<T> entity = GetFirstAmbiguity();
          while (entity != null) {
            ++result;
            entity = entity.GetNextAmbiguity();
          }
        }
        
        return result;
      }
    }


    protected AbstractAmbiguousEntity() {
      this.ambiguityChain = null;
    }

    /// Get the first (most likely) ambiguous entity.
    public AmbiguousEntity<T> GetFirstAmbiguity() 
    {
      AmbiguousEntity<T> result = null;
      
      if (ambiguityChain != null) {
        result = ambiguityChain.Get(0);
      }
      
      return result;
    }

    /// Get the next (less likely than this) ambiguous entity.
    public AmbiguousEntity<T> GetNextAmbiguity() 
    {
      AmbiguousEntity<T> result = null;
      
      if (ambiguityChain != null) {
        // get the next ambiguity that doesn't match this
        result = ambiguityChain.GetNext(this);
        while (result != null && result.Matches(this))
          result = ambiguityChain.GetNext(result);
      }
      
      return result;
    }


    /// Safely and efficiently typecast this to an AmbiguousEntity.
    public AmbiguousEntity<T> AsAmbiguousEntity() {
      return this;
    }

    /// Rule out this as a valid entity in its ambiguity chain.
    public virtual void Discard() {
      Remove();
    }

    /// Resolve this instance as an unambiguous entity.
    public virtual void Resolve() {
      Remove();
    }

    /// Remove this instance from its ambiguity chain.
    public void Remove() {
      if (ambiguityChain != null) {
        ambiguityChain.Remove(this);
        this.ambiguityChain = null;
      }
    }

    /// Add another ambiguous entity to the end of the chain.
    public void AddAmbiguity(AmbiguousEntity<T> leastLikelyEntity) {
      if (leastLikelyEntity == null) return;
      if (ambiguityChain == null) {
        this.ambiguityChain = new AmbiguityChain<T>();
        this.ambiguityChain.Add(this);
      }
      ambiguityChain.Add(leastLikelyEntity);
      ((AbstractAmbiguousEntity<T>)leastLikelyEntity).ambiguityChain = this.ambiguityChain;
    }

    /// Insert a less-likley ambiguous entity immediately after this.
    public void InsertAmbiguity(AmbiguousEntity<T> lessLikelyEntity) {
      if (lessLikelyEntity == null) return;
      if (ambiguityChain == null) {
        if (lessLikelyEntity.IsAmbiguous) {
          AbstractAmbiguousEntity<T> other = (AbstractAmbiguousEntity<T>)lessLikelyEntity;
          other.ambiguityChain.InsertAfter(null, this);
          this.ambiguityChain = other.ambiguityChain;
        }
        else {
          this.ambiguityChain = new AmbiguityChain<T>();
          this.ambiguityChain.Add(this);
          this.ambiguityChain.Add(lessLikelyEntity);
        }
      }
      else {
        ambiguityChain.InsertAfter(this, lessLikelyEntity);
      }
      ((AbstractAmbiguousEntity<T>)lessLikelyEntity).ambiguityChain = this.ambiguityChain;
    }

    /// Determine whether this ambiguous entity matches (is a duplicate of) the other
    public abstract bool Matches(AmbiguousEntity<T> other);
  }
}
