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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Relation;

/**
 * @author emera
 */
public class RelationCellEditorSupport extends EditingSupport
{
	private final PropertyDescription dp;
	CellEditor cellEditor;
	private final FlattenedSolution flattenedSolution;

	public RelationCellEditorSupport(ColumnViewer viewer, PropertyDescription dp, CellEditor editor, FlattenedSolution fs)
	{
		super(viewer);
		this.dp = dp;
		this.cellEditor = editor;
		this.flattenedSolution = fs;
	}

	@Override
	protected Object getValue(Object element)
	{
		Map<String, Object> row = (Map<String, Object>)element;
		Object val = row.get(dp.getName());
		if (val == null)
		{
			return RelationContentProvider.NONE;
		}
		Relation[] relations = flattenedSolution.getRelationSequence((String)val);
		if (relations == null)
		{
			return new UnresolvedValue((String)val);
		}
		return new RelationsWrapper(relations);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		String relationName = null;
		if (value instanceof RelationsWrapper)
		{
			RelationsWrapper relationsWrapper = (RelationsWrapper)value;
			Relation relation = relationsWrapper.relations[relationsWrapper.relations.length - 1];
			relationName = relation.getName();
		}

		Map<String, Object> rowValue = (Map<String, Object>)element;
		rowValue.put(dp.getName(), relationName);
		getViewer().update(element, null);
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return cellEditor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return true;
	}
}