/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.sd.io.FileUtil;

/**
 * Wrapper for generating dot and/or image files from a dotMaker.
 * <p>
 * @author Spence Koehler
 */
public class DotWrapper {
  
  private DotMaker dotMaker;
  private File workingDir;
  private String filePrefix;

  private File _imageFileHandle;
  private File _dotFileHandle;
  private boolean _populatedImageFile;
  private boolean _populatedDotFile;
  private ExecUtil.ExecResult _execResult;

  /**
   * Construct a new instance.
   *
   * @param dotMaker  The dot maker to use for creating the imaage.
   * @param workingDir  The working directory for generated dot and/or image files.
   * @param filePrefix  The prefix to use for generated temporary files.
   */
  public DotWrapper(DotMaker dotMaker, File workingDir, String filePrefix) {
    this.dotMaker = dotMaker;
    this.workingDir = workingDir;
    this.filePrefix = filePrefix;

    // initialize lazy load vars
    this._imageFileHandle = null;
    this._dotFileHandle = null;
    this._populatedImageFile = false;
    this._populatedDotFile = false;
    this._execResult = null;
  }


  public DotMaker getDotMaker() {
    return dotMaker;
  }

  public File getWorkingDir() {
    return workingDir;
  }

  public String getFilePrefix() {
    return filePrefix;
  }

  public ExecUtil.ExecResult getExecResult() {
    return _execResult;
  }


  public boolean populatedDotFile() {
    return _populatedDotFile;
  }

  public boolean populatedImageFile() {
    return _populatedImageFile;
  }

  public boolean dotFileExists() {
    return _dotFileHandle != null && _dotFileHandle.exists();
  }

  public boolean imageFileExists() {
    return _imageFileHandle != null && _imageFileHandle.exists();
  }

  /**
   * Delete this wrapper's populated files.
   */
  public void deletePopulatedFiles() {
    if (_populatedImageFile && _imageFileHandle != null) {
      _imageFileHandle.delete();
      _populatedImageFile = false;
    }
    if (_populatedDotFile && _dotFileHandle != null) {
      _dotFileHandle.delete();
      _populatedDotFile = false;
    }
  }


  /**
   * Set this instance's image file handle.
   * <p>
   * NOTE: Setting this after a file handle already exists abandons the old
   *       File and sets the new handle to be lazily populated.
   *
   * @return the previous file handle, possibly null.
   */
  public File setImageFileHandle(File imageFileHandle) {
    final File result = _imageFileHandle;
    this._imageFileHandle = imageFileHandle;
    this._populatedImageFile = false;
    return result;
  }

  /**
   * Set this instance's dot file handle.
   * <p>
   * NOTE: Setting this after a file handle already exists abandons the old
   *       File and sets the new handle to be lazily populated.
   *
   * @return the previous file handle, possibly null.
   */
  public File setDotFileHandle(File dotFileHandle) {
    final File result = _dotFileHandle;
    this._dotFileHandle = dotFileHandle;
    this._populatedDotFile = false;
    return result;
  }

  /**
   * Get this instance's image file handle.
   * <p>
   * Note that calling this method will not populate the file, but may create
   * a new temporary file if a file handle has not yet been set.
   *
   * @return the file handle.
   */
  public File getImageFileHandle() throws IOException {
    if (_imageFileHandle == null) {
      if (!workingDir.exists()) workingDir.mkdirs();
      if (_dotFileHandle != null) {
        _imageFileHandle = copyFileName(_dotFileHandle, "\\.dot$", ".png");
      }
      if (_imageFileHandle == null) {
        _imageFileHandle = File.createTempFile(filePrefix, ".png", workingDir);
      }
    }
    return _imageFileHandle;
  }

  /**
   * Get this instance's dot file handle.
   * <p>
   * Note that calling this method will not populate the file, but may create
   * a new temporary file if a file handle has not yet been set.
   *
   * @return the file handle.
   */
  public File getDotFileHandle() throws IOException {
    if (_dotFileHandle == null) {
      if (!workingDir.exists()) workingDir.mkdirs();
      if (_imageFileHandle != null) {
        _dotFileHandle = copyFileName(_imageFileHandle, "\\.png$", ".dot");
      }
      if (_dotFileHandle == null) {
        _dotFileHandle = File.createTempFile(filePrefix, ".dot", workingDir);
      }
    }
    return _dotFileHandle;
  }

  /**
   * Get this instance's (populated) image File.
   * <p>
   * Note that calling this method will populate the file if necessary.
   * <p>
   * If the dot file has been generated, then it will be used to create the
   * image; otherwise, the image will be created directly without persisting
   * the dot file.
   *
   * @return the file handle or null if failed to populate.
   */
  public File getImageFile() throws IOException {
    final File result = getImageFileHandle();
    boolean success = false;

    if (!_populatedImageFile || !result.exists()) {
      if (_populatedDotFile) {
        // use populated dot file to generate image
        success = createImageFromDot();
      }
      else {
        // generate image file directly
        success = createImageDirectly();
      }
      _populatedImageFile = true;
    }
    else success = true;

    return success ? result : null;
  }

  /**
   * Get this instance's (populated) dot File.
   * <p>
   * Note that calling this method will populate the file if necessary.
   *
   * @return the file handle.
   */
  public File getDotFile() throws IOException {
    final File result = getDotFileHandle();

    if (!_populatedDotFile || !result.exists()) {
      final Writer writer = FileUtil.getWriter(result);
      dotMaker.writeDot(writer);
      writer.close();
      _populatedDotFile = true;
    }

    return result;
  }

  private final File copyFileName(File file, String pattern, String replace) {
    File result = null;

    final String filename = file.getName();
    final String newFilename = filename.replaceFirst(pattern, replace);
    if (!newFilename.equals(filename)) {
      result = new File(workingDir, newFilename);
    }

    return result;
  }

  private final boolean createImageFromDot() throws IOException {

    final File dotFile = getDotFile();
    final File imageFile = getImageFileHandle();

    this._execResult = ExecUtil.executeProcess(new String[] {
        "/usr/bin/dot",
        "-Tpng",
        "-o",
        imageFile.getAbsolutePath(),
        dotFile.getAbsolutePath(),
      });

    final boolean result = (_execResult == null || _execResult.failed()) ? false : true;

    return result;
  }

  private final boolean createImageDirectly() throws IOException {
    
    final File imageFile = getImageFileHandle();
    final String dotContents = getDotContents();

    this._execResult = ExecUtil.executeProcess(
      dotContents,
      "/usr/bin/dot -Tpng -o " + imageFile.getAbsolutePath());

    final boolean result = (_execResult == null || _execResult.failed()) ? false : true;

    return result;
  }

  private final String getDotContents() throws IOException {
    final Writer writer = new StringWriter();
    dotMaker.writeDot(writer);
    writer.close();
    return writer.toString();
  }
}
