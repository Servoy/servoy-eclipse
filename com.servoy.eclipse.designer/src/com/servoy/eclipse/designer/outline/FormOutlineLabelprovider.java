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
package com.servoy.eclipse.designer.outline;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.property.PersistContext;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SupportNameLabelProvider;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;

public class FormOutlineLabelprovider extends LabelProvider implements IPersistLabelProvider, IColorProvider
{
	public static final FormOutlineLabelprovider INSTANCE = new FormOutlineLabelprovider();

	@Override
	public Image getImage(Object element)
	{
		if (element == FormOutlineContentProvider.PARTS)
		{
			return Activator.getDefault().loadImageFromOldLocation("parts.gif");
		}
		if (element == FormOutlineContentProvider.ELEMENTS)
		{
			return Activator.getDefault().loadImageFromBundle("element.gif");
		}
		if (element instanceof PersistContext)
		{
			String image = ElementUtil.getPersistImageName(((PersistContext)element).getPersist());
			if (image == null)
			{
				image = "element.gif";
			}
			Image img = Activator.getDefault().loadImageFromOldLocation(image);
			if (img == null)
			{
				img = Activator.getDefault().loadImageFromBundle(image);
			}
			return img;
		}
		if (element instanceof FormElementGroup)
		{
			return Activator.getDefault().loadImageFromBundle("group.gif");
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element)
	{
		if (element == FormOutlineContentProvider.PARTS)
		{
			return "parts";
		}
		if (element == FormOutlineContentProvider.ELEMENTS)
		{
			return "elements";
		}
		if (element instanceof PersistContext)
		{
			if (((PersistContext)element).getPersist() instanceof Part)
			{
				return ((Part)((PersistContext)element).getPersist()).getEditorName();
			}
			return SupportNameLabelProvider.INSTANCE_DEFAULT_ANONYMOUS.getText(((PersistContext)element).getPersist());
		}
		if (element instanceof FormElementGroup)
		{
			String name = ((FormElementGroup)element).getName();
			return name == null ? Messages.LabelAnonymous : name;
		}
		return "???";
	}

	public Color getBackground(Object element)
	{
		return null;
	}

	public Color getForeground(Object element)
	{
		if (element instanceof PersistContext &&
			ElementUtil.isReadOnlyFormElement(((PersistContext)element).getContext(), ((PersistContext)element).getPersist()))
		{
			// readonly elements
			return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		}
		return null;
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof PersistContext)
		{
			return ((PersistContext)value).getPersist();
		}
		return null;
	}
}
