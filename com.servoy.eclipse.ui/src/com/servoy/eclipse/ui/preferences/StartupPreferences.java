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
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
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
	public static final String STARTUP_SHOW_START_PAGE = "servoy.developer.showStartPage";
	public static boolean DEFAULT_ERROR_CONFIRMATION = true;
	public static boolean DEFAULT_WARNING_CONFIRMATION = false;
	public static boolean DEFAULT_STARTUP_EXTENSION_UPDATE_CHECK = false;
	public static boolean DEFAULT_STARTUP_SHOW_START_PAGE = true;

	private static final int RETRIES_DEFAULT = 5;

	private Spinner retriesSpinner;
	private Text startupLauncherText;
	private Text shutdownLauncherText;
	private Text settingsFileText;
	private Button showErrorsConfirmation;
	private Button showWarningsConfirmation;
	private Button showStartPageCheck;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		// create contents
		Composite composite = new Composite(parent, SWT.NONE);
		Group nativeLaunchers = new Group(composite, SWT.NONE);
		nativeLaunchers.setText("Native launchers");

		Group settings = new Group(composite, SWT.NONE);
		settings.setText("Settings");

		Group others = new Group(composite, SWT.NONE);

		Label startupLauncherLabel;
		startupLauncherLabel = new Label(nativeLaunchers, SWT.NONE);
		startupLauncherLabel.setText("Startup launcher");
		startupLauncherText = new Text(nativeLaunchers, SWT.BORDER);

		Label shutdownLauncherLabel;
		shutdownLauncherLabel = new Label(nativeLaunchers, SWT.NONE);
		shutdownLauncherLabel.setText("Shutdown launcher");
		shutdownLauncherText = new Text(nativeLaunchers, SWT.BORDER);

		Label settingsFileLabel;
		settingsFileLabel = new Label(nativeLaunchers, SWT.NONE);
		settingsFileLabel.setText("Properties file");
		settingsFileLabel.setToolTipText(
			"Set properties file (by default servoy.properties) from application server directory, so a relative to that dir,  to be used for this workspace. After change, restart is required.");
		settingsFileText = new Text(nativeLaunchers, SWT.BORDER);
		settingsFileText.setToolTipText(
			"Set properties file (by default servoy.properties) from application server directory, so a relative to that dir,  to be used for this workspace. After change, restart is required.");

		Label retriesLabel;
		retriesLabel = new Label(settings, SWT.NONE);
		retriesLabel.setText("Max repository connect retries");
		retriesSpinner = new Spinner(settings, SWT.BORDER);
		retriesSpinner.setValues(0, 1, 100, 0, 1, 5);

		showErrorsConfirmation = new Button(others, SWT.CHECK);
		showErrorsConfirmation.setText("Check for error markers when launching (debug) client");
		showWarningsConfirmation = new Button(others, SWT.CHECK);
		showWarningsConfirmation.setText("Check for warning markers when launching (debug) client");
		showStartPageCheck = new Button(others, SWT.CHECK);
		showStartPageCheck.setText("Show start page");

		// layout contents
		GridLayoutFactory gridFactory = GridLayoutFactory.fillDefaults();
		gridFactory.extendedMargins(5, 5, 5, 5);
		gridFactory.numColumns(2);
		gridFactory.generateLayout(settings);
		gridFactory.generateLayout(nativeLaunchers);
		gridFactory.numColumns(1);
		gridFactory.generateLayout(others);
		gridFactory.extendedMargins(0, 5, 5, 5);
		gridFactory.generateLayout(composite);
		((GridData)startupLauncherText.getLayoutData()).widthHint = 200;
		((GridData)shutdownLauncherText.getLayoutData()).widthHint = 200;
		((GridData)settingsFileText.getLayoutData()).widthHint = 200;
		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = Settings.getInstance();
		startupLauncherText.setText(settings.getProperty(STARTUP_LAUNCHER_SETTING, ""));
		shutdownLauncherText.setText(settings.getProperty(SHUTDOWN_LAUNCHER_SETTING, ""));
		retriesSpinner.setSelection(Utils.getAsInteger(settings.getProperty(RETRIES_SETTING, String.valueOf(RETRIES_DEFAULT))));
		settingsFileText.setText(InstanceScope.INSTANCE.getNode(com.servoy.eclipse.core.Activator.PLUGIN_ID).get(
			com.servoy.eclipse.core.Activator.PROPERTIES_FILE_PATH_SETTING, "servoy.properties"));

		IEclipsePreferences eclipsePreferences = Activator.getDefault().getEclipsePreferences();
		showErrorsConfirmation.setSelection(eclipsePreferences.getBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_ERRORS, DEFAULT_ERROR_CONFIRMATION));
		showWarningsConfirmation.setSelection(eclipsePreferences.getBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_WARNINGS, DEFAULT_WARNING_CONFIRMATION));
		showStartPageCheck.setSelection(Utils.getAsBoolean(Settings.getInstance().getProperty(STARTUP_SHOW_START_PAGE, "true")));
	}

	@Override
	public boolean performOk()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = Settings.getInstance();
		settings.setProperty(STARTUP_LAUNCHER_SETTING, startupLauncherText.getText());
		settings.setProperty(SHUTDOWN_LAUNCHER_SETTING, shutdownLauncherText.getText());
		settings.setProperty(RETRIES_SETTING, String.valueOf(retriesSpinner.getSelection()));

		InstanceScope.INSTANCE.getNode(com.servoy.eclipse.core.Activator.PLUGIN_ID).put(com.servoy.eclipse.core.Activator.PROPERTIES_FILE_PATH_SETTING,
			settingsFileText.getText());
		try
		{
			InstanceScope.INSTANCE.getNode(com.servoy.eclipse.core.Activator.PLUGIN_ID).flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}

		IEclipsePreferences eclipsePreferences = Activator.getDefault().getEclipsePreferences();
		eclipsePreferences.putBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_ERRORS, showErrorsConfirmation.getSelection());
		eclipsePreferences.putBoolean(DEBUG_CLIENT_CONFIRMATION_WHEN_WARNINGS, showWarningsConfirmation.getSelection());
		Settings.getInstance().setProperty(STARTUP_SHOW_START_PAGE, Boolean.valueOf(showStartPageCheck.getSelection()).toString());
		return true;
	}

	@Override
	protected void performDefaults()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = Settings.getInstance();
		startupLauncherText.setText(settings.getProperty(STARTUP_LAUNCHER_SETTING, ""));
		shutdownLauncherText.setText(settings.getProperty(SHUTDOWN_LAUNCHER_SETTING, ""));
		settingsFileText.setText("servoy.properties");
		retriesSpinner.setSelection(RETRIES_DEFAULT);

		showErrorsConfirmation.setSelection(DEFAULT_ERROR_CONFIRMATION);
		showWarningsConfirmation.setSelection(DEFAULT_WARNING_CONFIRMATION);
		showStartPageCheck.setSelection(DEFAULT_STARTUP_SHOW_START_PAGE);
		super.performDefaults();
	}
}
