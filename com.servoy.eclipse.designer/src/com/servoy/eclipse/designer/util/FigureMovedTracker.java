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

package com.servoy.eclipse.designer.util;

import org.eclipse.draw2d.AncestorListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;

/**
 * Helper that uses the locator on a figure when its ancestor moved.
 * 
 * @author rgansevles
 *
 */
public class FigureMovedTracker implements AncestorListener
{

	private final IFigure figure;
	private final Locator locator;

	public FigureMovedTracker(IFigure figure, Locator locator)
	{
		this.figure = figure;
		this.locator = locator;
	}

	/**
	 * @return the locator
	 */
	public Locator getLocator()
	{
		return locator;
	}

	public void ancestorAdded(IFigure ancestor)
	{
	}

	public void ancestorMoved(IFigure ancestor)
	{
		getLocator().relocate(figure);
	}

	public void ancestorRemoved(IFigure ancestor)
	{
	}

}
