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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.eclipse.designer.util.DelegateLayoutManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Utils;

/**
 * Calculate the body part height after delegate layout.
 * Used for debugging mobile forms in developer web client.
 * 
 * @author rgansevles
 *
 */
public class SetHeightToBodyPartLayoutManager extends DelegateLayoutManager
{
	private final Form form;

	public SetHeightToBodyPartLayoutManager(LayoutManager layoutManager, Form form)
	{
		super(layoutManager);
		this.form = form;
	}

	@Override
	public void layout(IFigure container)
	{
		super.layout(container);

		// use created layout to adjust body part
		int max = 0;
		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			if (child instanceof MobilePartFigure)
			{
				continue;
			}

			Rectangle bounds = child.getBounds();
			max = Math.max(max, bounds.y + bounds.height);
		}

		if (max != 0)
		{
			for (Part part : Utils.iterate(form.getParts()))
			{
				if (part.getPartType() == Part.BODY)
				{
					part.setHeight(max);
				}
			}
		}
	}
}
