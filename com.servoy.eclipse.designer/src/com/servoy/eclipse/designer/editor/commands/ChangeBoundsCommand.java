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
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.StaticContentSpecLoader;


/**
 * Command to apply dragging changes to the PersistPropertySource object. Children of the persist are moved along.
 * 
 * @author rgansevles
 * 
 */
public class ChangeBoundsCommand extends BaseRestorableCommand implements ISupportModels
{
	/** Objects to manipulate. */
	private final Point moveDelta;
	private final EditPart editPart;
	private final Dimension sizeDelta;

	private List<Object> models = null;

	/**
	 * Create a command that can resize and/or move ISupportBound objects.
	 * 
	 */
	public ChangeBoundsCommand(EditPart editPart, Point moveDelta, Dimension sizeDelta)
	{
		super("");
		this.editPart = editPart;
		this.moveDelta = moveDelta;
		this.sizeDelta = sizeDelta;
	}

	@Override
	public void execute()
	{
		String label;
		if (sizeDelta == null || (sizeDelta.width == 0 && sizeDelta.height == 0))
		{
			label = "move";
		}
		else
		{
			label = "resize";
		}

		String name = null;
		if (editPart.getModel() instanceof ISupportName)
		{
			name = ((ISupportName)editPart.getModel()).getName();
		}
		if (name != null)
		{
			label += " " + name;
		}
		setLabel(label);
		redo();
	}

	@Override
	public boolean canExecute()
	{
		return changeBounds(false);
	}

	@Override
	public void redo()
	{
		changeBounds(true);
	}

	public boolean changeBounds(boolean change)
	{
		List<EditPart> toApply = new ArrayList<EditPart>();
		models = new ArrayList<Object>();
		try
		{
			toApply.add(editPart);
			while (toApply.size() > 0)
			{
				EditPart ep = toApply.remove(0);
				if (!(ep instanceof GraphicalEditPart) || !changeBounds((GraphicalEditPart)ep, change))
				{
					return false;
				}
				Object model = ep.getModel();
				models.add(model);
				if ((sizeDelta == null || (sizeDelta.width == 0 && sizeDelta.height == 0)/* move, not resize */) && model instanceof ISupportChilds)
				{
					Iterator<IPersist> it = ((ISupportChilds)model).getAllObjects();
					while (it.hasNext())
					{
						IPersist child = it.next();
						EditPart childEditPart = (EditPart)editPart.getViewer().getEditPartRegistry().get(child);
						if (childEditPart != null)
						{
							toApply.add(childEditPart);
						}
					}
				}
			}
			return true;
		}
		finally
		{
			if (!change)
			{
				models = new ArrayList<Object>();
			}
		}
	}

	private static void setLocationAndSize(GraphicalEditPart ep, java.awt.Point location, java.awt.Dimension size)
	{
		IPropertySource propertySource = (IPropertySource)ep.getAdapter(IPropertySource.class);
		propertySource.setPropertyValue(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), location);
		propertySource.setPropertyValue(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), size);
	}

	protected boolean changeBounds(GraphicalEditPart ep, boolean change)
	{
		Rectangle bounds = ep.getFigure().getBounds();
		int x = bounds.x + (moveDelta == null ? 0 : moveDelta.x);
		int y = bounds.y + (moveDelta == null ? 0 : moveDelta.y);
		int width = bounds.width + (sizeDelta == null ? 0 : sizeDelta.width);
		int height = bounds.height + (sizeDelta == null ? 0 : sizeDelta.height);

		if (x < 0 || y < 0 || width < 0 || height < 0) return false;

		if (change)
		{
			saveState(ep.getModel());
			setLocationAndSize(ep, new java.awt.Point(x, y), new java.awt.Dimension(width, height));
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
