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


import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.sd.util.tree.*;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class TreeDiffViewer {
  
  public static final void view1(Tree<XmlLite.Data> xmlTree1, Tree<XmlLite.Data> xmlTree2) throws Exception {
    view1(xmlTree1, xmlTree2, System.out);
  }

  public static final void view1(Tree<XmlLite.Data> xmlTree1, Tree<XmlLite.Data> xmlTree2, PrintStream out) throws Exception {

    final StructureMatcher<XmlLite.Data> matcher = new StructureMatcher<XmlLite.Data>(xmlTree1, xmlTree2);
    matcher.setNodeComparer(new NodeComparer<XmlLite.Data>() {
        public boolean matches(Tree<XmlLite.Data> node1, Tree<XmlLite.Data> node2) {
          boolean result = false;

          final XmlLite.Data data1 = node1.getData();
          final XmlLite.Data data2 = node2.getData();

          final XmlLite.Tag tag1 = data1.asTag();
          final XmlLite.Tag tag2 = data2.asTag();
          final XmlLite.Text text1 = data1.asText();
          final XmlLite.Text text2 = data2.asText();

          result = ((tag1 != null && tag2 != null && tag1.name.equals(tag2.name)) ||
                    (text1 != null && text2 != null && text1.text.equals(text2.text)));

          return result;
        }
      });

    final Tree<XmlLite.Data> ix = matcher.getTemplate();
    final StructureMatcher.MatchRatio matchRatio1 = matcher.getMatchRatio1();
    final StructureMatcher.MatchRatio matchRatio2 = matcher.getMatchRatio2();
    final StructureMatcher.MatchRatio matchRatio = matcher.getMatchRatio();

    out.println("\nIntersection:");
    if (ix == null) {
      out.println("  <NULL>");
    }
    else {
      PathHelper.dumpPaths(ix, out);
    }

    out.println();
    out.println("matchRatio1=" + matchRatio1);
    out.println("matchRatio2=" + matchRatio2);
    out.println();
    out.println("matchRatio=" + matchRatio);
  }


  public static final void view2(Tree<XmlLite.Data> xmlTree1, Tree<XmlLite.Data> xmlTree2) throws Exception {
    view2(xmlTree1, xmlTree2, System.out);
  }

  public static final void view2(Tree<XmlLite.Data> xmlTree1, Tree<XmlLite.Data> xmlTree2, PrintStream out) throws Exception {

    final LeafDiffer<XmlLite.Data> leafDiffer = new LeafDiffer<XmlLite.Data>(xmlTree1, xmlTree2);
    leafDiffer.setTextExtractor(new TextExtractor<XmlLite.Data>() {
        public String extractText(Tree<XmlLite.Data> node) {
          String result = null;

          final XmlLite.Text text = node.getData().asText();
          if (text != null) {
            result = text.text;
          }

          return result;
        }
      });

    final List<Tree<XmlLite.Data>> dxs1 = leafDiffer.getDisjunctionNodes1();
    final List<Tree<XmlLite.Data>> dxs2 = leafDiffer.getDisjunctionNodes2();

    int counter = 0;
    for (Tree<XmlLite.Data> dx : dxs1) {
      out.println("\nDisjunction1 #" + counter);
      PathHelper.dumpPaths(dx, out);
      ++counter;
    }

    out.println();

    counter = 0;
    for (Tree<XmlLite.Data> dx : dxs2) {
      out.println("\nDisjunction2 #" + counter);
      PathHelper.dumpPaths(dx, out);
      ++counter;
    }
  }


  public static final void main(String[] args) throws Exception {
    //arg0: html1
    //arg1: html2

    final Tree<XmlLite.Data> xmlTree1 = XmlFactory.readXmlTree(new File(args[0]), true, true, false);
    final Tree<XmlLite.Data> xmlTree2 = XmlFactory.readXmlTree(new File(args[1]), true, true, false);

    view1(xmlTree1, xmlTree2);
    view2(xmlTree1, xmlTree2);
  }
}
