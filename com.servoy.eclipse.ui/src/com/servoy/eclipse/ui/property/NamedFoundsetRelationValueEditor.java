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
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;

/**
 *  Value editor for namedFoundset property. It is able to open global relations if available.
 *
 * @author acostescu
 *
 */
public class NamedFoundsetRelationValueEditor implements IValueEditor<String>
{

	private final FlattenedSolution fs;

	public NamedFoundsetRelationValueEditor(Form form)
	{
		this.fs = ModelUtils.getEditingFlattenedSolution(form);
	}

	public void openEditor(String value)
	{
		EditorUtil.openRelationEditor(fs.getRelation(value));
	}

	public boolean canEdit(String value)
	{
		return value != null && fs.getRelation(value) != null;
	}

}
