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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;


/**
 * Command to apply dragging changes to the ISupportBounds object. Children of the persist are moved along.
 * 
 * @author rgansevles
 * 
 */
public class ChangeBoundsCommand extends Command implements ISupportModels
{
	/** Objects to manipulate. */
	private final ISupportBounds supportBounds;
	private final Point moveDelta;
	private final Dimension sizeDelta;

	private List<Object> models = null;
	private boolean applyToChildren;
	private Map<ISupportBounds, java.awt.Point> undoLocation = null;
	private Map<ISupportBounds, java.awt.Dimension> undoSize = null;

	/**
	 * Create a command that can resize and/or move ISupportBound objects.
	 * 
	 */
	public ChangeBoundsCommand(ISupportBounds supportBounds, Point moveDelta, Dimension sizeDelta)
	{
		this.supportBounds = supportBounds;
		this.moveDelta = moveDelta;
		this.sizeDelta = sizeDelta;
	}

	public ChangeBoundsCommand(ISupportBounds supportBounds, Rectangle newBounds)
	{
		this.supportBounds = supportBounds;
		java.awt.Point loc = supportBounds.getLocation();
		this.moveDelta = new Point(newBounds.x - loc.x, newBounds.y - loc.y);
		java.awt.Dimension dim = supportBounds.getSize();
		this.sizeDelta = new Dimension(newBounds.width - dim.width, newBounds.height - dim.height);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
	public void execute()
	{
		applyToChildren = true;
		String label;
		if (sizeDelta.width == 0 && sizeDelta.height == 0)
		{
			label = "move";
		}
		else
		{
			label = "resize";
			applyToChildren = false;
		}
		if (supportBounds instanceof ISupportName && ((ISupportName)supportBounds).getName() != null)
		{
			label += " " + ((ISupportName)supportBounds).getName();
		}
		setLabel(label);
		redo();
	}

	@Override
	public boolean canExecute()
	{
		return changeBounds(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		changeBounds(true);
	}

	public boolean changeBounds(boolean change)
	{
		undoLocation = new HashMap<ISupportBounds, java.awt.Point>();
		undoSize = new HashMap<ISupportBounds, java.awt.Dimension>();
		List<Object> toApply = new ArrayList<Object>();
		models = new ArrayList<Object>();
		try
		{
			toApply.add(supportBounds);
			while (toApply.size() > 0)
			{
				Object o = toApply.remove(0);
				if (o instanceof ISupportBounds)
				{
					ISupportBounds sb = (ISupportBounds)o;
					undoLocation.put(sb, sb.getLocation());
					undoSize.put(sb, sb.getSize());
					if (!changeBounds(sb, moveDelta, sizeDelta, change))
					{
						return false;
					}
				}
				models.add(o);
				if (applyToChildren && o instanceof ISupportChilds)
				{
					Iterator<IPersist> it = ((ISupportChilds)o).getAllObjects();
					while (it.hasNext())
					{
						toApply.add(it.next());
					}
				}
			}
			return true;
		}
		finally
		{
			if (!change)
			{
				undoLocation = new HashMap<ISupportBounds, java.awt.Point>();
				undoSize = new HashMap<ISupportBounds, java.awt.Dimension>();
				models = new ArrayList<Object>();
			}
		}
	}

	@Override
	public boolean canUndo()
	{
		return undoLocation != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
	public void undo()
	{
		for (ISupportBounds sb : undoLocation.keySet())
		{
			setLocationAndSize(sb, undoLocation.get(sb), undoSize.get(sb));
		}
		undoLocation = null;
		undoSize = null;
	}

	private static void setLocationAndSize(ISupportBounds supportBounds, java.awt.Point location, java.awt.Dimension size)
	{
		supportBounds.setLocation(location);
		supportBounds.setSize(size);

		// fire persist change recursively
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, supportBounds, true);
	}

	private static boolean changeBounds(ISupportBounds supportBounds, Point moveDelta, Dimension sizeDelta, boolean change)
	{
		java.awt.Point loc = supportBounds.getLocation();
		int x = loc.x + moveDelta.x;
		int y = loc.y + moveDelta.y;

		java.awt.Dimension dim = supportBounds.getSize();
		int width = dim.width + sizeDelta.width;
		int height = dim.height + sizeDelta.height;

		if (x < 0 || y < 0 || width < 0 || height < 0) return false;

		if (change)
		{
			setLocationAndSize(supportBounds, new java.awt.Point(x, y), new java.awt.Dimension(width, height));
		}
		return true;
	}

	public Object[] getModels()
	{
		if (models == null)
		{
			return null;
		}
		return models.toArray();
	}

}
