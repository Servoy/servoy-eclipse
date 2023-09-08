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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.PrefUtil;

import com.servoy.eclipse.core.util.ServoyMessageDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.preferences.Ng2DesignerPreferences;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.tweaks.IconPreferences;
import com.servoy.j2db.util.ObjectWrapper;

/**
 * Main preference page for Servoy settings.
 *
 * @author rgansevles
 *
 */
public class ServoyGlobalPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

	private Button toolbarsInFormWindowButton;
	private Button closeEditorOnExitButton;
	private Button openFirstFormDesignerButton;
	private Button showColumnsInDbOrderButton;
	private Button showLegacySolutionTypesButton;
	private Button showColumnsInAlphabeticOrderButton;
	private Button showNavigatorDefaultButton;
	private ComboViewer encapsulationTypeCombo;
	private Spinner waitForSolutionToBeLoadedInTestClientSpinner;
	private Button useDarkIconsButton;
	private Button contextMenuTutorialsButton;
	private Button launchNGButton;
	private Button showNGDesignerButton;
	private Button showForumNotificationsButton;


	public void init(IWorkbench workbench)
	{
		/*
		 * delay setup until here so sub-classes implementing the IExecutableExtension can look up the plugin specific preference store
		 */
		setDescription("Servoy Preferences");
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite rootContainer = new Composite(parent, SWT.NONE);

		rootContainer.setLayout(new GridLayout(1, false));
		rootContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Servoy Design Perspective
		Group formEditorOptionsContainer = new Group(rootContainer, SWT.NONE);
		formEditorOptionsContainer.setText("Servoy Design Perspective Options");
		formEditorOptionsContainer.setLayout(new GridLayout(2, false));
		formEditorOptionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		toolbarsInFormWindowButton = new Button(formEditorOptionsContainer, SWT.CHECK);
		toolbarsInFormWindowButton.setText("Show Form Editing Toolbars inside Form Editor");

		Button resetToolbarsButton = new Button(formEditorOptionsContainer, SWT.NONE);
		resetToolbarsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				new DesignerPreferences().saveCoolbarLayout(null);
			}
		});
		resetToolbarsButton.setText("Show all");

		closeEditorOnExitButton = new Button(formEditorOptionsContainer, SWT.CHECK);
		closeEditorOnExitButton.setText("Close Editors at shutdown");
		new Label(formEditorOptionsContainer, SWT.NONE); // dummy

		openFirstFormDesignerButton = new Button(formEditorOptionsContainer, SWT.CHECK);
		openFirstFormDesignerButton.setText("Open Solution's main Form in Form Editor on Solution activation");
		new Label(formEditorOptionsContainer, SWT.NONE); // dummy

		// Wizard options
		Group wizardOptionsContainer = new Group(rootContainer, SWT.NONE);
		wizardOptionsContainer.setText("Wizard options");
		wizardOptionsContainer.setLayout(new GridLayout(1, false));
		wizardOptionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group columnsOrderGroup = new Group(wizardOptionsContainer, SWT.NONE);
		columnsOrderGroup.setText("Show table columns");
		columnsOrderGroup.setLayout(new GridLayout(1, true));

		showColumnsInAlphabeticOrderButton = new Button(columnsOrderGroup, SWT.RADIO);
		showColumnsInAlphabeticOrderButton.setText("in alphabetic order (key columns first)");

		showColumnsInDbOrderButton = new Button(columnsOrderGroup, SWT.RADIO);
		showColumnsInDbOrderButton.setText("in database defined order");

		showLegacySolutionTypesButton = new Button(wizardOptionsContainer, SWT.CHECK);
		showLegacySolutionTypesButton.setText("Show legacy solution types (smart client, web client..)");

		//Form Properties
		Group formProperties = new Group(rootContainer, SWT.NONE);
		formProperties.setText("Form Properties");
		formProperties.setLayout(new GridLayout(1, true));
		formProperties.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		showNavigatorDefaultButton = new Button(formProperties, SWT.CHECK);
		showNavigatorDefaultButton.setText("Set default navigator on new forms");

		Group encapsulationProperties = new Group(formProperties, SWT.NONE);
		encapsulationProperties.setText("Encapsulation Properties");
		encapsulationProperties.setLayout(new GridLayout(1, true));

		encapsulationTypeCombo = new ComboViewer(encapsulationProperties);
		encapsulationTypeCombo.setContentProvider(new ArrayContentProvider());
		encapsulationTypeCombo.setLabelProvider(new LabelProvider());
		encapsulationTypeCombo.setInput(
			new ObjectWrapper[] { new ObjectWrapper("Public, Hide All", new Integer(DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL)), new ObjectWrapper(
				"Public", new Integer(DesignerPreferences.ENCAPSULATION_PUBLIC)) });

		// Appearance
		Group appearanceOptionsContainer = new Group(rootContainer, SWT.NONE);
		appearanceOptionsContainer.setText("Appearance Options");
		appearanceOptionsContainer.setLayout(new GridLayout(1, false));
		appearanceOptionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		useDarkIconsButton = new Button(appearanceOptionsContainer, SWT.CHECK);
		useDarkIconsButton.setText("Use dark theme icons (restart required)");

		// Context menu tutorials
		Group contextMenuTutorialsContainer = new Group(rootContainer, SWT.NONE);
		contextMenuTutorialsContainer.setText("Tutorials directly into the context menu");
		contextMenuTutorialsContainer.setLayout(new GridLayout(1, true));
		contextMenuTutorialsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		contextMenuTutorialsButton = new Button(contextMenuTutorialsContainer, SWT.CHECK);
		contextMenuTutorialsButton.setText("Activate or deactivate the tutorials from the context menu");
		contextMenuTutorialsButton.setToolTipText("Activate or deactivate the tutorials from the context menu");

		Group launcherSettings = new Group(rootContainer, SWT.NONE); // TODO it would really be nicer to have these in a real launch configuration page (similar to what mobile client lauchers do)
		launcherSettings.setText("Launcher settings");
		launcherSettings.setLayout(new GridLayout(3, false));
		launcherSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label testSolutionLoadTimeoutLabel = new Label(launcherSettings, SWT.NONE);
		testSolutionLoadTimeoutLabel.setText("Maximum time to wait for a solution to load in (non-mobile) jsunit test client: ");
		waitForSolutionToBeLoadedInTestClientSpinner = new Spinner(launcherSettings, SWT.BORDER);
		waitForSolutionToBeLoadedInTestClientSpinner.setMinimum(5);
		waitForSolutionToBeLoadedInTestClientSpinner.setMaximum(Integer.MAX_VALUE);
		waitForSolutionToBeLoadedInTestClientSpinner.setDigits(0);
		waitForSolutionToBeLoadedInTestClientSpinner.setIncrement(10); // 10 sec
		waitForSolutionToBeLoadedInTestClientSpinner.setPageIncrement(60); // 1 min
//		waitForSolutionToBeLoadedInTestClientSpinner.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));
		Label testSolutionLoadTimeoutLabelUnits = new Label(launcherSettings, SWT.NONE);
		testSolutionLoadTimeoutLabelUnits.setText(" sec.");
		String tt = "Maximum number of seconds in which a solution should load and be ready for testing.\nSolutions with long running onOpen handlers might want to change this.\nIf a solution doesn't load in this time frame, the tests will fail.\n\nDefault value: 300 sec.";
		testSolutionLoadTimeoutLabel.setToolTipText(tt);
		waitForSolutionToBeLoadedInTestClientSpinner.setToolTipText(tt);
		testSolutionLoadTimeoutLabelUnits.setToolTipText(tt);

		// launch NG
		launchNGButton = new Button(rootContainer, SWT.CHECK);
		launchNGButton.setText("Start NG Client should launch Titanium NGClient");

		// form designer version
		showNGDesignerButton = new Button(rootContainer, SWT.CHECK);
		showNGDesignerButton.setText("Open forms with the Titanium NGClient Form Designer");

		// forum notifications
		showForumNotificationsButton = new Button(rootContainer, SWT.CHECK);
		showForumNotificationsButton.setText("Show forum notifications (require developer restart)");

		initializeFields();

		return rootContainer;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();
		Ng2DesignerPreferences ng2Prefs = new Ng2DesignerPreferences();

		toolbarsInFormWindowButton.setSelection(prefs.getFormToolsOnMainToolbar());
		closeEditorOnExitButton.setSelection(prefs.getCloseEditorOnExit());
		openFirstFormDesignerButton.setSelection(prefs.getOpenFirstFormDesigner());
		showColumnsInDbOrderButton.setSelection(prefs.getShowColumnsInDbOrder());
		showLegacySolutionTypesButton.setSelection(prefs.getShowLegacySolutionTypes());
		showColumnsInAlphabeticOrderButton.setSelection(!showColumnsInDbOrderButton.getSelection());
		showNavigatorDefaultButton.setSelection(prefs.getShowNavigatorDefault());
		setEncapsulationTypeValue(prefs.getEncapsulationType());
		waitForSolutionToBeLoadedInTestClientSpinner.setSelection(prefs.getTestClientLoadTimeout());
		contextMenuTutorialsButton.setSelection(prefs.useContextMenuTutorials());
		useDarkIconsButton.setSelection(IconPreferences.getInstance().getUseDarkThemeIcons());
		launchNGButton.setSelection(ng2Prefs.launchNG2());
		showNGDesignerButton.setSelection(ng2Prefs.showNG2Designer());
		showForumNotificationsButton.setSelection(prefs.showForumNotifications());
	}

	@Override
	protected void performDefaults()
	{
		toolbarsInFormWindowButton.setSelection(DesignerPreferences.FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT);
		closeEditorOnExitButton.setSelection(DesignerPreferences.CLOSE_EDITORS_ON_EXIT_DEFAULT);
		PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.CLOSE_EDITORS_ON_EXIT, DesignerPreferences.CLOSE_EDITORS_ON_EXIT_DEFAULT);
		openFirstFormDesignerButton.setSelection(DesignerPreferences.OPEN_FIRST_FORM_DESIGNER_DEFAULT);
		showColumnsInDbOrderButton.setSelection(DesignerPreferences.SHOW_COLUMNS_IN_DB_ORDER_DEFAULT);
		showLegacySolutionTypesButton.setSelection(DesignerPreferences.SHOW_LEGACY_SOLUTION_TYPES_DEFAULT);
		showColumnsInAlphabeticOrderButton.setSelection(!showColumnsInDbOrderButton.getSelection());
		showNavigatorDefaultButton.setSelection(DesignerPreferences.SHOW_NAVIGATOR_DEFAULT);
		setEncapsulationTypeValue(DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL);
		waitForSolutionToBeLoadedInTestClientSpinner.setSelection(DesignerPreferences.WAIT_FOR_SOLUTION_TO_BE_LOADED_IN_TEST_CLIENT_DEFAULT);
		useDarkIconsButton.setSelection(IconPreferences.USE_DARK_THEME_ICONS_DEFAULT);
		contextMenuTutorialsButton.setSelection(DesignerPreferences.USE_CONTEXT_MENU_TUTORIALS_DEFAULT);
		launchNGButton.setSelection(Ng2DesignerPreferences.LAUNCH_NG2_DEFAULT);
		showNGDesignerButton.setSelection(Ng2DesignerPreferences.NG2_DESIGNER_DEFAULT);
		showForumNotificationsButton.setSelection(DesignerPreferences.FORUM_NOTIFICATIONS_DEFAULT);
		super.performDefaults();
	}

	@Override
	public void setVisible(boolean visible)
	{
		if (visible)
		{
			useDarkIconsButton.setSelection(IconPreferences.getInstance().getUseDarkThemeIcons());
		}
		super.setVisible(visible);
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();
		Ng2DesignerPreferences ng2Prefs = new Ng2DesignerPreferences();

		prefs.setFormToolsOnMainToolbar(toolbarsInFormWindowButton.getSelection());
		prefs.setCloseEditorsOnExit(closeEditorOnExitButton.getSelection());
		PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.CLOSE_EDITORS_ON_EXIT, closeEditorOnExitButton.getSelection());
		prefs.setOpenFirstFormDesigner(openFirstFormDesignerButton.getSelection());
		prefs.setShowColumnsInDbOrder(showColumnsInDbOrderButton.getSelection());
		prefs.setShowLegacySolutionTypes(showLegacySolutionTypesButton.getSelection());
		prefs.setShowNavigatorDefault(showNavigatorDefaultButton.getSelection());
		prefs.setEncapsulationType(getFirstElementValue(encapsulationTypeCombo, Integer.valueOf(DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL)).intValue());
		prefs.setTestClientLoadTimeout(waitForSolutionToBeLoadedInTestClientSpinner.getSelection());
		prefs.setContextMenuTutorials(contextMenuTutorialsButton.getSelection());
		ng2Prefs.setLaunchNG2(launchNGButton.getSelection());
		ng2Prefs.setShowNG2Designer(showNGDesignerButton.getSelection());
		prefs.setShowForumNotifications(showForumNotificationsButton.getSelection());
		prefs.save();
		ng2Prefs.save();

		IconPreferences iconPreferences = IconPreferences.getInstance();
		if (useDarkIconsButton.getSelection() != iconPreferences.getUseDarkThemeIcons() && !iconPreferences.isChanged()) //we set it once more if it was not already set by the theme change
		{
			iconPreferences.setUseDarkThemeIcons(useDarkIconsButton.getSelection());
			iconPreferences.save(true);
			if (ServoyMessageDialog.openQuestion(UIUtils.getActiveShell(), "Use dark icons preference changed",
				"It is strongly recommended to restart your Servoy Developer. Would you like to restart now?"))
			{
				PlatformUI.getWorkbench().restart();
			}
		}
		return true;
	}

	/**
	 * @param viewer
	 * @param default
	 * @return
	 */
	protected <T> T getFirstElementValue(ComboViewer viewer, T defaultValue)
	{
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection)
		{
			Object firstElement = ((IStructuredSelection)selection).getFirstElement();
			if (firstElement instanceof ObjectWrapper)
			{
				T type = (T)((ObjectWrapper)firstElement).getType();
				if (type != null)
				{
					return type;
				}
			}
		}
		return defaultValue;
	}

	private void setEncapsulationTypeValue(int enc_type)
	{
		Integer encType = Integer.valueOf(enc_type);
		for (ObjectWrapper ow : (ObjectWrapper[])encapsulationTypeCombo.getInput())
		{
			if (ow.getType().equals(encType))
			{
				encapsulationTypeCombo.setSelection(new StructuredSelection(ow));
				return;
			}
		}
	}
}
