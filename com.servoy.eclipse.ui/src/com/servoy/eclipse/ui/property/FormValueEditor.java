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

import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;

/**
 * IValueEditor for forms, opens form designer.
 *
 * @author rgansevles
 */

public class FormValueEditor implements IValueEditor<String>
{
	protected final FlattenedSolution flattenedSolution;

	public FormValueEditor(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	public void openEditor(String value)
	{
		EditorUtil.openFormDesignEditor(flattenedSolution.getForm(value));
	}

	public boolean canEdit(String value)
	{
		if (value == null) return false;
		return !Form.NAVIGATOR_NONE.equals(value) && !Form.NAVIGATOR_IGNORE.equals(value) && flattenedSolution.getForm(value) != null;
	}
}
