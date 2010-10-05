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
import org.eclipse.gef.SnapToGrid;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Menu;

import com.servoy.eclipse.designer.editor.SnapToElementAlignment;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;

/**
 * Action for selecting snapmode in form designer toolbar.
 * 
 * @author rgansevles
 */

public class SelectSnapmodeAction extends ViewerDropdownPropertyAction
{
	/**
	 * Constructor
	 * 
	 * @param diagramViewer the GraphicalViewer whose grid enablement and visibility properties are to be toggled
	 */
	public SelectSnapmodeAction(GraphicalViewer diagramViewer)
	{
		super(diagramViewer, DesignerActionFactory.SELECT_SNAPMODE.getId(), DesignerActionFactory.SELECT_SNAPMODE_TEXT,
			DesignerActionFactory.SELECT_SNAPMODE_TOOLTIP, DesignerActionFactory.SELECT_SNAPTMODE_IMAGE);
	}

	protected void setSnapMode(boolean grid, boolean alignment)
	{
		diagramViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(grid));
		diagramViewer.setProperty(SnapToElementAlignment.PROPERTY_ALIGNMENT_ENABLED, Boolean.valueOf(alignment));
	}

	@Override
	protected void fillMenu(Menu menu)
	{
		add(new Action("None", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				setSnapMode(false, false);
			}

			@Override
			public boolean isChecked()
			{
				return !Boolean.TRUE.equals(diagramViewer.getProperty(SnapToGrid.PROPERTY_GRID_ENABLED)) &&
					!Boolean.TRUE.equals(diagramViewer.getProperty(SnapToElementAlignment.PROPERTY_ALIGNMENT_ENABLED));
			}
		});
		add(new Action("Grid", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				setSnapMode(true, false);
			}

			@Override
			public boolean isChecked()
			{
				return Boolean.TRUE.equals(diagramViewer.getProperty(SnapToGrid.PROPERTY_GRID_ENABLED));
			}
		});
		add(new Action("Alignment", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				setSnapMode(false, true);
			}

			@Override
			public boolean isChecked()
			{
				return Boolean.TRUE.equals(diagramViewer.getProperty(SnapToElementAlignment.PROPERTY_ALIGNMENT_ENABLED));
			}
		});
	}
}
