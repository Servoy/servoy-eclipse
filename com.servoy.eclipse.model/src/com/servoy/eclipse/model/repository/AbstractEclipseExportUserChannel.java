package com.servoy.eclipse.model.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;

public abstract class AbstractEclipseExportUserChannel implements IXMLExportUserChannel
{
	protected final IExportSolutionModel exportModel;
	protected final IProgressMonitor monitor;

	public AbstractEclipseExportUserChannel(IExportSolutionModel exportModel, IProgressMonitor monitor)
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

	@Override
	public void clientInfo(String message, int priority)
	{
		this.info(message, priority);
	}

	public boolean getExportAllTablesFromReferencedServers()
	{
		return exportModel.isExportAllTablesFromReferencedServers();
	}

	public String getTableMetaData(ITable table) throws IOException
	{
		DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			displayWarningMessage("Error exporting table meta data", "Cannot find internal data model manager.", false);
			return null;
		}

		WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		String metadatapath = dmm.getMetaDataFile(table.getDataSource()).getFullPath().toString();
		if (!wsa.exists(metadatapath))
		{
			throw new IOException("Checking table meta data failed for table '" + table.getName() + "' in server '" + table.getServerName() +
				"', current workspace does not have table meta data file.\n" + //
				"Update the meta data for this table first.");
		}
		String wscontents = wsa.getUTF8Contents(metadatapath); // this cannot currently return null (it would throw an exception rather then return null)

		if (wscontents != null && exportModel.isCheckMetadataTables() && table instanceof Table)
		{
			// check if current contents matches data file
			String dbcontents;
			try
			{
				dbcontents = MetaDataUtils.generateMetaDataFileContents(table, -1);
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
					"Update the meta data for this table first.");
			}
		}

		return wscontents;
	}
}
