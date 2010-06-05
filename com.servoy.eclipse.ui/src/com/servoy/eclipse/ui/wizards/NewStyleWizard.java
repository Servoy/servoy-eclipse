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
package com.servoy.eclipse.ui.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.builder.ChangeResourcesProjectQuickFix.IValidator;
import com.servoy.eclipse.core.builder.ChangeResourcesProjectQuickFix.ResourcesProjectChooserComposite;
import com.servoy.eclipse.core.builder.ChangeResourcesProjectQuickFix.ResourcesProjectSetupJob;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.core.repository.StringResourceDeserializer;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.util.IdentDocumentValidator;

/**
 * Wizard used in order to create a new Servoy style. Will optionally create new resource project (for styles & other info).
 * 
 * @author Andrei Costescu
 */
public class NewStyleWizard extends Wizard implements INewWizard
{

	private NewStyleWizardPage page1;
	private NewStyleContentPage page2;
	private WizardPage errorPage;

	/**
	 * Creates a new wizard.
	 */
	public NewStyleWizard()
	{
		setWindowTitle("New style");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("solution_wizard_description.gif"));
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		ServoyResourcesProject defaultResourceProjectToUse = null;
		try
		{
			if (selection.size() == 1)
			{
				Object selectedObject = selection.getFirstElement();
				IProject p = null;
				if (selectedObject instanceof IProject)
				{
					p = (IProject)selectedObject;
				}
				else if (selectedObject instanceof IAdaptable)
				{
					p = (IProject)((IAdaptable)selectedObject).getAdapter(IProject.class);
				}

				if (p != null)
				{
					if (p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						defaultResourceProjectToUse = (ServoyResourcesProject)p.getNature(ServoyResourcesProject.NATURE_ID);
					}
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Exception while trying to find default resource project for new style", e);
		}

		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() == null)
		{
			errorPage = new WizardPage("No active Servoy solution project found")
			{

				public void createControl(Composite parent)
				{
					setControl(new Composite(parent, SWT.NONE));
				}

			};
			errorPage.setTitle("No active Servoy solution project found");
			errorPage.setErrorMessage("Please activate a Servoy solution project before trying to create a new style");
			errorPage.setPageComplete(false);
			page1 = null;
			page2 = null;
		}
		else
		{
			errorPage = null;
			page1 = new NewStyleWizardPage("New Style", defaultResourceProjectToUse);
			page2 = new NewStyleContentPage("Initial style content");
		}
	}

	@Override
	public void addPages()
	{
		if (errorPage != null)
		{
			addPage(errorPage);
		}
		else
		{
			addPage(page1);
			addPage(page2);
		}
	}

	public static String getSampleStyleContent()
	{
		StringBuffer sample = new StringBuffer();
		sample.append("field\n{\n");
		sample.append("	background-color: #e9e8e8;\n");
		sample.append("	border-style: inset;\n");
		sample.append("	font: 11pt \"Verdana\";\n");
		sample.append("	text-align: left;\n");
		sample.append("	margin: 1px 2px 1px 1px;\n");
		sample.append("}\n");
		sample.append("check\n{\n");
		sample.append("	background-color: #eeeeee;\n");
		sample.append("	border-style: empty;\n");
		sample.append("	font: bold 10pt \"Verdana\";\n");
		sample.append("}\n");
		sample.append("radio\n{\n");
		sample.append("	background-color: #dddddd;\n");
		sample.append("	border-style: empty;\n");
		sample.append("	font: bold 10pt \"Verdana\";\n");
		sample.append("}\n");
		sample.append("combobox\n{\n");
		sample.append("	color: #0000ff;\n");
		sample.append("	font: italic 10pt \"Verdana\";\n");
		sample.append("}\n");
		sample.append("portal\n{\n");
		sample.append("	background-color: #cccccc;\n");
		sample.append("	border-style: inset;\n");
		sample.append("}\n");
		sample.append("tabpanel\n{\n");
		sample.append("	background-color: #cceecc;\n");
		sample.append("	border-style: inset;\n");
		sample.append("}\n");
		sample.append("form\n{\n");
		sample.append("	background-color: #ddeecc;\n");
		sample.append("	border-style: inset;\n");
		sample.append("}\n");
		sample.append("label\n{\n");
		sample.append("	color: #993300;\n");
		sample.append("	border-style: solid;\n");
		sample.append("	font: bold 10pt Verdana;\n");
		sample.append("}\n");
		sample.append("label.mytext\n{\n");
		sample.append("	color: red;\n");
		sample.append("	border-style: solid;\n");
		sample.append("	vertical-align: middle;\n");
		sample.append("	font: bold italic 10pt Verdana;\n");
		sample.append("	border-width: 1px 1px 1px 1px;\n");
		sample.append("	border-color: #111111 #111111 #111111 #111111;\n");
		sample.append("	margin: 2px 2px 2px 2px;\n");
		sample.append("}\n");
		sample.append("button\n{\n");
		sample.append("	color: red;\n");
		sample.append("	margin: 10px 10px 0px 0px;\n");
		sample.append("	font: bold 11pt \"Verdana\";\n");
		sample.append("}");

		return sample.toString();
	}

	@Override
	public boolean performFinish()
	{
		final ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		final ServoyProject activeProject = servoyModel.getActiveProject();
		if (activeProject != null)
		{
			WorkspaceJob job;
			ISchedulingRule rule = new SerialRule();
			// if there is an active resource project, the new style will be added to it; else we must add a reference to the chosen (maybe new) resource project
			if (activeProject.getResourcesProject() == null)
			{
				IProject newResourcesProject;
				if (page1.getResourceProjectData().getNewResourceProjectName() != null)
				{
					newResourcesProject = ServoyModel.getWorkspace().getRoot().getProject(page1.getResourceProjectData().getNewResourceProjectName());
				}
				else
				{
					newResourcesProject = page1.getResourceProjectData().getExistingResourceProject().getProject();
				}
				// create new resource project if necessary and reference it from active project
				job = new ResourcesProjectSetupJob("Setting up resources project for active solution", newResourcesProject, null, activeProject, false);
				job.setRule(rule);
				job.setUser(true);
				job.schedule();
			}

			// the first job normally will result in an available resources project for the active solution,
			// so all that is left now is to create the style

			job = new WorkspaceJob("Creating style")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					monitor.beginTask("Creating style", 2);

					if (activeProject.getResourcesProject() != null)
					{
						// ok, now, finally, we can create the style
						try
						{
							EclipseRepository rep = (EclipseRepository)ServoyModel.getDeveloperRepository();
							Style s = (Style)rep.createNewRootObject(page1.getNewStyleName(), IRepository.STYLES);
							s.setCSSText(page2.getInitialStyleContent());
							rep.updateRootObject(s);
							StringResourceDeserializer.fixStyleCssProfile(activeProject.getResourcesProject().getProject().getName(), s);

							activeProject.getResourcesProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
						}
						catch (RepositoryException e)
						{
							MessageDialog.openError(getShell(), "Cannot create new style", "Reason: " + e.getMessage());
						}
					}
					else
					{
						MessageDialog.openError(getShell(), "Cannot create new style", "Cannot find a resources project for the active solution.");
						ServoyLog.logError(
							"Cannot find an active resources project in order to create a new style. Either the creation of a new project failed, the reference from the active solution project failed to be added or it is a model refresh issue.",
							null);
					}
					monitor.worked(1);
					monitor.done();

					return Status.OK_STATUS;
				}

			};
			job.setRule(rule);
			job.setUser(true);
			job.schedule();

			job = new WorkspaceJob("Opening style")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					monitor.beginTask("Opening style", 1);
					final Style s = (Style)servoyModel.getActiveRootObject(page1.getNewStyleName(), IRepository.STYLES);
					if (s != null)
					{
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								EditorUtil.openStyleEditor(s);
							}
						});
					}
					monitor.worked(1);
					monitor.done();

					return Status.OK_STATUS;
				}

			};
			job.setRule(rule);
			job.setUser(true);
			job.schedule();

			return true;
		}
		else
		{
			MessageDialog.openError(getShell(), "Cannot create new style",
				"No servoy solution is currently active.\nYou must have an active solution in order to create a style");
			return false;
		}
	}

	public class NewStyleWizardPage extends WizardPage implements Listener, IValidator
	{
		private static final String NEW_RESOURCE_PROJECT = "1";
		private static final String EXISTING_RESOURCE_PROJECT = "2";

		private String styleName;
		private Text styleNameField;
		private ServoyResourcesProject defaultResourceProjectToUse;
		private ResourcesProjectChooserComposite resourceProjectComposite;

		/**
		 * Creates a new style creation wizard page.
		 * 
		 * @param pageName the name of the page
		 * @param defaultResourceProjectToUse the initially selected resource project in which the style should be created.
		 */
		public NewStyleWizardPage(String pageName, ServoyResourcesProject defaultResourceProjectToUse)
		{
			super(pageName);
			setTitle("Create a new style");
			setDescription("- a new style will be created");

			this.defaultResourceProjectToUse = defaultResourceProjectToUse;
		}

		/**
		 * Returns the composite that handles the resources project data. It can be used to determine what the user selected.
		 * 
		 * @return the composite that handles the resources project data. It can be used to determine what the user selected.
		 */
		public ResourcesProjectChooserComposite getResourceProjectData()
		{
			return resourceProjectComposite;
		}

		/**
		 * Returns the name of the new style.
		 * 
		 * @return the name of the new style.
		 */
		public String getNewStyleName()
		{
			return styleName;
		}

		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);

			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);

			// style name UI
			Label styleLabel;
			styleLabel = new Label(topLevel, SWT.NONE);
			styleLabel.setText("Style name");

			styleNameField = new Text(topLevel, SWT.BORDER);
			styleNameField.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					handleStyleNameChanged();
				}
			});

			// setup initial resources project selection and see if there is any point in leting the user
			// choose resources project
			ServoyResourcesProject active = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
			boolean resourcesVisible = true;
			if (active != null)
			{
				// this means there currently is an active styles project - so we use it
				defaultResourceProjectToUse = active;
				resourcesVisible = false;
				setDescription("- a new style will be created in resources project \"" + active.getProject().getName() + "\"");
			}
			resourceProjectComposite = new ResourcesProjectChooserComposite(topLevel, SWT.NONE, this,
				"Choose the resources project (it will become the resource project for the active solution)", defaultResourceProjectToUse);
			resourceProjectComposite.setVisible(resourcesVisible);

			// layout of the page
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 20;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			topLevel.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(styleNameField, 0, SWT.CENTER);
			styleLabel.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(styleLabel, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			styleNameField.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(styleNameField, 20);
			formData.bottom = new FormAttachment(100, 0);
			resourceProjectComposite.setLayoutData(formData);
		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			if (visible)
			{
				styleNameField.setFocus();
				setPageComplete(validatePage());
			}
		}

		private void handleStyleNameChanged()
		{
			styleName = styleNameField.getText();
			setPageComplete(validatePage());
		}

		/**
		 * Sees if the data is filled up correctly.
		 * 
		 * @return true if the content is OK (page ready); false otherwise.
		 */
		protected boolean validatePage()
		{
			String error = null;
			if (styleNameField.getText().trim().length() == 0)
			{
				error = "Please give a name for the new style";
			}
			else
			{
				error = resourceProjectComposite.validate();
			}
			if (error == null)
			{
				// see if style name is OK
				if (!IdentDocumentValidator.isJavaIdentifier(styleName))
				{
					error = "Style name has unsupported characters";
				}
				else if (styleName.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH)
				{
					error = "Style name is too long";
				}
				else
				{
					IStatus validationResult = ServoyModel.getWorkspace().validateName(styleName, IResource.FILE);
					if (!validationResult.isOK())
					{
						error = "The name of the style to be created is not valid: " + validationResult.getMessage();
					}
					List<IRootObject> allStylesList = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.STYLES);
					for (IRootObject style : allStylesList)
					{
						if (styleName.toLowerCase().equals(style.getName().toLowerCase()))
						{
							error = "A style with this name already exists. Please modify the style name.";
							break;
						}
					}
				}
			}
			setErrorMessage(error);
			return error == null;
		}

		/**
		 * The <code>NewStyleWizardPage</code> implementation of this <code>Listener</code> method handles all events and enablements for controls on this page.
		 * Subclasses may extend.
		 */
		public void handleEvent(Event event)
		{
			setPageComplete(validatePage());
		}

		public String validate()
		{
			setPageComplete(validatePage());
			return getErrorMessage();
		}
	}

	public class NewStyleContentPage extends WizardPage implements Listener
	{

		private boolean useSampleContent = true;
		private Button useOrNot;
		private Text textArea;

		/**
		 * Creates a new style creation wizard page.
		 * 
		 * @param pageName the name of the page
		 */
		public NewStyleContentPage(String pageName)
		{
			super(pageName);
			setTitle("Create a new style");
			setDescription("- choose the initial content of the new style");
		}

		public String getInitialStyleContent()
		{
			if (useSampleContent)
			{
				return getSampleStyleContent();
			}
			else
			{
				return "";
			}
		}

		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);

			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);

			// check box
			useOrNot = new Button(topLevel, SWT.CHECK);
			useOrNot.setText("Fill new style with sample content");
			useOrNot.setSelection(true);
			useOrNot.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					checkboxChanged();
				}
			});

			// text area
			textArea = new Text(topLevel, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
			textArea.setText(getSampleStyleContent());
			textArea.setEditable(false);

			// layout of the page
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 20;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			topLevel.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(0, 0);
			useOrNot.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(useOrNot, 0);
			formData.bottom = new FormAttachment(100, 0);
			formData.right = new FormAttachment(100, 0);
			formData.height = 100;
			textArea.setLayoutData(formData);
		}

		protected void checkboxChanged()
		{
			useSampleContent = useOrNot.getSelection();
			textArea.setEnabled(useSampleContent);
		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			if (visible)
			{
				useOrNot.setFocus();
				setPageComplete(true);
			}
		}

		/**
		 * The <code>NewStyleContentPage</code> implementation of this <code>Listener</code> method handles all events and enablements for controls on this
		 * page. Subclasses may extend.
		 */
		public void handleEvent(Event event)
		{
			// not used
		}

	}

}