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
import org.eclipse.ui.internal.util.PrefUtil;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.tweaks.IconPreferences;
import com.servoy.j2db.persistence.ColumnInfo;
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
	private Button showColumnsInAlphabeticOrderButton;
	private ComboViewer primaryKeySequenceTypeCombo;
	private Button showNavigatorDefaultButton;
	private ComboViewer encapsulationTypeCombo;
	private Spinner waitForSolutionToBeLoadedInTestClientSpinner;
	private Button useDarkIconsButton;

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

		//Table Creation Options
		Group tableCreationSettings = new Group(rootContainer, SWT.NONE);
		tableCreationSettings.setText("Table Creation Settings");
		tableCreationSettings.setLayout(new GridLayout(1, false));
		tableCreationSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group defaultPrimaryKeySequenceType = new Group(tableCreationSettings, SWT.NONE);
		defaultPrimaryKeySequenceType.setText("Default Primary Key Sequence Type");
		defaultPrimaryKeySequenceType.setLayout(new GridLayout(1, true));

		primaryKeySequenceTypeCombo = new ComboViewer(defaultPrimaryKeySequenceType);
		primaryKeySequenceTypeCombo.setContentProvider(new ArrayContentProvider());
		primaryKeySequenceTypeCombo.setLabelProvider(new LabelProvider());
		primaryKeySequenceTypeCombo.setInput(
			new ObjectWrapper[] { new ObjectWrapper("Servoy Sequence", new Integer(ColumnInfo.SERVOY_SEQUENCE)), new ObjectWrapper("Database Sequence",
				new Integer(ColumnInfo.DATABASE_SEQUENCE)), new ObjectWrapper("Database Identity",
					new Integer(ColumnInfo.DATABASE_IDENTITY)), new ObjectWrapper("UUID Generator", new Integer(ColumnInfo.UUID_GENERATOR)) });

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

		initializeFields();

		return rootContainer;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		toolbarsInFormWindowButton.setSelection(prefs.getFormToolsOnMainToolbar());
		closeEditorOnExitButton.setSelection(prefs.getCloseEditorOnExit());
		openFirstFormDesignerButton.setSelection(prefs.getOpenFirstFormDesigner());
		showColumnsInDbOrderButton.setSelection(prefs.getShowColumnsInDbOrder());
		showColumnsInAlphabeticOrderButton.setSelection(!showColumnsInDbOrderButton.getSelection());
		setPrimaryKeySequenceTypeValue(prefs.getPrimaryKeySequenceType());
		showNavigatorDefaultButton.setSelection(prefs.getShowNavigatorDefault());
		setEncapsulationTypeValue(prefs.getEncapsulationType());
		waitForSolutionToBeLoadedInTestClientSpinner.setSelection(prefs.getTestClientLoadTimeout());
		useDarkIconsButton.setSelection(IconPreferences.getInstance().getUseDarkThemeIcons());
	}

	@Override
	protected void performDefaults()
	{
		toolbarsInFormWindowButton.setSelection(DesignerPreferences.FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT);
		closeEditorOnExitButton.setSelection(DesignerPreferences.CLOSE_EDITORS_ON_EXIT_DEFAULT);
		PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.CLOSE_EDITORS_ON_EXIT, DesignerPreferences.CLOSE_EDITORS_ON_EXIT_DEFAULT);
		openFirstFormDesignerButton.setSelection(DesignerPreferences.OPEN_FIRST_FORM_DESIGNER_DEFAULT);
		showColumnsInDbOrderButton.setSelection(DesignerPreferences.SHOW_COLUMNS_IN_DB_ORDER_DEFAULT);
		showColumnsInAlphabeticOrderButton.setSelection(!showColumnsInDbOrderButton.getSelection());
		setPrimaryKeySequenceTypeValue(DesignerPreferences.PK_SEQUENCE_TYPE_DEFAULT);
		showNavigatorDefaultButton.setSelection(DesignerPreferences.SHOW_NAVIGATOR_DEFAULT);
		setEncapsulationTypeValue(DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL);
		waitForSolutionToBeLoadedInTestClientSpinner.setSelection(DesignerPreferences.WAIT_FOR_SOLUTION_TO_BE_LOADED_IN_TEST_CLIENT_DEFAULT);
		useDarkIconsButton.setSelection(IconPreferences.USE_DARK_THEME_ICONS_DEFAULT);
		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		prefs.setFormToolsOnMainToolbar(toolbarsInFormWindowButton.getSelection());
		prefs.setCloseEditorsOnExit(closeEditorOnExitButton.getSelection());
		PrefUtil.getAPIPreferenceStore().setValue(IWorkbenchPreferenceConstants.CLOSE_EDITORS_ON_EXIT, closeEditorOnExitButton.getSelection());
		prefs.setOpenFirstFormDesigner(openFirstFormDesignerButton.getSelection());
		prefs.setShowColumnsInDbOrder(showColumnsInDbOrderButton.getSelection());
		prefs.setPrimaryKeySequenceType(
			((Integer)((ObjectWrapper)((IStructuredSelection)primaryKeySequenceTypeCombo.getSelection()).getFirstElement()).getType()).intValue());
		prefs.setShowNavigatorDefault(showNavigatorDefaultButton.getSelection());
		prefs.setEncapsulationType(
			((Integer)((ObjectWrapper)((IStructuredSelection)encapsulationTypeCombo.getSelection()).getFirstElement()).getType()).intValue());
		prefs.setTestClientLoadTimeout(waitForSolutionToBeLoadedInTestClientSpinner.getSelection());
		prefs.save();

		IconPreferences.getInstance().setUseDarkThemeIcons(useDarkIconsButton.getSelection());
		IconPreferences.getInstance().save();
		return true;
	}

	private void setPrimaryKeySequenceTypeValue(int pk_seq_type)
	{
		Integer seqType = Integer.valueOf(pk_seq_type);
		for (ObjectWrapper ow : (ObjectWrapper[])primaryKeySequenceTypeCombo.getInput())
		{
			if (ow.getType().equals(seqType))
			{
				primaryKeySequenceTypeCombo.setSelection(new StructuredSelection(ow));
				return;
			}
		}
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
