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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.util.DatabaseUtils;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author jcompagner
 *
 */
public class FormatCellEditor extends TextDialogCellEditor
{

	private final IPersist persist;
	private final String formatForPropertyname;

	/**
	 * @param parent
	 * @param persist 
	 */
	public FormatCellEditor(Composite parent, IPersist persist, String formatForPropertyname)
	{
		super(parent, SWT.NONE, null);
		this.persist = persist;
		this.formatForPropertyname = formatForPropertyname;
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{

		IPropertySource propertySource = (IPropertySource)Platform.getAdapterManager().getAdapter(persist, IPropertySource.class);
		String dataProviderID = (String)propertySource.getPropertyValue(formatForPropertyname);

		String formatString = (String)getValue();
		int type = DatabaseUtils.getDataproviderType(persist, formatString, dataProviderID);
		FormatDialog dialog = new FormatDialog(cellEditorWindow.getShell(), formatString, type);
		dialog.open();
		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return TextDialogCellEditor.CANCELVALUE;
		}
		return dialog.getFormatString();
	}
}
