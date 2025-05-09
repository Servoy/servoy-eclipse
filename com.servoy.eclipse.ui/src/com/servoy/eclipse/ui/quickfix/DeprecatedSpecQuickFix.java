/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ui.quickfix;

import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.IAutomaticImportWPMPackages;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.UUID;

/**
 * Quick fix to change deprecated component/layout types to their replacements if possible.
 * @author emera
 */
public class DeprecatedSpecQuickFix extends WorkbenchMarkerResolution
{
	private final IMarker marker;

	public DeprecatedSpecQuickFix(final IMarker marker)
	{
		this.marker = marker;
	}

	@Override
	public String getLabel()
	{
		return "Change type of deprecated component to the replacement '" + marker.getAttribute("replacement", "") +
			"' (might install packages automatically).";
	}

	@Override
	public void run(IMarker mk)
	{
		String replacement = mk.getAttribute("replacement", "");
		try
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(mk.getAttribute("solutionName", ""));
			if (servoyProject.isSolutionLoaded())
			{
				IPersist persist = AbstractRepository.searchPersist(servoyProject.getEditingSolution(), UUID.fromString(mk.getAttribute("uuid", "")));
				if (persist instanceof WebComponent)
				{
					WebComponent webComponent = (WebComponent)persist;

					WebObjectSpecification oldSpec = WebComponentSpecProvider.getSpecProviderState()
						.getWebObjectSpecification(webComponent.getTypeName());
					WebObjectSpecification newSpec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(replacement);

					if (oldSpec != null && newSpec != null)
					{
						Collection<PropertyDescription> deprecated = oldSpec.getAllPropertiesNames()//
							.stream().map((String propname) -> oldSpec.getProperty(propname))//
							.filter((PropertyDescription pd) -> pd != null && pd.getTag("replacement") != null &&
								newSpec.getProperty((String)pd.getTag("replacement")) != null)//
							.collect(Collectors.toCollection(ArrayList::new));

						Map<String, Object> propertyValues = !deprecated.isEmpty()
							? deprecated.stream().filter(pd -> webComponent.getProperty(pd.getName()) != null)
								.collect(Collectors.toMap(pd -> (String)pd.getTag("replacement"), pd -> webComponent.getProperty(pd.getName())))
							: Collections.EMPTY_MAP;

						for (PropertyDescription property : deprecated)
						{
							webComponent.clearProperty(property.getName());
						}

						webComponent.setTypeName(replacement);

						//copy the values for the replaced properties
						for (String propertyName : propertyValues.keySet())
						{
							webComponent.setProperty(propertyName, propertyValues.get(propertyName));
						}
					}
					else if (newSpec != null)
					{
						webComponent.setTypeName(replacement);
					}

				}
				if (persist instanceof LayoutContainer)
				{
					String[] packLayoutNames = replacement.split("-");
					((LayoutContainer)persist).setPackageName(packLayoutNames[0]);
					((LayoutContainer)persist).setSpecName(packLayoutNames[1]);
				}
				servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, false, false);
			}
		}
		catch (RepositoryException e)
		{
			String message = "Could not replace deprecated component type with " + replacement;
			ServoyLog.logError(message, e);
			Display.getDefault().asyncExec(() -> {
				MessageDialog.openError(UIUtils.getActiveShell(), IMarker.MESSAGE, message);
			});
		}
	}


	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor)
	{
		//import missing package if it is the case
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(marker.getAttribute("solutionName", null));
		if (servoyProject.isSolutionLoaded())
		{
			IPersist persist = AbstractRepository.searchPersist(servoyProject.getEditingSolution(), UUID.fromString(marker.getAttribute("uuid", "")));
			String replacement = marker.getAttribute("replacement", "");
			String packageName = replacement.split("-")[0];
			if (persist instanceof WebComponent && !WebComponentSpecProvider.getSpecProviderState().getPackageNames().contains(packageName) ||
				persist instanceof LayoutContainer && !WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().containsKey(packageName))
			{
				List<IAutomaticImportWPMPackages> defaultImports = ModelUtils.getExtensions(IAutomaticImportWPMPackages.EXTENSION_ID);
				if (defaultImports != null && defaultImports.size() > 0)
				{
					defaultImports.get(0).importPackage(packageName);
				}
			}

			String message = null;
			String[] packLayoutNames = replacement.split("-");
			if (packLayoutNames.length != 2)
			{
				message = "The replacement '" + replacement + "' is incorrectly specified. It should be of the form packagename-layoutname";
			}
			if (persist instanceof WebComponent)
			{
				if (!WebComponentSpecProvider.getSpecProviderState().getPackageNames().contains(packageName))
				{
					message = "The package with name '" + packageName + "' cannot be found. You might need to install it manually.";
				}
				else if (WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(replacement) == null)
				{
					message = "The component with name '" + packLayoutNames[1] + "' does not exist in package '" + packageName + "'";
				}
			}
			if (persist instanceof LayoutContainer)
			{

				if (!WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().containsKey(packageName))
				{
					message = "The package with name '" + packageName + "' cannot be found. You might need to install it manually.";
				}
				else if (!WebComponentSpecProvider.getSpecProviderState().getLayoutsInPackage(packLayoutNames[0]).contains(packLayoutNames[1]))
				{
					message = "The layout with name '" + packLayoutNames[1] + "' does not exist in package '" + packageName + "'";
				}
			}

			if (message != null)
			{
				String mess = "Could not replace deprecated component type with '" + replacement + "'." + message;
				ServoyLog.logError(new Exception(mess));
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(UIUtils.getActiveShell(), "Cannot quickfix deprecated marker", mess);
				});
				return;
			}
		}

		super.run(markers, monitor);
	}

	@Override
	public String getDescription()
	{
		return null;
	}

	@Override
	public Image getImage()
	{
		return null;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers)
	{
		try
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
				marker.getAttribute("solutionName", null));
			if (servoyProject != null)
			{
				return stream(servoyProject.getProject().findMarkers(ServoyBuilder.DEPRECATED_SPEC, false, IResource.DEPTH_INFINITE)).filter(
					mk -> mk.getId() != marker.getId() && mk.getAttribute("replacement", null) != null &&
						mk.getAttribute("replacement", "").equals(marker.getAttribute("replacement", "")))
					.toArray(IMarker[]::new);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Could not find all deprecated spec markers.", e);
		}
		return markers;
	}

}
