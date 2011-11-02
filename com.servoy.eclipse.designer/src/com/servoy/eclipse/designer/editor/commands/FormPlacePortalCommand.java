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

import java.util.Map;

import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Command to place a portal in the form designer.
 * 
 * @author rgansevles
 */
public class FormPlacePortalCommand extends FormPlaceElementCommand
{
	private final boolean fillText;
	private final boolean fillName;
	private final boolean placeAsLabels;
	private final boolean placeWithLabels;

	/**
	 * Command to add a portal.
	 * 
	 * @param parent
	 * @param location
	 * @param object
	 */
	public FormPlacePortalCommand(IApplication application, ISupportFormElements parent, Object object, Object requestType,
		Map<Object, Object> objectProperties, IFieldPositioner fieldPositioner, Point defaultLocation, org.eclipse.draw2d.geometry.Dimension size,
		boolean fillText, boolean fillName, boolean placeAsLabels, boolean placeWithLabels, IPersist context)
	{
		super(application, parent, object, requestType, objectProperties, fieldPositioner, defaultLocation, size, context);
		this.fillText = fillText;
		this.fillName = fillName;
		this.placeAsLabels = placeAsLabels;
		this.placeWithLabels = placeWithLabels;
	}

	@Override
	protected IPersist[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof Form)
		{
			setLabel("place portal");
			return new IPersist[] { ElementFactory.createPortal((Form)parent, (Object[])object, fillText, fillName, placeAsLabels, placeWithLabels, location) };
		}
		return null;
	}
}
