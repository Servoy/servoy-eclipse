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
package com.servoy.eclipse.designer.editor.mobile.editparts;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.MarginBorder;

import com.servoy.eclipse.designer.editor.BaseFormGraphicalRootEditPart;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.FormBorderGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.MobileFormBackgroundLayer;

/**
 * Root pane for forms, add datarender-layer.
 * 
 * @author rgansevles
 * 
 */
public class MobileFormGraphicalRootEditPart extends BaseFormGraphicalRootEditPart
{
	/**
	 * The layer containing feedback for the selected element, this layer is below the handle layer and above the layers with the elements.
	 */
	public static final String SELECTED_ELEMENT_FEEDBACK_LAYER = "Feedback Layer for selected element";

	/**
	 * The layer that prints the form background.
	 */
	public static final String FORM_BACKGROUND_LAYER = "Layer for form background";

	public MobileFormGraphicalRootEditPart(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	protected void createLayers(LayeredPane layeredPane)
	{
		layeredPane.add(new MobileFormBackgroundLayer(getEditorPart()), FORM_BACKGROUND_LAYER);
		layeredPane.add(getPrintableLayers(), PRINTABLE_LAYERS);

		FreeformLayer layer = new FreeformLayer();
		layer.setEnabled(false);
		layeredPane.add(layer, SELECTED_ELEMENT_FEEDBACK_LAYER);

		layeredPane.add(new FreeformLayer(), HANDLE_LAYER);

		layer = new FreeformLayer();
		layer.setEnabled(false);
		layeredPane.add(layer, FEEDBACK_LAYER);

		layeredPane.setBorder(new MarginBorder(FormBorderGraphicalEditPart.BORDER_MARGIN)); // Add some space for the border
	}

}
