/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.actions.OpenWithMenu;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.AdaptableWrapper;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.ISelectionContributionItem;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.util.Pair;

/**
 * @author lvostinar
 *
 */
public class OpenWithMediaFile extends OpenWithMenu implements ISelectionContributionItem
{
	private IFile mediaFile;
	private final AdaptableWrapper adaptableWrapper;

	public OpenWithMediaFile(AdaptableWrapper adaptableWrapper)
	{
		super(EditorUtil.getActivePage(), adaptableWrapper);
		this.adaptableWrapper = adaptableWrapper;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		mediaFile = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.MEDIA_IMAGE;
			if (state)
			{
				Media media = (Media)((SimpleUserNode)sel.getFirstElement()).getRealObject();
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(media, false);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				mediaFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				adaptableWrapper.setWrappedValue(mediaFile);
			}
		}
	}

	@Override
	public boolean isEnabled()
	{
		return mediaFile != null;
	}
}
