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


import org.sd.extract.datetime.DateTimeExtractor;
import org.sd.extract.datetime.DateTimeFlags;
import org.sd.extract.datetime.DateTimeInterpretation;
import org.sd.util.LineBuilder;
import org.sd.util.StatsAccumulator;
import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Container for extractions with the same path key.
 * <p>
 * @author Spence Koehler
 */
public class ExtractionGroup {

  private static final AtomicLong NEXT_GROUP_ID = new AtomicLong(0L);

  private long groupId;
  private String pathKey;
  private String fixedPathKey;
  private String extractionType;
  private List<Extraction> extractions;
  private StatsAccumulator pathIndexStats;
  private Set<String> _structureKeys;

  public ExtractionGroup(String pathKey, String fixedPathKey, String extractionType) {
    this.groupId = NEXT_GROUP_ID.getAndIncrement();
    this.pathKey = pathKey;
    this.fixedPathKey = fixedPathKey;
    this.extractionType = extractionType;
    this.extractions = null;
    this.pathIndexStats = new StatsAccumulator();
  }

  protected ExtractionGroup(ExtractionGroup other) {
    this(other, other.getExtractions());
  }

  protected ExtractionGroup(ExtractionGroup other, List<Extraction> extractions) {
    this.groupId = NEXT_GROUP_ID.getAndIncrement();
    this.pathKey = other.pathKey;
    this.fixedPathKey = other.fixedPathKey;
    this.extractionType = other.extractionType;
    this.extractions = null;

    this.pathIndexStats = new StatsAccumulator();

    for (Extraction e : extractions) {
      addExtraction(e, true);
    }
  }

  /**
   * Add the extraction to this group if it has the correct path key.
   * 
   * @return true if added; otherwise, false.
   */
  public boolean addExtraction(Extraction extraction) {
    return addExtraction(extraction, false);
  }

  /**
   * Unconditionally add the extraction to this group or enforce
   * having the correct path key.
   * 
   * @return true if added; otherwise, false.
   */
  protected final boolean addExtraction(Extraction extraction, boolean unconditionally) {
    boolean result = false;

    final String eKey = extraction.getPathKey();
    if (eKey != null && (unconditionally || acceptPathKey(eKey) && extractionType.equals(extraction.getExtractionType()))) {
      if (extractions == null) extractions = new ArrayList<Extraction>();
      extractions.add(extraction);

      extraction.setGroupId(groupId);

      final int curPathIndex = extraction.getPathIndex();
      pathIndexStats.add(curPathIndex);

      _structureKeys = null;

      result = true;
    }

    return result;
  }

  public boolean acceptPathKey(String pathKey) {
    return fixedPathKey.equals(ExtractionUtil.fixPathKey(pathKey));
  }

  public String getPathKey() {
    return pathKey;
  }

  public Set<String> getStructureKeys() {
    if (_structureKeys == null) {
      _structureKeys = new HashSet<String>();
      for (Extraction extraction : extractions) {
        final Interpretation interp = extraction.getInterpretation();
        if (interp != null) {
          _structureKeys.add(interp.getStructureKey());
        }
      }
    }
    return _structureKeys;
  }

  public int getMinPathIndex() {
    return (int)(pathIndexStats.getMin() + 0.5);
  }

  public int getMaxPathIndex() {
    return (int)(pathIndexStats.getMax() + 0.5);
  }

  // get average number of (non-empty) paths between extractions.
  public int getSpread() {
    int result = 0;

    final long num = pathIndexStats.getN() - 1;
    if (num > 0L) {
      result = (int)(getTotalSpread() / num);
    }

    return result;
  }

  // get total number of (non-empty) paths between (first n-1) extractions.
  public int getTotalSpread() {
    int result = 0;

    // get average number of (non-empty) paths between extractions.
    if (extractions != null && extractions.size() > 0) {
      final TextContainer textContainer = extractions.get(0).getDocText().getTextContainer();
      int maxPath = (extractions.size() == 1) ? textContainer.getCount() : (int)pathIndexStats.getMax();
      result = textContainer.numNonEmptyPaths((int)pathIndexStats.getMin(), maxPath);
    }

    return result;
  }

  public int size() {
    return (int)pathIndexStats.getN();
  }

  public String getExtractionType() {
    return extractionType;
  }

  /**
   * Get this extraction group's extractions.
   */
  public List<Extraction> getExtractions() {
    return extractions;
  }

  /**
   * Get this extraction group's extractions along with extrapolations according
   * to common headings.
   */
  public ExtractionGroup buildExtrapolatedGroup(HeadingOrganizer headingOrganizer) {
    if (extractions == null) return null;

    final List<Extraction> result = new ArrayList<Extraction>();

    if (headingOrganizer.hasKeyHeading(pathKey)) {
      buildExtrapolatedKeyGroup(headingOrganizer, result);
    }
    else {
      buildExtrapolatedIndexGroup(headingOrganizer, result);
    }

    return new ExtractionGroup(this, result);
  }

  /**
   * Extrapolate by path key covered by headings.
   * <p>
   * Here we are trying to create a group out of paths that share a common key and
   * are under common headings. This should capture fields in records in the form of
   * lists, table columns or rows, etc.
   */
  private final void buildExtrapolatedKeyGroup(HeadingOrganizer headingOrganizer, List<Extraction> result) {
    // find table (row/col) heading(s) for this group.
    // get heading stack applicable to the highest table (row/col) heading(s)
    // make a new stack adding the table heading(s) to the heading stack
    //
    // iterate through paths from the table (row/col) heading(s) to end of table(s) (row/col) accounting for results
    //    final XmlLite.Tag tableTag = highest heading's tr (row) or table (col)
    //    List<DocText> docTexts = textContainer.getDocTexts(tableTag);
    //    if docText is the next group extraction, add the group extraction and inc
    //    else if lowest heading's path is a prefix to the docText path, extrapolate

    final HeadingOrganizer.KeyHeadingStack keyHeadingStack = headingOrganizer.getKeyHeadings(pathKey);
    if (keyHeadingStack != null) {
      final XmlLite.Tag tag = keyHeadingStack.getTag();
      if (tag != null) {
        final Iterator<Extraction> exIter = extractions.iterator();
        Extraction curExtraction = exIter.next();
        DocText exDocText = curExtraction.getDocText();
        final TextContainer textContainer = exDocText.getTextContainer();

        final List<DocText> allDocTexts = textContainer.getDocTexts(tag);
        if (allDocTexts != null) {
          for (DocText docText : allDocTexts) {
            if (exDocText == docText) {
              result.add(curExtraction);
              curExtraction.setHeadingStack(keyHeadingStack);

              curExtraction = exIter.hasNext() ? exIter.next() : null;
              exDocText = (curExtraction != null) ? curExtraction.getDocText() : null;
            }
            else if (pathKey.equals(docText.getPathKey())) {
              final Extraction extrapolatedExtraction = buildExtrapolatedExtraction(docText, keyHeadingStack);
              result.add(extrapolatedExtraction);
            }
          }
        }
        else {
          while (curExtraction != null) {
            curExtraction.setHeadingStack(keyHeadingStack);
            result.add(curExtraction);
            curExtraction = exIter.hasNext() ? exIter.next() : null;
          }
        }
      }
    }
  }

  /**
   * Extrapolate by path indeces covered by headings.
   * <p>
   * Here we are trying to create a group out of consecutive paths under headings
   * regardless of keys. This should capture flowing text records like blog snippets,
   * etc.
   */
  private final void buildExtrapolatedIndexGroup(HeadingOrganizer headingOrganizer, List<Extraction> result) {

    int lastTrueExtractionIndex = -1;
    HeadingOrganizer.IndexHeadingStack headingStack = null;
    HeadingOrganizer.IndexHeadingStack lastHeadingStack = null;
    TextContainer textContainer = null;

    for (Extraction trueExtraction : extractions) {
      final DocText docText = trueExtraction.getDocText();
      if (textContainer == null) textContainer = docText.getTextContainer();

      final int trueExtractionIndex = docText.getPathIndex();
      headingStack = headingOrganizer.getHeadings(docText);

      if (headingStack != null) {
        if (lastHeadingStack != headingStack && lastHeadingStack != null) {
          // finish extrapolations to the end of the last
          lastTrueExtractionIndex = addExtrapolatedExtractions(result, textContainer, lastTrueExtractionIndex + 1, lastHeadingStack.getEndIndex(), lastHeadingStack);
        }

        // add extrapolated extractions from (max(headingStack.getStartIndex(), lastTrueExtractionIndex) + 1) to (trueExtraction.getDocText().getPathIndex() - 1)
        int startIndex = headingStack.getStartIndex();
        if (lastTrueExtractionIndex > startIndex) startIndex = lastTrueExtractionIndex;

        lastTrueExtractionIndex = addExtrapolatedExtractions(result, textContainer, startIndex + 1, trueExtractionIndex, headingStack);

        trueExtraction.setHeadingStack(headingStack);
        lastHeadingStack = headingStack;
      }
      else {
        lastTrueExtractionIndex = trueExtractionIndex;
      }

      // add true extraction
      result.add(trueExtraction);
    }

    // add extrapolated extractions from (lastTrueExtractionIndex + 1) to (headingStack.getEndIndex() - 1)
    if (headingStack != null) {
      addExtrapolatedExtractions(result, textContainer, lastTrueExtractionIndex + 1, headingStack.getEndIndex(), headingStack);
    }
  }

  public String getFieldsString() {
    final LineBuilder result = new LineBuilder();

    result.
      append(extractionType).
      append(pathKey);

    return result.toString();
  }

  private final Extraction buildExtrapolatedExtraction(DocText docText, HeadingOrganizer.HeadingStack headingStack) {
    final Extraction result = new Extraction(extractionType, docText, 0.0, null);

    result.setHeadingStack(headingStack);
    result.setExtrapolated(true);

    return result;
  }

  private final int addExtrapolatedExtractions(List<Extraction> result, TextContainer textContainer, int startIndex, int endIndex, HeadingOrganizer.HeadingStack headingStack) {
    if (endIndex < 0) {
      // account for "to end of document" case
      endIndex = textContainer.getCount();
    }

    if (!textContainer.isCaching()) {
      return endIndex;
    }

    // get DocTexts from textContainer that share this group's pathKey from startIndex (inclusive) to endIndex (exclusive)
    for (int pathIndex = startIndex; pathIndex < endIndex; ++pathIndex) {
      final DocText docText = textContainer.getDocText(pathIndex);

      if (docText != null && !"".equals(docText.getString()) && pathKey.equals(docText.getPathKey())) {
        final Extraction extrapolatedExtraction = buildExtrapolatedExtraction(docText, headingStack);
        result.add(extrapolatedExtraction);
      }
    }

    return endIndex;
  }

  /**
   * Get snippets based on this group's extractions.
   * <p>
   * Note that if the text container is not caching data, null will be returned.
   */
  public final List<ExtractionSnippet> getSnippets(ExtractionResults results) {
    final TextContainer textContainer = getTextContainer();
    if (textContainer == null || !textContainer.isCaching()) return null;

    List<ExtractionSnippet> result = null;

    // algorithm:
    //   find the deepest common tag (dct) for all of the extractions.
    //   divide the children of the dct into snippets based on the presence
    //   of an extraction and taking headings and tag changes into account.
    //
    //   if only 1 tag stack, use other groups to find the dct by:
    //   - finding deepestCommonTag across groups
    //   - walking down this group's tag stack until
    //     - all other group tags differ  or
    //     - stepping down would leave only 1 text path
//I'm here...
    //   - limit the paths included based on hints from other groups

    // universal vars
    XmlLite.Data dct = null;
    final List<TagStack> tagStacks = getTagStacks();

    // vars for single-post conditions
    boolean limitPaths = false;
    DateTimeFlags dtFlags = null;

    // detect single-post
    if (tagStacks.size() == 1 && results != null) {
      final List<TagStack> otherTagStacks = getOtherTagStacks(results);
      dtFlags = new DateTimeFlags((DateTimeInterpretation)extractions.get(0).getInterpretation());

      if (otherTagStacks != null && otherTagStacks.size() > 0) {
        final boolean[] backedOff = new boolean[]{false};
        dct = getDeepestUniqueTag(tagStacks.get(0), otherTagStacks, backedOff);
        limitPaths = backedOff[0];
      }
    }

    if (dct == null) {
      // multi-post conditions apply
      dct = getDeepestCommonTag(tagStacks);
    }

    // collect paths into snippets
    if (dct != null) {
      result = new ArrayList<ExtractionSnippet>();
      ExtractionSnippet curSnippet = null;
      ExtractionSnippet lastSnippet = null;
      final List<DocText> docTexts = textContainer.getDocTexts(dct);

      //iterate through snippets, adding one at a time to an ExtractionSnippet instance,
      for (DocText docText : docTexts) {
        if (curSnippet == null) {
          if ("".equals(docText.getString())) {
            // always start at a non-empty boundary
            continue;
          }
          curSnippet = new ExtractionSnippet(this, docText);
        }
        else {
          curSnippet = curSnippet.addDocText(docText);

          //
          // NOTE: the following check is AFTER adding the docText, because breaking
          //       tags (i.e. "hr", "br") are detected just BEFORE they are returned
          //       in a tagStack!
          //
          if (limitPaths) {
            // we're only grabbing one snippet for the document. check to see if it is time to stop.
            //
            // current heuristic:
            //   it's time to stop if we see an "hr" or "br" after completing the date
            //   where completing the date is including a "time" for a "date only"
            if (isTimeToBreak(docText, dtFlags)) {
              break;
            }
          }
        }

        if (lastSnippet != curSnippet) {
          result.add(curSnippet);
          lastSnippet = curSnippet;
        }
      }
    }

    return result;
  }

  /**
   * Heuristics for when it is time to break out of collecting docTexts for a post.
   */
  private final boolean isTimeToBreak(DocText docText, DateTimeFlags dtFlags) {
    boolean result = false;

    if (!dtFlags.hasDateAndTime()) {
      final Extraction e = docText.getExtraction(DateTimeExtractor.EXTRACTION_TYPE);
      if (e != null) {
        final DateTimeFlags curDtFlags = new DateTimeFlags((DateTimeInterpretation)e.getInterpretation());
        if (curDtFlags.hasTimeOnly()) {
          dtFlags.or(curDtFlags);
        }
      }
    }
    else {
      final TagStack curTagStack = docText.getTagStack();
      if (curTagStack.getSavedTags() != null) {  // NOTE: breaking tags are found int savedTags, not in the stack's path tags
        result = true;
      }
    }

    return result;
  }

  private final TextContainer getTextContainer() {
    TextContainer result = null;

    for (Extraction extraction : extractions) {
      final DocText docText = extraction.getDocText();
      result = docText.getTextContainer();
      break;
    }

    return result;
  }

  private final List<TagStack> getTagStacks() {
    final List<TagStack> result = new ArrayList<TagStack>();

    for (Extraction extraction : extractions) {
      final DocText docText = extraction.getDocText();
      result.add(docText.getTagStack());
    }

    return result;
  }

  /**
   * Get a representative tag stack for this group.
   * <p>
   * NOTE: the representative stack will be taken from the first extraction.
   */
  public final TagStack getTagStack() {
    TagStack result = null;

    if (extractions != null && extractions.size() > 0) {
      final Extraction extraction = extractions.get(0);
      final DocText docText = extraction.getDocText();
      result = docText.getTagStack();
    }

    return result;
  }

  /**
   * Get a representative tag stack from each other group in the results
   * matching this group's extraction type.
   */
  private final List<TagStack> getOtherTagStacks(ExtractionResults results) {
    List<TagStack> result = null;

    final Collection<ExtractionGroup> allGroups = results.getExtractionGroups(extractionType);
    if (allGroups != null && allGroups.size() > 1) {
      result = new ArrayList<TagStack>();

      for (ExtractionGroup group : allGroups) {
        if (!intersects(group)) {
          final TagStack curTagStack = group.getTagStack();
          if (curTagStack != null) result.add(curTagStack);
        }
      }
    }

    return result;
  }

  /**
   * Determine whether this group intersects another.
   */
  private final boolean intersects(ExtractionGroup other) {
    final int myMin = (int)pathIndexStats.getMin();
    final int myMax = (int)pathIndexStats.getMax();
    final int otherMin = (int)(other.pathIndexStats.getMin());
    final int otherMax = (int)(other.pathIndexStats.getMax());

    return !((myMax < otherMin) || (otherMax < myMin));
  }

  private final XmlLite.Tag getDeepestUniqueTag(TagStack tagStack, List<TagStack> otherTagStacks, boolean[] backedOff) {
    final LinkedList<TagStack> allStacks = new LinkedList<TagStack>(otherTagStacks);
    allStacks.addFirst(tagStack);

    // find the deepest common tag across stacks
    XmlLite.Tag dct = getDeepestCommonTag(allStacks);
    allStacks.removeFirst();

    if (dct != null) {

      // walk down tagStack until a tag is not present in any other stacks.
      final int maxPos = tagStack.depth();
      int tagPos = tagStack.hasTag(dct);

      for (++tagPos; tagPos < maxPos; ++tagPos) {
        dct = tagStack.getTag(tagPos);
        if (!anyHasTag(dct, allStacks)) {
          break;
        }
      }

      // if we walked so far as to only include a single text node, back off.
      if (tagPos == maxPos) {
        dct = null;
      }
      else {
        final TextContainer textContainer = getTextContainer();
        final List<DocText> docTexts = textContainer.getDocTexts(dct);

        if (docTexts.size() <= 1) {
          dct = tagStack.getTag(--tagPos);  // back off
          backedOff[0] = true;
        }
      }
    }

    return dct;
  }

  /**
   * Determine whether any of the stacks has the tag. Remove stacks that don't
   * have the tag.
   */
  private final boolean anyHasTag(XmlLite.Tag tag, LinkedList<TagStack> stacks) {
    boolean result = false;
    
    for (Iterator<TagStack> iter = stacks.iterator(); iter.hasNext(); ) {
      final TagStack stack = iter.next();
      if (stack.hasTag(tag) < 0) iter.remove();
      else result = true;  // continue searching to continue removing.
    }

    return result;
  }

  private final XmlLite.Tag getDeepestCommonTag(List<TagStack> tagStacks) {
    XmlLite.Tag result = null;

    if (tagStacks.size() == 1) {
      final TagStack tagStack = tagStacks.get(0);
      final int bodyPos = tagStack.hasTag("body");
      if (bodyPos >= 0) {
        result = tagStack.getTags().get(bodyPos);
      }
    }
    else {
      final List<Iterator<XmlLite.Tag>> iters = new ArrayList<Iterator<XmlLite.Tag>>();
      for (TagStack tagStack : tagStacks) {
        iters.add(tagStack.getTags().iterator());
      }

      while (allHaveNext(iters)) {
        final XmlLite.Tag curMatch = getNextIfMatches(iters);
        if (curMatch != null) {
          result = curMatch;  // this is a deeper match. keep it.
        }
        else {
          break;  // don't match anymore. time to go.
        }
      }
    }
    
    return result;
  }

  private final boolean allHaveNext(List<Iterator<XmlLite.Tag>> iters) {
    for (Iterator<XmlLite.Tag> iter : iters) {
      if (!iter.hasNext()) return false;
    }
    return true;
  }

  private final XmlLite.Tag getNextIfMatches(List<Iterator<XmlLite.Tag>> iters) {
    XmlLite.Tag result = null;
    for (Iterator<XmlLite.Tag> iter : iters) {
      final XmlLite.Tag tag = iter.next();
      if (result == null) {
        result = tag;
      }
      else if (result != tag) {
        return null;
      }
    }
    return result;
  }
}
