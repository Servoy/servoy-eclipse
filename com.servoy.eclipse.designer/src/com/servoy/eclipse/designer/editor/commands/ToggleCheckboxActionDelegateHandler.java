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

import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IAnchorConstants;

/**
 * Base action class to toggle change a boolean property of selected objects.
 *
 * @author rgansevles
 */
public abstract class ToggleCheckboxActionDelegateHandler extends SetPropertyActionDelegateHandler
{
	private final int flag;

	public ToggleCheckboxActionDelegateHandler(String propertyId, String name, int flag)
	{
		super(propertyId, name, null);
		this.flag = flag;
	}

	@Override
	protected Boolean calculateChecked()
	{
		for (Object sel : getSelection().toArray())
		{
			Object persist = null;
			if (sel instanceof EditPart)
			{
				persist = ((EditPart)sel).getModel();
			}
			if (sel instanceof PersistContext)
			{
				persist = ((PersistContext)sel).getPersist();
			}
			if (persist instanceof BaseComponent)
			{
				int anchors = ((BaseComponent)persist).getAnchors();
				if (anchors == 0) anchors = IAnchorConstants.DEFAULT;
				return (anchors & flag) == 0 ? Boolean.FALSE : Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	@Override
	protected Map<EditPart, Request> createSetPropertyRequests(List<EditPart> objects, Object newval)
	{
		// toggle current checked value
		return super.createSetPropertyRequests(objects, Boolean.valueOf(!calculateChecked().booleanValue()));
	}
}
