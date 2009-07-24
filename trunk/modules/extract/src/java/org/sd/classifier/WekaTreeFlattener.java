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
package org.sd.classifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.sd.io.FileUtil;

/**
 * When Weka trains a tree (like J48), it outputs a text representation of that tree,
 * which is converted to individuals rules with this tool.
 * 
 * @author Dave Barney
 */
public class WekaTreeFlattener {
  /** Command-line usage */
  public static final String USAGE = "\nUSAGE:\n\tjava " + WekaTreeFlattener.class.getName() + " <file containing text representaton of tree> <output file>\n";
  private static final int OFFSET = 4;
  
  public static void flattenTree(File treeFile, File outFile) throws IOException {
    final String[] lines = FileUtil.getFirstNLines(treeFile, -1, null);
    int start = 0;
    int stop = lines.length-1;
    
    while (lines[stop].startsWith("|") || lines[stop].startsWith(" ")) stop--;
    System.out.print("Parsing tree...");
    Node tree = flattenTree(lines, start, stop, 0);
    System.out.print("done!\nWriting rules to file...");
    writeToFile(outFile, tree);
    System.out.println("done!");
  }
  
  private static void writeToFile(File outFile, Node tree) throws IOException {
    BufferedWriter writer = FileUtil.getWriter(outFile);
    final String[] rules = buildRules(tree);
    for (String rule : rules) {
      writer.write(rule);
      writer.newLine();
    }
    writer.close();
  }
  
  private static String[] buildRules(Node tree) {
    ArrayList<String> rulesList = new ArrayList<String>();
    
    buildRules(rulesList, tree, "if (");
    
    final String[] rules = new String[rulesList.size()];
    return rulesList.toArray(rules);
  }
  
  private static void buildRules(ArrayList<String> rules, Node tree, String ruleHeader) {
    if (tree.classification != null && tree.classification.label.equals("non")) {
      double c = tree.classification.correct;
      double w = tree.classification.wrong;
      double p = c/(c+w);
      double r = c/1045;
      
      if (p >= .8 && r > 0.005) rules.add(ruleHeader + ") classification = " + tree.classification.label +
          "\t" + c + "\t" + w + "\t" + p + "\t" + r);
    } else {
      if (tree.ifTrue  != null) buildRules(rules, tree.ifTrue,  ruleHeader + " && " + tree.rule);
      if (tree.ifFalse != null) buildRules(rules, tree.ifFalse, ruleHeader + " && !" + tree.rule);
    }
  }

  private static Node flattenTree(String[] lines, int start, int stop, int offset) {
    final String startLine = lines[start].substring(offset);
    
    Node node = new Node();
    node.rule = startLine;
    
    if (node.rule.equals("f5:range <= 0")) {
      int x=0;
      x++;
    }
    
    if (startLine.endsWith(")")) {
      int splitIndex = startLine.lastIndexOf(":");
      node.rule = startLine.substring(0, splitIndex);
      node.ifTrue = new Node();
      node.ifTrue.rule = "CLASSIFICATION";
      
      String rest = startLine.substring(splitIndex+1);
      node.ifTrue.classification = new Classification(rest);
      
    } else {
      int stopStart = stop;
      while (stopStart > start+1 &&
             (lines[stopStart].substring(offset).startsWith("|") ||
              lines[stopStart].substring(offset).startsWith(" "))) {
        stopStart--;
      }

      if (stopStart <= start+1) {
        node.ifTrue = flattenTree(lines, start+1, stop, offset+OFFSET);
        if (node.ifTrue.classification != null) node.ifTrue.ifFalse = flattenTree(lines, start+2, stop, offset+OFFSET);
      } else {
        node.ifTrue = flattenTree(lines, start+1, stopStart-1, offset+OFFSET);
        
        if (node.ifTrue.ifTrue != null && node.ifTrue.ifTrue.rule.equals("CLASSIFICATION")) {
          Node falseBranch = flattenTree(lines, start+2, stopStart-1, offset+OFFSET);
          node.ifTrue.ifFalse = falseBranch.ifTrue;
        }
        if (start < stopStart) {
          Node falseBranch = flattenTree(lines, stopStart, stop, offset);
          node.ifFalse = falseBranch.ifTrue;
        }
      }
    }
    
    return node;
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println(USAGE);
      return;
    }
    
    File treeFile = new File(args[0]);
    File outFile = new File(args[1]);
    
    flattenTree(treeFile, outFile);
  }
  
  static class Node {
    String rule;
    Classification classification;
    Node ifTrue;
    Node ifFalse;
    
    public Node() {
      this.rule = null;
      this.classification = null;
      this.ifTrue = null;
      this.ifFalse = null;
    }
    
    public String toString() {
      return toString(this, "");
    }
    
    public String toString(Node node, String offset) {
      StringBuilder treeValue = new StringBuilder();
      
      treeValue.append(offset + node.rule);
      if (node.classification != null) {
        treeValue.append(": " + node.classification.label +
            " (" + node.classification.correct +
            "/" + node.classification.wrong + ")");
      }
      treeValue.append("\n");
      
      if (node.ifTrue  != null) treeValue.append(toString(node.ifTrue,  offset+"| "));
      if (node.ifFalse != null) treeValue.append(toString(node.ifFalse, offset+"| "));
      
      return treeValue.toString();
    }
  }
  
  static class Classification {
    String label;
    double correct;
    double wrong;
    
    public Classification(String classificationText) {
      this.label = classificationText.substring(0, classificationText.indexOf("(")).trim();
      String rest = classificationText.substring(classificationText.indexOf("(")+1, classificationText.indexOf(")"));
      if (rest.contains("/")) {
        String[] parts = rest.split("\\/");
        this.correct = Double.parseDouble(parts[0]);
        this.wrong = Double.parseDouble(parts[1]);
      } else {
        this.correct = Double.parseDouble(rest);
        this.wrong = 0;
      }
    }
  }
}
