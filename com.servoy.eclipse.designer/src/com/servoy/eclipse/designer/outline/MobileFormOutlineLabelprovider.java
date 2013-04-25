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

import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Portal;

/**
 * Label provider for Servoy mobile form in outline view.
 * 
 * @author gboros
 */

public class MobileFormOutlineLabelprovider extends FormOutlineLabelprovider
{
	public static final MobileFormOutlineLabelprovider MOBILE_FORM_OUTLINE_LABEL_PROVIDER_INSTANCE = new MobileFormOutlineLabelprovider();

	@Override
	public Image getImage(Object element)
	{
		if (element instanceof FormElementGroup)
		{
			return getImageForPersist(MobileFormOutlineContentProvider.getGroupMainComponent((FormElementGroup)element));
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element)
	{
		if (element instanceof MobileListModel)
		{
			ISupportChilds list = ((MobileListModel)element).component;
			if (list instanceof Portal) return ((Portal)list).getName();
		}
		if (element instanceof FormElementGroup)
		{
			IFormElement component = MobileFormOutlineContentProvider.getGroupMainComponent((FormElementGroup)element);
			String name = component != null ? component.getName() : null;
			return name == null ? Messages.LabelAnonymous : name;
		}

		return super.getText(element);
	}
}
