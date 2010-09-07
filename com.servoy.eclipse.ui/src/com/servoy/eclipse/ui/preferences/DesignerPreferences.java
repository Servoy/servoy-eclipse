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
package com.servoy.eclipse.ui.preferences;

import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.ui.property.ColorPropertyController;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Preferences holder for designer settings.
 * 
 * @author rgansevles
 * 
 */
public class DesignerPreferences
{

	public static final int PX = 0;
	public static final int CM = 1;
	public static final int IN = 2;

	public static final String METRICS_SETTING = "designer.preferdMetrics";
	public static final String STEP_SIZE_SETTING = "designer.stepSize";
	public static final String COPY_PASTE_OFFSET_SETTING = "copyPasteOffset";
	public static final String GUIDE_SIZE_SETTING = "guidesize";
	public static final String GRID_COLOR_SETTING = "gridcolor";
	public static final String GRID_SIZE_SETTING = "gridsize";
	public static final String GRID_POINTSIZE_SETTING = "pointsize";
	public static final String GRID_SHOW_SETTING = "showGrid";
	public static final String GRID_SNAPTO_SETTING = "snapToGrid";
	public static final String SAVE_EDITOR_STATE_SETTING = "saveEditorState";

	public static final int COPY_PASTE_OFFSET_DEFAULT = 10;
	public static final int STEP_SIZE_DEFAULT = 10;
	public static final int LARGE_STEP_SIZE_DEFAULT = 20;
	public static final int GUIDE_SIZE_DEFAULT = 10;
	public static final int GRID_SIZE_DEFAULT = 10;
	public static final String GRID_COLOR_DEFAULT = "#b4b4b4";
	public static final int GRID_POINTSIZE_DEFAULT = 2;
	public static final boolean GRID_SHOW_DEFAULT = true;
	public static final boolean GRID_SNAPTO_DEFAULT = true;
	public static final boolean SAVE_EDITOR_STATE_DEFAULT = true;
	public static final int METRICS_DEFAULT = PX;

	private final Settings settings;

	public DesignerPreferences(Settings settings)
	{
		this.settings = settings;
	}

	public int getMetrics()
	{
		return Utils.getAsInteger(settings.getProperty(METRICS_SETTING, String.valueOf(METRICS_DEFAULT)));
	}

	public void setMetrics(int metrics)
	{
		settings.setProperty(METRICS_SETTING, String.valueOf(metrics));
	}

	public int getStepSize()
	{
		return PersistHelper.createDimension(settings.getProperty(STEP_SIZE_SETTING, STEP_SIZE_DEFAULT + ",0")).width;
	}

	public int getLargeStepSize()
	{
		int largeStepSize = PersistHelper.createDimension(settings.getProperty(STEP_SIZE_SETTING, "0," + LARGE_STEP_SIZE_DEFAULT)).height;
		if (largeStepSize == getStepSize())
		{
			// old prefs stored 2 same numbers
			return 2 * getStepSize();
		}
		return largeStepSize;
	}

	public void setStepSize(int stepSize, int largeStepSize)
	{
		settings.setProperty(STEP_SIZE_SETTING, stepSize + "," + largeStepSize);
	}

	public int getCopyPasteOffset()
	{
		return Utils.getAsInteger(settings.getProperty(COPY_PASTE_OFFSET_SETTING, String.valueOf(COPY_PASTE_OFFSET_DEFAULT)));
	}

	public void setCopyPasteOffset(int copyPasteOffset)
	{
		settings.setProperty(COPY_PASTE_OFFSET_SETTING, String.valueOf(copyPasteOffset));
	}

	public int getGuideSize()
	{
		return Utils.getAsInteger(settings.getProperty(GUIDE_SIZE_SETTING, String.valueOf(GUIDE_SIZE_DEFAULT)));
	}

	public void setGuideSize(int guideSize)
	{
		settings.setProperty(GUIDE_SIZE_SETTING, String.valueOf(guideSize));
	}

	public RGB getGridColor()
	{
		return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty("gridColor",
			PersistHelper.createColor(settings.getProperty(GRID_COLOR_SETTING, GRID_COLOR_DEFAULT)));
	}

	public void setGridColor(RGB rgb)
	{
		settings.setProperty(GRID_COLOR_SETTING,
			PersistHelper.createColorString(ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue("gridColor", rgb)));
	}

	public int getGridSize()
	{
		return Utils.getAsInteger(settings.getProperty(GRID_SIZE_SETTING, String.valueOf(GRID_SIZE_DEFAULT)));
	}

	public void setGridSize(int gridSize)
	{
		settings.setProperty(GRID_SIZE_SETTING, String.valueOf(gridSize));
	}

	public int getGridPointSize()
	{
		return Utils.getAsInteger(settings.getProperty(GRID_POINTSIZE_SETTING, String.valueOf(GRID_POINTSIZE_DEFAULT)));
	}

	public void setGridPointSize(int gridPointSize)
	{
		settings.setProperty(GRID_POINTSIZE_SETTING, String.valueOf(gridPointSize));
	}

	public boolean getGridShow()
	{
		return Utils.getAsBoolean(settings.getProperty(GRID_SHOW_SETTING, String.valueOf(GRID_SHOW_DEFAULT)));
	}

	public void setGridShow(boolean gridShow)
	{
		settings.setProperty(GRID_SHOW_SETTING, String.valueOf(gridShow));
	}

	public boolean getGridSnapTo()
	{
		return Utils.getAsBoolean(settings.getProperty(GRID_SNAPTO_SETTING, String.valueOf(GRID_SNAPTO_DEFAULT)));
	}

	public void setGridSnapTo(boolean gridSnapTo)
	{
		settings.setProperty(GRID_SNAPTO_SETTING, String.valueOf(gridSnapTo));
	}

	public boolean getSaveEditorState()
	{
		return Utils.getAsBoolean(settings.getProperty(SAVE_EDITOR_STATE_SETTING, String.valueOf(SAVE_EDITOR_STATE_DEFAULT)));
	}

	public void setSaveEditorState(boolean saveEditorState)
	{
		settings.setProperty(SAVE_EDITOR_STATE_SETTING, String.valueOf(saveEditorState));
	}
}
