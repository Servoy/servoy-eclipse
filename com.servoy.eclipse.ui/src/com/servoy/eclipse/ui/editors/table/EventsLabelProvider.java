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
package com.servoy.eclipse.ui.editors.table;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

import com.servoy.eclipse.ui.editors.table.EventsComposite.EventNode;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;

public class EventsLabelProvider extends LabelProvider implements ITableLabelProvider
{
	public Image getColumnImage(Object element, int columnIndex)
	{
		if (element instanceof EventNode)
		{
			if (columnIndex == EventsComposite.CI_NAME)
			{
				EventNode node = (EventNode)element;
				if (node.isSolution())
				{
					return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
				}
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex)
	{
		if (element instanceof EventNode)
		{
			EventNode node = (EventNode)element;
			switch (columnIndex)
			{
				case EventsComposite.CI_NAME :
					if (node.isSolution()) return node.getSolution().getName();
					return node.getName();
				case EventsComposite.CI_METHOD :
					if (node.isSolution()) return "";
					return new SolutionContextDelegateLabelProvider(new MethodLabelProvider(PersistContext.create(node.getSolution()), true, true),
						node.getSolution()).getText(node.getMethodWithArguments());
				default :
					return columnIndex + ": " + element;
			}
		}

		return element.toString();
	}

}
