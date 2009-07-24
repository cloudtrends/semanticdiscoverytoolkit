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
package org.sd.cluster.data;


/**
 * Interface to manage and access data descriptors for a jvm.
 * <p>
 * @author Spence Koehler
 */
public interface DataManager {
  
  /**
   * Get the data descriptor with the given id for the currently active cluster.
   *
   * @param id      The id of the data descriptor.
   * @param create  If true, create the data descriptor if it doesn't already exist;
   *                If false, return null if the data descriptor does not exist.
   *
   * @return the data descriptor or null.
   */
  public DataDescriptor getDataDescriptor(String id, boolean create);

  /**
   * Get the ids of the data descriptors that currently exist for the currently
   * active cluster.
   */
  public String[] getDescriptorIds();

  // load in data descriptors (~/cluster/data/cluster-name/descriptor/{local-,lglobal-,vglobal-}<id>.def)
  // allow creation of new data descriptors (forward info to relevant nodes)
  // get input/output streams for data descriptor by name -vs- just access a data descriptor by name

  // impls: LocalDataManager, LiteralGlobalDataManager, VirtualGlobalDataManager ???
}
