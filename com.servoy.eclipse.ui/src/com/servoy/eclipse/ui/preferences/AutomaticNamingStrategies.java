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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
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
	private Button defaultFormEventHandlerNaming;
	private Button includeFormElementNameRadio;
	private Button includeFormElementDataProviderRadio;
	private Button includeFormElementDataProviderWithFallbackRadio;

	private Button defaultTableEventHandlerNaming;
	private Button includeTableNameInEventHandlerRadio;

	private Button defaultLoadedRelationsNaming;
	private Button customLoadedRelationsNamingPattern;
	private Text loadRelationsNamingPatternText;

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

		defaultFormEventHandlerNaming = new Button(grpFormEventHandlerNaming, SWT.RADIO);
		defaultFormEventHandlerNaming.setText("Default naming"); //$NON-NLS-1$

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

		Group grpLoadedRelationsNaming = new Group(rootPanel, SWT.NONE);
		grpLoadedRelationsNaming.setText("Loaded relations naming"); //$NON-NLS-1$
		grpLoadedRelationsNaming.setLayout(new GridLayout(1, false));
		grpLoadedRelationsNaming.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		defaultLoadedRelationsNaming = new Button(grpLoadedRelationsNaming, SWT.RADIO);
		defaultLoadedRelationsNaming.setText("Default naming"); //$NON-NLS-1$
		defaultLoadedRelationsNaming.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (defaultLoadedRelationsNaming.getSelection())
				{
					loadRelationsNamingPatternText.setText("");
				}
			}
		});

		customLoadedRelationsNamingPattern = new Button(grpLoadedRelationsNaming, SWT.RADIO);
		customLoadedRelationsNamingPattern.setText("Use custom pattern for single-item relations"); //$NON-NLS-1$
		customLoadedRelationsNamingPattern.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				loadRelationsNamingPatternText.setEnabled(customLoadedRelationsNamingPattern.getSelection());
				loadRelationsNamingPatternText.setText("${primarytable}_to_${foreigntable}");
			}
		});

		loadRelationsNamingPatternText = new Text(grpLoadedRelationsNaming, SWT.NONE);
		loadRelationsNamingPatternText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));
		loadRelationsNamingPatternText.setToolTipText("Define custom pattern, use ${primarytable}, ${foreigntable}, ${primarycolumn} and ${foreigncolumn} for substitution");

		initializeFields();

		return rootPanel;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		defaultFormEventHandlerNaming.setSelection(prefs.getFormEventHandlerNamingDefault());
		includeFormElementNameRadio.setSelection(prefs.getIncludeFormElementName());
		includeFormElementDataProviderRadio.setSelection(prefs.getIncludeFormElementDataProviderName());
		includeFormElementDataProviderWithFallbackRadio.setSelection(prefs.getIncludeFormElementDataProviderNameWithFallback());

		defaultTableEventHandlerNaming.setSelection(prefs.getTableEventHandlerNamingDefault());
		includeTableNameInEventHandlerRadio.setSelection(prefs.getIncludeTableName());

		String loadedRelationsNamingPattern = prefs.getLoadedRelationsNamingPattern();
		defaultLoadedRelationsNaming.setSelection(loadedRelationsNamingPattern == null);
		customLoadedRelationsNamingPattern.setSelection(!defaultLoadedRelationsNaming.getSelection());
		loadRelationsNamingPatternText.setText(loadedRelationsNamingPattern == null ? "" : loadedRelationsNamingPattern);
		loadRelationsNamingPatternText.setEnabled(customLoadedRelationsNamingPattern.getSelection());
	}

	@Override
	protected void performDefaults()
	{
		defaultFormEventHandlerNaming.setSelection(true);
		includeFormElementNameRadio.setSelection(false);
		includeFormElementDataProviderRadio.setSelection(false);
		includeFormElementDataProviderWithFallbackRadio.setSelection(false);

		defaultTableEventHandlerNaming.setSelection(true);
		includeTableNameInEventHandlerRadio.setSelection(false);

		defaultLoadedRelationsNaming.setSelection(true);
		customLoadedRelationsNamingPattern.setSelection(false);
		loadRelationsNamingPatternText.setText("");
		loadRelationsNamingPatternText.setEnabled(false);

		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		int formNaming;
		if (includeFormElementNameRadio.getSelection()) formNaming = DesignerPreferences.FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_NAME;
		else if (includeFormElementDataProviderRadio.getSelection()) formNaming = DesignerPreferences.FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_DATAPROVIDER;
		else if (includeFormElementDataProviderWithFallbackRadio.getSelection()) formNaming = DesignerPreferences.FORM_EVENT_HANDLER_NAMING_INCLUDE_FORM_ELEMENT_DATAPROVIDER_FALLBACK;
		else formNaming = DesignerPreferences.FORM_EVENT_HANDLER_NAMING_DEFAULT;
		prefs.setFormEventHandlerNaming(formNaming);

		prefs.setTableEventHandlerNaming(includeTableNameInEventHandlerRadio.getSelection() ? DesignerPreferences.TABLE_EVENT_HANDLER_NAMING_INCLUDE_TABLE_NAME
			: DesignerPreferences.TABLE_EVENT_HANDLER_NAMING_DEFAULT);

		String loadedRelationsNamingPattern = null;
		if (customLoadedRelationsNamingPattern.getSelection())
		{
			loadedRelationsNamingPattern = loadRelationsNamingPatternText.getText();
			if (loadedRelationsNamingPattern != null && loadedRelationsNamingPattern.trim().length() == 0)
			{
				loadedRelationsNamingPattern = null;
			}
		}
		prefs.setLoadedRelationsNamingPattern(loadedRelationsNamingPattern);

		prefs.save();

		return true;
	}
}
