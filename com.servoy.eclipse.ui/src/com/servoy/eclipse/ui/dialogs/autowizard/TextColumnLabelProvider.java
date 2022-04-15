/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Pair;

/**
 * @author jcomp
 *
 */
final class TextColumnLabelProvider extends ColumnLabelProvider
{
	private final PropertyDescription dp;

	/**
	 * @param dp
	 */
	public TextColumnLabelProvider(PropertyDescription dp)
	{
		this.dp = dp;
	}

	@Override
	public String getText(Object element)
	{
		Pair<String, Map<String, Object>> row = (Pair<String, Map<String, Object>>)element;
		Object value = row.getRight().get(dp.getName());
		return value == null ? "" : value.toString();
	}

	@Override
	public Image getImage(Object element)
	{
		return null;
	}
}