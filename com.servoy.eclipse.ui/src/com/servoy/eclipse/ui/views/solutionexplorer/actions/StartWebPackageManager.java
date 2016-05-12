/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class StartWebPackageManager extends Action
{
	private final SolutionExplorerView viewer;

	/**
	 *	Opens the web packages manager app in a browser
	 */
	public StartWebPackageManager(SolutionExplorerView viewer)
	{
		super("Manage web packages");
		this.viewer = viewer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		try
		{
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new IEditorInput()
			{

				@Override
				public <T> T getAdapter(Class<T> adapter)
				{
					return null;
				}

				@Override
				public String getToolTipText()
				{
					return "Web Package Manager";
				}

				@Override
				public IPersistableElement getPersistable()
				{
					return null;
				}

				@Override
				public String getName()
				{
					return "Web Package Manager";
				}

				@Override
				public ImageDescriptor getImageDescriptor()
				{
					return null;
				}

				@Override
				public boolean exists()
				{
					return true;
				}

				@Override
				public boolean equals(Object obj)
				{
					if (obj instanceof IEditorInput) return ((IEditorInput)obj).getName().equals(this.getName());
					return super.equals(obj);
				}
			}, "com.servoy.eclipse.ui.webpackagemanager");
		}
		catch (PartInitException e)
		{
			Debug.log(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		return node.getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES;
	}


}
