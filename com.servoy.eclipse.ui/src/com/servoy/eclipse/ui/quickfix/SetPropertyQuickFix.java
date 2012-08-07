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
package com.servoy.eclipse.ui.quickfix;

import org.eclipse.ui.views.properties.IPropertySource;


/**
 * Quickfix for properties by assigning a value.
 * 
 * @author rgansevles
 *
 */
public class SetPropertyQuickFix extends BaseSetPropertyQuickFix
{
	protected final Object value;

	public SetPropertyQuickFix(String solutionName, String uuid, String propertyName, String displayName, Object value)
	{
		super(solutionName, uuid, propertyName, displayName);
		this.value = value;
	}

	@Override
	protected void setPropertyValue(IPropertySource propertySource)
	{
		propertySource.setPropertyValue(getPropertyName(), value);
	}
}