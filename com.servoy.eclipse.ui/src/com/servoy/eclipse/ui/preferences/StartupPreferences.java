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


import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Preferences page for developer startup settings.
 * 
 * @author rgansevles
 * 
 */
public class StartupPreferences extends PreferencePage implements IWorkbenchPreferencePage
{
	// storedin Settings
	public static final String STARTUP_LAUNCHER_SETTING = "nativeStartupLauncher";
	public static final String SHUTDOWN_LAUNCHER_SETTING = "nativeShutdownLauncher";
	public static final String RETRIES_SETTING = "developer.maxRepositoryConnectRetries";

	// stored in ui plugin prefs
	public static final String DEBUG_CLIENT_CONFIRMATION_WHEN_ERRORS = "debugger.showConfirmationDialogWhenErrors";
	public static final String DEBUG_CLIENT_CONFIRMATION_WHEN_WARNINGS = "debugger.showConfirmationDialogWhenWarnings";
	public static boolean DEFAULT_ERROR_CONFIRMATION = true;
	public static boolean DEFAULT_WARNING_CONFIRMATION = false;

	private static final int RETRIES_DEFAULT = 5;

	private Spinner retriesSpinner;
	private Text startupLauncherText;
	private Text shutdownLauncherText;
	private Button showErrorsConfirmation;
	private Button showWarningsConfirmation;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		Label startupLauncherLabel;
		startupLauncherLabel = new Label(composite, SWT.NONE);
		startupLauncherLabel.setText("Startup launcher");

		Label shutdownLauncherLabel;
		shutdownLauncherLabel = new Label(composite, SWT.NONE);
		shutdownLauncherLabel.setText("Shutdown launcher");

		Label retriesLabel;
		retriesLabel = new Label(composite, SWT.NONE);
		retriesLabel.setText("Max repository connect retries");

		startupLauncherText = new Text(composite, SWT.BORDER);

		shutdownLauncherText = new Text(composite, SWT.BORDER);

		retriesSpinner = new Spinner(composite, SWT.BORDER);
		retriesSpinner.setValues(0, 1, 100, 0, 1, 5);

		Label lErrors = new Label(composite, SWT.NONE);
		Label lWarnings = new Label(composite, SWT.NONE);
		lErrors.setText("Debug client confirmation launch when errors");
		lWarnings.setText("Debug client confirmation launch when warnings");
		showErrorsConfirmation = new Button(composite, SWT.CHECK);
		showWarningsConfirmation = new Button(composite, SWT.CHECK);

		final GroupLayout groupLayout = new GroupLayout(composite);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(startupLauncherLabel).add(shutdownLauncherLabel).add(retriesLabel).add(lErrors).add(
					lWarnings)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(startupLauncherText).add(shutdownLauncherText).add(retriesSpinner).add(
					showErrorsConfirmation).add(showWarningsConfirmation)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(startupLauncherLabel).add(startupLauncherText)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(shutdownLauncherLabel).add(shutdownLauncherText)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(retriesSpinner).add(retriesLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(showErrorsConfirmation).add(lErrors)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(showWarningsConfirmation).add(lWarnings)).addContainerGap()));
		composite.setLayout(groupLayout);

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = ServoyModel.getSettings();
		startupLauncherText.setText(settings.getProperty(STARTUP_LAUNCHER_SETTING, ""));
		shutdownLauncherText.setText(settings.getProperty(SHUTDOWN_LAUNCHER_SETTING, ""));
		retriesSpinner.setSelection(Utils.getAsInteger(settings.getProperty(RETRIES_SETTING, String.valueOf(RETRIES_DEFAULT))));

		IEclipsePreferences eclipsePreferences = Activator.getDefault().getEclipsePreferences();
		showErrorsConfirmation.setSelection(eclipsePreferences.getBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_ERRORS, DEFAULT_ERROR_CONFIRMATION));
		showWarningsConfirmation.setSelection(eclipsePreferences.getBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_WARNINGS, DEFAULT_WARNING_CONFIRMATION));
	}

	@Override
	public boolean performOk()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = ServoyModel.getSettings();
		settings.setProperty(STARTUP_LAUNCHER_SETTING, startupLauncherText.getText());
		settings.setProperty(SHUTDOWN_LAUNCHER_SETTING, shutdownLauncherText.getText());
		settings.setProperty(RETRIES_SETTING, String.valueOf(retriesSpinner.getSelection()));

		IEclipsePreferences eclipsePreferences = Activator.getDefault().getEclipsePreferences();
		eclipsePreferences.putBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_ERRORS, showErrorsConfirmation.getSelection());
		eclipsePreferences.putBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_WARNINGS, showWarningsConfirmation.getSelection());
		return true;
	}

	@Override
	protected void performDefaults()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = ServoyModel.getSettings();
		startupLauncherText.setText(settings.getProperty(STARTUP_LAUNCHER_SETTING, ""));
		shutdownLauncherText.setText(settings.getProperty(SHUTDOWN_LAUNCHER_SETTING, ""));
		retriesSpinner.setSelection(RETRIES_DEFAULT);

		showErrorsConfirmation.setSelection(DEFAULT_ERROR_CONFIRMATION);
		showWarningsConfirmation.setSelection(DEFAULT_WARNING_CONFIRMATION);
		super.performDefaults();
	}
}
