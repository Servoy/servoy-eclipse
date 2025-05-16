package com.servoy.eclipse.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ngpackages.NGPackageManager;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager.ContainerPackageReader;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.FilteredTreeViewer;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.AddAsWebPackageAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewResourcesComponentsOrServicesPackageAction;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class NewPackageProjectWizard extends Wizard implements INewWizard
{

	private final NewPackageProjectPage page1;

	private SolutionExplorerView viewer; // if running from solEx, when finished the wizard will try to expand and select the newly created package

	private PlatformSimpleUserNode treeNode;
	private final String packageType;
	private final static String PROJECT_NAME_TAG = "##packagename##";

	public NewPackageProjectWizard()
	{
		this(IPackageReader.WEB_COMPONENT);
	}

	public NewPackageProjectWizard(String packageType)
	{
		setWindowTitle("New Package Project");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("solution_wizard_description.png"));
		this.packageType = packageType;
		page1 = new NewPackageProjectPage();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		addPage(page1);
		if (selection.getFirstElement() instanceof PlatformSimpleUserNode)
		{
			treeNode = (PlatformSimpleUserNode)selection.getFirstElement();
		}
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	public void setSolutionExplorerView(SolutionExplorerView solEx)
	{
		this.viewer = solEx;
	}

	@Override
	public boolean performFinish()
	{
		CreatePackageProjectJob createPackageProjectJob = new CreatePackageProjectJob(page1.getPackageName(), page1.getPackageDisplay(),
			page1.getPackageVersion(), page1.getReferencedProjects(), page1.getProjectLocation());
		createPackageProjectJob.schedule();
		return true;
	}


	/**
	 * @param newProject
	 */
	public static void copyAndRenameFiles(IProject project)
	{
		String projectName = project.getName();
		File packageFile = new File(project.getLocation().toOSString());
		copyAllEntries("packagetemplate", packageFile);
		replaceTagInFile(new File(packageFile, "angular.json"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "package.json"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "scripts/build.js"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "project/package.json"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "project/ng-package.json"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "project/karma.conf.js"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "project/src/ng2package.module.ts"), PROJECT_NAME_TAG, projectName);
		replaceTagInFile(new File(packageFile, "project/src/public-api.ts"), PROJECT_NAME_TAG, projectName);
		new File(packageFile, "project/src/ng2package.module.ts").renameTo(new File(packageFile, "project/src/" + projectName + ".module.ts"));
		ignoreNodeModules(project);
		try
		{
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	private static void ignoreNodeModules(IProject project)
	{
		IFile projectFile = project.getFile(".project");

		String projectFileContent = null;
		try (InputStream is = projectFile.getContents(true))
		{
			projectFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
			if (projectFileContent != null)
			{
				int startAddIndex = projectFileContent.indexOf("</projectDescription>");
				if (startAddIndex >= 0)
				{
					projectFileContent = projectFileContent.replaceFirst("</projectDescription>",
						"	<filteredResources>\n" +
							"		<filter>\n" +
							"			<id>1651500530696</id>\n" +
							"			<name></name>\n" +
							"			<type>26</type>\n" +
							"			<matcher>\n" +
							"				<id>org.eclipse.ui.ide.multiFilter</id>\n" +
							"				<arguments>1.0-name-matches-false-false-node_modules</arguments>\n" +
							"			</matcher>\n" +
							"		</filter>\n" +
							"	</filteredResources>\n</projectDescription>");
				}
				projectFile.setContents(new ByteArrayInputStream(projectFileContent.getBytes(Charset.forName("UTF8"))), true, false, null);
			}
		}
		catch (IOException | CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	private static void copyAllEntries(String entryPath, File packageFile)
	{
		Enumeration<URL> entries = com.servoy.eclipse.ngclient.ui.Activator.getInstance().getBundle().findEntries(entryPath, "*", true);
		while (entries.hasMoreElements())
		{
			URL entry = entries.nextElement();
			String filename = entry.getFile();
			try
			{
				if (!filename.endsWith("/"))
				{
					try (InputStream is = entry.openStream())
					{
						FileUtils.copyInputStreamToFile(is, new File(packageFile, filename.substring("/packagetemplate/".length())));
					}
				}
			}
			catch (Exception e)
			{
				Debug.error("Error copy file " + filename + "to node folder " + packageFile, e);
			}
		}
	}

	private static void replaceTagInFile(File file, String tagName, String tagValue)
	{
		try
		{
			String encoding = "UTF-8";
			String content = FileUtils.readFileToString(file, encoding);
			content = content.replaceAll(tagName, tagValue);
			FileUtils.writeStringToFile(file, content, encoding);
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	private class CreatePackageProjectJob extends WorkspaceJob
	{
		private final String projectName;
		private final IProject[] referencedProjects;
		private final String displayName;
		private final String version;
		private final String location;

		public CreatePackageProjectJob(String projectName, String displayName, String version, IProject[] referencedProjects, String location)
		{
			super("Creating Package Project");
			this.projectName = projectName;
			this.displayName = displayName;
			this.version = version;
			this.referencedProjects = referencedProjects;
			this.location = location;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
		{
			try
			{
				IProject newProject = NGPackageManager.createNGPackageProject(projectName, location);
				for (IProject iProject : referencedProjects)
				{
					IProjectDescription solutionProjectDescription = iProject.getDescription();
					AddAsWebPackageAction.addReferencedProjectToDescription(newProject, solutionProjectDescription);
					iProject.setDescription(solutionProjectDescription, new NullProgressMonitor());
				}
				NewResourcesComponentsOrServicesPackageAction.createManifest(newProject, displayName, projectName, version, packageType); // TODO symbolic name here instead of double projectName?
				try
				{
					copyAndRenameFiles(newProject);
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}

				if (viewer != null)
				{
					SimpleUserNode nodeToExpand = treeNode;
					if (treeNode != null && treeNode.parent != null)
					{
						List<IProject> projects = Arrays.asList(referencedProjects);
						if (treeNode.parent.getRealObject() instanceof ServoyProject &&
							!projects.contains(((ServoyProject)treeNode.parent.getRealObject()).getProject()))
						{
							ServoyProject activeSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
							if (activeSolution != null && projects.contains(activeSolution.getProject()))
							{
								PlatformSimpleUserNode node = viewer.getTreeContentProvider().findChildNode(
									viewer.getTreeContentProvider().getInvisibleRootNode(), activeSolution.getProject().getName());
								if (node != null) nodeToExpand = viewer.getTreeContentProvider().findChildNode(node, Messages.TreeStrings_Web_Packages);
							}
							else
							{
								nodeToExpand = viewer.getTreeContentProvider().findChildNode(viewer.getTreeContentProvider().getInvisibleRootNode(),
									Messages.TreeStrings_AllWebPackageProjects);
							}
						}
					}
					viewer.getSolExNavigator().revealWhenAvailable(nodeToExpand, new String[] { projectName }, true); // if in the future this action allows specifying display name, that one should be used here in the array instead
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}

			return Status.OK_STATUS;
		}

	}

	private class NewPackageProjectPage extends WizardPage
	{
		private Text packName;
		private FilteredTreeViewer ftv;
		private Text packDisplay;
		private Text packVersion;
		private ProjectLocationComposite projectLocationComposite;

		protected NewPackageProjectPage()
		{
			super("Create new Components Package Project");
			setDescription("Create a " + packageType + " package");
		}

		public String getProjectLocation()
		{
			return projectLocationComposite.getProjectLocation();
		}

		public IProject[] getReferencedProjects()
		{
			ISelection selection = ftv.getSelection();
			if (selection instanceof StructuredSelection)
			{
				StructuredSelection structuredSelection = (StructuredSelection)selection;
				Object[] array = structuredSelection.toArray();
				ArrayList<IProject> result = new ArrayList<IProject>();
				for (Object object : array)
				{
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(object.toString());
					if (project != null) result.add(project);
				}
				return result.toArray(new IProject[result.size()]);
			}
			return null;
		}

		private boolean validatePage()
		{
			setErrorMessage(null);
			boolean result = this.packName.getText().length() > 0;
			if (result)
			{
				String text = packName.getText();
				if (!Path.EMPTY.isValidSegment(text))
				{
					setErrorMessage(text + " is an invalid project name.");
					return false;
				}
				if (!text.matches("^[a-z][0-9a-z]*$"))
				{
					setErrorMessage("Package name must start with a letter and can contain only lowercase letters and numbers.");
					return false;
				}
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject p : projects)
				{
					try
					{
						if (text.equals(p.getName()))
						{
							setErrorMessage("Project " + text + " already exists in workspace.");
							return false;
						}
						if (p.isAccessible() && p.isOpen() && p.hasNature(ServoyNGPackageProject.NATURE_ID))
						{
							if (text.equals(new ContainerPackageReader(new File(p.getLocationURI()), p)))
							{
								setErrorMessage("Package project with packagename " + text + " already exists in workspace.");
								return false;
							}
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
				IPackageReader[] readers = WebComponentSpecProvider.getSpecProviderState().getAllPackageReaders();
				for (IPackageReader pr : readers)
				{
					String packageName = pr.getPackageName();

					if (packageName.equals(text))
					{
						setErrorMessage("There is already a package with name  " + text + " loaded in the workspace.");
						return false;
					}
				}
			}
			return result;
		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			packName.setFocus();
		}

		@Override
		public void createControl(Composite parent)
		{
			Composite container = new Composite(parent, SWT.NONE);
			Label packageLabel = new Label(container, SWT.NONE);
			packageLabel.setText("Package project name");
			packName = new Text(container, SWT.BORDER);
			packName.setFocus();
			packName.addModifyListener(new ModifyListener()
			{

				@Override
				public void modifyText(ModifyEvent e)
				{
					setPageComplete(validatePage());
				}
			});

			Label packageDisplayLabel = new Label(container, SWT.NONE);
			packageDisplayLabel.setText("Package display name");
			packDisplay = new Text(container, SWT.BORDER);

			Label packageVersionLabel = new Label(container, SWT.NONE);
			packageVersionLabel.setText("Package version");
			packVersion = new Text(container, SWT.BORDER);
			packVersion.setText("1.0.0");

			Label selectSolutionLabel = new Label(container, SWT.NONE);
			selectSolutionLabel.setText("Select the solutions that will use the new package");

			projectLocationComposite = new ProjectLocationComposite(container, SWT.NONE, this.getClass().getName());

			ITreeContentProvider contentProvider = FlatTreeContentProvider.INSTANCE;
			LabelProvider labelProvider = new LabelProvider();
			int treeStyle = SWT.MULTI | SWT.CHECK;
			ftv = new FilteredTreeViewer(container, true, false, contentProvider, labelProvider, null, treeStyle,
				new TreePatternFilter(TreePatternFilter.FILTER_PARENTS), new LeafnodesSelectionFilter(contentProvider));

			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

			List<String> availableSolutions = new ArrayList<String>();
			try
			{
				for (RootObjectMetaData rootObject : ApplicationServerRegistry.get().getDeveloperRepository().getRootObjectMetaDatas())
				{
					if (rootObject.getObjectTypeId() == IRepository.SOLUTIONS)
					{
						availableSolutions.add(rootObject.getName());
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			ftv.setInput(availableSolutions.toArray());

			if (treeNode != null && (treeNode.parent.getType() == UserNodeType.SOLUTION || treeNode.parent.getType() == UserNodeType.SOLUTION_ITEM))
			{
				ftv.setSelection(new StructuredSelection(((ServoyProject)treeNode.parent.getRealObject()).getProject().getName()));
			}
			else if (servoyModel.getActiveProject() != null)
			{
				ftv.setSelection(new StructuredSelection(servoyModel.getActiveProject().getSolution().getName()));
			}

			final GroupLayout groupLayout = new GroupLayout(container);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().add(packageLabel, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE).addPreferredGap(
					LayoutStyle.RELATED).add(packName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.add(
					groupLayout.createSequentialGroup().add(packageDisplayLabel, GroupLayout.PREFERRED_SIZE, 150,
						GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(packDisplay, GroupLayout.DEFAULT_SIZE,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.add(
					groupLayout.createSequentialGroup().add(packageVersionLabel, GroupLayout.PREFERRED_SIZE, 150,
						GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(packVersion, GroupLayout.DEFAULT_SIZE,
							GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.add(projectLocationComposite, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.add(selectSolutionLabel).add(ftv, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

			groupLayout.setVerticalGroup(
				groupLayout.createSequentialGroup().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(packageLabel).add(packName)).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(packageDisplayLabel).add(packDisplay)).add(
						groupLayout.createParallelGroup(GroupLayout.BASELINE).add(packageVersionLabel).add(packVersion))
					.add(projectLocationComposite).add(
						selectSolutionLabel)
					.add(ftv, GroupLayout.PREFERRED_SIZE, 280, Short.MAX_VALUE));
			groupLayout.setAutocreateGaps(true);
			groupLayout.setAutocreateContainerGaps(true);
			container.setLayout(groupLayout);

			setControl(container);
			setPageComplete(validatePage());
		}

		public String getPackageName()
		{
			return packName.getText();
		}

		public String getPackageDisplay()
		{
			return packDisplay.getText();
		}

		public String getPackageVersion()
		{
			return packVersion.getText();
		}
	}
}
