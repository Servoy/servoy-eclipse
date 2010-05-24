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

import org.eclipse.gef.Request;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.RepositoryException;


public class FormPlaceFieldCommand extends FormPlaceElementCommand
{
	private final boolean placeAsLabels;
	private final boolean placeWithLabels;
	private final boolean placeHorizontal;
	private final boolean fillText;
	private final boolean fillName;
	private final IFieldPositioner fieldPositioner;

	/**
	 * Command to add a field.
	 * 
	 * @param parent
	 * @param location
	 * @param object
	 */
	public FormPlaceFieldCommand(Request request, ISupportChilds parent, Object object, IFieldPositioner fieldPositioner, Point defaultLocation,
		boolean placeAsLabels, boolean placeWithLabels, boolean placeHorizontal, boolean fillText, boolean fillName)
	{
		super(request, parent, object, fieldPositioner, defaultLocation);
		this.fieldPositioner = fieldPositioner;
		this.placeAsLabels = placeAsLabels;
		this.placeWithLabels = placeWithLabels;
		this.placeHorizontal = placeHorizontal;
		this.fillText = fillText;
		this.fillName = fillName;
	}

	@Override
	protected IPersist[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof ISupportFormElements)
		{
			setLabel("place field(s)");
			return ElementFactory.createFields((ISupportFormElements)parent, (Object[])object, placeAsLabels, placeWithLabels, placeHorizontal, fillText,
				fillName, fieldPositioner, location);
		}
		return null;
	}
}
