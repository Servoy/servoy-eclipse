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
package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.editor.CreateElementRequest;

/**
 * Request for drag-and-drop.
 * 
 * @author rgansevles
 * 
 */
public class CreateDropRequest extends CreateElementRequest
{
	private final Object[] data;

	/**
	 * Creates a DataRequest with the given type, factory and data.
	 * 
	 * @param type The type of Request.
	 * @param factory creation factory
	 * @param data The data of the request.
	 */
	public CreateDropRequest(Object type, CreationFactory factory, Object[] data)
	{
		super(factory);
		setType(type);
		this.data = data;
	}

	public Object[] getData()
	{
		return data;
	}
}
