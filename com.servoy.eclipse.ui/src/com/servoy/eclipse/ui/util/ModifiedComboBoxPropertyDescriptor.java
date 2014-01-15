/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.util;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;

/**
 * @author hhardut
 *
 */
public class ModifiedComboBoxPropertyDescriptor extends ComboBoxPropertyDescriptor
{
	private final String[] labelsArray;

	public ModifiedComboBoxPropertyDescriptor(Object id, String displayName, String[] labelsArray)
	{
		super(id, displayName, labelsArray);
		this.labelsArray = labelsArray;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		ComboBoxCellEditor editor = new ComboBoxCellEditor(parent, labelsArray, SWT.READ_ONLY)
		{
			private CCombo combo;

			@Override
			protected Control createControl(Composite parent)
			{
				combo = (CCombo)super.createControl(parent);
				return combo;
			}

			@Override
			public void setItems(String[] items)
			{
				super.setItems(items);
				int count = combo.getItems().length;
				if (count <= ModifiedComboBoxCellEditor.VISIBLE_ITEM_COUNT)
				{
					combo.setVisibleItemCount(count == 0 ? count : count - 1);
				}
				else combo.setVisibleItemCount(ModifiedComboBoxCellEditor.VISIBLE_ITEM_COUNT); //default count - fixing bug introduced by eclipse 4.3
			}
		};
		return editor;
	}
}
