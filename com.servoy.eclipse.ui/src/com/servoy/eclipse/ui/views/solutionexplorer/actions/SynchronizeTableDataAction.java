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
import com.servoy.eclipse.model.repository.DataModelManager.TooManyRowsException;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.ISQLStatement;
import com.servoy.j2db.dataprocessing.SQLStatement;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.DataSourceUtils;

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
	private String dataSource;

	public SynchronizeTableDataAction(Shell shell)
	{
		this.shell = shell;
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("sync_tables.png")); //$NON-NLS-1$
		setToolTipText("Synchronize meta data for table marked as meta data table between database and workspace");
		setText("Synchronize Meta Data ...");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.TABLE)
			{
				SimpleUserNode userNode = (SimpleUserNode)sel.getFirstElement();
				TableWrapper tableWrapper = (TableWrapper)userNode.getRealObject();

				dataSource = tableWrapper.getDataSource();
				state = dataSource != null;
			}
			else
			{
				state = false;
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
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
			return;
		}

		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			UIUtils.reportWarning("Error synchronizing table", "Cannot find internal data model manager.");
			return;
		}

		if (!table.isMarkedAsMetaData())
		{
			if (!UIUtils.askQuestion(shell, "Table not marked as metadata table", "Table " + table.getName() + " in server " + table.getServerName() +
				" is not marked as metadata table, mark now?"))
			{
				// well, then not
				return;
			}
			table.setMarkedAsMetaData(true);
			IEditorPart tableEditor = EditorUtil.openTableEditor(table);
			if (tableEditor instanceof TableEditor)
			{
				((TableEditor)tableEditor).refresh();
			}
		}

		IFile dataFile = dmm.getTableDataFile(dataSource);
		if (dataFile == null)
		{
			UIUtils.reportWarning("Error synchronizing table", "Cannot find data file for datasource '" + dataSource + "'.");
			return;
		}

		switch (new SynchronizeTableDataDialog(shell).open())
		{
			case SynchronizeTableDataDialog.IMPORT_TO_DB :
				importTableData(table, dmm, dataFile);
				break;

			case SynchronizeTableDataDialog.SAVE_TO_WS :
				generateTableDataFile(table, dmm, dataFile);
				break;
		}
	}

	/**
	 * @param table
	 * @param dmm
	 * @param dataFile
	 */
	private void importTableData(Table table, DataModelManager dmm, IFile dataFile)
	{
		// import file into table
		if (!dataFile.exists())
		{
			UIUtils.reportWarning("Error synchronizing table", "Data file for datasource '" + dataSource + "' does not exist.");
			return;
		}

		try
		{
			// check for existing data
			QuerySelect query = dmm.createTableMetadataQuery(table, null);
			IDataSet ds = ApplicationServerSingleton.get().getDataServer().performQuery(ApplicationServerSingleton.get().getClientId(), table.getServerName(),
				null, query, null, false, 0, 1, IDataServer.RAW_QUERY, null);
			if (ds.getRowCount() > 0 &&
				!UIUtils.askQuestion(shell, "Table is not empty", "Table " + table.getName() + " in server " + table.getServerName() +
					" is not empty, data synchronize will delete existing data, continue?"))
			{
				// don't delete
				return;
			}

			// read the json
			String contents = new WorkspaceFileAccess(ServoyModel.getWorkspace()).getUTF8Contents(dataFile.getFullPath().toString());

			// parse dataset
			BufferedDataSet dataSet = dmm.deserializeTableMetaDataContents(contents);

			// delete existing data
			ApplicationServerSingleton.get().getDataServer().performUpdates(ApplicationServerSingleton.get().getClientId(),
				new ISQLStatement[] { new SQLStatement(IDataServer.RAW_QUERY, table.getServerName(), table.getName(), null, //
					new QueryDelete(query.getTable())) // delete entire table
				});
			// insert the data
			ApplicationServerSingleton.get().getDataServer().insertDataSet(ApplicationServerSingleton.get().getClientId(), dataSet, table.getDataSource(),
				table.getServerName(), table.getName(), null, null);

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

			UIUtils.showInformation(shell, "Table synchronization",
				"Successfully saved " + dataSet.getRowCount() + " records from workspace in table " + table.getName() + " in server " + table.getServerName());
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error synchronizing table", e);
			UIUtils.reportWarning("Error synchronizing table", "Error synchronizing table: " + e.getMessage());
		}
	}

	/**
	 * @param table
	 * @param dmm
	 * @param dataFile
	 */
	private void generateTableDataFile(Table table, DataModelManager dmm, IFile dataFile)
	{
		try
		{
			final int max = 1000;
			String contents;
			try
			{
				contents = dmm.generateMetaDataFileContents(table, max);
			}
			catch (TooManyRowsException e)
			{
				if (!UIUtils.askQuestion(shell, "Big table", "Table " + table.getName() + " in server " + table.getServerName() + " contains more than " + max +
					" rows, are you sure you want to copy this table data in the workspace?"))
				{
					// phew
					return;
				}

				// ok you've been warned...
				contents = dmm.generateMetaDataFileContents(table, -1);
			}

			// if file doesn't exist, this creates the file and its parent directories
			new WorkspaceFileAccess(ServoyModel.getWorkspace()).setUTF8Contents(dataFile.getFullPath().toString(), contents);

			UIUtils.showInformation(shell, "Table synchronization",
				"Successfully saved records from table " + table.getName() + " in server " + table.getServerName() + " in the workspace");
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error synchronizing table", e);
			UIUtils.reportWarning("Error synchronizing table", "Error synchronizing table: " + e.getMessage());
		}
	}

}
