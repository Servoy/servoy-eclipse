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

import org.eclipse.ui.PlatformUI;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.server.ngclient.utils.NGUtils;

/**
 * Shows the user the list of needed services and allows him to select more to export.
 * @author emera
 */
public class ServicesSelectionPage extends AbstractWebObjectSelectionPage
{
	private final SpecProviderState servicesSpecProviderState;

	protected ServicesSelectionPage(ExportWarModel exportModel, SpecProviderState servicesSpecProviderState, String pageName, String title, String description)
	{
		super(exportModel, pageName, title, description, "service");
		this.servicesSpecProviderState = servicesSpecProviderState;
		joinWithLastUsed();
	}

	@Override
	protected Set<String> getWebObjectsExplicitlyUsedBySolution()
	{
		return exportModel.getServicesUsedExplicitlyBySolution();
	}

	@Override
	protected void joinWithLastUsed()
	{
		if (exportModel.getServicesToExportWithoutUnderTheHoodOnes() == null ||
			webObjectsUsedExplicitlyBySolution.containsAll(exportModel.getServicesToExportWithoutUnderTheHoodOnes())) return;
		for (String service : exportModel.getServicesToExportWithoutUnderTheHoodOnes())
		{
			if (servicesSpecProviderState.getWebObjectSpecification(service) != null) selectedWebObjectsForListCreation.add(service);
		}
	}

	@Override
	protected Set<String> getAvailableItems(boolean alreadyPickedAtListCreationShouldBeInThere)
	{
		Set<String> availableServices = new TreeSet<String>();
		for (WebObjectSpecification spec : NGUtils.getAllWebServiceSpecificationsThatCanBeUncheckedAtWarExport(servicesSpecProviderState))
		{
			if (!exportModel.getPreferencesExcludedDefaultServicePackages().contains(spec.getPackageName()) &&
				!exportModel.getServicesNeededUnderTheHood().contains(spec.getName()) && // normally under the hood services would not be returned anyway by NGUtils.getAllWebServiceSpecificationsThatCanBeUncheckedAtWarExport
				(alreadyPickedAtListCreationShouldBeInThere || !selectedWebObjectsForListCreation.contains(spec.getName())))
			{
				availableServices.add(spec.getName());
			}
		}
		return availableServices;
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_services");
	}

	@Override
	public void storeInput()
	{
		exportModel.setServicesToExportWithoutUnderTheHoodOnes(new TreeSet<String>(Arrays.asList(selectedWebObjectsList.getItems())));
	}

}
