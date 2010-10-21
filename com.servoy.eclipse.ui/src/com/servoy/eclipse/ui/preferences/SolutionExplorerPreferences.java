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
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenSqlEditorAction;

public class SolutionExplorerPreferences extends PreferencePage implements IWorkbenchPreferencePage
{
	private Button chAutomaticPerspectiveSwitch;
	private Button chOpenFormEditor;
	private Button chOpenScriptEditor;

	public static final String DOUBLE_CLICK_ACTION = "dblClickAction";
	public static final String DOUBLE_CLICK_OPEN_FORM = "openFormEditor";
	public static final String DOUBLE_CLICK_OPEN_SCRIPT = "openSciptEditor";

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


		chAutomaticPerspectiveSwitch = new Button(cp, SWT.CHECK);
		chAutomaticPerspectiveSwitch.setText("Activate SQL Explorer perspective on 'Open SQL Editor'");

		chOpenFormEditor = new Button(cp, SWT.CHECK);
		chOpenFormEditor.setText("Double click on form in Solution Explorer View opens Form Editor");

		chOpenScriptEditor = new Button(cp, SWT.CHECK);
		chOpenScriptEditor.setText("Double click on form/globals in Solution Explorer View opens Script Editor");

		Preferences store = Activator.getDefault().getPluginPreferences();
		String option = store.getString(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY);
		chAutomaticPerspectiveSwitch.setSelection(MessageDialogWithToggle.ALWAYS.equals(option) ? true : false);

		option = store.getString(DOUBLE_CLICK_ACTION);
		if (DOUBLE_CLICK_OPEN_FORM.equals(option))
		{
			chOpenFormEditor.setSelection(true);
		}
		else if (DOUBLE_CLICK_OPEN_SCRIPT.equals(option))
		{
			chOpenScriptEditor.setSelection(true);
		}
		chOpenFormEditor.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (chOpenFormEditor.getSelection()) chOpenScriptEditor.setSelection(false);
			}
		});
		chOpenScriptEditor.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (chOpenScriptEditor.getSelection()) chOpenFormEditor.setSelection(false);
			}
		});
		return cp;
	}

	public void init(IWorkbench workbench)
	{

	}

	@Override
	public boolean performOk()
	{
		Preferences store = Activator.getDefault().getPluginPreferences();
		if (chAutomaticPerspectiveSwitch.getSelection())
		{
			store.setValue(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY, MessageDialogWithToggle.ALWAYS);
		}
		else
		{
			String option = store.getString(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY);
			if (option == null || MessageDialogWithToggle.PROMPT.equals(option))
			{
				store.setValue(OpenSqlEditorAction.AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY, MessageDialogWithToggle.NEVER);
			}
		}
		if (chOpenFormEditor.getSelection())
		{
			store.setValue(DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_FORM);
		}
		else if (chOpenScriptEditor.getSelection())
		{
			store.setValue(DOUBLE_CLICK_ACTION, DOUBLE_CLICK_OPEN_SCRIPT);
		}
		else
		{
			store.setValue(DOUBLE_CLICK_ACTION, Preferences.STRING_DEFAULT_DEFAULT);
		}
		Activator.getDefault().savePluginPreferences();

		return super.performOk();
	}

	@Override
	protected void performDefaults()
	{
		super.performDefaults();
		chAutomaticPerspectiveSwitch.setSelection(false);
		chOpenFormEditor.setSelection(false);
		chOpenScriptEditor.setSelection(false);
	}

}
