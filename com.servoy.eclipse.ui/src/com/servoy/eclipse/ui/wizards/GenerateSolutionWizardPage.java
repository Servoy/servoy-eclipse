/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.IValidator;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectChooserComposite;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author emera
 */
public class GenerateSolutionWizardPage extends WizardPage implements ICheckBoxView, Listener, IValidator
{

	private String solutionName;
	private Text solutionNameField;

	private CheckboxTableViewer checkboxTableViewer;
	private boolean allChecked;
	private ExpandItem collapsableItem;
	private Composite topLevel;
	private SolutionAdvancedSettingsComposite expandComposite;
	private ExpandBar expandBar;

	private Boolean addDefaultTheme;
	private int[] solutionTypeComboValues;
	private int solutionType;
	private Label configLabel;
	private NewSolutionWizard wizard;

	protected static final String IS_ADVANCED_USER_SETTING = "is_advanced_user";
	protected static final String SELECTED_SOLUTIONS_SETTING = "selected_solutions";
	protected static final String SHOULD_ADD_DEFAULT_THEME_SETTING = "should_add_default_theme";
	protected static final String NO_RESOURCE_PROJECT_SETTING = "no_resource_project";
	protected static final String RESOURCE_PROJECT_NAME_SETTING = "resource_project_name";
	protected static final String SOLUTION_TYPE = "solution_type";

	private static final String SECURITY = "security";
	private static final String UTILS = "utils";
	private static final String SEARCH = "search";
	private static final String NAVIGATION = "navigation";

	private static String[] toImport = new String[] { SECURITY, UTILS, SEARCH, NAVIGATION };
	private final static com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	protected GenerateSolutionWizardPage(String pageName)
	{
		super(pageName);
		setTitle("Create a new solution");
		setDescription("- a new Servoy solution project will be created");
	}


	@Override
	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		this.wizard = (NewSolutionWizard)getWizard();
		boolean isModuleWizard = wizard.shouldAddAsModule(null);

		// top level group
		topLevel = new Composite(parent, SWT.NONE);
		setControl(topLevel);

		// solution name UI
		Label solutionLabel = new Label(topLevel, SWT.NONE);
		solutionLabel.setText("Solution name ");

		solutionNameField = new Text(topLevel, SWT.BORDER);
		solutionNameField.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
		solutionNameField.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				handleSolutionNameChanged();
			}
		});

		Composite tableContainer = addConfigureModules(isModuleWizard);
		addAdvancedSettings(isModuleWizard);

		// layout of the page
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.marginTop = gridLayout.marginBottom = gridLayout.marginLeft = gridLayout.marginRight = 20;
		gridLayout.verticalSpacing = 10;
		topLevel.setLayout(gridLayout);
		topLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		solutionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		solutionNameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		if (!isModuleWizard)
		{
			configLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 6));
		}
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gridData.minimumWidth = 650;//otherwise the resources project text is not visible
		expandBar.setLayoutData(gridData);
	}


	public void addAdvancedSettings(boolean isModuleWizard)
	{
		expandBar = new ExpandBar(topLevel, SWT.NONE);
		expandBar.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		expandBar.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		expandComposite = new SolutionAdvancedSettingsComposite(expandBar, isModuleWizard);
		collapsableItem = new ExpandItem(expandBar, SWT.NONE, 0);
		collapsableItem.setHeight(expandComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		collapsableItem.setControl(expandComposite);
		expandBar.addExpandListener(new ExpandListener()
		{
			public void itemExpanded(ExpandEvent e)
			{
				collapsableItem.setText("Hide advanced solution settings");
				collapsableItem.setImage(uiActivator.loadImageFromBundle("collapse_tree.png"));
				resizeDialog();
			}

			public void itemCollapsed(ExpandEvent e)
			{
				collapsableItem.setText("Show advanced solution settings");
				collapsableItem.setImage(uiActivator.loadImageFromBundle("expandall.png"));
				resizeDialog();
			}
		});

		collapsableItem.setExpanded(getWizard().getDialogSettings().getBoolean(wizard.getSettingsPrefix() + IS_ADVANCED_USER_SETTING));
		if (collapsableItem.getExpanded())
		{
			Shell shell = getWizard().getContainer().getShell();
			shell.getDisplay().asyncExec(() -> {
				//center dialog vertically
				Rectangle parentSize = shell.getParent().getBounds();
				Rectangle bounds = shell.getBounds();
				bounds.y = (parentSize.height - bounds.height) / 2 + parentSize.y;
				shell.setBounds(bounds);
			});
		}
		collapsableItem.setText(collapsableItem.getExpanded() ? "Hide advanced solution settings" : "Show advanced solution settings");
		collapsableItem.setImage(uiActivator.loadImageFromBundle(collapsableItem.getExpanded() ? "collapse_tree.png" : "expandall.png"));

	}


	public Composite addConfigureModules(boolean isModuleWizard)
	{
		configLabel = new Label(topLevel, SWT.NONE);
		configLabel.setText("Configure the new solution with ");
		configLabel.setVisible(!isModuleWizard);

		Composite tableContainer = new Composite(topLevel, SWT.NONE);
		tableContainer.setVisible(!isModuleWizard);

		Table table = new Table(tableContainer, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		checkboxTableViewer = new CheckboxTableViewer(table);
		if (!isModuleWizard)
		{
			checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
			checkboxTableViewer.setInput(toImport);
			if (getDialogSettings().get(wizard.getSettingsPrefix() + SELECTED_SOLUTIONS_SETTING) != null)
			{
				String[] solutions = getDialogSettings().get(wizard.getSettingsPrefix() + SELECTED_SOLUTIONS_SETTING).trim().split(" ");
				if (solutions.length > 0) checkboxTableViewer.setCheckedElements(solutions);
			}
			else
			{
				checkboxTableViewer.setAllChecked(true);
			}

			allChecked = checkboxTableViewer.getCheckedElements().length == toImport.length;
			TableViewerColumn col = new TableViewerColumn(checkboxTableViewer, SWT.LEAD);
			checkboxTableViewer.getTable().setHeaderBackground(topLevel.getBackground());
			col.getColumn().setText("Select/Deselect All");
			col.getColumn().setImage(uiActivator.loadImageFromBundle(allChecked ? "check_on.png" : "check_off.png"));
			col.getColumn().setToolTipText(allChecked ? "Deselect all" : "Select all");
			col.getColumn().pack();

			col.getColumn().addListener(SWT.Selection, e -> {
				allChecked = !allChecked;
				col.getColumn().setImage(uiActivator.loadImageFromBundle(allChecked ? "check_on.png" : "check_off.png"));
				col.getColumn().setToolTipText(allChecked ? "Deselect all" : "Select all");
				checkboxTableViewer.setAllChecked(allChecked);
				setPageComplete(validatePage());
			});
			final TableColumnLayout layout = new TableColumnLayout();
			tableContainer.setLayout(layout);
			layout.setColumnData(col.getColumn(), new ColumnWeightData(15, 50, true));
			checkboxTableViewer.getTable().setHeaderVisible(true);
			checkboxTableViewer.addCheckStateListener(e -> setPageComplete(validatePage()));
		}
		return tableContainer;
	}

	/**
	 * Returns the name of the new solution.
	 *
	 * @return the name of the new solution.
	 */
	public String getNewSolutionName()
	{
		return solutionName;
	}

	private void handleSolutionNameChanged()
	{
		solutionName = solutionNameField.getText();
		setPageComplete(validatePage());
	}

	protected boolean validatePage()
	{
		setMessage(null, NONE);
		HashMap<String, List<String>> servers = null;
		if (!wizard.canCreateMissingServers() && !(servers = NewSolutionWizard.searchMissingServers(getSolutionsToImport())).isEmpty())
		{
			StringBuilder message = new StringBuilder();
			for (String server : servers.keySet())
			{
				message.append("Database Server '" + server + "', required by the module(s) " + servers.get(server).stream().collect(Collectors.joining(",")) +
					" is missing.\n");
			}
			message.append("Deselect not needed modules or press Cancel, create the databases and fill in the connection details.");
			setMessage(message.toString(), WARNING);
			getWizard().getContainer().updateButtons();
			return false;
		}

		String error = null;
		if (solutionNameField.getText().trim().length() == 0)
		{
			error = "Please give a name for the new solution";
		}
		if (error == null)
		{
			// see if solution name is OK
			if (!IdentDocumentValidator.isJavaIdentifier(solutionName))
			{
				error = "Solution name has unsupported characters";
			}
			else if (solutionName.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH)
			{
				error = "Solution name is too long";
			}
			else if (ServoyModel.getWorkspace().getRoot().getProject(solutionName).exists())
			{
				error = "A project with this name already exists in the workspace";
			}
			else
			{
				IStatus validationResult = ServoyModel.getWorkspace().validateName(solutionName, IResource.PROJECT);
				if (!validationResult.isOK())
				{
					error = "The name of the solution project to be created is not valid: " + validationResult.getMessage();
				}
			}
		}

		setErrorMessage(error);
		getWizard().getContainer().updateButtons();
		return error == null;
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			solutionNameField.setFocus();
			setPageComplete(validatePage());
		}
	}

	public boolean isChecked(String config)
	{
		return checkboxTableViewer.getChecked(config);
	}

	public List<String> getSolutionsToImport()
	{
		List<String> result = new ArrayList<>();
		if (solutionType == SolutionMetaData.NG_CLIENT_ONLY && checkboxTableViewer.getCheckedElements().length > 0)
		{
			if (isChecked(SECURITY))
			{
				result.add("svyProperties");
				result.add("svySecurity");
			}
			if (isChecked(UTILS) || isChecked(SEARCH) || isChecked(NAVIGATION) || isChecked(SECURITY)) result.add("svyUtils");
			if (isChecked(SEARCH)) result.add("svySearch");
			if (isChecked(NAVIGATION))
			{
				result.add("svyNavigation");
				result.add("svyNavigationUX");
			}
		}
		return result;
	}

	public List<String> getWebPackagesToImport()
	{
		if (solutionType == SolutionMetaData.NG_CLIENT_ONLY)
		{
			if (isChecked(NAVIGATION))
			{
				return Arrays.asList(NewSolutionWizardDefaultPackages.PACKAGES);
			}
			else
			{
				return Arrays.stream(NewSolutionWizardDefaultPackages.PACKAGES).filter(pack -> !"bootstrapextracomponents".equals(pack)).collect(
					Collectors.toList());
			}
		}
		return null;
	}

	@Override
	public IWizardPage getNextPage()
	{
		return null;
	}


	@Override
	public void selectAll()
	{
		checkboxTableViewer.setAllChecked(true);
	}

	@Override
	public void deselectAll()
	{
		checkboxTableViewer.setAllChecked(false);
	}

	public boolean mustAuthenticate()
	{
		return isChecked(SECURITY);
	}

	public boolean isAdvancedUser()
	{
		return collapsableItem.getExpanded();
	}

	public String getSelectedSolutions()
	{
		return Arrays.stream(toImport).filter(sol -> isChecked(sol)).reduce("", (res, name) -> res + name + " ");
	}

	private void resizeDialog()
	{
		Shell shell = getWizard().getContainer().getShell();
		shell.getDisplay().asyncExec(() -> {

			Point preferredSize = shell.computeSize(shell.getSize().x, SWT.DEFAULT, true);
			if (((WizardDialog)getWizard().getContainer()).getTray() != null && !collapsableItem.getExpanded())
			{
				//if the help page is visible, we don't shrink
				return;
			}

			Rectangle bounds = shell.getBounds();
			bounds.height = preferredSize.y;
			if (collapsableItem.getExpanded())
			{
				//when it is expanded we center the dialog, because sometimes it shows right on top
				Rectangle parentSize = shell.getParent().getBounds();
				bounds.y = (parentSize.height - bounds.height) / 2 + parentSize.y;
			}
			shell.setBounds(bounds);
			shell.layout(true, true);
		});
	}

	private final class SolutionAdvancedSettingsComposite extends Composite implements IValidator
	{
		private final Button defaultThemeCheck;
		private final Combo solutionTypeCombo;
		private final ProjectLocationComposite projectLocationComposite;
		private final ResourcesProjectChooserComposite resourceProjectComposite;

		public SolutionAdvancedSettingsComposite(Composite parent, boolean isModule)
		{
			super(parent, SWT.NONE);
			defaultThemeCheck = new Button(this, SWT.CHECK);
			defaultThemeCheck.setVisible(!isModule);
			if (!isModule)
			{
				defaultThemeCheck.setText("Add default servoy .less theme (configurable by a less properties file)");
				addDefaultTheme = (getDialogSettings().get(wizard.getSettingsPrefix() + SHOULD_ADD_DEFAULT_THEME_SETTING) != null)
					? Boolean.valueOf(getDialogSettings().get(wizard.getSettingsPrefix() + SHOULD_ADD_DEFAULT_THEME_SETTING)) : Boolean.TRUE;//ng solution is selected by default
				defaultThemeCheck.setSelection(addDefaultTheme.booleanValue());
				defaultThemeCheck.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						addDefaultTheme = Boolean.valueOf(defaultThemeCheck.getSelection());
					}
				});
			}

			// solution type
			Label solutionTypeLabel = new Label(this, SWT.NONE);
			solutionTypeLabel.setText("Solution type");
			solutionTypeCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
			String[] solutionTypeNames = new String[SolutionMetaData.solutionTypeNames.length - 1];
			System.arraycopy(SolutionMetaData.solutionTypeNames, 1, solutionTypeNames, 0, solutionTypeNames.length);
			solutionTypeCombo.setItems(solutionTypeNames);
			solutionTypeComboValues = new int[SolutionMetaData.solutionTypes.length - 1];
			System.arraycopy(SolutionMetaData.solutionTypes, 1, solutionTypeComboValues, 0, solutionTypeComboValues.length);
			int defaultSolutionType = getDialogSettings().get(wizard.getSettingsPrefix() + SOLUTION_TYPE) != null
				? getDialogSettings().getInt(wizard.getSettingsPrefix() + SOLUTION_TYPE) : SolutionMetaData.NG_CLIENT_ONLY;
			solutionTypeCombo.select(
				IntStream.range(0, solutionTypeComboValues.length).filter(i -> defaultSolutionType == solutionTypeComboValues[i]).findFirst().getAsInt());
			handleSolutionTypeComboSelected();
			solutionTypeCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					handleSolutionTypeComboSelected();
				}
			});

			projectLocationComposite = new ProjectLocationComposite(this, SWT.NONE, this.getClass().getName());

			resourceProjectComposite = new ResourcesProjectChooserComposite(this, SWT.NONE, this,
				"Please choose the resources project this solution will reference (for styles, column/sequence info, security)",
				ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject(), false, !isModule);

			// layout of the page
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 5;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			this.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(solutionTypeLabel, 0, SWT.LEFT);
			formData.top = new FormAttachment(solutionTypeLabel, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			solutionTypeCombo.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(solutionTypeCombo, 20);
			projectLocationComposite.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(projectLocationComposite, 30);
			resourceProjectComposite.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(resourceProjectComposite, 20);
			formData.bottom = new FormAttachment(100, 0);
			defaultThemeCheck.setLayoutData(formData);

			this.pack();

			if (getDialogSettings().get(wizard.getSettingsPrefix() + RESOURCE_PROJECT_NAME_SETTING) != null)
			{
				resourceProjectComposite.setResourceProjectName(getDialogSettings().get(wizard.getSettingsPrefix() + RESOURCE_PROJECT_NAME_SETTING));
			}

			if (getDialogSettings().getBoolean(wizard.getSettingsPrefix() + NO_RESOURCE_PROJECT_SETTING))
			{
				resourceProjectComposite.selectNoResourceProject();
			}
		}

		@Override
		public String validate()
		{
			if (resourceProjectComposite != null)
			{
				String error = resourceProjectComposite.validate();
				if (error == null && resourceProjectComposite.getNewResourceProjectName() != null &&
					(resourceProjectComposite.getNewResourceProjectName().equalsIgnoreCase(getNewSolutionName())))
				{
					error = "You cannot give the same name to the solution and the resource project to be created";
				}
			}
			setPageComplete(validatePage());
			return getErrorMessage();
		}

		private void handleSolutionTypeComboSelected()
		{
			solutionType = solutionTypeComboValues[solutionTypeCombo.getSelectionIndex()];
			defaultThemeCheck.setEnabled(SolutionMetaData.isNGOnlySolution(solutionType));
			defaultThemeCheck.setSelection(shouldAddDefaultTheme());
			checkboxTableViewer.getTable().setEnabled(solutionType == SolutionMetaData.NG_CLIENT_ONLY);
		}

		public void setSolutionTypes(int[] solTypes, int selected, boolean fixedType)
		{
			String[] solutionTypeNames = new String[solTypes.length];

			for (int j = 0; j < solTypes.length; j++)
			{
				for (int i = 0; i < SolutionMetaData.solutionTypes.length; i++)
				{
					if (SolutionMetaData.solutionTypes[i] == solTypes[j])
					{
						solutionTypeNames[j] = SolutionMetaData.solutionTypeNames[i];
						break;
					}
				}
			}

			solutionTypeComboValues = solTypes;
			solutionTypeCombo.setItems(solutionTypeNames);
			solutionTypeCombo.select(selected);

			handleSolutionTypeComboSelected();
			if (fixedType)
			{
				solutionTypeCombo.setEnabled(false);
			}
		}

		public String getProjectLocation()
		{
			return projectLocationComposite.getProjectLocation();
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
	}


	public String validate()
	{
		setPageComplete(validatePage());
		return getErrorMessage();
	}

	public boolean shouldAddDefaultTheme()
	{
		return addDefaultTheme != null ? (addDefaultTheme.booleanValue() && solutionType == SolutionMetaData.NG_CLIENT_ONLY)
			: solutionType == SolutionMetaData.NG_CLIENT_ONLY;
	}

	public void handleEvent(Event event)
	{
		setPageComplete(validatePage());
	}


	public ResourcesProjectChooserComposite getResourceProjectData()
	{
		return expandComposite.getResourceProjectData();
	}

	public String getProjectLocation()
	{
		return expandComposite.getProjectLocation();
	}


	public int getSolutionType()
	{
		return solutionType;
	}

	public void setSolutionTypes(int[] solutiontypes, int selected, boolean fixedType)
	{
		expandComposite.setSolutionTypes(solutiontypes, selected, fixedType);

	}

	@Override
	public void performHelp()
	{
		boolean focusNameField = solutionNameField.isFocusControl();
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(wizard.getHelpContextID());
		if (focusNameField)
		{
			solutionNameField.setFocus();
		}
	}
}
