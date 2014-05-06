/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.wizards.UpdateMetaDataWziard;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.dataprocessing.MetaDataUtils.TooManyRowsException;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

/**
 *
 * Action to update data for tables that are marked as meta data tables.
 * The data can be copied from workspace to database table, or from table to workspace.
 *
 * @author rgansevles
 *
 */
public class SynchronizeTableDataAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private final List<String> dataSources = new ArrayList<String>();
	private IServerInternal selectedServer = null;

	public SynchronizeTableDataAction(Shell shell)
	{
		this.shell = shell;

		// it appears right next to another action with the same icon - it's a bit confusing
//		setImageDescriptor(Activator.loadImageDescriptorFromBundle("sync_tables.png")); //$NON-NLS-1$

		setToolTipText("update meta data for table marked as meta data table between database and workspace");
		setText("Update Meta Data ...");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		dataSources.clear();
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = sel.size() > 0;
		@SuppressWarnings("unchecked")
		Iterator<SimpleUserNode> iterator = sel.iterator();
		while (state && iterator.hasNext())
		{
			SimpleUserNode userNode = iterator.next();
			UserNodeType type = userNode.getType();
			if (type == UserNodeType.TABLE)
			{
				TableWrapper tableWrapper = (TableWrapper)userNode.getRealObject();

				String dataSource = tableWrapper.getDataSource();
				if (dataSource != null)
				{
					dataSources.add(dataSource);
				}
				else state = false;
			}
			else if (type == UserNodeType.SERVER && sel.size() == 1)
			{
				IServerInternal server = (IServerInternal)userNode.getRealObject();
				dataSources.clear(); // they will be computed when running
				try
				{
					state = server.getTableNames(true).size() > 0;
				}
				catch (RepositoryException e)
				{
					ServoyLog.logWarning("Cannot get table list for server: " + server.getName(), e); //$NON-NLS-1$
				}
				if (state) selectedServer = server;
				break;
			}
			else
			{
				state = false;
			}
		}
		setEnabled(state);
	}

	private void getServerMetadataTables(IServerInternal server, IProgressMonitor monitor)
	{
		try
		{
			List<String> tableNames = server.getTableNames(true);
			monitor.beginTask("Checking for metadata tables", tableNames.size()); //$NON-NLS-1$
			for (String tableName : tableNames)
			{
				Table table = server.getTable(tableName);
				if (table.isMarkedAsMetaData() && MetaDataUtils.canBeMarkedAsMetaData(table))
				{
					dataSources.add(table.getDataSource());
				}
				monitor.worked(1);
			}
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
	}

	@Override
	public void run()
	{
		Job job = new Job("Generating metadata")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				runImpl(monitor);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(false);
		job.setUser(true);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.schedule();
	}

	private void runImpl(IProgressMonitor monitor)
	{
		if (selectedServer != null)
		{
			dataSources.clear();
			getServerMetadataTables(selectedServer, monitor);

			if (dataSources.size() == 0)
			{
				UIUtils.showInformation(shell, "Metadata update", "The server you selected does not have any metadata tables."); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
		}
		MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);
		List<Table> tables = new ArrayList<Table>();
		monitor.beginTask("Checking for metadata tables", dataSources.size()); //$NON-NLS-1$
		for (String dataSource : dataSources)
		{
			String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
			Table table = null;

			if (stn != null)
			{
				IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(stn[0], true, true);
				if (server != null)
				{
					try
					{
						table = server.getTable(stn[1]);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}

			if (table == null)
			{
				warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Error updating table  " + "Cannot find selected table for datasource '" +
					dataSource + "'."));
				continue;
			}


			if (!MetaDataUtils.canBeMarkedAsMetaData(table))
			{
				warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Table not marked as metadata table " + "Table '" + table.getName() +
					"' can't be a metadata table because it must have a UUID primary key, a " + MetaDataUtils.METADATA_MODIFICATION_COLUMN + " and a " +
					MetaDataUtils.METADATA_DELETION_COLUMN + " date column."));
				continue;
			}
			//the folowing "if" is very rare in practice because when you define : 2 columns of type date with the name modification_date and deletion_date and id of type UUID you usually think about metadata tables
			if (!table.isMarkedAsMetaData())
			{
				if (!UIUtils.askQuestion(shell, "Table not marked as metadata table", "Table " + table.getName() + " in server " + table.getServerName() +
					" is not marked as metadata table; mark now?"))
				{
					// well, then not
					continue;
				}
				table.setMarkedAsMetaData(true);
				IEditorPart tableEditor = EditorUtil.openTableEditor(table);
				if (tableEditor instanceof TableEditor)
				{
					((TableEditor)tableEditor).refresh();
				}
			}
			tables.add(table);
			monitor.worked(1);
		}
		// show warning multi Status
		if (warnings.getChildren().length > 0)
		{
			final MultiStatus fw = warnings;
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ErrorDialog.openError(shell, null, null, fw);
				}
			}, true);
			if (tables.size() == 0) return;
		}
		final int[] retValue = new int[1];

		Display.getDefault().syncExec(new Runnable()
		{

			@Override
			public void run()
			{
				retValue[0] = new SynchronizeTableDataDialog(shell).open();
			}
		});

		if (tables.size() > 0)
		{
			switch (retValue[0])
			{
				case SynchronizeTableDataDialog.IMPORT_TO_DB :
					importTableData(tables);
					break;

				case SynchronizeTableDataDialog.SAVE_TO_WS :
					generateTableDataFile(tables, monitor);
					break;
			}
		}
	}

	private void importTableData(List<Table> tables)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			UIUtils.reportWarning("Error updating table(s)", "Cannot find internal data model manager.");
			return;
		}
		final Pair<List<Table>, List<Table>> result = getTablesThatContainDataInDb(tables);
		if (result.getLeft().isEmpty() && result.getRight().isEmpty())
		{
			UIUtils.reportWarning("Info", "There is no meta data to be imported in the DB.");
			return;
		}

		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				new WizardDialog(shell, new UpdateMetaDataWziard(result.getLeft(), result.getRight(), shell)).open();
			}
		});
	}

	/**
	 * @param tables  - list of mixed tables (tables with data in DB and tables without data in DB)
	 * @return List of tables that contain data in DB
	 */
	private Pair<List<Table>, List<Table>> getTablesThatContainDataInDb(List<Table> tables)
	{
		//tables that contain data in the database
		List<Table> tablesWithDataInDB = new ArrayList<Table>();
		List<Table> tablesWithoutDataInDB = new ArrayList<Table>();

		MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);
		//filter only tables that contain data in their corresponding database
		for (Table table : tables)
		{
			try
			{ // check for existing data
				QuerySelect query = MetaDataUtils.createTableMetadataQuery(table, null);
				IDataSet ds = ApplicationServerSingleton.get().getDataServer().performQuery(ApplicationServerSingleton.get().getClientId(),
					table.getServerName(), null, query, null, false, 0, 1, IDataServer.META_DATA_QUERY, null);
				if (ds.getRowCount() > 0)
				{
					tablesWithDataInDB.add(table);
				}
				else
				{
					tablesWithoutDataInDB.add(table);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error while checking for existing data in DB ", e);
				warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Error while checking for existing data in DB " + e.getMessage()));
			}
		}
		// show warning dialog
		if (warnings.getChildren().length > 0)
		{
			final MultiStatus fw = warnings;
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ErrorDialog.openError(shell, null, null, fw);
				}
			}, false);
		}
		return new Pair<List<Table>, List<Table>>(tablesWithDataInDB, tablesWithoutDataInDB);
	}

	private void generateTableDataFile(List<Table> tables, IProgressMonitor monitor)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			UIUtils.reportWarning("Error updating table(s)", "Cannot find internal data model manager.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);

		monitor.beginTask("Creating metadata files for the tables", tables.size()); //$NON-NLS-1$
		for (Table table : tables)
		{
			try
			{
				final int max = 1000;
				String contents;
				try
				{
					contents = MetaDataUtils.generateMetaDataFileContents(table, max);
				}
				catch (TooManyRowsException e)
				{
					if (!UIUtils.askQuestion(shell, "Big table", "Table " + table.getName() + " in server " + table.getServerName() + " contains more than " +
						max + " rows, are you sure you want to copy this table data in the workspace?"))
					{
						// phew
						continue;
					}

					// ok you've been warned...
					contents = MetaDataUtils.generateMetaDataFileContents(table, -1);
				}

				IFile dataFile = dmm.getMetaDataFile(table.getDataSource());
				if (dataFile == null)
				{
					warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot find data file for datasource '" + table.getDataSource() + "'."));
					continue;
				}
				// if file doesn't exist, this creates the file and its parent directories
				new WorkspaceFileAccess(ServoyModel.getWorkspace()).setUTF8Contents(dataFile.getFullPath().toString(), contents);

				sb.append("Successfully saved records from table " + table.getName() + " in server " + table.getServerName() + " in the workspace");
				sb.append('\n');
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error updating table", e);
				warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Error updating table", e));
			}
			monitor.worked(1);
		}

		/*
		 * handle the updating result
		 */
		// show warning status messages in eclipse Platform UI way
		if (warnings.getChildren().length > 0)
		{
			final MultiStatus fw = warnings;
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ErrorDialog.openError(shell, null, null, fw);
				}
			}, false);
		}

		if (sb.length() > 1)
		{
			UIUtils.showScrollableDialog(shell, IMessageProvider.INFORMATION, "Workspace Update status", "The folowing workspace table data were updated:",
				sb.toString());
		}
		else
		{
			UIUtils.showInformation(shell, "No tables were updated", "No tables in the workspace were updated.");
		}

	}
}
