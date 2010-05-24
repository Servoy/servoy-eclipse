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
package com.servoy.eclipse.ui.labelproviders;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;

/**
 * Label provider for Media, value may be Media of mediaId (Integer).
 * 
 * @author rob
 * 
 */
public class MediaLabelProvider extends LabelProvider implements IPersistLabelProvider
{
	public static final int MEDIA_NONE = 0;

	private final FlattenedSolution flattenedSolution;

	public MediaLabelProvider(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null)
		{
			return Messages.LabelNone;
		}

		if (value instanceof Integer && ((Integer)value).intValue() == MEDIA_NONE)
		{
			return Messages.LabelNone;
		}
		Media media = (Media)getPersist(value);

		if (media == null)
		{
			return Messages.LabelUnresolved;
		}

		return media.getName();
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof Integer)
		{
			int mediaId = ((Integer)value).intValue();
			if (mediaId == MEDIA_NONE)
			{
				return null;
			}
			return flattenedSolution.getMedia(mediaId);
		}
		if (value instanceof Media)
		{
			return (Media)value;
		}

		return null;
	}
}
