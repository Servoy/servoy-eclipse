/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.model.export;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.model.repository.EclipseExportI18NHelper;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.xmlxport.IMetadataDefManager;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsAndSecurityBasedOnWorkspaceFiles;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;
import com.servoy.j2db.util.xmlxport.IXMLExporter;

/**
 * Used by various exporters (file exporter, cmd line file exporter, war exporter and cmd line war exporter) to export a solution as file.
 * @author emera
 */
public class SolutionExporter
{

	/**
	 * Does some checks if the sample data and metadata can be exported if the export model requires so,
	 * then exports a solution as file.
	 * @param activeSolution the solution to be exported
	 * @param exportFile the file where to export the solution
	 * @param exportModel the export model for the solution
	 * @param eeI18NHelper the I18nHelper to get the i18n file names and content
	 * @param userChannel the channel used to log/display messages to the user
	 * @param modulesWebPackages the web packages to export
	 * @param dbDown true if the db is down (or some needed servers/tables are not accessible)
	 * @param exportVersions a flag for exporting the versions (usually true for file exporter and false for war exporter)
	 * @param exportSolution include the active solution (if true a solution.xml and revision_info.xml are added to the resulting archive)
	 *
	 * @throws RepositoryException
	 * @throws JSONException
	 * @throws CoreException
	 * @throws IOException
	 */
	public static void exportSolutionToFile(Solution activeSolution, File exportFile, IExportSolutionModel exportModel,
		EclipseExportI18NHelper eeI18NHelper, IXMLExportUserChannel userChannel,
		Map<String, List<File>> modulesWebPackages, boolean dbDown, boolean exportVersions, boolean exportSolution)
		throws RepositoryException, JSONException, CoreException, IOException
	{
		final IApplicationServerSingleton as = ApplicationServerRegistry.get();
		IUserManager sm = as.getUserManager();
		AbstractRepository rep = (AbstractRepository)ApplicationServerRegistry.get().getDeveloperRepository();
		IXMLExporter exporter = as.createXMLExporter(rep, sm, userChannel, Settings.getInstance(), as.getDataServer(), as.getClientId(), eeI18NHelper);

		ITableDefinitionsAndSecurityBasedOnWorkspaceFiles tableDefAndSecFromFiles = null;
		IMetadataDefManager metadataDefManager = null;
		Pair<ITableDefinitionsAndSecurityBasedOnWorkspaceFiles, IMetadataDefManager> defManagers = TableDefinitionUtils.getTableDefinitionsFromDBI(
			activeSolution, exportModel.isExportReferencedModules(), exportModel.isExportI18NData(), exportModel.isExportAllTablesFromReferencedServers(),
			exportModel.isExportMetaData());
		if (defManagers != null)
		{
			tableDefAndSecFromFiles = defManagers.getLeft();
			metadataDefManager = defManagers.getRight();
		}
		final String[] warningMessage = new String[] { "" };
		if (exportModel.isExportSampleData() && dbDown)
		{
			warningMessage[0] = "Skipping sample data export, solution or module has non accesible database.";
		}
		if (exportModel.isExportMetaData() && dbDown)
		{
			warningMessage[0] = warningMessage[0] + "\nSkipping metadata export, solution or module has non accesible database.";
		}
		if (!"".equals(warningMessage[0]))
		{
			userChannel.displayWarningMessage("Export solution from database files (database server not accesible)", warningMessage[0], false);
		}

		exporter.exportSolutionToFile(activeSolution, exportFile, ClientVersion.getVersion(), ClientVersion.getReleaseNumber(),
			exportModel.isExportMetaData() && !dbDown, exportModel.isExportSampleData() && !dbDown, exportModel.getNumberOfSampleDataExported(),
			exportModel.isExportI18NData(), exportModel.isExportUsers(), exportModel.isExportReferencedModules(), exportModel.isProtectWithPassword(),
			tableDefAndSecFromFiles, exportModel.isExportUsingDbiFileInfoOnly(), metadataDefManager, exportSolution,
			exportModel.useImportSettings() ? exportModel.getImportSettings() : null,
			modulesWebPackages, exportVersions);
	}
}
