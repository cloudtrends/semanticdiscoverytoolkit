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
package org.sd.xml.record;


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Class to parse xml settings files using the Record abstraction.
 * <p>
 * @author Spence Koehler
 */
public class Settings {
  
  private Properties properties;
  private File settingsFile;
  private long lastModified;
  private Record _topRecord;

  /**
   * Load with the given properties and settings file.
   * <p>
   * Note that the settings will be reloaded if the settings file has been
   * modified.
   */
  public Settings(Properties properties, File settingsFile) {
    this.properties = properties;
    this.settingsFile = settingsFile;
    this.lastModified = 0L;
    this._topRecord = null;
  }

  /**
   * Construct with the given properties and top record.
   */
  public Settings(Properties properties, Record topRecord) {
    this.properties = properties;
    this.settingsFile = null;
    this.lastModified = 0L;
    this._topRecord = topRecord;
  }

  /**
   * Get the top record containing all settings.
   * <p>
   * If this instance was constructed using a settings file and that file
   * has been modified, the settings will be reloaded.
   */
  public Record getTopRecord() {
    if (hasNewSettings()) {
      try {
        _topRecord = new Record(settingsFile);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
      lastModified = settingsFile.lastModified();
    }
    return _topRecord;
  }

  /**
   * Determine whether new settings may be available.
   */
  public boolean hasNewSettings() {
    return (settingsFile != null && settingsFile.exists() &&
            (_topRecord == null || settingsFile.lastModified() > lastModified));
  }

  /**
   * Convenience method to get the (first) named settings record.
   * <p>
   * Note that a settings record is one of the root's children.
   * <p>
   * If this instance was constructed using a settings file and that file
   * has been modified, the settings will be reloaded.
   */
  public Record getSettingsRecord(String name) {
    final Record topRecord = getTopRecord();
    return topRecord.getRecord(name);
  }

  /**
   * Convenience method to get the all named settings records.
   * <p>
   * Note each settings record is one of the root's children.
   * <p>
   * If this instance was constructed using a settings file and that file
   * has been modified, the settings will be reloaded.
   */
  public List<Record> getAllSettingRecords(String name) {
    final Record topRecord = getTopRecord();
    return getAllSettingRecords(name, topRecord);
  }

  /**
   * Convenience method to get the all named settings records under
   * the given record.
   */
  public List<Record> getAllSettingRecords(String name, Record record) {
    return record.getRecords(name);
  }

  /**
   * Convenience method to get the setting record's first value for the given
   * attribute.
   * <p>
   * If the value doesn't exist, then null will be returned.
   * If the value exists with text, then the text will be returned.
   * If the value exists with no text, but has a property attribute,
   * then the named property will be retrieved from this instance's
   * properties.
   * If there is no 'property' attribute, then the property matching "attribute"
   * will be retrieved from this instance's properties.
   */
  public String getSettingsValue(Record settingsRecord, String attribute) {
    return getSettingsValue(settingsRecord, attribute, null);
  }

  /**
   * Convenience method to get the setting record's first value for the given
   * attribute.
   * <p>
   * If the value doesn't exist, then null will be returned.
   * If the value exists with text, then the text will be returned.
   * If the value exists with no text, but has a property attribute,
   * then the named property will be retrieved from this instance's
   * properties.
   * If there is no 'property' attribute, then the property matching "attribute"
   * will be retrieved from this instance's properties.
   */
  public String getSettingsValue(Record settingsRecord, String attribute, String defaultValue) {
    if (settingsRecord == null) return defaultValue;

    String result = settingsRecord.getValueString(attribute);

    if ((result == null || "".equals(result)) && properties != null) {
      final Record prec = settingsRecord.getValue(attribute + "@property");
      if (prec != null && prec.asValue() != null) {
        final String property = prec.asValue().getData();
        if (property != null && !"".equals(property)) {
          result = properties.getProperty(property, defaultValue);
        }
      }
      else {
        result = properties.getProperty(attribute, defaultValue);
      }
    }

    return result;
  }

  /**
   * Convenience method to get the first settings record and its first value
   * for the given attribute.
   */
  public String getSettingsValue(String name, String attribute) {
    return getSettingsValue(name, attribute, null);
  }

  /**
   * Convenience method to get the first settings record and its first value
   * for the given attribute.
   */
  public String getSettingsValue(String name, String attribute, String defaultValue) {
    String result = null;

    final Record settingsRecord = getSettingsRecord(name);
    if (settingsRecord != null) {
      result = getSettingsValue(settingsRecord, attribute, defaultValue);
    }

    return result;
  }

  /**
   * Get all values for the given attribute from the given settings record.
   */
  public Set<String> getSettingsValues(Record settingsRecord, String attribute) {
    if (settingsRecord == null) return null;

    Set<String> result = null;

    final List<Record> records = settingsRecord.getRecords(attribute);

    if (records != null && records.size() > 0) {
      for (Record record : records) {
        final String text = record.getText(properties, null);
        if (text != null && !"".equals(text)) {
          if (result == null) result = new HashSet<String>();
          result.add(text);
        }
      }
    }

    if (result == null && properties != null) {
      final String value = properties.getProperty(attribute);
      if (value != null && !"".equals(value)) {
        if (result == null) result = new HashSet<String>();
        result.add(value);
      }
    }

    return result;
  }

  /**
   * Convenience method to get the first settings record and all of its values
   * for the given attribute.
   */
  public Set<String> getSettingsValues(String name, String attribute) {
    Set<String> result = null;

    final Record settingsRecord = getSettingsRecord(name);
    if (settingsRecord != null) {
      result = getSettingsValues(settingsRecord, attribute);
    }

    return result;
  }
}
