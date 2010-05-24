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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;

/**
 * An cell editor for a text field that can be edited.
 * <p>
 * A combobox is shown with predefined values.
 * 
 * @author rob
 * 
 */

public class EditableComboboxPropertyController extends PropertyController<String, String>
{
	private final String[] displayValues;
	private static final ILabelProvider LABEL_PROVIDER = new LabelProvider()
	{
		@Override
		public String getText(Object element)
		{
			return element == null ? "" : element.toString();
		}
	};

	public EditableComboboxPropertyController(String id, String displayName, String[] displayValues)
	{
		super(id, displayName);
		this.displayValues = displayValues;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new ModifiedComboBoxCellEditor(parent, displayValues, SWT.NONE) // not SWT.READ_ONLY
		{
			private CCombo comboBox;

			@Override
			protected Control createControl(Composite controlParent)
			{
				comboBox = (CCombo)super.createControl(controlParent);
				return comboBox;
			}

			@Override
			protected void doSetValue(Object value)
			{
				comboBox.setText(LABEL_PROVIDER.getText(value));
			}

			@Override
			protected Object doGetValue()
			{
				return comboBox.getText();
			}
		};
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return LABEL_PROVIDER;
	}
}
