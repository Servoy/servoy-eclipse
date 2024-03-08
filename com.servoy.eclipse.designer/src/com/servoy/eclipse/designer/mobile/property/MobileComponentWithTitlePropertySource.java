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

import java.awt.Point;
import java.util.Arrays;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;
import com.servoy.eclipse.ui.property.IRAGTEST;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.RetargetingPropertySource;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Utils;

/**
 * Property source for a group that combines a text label with a component.
 *
 * @author rgansevles
 *
 */
public class MobileComponentWithTitlePropertySource extends RetargetingPropertySource implements IModelSavePropertySource
{
	/**
	 * @param group
	 * @param context
	 */
	public MobileComponentWithTitlePropertySource(FormElementGroup group, Form context)
	{
		super(group, context);
	}

	public Object getSaveModel()
	{
		return getModel();
	}

	@Override
	protected FormElementGroup getModel()
	{
		return (FormElementGroup)super.getModel();
	}

	@Override
	protected void fillPropertyDescriptors()
	{
		// determine title and component
		IFormElement title = null;
		IFormElement component = null;
		for (IFormElement element : Utils.iterate(getModel().getElements()))
		{
			if (element instanceof AbstractBase && ((AbstractBase)element).getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName) != null)
			{
				title = element;
			}
			else
			{
				component = element;
			}
		}

		// if no element has title property, assume first element is title, second is component
		if (title == null)
		{
			component = null;
			IFormElement[] asArray = Utils.asArray(getModel().getElements(), IFormElement.class);
			Arrays.sort(asArray, PositionComparator.XY_PERSIST_COMPARATOR);
			if (asArray.length > 0) title = asArray[0];
			if (asArray.length > 1) component = asArray[1];
			if (title instanceof AbstractBase)
			{
				// tag title element for next time
				((AbstractBase)title).putCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName, Boolean.TRUE);
			}
		}

		String prefix;
		IRAGTEST titlePropertySource = null, elementPropertySource;
		if (title != null)
		{
			// show just the properties that are used for the title in the mobile client
			elementPropertySources.put(prefix = "title", titlePropertySource = PersistPropertySource.createPersistPropertySource(title, getContext(), false));
			addMethodPropertyDescriptor(titlePropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), "titleDataProvider");
			addMethodPropertyDescriptor(titlePropertySource, prefix, StaticContentSpecLoader.PROPERTY_DISPLAYSTAGS.getPropertyName(), "titleDisplaysTags");
			addMethodPropertyDescriptor(titlePropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "titleText");
			// not supported yet in mobile client // addMethodPropertyDescriptor(titlePropertySource, prefix, IMobileProperties.HEADER_SIZE.propertyName);
		}

		if (component != null)
		{
			// show all properties for the component
			elementPropertySources.put(prefix = null,
				elementPropertySource = PersistPropertySource.createPersistPropertySource(component, getContext(), false));
			String propertyName;
			for (IPropertyDescriptor desc : elementPropertySource.getPropertyDescriptors())
			{
				propertyName = desc.getId().toString();
				// titleText should be skipped for fields
				if (component.getTypeID() == IRepository.FIELDS && StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName().equals(propertyName) &&
					titlePropertySource != null)
				{
					continue;
				}

				String displayName = null;
				// headersize should be labelsize for component
				if (IMobileProperties.HEADER_SIZE.propertyName.equals(propertyName) && titlePropertySource != null)
				{
					displayName = "labelSize";
				}

				addMethodPropertyDescriptor(elementPropertySource, prefix, propertyName, displayName);
			}

			// if label, add support to hide the titleText
			if (titlePropertySource != null && elementPropertySource.getPersist().getTypeID() == IRepository.GRAPHICALCOMPONENTS) addMethodPropertyDescriptor(
				titlePropertySource, "title", StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName(), "titleVisible");
		}
	}

	@Override
	public String toString()
	{
		return "Component with title";
	}

	@Override
	public void setPropertyValue(Object id, Object value)
	{
		if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(id))
		{
			// move title location as well, relative to component move
			Object oldValue = getPropertyValue(id);
			Point oldLoc = null;
			if (oldValue instanceof ComplexProperty< ? >)
			{
				oldLoc = (Point)((ComplexProperty)oldValue).getValue();
			}

			super.setPropertyValue(id, value);

			Object titeValue = getPropertyValue("title.location");
			Point oldTitleLoc = null;
			if (titeValue instanceof ComplexProperty< ? >)
			{
				oldTitleLoc = (Point)((ComplexProperty)titeValue).getValue();
			}
			Point newLoc;
			if (value instanceof ComplexProperty)
			{
				newLoc = (Point)((ComplexProperty)value).getValue();
			}
			else
			{
				newLoc = (Point)value;
			}
			super.setPropertyValue("title.location", oldTitleLoc == null || oldLoc == null || newLoc == null ? value : new Point(oldTitleLoc.x + newLoc.x -
				oldLoc.x, oldTitleLoc.y + newLoc.y - oldLoc.y));
		}
		else
		{
			super.setPropertyValue(id, value);
		}
	}
}
