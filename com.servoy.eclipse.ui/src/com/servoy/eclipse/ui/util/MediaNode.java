/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * Class used to represent a solution media folder or image in the developer
 * 
 * @author gboros
 *
 */
public class MediaNode
{
	public static enum TYPE
	{
		FOLDER, IMAGE
	}

	private final String name;
	private final String path;
	private final String info;
	private final TYPE type;
	private final Media media;

	private final IMediaProvider mediaProvider;

	public MediaNode(String name, String path, TYPE type, IMediaProvider mediaProvider)
	{
		this.name = name;
		this.path = path;
		this.type = type;
		this.info = null;
		this.mediaProvider = mediaProvider;
		this.media = null;
	}

	public MediaNode(String name, String path, TYPE type, IMediaProvider mediaProvider, String info, Media media)
	{
		this.name = name;
		this.path = path;
		this.type = type;
		this.info = info;
		this.mediaProvider = mediaProvider;
		this.media = media;
	}

	public String getName()
	{
		return name;
	}

	public String getPath()
	{
		return path;
	}

	public TYPE getType()
	{
		return type;
	}

	public String getInfo()
	{
		return info;
	}

	public IMediaProvider getMediaProvider()
	{
		return mediaProvider;
	}

	public Media getMedia()
	{
		return media;
	}

	public MediaNode getParent()
	{
		if (path.length() > 0)
		{
			int idx = path.length() - 1;
			if (path.charAt(idx) == '/') idx--;

			while (idx > -1 && path.charAt(idx) != '/')
				idx--;
			if (idx > 0)
			{
				int nameIdx = idx - 1;
				while (nameIdx > -1 && path.charAt(nameIdx) != '/')
					nameIdx--;
				if (nameIdx < 0) nameIdx = 0;
				else nameIdx++;

				return new MediaNode(path.substring(nameIdx, idx), path.substring(0, idx + 1), TYPE.FOLDER, mediaProvider);
			}
		}
		return null;
	}

	public boolean hasChildren(EnumSet<TYPE> mediaNodeFilter)
	{
		MediaNode[] children = getChildren(mediaNodeFilter);

		if (children != null) return children.length > 0;
		else return false;
	}

	public MediaNode[] getChildren(EnumSet<TYPE> mediaNodeFilter)
	{
		if (getType() == TYPE.IMAGE) return null;

		List<MediaNode> childrenNodes = new ArrayList<MediaNode>();
		MediaNode node = null;

		String mediaFolder = getPath();
		for (Media mediaItem : Utils.asList(mediaProvider.getMedias(false)))
		{
			String mediaName = mediaItem.getName();

			if (mediaName != null && (mediaFolder == null || mediaName.startsWith(mediaFolder)))
			{
				String mediaPath = mediaFolder == null ? mediaName : mediaName.substring(mediaFolder.length());
				int pathSepIdx = mediaPath.indexOf('/');
				if (pathSepIdx != -1) // it is a directory
				{
					if (mediaNodeFilter.contains(TYPE.FOLDER))
					{
						String dirName = mediaPath.substring(0, pathSepIdx);
						node = new MediaNode(dirName, ((mediaFolder == null ? "" //$NON-NLS-1$
							: mediaFolder) + dirName + '/'), TYPE.FOLDER, mediaProvider);
					}
				}
				else
				{
					if (mediaNodeFilter.contains(TYPE.IMAGE))
					{
						node = new MediaNode(mediaPath, mediaName, TYPE.IMAGE, mediaProvider, "\"media:///" + mediaName + "\"", mediaItem); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				if (node != null && childrenNodes.indexOf(node) == -1) childrenNodes.add(node);
			}
		}

		if (mediaNodeFilter.contains(TYPE.FOLDER))
		{
			// read directories that are only present in the workspace
			Solution mediaProviderSolution = null;
			if (mediaProvider instanceof Solution)
			{
				mediaProviderSolution = (Solution)mediaProvider;
			}
			else if (mediaProvider instanceof FlattenedSolution)
			{
				mediaProviderSolution = ((FlattenedSolution)mediaProvider).getSolution();
			}

			if (mediaProviderSolution != null)
			{
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(mediaProviderSolution.getName());
				IFolder mediasFolder = (IFolder)servoyProject.getProject().findMember(
					new Path(SolutionSerializer.MEDIAS_DIR + (mediaFolder != null ? "/" + mediaFolder : ""))); //$NON-NLS-1$ //$NON-NLS-2$
				if (mediasFolder != null)
				{
					try
					{
						for (IResource mediasFolderChild : mediasFolder.members())
						{
							if (mediasFolderChild.getType() == IResource.FOLDER)
							{
								final boolean[] hasFile = new boolean[] { false };
								mediasFolderChild.accept(new IResourceVisitor()
								{
									public boolean visit(IResource resource) throws CoreException
									{
										if (resource.getType() == IResource.FILE)
										{
											hasFile[0] = true;
											return false;
										}
										return true;
									}

								});
								if (!hasFile[0])
								{
									String dirName = mediasFolderChild.getName();
									node = new MediaNode(dirName, ((mediaFolder == null ? "" : mediaFolder) + dirName + '/'), TYPE.FOLDER, mediaProvider); //$NON-NLS-1$
									childrenNodes.add(node);
								}
							}
						}
					}
					catch (CoreException ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}

		MediaNode[] mediaNodeA = childrenNodes.toArray(new MediaNode[childrenNodes.size()]);
		Arrays.sort(mediaNodeA, mediaNodeComparator);

		return mediaNodeA;
	}

	@Override
	public int hashCode()
	{
		if (path == null) return 0;
		else return path.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		return path != null ? path.equals(((MediaNode)obj).getPath()) : ((MediaNode)obj).getPath() == null;
	}

	private static final MediaNodeComparator mediaNodeComparator = new MediaNodeComparator();

	static class MediaNodeComparator implements Comparator<MediaNode>
	{
		private MediaNodeComparator()
		{
		}

		public int compare(MediaNode o1, MediaNode o2)
		{
			if (o1.getType() == o2.getType())
			{
				return o1.getName().compareTo(o2.getName());
			}
			else if (o1.getType() == TYPE.FOLDER)
			{
				return -1;
			}
			else if (o2.getType() == TYPE.FOLDER)
			{
				return 1;
			}

			return 0;
		}
	}
}