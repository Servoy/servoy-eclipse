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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.jabsorb.JSONSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
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

	private static final String DESIGNER_SETTINGS_PREFIX = "designer.";

	public static final String METRICS_SETTING = "preferdMetrics";
	public static final String STEP_SIZE_SETTING = "stepSize";
	public static final String COPY_PASTE_OFFSET_SETTING = "copyPasteOffset";
	public static final String ALIGNMENT_THRESHOLD_SETTING = "alignmentThreshold";
	public static final String ALIGNMENT_INDENT_SETTING = "alignmentIndent";
	public static final String ALIGNMENT_DISTANCES_SETTING = "alignmentDistances";
	public static final String GUIDE_SIZE_SETTING = "guidesize";
	public static final String GRID_COLOR_SETTING = "gridcolor";
	public static final String SAME_HEIGHT_WIDTH_INDICATOR_COLOR_SETTING = "sameHeightWidthColor";
	public static final String ALIGNMENT_GUIDE_COLOR_SETTING = "alignmentguidecolor";
	public static final String GRID_SIZE_SETTING = "gridsize";
	public static final String GRID_POINTSIZE_SETTING = "pointsize";
	public static final String SNAPTO_SETTING = "snapTo";
	public static final String FEEDBACK_ALIGNMENT_SETTING = "feedBackAlignment";
	public static final String FEEDBACK_GRID_SETTING = "feedBackGrid";
	public static final String ANCHOR_SETTING = "anchor";
	public static final String CLOSE_EDITORS_ON_EXIT_SETTING = "saveEditorState";
	public static final String OPEN_FIRST_FORM_DESIGNER_SETTING = "openFirstFormDesigner";
	public static final String SHOW_COLUMNS_IN_DB_ORDER_SETTING = "showColumnsInDbOrder";
	public static final String FORM_TOOLS_ON_MAIN_TOOLBAR_SETTING = "formToolsOnMainToolbar";
	public static final String FORM_COOLBAR_LAYOUT_SETTING = "formCoolBarLayout";
	public static final String SHOW_SAME_SIZE_SETTING = "showSameSizeFeedback";
	public static final String SHOW_ANCHORING_SETTING = "showAnchoringFeedback";
	public static final String PALETTE_CUSTOMIZATION_SETTING = "paletteCustomization";
	public static final String PAINT_PAGEBREAKS_SETTING = "paintPageBreaks";
	public static final String SHOW_RULERS_SETTING = "showRulers";
	public static final String MARQUEE_SELECT_OUTER_SETTING = "marqueeSelectOuter";
	public static final String CLASSIC_FORM_EDITOR_MOBILE_SETTING = "classicFormEditorMobile";
	public static final String CLASSIC_FORM_EDITOR_SETTING = "classicFormEditor";
	public static final String FORM_EVENT_HANDLER_NAMING_SETTING = "formEventHandlerNaming";
	public static final String TABLE_EVENT_HANDLER_NAMING_SETTING = "tableEventHandlerNaming";
	public static final String LOADED_RELATIONS_NAMING_PATTERN_SETTING = "loadedRelationsNamingPattern";
	public static final String PK_SEQUENCE_TYPE_SETTING = "primaryKeySequenceType";
	public static final String SHOW_NAVIGATOR_DEFAULT_SETTING = "showNavigatorDefault";
	public static final String ENCAPSULATION_TYPE = "encapsulationType";

	public static final String SNAP_TO_ALIGMNENT = "alignment";
	public static final String SNAP_TO_GRID = "grid";
	public static final String SNAP_TO_NONE = "none";

	public static final int COPY_PASTE_OFFSET_DEFAULT = 10;
	public static final int ALIGNMENT_THRESHOLD_DEFAULT = 3;
	public static final int ALIGNMENT_INDENT_DEFAULT = 5;
	public static final int[] ALIGNMENT_DISTANCES_DEFAULT = { 5, 10, 15 }; // small, medium, large
	public static final int STEP_SIZE_DEFAULT = 10;
	public static final int LARGE_STEP_SIZE_DEFAULT = 20;
	public static final int GUIDE_SIZE_DEFAULT = 10;
	public static final int GRID_SIZE_DEFAULT = 10;
	public static final String GRID_COLOR_DEFAULT = "#b4b4b4";
	public static final String SAME_HEIGHT_WIDTH_INDICATOR_COLOR_DEFAULT = "#00007f";
	public static final String ALIGNMENT_GUIDE_COLOR_DEFAULT = "#8eacc3";
	public static final int GRID_POINTSIZE_DEFAULT = 2;
	public static final String SNAPTO_DEFAULT = SNAP_TO_ALIGMNENT;
	public static final boolean FEEDBACK_ALIGNMENT_DEFAULT = true;
	public static final boolean FEEDBACK_GRID_DEFAULT = false;
	public static final boolean ANCHOR_DEFAULT = false;
	public static final boolean CLOSE_EDITORS_ON_EXIT_DEFAULT = false;
	public static final boolean OPEN_FIRST_FORM_DESIGNER_DEFAULT = true;
	public static final boolean SHOW_COLUMNS_IN_DB_ORDER_DEFAULT = false;
	public static final boolean SHOW_SAME_SIZE_DEFAULT = true;
	public static final boolean SHOW_ANCHORING_DEFAULT = true;
	public static final boolean FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT = true;
	public static final int METRICS_DEFAULT = PX;
	public static final boolean PAINT_PAGEBREAKS_DEFAULT = false;
	public static final boolean SHOW_RULERS_DEFAULT = true;
	public static final boolean MARQUEE_SELECT_OUTER_DEFAULT = true;
	public static final boolean CLASSIC_FORM_EDITOR_MOBILE_DEFAULT = true;
	public static final boolean CLASSIC_FORM_EDITOR_SETTING_DEFAULT = false;

	public static final int FORM_EVENT_HANDLER_NAMING_DEFAULT = 0;
	public static final int FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_NAME = 1;
	public static final int FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_DATAPROVIDER = 2;
	public static final int FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_DATAPROVIDER_FALLBACK = 4;

	public static final int TABLE_EVENT_HANDLER_NAMING_DEFAULT = 0;
	public static final int TABLE_EVENT_HANDLER_NAMING_INCLUDE_TABLE_NAME = 1;

	public static final boolean USE_SERVOY_SEQUENCE_DEFAULT = true;
	public static final boolean USE_DATABASE_SEQUENCE_DEFAULT = false;
	public static final int PK_SEQUENCE_TYPE_DEFAULT = ColumnInfo.SERVOY_SEQUENCE;
	public static final boolean SHOW_NAVIGATOR_DEFAULT = true;

	public static final int ENCAPSULATION_PUBLIC_HIDE_ALL = PersistEncapsulation.HIDE_CONTROLLER | PersistEncapsulation.HIDE_DATAPROVIDERS |
		PersistEncapsulation.HIDE_ELEMENTS | PersistEncapsulation.HIDE_FOUNDSET;
	public static final int ENCAPSULATION_PUBLIC = PersistEncapsulation.DEFAULT;

	protected final IEclipsePreferences eclipsePreferences;

	public DesignerPreferences()
	{
		eclipsePreferences = Activator.getDefault().getEclipsePreferences();
	}

	public void save()
	{
		try
		{
			eclipsePreferences.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	protected String getProperty(String key, String defaultValue)
	{
		return eclipsePreferences.get(DESIGNER_SETTINGS_PREFIX + key, defaultValue);
	}

	protected int getProperty(String key, int defaultValue)
	{
		return eclipsePreferences.getInt(DESIGNER_SETTINGS_PREFIX + key, defaultValue);
	}

	protected boolean getProperty(String key, boolean defaultValue)
	{
		return eclipsePreferences.getBoolean(DESIGNER_SETTINGS_PREFIX + key, defaultValue);
	}

	protected void setProperty(String key, String value)
	{
		if (value == null)
		{
			removeProperty(key);
		}
		else
		{
			eclipsePreferences.put(DESIGNER_SETTINGS_PREFIX + key, value);
		}
	}

	protected void setProperty(String key, int value)
	{
		eclipsePreferences.putInt(DESIGNER_SETTINGS_PREFIX + key, value);
	}

	protected void setProperty(String key, boolean value)
	{
		eclipsePreferences.putBoolean(DESIGNER_SETTINGS_PREFIX + key, value);
	}

	protected void removeProperty(String key)
	{
		eclipsePreferences.remove(DESIGNER_SETTINGS_PREFIX + key);
	}

	private static String getKeyPostfix(String key)
	{
		if (key == null || !key.startsWith(DESIGNER_SETTINGS_PREFIX))
		{
			return null;
		}
		return key.substring(DESIGNER_SETTINGS_PREFIX.length());
	}

	public static boolean isMetricsSetting(String key)
	{
		return METRICS_SETTING.equals(getKeyPostfix(key));
	}

	public int getMetrics()
	{
		return getProperty(METRICS_SETTING, METRICS_DEFAULT);
	}

	public void setMetrics(int metrics)
	{
		setProperty(METRICS_SETTING, metrics);
	}

	public int getStepSize()
	{
		return PersistHelper.createDimension(getProperty(STEP_SIZE_SETTING, STEP_SIZE_DEFAULT + ",0")).width;
	}

	public int getLargeStepSize()
	{
		return PersistHelper.createDimension(getProperty(STEP_SIZE_SETTING, "0," + LARGE_STEP_SIZE_DEFAULT)).height;
	}

	public void setStepSize(int stepSize, int largeStepSize)
	{
		setProperty(STEP_SIZE_SETTING, stepSize + "," + largeStepSize);
	}

	public int getCopyPasteOffset()
	{
		return getProperty(COPY_PASTE_OFFSET_SETTING, COPY_PASTE_OFFSET_DEFAULT);
	}

	public void setCopyPasteOffset(int copyPasteOffset)
	{
		setProperty(COPY_PASTE_OFFSET_SETTING, copyPasteOffset);
	}

	public int getAlignmentThreshold()
	{
		return getProperty(ALIGNMENT_THRESHOLD_SETTING, ALIGNMENT_THRESHOLD_DEFAULT);
	}

	public void setAlignmentThreshold(int alignmentThreshold)
	{
		setProperty(ALIGNMENT_THRESHOLD_SETTING, alignmentThreshold);
	}

	public int getAlignmentIndent()
	{
		return getProperty(ALIGNMENT_INDENT_SETTING, ALIGNMENT_INDENT_DEFAULT);
	}

	public void setAlignmentIndent(int indent)
	{
		setProperty(ALIGNMENT_INDENT_SETTING, indent);
	}

	public int[] getAlignmentDistances()
	{
		String property = getProperty(ALIGNMENT_DISTANCES_SETTING, null);
		if (property == null)
		{
			return ALIGNMENT_DISTANCES_DEFAULT;
		}
		int[] distances = new int[3];
		String[] nrs = property.split(":");
		if (nrs.length != 3)
		{
			return ALIGNMENT_DISTANCES_DEFAULT;
		}
		distances[0] = Utils.getAsInteger(nrs[0]);
		distances[1] = Utils.getAsInteger(nrs[1]);
		distances[2] = Utils.getAsInteger(nrs[2]);
		Arrays.sort(distances);
		return distances;
	}

	public void setAlignmentDistances(int smallDistance, int mediumDistance, int largeDistance)
	{
		setProperty(ALIGNMENT_DISTANCES_SETTING, String.valueOf(smallDistance) + ':' + String.valueOf(mediumDistance) + ':' + String.valueOf(largeDistance));
	}

	public boolean getAnchor()
	{
		return getProperty(ANCHOR_SETTING, ANCHOR_DEFAULT);
	}

	public void setAnchor(boolean anchor)
	{
		setProperty(ANCHOR_SETTING, anchor);
	}

	public boolean getPaintPageBreaks()
	{
		return getProperty(PAINT_PAGEBREAKS_SETTING, PAINT_PAGEBREAKS_DEFAULT);
	}

	public void setPaintPageBreaks(boolean paintPageBreaks)
	{
		setProperty(PAINT_PAGEBREAKS_SETTING, paintPageBreaks);
	}

	public boolean getShowRulers()
	{
		return getProperty(SHOW_RULERS_SETTING, SHOW_RULERS_DEFAULT);
	}

	public void setShowRulers(boolean showRulers)
	{
		setProperty(SHOW_RULERS_SETTING, showRulers);
	}

	public boolean getMarqueeSelectOuter()
	{
		return getProperty(MARQUEE_SELECT_OUTER_SETTING, MARQUEE_SELECT_OUTER_DEFAULT);
	}

	public void setMarqueeSelectOuter(boolean outer)
	{
		setProperty(MARQUEE_SELECT_OUTER_SETTING, outer);
	}

	public boolean getClassicFormEditorInMobile()
	{
		return getProperty(CLASSIC_FORM_EDITOR_MOBILE_SETTING, CLASSIC_FORM_EDITOR_MOBILE_DEFAULT);
	}

	public void setClassicFormEditorInMobile(boolean classic)
	{
		setProperty(CLASSIC_FORM_EDITOR_MOBILE_SETTING, classic);
	}

	public boolean getClassicFormEditor()
	{
		return getProperty(CLASSIC_FORM_EDITOR_SETTING, CLASSIC_FORM_EDITOR_SETTING_DEFAULT);
	}

	public void setClassicFormEditor(boolean useClassic)
	{
		setProperty(CLASSIC_FORM_EDITOR_SETTING, useClassic);
	}

	public static boolean isGuideSetting(String key)
	{
		return GUIDE_SIZE_SETTING.equals(getKeyPostfix(key));
	}

	public int getGuideSize()
	{
		return getProperty(GUIDE_SIZE_SETTING, GUIDE_SIZE_DEFAULT);
	}

	public void setGuideSize(int guideSize)
	{
		setProperty(GUIDE_SIZE_SETTING, guideSize);
	}

	public RGB getGridColor()
	{
		return ColorResource.ColorAwt2Rgb(PersistHelper.createColor(getProperty(GRID_COLOR_SETTING, GRID_COLOR_DEFAULT)));
	}

	public RGB getSameHeightWidthIndicatorColor()
	{
		return ColorResource.ColorAwt2Rgb(PersistHelper.createColor(getProperty(SAME_HEIGHT_WIDTH_INDICATOR_COLOR_SETTING,
			SAME_HEIGHT_WIDTH_INDICATOR_COLOR_DEFAULT)));
	}

	public void setGridColor(RGB rgb)
	{
		setProperty(GRID_COLOR_SETTING, PersistHelper.createColorString(ColorResource.ColoRgb2Awt(rgb)));
	}

	public void setSameHeightWidthIndicatorColor(RGB rgb)
	{
		setProperty(SAME_HEIGHT_WIDTH_INDICATOR_COLOR_SETTING, PersistHelper.createColorString(ColorResource.ColoRgb2Awt(rgb)));
	}

	public RGB getAlignmentGuideColor()
	{
		return ColorResource.ColorAwt2Rgb(PersistHelper.createColor(getProperty(ALIGNMENT_GUIDE_COLOR_SETTING, ALIGNMENT_GUIDE_COLOR_DEFAULT)));
	}

	public void setAlignmentGuideColor(RGB rgb)
	{
		setProperty(ALIGNMENT_GUIDE_COLOR_SETTING, PersistHelper.createColorString(ColorResource.ColoRgb2Awt(rgb)));
	}

	public int getGridSize()
	{
		return getProperty(GRID_SIZE_SETTING, GRID_SIZE_DEFAULT);
	}

	public void setGridSize(int gridSize)
	{
		setProperty(GRID_SIZE_SETTING, gridSize);
	}

	public int getGridPointSize()
	{
		return getProperty(GRID_POINTSIZE_SETTING, GRID_POINTSIZE_DEFAULT);
	}

	public void setGridPointSize(int gridPointSize)
	{
		setProperty(GRID_POINTSIZE_SETTING, gridPointSize);
	}

	public boolean getNoneSnapTo()
	{
		return !getGridSnapTo() && !getAlignmentSnapTo();
	}

	public boolean getGridSnapTo()
	{
		return SNAP_TO_GRID.equals(getProperty(SNAPTO_SETTING, SNAPTO_DEFAULT));
	}

	public boolean getAlignmentSnapTo()
	{
		return SNAP_TO_ALIGMNENT.equals(getProperty(SNAPTO_SETTING, SNAPTO_DEFAULT));
	}

	public void setSnapTo(boolean grid, boolean alignment)
	{
		setProperty(SNAPTO_SETTING, grid ? SNAP_TO_GRID : alignment ? SNAP_TO_ALIGMNENT : SNAP_TO_NONE);
	}

	public boolean getFeedbackGrid()
	{
		return getProperty(FEEDBACK_GRID_SETTING, FEEDBACK_GRID_DEFAULT);
	}

	public void setFeedbackGrid(boolean feedbackGrid)
	{
		setProperty(FEEDBACK_GRID_SETTING, feedbackGrid);
	}

	public boolean getFeedbackAlignment()
	{
		return getProperty(FEEDBACK_ALIGNMENT_SETTING, FEEDBACK_ALIGNMENT_DEFAULT);
	}

	public void setFeedbackAlignment(boolean feedbackAlignment)
	{
		setProperty(FEEDBACK_ALIGNMENT_SETTING, feedbackAlignment);
	}

	public boolean getCloseEditorOnExit()
	{
		return getProperty(CLOSE_EDITORS_ON_EXIT_SETTING, CLOSE_EDITORS_ON_EXIT_DEFAULT);
	}

	public void setCloseEditorsOnExit(boolean saveEditorState)
	{
		setProperty(CLOSE_EDITORS_ON_EXIT_SETTING, saveEditorState);
	}

	public boolean getOpenFirstFormDesigner()
	{
		return getProperty(OPEN_FIRST_FORM_DESIGNER_SETTING, OPEN_FIRST_FORM_DESIGNER_DEFAULT);
	}

	public void setOpenFirstFormDesigner(boolean openFirstFormDesigner)
	{
		setProperty(OPEN_FIRST_FORM_DESIGNER_SETTING, openFirstFormDesigner);
	}

	public boolean getShowColumnsInDbOrder()
	{
		return getProperty(SHOW_COLUMNS_IN_DB_ORDER_SETTING, SHOW_COLUMNS_IN_DB_ORDER_DEFAULT);
	}

	public void setShowColumnsInDbOrder(boolean showColumnsInDbOrder)
	{
		setProperty(SHOW_COLUMNS_IN_DB_ORDER_SETTING, showColumnsInDbOrder);
	}

	public boolean getShowSameSizeFeedback()
	{
		return getProperty(SHOW_SAME_SIZE_SETTING, SHOW_SAME_SIZE_DEFAULT);
	}

	public void setShowSameSizeFeedback(boolean showSameSize)
	{
		setProperty(SHOW_SAME_SIZE_SETTING, showSameSize);
	}

	public boolean getShowAnchorFeedback()
	{
		return getProperty(SHOW_ANCHORING_SETTING, SHOW_ANCHORING_DEFAULT);
	}

	public void setShowAnchorFeedback(boolean showAnchoring)
	{
		setProperty(SHOW_ANCHORING_SETTING, showAnchoring);
	}

	public boolean getFormToolsOnMainToolbar()
	{
		return getProperty(FORM_TOOLS_ON_MAIN_TOOLBAR_SETTING, FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT);
	}

	public void setFormToolsOnMainToolbar(boolean formToolsOnMainToolbar)
	{
		setProperty(FORM_TOOLS_ON_MAIN_TOOLBAR_SETTING, formToolsOnMainToolbar);
	}

	public void setFormEventHandlerNaming(int value)
	{
		setProperty(FORM_EVENT_HANDLER_NAMING_SETTING, value);
	}

	public int getFormEventHandlerNaming()
	{
		return getProperty(FORM_EVENT_HANDLER_NAMING_SETTING, FORM_EVENT_HANDLER_NAMING_DEFAULT);
	}

	public boolean getFormEventHandlerNamingDefault()
	{
		return getFormEventHandlerNaming() == FORM_EVENT_HANDLER_NAMING_DEFAULT;
	}

	public boolean getIncludeFormElementName()
	{
		return getFormEventHandlerNaming() == FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_NAME;
	}

	public boolean getIncludeFormElementDataProviderName()
	{
		return getFormEventHandlerNaming() == FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_DATAPROVIDER;
	}

	public boolean getIncludeFormElementDataProviderNameWithFallback()
	{
		return getFormEventHandlerNaming() == FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_DATAPROVIDER_FALLBACK;
	}

	public void setTableEventHandlerNaming(int value)
	{
		setProperty(TABLE_EVENT_HANDLER_NAMING_SETTING, value);
	}

	public int getTableEventHandlerNaming()
	{
		return getProperty(TABLE_EVENT_HANDLER_NAMING_SETTING, TABLE_EVENT_HANDLER_NAMING_DEFAULT);
	}

	public boolean getTableEventHandlerNamingDefault()
	{
		return getTableEventHandlerNaming() == TABLE_EVENT_HANDLER_NAMING_DEFAULT;
	}

	public boolean getIncludeTableName()
	{
		return getTableEventHandlerNaming() == TABLE_EVENT_HANDLER_NAMING_INCLUDE_TABLE_NAME;
	}

	public void setLoadedRelationsNamingPattern(String value)
	{
		setProperty(LOADED_RELATIONS_NAMING_PATTERN_SETTING, value);
	}

	public String getLoadedRelationsNamingPattern()
	{
		return getProperty(LOADED_RELATIONS_NAMING_PATTERN_SETTING, null);
	}

	public int getPrimaryKeySequenceType()
	{
		return getProperty(PK_SEQUENCE_TYPE_SETTING, PK_SEQUENCE_TYPE_DEFAULT);
	}

	public void setPrimaryKeySequenceType(int primaryKeySequenceType)
	{
		setProperty(PK_SEQUENCE_TYPE_SETTING, primaryKeySequenceType);
	}

	public boolean getShowNavigatorDefault()
	{
		return getProperty(SHOW_NAVIGATOR_DEFAULT_SETTING, SHOW_NAVIGATOR_DEFAULT);
	}

	public void setShowNavigatorDefault(boolean showNavigatorDefault)
	{
		setProperty(SHOW_NAVIGATOR_DEFAULT_SETTING, showNavigatorDefault);
	}

	public int getEncapsulationType()
	{
		return getProperty(ENCAPSULATION_TYPE, ENCAPSULATION_PUBLIC_HIDE_ALL);
	}

	public void setEncapsulationType(int encType)
	{
		setProperty(ENCAPSULATION_TYPE, encType);
	}

	public static boolean isCoolbarSetting(String key)
	{
		return FORM_COOLBAR_LAYOUT_SETTING.equals(getKeyPostfix(key));
	}

	public static boolean isPaletteSetting(String key)
	{
		return PALETTE_CUSTOMIZATION_SETTING.equals(getKeyPostfix(key));
	}

	public void saveCoolbarLayout(CoolbarLayout coolbarLayout)
	{
		if (coolbarLayout == null)
		{
			removeProperty(FORM_COOLBAR_LAYOUT_SETTING);
		}
		else
		{
			Map<String, Object> map = new HashMap<String, Object>();
			if (coolbarLayout.itemOrder.length > 0) map.put("itemOrder", coolbarLayout.itemOrder);
			if (coolbarLayout.wrapIndices.length > 0) map.put("wrapIndices", coolbarLayout.wrapIndices);
			if (coolbarLayout.hiddenBars.length > 0) map.put("hiddenBars", coolbarLayout.hiddenBars);
			if (coolbarLayout.ids.length > 0) map.put("ids", coolbarLayout.ids);
			if (coolbarLayout.sizes.length > 0)
			{
				int[] sizesX = new int[coolbarLayout.sizes.length];
				int[] sizesY = new int[coolbarLayout.sizes.length];
				for (int i = 0; i < coolbarLayout.sizes.length; i++)
				{
					sizesX[i] = coolbarLayout.sizes[i].x;
					sizesY[i] = coolbarLayout.sizes[i].y;
				}
				map.put("sizesX", sizesX);
				map.put("sizesY", sizesY);
			}

			JSONSerializer serializer = new JSONSerializer();
			try
			{
				serializer.registerDefaultSerializers();
				setProperty(FORM_COOLBAR_LAYOUT_SETTING, serializer.toJSON(map));
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		save();
	}

	public CoolbarLayout getCoolbarLayout()
	{
		String property = getProperty(FORM_COOLBAR_LAYOUT_SETTING, null);
		if (property != null)
		{
			JSONSerializer serializer = new JSONSerializer();
			try
			{
				serializer.registerDefaultSerializers();
				Map map = (Map)serializer.fromJSON(property);

				Integer[] ints = (Integer[])map.get("itemOrder");

				int[] itemOrder = new int[ints == null ? 0 : ints.length];
				for (int i = 0; i < itemOrder.length; i++)
				{
					itemOrder[i] = ints[i].intValue();
				}

				ints = (Integer[])map.get("wrapIndices");
				int[] wrapIndices = new int[ints == null ? 0 : ints.length];
				for (int i = 0; i < wrapIndices.length; i++)
				{
					wrapIndices[i] = ints[i].intValue();
				}
				Integer[] sizesX = (Integer[])map.get("sizesX");
				Integer[] sizesY = (Integer[])map.get("sizesY");
				Point[] sizes = new Point[sizesX == null ? 0 : sizesX.length];
				for (int i = 0; i < sizesX.length; i++)
				{
					sizes[i] = new Point(sizesX[i].intValue(), sizesY[i].intValue());
				}
				String[] hiddenBars = (String[])map.get("hiddenBars");
				String[] ids = (String[])map.get("ids");
				return new CoolbarLayout(itemOrder, wrapIndices, sizes, hiddenBars == null ? new String[0] : hiddenBars, ids == null ? new String[0] : ids);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}

	public static class CoolbarLayout
	{
		public final int[] itemOrder;
		public final int[] wrapIndices;
		public final Point[] sizes;
		public final String[] hiddenBars;
		public final String[] ids;

		public CoolbarLayout(int[] itemOrder, int[] wrapIndices, Point[] sizes, String[] hiddenBars, String[] ids)
		{
			this.itemOrder = itemOrder;
			this.wrapIndices = wrapIndices;
			this.sizes = sizes;
			this.hiddenBars = hiddenBars;
			this.ids = ids;
		}
	}

	public PaletteCustomization getPaletteCustomization()
	{
		String property = getProperty(PALETTE_CUSTOMIZATION_SETTING, null);
		if (property != null)
		{
			try
			{
				Map map = (Map)ServoyJSONObject.toJava(new ServoyJSONObject(property, false));
				return new PaletteCustomization((List<String>)map.get(PaletteCustomization.DRAWERS),
					(Map<String, List<String>>)map.get(PaletteCustomization.DRAWER_ENTRIES),
					(Map<String, Object>)map.get(PaletteCustomization.ENTRY_PROPERTIES));
			}
			catch (JSONException e)
			{
				ServoyLog.logError("Could not read palette preferences", e);
			}
		}
		return null;
	}

	public void setPaletteCustomization(PaletteCustomization prefs)
	{
		try
		{
			JSONObject jsonObject = null;
			if (prefs != null)
			{
				jsonObject = new JSONObject();
				if (prefs.drawers != null)
				{
					jsonObject.put(PaletteCustomization.DRAWERS, prefs.drawers);
				}
				if (prefs.drawerEntries != null && prefs.drawerEntries.size() > 0)
				{
					jsonObject.put(PaletteCustomization.DRAWER_ENTRIES, prefs.drawerEntries);
				}
				if (prefs.entryProperties != null && prefs.entryProperties.size() > 0)
				{
					jsonObject.put(PaletteCustomization.ENTRY_PROPERTIES, prefs.entryProperties);
				}
			}
			if (jsonObject != null && jsonObject.keys().hasNext())
			{
				setProperty(PALETTE_CUSTOMIZATION_SETTING, ServoyJSONObject.toString(jsonObject, false, false, false));
			}
			else
			{
				removeProperty(PALETTE_CUSTOMIZATION_SETTING);
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Could not save palette preferences", e);
		}
	}

	public static class PaletteCustomization
	{
		public static final String ENTRY_PROPERTIES = "entryProperties";
		public static final String DRAWER_ENTRIES = "drawerEntries";
		public static final String DRAWERS = "drawers";

		public static final String PROPERTY_INITIAL_STATE = "initialState";
		public static final String PROPERTY_DESCRIPTION = "description";
		public static final String PROPERTY_LABEL = "label";
		public static final String PROPERTY_HIDDEN = "hidden";

		public final List<String> drawers;
		public final Map<String, List<String>> drawerEntries;
		public final Map<String, Object> entryProperties;

		/**
		 * @param drawers
		 * @param drawerEntries
		 * @param entryProperties
		 */
		public PaletteCustomization(List<String> drawers, Map<String, List<String>> drawerEntries, Map<String, Object> entryProperties)
		{
			this.drawers = drawers;
			this.drawerEntries = drawerEntries;
			this.entryProperties = entryProperties;
		}
	}

}
