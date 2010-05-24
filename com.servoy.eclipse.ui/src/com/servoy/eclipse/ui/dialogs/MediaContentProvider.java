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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Media;

/**
 * Content provider class for media.
 * 
 * @author rob
 * 
 */

public class MediaContentProvider extends FlatTreeContentProvider
{
	private final FlattenedSolution flattenedSolution;

	public MediaContentProvider(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof MediaListOptions)
		{
			MediaListOptions options = (MediaListOptions)inputElement;

			List<Integer> mediaIds = new ArrayList<Integer>();
			if (options.includeNone)
			{
				mediaIds.add(new Integer(MediaLabelProvider.MEDIA_NONE));
			}

			Iterator<Media> it = flattenedSolution.getMedias(true);
			while (it.hasNext())
			{
				mediaIds.add(new Integer(it.next().getID()));
			}

			return mediaIds.toArray();
		}
		return new Object[0];
	}

	public static class MediaListOptions
	{
		public final boolean includeNone;

		public MediaListOptions(boolean includeNone)
		{
			this.includeNone = includeNone;
		}
	}

}
