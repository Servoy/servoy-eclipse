/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;

/**
 * SelectionEditPolicy for use in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileSelectionEditPolicy extends SelectionEditPolicy
{
	Figure selectedBox;

	@Override
	protected void hideSelection()
	{
		if (selectedBox != null)
		{
			getLayer(LayerConstants.HANDLE_LAYER).remove(selectedBox);
		}
		selectedBox = null;
	}

	@Override
	protected void showSelection()
	{
		selectedBox = new Figure();
		selectedBox.setOpaque(false);
		LineBorder border = new LineBorder(2);
		border.setColor(ColorConstants.darkBlue);
		border.setStyle(Graphics.LINE_DASH);
		selectedBox.setBorder(border);
		selectedBox.setBounds(getHostFigure().getBounds());
		getLayer(LayerConstants.HANDLE_LAYER).add(selectedBox);
	}

}
