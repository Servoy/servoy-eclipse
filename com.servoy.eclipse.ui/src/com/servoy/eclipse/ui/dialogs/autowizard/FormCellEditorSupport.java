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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author emera
 */
public class FormCellEditorSupport extends EditingSupport
{
	private final PersistContext persistContext;
	private final CellEditor cellEditor;
	private final PropertyDescription dp;

	public FormCellEditorSupport(ColumnViewer viewer, PropertyDescription dp, PersistContext persistContext,
		ILabelProvider formLabelProvider, FlattenedSolution fs, Composite table)
	{
		super(viewer);
		this.dp = dp;
		this.cellEditor = new ListSelectCellEditor(table, "Select form",
			new FormContentProvider(fs, null /* persist is solution */), formLabelProvider,
			null, false,
			new FormContentProvider.FormListOptions(FormListOptions.FormListType.FORMS, null,
				true, false, false, false, null),
			SWT.NONE, null, "Select form dialog");
		this.persistContext = persistContext;
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

	@Override
	protected Object getValue(Object value)
	{
		Map<String, Object> row = (Map<String, Object>)value;
		Object val = row.get(dp.getName());
		IPersist persist = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext())
			.searchPersist((String)val);
		if (persist instanceof AbstractBase)
		{
			return new Integer(persist.getID());
		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		Form frm = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext())
			.getForm(((Integer)value).intValue());
		String val = (frm == null) ? null : frm.getUUID().toString();
		Map<String, Object> rowValue = (Map<String, Object>)element;
		rowValue.put(dp.getName(), val);
		getViewer().update(element, null);
	}
}