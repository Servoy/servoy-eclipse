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
package com.servoy.eclipse.designer.editor.commands;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.elements.IPlaceDataProviderConfiguration;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.util.Pair;

/**
 * Request with options for placing fields.
 *
 * @author rgansevles
 *
 */
public class DataFieldRequest extends DataRequest implements IPlaceDataProviderConfiguration
{
	public final boolean placeAsLabels;
	public final boolean placeWithLabels;
	public final boolean placeHorizontal;
	public final boolean fillText;
	public final boolean fillName;
	public final int fieldSpacing;
	public final int labelSpacing;
	public final String labelComponent;
	public final boolean placeOnTop;
	private Dimension labelSize;
	private Dimension fieldSize;
	private final boolean automaticI18N;
	private final String i18nPrefix;

	public DataFieldRequest(Object type, Object[] data, boolean placeAsLabels, boolean placeWithLabels, boolean placeHorizontal, boolean fillText,
		boolean fillName)
	{
		super(type, data);
		this.placeAsLabels = placeAsLabels;
		this.placeWithLabels = placeWithLabels;
		this.placeHorizontal = placeHorizontal;
		this.fillText = fillText;
		this.fillName = fillName;
		this.fieldSpacing = -1;
		this.labelSpacing = -1;
		this.labelComponent = null;
		this.placeOnTop = false;
		this.automaticI18N = false;
		this.i18nPrefix = null;
	}

	/**
	 * @param requestType
	 * @param array
	 * @param b
	 * @param placeWithLabels2
	 * @param c
	 * @param fillText2
	 * @param fillName2
	 * @param fieldSpacing
	 * @param labelSpacing
	 * @param labelComponent
	 * @param placeOnTop
	 */
	public DataFieldRequest(Object type, List<Pair<IDataProvider, Object>> data, boolean placeAsLabels, boolean placeWithLabels, boolean placeHorizontal,
		boolean fillText, boolean fillName, int fieldSpacing, int labelSpacing, String labelComponent, boolean placeOnTop, Dimension fieldSize,
		Dimension labelSize, boolean automaticI18N, String i18nPrefix)
	{
		super(type, data);
		this.placeAsLabels = placeAsLabels;
		this.placeWithLabels = placeWithLabels;
		this.placeHorizontal = placeHorizontal;
		this.fillText = fillText;
		this.fillName = fillName;
		this.fieldSpacing = fieldSpacing;
		this.labelSpacing = labelSpacing;
		this.labelComponent = labelComponent;
		this.placeOnTop = placeOnTop;
		this.labelSize = labelSize;
		this.fieldSize = fieldSize;
		this.automaticI18N = automaticI18N;
		this.i18nPrefix = i18nPrefix;
	}

	/**
	 * @param requestType
	 * @param data
	 * @param extendedData
	 */
	public DataFieldRequest(Object type, Object[] data, Map<Object, Object> extendedData)
	{
		this(type, data, false, false, false, false, false);
		setExtendedData(extendedData);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Pair<IDataProvider, Object>> getDataProvidersConfig()
	{
		Object object = getData();
		if (object instanceof List< ? >)
		{
			return (List<Pair<IDataProvider, Object>>)object;
		}
		else if (object instanceof Object[])
		{
			List<Pair<IDataProvider, Object>> lst = new ArrayList<>();
			for (Object pb : (Object[])object)
			{
				lst.add(new Pair<IDataProvider, Object>((IDataProvider)pb, null));
			}
			return lst;
		}
		return null;
	}

	@Override
	public int getFieldSpacing()
	{
		return fieldSpacing;
	}

	@Override
	public boolean isFillName()
	{
		return fillName;
	}

	@Override
	public boolean isFillText()
	{
		return fillText;
	}

	@Override
	public boolean isPlaceOnTop()
	{
		return placeOnTop;
	}

	@Override
	public boolean isPlaceWithLabels()
	{
		return placeWithLabels;
	}

	@Override
	public String getLabelComponent()
	{
		return labelComponent;
	}

	@Override
	public int getLabelSpacing()
	{
		return labelSpacing;
	}

	@Override
	public boolean isPlaceHorizontally()
	{
		return placeHorizontal;
	}

	@Override
	public Dimension getFieldSize()
	{
		return fieldSize;
	}

	@Override
	public Dimension getLabelSize()
	{
		return labelSize;
	}

	@Override
	public boolean isAutomaticI18N()
	{
		return automaticI18N;
	}

	@Override
	public String getI18NPrefix()
	{
		return i18nPrefix;
	}
}
