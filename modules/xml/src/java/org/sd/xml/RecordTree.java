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
package org.sd.xml;

import org.sd.util.tree.Tree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Tree to organize xml section data.
 * <p>
 * @author Spence Koehler
 */
public class RecordTree {

  private SectionModel sectionModel;
  private Tree<Data> recordTree;

  public RecordTree(SectionModel sectionModel) {
    this.sectionModel = sectionModel;
    init();
  }
  
  public void dump(BufferedWriter writer) throws IOException {
    for (Iterator<Tree<Data>> iter = recordTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<Data> node = iter.next();
      final Data data = node.getData();

      final int indent = node.depth() * 2;
      for (int i = 0; i < indent - 1; ++i) writer.write(' ');
      writer.write(data.toString());
      writer.newLine();
    }
  }

  public Tree<Data> getRecordTree() {
    return recordTree;
  }

//   /**
//    * Get the first heading found (strictly) above (not including) the given node.
//    */
//   public Tree<Data> getClosestHeading(Tree<Data> recordNode, int minStrength) {
//     Tree<Data> result = recordNode.getParent();
//     while (result != null) {
//       final ElementData element = result.getData().getElementData();
//       if (element != null && element.getHeadingStrength() >= minStrength) {
//         break;  // found it
//       }
//     }
//     return result;
//   }

  public int getHeadingStrength(Tree<Data> headingNode) {
    final ElementData element = headingNode.getData().getElementData();
    return (element != null) ? element.getHeadingStrength() : 0;
  }

  private final void init() {
    int id = 0;
    this.recordTree = new Tree<Data>(new DocData(id++));

    boolean inRecordSection = false;
    boolean inRecord = false;

    Tree<Data> curRecordSectionHeadingNode = recordTree;
    Tree<Data> curRecordHeadingNode = recordTree;

    Tree<Data> curRecordSectionNode = recordTree;
    Tree<Data> curRecordNode = recordTree;

    Tree<XmlLite.Data> prevRecordSectionNode = null;
    Tree<XmlLite.Data> prevRecordNode = null;
    

    final int num = sectionModel.getNumPaths();
    for (int i = 0; i < num; ++i) {
      final SectionModel.Path path = sectionModel.getPath(i);
      if (path.text == null) continue;

      final int headingStrength = path.getHeading();
      final Tree<XmlLite.Data> recordSectionNode = path.specialNodes.get("recordSection");
      final Tree<XmlLite.Data> recordNode = recordSectionNode != null ? path.specialNodes.get("record") : null;

      if (recordSectionNode == null) {
        curRecordSectionNode = curRecordSectionHeadingNode;
        curRecordHeadingNode = curRecordSectionNode;
        inRecordSection = false;
      }
      else if (recordSectionNode != prevRecordSectionNode) {
        final Tree<Data> nextRecordSectionNode = new Tree<Data>(new RecordSectionData(id++, recordSectionNode));
        curRecordSectionHeadingNode.addChild(nextRecordSectionNode);
        curRecordSectionNode = nextRecordSectionNode;
        curRecordHeadingNode = curRecordSectionNode;
        inRecordSection = true;
      }

      if (recordNode == null) {
        curRecordNode = curRecordHeadingNode;
        inRecord = false;
      }
      else if (recordNode != prevRecordNode) {
        final Tree<Data> nextRecordNode = new Tree<Data>(new RecordData(id++, recordNode));
        curRecordHeadingNode.addChild(nextRecordNode);
        curRecordNode = nextRecordNode;
        inRecord = true;
      }

      if (headingStrength > 0) {
        final Tree<Data> headingNode = new Tree<Data>(new ElementData(id++, path));
        Tree<Data> headingParent = null;

        //if inRecord, 
        if (inRecord) {
          if (headingStrength > 3) {
            headingParent = findHeadingParent(curRecordNode, curRecordNode, headingStrength);
          }
        }
        //if inRecordSection
        else if (inRecordSection) {
          headingParent = findHeadingParent(curRecordNode, curRecordSectionNode, headingStrength);
          curRecordHeadingNode = headingNode;
        }
        //else
        else {
          headingParent = findHeadingParent(curRecordNode, recordTree, headingStrength);
          curRecordSectionHeadingNode = headingNode;
          curRecordHeadingNode = headingNode;
        }

        // stitch heading in
        if (headingParent != null) {
          headingParent.addChild(headingNode);
          curRecordNode = headingNode;
        }
      }
      else {
        curRecordNode.addChild(new Tree<Data>(new ElementData(id++, path)));
      }

      prevRecordSectionNode = recordSectionNode;
      prevRecordNode = recordNode;
    }
  }
  
  private Tree<Data> findHeadingParent(Tree<Data> fromNode, Tree<Data> toNode, int strength) {
    // find a heading from (including) fromNode to toNode (inclusive) that is
    // greater in strength than strength or return toNode.
    Tree<Data> result = toNode;

    while (fromNode != toNode && fromNode != null) {
      final int curStrength = getHeadingStrength(fromNode);
      if (curStrength > strength) {
        result = fromNode;
        break;
      }
      fromNode = fromNode.getParent();
    }
    return result;
  }

  public static interface Data {
    /**
     * Get the xml node represented by this data.
     */
    public Tree<XmlLite.Data> getXmlNode();
    public String getPathKey();
    public int getId();

    // safe/efficient downcasting
    public DocData getDocData();
    public RecordSectionData getRecordSectionData();
    public RecordData getRecordData();
    public ElementData getElementData();
  }

  private abstract class AbstractData implements Data {
    public final int id;
    public final Tree<XmlLite.Data> xmlNode;
    private AbstractData(int id, Tree<XmlLite.Data> xmlNode) {
      this.id = id;
      this.xmlNode = xmlNode;
    }
    public int getId() {
      return id;
    }
    public Tree<XmlLite.Data> getXmlNode() {
      return xmlNode;
    }
    public String getPathKey() {
      //todo: set or parameterize includeMask, etc.
      return xmlNode != null ? sectionModel.getPathHelper().buildPathKey(xmlNode) : null;
    }
    public DocData getDocData() {
      return null;
    }
    public RecordSectionData getRecordSectionData() {
      return null;
    }
    public RecordData getRecordData() {
      return null;
    }
    public ElementData getElementData() {
      return null;
    }
  }

  public final class DocData extends AbstractData {
    private DocData(int id) {
      super(id, null);
    }
    public DocData getDocData() {
      return this;
    }
    public String toString() {
      return "Document";
    }
  }

  public final class RecordSectionData extends AbstractData {
    private RecordSectionData(int id, Tree<XmlLite.Data> xmlNode) {
      super(id, xmlNode);
    }
    public RecordSectionData getRecordSectionData() {
      return this;
    }
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(" RecordSection: ").append(getPathKey());
      return result.toString();
    }
  }

  public final class RecordData extends AbstractData {
    private RecordData(int id, Tree<XmlLite.Data> xmlNode) {
      super(id, xmlNode);
    }
    public RecordData getRecordData() {
      return this;
    }
    public int getRecordNum() {
      return sectionModel.getTreeAnalyzer().getNodeInfo(xmlNode).localRepeatIndex;
    }
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(" Record-").append(getRecordNum()).
        append(": ").append(getPathKey());
      return result.toString();
    }
//NOTE: can detect when a record has paragraphs by finding multiple blocks of consecutive text nodes within
//      todo: add notion of getting/iterating over Paragraphs???
  }

  public final class ElementData extends AbstractData {
    public final SectionModel.Path path;
    private ElementData(int id, SectionModel.Path path) {
      super(id, path.leaf);
      this.path = path;
    }
    public ElementData getElementData() {
      return this;
    }
    public SectionModel.Path getPath() {
      return path;
    }
    public boolean hasNextConsecutive() {
      return path.hasNextConsecutive();
    }
    public int getHeadingStrength() {
      return path.getHeading();
    }
    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append((hasNextConsecutive()) ? '+' : ' ');
      if (path.getHeading() > 0) {
        result.append("H").append(path.getHeading()).append(":");
      }
      else {
        result.append("Text: ");
      }
      result.append(getPathKey());

      return result.toString();
    }
  }


  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Usage: RecordTree xmlInputFile outputFile");
    }
    else {
      final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(new File(args[0]), true, true, false);
      final SectionModel sectionModel = new SectionModel(xmlTree);
      final RecordTree recordTree = new RecordTree(sectionModel);

      final BufferedWriter writer = org.sd.io.FileUtil.getWriter(args[1]);
      recordTree.dump(writer);
      writer.close();
    }
  }
}
