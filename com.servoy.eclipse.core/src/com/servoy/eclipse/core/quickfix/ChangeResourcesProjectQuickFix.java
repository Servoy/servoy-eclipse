/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.core.quickfix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Quick fix that allows the user to change the resources project or create a new one.
 *
 * @author acostescu
 */
public class ChangeResourcesProjectQuickFix implements IMarkerResolution
{

	public String getLabel()
	{
		return "Change (or create new) resources project";
	}

	public void run(IMarker marker)
	{
		final IProject project = (IProject)marker.getResource();
		try
		{
			if (project.hasNature(ServoyProject.NATURE_ID))
			{
				ServoyProject servoyProject = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);

				// show resource project choice dialog
				final ResourceProjectChoiceDialog dialog = new ResourceProjectChoiceDialog(UIUtils.getActiveShell(),
					"Resources project for solution '" + servoyProject.getProject().getName() + "'", servoyProject.getResourcesProject(), false);

				if (dialog.open() == Window.OK)
				{
					IProject newResourcesProject;
					if (dialog.getResourceProjectData().getNewResourceProjectName() != null)
					{
						newResourcesProject = ServoyModel.getWorkspace().getRoot().getProject(dialog.getResourceProjectData().getNewResourceProjectName());
					}
					else
					{
						newResourcesProject = dialog.getResourceProjectData().getExistingResourceProject().getProject();
					}
					if (newResourcesProject != null)
					{
						// ok now associate the selected(create if necessary) resources project with the solution resources project
						// create new resource project if necessary and reference it from selected solution
						WorkspaceJob job = new ResourcesProjectSetupJob(
							"Setting up resources project for solution '" + servoyProject.getProject().getName() + "'", newResourcesProject, null,
							servoyProject.getProject(), true);
						job.setRule(servoyProject.getProject().getWorkspace().getRoot());
						job.setUser(true);
						job.schedule();
					}
				}
			}
			else
			{
				ServoyLog.logWarning("ChangeResourcesProjectFix applied on non-servoy solution project", null);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * A dialog that can be used to change the resources project for a solution project.
	 */
	public static class ResourceProjectChoiceDialog extends Dialog implements IValidator
	{

		private final ServoyResourcesProject initialSelection;
		private ResourcesProjectChooserComposite chooserComposite;
		private Label errorText;
		private Label errorIcon;
		private final String titleText;
		private final boolean allowInitialSelection;

		public ResourceProjectChoiceDialog(Shell parentShell, String titleText, ServoyResourcesProject initialSelection, boolean allowInitialSelection)
		{
			super(parentShell);
			this.initialSelection = initialSelection;
			this.titleText = titleText;
			this.allowInitialSelection = allowInitialSelection;
			setBlockOnOpen(true);
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		public ResourcesProjectChooserComposite getResourceProjectData()
		{
			return chooserComposite;
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite composite = (Composite)super.createDialogArea(parent);
			GridLayout gl = ((GridLayout)composite.getLayout());
			if (gl.marginTop == 0) gl.marginTop = gl.marginHeight;
			gl.marginHeight = 0;
			gl.marginBottom = 0;

			GridLayout tgl = (GridLayout)composite.getLayout();
			tgl.marginBottom = 0;

			// set up error labels & icon
			GridData gd;
			// resource chooser
			gd = new GridData();
			gd.horizontalAlignment = SWT.FILL;
			gd.verticalAlignment = SWT.FILL;
			gd.grabExcessHorizontalSpace = true;
			gd.grabExcessVerticalSpace = true;
			chooserComposite = new ResourcesProjectChooserComposite(composite, SWT.NONE, this, "Please select the resources project", initialSelection,
				initialSelection == null);
			chooserComposite.setLayoutData(gd);

			Composite errorComposite = new Composite(composite, SWT.NONE);
			gl = new GridLayout(2, false);
			errorComposite.setLayout(gl);
			gl.marginHeight = gl.marginWidth = 0;

			errorIcon = new Label(errorComposite, SWT.NONE);
			errorIcon.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
			gd = new GridData();
			gd.horizontalAlignment = SWT.FILL;
			gd.grabExcessHorizontalSpace = false;
			errorIcon.setLayoutData(gd);

			errorText = new Label(errorComposite, SWT.WRAP);
			Color c = new Color(null, 255, 0, 0);
			errorText.setForeground(c);
			c.dispose();

			gd = new GridData();
			gd.horizontalAlignment = SWT.FILL;
			gd.grabExcessHorizontalSpace = true;
			errorText.setLayoutData(gd);

			gd = new GridData();
			gd.horizontalAlignment = SWT.FILL;
			errorComposite.setLayoutData(gd);

			applyDialogFont(composite);
			return composite;
		}

		public String validate()
		{
			if (chooserComposite == null) return null;
			String errorMessage = chooserComposite.validate();
			if (errorMessage == null && chooserComposite.getExistingResourceProject() == initialSelection && initialSelection != null && !allowInitialSelection)
			{
				errorMessage = "The selected resources project is already used by the solution";
			}
			if (errorMessage == null)
			{
				errorText.setVisible(false);
				errorIcon.setVisible(false);
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
			else
			{
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				errorText.setText(errorMessage);
				errorText.setVisible(true);
				errorIcon.setVisible(true);
			}
			return errorMessage;
		}

		@Override
		public void create()
		{
			super.create();
			getShell().setText(titleText);
			validate();
		}

	}

	/**
	 * A job that can be used to change the resources project for a solution project.
	 */
	public static class ResourcesProjectSetupJob extends WorkspaceJob
	{
		public final static String CLOSED_DELETED_RESOURCES_PROJECT_KEY = "c_D_R";
		public final static String CLOSED_DELETED_RESOURCES_PROJECT_DELIM = ">"; // some illegal char for project names

		private final IProject newResourcesProject;
		private final IProject solutionProject;
		private final boolean checkForExistingResourcesProject;
		private final IProject projectToRemoveFromReferences;

		public ResourcesProjectSetupJob(String name, IProject newResourcesProject, IProject projectToRemoveFromReferences, IProject solutionProject,
			boolean checkForExistingResourcesProject)
		{
			super(name);
			this.newResourcesProject = newResourcesProject;
			this.solutionProject = solutionProject;
			this.checkForExistingResourcesProject = checkForExistingResourcesProject;
			this.projectToRemoveFromReferences = projectToRemoveFromReferences;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
		{
			monitor.beginTask(getName(), 2);
			// create Resource project if needed
			monitor.setTaskName("Creating new resources project if needed");
			createResourcesProjectIfNeeded(newResourcesProject);
			monitor.worked(1);

			// link active solution project to the resource project; store project description
			monitor.setTaskName("Linking solution project to the resource project");
			IProjectDescription description = solutionProject.getDescription();
			IProject[] oldReferences = description.getReferencedProjects();

			QualifiedName qn = new QualifiedName(Activator.PLUGIN_ID, ResourcesProjectSetupJob.CLOSED_DELETED_RESOURCES_PROJECT_KEY);
			String closedOrDeletedResourceReferences = solutionProject.getPersistentProperty(qn);
			solutionProject.setPersistentProperty(qn, null);

			if (checkForExistingResourcesProject || closedOrDeletedResourceReferences != null)
			{
				List<IProject> oldToKeep = new ArrayList<IProject>();

				List<String> closedOrDeletedRP = closedOrDeletedResourceReferences != null
					? Arrays.asList(closedOrDeletedResourceReferences.split(ResourcesProjectSetupJob.CLOSED_DELETED_RESOURCES_PROJECT_DELIM)) : null;
				for (IProject p : oldReferences)
				{
					if (!(p.exists() && p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID)) && (p != projectToRemoveFromReferences) &&
						(closedOrDeletedRP == null || !closedOrDeletedRP.contains(p.getName())))
					{
						oldToKeep.add(p);
					}
				}
				oldReferences = oldToKeep.toArray(new IProject[oldToKeep.size()]);
			}
			ArrayList<IProject> newReferences = new ArrayList<IProject>(oldReferences.length + 1);
			if (newResourcesProject != null) newReferences.add(newResourcesProject);
			for (IProject ref : oldReferences)
			{
				if (ref != newResourcesProject)
				{
					newReferences.add(ref);
				}
			}
			description.setReferencedProjects(newReferences.toArray(new IProject[newReferences.size()]));
			solutionProject.setDescription(description, null);
			monitor.worked(1);

			monitor.done();
			return Status.OK_STATUS;
		}

		public static void createResourcesProjectIfNeeded(IProject newResourcesProject) throws CoreException
		{
			if (newResourcesProject != null && !newResourcesProject.exists())
			{
				// create a new resource project
				newResourcesProject.create(null);
				newResourcesProject.open(null);
				IProjectDescription resourceProjectDescription = newResourcesProject.getDescription();
				resourceProjectDescription.setNatureIds(new String[] { ServoyResourcesProject.NATURE_ID });
				newResourcesProject.setDescription(resourceProjectDescription, null);
			}
		}

	}

	/**
	 * A composite that can be used to change the resources project for a solution project.
	 */
	public static class ResourcesProjectChooserComposite extends Composite implements IValidator
	{
		private boolean createResourceProject;
		private boolean noResourceProject;

		private static final String NEW_RESOURCE_PROJECT = "1";
		private static final String EXISTING_RESOURCE_PROJECT = "2";
		private static final String NO_RESOURCE_PROJECT = "3";

		private final Button radio3;
		private final Button radio2;
		private final Button radio1;
		private final Text resourceProjectNameField;
		private final ListViewer resourceProjectList;

		private ServoyResourcesProject existingResourcesProject;
		private String resourcesProjectName;

		private final IValidator validator;

		public ResourcesProjectChooserComposite(Composite parent, int style, IValidator validator, String text, ServoyResourcesProject initialResourcesProject,
			boolean noResourcesProject)
		{
			this(parent, style, validator, text, initialResourcesProject, noResourcesProject, true);
		}

		public ResourcesProjectChooserComposite(Composite parent, int style, IValidator validator, String text, ServoyResourcesProject initialResourcesProject,
			boolean noResourcesProject, boolean canCreate)
		{
			super(parent, style);
			setLayout(new FillLayout());
			Group radioGroupComposite = new Group(this, SWT.NONE);
			this.validator = validator;

			radioGroupComposite.setText(text);
			GridLayout gridLayout = new GridLayout();
			gridLayout.horizontalSpacing = 8;
			gridLayout.numColumns = 2;
			radioGroupComposite.setLayout(gridLayout);

			radio1 = new Button(radioGroupComposite, SWT.RADIO | SWT.LEFT);
			radio1.setText("Create new resource project");
			radio1.setData(NEW_RESOURCE_PROJECT);
			radio1.setVisible(canCreate);
			radio1.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					handleResourceProjectRadioSelected((String)event.widget.getData());
				}
			});

			resourceProjectNameField = new Text(radioGroupComposite, SWT.BORDER);
			resourceProjectNameField.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					handleResourceProjectNameChanged();
				}
			});
			resourceProjectNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			resourceProjectNameField.setText("resources");
			resourceProjectNameField.setVisible(canCreate);

			radio2 = new Button(radioGroupComposite, SWT.RADIO | SWT.LEFT);
			radio2.setText("Use existing resource project");
			radio2.setData(EXISTING_RESOURCE_PROJECT);
			radio2.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					handleResourceProjectRadioSelected((String)event.widget.getData());
				}
			});
			radio2.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

			resourceProjectList = new ListViewer(radioGroupComposite, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
			resourceProjectList.setContentProvider(new ArrayContentProvider());
			ServoyResourcesProject[] resourcesProjects = ServoyModelManager.getServoyModelManager().getServoyModel().getResourceProjects();
			resourceProjectList.setInput(resourcesProjects);
			resourceProjectList.getControl().setVisible(canCreate);
			resourceProjectList.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleExistingResourceProjectChanged((ServoyResourcesProject)((IStructuredSelection)event.getSelection()).getFirstElement());
				}
			});

			radio3 = new Button(radioGroupComposite, SWT.RADIO | SWT.LEFT);
			radio3.setText("No resources project");
			radio3.setData(NO_RESOURCE_PROJECT);
			radio3.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					handleResourceProjectRadioSelected((String)event.widget.getData());
				}
			});

			GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
			data.heightHint = canCreate ? 60 : 0;
			data.widthHint = 150;
			resourceProjectList.getList().setLayoutData(data);

			if (noResourcesProject)
			{
				radio3.setSelection(true);
			}
			else
			{
				// setup initial page state
				selectResourceProjectRadio(resourcesProjects);
				selectExistingResourceProject(resourceProjectList, resourcesProjects, initialResourcesProject);
			}
		}

		private void selectResourceProjectRadio(ServoyResourcesProject[] resourcesProjects)
		{
			if (resourcesProjects.length > 0)
			{
				radio2.setSelection(true);
				resourceProjectNameField.setEnabled(false);
				createResourceProject = false;
			}
			else
			{
				radio1.setSelection(true);
				resourceProjectList.getList().setEnabled(false);
				createResourceProject = true;
				radio2.setEnabled(false);
			}
		}

		private void selectExistingResourceProject(ListViewer resourceProjectList, ServoyResourcesProject[] resourcesProjects,
			ServoyResourcesProject resourceProjectToUse)
		{
			if (resourceProjectToUse != null)
			{
				resourceProjectList.setSelection(new StructuredSelection(resourceProjectToUse), true);
			}
			else if (resourcesProjects.length > 0)
			{
				resourceProjectList.setSelection(new StructuredSelection(resourcesProjects[0]), true);
			}
		}

		private void handleResourceProjectRadioSelected(String newSelection)
		{
			if (newSelection == NEW_RESOURCE_PROJECT)
			{
				createResourceProject = true;
				resourceProjectNameField.setEnabled(true);
				resourceProjectList.getList().setEnabled(false);
				noResourceProject = false;
			}
			else if (newSelection == EXISTING_RESOURCE_PROJECT)
			{
				createResourceProject = false;
				resourceProjectList.getList().setEnabled(true);
				resourceProjectNameField.setEnabled(false);
				noResourceProject = false;
			}
			else if (newSelection == NO_RESOURCE_PROJECT)
			{
				createResourceProject = false;
				resourceProjectNameField.setEnabled(false);
				resourceProjectList.getList().setEnabled(false);
				noResourceProject = true;
			}
			if (validator != null) validator.validate();
		}

		private void handleResourceProjectNameChanged()
		{
			resourcesProjectName = resourceProjectNameField.getText();
			if (validator != null) validator.validate();
		}

		private void handleExistingResourceProjectChanged(ServoyResourcesProject resourceProject)
		{
			existingResourcesProject = resourceProject;
			if (validator != null) validator.validate();
		}

		public ServoyResourcesProject getExistingResourceProject()
		{
			return createResourceProject ? null : (noResourceProject ? null : existingResourcesProject);
		}

		public String getNewResourceProjectName()
		{
			return createResourceProject ? resourcesProjectName : null;
		}

		public String validate()
		{
			String error = null;
			if (createResourceProject)
			{
				// check the validity of the new resource project name (to see that it does not exist
				if (resourceProjectNameField.getText().trim().length() == 0)
				{
					error = "Please give a name for the new resource project";
				}
				else if (ServoyModel.getWorkspace().getRoot().getProject(resourcesProjectName).exists())
				{
					error = "A project with the given name already exists in the workspace";
				}
				else
				{
					IStatus validationResult = ServoyModel.getWorkspace().validateName(resourcesProjectName, IResource.PROJECT);
					if (!validationResult.isOK())
					{
						error = "The name of the resource project to be created is not valid: " + validationResult.getMessage();
					}
				}
			}
			else if (existingResourcesProject == null && !noResourceProject)
			{
				error = "Please select one of the resource projects in the list"; // this should never happen (radio button should be disabled)
			}
			return error;
		}


		public void setResourceProjectName(String name)
		{
			Optional<ServoyResourcesProject> resProject = Arrays.stream(
				ServoyModelManager.getServoyModelManager().getServoyModel().getResourceProjects()).filter(p -> name.equals(p.getProject().getName())).findAny();
			if (resProject.isPresent())
			{
				resourceProjectList.setSelection(new StructuredSelection(resProject.get()));
				handleResourceProjectRadioSelected(EXISTING_RESOURCE_PROJECT);
			}
		}


		public void selectNoResourceProject()
		{
			radio1.setSelection(false);
			radio2.setSelection(false);
			radio3.setSelection(true);
			handleResourceProjectRadioSelected(NO_RESOURCE_PROJECT);
		}
	}

	/**
	 * Interface for objects that can validate the data they hold.
	 */
	public interface IValidator
	{
		/**
		 * Validates the data held by this object.
		 *
		 * @return If the data is valid returns null, otherwise returns a message explaining the problem.
		 */
		String validate();
	}

}