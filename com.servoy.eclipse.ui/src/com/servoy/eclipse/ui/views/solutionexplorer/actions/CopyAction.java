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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
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

import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.dnd.IDragData;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportText;

/**
 * Copies the selected objects to the clipboard - if it is a valid selection from a "copy" point of view
 * 
 * @author Andrei Costescu
 */
public class CopyAction extends Action implements ISelectionChangedListener
{

	private final Display display;
	private ISelection validSelection;

	/**
	 * New copy action.
	 * 
	 * @param display the display used to create a clipboard.
	 */
	public CopyAction(Display display)
	{
		this.display = display;
		setEnabled(false);

		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setText("Copy");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		ISelection sel = event.getSelection();
		boolean enabled = true;
		if (sel instanceof StructuredSelection)
		{
			StructuredSelection s = (StructuredSelection)sel;
			Iterator<SimpleUserNode> it = s.iterator();
			int persistType = -1;
			while (it.hasNext() && enabled)
			{
				SimpleUserNode node = it.next();
				Object realObject = node.getRealObject();
				if (node.getType() == UserNodeType.SOLUTION && realObject != null)
				{
					realObject = ((ServoyProject)realObject).getSolution();
				}
				if (node.getType() == UserNodeType.FORM_ELEMENTS_ITEM)
				{
					realObject = ((Object[])realObject)[0];
				}

				if ((!(realObject instanceof IPersist)) ||
					(node.getType() != UserNodeType.SOLUTION && node.getType() != UserNodeType.SOLUTION_ITEM &&
						node.getType() != UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE && node.getType() != UserNodeType.FORM &&
						node.getType() != UserNodeType.FORM_ELEMENTS_ITEM && node.getType() != UserNodeType.RELATION))
				{
					enabled = false;
				}
				else
				{
					if (persistType != -1 && persistType != ((IPersist)realObject).getTypeID())
					{
						enabled = false;
					}
					persistType = ((IPersist)realObject).getTypeID();
				}
			}
		}
		else
		{
			enabled = false;
		}

		if (enabled)
		{
			validSelection = sel;
		}
		else
		{
			validSelection = null;
		}
		setEnabled(enabled);
	}

	@Override
	public void run()
	{
		if (validSelection == null) return;
		if (validSelection instanceof StructuredSelection)
		{
			StructuredSelection s = (StructuredSelection)validSelection;
			Iterator<SimpleUserNode> it = s.iterator();
			List<Object> data = new ArrayList<Object>();
			List<Transfer> transfers = new ArrayList<Transfer>();
			List<Object> persistDragData = new ArrayList<Object>();
			String text = "";

			while (it.hasNext())
			{
				SimpleUserNode node = it.next();
				Object realObject = node.getRealObject();
				if (((realObject instanceof ServoyProject) && (((ServoyProject)realObject).getSolution() != null)))
				{
					realObject = ((ServoyProject)realObject).getSolution();
				}
				if (node.getType() == UserNodeType.FORM_ELEMENTS_ITEM)
				{
					realObject = ((Object[])realObject)[0];
				}
				IPersist persist = (IPersist)realObject;
				String string = null;
				if (persist instanceof ISupportText)
				{
					string = ((ISupportText)persist).getText();
				}
				if (string == null && persist instanceof ISupportName)
				{
					string = ((ISupportName)persist).getName();
				}

				IDragData dragData = (IDragData)Platform.getAdapterManager().getAdapter(persist, IDragData.class);

				if (dragData != null)
				{
					persistDragData.add(dragData);
				}

				if (string != null)
				{
					text += " " + string;
				}
			}
			if (persistDragData.size() != 0)
			{
				data.add(persistDragData.toArray(new Object[persistDragData.size()]));
				transfers.add(FormElementTransfer.getInstance());

				if (text.length() > 0)
				{
					data.add(text.substring(1));
					transfers.add(TextTransfer.getInstance());
				}
				Clipboard clipboard = new Clipboard(display);
				clipboard.setContents(data.toArray(new Object[data.size()]), transfers.toArray(new Transfer[data.size()]));
				clipboard.dispose();
			}
		}
	}
}