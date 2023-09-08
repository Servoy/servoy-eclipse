/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewScopeAction;
import com.servoy.j2db.persistence.Solution;

/**
 * @author jcompagner
 *
 */
public class RenameScopeNameQuickFix implements IMarkerResolution2
{

	private final IResource resource;

	/**
	 * @param resource
	 */
	public RenameScopeNameQuickFix(IResource resource)
	{
		this.resource = resource;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel()
	{
		return "Rename the scope";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker)
	{
		ServoyProject project = ServoyModelFinder.getServoyModel().getServoyProject(resource.getProject().getName());

		String oldname = resource.getName().substring(0, resource.getName().length() - SolutionSerializer.JS_FILE_EXTENSION.length());
		String scopeName = NewScopeAction.askScopeName(UIUtils.getActiveShell(), oldname, project);
		if (scopeName == null || scopeName.equals(oldname))
		{
			return;
		}

		Solution solution = project.getSolution();

		WorkspaceFileAccess wsfa = new WorkspaceFileAccess(project.getProject().getWorkspace());
		String oldScriptPath = SolutionSerializer.getRelativePath(solution, false) + oldname + SolutionSerializer.JS_FILE_EXTENSION;
		String newScriptPath = SolutionSerializer.getRelativePath(solution, false) + scopeName + SolutionSerializer.JS_FILE_EXTENSION;
		// if the file isn't there, create it here so that the formatter sees the js file.
		if (!wsfa.exists(newScriptPath))
		{
			// file doesn't exist, create the file and its parent directories
			try
			{
				wsfa.move(oldScriptPath, newScriptPath);
			}
			catch (IOException e)
			{
				ServoyLog.logError("Could not rename global scope " + scopeName + " in project  " + project, e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription()
	{
		return getLabel();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage()
	{
		return Activator.getDefault().loadImageFromBundle("scopes.png");
	}

}
