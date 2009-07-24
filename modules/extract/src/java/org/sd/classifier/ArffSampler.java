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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.sd.io.FileUtil;

public class ArffSampler {
  /** Command-line usage */
  public static final String USAGE = "\njava " + ArffSampler.class.getName() + " <input arff> <input src> <output arff> <output src>\n";
  
  public static void sample(File inArffFile, File inSrcFile, File outArffFile, File outSrcFile) throws IOException {
    final BufferedReader inArff = FileUtil.getReader(inArffFile);
    final BufferedReader inSrc = FileUtil.getReader(inSrcFile);
    final BufferedWriter outArff = FileUtil.getWriter(outArffFile);
    final BufferedWriter outSrc = FileUtil.getWriter(outSrcFile);
    String line;
    
    int b2bCount = 0;
    int srcIndex = 1;
    while ((line = inArff.readLine()) != null) {
      if (line.startsWith("B2C,")) {
        final String srcLine = inSrc.readLine();
        for (int i=0; i < 2; i++) {
          outArff.write("NON_B2B" + line.substring(3) + "\n");
          outSrc.write((srcIndex++) + srcLine.substring(srcLine.indexOf("|")) + "\n");
        }
      } else if (line.startsWith("OTHER,")) {
        final String srcLine = inSrc.readLine();
        for (int i=0; i < 2; i++) {
          outArff.write("NON_B2B" + line.substring(5) + "\n");
          outSrc.write((srcIndex++) + srcLine.substring(srcLine.indexOf("|")) + "\n");
        }
      } else if (line.startsWith("BOTH,")) {
        // skip line
        inSrc.readLine();
      } else if (line.startsWith("B2B,")) {
        final String srcLine = inSrc.readLine();
        if (b2bCount % 10 == 0) {
          outArff.write(line + "\n");
          outSrc.write((srcIndex++) + srcLine.substring(srcLine.indexOf("|")) + "\n");
        }
        b2bCount++;
      } else {
         outArff.write(line + "\n");
      }
    }
    
    inArff.close();
    outArff.close();
    outSrc.close();
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.err.println(USAGE);
      return;
    }
    
    File inArffFile = new File(args[0]);
    File inSrcFile = new File(args[1]);
    File outArffFile = new File(args[2]);
    File outSrcFile = new File(args[3]);
    
    sample(inArffFile, inSrcFile, outArffFile, outSrcFile);
  }
}
