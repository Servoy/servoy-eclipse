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

package com.servoy.eclipse.marketplace;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.parser.Content;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;


/**
 * 
 * Content installer for Servoy Marketplace
 * @author gboros
 *
 */
public class ContentInstaller
{
	private final Content content;

	public ContentInstaller(Content content)
	{
		this.content = content;
	}

	public void installAll()
	{
		install(getAllInstallItems());
	}

	public void install(ArrayList<InstallItem> installItems)
	{
		for (final InstallItem installItem : installItems)
		{
			if (MessageDialog.openConfirm(UIUtils.getActiveShell(), "Servoy Marketplace",
				"You have choosen to install the follwing product from the Servoy Marketplace\n\n" + installItem.getName() + "\n\nProceed with the install ?"))
			{
				try
				{
					MarketplaceProgressMonitorDialog progressMonitorDialog = new MarketplaceProgressMonitorDialog(UIUtils.getActiveShell(),
						installItem.getName());
					progressMonitorDialog.run(true, false, new IRunnableWithProgress()
					{

						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
						{
							try
							{
								installItem.install(monitor);
								if (installItem.isRestartRequired())
								{
									Display.getDefault().syncExec(new Runnable()
									{
										public void run()
										{
											if (MessageDialog.openQuestion(UIUtils.getActiveShell(), "Servoy Marketplace",
												"Servoy Developer must be restarted to complete the installation.\n\nDo you want to restart now ?"))
											{
												PlatformUI.getWorkbench().restart();
											}
										}
									});
								}
							}
							catch (final Exception ex)
							{
								Display.getDefault().syncExec(new Runnable()
								{
									public void run()
									{
										Throwable cause = ex.getCause();
										String msg = cause != null ? cause.getMessage() : ex.getMessage();
										MessageDialog.openError(UIUtils.getActiveShell(), "Servoy Marketplace", "Error installing " + installItem.getName() +
											".\n\n" + msg);
									}
								});
							}
						}
					});
				}
				catch (Exception ex1)
				{
					ServoyLog.logError(ex1);
				}
			}
		}
	}

	public ArrayList<InstallItem> getAllInstallItems()
	{
		ArrayList<InstallItem> allInstallItems = new ArrayList<InstallItem>();
		allInstallItems.addAll(getSolutionInstallItems());
		allInstallItems.addAll(getUpdateURLInstallItems());
		allInstallItems.addAll(getTeamProjectSetInstallItems());
		allInstallItems.addAll(getStyleInstallItems());

		return allInstallItems;
	}

	public ArrayList<InstallItem> getSolutionInstallItems()
	{
		ArrayList<InstallItem> solutionInstallItems = new ArrayList<InstallItem>();

		File[] solutionFiles = content.getSolutionFiles(getInstallDir());
		if (solutionFiles != null)
		{
			for (File solutionFile : solutionFiles)
				solutionInstallItems.add(new SolutionInstall(solutionFile));
		}

		return solutionInstallItems;
	}

	public ArrayList<InstallItem> getUpdateURLInstallItems()
	{
		ArrayList<InstallItem> updateURLInstallItems = new ArrayList<InstallItem>();

		if (content.eclipseUpdateSiteURLs != null)
		{
			for (String updateURL : content.eclipseUpdateSiteURLs)
				updateURLInstallItems.add(new UpdateURLInstall(updateURL));
		}

		return updateURLInstallItems;
	}

	public ArrayList<InstallItem> getStyleInstallItems()
	{
		ArrayList<InstallItem> styleInstallItems = new ArrayList<InstallItem>();

		File[] styleFiles = content.getStyleFiles(getInstallDir());
		if (styleFiles != null)
		{
			for (File styleFile : styleFiles)
				styleInstallItems.add(new StyleInstall(styleFile));
		}

		return styleInstallItems;
	}

	public ArrayList<InstallItem> getTeamProjectSetInstallItems()
	{
		ArrayList<InstallItem> teamProjectSetInstallItems = new ArrayList<InstallItem>();

		File[] teamProjectSetFiles = content.getTeamProjectSets(getInstallDir());
		if (teamProjectSetFiles != null)
		{
			for (File teamProjectSetFile : teamProjectSetFiles)
				teamProjectSetInstallItems.add(new TeamProjectSetInstall(teamProjectSetFile));
		}

		return teamProjectSetInstallItems;
	}

	private String getInstallDir()
	{
		String installDir = ApplicationServerSingleton.get().getServoyApplicationServerDirectory();
		installDir = installDir.substring(0, installDir.indexOf("application_server/"));

		return installDir;
	}

	class MarketplaceProgressMonitorDialog extends ProgressMonitorDialog
	{
		private final String installName;

		public MarketplaceProgressMonitorDialog(Shell parent, String installName)
		{
			super(parent);
			this.installName = installName;
		}

		@Override
		protected void configureShell(final Shell shell)
		{
			super.configureShell(shell);
			shell.setText("Servoy Marketplace - Installing " + installName + " ...");
		}
	}
}
