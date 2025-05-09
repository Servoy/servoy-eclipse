/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.NewPackageProjectWizard;

/**
 * @author jcompagner@servoy.com
 *
 */
public class WebPackageUpgradeAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;
	private final Shell shell;
	private final String packageType;
	private IProject project;
	private IPackageReader reader;

	/**
	 * @param text - the label of the menu entry
	 */
	public WebPackageUpgradeAction(SolutionExplorerView solutionExplorerView, Shell shell, String text, String packageType)
	{
		this.viewer = solutionExplorerView;
		this.shell = shell;
		this.packageType = packageType;
		setText(text);
	}

	@Override
	public void run()
	{
		// first copy the standard TiNG files
		NewPackageProjectWizard.copyAndRenameFiles(project);

		// adjust the manifest to have the right TiNG entries
		if (IPackageReader.WEB_COMPONENT.equals(packageType))
		{
			try
			{
				Manifest manifest = reader.getManifest();
				manifest.getMainAttributes().put(new Attributes.Name("NPM-PackageName"), project.getName());
				manifest.getMainAttributes().put(new Attributes.Name("NG2-Module"), project.getName() + "Module");
				manifest.getMainAttributes().put(new Attributes.Name("Entry-Point"), "dist");
				File manifestFile = new File(reader.getResource(), "META-INF/MANIFEST.MF");
				try (FileOutputStream fos = new FileOutputStream(manifestFile))
				{
					manifest.write(fos);
				}
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}

			// create all the components stubs that are in the package and try to copy over the current ng1 template
			final File projectRoot = new File(project.getLocation().toOSString());
			final File srcRoot = new File(projectRoot, "project/src/");
			Collection<String> components = WebComponentSpecProvider.getSpecProviderState().getWebObjectsInPackage(reader.getPackageName());
			components.forEach(component -> {
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(component);
				String definition = spec.getDefinition();
				if (definition != null)
				{
					int index = definition.lastIndexOf('/');
					String name = definition.substring(index + 1, definition.length() - 3);
					NewWebObjectAction.addNG2Code(name, packageType, project);
					File ng1Htm = new File(projectRoot, name + '/' + name + ".html");
					File ng2Html = new File(srcRoot, name + '/' + name + ".html");
					try
					{
						FileUtils.copyFile(ng1Htm, ng2Html);
					}
					catch (IOException e)
					{
						// just ignore
					}
				}
			});
			try
			{
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}

	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		if (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			Object realObject = node.getRealObject();
			if (realObject instanceof IPackageReader)
			{
				reader = (IPackageReader)realObject;
				realObject = SolutionExplorerTreeContentProvider.getResource((IPackageReader)realObject);
			}
			state = realObject instanceof IProject && (node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE);
			if (state)
			{
				project = (IProject)realObject;
				state = !project.getFile("angular.json").exists();
			}
		}
		setEnabled(state);
	}

}
