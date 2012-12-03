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

package com.servoy.eclipse.designer.util;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Dimension;

import com.servoy.j2db.util.IDelegate;

/**
 * Simple delegate layout manager.
 * 
 * @author rgansevles
 *
 */
public class DelegateLayoutManager implements LayoutManager, IDelegate<LayoutManager>
{
	private final LayoutManager layoutManager;

	public DelegateLayoutManager(LayoutManager layoutManager)
	{
		this.layoutManager = layoutManager;
	}

	public LayoutManager getDelegate()
	{
		return layoutManager;
	}

	/**
	 * @param child
	 * @return
	 * @see org.eclipse.draw2d.LayoutManager#getConstraint(org.eclipse.draw2d.IFigure)
	 */
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
	public Dimension getPreferredSize(IFigure container, int wHint, int hHint)
	{
		return layoutManager.getPreferredSize(container, wHint, hHint);
	}

	/**
	 * 
	 * @see org.eclipse.draw2d.LayoutManager#invalidate()
	 */
	public void invalidate()
	{
		layoutManager.invalidate();
	}

	/**
	 * @param container
	 * @see org.eclipse.draw2d.LayoutManager#layout(org.eclipse.draw2d.IFigure)
	 */
	public void layout(IFigure container)
	{
		layoutManager.layout(container);
	}

	/**
	 * @param child
	 * @see org.eclipse.draw2d.LayoutManager#remove(org.eclipse.draw2d.IFigure)
	 */
	public void remove(IFigure child)
	{
		layoutManager.remove(child);
	}

	/**
	 * @param child
	 * @param constraint
	 * @see org.eclipse.draw2d.LayoutManager#setConstraint(org.eclipse.draw2d.IFigure, java.lang.Object)
	 */
	public void setConstraint(IFigure child, Object constraint)
	{
		layoutManager.setConstraint(child, constraint);
	}
}
