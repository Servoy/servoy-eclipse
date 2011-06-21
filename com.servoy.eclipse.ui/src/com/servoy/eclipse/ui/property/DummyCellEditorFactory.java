/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * CellEditorFactory that creates a dummy cell editor, this enables restore-default in the properties view menu.
 * 
 * @author rgansevles
 *
 */
public class DummyCellEditorFactory implements ICellEditorFactory
{
	private final ILabelProvider labelProvider;

	public DummyCellEditorFactory(ILabelProvider labelProvider)
	{
		this.labelProvider = labelProvider;
	}

	public CellEditor createPropertyEditor(Composite parent)
	{
		return new CellEditor(parent, SWT.NONE)
		{
			private Object value;

			@Override
			protected Control createControl(Composite parent)
			{
				return new Label(parent, SWT.NONE);
			}

			@Override
			protected Object doGetValue()
			{
				return value;
			}

			@Override
			protected void doSetFocus()
			{
			}

			@Override
			protected void doSetValue(Object newValue)
			{
				this.value = newValue;
				if (getControl() != null && !getControl().isDisposed())
				{
					((Label)getControl()).setText(labelProvider.getText(newValue));
				}
			}
		};
	}
}