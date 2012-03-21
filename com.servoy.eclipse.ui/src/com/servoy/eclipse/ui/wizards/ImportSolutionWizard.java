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
package com.servoy.eclipse.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.IValidator;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectChooserComposite;
import com.servoy.eclipse.core.repository.EclipseImportUserChannel;
import com.servoy.eclipse.core.repository.XMLEclipseWorkspaceImportHandlerVersions11AndHigher;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;

public class ImportSolutionWizard extends Wizard implements IImportWizard
{
	private ImportSolutionWizardPage page;
	private SolutionImportedPage finishPage;
	private String importMessageDetails;

	@Override
	public boolean performFinish()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	@Override
	public boolean canFinish()
	{
		if (finishPage.canFinish()) return true;
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setNeedsProgressMonitor(true);
		page = new ImportSolutionWizardPage(this, "Import solution"); //$NON-NLS-1$
		finishPage = new SolutionImportedPage("Solution imported"); //$NON-NLS-1$
	}

	@Override
	public void addPages()
	{
		addPage(page);
		addPage(finishPage);
	}

	public static String initialPath = getInitialImportPath();
	private String solutionFilePath;
	private boolean askForImportServerName;

	private static String getInitialImportPath()
	{
		String as_dir = ApplicationServerSingleton.get().getServoyApplicationServerDirectory().replace("\\", "/").replace("//", "/"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (!as_dir.endsWith("/")) as_dir += "/"; //$NON-NLS-1$//$NON-NLS-2$
		return as_dir + "solutions/examples"; //$NON-NLS-1$
	}

	public void setSolutionFilePath(String solutionFilePath)
	{
		this.solutionFilePath = solutionFilePath;
	}

	public String getSolutionFilePath()
	{
		return solutionFilePath;
	}

	public void setAskForImportServerName(boolean askForImportServerName)
	{
		this.askForImportServerName = askForImportServerName;
	}

	public boolean shouldAskForImportServerName()
	{
		return askForImportServerName;
	}

	public class ImportSolutionWizardPage extends WizardPage implements IValidator
	{
		private ResourcesProjectChooserComposite resourceProjectComposite;
		private Text filePath;
		private Button browseButton;
		private Button cleanImport;
		private Button allowDataModelChange;
		private Button displayDataModelChanges;
		private Button activateSolution;

		private final ImportSolutionWizard wizard;

		protected ImportSolutionWizardPage(ImportSolutionWizard wizard, String pageName)
		{
			super(pageName);
			this.wizard = wizard;
			setTitle("Import a solution"); //$NON-NLS-1$
			setDescription("A solution (with or without modules) will be imported into the workspace from a .servoy file."); //$NON-NLS-1$
		}

		public void createControl(Composite parent)
		{
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);

			filePath = new Text(topLevel, SWT.BORDER);
			filePath.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					validate();
					wizard.getContainer().updateButtons();
				}
			});

			String solutionFilePath = wizard.getSolutionFilePath();
			if (solutionFilePath != null) filePath.setText(solutionFilePath);

			browseButton = new Button(topLevel, SWT.NONE);
			browseButton.setText("Browse"); //$NON-NLS-1$
			browseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
					dlg.setFilterExtensions(new String[] { "*.servoy" }); //$NON-NLS-1$
					if (initialPath != null) dlg.setFilterPath(initialPath);
					if (dlg.open() != null)
					{
						initialPath = dlg.getFilterPath();
						filePath.setText(dlg.getFilterPath() + File.separator + dlg.getFileName());
					}
				}
			});
			cleanImport = new Button(topLevel, SWT.CHECK);
			cleanImport.setText("Clean Import"); //$NON-NLS-1$

			allowDataModelChange = new Button(topLevel, SWT.CHECK);
			allowDataModelChange.setText("Allow data model (database) changes"); //$NON-NLS-1$
			allowDataModelChange.setSelection(true);

			displayDataModelChanges = new Button(topLevel, SWT.CHECK);
			displayDataModelChanges.setText("Display data model (database) changes"); //$NON-NLS-1$

			activateSolution = new Button(topLevel, SWT.CHECK);
			activateSolution.setText("Activate solution after import"); //$NON-NLS-1$
			activateSolution.setSelection(true);

			resourceProjectComposite = new ResourcesProjectChooserComposite(topLevel, SWT.NONE, this,
				"Please choose the resources project the solution will reference (for styles, column/sequence info, security)", //$NON-NLS-1$
				ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject());

			// layout of the page
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 5;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			topLevel.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(browseButton, 0, SWT.CENTER);
			formData.right = new FormAttachment(100, -100);
			filePath.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(filePath, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			browseButton.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(filePath, 0, SWT.LEFT);
			formData.top = new FormAttachment(filePath, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			cleanImport.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(cleanImport, 0, SWT.LEFT);
			formData.top = new FormAttachment(cleanImport, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			allowDataModelChange.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(allowDataModelChange, 10, SWT.LEFT);
			formData.top = new FormAttachment(allowDataModelChange, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			displayDataModelChanges.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(cleanImport, 0, SWT.LEFT);
			formData.top = new FormAttachment(displayDataModelChanges, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			activateSolution.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(activateSolution, 10);
			formData.bottom = new FormAttachment(100, 0);
			resourceProjectComposite.setLayoutData(formData);
		}

		private boolean doSolutionImport()
		{
			importMessageDetails = ""; //$NON-NLS-1$
			final File file = new File(page.getPath());
			if (!file.exists() || !file.isFile())
			{
				finishPage.setErrorMessage("Import failed"); //$NON-NLS-1$
				finishPage.setTitle("Solution not imported"); //$NON-NLS-1$
				importMessageDetails = "Could not import solution: Invalid file"; //$NON-NLS-1$
				return false;
			}
			if (!file.canRead())
			{
				finishPage.setErrorMessage("Import failed"); //$NON-NLS-1$
				finishPage.setTitle("Solution not imported"); //$NON-NLS-1$
				importMessageDetails = "Could not import solution: File cannot be read"; //$NON-NLS-1$
				return false;
			}
			if (page.getErrorMessage() != null)
			{
				finishPage.setErrorMessage("Import failed"); //$NON-NLS-1$
				finishPage.setTitle("Solution not imported"); //$NON-NLS-1$
				importMessageDetails = "Could not import solution: " + page.getErrorMessage(); //$NON-NLS-1$
				return false;
			}
			final String resourcesProjectName = page.getNewName();
			final ServoyResourcesProject existingProject = page.getResourcesProject();
			final boolean isCleanImport = page.isCleanImport();
			final boolean allowDataModelChanges = page.getAllowDataModelChange();
			final boolean doDisplayDataModelChanges = page.getDisplayDataModelChange();
			final boolean doActivateSolution = page.getActivateSolution();
			IRunnableWithProgress runnable = new IRunnableWithProgress()
			{
				public void run(IProgressMonitor monitor)
				{
					final EclipseImportUserChannel userChannel = new EclipseImportUserChannel(allowDataModelChanges, doDisplayDataModelChanges, getShell());
					IApplicationServerSingleton as = ApplicationServerSingleton.get();
					try
					{
						IXMLImportEngine importEngine = as.createXMLImportEngine(file, (EclipseRepository)ServoyModel.getDeveloperRepository(),
							as.getDataServer(), as.getClientId(), userChannel);

						IXMLImportHandlerVersions11AndHigher x11handler = as.createXMLInMemoryImportHandler(importEngine.getVersionInfo(), as.getDataServer(),
							as.getClientId(), userChannel, (EclipseRepository)ServoyModel.getDeveloperRepository());

						x11handler.setAskForImportServerName(ImportSolutionWizard.this.shouldAskForImportServerName());

						IRootObject[] rootObjects = XMLEclipseWorkspaceImportHandlerVersions11AndHigher.importFromJarFile(importEngine, x11handler,
							userChannel, (EclipseRepository)ServoyModel.getDeveloperRepository(), resourcesProjectName, existingProject, monitor,
							doActivateSolution, isCleanImport);
						if (rootObjects != null)
						{
							String detail = userChannel.getAllImportantMSGes() + "\nSolution '" + rootObjects[0].getName() + "' imported"; //$NON-NLS-1$ //$NON-NLS-2$
							if (doActivateSolution) detail += " and activated"; //$NON-NLS-1$
							detail += "."; //$NON-NLS-1$
							importMessageDetails = detail;
						}
					}
					catch (final RepositoryException ex)
					{
						// Don't show an error message if the import was canceled.
						if (!ex.hasErrorCode(ServoyException.InternalCodes.OPERATION_CANCELLED))
						{
							// Don't show an stack trace for CRC related messages.
							if (!ex.hasErrorCode(ServoyException.InternalCodes.CHECKSUM_FAILURE))
							{
								ServoyLog.logError(ex);
							}
							finishPage.setErrorMessage("Import failed"); //$NON-NLS-1$
							finishPage.setTitle("Solution not imported"); //$NON-NLS-1$
							importMessageDetails = "Could not import solution: " + ex.getMessage(); //$NON-NLS-1$
						}
					}
					catch (final Exception ex)
					{
						ServoyLog.logError(ex);
						String msg = "An unexpected error occured"; //$NON-NLS-1$
						if (ex.getMessage() != null) msg += ex.getMessage();
						else msg += ". Check the log for more details."; //$NON-NLS-1$
						final String mymsg = msg;
						finishPage.setErrorMessage("Import failed"); //$NON-NLS-1$
						finishPage.setTitle("Solution not imported"); //$NON-NLS-1$
						importMessageDetails = "Could not import solution: " + mymsg; //$NON-NLS-1$

					}
				}
			};
			try
			{
				getContainer().run(true, false, runnable);
			}
			catch (InvocationTargetException e)
			{
				ServoyLog.logError(e);
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError(e);
			}
			return true;
		}

		public String validate()
		{
			String error = null;
			if (filePath.getText().trim().length() == 0)
			{
				error = "Please select servoy file to import."; //$NON-NLS-1$
			}
			else if (resourceProjectComposite != null) error = resourceProjectComposite.validate();
			setErrorMessage(error);
			return error;
		}

		public String getPath()
		{
			return filePath.getText();
		}

		public ServoyResourcesProject getResourcesProject()
		{
			return resourceProjectComposite.getExistingResourceProject();
		}

		public String getNewName()
		{
			return resourceProjectComposite.getNewResourceProjectName();
		}

		public boolean isCleanImport()
		{
			return cleanImport.getSelection();
		}

		public boolean getAllowDataModelChange()
		{
			return allowDataModelChange.getSelection();
		}

		public boolean getDisplayDataModelChange()
		{
			return displayDataModelChanges.getSelection();
		}

		public boolean getActivateSolution()
		{
			return activateSolution.getSelection();
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return validate() == null;
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (canFlipToNextPage())
			{
				doSolutionImport();
				finishPage.setTextMessage(importMessageDetails != null ? importMessageDetails : ""); //$NON-NLS-1$
			}
			return finishPage;
		}
	}

	private class SolutionImportedPage extends WizardPage implements IValidator
	{
		private Text message;

		protected SolutionImportedPage(String pageName)
		{
			super(pageName);
			setTitle(pageName);
		}

		public void setTextMessage(String msg)
		{
			message.setText(msg);
		}

		public void createControl(Composite parent)
		{
			Composite container = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 1;

			message = new Text(container, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalSpan = 1;
			message.setLayoutData(gridData);
			message.setEditable(false);

			setControl(container);
			setPageComplete(true);
		}

		public String validate()
		{
			return null;
		}

		public boolean canFinish()
		{
			return finishPage.isCurrentPage();
		}

		@Override
		public IWizardPage getPreviousPage()
		{
			return null;
		}
	}

}
