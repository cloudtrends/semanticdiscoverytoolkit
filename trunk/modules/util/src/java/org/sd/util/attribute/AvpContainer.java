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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for attribute value pairs.
 * <p>
 * Supports strongly typed canonical attribute access as well as free-typed
 * attribute access.
 * <p>
 * Templated for the canonical attribute enumeration, E, and the value object, V.
 * <p>
 * NOTE that attributes are case-insensitive!
 * <p>
 * Each attribute can map to only a single AttValPair. If multiple AttValPair
 * instances can be referenced through a single attribute type, then the
 * MultiValueDecorator aspects of the AttValPair instances should be used.
 * <p>
 * An attribute classifier is required for the accessors that require a generic
 * string to be converted to a canonical attribute.
 *
 * @author Spence Koehler
 */
public class AvpContainer <E extends Canonical, V, M> {

  private AttributeClassifier<E> attributeClassifier;
  private Map<E, AttValPair<E, V, M>> strongAVPs;
  private Map<String, AttValPair<E, V, M>> unclassifiedAVPs;
  private Map<String, AttValPair<E, V, M>> classifiedAVPs;
  private M metaData;

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

  /**
   * Default constructor without an attribute classifier.
   */
  public AvpContainer() {
    this(null);
  }

  /**
   * Construct with the given attributeClassifier.
   */
  public AvpContainer(AttributeClassifier<E> attributeClassifier) {
    this.attributeClassifier = attributeClassifier;
    this.strongAVPs = null;
    this.unclassifiedAVPs = null;
    this.classifiedAVPs = null;
    this.metaData = null;
  }

  /**
   * Determine whether this container is empty (of attribute value pairs).
   */
  public boolean isEmpty() {
    return (strongAVPs == null || strongAVPs.size() == 0) && (unclassifiedAVPs == null || unclassifiedAVPs.size() == 0);
  }

  /**
   * Get the number of attribute value pairs stored in this container.
   */
  public int size() {
    int result = 0;

    if (strongAVPs != null) result += strongAVPs.size();
    if (unclassifiedAVPs != null) result += unclassifiedAVPs.size();

    return result;
  }

  /**
   * Get the canonical attribute's stored attribute/value pair, or null.
   */
  public AttValPair<E, V, M> get(E attType) {
    AttValPair<E, V, M> result = null;

    if (attType != null && attType.isCanonical() && strongAVPs != null) {
      result = strongAVPs.get(attType);

      // check for an unclassifiedAVP that now maps to a strongAVP
      if (result == null && unclassifiedAVPs != null) {
        result = get(attType.toString());
      }
    }

    return result;
  }

  /**
   * Get the attribute/value pair associated with the given attribute type, or
   * null. Use the attributeClassifier if present and needed.
   */
  public AttValPair<E, V, M> get(String attType) {
    AttValPair<E, V, M> result = null;

    if (attType == null || isEmpty()) return result;

    final String normAtt = attType.toLowerCase();

    // first check in unclassified
    if (result == null && unclassifiedAVPs != null) {
      result = unclassifiedAVPs.get(normAtt);
    }

    // next check in previously classified
    if (result == null && classifiedAVPs != null) {
      result = classifiedAVPs.get(normAtt);
    }

    // compute classification(s) of the attType text
    if (result == null && attributeClassifier != null && strongAVPs != null) {
      Attribute<E> attribute = attributeClassifier.getAttribute(attType);

      // retrieve stored avp's (and their ambiguities)
      while (attribute != null) {
        final E att = attribute.getAttType();

        final AttValPair<E, V, M> avp = strongAVPs.get(att);

        // create shallow copies with a new ambiguity chain for the result
        if (avp != null) {
          result = avp.copyAmbiguityChain(result);
        }

        // inc to next attribute classification ambiguity
        attribute = attribute.nextAmbiguity();
      }

      if (result != null) {
        classifiedAVPs.put(normAtt, result);
      }
    }

    return result;
  }

  /** Get all AttValPair instances in this container. */
  public List<AttValPair<E, V, M>> getAll() {
    List<AttValPair<E, V, M>> result = null;

    if (strongAVPs != null) {
      for (AttValPair<E, V, M> avp : strongAVPs.values()) {
        if (result == null) result = new ArrayList<AttValPair<E, V, M>>();
        result.add(avp);
      }
    }

    if (unclassifiedAVPs != null) {
      for (AttValPair<E, V, M> avp : unclassifiedAVPs.values()) {
        if (result == null) result = new ArrayList<AttValPair<E, V, M>>();
        result.add(avp);
      }
    }

    return result;
  }

  /** Get all canonical (strongly typed) AttValPair instances in this container. */
  public List<AttValPair<E, V, M>> getAllCanonical() {
    return strongAVPs == null ? null : new ArrayList<AttValPair<E, V, M>>(strongAVPs.values());
  }

  /**
   * Add an attribute/value pair instance to this contianer.
   * <p>
   * As a side-effect, the avp's non-canonical attType may be updated and the
   * avp may become ambiguous.
   * <p>
   * NOTE: this container stores copies of the submitted avp's.
   */
  public void add(AttValPair<E, V, M> avp) {
    doAdd(avp, false);
  }

  /**
   * Convenience method to add a generic AttValPair.
   */
  public void add(String attType, V value) {
    this.add(new AttValPair<E, V, M>(attType, value));
  }

  /**
   * Set the attribute/value pair, overriding any existing matches
   * in this container (instead of adding as ambiguous).
   */
  public void override(AttValPair<E, V, M> avp) {
    doAdd(avp, true);
  }

  private final void doAdd(AttValPair<E, V, M> avp, boolean override) {
    if (avp != null) {
      Map<String, Attribute<E>> classifications = null;

      // update missing attType if needed and possible
      if (avp.getAttType() == null && attributeClassifier != null) {
        classifications = doClassify(avp);
      }

      if (override) {
        removeAll(avp);
      }

      // if avp has a canonical attribute (strongly typed), then add all
      // (unique) ambiguities for strong reference
      while (avp != null) {
        doAddInstance(avp, classifications);
        avp = avp.nextAmbiguity();
      }
    }
  }

  /**
   * Remove all attribute/value pairs from this instance having the attribute(s)
   * of the given instance.
   * <p>
   * Note that if the given avp is ambiguous with different attributes, all
   * ambiguous entries will be removed!
   */
  public void removeAll(AttValPair<E, V, M> avp) {
    if (avp == null) return;

    for (avp = avp.firstAmbiguity(); avp != null; avp = avp.nextAmbiguity()) {
      avp.setContainer(null);
      doRemoveAll(avp);
    }
  }

  /**
   * Remove just the given attribute value pair instance (and none of its
   * ambiguities).
   * <p>
   * Package protected for intended access from AttValPair, which will be
   * responsible for calling the underlying "discard" to remove it from
   * its chain.
   * <p>
   * This method will update the mapping to the first ambiguity by type
   * if necessary.
   */
  void remove(AttValPair<E, V, M> avp) {
    if (avp != null) {
      avp.setContainer(null);
      doRemove(avp);
    }
  }

  /**
   * Remove all attribute/value pairs from this instance having the attType.
   */
  public boolean remove(E attType) {
    boolean result = false;

    if (attType != null && attType.isCanonical()) {
      if (strongAVPs != null) {
        if (strongAVPs.containsKey(attType)) {
          // remove from classifiedAVPs
          if (classifiedAVPs != null && classifiedAVPs.size() > 0) {
            AttValPair<E, V, M> avp = strongAVPs.get(attType);
            while (avp != null) {
              disconnect(classifiedAVPs, avp, true);
              avp = avp.nextAmbiguity();
            }
          }

          // remove from strongAVPs
          strongAVPs.get(attType).clearAllContainers();
          strongAVPs.remove(attType);
          result = true;
        }
      }
    }

    return result;
  }

  /**
   * Remove all attribute/value pairs from this instance having the attType.
   * <p>
   * Note that if the given attType is ambiguous with different attributes, all
   * ambiguous entries will be removed!
   */
  public boolean remove(String attType) {
    boolean result = false;

    if (attType == null || isEmpty()) return result;

    final String normAtt = attType.toLowerCase();

    // first check in unclassified
    if (!result && unclassifiedAVPs != null && unclassifiedAVPs.containsKey(normAtt)) {
      unclassifiedAVPs.get(normAtt).clearAllContainers();
      result = (unclassifiedAVPs.remove(normAtt) != null);
    }

    // next check in previously classified
    if (!result && classifiedAVPs != null && classifiedAVPs.size() > 0) {
      if (classifiedAVPs.containsKey(normAtt)) {
        AttValPair<E, V, M> avp = classifiedAVPs.get(normAtt);
        while (avp != null) {
          final E strongType = avp.getAttType();
          if (strongAVPs.containsKey(strongType)) {
            strongAVPs.get(strongType).clearAllContainers();
            strongAVPs.remove(strongType);
          }
          avp = avp.nextAmbiguity();
        }

        // remove from classifiedAVPs
        classifiedAVPs.get(normAtt).clearAllContainers();
        classifiedAVPs.remove(normAtt);
        result = true;
      }
    }

    // compute classification(s) of the attType text
    if (!result && attributeClassifier != null && strongAVPs != null) {
      Attribute<E> attribute = attributeClassifier.getAttribute(attType);

      // remove stored avp's (and their ambiguities)
      while (attribute != null) {
        final E att = attribute.getAttType();

        if (strongAVPs.containsKey(att)) {
          result = remove(att);
        }
      }
    }

    return result;
  }

  /** Determine whether this instance holds meta-data. */
  public boolean hasMetaData() {
    return metaData != null;
  }

  /** Get the meta-data associated with this instance. */
  public M getMetaData() {
    return metaData;
  }

  /** Set the meta-data for this instance. */
  public M setMetaData(M metaData) {
    M result = this.metaData;
    this.metaData = metaData;
    return result;
  }

  /**
   * Determine whether this instance has any ambiguous attValPair instances.
   */
  public boolean hasAmbiguity() {
    boolean result = false;

    if (strongAVPs != null) {
      for (AttValPair<E, V, M> avp : strongAVPs.values()) {
        if (avp.isAmbiguous()) {
          result = true;
          break;
        }
      }
    }

    if (!result && unclassifiedAVPs != null) {
      for (AttValPair<E, V, M> avp : unclassifiedAVPs.values()) {
        if (avp.isAmbiguous()) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get those attValPair instances that are ambiguous (e.g., to find/resolve
   * ambiguities), or null.
   */
  public List<AttValPair<E, V, M>> getAmbiguities() {
    List<AttValPair<E, V, M>> result = null;

    if (strongAVPs != null) {
      for (AttValPair<E, V, M> avp : strongAVPs.values()) {
        if (avp.isAmbiguous()) {
          if (result == null) result = new ArrayList<AttValPair<E, V, M>>();
          result.add(avp);
        }
      }
    }

    if (unclassifiedAVPs != null) {
      for (AttValPair<E, V, M> avp : unclassifiedAVPs.values()) {
        if (avp.isAmbiguous()) {
          if (result == null) result = new ArrayList<AttValPair<E, V, M>>();
          result.add(avp);
        }
      }
    }

    return result;
  }

  /**
   * Get a string representation of this container's contents.
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append('[');

    boolean didFirst = false;

    if (strongAVPs != null) {
      for (AttValPair<E, V, M> avp : strongAVPs.values()) {
        if (didFirst) result.append("; ");
        result.append(avp.toString());
        didFirst = true;
      }
    }
  
    if (unclassifiedAVPs != null) {
      for (AttValPair<E, V, M> avp : unclassifiedAVPs.values()) {
        if (didFirst) result.append("; ");
        result.append(avp.toString());
        didFirst = true;
      }
    }
  
    result.append(']');
    if (metaData != null) {
      result.append('+');
    }

    return result.toString();
  }

  private final void doAddInstance(AttValPair<E, V, M> avp, Map<String, Attribute<E>> classifications) {

    // update strongAVPs
    final E attType = avp.getAttType();
    if (attType != null && attType.isCanonical()) {
      if (strongAVPs == null) strongAVPs = new LinkedHashMap<E, AttValPair<E, V, M>>();
      final AttValPair<E, V, M> curAVP = strongAVPs.get(attType);

      final AttValPair<E, V, M> copyToAdd = new AttValPair<E, V, M>(avp);
      copyToAdd.setContainer(this);

      if (curAVP == null) {
        strongAVPs.put(attType, copyToAdd);
      }
      else {
        if (!avp.isAmbiguous()) {
          // insert non-ambiguous new to the front of the ambiguity chain
          //
          // NOTE: this has the effect that later unambiguous adds with the
          //       same att are ambiguous, but take precedence
          //
          copyToAdd.insertAmbiguity(curAVP);
          strongAVPs.put(attType, copyToAdd);
        }
        else {
          // add to the end of the ambiguity chain
          curAVP.addAmbiguity(copyToAdd);
        }
      }

      // update classifiedAVPs
      if (classifications != null && classifications.containsKey(avp.getOtherType())) {
        classifiedAVPs = updateMapWith(classifiedAVPs, avp);
      }
    }

    // store unclassified attribute
    else {
      unclassifiedAVPs = updateMapWith(unclassifiedAVPs, avp);
    }
  }

  private final Map<String, AttValPair<E, V, M>> updateMapWith(Map<String, AttValPair<E, V, M>> map, AttValPair<E, V, M> avp) {
    if (avp != null && avp.hasOtherType()) {
      if (map == null) map = new LinkedHashMap<String, AttValPair<E, V, M>>();
      final String normAtt = avp.getOtherType().toLowerCase();
      final AttValPair<E, V, M> curAVP = map.get(normAtt);

      final AttValPair<E, V, M> copyToAdd = new AttValPair<E, V, M>(avp);
      copyToAdd.setContainer(this);

      if (curAVP == null) {
        // set avp
        map.put(normAtt, copyToAdd);
      }
      else {
        // add at end
        curAVP.addAmbiguity(copyToAdd);
      }
    }

    return map;
  }


  private final String getWeakType(AttValPair<E, V, M> avp) {
    String result = avp.getOtherType();

    if (result == null) {
      final E attType = avp.getAttType();
      if (attType != null) {
        result = attType.toString().toLowerCase();
      }
    }

    return result == null ? "" : result;
  }

  private final boolean doRemoveAll(AttValPair<E, V, M> avp) {
    boolean result = false;

    if (avp == null) return result;

    // remove strongAVPs and classifiedAVPs
    if (!remove(avp.getAttType()) && avp.hasOtherType() && unclassifiedAVPs != null && unclassifiedAVPs.size() > 0) {
      // remove  unclassifiedAVPs
      final String normAtt = avp.getOtherType().toLowerCase();
      if (unclassifiedAVPs.containsKey(normAtt)) {
        unclassifiedAVPs.get(normAtt).clearAllContainers();
        result = (unclassifiedAVPs.remove(normAtt) != null);
      }
    }
    else {
      result = true;
    }

    return result;
  }

  private final void doRemove(AttValPair<E, V, M> avp) {
    if (avp == null) return;

    if (avp.isCanonical()) {
      // disconnect from strong
      disconnect(strongAVPs, avp);

      // disconnect from classified
      disconnect(classifiedAVPs, avp, true);
    }
    else {
      // disconnect from unclassified
      disconnect(unclassifiedAVPs, avp, false);
    }

    avp.setContainer(null);
  }

  private final boolean disconnect(Map<E, AttValPair<E, V, M>> map, AttValPair<E, V, M> avp) {
    boolean result = false;

    if (map != null && map.size() > 0 && avp.isCanonical()) {
      final E attType = avp.getAttType();
      AttValPair<E, V, M> foundAVP = map.get(attType);
      boolean isFirst = true;
      while (foundAVP != null) {
        if (matches(avp, foundAVP)) {
          result = true;

          // remove the first from the ambiguity chain
          if (isFirst) {
            final AttValPair<E, V, M> nextAVP = foundAVP.nextAmbiguity();
            if (nextAVP != null) {
              map.put(attType, nextAVP);
            }
            else {
              // there are no more avps for this attType
              map.remove(attType);
            }
          }
          break;
        }
        isFirst = false;
        foundAVP = foundAVP.nextAmbiguity();
      }
    }

    return result;
  }

  private final boolean disconnect(Map<String, AttValPair<E, V, M>> map, AttValPair<E, V, M> avp, boolean normalize) {
    boolean result = false;

    if (map != null && map.size() > 0 && avp.hasOtherType()) {
      final String normAtt = normalize ? avp.getOtherType().toLowerCase() : avp.getOtherType();
      AttValPair<E, V, M> foundAVP = map.get(normAtt);
      boolean isFirst = true;
      while (foundAVP != null) {
        if (matches(avp, foundAVP)) {
          result = true;

          // remove the first from the ambiguity chain
          if (isFirst) {
            final AttValPair<E, V, M> nextAVP = foundAVP.nextAmbiguity();
            if (nextAVP != null) {
              map.put(normAtt, nextAVP);
            }
            else {
              // remove when empty
              map.remove(normAtt);
            }
          }

          foundAVP.remove();

          break;
        }
        isFirst = false;
        foundAVP = foundAVP.nextAmbiguity();
      }
    }

    return result;
  }

  private final boolean matches(AttValPair<E, V, M> avp1, AttValPair<E, V, M> avp2) {
    boolean result = (avp1 == avp2);

    if (!result) {
      // copies share the same attType instance
      if (avp1.getAttType() == avp2.getAttType()) {
        // and have the same normalized other type
        if ((!avp1.hasOtherType() && !avp2.hasOtherType()) ||
            (avp1.hasOtherType() && avp2.hasOtherType() && avp1.getOtherType().equalsIgnoreCase(avp2.getOtherType()))) {
          // check for matching values
          if (avp1.getValuesCount() == avp2.getValuesCount()) {
            if (avp1.getValuesCount() == 0 || avp1.getValues().equals(avp2.getValues())) {
              result = true;
            }
          }
        }
      }
    }

    return result;
  }

  private final Map<String, Attribute<E>> doClassify(AttValPair<E, V, M> avp) {
    // compute classifications
    final Map<String, Attribute<E>> classifications = getClassifications(avp);

    // update avp with classification results
    if (classifications != null) {
      while (avp != null) {
        final AttValPair<E, V, M> nextAVP = avp.nextAmbiguity();

        if (!avp.isCanonical() && avp.hasOtherType()) {
          // then avp needs to be classified
          Attribute<E> attribute = classifications.get(avp.getOtherType());

          if (attribute != null) {
            // set the classified attType
            avp.setAttType(attribute.getAttType());

            // insert an ambiguous avp for each ambiguous attribute
            if (attribute.isAmbiguous()) {
              for (attribute = attribute.nextAmbiguity(); attribute != null; attribute = attribute.nextAmbiguity()) {
                final AttValPair<E, V, M> ambAVP = new AttValPair<E, V, M>(avp);
                ambAVP.setAttType(attribute.getAttType());
                avp.addAmbiguity(ambAVP);
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

  private final Map<String, Attribute<E>> getClassifications(AttValPair<E, V, M> avp) {
    Map<String, Attribute<E>> result = null;

    if (attributeClassifier == null) return result;

    while (avp != null) {
      if (!avp.isCanonical() && avp.hasOtherType()) {
        final String otherType = avp.getOtherType();
        if (result == null || !result.containsKey(otherType)) {
          final Attribute<E> attribute = attributeClassifier.getAttribute(otherType);
          if (result == null) result = new LinkedHashMap<String, Attribute<E>>();
          result.put(otherType, attribute);
        }
      }
      avp = avp.nextAmbiguity();
    }

    return result;
  }
}
