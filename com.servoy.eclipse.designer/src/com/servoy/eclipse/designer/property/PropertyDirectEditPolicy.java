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

import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Edit policy for directly editing edit parts.
 * 
 * @see PropertyDirectEditManager
 * @author rgansevles
 * 
 */
public class PropertyDirectEditPolicy extends DirectEditPolicy
{
	private final IPersist persist;
	private final Form form; // context

	/**
	 * @param persist
	 * @param form
	 */
	public PropertyDirectEditPolicy(IPersist persist, Form form)
	{
		this.persist = persist;
		this.form = form;
	}

	@Override
	protected Command getDirectEditCommand(DirectEditRequest request)
	{
		IPropertyDescriptor propertyDescriptor = (IPropertyDescriptor)request.getDirectEditFeature();
		return SetValueCommand.createSetvalueCommand("Direct edit", new PersistPropertySource(persist, form, false), (String)propertyDescriptor.getId(),
			request.getCellEditor().getValue());
	}

	@Override
	protected void showCurrentEditValue(DirectEditRequest request)
	{
	}

}
