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

package com.servoy.eclipse.core.elements;

import java.awt.Dimension;
import java.util.List;

import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 * @since 8.2
 *
 */
public interface IPlaceDataProviderConfiguration
{

	public List<Pair<IDataProvider, Object>> getDataProvidersConfig();

	public int getFieldSpacing();

	/**
	 * @return the fillName
	 */
	public boolean isFillName();

	/**
	 * @return the fillText
	 */
	public boolean isFillText();

	/**
	 * @return the placeOnTop
	 */
	public boolean isPlaceOnTop();

	/**
	 * @return the placeWithLabels
	 */
	public boolean isPlaceWithLabels();

	/**
	 * @return the labelComponent
	 */
	public String getLabelComponent();

	/**
	 * @return the labelSpacing
	 */
	public int getLabelSpacing();

	/**
	 * @return the placeHorizontally
	 */
	public boolean isPlaceHorizontally();

	/**
	 * @return the fieldSize
	 */
	public Dimension getFieldSize();

	/**
	 * @return the labelSize
	 */
	public Dimension getLabelSize();

	/**
	 * @return the automaticI18N
	 */
	public boolean isAutomaticI18N();

	/**
	 * @return the i18n prefix
	 */
	public String getI18NPrefix();
}
