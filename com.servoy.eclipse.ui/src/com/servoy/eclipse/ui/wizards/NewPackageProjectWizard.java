package com.servoy.eclipse.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.FilteredTreeViewer;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.AddAsWebPackageAction;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.util.Debug;

public class NewPackageProjectWizard extends Wizard implements INewWizard
{

	private final NewPackageProjectPage page1 = new NewPackageProjectPage();

	private final Map<UserNodeType, String> typeToTypeName = new HashMap<UserNodeType, String>();

	public NewPackageProjectWizard()
	{
		setWindowTitle("New Package Project");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("solution_wizard_description.gif"));
		typeToTypeName.put(UserNodeType.COMPONENTS_PROJECTS, IPackageReader.WEB_COMPONENT);
		typeToTypeName.put(UserNodeType.SERVICES_PROJECTS, IPackageReader.WEB_SERVICE);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		addPage(page1);
		if (selection.getFirstElement() instanceof PlatformSimpleUserNode)
		{
			PlatformSimpleUserNode platformSimpleUserNode = (PlatformSimpleUserNode)selection.getFirstElement();
			String typeName = typeToTypeName.get(platformSimpleUserNode.getType());
			if (typeName != null) page1.setDefaultPackageType(typeName);
		}

	}

	@Override
	public boolean performFinish()
	{
		CreatePackageProjectJob createPackageProjectJob = new CreatePackageProjectJob(page1.getPackageName(), page1.getPackageType(),
			page1.getReferencedProjects());
		createPackageProjectJob.schedule();
		return true;
	}

	private class CreatePackageProjectJob extends WorkspaceJob
	{

		private final String projectName;
		private final String packageType;
		private final IProject[] referencedProjects;

		/**
		 * @param name
		 */
		public CreatePackageProjectJob(String projectName, String packageType, IProject[] referencedProjects)
		{
			super("Creating Package Project");
			this.projectName = projectName;
			this.packageType = packageType;
			this.referencedProjects = referencedProjects;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
		{
			IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(projectName);
			try
			{
				newProject.create(new NullProgressMonitor());
				newProject.open(new NullProgressMonitor());
				IProjectDescription description = newProject.getDescription();
				description.setNatureIds(new String[] { ServoyNGPackageProject.NATURE_ID });
				newProject.setDescription(description, new NullProgressMonitor());
				for (IProject iProject : referencedProjects)
				{
					IProjectDescription solutionProjectDescription = iProject.getDescription();
					AddAsWebPackageAction.addReferencedProjectToDescription(newProject, solutionProjectDescription);
					iProject.setDescription(solutionProjectDescription, new NullProgressMonitor());
				}
				createManifest(newProject, projectName, packageType);
			}
			catch (CoreException e)
			{
				Debug.log(e);
			}
			catch (IOException e)
			{
				Debug.log(e);
			}
			return Status.OK_STATUS;
		}

	}


	private void createManifest(IProject pack, String projectName, String packageType) throws CoreException, IOException
	{
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(new Attributes.Name(Package.BUNDLE_NAME), projectName);
		manifest.getMainAttributes().put(new Attributes.Name(Package.BUNDLE_SYMBOLIC_NAME), projectName);
		manifest.getMainAttributes().put(new Attributes.Name(Package.PACKAGE_TYPE), packageType);

		IFolder metainf = pack.getFolder("META-INF");
		metainf.create(true, true, new NullProgressMonitor());
		IFile m = metainf.getFile("MANIFEST.MF");
		m.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());

		try (OutputStream out = new FileOutputStream(new File(m.getLocationURI()), false))
		{
			manifest.write(out);
		}
	}


	private class NewPackageProjectPage extends WizardPage
	{
		private Text packName;
		private FilteredTreeViewer ftv;
		private Group packageTypeGroup;
		private String defaultPackageTypeName;

		/**
		 * @param pageName
		 * @param title
		 * @param titleImage
		 */
		protected NewPackageProjectPage()
		{
			super("Create new Components Package Project");
		}

		/**
		 * @param typeName
		 */
		public void setDefaultPackageType(String typeName)
		{
			this.defaultPackageTypeName = typeName;
		}

		/**
		 * @return
		 */
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

				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject iProject : projects)
				{
					if (iProject.getName().equals(text))
					{
						setErrorMessage("Project " + text + " already exists in workspace.");
						result = false;
					}
				}

			}
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public void createControl(Composite parent)
		{
			Composite container = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = 0;

			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalAlignment = GridData.FILL_HORIZONTAL;
			data.grabExcessHorizontalSpace = true;

			container.setLayout(layout);
			container.setLayoutData(data);

			data = new GridData();
			data.horizontalAlignment = SWT.LEFT;
			data.grabExcessHorizontalSpace = false;


			Label typeLabel = new Label(container, SWT.NONE);
			typeLabel.setText("This Package Project will contain");
			typeLabel.setLayoutData(data);

			data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;

			packageTypeGroup = new Group(container, SWT.SHADOW_IN);
			packageTypeGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
			Button componentButton = new Button(packageTypeGroup, SWT.RADIO);
			componentButton.setText(IPackageReader.WEB_COMPONENT);
			if (defaultPackageTypeName == null || IPackageReader.WEB_COMPONENT.equals(defaultPackageTypeName)) componentButton.setSelection(true);
			Button serviceButton = new Button(packageTypeGroup, SWT.RADIO);
			serviceButton.setText(IPackageReader.WEB_SERVICE);
			if (IPackageReader.WEB_SERVICE.equals(defaultPackageTypeName)) serviceButton.setSelection(true);
			packageTypeGroup.setLayoutData(data);


			data = new GridData();
			data.horizontalAlignment = SWT.LEFT;
			data.grabExcessHorizontalSpace = false;


			Label packageLabel = new Label(container, SWT.NONE);
			packageLabel.setText("Package Project name");
			packageLabel.setLayoutData(data);

			data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;

			packName = new Text(container, SWT.BORDER);
			packName.setLayoutData(data);
			packName.addModifyListener(new ModifyListener()
			{

				@Override
				public void modifyText(ModifyEvent e)
				{
					setPageComplete(validatePage());
				}
			});


			Composite treeContainer = new Composite(container, SWT.NONE);
			data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			data.horizontalSpan = 2;
			treeContainer.setLayout(new GridLayout(1, false));
			treeContainer.setLayoutData(data);

			Label selectSolutionLabel = new Label(treeContainer, SWT.NONE);
			selectSolutionLabel.setText("Select the solutions that will use the new package");
			data = new GridData();
			data.horizontalAlignment = SWT.LEFT;
			data.grabExcessHorizontalSpace = false;
			selectSolutionLabel.setLayoutData(data);

			ITreeContentProvider contentProvider = FlatTreeContentProvider.INSTANCE;
			LabelProvider labelProvider = new LabelProvider();
			int treeStyle = SWT.MULTI | SWT.CHECK;
			ftv = new FilteredTreeViewer(treeContainer, true, false, contentProvider, labelProvider, null, treeStyle,
				new TreePatternFilter(TreePatternFilter.FILTER_PARENTS), new LeafnodesSelectionFilter(contentProvider));


			data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			data.verticalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			data.grabExcessVerticalSpace = true;
			ftv.setLayoutData(data);

			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();


			List<String> availableSolutions = new ArrayList<String>();
			try
			{
				for (RootObjectMetaData rootObject : ServoyModel.getDeveloperRepository().getRootObjectMetaDatas())
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

			if (servoyModel.getActiveProject() != null) ftv.setSelection(new StructuredSelection(servoyModel.getActiveProject().getSolution().getName()));

			setControl(container);
			setPageComplete(validatePage());
		}

		public String getPackageName()
		{
			return packName.getText();
		}

		public String getPackageType()
		{
			Control[] children = packageTypeGroup.getChildren();

			for (Control control : children)
			{
				if (control instanceof Button)
				{
					Button radioButton = (Button)control;
					if (radioButton.getSelection()) return radioButton.getText();
				}
			}
			return null;
		}


	}

}
