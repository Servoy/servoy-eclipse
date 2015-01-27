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
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.warexporter.export.ExportWarModel;

/**
 * Shows the user the list of needed services and allows him to select more to export.
 * @author emera
 */
public class ServicesSelectionPage extends AbstractComponentsSelectionPage
{
	protected ServicesSelectionPage(ExportWarModel exportModel, String pageName, String title, String description, IWizardPage nextPage)
	{
		super(exportModel, pageName, title, description, nextPage, "service");
		componentsUsed = exportModel.getUsedServices();
		selectedComponents = new TreeSet<String>(componentsUsed);
		if (exportModel.getExportedServices() == null || exportModel.getExportedServices().containsAll(componentsUsed) &&
			componentsUsed.containsAll(exportModel.getExportedServices())) return;
		WebServiceSpecProvider provider = WebServiceSpecProvider.getInstance();
		for (String service : exportModel.getExportedServices())
		{
			if (provider.getWebServiceSpecification(service) != null) selectedComponents.add(service);
		}
	}

	@Override
	protected Set<String> getAvailableItems()
	{
		Set<String> availableComponents = new TreeSet<String>();
		for (WebComponentSpecification spec : WebServiceSpecProvider.getInstance().getWebServiceSpecifications())
		{
			if (!selectedComponents.contains(spec.getName())) availableComponents.add(spec.getName());
		}
		return availableComponents;
	}

	@Override
	public IWizardPage getNextPage()
	{
		exportModel.setExportedServices(new TreeSet<String>(Arrays.asList(selectedComponentsList.getItems())));
		if (nextPage != null) return nextPage;
		return super.getNextPage();
	}
}
