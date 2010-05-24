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

import java.awt.Component;
import java.beans.PropertyEditor;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.ui.dialogs.AwtComponentDialog;

public class BeanCustomCellEditor extends DialogCellEditor
{
	private final PropertyEditor propertyEditor;
	private final String title;
	private final String propertyName;

	public BeanCustomCellEditor(Composite parent, PropertyEditor propertyEditor, ILabelProvider labelProvider, String propertyName, String title,
		boolean readOnly)
	{
		super(parent, labelProvider, null, readOnly, SWT.NONE);
		this.propertyEditor = propertyEditor;
		this.propertyName = propertyName;
		this.title = title;
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		Component customEditor;
		Object oldValue;
		try
		{
			oldValue = getValue();
			propertyEditor.setValue(oldValue);
			customEditor = propertyEditor.getCustomEditor();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not create custom cell editor for bean editor " + propertyEditor, e);
			return null;
		}
		AwtComponentDialog dialog = new AwtComponentDialog(cellEditorWindow.getShell(), customEditor, title, "customBeanProperty." +
			propertyEditor.getClass().getName() + '.' + propertyName);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			return propertyEditor.getValue();
		}

		// restore previous value
		propertyEditor.setValue(oldValue);
		return null;
	}
}
