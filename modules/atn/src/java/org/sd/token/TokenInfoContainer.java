/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Container for organizing TokenInfo instances by their endPos.
 * <p>
 * @author Spence Koehler
 */
public class TokenInfoContainer<T extends TokenInfo> {
  
  private TreeMap<Integer, List<T>> tokenInfoList;
  public TreeMap<Integer, List<T>> getTokenInfoList() {
    return tokenInfoList;
  }

  public TokenInfoContainer() {
    this.tokenInfoList = new TreeMap<Integer, List<T>>();
  }

  public void add(T tokenInfo, int offset) {
    final int endIndex = offset + tokenInfo.getTokenEnd();
    List<T> tokenInfos = tokenInfoList.get(endIndex);
    if (tokenInfos == null) {
      tokenInfos = new ArrayList<T>();
      tokenInfoList.put(endIndex, tokenInfos);
    }
    tokenInfos.add(tokenInfo);
  }

  public T getFirst(int endPos) {
    T result = null;

    final List<T> tokenInfos = tokenInfoList.get(endPos);
    if (tokenInfos != null && tokenInfos.size() > 0) {
      result = tokenInfos.get(0);
    }

    return result;
  }

  public List<T> getAll(int endPos) {
    return tokenInfoList.get(endPos);
  }

  public void adjustEnd(int curEnd, int updatedEnd) {
    final List<T> tokenInfos = tokenInfoList.get(curEnd);
    if (tokenInfos != null) {
      for (T tokenInfo : tokenInfos) {
        tokenInfo.setTokenEnd(updatedEnd);
      }
      tokenInfoList.remove(curEnd);
      tokenInfoList.put(updatedEnd, tokenInfos);
    }
  }
}
