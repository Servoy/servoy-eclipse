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
package com.servoy.eclipse.core.nature;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.model.util.ServoyLog;

public class ToggleNatureHandler extends AbstractHandler
{

	private final String natureID;
	private final String[] secondaryNatureIDs;

	/**
	 * Creates a new ToggleNatureHandler that toggles the project nature of the selected object with id natureID. If the project nature will be added, it makes
	 * sure that the secondary natures are also added. When deleting the project nature, the secondary natures will not be deleted.
	 *
	 * @param natureID the nature id of the nature to toggle.
	 * @param secondaryNatureIDs any natures that are required by the nature to be toggled.
	 */
	public ToggleNatureHandler(String natureID, String[] secondaryNatureIDs)
	{
		this.natureID = natureID;

		if (secondaryNatureIDs == null)
		{
			secondaryNatureIDs = new String[0];
		}
		this.secondaryNatureIDs = secondaryNatureIDs;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection instanceof IStructuredSelection)
		{
			IWorkbenchPart targetPart = HandlerUtil.getActivePart(event);
			for (Iterator< ? > it = ((IStructuredSelection)selection).iterator(); it.hasNext();)
			{
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject)
				{
					project = (IProject)element;
				}
				else if (element instanceof IAdaptable)
				{
					project = ((IAdaptable)element).getAdapter(IProject.class);
				}
				if (project != null)
				{
					if (project.isOpen())
					{
						if (MessageDialog.openConfirm(targetPart.getSite().getShell(), "Change nature",
							"Are you sure you want to change nature for project '" + project.getName() + "'?")) toggleNature(project);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Toggles sample nature on a project
	 *
	 * @param project to have sample nature added or removed
	 */
	protected void toggleNature(IProject project)
	{
		try
		{
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			ArrayList<String> newNatures = new ArrayList<String>();

			boolean removed = false;
			for (String nature : natures)
			{
				boolean neutralNature = true;
				if (natureID.equals(nature))
				{
					// Remove the nature
					neutralNature = false;
					removed = true;
				}
				else for (String secondaryNature : secondaryNatureIDs)
				{
					if (secondaryNature.equals(nature))
					{
						neutralNature = false;
						break;
					}
				}
				if (neutralNature)
				{
					newNatures.add(nature);
				}
			}

			if (!removed)
			{
				// Add the nature
				newNatures.add(natureID);
				for (String e : secondaryNatureIDs)
				{
					newNatures.add(e);
				}
			}

			description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
			project.setDescription(description, null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

}
