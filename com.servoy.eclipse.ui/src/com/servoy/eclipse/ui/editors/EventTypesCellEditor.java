/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.EventTypesDialog;
import com.servoy.eclipse.ui.property.EventTypesPropertyController.EventTypesLabelProvider;

/**
 * @author lvostinar
 *
 */
public class EventTypesCellEditor extends DialogCellEditor
{
	public EventTypesCellEditor(Composite parent)
	{
		super(parent, EventTypesLabelProvider.LABEL_INSTANCE, null, false, SWT.NONE);
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		try
		{
			EventTypesDialog dialog = new EventTypesDialog(cellEditorWindow.getShell(), getValue());
			dialog.open();

			if (dialog.getReturnCode() != Window.CANCEL)
			{
				return dialog.getValue();
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}
}
