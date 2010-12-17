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

package com.servoy.eclipse.ui.editors.table.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.j2db.persistence.Column;

/**
 * This class implements the action of copying the text of a selected column's name to the clipboard.
 * 
 * @author acostache
 *
 */
public class CopyColumnNameAction extends Action implements ISelectionChangedListener
{
	private final Display display;
	private ISelection validSelection;

	/**
	 * New copy column name action.
	 * 
	 * @param display the display used to create a clipboard.
	 */
	public CopyColumnNameAction(Display display)
	{
		this.display = display;
		setEnabled(false);

		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setText("Copy"); //$NON-NLS-1$
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		ISelection sel = event.getSelection();
		boolean enabled = true;
		if (!(sel instanceof StructuredSelection))
		{
			enabled = false;
			validSelection = null;
		}
		else validSelection = sel;
		setEnabled(enabled);
	}

	@Override
	public void run()
	{
		if (validSelection == null) return;
		if (validSelection instanceof StructuredSelection)
		{
			StructuredSelection s = (StructuredSelection)validSelection;
			Column selCol = (Column)s.getFirstElement();

			String textData = selCol.getName();
			Object[] data = new Object[] { textData };
			TextTransfer textTransfer = TextTransfer.getInstance();
			Transfer[] transfers = new Transfer[] { textTransfer };

			Clipboard clipboard = new Clipboard(display);
			clipboard.setContents(data, transfers);
			clipboard.dispose();
		}
	}
}
