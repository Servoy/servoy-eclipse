/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import java.awt.Dimension;
import java.util.List;

import com.servoy.eclipse.core.elements.IPlaceDataProviderConfiguration;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 *
 * @since 8.2
 *
 */
public class PlaceDataProviderConfiguration implements IPlaceDataProviderConfiguration
{

	private final List<Pair<IDataProvider, Object>> selection;
	private final int fieldSpacing;
	private final boolean placeWithLabels;
	private final String labelComponent;
	private final int labelSpacing;
	private final boolean placeOnTop;
	private final boolean fillName;
	private final boolean fillText;
	private final boolean placeHorizontally;
	private final Dimension fieldSize;
	private final Dimension labelSize;
	private final boolean automaticI18N;
	private final String i18nPrefix;

	/**
	 * @param selection
	 * @param fieldSpacing
	 * @param placeWithLabels
	 * @param labelComponent
	 * @param labelSpacing
	 * @param placeOnTop
	 * @param fillName
	 * @param fillText
	 */
	public PlaceDataProviderConfiguration(List<Pair<IDataProvider, Object>> selection, int fieldSpacing, boolean placeWithLabels, String labelComponent,
		int labelSpacing, boolean placeOnTop, boolean fillName, boolean fillText, boolean placeHorizontally, Dimension fieldSize, Dimension labelSize,
		boolean automaticI18N, String i18nPrefix)
	{
		this.selection = selection;
		this.fieldSpacing = fieldSpacing;
		this.placeWithLabels = placeWithLabels;
		this.labelComponent = labelComponent;
		this.labelSpacing = labelSpacing;
		this.placeOnTop = placeOnTop;
		this.fillName = fillName;
		this.fillText = fillText;
		this.placeHorizontally = placeHorizontally;
		this.fieldSize = fieldSize;
		this.labelSize = labelSize;
		this.automaticI18N = automaticI18N;
		this.i18nPrefix = i18nPrefix;
	}

	/**
	 * @return the selection
	 */
	public List<Pair<IDataProvider, Object>> getDataProvidersConfig()
	{
		return selection;
	}

	/**
	 * @return the fieldSpacing
	 */
	public int getFieldSpacing()
	{
		return fieldSpacing;
	}

	/**
	 * @return the fillName
	 */
	public boolean isFillName()
	{
		return fillName;
	}

	/**
	 * @return the fillText
	 */
	public boolean isFillText()
	{
		return fillText;
	}

	/**
	 * @return the placeOnTop
	 */
	public boolean isPlaceOnTop()
	{
		return placeOnTop;
	}

	/**
	 * @return the placeWithLabels
	 */
	public boolean isPlaceWithLabels()
	{
		return placeWithLabels;
	}

	/**
	 * @return the labelComponent
	 */
	public String getLabelComponent()
	{
		return labelComponent;
	}

	/**
	 * @return the labelSpacing
	 */
	public int getLabelSpacing()
	{
		return labelSpacing;
	}

	/**
	 * @return the placeHorizontally
	 */
	public boolean isPlaceHorizontally()
	{
		return placeHorizontally;
	}

	/**
	 * @return the fieldSize
	 */
	public Dimension getFieldSize()
	{
		return fieldSize;
	}

	/**
	 * @return the labelSize
	 */
	public Dimension getLabelSize()
	{
		return labelSize;
	}

	/**
	 * @return the automaticI18N
	 */
	public boolean isAutomaticI18N()
	{
		return (fillText || placeWithLabels) && automaticI18N;
	}

	/**
	 * @return the i18nPrefix
	 */
	public String getI18NPrefix()
	{
		return i18nPrefix;
	}
}
