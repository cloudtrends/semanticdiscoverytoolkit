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


import org.sd.io.FileUtil;
import org.sd.xml.HtmlHelper;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A "mega extractor" to apply multiple regular extractors to a document (text
 * container).
 * <p>
 * @author Spence Koehler
 */
public abstract class ExtractionPipeline {

  /**
   * Build a text container for the file.
   */
  protected abstract TextContainer buildTextContainer(String streamId, InputStream inputStream, boolean keepDocTexts, boolean keepEmpties, AtomicBoolean die) throws IOException;


  /**
   * Convenience method to build a default pipeline over html data.
   *
   * @param extractHeadings  If true, a default HeadingExtractor will be added.
   * @param keepDocTexts     Set to true if will use extrapolation or other text container access.
   * @param extractors       Extractors to add. Okay if null.
   *
   * @return a new extraction pipeline.
   */
  public static final ExtractionPipeline buildDefaultHtmlPipeline(boolean extractHeadings, boolean keepDocTexts,
                                                                  boolean keepEmpties, Extractor[] extractors) {
    final ExtractionPipeline result = new XmlExtractionPipeline(true, HtmlHelper.DEFAULT_IGNORE_TAGS, keepDocTexts, keepEmpties);

    result.addExtractors(extractHeadings, extractors);

    return result;
  }

  public static final ExtractionPipeline buildDefaultTextFilePipeline(boolean keepDocTexts, boolean keepEmpties, Extractor[] extractors) {
    final ExtractionPipeline result = new TextFileExtractionPipeline(keepDocTexts);

    if (extractors != null) {
      for (Extractor extractor : extractors) {
        result.addExtractor(extractor);
      }
    }

    return result;
  }

  /**
   * Convenience method to build a default pipeline over html data.
   *
   * @param extractHeadings  If true, a default HeadingExtractor will be added.
   * @param keepDocTexts     Set to true if will use extrapolation or other text container access.
   * @param extractors       Extractors to add. Okay if null.
   *
   * @return a new extraction pipeline.
   */
  public static final ExtractionPipeline buildDefaultHtmlPipeline(boolean extractHeadings, boolean keepDocTexts,
                                                                  boolean keepEmpties, List<Extractor> extractors) {
    final ExtractionPipeline result = new XmlExtractionPipeline(true, HtmlHelper.DEFAULT_IGNORE_TAGS, keepDocTexts, keepEmpties);

    result.addExtractors(extractHeadings, extractors);

    return result;
  }


  private List<Extractor> extractors;
  private boolean keepDocTexts;
  private boolean keepEmpties;

  /**
   * Default constructor.
   * <p>
   * Created text containers will keep (cache) doc texts only if one of the
   * added extractors needs the doc text cache.
   */
  public ExtractionPipeline() {
    this(false, false);
  }

  /**
   * Construct forcing created text containers to keep (cache) doc texts.
   */
  public ExtractionPipeline(boolean keepDocTexts, boolean keepEmpties) {
    this.extractors = null;
    this.keepDocTexts = keepDocTexts;
    this.keepEmpties = keepEmpties;
  }

  /**
   * Add an extractor to this pipeline.
   */
  public final void addExtractor(Extractor extractor) {
    if (extractor != null) {
      if (extractors == null) extractors = new ArrayList<Extractor>();
      extractors.add(extractor);

      if (!keepDocTexts && extractor.needsDocTextCache()) {
        this.keepDocTexts = true;
      }
    }
  }


  /**
   * Add the given extractors to this pipeline.
   *
   * @param extractHeadings  if true, the default heading extractor will also be added.
   * @param extractors       the list of extractors to add.
   */
  public final void addExtractors(boolean extractHeadings, List<Extractor> extractors) {
    HeadingExtractor headingExtractor = null;

    if (extractHeadings) {
      headingExtractor = HeadingExtractor.getDefaultInstance();
      addExtractor(headingExtractor);
    }

    if (extractors != null) {
      for (Extractor extractor : extractors) {
        if (!extractHeadings || (extractHeadings && extractor != headingExtractor)) {
          addExtractor(extractor);
        }
      }
    }
  }

  /**
   * Add the given extractors to this pipeline.
   *
   * @param extractHeadings  if true, the default heading extractor will also be added.
   * @param extractors       the list of extractors to add.
   */
  public final void addExtractors(boolean extractHeadings, Extractor[] extractors) {
    HeadingExtractor headingExtractor = null;

    if (extractHeadings) {
      headingExtractor = HeadingExtractor.getDefaultInstance();
      addExtractor(headingExtractor);
    }

    if (extractors != null) {
      for (Extractor extractor : extractors) {
        if (!extractHeadings || (extractHeadings && extractor != headingExtractor)) {
          addExtractor(extractor);
        }
      }
    }
  }

  /**
   * Get this pipeline's extractors.
   */
  public List<Extractor> getExtractors() {
    return extractors;
  }

  /**
   * Should extract if any of the extractors in this pipeline should extract.
   */
  public boolean shouldExtract(DocText docText) {
    if (extractors ==  null) return false;

    boolean result = false;

    for (Extractor extractor : extractors) {
      if (extractor.shouldExtract(docText)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Is finished with doc text if ALL of the extractors in ths pipeline are finished.
   */
  public boolean isFinishedWithDocText(DocText docText) {
    boolean result = false;

    if (extractors != null) {
      result = true;

      for (Extractor extractor : extractors) {
        if (!extractor.isFinishedWithDocText(docText)) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  /**
   * 
   */
  public List<Extraction> extract(DocText docText, AtomicBoolean die) {
    List<Extraction> result = null;

    if (extractors != null) {
      for (Extractor extractor : extractors) {
        final List<Extraction> extractions = extractor.extract(docText, die);
        if (extractions != null) {
          if (result == null) result = new ArrayList<Extraction>();
          result.addAll(extractions);
        }
        if (die != null && die.get()) break;
      }
    }

    return result;
  }

  /**
   * Is finished with document if ALL of the extractors in this pipeline are finished.
   */
  public boolean isFinishedWithDocument(DocText docText) {
    boolean result = false;

    if (extractors != null) {
      result = true;

      for (Extractor extractor : extractors) {
        if (!isFinishedWithDocument(docText)) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Run the pipelined extractors over the given text container, storing
   * the results within.
   * <p>
   * The resultant text container will be iterated up to the point where
   * all extractors are finished with its document.
   * <p>
   * NOTE: It is the responsibility of the caller of this method to close
   *       non-null returned text containers.
   */
  public TextContainer runExtractors(File file, AtomicBoolean die, boolean turnOffCaching) throws IOException {
    return runExtractors(file.getAbsolutePath(), FileUtil.getInputStream(file), die, turnOffCaching);
  }

  /**
   * Run the pipelined extractors over the given text container, storing
   * the results within.
   * <p>
   * The resultant text container will be iterated up to the point where
   * all extractors are finished with its document.
   * <p>
   * NOTE: It is the responsibility of the caller of this method to close
   *       non-null returned text containers.
   */
  public TextContainer runExtractors(String streamId, InputStream inputStream, AtomicBoolean die, boolean turnOffCaching) throws IOException {
    TextContainer result = null;

    if (extractors != null) {
      result = buildTextContainer(streamId, inputStream, turnOffCaching ? false : keepDocTexts, keepEmpties, die);

      while (result.hasNext() && (die == null || !die.get())) {
        final DocText docText = result.next();
        if ("".equals(docText.getString())) continue;  // ignore empty lines
      
        int numFinished = 0;

        for (Extractor extractor : extractors) {
          if (die != null && die.get()) break;

          if (extractor.shouldExtract(docText)) {
            final List<Extraction> extractions = extractor.extract(docText, die);

            if (extractions != null) {
              for (Extraction extraction : extractions) {
                result.addExtraction(extraction, extractor);
              }
            }
          }

          if (extractor.isFinishedWithDocument(docText)) {
            ++numFinished;
          }

          if (extractor.isFinishedWithDocText(docText)) {
            break;
          }
        }

        if (numFinished == extractors.size()) break;
      }
    }

    return result;
  }

  /**
   * Auxiliary method to run from a main.
   */
  public final void mainRunner(String[] args, String extractionType) throws IOException {
    mainRunner(args, new String[]{extractionType});
  }

  /**
   * Auxiliary method to run from a main.
   */
  public final void mainRunner(File[] files, String extractionType) throws IOException {
    mainRunner(files, new String[]{extractionType});
  }

  /**
   * Auxiliary method to run from a main.
   */
  public final void mainRunner(String[] args, String[] extractionTypes) throws IOException {
    final File[] files = new File[args.length];
    
    for (int i=0; i < args.length; i++) {
      files[i] = new File(args[i]);
    }
    
    mainRunner(files, extractionTypes);
  }

  /**
   * Auxiliary method to run from a main.
   */
  public final void mainRunner(File[] files, String[] extractionTypes) throws IOException {
    final ExtractionRunner extractionRunner = new ExtractionRunner(this) {
        public void close() throws IOException {
        }
      };

    for (int i = 0; i < files.length; ++i) {
      try {
        if (files[i].isDirectory() || files[i] == null) continue;
        final String fileName = files[i].getName();
        if (!fileName.endsWith(".html.gz") && !fileName.endsWith(".html")) continue;
        
        final ExtractionResults extractionResults = extractionRunner.run(files[i], null, false);
  
        if (extractionResults != null) {
          final List<Extraction> extractions = extractionResults.getExtractions(extractionTypes);
          if (extractions != null) {
            Collections.sort(extractions, DocumentOrderExtractionComparator.getInstance());
        
            for (Extraction extraction : extractions) {
              System.out.println(fileName + "|" + extraction.getFieldsString());
            }
          }
          else {
            System.out.println(fileName + "|||nothing|||");
          }
        }
        else {
          System.out.println(fileName + "|||nothing|||");
        }
      } catch (Exception e) {
        System.out.println(files[i].getName() + "|||Error|||");
        e.printStackTrace(System.out);
      }
    }
  }
}
