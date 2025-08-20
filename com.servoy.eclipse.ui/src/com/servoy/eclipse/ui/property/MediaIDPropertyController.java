/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Media;

/**
 * Property controller for media id based properties.
 * @author acostescu
 */
public class MediaIDPropertyController extends MediaPropertyController<String>
{

	protected final FlattenedSolution editingFlattenedSolution;

	public MediaIDPropertyController(Object id, String displayName, PersistContext persistContext, FlattenedSolution editingFlattenedSolution,
		boolean includeNone, com.servoy.eclipse.ui.property.MediaPropertyController.MediaPropertyControllerConfig config)
	{
		super(id, displayName, persistContext, includeNone, config);
		this.editingFlattenedSolution = editingFlattenedSolution;
	}

	@Override
	protected IPropertyConverter<String, MediaNode> createConverter()
	{
		// convert between media node and image name
		return new IPropertyConverter<String, MediaNode>()
		{
			public MediaNode convertProperty(@SuppressWarnings("hiding") Object id, String value)
			{
				if (value != null)
				{
					Media media = editingFlattenedSolution.getMedia(value);
					if (media != null)
					{
						String mediaFullPath = media.getName();//this may include the path
						String mediaName = mediaFullPath != null && mediaFullPath.indexOf("/") > 0 ? mediaFullPath.substring(mediaFullPath.lastIndexOf("/") + 1)
							: mediaFullPath;
						return new MediaNode(mediaName, mediaFullPath, MediaNode.TYPE.IMAGE, editingFlattenedSolution.getSolution(), null, media);
					}
					return MediaLabelProvider.MEDIA_NODE_UNRESOLVED;
				}
				return MediaLabelProvider.MEDIA_NODE_NONE;
			}

			public String convertValue(@SuppressWarnings("hiding") Object id, MediaNode value)
			{
				return value == null || value == MediaLabelProvider.MEDIA_NODE_NONE ? null : value.getMedia().getUUID().toString();
			}
		};
	}

}
