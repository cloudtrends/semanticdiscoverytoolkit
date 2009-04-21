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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to manage the users.csv file.
 * <p>
 * @author Spence Koehler
 */
public class UsersCsv {

  public static final String USERS_CSV_RESOURCE = "resources/users.csv";

  private static final UsersCsv INSTANCE = new UsersCsv();

  public static final UsersCsv getInstance() {
    return INSTANCE;
  }

  final Map<String, User> users;  //user name -> user instance

  private UsersCsv() {
    try {
      this.users = loadUsersCsv();
    }
    catch (IOException e) {
      throw new IllegalStateException("can't load users! need '" + FileUtil.getFilename(UsersCsv.class, USERS_CSV_RESOURCE) + "' properly defined!", e);
    }
  }

  public int getLowPort(String userName) {
    final User user = users.get(userName);

    if (user == null) {
      throw new IllegalStateException("User '" + userName + "' is undefined in '" + FileUtil.getFilename(UsersCsv.class, USERS_CSV_RESOURCE + "'!"));
    }

    return user.lowPort;
  }

  public int getHighPort(String userName) {
    final User user = users.get(userName);

    if (user == null) {
      throw new IllegalStateException("User '" + userName + "' is undefined in '" + FileUtil.getFilename(UsersCsv.class, USERS_CSV_RESOURCE + "'!"));
    }

    return user.highPort;
  }

  private final Map<String, User> loadUsersCsv() throws IOException {
    final Map<String, User> result = new LinkedHashMap<String, User>();
    final BufferedReader reader = FileUtil.getReader(this.getClass(), USERS_CSV_RESOURCE);
    String line = null;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.length() > 0 && !line.startsWith("#")) {
        final User user = new User(line);
        result.put(user.name, user);
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
  }
}
