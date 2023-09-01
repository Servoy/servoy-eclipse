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
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Create a new component/service package.
 * @author emera
 */
public class NewResourcesComponentsOrServicesPackageAction extends Action
{

	private final SolutionExplorerView viewer;
	private final Shell shell;

	private String packageName;
	private String packageDisplayName;
	private String componentOrServiceName;
	private final String packageType;

	public NewResourcesComponentsOrServicesPackageAction(SolutionExplorerView viewer, Shell shell, String text, String type)
	{
		super();
		this.viewer = viewer;
		this.shell = shell;
		packageType = type;
		setText(text);
		setToolTipText(text);
	}

	@Override
	public void run()
	{
		final SimpleUserNode node = viewer.getSelectedTreeNode();
		final String type = packageType.replace("Web-", "");
		final IFolder folder = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject().getFolder(
			(String)node.getRealObject());

		MessageDialog dialog = new MessageDialog(UIUtils.getActiveShell(), getText(), null, null, MessageDialog.CONFIRM,
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
				if (packageName == null && node.getType() == UserNodeType.SERVICES_FROM_RESOURCES) packName.setText("services");
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
				if (componentOrServiceName != null) compName.setText(componentOrServiceName);
				compName.setLayoutData(data);

				return container;
			}

			@Override
			public boolean close()
			{
				packageName = (packName != null ? packName.getText() : null);
				packageDisplayName = (packDisplay != null ? packDisplay.getText() : null);
				componentOrServiceName = ((compName.getText() != null && compName.getText().length() > 0) ? compName.getText() : null);
				return super.close();
			}
		};
		int code = dialog.open();
		if (code != 0) return;
		while (checkIfEmpty(packageName, "Package name was not provided.") ||
			/* checkIfEmpty(componentName, "The name of the first component was not provided.") || */ !isNameValid(node) ||
			!isNameValid(packageName, "^[a-z][0-9a-z]*$", "Package name must start with a letter and must contain only lowercase letters or numbers") ||
			(componentOrServiceName != null && !isNameValid(componentOrServiceName, "^[a-zA-Z][0-9a-zA-Z]*$",
				type + " name must start with a letter and must contain only alphanumeric characters")))
		{
			code = dialog.open();
			if (code != 0) return;
		}
		if (checkIfEmpty(packageDisplayName, null))
		{
			packageDisplayName = packageName;
		}

		IWorkspaceRunnable createJob = new IWorkspaceRunnable()
		{

			@Override
			public void run(IProgressMonitor monitor) throws CoreException
			{
				try
				{

					if (!folder.exists()) folder.create(IResource.FORCE, true, monitor);

					IFolder pack = folder.getFolder(packageName);
					if (pack.exists())
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(shell, getText(), "Package " + packageName + " already exists in " + node.getName());
							}
						});
						return;
					}

					pack.create(IResource.FORCE, true, monitor);
					createManifest(pack, packageDisplayName, packageName, "1.0.0", packageType);

					if (componentOrServiceName != null)
					{
						NewWebObjectAction newComponent = new NewWebObjectAction(viewer, shell, "Component", "");
						newComponent.createComponentOrService(pack, type, componentOrServiceName, null);
					}

					if (viewer != null)
					{
						if (componentOrServiceName == null) viewer.getSolExNavigator().revealWhenAvailable(node, new String[] { packageDisplayName }, true);
						else viewer.getSolExNavigator().revealWhenAvailable(node, new String[] { packageDisplayName, componentOrServiceName }, true); // if in the future this action allows specifying component or service display name, that one should be used here in the array instead
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("Could not create component/service package.", e);
				}
			}
		};

		RunInWorkspaceJob job = new RunInWorkspaceJob(createJob);
		job.setName("Create component");
		job.setRule(ServoyModel.getWorkspace().getRoot());
		job.setUser(false);
		job.schedule();
	}

	private boolean isNameValid(SimpleUserNode node)
	{
		List<PackageSpecification< ? extends WebObjectSpecification>> allPackageSpecs = new ArrayList<>(
			WebServiceSpecProvider.getSpecProviderState().getWebObjectSpecifications().values());
		SpecProviderState compspecProviderState = WebComponentSpecProvider.getSpecProviderState();
		allPackageSpecs.addAll(compspecProviderState.getWebObjectSpecifications().values());
		allPackageSpecs.addAll(compspecProviderState.getLayoutSpecifications().values());

		for (PackageSpecification< ? extends WebObjectSpecification> p : allPackageSpecs)
		{
			if (p.getPackageName().equals(packageName))
			{
				MessageDialog.openError(shell, getText(), "A " + node.getName().toLowerCase() + " package with name " + packageName + " already exists.");
				return false;
			}
			if (p.getPackageDisplayname().equals(packageDisplayName))
			{
				MessageDialog.openError(shell, getText(),
					"A " + node.getName().toLowerCase() + " package with display name " + packageDisplayName + " already exists.");
				return false;
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

	private boolean isNameValid(String value, String regex, String message)
	{
		if (!value.matches(regex))
		{
			if (message != null)
			{
				MessageDialog.openError(shell, getText(), message);
			}
			return false;
		}
		return true;
	}

	public static void createManifest(IContainer pack, String packageDisplayName, String packageName, String version, String packageType)
		throws CoreException, IOException
	{
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(new Attributes.Name("Bundle-Name"), isEmpty(packageDisplayName) ? packageName : packageDisplayName);
		manifest.getMainAttributes().put(new Attributes.Name("Bundle-SymbolicName"), packageName);
		manifest.getMainAttributes().put(new Attributes.Name(Package.PACKAGE_TYPE), packageType);
		manifest.getMainAttributes().put(new Attributes.Name("Bundle-Version"), isEmpty(version) ? "1.0.0" : version);

		if (IPackageReader.WEB_COMPONENT.equals(packageType))
		{
			manifest.getMainAttributes().put(new Attributes.Name("NPM-PackageName"), packageName);
			manifest.getMainAttributes().put(new Attributes.Name("NG2-Module"), packageName + "Module");
			manifest.getMainAttributes().put(new Attributes.Name("Entry-Point"), "dist");
		}

		IFolder metainf = pack.getFolder(new Path("META-INF"));
		metainf.create(true, true, new NullProgressMonitor());
		IFile m = metainf.getFile("MANIFEST.MF");
		m.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());

		try (OutputStream out = new FileOutputStream(new File(m.getLocationURI()), false))
		{
			manifest.write(out);
		}
		m.refreshLocal(0, new NullProgressMonitor());
	}

	/**
	 * @param packageDisplayName2
	 * @return
	 */
	private static boolean isEmpty(String str)
	{
		return str == null || str.trim().length() == 0;
	}

}
