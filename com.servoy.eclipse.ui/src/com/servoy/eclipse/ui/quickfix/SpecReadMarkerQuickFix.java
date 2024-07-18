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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.RemovePackageProjectReferenceAction;

/**
 * @author emera
 */
public class SpecReadMarkerQuickFix implements IMarkerResolution
{
	private final IResource resource;

	public SpecReadMarkerQuickFix(IResource iResource)
	{
		this.resource = iResource;
	}

	@Override
	public String getLabel()
	{
		return "Delete the package";
	}

	@Override
	public void run(IMarker marker)
	{
		try
		{
			if (resource instanceof IProject)
			{
				IProject[] referencingProjects = ((IProject)resource).getReferencingProjects();
				for (IProject iProject : referencingProjects)
				{
					RemovePackageProjectReferenceAction.removeProjectReference(iProject, (IProject)resource);
				}
				resource.delete(true, new NullProgressMonitor());
				IProject resources = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject();
				resources.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			}
		}
		catch (final Exception e)
		{
			ServoyLog.logError(e);
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					org.eclipse.jface.dialogs.MessageDialog.openError(UIUtils.getActiveShell(), "Cannot delete package " + resource.getName(),
						e.getMessage());
				}
			});
		}
	}
}
