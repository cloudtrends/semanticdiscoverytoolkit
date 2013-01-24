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

  private boolean populatedEdges;

  private int nextId;
  private Map<Integer, String> id2label;
  private Map<Integer, List<Integer>> id2ids;
  private Map<String, Integer> label2id;
  private Map<Pair, String> edge2label;

  private Map<String, String> nodeAttributes;
  private Map<String, String> edgeAttributes;
  private Map<String, String> graphAttributes;

  public DotMaker() {
    this.populatedEdges = false;
    this.nextId = 0;
    this.id2label = id2label = new LinkedHashMap<Integer, String>();
    this.id2ids = new LinkedHashMap<Integer, List<Integer>>();
    this.label2id = new HashMap<String, Integer>();
    this.edge2label = new HashMap<Pair, String>();

    this.nodeAttributes = null;
    this.edgeAttributes = null;
    this.graphAttributes = null;
  }

  /**
   * Set a nodeAttribute.
   */
  public final void setNodeAttribute(String nodeAttributeKey, String nodeAttributeValue) {
    if (nodeAttributes == null) nodeAttributes = new LinkedHashMap<String, String>();
    nodeAttributes.put(nodeAttributeKey, nodeAttributeValue);
  }

  public final String getNodeAttribute(String nodeAttributeKey) {
    String result = null;
    if (nodeAttributes != null) {
      result = nodeAttributes.get(nodeAttributeKey);
    }
    return result;
  }

  public final boolean hasNodeAttribute(String nodeAttributeKey) {
    return nodeAttributes == null ? false : nodeAttributes.containsKey(nodeAttributeKey);
  }

  /**
   * Set a edgeAttribute.
   */
  public final void setEdgeAttribute(String edgeAttributeKey, String edgeAttributeValue) {
    if (edgeAttributes == null) edgeAttributes = new LinkedHashMap<String, String>();
    edgeAttributes.put(edgeAttributeKey, edgeAttributeValue);
  }

  public final String getEdgeAttribute(String edgeAttributeKey) {
    String result = null;
    if (edgeAttributes != null) {
      result = edgeAttributes.get(edgeAttributeKey);
    }
    return result;
  }

  public final boolean hasEdgeAttribute(String edgeAttributeKey) {
    return edgeAttributes == null ? false : edgeAttributes.containsKey(edgeAttributeKey);
  }

  /**
   * Set a graphAttribute.
   */
  public final void setGraphAttribute(String graphAttributeKey, String graphAttributeValue) {
    if (graphAttributes == null) graphAttributes = new LinkedHashMap<String, String>();
    graphAttributes.put(graphAttributeKey, graphAttributeValue);
  }

  public final String getGraphAttribute(String graphAttributeKey) {
    String result = null;
    if (graphAttributes != null) {
      result = graphAttributes.get(graphAttributeKey);
    }
    return result;
  }

  public final boolean hasGraphAttribute(String graphAttributeKey) {
    return graphAttributes == null ? false : graphAttributes.containsKey(graphAttributeKey);
  }


  /**
   * Fix a string for use within a 'record' node.
   * <p>
   * This currently replaces special symbols with underscores and new lines
   * with spaces.
   */
  public static String fixRecordString(String string) {
    if (string == null) return "";

    final StringBuilder result = new StringBuilder();

    // manage symbols "|<>{}" in result
    final int len = string.length();
    for (int i = 0; i < len; ++i) {
      final char c = string.charAt(i);
      if (c == '|' || c == '<' || c == '>' || c == '{' || c == '}' || c == '"' || c == '\'') {
        result.append('_');
      }
      else if (c == 10) {
        result.append(' ');
      }
      else if (c == 13) {
        // ignore
      }
      else {
        result.append(c);
      }
    }
    
    return result.toString();
  }

  private static final String fixNodeString(String string) {
    if (string == null) return "";

    final StringBuilder result = new StringBuilder();

    // manage symbols "|<>{}" in result
    final int len = string.length();
    for (int i = 0; i < len; ++i) {
      final char c = string.charAt(i);
      if (c == '"') {
        result.append('_');
      }
      else {
        result.append(c);
      }
    }
    
    return result.toString();
  }


  protected final int getNextId() {
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
    if (!populatedEdges) {
      populateEdges();
      populatedEdges = true;
    }

    makeHeader(writer);
    makeEdges(writer);
    makeFooter(writer);
    writer.flush();
  }

  protected void makeHeader(Writer writer) throws IOException {
    if (!hasGraphAttribute("rankdir")) setGraphAttribute("rankdir", "TB");
    if (!hasNodeAttribute("shape")) setNodeAttribute("shape", "plaintext");

    writer.write("digraph G {\n");
    writeAttributeEntries(writer, graphAttributes);
    writer.write("  node [");
    writeAttributesList(writer, nodeAttributes, false);
    writer.write("];\n\n");
    //todo: setup for proper formatting
  }

  protected void writeAttributeEntries(Writer writer, Map<String, String> attributes) throws IOException {
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      writer.write("  ");
      writeAttributeEntry(entry, writer);
      writer.write(";\n");
    }
  }

  protected void writeAttributesList(Writer writer, Map<String, String> attributes, boolean didOne) throws IOException {
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      if (didOne) writer.write(", ");
      writeAttributeEntry(entry, writer);
      didOne = true;
    }
  }

  protected void collectAttributesList(StringBuilder builder, Map<String, String> attributes, boolean didOne) {
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      if (didOne) builder.append(", ");
      collectAttributeEntry(entry, builder);
      didOne = true;
    }
  }

  private final void writeAttributeEntry(Map.Entry<String, String> entry, Writer writer) throws IOException {
    writer.write(entry.getKey());
    writer.write('=');
    writer.write(entry.getValue());
  }

  private final void collectAttributeEntry(Map.Entry<String, String> entry, StringBuilder builder) {
    builder.append(entry.getKey());
    builder.append('=');
    builder.append(entry.getValue());
  }

  protected void makeEdges(Writer writer) throws IOException {
    // write definition ("nodeNNN[label="xxx"];") lines
    for (Map.Entry<Integer, String> entry : id2label.entrySet()) {
      final int id = entry.getKey();
      final String label = entry.getValue();
      writer.write("  node" + id + " [label=\"" + fixNodeString(label) + "\"];\n");
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
        if (edgeLabel != null || edgeAttributes != null) {
          edgeString.append(" [");
          if (edgeLabel != null) {
            edgeString.append("label=\"").append(edgeLabel).append("\"");
          }
          if (edgeAttributes != null) {
            collectAttributesList(edgeString, edgeAttributes, edgeLabel != null);
          }
          edgeString.append("]");
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
