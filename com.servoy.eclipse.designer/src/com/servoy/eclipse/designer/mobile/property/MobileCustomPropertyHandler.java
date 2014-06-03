/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.designer.mobile.property;

import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.ValuesPropertyType;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.property.IPropertyHandler;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Utils;

/**
 * Property handler for pseduo
 *  properties that are stored in the mobile custom properties.
 *
 * @author rgansevles
 *
 */
public class MobileCustomPropertyHandler implements IPropertyHandler
{
	public static String[] DATA_ICONS = new String[] { //
	"alert", "arrow-d", "arrow-l", "arrow-r", "arrow-u", "back", "bars", "check",
//			"custom",
	"delete", "edit", "forward", "gear", "grid", "home", "info", "minus", "plus", "refresh", "search", "star" };

	public static final PropertyDescription DATA_ICONS_VALUES = new PropertyDescription(IMobileProperties.DATA_ICON.propertyName, ValuesPropertyType.INSTANCE,
		new ValuesConfig().setValues(DATA_ICONS).addDefault(null, null));


	public static final PropertyDescription HEADERSIZE_VALUES = new PropertyDescription(IMobileProperties.HEADER_SIZE.propertyName,
		ValuesPropertyType.INSTANCE, new ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(5), Integer.valueOf(6) },
			new String[] { "h1", "h2", "h3", "h4", "h5", "h6" }).addDefault(null, null));


	public static final String ALIGN_RIGHT_NAME = "alignRight";
	public static final PropertyDescription ALIGN_RIGHT_DESCRIPTION = new PropertyDescription(ALIGN_RIGHT_NAME, BooleanPropertyType.INSTANCE);


	public static final String RADIO_STYLE_NAME = "horizontal";
	public static final PropertyDescription RADIO_STYLE_DESCRIPTION = new PropertyDescription(RADIO_STYLE_NAME, BooleanPropertyType.INSTANCE);


	public static final String STICKY_PART_NAME = "sticky";
	public static final PropertyDescription STICKY_PART_DESCRIPTION = new PropertyDescription(STICKY_PART_NAME, BooleanPropertyType.INSTANCE);


	private final String name;

	public MobileCustomPropertyHandler(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isProperty()
	{
		return true;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{

		if (name.equals(IMobileProperties.HEADER_SIZE.propertyName))
		{
			return HEADERSIZE_VALUES;
		}

		if (RADIO_STYLE_NAME.equals(name))
		{
			return RADIO_STYLE_DESCRIPTION;
		}

		if (name.equals(IMobileProperties.DATA_ICON.propertyName))
		{
			return DATA_ICONS_VALUES;
		}

		if (STICKY_PART_NAME.equals(name))
		{
			return STICKY_PART_DESCRIPTION;
		}

		if (ALIGN_RIGHT_NAME.equals(name))
		{
			return ALIGN_RIGHT_DESCRIPTION;
		}

		return null;
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		AbstractBase persist = (AbstractBase)obj;
		if (ALIGN_RIGHT_NAME.equals(name))
		{
			return Boolean.valueOf(persist.getCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName) != null);
		}
		if (RADIO_STYLE_NAME.equals(name))
		{
			// 0: vertical (default)
			// 1: horizontal
			// ... future
			return Boolean.valueOf(IMobileProperties.RADIO_STYLE_HORIZONTAL.equals(persist.getCustomMobileProperty(IMobileProperties.RADIO_STYLE.propertyName)));
		}

		if (STICKY_PART_NAME.equals(name))
		{
			Part part = getPart(persist, persistContext);
			if (part == null)
			{
				return Boolean.FALSE;
			}
			int partType = part.getPartType();
			return Boolean.valueOf(partType == IPartConstants.TITLE_HEADER || partType == IPartConstants.TITLE_FOOTER);
		}

		return persist.getCustomMobileProperty(name);
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		AbstractBase persist = (AbstractBase)obj;

		if (ALIGN_RIGHT_NAME.equals(name))
		{
			if (Boolean.TRUE.equals(value))
			{
				persist.putCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName, Boolean.TRUE);
				persist.putCustomMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON.propertyName, null);
			}
			else
			{
				persist.putCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName, null);
				persist.putCustomMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON.propertyName, Boolean.TRUE);
			}
		}
		else if (RADIO_STYLE_NAME.equals(name))
		{
			// 0: vertical (default)
			// 1: horizontal
			// ... future
			persist.putCustomMobileProperty(IMobileProperties.RADIO_STYLE.propertyName, Boolean.TRUE.equals(value) ? IMobileProperties.RADIO_STYLE_HORIZONTAL
				: null);
		}
		else if (STICKY_PART_NAME.equals(name))
		{
			Part part = getPart(persist, persistContext);
			if (part != null)
			{
				int partType = part.getPartType();
				if (Boolean.TRUE.equals(value))
				{
					if (partType == IPartConstants.HEADER) part.setPartType(IPartConstants.TITLE_HEADER);
					else if (partType == IPartConstants.FOOTER) part.setPartType(IPartConstants.TITLE_FOOTER);
				}
				else
				{
					if (partType == IPartConstants.TITLE_HEADER) part.setPartType(IPartConstants.HEADER);
					else if (partType == IPartConstants.TITLE_FOOTER) part.setPartType(IPartConstants.FOOTER);
				}
			}
		}
		else
		{
			persist.putCustomMobileProperty(name, value);
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persist, false);
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return csp != null && csp.supports(ClientSupport.mc);
	}

	public boolean shouldShow(Object obj)
	{
		return true;
	}

	private static Part getPart(AbstractBase persist, PersistContext persistContext)
	{
		if (persist instanceof Part)
		{
			return (Part)persist;
		}
		if (persist instanceof GraphicalComponent && persist.getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
		{
			// header text, find header part
			Form form = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
			// TODO: check flattened form?
			for (Part part : Utils.iterate(form.getParts()))
			{
				if (PersistUtils.isHeaderPart(part.getPartType()))
				{
					return part;
				}
			}
		}
		return null;
	}
}
