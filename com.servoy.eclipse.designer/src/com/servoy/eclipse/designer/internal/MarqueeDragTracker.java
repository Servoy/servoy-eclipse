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
package com.servoy.eclipse.designer.internal;

import java.util.Collections;

import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.DragTracker;
import org.eclipse.swt.widgets.Display;

/**
 * A marqueeSelectionTool that implements the DragTracker interface.
 */
public class MarqueeDragTracker extends MarqueeSelectionTool implements DragTracker
{
	protected AutoexposeHelper exposeHelper;
	protected PrecisionPoint sourceRelativeStartPoint;

	/**
	 * Called when the mouse button is released. Overridden to do nothing, since a drag tracker does not need to unload when finished.
	 */
	@Override
	protected void handleFinished()
	{
	}

	@Override
	protected boolean handleHover()
	{
		if (isInState(STATE_DRAG_IN_PROGRESS | STATE_ACCESSIBLE_DRAG_IN_PROGRESS))
		{
			updateAutoexposeHelper();
		}
		return true;
	}

	protected void updateAutoexposeHelper()
	{
		if (exposeHelper != null)
		{
			return;
		}
		AutoexposeHelper.Search search;
		search = new AutoexposeHelper.Search(getLocation());
		getCurrentViewer().findObjectAtExcluding(getLocation(), Collections.EMPTY_LIST, search);
		setAutoexposeHelper(search.result);
	}

	/**
	 * Sets the active autoexpose helper to the given helper, or <code>null</code>. If the helper is not <code>null</code>, a runnable is queued on the
	 * event thread that will trigger a subsequent {@link #doAutoexpose()}. The helper is typically updated only on a hover event.
	 * 
	 * @param helper the new autoexpose helper or <code>null</code>
	 */
	protected void setAutoexposeHelper(AutoexposeHelper helper)
	{
		exposeHelper = helper;
		if (exposeHelper != null)
		{
			if (sourceRelativeStartPoint == null && isInState(STATE_DRAG_IN_PROGRESS | STATE_ACCESSIBLE_DRAG_IN_PROGRESS))
			{
				sourceRelativeStartPoint = new PrecisionPoint(getStartLocation());
				getMarqueeFeedbackFigure().translateToRelative(sourceRelativeStartPoint);
			}

			Display.getCurrent().asyncExec(new QueuedAutoexpose());
		}
	}

	class QueuedAutoexpose implements Runnable
	{
		public void run()
		{
			if (exposeHelper != null)
			{
				if (exposeHelper.step(getLocation()))
				{
					handleAutoexpose();
					Display.getCurrent().asyncExec(new QueuedAutoexpose());
				}
				else
				{
					setAutoexposeHelper(null);
				}
			}
		}
	}

	@Override
	public void deactivate()
	{
		setAutoexposeHelper(null);
		sourceRelativeStartPoint = null;
		super.deactivate();
	}

	public void handleAutoexpose()
	{
		repairStartLocation();
	}

	/**
	 * If auto scroll (also called auto expose) is being performed, the start location moves during the scroll. This method updates that location.
	 */
	protected void repairStartLocation()
	{
		if (sourceRelativeStartPoint != null)
		{
			PrecisionPoint newStart = (PrecisionPoint)sourceRelativeStartPoint.getCopy();
			getMarqueeFeedbackFigure().translateToAbsolute(newStart);
			setStartLocation(newStart);
		}
	}
}
