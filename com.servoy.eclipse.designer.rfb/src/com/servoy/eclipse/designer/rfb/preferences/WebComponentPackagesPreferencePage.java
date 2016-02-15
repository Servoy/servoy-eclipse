package com.servoy.eclipse.designer.rfb.preferences;

import java.util.Set;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;

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
			if (WebComponentSpecProvider.getInstance().getPackageDisplayName(packageName) != null)
			{
				displayName = WebComponentSpecProvider.getInstance().getPackageDisplayName(packageName);
			}
			else if (WebServiceSpecProvider.getInstance().getPackageDisplayName(packageName) != null)
			{
				displayName = WebServiceSpecProvider.getInstance().getPackageDisplayName(packageName);
			}
			if (packageName.equals("servoyservices")) continue;//we do not allow disabling the services package
			if (packageName.equals("servoycore")) continue;//we do not allow disabling the core package
			// TODO add some tooltips or a label to this page explaining what all this means to the user...
			addField(new BooleanFieldEditor("com.servoy.eclipse.designer.rfb.packages.enable." + packageName, "Enable " + displayName + " package",
					getFieldEditorParent()));
		}
	}

}
