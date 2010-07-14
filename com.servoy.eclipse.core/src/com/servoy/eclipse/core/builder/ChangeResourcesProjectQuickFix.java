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
package com.servoy.eclipse.core.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;

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
				final ResourceProjectChoiceDialog dialog = new ResourceProjectChoiceDialog(Display.getCurrent().getActiveShell(),
					"Resources project for solution '" + servoyProject.getProject().getName() + "'", servoyProject.getResourcesProject());

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
					// ok now associate the selected(create if necessary) resources project with the solution resources project
					WorkspaceJob job;
					// create new resource project if necessary and reference it from selected solution
					job = new ResourcesProjectSetupJob("Setting up resources project for solution '" + servoyProject.getProject().getName() + "'",
						newResourcesProject, null, servoyProject, true);
					job.setRule(servoyProject.getProject().getWorkspace().getRoot());
					job.setUser(true);
					job.schedule();
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

		public ResourceProjectChoiceDialog(Shell parentShell, String titleText, ServoyResourcesProject initialSelection)
		{
			super(parentShell);
			this.initialSelection = initialSelection;
			this.titleText = titleText;
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

			// set up error labels & icon
			GridData gd;
			Composite errorComposite = new Composite(composite, SWT.NONE);
			errorComposite.setLayout(new GridLayout(2, false));

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

			// resource chooser
			gd = new GridData();
			gd.horizontalAlignment = SWT.FILL;
			gd.verticalAlignment = SWT.FILL;
			gd.grabExcessHorizontalSpace = true;
			gd.grabExcessVerticalSpace = true;
			chooserComposite = new ResourcesProjectChooserComposite(composite, SWT.NONE, this, "Please select the resources project", initialSelection);
			chooserComposite.setLayoutData(gd);
			return composite;
		}

		public String validate()
		{
			if (chooserComposite == null) return null;
			String errorMessage = chooserComposite.validate();
			if (errorMessage == null && chooserComposite.getExistingResourceProject() == initialSelection && initialSelection != null)
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

		private final IProject newResourcesProject;
		private final ServoyProject solutionProject;
		private final boolean checkForExistingResourcesProject;
		private final IProject projectToRemoveFromReferences;

		public ResourcesProjectSetupJob(String name, IProject newResourcesProject, IProject projectToRemoveFromReferences, ServoyProject solutionProject,
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
			if (!newResourcesProject.exists())
			{
				// create a new resource project
				monitor.setTaskName("Creating new resources project");
				newResourcesProject.create(null);
				newResourcesProject.open(null);
				IProjectDescription resourceProjectDescription = newResourcesProject.getDescription();
				resourceProjectDescription.setNatureIds(new String[] { ServoyResourcesProject.NATURE_ID });
				newResourcesProject.setDescription(resourceProjectDescription, null);
			}
			monitor.worked(1);

			// link active solution project to the resource project; store project description
			monitor.setTaskName("Linking active solution project to the resource project");
			IProjectDescription description = solutionProject.getProject().getDescription();
			IProject[] oldReferences = description.getReferencedProjects();
			if (checkForExistingResourcesProject)
			{
				List<IProject> old = new ArrayList<IProject>();
				for (IProject p : oldReferences)
				{
					if (!(p.exists() && p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID)) && (p != projectToRemoveFromReferences))
					{
						old.add(p);
					}
				}
				oldReferences = old.toArray(new IProject[old.size()]);
			}
			ArrayList<IProject> newReferences = new ArrayList<IProject>(oldReferences.length + 1);
			newReferences.add(newResourcesProject);
			for (IProject ref : oldReferences)
			{
				if (ref != newResourcesProject)
				{
					newReferences.add(ref);
				}
			}
			description.setReferencedProjects(newReferences.toArray(new IProject[newReferences.size()]));
			solutionProject.getProject().setDescription(description, null);
			monitor.worked(1);

			monitor.done();
			return Status.OK_STATUS;
		}

	}

	/**
	 * A composite that can be used to change the resources project for a solution project.
	 */
	public static class ResourcesProjectChooserComposite extends Composite implements IValidator
	{
		private boolean createResourceProject;

		private static final String NEW_RESOURCE_PROJECT = "1";
		private static final String EXISTING_RESOURCE_PROJECT = "2";

		private final Button radio2;
		private final Button radio1;
		private final Text resourceProjectNameField;
		private final ListViewer resourceProjectList;

		private ServoyResourcesProject existingResourcesProject;
		private String resourcesProjectName;

		private final IValidator validator;

		public ResourcesProjectChooserComposite(Composite parent, int style, IValidator validator, String text, ServoyResourcesProject initialResourcesProject)
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
			resourceProjectList.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleExistingResourceProjectChanged((ServoyResourcesProject)((IStructuredSelection)event.getSelection()).getFirstElement());
				}
			});
			GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
			data.heightHint = 60;
			data.widthHint = 150;
			resourceProjectList.getList().setLayoutData(data);

			// setup initial page state
			selectResourceProjectRadio(resourcesProjects);
			selectExistingResourceProject(resourceProjectList, resourcesProjects, initialResourcesProject);
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
			}
			else if (newSelection == EXISTING_RESOURCE_PROJECT)
			{
				createResourceProject = false;
				resourceProjectList.getList().setEnabled(true);
				resourceProjectNameField.setEnabled(false);
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
			return createResourceProject ? null : existingResourcesProject;
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
			else if (existingResourcesProject == null)
			{
				error = "Please select one of the resource projects in the list"; // this should never happen (radio button should be disabled)
			}
			return error;
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