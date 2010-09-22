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

package com.servoy.eclipse.ui.property;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Style;

/**
 * Value editor for style classes, opens the style for a form and selects the style class.
 * 
 * @author rgansevles
 *
 */
public class StyleClassValueEditor implements IValueEditor<String>
{
	private final Form form;

	public StyleClassValueEditor(Form form)
	{
		this.form = form;
	}

	public void openEditor(String value)
	{
		Style style = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form).getStyleForForm(form, null);
		if (style != null)
		{
			EditorUtil.openStyleEditor(style, value);
		}
	}

	public boolean canEdit(String value)
	{
		return ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form).getStyleForForm(form, null) != null;
	}
}
