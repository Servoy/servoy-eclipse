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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.IValidator;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

public class NewStylesheetWizard extends Wizard implements INewWizard
{

	private static final String IMPORT_STANDARD_NGCLIENT_CSS = "@import \"standard_ngclient.css\";\n\n";

	public static final String ID = "com.servoy.eclipse.ui.NewStyleheetWizard";

	private NewStyleWizardPage page1;
	private NewStyleContentPage page2;
	private WizardPage errorPage;

	private ServoyProject defaultProjectToUse;

	/**
	 * Creates a new wizard.
	 */
	public NewStylesheetWizard()
	{
		setWindowTitle("New stylesheet");
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("solution_wizard_description.png"));
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		defaultProjectToUse = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		try
		{
			if (selection.size() == 1)
			{
				Object selectedObject = selection.getFirstElement();
				IProject p = null;
				if (selectedObject instanceof ServoyProject)
				{
					p = (IProject)selectedObject;
				}
				else if (selectedObject instanceof IAdaptable)
				{
					p = ((IAdaptable)selectedObject).getAdapter(IProject.class);
				}


				if (p.isOpen() && p.hasNature(ServoyProject.NATURE_ID))
				{
					defaultProjectToUse = (ServoyProject)p.getNature(ServoyProject.NATURE_ID);
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Exception while trying to find default servoy project for new stylesheet", e);
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
			errorPage.setErrorMessage("Please activate a Servoy solution project before trying to create a new stylesheet");
			errorPage.setPageComplete(false);
			page1 = null;
			page2 = null;
		}
		else
		{
			errorPage = null;
			page1 = new NewStyleWizardPage("New Stylesheet", defaultProjectToUse);
			page2 = new NewStyleContentPage("Initial stylesheet content");
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

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	protected String getSampleStyleContent()
	{
		// TODO is defaultCharset always right here?
		return Utils.getTXTFileContent(this.getClass().getResourceAsStream("sample_ngstyle.css"), Charset.defaultCharset());
	}

	protected String getStandardStyleContent()
	{
		// rules that all should have but they are not in servoy css directly to make them easier to change by developers as needed

		// TODO is defaultCharset always right here?
		return Utils.getTXTFileContent(this.getClass().getResourceAsStream("standard_ngstyle.css"), Charset.defaultCharset());
	}

	@Override
	public boolean performFinish()
	{
		WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		try
		{
			String mediaPath = page1.solution + "/" + SolutionSerializer.MEDIAS_DIR + "/" + page1.path;
			if (page1.path.length() > 0) mediaPath += "/";
			wsa.createFolder(mediaPath);
			IPath solutionCSSPath = new Path(mediaPath + page1.styleName + (!page1.styleName.contains(".") ? ".css" : ""));
			IFile solutionCSSFile = ResourcesPlugin.getWorkspace().getRoot().getFile(solutionCSSPath);
			if (solutionCSSFile != null && !solutionCSSFile.exists())
			{
				String cssContent = page2.useSampleContent ? getSampleStyleContent() : "";
				if (page2.useStandardContent)
				{
					// import standard css in solution css
					cssContent = IMPORT_STANDARD_NGCLIENT_CSS + cssContent;

					// write standard css in solution if not already present, next to the solution css
					IPath standardCSSPath = new Path(mediaPath + "standard_ngclient.css");
					IFile standardCSSFile = ResourcesPlugin.getWorkspace().getRoot().getFile(standardCSSPath);
					if (standardCSSFile != null && !standardCSSFile.exists())
					{
						InputStream inputStream = new ByteArrayInputStream(getStandardStyleContent().getBytes()); // uses default charset
						standardCSSFile.create(inputStream, false, null);
						inputStream.close();
					}
				}
				InputStream inputStream = new ByteArrayInputStream(cssContent.getBytes()); // uses default charset
				solutionCSSFile.create(inputStream, false, null);
				inputStream.close();
				EditorUtil.openFileEditor(solutionCSSFile);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			return false;
		}
		return true;

	}

	public class NewStyleWizardPage extends WizardPage implements Listener, IValidator
	{
		private String styleName;
		private Text styleNameField;
		private Text pathField;
		private String path;
		private Combo solutionCombo;
		private String solution;

		/**
		 * Creates a new style creation wizard page.
		 *
		 * @param pageName the name of the page
		 * @param defaultProjectToUse the initially selected resource project in which the style should be created.
		 */
		public NewStyleWizardPage(String pageName, ServoyProject defaultProjectToUse)
		{
			super(pageName);
			setTitle("Create a new style");
			setDescription("- a new style will be created");
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

			Composite container = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 2;

			GridData data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.widthHint = 100;
			data.grabExcessVerticalSpace = false;
			Label styleLabel = new Label(container, SWT.NONE);
			styleLabel.setText("Style name");
			styleLabel.setLayoutData(data);

			data = new GridData(GridData.FILL_BOTH);
			data.widthHint = 200;
			data.grabExcessVerticalSpace = false;
			styleNameField = new Text(container, SWT.BORDER);
			styleNameField.setLayoutData(data);
			styleNameField.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					handleStyleNameChanged();
				}
			});

			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.widthHint = 100;
			data.grabExcessVerticalSpace = false;
			Label solutionLabel = new Label(container, SWT.NONE);
			solutionLabel.setText("Solution");
			solutionLabel.setLayoutData(data);

			data = new GridData(GridData.FILL_BOTH);
			data.widthHint = 200;
			data.grabExcessVerticalSpace = false;
			solutionCombo = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			solutionCombo.setLayoutData(data);
			ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			String[] items = new String[projects.length];
			int i = 0;
			int selected = -1;
			for (ServoyProject project : projects)
			{
				if (defaultProjectToUse.getSolution().getName().equals(project.getSolution().getName())) selected = i;
				items[i++] = project.getSolution().getName();
			}
			solutionCombo.setItems(items);
			solutionCombo.select(selected);
			solutionCombo.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					handleStyleNameChanged();
				}
			});

			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.widthHint = 100;
			data.grabExcessVerticalSpace = false;
			Label pathLabel;
			pathLabel = new Label(container, SWT.NONE);
			pathLabel.setText("Path under media");
			pathLabel.setLayoutData(data);

			data = new GridData(GridData.FILL_BOTH);
			data.widthHint = 200;
			data.grabExcessVerticalSpace = false;
			pathField = new Text(container, SWT.BORDER);
			pathField.setLayoutData(data);
			pathField.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					handleStyleNameChanged();
				}
			});

			setControl(container);
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
			styleName = styleNameField.getText().trim();
			path = pathField.getText().trim();
			solution = solutionCombo.getText().trim();
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
					String pathString = solution + "/" + SolutionSerializer.MEDIAS_DIR + "/" + path;
					if (path.length() > 0) pathString += "/";
					final String regex = "[_a-zA-Z0-9\\-\\.]+";
					Pattern pattern = Pattern.compile(regex);
					if (!pattern.matcher(styleName).matches())
					{
						error = "Invalid new media name";
					}
					String fileName = styleName + ".css";
					IPath fullPath = new Path(pathString + fileName);
					if (ResourcesPlugin.getWorkspace().getRoot().getFile(fullPath).exists())
					{
						error = "A file with this name already exists. Please modify the file name.";
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
		private Button useSampleContentButton;

		private boolean useStandardContent = true;
		private Button useStandardContentButton;

		private Text textArea;

		/**
		 * Creates a new style creation wizard page.
		 *
		 * @param pageName the name of the page
		 */
		public NewStyleContentPage(String pageName)
		{
			super(pageName);
			setTitle("Create a new stylesheet");
			setDescription("- choose the initial content of the new stylesheet");
		}

		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);

			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);

			// use sample check box
			useSampleContentButton = new Button(topLevel, SWT.CHECK);
			useSampleContentButton.setText("Fill new stylesheet with sample content");
			useSampleContentButton.setSelection(true);
			useSampleContentButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					sampleCheckboxChanged();
				}
			});

			// use standard check box
			useStandardContentButton = new Button(topLevel, SWT.CHECK);
			useStandardContentButton.setText("Create (if needed) and reference 'standard_ngclient.css' it in new stylesheet");
			useStandardContentButton.setToolTipText(
				"Rules that will be helpful in most cases are set in 'standard_ngclient.css'.\nThey are set here and not in a predefined css file so that they can be edited/removed easier.\nLeave this checked if you are unsure what to do.");
			useStandardContentButton.setSelection(true);
			useStandardContentButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					standardCheckboxChanged();
				}
			});

			// text area
			textArea = new Text(topLevel, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
			updateTextArea();
			textArea.setEditable(false);

			// layout of the page
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 20;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			topLevel.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(0, 0);
			useSampleContentButton.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(useSampleContentButton, 0);
			formData.bottom = new FormAttachment(useStandardContentButton, 0);
			formData.right = new FormAttachment(100, 0);
			formData.height = 100;
			textArea.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.bottom = new FormAttachment(100, 0);
			useStandardContentButton.setLayoutData(formData);
		}

		protected void sampleCheckboxChanged()
		{
			useSampleContent = useSampleContentButton.getSelection();
			updateTextArea();
		}

		protected void standardCheckboxChanged()
		{
			useStandardContent = useStandardContentButton.getSelection();
			updateTextArea();
		}

		private void updateTextArea()
		{
			textArea.setEnabled(useSampleContent || useStandardContent);
			StringBuffer sb = new StringBuffer();
			if (useStandardContent) sb.append(IMPORT_STANDARD_NGCLIENT_CSS);
			if (useSampleContent) sb.append(getSampleStyleContent());

			textArea.setText(sb.toString());
		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			if (visible)
			{
				useSampleContentButton.setFocus();
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

	public String getMediaName()
	{
		return page1.styleName + ".css";
	}

	public String getPath()
	{
		return page1.path;
	}

	public String getSolution()
	{
		return page1.solution;
	}

}