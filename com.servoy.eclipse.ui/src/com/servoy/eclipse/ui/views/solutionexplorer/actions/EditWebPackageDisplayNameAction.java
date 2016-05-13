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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.util.TextFieldDialog;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * @author emera
 */
public class EditWebPackageDisplayNameAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private final SolutionExplorerView viewer;

	public EditWebPackageDisplayNameAction(SolutionExplorerView viewer, Shell shell, String text)
	{
		super();
		this.viewer = viewer;
		this.shell = shell;
		setText(text);
		setToolTipText(text);
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
			state = node.getRealObject() instanceof IContainer &&
				(node.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE || node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE ||
					node.getType() == UserNodeType.SERVICES_NONPROJECT_PACKAGE || node.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE);
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		boolean componentsNotServices = (node.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE ||
			node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE);
		String packageName = SolutionExplorerTreeContentProvider.getPackageName(node); // we cannot rely on node name to be the display name directly cause if 'includeFromModules' option is enabled in SolEx the node name will be prefixed with the module name in some cases
		String name = (componentsNotServices ? WebComponentSpecProvider.getInstance().getPackageDisplayName(packageName)
			: WebServiceSpecProvider.getInstance().getPackageDisplayName(packageName));
		String newName = null;
		TextFieldDialog dialog = new TextFieldDialog(shell, getText(), null,
			"Please provide the new package display name for package with symbolic name '" + packageName + "'.", MessageDialog.NONE,
			new String[] { "OK", "Cancel" }, name);
		dialog.setBlockOnOpen(true);

		int code = dialog.open();
		newName = dialog.getSelectedText();
		if (code != 0 || name.equals(newName)) return;
		while (!isNameValid(node, newName, componentsNotServices))
		{
			code = dialog.open();
			newName = dialog.getSelectedText();
			if (code != 0 || name.equals(newName)) return;
		}
		updatePackageName(node, newName);
	}

	private boolean isNameValid(PlatformSimpleUserNode node, String packageDisplayName, boolean componentsNotServices)
	{
		if ("".equals(packageDisplayName))
		{
			MessageDialog.openError(shell, getText(), "The package display name cannot be empty");

			return false;
		}
		Map<String, PackageSpecification<WebObjectSpecification>> specifications = (componentsNotServices
			? WebComponentSpecProvider.getInstance().getWebComponentSpecifications() : WebServiceSpecProvider.getInstance().getWebServiceSpecifications());
		for (PackageSpecification<WebObjectSpecification> p : specifications.values())
		{
			if (p.getPackageDisplayname().equals(packageDisplayName))
			{
				MessageDialog.openError(shell, getText(),
					"A " + node.getName().toLowerCase() + " package with display name " + packageDisplayName + " already exists.");
				return false;
			}
		}
		return true;
	}

	private void updatePackageName(PlatformSimpleUserNode node, String newName)
	{
		OutputStream out = null;
		InputStream in = null;
		try
		{
			IFile m = ((IContainer)node.getRealObject()).getFile(new Path("META-INF/MANIFEST.MF"));
			m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			Manifest manifest = new Manifest(m.getContents());
			Attributes attr = manifest.getMainAttributes();
			attr.putValue("Bundle-Name", newName);

			out = new ByteArrayOutputStream();
			manifest.write(out);
			in = new ByteArrayInputStream(out.toString().getBytes());
			m.setContents(in, IResource.FORCE, new NullProgressMonitor());
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			if (out != null) try
			{
				out.close();
			}
			catch (IOException e)
			{
			}
			if (in != null) try
			{
				in.close();
			}
			catch (IOException e)
			{
			}
		}
	}
}
