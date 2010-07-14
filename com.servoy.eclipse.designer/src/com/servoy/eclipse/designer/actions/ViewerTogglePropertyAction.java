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
package com.servoy.eclipse.designer.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Base class for actions that toggle an editor setting (like snap-to-gid) in form designer.
 * 
 * @author rgansevles
 */
public abstract class ViewerTogglePropertyAction extends Action
{
	private final GraphicalViewer diagramViewer;
	private final String property;

	/**
	 * Constructor
	 * 
	 * @param diagramViewer the GraphicalViewer whose grid enablement and visibility properties are to be toggled
	 */
	public ViewerTogglePropertyAction(GraphicalViewer diagramViewer, String actionId, String text, String tooltip, ImageDescriptor imageDescriptor,
		String property)
	{
		super(text, IAction.AS_CHECK_BOX);
		this.diagramViewer = diagramViewer;
		this.property = property;
		setToolTipText(tooltip);
		setId(actionId);
		setActionDefinitionId(actionId);
		setImageDescriptor(imageDescriptor);
		setChecked(isChecked());
	}

	/**
	 * @see org.eclipse.jface.action.IAction#isChecked()
	 */
	@Override
	public boolean isChecked()
	{
		return Boolean.TRUE.equals(diagramViewer.getProperty(property));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		diagramViewer.setProperty(property, new Boolean(!isChecked()));
	}

}
