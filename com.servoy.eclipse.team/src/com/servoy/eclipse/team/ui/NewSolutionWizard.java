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

import java.rmi.UnmarshalException;
import java.util.Date;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.team.RepositoryAccessPoint;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITeamRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.SolutionMetaData;

public class NewSolutionWizard extends Wizard implements INewWizard
{
	private RepositoryWizardPage repositoryPage;
	private SolutionWizardPage solutionPage;

	private RepositoryAccessPoint repositoryAP;

	public class Result
	{
		private final String serverAddress;
		private final String user;
		private final String password;

		private final SolutionMetaData selectedSolutionMetaData;
		private final String selectedSolution;
		private final int selectedVersion;

		private final boolean checkoutModules;

		public Result(String serverAddress, String user, String password, SolutionMetaData selectedSolutionMetaData, String selectedSolution,
			int selectedVersion, boolean checkoutModules)
		{
			this.serverAddress = serverAddress;
			this.user = user;
			this.password = password;

			this.selectedSolutionMetaData = selectedSolutionMetaData;
			this.selectedSolution = selectedSolution;
			this.selectedVersion = selectedVersion;
			this.checkoutModules = checkoutModules;
		}

		public String getServerAddress()
		{
			return serverAddress;
		}

		public String getUser()
		{
			return user;
		}

		public String getPassword()
		{
			return password;
		}

		public SolutionMetaData getSelectedSolutionMetaData()
		{
			return selectedSolutionMetaData;
		}

		public String getSelectedSolution()
		{
			return selectedSolution;
		}

		public int getSelectedVersion()
		{
			return selectedVersion;
		}

		public boolean isCheckoutModules()
		{
			return checkoutModules;
		}

	}

	class SolutionInfo
	{
		private final RootObjectMetaData metaData;
		private final Date[] releaseDates;

		public SolutionInfo(RootObjectMetaData metaData, Date[] releaseDates)
		{
			this.metaData = metaData;
			this.releaseDates = releaseDates;
		}

		public RootObjectMetaData getMetaData()
		{
			return metaData;
		}

		public Date[] getReleaseDates()
		{
			return releaseDates;
		}
	}

	public NewSolutionWizard()
	{
		setWindowTitle("Checkout Servoy solution from repository");
	}

	private WorkspaceJob checkoutJob;
	private WorkspaceJob activateJob;

	// used in case the user wants to checkout a specific release, to warn him about the
	// not working commits on branches
	private boolean bConfirmedRelease;

	@Override
	public boolean performFinish()
	{
		repositoryPage.saveEnteredValues();

		String serverAddress = repositoryPage.getServerAddress();
		String user = repositoryPage.getUser();
		String password = repositoryPage.getPassword();

		SolutionMetaData selectedSolutionMetaData = (SolutionMetaData)solutionPage.getSelectedRootObjectMetaData();
		final String selectedSolution = solutionPage.getSelectedSolution();
		int selectedVersion = solutionPage.getSelectedVersion();
		boolean checkoutModules = solutionPage.shouldCheckoutModules();

		if (selectedVersion != -1)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					bConfirmedRelease = MessageDialog.openQuestion(
						getShell(),
						"Warning",
						"You choose to check out a specific release.\nThis means you will not be able to commit changes to it later.\nIf you want to make changes, check out HEAD instead\nContinue with current release ?");
				}
			});
			if (!bConfirmedRelease) return false;
		}

		final Result result = new Result(serverAddress, user, password, selectedSolutionMetaData, selectedSolution, selectedVersion, checkoutModules);

		if (checkoutJob != null)
		{
			checkoutJob.cancel();
		}
		checkoutJob = new WorkspaceJob("Checkout '" + selectedSolution + "'")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				IStatus checkoutJobStatus = Status.CANCEL_STATUS;
				try
				{
					ServoyTeamProvider.checkoutSelectedSolution(result);
					checkoutJobStatus = Status.OK_STATUS;
				}
				catch (final Exception ex)
				{
					ServoyLog.logError("Cannot checkout '" + selectedSolution + "'", ex);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(getShell(), "Error", "Cannot checkout '" + selectedSolution + "'.\n" + ex.getMessage());
						}
					});
				}
				return checkoutJobStatus;
			}
		};
		checkoutJob.setUser(true);
		ISchedulingRule rule = ServoyModel.getWorkspace().getRoot();
		checkoutJob.setRule(rule);
		checkoutJob.schedule();

		if (solutionPage.shouldActivateSolution())
		{
			activateJob = new WorkspaceJob("Activate '" + selectedSolution + "'")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					if (checkoutJob.getResult() == Status.OK_STATUS)
					{
						// make the solution active
						ServoyProject newServoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().refreshServoyProjects().getServoyProject(
							result.getSelectedSolution());
						ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(newServoyProject, true);
					}
					return Status.OK_STATUS;
				}

			};
			activateJob.setRule(rule);
			activateJob.schedule();
		}

		return true;
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer)
	{
		super.setContainer(wizardContainer);
		if (wizardContainer instanceof WizardDialog)
		{
			((WizardDialog)wizardContainer).addPageChangingListener(new IPageChangingListener()
			{

				public void handlePageChanging(PageChangingEvent event)
				{
					if (event.getTargetPage() == NewSolutionWizard.this.solutionPage) NewSolutionWizard.this.fillSolutionWizardPage();
				}
			});
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void addPages()
	{
		repositoryPage = new RepositoryWizardPage("Servoy", "Enter repository access information", null);
		solutionPage = new SolutionWizardPage("Servoy", "Select solution to checkout", null);
		addPage(repositoryPage);
		addPage(solutionPage);
	}

	private SolutionInfo[] repositorySolutionInfos;
	WorkspaceJob solutionsListJob;

	public void fillSolutionWizardPage()
	{
		String serverAddress = repositoryPage.getServerAddress();
		String user = repositoryPage.getUser();
		String password = repositoryPage.getPassword();

		repositoryAP = RepositoryAccessPoint.getInstance(serverAddress, user, password);

		if (solutionsListJob != null)
		{
			solutionsListJob.cancel();
		}

		solutionsListJob = new WorkspaceJob("Getting solutions")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				try
				{
					ITeamRepository repository = repositoryAP.getRepository();
					if (repository == null) throw new Exception("Team repository is not available.");
					RootObjectMetaData[] rootObjectMetaData = repository.getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
					repositorySolutionInfos = new SolutionInfo[rootObjectMetaData.length];
					for (int i = 0; i < rootObjectMetaData.length; i++)
					{
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						Date[] releaseDates = repository.getRootObjectReleaseDates(rootObjectMetaData[i].getRootObjectId());
						repositorySolutionInfos[i] = new SolutionInfo(rootObjectMetaData[i], releaseDates);
					}

					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							solutionPage.setRepositorySolutionInfos(repositorySolutionInfos);
							solutionPage.setDescription("Select solution to checkout");
						}
					});

				}
				catch (final Exception ex)
				{
					ServoyLog.logError("Cannot get solutions from repository", ex);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							String message;
							if (ex instanceof UnmarshalException) message = "The remote Servoy version is different from the local one";
							else message = ex.getMessage();
							NewSolutionWizard.this.getContainer().showPage(NewSolutionWizard.this.repositoryPage);
							MessageDialog.openError(getShell(), "Error", "Cannot get solutions from : " + repositoryPage.getServerAddress() + "\n" + message);
						}
					});
				}
				return Status.OK_STATUS;
			}
		};

		solutionPage.setDescription("Loading solutions ...");
		solutionPage.setRepositorySolutionInfos(new SolutionInfo[0]);
		ISchedulingRule rule = new SerialRule();
		solutionsListJob.setRule(rule);
		solutionsListJob.schedule();
	}

	@Override
	public boolean canFinish()
	{
		return getContainer().getCurrentPage().equals(solutionPage) && solutionPage.canFinish();
	}

	@Override
	public boolean performCancel()
	{
		if (solutionsListJob != null)
		{
			solutionsListJob.cancel();
		}
		return true;
	}
}