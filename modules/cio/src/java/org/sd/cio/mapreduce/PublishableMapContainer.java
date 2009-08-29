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
package org.sd.cio.mapreduce;


import org.sd.io.Publishable;

/**
 * A container holding an ordered map whose entries are written as binary to
 * a file. Note that this generates binary data with no need for delimiters
 * or escaping at the cost of human readability and easy manipulation.
 * <p>
 * @author Spence Koehler
 */
public abstract class PublishableMapContainer<K extends Publishable, V extends Publishable> extends MapContainer<K, V> {
  
//todo: implement this... no delimiters or escaping required, but output isn't human readable.

}
