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
package com.servoy.eclipse.ui.editors.relation;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.util.Utils;

public class RelationItemLabelProvider extends LabelProvider implements ITableLabelProvider
{
	private final RelationEditor relationEditor;

	public RelationItemLabelProvider(RelationEditor re)
	{
		relationEditor = re;
	}

	public Image getColumnImage(Object element, int columnIndex)
	{
		if (columnIndex == RelationEditor.CI_DELETE)
		{
			Integer[] info = (Integer[])element;
			if ((info[RelationEditor.CI_FROM] != null && info[RelationEditor.CI_FROM] > 0) ||
				(info[RelationEditor.CI_TO] != null && info[RelationEditor.CI_TO] > 0))
			{
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex)
	{
		Integer[] info = (Integer[])element;
		switch (columnIndex)
		{
			case RelationEditor.CI_FROM :
			case RelationEditor.CI_TO :
				String[] dps = relationEditor.getDataProviders(columnIndex);
				return (info[columnIndex] != null ? dps[Utils.getAsInteger(info[columnIndex])] : RelationEditor.EMPTY);
			case RelationEditor.CI_OP :
				return (info[columnIndex] != null ? RelationItem.getOperatorAsString(Utils.getAsInteger(info[columnIndex])) : RelationEditor.EMPTY);
			case RelationEditor.CI_DELETE :
				return "";
			default :
				return columnIndex + ": " + element;
		}
	}
}
