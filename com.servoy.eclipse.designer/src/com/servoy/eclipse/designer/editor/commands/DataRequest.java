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

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.Request;

/**
 * Typed request with a data object and location.
 * 
 * @author rob
 * 
 */
public class DataRequest extends Request
{
	private final Point location;
	private final Object data;

	/**
	 * Creates a DataRequest with the given type and data.
	 * 
	 * @param type The type of Request.
	 * @param data The data of the request.
	 */
	public DataRequest(Object type, Object data)
	{
		this(type, null, data);
	}

	/**
	 * Creates a DataRequest with the given type and data.
	 * 
	 * @param type The type of Request.
	 * @param location where the request should be executed
	 * @param data The data of the request.
	 */
	public DataRequest(Object type, Point location, Object data)
	{
		this.location = location;
		this.data = data;
		setType(type);
	}


	public Point getlocation()
	{
		return location;
	}


	public Object getData()
	{
		return data;
	}
}
