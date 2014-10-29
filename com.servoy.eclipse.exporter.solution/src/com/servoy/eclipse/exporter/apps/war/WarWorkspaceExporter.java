/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.exporter.apps.war;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.ExportException;
import com.servoy.eclipse.model.war.exporter.IWarExportModel;
import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Eclipse application that can be used for exporting servoy solutions in .war format.
 * @author gboros
 */
public class WarWorkspaceExporter extends AbstractWorkspaceExporter<WarArgumentChest>
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter#createArgumentChest(org.eclipse.equinox.app.IApplicationContext)
	 */
	@Override
	protected WarArgumentChest createArgumentChest(IApplicationContext context)
	{
		return new WarArgumentChest((String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter#exportActiveSolution(com.servoy.eclipse.exporter.apps.common.IArgumentChest)
	 */
	@Override
	protected void exportActiveSolution(final WarArgumentChest configuration)
	{
		WarExporter warExporter = new WarExporter(new IWarExportModel()
		{
			@Override
			public boolean isExportActiveSolution()
			{
				return configuration.isExportActiveSolutionOnly();
			}

			@Override
			public String getStartRMIPort()
			{
				return null;
			}

			@Override
			public boolean getStartRMI()
			{
				return false;
			}

			@Override
			public String getServoyPropertiesFileName()
			{
				String servoyPropertiesFileName = configuration.getSettingsFileName();
				if (servoyPropertiesFileName == null)
				{
					servoyPropertiesFileName = getServoyApplicationServerDir() + File.separator + "servoy.properties";
				}
				return servoyPropertiesFileName;
			}

			@Override
			public ServerConfiguration getServerConfiguration(String serverName)
			{
				return null;
			}

			@Override
			public SortedSet<String> getSelectedServerNames()
			{
				return null;
			}

			@Override
			public List<String> getPlugins()
			{
				Set<String> names = null;
				if (configuration.getPlugins() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getPlugins().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getPluginManager().getPluginsDir(), names);
			}

			@Override
			public List<String> getLafs()
			{
				Set<String> names = null;
				if (configuration.getLafs() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getLafs().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getLafManager().getLAFDir(), names);
			}

			@Override
			public String getWarFileName()
			{
				ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
				return configuration.getExportFilePath() + File.separator + activeProject.getProject().getName() + ".war";
			}

			@Override
			public List<String> getDrivers()
			{
				Set<String> names = null;
				if (configuration.getDrivers() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getDrivers().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getServerManager().getDriversDir(), names);
			}

			@Override
			public List<String> getBeans()
			{
				Set<String> names = null;
				if (configuration.getBeans() != null)
				{
					names = new HashSet<String>(Arrays.asList(configuration.getBeans().toLowerCase().split(" ")));
				}
				return getFiles(ApplicationServerRegistry.get().getBeanManager().getBeansDir(), names);
			}

			@Override
			public boolean allowOverwriteSocketFactoryProperties()
			{
				return false;
			}

			@Override
			public String getServoyApplicationServerDir()
			{
				return configuration.getAppServerDir();
			}

			List<String> getFiles(File dir, final Set<String> fileNames)
			{
				return Arrays.asList(dir.list(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						boolean accept = fileNames != null ? fileNames.contains(name.toLowerCase()) : true;
						return accept && (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"));
					}
				}));
			}

			@Override
			public List<String> getPluginLocations()
			{
				return Arrays.asList(configuration.getPluginLocations().split(" "));
			}
		});
		try
		{
			warExporter.doExport(new IProgressMonitor()
			{
				@Override
				public void worked(int work)
				{
				}

				@Override
				public void subTask(String name)
				{
					outputExtra(name);
				}

				@Override
				public void setTaskName(String name)
				{
					outputExtra(name);
				}

				@Override
				public void setCanceled(boolean value)
				{
				}

				@Override
				public boolean isCanceled()
				{
					return false;
				}

				@Override
				public void internalWorked(double work)
				{
				}

				@Override
				public void done()
				{
				}

				@Override
				public void beginTask(String name, int totalWork)
				{
					outputExtra(name);
				}
			});
		}
		catch (ExportException ex)
		{
			ServoyLog.logError("Failed to export solution.", ex);
			outputError("Exception while exporting solution. EXPORT FAILED for this solution. Check workspace log.");
			exitCode = EXIT_EXPORT_FAILED;
		}
	}
}
