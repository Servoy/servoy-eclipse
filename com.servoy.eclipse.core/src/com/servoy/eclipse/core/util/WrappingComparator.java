/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.core.util;

import java.util.Comparator;

/**
 * A proxied comparator where the values that are to be compared are converted into other values
 * before being given to the wrapped comparator.
 *
 * @author acostescu
 */
public abstract class WrappingComparator<T, WT> implements Comparator<T>
{

	private final Comparator< ? super WT> wrappedComparator;

	public WrappingComparator(Comparator< ? super WT> wrappedComparator)
	{
		this.wrappedComparator = wrappedComparator;
	}

	protected abstract WT convertToWrappedComparatorValue(T value);

	@Override
	public int compare(T o1, T o2)
	{
		return wrappedComparator.compare(convertToWrappedComparatorValue(o1), convertToWrappedComparatorValue(o2));
	}

}
