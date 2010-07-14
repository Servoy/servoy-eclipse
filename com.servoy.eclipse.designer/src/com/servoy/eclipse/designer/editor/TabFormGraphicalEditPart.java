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
package com.servoy.eclipse.designer.editor;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.internal.ui.palette.editparts.RaisedBorder;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;

/**
 * Edit part for tabs in a tabpanel in form designer.
 * 
 * @author rgansevles
 */

public class TabFormGraphicalEditPart extends BasePersistGraphicalEditPart
{
	public TabFormGraphicalEditPart(IApplication application, Tab tab, boolean readOnly)
	{
		super(application, tab, readOnly);
	}

	@Override
	protected List getModelChildren()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	protected void createEditPolicies()
	{
		if (!isReadOnly())
		{
			installEditPolicy(PasteToSupportChildsEditPolicy.PASTE_ROLE, new PasteToSupportChildsEditPolicy(getFieldPositioner()));
			installEditPolicy(EditPolicy.COMPONENT_ROLE, new PersistEditPolicy(getFieldPositioner()));
		}
	}

	@Override
	protected IFigure createFigure()
	{
		Label label = new Label();
		label.setFont(FontResource.getDefaultFont(SWT.ITALIC, 0));
		label.setBorder(new RaisedBorder(2, 2, 2, 2));
		label.setOpaque(false);
		updateLabel((Tab)getPersist(), label);
		return label;
	}

	protected void updateLabel(Tab tab, Label label)
	{
		label.setText(tab.getText());
		// TODO: show tab image
		if (tab.getImageMediaID() > 0)
		{
			ServoyLog.log(IStatus.WARNING, IStatus.OK, "Image in tab not yet supported in developer", null);
		}
		int x = 0;
		int y = 0;
		Point loc = tab.getLocation();
		if (loc == null)
		{
			java.awt.Point panelLoc = ((TabPanel)tab.getParent()).getLocation();
			loc = new java.awt.Point(panelLoc == null ? x : panelLoc.x, panelLoc == null ? y : (panelLoc.y + 20));
		}
		x = loc.x > 0 ? loc.x : x;
		y = loc.y > 0 ? loc.y : y;
		tab.setLocation(new java.awt.Point(x, y));
		Dimension textDim = FigureUtilities.getStringExtents(label.getText(), label.getFont());
		label.setBounds(new Rectangle(x, y, Math.min(textDim.width + 4, 250), Math.min(textDim.height + 4, 40)));
	}

	/**
	 * Form in tab panel has no children
	 */
	@Override
	protected EditPart createChild(Object child)
	{
		return null;
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		updateLabel((Tab)getPersist(), (Label)getFigure());
	}

}
