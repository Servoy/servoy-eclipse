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
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

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
public class SetHeightToBodyPartXYLayoutManager extends XYLayout implements LayoutManager
{
	private final XYLayout layoutManager;
	protected final Form form;

	public SetHeightToBodyPartXYLayoutManager(XYLayout layoutManager, Form form)
	{
		this.layoutManager = layoutManager;
		this.form = form;
	}

	/**
	 * @param child
	 * @return
	 * @see org.eclipse.draw2d.LayoutManager#getConstraint(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public Object getConstraint(IFigure child)
	{
		return layoutManager.getConstraint(child);
	}

	/**
	 * @param container
	 * @param wHint
	 * @param hHint
	 * @return
	 * @see org.eclipse.draw2d.LayoutManager#getMinimumSize(org.eclipse.draw2d.IFigure, int, int)
	 */
	@Override
	public Dimension getMinimumSize(IFigure container, int wHint, int hHint)
	{
		return layoutManager.getMinimumSize(container, wHint, hHint);
	}

	/**
	 * @param container
	 * @param wHint
	 * @param hHint
	 * @return
	 * @see org.eclipse.draw2d.LayoutManager#getPreferredSize(org.eclipse.draw2d.IFigure, int, int)
	 */
	@Override
	public Dimension getPreferredSize(IFigure container, int wHint, int hHint)
	{
		return layoutManager.getPreferredSize(container, wHint, hHint);
	}

	/**
	 * 
	 * @see org.eclipse.draw2d.LayoutManager#invalidate()
	 */
	@Override
	public void invalidate()
	{
		layoutManager.invalidate();
	}

	/**
	 * @param child
	 * @see org.eclipse.draw2d.LayoutManager#remove(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public void remove(IFigure child)
	{
		layoutManager.remove(child);
	}

	/**
	 * @param child
	 * @param constraint
	 * @see org.eclipse.draw2d.LayoutManager#setConstraint(org.eclipse.draw2d.IFigure, java.lang.Object)
	 */
	@Override
	public void setConstraint(IFigure child, Object constraint)
	{
		layoutManager.setConstraint(child, constraint);
	}

	@Override
	public void layout(IFigure container)
	{
		layoutManager.layout(container);

		// use created layout to adjust body part
		int max = 0;
		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			Rectangle bounds = child.getBounds();
			if (child instanceof MobilePartFigure)
			{
				if (((MobilePartFigure)child).getPartType() == Part.FOOTER)
				{
					max = bounds.y;
					break; // align to footer
				}
				continue;
			}

			// if there is no footer align to lowest element
			max = Math.max(max, bounds.y + bounds.height);
		}

		if (max != 0)
		{
			for (Part part : Utils.iterate(form.getParts()))
			{
				if (part.getPartType() == Part.BODY)
				{
					part.setHeight(max);
					return;
				}
			}
		}
	}

	@Override
	public Point getOrigin(IFigure parent)
	{
		return layoutManager.getOrigin(parent);
	}

	@Override
	public boolean isObservingVisibility()
	{
		return layoutManager.isObservingVisibility();
	}

	@Override
	public void setObserveVisibility(boolean newValue)
	{
		layoutManager.setObserveVisibility(newValue);
	}
}
