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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Create a new component/service package.
 * @author emera
 */
public class NewComponentPackageAction extends Action
{

	private final SolutionExplorerView viewer;
	private final Shell shell;

	private String packageName;
	private String packageDisplayName;
	private String componentName;

	/**
	 * @param solutionExplorerView
	 * @param shell
	 * @param text
	 */
	public NewComponentPackageAction(SolutionExplorerView viewer, Shell shell, String text)
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
		final PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		final String type = UserNodeType.COMPONENTS == node.getType() ? "Component" : "Service";
		IFolder folder = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject().getFolder(
			(String)node.getRealObject());

		MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), getText(), null, null, MessageDialog.CONFIRM,
			new String[] { "OK", "Cancel" }, 0)
		{
			private Text packName;
			private Text packDisplay;
			private Text compName;


			@Override
			protected Control createMessageArea(Composite parent)
			{
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				data.horizontalAlignment = GridData.FILL_HORIZONTAL;
				data.grabExcessHorizontalSpace = true;
				data.horizontalSpan = 2;

				Composite container = new Composite(parent, SWT.FILL);
				GridLayout layout = new GridLayout(2, true);
				layout.marginWidth = 0;

				container.setLayout(layout);
				container.setLayoutData(data);

				data = new GridData();
				data.horizontalAlignment = SWT.FILL;
				data.grabExcessHorizontalSpace = true;

				Label packageLabel = new Label(container, SWT.NONE);
				packageLabel.setText("Package name");
				packName = new Text(container, SWT.BORDER);
				if (packageName == null && node.getType() == UserNodeType.SERVICES) packName.setText("services");
				if (packageName != null) packName.setText(packageName);
				packName.setLayoutData(data);

				Label packageDisplayLabel = new Label(container, SWT.NONE);
				packageDisplayLabel.setText("Package display name");
				packDisplay = new Text(container, SWT.BORDER);
				if (packageDisplayName != null) packDisplay.setText(packageDisplayName);
				packDisplay.setLayoutData(data);

				Label componentLabel = new Label(container, SWT.NONE);
				componentLabel.setText(type + " name");
				compName = new Text(container, SWT.BORDER);
				if (componentName != null) compName.setText(componentName);
				compName.setLayoutData(data);

				return container;
			}

			@Override
			public boolean close()
			{
				packageName = (packName != null ? packName.getText() : null);
				packageDisplayName = (packDisplay != null ? packDisplay.getText() : null);
				componentName = (compName != null ? compName.getText() : null);
				return super.close();
			}
		};
		int code = dialog.open();
		if (code != 0) return;
		while (checkIfEmpty(packageName, "Package name was not provided.") ||
			checkIfEmpty(componentName, "The name of the first component was not provided.") || !isNameValid(node) ||
			!isNameValid(packageName, "Package name must start with a letter and must contain only alphanumeric characters") ||
			!isNameValid(componentName, type + " name must start with a letter and must contain only alphanumeric characters"))
		{
			code = dialog.open();
			if (code != 0) return;
		}
		if (checkIfEmpty(packageDisplayName, null))
		{
			packageDisplayName = packageName;
		}

		try
		{

			if (!folder.exists()) folder.create(IResource.FORCE, true, new NullProgressMonitor());

			IFolder pack = folder.getFolder(packageName);
			if (pack.exists())
			{
				MessageDialog.openError(shell, getText(), "Package " + packageName + " already exists in " + node.getName());
				return;
			}

			pack.create(IResource.FORCE, true, new NullProgressMonitor());
			createManifest(pack);

			NewComponentAction newComponent = new NewComponentAction(viewer, shell, "");
			newComponent.createComponent(pack, type, componentName);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not create component/service package.", e);
		}
	}

	private boolean isNameValid(PlatformSimpleUserNode node)
	{
		if (node.getType() == UserNodeType.SERVICES)
		{
			if (!packageName.toLowerCase().endsWith("services"))
			{
				MessageDialog.openError(shell, getText(), "Service package names must end with \"services\"");
				return false;
			}
		}
		if (node.children != null)
		{
			for (SimpleUserNode n : node.children)
			{
				if (n.getName().equals(packageDisplayName))
				{
					MessageDialog.openError(shell, getText(), "A package with display name " + packageDisplayName + " already exists in " + node.getName());
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkIfEmpty(String value, String message)
	{
		if (value == null || value.trim().equals(""))
		{
			if (message != null)
			{
				MessageDialog.openError(shell, getText(), message);
			}
			return true;
		}
		return false;
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
	 * @param pack
	 * @throws IOException
	 * @throws CoreException
	 */
	private void createManifest(IFolder pack) throws CoreException, IOException
	{
		OutputStream out = null;
		try
		{
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			manifest.getMainAttributes().put(new Attributes.Name("Bundle-Name"), packageDisplayName);

			IFolder metainf = pack.getFolder("META-INF");
			metainf.create(true, true, new NullProgressMonitor());
			IFile m = metainf.getFile("MANIFEST.MF");
			m.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
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
}
