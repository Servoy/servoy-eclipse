package com.servoy.eclipse.designer.rfb.preferences;

import java.util.Set;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
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
		super(FLAT); // needs to be FLAT in order to have a separate parent for each FieldEditor - needed by the hack in createFieldEditors(...) below
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
		boolean first = true;
		Composite parentForLastFieldThatShouldBeSeparatedVisually = getFieldEditorParent(); // this call returns a separate instance each time if style of this page is FLAT - see constructor
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
			addField(new BooleanFieldEditor("com.servoy.eclipse.designer.rfb.packages.enable." + packageName, displayName,
				parentForLastFieldThatShouldBeSeparatedVisually));

			if (first)
			{
				setTopMargin(parentForLastFieldThatShouldBeSeparatedVisually, 10);
				first = false;
			}

			parentForLastFieldThatShouldBeSeparatedVisually = getFieldEditorParent(); // this call returns a separate instance each time if style of this page is FLAT - see constructor
		}

		BooleanFieldEditor showLegacyEditor = new BooleanFieldEditor("com.servoy.eclipse.designer.rfb.show.default.package",
			"Always show the default/legacy servoy components in designer (if enabled above)",
			parentForLastFieldThatShouldBeSeparatedVisually);
		showLegacyEditor.getDescriptionControl(parentForLastFieldThatShouldBeSeparatedVisually)
			.setToolTipText("if this is not checked, the legacy components will only show in the form designer's palette if that form already uses them");
		addField(showLegacyEditor);

		setTopMargin(parentForLastFieldThatShouldBeSeparatedVisually, 25);
	}


	private void setTopMargin(Composite parentForLastFieldThatShouldBeSeparatedVisually, int topMargin)
	{
		// a bit of a hack to separate last checkbox which is not representing a package but a separate thing
		Layout layoutOfP = parentForLastFieldThatShouldBeSeparatedVisually.getLayout();
		if (layoutOfP instanceof GridLayout)
		{
			((GridLayout)layoutOfP).marginTop = topMargin;
		}
	}

}
