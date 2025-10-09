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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.dnd.IDragData;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportText;

/**
 * Copy an draggable objects and their name or text as string (if available) to the clipboard.
 *
 * @author rgansevles
 *
 */
public class FormElementCopyCommand extends Command
{
	private final Object[] elements;

	public FormElementCopyCommand(Object[] elements)
	{
		this.elements = elements;
	}

	@Override
	public void execute()
	{
		// sort elements, parent objects first
		Arrays.sort(elements, new Comparator<Object>()
		{
			public int compare(Object o1, Object o2)
			{
				if (o2 instanceof ISupportChilds && o1 instanceof IPersist)
				{
					IPersist persist1 = ((IPersist)o1).getParent();
					while (persist1 != null)
					{
						if (persist1.equals(o2))
						{
							return 1;
						}
						persist1 = persist1.getParent();
					}
				}
				if (o1 instanceof ISupportChilds && o2 instanceof IPersist)
				{
					IPersist persist2 = ((IPersist)o2).getParent();
					while (persist2 != null)
					{
						if (persist2.equals(o1))
						{
							return -1;
						}
						persist2 = persist2.getParent();
					}
				}
				// objects are not in same hierarchy
				return 0;
			}
		});

		StringBuilder sb = null;
		List<IDragData> dragDatas = new ArrayList<IDragData>(elements.length);
		for (Object element : elements)
		{
			if (element instanceof WebFormComponentChildType formElementChild)
			{
				element = formElementChild.getElement();
			}
			IDragData dragData = Platform.getAdapterManager().getAdapter(element, IDragData.class);
			if (dragData != null)
			{
				dragDatas.add(dragData);
			}

			String string = null;
			if (element instanceof ISupportText)
			{
				string = ((ISupportText)element).getText();
			}
			else if (string == null && element instanceof ISupportName)
			{
				string = ((ISupportName)element).getName();
			}
			if (string != null && string.length() > 0)
			{
				if (sb == null) sb = new StringBuilder();
				sb.append(string);
			}
		}

		List<Object> data = new ArrayList<Object>();
		List<Transfer> transfers = new ArrayList<Transfer>();

		if (dragDatas.size() > 0)
		{
			data.add(dragDatas.toArray());
			transfers.add(FormElementTransfer.getInstance());
		}

		if (sb != null)
		{
			data.add(sb.toString());
			transfers.add(TextTransfer.getInstance());
		}

		if (data.size() == 0)
		{
			return;
		}

		Clipboard cb = new Clipboard(Display.getCurrent());
		cb.setContents(data.toArray(new Object[data.size()]), transfers.toArray(new Transfer[data.size()]));
		cb.dispose();
	}

	@Override
	public boolean canUndo()
	{
		return false;
	}
}
