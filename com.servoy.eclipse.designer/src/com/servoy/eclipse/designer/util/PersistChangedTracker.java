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

import java.util.Collection;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.IPersist;

/**
 * Helper that uses the locator on a figure when its persist model has changed.
 * 
 * @author rgansevles
 *
 */
public class PersistChangedTracker implements IPersistChangeListener
{

	private final IFigure figure;
	private final Locator locator;
	private final IPersist persist;

	public PersistChangedTracker(IFigure figure, IPersist persist, Locator locator)
	{
		this.figure = figure;
		this.persist = persist;
		this.locator = locator;
		persistChanged();
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, this);
	}

	/**
	 * @return the locator
	 */
	public Locator getLocator()
	{
		return locator;
	}

	/**
	 * @return the figure
	 */
	public IFigure getFigure()
	{
		return figure;
	}

	public void unhook()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, this);
	}

	public void persistChanged()
	{
		getLocator().relocate(figure);
		figure.repaint();
	}

	public void persistChanges(Collection<IPersist> changes)
	{
		for (IPersist changed : changes)
		{
			if (persist.equals(changed))
			{
				// refresh later when the figure was updated
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						persistChanged();
					}
				});
				return;
			}
		}
	}
}
