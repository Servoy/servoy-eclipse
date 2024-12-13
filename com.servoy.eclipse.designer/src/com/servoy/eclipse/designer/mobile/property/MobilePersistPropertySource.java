/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.persistence.constants.IColumnTypeConstants;
import com.servoy.base.persistence.constants.IFieldConstants;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ComboboxPropertyController;
import com.servoy.eclipse.ui.property.ComboboxPropertyModel;
import com.servoy.eclipse.ui.property.IPropertyHandler;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Utils;

/**
 * PersistPropertySource to filter out properties not used in mobile solutions.
 *
 * @author rgansevles
 *
 */
public class MobilePersistPropertySource extends PersistPropertySource
{
	public static final ComboboxPropertyController<Integer> MOBILE_DISPLAY_TYPE_CONTROLLER = new ComboboxPropertyController<Integer>(
		"displayType",
		RepositoryHelper.getDisplayName("displayType", Field.class),
		new ComboboxPropertyModel<Integer>(
			new Integer[] { Integer.valueOf(Field.TEXT_FIELD), Integer.valueOf(Field.TEXT_AREA), Integer.valueOf(Field.CALENDAR), Integer
				.valueOf(Field.COMBOBOX), Integer.valueOf(Field.RADIOS), Integer.valueOf(Field.CHECKS), Integer.valueOf(Field.PASSWORD) },
			new String[] { "TEXT_FIELD", "TEXT_AREA", "CALENDAR", "COMBOBOX", "RADIOS", "CHECKS", "PASSWORD" }),
		Messages.LabelUnresolved);


	/**
	 * @param persistContext
	 * @param readonly
	 */
	public MobilePersistPropertySource(PersistContext persistContext, boolean readonly)
	{
		super(persistContext, readonly);
	}

	@Override
	protected boolean shouldShow(PropertyDescriptorWrapper propertyDescriptor)
	{
		IPersist persist = getPersist();
		if (isInternalProperty(propertyDescriptor.propertyDescriptor.getName()))
		{
			// Special case: add as hidden property, needed for setting value via palette
			return true;
		}
		if (!propertyDescriptor.propertyDescriptor.hasSupportForClientType(persist, ClientSupport.mc))
		{
			// do not show the property if the read-method is not flagged for mobile client
			return false;
		}
		if (propertyDescriptor.propertyDescriptor.getName().equals(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName()))
		{
			int dataproviderType = EclipseDatabaseUtils.getDataproviderType(persist, persist instanceof Field ? ((Field)persist).getFormat() : null,
				persist instanceof ISupportDataProviderID ? ((ISupportDataProviderID)persist).getDataProviderID() : null);
			if (dataproviderType == IColumnTypeConstants.INTEGER || dataproviderType == IColumnTypeConstants.NUMBER ||
				dataproviderType == IColumnTypeConstants.DATETIME)
			{
				return persist instanceof GraphicalComponent ||
					(persist instanceof Field &&
						((((Field)persist).getDisplayType() == IFieldConstants.TEXT_FIELD) || ((Field)persist).getDisplayType() == IFieldConstants.CALENDAR));
			}
			return false;
		}
		if (propertyDescriptor.propertyDescriptor.getName().equals(StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()))
		{
			if ((persist instanceof Field && ((Field)persist).getDisplayType() == IFieldConstants.CALENDAR))
			{
				return false;
			}
		}
		if (propertyDescriptor.propertyDescriptor.getName().equals("loginFormID"))
		{
			return true;
		}
		if (propertyDescriptor.propertyDescriptor.getName().equals("innerHTML") && (persistContext.getPersist() instanceof Bean))
		{
			return true;
		}
		return super.shouldShow(propertyDescriptor);
	}

	@Override
	protected boolean hideForProperties(PropertyDescriptorWrapper propertyDescriptor)
	{
		if (isInternalProperty(propertyDescriptor.propertyDescriptor.getName()))
		{
			// Special case: add as hidden property, needed for setting value via palette
			return true;
		}
		if ((StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName().equals(propertyDescriptor.propertyDescriptor.getName()) ||
			StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(propertyDescriptor.propertyDescriptor.getName()) ||
			StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(
				propertyDescriptor.propertyDescriptor.getName())) &&
			getPersist() instanceof GraphicalComponent &&
			Boolean.TRUE.equals(((GraphicalComponent)getPersist()).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName)) &&
			getContext() instanceof Form)
		{
			return true;
		}

		return RepositoryHelper.hideForMobileProperties(propertyDescriptor.propertyDescriptor.getName(), getPersist().getClass(),
			(getPersist() instanceof Field) ? ((Field)getPersist()).getDisplayType() : 0, isButton(getPersist())) ||

			super.hideForProperties(propertyDescriptor);
	}

	private boolean isInternalProperty(String propertyDescriptor)
	{
		if (StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName().equals(propertyDescriptor))
		{
			return true;
		}
		if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(propertyDescriptor))
		{
			return true;
		}
		if (StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName().equals(propertyDescriptor))
		{
			return true;
		}
		return false;
	}

	@Override
	protected IPropertyHandler[] getPseudoProperties(Class< ? > clazz, PersistContext context)
	{
		if (GraphicalComponent.class == clazz && isButton(getPersist()))
		{
			// button
			GraphicalComponent gc = (GraphicalComponent)getPersist();
			if (gc.getCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName) != null)
			{
				return new IPropertyHandler[] { new MobileCustomPropertyHandler(MobileCustomPropertyHandler.ALIGN_RIGHT_NAME), new MobileCustomPropertyHandler(
					IMobileProperties.DATA_ICON.propertyName) };
			}
			return new IPropertyHandler[] { new MobileCustomPropertyHandler(IMobileProperties.DATA_ICON.propertyName) };
		}

		if (GraphicalComponent.class == clazz && !isButton(getPersist()))
		{
			if (((AbstractBase)getPersist()).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) == null)
			{
				// script label
				return new IPropertyHandler[] { new MobileCustomPropertyHandler(IMobileProperties.HEADER_SIZE.propertyName) };
			}
			else
			{
				// header text, add header properties
				return new IPropertyHandler[] { new MobileCustomPropertyHandler(MobileCustomPropertyHandler.STICKY_PART_NAME) };
			}
		}

		if (Field.class == clazz)
		{
			Field field = (Field)getPersist();
			if (field.getDisplayType() == Field.RADIOS)
			{
				// radios check
				return new IPropertyHandler[] { new MobileCustomPropertyHandler(MobileCustomPropertyHandler.RADIO_STYLE_NAME) };
			}
		}

		if (Part.class == clazz)
		{
			return new IPropertyHandler[] { new MobileCustomPropertyHandler(MobileCustomPropertyHandler.STICKY_PART_NAME) };
		}

		return null;
	}

	/**
	 * @param persist
	 * @return
	 */
	private static boolean isButton(IPersist persist)
	{
		return persist instanceof GraphicalComponent && ComponentFactory.isButton((GraphicalComponent)persist);
	}

	@Override
	protected IPropertyDescriptor createPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor, FlattenedSolution flattenedEditingSolution, Form form,
		String id) throws RepositoryException
	{

		if (id.equals(StaticContentSpecLoader.PROPERTY_DISPLAYTYPE.getPropertyName()))
		{
			return MOBILE_DISPLAY_TYPE_CONTROLLER;
		}

		return super.createPropertyDescriptor(propertyDescriptor, flattenedEditingSolution, form, id);
	}

	private static PersistContext getHeaderPartForHeaderText(PersistContext persistContext)
	{
		if (persistContext.getPersist() instanceof GraphicalComponent &&
			Boolean.TRUE.equals(((GraphicalComponent)persistContext.getPersist()).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName)) &&
			persistContext.getContext() instanceof Form)
		{
			// header text, find header part
			Form form = (Form)persistContext.getContext();
			for (Part part : Utils.iterate(form.getParts()))
			{
				if (PersistUtils.isHeaderPart(part.getPartType()))
				{
					return PersistContext.create(part, form);
				}
			}
		}

		return null;
	}

	@Override
	public void setPersistPropertyValue(Object id, Object value)
	{
		super.setPersistPropertyValue(id, value);

		// set style on header text, copy to header part for FormList (not for InsetList)
		if (StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName().equals(id))
		{
			PersistContext headerPart = getHeaderPartForHeaderText(persistContext);
			if (headerPart != null)
			{
				new PersistPropertySource(headerPart, false).setPersistPropertyValue(id, value);
			}
		}
	}

	@Override
	protected String getActualComponentName()
	{
		IPersist persist = persistContext.getPersist();

		// if header text, find header part
		PersistContext headerPart = getHeaderPartForHeaderText(persistContext);
		if (headerPart != null)
		{
			persist = headerPart.getPersist();
		}

		return getActualComponentName(persist);
	}
}
