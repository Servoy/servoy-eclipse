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
package com.servoy.eclipse.ui.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;

import com.servoy.eclipse.dnd.IDragData;

/**
 * @author jcompagner
 * 
 */
public class UserNodeListDragSourceListener implements DragSourceListener
{
	public static IDragData[] dragObjects;

	private final ISelectionProvider list;
	private final Transfer transfer;

	public UserNodeListDragSourceListener(ISelectionProvider list, Transfer transfer)
	{
		this.list = list;
		this.transfer = transfer;
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragFinished(DragSourceEvent event)
	{
		dragObjects = null;
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragSetData(DragSourceEvent event)
	{
		if (dragObjects != null && transfer.isSupportedType(event.dataType))
		{
			event.data = dragObjects;
		}
	}

	/**
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragStart(DragSourceEvent event)
	{
		dragObjects = null;
		List<IDragData> lst = new ArrayList<IDragData>();
		Iterator<Object> iterator = ((IStructuredSelection)list.getSelection()).iterator();
		while (iterator.hasNext())
		{
			Object element = iterator.next();
			if (element instanceof SimpleUserNode)
			{
				Object real = ((SimpleUserNode)element).getRealObject();
				if (real != null)
				{
					IDragData dragObject = (IDragData)Platform.getAdapterManager().getAdapter(real, IDragData.class);
					if (dragObject != null)
					{
						lst.add(dragObject);
					}
				}
			}
		}

		if (lst.size() > 0)
		{
			dragObjects = lst.toArray(new IDragData[lst.size()]);
			event.image = com.servoy.eclipse.ui.Activator.getDefault().loadImageFromOldLocation("button.gif"); // TODO: better drag image
		}

		event.doit = dragObjects != null;
	}
}
