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


import org.sd.nlp.Normalizer;
import org.sd.util.LineBuilder;
import org.sd.util.tree.Tree;
import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extractor to pull contextual strings from a document surrounding (before)
 * pivot extractions. Note that the pivot extractors must set a property on their
 * extracted text nodes with key="extracted", value=[extractionType].
 * <p>
 * @author Spence Koehler
 */
public class PivotContextExtractor implements Extractor {
  
  private String extractionType;
  private ExtractionPipeline pivotExtractors;

  private int maxNumBack;
  private boolean includeBackHeading;
  private int maxNumForward;

  private Disambiguator _disambiguator;
  private final Object disambiguatorMutex = new Object();

  public PivotContextExtractor(String extractionType, ExtractionPipeline pivotExtractors,
                               int maxNumBack, boolean includeBackHeading) {
    this(extractionType, pivotExtractors, maxNumBack, includeBackHeading, 0);
  }

  public PivotContextExtractor(String extractionType, ExtractionPipeline pivotExtractors,
                               int maxNumBack, boolean includeBackHeading, int maxNumForward) {

    this.extractionType = extractionType;
    this.pivotExtractors = pivotExtractors;

    this.maxNumBack = maxNumBack;
    this.includeBackHeading = includeBackHeading;
    this.maxNumForward = maxNumForward;

    this._disambiguator = null;
  }

  /**
   * Get this extractor's type designator.
   */
  public String getExtractionType() {
    return extractionType;
  }

  /**
   * Determine whether this extractor needs the doc text's text container to
   * cache doc text instances.
   */
  public boolean needsDocTextCache() {
    return true;
  }

  /**
   * Determine whether the given doc text should be accepted for extraction.
   */
  public boolean shouldExtract(DocText docText) {
    return pivotExtractors.shouldExtract(docText);
  }

  /**
   * Perform the extraction on the doc text.
   * 
   * @param docText  The docText to extract from.
   * @param die      Trigger to halt processing. Needs to be monitored and
   *                 obeyed; but can also be set from within the implementation.
   *                 Use with care!
   *
   * @return one or more extractions or null.
   */
  public List<Extraction> extract(DocText docText, AtomicBoolean die) {
    List<Extraction> result = null;

    final List<Extraction> extractions = pivotExtractors.extract(docText, die);

    if (die != null && die.get()) return null;

    if (extractions != null) {

      // find the strings that have the extractions.
      final List<Tree<XmlLite.Data>> textNodes = XmlTreeHelper.getTextNodes(docText.getXmlNode());
      final int[] extIndex = findExtractionIndexes(extractions, textNodes);
      if (extIndex != null) {
        result = new ArrayList<Extraction>();

        int lastExtIndex = -1;
        int nextExtIndex = extIndex.length;
        final int numTextNodes = textNodes.size();

        final TextContainer textContainer = docText.getTextContainer();
        final int pathIndex = docText.getPathIndex();
        final TagStack extTagStack = docText.getTagStack();
        Integer baseHeadingStrength = HeadingExtractor.getHeadingProperty(docText);
        if (baseHeadingStrength == null) baseHeadingStrength = 0;
          
        //
        // grab context backward from each extraction
        //
        // first from within the doc text's data
        //
        for (int extOffset = 0; extOffset < extIndex.length; ++extOffset) {
          final LinkedList<String> strings = new LinkedList<String>();
          final int curExtIndex = extIndex[extOffset];
          int stringCountBack = 0;
          int stringCountForward = 0;
          boolean doneBack = (extOffset > 0);
          boolean doneForward = false;

          // compute deepest "constant" tag node instance between extraction's node and prior node. stop when tag node instance changes.
          final Tree<XmlLite.Data> constantNodeBack = findDeepestCommonNode(textNodes, curExtIndex);

          Integer headingBase = null;

          // get extraction data from strings after an extraction in the doc text's node.
          for (int textNodeIndex = curExtIndex; textNodeIndex < numTextNodes && stringCountForward < maxNumForward; ++textNodeIndex) {
            final Tree<XmlLite.Data> textNode = textNodes.get(textNodeIndex);

            boolean isHeading = false;
            Integer headingStrength = getHeadingStrength(textNode);

            if (textNodeIndex == curExtIndex) {
              headingBase = headingStrength;
            }
            else {
              isHeading = isHeading(headingStrength, headingBase);

              if (isHeading) {
                // we're done going forward.
                doneForward = true;
                break;
              }

              if (!isHeading && constantNodeBack != null && !constantNodeBack.isAncestor(textNode)) {
                doneForward = true;
                break;
              }

              final String nodeString = buildNodeString(docText, textNode, getDelta(headingStrength, headingBase));
              strings.addLast(nodeString);
              ++stringCountForward;
            }
          }

          // get extraction data from strings prior to an extraction in the doc text's node.
          for (int textNodeIndex = curExtIndex; textNodeIndex > lastExtIndex && stringCountBack < maxNumBack; --textNodeIndex) {
            final Tree<XmlLite.Data> textNode = textNodes.get(textNodeIndex);

            boolean isHeading = false;
            Integer headingStrength = getHeadingStrength(textNode);

            if (textNodeIndex == curExtIndex) {
              headingBase = headingStrength;
            }
            else {
              isHeading = isHeading(headingStrength, headingBase);

              if (isHeading && !includeBackHeading) {
                // we're done going back.
                doneBack = true;
                break;
              }

              if (!isHeading && constantNodeBack != null && !constantNodeBack.isAncestor(textNode)) {
                doneBack = true;
                break;
              }

              final String nodeString = buildNodeString(docText, textNode, getDelta(headingStrength, headingBase));
              strings.addFirst(nodeString);
              ++stringCountBack;

              // if textNode is a heading, don't go back any farther.
              if (isHeading) {
                doneBack = true;
                break;
              }
            }
          }

          //
          // continue grabbing context forward from the extractions
          //
          // next from following doc texts
          //
          if (!doneForward && stringCountForward < maxNumForward) {
            // get context data from following node(s)
            XmlLite.Tag commonTagBack = null;

            for (int pathOffset = 1; stringCountForward < maxNumForward; ++pathOffset) {
              final DocText nextDocText = textContainer.getDocText(pathIndex + pathOffset);
              if (nextDocText == null) {
                if (!textContainer.isCaching()) {
                  System.err.println("*** WARNING: non-caching textContainer being used in caching operation!");
                }

                break;
              }

              // if heading or empty, then quit; otherwise, add it.
              final Integer headingStrength = HeadingExtractor.getHeadingProperty(nextDocText);
              final boolean isHeading = isHeading(headingStrength, baseHeadingStrength);

              if (!isHeading) {
                // if the path is no longer in the same common subranch as the extractions and their predecessor, break out.
                final TagStack nextTagStack = nextDocText.getTagStack();
                if (pathOffset == 1) {
                  // compute common tag
                  commonTagBack = nextTagStack.getDeepestCommonTag(extTagStack);
                }
                else {
                  // apply common tag
                  if (commonTagBack != null && nextTagStack.hasTag(commonTagBack) < 0) {
                    if (strings.size() < 3) {
                      // recompute common tag. we don't have enough data yet.
                      // we get here, i.e., when data is in 2 cols and multiple rows of a table.
                      commonTagBack = nextTagStack.getDeepestCommonTag(extTagStack);
                    }
                    else {
                      break;
                    }
                  }
                }
              }

              final Tree<XmlLite.Data> xmlNode = nextDocText.getXmlNode();
              for (Iterator<Tree<XmlLite.Data>> iter = xmlNode.iterator(Tree.Traversal.DEPTH_FIRST);
                   iter.hasNext() && stringCountForward < maxNumForward; ) {
                final Tree<XmlLite.Data> curNode = iter.next();
                final XmlLite.Text nodeText = curNode.getData().asText();
                if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
                  final String nodeString = buildNodeString(nextDocText, curNode, getDelta(headingStrength, baseHeadingStrength));
                  strings.addLast(nodeString);
                  ++stringCountForward;
                }
                if (isHeading) break;
              }

              if (isHeading) break;
            }
          }


          //
          // continue grabbing context backward from the extractions
          //
          // next from prior doc texts
          //
          if (!doneBack && stringCountBack < maxNumBack) {
            // get context data from prior node(s)
            XmlLite.Tag commonTagBack = null;

            for (int pathOffset = 1; stringCountBack < maxNumBack && pathIndex - pathOffset >= 0; ++pathOffset) {
              final DocText priorDocText = textContainer.getDocText(pathIndex - pathOffset);
              if (priorDocText == null) {
                if (!textContainer.isCaching()) {
                  System.err.println("*** WARNING: non-caching textContainer being used in caching operation!");
                }

                break;
              }

              // if heading or empty, then quit; otherwise, add it.
              final Integer headingStrength = HeadingExtractor.getHeadingProperty(priorDocText);
              final boolean isHeading = isHeading(headingStrength, baseHeadingStrength);

              if (!isHeading) {
                // if the path is no longer in the same common subranch as the extractions and their predecessor, break out.
                final TagStack priorTagStack = priorDocText.getTagStack();
                if (pathOffset == 1) {
                  // compute common tag
                  commonTagBack = priorTagStack.getDeepestCommonTag(extTagStack);
                }
                else {
                  // apply common tag
                  if (commonTagBack != null && priorTagStack.hasTag(commonTagBack) < 0) {
                    if (strings.size() < 3) {
                      // recompute common tag. we don't have enough data yet.
                      // we get here, i.e., when data is in 2 cols and multiple rows of a table.
                      commonTagBack = priorTagStack.getDeepestCommonTag(extTagStack);
                    }
                    else {
                      break;
                    }
                  }
                }
              }

              final Tree<XmlLite.Data> xmlNode = priorDocText.getXmlNode();
              int insertIndex = 0;
              for (Iterator<Tree<XmlLite.Data>> iter = xmlNode.iterator(Tree.Traversal.DEPTH_FIRST);
                   iter.hasNext() && stringCountBack < maxNumBack; ) {
                final Tree<XmlLite.Data> curNode = iter.next();
                final XmlLite.Text nodeText = curNode.getData().asText();
                if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
                  final String nodeString = buildNodeString(priorDocText, curNode, getDelta(headingStrength, baseHeadingStrength));
                  strings.add(insertIndex++, nodeString);
                  ++stringCountBack;
                }
                if (isHeading) break;
              }

              if (isHeading) break;
            }
          }

          result.add(new Extraction(extractionType, docText, 1.0, new ExtractionStringsData(strings, true)));

          lastExtIndex = curExtIndex;
        }
      }
    }

    return result;
  }

  /**
   * Provide access to this extractor's normalizer.
   */
  public Normalizer getNormalizer() {
    return null;
  }

  /**
   * Determine whether this extractor hasfinished processing the doc text
   * in the pipeline.
   */
  public boolean isFinishedWithDocText(DocText docText) {
    return pivotExtractors.isFinishedWithDocText(docText);
  }

  /**
   * Determine whether this extractor is finished processing the doc texts's
   * document.
   */
  public boolean isFinishedWithDocument(DocText docText) {
    return pivotExtractors.isFinishedWithDocText(docText);
  }

  /**
   * Get this extractor's disambiguator.
   */
  public Disambiguator getDisambiguator() {
    synchronized (disambiguatorMutex) {
      if (_disambiguator == null) {
        _disambiguator = new FirstInterpretationDisambiguator(DefaultInterpreter.getInstance());
      }
    }

// [pivotContextExtractionType] | [pathkey] | [pathIndex] | "primary" | [pathText] | [headingStrings] | { [contextPathIndexOffset-i] | [contextPathKey-i] | [contextHeadingStrength-i] | [contextPathText-i] } | ...

    return _disambiguator;
  }

  private final String buildNodeString(DocText docText, Tree<XmlLite.Data> textNode, int headingDelta) {
    final LineBuilder result = new LineBuilder();

    final String pathText = textNode.getData().asText().text;  // safe based on how textNodes was created.

    //  pathIndexOffset| pathKey | headingDelta | pathText
    result.
      append(docText.getPathIndex()).
      append(docText.getPathKey(textNode)).
      append(headingDelta).
      append(pathText);
    
    return result.toString();
  }

  private final int[] findExtractionIndexes(List<Extraction> extractions, List<Tree<XmlLite.Data>> textNodes) {
    final List<Integer> indexes = new ArrayList<Integer>();

    int stringIndex = 0;
    for (Tree<XmlLite.Data> textNode : textNodes) {
      if (textNode.getData().hasProperty("extracted")) {
        indexes.add(stringIndex);
      }
      ++stringIndex;
    }

    final int[] result = new int[indexes.size()];
    int offset = 0;
    for (Integer index : indexes) {
      result[offset++] = index;
    }

    return result;
  }

  /**
   * Find the deepest common node between the trees of textNodes[extIndex] and textNodes[extIndex - 1], or null.
   */
  private final Tree<XmlLite.Data> findDeepestCommonNode(List<Tree<XmlLite.Data>> textNodes, int extIndex) {
    Tree<XmlLite.Data> result = null;

    if (extIndex > 0) {
      final Tree<XmlLite.Data> node1 = textNodes.get(extIndex - 1);
      final Tree<XmlLite.Data> node2 = textNodes.get(extIndex);

      result = node1.getDeepestCommonAncestor(node2);
    }

    return result;
  }

  private final Integer getHeadingStrength(Tree<XmlLite.Data> textNode) {
    Integer result = null;

    Tree<XmlLite.Data> tagNode = textNode.getParent();
    while (tagNode != null && result == null) {
      final XmlLite.Tag tag = tagNode.getData().asTag();
      if (tag != null) {
        result = HeadingExtractor.getDefaultInstance().getHeadingStrength(tag);
      }
      tagNode = tagNode.getParent();
    }

    return result;
  }

  private final boolean isHeading(Integer strength, Integer headingBase) {
    boolean result = false;

    if (headingBase == null) {
      result = (strength != null);
    }
    else {
      if (strength != null) {
        result = (strength > headingBase);
      }
    }

    return result;
  }

  private final int getDelta(Integer strength, Integer base) {
    int a = (strength == null) ? 0 : strength;
    int b = (base == null) ? 0 : base;
    return a - b;
  }
}
