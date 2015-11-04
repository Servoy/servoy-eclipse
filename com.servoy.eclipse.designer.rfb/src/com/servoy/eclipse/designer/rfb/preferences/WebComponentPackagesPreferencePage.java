package com.servoy.eclipse.designer.rfb.preferences;

import java.util.Set;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sablo.specification.WebComponentSpecProvider;

import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;

public class WebComponentPackagesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{

	public WebComponentPackagesPreferencePage()
	{
		super(GRID);
	}


	@Override
	public void init(IWorkbench workbench)
	{
		setPreferenceStore(com.servoy.eclipse.debug.Activator.getDefault().getPreferenceStore());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors()
	{
		Set<String> defaultPackageNames = ResourceProvider.getDefaultPackageNames();
		for (String packageName : defaultPackageNames)
		{
			String displayName = packageName;
			if (WebComponentSpecProvider.getInstance().getPackageDisplayName(packageName) != null)
				displayName = WebComponentSpecProvider.getInstance().getPackageDisplayName(packageName);
			if (packageName.equals("servoyservices")) continue;//we do not allow disabling the services package
			if (packageName.equals("servoycore")) continue;//we do not allow disabling the core package
			addField(new BooleanFieldEditor("com.servoy.eclipse.designer.rfb.packages.enable." + packageName, "Enable " + displayName + " package",
				getFieldEditorParent()));
		}
	}

}
