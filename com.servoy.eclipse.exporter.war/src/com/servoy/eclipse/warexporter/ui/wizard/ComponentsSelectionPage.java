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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.ui.PlatformUI;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.warexporter.export.ExportWarModel;

/**
 * Shows the user the list of needed components for export and allows him to add more components to export.
 * @author emera
 */
public class ComponentsSelectionPage extends AbstractWebObjectSelectionPage
{

	private final SpecProviderState componentsSpecProviderState;

	protected ComponentsSelectionPage(ExportWarModel exportModel, SpecProviderState componentsSpecProviderState, String pageName, String title,
		String description)
	{
		super(exportModel, pageName, title, description, "component");
		this.componentsSpecProviderState = componentsSpecProviderState;
	}

	@Override
	protected Set<String> getWebObjectsExplicitlyUsedBySolution()
	{
		return exportModel.getComponentsUsedExplicitlyBySolution();
	}

	@Override
	protected void joinWithLastUsed()
	{
		Set<String> componentsToExportWithoutUnderTheHoodOnes = exportModel.getComponentsToExportWithoutUnderTheHoodOnes();
		if (componentsToExportWithoutUnderTheHoodOnes == null ||
			webObjectsUsedExplicitlyBySolution.containsAll(componentsToExportWithoutUnderTheHoodOnes)) return;

		for (String component : componentsToExportWithoutUnderTheHoodOnes)
		{
			// make sure that the component exported the last time was not removed
			if (componentsSpecProviderState.getWebObjectSpecification(component) != null) selectedWebObjectsForListCreation.add(component);
		}
	}

	@Override
	protected Set<String> getAvailableItems(boolean alreadyPickedAtListCreationShouldBeInThere)
	{
		Set<String> availableComponents = new TreeSet<String>();
		List<String> preferencesExcludedDefaultComponentPackages = exportModel.getPreferencesExcludedDefaultComponentPackages();
		Set<String> componentsNeededUnderTheHood = exportModel.getComponentsNeededUnderTheHood();
		for (WebObjectSpecification spec : componentsSpecProviderState.getAllWebObjectSpecifications())
		{
			if (!preferencesExcludedDefaultComponentPackages.contains(spec.getPackageName()) &&
				!componentsNeededUnderTheHood.contains(spec.getName()) &&
				(alreadyPickedAtListCreationShouldBeInThere || !selectedWebObjectsForListCreation.contains(spec.getName())))
			{
				availableComponents.add(spec.getName());
			}
		}
		return availableComponents;
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_components");
	}

	@Override
	public void storeInput()
	{
		exportModel.setComponentsToExportWithoutUnderTheHoodOnes(new TreeSet<String>(Arrays.asList(selectedWebObjectsList.getItems())));
	}

}
