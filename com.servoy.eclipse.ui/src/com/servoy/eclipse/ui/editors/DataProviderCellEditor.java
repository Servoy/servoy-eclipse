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
package com.servoy.eclipse.ui.editors;


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.dialogs.DataProviderDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.property.DataProviderConverter;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;

/**
 * A cell editor that manages a dataprovider field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @author rgansevles
 */
public class DataProviderCellEditor extends DialogCellEditor
{
	private final Form form;
	private final FlattenedSolution flattenedSolution;
	private final DataProviderOptions input;
	private final DataProviderConverter converter;
	private final ITable table;
	private final String title;

	/**
	 * Creates a new dataprovider cell editor parented under the given control.
	 *
	 * @param parent
	 * @param labelProvider
	 * @param valueEditor
	 * @param form plain form, not flattened
	 * @param flattenedSolution
	 * @param readOnly
	 * @param input
	 * @param converter
	 */
	public DataProviderCellEditor(Composite parent, ILabelProvider labelProvider, IValueEditor<Object> valueEditor, Form form,
		FlattenedSolution flattenedSolution, boolean readOnly, DataProviderOptions input, DataProviderConverter converter, ITable table, String... title)
	{
		super(parent, labelProvider, valueEditor, readOnly, SWT.NONE);
		this.form = form instanceof FlattenedForm ? flattenedSolution.getForm(form.getID()) : form;
		this.flattenedSolution = flattenedSolution;
		this.input = input;
		this.converter = converter;
		this.table = table;
		this.title = title.length > 0 ? title[0] : "Select Data Provider";
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		DataProviderDialog dialog = new DataProviderDialog(cellEditorWindow.getShell(), getLabelProvider(), PersistContext.create(form), flattenedSolution,
			table != null ? table
				: ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(flattenedSolution.getFlattenedForm(form).getDataSource()),
			input, getSelection(), SWT.NONE, title);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			return ((IStructuredSelection)dialog.getSelection()).getFirstElement(); // single select
		}
		return null;
	}

	@Override
	public final Object doGetValue()
	{
		Object value = super.doGetValue();
		if (value instanceof IDataProvider)
		{
			return converter.convertValue(null, (IDataProvider)value);
		}
		else
		{
			return value;
		}
	}

	@Override
	public StructuredSelection getSelection()
	{
		Object value = super.getValue();
		if (value instanceof String) value = converter.convertProperty(null, (String)value);
		if (value == null) return StructuredSelection.EMPTY;
		if (value instanceof Object[])
		{
			return new StructuredSelection((Object[])value);
		}
		return new StructuredSelection(value);
	}

	public static class DataProviderValueEditor implements IValueEditor<Object>
	{
		private final DataProviderConverter converter;

		public DataProviderValueEditor(DataProviderConverter converter)
		{
			this.converter = converter;
		}

		public boolean canEdit(Object value)
		{
			IDataProvider provider = null;
			if (value instanceof String) provider = converter.convertProperty(null, (String)value);
			else if (value instanceof IDataProvider) provider = (IDataProvider)value;
			return provider != null && provider != DataProviderContentProvider.NONE;
		}

		public void openEditor(Object value)
		{
			IDataProvider provider = converter.convertProperty(null, (String)value);
			EditorUtil.openDataProviderEditor(provider);
		}
	}

}
