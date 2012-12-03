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
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.CheckboxPropertyDescriptor;
import com.servoy.eclipse.ui.property.ComboboxPropertyController;
import com.servoy.eclipse.ui.property.ComboboxPropertyModel;
import com.servoy.eclipse.ui.property.DelegatePropertySetterController;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.annotations.ServoyMobile;

/**
 * PersistPropertySource to filter out properties not used in mobile solutions.
 * 
 * @author rgansevles
 *
 */
public class MobilePersistPropertySource extends PersistPropertySource
{
	public static final String HEADER_SIZE_PROPERTY = "headerSize"; //$NON-NLS-1$
	public static final PropertyController<Integer, Integer> MOBILE_LABEL_HEADERSIZE_CONTROLLER = new DelegatePropertySetterController<Integer, MobilePersistPropertySource>(
		new ComboboxPropertyController<Integer>(HEADER_SIZE_PROPERTY, HEADER_SIZE_PROPERTY, new ComboboxPropertyModel<Integer>(
			new Integer[] { Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(5), Integer.valueOf(6) }),
			Messages.LabelDefault), HEADER_SIZE_PROPERTY)
	{

		public void setProperty(MobilePersistPropertySource propertySource, Integer value)
		{
			((AbstractBase)propertySource.getPersist()).putCustomMobileProperty(HEADER_SIZE_PROPERTY, value);
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, propertySource.getPersist(), false);
		}

		public Integer getProperty(MobilePersistPropertySource propertySource)
		{
			return (Integer)((AbstractBase)propertySource.getPersist()).getCustomMobileProperty(HEADER_SIZE_PROPERTY);
		}
	};

	public static final String RADIO_STYLE_NAME = "horizontal"; //$NON-NLS-1$
	public static final String RADIO_STYLE_PROPERTY = "radioStyle"; //$NON-NLS-1$
	public static final Integer RADIO_STYLE_HORIZONTAL = Integer.valueOf(1);
	public static final PropertyController<Boolean, Boolean> MOBILE_RADIO_STYLE_CONTROLLER = new DelegatePropertySetterController<Boolean, MobilePersistPropertySource>(
		new CheckboxPropertyDescriptor(RADIO_STYLE_NAME, RADIO_STYLE_NAME), RADIO_STYLE_NAME)
	{
		// 0: vertical (default)
		// 1: horizontal
		// ... future
		public void setProperty(MobilePersistPropertySource propertySource, Boolean value)
		{
			((AbstractBase)propertySource.getPersist()).putCustomMobileProperty(RADIO_STYLE_PROPERTY, Boolean.TRUE.equals(value) ? RADIO_STYLE_HORIZONTAL
				: null);
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, propertySource.getPersist(), false);
		}

		public Boolean getProperty(MobilePersistPropertySource propertySource)
		{
			return Boolean.valueOf(RADIO_STYLE_HORIZONTAL.equals(((AbstractBase)propertySource.getPersist()).getCustomMobileProperty(RADIO_STYLE_PROPERTY)));
		}
	};

	public static final String DATA_ICON_PROPERTY = "dataIcon"; //$NON-NLS-1$
	public static String[] DATA_ICONS = new String[] { //
	"alert", //$NON-NLS-1$
	"arrow-d", //$NON-NLS-1$
	"arrow-l", //$NON-NLS-1$
	"arrow-r", //$NON-NLS-1$
	"arrow-u", //$NON-NLS-1$
	"back", //$NON-NLS-1$
	"check", //$NON-NLS-1$
//	"custom", //$NON-NLS-1$
	"delete", //$NON-NLS-1$
	"forward", //$NON-NLS-1$
	"gear", //$NON-NLS-1$
	"grid", //$NON-NLS-1$
	"home", //$NON-NLS-1$
	"info", //$NON-NLS-1$
	"minus", //$NON-NLS-1$
	"plus", //$NON-NLS-1$
	"refresh", //$NON-NLS-1$
	"search", //$NON-NLS-1$
	"star" //$NON-NLS-1$
	};

	public static final PropertyController<String, String> MOBILE_ICONS_CONTROLLER = new DelegatePropertySetterController<String, MobilePersistPropertySource>(
		new ComboboxPropertyController<String>(DATA_ICON_PROPERTY, DATA_ICON_PROPERTY, new ComboboxPropertyModel<String>(DATA_ICONS), Messages.LabelDefault),
		DATA_ICON_PROPERTY)
	{
		public void setProperty(MobilePersistPropertySource propertySource, String value)
		{
			((AbstractBase)propertySource.getPersist()).putCustomMobileProperty(DATA_ICON_PROPERTY, value);
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, propertySource.getPersist(), false);
		}

		public String getProperty(MobilePersistPropertySource propertySource)
		{
			return (String)((AbstractBase)propertySource.getPersist()).getCustomMobileProperty(DATA_ICON_PROPERTY);
		}
	};

	/**
	 * @param persistContext
	 * @param readonly
	 */
	public MobilePersistPropertySource(PersistContext persistContext, boolean readonly)
	{
		super(persistContext, readonly);
	}

	@Override
	protected boolean shouldShow(PropertyDescriptorWrapper propertyDescriptor) throws RepositoryException
	{
		if (propertyDescriptor.propertyDescriptor.getReadMethod() != null &&
			propertyDescriptor.propertyDescriptor.getReadMethod().getAnnotation(ServoyMobile.class) == null)
		{
			// do not show the property if the read-method is not flagged
			return false;
		}

		return super.shouldShow(propertyDescriptor);
	}

	@Override
	protected boolean hideForProperties(PropertyDescriptorWrapper propertyDescriptor)
	{
		if (propertyDescriptor.propertyDescriptor.getName().equals(StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName()) &&
			getPersist() instanceof Field && ((Field)getPersist()).getDisplayType() == Field.COMBOBOX)
		{
			return true;
		}

		if (propertyDescriptor.propertyDescriptor.getName().equals(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName()) &&
			isLabel(getPersist()))
		{
			return true;
		}

		if (propertyDescriptor.propertyDescriptor.getName().equals(StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName()) && getPersist() instanceof Form)
		{
			return true;
		}

		return super.hideForProperties(propertyDescriptor);
	}

	@Override
	protected String[] getPseudoPropertyNames(Class< ? > clazz)
	{
		if (GraphicalComponent.class == clazz && isButton(getPersist()))
		{
			// button
			return new String[] { DATA_ICON_PROPERTY };
		}

		if (GraphicalComponent.class == clazz && isLabel(getPersist()))
		{
			// script label
			return new String[] { HEADER_SIZE_PROPERTY };
		}

		if (Field.class == clazz)
		{
			Field field = (Field)getPersist();
			if (field.getDisplayType() == Field.RADIOS)
			{
				// radios check
				return new String[] { RADIO_STYLE_NAME };
			}
		}

		return null;
	}

	/**
	 * @param persist
	 * @return
	 */
	private static boolean isLabel(IPersist persist)
	{
		return !isButton(persist) && persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getDataProviderID() == null &&
			!((GraphicalComponent)persist).getDisplaysTags();
	}

	/**
	 * @param persist
	 * @return
	 */
	private static boolean isButton(IPersist persist)
	{
		return persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getOnActionMethodID() != 0 &&
			((GraphicalComponent)persist).getShowClick();
	}

	@Override
	protected IPropertyDescriptor getPropertiesPropertyDescriptor(IPropertySource propertySource, String id, String displayName, String name,
		FlattenedSolution flattenedEditingSolution, Form form) throws RepositoryException
	{
		if (name.equals(HEADER_SIZE_PROPERTY))
		{
			return MOBILE_LABEL_HEADERSIZE_CONTROLLER;
		}

		if (name.equals(RADIO_STYLE_NAME))
		{
			return MOBILE_RADIO_STYLE_CONTROLLER;
		}

		if (name.equals(DATA_ICON_PROPERTY))
		{
			return MOBILE_ICONS_CONTROLLER;
		}

		return super.getPropertiesPropertyDescriptor(propertySource, id, displayName, name, flattenedEditingSolution, form);
	}

}
