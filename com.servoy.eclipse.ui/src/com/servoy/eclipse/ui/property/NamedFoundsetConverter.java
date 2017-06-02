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

package com.servoy.eclipse.ui.property;

import java.util.Iterator;

import org.eclipse.team.internal.ui.Utils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;

/**
 * @author lvostinar
 *
 */
public class NamedFoundsetConverter implements IPropertyConverter<String, Form>
{
	private final FlattenedSolution flattenedSolution;

	public NamedFoundsetConverter(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	public Form convertProperty(Object id, String value)
	{
		if (value != null)
		{
			Iterator<Form> it = flattenedSolution.getForms(false);
			while (it.hasNext())
			{
				Form form = it.next();
				if (form.getNamedFoundSet() != null && Utils.equalObject(form.getNamedFoundSet(), Form.NAMED_FOUNDSET_SEPARATE_PREFIX + value))
				{
					return form;
				}
			}
		}
		return null;
	}

	public String convertValue(Object id, Form value)
	{
		return value.getNamedFoundSet().substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
	}
}
