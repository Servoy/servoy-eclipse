/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ui.property;

/**
 * Something that can provide a text toolTip (can contain multiple lines of text) but can also update the toolTip value that it keeps.
 *
 * @author acostescu
 */
public interface IKeepsTooltip extends IProvidesTooltip
{

	/**
	 * If the tooltip text is easily available for this object, remember it by calling this method.
	 */
	void setTooltipText(String tooltipText);

	/**
	 * If the tooltip requires too much code execution and it might even not be shown/needed for this object, one can set a provider that will
	 * only be called when {@link #getTooltipText()} is called. If provider will return null, we fall-back to the tooltipText (String) if available.
	 */
	void setTooltipProvider(IProvidesTooltip tooltipProvider);

}
