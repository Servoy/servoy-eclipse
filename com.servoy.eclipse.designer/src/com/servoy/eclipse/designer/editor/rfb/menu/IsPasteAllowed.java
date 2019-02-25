/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.menu;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;

/**
 * @author costinchiulan
 *
 */
public class IsPasteAllowed extends PropertyTester
{

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
	{
		boolean isAllowed = false;
		if (receiver instanceof PersistContext)
		{
			IPersist persist = ((PersistContext)receiver).getPersist();
			Object clipboardContent = getClipboardContents();
			if (persist instanceof LayoutContainer)
			{
				if (clipboardContent instanceof Object[])
				{
					isAllowed = true;
					for (Object tp : (Object[])clipboardContent)
					{
						if (tp instanceof PersistDragData) return isAllowed;
					}
				}
			}
			else
			{
				isAllowed = (clipboardContent instanceof Object[] && ((Object[])clipboardContent).length > 0 &&
					((PersistContext)receiver).getContext() instanceof Form && !((Form)((PersistContext)receiver).getContext()).isResponsiveLayout());
			}
		}
		return isAllowed;
	}

	public static Object getClipboardContents()
	{
		Object clip = null;
		Clipboard cb = new Clipboard(Display.getDefault());
		try
		{
			TransferData[] transferTypes = cb.getAvailableTypes();
			for (TransferData transferData : transferTypes)
			{
				if (FormElementTransfer.getInstance().isSupportedType(transferData))
				{
					clip = cb.getContents(FormElementTransfer.getInstance());
					break;
				}
				if (clip == null && TextTransfer.getInstance().isSupportedType(transferData))
				{
					clip = cb.getContents(TextTransfer.getInstance());
					continue;
				}
			}
		}
		finally
		{
			cb.dispose();
		}
		return clip;
	}


}
