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
import java.util.jar.Attributes.Name;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class EditWebPackageDetailsAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private final SolutionExplorerView viewer;

	public EditWebPackageDetailsAction(SolutionExplorerView viewer, Shell shell, String text)
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
			Object realObject = node.getRealObject();
			if (realObject instanceof IPackageReader)
			{
				realObject = SolutionExplorerTreeContentProvider.getResource((IPackageReader)realObject);
			}
			state = realObject instanceof IContainer && (node.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE ||
				node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE || node.getType() == UserNodeType.SERVICES_NONPROJECT_PACKAGE ||
				node.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE || node.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE);
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		IPackageReader packageReader = (IPackageReader)node.getRealObject();
		boolean componentsNotServices = (node.getType() == UserNodeType.COMPONENTS_NONPROJECT_PACKAGE ||
			node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE || node.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE);
		String packageName = packageReader.getPackageName();
		String displayName = packageReader.getPackageDisplayname();
		String version = packageReader.getVersion();
		String newName = null;
		String newVersion = null;

		EditDialog dialog = new EditDialog(packageName, displayName, version);
		int code = dialog.open();
		newName = dialog.getSelectedName();
		newVersion = dialog.getSelectedVersion();
		if (code != 0 || (displayName.equals(newName) && Utils.equalObjects(version, newVersion))) return;
		if (!displayName.equals(newName))
		{
			while (!isNameValid(node, newName, componentsNotServices))
			{
				code = dialog.open();
				newName = dialog.getSelectedName();
				if (code != 0 || (displayName.equals(newName) && Utils.equalObjects(version, newVersion))) return;
			}
		}

		updatePackageDetails((IContainer)SolutionExplorerTreeContentProvider.getResource(packageReader), newName, newVersion);
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

	private void updatePackageDetails(IContainer container, String newName, String newVersion)
	{
		OutputStream out = null;
		InputStream in = null;
		try
		{
			IFile m = container.getFile(new Path("META-INF/MANIFEST.MF"));
			m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			Manifest manifest = new Manifest(m.getContents());
			Attributes attr = manifest.getMainAttributes();
			attr.putValue("Bundle-Name", newName);
			if (newVersion != null && newVersion.trim().length() > 0) attr.putValue("Implementation-Version", newVersion);
			else attr.remove(new Name("Implementation-Version"));

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

	class EditDialog extends MessageDialog
	{
		private Text txtName;
		private final String defaultName;
		private String selectedName;

		private Text txtVersion;
		private final String defaultVersion;
		private String selectedVersion;


		EditDialog(String packageName, String defaultName, String defaultVersion)
		{
			super(shell, getText(), null, "Please provide the new package display name and version for package with symbolic name '" + packageName + "'.",
				MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0);
			this.defaultName = defaultName;
			this.defaultVersion = defaultVersion;
			setBlockOnOpen(true);
		}


		@Override
		protected Control createCustomArea(Composite parent)
		{
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalAlignment = GridData.FILL_HORIZONTAL;
			data.grabExcessHorizontalSpace = true;
			data.horizontalSpan = 2;

			Composite container = new Composite(parent, SWT.FILL);
			GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.horizontalSpacing = 10;

			container.setLayout(layout);
			container.setLayoutData(data);

			data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			data.minimumWidth = 250;

			Label nameLabel = new Label(container, SWT.NONE);
			nameLabel.setText("Name");
			txtName = new Text(container, SWT.BORDER);
			if (defaultName != null) txtName.setText(defaultName);
			txtName.setLayoutData(data);

			Label versionLabel = new Label(container, SWT.NONE);
			versionLabel.setText("Version");
			txtVersion = new Text(container, SWT.BORDER);
			if (defaultVersion != null) txtVersion.setText(defaultVersion);
			txtVersion.setLayoutData(data);

			return container;
		}

		@Override
		public boolean close()
		{
			selectedName = (txtName != null ? txtName.getText() : null);
			selectedVersion = (txtVersion != null ? txtVersion.getText() : null);
			return super.close();
		}

		public String getSelectedName()
		{
			return selectedName;
		}

		public String getSelectedVersion()
		{
			return selectedVersion;
		}
	}
}
