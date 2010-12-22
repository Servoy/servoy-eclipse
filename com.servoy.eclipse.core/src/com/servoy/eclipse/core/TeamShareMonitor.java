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
package com.servoy.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.core.IRepositoryProviderListener;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.MessageAndCheckBoxDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * The in-process repository is only meant to work by itself - so all Servoy related projects in the workspace should either not be attached to team or attached
 * to the in-process repository (because database information and sequence provider are the standard table based ones - using the in-process repository - not
 * the resources project).
 * 
 * This class monitors/checks existing Servoy related projects (solutions/resources) - and when it detects such a project is linked to a team provider other
 * than the in-process repository Servoy team provider, it will pop-up a warning dialog - if allowed by plugin settings flag.
 * 
 * It is only meant to be used when the in-process repository is activated.
 * 
 * @author acostescu
 * 
 */
public class TeamShareMonitor
{

	/**
	 * Identifier for the "warn when non-in process team share detected" boolean plugin preference.
	 */
	public static final String WARN_ON_NON_IN_PROCESS_TEAM_SHARE = "WARN_ON_NON_IN_PROCESS_TEAM_SHARE";
	private static final String SERVOY_TEAM_PROVIDER_ID = "com.servoy.eclipse.team.servoynature";

	private boolean warn = false;
	private NewShareListener newShareListener;
	private TeamShareMonitorExtension shareMonitorExtension;
	private final ServoyModel sm;
	private boolean ignoreSetExtensionCheck = false;
	private boolean dialogShown = false;

	public TeamShareMonitor(ServoyModel sm)
	{
		this.sm = sm;
		// read "warn" flag
		Preferences pluginPreferences = Activator.getDefault().getPluginPreferences();
		setAllowWarningDialogInternal(pluginPreferences.getBoolean(WARN_ON_NON_IN_PROCESS_TEAM_SHARE));
	}

	public void setAllowWarningDialog(boolean newWarn)
	{
		Preferences pluginPreferences = Activator.getDefault().getPluginPreferences();
		pluginPreferences.setValue(WARN_ON_NON_IN_PROCESS_TEAM_SHARE, newWarn);
		Activator.getDefault().savePluginPreferences();
		setAllowWarningDialogInternal(newWarn);
	}

	public synchronized void setShareMonitorExtension(TeamShareMonitorExtension shareMonitorExtension)
	{
		this.shareMonitorExtension = shareMonitorExtension;
		if (shareMonitorExtension != null)
		{
			synchronized (this)
			{
				if (!ignoreSetExtensionCheck && warn)
				{
					checkCurrentServoyRelatedProjects();
				}
			}
		}
	}

	private void setAllowWarningDialogInternal(boolean newWarn)
	{
		boolean oldWarn = warn;
		warn = newWarn;

		if (oldWarn == true)
		{
			if (newWarn == false && newShareListener != null)
			{
				RepositoryProviderManager.getInstance().removeListener(newShareListener);
				newShareListener = null;
			}
		}
		else if (newWarn == true)
		{
			// listen for new team shares
			newShareListener = new NewShareListener();
			RepositoryProviderManager.getInstance().addListener(newShareListener);

			// check of all Servoy related projects now
			checkCurrentServoyRelatedProjects();
		}
	}

	private synchronized void checkCurrentServoyRelatedProjects()
	{
		for (ServoyProject sp : sm.getServoyProjects())
		{
			RepositoryProvider provider = RepositoryProvider.getProvider(sp.getProject());
			if (provider == null) continue;
			if (!SERVOY_TEAM_PROVIDER_ID.equals(provider.getID()) ||
				(shareMonitorExtension != null && shareMonitorExtension.shouldWarnForProject(sp.getProject(), provider)))
			{
				// a Servoy related project is shared using another team provider then Servoy in-process repository
				askIfUserWantsToDisableInProcessRepository();
				return;
			}
		}
		for (ServoyResourcesProject srp : sm.getResourceProjects())
		{
			RepositoryProvider provider = RepositoryProvider.getProvider(srp.getProject());
			if (provider == null) continue;
			if (!SERVOY_TEAM_PROVIDER_ID.equals(provider.getID()) ||
				(shareMonitorExtension != null && shareMonitorExtension.shouldWarnForProject(srp.getProject(), provider)))
			{
				// a Servoy related project is shared using another team provider then Servoy in-process repository
				askIfUserWantsToDisableInProcessRepository();
				return;
			}
		}
	}

	private void askIfUserWantsToDisableInProcessRepository()
	{
		ignoreSetExtensionCheck = true;

		// in case multiple projects have just been checked out and the user chose to modify the setting, don't ask him again
		boolean initRepAsTeamProvider = Utils.getAsBoolean(ServoyModel.getSettings().getProperty(Settings.START_AS_TEAMPROVIDER_SETTING,
			String.valueOf(Settings.START_AS_TEAMPROVIDER_DEFAULT)));
		if (initRepAsTeamProvider)
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					if (!dialogShown)
					{
						Shell sh = UIUtils.getActiveShell();
						while (sh != null && sh.getParent() != null && sh.getParent() instanceof Shell)
						{
							sh = (Shell)sh.getParent();
						}
						MessageAndCheckBoxDialog dialog = new MessageAndCheckBoxDialog(
							sh,
							"Developer started repository",
							null,
							"The internal database based repository is being used.\n" +
								"You should only use Servoy Team Provider and only share to 'localhost'.\nUsing other team providers/locations may result in unwanted behavior or limited functionality (mainly related to database information and servoy sequences).\n" +
								"\nYou should disable starting such a repository with developer if you are going to use other team providers/locations), by setting '" +
								Settings.START_AS_TEAMPROVIDER_SETTING + "' to false.",
							"Do not show this warning in the future (can be changed in preferences). Always ignore.", false, MessageDialog.QUESTION,
							new String[] { "Change setting / restart", "Ignore" }, 0);
						dialogShown = true;
						try
						{
							int opt = dialog.open();
							if (dialog.isChecked())
							{
								setAllowWarningDialog(false);
							}
							if (opt == 0)
							{
								ServoyModel.getSettings().setProperty(Settings.START_AS_TEAMPROVIDER_SETTING, "false");
								try
								{
									ServoyModel.getSettings().save();

									// run later as we could be during startup - and some things are not initialized properly in this case
									// and could cause restart to fail
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
											PlatformUI.getWorkbench().restart();
										}
									});
								}
								catch (Exception e)
								{
									ServoyLog.logError(e);
								}
							} // else cancel or ignore
						}
						finally
						{
							dialogShown = false;
						}
					}
				}
			};
			Display.getDefault().asyncExec(r);
		}
	}

	private class NewShareListener implements IRepositoryProviderListener
	{

		public void providerMapped(RepositoryProvider provider)
		{
			IProject project = provider.getProject();
			try
			{
				if (project.hasNature(ServoyProject.NATURE_ID) || project.hasNature(ServoyResourcesProject.NATURE_ID))
				{
					if (!SERVOY_TEAM_PROVIDER_ID.equals(provider.getID()) ||
						(shareMonitorExtension != null && shareMonitorExtension.shouldWarnForProject(project, provider)))
					{
						// a Servoy related project is shared using another team provider then Servoy in-process repository
						askIfUserWantsToDisableInProcessRepository();
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}

		public void providerUnmapped(IProject project)
		{
			// not used
		}

	}

	public static interface TeamShareMonitorExtension
	{

		boolean shouldWarnForProject(IProject project, RepositoryProvider provider);

	}

}