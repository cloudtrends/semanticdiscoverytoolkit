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
package org.sd.match;


import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.util.tree.Tree2Dot;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Utility to convert the base64 osob string to a graph.
 * <p>
 * @author Spence Koehler
 */
public class GraphHelper {
  
  public static void main(String[] args) throws IOException {
    // arg0: base64 string
    // arg1: output dot file

    final OsobDeserializer deserializer = new OsobDeserializer();
    final ConceptModel model = deserializer.buildConceptModel(args[0]);
    final Tree<ConceptModel.Data> tree = model.getTree();
    final Tree2Dot<ConceptModel.Data> tree2dot = new Tree2Dot<ConceptModel.Data>(tree);

    if (args.length == 2) {
      final BufferedWriter writer = FileUtil.getWriter(args[1]);
      tree2dot.writeDot(writer);
      writer.close();
    }
    else {
      System.out.println(tree.toString());
    }
  }
}
