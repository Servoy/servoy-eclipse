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
package com.servoy.eclipse.designer.property;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * Edit policy for directly editing edit parts based on a property source.
 * 
 * @see PropertyDirectEditManager
 * @author rgansevles
 * 
 */
public class PropertyDirectEditPolicy extends DirectEditPolicy
{
	private final IPropertySource propertySource;

	public PropertyDirectEditPolicy(IPropertySource propertySource)
	{
		this.propertySource = propertySource;
	}

	@Override
	protected Command getDirectEditCommand(DirectEditRequest request)
	{
		IPropertyDescriptor propertyDescriptor = (IPropertyDescriptor)request.getDirectEditFeature();
		SetValueCommand setCommand = new SetValueCommand("Direct edit");
		setCommand.setTarget(propertySource);
		setCommand.setPropertyId(propertyDescriptor.getId());
		setCommand.setPropertyValue(request.getCellEditor().getValue());
		return setCommand;
	}

	@Override
	protected void showCurrentEditValue(DirectEditRequest request)
	{
	}

}
