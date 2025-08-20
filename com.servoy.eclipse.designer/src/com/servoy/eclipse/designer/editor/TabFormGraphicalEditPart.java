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
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPolicy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.util.Utils;

/**
 * Edit part for tabs in a tabpanel in form designer.
 *
 * @author rgansevles
 */

public class TabFormGraphicalEditPart extends BasePersistGraphicalEditPart
{
	protected String prevImageId = null;

	public TabFormGraphicalEditPart(IApplication application, Tab tab, boolean inherited)
	{
		super(application, tab, inherited);
	}

	@Override
	protected List getModelChildren()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(PasteToSupportChildsEditPolicy.PASTE_ROLE, new PasteToSupportChildsEditPolicy(application, getFieldPositioner()));
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new PersistEditPolicy(application, getFieldPositioner()));
	}

	@Override
	protected IFigure createFigure()
	{
		Label label = new Label();
		label.setFont(FontResource.getDefaultFont(SWT.ITALIC, 0));
		label.setBorder(new TabLikeBorder());
		label.setOpaque(true);
		label.setBackgroundColor(ColorConstants.button);
		label.setIconAlignment(PositionConstants.LEFT);
		label.setTextAlignment(PositionConstants.LEFT);
		if (isInherited()) label.setForegroundColor(ColorConstants.red);
		updateLabel((Tab)getPersist(), label);
		return label;
	}

	protected void updateLabel(Tab tab, Label label)
	{
		label.setText(tab.getText());

		if (!Utils.equalObjects(prevImageId, tab.getImageMediaID()))
		{
			prevImageId = tab.getImageMediaID();
			Image image = label.getIcon();
			if (image != null)
			{
				image.dispose();
				image = null;
			}
			if (tab.getImageMediaID() != null)
			{
				Media media = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(getPersist()).getMedia(
					tab.getImageMediaID());
				if (media != null)
				{
					image = new Image(Display.getCurrent(), new ImageData(new ByteArrayInputStream(media.getMediaData())));
				}
			}
			label.setIcon(image);
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

		Dimension preferredSize = label.getPreferredSize();
		label.setBounds(new Rectangle(x, y, Math.min(preferredSize.width + 10, 250), Math.max(preferredSize.height, 30)));

		label.setForegroundColor(ColorResource.INSTANCE.getColor(ColorResource.ColorAwt2Rgb(tab.getForeground())));
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		updateLabel((Tab)getPersist(), (Label)getFigure());
	}

	@Override
	public void deactivate()
	{
		Label label = (Label)getFigure();
		Image image = label.getIcon();
		if (image != null)
		{
			image.dispose();
			label.setIcon(null);
		}
		prevImageId = null;

		super.deactivate();
	}

}
