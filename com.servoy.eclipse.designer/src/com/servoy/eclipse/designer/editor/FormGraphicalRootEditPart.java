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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.editparts.GridLayer;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.internal.MarqueeDragTracker;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.j2db.util.Settings;

/**
 * Root pane for forms, add datarender-layer.
 * 
 * @author rgansevles
 * 
 */
public class FormGraphicalRootEditPart extends FreeformGraphicalRootEditPart implements PropertyChangeListener
{
	private final VisualFormEditor editorPart;
	private DottedGridLayer dottedGridLayer;

	public FormGraphicalRootEditPart(VisualFormEditor editorPart)
	{
		this.editorPart = editorPart;

		Settings settings = ServoyModelManager.getServoyModelManager().getServoyModel().getSettings();
		settings.addPropertyChangeListener(this, DesignerPreferences.GRID_POINTSIZE_SETTING);
		settings.addPropertyChangeListener(this, DesignerPreferences.GRID_SIZE_SETTING);
		settings.addPropertyChangeListener(this, DesignerPreferences.GRID_COLOR_SETTING);
	}

	@Override
	protected void createLayers(LayeredPane layeredPane)
	{
		layeredPane.add(new FormBackgroundLayer(editorPart));
		super.createLayers(layeredPane);
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

	/**
	 * registered properties: DesignerPreferences.GRID_*_SETTING
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		IFigure gridLayer = getLayer(LayerConstants.GRID_LAYER);
		if (gridLayer != null)
		{
			gridLayer.repaint();
		}
	}

	@Override
	public void deactivate()
	{
		Settings settings = ServoyModelManager.getServoyModelManager().getServoyModel().getSettings();
		settings.removePropertyChangeListener(this, DesignerPreferences.GRID_POINTSIZE_SETTING);
		settings.removePropertyChangeListener(this, DesignerPreferences.GRID_SIZE_SETTING);
		settings.removePropertyChangeListener(this, DesignerPreferences.GRID_COLOR_SETTING);

		super.deactivate();
	}
}
