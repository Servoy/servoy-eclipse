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
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.properties.PropertySheetEntry;

/**
 * @author lvostinar
 *
 */
public class PastePropertyValueAction extends Action
{
	private final Clipboard clipboard;
	private final String id;
	private IStructuredSelection selection;
	private final PropertySheetPage propertySheetPage;

	/**
	 * Creates the action.
	 *
	 * @param viewer the viewer
	 * @param name the name
	 * @param clipboard the clipboard
	 * @param modifiedPropertySheetPage
	 */
	public PastePropertyValueAction(String name,
		Clipboard clipboard, PropertySheetPage propertySheetPage)
	{
		super(name);
		this.clipboard = clipboard;
		this.id = name;
		this.propertySheetPage = propertySheetPage;
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("paste_edit.png"));
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

		String value = getClipboardText();
		if (value != null && value.length() > 0)
		{
			// cancel editor first
			propertySheetPage.refresh();
			for (Object element : selection)
			{
				((PropertySheetEntry)element).setValue(value);
			}
		}
	}

	/**
	 * Updates enablement based on the current selection.
	 *
	 * @param sel the selection
	 */
	public void selectionChanged(IStructuredSelection sel)
	{
		this.selection = sel;
		if (sel.isEmpty())
		{
			setEnabled(false);
		}
		else
		{
			String value = getClipboardText();
			setEnabled(value != null && value.length() > 0);
		}
		;
	}

	private String getClipboardText()
	{
		try
		{
			return (String)clipboard.getContents(TextTransfer
				.getInstance());
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}
}
