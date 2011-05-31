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
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.part.WorkbenchPart;

/**
 * Base class for actions that toggle an editor setting (like snap-to-gid) in form designer.
 * 
 * @author rgansevles
 */
public class ViewerTogglePropertyAction extends SelectionAction
{
	private final GraphicalViewer diagramViewer;
	private final String property;

	public ViewerTogglePropertyAction(WorkbenchPart workbenchPart, GraphicalViewer diagramViewer, String text, String property)
	{
		this(workbenchPart, diagramViewer, null, text, null, null, property);
	}

	/**
	 * Constructor
	 * 
	 * @param diagramViewer the GraphicalViewer whose properties are to be toggled
	 */
	public ViewerTogglePropertyAction(WorkbenchPart workbenchPart, GraphicalViewer diagramViewer, String actionId, String text, String tooltip,
		ImageDescriptor imageDescriptor, String property)
	{
		super(workbenchPart, IAction.AS_CHECK_BOX);
		this.diagramViewer = diagramViewer;
		this.property = property;
		setText(text);
		setToolTipText(tooltip);
		setId(actionId);
		setImageDescriptor(imageDescriptor);
		setChecked(calculateChecked());
	}

	@Override
	protected boolean calculateEnabled()
	{
		return true;
	}


	public boolean calculateChecked()
	{
		return Boolean.TRUE.equals(diagramViewer.getProperty(property));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		boolean newValue = !calculateChecked();
		diagramViewer.setProperty(property, Boolean.valueOf(newValue));
		setChecked(newValue);
	}
}
