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

package com.servoy.eclipse.designer.editor.mobile.commands;

import org.eclipse.gef.commands.Command;

import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.j2db.persistence.AbstractBase;

/**
 * Command to set a mobile property.
 * 
 * @author rgansevles
 *
 */
public class PutCustomMobilePropertyCommand<T> extends Command
{
	private final AbstractBase element;
	private final MobileProperty<T> property;
	private final T value;
	private T oldValue;

	public PutCustomMobilePropertyCommand(AbstractBase element, MobileProperty<T> property, T value)
	{
		this.element = element;
		this.property = property;
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute()
	{
		oldValue = (T)element.putCustomMobileProperty(property.propertyName, value);
	}

	@Override
	public void undo()
	{
		element.putCustomMobileProperty(property.propertyName, oldValue);
		oldValue = null;
	}
}
