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
package com.servoy.eclipse.ui.util;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Extended ComboBoxCellEditor.
 * <p>
 * When a value is selected, the value is applied immediately when the user makes a selection.
 * 
 * @author rob
 * 
 */
public class ModifiedComboBoxCellEditor extends ComboBoxCellEditor
{

	public ModifiedComboBoxCellEditor(Composite parent, String[] items, int style)
	{
		super(parent, items, style);
	}

	@Override
	protected Control createControl(Composite parent)
	{
		CCombo combo = (CCombo)super.createControl(parent);
		combo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				// the selection is already updated at this point using the SelectionAdapter created in super.createControl()
				fireApplyEditorValue();
			}
		});
		return combo;
	}
}
