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
import org.eclipse.ui.IWorkbenchPreferencePage;

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
	public static final boolean SAVE_EDITOR_STATE_DEFAULT = true;
	public static final boolean OPEN_FIRST_FORM_DESIGNER_DEFAULT = true;

	private Label enhancedSecurityLabel;
	private Button securityChangeButton;
	private Button saveEditorStateButton;
	private Button openFirstFormDesignerButton;

	public ServoyGlobalPreferencePage()
	{
	}

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

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootContainer.setLayout(layout);

		GridData rootGridData = new GridData();
		rootGridData.verticalAlignment = GridData.FILL;
		rootGridData.horizontalAlignment = GridData.FILL;
		rootContainer.setLayoutData(rootGridData);

		Group securityInfoContainer = new Group(rootContainer, SWT.NONE);
		securityInfoContainer.setText("Security Information");
		GridLayout securityInfoLayout = new GridLayout();
		securityInfoLayout.numColumns = 2;
		securityInfoContainer.setLayout(securityInfoLayout);

		GridData securityInfoGridData = new GridData();
		securityInfoGridData.verticalAlignment = GridData.FILL;
		securityInfoGridData.horizontalAlignment = GridData.FILL;
		securityInfoContainer.setLayoutData(securityInfoGridData);

		enhancedSecurityLabel = new Label(securityInfoContainer, SWT.NONE);
		enhancedSecurityLabel.setText("loading...");

		securityChangeButton = new Button(securityInfoContainer, SWT.NONE);
		securityChangeButton.setText("Change");
		securityChangeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				new DeveloperPreferences(ServoyModel.getSettings()).setEnhancedSecurity(true);
				initializeFields();
			}
		});

		Group optionsContainer = new Group(rootContainer, SWT.NONE);
		optionsContainer.setText("Form Editor Options");

		GridLayout optionsLayout = new GridLayout();
		optionsLayout.numColumns = 1;
		optionsContainer.setLayout(optionsLayout);

		GridData optionsGridData = new GridData();
		optionsGridData.verticalAlignment = GridData.FILL;
		optionsGridData.horizontalAlignment = GridData.FILL;
		optionsContainer.setLayoutData(optionsGridData);

		saveEditorStateButton = new Button(optionsContainer, SWT.CHECK);
		saveEditorStateButton.setText("Re-open Form Editors at startup");

		openFirstFormDesignerButton = new Button(optionsContainer, SWT.CHECK);
		openFirstFormDesignerButton.setText("Open the first form designer on activating a solution");

		initializeFields();

		return rootContainer;
	}

	protected void initializeFields()
	{
		if (new DeveloperPreferences(ServoyModel.getSettings()).getEnhancedSecurity())
		{
			enhancedSecurityLabel.setText("Servoy Application Server is running with Enhanced Security");
			enhancedSecurityLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			securityChangeButton.setVisible(false);
		}
		else
		{
			enhancedSecurityLabel.setText("Servoy Application Server NOT is running with Enhanced Security, this is strongly discouraged");
			enhancedSecurityLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
			securityChangeButton.setVisible(true);
		}

		DesignerPreferences prefs = new DesignerPreferences();

		saveEditorStateButton.setSelection(prefs.getSaveEditorState());
		openFirstFormDesignerButton.setSelection(prefs.getOpenFirstFormDesigner());
	}

	@Override
	protected void performDefaults()
	{
		saveEditorStateButton.setSelection(ServoyGlobalPreferencePage.SAVE_EDITOR_STATE_DEFAULT);
		openFirstFormDesignerButton.setSelection(ServoyGlobalPreferencePage.OPEN_FIRST_FORM_DESIGNER_DEFAULT);

		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		prefs.setSaveEditorState(saveEditorStateButton.getSelection());
		prefs.setOpenFirstFormDesigner(openFirstFormDesignerButton.getSelection());

		prefs.save();

		return true;
	}
}
