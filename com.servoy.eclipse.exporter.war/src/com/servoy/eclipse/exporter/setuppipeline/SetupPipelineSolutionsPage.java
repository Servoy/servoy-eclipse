/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.exporter.setuppipeline;


import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import com.servoy.eclipse.model.ServoyModelFinder;

/**
 * @author Diana
 *
 */
public class SetupPipelineSolutionsPage extends WizardPage
{

	private List nonActiveSolutionsList; // Keep reference for later use
	private Button includeNonActiveCheckbox;

	private Path activeSolutionParentDir;
	private String activeSolutionName;

	protected SetupPipelineSolutionsPage(String pageName)
	{
		super(pageName);
		setTitle("Solutions setup");
		setDescription("Select solutions to be included in the pipeline.");
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		// Checkbox: Include non-active solutions
		includeNonActiveCheckbox = new Button(container, SWT.CHECK);
		includeNonActiveCheckbox.setText("Include non-active solutions from the repository/workspace");

		nonActiveSolutionsList = new List(container, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true);
		listData.heightHint = 150; // Ensures enough space for multiple selections
		nonActiveSolutionsList.setLayoutData(listData);

		activeSolutionName = ServoyModelFinder.getServoyModel().getActiveProject().getProject().getName();
		IProject solutionProject = ServoyModelFinder.getServoyModel().getActiveProject().getProject();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IPath solutionDir = solutionProject.getLocation();
		activeSolutionParentDir = solutionDir.toPath().getParent();

		for (IProject project : root.getProjects())
		{
			IPath projectDir = project.getLocation();
			Path projectDirParent = projectDir.toPath().getParent();
			if (project.isOpen() && !project.getName().equals(activeSolutionName) && activeSolutionParentDir.equals(projectDirParent))
			{
				nonActiveSolutionsList.add(project.getName());
			}
		}

		setControl(container);
	}

	public Path getActiveSolutionParentDir()
	{
		return activeSolutionParentDir;
	}

	public String getActiveSolutionName()
	{
		return activeSolutionName;
	}

	/**
	 * Get selected solutions from the list.
	 */
	public String[] getSelectedSolutions()
	{
		return nonActiveSolutionsList.getSelection();
	}

	/**
	 * Check if checkbox is selected.
	 */
	public boolean isIncludeNonActiveSelected()
	{
		return includeNonActiveCheckbox.getSelection();
	}
}