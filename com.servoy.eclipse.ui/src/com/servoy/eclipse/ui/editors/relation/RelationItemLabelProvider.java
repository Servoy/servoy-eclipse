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

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.LiteralDataprovider;

/**
 * Label provider for relation items in relation editor.
 *
 * @author jblok
 */

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
			RelationRow info = (RelationRow)element;
			if ((info.getCIFrom() != null) || (info.getCITo() != null))
			{
				return Activator.getDefault().loadImageFromBundle("delete.png");
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex)
	{
		RelationRow info = (RelationRow)element;
		switch (columnIndex)
		{
			case RelationEditor.CI_FROM :
				if (info.getCIFrom() == null) return RelationEditor.EMPTY;
				if (info.getRawCIFrom().startsWith(LiteralDataprovider.LITERAL_PREFIX)) return info.getCIFrom();
				String ci_from = relationEditor.getDataProvidersIndex(RelationEditor.CI_FROM, info.getCIFrom());
				if (ci_from != null) return info.getCIFrom();
				return UnresolvedValue.getUnresolvedMessage(info.getCIFrom());
			case RelationEditor.CI_OP :
				return OperatorEditingSupport.getColumnText(info.getMaskedOperator());
			case RelationEditor.CI_MASK :
				return OperatorMaskEditingSupport.getColumnText(info.getMask());
			case RelationEditor.CI_TO :
				if (info.getCITo() == null) return RelationEditor.EMPTY;
				String ci_to = relationEditor.getDataProvidersIndex(RelationEditor.CI_TO, info.getCITo());
				return ci_to != null ? info.getCITo() : UnresolvedValue.getUnresolvedMessage(info.getCITo());
			case RelationEditor.CI_DELETE :
				return "";
			default :
				return columnIndex + ": " + element;
		}
	}
}
