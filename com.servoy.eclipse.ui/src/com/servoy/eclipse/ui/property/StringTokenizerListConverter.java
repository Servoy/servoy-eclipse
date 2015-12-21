/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.util.Utils;

/**
 * Split strings to list of elements.
 *
 * @author rgansevles
 *
 */
public class StringTokenizerListConverter implements IPropertyConverter<String, List<String>>
{
	private final String delim;
	private final boolean trim;

	public StringTokenizerListConverter(String delim, boolean trim)
	{
		this.delim = delim;
		this.trim = trim;
	}

	public List<String> convertProperty(Object id, String value)
	{
		if (value == null || (trim ? value.trim() : value).length() == 0)
		{
			return null;
		}
		return new ArrayList<>(Arrays.asList(Utils.getTokenElements(value, delim, trim)));
	}

	public String convertValue(Object id, List<String> value)
	{
		return ModelUtils.getTokenValue(value == null ? null : value.toArray(), delim);
	}
}
