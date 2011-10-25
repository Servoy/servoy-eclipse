/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for automatic naming strategies settings.
 * These strategies apply to event handler naming of form elements and tables. 
 * 
 * @author acostache
 *
 */
public class AutomaticNamingStrategies extends PreferencePage implements IWorkbenchPreferencePage
{
	private Button defalutFormEventHandlerNaming;
	private Button includeFormElementNameRadio;
	private Button includeFormElementDataProviderRadio;
	private Button includeFormElementDataProviderWithFallbackRadio;

	private Button defaultTableEventHandlerNaming;
	private Button includeTableNameInEventHandlerRadio;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite rootPanel = new Composite(parent, SWT.NONE);
		rootPanel.setLayout(new GridLayout(1, true));
		rootPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		Group grpFormEventHandlerNaming = new Group(rootPanel, SWT.NONE);
		grpFormEventHandlerNaming.setText("Form event handler naming"); //$NON-NLS-1$
		grpFormEventHandlerNaming.setLayout(new GridLayout(1, false));
		grpFormEventHandlerNaming.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		defalutFormEventHandlerNaming = new Button(grpFormEventHandlerNaming, SWT.RADIO);
		defalutFormEventHandlerNaming.setText("Default naming"); //$NON-NLS-1$

		includeFormElementNameRadio = new Button(grpFormEventHandlerNaming, SWT.RADIO);
		includeFormElementNameRadio.setText("Include element name"); //$NON-NLS-1$

		includeFormElementDataProviderRadio = new Button(grpFormEventHandlerNaming, SWT.RADIO);
		includeFormElementDataProviderRadio.setText("Include dataprovider name"); //$NON-NLS-1$

		includeFormElementDataProviderWithFallbackRadio = new Button(grpFormEventHandlerNaming, SWT.RADIO);
		includeFormElementDataProviderWithFallbackRadio.setText("Include dataprovider name (fallback to element name)"); //$NON-NLS-1$

		Group grpTableEventHandlerNaming = new Group(rootPanel, SWT.NONE);
		grpTableEventHandlerNaming.setText("Table event handler naming"); //$NON-NLS-1$
		grpTableEventHandlerNaming.setLayout(new GridLayout(1, false));
		grpTableEventHandlerNaming.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		defaultTableEventHandlerNaming = new Button(grpTableEventHandlerNaming, SWT.RADIO);
		defaultTableEventHandlerNaming.setText("Default naming"); //$NON-NLS-1$

		includeTableNameInEventHandlerRadio = new Button(grpTableEventHandlerNaming, SWT.RADIO);
		includeTableNameInEventHandlerRadio.setText("Include table name"); //$NON-NLS-1$

		initializeFields();

		return rootPanel;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		defalutFormEventHandlerNaming.setSelection(prefs.getDefaultFormEventHandlerNaming());
		includeFormElementNameRadio.setSelection(prefs.getIncludeFormElementName());
		includeFormElementDataProviderRadio.setSelection(prefs.getIncludeFormElementDataProviderName());
		includeFormElementDataProviderWithFallbackRadio.setSelection(prefs.getIncludeFormElementDataProviderNameWithFallback());

		defaultTableEventHandlerNaming.setSelection(prefs.getDefaultTableEventHandlerNaming());
		includeTableNameInEventHandlerRadio.setSelection(prefs.getIncludeTableName());
	}

	@Override
	protected void performDefaults()
	{
		defalutFormEventHandlerNaming.setSelection(DesignerPreferences.DEFAULT_FORM_EVENT_HANDLER_NAMING_DEFAULT);
		includeFormElementNameRadio.setSelection(DesignerPreferences.INCLUDE_FORM_ELEMENT_NAME_DEFAULT);
		includeFormElementDataProviderRadio.setSelection(DesignerPreferences.INCLUDE_FORM_ELEMENT_DATAPROVIDER_DEFAULT);
		includeFormElementDataProviderWithFallbackRadio.setSelection(DesignerPreferences.INCLUDE_FORM_ELEMENT_DATAPROVIDER_FALLBACK_DEFAULT);

		defaultTableEventHandlerNaming.setSelection(DesignerPreferences.DEFAULT_TABLE_EVENT_HANDLER_NAMING_DEFAULT);
		includeTableNameInEventHandlerRadio.setSelection(DesignerPreferences.INCLUDE_TABLE_NAME_DEFAULT);

		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		prefs.setDefaultFormEventHandlerNaming(defalutFormEventHandlerNaming.getSelection());
		prefs.setIncludeFormElementName(includeFormElementNameRadio.getSelection());
		prefs.setIncludeFormElementDataProviderName(includeFormElementDataProviderRadio.getSelection());
		prefs.setIncludeFormElementDataProviderNameWithFallback(includeFormElementDataProviderWithFallbackRadio.getSelection());

		prefs.setDefaultTableEventHandlerNaming(defaultTableEventHandlerNaming.getSelection());
		prefs.setIncludeTableName(includeTableNameInEventHandlerRadio.getSelection());

		prefs.save();

		return true;
	}

}
