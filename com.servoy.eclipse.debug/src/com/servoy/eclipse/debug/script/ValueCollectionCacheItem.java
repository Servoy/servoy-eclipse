/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.debug.script;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueCollectionReference;

/**
 * @author jcompagner
 * @since 2020.03
 */
public class ValueCollectionCacheItem
{
	private final Set<IFile> files;
	private final IValueCollectionReference collection;
	private final long timestamp;

	public ValueCollectionCacheItem(Set<IFile> files, IValueCollectionReference collection)
	{
		this.files = new HashSet<IFile>(files);
		this.collection = collection;
		this.timestamp = files.stream().mapToLong(file -> file.getModificationStamp()).sum();
	}

	/**
	 * only returns the valuecollection if the timestamp of the files is the same, so the cache is still valid.
	 *
	 * @return the stored valuecollection
	 */
	public IValueCollection get()
	{
		if (files.stream().mapToLong(file -> file.getModificationStamp()).sum() == timestamp)
		{
			return collection.getValueCollection();
		}
		return null;
	}

	public Set<IFile> files()
	{
		return Collections.unmodifiableSet(files);
	}
}
