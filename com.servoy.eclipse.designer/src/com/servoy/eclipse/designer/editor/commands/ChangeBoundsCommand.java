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
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;


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
			if (!(editPart instanceof GraphicalEditPart) || !changeBounds((GraphicalEditPart)editPart, change, true))// allow resize to the component it'self
			{
				return false;
			}
			models.add(editPart.getModel());
			addChildren(toApply, editPart.getModel());
			while (toApply.size() > 0)
			{
				EditPart ep = toApply.remove(0);
				if (!(ep instanceof GraphicalEditPart) || !changeBounds((GraphicalEditPart)ep, change, false)) // don't allow resize to the component's children
				{
					return false;
				}
				Object model = ep.getModel();
				models.add(model);
				addChildren(toApply, model);
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

	/**
	 * Adds corresponding EditPart children of model <b>parrentModel</b> to the list <b>childrenList</b>.
	 * 
	 * @param childrenList
	 * @param parentModel
	 */
	public void addChildren(List<EditPart> childrenList, Object parentModel)
	{
		if (parentModel instanceof ISupportChilds)
		{
			Iterator<IPersist> it = ((ISupportChilds)parentModel).getAllObjects();
			while (it.hasNext())
			{
				IPersist child = it.next();
				EditPart childEditPart = (EditPart)editPart.getViewer().getEditPartRegistry().get(child);
				if (childEditPart != null)
				{
					childrenList.add(childEditPart);
				}
			}
		}
	}

	protected <T> void setBoundsProperty(GraphicalEditPart ep, IPropertySource propertySource, TypedProperty<T> property, T value)
	{
		if (propertySource instanceof IModelSavePropertySource)
		{
			// supports saving of state, set property and check if model changed (first change in inherited object)
			setPropertyValue((IModelSavePropertySource)propertySource, property.getPropertyName(), value);
		}
		else
		{
			saveState(ep.getModel());
			propertySource.setPropertyValue(property.getPropertyName(), value);
		}
	}

	protected boolean changeBounds(GraphicalEditPart ep, boolean change, boolean allowResize)
	{
		ISupportBounds model = (ISupportBounds)ep.getModel();
		java.awt.Point location = model.getLocation();
		java.awt.Dimension size = model.getSize();

		boolean moved = false;
		int x = location.x;
		if (moveDelta != null && moveDelta.x != 0)
		{
			x += moveDelta.x;
			moved = true;
		}
		int y = location.y;
		if (moveDelta != null && moveDelta.y != 0)
		{
			y += moveDelta.y;
			moved = true;
		}
		boolean resized = false;
		int width = size.width;
		if (sizeDelta != null && sizeDelta.width != 0)
		{
			width += sizeDelta.width;
			resized = true;
		}
		int height = size.height;
		if (sizeDelta != null && sizeDelta.height != 0)
		{
			height += sizeDelta.height;
			resized = true;
		}

		if (x < 0 || y < 0) return false;

		if (change)
		{
			IPropertySource propertySource = (IPropertySource)ep.getAdapter(IPropertySource.class);
			if (moved)
			{
				setBoundsProperty(ep, propertySource, StaticContentSpecLoader.PROPERTY_LOCATION, new java.awt.Point(x, y));
			}
			if (resized && allowResize)
			{
				setBoundsProperty(ep, propertySource, StaticContentSpecLoader.PROPERTY_SIZE, new java.awt.Dimension(width, height));
			}
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
