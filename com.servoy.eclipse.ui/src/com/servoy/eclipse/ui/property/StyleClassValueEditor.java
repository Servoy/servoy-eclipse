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

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
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
	private final IPersist persist;

	public StyleClassValueEditor(Form form, IPersist persist)
	{
		this.form = form;
		this.persist = persist;
	}

	public void openEditor(String value)
	{
		Style style = ModelUtils.getEditingFlattenedSolution(form).getStyleForForm(form, null);
		if (style != null)
		{
			String lookup = StyleClassesComboboxModel.getStyleLookupname(persist);
			if (value == null) value = lookup;
			else value = lookup + "." + value;
			EditorUtil.openStyleEditor(style, value);
		}
	}

	public boolean canEdit(String value)
	{
		return ModelUtils.getEditingFlattenedSolution(form).getStyleForForm(form, null) != null;
	}
}
