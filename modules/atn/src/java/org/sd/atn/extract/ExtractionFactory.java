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
package org.sd.atn.extract;


import org.sd.util.FileContext;
import org.sd.util.InputContext;
import org.sd.util.LineContext;
import org.sd.util.ParagraphContext;
import org.sd.util.StringInputContext;
import org.sd.xml.DomContext;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;

/**
 * Factory for building extractions based on InputContext type.
 * <p>
 * Note that a generic Extraction is decoupled from any input context.
 * This factory provides a coupling between InputContext and Extraction for
 * those cases where this is necessary.
 *
 * @author Spence Koehler
 */
public abstract class ExtractionFactory {
  
  public static ExtractionFactory getFactory(InputContext inputContext) {
    ExtractionFactory result = null;

    final Class<?> inputContextRootClass = inputContext.getContextRoot().getClass();

    if (DomContext.class.isAssignableFrom(inputContextRootClass)) {
      result = new XmlExtractionFactory(inputContext);
    }
    else if (FileContext.class.isAssignableFrom(inputContextRootClass)) {
      result = new FileExtractionFactory(inputContext);
    }
    else if (StringInputContext.class.isAssignableFrom(inputContextRootClass)) {
      result = new StringExtractionFactory(inputContext);
    }

    return result;
  }


  private InputContext rootContext;

  public ExtractionFactory(InputContext inputContext) {
    this.rootContext = inputContext == null ? null : inputContext.getContextRoot();
  }

  public InputContext getRootContext() {
    return rootContext;
  }

  public abstract Extraction buildParentExtraction(String type, Extraction firstChildExtraction, Extraction lastChildExtraction);

  public abstract Extraction buildLeafExtraction(String type, InputContext inputContext, int startIndex, int endIndex);

  public abstract InputContext getInputContext(Extraction extration, int[] startPos, int[] endPos);


  public static class XmlExtractionFactory extends ExtractionFactory {

    public XmlExtractionFactory(InputContext inputContext) {
      super(inputContext);
    }

    public Extraction buildParentExtraction(String type, Extraction firstChildExtraction, Extraction lastChildExtraction) {
      final XmlTextExtraction fce = (XmlTextExtraction)firstChildExtraction;
      final XmlTextExtraction lce = (XmlTextExtraction)lastChildExtraction;
      final DomNode domNode = DomUtil.getDeepestCommonAncestor(fce.getNode(), lce.getNode());

      final int startIndex = fce.getStartIndex() + DomUtil.getTextPos(domNode, fce.getNode());
      final int endIndex = lce.getEndIndex() + DomUtil.getTextPos(domNode, lce.getNode());

      return new XmlTextExtraction(type, domNode.getDomContext(), startIndex, endIndex);
    }

    public Extraction buildLeafExtraction(String type, InputContext inputContext, int startIndex, int endIndex) {
      final DomContext domContext = (DomContext)inputContext;
      final DomNode domNode = domContext.getDomNode();

      return new XmlTextExtraction(type, domNode.getDomContext(), startIndex, endIndex);
    }

    public InputContext getInputContext(Extraction extraction, int[] startPos, int[] endPos) {
      final XmlTextExtraction e = (XmlTextExtraction)extraction;
      final DomContext domContext = e.getContext();
      startPos[0] = e.getStartIndex();
      endPos[0] = e.getEndIndex();
      return domContext;
    }
  }

  public static class FileExtractionFactory extends ExtractionFactory {

    public FileExtractionFactory(InputContext inputContext) {
      super(inputContext);
    }

    public Extraction buildParentExtraction(String type, Extraction firstChildExtraction, Extraction lastChildExtraction) {
      final FileExtraction fce = (FileExtraction)firstChildExtraction;
      final FileExtraction lce = (FileExtraction)lastChildExtraction;
      
      return new FileExtraction(type, fce.getFileContext(), fce.getFileStartPos(), lce.getFileEndPos());
    }

    public Extraction buildLeafExtraction(String type, InputContext inputContext, int startIndex, int endIndex) {
      Extraction result = null;

      if (inputContext instanceof FileContext) {
        result = new FileExtraction(type, (FileContext)inputContext, startIndex, endIndex);
      }
      else if (inputContext instanceof ParagraphContext) {
        result = new FileExtraction(type, (ParagraphContext)inputContext, startIndex, endIndex);
      }
      else if (inputContext instanceof LineContext) {
        result = new FileExtraction(type, (LineContext)inputContext, startIndex, endIndex);
      }

      return result;
    }

    public InputContext getInputContext(Extraction extraction, int[] startPos, int[] endPos) {
      final FileExtraction e = (FileExtraction)extraction;
      final FileContext fileContext = e.getFileContext();
      startPos[0] = e.getFileStartPos();
      endPos[0] = e.getFileEndPos();
      return fileContext;
    }
  }

  public static class StringExtractionFactory extends ExtractionFactory {

    public StringExtractionFactory(InputContext inputContext) {
      super(inputContext);
    }

    public Extraction buildParentExtraction(String type, Extraction firstChildExtraction, Extraction lastChildExtraction) {
      final StringExtraction fce = (StringExtraction)firstChildExtraction;
      final StringExtraction lce = (StringExtraction)lastChildExtraction;

      return new StringExtraction(type, fce.getStringContext(), fce.getStartPos(), lce.getEndPos());
    }

    public Extraction buildLeafExtraction(String type, InputContext inputContext, int startIndex, int endIndex) {
      final StringInputContext stringContext = (StringInputContext)inputContext;
      return new StringExtraction(type, stringContext, startIndex, endIndex);
    }

    public InputContext getInputContext(Extraction extraction, int[] startPos, int[] endPos) {
      final StringExtraction e = (StringExtraction)extraction;
      final StringInputContext stringContext = e.getStringContext();
      startPos[0] = e.getStartPos();
      endPos[0] = e.getEndPos();
      return stringContext;
    }
  }
}
