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

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.annotations.ServoyMobile;

/**
 * PersistPropertySource to filter out properties not used in mobile solutions.
 * 
 * @author rgansevles
 *
 */
public class MobilePersistPropertySource extends PersistPropertySource
{
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

		if (propertyDescriptor.propertyDescriptor.getName().equals("editable") && getPersist() instanceof Field &&
			((Field)getPersist()).getDisplayType() == Field.COMBOBOX)
		{
			return false;
		}

		return super.shouldShow(propertyDescriptor);
	}

	@Override
	protected String[] getPseudoPropertyNames(Class< ? > clazz)
	{
		return null;
	}

}
