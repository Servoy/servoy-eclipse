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
package com.servoy.eclipse.team.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.team.core.RepositoryProvider;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.team.ServoyTeamProvider;

public class SynchronizeWizardPage extends WizardPage
{
	public static final String NAME = "SynchronizeWizardPage";
	private List projectsList;


	protected SynchronizeWizardPage(String title, String description, ImageDescriptor titleImage)
	{
		super(SynchronizeWizardPage.NAME, title, titleImage);
		setDescription(description);
	}

	public void createControl(Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new FillLayout());
		projectsList = new List(composite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		fillProjectsList(projectsList);
		setControl(composite);
	}

	private void fillProjectsList(List list)
	{
		ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects();

		for (ServoyProject project : projects)
		{
			if (RepositoryProvider.getProvider(project.getProject()) instanceof ServoyTeamProvider) list.add(project.getSolution().getName());
		}
	}

	public ServoyProject[] getSelectedProjects()
	{
		int[] selections = projectsList.getSelectionIndices();
		ServoyProject[] selectedProjects = new ServoyProject[selections.length];

		int i = 0;
		for (int selectedIdx : selections)
		{
			selectedProjects[i++] = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(projectsList.getItem(selectedIdx));
		}

		return selectedProjects;
	}
}
