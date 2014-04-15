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
using System.Collections.Specialized;

namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Container for attribute value pairs.
  /// 
  /// Supports strongly typed canonical attribute access as well as free-typed
  /// attribute access.
  /// 
  /// Templated for the canonical attribute enumeration, E, and the value object, V.
  /// 
  /// NOTE that attributes are case-insensitive!
  /// 
  /// Each attribute can map to only a single AttValPair. If multiple AttValPair
  /// instances can be referenced through a single attribute type, then the
  /// MultiValueDecorator aspects of the AttValPair instances should be used.
  /// 
  /// An attribute classifier is required for the accessors that require a generic
  /// string to be converted to a canonical attribute.
  /// 
  /// Note that the container itself can be ambiguous (or have multiple ambiguous
  /// manifestations).
  /// </summary>
  /// <author>Spence Koehler</author>
  public class AvpContainer<E,V,M> 
    : AbstractAmbiguousEntity<AvpContainer<E,V,M>> 
    where E : Canonical
  {

    private AttributeClassifier<E> attributeClassifier;
    private IDictionary<E, AttValPair<E,V,M>> strongAVPs;
    private IDictionary<string, AttValPair<E,V,M>> unclassifiedAVPs;
    private IDictionary<string, AttValPair<E,V,M>> classifiedAVPs;
    private M metaData;

    /// Determine whether this container is empty (of attribute value pairs).
    public bool IsEmpty {
      get {
        return 
          (strongAVPs == null || strongAVPs.Count == 0) && 
          (unclassifiedAVPs == null || unclassifiedAVPs.Count == 0);
      }
    }

    /// The number of attribute value pairs stored in this container.
    public int Count {
      get {
        
        int result = 0;
        
        if (strongAVPs != null) result += strongAVPs.Count;
        if (unclassifiedAVPs != null) result += unclassifiedAVPs.Count;

        return result;
      }
    }

    /// Determine whether this instance holds meta-data.
    public bool HasMetaData { get { return metaData != null; } }

    /// Get the meta-data associated with this instance.
    public M getMetaData() {
      return metaData;
    }

    /// Set the meta-data for this instance.
    public M setMetaData(M metaData) {
      M result = this.metaData;
      this.metaData = metaData;
      return result;
    }

    /// Determine whether this instance has any ambiguous attValPair instances.
    public bool HasAmbiguity {
      get
      {
        bool result = false;

        if (strongAVPs != null) {
          foreach(AttValPair<E,V,M> avp in strongAVPs.Values) {
            if (avp.IsAmbiguous) {
              result = true;
              break;
            }
          }
        }

        if (!result && unclassifiedAVPs != null) {
          foreach(AttValPair<E,V,M> avp in unclassifiedAVPs.Values) {
            if (avp.IsAmbiguous) {
              result = true;
              break;
            }
          }
        }

        return result;
      }
    }

    /// Get those attValPair instances that are ambiguous (e.g., to find/resolve
    /// ambiguities), or null.
    public IList<AttValPair<E,V,M>> Ambiguities {
      get
      {
        IList<AttValPair<E,V,M>> result = null;

        if (strongAVPs != null) {
          foreach(AttValPair<E,V,M> avp in strongAVPs.Values) {
            if (avp.IsAmbiguous) {
              if (result == null) result = new List<AttValPair<E,V,M>>();
              result.Add(avp);
            }
          }
        }

        if (unclassifiedAVPs != null) {
          foreach(AttValPair<E,V,M> avp in unclassifiedAVPs.Values) {
            if (avp.IsAmbiguous) {
              if (result == null) result = new List<AttValPair<E,V,M>>();
              result.Add(avp);
            }
          }
        }

        return result;
      }
    }

    #region "Implement AmbiguousEntity interface"
    /// Safely and efficiently typecast this to an AvpContainer.
    public override AvpContainer<E,V,M> Entity { get { return this; } }

    /// Simple typecasting helper auxiliary for getting the first ambiguity.
    public AvpContainer<E,V,M> FirstAmbiguity() {
      return (AvpContainer<E,V,M>)GetFirstAmbiguity();
    }

    /// Simple typecasting helper auxiliary for getting the next ambiguity.
    public AvpContainer<E,V,M> NextAmbiguity() {
      return (AvpContainer<E,V,M>)GetNextAmbiguity();
    }
    #endregion

    //
    // IMPLEMENTATION NOTES:
    //
    // - Strong AVPs that are created due to classification are also stored in classifiedAVPs
    //   - these will have copies of each AVP, because the ambiguity chains will be different
    //     - the strong AVPs will all have the same attType
    //     - the classified AVPs will all have the same otherType
    //   - these are both preserved so that access through either the enum or text will give the same AVP instance(s)
    //   - both a weak "add" and "get" can lead to a classified AVP insert
    //
    // - Unclassified AVPs holds those that could not be successfully classified
    //
    // - Removal of an AVP (e.g., while disambiguating) needs to occur from both the strong and classified stores
    //
    
    /// Default constructor without an attribute classifier.
    public AvpContainer() : this(null) { }

    /// Construct with the given attributeClassifier.
    public AvpContainer(AttributeClassifier<E> attributeClassifier) {
      this.attributeClassifier = attributeClassifier;
      this.strongAVPs = null;
      this.unclassifiedAVPs = null;
      this.classifiedAVPs = null;
      this.metaData = default(M);
    }

    /// Get the canonical attribute's stored attribute/value pair, or null.
    public AttValPair<E,V,M> Get(E attType) 
    {
      AttValPair<E,V,M> result = null;

      if (attType != null && attType.IsCanonical && 
          strongAVPs != null) 
      {
        result = strongAVPs.Get(attType);

        // check for an unclassifiedAVP that now maps to a strongAVP
        if (result == null && unclassifiedAVPs != null) {
          result = Get(attType.ToString());
        }
      }

      return result;
    }

    /// Get the attribute/value pair associated with the given attribute type, or
    /// null. Use the attributeClassifier if present and needed.
    public AttValPair<E,V,M> Get(string attType) 
    {
      AttValPair<E,V,M> result = null;

      if (attType == null || IsEmpty) return result;

      string normAtt = attType.ToLower();

      // first check in unclassified
      if (result == null && unclassifiedAVPs != null) {
        result = unclassifiedAVPs.Get(normAtt);
      }

      // next check in previously classified
      if (result == null && classifiedAVPs != null) {
        result = classifiedAVPs.Get(normAtt);
      }

      // compute classification(s) of the attType text
      if (result == null && attributeClassifier != null && 
          strongAVPs != null) 
      {
        Attribute<E> attribute = attributeClassifier.GetAttribute(attType);

        // retrieve stored avp's (and their ambiguities)
        while (attribute != null) 
        {
          E att = attribute.AttType;

          AttValPair<E,V,M> avp = strongAVPs.Get(att);

          // create shallow copies with a new ambiguity chain for the result
          if (avp != null) {
            result = avp.CopyAmbiguityChain(result);
          }

          // inc to next attribute classification ambiguity
          attribute = attribute.NextAmbiguity();
        }

        if (result != null) {
          classifiedAVPs.Put(normAtt, result);
        }
      }

      return result;
    }

    /// Get all AttValPair instances in this container.
    public IList<AttValPair<E,V,M>> GetAll() 
    {
      IList<AttValPair<E,V,M>> result = null;

      if (strongAVPs != null) {
        foreach(AttValPair<E,V,M> avp in strongAVPs.Values) 
        {
          if (result == null) result = new List<AttValPair<E,V,M>>();
          result.Add(avp);
        }
      }

      if (unclassifiedAVPs != null) {
        foreach(AttValPair<E,V,M> avp in unclassifiedAVPs.Values) 
        {
          if (result == null) result = new List<AttValPair<E,V,M>>();
          result.Add(avp);
        }
      }

      return result;
    }

    /// Get all canonical (strongly typed) AttValPair instances in this container.
    public IList<AttValPair<E,V,M>> GetAllCanonical() 
    {
      return strongAVPs == null ? null : 
                                  new List<AttValPair<E,V,M>>(strongAVPs.Values);
    }

    /// Add an attribute/value pair instance to this contianer.
    /// 
    /// As a side-effect, the avp's non-canonical attType may be updated and the
    /// avp may become ambiguous.
    /// 
    /// An unambiguous avp when added will be inserted to the front of the
    /// ambiguity chain. To add to the end, use "addNext" instead.
    /// 
    /// NOTE: this container stores copies of the submitted avp's.
    public void Add(AttValPair<E,V,M> avp) {
      doAdd(avp, false, false);
    }
    
    /// Convenience method to add a generic AttValPair.
    /// 
    /// Note that as an unambiguous avp when added, the new avp will be inserted
    /// to the front of the ambiguity chain. To add to the end, use "addNext"
    /// instead.
    public void Add(string attType, V value) {
      this.Add(new AttValPair<E,V,M>(default(E), attType, value));
    }

    public void AddNext(AttValPair<E,V,M> avp) {
      doAdd(avp, false, true);
    }

    public void AddNext(string attType, V value) {
      this.AddNext(new AttValPair<E,V,M>(default(E), attType, value));
    }

    /// Set the attribute/value pair, overriding any existing matches
    /// in this container (instead of adding as ambiguous).
    public void Override(AttValPair<E,V,M> avp) {
      doAdd(avp, true, false);
    }

    private void doAdd(AttValPair<E,V,M> avp, bool doOverride, bool next) {
      if (avp != null) {
        IDictionary<string, Attribute<E>> classifications = null;

        // update missing attType if needed and possible
        if (avp.AttType == null && attributeClassifier != null) {
          classifications = doClassify(avp);
        }

        if (doOverride) {
          RemoveAll(avp);
        }

        // if avp has a canonical attribute (strongly typed), then add all
        // (unique) ambiguities for strong reference
        while (avp != null) {
          doAddInstance(avp, classifications, next);
          avp = avp.NextAmbiguity();
        }
      }
    }

    /// Remove all attribute/value pairs from this instance having the attribute(s)
    /// of the given instance.
    /// <p>
    /// Note that if the given avp is ambiguous with different attributes, all
    /// ambiguous entries will be removed!
    public void RemoveAll(AttValPair<E,V,M> avp) 
    {
      if (avp == null) return;

      if (avp.IsAmbiguous) {
        for (avp = avp.FirstAmbiguity(); avp != null; avp = avp.NextAmbiguity()) {
          avp.SetContainer(null);
          doRemoveAll(avp);
        }
      }
      else {
        avp.SetContainer(null);
        doRemoveAll(avp);
      }
    }

    /// Remove just the given attribute value pair instance (and none of its
    /// ambiguities).
    /// <p>
    /// Package protected for intended access from AttValPair, which will be
    /// responsible for calling the underlying "discard" to remove it from
    /// its chain.
    /// <p>
    /// This method will update the mapping to the first ambiguity by type
    /// if necessary.
    internal void Remove(AttValPair<E,V,M> avp) 
    {
      if (avp != null) {
        avp.SetContainer(null);
        doRemove(avp);
      }
    }

    /// Remove all attribute/value pairs from this instance having the attType.
    public bool Remove(E attType) 
    {
      bool result = false;

      if (attType != null && attType.IsCanonical) {
        if (strongAVPs != null) {
          if (strongAVPs.ContainsKey(attType)) {
            // remove from classifiedAVPs
            if (classifiedAVPs != null && classifiedAVPs.Count > 0) {
              AttValPair<E,V,M> avp = strongAVPs.Get(attType);
              while (avp != null) {
                disconnect(classifiedAVPs, avp, true);
                avp = avp.NextAmbiguity();
              }
            }

            // remove from strongAVPs
            strongAVPs.Get(attType).ClearAllContainers();
            strongAVPs.Remove(attType);
            result = true;
          }
        }
      }

      return result;
    }

    /// Remove all attribute/value pairs from this instance having the attType.
    /// 
    /// Note that if the given attType is ambiguous with different attributes, all
    /// ambiguous entries will be removed!
    public bool Remove(string attType) 
    {
      bool result = false;

      if (attType == null || IsEmpty) return result;

      string normAtt = attType.ToLower();

      // first check in unclassified
      if (!result && unclassifiedAVPs != null && unclassifiedAVPs.ContainsKey(normAtt)) {
        unclassifiedAVPs.Get(normAtt).ClearAllContainers();
        result = (unclassifiedAVPs.Remove(normAtt) != null);
      }

      // next check in previously classified
      if (!result && classifiedAVPs != null && classifiedAVPs.Count > 0) {
        if (classifiedAVPs.ContainsKey(normAtt)) {
          AttValPair<E,V,M> avp = classifiedAVPs.Get(normAtt);
          while (avp != null) {
            E strongType = avp.AttType;
            if (strongAVPs.ContainsKey(strongType)) {
              strongAVPs.Get(strongType).ClearAllContainers();
              strongAVPs.Remove(strongType);
            }
            avp = avp.NextAmbiguity();
          }

          // remove from classifiedAVPs
          classifiedAVPs.Get(normAtt).ClearAllContainers();
          classifiedAVPs.Remove(normAtt);
          result = true;
        }
      }

      // compute classification(s) of the attType text
      if (!result && attributeClassifier != null && strongAVPs != null) {
        Attribute<E> attribute = attributeClassifier.GetAttribute(attType);

        // remove stored avp's (and their ambiguities)
        while (attribute != null) {
          E att = attribute.AttType;

          if (strongAVPs.ContainsKey(att)) {
            result = Remove(att);
          }
          attribute = attribute.NextAmbiguity();
        }
      }

      return result;
    }

    /// Get a string representation of this container's contents.
    public override string ToString() {
      StringBuilder result = new StringBuilder();

      result.Append('[');

      bool didFirst = false;

      if (strongAVPs != null) {
        foreach(AttValPair<E,V,M> avp in strongAVPs.Values) {
          if (didFirst) result.Append("; ");
          result.Append(avp.ToString());
          didFirst = true;
        }
      }
  
      if (unclassifiedAVPs != null) {
        foreach(AttValPair<E,V,M> avp in unclassifiedAVPs.Values) {
          if (didFirst) result.Append("; ");
          result.Append(avp.ToString());
          didFirst = true;
        }
      }
  
      result.Append(']');
      if (metaData != null) {
        result.Append('+');
      }

      return result.ToString();
    }

    private void doAddInstance(AttValPair<E,V,M> avp, 
                               IDictionary<string, Attribute<E>> classifications, 
                               bool next) 
    {
      // update strongAVPs
      E attType = avp.AttType;
      if (attType != null && attType.IsCanonical) {
        if (strongAVPs == null) strongAVPs = new OrderedDictionary<E, AttValPair<E,V,M>>();
        AttValPair<E,V,M> curAVP = strongAVPs.Get(attType);

        AttValPair<E,V,M> copyToAdd = new AttValPair<E,V,M>(avp);
        copyToAdd.SetContainer(this);

        if (curAVP == null) {
          strongAVPs.Put(attType, copyToAdd);
        }
        else {
          if (!avp.IsAmbiguous && !next) {
            // insert non-ambiguous new to the front of the ambiguity chain
            //
            // NOTE: this has the effect that later unambiguous adds with the
            //       same att are ambiguous, but take precedence
            //
            copyToAdd.InsertAmbiguity(curAVP);
            strongAVPs.Put(attType, copyToAdd);
          }
          else {
            // add to the end of the ambiguity chain
            curAVP.AddAmbiguity(copyToAdd);
          }
        }

        // update classifiedAVPs
        if (classifications != null && classifications.ContainsKey(avp.OtherType)) {
          classifiedAVPs = updateMapWith(classifiedAVPs, avp);
        }
      }

      // store unclassified attribute
      else {
        unclassifiedAVPs = updateMapWith(unclassifiedAVPs, avp);
      }
    }

    private IDictionary<string, AttValPair<E,V,M>> 
      updateMapWith(IDictionary<string, AttValPair<E,V,M>> map, AttValPair<E,V,M> avp) 
    {
      if (avp != null && avp.HasOtherType) {
        if (map == null) map = new OrderedDictionary<string, AttValPair<E,V,M>>();
        string normAtt = avp.OtherType.ToLower();
        AttValPair<E,V,M> curAVP = map.Get(normAtt);

        AttValPair<E,V,M> copyToAdd = new AttValPair<E,V,M>(avp);
        copyToAdd.SetContainer(this);

        if (curAVP == null) {
          // set avp
          map.Put(normAtt, copyToAdd);
        }
        else {
          // add at end
          curAVP.AddAmbiguity(copyToAdd);
        }
      }

      return map;
    }

    private string getWeakType(AttValPair<E,V,M> avp) {
      string result = avp.OtherType;

      if (result == null) {
        E attType = avp.AttType;
        if (attType != null) {
          result = attType.ToString().ToLower();
        }
      }

      return result == null ? "" : result;
    }

    private bool doRemoveAll(AttValPair<E,V,M> avp) {
      bool result = false;

      if (avp == null) return result;

      // remove strongAVPs and classifiedAVPs
      if (!Remove(avp.AttType) && avp.HasOtherType && 
          unclassifiedAVPs != null && unclassifiedAVPs.Count > 0) {
        // remove  unclassifiedAVPs
        string normAtt = avp.OtherType.ToLower();
        if (unclassifiedAVPs.ContainsKey(normAtt)) {
          unclassifiedAVPs.Get(normAtt).ClearAllContainers();
          result = (unclassifiedAVPs.Remove(normAtt) != null);
        }
      }
      else {
        result = true;
      }

      return result;
    }

    private void doRemove(AttValPair<E,V,M> avp) {
      if (avp == null) return;

      if (avp.IsCanonical) {
        // disconnect from strong
        disconnect(strongAVPs, avp);

        // disconnect from classified
        disconnect(classifiedAVPs, avp, true);
      }
      else {
        // disconnect from unclassified
        disconnect(unclassifiedAVPs, avp, false);
      }

      avp.SetContainer(null);
    }

    private bool disconnect(IDictionary<E, AttValPair<E,V,M>> map, 
                            AttValPair<E,V,M> avp) 
    {
      bool result = false;

      if (map != null && map.Count > 0 && avp.IsCanonical) {
        E attType = avp.AttType;
        AttValPair<E,V,M> foundAVP = map.Get(attType);
        bool isFirst = true;
        while (foundAVP != null) {
          if (matches(avp, foundAVP)) {
            result = true;

            // remove the first from the ambiguity chain
            if (isFirst) {
              AttValPair<E,V,M> nextAVP = foundAVP.NextAmbiguity();
              if (nextAVP != null) {
                map.Put(attType, nextAVP);
              }
              else {
                // there are no more avps for this attType
                map.Remove(attType);
              }
            }
            break;
          }
          isFirst = false;
          foundAVP = foundAVP.NextAmbiguity();
        }
      }

      return result;
    }

    private bool disconnect(IDictionary<string, AttValPair<E,V,M>> map, 
                            AttValPair<E,V,M> avp, bool normalize) 
    {
      bool result = false;

      if (map != null && map.Count > 0 && avp.HasOtherType) {
        string normAtt = normalize ? avp.OtherType.ToLower() : avp.OtherType;
        AttValPair<E,V,M> foundAVP = map.Get(normAtt);
        bool isFirst = true;
        while (foundAVP != null) {
          if (matches(avp, foundAVP)) {
            result = true;

            // remove the first from the ambiguity chain
            if (isFirst) {
              AttValPair<E,V,M> nextAVP = foundAVP.NextAmbiguity();
              if (nextAVP != null) {
                map.Put(normAtt, nextAVP);
              }
              else {
                // remove when empty
                map.Remove(normAtt);
              }
            }

            foundAVP.Remove();

            break;
          }
          isFirst = false;
          foundAVP = foundAVP.NextAmbiguity();
        }
      }

      return result;
    }

    private bool matches(AttValPair<E,V,M> avp1, AttValPair<E,V,M> avp2) 
    {
      bool result = (avp1 == avp2);

      if (!result) {
        // copies share the same attType instance
        if (object.ReferenceEquals(avp1.AttType, avp2.AttType)) {
          // and have the same normalized other type
          if ((!avp1.HasOtherType && !avp2.HasOtherType) ||
              (avp1.HasOtherType && avp2.HasOtherType && 
               string.Equals(avp1.OtherType.ToLower(), avp2.OtherType.ToLower()))) 
          {
            // check for matching values
            if (avp1.ValuesCount == avp2.ValuesCount) 
            {
              if (avp1.ValuesCount == 0 || avp1.Values.Equals(avp2.Values)) {
                result = true;
              }
            }
          }
        }
      }

      return result;
    }

    private IDictionary<string, Attribute<E>> doClassify(AttValPair<E,V,M> avp) 
    {
      // compute classifications
      IDictionary<string, Attribute<E>> classifications = GetClassifications(avp);

      // update avp with classification results
      if (classifications != null) {
        while (avp != null) {
          AttValPair<E,V,M> nextAVP = avp.NextAmbiguity();

          if (!avp.IsCanonical && avp.HasOtherType) {
            // then avp needs to be classified
            Attribute<E> attribute = classifications.Get(avp.OtherType);

            if (attribute != null) {
              // set the classified attType
              avp.AttType = attribute.AttType;

              // insert an ambiguous avp for each ambiguous attribute
              if (attribute.IsAmbiguous) {
                for (attribute = attribute.NextAmbiguity(); 
                     attribute != null; 
                     attribute = attribute.NextAmbiguity()) 
                {
                  AttValPair<E,V,M> ambAVP = new AttValPair<E,V,M>(avp);
                  ambAVP.AttType = attribute.AttType;
                  avp.AddAmbiguity(ambAVP);
                  avp = ambAVP;
                }
              }
            }
          }

          // inc to update next ambiguity
          avp = nextAVP;
        }
      }

      return classifications;
    }

    private IDictionary<string, Attribute<E>> GetClassifications(AttValPair<E,V,M> avp) 
    {
      IDictionary<string, Attribute<E>> result = null;

      if (attributeClassifier == null) return result;

      while (avp != null) {
        if (!avp.IsCanonical && avp.HasOtherType) {
          string otherType = avp.OtherType;
          if (result == null || !result.ContainsKey(otherType)) {
            Attribute<E> attribute = attributeClassifier.GetAttribute(otherType);
            if (result == null) 
              result = new OrderedDictionary<string, Attribute<E>>();
            result.Put(otherType, attribute);
          }
        }
        avp = avp.NextAmbiguity();
      }

      return result;
    }

    #region "Implement AmbiguousEntity interface"
    /// Determine whether this ambiguous entity matches (is a duplicate of) the other
    public override bool Matches(AmbiguousEntity<AvpContainer<E,V,M>> other) {
      bool result = (this == other);
      if (!result && other != null) {
        AvpContainer<E,V,M> otherAvpC = other.Entity;
        if (this.attributeClassifier == otherAvpC.attributeClassifier ||
            (this.attributeClassifier != null && 
             this.attributeClassifier.Equals(otherAvpC.attributeClassifier))) 
        {
          if (matches(this.strongAVPs, otherAvpC.strongAVPs) && 
              matches(this.unclassifiedAVPs, otherAvpC.unclassifiedAVPs)) 
          {
            result = true;
          }
        }
      }
      return result;
    }

    private static bool matches<X,E,V,M>(IDictionary<X, AttValPair<E, V, M>> map1, 
                                         IDictionary<X, AttValPair<E,V,M>> map2) 
      where E : Canonical
    {
      bool result = (map1 == map2);

      if (!result && 
          map1 != null && map2 != null && 
          map1.Count == map2.Count) 
      {
        result = true;
        foreach(KeyValuePair<X, AttValPair<E,V,M>> entry1 in map1) 
        {
          AttValPair<E,V,M> avp1 = entry1.Value;
          AttValPair<E,V,M> avp2 = map2.Get(entry1.Key);

          if (avp1 == avp2) continue;
          else if (avp1 == null || avp2 == null) {
            result = false;
          }
          else {
            while (result && avp1 != null && avp2 != null) {
              if (!avp1.Matches(avp2)) {
                result = false;
                break;
              }
              else {
                avp1 = avp1.NextAmbiguity();
                avp2 = avp2.NextAmbiguity();
              }
            }
            if (result && (avp1 != null || avp2 != null)) result = false;  // one has extras
          }
          if (!result) break;
        }
      }

      return result;
    }
    #endregion
  }
}
