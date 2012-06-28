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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.dataprocessing.MetaDataUtils.TooManyRowsException;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;

/**
 * 
 * Action to synchronize data for tables that are marked as meta data tables.
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

		setToolTipText("Synchronize meta data for table marked as meta data table between database and workspace");
		setText("Synchronize Meta Data ...");
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

	private void getServerMetadataTables(IServerInternal server)
	{
		try
		{
			List<String> tableNames = server.getTableNames(true);
			for (String tableName : tableNames)
			{
				Table table = server.getTable(tableName);
				if (table.isMarkedAsMetaData() && MetaDataUtils.canBeMarkedAsMetaData(table))
				{
					dataSources.add(table.getDataSource());
				}
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
		if (selectedServer != null)
		{
			dataSources.clear();
			getServerMetadataTables(selectedServer);

			if (dataSources.size() == 0)
			{
				UIUtils.showInformation(shell, "Metadata Synchronize", "The server you selected does not have any metadata tables."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		List<Table> tables = new ArrayList<Table>();
		for (String dataSource : dataSources)
		{
			String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
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
				UIUtils.reportWarning("Error synchronizing table", "Cannot find selected table for datasource '" + dataSource + "'.");
				continue;
			}

			if (!MetaDataUtils.canBeMarkedAsMetaData(table))
			{
				UIUtils.showInformation(shell, "Table not marked as metadata table", "Table '" + table.getName() +
					"' can't be a metadata table because it must have a UUID primary key, a " + MetaDataUtils.METADATA_MODIFICATION_COLUMN + " and a " +
					MetaDataUtils.METADATA_DELETION_COLUMN + " date column.");
				continue;
			}
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
		}
		if (tables.size() > 0)
		{
			switch (new SynchronizeTableDataDialog(shell).open())
			{
				case SynchronizeTableDataDialog.IMPORT_TO_DB :
					importTableData(tables);
					break;

				case SynchronizeTableDataDialog.SAVE_TO_WS :
					generateTableDataFile(tables);
					break;
			}
		}
	}

	private void importTableData(List<Table> tables)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			UIUtils.reportWarning("Error synchronizing table(s)", "Cannot find internal data model manager.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (Table table : tables)
		{
			IFile dataFile = dmm.getMetaDataFile(table.getDataSource());
			if (dataFile == null)
			{
				UIUtils.reportWarning("Error synchronizing table", "Cannot find data file for datasource '" + table.getDataSource() + "'.");
				continue;
			}

			// import file into table
			if (!dataFile.exists())
			{
				UIUtils.reportWarning("Error synchronizing table", "Data file for datasource '" + table.getDataSource() + "' does not exist.");
				continue;
			}

			try
			{
				// check for existing data
				QuerySelect query = MetaDataUtils.createTableMetadataQuery(table, null);
				IDataSet ds = ApplicationServerSingleton.get().getDataServer().performQuery(ApplicationServerSingleton.get().getClientId(),
					table.getServerName(), null, query, null, false, 0, 1, IDataServer.META_DATA_QUERY, null);
				if (ds.getRowCount() > 0 &&
					!UIUtils.askQuestion(shell, "Table is not empty", "Table " + table.getName() + " in server " + table.getServerName() +
						" is not empty, data synchronize will delete existing data, continue?"))
				{
					// don't delete
					continue;
				}

				// read the json
				String contents = new WorkspaceFileAccess(ServoyModel.getWorkspace()).getUTF8Contents(dataFile.getFullPath().toString());

				int ninserted = MetaDataUtils.loadMetadataInTable(table, contents);

				// flush developer clients
				IDebugJ2DBClient debugJ2DBClient = com.servoy.eclipse.core.Activator.getDefault().getDebugJ2DBClient();
				if (debugJ2DBClient != null)
				{
					((FoundSetManager)debugJ2DBClient.getFoundSetManager()).flushCachedDatabaseData(table.getDataSource());
				}
				IDebugWebClient debugWebClient = com.servoy.eclipse.core.Activator.getDefault().getDebugWebClient();
				if (debugWebClient != null)
				{
					((FoundSetManager)debugWebClient.getFoundSetManager()).flushCachedDatabaseData(table.getDataSource());
				}
				sb.append("Successfully saved " + ninserted + " records from workspace in table " + table.getName() + " in server " + table.getServerName());
				sb.append('\n');
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error synchronizing table", e);
				UIUtils.reportWarning("Error synchronizing table", "Error synchronizing table: " + e.getMessage());
			}
		}
		UIUtils.showInformation(shell, "Table synchronization", sb.toString());
	}

	private void generateTableDataFile(List<Table> tables)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			UIUtils.reportWarning("Error synchronizing table(s)", "Cannot find internal data model manager.");
			return;
		}

		StringBuilder sb = new StringBuilder();
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
					UIUtils.reportWarning("Error synchronizing table", "Cannot find data file for datasource '" + table.getDataSource() + "'.");
					continue;
				}
				// if file doesn't exist, this creates the file and its parent directories
				new WorkspaceFileAccess(ServoyModel.getWorkspace()).setUTF8Contents(dataFile.getFullPath().toString(), contents);

				sb.append("Successfully saved records from table " + table.getName() + " in server " + table.getServerName() + " in the workspace");
				sb.append('\n');
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error synchronizing table", e);
				UIUtils.reportWarning("Error synchronizing table", "Error synchronizing table: " + e.getMessage());
			}
		}
		UIUtils.showInformation(shell, "Table synchronization", sb.toString());
	}

}
