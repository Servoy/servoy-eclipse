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

import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * Just a simple property descriptor that can have/provide a tooltip text.
 *
 * @author acostescu
 */
public class PropertyDescriptorWithTooltip extends PropertyDescriptor implements IKeepsTooltip
{

	private String tooltipText;
	private IProvidesTooltip tooltipProvider;

	public PropertyDescriptorWithTooltip(Object id, String displayName)
	{
		super(id, displayName);
	}

	@Override
	public String getTooltipText()
	{
		String tooltip = tooltipText;
		if (tooltipProvider != null) tooltip = tooltipProvider.getTooltipText();
		return tooltip;
	}

	@Override
	public void setTooltipText(String tooltipText)
	{
		this.tooltipText = tooltipText;
	}

	@Override
	public void setTooltipProvider(IProvidesTooltip tooltipProvider)
	{
		this.tooltipProvider = tooltipProvider;
	}

	@Override
	public String toString()
	{
		return "PropertyDescriptorWithTooltip:" + getId() + " - " + getDisplayName();
	}

}