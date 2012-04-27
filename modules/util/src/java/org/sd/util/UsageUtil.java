/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.sd.util.ReflectUtil;

/**
 * Utility class for reflectively getting usage notes from a class or method.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "This utility extracts usage (annotation) notes from classes.\n" +
       "An example interaction would be:\n" +
       "\n" +
       "  import org.sd.util.UsageUtil;\n" +
       "  System.out.println(UsageUtil.asString(UsageUtil.getUsageNotes(className, immediateOnly)));"
)
public class UsageUtil {

  /**
   * Get usage notes for the named class, returning null if class not found
   * and an empty result if the found class has no usage notes.
   */
  @Usage(notes = "Get usage notes for the named class.")
  public static final Map<String, List<String>> getUsageNotes(String className, boolean immediateOnly) {
    Map<String, List<String>> result = null;

    try {
      final Class<?> clazz = ReflectUtil.getClass(className);
      result = getUsageNotes(clazz, immediateOnly);
    }
    catch (IllegalArgumentException e) {
      result = null;
    }

    return result;
  }

  /**
   * Get the (declared/immediate) usage (annotation) notes for the given class
   * and its methods.
   * <p>
   * Entries in the map are the class's usage notes (mapped by className) if
   * present followed by the existing declared usage (annotations) of its
   * methods (mapped by methodName).
   * <p>
   * If the class is null, a null map will be returned.
   * <p>
   * If the class has no usage notes, the returned map will be empty.
   */
  @Usage(notes = "Get usage notes for the class.")
  public static final Map<String, List<String>> getUsageNotes(Class<?> clazz, boolean immediateOnly) {
    Map<String, List<String>> result = null;

    if (clazz != null) {
      result = new LinkedHashMap<String, List<String>>();

      collectUsageAnnotations(result, clazz.getName(), clazz, immediateOnly);

      final Method[] methods = clazz.getDeclaredMethods();
      for (Method method : methods) {
        collectUsageAnnotations(result, method.getName(), method, immediateOnly);
      }
    }

    return result;
  }

  private static final void collectUsageAnnotations(Map<String, List<String>> result, String name, AnnotatedElement elt, boolean immediateOnly) {

    final Annotation[] annotations = immediateOnly ? elt.getDeclaredAnnotations() : elt.getAnnotations();
    for (Annotation annotation : annotations) {
      final Class<? extends Annotation> annotationType = annotation.annotationType();
      if (Usage.class.getName().equals(annotationType.getName())) {
        final Usage usage = (Usage)annotation;
        List<String> notes = result.get(name);
        if (notes == null) {
          notes = new ArrayList<String>();
          result.put(name, notes);
        }
        notes.add(usage.notes());
        break;
      }
    }
  }


  @Usage(notes = "Format notes into columns.")
  public static String asString(Map<String, List<String>> usageNotes) {
    if (usageNotes == null) return null;

   final StringBuilder result = new StringBuilder();

    // determine the (first) column width
    int colWidth = 0;
    for (String name : usageNotes.keySet()) {
      if (name.length() > colWidth) colWidth = name.length();
    }
    colWidth += 2;

    // show data
    for (Map.Entry<String, List<String>> entry : usageNotes.entrySet()) {
      final String name = entry.getKey();
      final List<String> notes = entry.getValue();
      final int nameLen = name.length();

      for (String notesInstance : notes) {
        if (result.length() > 0) result.append('\n');
        for (int i = 0; i < colWidth - nameLen - 2; ++i) result.append(' ');
        result.append(name).append("  ");
        final String[] noteLines = notesInstance.split("\n");
        result.append(noteLines[0]);
        for (int j = 1; j < noteLines.length; ++j) {
          result.append('\n');
          for (int i = 0; i < colWidth; ++i) result.append(' ');
          result.append(noteLines[j]);
        }
      }
    }

    return result.toString();
  }


  public static void main(String[] args) throws IOException {
    // Properties:
    //   immediateOnly -- (optional, default=true) true or false for including only immediate or all annotations.
    //
    // Args:
    //   class names (fully qualified) whose usage notes to retrieve.
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties p = pp.getProperties();
    args = pp.getArgs();

    final boolean immediateOnly = "true".equals(p.getProperty("immediateOnly", "true"));

    for (String arg : args) {
      final Map<String, List<String>> usageNotes = getUsageNotes(arg, immediateOnly);
      System.out.println(asString(usageNotes));
    }
  }
}
