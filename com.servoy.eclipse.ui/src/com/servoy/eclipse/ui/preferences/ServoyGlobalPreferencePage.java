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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.internal.util.PrefUtil;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.debug.DeveloperPreferences;

/**
 * Main preference page for Servoy settings.
 * 
 * @author rgansevles
 *
 */
public class ServoyGlobalPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
	private Button securityChangeButton;
	private Button toolbarsInFormWindowButton;
	private Button closeEditorOnExitButton;
	private Button openFirstFormDesignerButton;
	private Button showColumnsInDbOrderButton;
	private Button showColumnsInAlphabeticOrderButton;
	private Label enhancedSecurityLabel;

	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
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
		rootContainer.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		if (!new DeveloperPreferences(ServoyModel.getSettings()).getEnhancedSecurity())
		{
			Group securityInfoContainer = new Group(rootContainer, SWT.NONE);
			securityInfoContainer.setText("Security Information");
			securityInfoContainer.setLayout(new GridLayout(2, false));
			securityInfoContainer.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

			enhancedSecurityLabel = new Label(securityInfoContainer, SWT.NONE);
			enhancedSecurityLabel.setText("Servoy Application Server NOT is running with Enhanced Security, this is strongly discouraged");
			enhancedSecurityLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));

			securityChangeButton = new Button(securityInfoContainer, SWT.NONE);
			securityChangeButton.setText("Change");
			securityChangeButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					new DeveloperPreferences(ServoyModel.getSettings()).setEnhancedSecurity(true);

					enhancedSecurityLabel.setText("Servoy Application Server is running with Enhanced Security");
					enhancedSecurityLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
					securityChangeButton.setVisible(false);
				}
			});
		}

		// Form editor options
		Group formEditorOptionsContainer = new Group(rootContainer, SWT.NONE);
		formEditorOptionsContainer.setText("Form Editor Options");
		formEditorOptionsContainer.setLayout(new GridLayout(2, false));
		formEditorOptionsContainer.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		toolbarsInFormWindowButton = new Button(formEditorOptionsContainer, SWT.CHECK);
		toolbarsInFormWindowButton.setText("Show Form Editing Toolbars inside Form Editor"); //$NON-NLS-1$

		Button resetToolbarsButton = new Button(formEditorOptionsContainer, SWT.NONE);
		resetToolbarsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				new DesignerPreferences().saveCoolbarLayout(null);
			}
		});
		resetToolbarsButton.setText("Show all"); //$NON-NLS-1$

		closeEditorOnExitButton = new Button(formEditorOptionsContainer, SWT.CHECK);
		closeEditorOnExitButton.setText("Close Editors at shutdown");
		new Label(formEditorOptionsContainer, SWT.NONE); // dummy

		openFirstFormDesignerButton = new Button(formEditorOptionsContainer, SWT.CHECK);
		openFirstFormDesignerButton.setText("Open the first form designer on activating a solution");
		new Label(formEditorOptionsContainer, SWT.NONE); // dummy

		// Wizard options
		Group wizardOptionsContainer = new Group(rootContainer, SWT.NONE);
		wizardOptionsContainer.setText("Wizard options");
		wizardOptionsContainer.setLayout(new GridLayout(1, false));
		wizardOptionsContainer.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		Group columnsOrderGroup = new Group(wizardOptionsContainer, SWT.NONE);
		columnsOrderGroup.setText("Show table columns");
		columnsOrderGroup.setLayout(new GridLayout(1, true));

		showColumnsInAlphabeticOrderButton = new Button(columnsOrderGroup, SWT.RADIO);
		showColumnsInAlphabeticOrderButton.setText("in alphabetic order (key columns first)");

		showColumnsInDbOrderButton = new Button(columnsOrderGroup, SWT.RADIO);
		showColumnsInDbOrderButton.setText("in database defined order");

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

		prefs.save();

		return true;
	}
}
