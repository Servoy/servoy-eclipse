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
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
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
	private final String type;

	public NewComponentAction(SolutionExplorerView viewer, Shell shell, String type, String text)
	{
		super();
		this.viewer = viewer;
		this.shell = shell;
		this.type = type;
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

		String componentName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
		if (componentName == null) return;
		while (!isNameValid(componentName, type + " name must start with a letter and must contain only alphanumeric characters"))
		{
			componentName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
			if (componentName == null) return;
		}
		componentName = componentName.trim();

		final String compName = componentName;
		final IResource packageRoot = (IResource)node.getRealObject();
		IWorkspaceRunnable createJob = new IWorkspaceRunnable()
		{

			@Override
			public void run(IProgressMonitor monitor) throws CoreException
			{
				createComponent(packageRoot, type, compName);
			}
		};

		RunInWorkspaceJob job = new RunInWorkspaceJob(createJob);
		job.setName("Create component");
		job.setRule(ServoyModel.getWorkspace().getRoot());
		job.setUser(false);
		job.schedule();
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
	 * @param elementType
	 * @param componentName
	 * @param displayName
	 */
	void createComponent(IResource packageRoot, final String elementType, final String componentName)
	{
		if (!(packageRoot instanceof IFolder) && !(packageRoot instanceof IProject)) return;
		IFolder folder = null;
		if (packageRoot instanceof IFolder) folder = ((IFolder)packageRoot).getFolder(componentName);
		if (packageRoot instanceof IProject) folder = ((IProject)packageRoot).getFolder(componentName);
		InputStream in = null;
		try
		{
			if (folder.exists())
			{
				final String folderName = folder.getName();
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(shell, getText(), elementType + " " + componentName + " already exists in package " + folderName);
					}
				});
				return;
			}

			String moduleName = packageRoot.getName() + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);

			folder.create(IResource.FORCE, true, new NullProgressMonitor());
			if (elementType.equals("Component"))
			{
				in = uiActivator.getBundle().getEntry("/component-templates/component.html").openStream();
				createFile(componentName + ".html", folder, in);
				in.close();
			}
			if (!elementType.equals("Layout"))
			{
				in = uiActivator.getBundle().getEntry("/component-templates/" + elementType.toLowerCase() + ".js").openStream();
				String text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentName);
				text = text.replaceAll("\\$\\{PACKAGENAME\\}", packageRoot.getName());
				createFile(componentName + ".js", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
				in = uiActivator.getBundle().getEntry("/component-templates/" + elementType.toLowerCase() + ".spec").openStream();
				text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentName);
				text = text.replaceAll("\\$\\{DASHEDNAME\\}", getDashedName(componentName));
				text = text.replaceAll("\\$\\{PACKAGENAME\\}", packageRoot.getName());
				createFile(componentName + ".spec", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
			}
			else
			{
				in = uiActivator.getBundle().getEntry("/component-templates/" + elementType.toLowerCase() + ".json").openStream();
				String text = IOUtils.toString(in, "UTF-8");
				createFile(componentName + ".json", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
				in = uiActivator.getBundle().getEntry("/component-templates/" + elementType.toLowerCase() + ".spec").openStream();
				text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentName);
				text = text.replaceAll("\\$\\{DASHEDNAME\\}", getDashedName(componentName));
				text = text.replaceAll("\\$\\{PACKAGENAME\\}", packageRoot.getName());
				createFile(componentName + ".spec", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
			}


			addToManifest(componentName, elementType, packageRoot);
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

	private String getDashedName(String name)
	{
		if (name != null && name.length() > 0)
		{
			StringBuilder dashedName = new StringBuilder();
			dashedName.append(name.charAt(0));
			for (int i = 1; i < name.length(); i++)
			{
				if (name.charAt(i) >= 'A' && name.charAt(i) <= 'Z')
				{
					dashedName.append('-');
				}
				dashedName.append(name.charAt(i));
			}
			return dashedName.toString();
		}

		return name;
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
		EditorUtil.openFileEditor(file);
	}

	/**
	 * @param componentName
	 * @param node
	 * @param pack
	 * @throws IOException
	 * @throws CoreException
	 * @throws FileNotFoundException
	 */
	private void addToManifest(String componentName, String elementType, IResource packageRoot) throws IOException, CoreException, FileNotFoundException
	{
		if (!(packageRoot instanceof IFolder) && !(packageRoot instanceof IProject)) return;
		IFile m = null;
		if (packageRoot instanceof IFolder) m = ((IFolder)packageRoot).getFile("META-INF/MANIFEST.MF");
		if (packageRoot instanceof IProject) m = ((IProject)packageRoot).getFile("META-INF/MANIFEST.MF");
		m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		Manifest manifest = new Manifest(m.getContents());
		Attributes attr = new Attributes();
		attr.put(new Attributes.Name("Web-" + elementType), "True");
		manifest.getEntries().put(componentName + "/" + componentName + ".spec", attr);

		OutputStream out = null;
		InputStream in = null;
		try
		{
			out = new ByteArrayOutputStream();
			manifest.write(out);
			in = new ByteArrayInputStream(out.toString().getBytes());
			m.setContents(in, IResource.FORCE, new NullProgressMonitor());
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			if (out != null) out.close();
			if (in != null) in.close();
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
		return (packageRoot instanceof IFolder || packageRoot instanceof IProject);
	}
}
