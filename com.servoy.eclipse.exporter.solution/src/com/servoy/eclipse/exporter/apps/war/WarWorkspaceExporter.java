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
import java.util.List;
import java.util.SortedSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
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
			public boolean isExportActiveSolutionOnly()
			{
				return true;
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
				return getFiles(ApplicationServerRegistry.get().getPluginManager().getPluginsDir());
			}

			@Override
			public List<String> getLafs()
			{
				return getFiles(ApplicationServerRegistry.get().getLafManager().getLAFDir());
			}

			@Override
			public String getFileName()
			{
				return configuration.getExportFilePath() + File.separator + configuration.getSolutionNames()[0] + ".war";
			}

			@Override
			public List<String> getDrivers()
			{
				return getFiles(ApplicationServerRegistry.get().getServerManager().getDriversDir());
			}

			@Override
			public List<String> getBeans()
			{
				return getFiles(ApplicationServerRegistry.get().getBeanManager().getBeansDir());
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

			List<String> getFiles(File dir)
			{
				return Arrays.asList(dir.list(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}));
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
			outputError("Exception while exporting solution. EXPORT FAILED for this solution. Check workspace log."); //$NON-NLS-1$
			exitCode = EXIT_EXPORT_FAILED;
		}
	}
}
