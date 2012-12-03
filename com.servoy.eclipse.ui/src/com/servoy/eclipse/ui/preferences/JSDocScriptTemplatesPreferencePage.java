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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Preference page method and variable jsdoc templates.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public class JSDocScriptTemplatesPreferencePage extends WorkspaceOrProjectPreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{

	private JSDocScriptTemplates jsDocScriptTemplates;
	private Text newVarJsDoc;
	private Text newMethodJsDoc;
	ControlEnableState rootPannelEnabledState = null;
	Composite rootPanel = null;

	public JSDocScriptTemplatesPreferencePage()
	{
		super();
	}

	public void init(IWorkbench workbench)
	{

		setProject(null);
		jsDocScriptTemplates = JSDocScriptTemplates.getTemplates(null, false);
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		rootPanel = new Composite(parent, SWT.NONE);
		rootPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		rootPanel.setLayout(null);

		newMethodJsDoc = new Text(rootPanel, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		newMethodJsDoc.setBounds(17, 34, 313, 129);

		newVarJsDoc = new Text(rootPanel, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		newVarJsDoc.setBounds(17, 188, 313, 129);

		Label lblNewMethodJavadoc = new Label(rootPanel, SWT.NONE);
		lblNewMethodJavadoc.setBounds(10, 10, 290, 17);
		lblNewMethodJavadoc.setText("New method javadoc tags");

		Label lblNewVariableJavadoc = new Label(rootPanel, SWT.NONE);
		lblNewVariableJavadoc.setText("New variable javadoc tags");
		lblNewVariableJavadoc.setBounds(10, 165, 290, 17);

		initializeFields();
		if (isProjectPreferencePage())
		{
			if (hasProjectSpecificOptions(getProject()))
			{
				enablePreferencePageContent(true);
				getfUseProjectSettings().setSelection(true);
			}
			else
			{
				enablePreferencePageContent(false);
				getfUseProjectSettings().setSelection(false);
			}
		}

		return rootPanel;
	}

	protected void initializeFields()
	{
		newMethodJsDoc.setText(jsDocScriptTemplates.getMethodTemplateProperty());
		newVarJsDoc.setText(jsDocScriptTemplates.getVariableTemplateProperty());
	}

	@Override
	protected void performDefaults()
	{
		newMethodJsDoc.setText("");
		newVarJsDoc.setText("");

		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		String method = newMethodJsDoc.getText();
		String var = newVarJsDoc.getText();

		if (!isProjectSpecificSettingsChecked() && isProjectPreferencePage())
		{
			// delete settings
			jsDocScriptTemplates.clear();
			jsDocScriptTemplates.save();
			return true;
		}
		jsDocScriptTemplates.setMethodTemplateProperty(method);
		jsDocScriptTemplates.setVariableTemplateProperty(var);
		jsDocScriptTemplates.save();
		return true;
	}


	public IAdaptable getElement()
	{
		return getProject();
	}


	public void setElement(IAdaptable element)
	{
		setProject((IProject)element.getAdapter(IResource.class));
		if (getProject() != null)
		{
			jsDocScriptTemplates = JSDocScriptTemplates.getTemplates(getProject(), false);
		}
	}

	@Override
	protected String getPreferencePageId()
	{
		return "com.servoy.eclipse.ui.preferences.jsDocTemplates";
	}


	@Override
	protected String getPropertyPageId()
	{
		return "com.servoy.eclipse.ui.propertyPage.jsDocTemplates";
	}


	@Override
	protected boolean hasProjectSpecificOptions(IProject project)
	{
		try
		{
			JSDocScriptTemplates templatesemplates = JSDocScriptTemplates.getTemplates(project, false);
			if (templatesemplates != null)
			{
				if (templatesemplates.getSettingsNode().keys().length > 0)
				{
					return true;
				}
			}
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
		return false;
	}


	@Override
	protected void enablePreferencePageContent(boolean enable)
	{
		if (enable)
		{
			if (rootPannelEnabledState != null)
			{
				rootPannelEnabledState.restore();
				rootPannelEnabledState = null;
			}
		}
		else
		{
			if (rootPannelEnabledState == null)
			{
				rootPannelEnabledState = ControlEnableState.disable(rootPanel);
			}
		}

	}

}
