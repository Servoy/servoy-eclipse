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
package com.servoy.eclipse.team.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.TeamShareMonitor;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.team.Activator;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.util.Settings;

public class PreferencesPage extends PreferencePage implements IWorkbenchPreferencePage
{
	public static final String AUTOMATIC_RESOURCE_UPDATE_ON_CHECKOUT_PROPERTY = "automaticResourcesUpdateOnCheckout";
	public static final String AUTOMATIC_RESOURCE_SYNCH_PROPERTY = "automaticResourcesSynch";
	public static final String AUTOMATIC_MODULES_SYNCH_PROPERTY = "automaticModulesSynch";
	public static final String TEMP_TEAM_DIRECTORY_PROPERTY = "temporaryTeamDirectory";

	private Button chAutomaticResourceSynch;
	private Button chAutomaticModulesSynch;
	private Button chAutomaticResourceUpdateOnCheckout;
	private Button warnWhenUsingOtherTeamProvidersWithInProcessRep;
	private Text txTemporaryDirectory;

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite cp = new Composite(parent, SWT.NULL);

		chAutomaticResourceUpdateOnCheckout = new Button(cp, SWT.CHECK);
		chAutomaticResourceUpdateOnCheckout.setText("Checkout solution, also update resources");

		chAutomaticResourceSynch = new Button(cp, SWT.CHECK);
		chAutomaticResourceSynch.setText("Synchronize/commit/update solution, also do it on resources");

		chAutomaticModulesSynch = new Button(cp, SWT.CHECK);
		chAutomaticModulesSynch.setText("Synchronize/commit/update solution, also do it on modules");

		warnWhenUsingOtherTeamProvidersWithInProcessRep = new Button(cp, SWT.CHECK);
		warnWhenUsingOtherTeamProvidersWithInProcessRep.setText("Show warning when using non-Servoy team provider or non 'localhost' location while internal database");

		Label warnWhenUsingOtherTeamProvidersWithInProcessRep2 = new Label(cp, SWT.CHECK);
		warnWhenUsingOtherTeamProvidersWithInProcessRep2.setText("based repository is active (see admin setting '" + Settings.START_AS_TEAMPROVIDER_SETTING +
			"')");


		Group temporaryDirectoryGroup;
		temporaryDirectoryGroup = new Group(cp, SWT.NONE);
		temporaryDirectoryGroup.setText("Temporary directory (changes effective after restart)");

		txTemporaryDirectory = new Text(temporaryDirectoryGroup, SWT.BORDER);

		Label tempDirLabel = new Label(temporaryDirectoryGroup, SWT.NONE);
		tempDirLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		tempDirLabel.setText("Note: will be deleted at exit");

		int textHeight = FontResource.getTextExtent(txTemporaryDirectory, txTemporaryDirectory.getFont(), "X").y + 15;

		final GroupLayout groupLayout = new GroupLayout(cp);


		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().addContainerGap().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(temporaryDirectoryGroup, GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE).add(
							chAutomaticResourceUpdateOnCheckout, GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE).add(chAutomaticResourceSynch,
							GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE).add(chAutomaticModulesSynch, GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE).add(
							warnWhenUsingOtherTeamProvidersWithInProcessRep, GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE).add(
							warnWhenUsingOtherTeamProvidersWithInProcessRep2, GroupLayout.DEFAULT_SIZE, 685, Short.MAX_VALUE)).addContainerGap()))));


		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(chAutomaticResourceUpdateOnCheckout).addPreferredGap(
				LayoutStyle.RELATED).add(chAutomaticResourceSynch).addPreferredGap(LayoutStyle.RELATED).add(chAutomaticModulesSynch).addPreferredGap(
				LayoutStyle.RELATED).add(warnWhenUsingOtherTeamProvidersWithInProcessRep).add(warnWhenUsingOtherTeamProvidersWithInProcessRep2).addPreferredGap(
				LayoutStyle.RELATED).add(temporaryDirectoryGroup, GroupLayout.PREFERRED_SIZE, 74, GroupLayout.PREFERRED_SIZE).addContainerGap(135,
				Short.MAX_VALUE)));


		final GroupLayout groupLayout_1 = new GroupLayout(temporaryDirectoryGroup);
		groupLayout_1.setHorizontalGroup(groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout_1.createSequentialGroup().addContainerGap().add(txTemporaryDirectory, GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(tempDirLabel, GroupLayout.PREFERRED_SIZE, 225, GroupLayout.PREFERRED_SIZE).addContainerGap()));
		groupLayout_1.setVerticalGroup(groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout_1.createSequentialGroup().addContainerGap().add(
				groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(txTemporaryDirectory, GroupLayout.PREFERRED_SIZE, textHeight,
					GroupLayout.PREFERRED_SIZE).add(tempDirLabel)).addContainerGap(18, Short.MAX_VALUE)));
		temporaryDirectoryGroup.setLayout(groupLayout_1);
		cp.setLayout(groupLayout);

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
		chAutomaticResourceUpdateOnCheckout.setSelection(store.getBoolean(AUTOMATIC_RESOURCE_UPDATE_ON_CHECKOUT_PROPERTY));
		txTemporaryDirectory.setText(store.getString(TEMP_TEAM_DIRECTORY_PROPERTY));
		chAutomaticResourceSynch.setSelection(store.getBoolean(AUTOMATIC_RESOURCE_SYNCH_PROPERTY));
		chAutomaticModulesSynch.setSelection(store.getBoolean(AUTOMATIC_MODULES_SYNCH_PROPERTY));

		store = com.servoy.eclipse.core.Activator.getDefault().getPluginPreferences();
		warnWhenUsingOtherTeamProvidersWithInProcessRep.setSelection(store.getBoolean(TeamShareMonitor.WARN_ON_NON_IN_PROCESS_TEAM_SHARE));
	}

	private void storeValues()
	{
		Preferences store = Activator.getDefault().getPluginPreferences();
		store.setValue(AUTOMATIC_RESOURCE_UPDATE_ON_CHECKOUT_PROPERTY, chAutomaticResourceUpdateOnCheckout.getSelection());
		store.setValue(TEMP_TEAM_DIRECTORY_PROPERTY, txTemporaryDirectory.getText());
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
		chAutomaticResourceUpdateOnCheckout.setSelection(store.getDefaultBoolean(AUTOMATIC_RESOURCE_UPDATE_ON_CHECKOUT_PROPERTY));
		txTemporaryDirectory.setText(store.getDefaultString(TEMP_TEAM_DIRECTORY_PROPERTY));
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
