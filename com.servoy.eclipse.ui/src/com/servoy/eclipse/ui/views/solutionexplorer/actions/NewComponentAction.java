/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Create a new component/service in the selected component package.
 * @author emera
 */
public class NewComponentAction extends Action
{

	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();
	private final Shell shell;
	private final SolutionExplorerView viewer;

	public NewComponentAction(SolutionExplorerView viewer, Shell shell, String text)
	{
		super();
		this.viewer = viewer;
		this.shell = shell;
		setText(text);
		setToolTipText(text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		String type = UserNodeType.COMPONENTS_PACKAGE == node.getType() ? "Component" : "Service";

		String componentName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
		if (componentName == null) return;
		while (!isNameValid(componentName, type + " name must start with a letter and must contain only alphanumeric characters"))
		{
			componentName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
			if (componentName == null) return;
		}
		componentName = componentName.trim();

		IResource packageRoot = (IResource)node.getRealObject();
		createComponent(packageRoot, type, componentName);
	}

	private boolean isNameValid(String value, String message)
	{
		if (!value.matches("^[a-zA-Z][0-9a-zA-Z]*$"))
		{
			if (message != null)
			{
				MessageDialog.openError(shell, getText(), message);
			}
			return false;
		}
		return true;
	}

	/**
	 * @param folder
	 * @param type
	 * @param componentName
	 * @param displayName
	 */
	void createComponent(IResource packageRoot, String type, String componentName)
	{
		if (packageRoot instanceof IFolder)
		{
			IFolder pack = (IFolder)packageRoot;
			InputStream in = null;
			try
			{
				IFolder folder = pack.getFolder(componentName);
				if (folder.exists())
				{
					MessageDialog.openError(shell, getText(), type + " " + componentName + " already exists in package " + folder.getName());
					return;
				}

				String moduleName = pack.getName() + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);

				folder.create(IResource.FORCE, true, new NullProgressMonitor());
				if (type.equals("Component"))
				{
					createFile(componentName + ".html", folder, uiActivator.getBundle().getEntry("/component-templates/component.html").openStream());
				}
				in = uiActivator.getBundle().getEntry("/component-templates/" + type.toLowerCase() + ".js").openStream();
				String text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentName);
				text = text.replaceAll("\\$\\{PACKAGENAME\\}", pack.getName());
				createFile(componentName + ".js", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));

				in = uiActivator.getBundle().getEntry("/component-templates/" + type.toLowerCase() + ".spec").openStream();
				text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentName);
				text = text.replaceAll("\\$\\{PACKAGENAME\\}", pack.getName());
				createFile(componentName + ".spec", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));

				addToManifest(componentName, type, pack);
			}
			catch (IOException e)
			{
				ServoyLog.logError("Cannot create component.", e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Cannot create component.", e);
			}
			finally
			{
				if (in != null) try
				{
					in.close();
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	/**
	 * @param componentName
	 * @param folder
	 * @param in
	 * @throws CoreException
	 */
	private void createFile(String componentName, IFolder folder, InputStream in) throws CoreException
	{
		IFile file = folder.getFile(componentName);
		file.create(in != null ? in : new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		EditorUtil.openComponentFileEditor(file);
	}

	/**
	 * @param componentName
	 * @param node
	 * @param pack
	 * @throws IOException
	 * @throws CoreException
	 * @throws FileNotFoundException
	 */
	private void addToManifest(String componentName, String type, IFolder pack) throws IOException, CoreException, FileNotFoundException
	{
		IFile m = pack.getFile("META-INF/MANIFEST.MF");
		m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		Manifest manifest = new Manifest(m.getContents());
		Attributes attr = new Attributes();
		if (type.equals("Component"))
		{
			attr.put(new Attributes.Name("Web-Component"), "True");
		}
		else
		{
			attr.put(new Attributes.Name("Web-Service"), "True");
		}
		manifest.getEntries().put(componentName + "/" + componentName + ".spec", attr);

		OutputStream out = null;
		try
		{
			out = new FileOutputStream(new File(m.getLocationURI()), false);
			manifest.write(out);
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			if (out != null) out.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		IResource packageRoot = (IResource)node.getRealObject();
		return (packageRoot instanceof IFolder);
	}
}
