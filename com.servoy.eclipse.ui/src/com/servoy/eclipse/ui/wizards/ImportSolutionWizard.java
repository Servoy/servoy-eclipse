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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.IValidator;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectChooserComposite;
import com.servoy.eclipse.core.repository.EclipseImportUserChannel;
import com.servoy.eclipse.core.repository.XMLEclipseWorkspaceImportHandlerVersions11AndHigher;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.WebPackagesListener;
import com.servoy.eclipse.ui.svygen.AISolutionGenerator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.CryptUtils;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

public class ImportSolutionWizard extends Wizard implements IImportWizard
{
	private ImportSolutionWizardPage page;
	private String titleText;

	public ImportSolutionWizard()
	{
		// default
	}

	public ImportSolutionWizard(String titleText)
	{
		this.titleText = titleText;
	}

	@Override
	public boolean performFinish()
	{
		return doSolutionImport();
	}

	@Override
	public boolean canFinish()
	{
		return page.canFinish();
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setNeedsProgressMonitor(true);
		setWindowTitle("Import solution wizard");
		page = new ImportSolutionWizardPage(this, "Import solution");
	}

	@Override
	public void addPages()
	{
		addPage(page);
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	public static String initialPath = getInitialImportPath();
	private String solutionFilePath;
	private boolean allowSolutionFilePathSelection = true;
	private boolean askForImportServerName;
	private boolean activateSolution = true;
	private boolean reportImportFail;
	private boolean shouldSkipModulesImport = false;
	private final HashMap<String, Integer> existingSolutionAction = new HashMap<String, Integer>();
	private boolean allowDataModelChanges = false;
	private boolean importSampleData = false;
	private Boolean importDatasources;
	private boolean allowSQLKeywords;
	private boolean createMissingServer = false;
	private String isMissingServer;
	private boolean shouldOverwriteModule = false;
	private boolean showFinishDialog = true;

	private static String getInitialImportPath()
	{
		String as_dir = ApplicationServerRegistry.get().getServoyApplicationServerDirectory().replace("\\", "/").replace("//", "/");
		if (!as_dir.endsWith("/")) as_dir += "/";
		return as_dir + "solutions/examples";
	}

	public void setSolutionFilePath(String solutionFilePath)
	{
		this.solutionFilePath = solutionFilePath;
	}

	public String getSolutionFilePath()
	{
		return solutionFilePath;
	}

	public void setAllowSolutionFilePathSelection(boolean allowSolutionFilePathSelection)
	{
		this.allowSolutionFilePathSelection = allowSolutionFilePathSelection;
	}

	public void setAskForImportServerName(boolean askForImportServerName)
	{
		this.askForImportServerName = askForImportServerName;
	}

	public boolean shouldAskForImportServerName()
	{
		return askForImportServerName;
	}

	public void setSkipModulesImport(boolean skip)
	{
		this.shouldSkipModulesImport = skip;
	}

	public boolean shouldSkipModulesImport()
	{
		return shouldSkipModulesImport;
	}

	public void setOverwriteModule(boolean overwrite)
	{
		this.shouldOverwriteModule = overwrite;
	}

	public boolean shouldOverwriteModule()
	{
		return shouldOverwriteModule;
	}

	protected String getFirstPageTitle()
	{
		return titleText;
	}

	public void setActivateSolution(boolean activateSolution)
	{
		this.activateSolution = activateSolution;
	}

	public void setReportImportFail(boolean reportImportFail)
	{
		this.reportImportFail = reportImportFail;
	}

	/**
	 * AES Decryption of the specified file and write the output in a temporary file.
	 *
	 * @return file
	 *
	 */
	private File fileDecryption(File file)
	{
		String password = null;
		if (CryptUtils.checkEncryption(file))
		{
			password = UIUtils.showPasswordDialog(getShell(), "This solution is password protected", "Please enter protection password:", "", null);
		}
		else return file;

		return CryptUtils.fileDecryption(file, password);
	}


	/**
	 * @param file
	 * @param resourcesProjectName
	 * @param existingProject
	 * @param isCleanImport
	 * @param doDisplayDataModelChanges
	 * @param doActivateSolution
	 */
	public void doImport(final File file, final String resourcesProjectName, final ServoyResourcesProject existingProject, final boolean isCleanImport,
		final boolean doDisplayDataModelChanges, final boolean doActivateSolution, String projectLocation, IRunnableContext context, IProgressMonitor mon,
		final boolean forceActivateResourcesProject, final boolean keepResourcesProjectOpen, final Set<IProject> projectsToDeleteAfterImport)
	{
		IRunnableWithProgress runnable = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor)
			{
				final EclipseImportUserChannel userChannel = new EclipseImportUserChannel(doDisplayDataModelChanges, getShell())
				{
					@Override
					public int askImportSampleData()
					{
						if (ImportSolutionWizard.this.shouldImportSampleData())
						{
							return OK_ACTION;
						}
						return super.askImportSampleData();
					}

					@Override
					public int askImportDatasources()
					{
						Boolean value = ImportSolutionWizard.this.shouldImportDatasources();
						if (value != null)
						{
							return value.booleanValue() ? OK_ACTION : CANCEL_ACTION;
						}
						int retValue = super.askImportDatasources();
						ImportSolutionWizard.this.setImportDatasources(retValue == OK_ACTION ? Boolean.TRUE : Boolean.FALSE);
						return retValue;
					}

					@Override
					public int getAllowDataModelChange(String serverName)
					{
						if (ImportSolutionWizard.this.shouldAllowDataModelChanges())
						{
							return OK_ACTION;
						}
						return super.getAllowDataModelChange(serverName);
					}

					@Override
					public int askStyleAlreadyExistsAction(String name)
					{
						if (existingSolutionAction.containsKey(name)) return existingSolutionAction.get(name).intValue();
						if (ImportSolutionWizard.this.shouldOverwriteModule())
						{
							return OVERWRITE_ACTION;
						}
						if (ImportSolutionWizard.this.shouldSkipModulesImport())
						{
							return SKIP_ACTION;
						}
						return super.askStyleAlreadyExistsAction(name);
					}

					@Override
					public int askAllowSQLKeywords()
					{
						if (ImportSolutionWizard.this.askAllowSQLKeywords())
						{
							return OK_ACTION;
						}
						return super.askAllowSQLKeywords();
					}

					private ServerConfig getValidPostgresServerConfig()
					{
						return Arrays.stream(ApplicationServerRegistry.get().getServerManager().getServerConfigs())
							.filter(
								s -> s.isEnabled() && s.isPostgresDriver() &&
									ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()) != null &&
									((IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(s.getServerName()))
										.isValid())
							.findAny()
							.orElse(null);
					}

					@Override
					public int askUnknownServerAction(String name)
					{
						if (ImportSolutionWizard.this.shouldCreateMissingServer())
						{
							ServerConfig sc = getValidPostgresServerConfig();
							if (sc == null)
							{
								//if we don't have a valid postgres config, we ask the user
								//to select an existing server or cancel
								int asked = super.askUnknownServerAction(name);
								if (asked == CANCEL_ACTION)
								{
									//if the user cancelled we use this info to know what further actions to take
									//for instance show a tutorial on how to create a db connection
									isMissingServer = name;
									monitor.setCanceled(true);
								}
								return asked;
							}

							ServerConfig serverConfig = createServer(name, sc);
							if (serverConfig == null)
							{
								return cannotCreateServer(monitor, name);
							}
							try
							{
								ApplicationServerRegistry.get().getServerManager().testServerConfigConnection(serverConfig, 0);
								ApplicationServerRegistry.get().getServerManager().saveServerConfig(null, serverConfig);
							}
							catch (Exception ex)
							{
								ServoyLog.logError(ex);
								return cannotCreateServer(monitor, name);
							}

							return RETRY_ACTION;
						}
						else
						{
							return super.askUnknownServerAction(name);
						}
					}

					protected int cannotCreateServer(IProgressMonitor monitor, String name)
					{
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(UIUtils.getActiveShell(), "Cannot create server '" + name + "'",
									"An unexpected error occured while creating new server, please create the server manually.");
							}
						});
						isMissingServer = name;
						monitor.setCanceled(true);
						return CANCEL_ACTION;
					}

					protected ServerConfig createServer(String name, ServerConfig sc)
					{
						ServerConfig serverConfig = sc.newBuilder()
							.setServerName(name)
							.setServerUrl(EclipseDatabaseUtils.getPostgresServerUrl(sc, name))
							.setDataModelCloneFrom(null)
							.setEnabled(true)
							.setSkipSysTables(false)
							.setIdleTimeout(-1)
							.build();

						if (ApplicationServerRegistry.get().getServerManager().validateServerConfig(null, serverConfig) != null)
						{
							// something is wrong
							return null;
						}

						// create server
						ITransactionConnection connection = null;
						PreparedStatement ps = null;
						try
						{
							IServerInternal serverPrototype = (IServerInternal)ApplicationServerRegistry.get().getServerManager()
								.getServer(sc.getServerName());
							connection = serverPrototype.getUnmanagedConnection();
							ps = connection.prepareStatement("CREATE DATABASE \"" + name + "\" WITH ENCODING 'UNICODE';");
							ps.execute();
							ps.close();
							ps = null;
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
							serverConfig = null;
						}
						finally
						{
							Utils.closeConnection(connection);
							Utils.closeStatement(ps);
						}
						return serverConfig;
					}
				};
				IApplicationServerSingleton as = ApplicationServerRegistry.get();
				String title = "Solution imported";
				String description = null;
				Status status = null;
				WebPackagesListener.setIgnore(true);
				try
				{
					IXMLImportEngine importEngine = as.createXMLImportEngine(fileDecryption(file),
						(EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository(),
						as.getDataServer(), as.getClientId(), userChannel);

					IXMLImportHandlerVersions11AndHigher x11handler = as.createXMLInMemoryImportHandler(importEngine.getVersionInfo(), as.getDataServer(),
						as.getClientId(), userChannel, (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository());

					x11handler.setAskForImportServerName(ImportSolutionWizard.this.shouldAskForImportServerName());

					IRootObject[] rootObjects = XMLEclipseWorkspaceImportHandlerVersions11AndHigher.importFromJarFile(importEngine, x11handler, userChannel,
						(EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository(), resourcesProjectName, existingProject, monitor,
						doActivateSolution,
						isCleanImport, projectLocation, reportImportFail, forceActivateResourcesProject, keepResourcesProjectOpen, projectsToDeleteAfterImport);

					userChannel.getFormCss().forEach((k, v) -> {
						if (rootObjects[0] instanceof Solution sol)
						{
							IPersist frm = sol.getChild(Utils.getAsUUID(k, false));
							if (frm instanceof Form form)
							{
								AISolutionGenerator.createFormCSS(form, v);
							}
						}
					});

					if (rootObjects != null)
					{
						title = "Solution imported";
						description = "Solution '" + rootObjects[0].getName() + "' imported";
						if (doActivateSolution) description += " and activated";
						description += ".";
						status = new Status(IStatus.INFO, Activator.PLUGIN_ID, userChannel.getAllImportantMSGes(), null);
					}
				}
				catch (final RepositoryException ex)
				{
					title = "Solution not imported";
					if (ex.hasErrorCode(ServoyException.InternalCodes.OPERATION_CANCELLED))
					{
						// Don't show an error message if the import was canceled.
						description = "Import cancelled";
						status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Solution import was cancelled.", null);
					}
					else
					{
						// Don't show an stack trace for CRC related messages.
						if (!ex.hasErrorCode(ServoyException.InternalCodes.CHECKSUM_FAILURE))
						{
							ServoyLog.logError(ex);
						}
						description = "Import failed";
						status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not import solution: " + ex.getMessage(), null);
					}
				}
				catch (final Exception ex)
				{
					ServoyLog.logError(ex);
					String msg = "An unexpected error occured";
					if (ex.getMessage() != null) msg += ex.getMessage();
					else msg += ". Check the log for more details.";
					final String mymsg = msg;
					description = "Import failed";
					title = "Solution not imported";
					status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not import solution: " + mymsg, ex);
				}
				finally
				{
					WebPackagesListener.setIgnore(false);
				}
				showDetailsDialog(title, description, status);
			}
		};
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		try
		{
			servoyModel.setSolutionImportInProgressFlag(true); // suspended many of Solex's listeners to avoid unwanted flickers and unneded code running
			if (context != null) context.run(true, false, runnable);
			else runnable.run(mon);
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			servoyModel.setSolutionImportInProgressFlag(false); // resumes Solex listeners that were suspended above and does a full refresh
		}

		// trigger start debug action delegate for refreshing enablement of debug client buttons by refreshing selection
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				IWorkbenchPage iwpage = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage();
				if (iwpage != null && iwpage.getActivePart() instanceof SolutionExplorerView)
				{
					SolutionExplorerView view = (SolutionExplorerView)iwpage.getActivePart();
					view.refreshSelection();
				}
			}
		});
	}

	public void shouldCreateMissingServer(boolean create)
	{
		this.createMissingServer = create;
	}

	protected boolean shouldCreateMissingServer()
	{
		return createMissingServer;
	}

	protected boolean askAllowSQLKeywords()
	{
		return allowSQLKeywords;
	}

	public void shouldAllowSQLKeywords(boolean allow)
	{
		this.allowSQLKeywords = allow;
	}

	protected boolean shouldAllowDataModelChanges()
	{
		return allowDataModelChanges;
	}

	public void setAllowDataModelChanges(boolean allowDataModelChanges)
	{
		this.allowDataModelChanges = allowDataModelChanges;
	}

	public void setImportSampleData(boolean importSampleData)
	{
		this.importSampleData = importSampleData;
	}

	protected boolean shouldImportSampleData()
	{
		return importSampleData;
	}

	public void setImportDatasources(Boolean importDatasources)
	{
		this.importDatasources = importDatasources;
	}

	public Boolean shouldImportDatasources()
	{
		return importDatasources;
	}

	public class ImportSolutionWizardPage extends WizardPage implements IValidator
	{
		private class SolutionImportActionsDialog extends MessageDialog
		{
			private final Map<String, String> workspaceVersions;
			private final JSONObject versions;

			private SolutionImportActionsDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
				String[] dialogButtonLabels, int defaultIndex, Map<String, String> workspaceVersions, JSONObject versions)
			{
				super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
				this.workspaceVersions = workspaceVersions;
				this.versions = versions;
			}

			@Override
			protected Control createCustomArea(Composite parent)
			{
				Composite composite = new Composite(parent, SWT.NONE);
				Color backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
				composite.setBackground(backgroundColor);
				GridLayout gridLayout = new GridLayout();
				gridLayout.numColumns = 4;
				composite.setLayout(gridLayout);
				composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

				GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
				FontDescriptor descriptor = FontDescriptor.createFrom(parent.getFont()).setStyle(SWT.BOLD);
				Font font = descriptor.createFont(getShell().getDisplay());

				addLabel(composite, backgroundColor, gd, "Module", font).addDisposeListener((e) -> descriptor.destroyFont(font));
				addLabel(composite, backgroundColor, gd, "Workspace Version", font);
				addLabel(composite, backgroundColor, gd, "Import Version", font);
				addLabel(composite, backgroundColor, gd, "Action Taken", font);

				for (String name : workspaceVersions.keySet())
				{
					addLabel(composite, backgroundColor, gd, name, null);
					addLabel(composite, backgroundColor, gd, workspaceVersions.get(name), null);
					addLabel(composite, backgroundColor, gd, versions.optString(name, "-"), null);
					addLabel(composite, backgroundColor, gd,
						existingSolutionAction.get(name).intValue() == IXMLImportUserChannel.OVERWRITE_ACTION ? "Overwrite" : "Skip import",
						null);
				}
				getShell().layout(true, true);
				return super.createCustomArea(parent);
			}

			protected Label addLabel(Composite composite, Color backgroundColor, GridData gd, String text, Font font)
			{
				Label l = new Label(composite, SWT.NONE);
				l.setBackground(backgroundColor);
				l.setLayoutData(gd);
				if (font != null) l.setFont(font);
				l.setText(text);

				return l;
			}
		}

		private ResourcesProjectChooserComposite resourceProjectComposite;
		private Text filePath;
		private Button browseButton;
		private Button cleanImport;
		private Button displayDataModelChanges;
		private Button bActivateSolution;

		private final ImportSolutionWizard wizard;
		private ProjectLocationComposite projectLocationComposite;

		protected ImportSolutionWizardPage(ImportSolutionWizard wizard, String pageName)
		{
			super(pageName);
			this.wizard = wizard;
			setTitle(wizard.getFirstPageTitle() == null ? "Preparing to import solution" : wizard.getFirstPageTitle());
			setDescription("A solution (can be with modules) will be imported into the workspace using the selected import options.");
		}

		public boolean canFinish()
		{
			return validate() == null;
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
					if (wizard.getContainer().getCurrentPage() != null)
					{
						wizard.getContainer().updateButtons();
					}
				}
			});
			filePath.setEditable(allowSolutionFilePathSelection);

			browseButton = new Button(topLevel, SWT.NONE);
			browseButton.setText("Browse");
			browseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
					dlg.setFilterExtensions(new String[] { "*.servoy" });
					if (initialPath != null) dlg.setFilterPath(initialPath);
					if (dlg.open() != null)
					{
						initialPath = dlg.getFilterPath();
						filePath.setText(dlg.getFilterPath() + File.separator + dlg.getFileName());
					}
				}
			});
			browseButton.setEnabled(allowSolutionFilePathSelection);

			final String solutionFilePath = wizard.getSolutionFilePath();
			if (solutionFilePath != null)
			{
				Display.getCurrent().asyncExec(new Runnable()
				{
					public void run()
					{
						filePath.setText(solutionFilePath); // to avoid large dialog widths if path is long
					}
				});
				filePath.setEditable(false);
				browseButton.setEnabled(false);
			}

			cleanImport = new Button(topLevel, SWT.CHECK);
			cleanImport.setText("Clean Import");

			displayDataModelChanges = new Button(topLevel, SWT.CHECK);
			displayDataModelChanges.setText("Display data model (database) changes");

			bActivateSolution = new Button(topLevel, SWT.CHECK);
			bActivateSolution.setText("Activate solution after import");
			bActivateSolution.setSelection(activateSolution);

			projectLocationComposite = new ProjectLocationComposite(topLevel, SWT.NONE, this.getClass().getName());

			resourceProjectComposite = new ResourcesProjectChooserComposite(topLevel, SWT.NONE, this,
				"Please choose the resources project the solution will reference (for styles, column/sequence info, security)",
				ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject(), false);

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
			displayDataModelChanges.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(cleanImport, 0, SWT.LEFT);
			formData.top = new FormAttachment(displayDataModelChanges, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			bActivateSolution.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(bActivateSolution, 10);
			projectLocationComposite.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(projectLocationComposite, 10);
			formData.bottom = new FormAttachment(100, 0);
			resourceProjectComposite.setLayoutData(formData);
		}


		@SuppressWarnings("boxing")
		private boolean canOverwiteModules(File file)
		{
			try (ZipFile zipFile = new ZipFile(file))
			{
				ZipEntry entry = zipFile.getEntry("export/versions.json");
				if (entry == null) return true; //versions file missing, import as usually
				try (InputStream is = zipFile.getInputStream(entry))
				{
					String content = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
					JSONObject versions = new JSONObject(content);
					IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
					boolean showDialog = false;
					Map<String, String> workspaceVersions = new TreeMap<>(NameComparator.INSTANCE);
					int toImport = 0;
					for (String name : versions.keySet())
					{
						toImport++;
						if (servoyModel.getServoyProject(name) != null)
						{
							Solution sol = servoyModel.getServoyProject(name).getSolution();
							if (sol != null)
							{
								workspaceVersions.put(name, !Utils.stringIsEmpty(sol.getVersion()) ? sol.getVersion() : "-");
								if (!versions.optString(name, "").equals(sol.getVersion()))
								{
									existingSolutionAction.put(name, IXMLImportUserChannel.OVERWRITE_ACTION);
									showDialog = true; //we only show the dialog if we have solutions to overwrite
								}
								else
								{
									//if the version is the same, then we skip
									existingSolutionAction.put(name, IXMLImportUserChannel.SKIP_ACTION);
								}
							}
						}
					}
					if (toImport == existingSolutionAction.values().stream().filter(val -> val == IXMLImportUserChannel.SKIP_ACTION).count())
					{
						//they are the same versions as existing
						boolean result = MessageDialog.openQuestion(getShell(), "Project already exists in the workspace",
							"Do you want to fully overwrite the installed sample again?");
						if (result)
						{
							//we clear the map, so all solutions are marked as overwrite
							existingSolutionAction.clear();
							setOverwriteModule(true);
						}
						return result;

					}
					if (showDialog)
					{
						final MessageDialog dialog = new SolutionImportActionsDialog(getShell(), "Existing modules in the workspace", null,
							"The following actions will be taken on import for the existing solutions/modules:", MessageDialog.WARNING,
							new String[] { "Ok", "Cancel import" }, 0, workspaceVersions, versions);
						int result = dialog.open();
						return result == 0;
					}
					return true;
				}
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}

			return false;
		}

		public String validate()
		{
			String error = null;
			if (filePath.getText().trim().length() == 0)
			{
				error = "Please select .servoy file to import.";
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

		public boolean getDisplayDataModelChange()
		{
			return displayDataModelChanges.getSelection();
		}

		public boolean getActivateSolution()
		{
			return bActivateSolution.getSelection();
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return false;
		}
	}

	public String isMissingServer()
	{
		return isMissingServer;
	}

	private boolean doSolutionImport()
	{
		final File file = new File(page.getPath());
		if (!file.exists() || !file.isFile())
		{
			showDetailsDialog("Solution not imported", "Import failed",
				new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not import solution: Invalid file", null));
			return false;
		}
		if (!file.canRead())
		{
			showDetailsDialog("Solution not imported", "Import failed",
				new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not import solution: File cannot be read", null));
			return false;
		}
		if (page.getErrorMessage() != null)
		{
			showDetailsDialog("Solution not imported", "Import failed",
				new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not import solution: " + page.getErrorMessage(), null));
			return false;
		}

		File decryptedFile = fileDecryption(file);

		if (!page.canOverwiteModules(decryptedFile))
		{
			Display.getDefault().asyncExec(() -> {
				if (!getShell().isDisposed()) getShell().close();
			});
			return false;
		}

		final String resourcesProjectName = page.getNewName();
		final ServoyResourcesProject existingProject = page.getResourcesProject();
		final boolean isCleanImport = page.isCleanImport();
		final boolean doDisplayDataModelChanges = page.getDisplayDataModelChange();
		final boolean doActivateSolution = page.getActivateSolution();
		doImport(decryptedFile, resourcesProjectName, existingProject, isCleanImport, doDisplayDataModelChanges, doActivateSolution,
			page.projectLocationComposite.getProjectLocation(), getContainer(), null, false, false, null);

		return true;
	}

	protected void showDetailsDialog(String title, String description, Status status)
	{
		if (!showFinishDialog) return;

		Display.getDefault().asyncExec(() -> {
			MultiStatus info = new MultiStatus(Activator.PLUGIN_ID, 1,
				status.getSeverity() == IStatus.INFO ? description : status.getMessage(), null);
			info.add(status);
			ErrorDialog.openError(getShell(), title, status.getSeverity() == IStatus.INFO ? null : description, info);
		});
	}

	public void showFinishDialog(boolean show)
	{
		this.showFinishDialog = show;
	}
}
