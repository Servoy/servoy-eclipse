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
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;

/**
 * Label provider for Media, value may be Media of mediaId (Integer).
 * 
 * @author rgansevles
 * 
 */
public class MediaLabelProvider extends LabelProvider implements IPersistLabelProvider
{
	public static final MediaNode MEDIA_NODE_NONE = new MediaNode(Messages.LabelNone, null, MediaNode.TYPE.IMAGE, null);

	@Override
	public String getText(Object value)
	{
		if (value instanceof MediaNode)
		{
			return ((MediaNode)value).getName();
		}

		Media media = (Media)getPersist(value);

		if (media != null)
		{
			return media.getName();
		}

		return Messages.LabelUnresolved;
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof MediaNode)
		{
			return ((MediaNode)value).getMedia();
		}

		if (value instanceof Media)
		{
			return (Media)value;
		}

		return null;
	}
}
