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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.Form;

/**
 * @author lvostinar
 *
 */
public class NamedFoundsetLabelProvider extends LabelProvider
{
	private static final Image FORM_IMAGE = Activator.getDefault().loadImageFromBundle("designer.gif");

	@Override
	public String getText(Object value)
	{
		if (value == NamedFoundsetContentProvider.NAMED_FOUNDSET)
		{
			return "Named foundsets";
		}
		if (value instanceof Form)
		{
			String name = ((Form)value).getNamedFoundSet().substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
			return name + " [" + ((Form)value).getDataSource() + "]";
		}
		return null;
	}

	@Override
	public Image getImage(Object value)
	{
		return (value == NamedFoundsetContentProvider.NAMED_FOUNDSET) ? FORM_IMAGE : null;
	}
}
