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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.repository.RepositoryAccessPoint;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.util.Utils;

public class SharingWizard extends Wizard implements IConfigurationWizard
{

	private RepositoryWizardPage repositoryPage;
	private IProject servoyProject;

	@Override
	public void addPages()
	{
		repositoryPage = new RepositoryWizardPage("Servoy", "Repository", null);
		addPage(repositoryPage);
	}

	private WorkspaceJob sharingJob;
	private boolean shareWithExistingName;

	@Override
	public boolean performFinish()
	{
		repositoryPage.saveEnteredValues();
		final String serverAddress = repositoryPage.getServerAddress();
		final String user = repositoryPage.getUser();
		final String passHash = Utils.calculateMD5HashBase64(repositoryPage.getPassword());

		if (sharingJob != null)
		{
			sharingJob.cancel();
		}

		sharingJob = new WorkspaceJob("Sharing '" + servoyProject.getName() + "'")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				try
				{
					RepositoryAccessPoint repositoryAP = RepositoryAccessPoint.getInstance(serverAddress, user, passHash);

					if (servoyProject.hasNature(ServoyProject.NATURE_ID)) // servoy project
					{
						RootObjectMetaData solutionMetaData = repositoryAP.getRepository().getRootObjectMetaData(servoyProject.getName(), IRepository.SOLUTIONS);
						if (solutionMetaData == null)
						{

							String resourceProjectName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject().getName();

							ServoyTeamProvider.createResourcesProject(resourceProjectName, repositoryAP, serverAddress, user, passHash);
							ServoyTeamProvider.createSolutionProject(repositoryAP, serverAddress, user, passHash, servoyProject.getName(), 1, null, null, false);
						}
						else
						{

							Display.getDefault().syncExec(new Runnable()
							{
								public void run()
								{
									shareWithExistingName = MessageDialog.openQuestion(getShell(), "Servoy share", "Solution with name '" +
										servoyProject.getName() + "' already exists in the repository. Share anyway ?");
								}
							});
							if (shareWithExistingName)
							{
								ServoyTeamProvider.createSolutionProject(repositoryAP, serverAddress, user, passHash, servoyProject.getName(), 1, null, null,
									false);
							}
						}
					}
					else if (servoyProject.hasNature(ServoyResourcesProject.NATURE_ID)) // style project
					{
						ServoyTeamProvider.createResourcesProject(servoyProject.getName(), repositoryAP, serverAddress, user, passHash);
					}
					
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							ResourceDecorator rd = (ResourceDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(
								ResourceDecorator.ID);
							rd.fireChanged(null);
						}
					});
				}
				catch (final Exception ex)
				{
					ServoyLog.logError("Cannot share '" + servoyProject.getName() + "'", ex);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(getShell(), "Error", "Cannot share '" + servoyProject.getName() + "'.\n" + ex.getMessage());
						}
					});
				}

				return Status.OK_STATUS;
			}

		};
		sharingJob.setUser(true);
		ISchedulingRule rule = new SerialRule();
		sharingJob.setRule(rule);
		sharingJob.schedule();


		return true;
	}

	public void init(IWorkbench workbench, IProject project)
	{
		servoyProject = project;
	}
}
