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
package com.servoy.eclipse.ui.views.solutionexplorer;

import java.rmi.RemoteException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.scripting.CalculationModeHandler;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

public class CalculationTracker implements ActiveEditorListener
{

	private final SolutionExplorerTreeContentProvider treeContentProvider;

	public CalculationTracker(SolutionExplorerTreeContentProvider treeContentProvider)
	{
		this.treeContentProvider = treeContentProvider;
	}

	public void activeEditorChanged(IEditorPart newActiveEditor)
	{
		boolean calculationIdentified = false;
		if (newActiveEditor instanceof ScriptEditor)
		{
			try
			{
				// see if it is a calculation JS file
				ScriptEditor scriptEditor = (ScriptEditor)newActiveEditor;
				IResource resource = null;
				if (scriptEditor.getInputModelElement() != null && scriptEditor.getInputModelElement().getResource() != null &&
					scriptEditor.getInputModelElement().getResource().exists())
				{
					resource = scriptEditor.getInputModelElement().getUnderlyingResource();
				}

				String[] serverTablename = SolutionSerializer.getDataSourceForCalculationJSFile(resource);
				if (serverTablename != null)
				{
					// seems to be a calculation - find it's Table object
					IProject project = resource.getProject();
					Solution solution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(project.getName()).getSolution();
					if (solution != null)
					{
						IServer server = solution.getServer(serverTablename[0]);
						if (server != null)
						{
							ITable tableOfCalculation = server.getTable(serverTablename[1]);
							if (tableOfCalculation != null)
							{
								CalculationModeHandler.getInstance().setCalculationMode(true);
								// we found what we need to change the contents of the tree
								treeContentProvider.startCalculationMode(solution, tableOfCalculation);
								calculationIdentified = true;
							}
						}
					}
				}
			}
			catch (ModelException e)
			{
				ServoyLog.logError("Editor exception while trying to determine if JS editor is showing a calculation", e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Repository exception while trying to get server from calculation editor", e);
			}
			catch (RemoteException e)
			{
				ServoyLog.logError("Repository exception while trying to get table from calculation editor", e);
			}
		}
		if (!calculationIdentified)
		{
			CalculationModeHandler.getInstance().setCalculationMode(false);
			treeContentProvider.stopCalculationMode();
		}
	}

}