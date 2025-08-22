/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;

/**
 * Content provider class for value lists.
 *
 * @author rgansevles
 *
 */

public class ValuelistContentProvider extends FlatTreeContentProvider
{

	private final FlattenedSolution flattenedSolution;
	private final IPersist context;

	public ValuelistContentProvider(FlattenedSolution flattenedSolution, IPersist context)
	{
		this.flattenedSolution = flattenedSolution;
		this.context = context;
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof ValuelistListOptions)
		{
			ValuelistListOptions options = (ValuelistListOptions)inputElement;

			List<String> vlUUIDS = new ArrayList<String>();
			if (options.includeNone) vlUUIDS.add(ValuelistLabelProvider.VALUELIST_NONE_STRING);

			Iterator<ValueList> it = flattenedSolution.getValueLists(true);
			while (it.hasNext())
			{
				ValueList obj = it.next();
				if (context != null && PersistEncapsulation.isModuleScope(obj, (Solution)context.getRootObject()))
				{
					continue;
				}
				vlUUIDS.add(obj.getUUID().toString());
			}

			return vlUUIDS.toArray();
		}

		return super.getElements(inputElement);
	}

	public static class ValuelistListOptions
	{
		public final boolean includeNone;

		public ValuelistListOptions(boolean includeNone)
		{
			this.includeNone = includeNone;
		}
	}

}
