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
package org.sd.io;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Miscellaneous file utilities.
 * <p>
 * @author Spence Koehler
 */
public class FileUtil {
  private final static int ARRAY_SIZE = 1000;
  
  /**
   * deletes all the files in the specified directory
   *
   * @param dirName
   * @return true if files successfully deleted.
   */
  public static boolean deleteFiles(String dirName) {
    return deleteDir(getFile(dirName), false, null);
  }

  public static boolean deleteDirContents(File dir) {
    return deleteDir(dir, false, null);
  }

  public static boolean deleteDir(File dir) {
    return deleteDir(dir, true, null);
  }

  public static boolean deleteDir(File dir, boolean includeThisDir, FilenameFilter exceptions) {

    boolean success = true;

    if (includeThisDir && exceptions != null && exceptions.accept(dir, dir.getName())) return success;

    if (dir.exists()) {
      if (dir.isDirectory()) {
        final File[] files = dir.listFiles();
        if (files != null) {
          for (File file : files) {
            success &= deleteDir(file, true, exceptions);
          }
        }

        if (includeThisDir) {
          success &= dir.delete();
        }
      }
      else {
        success = dir.delete();
      }
    }
    return success;
  }

  /**
   * Reads a text file and creates a single {@link String} instance to hold its contents.
   * 
   * @param file input file
   * @return text file contents as a String
   * @throws IOException
   */
  public static String getTextFileAsString(File file) throws IOException {
    StringBuilder buf = new StringBuilder();
    BufferedReader reader = getReader(file);
    String line;
    
    while ((line = reader.readLine()) != null) {
      buf.append(line + "\n");
    }
    
    reader.close();
    return buf.toString();
  }
  
  /**
   * Find the file with the given filename in the directory.
   *
   * @return the file or null if it does not exist.
   */
  public static final File findFile(File dir, final String filename, final boolean acceptAsPart) {
    File result = null;

    if (dir.isDirectory()) {
      File[] files = dir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            boolean result = false;

            if (acceptAsPart) {
              result = (name.indexOf(filename) >= 0);
            }
            else {
              result = filename.equals(name);
            }

            return result;
          }
        });
      if (files != null && files.length > 0) {
        result = files[0];
      }
    }

    return result;
  }

  /**
   * Find files whose names match the pattern in the given dir.
   *
   * @return the files matching the pattern or null if dir doesn't exist or an
   *         error occurs.
   */
  public static final File[] findFiles(File dir, final Pattern namePattern) {
    File[] result = null;

    if (dir != null) {
      result = dir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            final Matcher m = namePattern.matcher(name);
            return m.matches();
          }
        });
    }

    return result;
  }

  /**
   * Find files whose names match the pattern in the given dir and are 
   * descendants under the given dir.
   *
   * @return the files matching the pattern or null if dir doesn't exist or an
   *         error occurs.
   */
  public static final File[] findSubFiles(File dir, final Pattern namePattern) 
  {
    if(dir == null)
      return null;

    List<File> result = new ArrayList<File>();
    File[] files = dir.listFiles(
      new FilenameFilter() {
        public boolean accept(File dir, String name) {
          final Matcher m = namePattern.matcher(name);
          return m.matches();
        }
      });
    if(files != null)
      result.addAll(Arrays.asList(files));
    
    
    File[] subfiles = dir.listFiles();
    if(subfiles == null)
      return null;

    for(File file : subfiles)
    {
      if(file.isDirectory())
      {
        File[] subFiles = findSubFiles(file, namePattern);
        if(subFiles != null)
          result.addAll(Arrays.asList(subFiles));
      }
    }
    
    if(result.size() > 0)
      return result.toArray(new File[0]);
    else
      return new File[] {};
  }

  /**
   * Copy the contents of a directory
	 *
	 * @param source Source directory
	 * @param dest Destination directory
	 * @return true if the directory successfully copied all contents, false if any files failed
	 * @throws IOException if the destination directory already exists
	 * @throws FileNotFoundException if the source directory does not exist, or the source path is not a directory, or the destination directory cannot be created
   */
  public static boolean copyDir(File source, File dest) 
		throws IOException
	{
		boolean result = true;

		if(!source.exists())
			throw new FileNotFoundException("Source directory('" + source.getAbsolutePath() + "') cannot be found");
		else if(!source.isDirectory())
			throw new FileNotFoundException("Source file('" + source.getAbsolutePath() + "') is not a directory");

		if(dest.exists())
			throw new IOException("Unable to overwrite existing destination file('" + dest.getAbsolutePath() + "')");
		else if(!dest.mkdirs())
			throw new FileNotFoundException("Unable to create parent directories for destination file('" + dest.getAbsolutePath() + "')");
			
		for(File subFile : source.listFiles())
		{
			File destSubFile = new File(dest, subFile.getName());
			if(subFile.isDirectory())
			{
				if(!copyDir(subFile, destSubFile))
					result = false;
			}
			else
			{
				if(!copyFile(subFile, destSubFile))
					result = false;
			}
		}

		return result;
	}

  public static boolean copyFile(File source, File dest) 
	{
    boolean result = true;

    FileInputStream in = null;
    FileOutputStream out = null;

    final byte[] buffer = new byte[32768];  // 32K buffer
    try {
      in = new FileInputStream(source);
      out = new FileOutputStream(dest);

      while (true) {
        final int n = in.read(buffer);
        if (n < 0) break;
        out.write(buffer, 0, n);
      }
    }
    catch (IOException e) {
      // failed to copy
      result = false;
    }
    finally {
      try {
        if (in != null) in.close();
        if (out != null) out.close();
      }
      catch (IOException e) {
        // failed to close
        result = false;
      }
    }

    return result;
  }

  /**
   * Copy a file, squashing CR (ascii 13) chars.
   */
  public static boolean copyFileNoCR(File source, File dest) {
    boolean result = true;

//     if (dest.getFreeSpace() < source.length() + 1073741824) {
//       return false;  // not enough disk space to copy and leave 1G buffer for other data.
//     }

    BufferedReader reader = null;
    BufferedWriter writer = null;
    final char[] cbuf = new char[32768];  // 32K buffer

    try {
      reader = getReader(source);
      writer = getWriter(dest);

      while (true) {
        final int n = reader.read(cbuf, 0, cbuf.length);
        if (n < 0) break;
        for (int i = 0; i < n; ++i) {
          final char c = cbuf[i];
          if (c != '\r') writer.write(c);
        }
      }
    }
    catch (IOException e) {
      // failed to copy
      result = false;
    }
    finally {
      try {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
      }
      catch (IOException e) {
        // failed to close
        result = false;
      }
    }

    return result;
  }

  /**
   * Get a buffered reader for a UTF-* and possibly gzipped file.
   *
   * @param file - file to read from.
   * @return BufferedReader that handles gzipped files (if filename ends with ".gz") and UTF-8 encoding.
   * @throws IOException
   */
  public static BufferedReader getReader(File file) throws IOException {
    InputStream inputStream = getInputStream(file);

    // prepare for reset if possible
    if (inputStream.markSupported()) {
      inputStream.mark(2);
    }

    // decode BOM
    final String charsetName = getCharsetName(inputStream);

    // reset the input stream if we didn't find BOM
    if (charsetName == null) {
      if (inputStream.markSupported()) {
        inputStream.reset();
      }
      else {
        inputStream.close();
        inputStream = getInputStream(file);
      }
    }

    return getReader(inputStream, charsetName == null ? "UTF-8" : charsetName);
  }

  /**
   * Read the unicode byte order marker (BOM) of the file and return
   * the appropriate charset. If there is no byte order marker, then
   * default to UTF-8.
   * <ul>
   * <li>FEFF == UTF-16BE (big endian)</li>
   * <li>FFFE == UTF-16LE (little endian)</li>
   * </ul>
   */
  public static final String getCharsetName(File file) throws IOException {

    final InputStream inputStream = getInputStream(file);
    final String result = getCharsetName(inputStream);
    inputStream.close();

    return result == null ? "UTF-8" : result;
  }

  /**
   * Read the 2 bytes from the inputStream and decode as a BOM if possible.
   *
   * @return the charset-name indicated by the BOM, or null if the bytes cannot
   *         be intepreted as a BOM.
   */
  public static final String getCharsetName(InputStream inputStream) throws IOException {
    String result = null;

    final byte[] bytes = new byte[2];
    inputStream.read(bytes);
    
    if (bytes[0] == -1/*0xFF*/ && bytes[1] == -2/*0xFE*/) {
      result = "UTF-16LE";
    }
    else if (bytes[0] == -2/*0xFE*/ && bytes[1] == -1/*0xFF*/) {
      result = "UTF-16BE";
    }

    return result;
  }

  /**
   * Get a buffered reader for a UTF-8 and possibly gzipped file.
   *
   * @param file - file to read from.
   * @param encoding - encoding of file (i.e. "UTF-8")
   * @return BufferedReader that handles gzipped files (if filename ends with ".gz") and UTF-8 encoding.
   * @throws IOException
   */
  public static BufferedReader getReader(File file, String encoding) throws IOException {
    InputStream inputStream = getInputStream(file);
    return getReader(inputStream, encoding);
  }

  public static BufferedReader getReaderNoCR(File file) throws IOException {
    return new BufferedReader(new InputStreamReaderNoCR(getInputStream(file)));
  }

  private static final class InputStreamReaderNoCR extends InputStreamReader {
    InputStreamReaderNoCR(InputStream istream) {
      super(istream);
    }

    public int read() throws IOException {
      int result = 0;

      do {
        result = super.read();
      } while (result == '\r');

      return result;
    }
  }

  /**
   * Get a buffered reader for a UTF-8 and possibly gzipped file.
   *
   * @param filename - file to read from.
   * @return BufferedReader that handles gzipped files (if filename ends with ".gz") and UTF-8 encoding.
   * @throws IOException
   */
  public static BufferedReader getReader(String filename) throws IOException {
    return getReader(filename, "UTF-8");
  }

  /**
   * Get a buffered reader for a UTF-8 and possibly gzipped file.
   *
   * @param filename - file to read from.
   * @param encoding - encoding of file (i.e. "UTF-8")
   * @return BufferedReader that handles gzipped files (if filename ends with ".gz") and UTF-8 encoding.
   * @throws IOException
   */
  public static BufferedReader getReader(String filename, String encoding) throws IOException {
    return getReader(getFile(filename), encoding );
  }

  /**
   * Get a utf8-safe buffered reader over data in the input stream.
   */
  public static BufferedReader getReader(InputStream istream) {
    return getReader(istream, "UTF-8");
  }

  /**
   * Get a utf8-safe buffered reader over data in the input stream.
   */
  public static BufferedReader getReader(InputStream istream, String encoding) {
    BufferedReader result = null;

    if (istream != null) {
      try {
        result = new BufferedReader(new InputStreamReader(istream, encoding));
      }
      catch (UnsupportedEncodingException e) {
        throw new RuntimeException("UTF-8 not supported in FileUtil.");
      }
    }

    return result;
  }

  /**
   * Get a utf8-safe buffered reader over data in the (possibly gzipped) resource.
   */
  public static BufferedReader getReader(Class clazz, String resource) throws IOException {
    return getReader(getInputStream(clazz, resource));
  }

  public static Properties getProperties(Class clazz, String resource) {
    final Properties result = new Properties();

    try {
      result.load(getInputStream(clazz, resource));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  /**
   * Get a normal input stream or a gzip input stream depending on whether
   * filename's extension is ".gz".
   * <p>
   * If the filename is of the form a:b and b doesn't start with a '/', then
   * assume 'a' is a class and 'b' is a resource relative to that class.
   */
  public static InputStream getInputStream(String filename) throws IOException {
    InputStream result = null;

    if (File.separatorChar != '/') {
      if (File.separatorChar == '\\') {
        filename = filename.replaceAll("/", "\\\\");
      }
      else {
        filename = filename.replaceAll("/", File.separator);
      }
    }

    final File file = getFile(filename);
    if (!file.exists()) {
      final String[] pieces = filename.split(":");

      if (pieces.length == 2 && pieces[1].length() > 0 && pieces[1].charAt(0) != File.separatorChar) {
        // assume we have classpath:resource
        try {
          result = getInputStream(Class.forName(pieces[0]), pieces[1]);
        }
        catch (ClassNotFoundException e) {
          // must have assumed the wrong form.
          result = null;
        }
      }
    }

    if (result == null) {
      result = getInputStream(file);
    }

    return result;
  }

  /**
   * Get a normal input stream or a gzip input stream depending on whether
   * filename's extension is ".gz".
   */
  public static InputStream getInputStream(File file) throws IOException {
    return (file != null && file.exists()) ? getInputStream(file.toURI().toURL()) : null;
  }

  /**
   * Get a normal input stream or a gzip input stream depending on whether
   * the url's extension is ".gz".
   */
  public static InputStream getInputStream(URL url) throws IOException {
    InputStream inputStream = null;

    if (url != null) {
      inputStream = url.openStream();
      if (url.getPath().toLowerCase().endsWith(".gz")) {
        // GZIPInputStream has a bug in reading 4GB+ files, so use the workaround wrapper to
        //  ignore the bogus "Corrupt Gzip Trailer" error.
        inputStream = new GZIPInputStreamWorkaround(inputStream);
      }
    }

    return inputStream;
  }

  public static URL getUrl(Class clazz, String resource) {
    if (File.separatorChar != '/') {
      if (File.separatorChar == '\\') {
        //resource = resource.replaceAll("/", "\\\\");
      }
      else {
        //resource = resource.replaceAll("/", File.separator);
      }
    }
    return clazz.getResource(resource);
  }

  /**
   * Get the filename for the class's resource suitable for opening.
   */
  public static String getFilename(Class clazz, String resource) {
    String filename = null;
    if (clazz == null) {
      filename = resource;
    }
    else {
      final URL url = getUrl(clazz, resource);
      if (url != null) {
        filename = url.toString();
      }
    }

    return filename;
  }

  /**
   * Get a file handle for the class's resource.
   */
  public static final File getFile(Class clazz, String resource) {
    File result = null;

    if (clazz != null) {
      try {
        final URL url = clazz.getResource(resource);
        result = new File(url.toURI());
      }
      catch (URISyntaxException eat) {}
    }

    if (result == null) {
      final String filename = getFilename(clazz, resource);
      result = filename == null ? null : getFile(filename);
    }

    return result;
  }

  /**
   * Get the main (as opposed to testing) resource file.
   */
  public static final File getResourceFile(Class clazz, String resource) {
//todo: use better mechanism for doing this!
    String result = FileUtil.getFilename(clazz, ".") + resource;
    result = result.replace("/junit-classes/", "/classes/");
    return FileUtil.getFile(result);
  }

  /**
   * Get the test (as opposed to main) resource file.
   */
  public static final File getTestResourceFile(Class clazz, String resource) {
//todo: use better mechanism for doing this!
    String result = FileUtil.getFilename(clazz, ".") + resource;
    result = result.replace("/classes/", "/junit-classes/");
    return FileUtil.getFile(result);
  }

  /**
   * Get a normal input stream or a gzip input stream depending on whether
   * the resource's extension is ".gz".
   */
  public static InputStream getInputStream(Class clazz, String resource) throws IOException {
    return getInputStream(getUrl(clazz, resource));
  }

  /**
   * Get the file referenced by the resource name, dereferencing appropriately
   * if the filename is a valid URL.
   */
  public static File getFile(String resourceName) {
    File result = null;

    try {
      final URL url = new URL(resourceName);
      result = new File(url.toURI());
    }
    catch (MalformedURLException me) {
      result = new File(resourceName);
    }
    catch (URISyntaxException ue) {
      result = new File(resourceName);
    }
    catch (IllegalArgumentException ue) {
      result = new File(resourceName);
    }

    return result;
  }

  /**
   * Get a buffered writer for a UTF-8 (and possibly gzipped) file.
   *
   * @param filename - file to write to
   * @return BufferedReader that handles gzipping (if filename ends in ".gz") and is UTF-8 encoded.
   * @throws IOException
   */
  public static BufferedWriter getWriter(String filename) throws IOException {
    return getWriter( filename, false );
  }

  /**
   * Get a buffered writer for a UTF-8 (and possibly gzipped) file.
   *
   * @param file - file to write to
   * @return BufferedReader that handles gzipping (if filename ends in ".gz") and is UTF-8 encoded.
   * @throws IOException
   */
  public static BufferedWriter getWriter(File file) throws IOException {
    return getWriter( file, false );
  }

  /**
   * Get a buffered writer for a UTF-8 (and possibly gzipped) file.
   *
   * @param filename - file to write to
   * @param append - flag to append output
   * @return BufferedReader that handles gzipping (if filename ends in ".gz") and is UTF-8 encoded.
   * @throws IOException
   */
  public static BufferedWriter getWriter(String filename, boolean append) throws IOException {
    return getWriter(getFile(filename), append );
  }

  /**
   * Get a buffered writer for a UTF-8 (and possibly gzipped) file.
   *
   * @param file - file to write to
   * @param append - flag to append output
   * @return BufferedReader that handles gzipping (if filename ends in ".gz") and is UTF-8 encoded.
   * @throws IOException
   */
  public static BufferedWriter getWriter(File file, boolean append) throws IOException {
    // make directories if they don't exist yet.
    final File parent = file.getParentFile();
    if (parent != null && !parent.exists()) parent.mkdirs();

    // open the output stream for gzipped or plain text.
    final OutputStream outputStream = getOutputStream(file, append);

    return getWriter(outputStream);
  }

  /**
   * Get an output stream for a (possibly gzipped) file.
   */
  public static OutputStream getOutputStream(File file, boolean append) throws IOException {
    // open the output stream for gzipped or plain text.
    OutputStream outputStream = new FileOutputStream(file, append);
    if (file.getName().toLowerCase().endsWith(".gz")) {
      outputStream = new GZIPOutputStream(outputStream);
    }
    return outputStream;
  }

  /**
   * Get an auto flushing, UTF-8 encoding PrintStream to the (possibly gzipped) file.
   */
  public static PrintStream getPrintStream(File file, boolean append) throws IOException {
    final OutputStream outputStream = getOutputStream(file, append);
    return getPrintStream(outputStream);
  }

  public static BufferedWriter getWriter(OutputStream outputStream) throws IOException {
    return new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
  }

  /**
   * Get the path to the directory containing the resource (including the trailing slash).
   */
  public static String getBasePath(Class clazz, String resource) {
    final String filename = getFilename(clazz, resource);
    return getBasePath(filename);
  }

  /**
   * Get the path to the directory of the file (including the trailing slash).
   */
  public static String getBasePath(String fullPath) {
    String result = null;
    if (fullPath != null) {
      final int pos = fullPath.lastIndexOf(File.separatorChar);
      if (pos >= 0) {
        result = fullPath.substring(0, pos + 1);
      }
    }
    return result;
  }

  /**
   * Get the amount of time since the file was last modified in milliseconds.
   *
   * @return the age or 0 if it doesn't exist.
   */
  public static long getAge(File file) {
    long result = 0L;

    if (file.exists()) {
      final long lastModified = file.lastModified();
      final long curTime = System.currentTimeMillis();
      result = curTime - lastModified;
    }

    return result;
  }

  /**
   * Ensure that directories exist to create the given file.
   */
  public static void mkdirs(String fullPath) {
    final String basePath = getBasePath(fullPath);
    final File file = getFile(basePath);
    if (!file.exists()) file.mkdirs();
  }

  /**
   * Get the amount of disk space, in bytes, consumed by the file.
   * <p>
   * If the file is a directory, get the disk space consumed by the directory
   * with all of its contents (recursively).
   * 
   */
  public static final long size(File fileOrDirectory) {
    long result = 0L;

    if (fileOrDirectory != null) {
      if (fileOrDirectory.isDirectory()) {
        final File[] files = fileOrDirectory.listFiles();
        for (File file : files) {
          result += size(file);
        }
      }
      else {
        result = fileOrDirectory.length();
      }
    }

    return result;
  }

  /**
   * Ensure a file exists at the given path.
   * <p>
   * If it already exists, do nothing; if it does not exist, then create an
   * empty file at path.
   *
   * @return true if the file was created; otherwise, false.
   */
  public static final boolean touch(String path) throws IOException {
    return touch(getFile(path));
  }

  /**
   * Ensure a file exists at the given path.
   * <p>
   * If it already exists, do nothing; if it does not exist, then create an
   * empty file at path.
   *
   * @return true if the file was created; otherwise, false.
   */
  public static final boolean touch(File path) throws IOException {
    boolean result = false;

    if (!path.exists()) {
      final BufferedWriter writer = getWriter(path, true);
      writer.close();
      result = true;
    }

    return result;
  }

  /**
   * Ensure that the given file's parent directories all exist, making them if necessary.
   */
  public static boolean makeParentDirectories(File file) {
    boolean result = false;

    final File parentFile = file.getParentFile();
    if (parentFile == null) return false;

    if (!parentFile.exists()) {
      if (makeParentDirectories(parentFile)) {
        parentFile.mkdir();
        result = parentFile.exists();
      }
    }
    else {
      result = true;
    }

    return result;
  }


  /**
   * Extract the file name from the full path.
   */
  public static String getFileName(String fullPath) {
    String result = null;
    if (fullPath != null) {
      final int pos = fullPath.lastIndexOf(File.separatorChar);
      if (pos >= 0) {
        result = fullPath.substring(pos + 1);
      }
    }
    return result;
  }

  /**
   * Concatenate the base file name to the given path.
   * <p>
   * Assume fullPath is the path to a directory and deal appropriately with
   * the presence or absence of a trailing slash on the path.
   */
  public static String getFilename(String fullPath, String baseName) {
    final boolean hasTrailingSlash = (fullPath.charAt(fullPath.length() - 1) == File.separatorChar);
    return hasTrailingSlash ? (fullPath + baseName) : (fullPath + File.separatorChar + baseName);
  }

  /**
   * Concatenate the file name to the parent for a full absolute path.
   */
  public static String getFilename(File parent, String name) {
    return getFilename(parent.getAbsolutePath(), name);
  }

  /**
   * Get the base name of the given file.
   * <p>
   * If ext is null, then the base name is all of the filename up to the last
   * extension, defined as the text from the last '.' in the name.
   * <p>
   * If ext is non-null, then the base name is all of the filename up to ext.
   * If the filename does not end in ext, then the full filename is the
   * basename.
   */
  public static final String getBaseName(String filename, String ext) {
    String result = filename;

    if (ext == null) {
      final int lastDotPos = filename.lastIndexOf('.');
      if (lastDotPos >= 0) {
        result = filename.substring(0, lastDotPos);
      }
    }
    else if (filename.endsWith(ext)) {
      result = filename.substring(0, filename.length() - ext.length());
    }

    return result;
  }

  private static final Pattern NAME_PATTERN = Pattern.compile("^([^\\.]+)\\..*$");

  /**
   * Convert input file to output file by
   * <ul>
   * <li>taking name up to first '.',</li>
   * <li>making a directory under the input file's dir</li>
   * <li>creating output file with same name as input but in the new directory.</li>
   * </ul>
   */
  public static final File input2output(File input, boolean create) {
    File result = null;
    
    final String name = input.getName();
    final Matcher m = NAME_PATTERN.matcher(name);
    if (m.matches()) {
      final String newDirName = m.group(1);
      final File newDir = new File(input.getParentFile(), newDirName);
      if (create && input.exists() && !newDir.exists()) newDir.mkdirs();
      result = new File(newDir, name);
    }

    return result;
  }

  /**
   * Get an error's stack trace as a string.
   */
  public static String getStackTrace(Throwable t) {
    String result = null;

    try {
      final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      final PrintStream out = new PrintStream(bytesOut);
      t.printStackTrace(out);
      out.close();
      bytesOut.close();
      result = new String(bytesOut.toString("UTF-8"));
    }
    catch (UnsupportedEncodingException e) {
      result = "StackTrace for '" + t.toString() + "' unavailable due to '" + e.toString() + "'";
    }
    catch (IOException e) {
      result = "StackTrace for '" + t.toString() + "' unavailable due to '" + e.toString() + "'";
    }

    return result;
  }

  /**
   * Read the file's bytes.
   */
  public static byte[] readBytes(File file) throws IOException {
    byte[] result = null;
    InputStream in = null;
    final byte[] buffer = new byte[32768];  // 32K buffer

    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      in = getInputStream(file);

      while (true) {
        final int n = in.read(buffer);
        if (n < 0) break;
        out.write(buffer, 0, n);
      }

      out.close();
      result = out.toByteArray();
    }
    finally {
      if (in != null) in.close();
    }

    return result;
  }

  /**
   * Read the file's bytes.
   */
  public static byte[] readRawBytes(File file) throws IOException {
    byte[] result = null;
    InputStream in = null;
    final byte[] buffer = new byte[32768];  // 32K buffer

    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      in = new FileInputStream(file);

      while (true) {
        final int n = in.read(buffer);
        if (n < 0) break;
        out.write(buffer, 0, n);
      }

      out.close();
      result = out.toByteArray();
    }
    finally {
      if (in != null) in.close();
    }

    return result;
  }

  /**
   * Read all non-empty lines from a text file into an ArrayList.
   *
   * @param filename - File to read from
   * @return ArrayList of String, one per non-empty line in the text file.
   * @throws IOException
   */
  public static ArrayList<String> readLines(String filename) throws IOException {
    BufferedReader reader = FileUtil.getReader(filename);
    String line;
    ArrayList<String> lineList = new ArrayList<String>();
    while ((line = reader.readLine()) != null) {
      if ((line = line.trim()).length() > 0) {
        lineList.add(line);
      }
    }
    reader.close();
    return lineList;
  }

  /**
   * Count the number of lines in a file
   *
   * @param filename - File to read from
   * @return the number of lines in the file
   * @throws IOException
   */
  public static long countLines(String filename) throws IOException {
    BufferedReader reader = FileUtil.getReader(filename);
    long lineCount = 0;
    while (reader.readLine() != null) {
      lineCount++;
    }
    reader.close();
    return lineCount;
  }

  /**
   * Count the number of lines in the given file.
   *
   * @param file  The file whose lines to count.
   *
   * @return the number of lines or -1 if unable to compute.
   */
  public static final long countLines(File file) {
    long lineCount = 0;
    BufferedReader reader = null;

    try {
      reader = getReader(file);
      while (reader.readLine() != null) ++lineCount;
    }
    catch (IOException e) {
      lineCount = -1;
    }
    finally {
      if (reader != null) {
        try {
          reader.close();
        }
        catch (IOException ignore) {}
      }
    }

    return lineCount;
  }

  /**
   * Read in the entire file as a list of strings, one per line, returning null
   * if there is a problem.
   */
  public static List<String> readLinesIfCan(String filename) {
    List<String> result = null;

    try {
      result = readLines(filename);
    }
    catch (IOException e) {
      // couldn't do it. result will be null.
    }

    return result;
  }

  /**
   * Read in the entire file as a string.
   *
   * @param filename - File to read from
   * @return a string with the file contents
   *
   * @throws IOException
   */
  public static String readAsString(String filename) throws IOException {
    return readAsString(getFile(filename), null);
  }

  /**
   * Read in the entire file as a string.
   *
   * @param file - File to read from
   * @return a string with the file contents
   *
   * @throws IOException
   */
  public static String readAsString(File file) throws IOException {
    return readAsString(file, null);
  }

  /**
   * Read in the entire file as a string, returning null if there is a problem.
   */
  public static String readAsStringIfCan(String filename) {
    return readAsStringIfCan(getFile(filename));
  }

  /**
   * Read in the entire file as a string, returning null if there is a problem.
   */
  public static String readAsStringIfCan(File file) {
    String result = null;
    try {
      result = readAsString(file);
    }
    catch (IOException e) {
      // coudn't do it. result will be null.
    }
    return result;
  }

  /**
   * Write the given string to the given file, appending if indicated.
   */
  public static void writeToFile(String filename, String contents, boolean append) throws IOException {
    writeToFile(getFile(filename), contents, append);
  }
  
  /**
   * Write the given string to the given file, appending if indicated.
   */
  public static void writeToFile(File file, String contents, boolean append) throws IOException {
    final BufferedWriter writer = getWriter(file, append);
    writer.write(contents);
    writer.close();
  }

  /**
   * Read strings from the given file.
   * <p>
   * @param result             The collector for read strings.
   * @param file               The name of the file to load strings from.
   * @param splitPattern       The pattern to split lines into columns, or null if each line is a string to read.
   * @param keeperColumns      The columns after splitting to keep strings from, or null if all columns are to be kept.
   * @param ignoreStartString  Pattern for lines to ignore when they start with this string or null to keep all lines.
   * @param ignoreBlankStrings If true, blank strings will not be added to the results.
   * @param ignoreFirstLine    If true, the first line will be ignored.
   *
   * @return the number of strings read from the file.
   */
  public static final int readStrings(Collection<String> result, File file, String splitPattern, int[] keeperColumns, String ignoreStartString, boolean ignoreBlankStrings, boolean ignoreFirstLine) throws IOException {
    final int startSize = result.size();

    final BufferedReader reader = getReader(file);
    String line = ignoreFirstLine ? reader.readLine() : null;
    while ((line = reader.readLine()) != null) {
      if (ignoreBlankStrings && "".equals(line)) continue;
      if (ignoreStartString != null && line.startsWith(ignoreStartString)) continue;
      if (splitPattern != null) {
        final String[] pieces = line.split(splitPattern);
        if (keeperColumns == null) {
          for (String piece : pieces) {
            if (!ignoreBlankStrings || !"".equals(piece)) {
              result.add(piece);
            }
          }
        }
        else {
          for (int keeperColumn : keeperColumns) {
            if (keeperColumn < pieces.length) {
              final String piece = pieces[keeperColumn];
              if (!ignoreBlankStrings || !"".equals(piece)) {
                result.add(piece);
              }
            }
          }
        }
      }
      else {
        result.add(line);
      }
    }
    reader.close();

    return result.size() - startSize;
  }

  /**
   * Read in the entire file as a string.
   *
   * @param filename - File to read from
   * @param lineIgnorer - Designates lines to ignore. Don't ignore any if null.
   * @return a string with the file contents
   *
   * @throws IOException
   */
  public static String readAsString(String filename, LineIgnorer lineIgnorer) throws IOException {
    return readAsString(getFile(filename), lineIgnorer);
  }

  /**
   * Read in the entire file as a string.
   *
   * @param file - File to read from
   * @param lineIgnorer - Designates lines to ignore. Don't ignore any if null.
   * @return a string with the file contents
   *
   * @throws IOException
   */
  public static String readAsString(File file, LineIgnorer lineIgnorer) throws IOException {
    BufferedReader reader = FileUtil.getReader(file);
    final String result = readAsString(reader, lineIgnorer);
    reader.close();
    return result;
  }

  /**
   * Read in the entire file as a string.
   *
   * @param reader - Reader to read from
   * @return a string with the file contents
   *
   * @throws IOException
   */
  public static String readAsString(BufferedReader reader) throws IOException {
    return readAsString(reader, null);
  }

  /**
   * Read in the entire file as a string.
   *
   * @param reader - Reader to read from.
   * @param lineIgnorer - Specifies lines to ignore.
   * @return a string with the file contents.
   *
   * @throws IOException
   */
  public static String readAsString(BufferedReader reader, LineIgnorer lineIgnorer) throws IOException {
    if (reader == null) return null;

    final StringBuilder result = new StringBuilder();
    String line;
    boolean didFirst = false;
    while ((line = reader.readLine()) != null) {
      if (lineIgnorer == null || !lineIgnorer.ignoreLine(line)) {
        if (didFirst) result.append('\n');
        result.append(line);
        didFirst = true;
      }
    }
    return result.toString();
  }

  public static String[] getAllLines(File file) throws IOException {
    return getFirstNLines(file, -1, null);
  }
  
  /**
   * Get up to <code>numLines</code> of text file (or all if -1).
   * 
   * @param file file to read
   * @param nLines N lines to read (or -1 to read all)
   * @param encoding encoding of input file (or defauilt encoding if null)
   * @return first N lines of file as string array
   * @throws IOException
   */
  public static String[] getFirstNLines(File file, int nLines, String encoding) throws IOException {
    BufferedReader br = null;
    
    if (encoding == null) {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
    } else {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
    }

    String line;
    ArrayList<String> contents = new ArrayList<String>(ARRAY_SIZE);
    int count = 0;

    while ((line = br.readLine()) != null && (nLines == -1 || count < nLines)) {
      count++;
      contents.add(line.trim());
    }

    String[] lines = new String[contents.size()];
    contents.toArray(lines);

    return lines;
  }

  /**
   * Get the first line of the file.
   */
  public static String getFirstLine(File file) {
    String result = null;

    BufferedReader reader = null;
    try {
      reader = FileUtil.getReader(file);
      result = reader.readLine();
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Can't getFirstLine(" + file + ")!", e);
    }
    finally {
      try {
        if (reader != null) reader.close();
      }
      catch (IOException e) {
        throw new IllegalArgumentException("Can't getFirstLine(" + file + ")!", e);
      }
    }

    return result;
  }

  /**
   * Get the last line of the named file.
   */
  public static String getLastLine(String filename) {
    final File file = getFile(filename);
    return getLastLine(file);
  }

  /**
   * Get the last (non-empty) line of the file.
   */
  public static String getLastLine(File file) {
    String result = null;

    RandomAccessFile raFile = null;

    if (file.exists()) {
      try {
        raFile = new RandomAccessFile(file, "r");

        long len = raFile.length() - 1024;   // assume no line is longer than 1K
        if (len > 0) raFile.seek(len);
        String line = null;
        while ((line = raFile.readLine()) != null) {
          if (line.trim().length() > 0) {  // keep last non-empty line.
            result = line;
          }
        }
      }
      catch (IOException e) {
      }
      finally {
        if (raFile != null) {
          try {
            raFile.close();
          }
          catch (IOException e) {
          }
        }
      }
    }

    return result;
  }

  /**
   * Get the last (non-empty, up to 1K) lines of the file.
   * <p>
   * This implementation uses a RandomAccessFile to jump near the
   * end of the file instead of iterating through the entire file.
   * <p>
   * Note that for ".gz" files, this method will not work while that
   * implemented in getLastLines2 will; however, this method calls
   * getLastLines2 if the file ends with ".gz" so it can be used in
   * any case.
   */
  public static String[] getLastLines(File file) {
    if (file.getName().endsWith(".gz")) {
      return getLastLines2(file, 10);
    }

    List<String> result = new ArrayList<String>();

    RandomAccessFile raFile = null;

    if (file.exists()) {
      try {
        raFile = new RandomAccessFile(file, "r");

        long len = raFile.length() - 1024;   // assume no line is longer than 1K
        if (len > 0) raFile.seek(len);

        boolean didFirst = false;
        String line = null;
        while ((line = raFile.readLine()) != null) {
          if (didFirst && line.trim().length() > 0) {  // keep last non-empty line.
            result.add(line);
          }
          didFirst = true;
        }
      }
      catch (IOException e) {
      }
      finally {
        if (raFile != null) {
          try {
            raFile.close();
          }
          catch (IOException e) {
          }
        }
      }
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Get the last (non-empty) lines by reading through the entire file and just
   * keeping up to the last N (non-empty) lines.
   * <p>
   * Note that for ".gz" files, this method will work while getLastLines will not.
   */
  public static String[] getLastLines2(File file, int numLines) {
    LinkedList<String> result = new LinkedList<String>();

    if (file.exists()) {
      BufferedReader reader = null;
      try {
        reader = getReader(file);
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (!"".equals(line)) {
            result.addLast(line);
            if (result.size() > numLines) {
              result.removeFirst();
            }
          }
        }
      }
      catch (IOException e) {
      }
      finally {
        if (reader != null) {
          try {
            reader.close();
          }
          catch (IOException ignore) {
          }
        }
      }
    }
    
    return result.toArray(new String[result.size()]);
  }

  public static interface LineIgnorer {
    public boolean ignoreLine(String line);
  }

  public static final LineIgnorer LINUX_COMMENT_IGNORER = new LineIgnorer() {
      public boolean ignoreLine(String line) {
        line = line.trim();
        return line.length() == 0 || '#' == line.charAt(0);
      }
    };

  public static PrintStream getPrintStream(String filename, boolean append) throws IOException {
    final File file = getFile(filename);
    final OutputStream outputStream = getOutputStream(file, append);
    return getPrintStream(outputStream);
  }

  public static PrintStream getPrintStream(OutputStream out) {
    try {
      return new PrintStream(out, true, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 not supported in FileUtil.");
    }
  }

  /**
   * Get the amount of free disk space (in kilobytes) on the file's partition.
   *
   * @return the free disk space or -1 if the file doesn't exist or "df -k" failed.
   */
  public static final long getFreeDiskSpace(File file) {
    return (file != null && file.exists()) ? file.getFreeSpace() / 1024 : -1l;
    
//    long result = -1L;
//
//    if (file != null && file.exists()) {
//      final ExecUtil.ExecResult execResult = ExecUtil.executeProcess("df -k " + file.getAbsolutePath());
//      if (execResult != null && !execResult.failed()) {
//        final String[] pieces = execResult.output.split("\\s+");
//        try {
//          result = Long.parseLong(pieces[10]);
//        }
//        catch (Exception e) {
//          System.err.println(new Date() + ": getFreeDiskSpace(" + file + ") failed!");
//          e.printStackTrace(System.err);
//        }
//      }
//    }
//
//    return result;  // df -k is already in kilobytes
  }

  /**
   * Build a unique filename for output by finding the first number starting
   * from 1 for which a file doesn't exist of the form
   * <p>
   * "filename + '.' + num + postfix"
   * <p>
   * The presence of the postfix is optional on the candidateFilename.
   * If it does not exist, it will be added. Numbers appear immediately
   * before the postfix (which should include its preceding dot if
   * necessary).
   */
  public static final String buildOutputFilename(String candidateFilename, String defaultPostfix) {
    int lastDotPos = candidateFilename.lastIndexOf('.');
    if (lastDotPos < 0) lastDotPos = candidateFilename.length();
    final String prefix = candidateFilename.substring(0, lastDotPos);
    final String postfix = (lastDotPos < candidateFilename.length()) ? candidateFilename.substring(lastDotPos) : defaultPostfix;

    String result = null;

    for (int id = 1; result == null; ++id) {
      String curFilename = prefix + "." + id + postfix;
      final File file = new File(curFilename);
      if (!file.exists()) {
        result = curFilename;
        break;
      }
    }

    return result;
  }
}
