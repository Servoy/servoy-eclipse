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
	private Button securityChangeButton;
	private Button toolbarsInFormWindowButton;
	private Button closeEditorOnExitButton;
	private Button openFirstFormDesignerButton;
	private Button showColumnsInDbOrderButton;
	private Button showColumnsInAlphabeticOrderButton;
	private Label enhancedSecurityLabel;
	private ComboViewer primaryKeySequenceTypeCombo;
	private Button showNavigatorDefaultButton;

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

		// Servoy Design Perspective
		Group formEditorOptionsContainer = new Group(rootContainer, SWT.NONE);
		formEditorOptionsContainer.setText("Servoy Design Perspective Options");
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
		openFirstFormDesignerButton.setText("Open Solution's main Form in Form Editor on Solution activation");
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

		//Table Creation Options
		Group tableCreationSettings = new Group(rootContainer, SWT.NONE);
		tableCreationSettings.setText("Table Creation Settings"); //$NON-NLS-1$
		tableCreationSettings.setLayout(new GridLayout(1, false));
		tableCreationSettings.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		Group defaultPrimaryKeySequenceType = new Group(tableCreationSettings, SWT.NONE);
		defaultPrimaryKeySequenceType.setText("Default Primary Key Sequence Type"); //$NON-NLS-1$
		defaultPrimaryKeySequenceType.setLayout(new GridLayout(1, true));

		primaryKeySequenceTypeCombo = new ComboViewer(defaultPrimaryKeySequenceType);
		primaryKeySequenceTypeCombo.setContentProvider(new ArrayContentProvider());
		primaryKeySequenceTypeCombo.setLabelProvider(new LabelProvider());
		primaryKeySequenceTypeCombo.setInput(new ObjectWrapper[] { new ObjectWrapper("Servoy Sequence", new Integer(ColumnInfo.SERVOY_SEQUENCE)), new ObjectWrapper( //$NON-NLS-1$
			"Database Sequence", new Integer(ColumnInfo.DATABASE_SEQUENCE)), new ObjectWrapper("Database Identity", new Integer(ColumnInfo.DATABASE_IDENTITY)), new ObjectWrapper( //$NON-NLS-1$ //$NON-NLS-2$
			"UUID Generator", new Integer(ColumnInfo.UUID_GENERATOR)) }); //$NON-NLS-1$

		//Form Properties
		Group formProperties = new Group(rootContainer, SWT.NONE);
		formProperties.setText("Form Properties"); //$NON-NLS-1$
		formProperties.setLayout(new GridLayout(1, true));
		formProperties.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		showNavigatorDefaultButton = new Button(formProperties, SWT.CHECK);
		showNavigatorDefaultButton.setText("Show Navigator default setting - use at new form creation"); //$NON-NLS-1$

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
		prefs.setPrimaryKeySequenceType(((Integer)((ObjectWrapper)((IStructuredSelection)primaryKeySequenceTypeCombo.getSelection()).getFirstElement()).getType()).intValue());
		prefs.setShowNavigatorDefault(showNavigatorDefaultButton.getSelection());
		prefs.save();

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
}
