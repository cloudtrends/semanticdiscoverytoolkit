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
package org.sd.cluster.job;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

/**
 * Utility to split work batches at key units.
 * <p>
 * @author Spence Koehler
 */
public class WorkBatchSplitter {
  
  //java -Xmx640m org.sd.cluster.job.WorkBatchSplitter workbatch /home/sbk/sd/resources/ontology/batch/global.011007-take2-batches-1 /home/sbk/sd/resources/ontology/batch/global.011007-take2-batches-1a /home/sbk/sd/resources/ontology/batch/global.011007-take2-batches-1b www.dabbler.com www.smilefood.de www.jumptheshark.net www.chemplex.com www.techgalaxy.net www.toolsthatwork.com www.agamendon.de www.pcbarcode.com www.turkishkaolin.com www.formulafoods.co.nz www.hazmatmedical.com www.kimmotor.com www.akebono.nl www.tsaicapital.com www.ttthg.com
  public static void main(String[] args) throws IOException {
    //arg0: work filename prefix  (i.e. workbatch) (assuming "workbatch-\\d+.dat" pattern)
    //arg1: input work batch dir
    //arg2: output work batch dir 1
    //arg3: output work batch dir 2
    //args4+: domains to split at (inclusive)
    //
    // split up to and including the domains and place into work batch dir 1
    // place remaining domains into work batch dir 2

    final String prefix = args[0];
    final String inputDir = args[1];
    final String outputDir1 = args[2];
    final String outputDir2 = args[3];
    final int startInd = 4;

    final WorkBatch.WorkUnitComparer c = new WorkBatch.EndsWithAnyComparer(args, 4);

    final File iDir = new File(inputDir);
    final File oDir1 = new File(outputDir1);
    final File oDir2 = new File(outputDir2);

    if (!oDir1.exists()) oDir1.mkdirs();
    if (!oDir2.exists()) oDir2.mkdirs();

    final Pattern pattern = Pattern.compile(prefix + "-\\d+.dat");
    final File[] ifiles = iDir.listFiles(new FileFilter() {
        public boolean accept(File file) {
          final String name = file.getName();
          final Matcher m = pattern.matcher(name);
          return m.matches();
        }
      });
    
    for (File ifile : ifiles) {
      final String ifilename = ifile.getAbsolutePath();
      final String iname = ifile.getName();
      final WorkBatch ibatch = new WorkBatch(ifilename);
      final List<UnitOfWork> iworkUnits = ibatch.getWorkUnits();

      final File ofile1 = new File(oDir1, iname);
      final File ofile2 = new File(oDir2, iname);
      final WorkBatch obatch1 = new WorkBatch(ofile1.getAbsolutePath());
      final WorkBatch obatch2 = new WorkBatch(ofile2.getAbsolutePath());
      WorkBatch obatch = obatch1;

      System.out.println("Processing '" + iname + "'...");

      for (UnitOfWork iworkUnit : iworkUnits) {
        // add to obatch
        obatch.addWorkUnit(iworkUnit);

        if (c.matches(iworkUnit)) {
          // switch to obatch2
          obatch = obatch2;

          System.out.println("\tsplitting at '" + iworkUnit + "'");
        }
      }

      obatch1.save();
      obatch2.save();
    }
  }
}
