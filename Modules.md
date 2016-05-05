

# Introduction #

The project modules are broken up into the two categories of **base** and **advanced** modules.

**Base** modules provide core classes and methods for building projects and advanced modules.

**Advanced** modules rely on the base modules for solving more complex problems.


# Details #

## Base Modules ##

The base modules are named "io", "util", "xml", "nlp", "cio", "bdb", and "text".

These modules are split by their dependencies on each other. Each module can be used as a standalone as long as the modules it depends on are also available. For convenience, all of the "base" modules are packaged into a "base" jar so that each module does not need to be referenced separately. Modules are provided in jars according to the naming convention of "sd-_module_._date_.jar", where _module_ is the module name or "base" for the aggregation of all base modules.

### io ###
This module holds simple wrappers and utilities for dealing with common input/output tasks, featuring:

  * FileUtil -- a class with static utility methods for obtaining handles on streams and readers for content that may be gzipped
  * Publishable -- an interface for serialization and marshaling of objects 'across the wire" through DataInput/Output streams
  * DataHelper -- a class with static utility methods for serializing/deserializing publishable instances

### util ###
This module holds simple wrappers, utilities, and algorithm implementations for dealing with common general tasks, featuring:

  * various string manipulation utilities including character encodings, dates, regular expressions, hashing, and floating point strings
  * various containers for histograms, bit vectors, properties, and caches
  * numeric range and statistical utilities
  * ReflectUtil -- a wrapper for java reflection
  * tree -- a package with a generic implementation of a tree of connected data containing nodes including various traversal utilities
  * logic, threading, and finite state grammar discovery

### xml ###
This module holds a DOM- and XPath-like model for representing and working with xml. It does not implement the DOM specifications, but provides instead a light-weight implementation to be used in cases where speed and efficiency are required while processing xml documents. The package features:

  * XmlLite -- A light-weight xml parsing infrastructure creating util.tree.Tree<XmlLite.Data> nodes from xml input
  * Xml Rippers -- utilities for incrementally processing xml data without loading full documents into memory
  * XmlTreeHelper -- static utility methods for manipulating xml content

### nlp ###
This module provides basic finite state machine based context free grammar phrase parsing of natural language sentences as well as normalization, tokenization, and string wrapping interfaces and implementations. The string manipulation is geared toward preserving the original input with back-mappings while efficiently utilizing potentially multiple normalizations for different purposes.

### cio ###
This module holds more complex input/output utilities that rely on other modules. For example, the MessageHelper uses the util module's ReflectUtil to marshal objects "across the wire," so to speak.

### bdb ###
This module provides a wrapper around Berkeley DB for simplified usage of the database as a time based queue and as a simple database-backed map for large quantities of data.
text
This module combines elements from the other base modules to implement more complex algorithms. Included are utilities for searching for common substring matches within strings, including applying logic to the matching of sets of strings within text.

### text ###
This module combines elements from the other base modules to implement more complex algorithms. Included are utilities for searching for common substring matches within strings, including applying logic to the matching of sets of strings within text.

## Advanced Modules ##
The advanced modules depend on the base modules but not on each other and are named "cluster", "extract", "lang", and "crawl".

### cluster ###
This module serves as a framework for parallel distributed processing across (linux) machines. It is similar to the hadoop project. As it was developed during the same time frame and without knowledge of the other project, it is independent of hadoop and differs in many respects. The model allows multiple "nodes" (jvms) per machine to work on jobs in parallel using a "push forward" model. Current efforts are driving toward an implementation of the MapReduce processing model.

### extract ###
This module is still evolving. It defines frameworks for information extraction and classification.

  * The extraction framework provides mechanisms for applying multiple extractors to input. It includes mechanisms for correlation and extrapolation of fields.
  * The classification framework can be thought of as a wrapper around weka that provides for the same code paths to be executed during training and executing models.

### lang ###
This module is still evolving and stands as a placeholder for creating language-specific natural language processing models. It includes a java wrapper around Princeton's WordNet.

### crawl ###
This module provides capabilities for fetching, analyzing, and organizing web pages dynamically retrieved from the internet.