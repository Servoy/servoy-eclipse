/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.property.types;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;

/**
 * Provides properties view look & feel for custom JSON object sablo/ng property types.
 *
 * @author acostescu
 */
public class CustomObjectTypePropertyDescriptorFactory implements ITypePropertyDescriptorFactory
{

	@Override
	public IPropertyDescriptor createPropertyDescriptor(Object id, IPropertySource persistPropertySource, String displayName,
		FlattenedSolution flattenedEditingSolution, PersistContext persistContext, PropertyDescription propertyDescription)
	{
		return new CustomObjectTypePropertyController(id, displayName, persistContext, propertyDescription);
	}

}
