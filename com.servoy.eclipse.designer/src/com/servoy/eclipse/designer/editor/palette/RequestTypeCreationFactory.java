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

package com.servoy.eclipse.designer.editor.palette;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.editor.VisualFormEditor.RequestType;
import com.servoy.eclipse.dnd.FormElementDragData.DataProviderDragData;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.IDragData;

/**
 * Creation factory for GEF palette.
 * Does not create new objects, just simply holds the RequestType.
 * 
 * @author rgansevles
 *
 */
public class RequestTypeCreationFactory implements CreationFactory
{
	private final RequestType requestType;
	private Object newObject;
	private Object data;
	private Dimension newObjectSize;
	private IGetSize newObjectSizeCalculator;
	private Map<String, Object> extendedData;

	public RequestTypeCreationFactory(RequestType requestType)
	{
		this(requestType, null, null);
	}

	public RequestTypeCreationFactory(RequestType requestType, IGetSize newObjectSizeCalculator)
	{
		this(requestType, null, newObjectSizeCalculator);
	}

	public RequestTypeCreationFactory(RequestType requestType, Dimension newObjectSize)
	{
		this(requestType, newObjectSize, null);
	}

	private RequestTypeCreationFactory(RequestType requestType, Dimension newObjectSize, IGetSize newObjectSizeCalculator)
	{
		this.requestType = requestType;
		this.newObjectSize = newObjectSize;
		this.newObjectSizeCalculator = newObjectSizeCalculator;
	}

	/*
	 * Call the newObjectSizeCalculator only once.
	 */
	public Dimension getNewObjectSize()
	{
		if (newObjectSize != null)
		{
			return newObjectSize;
		}
		if (newObjectSizeCalculator != null)
		{
			newObjectSize = newObjectSizeCalculator.getSize();
			newObjectSizeCalculator = null;
		}
		return newObjectSize;
	}

	public Object getNewObject()
	{
		return newObject;
	}

	public void setNewObject(Object newObject)
	{
		this.newObject = newObject;
	}

	public Object getObjectType()
	{
		return requestType;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Object data)
	{
		this.data = data;
	}

	/**
	 * @return the data
	 */
	public Object getData()
	{
		return data;
	}

	public Map<String, Object> getExtendedData()
	{
		if (extendedData == null)
		{
			extendedData = new HashMap<String, Object>();
		}
		return extendedData;
	}

	public void setExtendedData(Map<String, Object> map)
	{
		extendedData = map;
	}

	/**
	 * @param dragData
	 * @return
	 */
	public static Dimension getDefaultElementSize(IDragData dragData)
	{
		if (dragData instanceof PersistDragData)
		{
			return new Dimension(((PersistDragData)dragData).width, ((PersistDragData)dragData).height);
		}
		if (dragData instanceof DataProviderDragData)
		{
			return new Dimension(140, 20);
		}
		// some default, should not happen
		return new Dimension(100, 100);
	}

	/**
	 * Simple interface for getting the size in a lazy way.
	 * 
	 * @author rgansevles
	 *
	 */
	public interface IGetSize
	{
		Dimension getSize();
	}
}
