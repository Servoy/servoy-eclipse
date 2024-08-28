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

package com.servoy.eclipse.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.views.properties.IPropertySheetEntry;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;

/**
 * @author lvostinar
 *
 */
public class CopyPropertyValueAction extends Action
{
	private final Clipboard clipboard;
	private final String id;
	private IStructuredSelection selection;

	/**
	 * Creates the action.
	 *
	 * @param viewer the viewer
	 * @param name the name
	 * @param clipboard the clipboard
	 */
	public CopyPropertyValueAction(String name,
		Clipboard clipboard)
	{
		super(name);
		this.clipboard = clipboard;
		this.id = name;
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("copy_edit.png"));
	}

	@Override
	public String getId()
	{
		return id;
	}

	/**
	 * Performs this action.
	 */
	@Override
	public void run()
	{
		if (selection.isEmpty())
		{
			return;
		}
		// Assume single selection
		IPropertySheetEntry entry = (IPropertySheetEntry)selection
			.getFirstElement();

		setClipboard(entry.getValueAsString());
	}

	/**
	 * Updates enablement based on the current selection.
	 *
	 * @param sel the selection
	 */
	public void selectionChanged(IStructuredSelection sel)
	{
		setEnabled(!sel.isEmpty());
		this.selection = sel;
	}

	private void setClipboard(String text)
	{
		try
		{
			Object[] data = new Object[] { text };
			Transfer[] transferTypes = new Transfer[] { TextTransfer
				.getInstance() };
			clipboard.setContents(data, transferTypes);
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
		}
	}
}
