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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <p>
 * Note that the container itself can be ambiguous (or have multiple ambiguous
 * manifestations).
 *
 * @author Spence Koehler
 */
public class AvpContainer <E extends Canonical, V, M> extends AbstractAmbiguousEntity<AvpContainer<E, V, M>> {

  private AttributeClassifier<E> attributeClassifier;
  private Map<E, AttValPair<E, V, M>> strongAVPs;
  private Map<String, AttValPair<E, V, M>> unclassifiedAVPs;
  private Map<String, AttValPair<E, V, M>> classifiedAVPs;
  private M metaData;

  // map from enum to all weak names that have mapped to it for tidying up classifiedAVPs
  private Map<E, Set<String>> classifiedAtts;

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
    this.classifiedAtts = null;
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
        addClassifiedAtt(att, normAtt);

        final AttValPair<E, V, M> avp = strongAVPs.get(att);

        if (avp != null) {
          if (attribute.isAmbiguous()) {
            // create shallow copies with a new ambiguity chain for the result
            result = avp.copyAmbiguityChain(result);

            // store in classifiedAVPs
            if (result != null) {
              if (classifiedAVPs == null) classifiedAVPs = new LinkedHashMap<String, AttValPair<E,V,M>>();
              classifiedAVPs.put(normAtt, result);
            }
          }
          else {
            // no need to replicate into classifiedAVPs when attribute is unambiguous
            result = avp;
          }
        }

        // inc to next attribute classification ambiguity
        attribute = attribute.nextAmbiguity();
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
   * An unambiguous avp when added will be inserted to the front of the
   * ambiguity chain. To add to the end, use "addNext" instead.
   * <p>
   * NOTE: this container stores copies of the submitted avp's.
   *
   * @return the added avp.
   */
  public AttValPair<E, V, M> add(AttValPair<E, V, M> avp) {
    doAdd(avp, false, false);
    return avp;
  }

  /**
   * Adopt the avp into this container (by setting avp's container
   * to be this.).
   */
  public void adopt(AttValPair<E, V, M> avp) {
    avp.setContainer(this);
  }

  /**
   * Convenience method to add a generic AttValPair.
   * <p>
   * Note that as an unambiguous avp when added, the new avp will be inserted
   * to the front of the ambiguity chain. To add to the end, use "addNext"
   * instead.
   */
  public AttValPair<E, V, M> add(String attType, V value) {
    return this.add(new AttValPair<E, V, M>(attType, value));
  }

  public AttValPair<E, V, M> addNext(AttValPair<E, V, M> avp) {
    doAdd(avp, false, true);
    return avp;
  }

  public AttValPair<E, V, M> addNext(String attType, V value) {
    return this.addNext(new AttValPair<E, V, M>(attType, value));
  }

  /**
   * Set the attribute/value pair, overriding any existing matches
   * in this container (instead of adding as ambiguous).
   */
  public void override(AttValPair<E, V, M> avp) {
    doAdd(avp, true, false);
  }

  private final void doAdd(AttValPair<E, V, M> avp, boolean override, boolean next) {
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
        doAddInstance(avp, classifications, next);
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

    if (avp.isAmbiguous()) {
      for (avp = avp.firstAmbiguity(); avp != null; avp = avp.nextAmbiguity()) {
        avp.setContainer(null);
        doRemoveAll(avp);
      }
    }
    else {
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
              disconnect(classifiedAVPs, avp, true, getClassifiedAtts(avp.getAttType()));
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
        attribute = attribute.nextAmbiguity();
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

  private final void doAddInstance(AttValPair<E, V, M> avp, Map<String, Attribute<E>> classifications, boolean next) {

    // update strongAVPs
    final E attType = avp.getAttType();
    if (attType != null && attType.isCanonical()) {
      if (strongAVPs == null) strongAVPs = new LinkedHashMap<E, AttValPair<E, V, M>>();
      final AttValPair<E, V, M> curAVP = strongAVPs.get(attType);

      final AttValPair<E, V, M> copyToAdd = new AttValPair<E, V, M>(avp, false);
      copyToAdd.setContainer(this);

      if (curAVP == null) {
        strongAVPs.put(attType, copyToAdd);
      }
      else {
        if (!avp.isAmbiguous() && !next) {
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

      final AttValPair<E, V, M> copyToAdd = new AttValPair<E, V, M>(avp, false);
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
      disconnect(classifiedAVPs, avp, true, getClassifiedAtts(avp.getAttType()));
    }
    else {
      // disconnect from unclassified
      disconnect(unclassifiedAVPs, avp, false, null);
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

  private final boolean disconnect(Map<String, AttValPair<E, V, M>> map, AttValPair<E, V, M> avp, boolean normalize, Set<String> altAtts) {
    boolean result = false;

    if (map != null && map.size() > 0 && avp.hasOtherType()) {
      if (altAtts != null) {
        for (String normAtt : altAtts) {
          if (disconnectAux(map, avp, normAtt)) {
            result = true;
          }
        }
      }
      else {
        final String normAtt = normalize ? avp.getOtherType().toLowerCase() : avp.getOtherType();
        if (disconnectAux(map, avp, normAtt)) {
          result = true;
        }
      }
    }

    return result;
  }

  private final boolean disconnectAux(Map<String, AttValPair<E, V, M>> map, AttValPair<E, V, M> avp, String normAtt) {
    boolean result = false;

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
          final String otherType = avp.getOtherType();
          final String normAtt = otherType.toLowerCase();
          Attribute<E> attribute = classifications.get(otherType);

          if (attribute != null) {
            // set the classified attType
            avp.setAttType(attribute.getAttType());

            // store in classifiedAtts
            addClassifiedAtt(attribute.getAttType(), normAtt);

            // insert an ambiguous avp for each ambiguous attribute
            if (attribute.isAmbiguous()) {
              for (attribute = attribute.nextAmbiguity(); attribute != null; attribute = attribute.nextAmbiguity()) {
                final AttValPair<E, V, M> ambAVP = new AttValPair<E, V, M>(avp, true);
                ambAVP.setAttType(attribute.getAttType());
                addClassifiedAtt(attribute.getAttType(), normAtt);
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

  private final void addClassifiedAtt(E att, String normAtt) {
    if (classifiedAtts == null) classifiedAtts = new HashMap<E, Set<String>>();
    Set<String> normAtts = classifiedAtts.get(att);
    if (normAtts == null) {
      normAtts = new HashSet<String>();
      classifiedAtts.put(att, normAtts);
    }
    normAtts.add(normAtt);
  }

  private final Set<String> getClassifiedAtts(E att) {
    Set<String> result = null;

    if (att != null && classifiedAtts != null) {
      result = classifiedAtts.get(att);
    }

    return result;
  }

  ////////
  ///
  /// Implement AmbiguousEntity interface
  ///

  /** Simple typecasting helper auxiliary for getting the next ambiguity. */
  public AvpContainer<E, V, M> nextAmbiguity() {
    return (AvpContainer<E, V, M>)getNextAmbiguity();
  }

  /** Simple typecasting helper auxiliary for getting the first ambiguity. */
  public AvpContainer<E, V, M> firstAmbiguity() {
    return (AvpContainer<E, V, M>)getFirstAmbiguity();
  }

  /**
   * Safely and efficiently typecast this to an AvpContainer.
   */
  public AvpContainer<E, V, M> getEntity() {
    return this;
  }

  /** Determine whether this ambiguous entity matches (is a duplicate of) the other */
  public boolean matches(AmbiguousEntity<AvpContainer<E, V, M>> other) {
    boolean result = (this == other);
    if (!result && other != null) {
      final AvpContainer<E, V, M> otherAvpC = other.getEntity();
      if (this.attributeClassifier == otherAvpC.attributeClassifier ||
          (this.attributeClassifier != null && this.attributeClassifier.equals(otherAvpC.attributeClassifier))) {
        if (matches(this.strongAVPs, otherAvpC.strongAVPs) && matches(this.unclassifiedAVPs, otherAvpC.unclassifiedAVPs)) {
          result = true;
        }
      }
    }
    return result;
  }

  private static <X, E extends Canonical, V, M> boolean matches(Map<X, AttValPair<E, V, M>> map1, Map<X, AttValPair<E, V, M>> map2) {
    boolean result = (map1 == map2);

    if (!result && map1 != null && map2 != null && map1.size() == map2.size()) {
      result = true;
      for (Map.Entry<X, AttValPair<E, V, M>> entry1 : map1.entrySet()) {
        AttValPair<E, V, M> avp1 = entry1.getValue();
        AttValPair<E, V, M> avp2 = map2.get(entry1.getKey());

        if (avp1 == avp2) continue;
        else if (avp1 == null || avp2 == null) {
          result = false;
        }
        else {
          while (result && avp1 != null && avp2 != null) {
            if (!avp1.matches(avp2)) {
              result = false;
              break;
            }
            else {
              avp1 = avp1.nextAmbiguity();
              avp2 = avp2.nextAmbiguity();
            }
          }
          if (result && (avp1 != null || avp2 != null)) result = false;  // one has extras
        }
        if (!result) break;
      }
    }

    return result;
  }

  ///
  /// end of AmbiguousEntity interface implementation
  ///
  ////////
}
