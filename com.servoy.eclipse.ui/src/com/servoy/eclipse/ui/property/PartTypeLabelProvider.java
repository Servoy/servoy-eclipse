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
package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.Part;

/**
 * Label provider for Part type.
 * <p>
 * Value may be Part or Integer (part type).
 * 
 * @author rgansevles
 * 
 */
public class PartTypeLabelProvider extends LabelProvider
{
	public static final PartTypeLabelProvider INSTANCE = new PartTypeLabelProvider();

	@Override
	public String getText(Object value)
	{
		if (value instanceof Part)
		{
			String text = Part.getDisplayName(((Part)value).getPartType());
			if (((Part)value).isOverrideElement())
			{
				text += " (" + Messages.LabelOverride + ")";
			}
			return text;
		}
		if (value instanceof Integer)
		{
			return Part.getDisplayName(((Integer)value).intValue());
		}
		return Messages.LabelUnresolved;
	}
}
