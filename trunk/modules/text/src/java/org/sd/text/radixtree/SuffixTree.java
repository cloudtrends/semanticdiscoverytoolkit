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
package org.sd.text.radixtree;


import java.util.concurrent.atomic.AtomicInteger;


/**
 * Implementation of Ukkonen's Suffix Tree Algorithm for inserting into a
 * suffix tree in time O(n), where n is the length of the string to insert.
 * <p>
 * Ported to java from Mark Nelson's c++ code at http://marknelson.us/1996/08/01/suffix-trees/
 *
 * @author Spence Koehler
 */
public class SuffixTree {

  /**
   * The maximum input string length this program will handle is defined
   * here.  A suffix tree can have as many as 2N edges/nodes.  The edges
   * are stored in a hash table, whose size is also defined here.
   */
  public static final int MAX_LENGTH = 1000;
  public static final int HASH_TABLE_SIZE = 2179;  // A prime roughly 10% larger


  // Instance vars.
  private int maxLength;
  private int hashTableSize;

  // The count is 1 at the start because the initial tree has the root node
  // defined, with no children.
  private AtomicInteger nodeCount;
  
  // This is the hash table where all the currently defined edges are stored.
  // You can dump out all the currently defined edges by iterating through the
  // table and finding edges whose start_node is not -1.
  private Edge[] edges;

  // The array of defined nodes.
  private Node[] nodes;

  /**
   * Construct with the default maxLength (1000) and hashTableSize (2179).
   */
  public SuffixTree() {
    this(MAX_LENGTH, HASH_TABLE_SIZE);
  }

  /**
   * Construct with the given params.
   *
   * @param maxLength  The maximum input string length (N) this tree will
   *                   handle. A suffix tree can have as many as 2N edges/nodes.
   * @param hashTableSize  Size of hash table in which the edges are stored.
   *                       This should be a prime roughly 10% larger than N.
   */
  public SuffixTree(int maxLength, int hashTableSize) {
    this.maxLength = maxLength;
    this.hashTableSize = hashTableSize;
    this.nodeCount = new AtomicInteger(1);
    this.edges = new Edge[hashTableSize];
    this.nodes = new Node[maxLength * 2];
  }

  public void addString(String string) {
    final char[] text = (string + "$").toCharArray();
    final int n = text.length - 1;

    //
    // The active point is the first non-leaf suffix in the tree.  We start by
    // setting this to be the empty string at node 0.  The AddPrefix() function
    // will update this value after every new prefix is added.
    //
    Suffix active = new Suffix(0, 0, -1);  // The initial active prefix
    for (int i = 0 ; i <= n ; i++) {
      addPrefix(active, i, text);
    }
  }

  /**
   * This routine constitutes the heart of the algorithm. It is called
   * repetitively, once for each of the prefixes of the input string.  The
   * prefix in question is denoted by the index of its last character.
   *
   * At each prefix, we start at the active point, and add a new edge denoting
   * the new last character, until we reach a point where the new edge is not
   * needed due to the presence of an existing edge starting with the new last
   * character.  This point is the end point.
   *
   * Luckily for use, the end point just happens to be the active point for
   * the next pass through the tree.  All we have to do is update it's
   * last_char_index to indicate that it has grown by a single character, and
   * then this routine can do all its work one more time.
   */
  private final void addPrefix(Suffix active, int last_char_index, char[] text) {

    int parent_node;
    int last_parent_node = -1;
    final int n = text.length - 1; // don't count the end of string char.

    while (true) {
      Edge edge = null;
      parent_node = active.origin_node;

      //
      // Step 1 is to try and find a matching edge for the given node.
      // If a matching edge exists, we are done adding edges, so we break
      // out of this big loop.
      //
      if (active.explicit()) {
        edge = find(active.origin_node, text[last_char_index], text);
        if (edge.start_node != -1) {
          break;
        }
      }
      else { //implicit node, a little more complicated
        edge = find(active.origin_node, text[active.first_char_index], text);
        int span = active.last_char_index - active.first_char_index;
        if (text[edge.first_char_index + span + 1] == text[last_char_index]) {
          break;
        }
        parent_node = splitEdge(edge, active, text);
      }

      //
      // We didn't find a matching edge, so we create a new one, add
      // it to the tree at the parent node position, and insert it
      // into the hash table.  When we create a new node, it also
      // means we need to create a suffix link to the new node from
      // the last node we visited.
      //
      final Edge new_edge = new Edge(last_char_index, n, parent_node, nodeCount.getAndIncrement());
      insert(new_edge, text);
      if (last_parent_node > 0) {
        nodes[last_parent_node].suffix_node = parent_node;
      }
      last_parent_node = parent_node;

      //
      // This final step is where we move to the next smaller suffix
      //
      if (active.origin_node == 0) {
        active.first_char_index++;
      }
      else {
        active.origin_node = nodes[active.origin_node].suffix_node;
      }
      active.canonize(this, text);
    }
    if (last_parent_node > 0) {
      nodes[last_parent_node].suffix_node = parent_node;
    }
    active.last_char_index++;  //Now the endpoint is the next active point
    active.canonize(this, text);
  }

//   private void validate() {
//   }

//   private int walk_tree(int start_node, int last_char_so_far) {
//   }

  ////////
  //
  // Edge functions
  //

  /**
   * Edges are inserted into the hash table using this hashing function.
   */
  private final int hash(int node, int c) {
    return ((node << 8) + c) % hashTableSize;
  }

  /**
   * A given edge gets a copy of itself inserted into the table with this
   * function.  It uses a linear probe technique, which means in the case
   * of a collision, we just step forward through the table until we find
   * the first unused slot.
   */
  public void insert(Edge edge, char[] text) {
    int i = hash(edge.start_node, text[edge.first_char_index]);
    while (edges[i].start_node != -1) {
      i = ++i % hashTableSize;
    }
    edges[i] = edge;
  }

  /**
   * Removing an edge from the hash table is a little more tricky. You have
   * to worry about creating a gap in the table that will make it impossible
   * to find other entries that have been inserted using a probe.  Working
   * around this means that after setting an edge to be unused, we have to
   * walk ahead in the table, filling in gaps until all the elements can be
   * found.
   *
   * Knuth, Sorting and Searching, Algorithm R, p. 527
   */
  private final void remove(Edge edge, char[] text) {
    int i = hash(edge.start_node, text[edge.first_char_index]);
    while (edges[i].start_node != edge.start_node ||
           edges[i].first_char_index != edge.first_char_index) {
      i = ++i % hashTableSize;
    }
    while (true) {
      edges[i].start_node = -1;
      int j = i;
      while (true) {
        i = ++i % hashTableSize;
        if (edges[i].start_node == -1) {
          return;
        }
        int r = hash(edges[i].start_node, text[edges[i].first_char_index]);
        if (i >= r && r > j) {
          continue;
        }
        if (r > j && j > i) {
          continue;
        }
        if (j > i && i >= r) {
          continue;
        }
        break;
      }
      edges[j] = edges[i];
    }
  }

  /**
   * The whole reason for storing edges in a hash table is that it makes this
   * function fairly efficient.  When I want to find a particular edge leading
   * out of a particular node, I call this function.  It locates the edge in
   * the hash table, and returns a copy of it.  If the edge isn't found, the
   * edge that is returned to the caller will have start_node set to -1, which
   * is the value used in the hash table to flag an unused entry.
   */
  private final Edge find(int node, int c, char[] text) {
    int i = hash(node, c);
    while (true) {
      if (edges[i].start_node == node) {
        if (c == text[edges[i].first_char_index]) {
          return edges[i];
        }
      }
      if (edges[i].start_node == -1) {
        return edges[i];
      }
      i = ++i % hashTableSize;
    }
  }

  /**
   * When a suffix ends on an implicit node, adding a new character means I
   * have to split an existing edge.  This function is called to split an
   * edge at the point defined by the Suffix argument. The existing edge loses
   * its parent, as well as some of its leading characters.  The newly created
   * edge descends from the original parent, and now has the existing edge as
   * a child.
   *
   * Since the existing edge is getting a new parent and starting character,
   * its hash table entry will no longer be valid.  That's why it gets removed
   * at the start of the function.  After the parent and start char have been
   * recalculated, it is re-inserted.
   *
   * The number of characters stolen from the original node and given to the
   * new node is equal to the number of characters in the suffix argument,
   * which is last - first + 1;
   */
  public int splitEdge(Edge edge, Suffix suffix, char[] text) {
    remove(edge, text);
    final Edge new_edge =
      new Edge(edge.first_char_index,
               edge.first_char_index + suffix.last_char_index - suffix.first_char_index,
               suffix.origin_node,
               nodeCount.getAndIncrement());
    insert(new_edge, text);
    nodes[new_edge.end_node].suffix_node = suffix.origin_node;
    edge.first_char_index += suffix.last_char_index - suffix.first_char_index + 1;
    edge.start_node = new_edge.end_node;
    insert(edge, text);
    return new_edge.end_node;
  }

// //
// // This routine prints out the contents of the suffix tree
// // at the end of the program by walking through the
// // hash table and printing out all used edges.  It
// // would be really great if I had some code that will
// // print out the tree in a graphical fashion, but I don't!
// //

// void dump_edges( int current_n )
// {
//     cout << " Start  End  Suf  First Last  String\n";
//     for ( int j = 0 ; j < HASH_TABLE_SIZE ; j++ ) {
//         Edge *s = Edges + j;
//         if ( s->start_node == -1 )
//             continue;
//         cout << setw( 5 ) << s->start_node << " "
//              << setw( 5 ) << s->end_node << " "
//              << setw( 3 ) << Nodes[ s->end_node ].suffix_node << " "
//              << setw( 5 ) << s->first_char_index << " "
//              << setw( 6 ) << s->last_char_index << "  ";
//         int top;
//         if ( current_n > s->last_char_index )
//             top = s->last_char_index;
//         else
//             top = current_n;
//         for ( int l = s->first_char_index ;
//                   l <= top;
//                   l++ )
//             cout << T[ l ];
//         cout << "\n";
//     }
// }

  //
  // End of Edge functions
  //
  ////////


  /**
   * When a new tree is added to the table, we step through all the currently
   * defined suffixes from the active point to the end point.  This structure
   * defines a Suffix by its final character. In the canonical representation,
   * we define that last character by starting at a node in the tree, and
   * following a string of characters, represented by first_char_index and
   * last_char_index.  The two indices point into the input string.  Note that
   * if a suffix ends at a node, there are no additional characters needed to
   * characterize its last character position. When this is the case, we say
   * the node is Explicit, and set first_char_index &gt; last_char_index to
   * flag that.
   */
  private static final class Suffix {
    private int origin_node;
    private int first_char_index;
    private int last_char_index;

    Suffix(int node, int start, int stop) {
      this.origin_node = node;
      this.first_char_index = start;
      this.last_char_index = stop;
    }

    public final boolean explicit() {
      return first_char_index > last_char_index;
    }

    public final boolean implicit() {
      return last_char_index >= first_char_index;
    }

    public void canonize(SuffixTree suffixTree, char[] text) {
      if (!explicit()) {
        Edge edge = suffixTree.find(origin_node, text[first_char_index], text);
        int edge_span = edge.last_char_index - first_char_index;
        while (edge_span <= (last_char_index - first_char_index)) {
          first_char_index = first_char_index + edge_span + 1;
          origin_node = edge.end_node;
          if (first_char_index <= last_char_index) {
            edge = suffixTree.find(edge.end_node, text[first_char_index], text);
            edge_span = edge.last_char_index - edge.first_char_index;
          }
        }
      }
    }
  }

  /**
   * The suffix tree is made up of edges connecting nodes. Each edge represents
   * a string of characters starting at first_char_index and ending at
   * last_char_index. Edges can be inserted and removed from a hash table,
   * based on the hash() function defined here.  The hash table indicates an
   * unused slot by setting the start_node value to -1.
   */
  private static final class Edge {
    private int first_char_index;
    private int last_char_index;
    private int end_node;
    private int start_node;

    /**
     * The default constructor for Edge just sets start_node to the invalid
     * value.  This is done to guarantee that the hash table is initially
     * filled with unused edges.
     */
    Edge() {
      this.start_node = -1;
    }

    /**
     * I create new edges in the program while walking up the set of suffixes
     * from the active point to the endpoint.  Each time I create a new edge,
     * I also add a new node for its end point.  The node entry is already
     * present in the Nodes[] array, and its suffix node is set to -1 by the
     * default Node() ctor, so I don't have to do anything with it at this
     * point.
     */
    Edge(int init_first, int init_last, int parent_node, int end_node) {
      this.first_char_index = init_first;
      this.last_char_index = init_last;
      this.start_node = parent_node;
      this.end_node = end_node;
    }
  }

  /**
   * The only information contained in a node is the suffix link. Each suffix
   * in the tree that ends at a particular node can find the next smaller
   * suffix by following the suffix_node link to a new node.  Nodes are stored
   * in a simple array.
   */
  private static final class Node {
    private int suffix_node;

    Node() {
      this.suffix_node = -1;
    }
  }
}
