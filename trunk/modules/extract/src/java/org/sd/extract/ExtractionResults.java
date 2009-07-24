/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.extract;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Container for extraction results from a document.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionResults {

  // extractionType -> { pathKey -> extractionGroup }
  private Map<String, Map<String, ExtractionGroup>> type2keyGroups;

  // extractionType -> disambiguator
  private Map<String, Disambiguator> disambiguators;

  private final AtomicBoolean computedHeadingOrganizer = new AtomicBoolean(false);
  private HeadingOrganizer _headingOrganizer;

  /**
   * Construct a new instance.
   */
  public ExtractionResults() {
    this.type2keyGroups = new LinkedHashMap<String, Map<String, ExtractionGroup>>();
    this.disambiguators = null;
    this._headingOrganizer = null;
  }

  /**
   * Add the extraction to this instance.
   */
  public void addExtraction(Extraction extraction) {
    final String extractionType = extraction.getExtractionType();
    final String pathKey = extraction.getPathKey();
    final String fixedPathKey = ExtractionUtil.fixPathKey(pathKey);

    if (computedHeadingOrganizer.get() && extractionType.equals(HeadingExtractor.EXTRACTION_TYPE)) {
      computedHeadingOrganizer.set(false);
      _headingOrganizer = null;
    }

    Map<String, ExtractionGroup> key2group = type2keyGroups.get(extractionType);
    if (key2group == null) {
      key2group = new LinkedHashMap<String, ExtractionGroup>();
      type2keyGroups.put(extractionType, key2group);
    }
    ExtractionGroup extractionGroup = key2group.get(fixedPathKey);
    if (extractionGroup == null) {
      extractionGroup = new ExtractionGroup(pathKey, fixedPathKey, extractionType);
      key2group.put(fixedPathKey, extractionGroup);
    }
    extractionGroup.addExtraction(extraction);
  }

  /**
   * If we don't have the disambiguator yet, record it.
   */
  public void addDisambiguatorIfNeeded(Extractor extractor) {
    final String extractionType = extractor.getExtractionType();
    final Disambiguator disambiguator = extractor.getDisambiguator();

    if (disambiguator != null) {
      if (disambiguators == null) {
        disambiguators = new HashMap<String, Disambiguator>();
      }
      if (disambiguators.get(extractionType) == null) {
        disambiguators.put(extractionType, extractor.getDisambiguator());
      }
    }
  }

  /**
   * After all extractions have been added, disambiguate the groups.
   * <p>
   * Called by the ExtractionRunner.
   */
  void disambiguate() {
    if (disambiguators == null) return;

    for (Map<String, ExtractionGroup> key2group : type2keyGroups.values()) {
      for (ExtractionGroup group : key2group.values()) {
        final Disambiguator disambiguator = disambiguators.get(group.getExtractionType());
        if (disambiguator != null) {
          disambiguator.disambiguate(group);
        }
      }
    }
  }

  /**
   * Get the types of extractions present in these results.
   */
  public Set<String> getExtractionTypes() {
    Set<String> result = null;

    if (type2keyGroups != null) {
      result = type2keyGroups.keySet();
    }

    return result;
  }

  /**
   * Get the extraction group for the given extraction type and path key.
   *
   * @return the group or null.
   */
  public ExtractionGroup getExtractionGroup(String extractionType, String pathKey) {
    ExtractionGroup result = null;

    final Map<String, ExtractionGroup> key2group = type2keyGroups.get(extractionType);
    if (key2group != null) {
      result = key2group.get(ExtractionUtil.fixPathKey(pathKey));
    }

    return result;
  }

  /**
   * Get the extraction groups for the given extraction type.
   *
   * @return the non-empty groups or null.
   */
  public Collection<ExtractionGroup> getExtractionGroups(String extractionType) {
    Collection<ExtractionGroup> result = null;

    final Map<String, ExtractionGroup> key2group = type2keyGroups.get(extractionType);
    if (key2group != null) {
      result = key2group.values();
    }

    return result;
  }

  /**
   * Get the extractions for the given extraction type.
   *
   * @return the non-empty extractions or null.
   */
  public List<Extraction> getExtractions(String extractionType) {
    List<Extraction> result = null;

    final Collection<ExtractionGroup> groups = getExtractionGroups(extractionType);
    if (groups != null) {
      result = new ArrayList<Extraction>();
      for (ExtractionGroup group : groups) {
        result.addAll(group.getExtractions());
      }
    }

    return result;
  }

  /**
   * Get the extractions for the given extraction types.
   *
   * @return the non-empty extractions or null.
   */
  public List<Extraction> getExtractions(String[] extractionTypes) {
    List<Extraction> result = null;

    for (String extractionType : extractionTypes) {
      final List<Extraction> extractions = getExtractions(extractionType);
      if (extractions != null) {
        if (result == null) result = new ArrayList<Extraction>();
        result.addAll(extractions);
      }
    }

    return result;
  }

  /**
   * Get all extractions in these results, sorted in document order.
   */
  public List<Extraction> getExtractions() {
    List<Extraction> result = null;

    final Set<String> extractionTypes = getExtractionTypes();
    if (extractionTypes != null) {
      for (String extractionType : extractionTypes) {
        final List<Extraction> typeExtractions = getExtractions(extractionType);
        if (typeExtractions != null) {
          if (result == null) result = new ArrayList<Extraction>();
          result.addAll(typeExtractions);
        }
      }

      if (result != null) {
        // sort results
        Collections.sort(result, DocumentOrderExtractionComparator.getInstance());
      }
    }

    return result;
  }

  /**
   * Utility method to get extractions incuding extrapolations where possible.
   * <p>
   * Note that the extraction pipeline that generated this results instance must
   * have a HeadingExtractor for there to be any extrapolation.
   * <p>
   * If there are no extrapolations, the unextrapolated groups will be returned.
   * <p>
   * If there are no results for the extraction type, null will be returned.
   */
  public ExtractionGroup[] getExtrapolatedGroups(String extractionType) {
    Collection<ExtractionGroup> groups = getExtractionGroups(extractionType);
    if (groups == null) return null;

    final ExtractionGroup[] result = new ExtractionGroup[groups.size()];

    final HeadingOrganizer headingOrganizer = getHeadingOrganizer();
    if (headingOrganizer != null) {
      int index = 0;
      for (ExtractionGroup group : groups) {
        result[index++] = group.buildExtrapolatedGroup(headingOrganizer);
      }
    }
    else {
      int index = 0;
      for (ExtractionGroup group : groups) {
        result[index++] = group;
      }
    }
    
    return result;
  }

  public HeadingOrganizer getHeadingOrganizer() {
    if (!computedHeadingOrganizer.get()) {
      synchronized (computedHeadingOrganizer) {
        final List<Extraction> headings = getExtractions(HeadingExtractor.EXTRACTION_TYPE);
        if (headings != null) {
          _headingOrganizer = new HeadingOrganizer(headings);
        }
        computedHeadingOrganizer.set(true);
      }
    }
    return _headingOrganizer;
  }
}
