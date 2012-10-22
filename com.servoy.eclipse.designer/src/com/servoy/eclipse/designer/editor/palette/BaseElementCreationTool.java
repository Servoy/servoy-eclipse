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

package com.servoy.eclipse.designer.editor.palette;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.swt.SWT;

/**
 * Tool for creating elements from the palette in the form editor.
 * 
 * @author rgansevles
 *
 */
public class BaseElementCreationTool extends CreationTool
{
	protected IFigure getFeedbackLayer()
	{
		EditPart targetEditPart = getTargetEditPart();
		if (targetEditPart == null || targetEditPart.getViewer() == null)
		{
			return null;
		}
		return LayerManager.Helper.find(targetEditPart).getLayer(LayerConstants.FEEDBACK_LAYER);
	}

	@Override
	protected void handleFinished()
	{
		if (getCurrentInput().isModKeyDown(SWT.MOD1))
		{
			reactivate();
		}
		else
		{
			super.handleFinished();
		}
	}
}
