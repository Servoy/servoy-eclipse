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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gef.editparts.GridLayer;
import org.eclipse.gef.editparts.GuideLayer;

import com.servoy.eclipse.designer.internal.MarqueeDragTracker;

/**
 * Root pane for forms, add datarender-layer.
 * 
 * @author rgansevles
 * 
 */
public class FormGraphicalRootEditPart extends BaseFormGraphicalRootEditPart
{
	/**
	 * The layer containing feedback for the selected element, this layer is below the handle layer and above the layers with the elements.
	 */
	public static final String SELECTED_ELEMENT_FEEDBACK_LAYER = "Feedback Layer for selected element"; //$NON-NLS-1$

	/**
	 * The layer that prints the form background.
	 */
	public static final String FORM_BACKGROUND_LAYER = "Layer for form background"; //$NON-NLS-1$

	private final PropertyChangeListener viewerPropertyChangeListener = new PropertyChangeListener()
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			String property = evt.getPropertyName();
			if (FormBackgroundLayer.PROPERTY_PAINT_PAGEBREAKS.equals(property))
			{
				getLayer(FORM_BACKGROUND_LAYER).repaint();
			}
		}
	};

	public FormGraphicalRootEditPart(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	protected void createLayers(LayeredPane layeredPane)
	{
		layeredPane.add(new FormBackgroundLayer(getEditorPart()), FORM_BACKGROUND_LAYER);
		layeredPane.add(getPrintableLayers(), PRINTABLE_LAYERS);
		layeredPane.add(createGridLayer(), GRID_LAYER);

		FreeformLayer layer = new FreeformLayer();
		layer.setEnabled(false);
		layeredPane.add(layer, SELECTED_ELEMENT_FEEDBACK_LAYER);

		layeredPane.add(new FreeformLayer(), HANDLE_LAYER);

		layer = new FreeformLayer();
		layer.setEnabled(false);
		layeredPane.add(layer, FEEDBACK_LAYER);

		layeredPane.add(new GuideLayer(), GUIDE_LAYER);

		layeredPane.setBorder(new MarginBorder(FormBorderGraphicalEditPart.BORDER_MARGIN)); // Add some space for the border
	}

	@Override
	protected GridLayer createGridLayer()
	{
		return new DottedGridLayer(getEditorPart());
	}

	@Override
	public DragTracker getDragTracker(Request request)
	{
		return new MarqueeDragTracker();
	}

	/**
	 * @see org.eclipse.gef.editparts.AbstractEditPart#register()
	 */
	@Override
	protected void register()
	{
		super.register();
		getViewer().addPropertyChangeListener(viewerPropertyChangeListener);
	}

	/**
	 * @see AbstractEditPart#unregister()
	 */
	@Override
	protected void unregister()
	{
		getViewer().removePropertyChangeListener(viewerPropertyChangeListener);
		super.unregister();
	}

}
