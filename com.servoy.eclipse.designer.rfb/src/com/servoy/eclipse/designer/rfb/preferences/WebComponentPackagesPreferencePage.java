package com.servoy.eclipse.designer.rfb.preferences;

import java.util.Set;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;

public class WebComponentPackagesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{

	public WebComponentPackagesPreferencePage()
	{
		super(GRID);
		setMessage("Mark packages as unused.");
		setDescription("Unchecked component/service packages are not loaded in the client or shown in the palette:");
	}


	@Override
	public void init(IWorkbench workbench)
	{
		setPreferenceStore(PlatformUI.getPreferenceStore());
	}

	@Override
	protected void createFieldEditors()
	{
		Set<String> defaultPackageNames = ResourceProvider.getDefaultPackageNames();
		for (String packageName : defaultPackageNames)
		{
			String displayName = packageName;
			SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
			if (componentsSpecProviderState.getPackageDisplayName(packageName) != null)
			{
				displayName = componentsSpecProviderState.getPackageDisplayName(packageName);
			}
			else
			{
				SpecProviderState servicesSpecProviderState = WebServiceSpecProvider.getSpecProviderState();
				if (servicesSpecProviderState.getPackageDisplayName(packageName) != null)
				{
					displayName = servicesSpecProviderState.getPackageDisplayName(packageName);
				}
			}
			if (packageName.equals("servoyservices")) continue;//we do not allow disabling the services package
			if (packageName.equals("servoycore")) continue;//we do not allow disabling the core package
			addField(new BooleanFieldEditor("com.servoy.eclipse.designer.rfb.packages.enable." + packageName, displayName, getFieldEditorParent()));
		}
	}

}
