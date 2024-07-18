/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.commands;

import java.awt.Point;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to reorder elements in mobile form content
 *
 * @author rgansevles
 *
 */
public class ReorderContentElementsCommand extends BaseRestorableCommand
{
	private final GraphicalEditPart editPart;
	private final Rectangle rectangle;

	/**
	 * @param childEditPart
	 * @param constraint
	 */
	public ReorderContentElementsCommand(GraphicalEditPart editPart, Rectangle rectangle)
	{
		super("Reorder elements");
		this.editPart = editPart;
		this.rectangle = rectangle;
	}

	@Override
	public void execute()
	{
		// save state of all other edit parts, reordering may update model of other editparts via SetBoundsToSupportBoundsFigureListener
		List< ? extends EditPart> children = editPart.getParent().getChildren();
		for (EditPart ep : children)
		{
			saveState(ep.getModel());
		}

		Display.getDefault().asyncExec(new Runnable()
		{
			// set property later because an invalidate of figure causes location via layout manager to be reset to previous
			public void run()
			{
				editPart.getAdapter(IPropertySource.class).setPropertyValue(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(),
					new Point(editPart.getFigure().getBounds().x, rectangle.y));
			}
		});
	}
}
