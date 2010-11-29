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

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.editparts.GridLayer;
import org.eclipse.gef.editparts.GuideLayer;

import com.servoy.eclipse.designer.internal.MarqueeDragTracker;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * Root pane for forms, add datarender-layer.
 * 
 * @author rgansevles
 * 
 */
public class FormGraphicalRootEditPart extends FreeformGraphicalRootEditPart
{
	/**
	 * The layer containing feedback for the selected element, this layer is below the handle layer and above the layers with the elements.
	 */
	public static final String SELECTED_ELEMENT_FEEDBACK_LAYER = "Feedback Layer for selected element"; //$NON-NLS-1$

	private final VisualFormEditor editorPart;

	protected final IPreferenceChangeListener preferenceChangeListener = new IPreferenceChangeListener()
	{

		public void preferenceChange(PreferenceChangeEvent event)
		{
			if (DesignerPreferences.isGridSetting(event.getKey()))
			{
				IFigure gridLayer = getLayer(LayerConstants.GRID_LAYER);
				if (gridLayer != null)
				{
					gridLayer.repaint();
				}
			}
		}
	};

	public FormGraphicalRootEditPart(VisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
		Activator.getDefault().getEclipsePreferences().addPreferenceChangeListener(preferenceChangeListener);
	}

	@Override
	protected void createLayers(LayeredPane layeredPane)
	{
		layeredPane.add(new FormBackgroundLayer(editorPart));
		layeredPane.add(createGridLayer(), GRID_LAYER);
		layeredPane.add(getPrintableLayers(), PRINTABLE_LAYERS);

		FreeformLayer layer = new FreeformLayer();
		layer.setEnabled(false);
		layeredPane.add(layer, SELECTED_ELEMENT_FEEDBACK_LAYER);

		layeredPane.add(new FreeformLayer(), HANDLE_LAYER);

		layer = new FreeformLayer();
		layer.setEnabled(false);
		layeredPane.add(layer, FEEDBACK_LAYER);

		layeredPane.add(new GuideLayer(), GUIDE_LAYER);
	}

	@Override
	protected GridLayer createGridLayer()
	{
		return new DottedGridLayer(editorPart);
	}

	@Override
	public DragTracker getDragTracker(Request request)
	{
		return new MarqueeDragTracker();
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter == AutoexposeHelper.class) return new CustomViewportAutoexposeHelper(this, new Insets(100), true);
		return super.getAdapter(adapter);
	}

	public VisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	@Override
	public void deactivate()
	{
		Activator.getDefault().getEclipsePreferences().removePreferenceChangeListener(preferenceChangeListener);
		super.deactivate();
	}
}
