package com.servoy.eclipse.ui.wizards;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ngpackages.NGPackageManager;
import com.servoy.eclipse.model.nature.ServoyProject;
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
import com.servoy.j2db.util.Debug;

public class NewPackageProjectWizard extends Wizard implements INewWizard
{

	private final NewPackageProjectPage page1;

	private SolutionExplorerView viewer; // if running from solEx, when finished the wizard will try to expand and select the newly created package

	private PlatformSimpleUserNode treeNode;
	private final String packageType;

	public NewPackageProjectWizard(String packageType)
	{
		setWindowTitle("New Package Project");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("solution_wizard_description.gif"));
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

	public void setSolutionExplorerView(SolutionExplorerView solEx)
	{
		this.viewer = solEx;
	}

	@Override
	public boolean performFinish()
	{
		CreatePackageProjectJob createPackageProjectJob = new CreatePackageProjectJob(page1.getPackageName(), page1.getReferencedProjects());
		createPackageProjectJob.schedule();
		return true;
	}

	private class CreatePackageProjectJob extends WorkspaceJob
	{
		private final String projectName;
		private final IProject[] referencedProjects;

		public CreatePackageProjectJob(String projectName, IProject[] referencedProjects)
		{
			super("Creating Package Project");
			this.projectName = projectName;
			this.referencedProjects = referencedProjects;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
		{
			try
			{
				IProject newProject = NGPackageManager.createProject(projectName);
				for (IProject iProject : referencedProjects)
				{
					IProjectDescription solutionProjectDescription = iProject.getDescription();
					AddAsWebPackageAction.addReferencedProjectToDescription(newProject, solutionProjectDescription);
					iProject.setDescription(solutionProjectDescription, new NullProgressMonitor());
				}
				NewResourcesComponentsOrServicesPackageAction.createManifest(newProject, projectName, projectName, packageType); // TODO symbolic name here instead of double projectName?

				if (viewer != null)
				{
					SimpleUserNode nodeToExpand = treeNode;
					if (treeNode.parent != null)
					{
						List<IProject> projects = Arrays.asList(referencedProjects);
						if (treeNode.parent.getRealObject() instanceof ServoyProject &&
							!projects.contains(((ServoyProject)treeNode.parent.getRealObject()).getProject()))
						{
							ServoyProject activeSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
							if (activeSolution != null && projects.contains(activeSolution.getProject()))
							{
								PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getTreeContentProvider().findChildNode(
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
				Debug.log(e);
			}
			catch (IOException e)
			{
				Debug.log(e);
			}

			return Status.OK_STATUS;
		}

	}

	private class NewPackageProjectPage extends WizardPage
	{
		private Text packName;
		private FilteredTreeViewer ftv;

		protected NewPackageProjectPage()
		{
			super("Create new Components Package Project");
			setDescription("Create a " + packageType + " package");
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

				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject iProject : projects)
				{
					if (iProject.getName().toLowerCase().equals(text.toLowerCase()))
					{
						setErrorMessage("Project " + text + " already exists in workspace.");
						result = false;
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
			packageLabel.setText("Package Project name");
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

			Label selectSolutionLabel = new Label(container, SWT.NONE);
			selectSolutionLabel.setText("Select the solutions that will use the new package");

			ITreeContentProvider contentProvider = FlatTreeContentProvider.INSTANCE;
			LabelProvider labelProvider = new LabelProvider();
			int treeStyle = SWT.MULTI | SWT.CHECK;
			ftv = new FilteredTreeViewer(container, true, false, contentProvider, labelProvider, null, treeStyle,
				new TreePatternFilter(TreePatternFilter.FILTER_PARENTS), new LeafnodesSelectionFilter(contentProvider));

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

			if (treeNode.parent.getType() == UserNodeType.SOLUTION || treeNode.parent.getType() == UserNodeType.SOLUTION_ITEM)
			{
				ftv.setSelection(new StructuredSelection(((ServoyProject)treeNode.parent.getRealObject()).getProject().getName()));
			}
			else if (servoyModel.getActiveProject() != null)
			{
				ftv.setSelection(new StructuredSelection(servoyModel.getActiveProject().getSolution().getName()));
			}

			final GroupLayout groupLayout = new GroupLayout(container);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().add(packageLabel).addPreferredGap(LayoutStyle.UNRELATED).add(packName, GroupLayout.DEFAULT_SIZE,
					GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).add(selectSolutionLabel).add(ftv, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE));
			groupLayout.setVerticalGroup(
				groupLayout.createSequentialGroup().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(packageLabel).add(packName)).add(
					selectSolutionLabel).add(ftv));
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
	}
}
