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

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.persistence.constants.IFieldConstants;
import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.resource.ImageResource;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.Part;
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
	public Image getImage(Object object)
	{
		Object element;
		if (object instanceof FormElementGroup)
		{
			element = MobileFormOutlineContentProvider.getGroupMainComponent((FormElementGroup)object);
		}
		else if (object instanceof PersistContext)
		{
			element = ((PersistContext)object).getPersist();
		}
		else
		{
			element = object;
		}

		if (element instanceof MobileListModel)
		{
			return ImageResource.INSTANCE.getImage(Activator.loadImageDescriptorFromBundle("mobile/insetlist16.png"));
		}

		if (element instanceof IPersist)
		{
			String image;
			if (element instanceof Field)
			{
				switch (((Field)element).getDisplayType())
				{
					case IFieldConstants.RADIOS :
						image = "mobile/radios16.png";
						break;
					case IFieldConstants.CALENDAR :
						image = "mobile/calendar16.png";
						break;
					case IFieldConstants.CHECKS :
						image = "mobile/checks16.png";
						break;
					case IFieldConstants.TEXT_AREA :
						image = "mobile/textarea16.png";
						break;
					case IFieldConstants.PASSWORD :
						image = "mobile/password16.png";
						break;
					case IFieldConstants.COMBOBOX :
						image = "mobile/combo16.png";
						break;
					default :
						image = "mobile/text16.png";
				}
			}
			else if (element instanceof GraphicalComponent)
			{
				if (ComponentFactory.isButton((GraphicalComponent)element))
				{
					image = "mobile/button16.png";
				}
				else
				{
					image = "mobile/label16.png";
				}
			}
			else if (element instanceof Bean)
			{
				image = "mobile/bean16.png";
			}
			else if (element instanceof Part)
			{
				int partType = ((Part)element).getPartType();
				if (PersistUtils.isHeaderPart(partType))
				{
					image = "mobile/header16.png";
				}
				else if (PersistUtils.isFooterPart(partType))
				{
					image = "mobile/footer16.png";
				}
				else return null;
			}
			else if (element instanceof AbstractBase && ((AbstractBase)element).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
			{
				image = "mobile/headertitle16.png";
			}
			else
			{
				return getImageForPersist((IPersist)element);
			}

			return ImageResource.INSTANCE.getImage(Activator.loadImageDescriptorFromBundle(image));
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
			ISupportFormElement component = MobileFormOutlineContentProvider.getGroupMainComponent((FormElementGroup)element);
			String name = component != null ? component.getName() : null;
			return name == null ? Messages.LabelAnonymous : name;
		}

		if (element instanceof PersistContext)
		{
			IPersist persist = ((PersistContext)element).getPersist();
			if (persist instanceof Part)
			{
				int partType = ((Part)persist).getPartType();
				if (PersistUtils.isHeaderPart(partType))
				{
					return Messages.LabelHeader;
				}
				if (PersistUtils.isFooterPart(partType))
				{
					return Messages.LabelFooter;
				}
			}
		}

		return super.getText(element);
	}
}
