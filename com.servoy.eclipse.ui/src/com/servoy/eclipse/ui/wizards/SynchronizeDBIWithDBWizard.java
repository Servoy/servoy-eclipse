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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.DataModelManager;
import com.servoy.eclipse.core.util.DatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IColumnInfoBasedSequenceProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * Wizard that allows the user to check for .dbi files that do not have a corresponding table in the database. If such files are found, the user can choose
 * either to create a new table according to that file, or delete the file.<BR>
 * This wizard will also automatically create default .dbi files for tables that exist, but do not have a corresponding file.
 * 
 * @author Andrei Costescu
 */
public class SynchronizeDBIWithDBWizard extends Wizard implements IWorkbenchWizard
{

	private WizardPage errorPage;
	private SplitInThreeWizardPage<IServerInternal, String> page1;
	private SplitInThreeWizardPage<IServerInternal, String> page2;
	private CheckBoxWizardPage page3;
	private final List<IServerInternal> servers = new ArrayList<IServerInternal>();
	private DataModelManager dmm;
	private IWorkbenchPage activePage;

	public SynchronizeDBIWithDBWizard()
	{
		setWindowTitle("Synchronize DB tables with DB information");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("sync_tables_large.png"));
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setNeedsProgressMonitor(true);
		activePage = (workbench != null && workbench.getActiveWorkbenchWindow() != null) ? workbench.getActiveWorkbenchWindow().getActivePage() : null;

		servers.clear();
		dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IServerManagerInternal sm = ServoyModel.getServerManager();
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
			errorPage.setErrorMessage("Synchronizing database tables with the database information files requires an active resources project.\nThe database information files (.dbi) are located in resources projects.");
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
			boolean serversIdentified;
			if (selection instanceof IStructuredSelection)
			{
				serversIdentified = true;
				IStructuredSelection sel = selection;
				Iterator it = selection.iterator();
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
			}
			else
			{
				serversIdentified = false;
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
						else if (o1 == null) return -1;
						else if (o2 == null) return 1;
						else
						{
							int result = o1.getLeft().getName().compareToIgnoreCase(o2.getLeft().getName());
							if (result == 0)
							{
								result = o1.getRight().compareToIgnoreCase(o2.getRight());
							}
							return result;
						}
					}
				};

				Image serverImage = Activator.getDefault().loadImageFromBundle("server.gif");
				Image tableImage = Activator.getDefault().loadImageFromBundle("portal.gif");

				if (foundMissingTables.size() > 0)
				{
					page1 = new SplitInThreeWizardPage<IServerInternal, String>(
						"Missing tables",
						"Database information files (.dbi from resources project) can point to tables that do not exist in the database.\nYou can choose to create those tables according to the information or delete the unwanted information files.",
						"Ignore", "Create table", "Delete .dbi", "Ignore all", "Create all", "Delete all", foundMissingTables, comparator, serverImage,
						tableImage);
				}
				else
				{
					page1 = null;
				}
				if (foundSupplementalTables.size() > 0)
				{
					page2 = new SplitInThreeWizardPage<IServerInternal, String>(
						"Missing database information files",
						"Tables in the database can lack an associated database information file (.dbi in the resources project).\nYou can choose to create the database information file or delete the table from the database.",
						"Ignore", "Create .dbi", "Delete table", "Ignore all", "Create all", "Delete all", foundSupplementalTables, comparator, serverImage,
						tableImage);
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
			page3 = new CheckBoxWizardPage(
				"Synchronize at column level",
				"For tables that exist in the database and also have a database information file in the resources project,\ncolumn information can differ.",
				"Column differences between the DB and the DB information files are only noticed when the DB information files are read.\nAs the files are read only when needed, you might want to trigger a load in order to see the differences\nin the 'Problems' view.",
				"Read/check existing DB information files for existing tables", true);
		}
	}

	private List<Pair<IServerInternal, String>> getMissingTables(List<IServerInternal> servers, DataModelManager dmm)
	{
		// choose which of the missing tables (tables that do not exist in the DB but for which there are .dbi files)
		// will be created and which of the .dbi files are no longer wanted.
		final List<Pair<IServerInternal, String>> foundMissingTables = new ArrayList<Pair<IServerInternal, String>>();
		for (final IServerInternal s : servers)
		{
			IFolder serverInformationFolder = dmm.getDBIFileContainer(s.getName());
			if (serverInformationFolder.exists())
			{
				try
				{
					final List<String> tableNames = s.getTableAndViewNames();
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
								if (!tableNames.contains(tableName))
								{
									if (tableName.toLowerCase().equals(tableName))
									{
										foundMissingTables.add(new Pair<IServerInternal, String>(s, tableName));
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

	private List<Pair<IServerInternal, String>> getSupplementalTables(List<IServerInternal> servers, DataModelManager dmm)
	{
		// choose which of the missing files (tables that exist in the DB but have no corresponding .dbi file) will be created
		// and which of the tables corresponding to these missing files will be deleted 
		List<Pair<IServerInternal, String>> foundSupplementalTables = new ArrayList<Pair<IServerInternal, String>>();
		for (IServerInternal s : servers)
		{
			IFolder serverInformationFolder = dmm.getDBIFileContainer(s.getName());
			try
			{
				for (String tableName : s.getTableAndViewNames())
				{
					if (!serverInformationFolder.getFile(tableName + DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT).exists())
					{
						foundSupplementalTables.add(new Pair<IServerInternal, String>(s, tableName));
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
	}

	@Override
	public boolean performFinish()
	{
		try
		{
			getContainer().run(true, false, new IRunnableWithProgress()
			{
				public void run(IProgressMonitor m) throws InvocationTargetException, InterruptedException
				{
					try
					{
						ServoyModel.getWorkspace().run(new IWorkspaceRunnable()
						{
							public void run(IProgressMonitor monitor) throws CoreException
							{
								int work1 = (page1 != null ? page1.getSet2().size() + page1.getSet3().size() : 0);
								int work2 = (page2 != null ? page2.getSet2().size() + page2.getSet3().size() : 0);
								int work3 = (page3 != null ? (page3.isChecked() ? countTables() : 0) : 0);

								monitor.beginTask("Synchronizing database with database information files", work1 + work2 + work3);
								try
								{
									MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);
									if (page1 != null)
									{
										monitor.subTask("- handling missing tables");
										handleMissingTables(page1.getSet2(), page1.getSet3(), new SubProgressMonitor(monitor, work1), warnings, work1);
									}
									if (page2 != null)
									{
										monitor.subTask("- handling supplemental tables");
										handleSupplementalTables(page2.getSet2(), page2.getSet3(), new SubProgressMonitor(monitor, work2), warnings, work2);
									}
									if (page3 != null && page3.isChecked())
									{
										monitor.subTask("- read and check database information");
										readAndCheckDatabaseInformation(new SubProgressMonitor(monitor, work3), warnings, work3);
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
								}
								finally
								{
									monitor.done();
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
							count += s.getTableAndViewNames().size();
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
					try
					{
						monitor.subTask("- creating missing tables");
						for (Pair<IServerInternal, String> tableToCreate : tablesToCreate)
						{
							IFile file = dmm.getDBIFile(tableToCreate.getLeft().getName(), tableToCreate.getRight());
							if (file.exists())
							{
								try
								{
									InputStream is = file.getContents(true);
									String dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
									Utils.closeInputStream(is);
									String problems = DatabaseUtils.createNewTableFromColumnInfo(tableToCreate.getLeft(), tableToCreate.getRight(),
										dbiFileContent, false);
									if (problems != null)
									{
										StringTokenizer st = new StringTokenizer(problems, "\n");
										while (st.hasMoreTokens())
										{
											warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, st.nextToken()));
										}
									}
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
								warnings.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot find .dbi file to create missing table " +
									tableToCreate.getLeft().getName() + " - " + tableToCreate.getRight()));
							}
							monitor.worked(1);
						}

						monitor.subTask("- delete database information files for missing tables");
						for (Pair<IServerInternal, String> dbiFileToDelete : dbiFilesToDelete)
						{
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
						monitor.subTask("- creating missing .dbi files");
						for (Pair<IServerInternal, String> dbiFileToCreate : dbiFilesToCreate)
						{
							try
							{
								// make sure the table is loaded
								Table t = dbiFileToCreate.getLeft().getTable(dbiFileToCreate.getRight());
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
					finally
					{
						monitor.subTask("");
						monitor.done();
					}
				}

				private void readAndCheckDatabaseInformation(IProgressMonitor monitor, MultiStatus warnings, int workUnits)
				{
					monitor.beginTask("- read and check database information files", workUnits);
					monitor.subTask("- read and check database information files");
					try
					{
						for (IServerInternal s : servers)
						{
							for (String tableName : s.getTableAndViewNames())
							{
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
		private final String allSet1Label;
		private final String allSet2Label;
		private final String allSet3Label;

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
			String allSet2Label, String allSet3Label, List<Pair<T1, T2>> fatherChildrenPairs, Comparator<Pair<T1, T2>> c, Image image1, Image image2)
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
			treeViewer = new TreeViewer(treeComposite, SWT.BORDER | SWT.FULL_SELECTION);

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
			TreeViewerColumn dataViewerColumn = new TreeViewerColumn(treeViewer, dataColumn);

			TreeColumn set1Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET1);
			set1Column.setText(labelForSet1);
			TreeViewerColumn set1ViewerColumn = new TreeViewerColumn(treeViewer, set1Column);
			set1ViewerColumn.setEditingSupport(new RadioEditingSupport(treeViewer, contentProvider, editor, SET1));

			TreeColumn set2Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET2);
			set2Column.setText(labelForSet2);
			TreeViewerColumn set2ViewerColumn = new TreeViewerColumn(treeViewer, set2Column);
			set2ViewerColumn.setEditingSupport(new RadioEditingSupport(treeViewer, contentProvider, editor, SET2));

			TreeColumn set3Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET3);
			set3Column.setText(labelForSet3);
			TreeViewerColumn set3ViewerColumn = new TreeViewerColumn(treeViewer, set3Column);
			set3ViewerColumn.setEditingSupport(new RadioEditingSupport(treeViewer, contentProvider, editor, SET3));

			treeViewer.setLabelProvider(new PairSplitLabelProvider());
			treeViewer.expandAll();

			Button allInSet1 = new Button(topLevel, SWT.NONE);
			Button allInSet2 = new Button(topLevel, SWT.NONE);
			Button allInSet3 = new Button(topLevel, SWT.NONE);

			allInSet1.setText(allSet1Label);
			allInSet2.setText(allSet2Label);
			allInSet3.setText(allSet3Label);

			allInSet1.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					moveAllToSet(SET1);
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
					moveAllToSet(SET2);
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
					moveAllToSet(SET3);
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

		private void moveAllToSet(int set)
		{
			for (Pair<T1, T2> p : initialPairs)
			{
				contentProvider.setSet(p, set);
			}
			treeViewer.refresh();
		}

		public List<Pair<T1, T2>> getSet1()
		{
			ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
			for (Pair<T1, T2> p : initialPairs)
			{
				if (contentProvider.getSet(p) == SET1)
				{
					l.add(p);
				}
			}
			return l;
		}

		public List<Pair<T1, T2>> getSet2()
		{
			ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
			for (Pair<T1, T2> p : initialPairs)
			{
				if (contentProvider.getSet(p) == SET2)
				{
					l.add(p);
				}
			}
			return l;
		}

		public List<Pair<T1, T2>> getSet3()
		{
			ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
			for (Pair<T1, T2> p : initialPairs)
			{
				if (contentProvider.getSet(p) == SET3)
				{
					l.add(p);
				}
			}
			return l;
		}

		private class PairSplitLabelProvider extends LabelProvider implements ITableLabelProvider
		{

			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == DATA)
				{
					if (element instanceof Pair)
					{
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
					else if (element instanceof ISupportUpdateableName)
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

		private class RadioEditingSupport extends EditingSupport
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
				return contentProvider.getSet(element) == value;
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				// un-check is not allowed...
				contentProvider.setSet(element, this.value);
				treeViewer.refresh(element);
			}

		}

		private class PairTreeContentProvider<T1, T2> implements ITreeContentProvider
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
				return choices.get(element);
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
						choices.put(rootElement, NO_CHECKBOX);
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
						choices.put(o, SET1);
					}
				}
			}

		}

	}

}
