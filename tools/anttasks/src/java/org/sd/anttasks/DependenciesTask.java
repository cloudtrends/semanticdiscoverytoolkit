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
package org.sd.anttasks;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Reference;

/**
 * An ant task for working with dependency information.
 * <p>
 * Given a project defined by modules and their immediate dependencies in the
 * form of libraries (libs.txt) and other modules (deps.txt), generate
 * classpath and other dependency information for a module by following the
 * dependency chains.
 * <p>
 * NOTE: Only existing files/directories will actually be added to the path
 *       and filelist.
 * <p>
 * Attributes that are patterns serve to identify the path from a root to
 * a module component and allow for a moduleName to be substituted in the
 * pattern wherever a "%m" is found.
 *
 * @author Spence Koehler
 */
public class DependenciesTask extends Task {

  private String cpid;              // (output reference) id for generated classpath (path)
  private String distid;            // (output reference) id for generated dist filelist
  private String depby;             // (output) name for property to hold a space-delimited ordered list of modules that depend on the module.
  private String depbuildsid;       // (output reference) id for depending module build file filelist.

  private String testRefId;         // (input) path reference id for test classes for classpath prefix
  private String moduleName;        // module whose dependencies will be analyzed
  private File modulesRoot;         // path to modules root (null if no active module classes are to be considered)
  private File libsRoot;            // path to libs root
  private File moduleJarsRoot;      // path to module jars root
  private File localdist;           // path to localdist root
  private File depsRoot;            // path to deps root (from which depsFilePattern applies for all modules)
  private String modulesDirPattern; // patttern to identify moduleDir from the modulesRoot (defaults to "%m")
  private String classesPath;       // path to a module's classes from the modulesRoot (defaults to "build/classes")
  private String moduleRegex;       // (optional, default="^(.*)$$") regex to identify dependent module name (in group 1) from a deps file line
  private String depsFilePattern;   // pattern to identify depsFile (use %m to represent moduleName)
  private String libsFilePattern;   // pattern to identify libsFile (use %m to represent moduleName) relative to depsRoot
  private String libsFilterRegex;   // a regex to (inclusively) filter dependent module libraries (no filtering if absent)
  private String moduleJarPattern;  // pattern to identify a module's jar relative to moduleJarsRoot (defaults to "%m.jar")
  private boolean failOnMissing;    // (default=false) if true, then fail when a module jar or lib is missing
  private boolean flatlibs;         // (default=true) if true, then loaded libs.txt paths will be flattened
  private String modorder;          // space delimited string of all module names in order from having the least to the most dependencies
  private String antBuildName;      // name of module ant build files (default="build.xml")

  // NOTE: deps search path is (1) module's own area, (2) localdist area, (3) common area
  private String localDepsPath;     // (defaults to "deps") path to local module dependencies root containing <moduleName>.dep and <moduleName>.lib (overriding depsRoot if existing)


  public DependenciesTask() {
    super();
  }

  /**
   * Set (reference) id for generated classpath (path).
   */
  public void setCpid(String cpid) {
    this.cpid = cpid;
  }

  /**
   * Set (reference) id for generated dist filelist.
   */
  public void setDistid(String distid) {
    this.distid = distid;
  }

  /**
   * Set name for property to hold a space-delimited ordered list of modules
   * that depend on the module.
   */
  public void setDepby(String depby) {
    this.depby = depby;
  }

  /**
   * Set (reference) id for depending module build file filelist.
   */
  public void setDepbuildsid(String depbuildsid) {
    this.depbuildsid = depbuildsid;
  }


  /**
   * Set (input) path reference id for test classes for classpath prefix.
   */
  public void setTestRefId(String testRefId) {
    this.testRefId = testRefId;
  }

  /**
   * Set module whose dependencies will be analyzed.
   */
  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  /**
   * Set path to modules root.
   */
  public void setModulesRoot(File modulesRoot) {
    this.modulesRoot = modulesRoot;
  }

  /**
   * Set path to libs root.
   */
  public void setLibsRoot(File libsRoot) {
    this.libsRoot = libsRoot;
  }

  /**
   * Set path to module jars root.
   */
  public void setModuleJarsRoot(File moduleJarsRoot) {
    this.moduleJarsRoot = moduleJarsRoot;
  }

  /**
   * Set path to localdist root.
   */
  public void setLocaldist(File localdist) {
    this.localdist = localdist;
  }

  /**
   * Set path to deps root (from which depsFilePattern applies for all modules).
   */
  public void setDepsRoot(File depsRoot) {
    this.depsRoot = depsRoot;
  }


  /**
   * Set patttern to identify moduleDir from the modulesRoot (defaults to "%m")
   */
  public void setModulesDirPattern(String modulesDirPattern) {
    this.modulesDirPattern = modulesDirPattern;
  }

  /**
   * Set path to a module's classes from the modulesRoot (default="build/classes").
   */
  public void setClassesPath(String classesPath) {
    this.classesPath = classesPath;
  }

  /**
   * Set (optional, default="^(.*)$$") regex to identify dependent module name (in group 1) from a deps file line.
   */
  public void setModuleRegex(String moduleRegex) {
    this.moduleRegex = moduleRegex;
  }

  /**
   * Set pattern to identify depsFile relative to depsRoot (use %m to represent moduleName).
   */
  public void setDepsFilePattern(String depsFilePattern) {
    this.depsFilePattern = depsFilePattern;
  }

  /**
   * Set pattern to identify libsFile relative to depsRoot (use %m to represent moduleName).
   */
  public void setLibsFilePattern(String libsFilePattern) {
    this.libsFilePattern = libsFilePattern;
  }

  /**
   * Set a regex to (inclusively) filter dependent module libraries (no filtering if absent).
   */
  public void setLibsFilterRegex(String libsFilterRegex) {
    this.libsFilterRegex = libsFilterRegex;
  }

  /**
   * Set pattern to identify a module's jar relative to modulesRoot (defaults to "%m.jar").
   */
  public void setModuleJarPattern(String moduleJarPattern) {
    this.moduleJarPattern = moduleJarPattern;
  }

  /**
   * Set failOnMissing (default=false).
   * <p>
   * @param failOnMissing  If true, then fail when a module jar or lib is missing.
   */
  public void setFailOnMissing(boolean failOnMissing) {
    this.failOnMissing = failOnMissing;
  }

  /**
   * Set flatlibs (default=false).
   * <p>
   * @param flatlibs  If true, then loaded libs.txt paths will be flattened.
   */
  public void setFlatlibs(boolean flatlibs) {
    this.flatlibs = flatlibs;
  }

  /**
   * Set space delimited string of all module names in order from having the least to the most dependencies.
   */
  public void setModorder(String modorder) {
    this.modorder = modorder;
  }

  /**
   * Set space delimited string of all module names in order from having the least to the most dependencies.
   */
  public void setAntBuildName(String antBuildName) {
    this.antBuildName = antBuildName;
  }

  /**
   * Set path to local module dependencies root containing <moduleName>.dep
   * and <moduleName>.lib (overriding depsRoot if existing). [defaults to
   * "deps"]
   */
  public void setLocalDepsPath(String localDepsPath) {
    this.localDepsPath = localDepsPath;
  }

  /**
   * Execute this task.
   */
  public void execute() throws BuildException {
    final Project project = getProject();

    // if cpid, distid, or depby is null, then we won't do their work
    // if cpid, distid, or depby is an existing reference, then we won't do (repeat) the work
    final boolean setCpid = (cpid != null) && (project.getReference(cpid) == null);
    final boolean setDistid = (distid != null) && (project.getReference(distid) == null);
    final boolean setDepby = (depby != null) && (project.getProperty(depby) == null);
    final boolean setDepbuildsid = (depbuildsid != null) && (project.getReference(depbuildsid) == null);

    if (!setCpid && !setDistid && !setDepby) return;  // nothing to do.

    //
    // verify the inputs
    //
    // must specify moduleName
    if (moduleName == null || "".equals(moduleName)) {
      throw new BuildException("Must specify a 'moduleName'!");
    }
    if (libsRoot != null && !libsRoot.exists()) {
      throw new BuildException("'libsRoot' (" + libsRoot.getAbsolutePath() + ") must exist!");
    }
    if (depsRoot != null && !depsRoot.exists()) {
      throw new BuildException("'depsRoot' (" + depsRoot.getAbsolutePath() + ") must exist!");
    }
    if (modulesRoot != null && !modulesRoot.exists()) {
      throw new BuildException("'modulesRoot' (" + modulesRoot.getAbsolutePath() + ") must exist!");
    }
    if (moduleJarsRoot != null && !moduleJarsRoot.exists()) {
      throw new BuildException("'moduleJarsRoot' (" + moduleJarsRoot.getAbsolutePath() + ") must exist!");
    }
    if (setDepbuildsid && modulesRoot == null) {
      throw new BuildException("Must deinfe 'modulesRoot' when 'depbuildsid' is present.");
    }

    if (setDepby && (modorder == null || "".equals(modorder))) {
      throw new BuildException("Must define 'modorder' when 'depby' is present.");
    }

    if (depsRoot == null && libsFilePattern != null) {
      throw new BuildException("Must define 'depsRoot' when 'libsFilePattern' is present.");
    }
    if (depsRoot == null && depsFilePattern != null) {
      throw new BuildException("Must define 'depsRoot' when 'depsFilePattern' is present.");
    }

    // WARN that we won't be able to parse dependencies if depsFilePattern is missing
    if (depsFilePattern == null || "".equals(depsFilePattern)) {
      // Must specify the depsFilePattern to locate the dep
      project.log("No 'depsFilePattern' yields no dependency checking", project.MSG_WARN);
    }

    // WARN that we won't be able to parse libs if libsFilePattern is missing
    if (libsFilePattern == null || "".equals(libsFilePattern)) {
      // Must specify the libsFilePattern to locate the dep
      project.log("No 'libsFilePattern' yields no library checking", project.MSG_WARN);
    }

    //
    // Create dependency builder instance to collect information.
    //
    final DependencyBuilder builder =
      new DependencyBuilder(project, modulesRoot, libsRoot, moduleJarsRoot, localdist,
                            depsRoot, modulesDirPattern, classesPath, moduleRegex,
                            depsFilePattern, libsFilePattern, libsFilterRegex,
                            moduleJarPattern, testRefId, failOnMissing, flatlibs,
                            modorder, antBuildName, localDepsPath);

    //
    // create path for cpid (if warranted)
    //
    if (setCpid) {
      final Path path = new Path(project);

      // add this module's classpath
      builder.addTestRefId(path);
      builder.addModuleClasspath(path, moduleName, false, true);  // add main module's classpath without filtering
      builder.addDependentClasspaths(path, moduleName);     // add module's dependencies

      // add the reference to this new path in the project
      project.addReference(cpid, path);
    }

    //
    // create filelist for distid (if warranted)
    //
    if (setDistid) {
      // create a filelist holding a module's jar and libs along with dependent module jars and libs.
      Set<String> dependentLibs = builder.getDependentLibs(moduleName);
      final String dlibsString = asString(dependentLibs);

      final FileList filelist = new FileList();
      if (dlibsString != null) filelist.setFiles(dlibsString);
      project.addReference(distid, filelist);
    }

    //
    // create backward dependencies filelist for depby (if warranted)
    //
    if (setDepby || setDepbuildsid) {
      // create a filelist holding the build.xml files for dependent modules in order
      final String dependingMods = builder.getDependingModules(moduleName);
      if (dependingMods != null && setDepby) {
        project.setProperty(depby, dependingMods);

        if (setDepbuildsid) {
          Set<String> dependingBuilds = builder.getDependingBuilds(dependingMods);
          final String dbuildsString = asString(dependingBuilds);

          final FileList filelist = new FileList();
          if (dbuildsString != null) filelist.setFiles(dbuildsString);
          project.addReference(depbuildsid, filelist);
        }
      }
    }
  }

  // protected for junit access
  protected static final String asString(Set<String> strings) {
    StringBuilder result = null;

    if (strings != null && strings.size() > 0) {
      result = new StringBuilder();
      for (String string : strings) {
        if (result.length() > 0) result.append(' ');
        result.append(string);
      }
    }

    return result == null ? null : result.toString();
  }

  public static final class ModulePattern {
    private String pattern;

    public ModulePattern(String pattern) {
      this.pattern = pattern;
    }

    public String getPattern() {
      return pattern;
    }

    /**
     * Apply this pattern to the module name, returning the resultant string.
     * <p>
     * NOTE: escaping the '%m' is currently not supported. implement if/when it is needed.
     */
    public String apply(String moduleName) {
      String result = moduleName;

      if (pattern != null) {
        result = pattern.replaceAll("%m", moduleName);
      }

      return result;
    }
  }

  /**
   * Container for a module's module dependencies.
   */
  public static final class ModuleDeps {
    public final String module;
    public final List<String> deps;

    public ModuleDeps(String module, List<String> deps) {
      this.module = module;
      this.deps = deps;
    }

    public boolean hasDep(String moduleName) {
      return deps != null && deps.contains(moduleName);
    }
  }

  /**
   * Container for a module's library dependencies.
   */
  public static final class ModuleLibs {
    public final String module;
    public final List<String> libs;

    public ModuleLibs(String module, List<String> libs) {
      this.module = module;
      this.libs = libs;
    }

    public boolean hasLib(String moduleName) {
      return libs != null && libs.contains(moduleName);
    }

    public String toString() {
      return "ModuleLibs[" + module + "," + (libs == null ? 0 : libs.size()) + "]";
    }
  }

  public static final class DependencyBuilder {

    private Project project;
    private File modulesRoot;         // path to modules root (null if no module jars are to be considered)
    private File libsRoot;            // path to libs root
    private File moduleJarsRoot;      // path to module jars root
    private File localdist;           // path to localdist root
    private File depsRoot;            // path to deps root (from which depsFilePattern applies for all modules)
    private ModulePattern modulesDirPattern; // patttern to identify moduleDir from the modulesRoot (defaults to "%m")
    private String classesPath;       // path to a module's classes from the modulesRoot (defaults to "build/classes")
    private Pattern moduleRegex;       // (optional, default="^(.*)$$") regex to identify dependent module name (in group 1) from a deps file line
    private ModulePattern depsFilePattern;   // pattern to identify depsFile (use %m to represent moduleName)
    private ModulePattern libsFilePattern;   // pattern to identify libsFile (use %m to represent moduleName)
    private Pattern libsFilterRegex;  // a regex to (inclusively) filter dependent module libraries (no filtering if absent)
    private ModulePattern moduleJarPattern;  // pattern to identify a module's jar (defaults to "%m.jar")
    private String testRefId;         // (input) path reference id for test classes for classpath prefix
    private boolean failOnMissing;    // (default=false) if true, then fail when a module jar or lib is missing
    private boolean flatlibs;         // (default=true) if true, then loaded libs.txt paths will be flattened
    private String[] modorder;        // strings of all module names in order from having the least to the most dependencies
    private String antBuildName;      // name of module ant build files (default="build.xml")
    private String localDepsPath;     // (defaults to "deps") path to local module dependencies root containing <moduleName>.dep and <moduleName>.lib (overriding depsRoot if existing)

    private Map<String, ModuleDeps> mod2deps;  // map from moduleName to its ModuleDeps instance.
    private Map<String, ModuleLibs> mod2libs;  // map from moduleName to its ModuleLibs instance.

    public DependencyBuilder(Project project, File modulesRoot, File libsRoot,
                             File moduleJarsRoot, File localdist, File depsRoot,
                             String modulesDirPattern, String classesPath,
                             String moduleRegex, String depsFilePattern,
                             String libsFilePattern, String libsFilterRegex,
                             String moduleJarPattern, String testRefId,
                             boolean failOnMissing, boolean flatlibs, String modorder,
                             String antBuildName, String localDepsPath) {
      this.project = project;
      this.modulesRoot = modulesRoot;
      this.libsRoot = libsRoot;
      this.moduleJarsRoot = moduleJarsRoot;
      this.localdist = localdist;
      this.depsRoot = depsRoot;
      this.modulesDirPattern = new ModulePattern(modulesDirPattern == null ? "%m" : modulesDirPattern);
      this.classesPath = classesPath == null ? "build/classes" : classesPath;
      this.moduleRegex = (moduleRegex == null) ? null : Pattern.compile(moduleRegex);
      this.depsFilePattern = depsFilePattern == null ? null : new ModulePattern(depsFilePattern);
      this.libsFilePattern = libsFilePattern == null ? null : new ModulePattern(libsFilePattern);
      this.libsFilterRegex = libsFilterRegex == null ? null : Pattern.compile(libsFilterRegex);
      this.moduleJarPattern = new ModulePattern(moduleJarPattern == null ? "%m.jar" : moduleJarPattern);
      this.testRefId = testRefId;
      this.failOnMissing = failOnMissing;
      this.flatlibs = flatlibs;
      this.modorder = (modorder == null) ? null : modorder.split("\\s+");
      this.antBuildName = (antBuildName == null) ? "build.xml" : antBuildName;
      this.localDepsPath = localDepsPath == null ? "deps" : localDepsPath;

      this.mod2deps = new HashMap<String, ModuleDeps>();
      this.mod2libs = new HashMap<String, ModuleLibs>();
    }

    /**
     * Add the paths for testing referenced by the testRefId if specified.
     */
    public void addTestRefId(Path path) throws BuildException {
      if (testRefId != null) {
        final Object testPath = project.getReference(testRefId);
        if (testPath != null && testPath instanceof Path) {
          path.add((Path)testPath);
        }
        else {
          throw new BuildException("Bad 'testRefId' (" + testRefId + ")! [ref=" + testPath + "]");
        }
      }
    }

    /**
     * Add the given module's classpath to the given path.
     * <p>
     * NOTE: only existing files/directories will actually be added to the path.
     * <p>
     * A module's classpath consists of:
     * <ul>
     * <li>The module's built classes directory.</li>
     * <li>The localdist version of module's jar.</li>
     * <li>The module's jar.</li>
     * <li>The module's (filtered) libs.</li>
     * </ul>
     * @param path  The path to which the module classpath will be added.
     * @param moduleName  The module whose classpath will be added.
     * @param applyLibsFilter  If true, then the libsFilterRegex (if present) will be applied.
     * @param isFirst  True when the being called with the top module (jar not required to exist yet)
     */
    public void addModuleClasspath(Path path, String moduleName, boolean applyLibsFilter, boolean isFirst) throws BuildException {
      // add the module's built classes directory (if exists)
      final File classesDir = getModuleClassesDir(moduleName, isFirst);
      if (classesDir != null && isFirst) {  // only refer to compiled classes area for primary (first) module
        path.setLocation(classesDir);
      }

      // add the module's jar (localdist or common modlib if available/computable and exists)
      final File moduleJar = getJar(moduleName, isFirst);
      if (moduleJar != null) {
        path.setLocation(moduleJar);
      }

      // add the module's (filtered) libs
      final List<File> modLibs = getModuleLibs(moduleName, applyLibsFilter, isFirst);
      if (modLibs != null) {
        for (File modLib : modLibs) {
          path.setLocation(modLib);
        }
      }
    }

    /**
     * Add the classpaths for the modules on which the named module depends to
     * the given path.
     * <p>
     * NOTE: all classpaths will be added with the libsFilterRegex applied (if
     *       present).
     *
     * @param path  The path to which classpaths will be added.
     * @param moduleName  The module whose dependencies will be added.
     */
    public void addDependentClasspaths(Path path, String moduleName) throws BuildException {
      final Set<String> modules = new HashSet<String>();    // keep track of those we've already added

      modules.add(moduleName);  // assume we've already dealt with moduleName
      doAddDependentClasspaths(path, moduleName, modules, true);
    }


    /**
     * Recursive auxiliary to addDependentClasspaths.
     */
    private final void doAddDependentClasspaths(Path path, String moduleName, Set<String> modules, boolean isFirst) {

      final List<String> deps = getModuleDeps(moduleName, isFirst);

      if (deps != null) {
        for (String depModuleName : deps) {
          if (!modules.contains(depModuleName)) {
            modules.add(depModuleName);
            addModuleClasspath(path, depModuleName, true, false);

            // recurse to add dependencies for dependent module just added
            doAddDependentClasspaths(path, depModuleName, modules, false);
          }
        }
      }
    }

    /**
     * Add the module's jar and libs and the jars and libs of modules on which
     * it depends into the fileset.
     */
    public Set<String> getDependentLibs(String moduleName) throws BuildException {
      final Set<String> result = new LinkedHashSet<String>();  // preserve order while removing duplicates
      final Set<String> modules = new HashSet<String>();
      doGetDependentLibs(result, moduleName, modules, true);
      return result;
    }

    /**
     * Recursive auxiliary to addDependentLibs.
     */
    private final void doGetDependentLibs(Set<String> result, String moduleName, Set<String> modules, boolean isFirst) throws BuildException {
      if (!modules.contains(moduleName)) {
        modules.add(moduleName);

        // add module's jar
        if (!isFirst) {
          final File moduleJar = getJar(moduleName, false);
          if (moduleJar != null) result.add(moduleJar.getAbsolutePath());  // add it
        }

        // add module's libs
        final List<File> modLibs = getModuleLibs(moduleName, true, isFirst);  // get libs, apply filter
        if (modLibs != null) { // add 'em
          for (File modLib : modLibs) {
            result.add(modLib.getAbsolutePath());
          }
        }

        // get module's dependencies
        final List<String> deps = getModuleDeps(moduleName, isFirst);

        // add dependencies
        if (deps != null) {
          for (String depModuleName : deps) {
            doGetDependentLibs(result, depModuleName, modules, false);
          }
        }
      }
    }

    /**
     * Get a space-delimited ordered list of modules that depend on the module.
     */
    public String getDependingModules(String moduleName) throws BuildException {
      String result = null;

      if (modorder != null) {
        // collect depending modules
        final Set<String> results = new HashSet<String>();
        final Set<String> expanded = new HashSet<String>();
        doGetDependingModules(moduleName, results, expanded, true);

        // convert set of depending modules to ordered string
        final StringBuilder builder = new StringBuilder();
        for (String orderedmod : modorder) {
          if (results.contains(orderedmod)) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(orderedmod);
          }
        }
        result = builder.toString();
      }

      return result;
    }

    /**
     * Recursive auxiliary to getDependingModules.
     */
    private final void doGetDependingModules(String moduleName, Set<String> result, Set<String> expanded, boolean isFirst) throws BuildException {
      if (!expanded.contains(moduleName)) {  // for modules we haven't expanded yet
        expanded.add(moduleName);

        // find modules whose deps.txt contains moduleName
        for (String orderedmod : modorder) {
          if ("".equals(orderedmod)) continue;
          final ModuleDeps moduleDeps = getDeps(orderedmod, isFirst);
          if (moduleDeps.hasDep(moduleName)) {
            result.add(orderedmod);
            doGetDependingModules(orderedmod, result, expanded, false);
          }
        }
      }
    }

    public Set<String> getDependingBuilds(String dependingMods) {
      Set<String> result = null;

      if (dependingMods != null && !"".equals(dependingMods) && modulesRoot != null) {
        final String[] modules = dependingMods.split(" +");
        for (String module : modules) {
          // modulesRoot/module/antBuildName
          final File moduleDir = new File(modulesRoot, module);
          final File buildfile = new File(moduleDir, antBuildName);
          if (buildfile.exists()) {
            if (result == null) result = new LinkedHashSet<String>();
            result.add(buildfile.getAbsolutePath());
          }
          else {
            project.log("Can't find depending buildfile '" + buildfile + "'", project.MSG_WARN);
          }
        }
      }

      return result;
    }

    /**
     * Get the file pointing to the (existing) module directory.
     *
     * @return the existing module directory file, or null if it doesn't exist
     *         or we are unable to compute it.
     */
    private final File getModuleDir(String moduleName) {
      File result = null;

      if (modulesRoot != null) {
        // modulesRoot/modulesDirPattern
        result = new File(modulesRoot, modulesDirPattern.apply(moduleName));
      }

      return result != null && result.exists() ? result : null;
    }

    /**
     * Get the classes dir for a module.
     */
    private final File getModuleClassesDir(String moduleName, boolean isFirst) {
      File result = null;

      final File moduleDir = getModuleDir(moduleName);
      if (moduleDir != null) {
        result = new File(moduleDir, classesPath);
      }

      return result != null && (isFirst || result.exists()) ? result : null;
    }

    /**
     * Get the localdist jar for a module.
     */
    private final File getLocalJar(String moduleName) throws BuildException {
      File result = null;

      if (localdist != null) {
        // localdist/moduleJarPattern
        result = new File(localdist, moduleJarPattern.apply(moduleName));
      }

      return result == null ? result : result.exists() ? result : null;
    }

    /**
     * Get the jar for a module.
     */
    private final File getModuleJar(String moduleName, boolean isTop) throws BuildException {
      File result = null;

      if (moduleJarsRoot != null) {
        // moduleJarsRoot/moduleJarPattern
        result = new File(moduleJarsRoot, moduleJarPattern.apply(moduleName));
      }

      if (!isTop && failOnMissing && result != null && !result.exists()) {
        throw new BuildException("Required jar '" + result + "' is missing!");
      }

      return result == null ? result : result.exists() ? result : null;
    }

    /**
     * Get the libs file for a module.
     * <p>
     * NOTE: libs search path is (1) module's own area, (2) localdist area, (3) common area
     */
    private final File getModuleLibsFile(String moduleName, boolean isFirst) {
      File result = null;

      if (isFirst) {
        // get from module's own area (for top module only)
        final File moduleDir = getModuleDir(moduleName);
        if (moduleDir != null) {
          final File localDepsDir = new File(moduleDir, localDepsPath);
          result = new File(localDepsDir, libsFilePattern.apply(moduleName));
          if (!result.exists()) result = null; // allow fallback if missing
        }
      }

      if (result == null) {
        // get from localdist area
        if (localdist != null) {
          result = new File(localdist, libsFilePattern.apply(moduleName));
          if (!result.exists()) result = null;  // allow fallback if missing
        }
      }

      if (result == null) {
        // get from common area
        if (depsRoot != null && libsFilePattern != null) {
          // depsRoot/libsFilePattern
          result = new File(depsRoot, libsFilePattern.apply(moduleName));
        }
      }

      return result == null ? result : result.exists() ? result : null;
    }

    /**
     * Get the deps file for a modules.
     * <p>
     * NOTE: deps search path is (1) module's own area, (2) localdist area, (3) common area
     */
    private final File getModuleDepsFile(String moduleName, boolean isFirst) {
      File result = null;

      if (isFirst) {
        // get from module's own area (for top module only)
        final File moduleDir = getModuleDir(moduleName);
        if (moduleDir != null) {
          final File localDepsDir = new File(moduleDir, localDepsPath);
          result = new File(localDepsDir, depsFilePattern.apply(moduleName));
          if (!result.exists()) result = null; // allow fallback if missing
        }
      }

      if (result == null) {
        // get from localdist area
        if (localdist != null) {
          result = new File(localdist, depsFilePattern.apply(moduleName));
          if (!result.exists()) result = null;  // allow fallback if missing
        }
      }

      if (result == null) {
        // get from common area
        if (depsRoot != null && depsFilePattern != null) {
          // depsRoot/depsFilePattern
          result = new File(depsRoot, depsFilePattern.apply(moduleName));
        }
      }

      return result == null ? result : result.exists() ? result : null;
    }

    /**
     * Read and return the (existing, filtered) libs from the module's libs file.
     */
    private final List<File> getModuleLibs(String moduleName, boolean applyLibsFilter, boolean isFirst) throws BuildException {
      List<File> result = null;

      final ModuleLibs moduleLibs = getLibs(moduleName, isFirst);
      if (moduleLibs.libs != null) {
        for (String lib : moduleLibs.libs) {
          if (!applyLibsFilter || passesLibsFilter(lib)) {

            //NOTE: libs.txt entries are relative paths from libsRoot to libs
            //      if flatlibs, then need to strip leading paths to find lib
            if (flatlibs) {
              final String[] libpath = lib.split(":");
              lib = libpath[libpath.length - 1];
            }
            
            final File libFile = new File(libsRoot, lib);
            
            if (libFile.exists()) {
              if (result == null) result = new ArrayList<File>();
              result.add(libFile);
            }
            else if (failOnMissing) {
              throw new BuildException("Required jar '" + result + "' is missing!");
            }
          }
        }
      }

      return result;
    }

    /**
     * Determine whether the given lib passes the libs filter.
     * <p>
     * Applies the libsFilterRegex as an INCLUSIVE filter; that is, unless the
     * lib matches the regex, it will not "pass".
     */
    private final boolean passesLibsFilter(String lib) {
      boolean result = true;

      if (libsFilterRegex != null) {
        final Matcher m = libsFilterRegex.matcher(lib);
        result = m.matches();
      }

      return result;
    }

    /**
     * Read and return the modules from the module's deps file.
     */
    private final List<String> getModuleDeps(String moduleName, boolean isFirst) throws BuildException {
      final ModuleDeps deps = getDeps(moduleName, isFirst);
      return deps.deps;
    }

    /**
     * Get the module's jar. If !isFirst, throw a BuildException if no jar exists.
     * <p>
     * NOTE: module jar search path is (1) localdist area, (2) common modlib area
     */
    private final File getJar(String moduleName, boolean isFirst) throws BuildException {
      File result = null;

      // find the module's localdist jar (if available/computable and exists)
      result = getLocalJar(moduleName);
      if (result == null) {
        // find the module's common modlib jar (if available/computable and exists)
        result = getModuleJar(moduleName, isFirst);
      }

      return result;
    }

    /**
     * Get the module's libs from the cache, loading if necessary.
     */
    private final ModuleLibs getLibs(String moduleName, boolean isFirst) throws BuildException {
      ModuleLibs result = mod2libs.get(moduleName);

      if (result == null) {

        List<String> libs = null;
        if (libsRoot != null) {
          final File libsFile = getModuleLibsFile(moduleName, isFirst);
          try {
            libs = loadLines(libsFile);
          }
          catch (IOException e) {
            throw new BuildException(e);
          }
        }
        result = new ModuleLibs(moduleName, libs);
        
        mod2libs.put(moduleName, result);
      }

      return result;
    }

    /**
     * Get the module's dependencies from the cache, loading if necessary.
     */
    private final ModuleDeps getDeps(String moduleName, boolean isFirst) throws BuildException {
      ModuleDeps result = mod2deps.get(moduleName);

      if (result == null) {

        List<String> deps = null;
        if (depsRoot != null) {
          List<String> fileDeps = null;
          final File depsFile = getModuleDepsFile(moduleName, isFirst);
          try {
            fileDeps = loadLines(depsFile);
          }
          catch (IOException e) {
            throw new BuildException(e);
          }

          if (fileDeps != null) {
            // apply moduleRegex if non-null
            if (moduleRegex != null) {
              deps = new ArrayList<String>();
              for (String dep : fileDeps) {
                final Matcher m = moduleRegex.matcher(dep);
                if (m.matches()) {
                  deps.add(m.group(1));
                }
              }
            }
            else {
              deps = fileDeps;
            }
          }
        }
        result = new ModuleDeps(moduleName, deps);
        
        mod2deps.put(moduleName, result);
      }

      return result;
    }

    /**
     * Load non-empy, non-comment lines from a file.
     */
    private final List<String> loadLines(File file) throws IOException {
      List<String> result = null;

      if (file != null) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line = null;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if ("".equals(line) || line.startsWith("#")) continue;  // skip empty/comment lines

          if (result == null) result = new ArrayList<String>();
          result.add(line);
        }
        reader.close();
      }

      return result;
    }
  }
}
