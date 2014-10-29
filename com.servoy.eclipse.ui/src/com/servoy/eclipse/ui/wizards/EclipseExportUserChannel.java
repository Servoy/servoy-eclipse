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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;

public class EclipseExportUserChannel implements IXMLExportUserChannel
{
	private final IExportSolutionModel exportModel;
	private final IProgressMonitor monitor;

	public EclipseExportUserChannel(IExportSolutionModel exportModel, IProgressMonitor monitor)
	{
		this.exportModel = exportModel;
		this.monitor = monitor;
	}

	public <T extends List<String>> T getModuleIncludeList(T allModules)
	{
		String[] modules = exportModel.getModulesToExport();
		allModules.retainAll(Arrays.asList(modules != null ? modules : new String[] { }));
		return allModules;
	}

	public String getProtectionPassword()
	{
		return exportModel.getPassword();
	}

	public void info(String message, int priority)
	{
		// Seems that for each referenced module we get one message with priority INFO.
		// We send these to the progress monitor.
		if (priority == ILogLevel.INFO)
		{
			monitor.setTaskName(message);
			monitor.worked(1);
		}
	}

	public boolean getExportAllTablesFromReferencedServers()
	{
		return exportModel.isExportAllTablesFromReferencedServers();
	}

	public String getTableMetaData(ITable table) throws IOException
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			UIUtils.reportWarning("Error exporting table meta data", "Cannot find internal data model manager.");
			return null;
		}

		WorkspaceFileAccess wsa = new WorkspaceFileAccess(ServoyModel.getWorkspace());
		String metadatapath = dmm.getMetaDataFile(table.getDataSource()).getFullPath().toString();
		if (!wsa.exists(metadatapath))
		{
			throw new IOException("Checking table meta data failed for table '" + table.getName() + "' in server '" + table.getServerName() +
				"', current workspace does not have table meta data file.\n" + //
				"Update the meta data for this table first");
		}
		String wscontents = wsa.getUTF8Contents(metadatapath);

		if (wscontents != null && exportModel.isCheckMetadataTables() && table instanceof Table)
		{
			// check if current contents matches data file
			String dbcontents;
			try
			{
				dbcontents = MetaDataUtils.generateMetaDataFileContents((Table)table, -1);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				throw new IOException("Could not check table meta data from database " + e.getMessage());
			}
			if (!wscontents.equals(dbcontents))
			{
				throw new IOException("Checking table meta data failed for table '" + table.getName() + "' in server '" + table.getServerName() +
					"', current workspace contents does not match current database contents.\n" + //
					"Update the meta data for this table first");
			}
		}

		return wscontents;
	}
}
