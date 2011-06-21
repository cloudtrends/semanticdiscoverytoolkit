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
package org.sd.cluster.config;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to manage the users.csv file.
 * <p>
 * @author Spence Koehler
 */
public class UsersCsv {

  public static final String USERS_CSV_RESOURCE = "users.csv";
  public static final String MODULE_RESOURCE_PATH = "../resources/";
  public static final String CLASS_RESOURCE_PATH = "resources/";

  private static final UsersCsv INSTANCE = new UsersCsv();

  public static final UsersCsv getInstance() {
    return INSTANCE;
  }

  final Map<String, User> users;  //user name -> user instance

  private UsersCsv() {
    try {
      final InputStream inputStream = findUsersCsv();
      this.users = loadUsersCsv(inputStream);
    }
    catch (IOException e) {
      throw new IllegalStateException("can't load users! need '" + USERS_CSV_RESOURCE + "' in classpath or referenced through env var 'USERS_CSV'", e);
    }
  }
  
  public int getLowPort(String userName) {
    User user = users.get(userName);

    if (user == null) user = addUser(userName);

    if (user == null) {
      throw new IllegalStateException("User '" + userName + "' is undefined in '" + USERS_CSV_RESOURCE + "'!");
    }

    return user.lowPort;
  }

  public int getHighPort(String userName) {
    User user = users.get(userName);

    if (user == null) user = addUser(userName);

    if (user == null) {
      throw new IllegalStateException("User '" + userName + "' is undefined in '" + USERS_CSV_RESOURCE + "'!");
    }

    return user.highPort;
  }

  private final User addUser(String userSpec) {
    User result = null;

    if (userSpec.indexOf(',') >= 0) {
      result = new User(userSpec);
      users.put(result.name, result);
    }

    return result;
  }

  /**
   * Add the user to this instance.
   *
   * @param userName  the user to add.
   * @param lowPort  the low port for the user.
   * @param highPort  the high port for the user.
   * @param override  if false, then only add the user if the name doesn't already exist;
   *                  otherwise, add the user regardless.
   */
  public void addUser(String userName, int lowPort, int highPort, boolean override) {

    if (override || !this.users.containsKey(userName)) {
      this.users.put(userName, new User(userName, lowPort, highPort));
    }
  }

  private final Map<String, User> loadUsersCsv(InputStream usersCsvInputStream) throws IOException {
    final Map<String, User> result = new LinkedHashMap<String, User>();

    // check USERS_CSV environment variable, fallback to USERS_CSV_RESOURCE
    final BufferedReader reader = FileUtil.getReader(usersCsvInputStream);
    String line = null;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.length() > 0 && !line.startsWith("#")) {
        final User user = new User(line);
        result.put(user.name, user);
      }
    }
    reader.close();
    return result;
  }

  private final InputStream findUsersCsv() throws IOException {
    InputStream result = null;

    // check USERS_CSV environment variable, fallback to classpath, fallback to USERS_CSV_RESOURCE

    String override = System.getProperty("usersCSV");
    if (override == null) override = System.getenv("USERS_CSV");
    if (override != null) {
      final File file = FileUtil.getFile(override);
      if (file.exists()) {
        System.out.println(new Date() + ": Loading UsersCsv (using environment var) from '" + file + "'");
        result = FileUtil.getInputStream(file);
      }
    }

    // try finding USERS_CSV_RESOURCE on the classpath
    if (result == null) {
      final Enumeration<URL> urls = ClassLoader.getSystemResources(USERS_CSV_RESOURCE);
      if (urls != null) {
        while (urls.hasMoreElements()) {
          final URL url = urls.nextElement();
          try {
            final URI uri = url.toURI();
            final File file = new File(uri);
            System.out.println(new Date() + ": Loading UsersCsv (using classpath resource) from '" + file + "'");
            result = FileUtil.getInputStream(file);
          }
          catch (URISyntaxException e) {
            throw new IOException(e);
          }
        }
      }
    }

    // fallback to module users csv resource
    if (result == null) {
      result = FileUtil.getInputStream(MODULE_RESOURCE_PATH + USERS_CSV_RESOURCE);
      if (result != null) {
        System.out.println(new Date() + ": Loading UsersCsv (using module resource) from '" +
                           MODULE_RESOURCE_PATH + USERS_CSV_RESOURCE + "'");
      }
    }

    // fallback to default users csv resource
    if (result == null) {
      result = FileUtil.getInputStream(this.getClass(), "resources/" + USERS_CSV_RESOURCE);
      if (result != null) {
        System.out.println(new Date() + ": Loading UsersCsv (using default resource) from '" +
                           CLASS_RESOURCE_PATH + USERS_CSV_RESOURCE + "'");
      }
    }

    return result;
  }

  public final class User {
    public final String name;
    public final int lowPort;
    public final int highPort;

    public User(String fileLine) {
      final String[] pieces = fileLine.split(",");
      this.name = pieces[0];
      this.lowPort = Integer.parseInt(pieces[1]);
      this.highPort = Integer.parseInt(pieces[2]);
    }

    public User(String name, int lowPort, int highPort) {
      this.name = name;
      this.lowPort = lowPort;
      this.highPort = highPort;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(name).append(',').append(lowPort).append(',').append(highPort);

      return result.toString();
    }
  }

  public static final void main(String[] args) throws IOException {
    // echo out the loaded users
    final UsersCsv users = UsersCsv.getInstance();
    for (User user : users.users.values()) {
      System.out.println(user);
    }
  }
}
