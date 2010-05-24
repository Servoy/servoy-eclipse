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
package com.servoy.eclipse.ui.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.TeamShareMonitor;
import com.servoy.eclipse.ui.Activator;

public class TeamPreferences extends PreferencePage implements IWorkbenchPreferencePage
{
	public static final String AUTOMATIC_RESOURCE_SYNCH_PROPERTY = "automaticResourcesSynch";
	public static final String AUTOMATIC_MODULES_SYNCH_PROPERTY = "automaticModulesSynch";

	private Button chAutomaticResourceSynch;
	private Button chAutomaticModulesSynch;
	private Button warnWhenUsingOtherTeamProvidersWithInProcessRep;

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite cp = new Composite(parent, SWT.NULL);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		cp.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		cp.setLayoutData(data);


		chAutomaticResourceSynch = new Button(cp, SWT.CHECK);
		chAutomaticResourceSynch.setText("Synchronize/commit/update solution, also do it on resources");

		chAutomaticModulesSynch = new Button(cp, SWT.CHECK);
		chAutomaticModulesSynch.setText("Synchronize/commit/update solution, also do it on modules");

		warnWhenUsingOtherTeamProvidersWithInProcessRep = new Button(cp, SWT.CHECK);
		warnWhenUsingOtherTeamProvidersWithInProcessRep.setText("Show warning when using non-Servoy team provider or non 'localhost' location while internal database");

		Label nextLineLabel = new Label(cp, SWT.CHECK);
		nextLineLabel.setText("based repository is active (see admin setting 'servoy.application_server.startRepositoryAsTeamProvider')");

		initializeValues();

		return cp;
	}

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected void performApply()
	{
		storeValues();
		super.performApply();
	}

	@Override
	protected void performDefaults()
	{
		initializeDefaults();
		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		storeValues();
		return true;
	}

	private void initializeValues()
	{
		Preferences store = Activator.getDefault().getPluginPreferences();
		chAutomaticResourceSynch.setSelection(store.getBoolean(AUTOMATIC_RESOURCE_SYNCH_PROPERTY));
		chAutomaticModulesSynch.setSelection(store.getBoolean(AUTOMATIC_MODULES_SYNCH_PROPERTY));

		store = com.servoy.eclipse.core.Activator.getDefault().getPluginPreferences();
		warnWhenUsingOtherTeamProvidersWithInProcessRep.setSelection(store.getBoolean(TeamShareMonitor.WARN_ON_NON_IN_PROCESS_TEAM_SHARE));
	}

	private void storeValues()
	{
		Preferences store = Activator.getDefault().getPluginPreferences();
		store.setValue(AUTOMATIC_RESOURCE_SYNCH_PROPERTY, chAutomaticResourceSynch.getSelection());
		store.setValue(AUTOMATIC_MODULES_SYNCH_PROPERTY, chAutomaticModulesSynch.getSelection());
		Activator.getDefault().savePluginPreferences();

		TeamShareMonitor tsm = ServoyModelManager.getServoyModelManager().getServoyModel().getTeamShareMonitor();
		store = com.servoy.eclipse.core.Activator.getDefault().getPluginPreferences();
		if (tsm != null)
		{
			tsm.setAllowWarningDialog(warnWhenUsingOtherTeamProvidersWithInProcessRep.getSelection());
			warnWhenUsingOtherTeamProvidersWithInProcessRep.setSelection(store.getBoolean(TeamShareMonitor.WARN_ON_NON_IN_PROCESS_TEAM_SHARE));
		}
		else
		{
			store.setValue(TeamShareMonitor.WARN_ON_NON_IN_PROCESS_TEAM_SHARE, warnWhenUsingOtherTeamProvidersWithInProcessRep.getSelection());
			com.servoy.eclipse.core.Activator.getDefault().savePluginPreferences();
		}
	}

	private void initializeDefaults()
	{
		Preferences store = Activator.getDefault().getPluginPreferences();
		chAutomaticResourceSynch.setSelection(store.getDefaultBoolean(AUTOMATIC_RESOURCE_SYNCH_PROPERTY));
		chAutomaticModulesSynch.setSelection(store.getDefaultBoolean(AUTOMATIC_MODULES_SYNCH_PROPERTY));

		store = com.servoy.eclipse.core.Activator.getDefault().getPluginPreferences();
		warnWhenUsingOtherTeamProvidersWithInProcessRep.setSelection(store.getDefaultBoolean(TeamShareMonitor.WARN_ON_NON_IN_PROCESS_TEAM_SHARE));
	}

	public static boolean isAutomaticResourceSynch()
	{
		ServoyResourcesProject servoyResourceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
		return Activator.getDefault().getPluginPreferences().getBoolean(AUTOMATIC_RESOURCE_SYNCH_PROPERTY) && servoyResourceProject != null;
	}

	public static boolean isAutomaticModulesSynch()
	{
		return Activator.getDefault().getPluginPreferences().getBoolean(AUTOMATIC_MODULES_SYNCH_PROPERTY);
	}
}
