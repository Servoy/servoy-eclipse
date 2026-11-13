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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
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
import org.osgi.framework.Bundle;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Utils;

/**
 * Create a new component/service in the selected component package.
 * @author emera
 */
public class NewWebObjectAction extends Action
{
	private final Shell shell;
	private final SolutionExplorerView viewer;
	private final String type;

	private static final String COMPONENT_NAME_TAG = "##componentname##";
	private static final String COMPONENT_DASH_NAME_TAG = "##componentdashname##";
	private static final String COMPONENT_CLASSNAME_TAG = "##componentclassname##";
	private static final String PACKAGE_NAME_TAG = "##packagename##";

	public NewWebObjectAction(SolutionExplorerView viewer, Shell shell, String type, String text)
	{
		super();
		this.viewer = viewer;
		this.shell = shell;
		this.type = type;
		setText(text);
		setToolTipText(text);
	}

	@Override
	public void run()
	{
		final SimpleUserNode node = viewer.getSelectedTreeNode();

		String componentOrServiceName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
		if (componentOrServiceName == null) return;
		while (!isNameValid(componentOrServiceName, type + " name must start with a letter and must contain only alphanumeric characters"))
		{
			componentOrServiceName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
			if (componentOrServiceName == null) return;
		}
		componentOrServiceName = componentOrServiceName.trim();

		final String compOrSrvName = componentOrServiceName;
		final IResource packageRoot = SolutionExplorerTreeContentProvider.getResource((IPackageReader)node.getRealObject());
		IWorkspaceRunnable createJob = new IWorkspaceRunnable()
		{

			@Override
			public void run(IProgressMonitor monitor) throws CoreException
			{
				createComponentOrService(packageRoot, type, compOrSrvName, node);
			}
		};

		RunInWorkspaceJob job = new RunInWorkspaceJob(createJob);
		job.setName("Create component or service");
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

	public void createComponentOrService(IResource packageRoot, final String elementType, final String componentOrServiceName, SimpleUserNode parentNode)
	{
		if (!(packageRoot instanceof IFolder) && !(packageRoot instanceof IProject)) return;
		IFolder folder = null;
		if (packageRoot instanceof IFolder) folder = ((IFolder)packageRoot).getFolder(componentOrServiceName);
		if (packageRoot instanceof IProject) folder = ((IProject)packageRoot).getFolder(componentOrServiceName);
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
						MessageDialog.openError(shell, getText(), elementType + " " + componentOrServiceName + " already exists in package " + folderName);
					}
				});
				return;
			}


			// IMPORTANT: this should always stay in sync with WebObjectSpecification.scriptifyNameIfNeeded()
			String moduleName = packageRoot.getName() + componentOrServiceName.substring(0, 1).toUpperCase() + componentOrServiceName.substring(1);

			String fullName = "";

			folder.create(IResource.FORCE, true, new NullProgressMonitor());
			Bundle bundle = com.servoy.eclipse.ngclient.ui.Activator.getInstance().getBundle();
			//			if (elementType.equals(IPackageReader.WEB_COMPONENT))
			//			{
			//				in = bundle.getEntry("/component-templates/web-component.html").openStream();
			//				createFile(componentOrServiceName + ".html", folder, in);
			//				in.close();
			//			}
			if (!elementType.equals(IPackageReader.WEB_LAYOUT))
			{
				if (elementType.equals(IPackageReader.WEB_SERVICE))
				{
					fullName = componentOrServiceName;
				}
				else
				{
					fullName = packageRoot.getName() + "-" + getDashedName(componentOrServiceName);
				}
				//				in = bundle.getEntry("/component-templates/" + elementType.toLowerCase() + ".js")
				//					.openStream();
				//				String text = IOUtils.toString(in, "UTF-8");
				//				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				//				text = text.replaceAll("\\$\\{NAME\\}", componentOrServiceName);
				//				text = text.replaceAll("\\$\\{PACKAGENAME\\}", packageRoot.getName());
				//				createFile(componentOrServiceName + ".js", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				//				in.close();
				in = bundle.getEntry("/component-templates/" + elementType.toLowerCase() + ".spec")
					.openStream();
				String text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentOrServiceName); // IMPORTANT service name from service.spec template should always be built of packagename dash name so that WebObjectSpecification.scriptifyNameIfNeeded() can get the "moduleName" or scripting name out of that
				text = text.replaceAll("\\$\\{FULLNAME\\}", fullName);
				text = text.replaceAll("\\$\\{PACKAGENAME\\}", packageRoot.getName());
				createFile(componentOrServiceName + ".spec", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
			}
			else
			{
				fullName = getDashedName(componentOrServiceName);
				in = bundle.getEntry("/component-templates/" + elementType.toLowerCase() + ".json")
					.openStream();
				String text = IOUtils.toString(in, "UTF-8");
				createFile(componentOrServiceName + ".json", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
				in = bundle.getEntry("/component-templates/" + elementType.toLowerCase() + ".spec")
					.openStream();
				text = IOUtils.toString(in, "UTF-8");
				text = text.replaceAll("\\$\\{MODULENAME\\}", moduleName);
				text = text.replaceAll("\\$\\{NAME\\}", componentOrServiceName);
				text = text.replaceAll("\\$\\{FULLNAME\\}", fullName);
				createFile(componentOrServiceName + ".spec", folder, new ByteArrayInputStream(text.getBytes("UTF-8")));
				in.close();
			}


			addToManifest(componentOrServiceName, elementType, packageRoot);

			addNG2Code(componentOrServiceName, elementType, packageRoot);
			if (viewer != null && parentNode != null)
			{
				viewer.getSolExNavigator().revealWhenAvailable(parentNode, new String[] { componentOrServiceName }, true); // if in the future this action allows specifying display name, that one should be used here in the array instead
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot create component or service.", e);
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Cannot create component or service.", e);
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

	public static String getDashedName(String name)
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

	private void createFile(String componentOrServiceName, IFolder folder, InputStream in) throws CoreException
	{
		IFile file = folder.getFile(componentOrServiceName);
		file.create(in != null ? in : new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
		EditorUtil.openFileEditor(file);
	}

	public static void addNG2Code(String componentOrServiceName, String elementType, IResource packageRoot)
	{
		try
		{
			String moduleName = packageRoot.getProject().getName();
			File compDirectory = new File(packageRoot.getLocation().toOSString(), "project/src/");
			String className = componentOrServiceName.substring(0, 1).toUpperCase() + componentOrServiceName.substring(1);
			String charset = "UTF-8";
			Bundle bundle = com.servoy.eclipse.ngclient.ui.Activator.getInstance().getBundle();

			// Decide at runtime whether this is a modern standalone package (NG2-Components) or a legacy
			// module-based package (NG2-Module). Modern creation always produces NG2-Components, so the
			// standalone path is the common one; the legacy branch is a defensive back-compat path.
			IFile moduleFile = getModuleFile(packageRoot, moduleName);
			boolean legacyModulePackage = isLegacyModulePackage(packageRoot) && moduleFile != null && moduleFile.exists();

			if (elementType.equals(IPackageReader.WEB_COMPONENT))
			{
				compDirectory = new File(compDirectory, componentOrServiceName);
				FileUtils.forceMkdir(compDirectory);

				URL url = bundle.getEntry("/component-templates/componentng2.html");
				String bundleContent = Utils.getURLContent(url);
				FileUtils.writeStringToFile(new File(compDirectory, componentOrServiceName + ".html"), bundleContent, charset);

				url = bundle.getEntry("/component-templates/component.ts");
				bundleContent = Utils.getURLContent(url);
				bundleContent = bundleContent.replaceAll(PACKAGE_NAME_TAG, packageRoot.getName());
				bundleContent = bundleContent.replaceAll(COMPONENT_NAME_TAG, componentOrServiceName);
				bundleContent = bundleContent.replaceAll(COMPONENT_DASH_NAME_TAG, getDashedName(componentOrServiceName));
				bundleContent = bundleContent.replaceAll(COMPONENT_CLASSNAME_TAG, className);
				FileUtils.writeStringToFile(new File(compDirectory, componentOrServiceName + ".ts"), bundleContent, charset);

				url = bundle.getEntry("/component-templates/component.spec.ts");
				bundleContent = Utils.getURLContent(url);
				bundleContent = bundleContent.replaceAll(COMPONENT_NAME_TAG, componentOrServiceName);
				bundleContent = bundleContent.replaceAll(COMPONENT_CLASSNAME_TAG, className);
				FileUtils.writeStringToFile(new File(compDirectory, componentOrServiceName + ".spec.ts"), bundleContent, charset);

				FileUtils.writeStringToFile(new File(compDirectory.getParent(), "public-api.ts"),
					"\nexport * from './" + componentOrServiceName + "/" + componentOrServiceName + "';", charset, true);

				modifyBuildFile(packageRoot, componentOrServiceName, charset);

				if (legacyModulePackage)
				{
					editLegacyModuleFile(moduleFile, charset,
						"import { " + className + " } from './" + componentOrServiceName + "/" + componentOrServiceName + "';\n",
						new String[] { "declarations:", "exports:" }, className);
				}
				else
				{
					// modern standalone package: register the component class name in the NG2-Components manifest attribute
					addComponentToNG2ComponentsManifest(packageRoot, className);
				}
			}
			if (elementType.equals(IPackageReader.WEB_SERVICE))
			{
				URL url = bundle.getEntry("/component-templates/service.ts");
				String bundleContent = Utils.getURLContent(url);
				bundleContent = bundleContent.replaceAll(COMPONENT_NAME_TAG, componentOrServiceName);
				FileUtils.writeStringToFile(new File(compDirectory, componentOrServiceName + ".service.ts"), bundleContent, charset);

				FileUtils.writeStringToFile(new File(compDirectory, "public-api.ts"),
					"\nexport * from './" + componentOrServiceName + ".service';", charset, true);

				modifyBuildFile(packageRoot, componentOrServiceName, charset);

				// Services are never listed in NG2-Components (that attribute is for components only);
				// a standalone service is made available via @Injectable({ providedIn: 'root' }) + the public-api.ts export.
				if (legacyModulePackage)
				{
					String serviceClassName = componentOrServiceName + "Service";
					editLegacyModuleFile(moduleFile, charset,
						"import { " + serviceClassName + " } from './" + componentOrServiceName + ".service';\n",
						new String[] { "providers:" }, serviceClassName);
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private static IFile getModuleFile(IResource packageRoot, String moduleName)
	{
		if (packageRoot instanceof IFolder) return ((IFolder)packageRoot).getFile("project/src/" + moduleName + ".module.ts");
		if (packageRoot instanceof IProject) return ((IProject)packageRoot).getFile("project/src/" + moduleName + ".module.ts");
		return null;
	}

	/**
	 * A package is a legacy module-based package when its manifest declares NG2-Module but not NG2-Components.
	 */
	private static boolean isLegacyModulePackage(IResource packageRoot)
	{
		try
		{
			Manifest manifest = readPackageManifest(packageRoot);
			if (manifest == null) return false;
			Attributes attr = manifest.getMainAttributes();
			String ng2Module = attr.getValue("NG2-Module");
			String ng2Components = attr.getValue("NG2-Components");
			return ng2Module != null && ng2Module.trim().length() > 0 && (ng2Components == null || ng2Components.trim().length() == 0);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			return false;
		}
	}

	private static void editLegacyModuleFile(IFile moduleFile, String charset, String importStatement, String[] properties, String className)
		throws CoreException, IOException
	{
		if (moduleFile == null || !moduleFile.exists())
		{
			ServoyLog.logWarning("Cannot register '" + className +
				"' in the package module: the *.module.ts file is missing for this legacy NG2-Module package.", null);
			return;
		}
		InputStream is = moduleFile.getContents(true);
		String moduleFileContent = null;
		try
		{
			moduleFileContent = Utils.getTXTFileContent(is, Charset.forName(charset));
		}
		finally
		{
			Utils.closeInputStream(is);
		}
		if (moduleFileContent != null)
		{
			moduleFileContent = importStatement + moduleFileContent;
			for (String property : properties)
			{
				moduleFileContent = addToModuleFileContent(moduleFileContent, property, className);
			}
			moduleFile.setContents(new ByteArrayInputStream(moduleFileContent.getBytes(charset)), true, false, null);
		}
	}

	private static String addToModuleFileContent(String moduleFileContent, String property, String className)
	{
		int index = moduleFileContent.indexOf(property);
		int startIndex = moduleFileContent.indexOf("[", index);
		return moduleFileContent.substring(0, startIndex + 1) + "\n\t\t" + className + "," + moduleFileContent.substring(startIndex + 1);
	}

	private static void modifyBuildFile(IResource packageRoot, String componentOrServiceName, String charset) throws CoreException, UnsupportedEncodingException
	{
		IFile buildFile = null;
		if (packageRoot instanceof IFolder) buildFile = ((IFolder)packageRoot).getFile("scripts/build.js");
		if (packageRoot instanceof IProject) buildFile = ((IProject)packageRoot).getFile("scripts/build.js");
		InputStream is = buildFile.getContents(true);
		String buildFileContent = null;
		try
		{
			buildFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
		}
		finally
		{
			Utils.closeInputStream(is);
		}
		if (buildFileContent != null)
		{
			int startAddIndex = buildFileContent.indexOf("zip.addLocalFolder");
			if (startAddIndex >= 0)
			{
				buildFileContent = buildFileContent.replaceFirst("zip.addLocalFolder",
					"zip.addLocalFolder(\"./" + componentOrServiceName + "/\",\"/" + componentOrServiceName + "/\");\nzip.addLocalFolder");
			}
			buildFile.setContents(new ByteArrayInputStream(buildFileContent.getBytes(charset)), true, false, null);
		}
	}

	/**
	 * Reads the META-INF/MANIFEST.MF of the given package root (IFolder/IProject), or returns null if it cannot be read.
	 */
	private static Manifest readPackageManifest(IResource packageRoot) throws CoreException, IOException
	{
		IFile m = getManifestFile(packageRoot);
		if (m == null || !m.exists()) return null;
		m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		try (InputStream contents = m.getContents())
		{
			return new Manifest(contents);
		}
	}

	private static IFile getManifestFile(IResource packageRoot)
	{
		if (packageRoot instanceof IFolder) return ((IFolder)packageRoot).getFile("META-INF/MANIFEST.MF");
		if (packageRoot instanceof IProject) return ((IProject)packageRoot).getFile("META-INF/MANIFEST.MF");
		return null;
	}

	/**
	 * Appends a component class name to the package manifest's NG2-Components attribute, idempotently
	 * (no duplicates, order preserved). The rest of the manifest (including the per-component Name: sub-sections
	 * added by {@link #addToManifest}) is preserved by round-tripping through {@link java.util.jar.Manifest}.
	 */
	static void addComponentToNG2ComponentsManifest(IResource packageRoot, String className) throws CoreException, IOException
	{
		if (className == null || className.trim().length() == 0) return;
		IFile m = getManifestFile(packageRoot);
		if (m == null) return;
		m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());

		Manifest manifest;
		try (InputStream contents = m.getContents())
		{
			manifest = new Manifest(contents);
		}

		if (!addComponentToNG2ComponentsAttribute(manifest, className)) return; // idempotent / blank: nothing to do

		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			manifest.write(out);
			try (InputStream in = new ByteArrayInputStream(out.toByteArray()))
			{
				m.setContents(in, IResource.FORCE, new NullProgressMonitor());
			}
		}
	}

	/**
	 * Adds a component class name to the NG2-Components main attribute of the given manifest, idempotently: the value is
	 * parsed into an ordered, de-duplicated list, the trimmed class name is appended if not already present, and the
	 * attribute is written back comma-separated with no spaces. This is the pure in-memory part of
	 * {@link #addComponentToNG2ComponentsManifest(IResource, String)} (no file I/O), so it can be unit-tested directly.
	 *
	 * @return <code>true</code> when the manifest was modified, <code>false</code> when the class name was blank or
	 *         already present (nothing changed).
	 */
	public static boolean addComponentToNG2ComponentsAttribute(Manifest manifest, String className)
	{
		if (className == null || className.trim().length() == 0) return false;

		String current = manifest.getMainAttributes().getValue("NG2-Components");
		List<String> components = new ArrayList<>();
		if (current != null)
		{
			for (String part : current.split(","))
			{
				String trimmed = part.trim();
				if (trimmed.length() > 0 && !components.contains(trimmed)) components.add(trimmed);
			}
		}
		String newClassName = className.trim();
		if (components.contains(newClassName)) return false; // idempotent: nothing to do

		components.add(newClassName);
		manifest.getMainAttributes().put(new Attributes.Name("NG2-Components"), String.join(",", components));
		return true;
	}

	private void addToManifest(String componentOrServiceName, String elementType, IResource packageRoot)
		throws IOException, CoreException, FileNotFoundException
	{
		if (!(packageRoot instanceof IFolder) && !(packageRoot instanceof IProject)) return;
		IFile m = null;
		if (packageRoot instanceof IFolder) m = ((IFolder)packageRoot).getFile("META-INF/MANIFEST.MF");
		if (packageRoot instanceof IProject) m = ((IProject)packageRoot).getFile("META-INF/MANIFEST.MF");
		m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());

		OutputStream out = null;
		InputStream in = null;
		InputStream contents = m.getContents();
		try
		{
			Manifest manifest = new Manifest(contents);
			Attributes attr = new Attributes();
			attr.put(new Attributes.Name(elementType), "True");
			manifest.getEntries().put(componentOrServiceName + "/" + componentOrServiceName + ".spec", attr);
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
			if (contents != null) contents.close();
		}
	}

	@Override
	public boolean isEnabled()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		IResource packageRoot = SolutionExplorerTreeContentProvider.getResource((IPackageReader)node.getRealObject());
		return (packageRoot instanceof IFolder || packageRoot instanceof IProject);
	}
}
