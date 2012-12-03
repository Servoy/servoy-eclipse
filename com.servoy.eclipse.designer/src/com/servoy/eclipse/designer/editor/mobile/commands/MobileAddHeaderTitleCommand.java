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

package com.servoy.eclipse.designer.editor.mobile.commands;

import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * Command to add title to header in mobile form editor.
 *
 * @author rgansevles
 *
 */
public class MobileAddHeaderTitleCommand extends BaseFormPlaceElementCommand
{
	public MobileAddHeaderTitleCommand(IApplication application, Form form, CreateRequest request)
	{
		super(application, form, null, request.getType(), request.getExtendedData(), null, request.getLocation() == null ? null
			: request.getLocation().getSWTPoint(), null, form);
	}

	@Override
	protected Object[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof Form)
		{
			setLabel("place header text");
			GraphicalComponent label = ElementFactory.createLabel((Form)parent, "Title", null);
			label.putCustomMobileProperty("headeritem", Boolean.TRUE);
			label.putCustomMobileProperty("headerText", Boolean.TRUE);
			// for debug in developer
			label.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST);
			label.setHorizontalAlignment(ISupportTextSetup.CENTER);

			return new Object[] { label };
		}

		return null;
	}
}