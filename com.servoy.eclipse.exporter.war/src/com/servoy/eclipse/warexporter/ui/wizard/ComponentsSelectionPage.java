/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.warexporter.export.ExportWarModel;

/**
 * Shows the user the list of needed components for export and allows him to add more components to export.
 * @author emera
 */
public class ComponentsSelectionPage extends AbstractComponentsSelectionPage
{

	private final SpecProviderState componentsSpecProviderState;

	protected ComponentsSelectionPage(ExportWarModel exportModel, SpecProviderState componentsSpecProviderState, String pageName, String title,
		String description)
	{
		super(exportModel, pageName, title, description, "component");
		this.componentsSpecProviderState = componentsSpecProviderState;
		componentsUsed = exportModel.getUsedComponents();
		selectedComponents = new TreeSet<String>(componentsUsed);
		joinWithLastUsed();
	}

	@Override
	protected void joinWithLastUsed()
	{
		if (exportModel.getExportedComponents() == null ||
			exportModel.getExportedComponents().containsAll(componentsUsed) && componentsUsed.containsAll(exportModel.getExportedComponents())) return;
		for (String component : exportModel.getExportedComponents())
		{
			//make sure that the component exported the last time was not removed
			if (componentsSpecProviderState.getWebComponentSpecification(component) != null) selectedComponents.add(component);
		}
	}

	@Override
	protected Set<String> getAvailableItems()
	{
		Set<String> availableComponents = new TreeSet<String>();
		for (WebObjectSpecification spec : componentsSpecProviderState.getAllWebComponentSpecifications())
		{
			if (exportModel.getExcludedComponentPackages().contains(spec.getPackageName())) continue;
			if (!selectedComponents.contains(spec.getName())) availableComponents.add(spec.getName());
		}
		return availableComponents;
	}

	@Override
	public IWizardPage getNextPage()
	{
		exportModel.setExportedComponents(new TreeSet<String>(Arrays.asList(selectedComponentsList.getItems())));
		return super.getNextPage();
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_components");
	}
}
