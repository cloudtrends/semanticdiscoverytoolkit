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
package org.sd.util;


import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for creating AT&amp;T Graphviz dot files.
 * <p>
 * @author Spence Koehler
 */
public abstract class DotMaker {
  
  /**
   * Populate edges through calls to addId2Label and addEdge.
   */
  protected abstract void populateEdges();

  private int nextId;
  private Map<Integer, String> id2label;
  private Map<Integer, List<Integer>> id2ids;
  private Map<String, Integer> label2id;
  private Map<Pair, String> edge2label;

  private List<String> nodeAttributes;

  public DotMaker() {
    this.nextId = 0;
    this.id2label = id2label = new LinkedHashMap<Integer, String>();
    this.id2ids = new LinkedHashMap<Integer, List<Integer>>();
    this.label2id = new HashMap<String, Integer>();
    this.edge2label = new HashMap<Pair, String>();
  }

  /**
   * Add a nodeAttribute of the form 'attr=value'.
   */
  public void addNodeAttribute(String nodeAttribute) {
    if (nodeAttributes == null) nodeAttributes = new ArrayList<String>();
    nodeAttributes.add(nodeAttribute);
  }

  private final int getNextId() {
    return nextId++;
  }

  protected final Integer findLabelId(String label) {
    return label2id.get(label);
  }

  public final int addId2Label(String label) {
    final int result = getNextId();
    id2label.put(result, label);
    label2id.put(label, result);
    return result;
  }

  public final void addEdge(int fromId, int toId) {
    addEdge(fromId, toId, null);
  }

  public final void addEdge(int fromId, int toId, String label) {
    List<Integer> ids = id2ids.get(fromId);
    if (ids == null) {
      ids = new ArrayList<Integer>();
      id2ids.put(fromId, ids);
    }
    ids.add(toId);

    if (label != null) {
      edge2label.put(new Pair(fromId, toId), label);
    }
  }

  public void writeDot(Writer writer) throws IOException {
    populateEdges();

    makeHeader(writer);
    makeEdges(writer);
    makeFooter(writer);
    writer.flush();
  }

  protected void makeHeader(Writer writer) throws IOException {
    writer.write("digraph G {\n");
    writer.write("  rankdir=TB;\n");
    writer.write("  node [shape=plaintext");

    if (nodeAttributes != null) {
      for (String nodeAttribute : nodeAttributes) {
        writer.write(", " + nodeAttribute);
      }
    }

    writer.write("];\n\n");
    //todo: setup for proper formatting
  }

  protected void makeEdges(Writer writer) throws IOException {
    // write definition ("nodeNNN[label="xxx"];") lines
    for (Map.Entry<Integer, String> entry : id2label.entrySet()) {
      final int id = entry.getKey();
      final String label = entry.getValue();
      writer.write("  node" + id + " [label=\"" + label + "\"];\n");
    }

    writer.write("\n");

    // write edge (nodeAAA -> nodeBBB) lines
    for (Map.Entry<Integer, List<Integer>> entry : id2ids.entrySet()) {
      final int fromId = entry.getKey();
      final List<Integer> toIds = entry.getValue();

      final String fromNode = "  node" + fromId;

      for (Integer toId : toIds) {
        final String toNode = "node" + toId;
        final String edgeLabel = edge2label.get(new Pair(fromId, toId));

        final StringBuilder edgeString = new StringBuilder();
        edgeString.append(fromNode).append(" -> ").append(toNode);
        if (edgeLabel != null) {
          edgeString.append(" [label=\"").append(edgeLabel).append("\"]");
        }
        edgeString.append(";\n");

        writer.write(edgeString.toString());
      }
    }
  }

  protected void makeFooter(Writer writer) throws IOException {
    writer.write("}\n");
  }

  private static final class Pair {
    public final int from;
    public final int to;

    public Pair(int from, int to) {
      this.from = from;
      this.to = to;
    }

    public int hashCode() {
      return 31 * from + to;
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Pair) {
        final Pair other = (Pair)o;

        result = (this.from == other.from) && (this.to == other.to);
      }

      return result;
    }
  }
}
