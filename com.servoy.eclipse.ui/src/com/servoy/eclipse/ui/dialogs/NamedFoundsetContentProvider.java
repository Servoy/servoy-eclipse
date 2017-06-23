/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;

/**
 * @author lvostinar
 *
 */
public class NamedFoundsetContentProvider implements ITreeContentProvider
{

	public final static Object NAMED_FOUNDSET = new Object();
	public final FlattenedSolution flattenedSolution;

	public NamedFoundsetContentProvider(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		return new Object[] { NAMED_FOUNDSET };
	}

	@Override
	public Object[] getChildren(Object parentElement)
	{
		Map<String, Form> namedFoundsetForms = new HashMap<String, Form>();
		if (parentElement == NAMED_FOUNDSET)
		{
			Iterator<Form> it = flattenedSolution.getForms(false);
			while (it.hasNext())
			{
				Form form = it.next();
				if (form.getNamedFoundSet() != null && form.getNamedFoundSet().startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
				{
					String name = form.getNamedFoundSet().substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
					if (!namedFoundsetForms.containsKey(name))
					{
						namedFoundsetForms.put(name, form);
					}
				}
			}
		}
		return namedFoundsetForms.values().toArray();
	}

	@Override
	public Object getParent(Object element)
	{
		if (element != NAMED_FOUNDSET)
		{
			return NAMED_FOUNDSET;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		if (element == NAMED_FOUNDSET)
		{
			Object[] forms = getChildren(element);
			return forms != null && forms.length > 0;
		}
		return false;
	}

}
