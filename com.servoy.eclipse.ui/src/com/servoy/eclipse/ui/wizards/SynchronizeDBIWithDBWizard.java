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

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IColumnInfoBasedSequenceProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TableChangeHandler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * Wizard that allows the user to check for .dbi files that do not have a corresponding table in the database. If such files are found, the user can choose
 * either to create a new table according to that file, or delete the file.<BR>
 * This wizard will also automatically create default .dbi files for tables that exist, but do not have a corresponding file.
 *
 * @author acostescu
 */
public class SynchronizeDBIWithDBWizard extends Wizard implements IWorkbenchWizard
{
	private WizardPage errorPage;
	private InitialChoiceWizardPage initialChoicePage;
	private SplitInThreeWizardPage<IServerInternal, String> page1;
	private SplitInThreeWizardPage<IServerInternal, String> page2;
	private CheckBoxWizardPage page3;
	private final List<IServerInternal> servers = new ArrayList<IServerInternal>();
	private DataModelManager dmm;
	private IWorkbenchPage activePage;
	private String selectedTableName;

	public SynchronizeDBIWithDBWizard()
	{
		setWindowTitle(
			"Servoy stores table information in special DBI files located in resources project. This wizard helps you synchronize the file structure and database structure for each table.");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("sync_dbi.png"));
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setNeedsProgressMonitor(true);
		setForcePreviousAndNextButtons(true);
		activePage = (workbench != null && workbench.getActiveWorkbenchWindow() != null) ? workbench.getActiveWorkbenchWindow().getActivePage() : null;

		servers.clear();
		dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		IServerManagerInternal sm = ApplicationServerRegistry.get().getServerManager();
		if (dmm == null)
		{
			errorPage = new WizardPage("No database information files found")
			{

				public void createControl(Composite parent)
				{
					setControl(new Composite(parent, SWT.NONE));
				}

			};
			errorPage.setTitle("No database information files found");
			errorPage.setErrorMessage(
				"Synchronizing database tables with the database information files requires an active resources project.\nThe database information files (.dbi) are located in resources projects.");
			errorPage.setPageComplete(false);
			page1 = null;
			page2 = null;
			page3 = null;
		}
		else if (sm != null && sm.getGlobalSequenceProvider() instanceof IColumnInfoBasedSequenceProvider)
		{
			// When running the in-process repository, the first GlobalColumnInfoProvider in the list
			// will be TableBasedColumnInfoProvider; that one will be used to read the column infos,
			// and dataModelManager will only be used to write the in-memory column infos to dbi files.
			// In this case we want to make the wizard unusable - because this wizard is based on .dbi files & datamodelmanager
			// and reading from dbi files / solving some conflicts can either will not work, or will disrupt the
			// normal TableBasedColumnInfoProvider usage.
			// As TableBasedColumnInfoProvider and ColumnInfoBasedSequenceProvider are used when running in-process repository
			// we use the sequence provider to identify the case
			errorPage = new WizardPage("This wizard is unavailable")
			{

				public void createControl(Composite parent)
				{
					setControl(new Composite(parent, SWT.NONE));
				}

			};
			errorPage.setTitle("This wizard is unavailable");
			errorPage.setErrorMessage("Synchronizing requires that '.dbi' files are used. When the developer is started with admin setting\n\"" +
				Settings.START_AS_TEAMPROVIDER_SETTING +
				" = true\", database information is handled by the team repository directly, not through '.dbi' files.");
			errorPage.setPageComplete(false);
			page1 = null;
			page2 = null;
			page3 = null;
		}
		else
		{
			// if the selection is made up of server nodes, use the valid and enabled ones; otherwise use all the servers
			boolean serversIdentified = true;
			Iterator< ? > it = selection.iterator();
			while (it.hasNext() && serversIdentified)
			{
				Object element = it.next();
				if (element instanceof SimpleUserNode)
				{
					SimpleUserNode un = (SimpleUserNode)element;
					if (un.getType() == UserNodeType.SERVER)
					{
						IServerInternal s = (IServerInternal)un.getRealObject();
						if (s.getConfig().isEnabled() || s.isValid())
						{
							servers.add(s);
						}
					}
					else if (un.getType() == UserNodeType.TABLE || un.getType() == UserNodeType.VIEW)
					{
						IServerInternal s = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(
							((TableWrapper)un.getRealObject()).getServerName());
						if (s.getConfig().isEnabled() || s.isValid()) servers.add(s);
					}
					else
					{
						serversIdentified = false;
					}
				}
				else
				{
					serversIdentified = false;
				}
			}
			if (!serversIdentified || servers.size() == 0)
			{
				// no specific list of servers selected - use all of them
				String serverNames[] = sm.getServerNames(true, true, true, false);
				for (int i = serverNames.length - 1; i >= 0; i--)
				{
					servers.add((IServerInternal)sm.getServer(serverNames[i], true, true));
				}
			}

			Object firstSelectedElement = selection.getFirstElement();
			selectedTableName = firstSelectedElement instanceof UserNode && ((UserNode)firstSelectedElement).getRealObject() instanceof TableWrapper
				? ((TableWrapper)((UserNode)firstSelectedElement).getRealObject()).getTableName() : null;
		}
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	private static List<Pair<IServerInternal, String>> getMissingTables(List<IServerInternal> servers, final DataModelManager dmm)
	{
		// choose which of the missing tables (tables that do not exist in the DB but for which there are .dbi files)
		// will be created and which of the .dbi files are no longer wanted.
		final List<Pair<IServerInternal, String>> foundMissingTables = new ArrayList<Pair<IServerInternal, String>>();
		for (final IServerInternal s : servers)
		{
			IFolder serverInformationFolder = dmm.getServerInformationFolder(s.getName());
			if (serverInformationFolder.exists())
			{
				try
				{
					final List<String> tableNames = s.getTableAndViewNames(true);
					serverInformationFolder.accept(new IResourceVisitor()
					{

						public boolean visit(IResource resource) throws CoreException
						{
							String extension = resource.getFileExtension();
							if (extension != null && extension.equalsIgnoreCase(DataModelManager.COLUMN_INFO_FILE_EXTENSION))
							{
								// we found a .dbi file... see if the table exists
								String tableName = resource.getName().substring(0,
									resource.getName().length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
								if (!tableNames.contains(tableName) && !tableName.toUpperCase().startsWith(DataModelManager.TEMP_UPPERCASE_PREFIX))
								{
									if (tableName.toLowerCase().equals(tableName))
									{
										foundMissingTables.add(new Pair<IServerInternal, String>(s, tableName));
										dmm.updateMarkerStatesForMissingTable(null, s.getName(), tableName); // add marker as well
									}
									else
									{
										ServoyLog.logWarning("Found .dbi file that contains uppercase characters while synchronizing. It will be ignored.",
											null);
									}
								}
							}
							return true;
						}

					}, IResource.DEPTH_ONE, false);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		return foundMissingTables;
	}

	private static List<Pair<IServerInternal, String>> getSupplementalTables(List<IServerInternal> servers, DataModelManager dmm)
	{
		// choose which of the missing files (tables that exist in the DB but have no corresponding .dbi file) will be created
		// and which of the tables corresponding to these missing files will be deleted
		List<Pair<IServerInternal, String>> foundSupplementalTables = new ArrayList<Pair<IServerInternal, String>>();
		for (IServerInternal s : servers)
		{
			IFolder serverInformationFolder = dmm.getServerInformationFolder(s.getName());
			try
			{
				for (String tableName : s.getTableAndViewNames(true))
				{
					if (!serverInformationFolder.getFile(tableName + DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT).exists())
					{
						foundSupplementalTables.add(new Pair<IServerInternal, String>(s, tableName));
						dmm.addMissingDBIMarker(s.getName(), tableName, true);
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}

		return foundSupplementalTables;
	}

	@Override
	public void addPages()
	{
		if (errorPage != null)
		{
			addPage(errorPage);
		}
		else
		{
			initialChoicePage = new InitialChoiceWizardPage();
			initialChoicePage.setPageComplete(false);
			addPage(initialChoicePage);
		}
	}

	@Override
	public boolean performFinish()
	{
		try
		{
			getContainer().run(true, true, new IRunnableWithProgress()
			{
				public void run(IProgressMonitor m) throws InvocationTargetException, InterruptedException
				{
					try
					{
						ServoyModel.getWorkspace().run(new IWorkspaceRunnable()
						{
							public void run(IProgressMonitor monitor) throws CoreException
							{
								if (initialChoicePage.isSynchronizeDBI())
								{
									int work1 = (page1 != null ? page1.getSet2().size() + page1.getSet3().size() : 0);
									int work2 = (page2 != null ? page2.getSet2().size() + page2.getSet3().size() : 0);
									int work3 = (page3 != null ? (page3.isChecked() ? countTables() : 0) : 0);
									monitor.beginTask("Synchronizing database with database information files", work1 + work2 + work3);
									try
									{
										MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);
										if (page1 != null && !monitor.isCanceled())
										{
											monitor.subTask("- handling missing tables");
											handleMissingTables(page1.getSet2(), page1.getSet3(), new SubProgressMonitor(monitor, work1), warnings, work1);
										}
										if (page2 != null && !monitor.isCanceled())
										{
											monitor.subTask("- handling supplemental tables");
											handleSupplementalTables(page2.getSet2(), page2.getSet3(), new SubProgressMonitor(monitor, work2), warnings, work2);
										}
										if (page3 != null && page3.isChecked() && !monitor.isCanceled())
										{
											monitor.subTask("- read and check database information");
											readAndCheckDatabaseInformation(new SubProgressMonitor(monitor, work3), work3);
										}
										if (warnings.getChildren().length > 0)
										{
											final MultiStatus fw = warnings;
											UIUtils.runInUI(new Runnable()
											{
												public void run()
												{
													ErrorDialog.openError(getShell(), null, null, fw);
												}
											}, false);
										}
										UIUtils.runInUI(new Runnable()
										{
											public void run()
											{
												HandleDBIMarkersWizard wizard = new HandleDBIMarkersWizard(servers, selectedTableName);
												int returnCode = Window.OK;
												while (returnCode == Window.OK && wizard.hasMarkers(false))
												{
													WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
														wizard);
													dialog.create();
													returnCode = dialog.open();
													wizard = new HandleDBIMarkersWizard(servers, selectedTableName);
												}
											}
										}, false);
									}
									finally
									{
										monitor.done();
									}
								}
								if (initialChoicePage.reloadTables() && !initialChoicePage.isSynchronizeDBI())
								{
									monitor.beginTask("Reloading tables from database", IProgressMonitor.UNKNOWN);
									if (servers != null)
									{
										for (IServerInternal server : servers)
										{
											try
											{
												server.reloadTables();
											}
											catch (RepositoryException e)
											{
												Debug.error(e);
											}
										}
									}
								}
							}
						}, ServoyModel.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, m);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}

				private int countTables()
				{
					int count = 0;
					try
					{
						for (IServerInternal s : servers)
						{
							count += s.getTableAndViewNames(true).size();
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					return count;
				}

				private void handleMissingTables(List<Pair<IServerInternal, String>> tablesToCreate, List<Pair<IServerInternal, String>> dbiFilesToDelete,
					IProgressMonitor monitor, MultiStatus warnings, int workUnits)
				{
					monitor.beginTask("Handling missing tables", workUnits);
					Map<IServerInternal, List<String>> tablesAdded = new HashMap<IServerInternal, List<String>>();
					try
					{
						monitor.subTask("- creating missing tables");
						for (Pair<IServerInternal, String> tableToCreate : tablesToCreate)
						{
							if (monitor.isCanceled()) break;
							IFile file = dmm.getDBIFile(tableToCreate.getLeft().getName(), tableToCreate.getRight());
							if (file.exists())
							{
								try
								{
									InputStream is = file.getContents(true);
									String dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
									Utils.closeInputStream(is);
									String problems = EclipseDatabaseUtils.createNewTableFromColumnInfo(tableToCreate.getLeft(), tableToCreate.getRight(),
										dbiFileContent, EclipseDatabaseUtils.UPDATE_NOW, false);
									if (problems != null)
									{
										StringTokenizer st = new StringTokenizer(problems, "\n");
										while (st.hasMoreTokens())
										{
											warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, st.nextToken()));
										}
									}
									if (!tablesAdded.containsKey(tableToCreate.getLeft()))
									{
										tablesAdded.put(tableToCreate.getLeft(), new ArrayList<String>());
									}
									tablesAdded.get(tableToCreate.getLeft()).add(tableToCreate.getRight());
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
									warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
								}
							}
							else
							{
								ServoyLog.logError("Cannot find .dbi file to create missing table " + tableToCreate, null);
								warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
									"Cannot find .dbi file to create missing table " + tableToCreate.getLeft().getName() + " - " + tableToCreate.getRight()));
							}
							monitor.worked(1);
						}
						for (IServerInternal server : tablesAdded.keySet())
						{
							TableChangeHandler.getInstance().fireTablesAdded(server,
								tablesAdded.get(server).toArray(new String[tablesAdded.get(server).size()]));
						}

						if (!monitor.isCanceled())
						{
							monitor.subTask("- delete database information files for missing tables");
							for (Pair<IServerInternal, String> dbiFileToDelete : dbiFilesToDelete)
							{
								if (monitor.isCanceled()) break;
								IFile file = dmm.getDBIFile(dbiFileToDelete.getLeft().getName(), dbiFileToDelete.getRight());
								if (file.exists())
								{
									try
									{
										file.delete(true, null);
									}
									catch (CoreException e)
									{
										ServoyLog.logError(e);
										warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
									}
								}
								monitor.worked(1);
							}
						}
					}
					finally
					{
						monitor.subTask("");
						monitor.done();
					}
				}

				private void handleSupplementalTables(List<Pair<IServerInternal, String>> dbiFilesToCreate, List<Pair<IServerInternal, String>> tablesToDelete,
					IProgressMonitor monitor, MultiStatus warnings, int workUnits)
				{
					monitor.beginTask("Handling supplemental tables", workUnits);
					try
					{
						monitor.subTask("- deleting tables with missing database information files");
						for (Pair<IServerInternal, String> tableToDelete : tablesToDelete)
						{
							if (monitor.isCanceled()) break;
							try
							{
								tableToDelete.getLeft().removeTable(tableToDelete.getRight());
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
							}
							catch (SQLException e)
							{
								ServoyLog.logError(e);
								warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
							}
							monitor.worked(1);
						}
						if (!monitor.isCanceled())
						{
							monitor.subTask("- creating missing .dbi files");
							for (Pair<IServerInternal, String> dbiFileToCreate : dbiFilesToCreate)
							{
								if (monitor.isCanceled()) break;
								try
								{
									// make sure the table is loaded
									ITable t = dbiFileToCreate.getLeft().getTable(dbiFileToCreate.getRight());

									// write the file
									dmm.updateAllColumnInfo(t);
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError(e);
									warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
								}
								monitor.worked(1);
							}
						}
					}
					finally
					{
						monitor.subTask("");
						monitor.done();
					}
				}

				private void readAndCheckDatabaseInformation(IProgressMonitor monitor, int workUnits)
				{
					monitor.beginTask("- read and check database information files", workUnits);
					monitor.subTask("- read and check database information files");
					try
					{
						for (IServerInternal s : servers)
						{
							if (monitor.isCanceled()) break;
							for (String tableName : s.getTableAndViewNames(true))
							{
								if (monitor.isCanceled()) break;
								monitor.worked(1);
								s.refreshTable(tableName);
							}
						}
						// bring Problems view to front
						if (activePage != null)
						{
							UIUtils.runInUI(new Runnable()
							{
								public void run()
								{
									try
									{
										activePage.showView(IPageLayout.ID_PROBLEM_VIEW);
									}
									catch (PartInitException e)
									{
										ServoyLog.logError(e);
									}
								}
							}, false);
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					finally
					{
						monitor.subTask("");
						monitor.done();
					}
				}

			});
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			// operation canceled
		}
		return true;
	}

	/**
	 * A wizard page that contains one simple check box.
	 */
	public static class CheckBoxWizardPage extends WizardPage
	{
		private boolean checked;
		private final String labelText;
		private final String checkBoxText;

		/**
		 * Creates a new check box wizard page.
		 *
		 * @param title wizard page title.
		 * @param description wizard page description.
		 * @param labelText the text on the label above the check box.
		 * @param checkBoxText the text on the check box.
		 * @param checked the initial state of the check box.
		 */
		public CheckBoxWizardPage(String title, String description, String labelText, String checkBoxText, boolean checked)
		{
			super(title);
			setTitle(title);
			setDescription(description);
			this.checked = checked;
			this.labelText = labelText;
			this.checkBoxText = checkBoxText;
		}

		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);

			// create visual components
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);
			FormLayout layout = new FormLayout();
			layout.marginHeight = layout.marginWidth = 5;
			topLevel.setLayout(layout);

			Label infoLabel = new Label(topLevel, SWT.WRAP);
			infoLabel.setText(labelText);

			final Button checkBox = new Button(topLevel, SWT.CHECK);
			checkBox.setSelection(checked);
			checkBox.setText(checkBoxText);

			// layout the two components
			FormData data = new FormData();
			data.left = new FormAttachment(0, 0);
			data.top = new FormAttachment(0, 0);
			data.right = new FormAttachment(100, 0);
			infoLabel.setLayoutData(data);

			data = new FormData();
			data.left = new FormAttachment(0, 20);
			data.top = new FormAttachment(infoLabel, 20);
			data.right = new FormAttachment(100, 0);
			checkBox.setLayoutData(data);

			checkBox.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					checked = checkBox.getSelection();
				}

				public void widgetSelected(SelectionEvent e)
				{
					widgetDefaultSelected(e);
				}
			});
		}

		public boolean isChecked()
		{
			return checked;
		}

		@Override
		public void performHelp()
		{
			PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.synchronizedbi_synccolumns");
		}

	}

	public class InitialChoiceWizardPage extends WizardPage
	{
		private boolean reloadTables = false;
		private boolean synchronizeDBI = true;

		public InitialChoiceWizardPage()
		{
			super("Select synchronize choices");
		}

		@Override
		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);

			// create visual components
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);
			FormLayout layout = new FormLayout();
			layout.marginHeight = layout.marginWidth = 5;
			topLevel.setLayout(layout);

			Label infoLabel2 = new Label(topLevel, SWT.WRAP);
			infoLabel2.setText("Reload tables from database structure");
			FormData data = new FormData();
			data.left = new FormAttachment(0, 0);
			data.top = new FormAttachment(0, 0);
			data.right = new FormAttachment(100, 0);
			infoLabel2.setLayoutData(data);

			FontDescriptor descriptor1 = FontDescriptor.createFrom(infoLabel2.getFont()).setStyle(SWT.BOLD);
			Font font1 = descriptor1.createFont(infoLabel2.getDisplay());
			infoLabel2.setFont(font1);
			infoLabel2.addDisposeListener((e) -> descriptor1.destroyFont(font1));

			final Button checkBox2 = new Button(topLevel, SWT.CHECK | SWT.WRAP);
			checkBox2.setSelection(false);
			checkBox2.setText(
				"Reload database changes (in case changes were done outside Servoy you should reload the tables in memory so that Servoy contains the latest information)");
			data = new FormData();
			data.left = new FormAttachment(0, 20);
			data.top = new FormAttachment(infoLabel2, 20);
			data.right = new FormAttachment(100, 0);
			checkBox2.setLayoutData(data);
			checkBox2.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					reloadTables = checkBox2.getSelection();
					setPageComplete(reloadTables && !synchronizeDBI);
					InitialChoiceWizardPage.this.getContainer().updateButtons();
				}
			});

			Label infoLabel = new Label(topLevel, SWT.WRAP);
			infoLabel.setText("Synchronize DBI files with database structure");
			data = new FormData();
			data.left = new FormAttachment(0, 0);
			data.top = new FormAttachment(checkBox2, 50);
			data.right = new FormAttachment(100, 0);
			infoLabel.setLayoutData(data);

			FontDescriptor descriptor = FontDescriptor.createFrom(infoLabel.getFont()).setStyle(SWT.BOLD);
			Font font = descriptor.createFont(infoLabel.getDisplay());
			infoLabel.setFont(font);
			infoLabel.addDisposeListener((e) -> descriptor.destroyFont(font));

			Button checkBox = new Button(topLevel, SWT.CHECK | SWT.WRAP);
			checkBox.setSelection(true);
			checkBox.setText(
				"Synchronize DB tables with DB local files information");
			data = new FormData();
			data.left = new FormAttachment(0, 20);
			data.top = new FormAttachment(infoLabel, 20);
			data.right = new FormAttachment(100, 0);
			checkBox.setLayoutData(data);
			checkBox.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					synchronizeDBI = checkBox.getSelection();
					setPageComplete(reloadTables && !synchronizeDBI);
					InitialChoiceWizardPage.this.getContainer().updateButtons();
				}
			});


		}

		public boolean reloadTables()
		{
			return reloadTables;
		}

		public boolean isSynchronizeDBI()
		{
			return synchronizeDBI;
		}


		@Override
		public boolean canFlipToNextPage()
		{
			if (synchronizeDBI)
			{
				return true;
			}
			return false;
		}

		@Override
		public IWizardPage getNextPage()
		{
			if (synchronizeDBI)
			{
				IRunnableWithProgress job = new IRunnableWithProgress()
				{
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
					{
						if (reloadTables)
						{
							monitor.beginTask("Reloading tables from database", IProgressMonitor.UNKNOWN);
							if (servers != null)
							{
								for (IServerInternal server : servers)
								{
									try
									{
										server.reloadTables();
									}
									catch (RepositoryException e)
									{
										Debug.error(e);
									}
								}
							}
						}
						monitor.beginTask("Calculating differences between Database and local files information", IProgressMonitor.UNKNOWN);
						// find differences between the DB table lists and the .dbi files
						List<Pair<IServerInternal, String>> foundMissingTables = getMissingTables(servers, dmm);
						List<Pair<IServerInternal, String>> foundSupplementalTables = getSupplementalTables(servers, dmm);

						if (foundMissingTables.size() > 0 || foundSupplementalTables.size() > 0)
						{
							errorPage = null;

							Comparator<Pair<IServerInternal, String>> comparator = new Comparator<Pair<IServerInternal, String>>()
							{

								public int compare(Pair<IServerInternal, String> o1, Pair<IServerInternal, String> o2)
								{
									if (o1 == null && o2 == null) return 0;
									if (o1 == null) return -1;
									if (o2 == null) return 1;

									int result = o1.getLeft().getName().compareToIgnoreCase(o2.getLeft().getName());
									if (result == 0)
									{
										result = o1.getRight().compareToIgnoreCase(o2.getRight());
									}
									return result;

								}
							};

							Image serverImage = Activator.getDefault().loadImageFromBundle("server.png");
							Image tableImage = Activator.getDefault().loadImageFromBundle("portal.png");
							Image viewImage = Activator.getDefault().loadImageFromBundle("view.png");

							if (foundMissingTables.size() > 0)
							{
								page1 = new SplitInThreeWizardPage<IServerInternal, String>("Missing tables",
									"Database information files (.dbi from resources project) can point to tables that do not exist in the database.\nYou can choose to create those tables according to the information or delete the unwanted information files.",
									"Skip", "Create table", "Delete .dbi", "Skip all/multiselection", "Create all/multiselection", "Delete all/multiselection",
									foundMissingTables, comparator, serverImage, tableImage, viewImage, "com.servoy.eclipse.ui.synchronizedbi_missingtables");
							}
							else
							{
								page1 = null;
							}
							if (foundSupplementalTables.size() > 0)
							{
								page2 = new SplitInThreeWizardPage<IServerInternal, String>("Missing database information files",
									"Tables in the database can lack an associated database information file (.dbi in the resources project).\nYou can choose to create the database information file or delete the table from the database.",
									"Skip", "Create .dbi", "Delete table", "Skip all/multiselection", "Create all/multiselection", "Delete all/multiselection",
									foundSupplementalTables, comparator, serverImage, tableImage, viewImage, "com.servoy.eclipse.ui.synchronizedbi_missingdbi");
							}
							else
							{
								page2 = null;
							}
						}
						else
						{
							page1 = null;
							page2 = null;
						}

						// page3 offers to read/load all available database information in the inspected servers
						// so that the error markers that show differences in table columns will be created
						page3 = new CheckBoxWizardPage("Synchronize at column level",
							"For tables that exist in the database and also have a database information file in the resources project,\ncolumn information can differ.",
							"Column differences between the DB and the DB information files are only noticed when the DB information files are read.\nAs the files are read only when needed, you might want to trigger a load in order to see the differences\nin the 'Problems' view.",
							"Read/check existing DB information files for existing tables", true);
						if (page1 != null)
						{
							addPage(page1);
						}
						if (page2 != null)
						{
							addPage(page2);
						}
						if (page3 != null)
						{
							addPage(page3);
						}
						setPageComplete(true);
					}
				};
				try
				{
					getContainer().run(false, false, job);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				return super.getNextPage();
			}
			return null;
		}

		@Override
		public void performHelp()
		{
			PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.synchronizedbi_choicepage");
		}
	}
	/**
	 * The wizard page that is used by the user to split a set of tables into three sets.<br>
	 * UI consists of a single tree table with check boxes.
	 *
	 * @param <T1> the type of the first element in the pair.
	 * @param <T2> the type of the second element in the pair.
	 */
	public static class SplitInThreeWizardPage<T1, T2> extends WizardPage
	{

		public static final int NO_CHECKBOX = -1;
		public static final int DATA = 0;
		public static final int SET1 = 1;
		public static final int SET2 = 2;
		public static final int SET3 = 3;

		private TreeViewer treeViewer;
		private final String labelForSet1;
		private final String labelForSet2;
		private final String labelForSet3;
		private final SortedList<Pair<T1, T2>> initialPairs;
		private PairTreeContentProvider<T1, T2> contentProvider;
		private final Image image2;
		private final Image image1;
		private final Image image3;
		private final String allSet1Label;
		private final String allSet2Label;
		private final String allSet3Label;
		private final String helpID;

		/**
		 * Creates a new split-in-three wizard page. It will split the initial set "servers" into 3 subsets.
		 *
		 * @param fatherChildrenPairs the initial set to be split up.
		 * @param title the title of the page.
		 * @param description the description of the page.
		 * @param image2
		 * @param image1
		 * @param labelProvider the label provider used by the three tree viewers.
		 */
		public SplitInThreeWizardPage(String title, String description, String labelForSet1, String labelForSet2, String labelForSet3, String allSet1Label,
			String allSet2Label, String allSet3Label, List<Pair<T1, T2>> fatherChildrenPairs, Comparator<Pair<T1, T2>> c, Image image1, Image image2,
			Image image3, String helpID)
		{
			super(title);
			this.initialPairs = new SortedList<Pair<T1, T2>>(c, fatherChildrenPairs);

			setTitle(title);
			setDescription(description);

			this.labelForSet1 = labelForSet1;
			this.labelForSet2 = labelForSet2;
			this.labelForSet3 = labelForSet3;

			this.allSet1Label = allSet1Label;
			this.allSet2Label = allSet2Label;
			this.allSet3Label = allSet3Label;

			this.image1 = image1;
			this.image2 = image2;
			this.image3 = image3;

			this.helpID = helpID;

		}

		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);

			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);
			FormLayout formLayout = new FormLayout();
			formLayout.marginHeight = formLayout.marginWidth = 5;
			topLevel.setLayout(formLayout);

			// set up components
			Composite treeComposite = new Composite(topLevel, SWT.NONE);
			treeViewer = new TreeViewer(treeComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);

			Tree tree = treeViewer.getTree();
			tree.setLinesVisible(true);
			tree.setHeaderVisible(true);

			contentProvider = new PairTreeContentProvider<T1, T2>();
			treeViewer.setContentProvider(contentProvider);
			treeViewer.setInput(initialPairs);

			treeViewer.addDoubleClickListener(new IDoubleClickListener()
			{
				public void doubleClick(DoubleClickEvent event)
				{
					IStructuredSelection sel = (IStructuredSelection)event.getSelection();
					Object element = sel.getFirstElement();
					if (element != null)
					{
						if (treeViewer.getExpandedState(element))
						{
							treeViewer.collapseToLevel(element, 1);
						}
						else
						{
							treeViewer.expandToLevel(element, 1);
						}
					}
				}
			});

			final CheckboxCellEditor editor = new CheckboxCellEditor(tree, SWT.RADIO);
			TreeColumn dataColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, DATA);
			dataColumn.setText("");
			/* dataViewerColumn */new TreeViewerColumn(treeViewer, dataColumn);

			TreeColumn set1Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET1);
			set1Column.setText(labelForSet1);
			TreeViewerColumn set1ViewerColumn = new TreeViewerColumn(treeViewer, set1Column);
			set1ViewerColumn.setEditingSupport(new RadioEditingSupport<T1, T2>(treeViewer, contentProvider, editor, SET1));

			TreeColumn set2Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET2);
			set2Column.setText(labelForSet2);
			TreeViewerColumn set2ViewerColumn = new TreeViewerColumn(treeViewer, set2Column);
			set2ViewerColumn.setEditingSupport(new RadioEditingSupport<T1, T2>(treeViewer, contentProvider, editor, SET2));

			TreeColumn set3Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET3);
			set3Column.setText(labelForSet3);
			TreeViewerColumn set3ViewerColumn = new TreeViewerColumn(treeViewer, set3Column);
			set3ViewerColumn.setEditingSupport(new RadioEditingSupport<T1, T2>(treeViewer, contentProvider, editor, SET3));

			treeViewer.setLabelProvider(new PairSplitLabelProvider());
			treeViewer.expandAll();

			Button allInSet1 = new Button(topLevel, SWT.NONE);
			Button allInSet2 = new Button(topLevel, SWT.NONE);
			Button allInSet3 = new Button(topLevel, SWT.NONE);

			allInSet1.setText(allSet1Label);
			allInSet2.setText(allSet2Label);
			allInSet3.setText(allSet3Label);

			allInSet1.setToolTipText("Affects multiple selection or all elements(if single selection)");
			allInSet2.setToolTipText("Affects multiple selection or all elements(if single selection)");
			allInSet3.setToolTipText("Affects multiple selection or all elements(if single selection)");

			allInSet1.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					moveAllOrMultiSelectionToSet(SET1);
				}

				public void widgetSelected(SelectionEvent e)
				{
					widgetDefaultSelected(e);
				}
			});
			allInSet2.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					moveAllOrMultiSelectionToSet(SET2);
				}

				public void widgetSelected(SelectionEvent e)
				{
					widgetDefaultSelected(e);
				}
			});
			allInSet3.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					moveAllOrMultiSelectionToSet(SET3);
				}

				public void widgetSelected(SelectionEvent e)
				{
					widgetDefaultSelected(e);
				}
			});

			// layout components
			TreeColumnLayout layout = new TreeColumnLayout();
			treeComposite.setLayout(layout);
			layout.setColumnData(dataColumn, new ColumnWeightData(55, 150, true));
			layout.setColumnData(set1Column, new ColumnPixelData(100, true));
			layout.setColumnData(set2Column, new ColumnPixelData(100, true));
			layout.setColumnData(set3Column, new ColumnPixelData(100, true));

			FormData fd = new FormData();
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			fd.top = new FormAttachment(0, 0);
			fd.bottom = new FormAttachment(allInSet1, -5);
			treeComposite.setLayoutData(fd);

			fd = new FormData();
			fd.bottom = new FormAttachment(100, 0);
			fd.right = new FormAttachment(allInSet2, -5);
			allInSet1.setLayoutData(fd);

			fd = new FormData();
			fd.bottom = new FormAttachment(100, 0);
			fd.right = new FormAttachment(allInSet3, -5);
			allInSet2.setLayoutData(fd);

			fd = new FormData();
			fd.right = new FormAttachment(100, 0);
			fd.bottom = new FormAttachment(100, 0);
			allInSet3.setLayoutData(fd);
		}

		@Override
		public void performHelp()
		{
			PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpID);
		}

		private void moveAllOrMultiSelectionToSet(int set)
		{
			ISelection selection = treeViewer.getSelection();
			if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() > 1)
			{
				Iterator< ? > it = ((IStructuredSelection)selection).iterator();
				while (it.hasNext())
				{
					Object element = it.next();
					if (element instanceof Pair)
					{
						contentProvider.setSet(element, Integer.valueOf(set));
					}
				}
			}
			else
			{
				for (Pair<T1, T2> p : initialPairs)
				{
					contentProvider.setSet(p, Integer.valueOf(set));
				}
			}

			treeViewer.refresh();
		}

		public List<Pair<T1, T2>> getSet1()
		{
			ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
			if (contentProvider != null)
			{
				for (Pair<T1, T2> p : initialPairs)
				{
					if (contentProvider.getSet(p) == SET1)
					{
						l.add(p);
					}
				}
			}
			return l;
		}

		public List<Pair<T1, T2>> getSet2()
		{
			ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
			if (contentProvider != null)
			{
				for (Pair<T1, T2> p : initialPairs)
				{
					if (contentProvider.getSet(p) == SET2)
					{
						l.add(p);
					}
				}
			}
			return l;
		}

		public List<Pair<T1, T2>> getSet3()
		{
			ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
			if (contentProvider != null)
			{
				for (Pair<T1, T2> p : initialPairs)
				{
					if (contentProvider.getSet(p) == SET3)
					{
						l.add(p);
					}
				}
			}
			return l;
		}

		private class PairSplitLabelProvider extends LabelProvider implements ITableLabelProvider
		{
			Map<String, Set<String>> serverNameToViewNames = new HashMap<String, Set<String>>();

			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == DATA)
				{
					if (element instanceof Pair)
					{
						try
						{
							IServerInternal s = (IServerInternal)((Pair)element).getLeft();
							Set<String> serverViews = serverNameToViewNames.get(s.getName());
							if (serverViews == null)
							{
								serverViews = new HashSet<String>(s.getViewNames(true));
								serverNameToViewNames.put(s.getName(), serverViews);
							}

							if (serverViews.contains(((Pair)element).getRight()))
							{
								return image3;
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
						return image2;
					}
					else
					{
						return image1;
					}
				}
				return getImage(element);
			}

			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == 0)
				{
					if (element instanceof Pair)
					{
						return ((Pair<IServerInternal, String>)element).getRight();
					}
					if (element instanceof ISupportUpdateableName)
					{
						return ((ISupportUpdateableName)element).getName();
					}
				}
				else if (element instanceof Pair)
				{
					return (contentProvider.getSet(element) == columnIndex) ? "x" : "";
				}
				return "";
			}
		}

		public static class RadioEditingSupport<T1, T2> extends EditingSupport
		{

			private final int value;
			private final PairTreeContentProvider<T1, T2> contentProvider;
			private final CellEditor editor;

			public RadioEditingSupport(TreeViewer treeViewer, PairTreeContentProvider<T1, T2> contentProvider, CellEditor editor, int value)
			{
				super(treeViewer);
				this.contentProvider = contentProvider;
				this.value = value;
				this.editor = editor;
			}

			@Override
			protected boolean canEdit(Object element)
			{
				return contentProvider.getSet(element) != NO_CHECKBOX;
			}

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				return editor;
			}

			@Override
			protected Object getValue(Object element)
			{
				return Boolean.valueOf(contentProvider.getSet(element) == value);
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				// un-check is not allowed...
				contentProvider.setSet(element, Integer.valueOf(this.value));
				getViewer().refresh(element);
			}
		}

		public static class PairTreeContentProvider<T1, T2> implements ITreeContentProvider
		{
			private List<Pair<T1, T2>> list;
			private final Hashtable<Object, Integer> choices = new Hashtable<Object, Integer>();

			public Object[] getChildren(Object parentElement)
			{
				if (parentElement == list)
				{
					return getElements(parentElement);
				}
				else
				{
					ArrayList<Pair<T1, T2>> children = new ArrayList<Pair<T1, T2>>();
					for (Pair<T1, T2> pair : list)
					{
						if (parentElement == pair.getLeft())
						{
							children.add(pair);
						}
					}
					return children.toArray();
				}
			}

			public void setSet(Object element, Integer set)
			{
				choices.put(element, set);
			}

			public int getSet(Object element)
			{
				return choices.get(element).intValue();
			}

			public Object getParent(Object element)
			{
				T1 parent = null;
				for (Pair<T1, T2> pair : list)
				{
					if (element == pair)
					{
						parent = pair.getLeft();
						break;
					}
				}
				return parent;
			}

			public boolean hasChildren(Object element)
			{
				boolean found = false;
				for (Pair<T1, T2> pair : list)
				{
					if (element == pair.getLeft())
					{
						found = true;
						break;
					}
				}
				return found;
			}

			public Object[] getElements(Object inputElement)
			{
				List<Pair<T1, T2>> input = (List<Pair<T1, T2>>)inputElement;
				ArrayList<T1> roots = new ArrayList<T1>();
				// return the root elements
				T1 rootElement = null;
				for (Pair<T1, T2> pair : input)
				{
					if (rootElement != pair.getLeft())
					{
						rootElement = pair.getLeft();
						roots.add(rootElement);
						choices.put(rootElement, Integer.valueOf(NO_CHECKBOX));
					}
				}
				return roots.toArray();
			}

			public void dispose()
			{
				list = null;
				choices.clear();
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
			{
				list = (List<Pair<T1, T2>>)newInput;
				choices.clear();
				if (list != null)
				{
					for (Object o : list)
					{
						choices.put(o, Integer.valueOf(SET1));
					}
				}
			}
		}
	}

}
