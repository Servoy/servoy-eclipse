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

import com.servoy.eclipse.designer.editor.AlignmentfeedbackEditPolicy;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;

/**
 * Action for selecting feedback mode in form designer toolbar.
 * 
 * @author rgansevles
 */

public class SelectFeedbackmodeAction extends ViewerDropdownPropertyAction
{
	/**
	 * Constructor
	 * 
	 * @param diagramViewer the GraphicalViewer whose feedback mode is to be toggled
	 */
	public SelectFeedbackmodeAction(GraphicalViewer diagramViewer)
	{
		super(diagramViewer, DesignerActionFactory.SELECT_FEEDBACK.getId(), DesignerActionFactory.SELECT_FEEDBACK_TEXT,
			DesignerActionFactory.SELECT_FEEDBACK_TOOLTIP, DesignerActionFactory.SELECT_FEEDBACK_IMAGE);
	}

	protected void setFeedbackMode(boolean grid, boolean alignment)
	{
		diagramViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(grid));
		diagramViewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE, Boolean.valueOf(alignment));
	}

	@Override
	protected void fillMenu(Menu menu)
	{
		add(new Action("None", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				setFeedbackMode(false, false);
			}

			@Override
			public boolean isChecked()
			{
				return !Boolean.TRUE.equals(diagramViewer.getProperty(SnapToGrid.PROPERTY_GRID_VISIBLE)) &&
					!Boolean.TRUE.equals(diagramViewer.getProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE));
			}
		});
		add(new Action("Grid", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				setFeedbackMode(true, false);
			}

			@Override
			public boolean isChecked()
			{
				return Boolean.TRUE.equals(diagramViewer.getProperty(SnapToGrid.PROPERTY_GRID_VISIBLE));
			}
		});
		add(new Action("Alignment", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				setFeedbackMode(false, true);
			}

			@Override
			public boolean isChecked()
			{
				return Boolean.TRUE.equals(diagramViewer.getProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE));
			}
		});
	}
}
