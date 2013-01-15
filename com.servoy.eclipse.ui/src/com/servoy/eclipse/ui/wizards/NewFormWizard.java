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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IStatusChangedListener;
import com.servoy.eclipse.ui.util.SnapToGridFieldPositioner;
import com.servoy.eclipse.ui.views.PlaceFieldOptionGroup;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Create new form.
 * 
 * @author rgansevles
 */
public class NewFormWizard extends Wizard implements INewWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewFormWizard"; //$NON-NLS-1$

	private static Object SELECTION_NONE = new Object();

	private NewFormWizardPage newFormWizardPage;

	private DataProviderWizardPage dataProviderWizardPage;

	private WizardPage errorPage;

	private ServoyProject servoyProject;

	public NewFormWizard()
	{
		setWindowTitle("New form");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages()
	{
		if (errorPage != null)
		{
			addPage(errorPage);
		}
		else
		{
			// add pages to this wizard
			addPage(newFormWizardPage);
			if (dataProviderWizardPage != null) addPage(dataProviderWizardPage);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		Form selectedForm = null;
		TableWrapper selectedDataSource = null;
		// find the Servoy project to which the new form will be added
		if (selection.size() == 1)
		{
			Object selected = selection.getFirstElement();
			if (selected instanceof IResource)
			{
				IProject project = ((IResource)selected).getProject();
				servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(project.getName());
			}
			else if (selected instanceof SimpleUserNode)
			{
				SimpleUserNode node = ((SimpleUserNode)selected);
				if (node.getRealObject() instanceof Form)
				{
					selectedForm = (Form)node.getRealObject();
				}
				else if (node.getRealObject() instanceof TableWrapper)
				{
					selectedDataSource = (TableWrapper)node.getRealObject();
				}
				SimpleUserNode projectNode = node.getAncestorOfType(ServoyProject.class);
				if (projectNode != null)
				{
					servoyProject = (ServoyProject)projectNode.getRealObject();
				}
			}
		}
		if (servoyProject == null)
		{
			// use active project if any
			servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		}

		dataProviderWizardPage = null;
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
			errorPage.setErrorMessage("Please activate a Servoy solution project before trying to create a new form");
			errorPage.setPageComplete(false);
			newFormWizardPage = null;
		}
		else
		{
			// create pages for this wizard
			if (selectedDataSource != null)
			{
				newFormWizardPage = new NewFormWizardPage("New form", selectedDataSource);
			}
			else
			{
				newFormWizardPage = new NewFormWizardPage("New form", selectedForm);
			}
			if (servoyProject.getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE)
			{
				dataProviderWizardPage = new DataProviderWizardPage("Data Providers");
			}
		}
	}

	@Override
	public IDialogSettings getDialogSettings()
	{
		return EditorUtil.getDialogSettings("newFormWizard"); //$NON-NLS-1$  
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	@Override
	public boolean performFinish()
	{
		IDialogSettings settings = getDialogSettings();
		if (dataProviderWizardPage != null)
		{
			settings.put("placeHorizontal", dataProviderWizardPage.optionsGroup.isPlaceHorizontal()); //$NON-NLS-1$
			settings.put("placeAsLabels", dataProviderWizardPage.optionsGroup.isPlaceAsLabels()); //$NON-NLS-1$
			settings.put("placeLabels", dataProviderWizardPage.optionsGroup.isPlaceWithLabels()); //$NON-NLS-1$
			settings.put("fillText", dataProviderWizardPage.optionsGroup.isFillText()); //$NON-NLS-1$
			settings.put("fillName", dataProviderWizardPage.optionsGroup.isFillName()); //$NON-NLS-1$
		}

		TableWrapper tw = newFormWizardPage.getTableWrapper();
		settings.put("servername", tw == null ? null : tw.getServerName()); //$NON-NLS-1$
		settings.put("tablename", tw == null ? null : tw.getTableName()); //$NON-NLS-1$
		Style style = newFormWizardPage.getStyle();
		settings.put("style", style == null ? null : style.getName()); //$NON-NLS-1$
		Template template = newFormWizardPage.getTemplate();
		settings.put("templatename", template == null ? null : template.getName()); //$NON-NLS-1$

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();
		if (activeProject == null)
		{
			return false;
		}

		// create the new form
		if (servoyProject.getEditingSolution() == null)
		{
			MessageDialog.openError(getShell(), "Cannot create form",
				"Cannot get the Servoy Solution from the selected Servoy Project.\nThe form will not be created.");
			return false;
		}

		try
		{
			// create empty form
			String dataSource = DataSourceUtils.createDBTableDataSource(newFormWizardPage.getServerName(), newFormWizardPage.getTableName());
			Form form = servoyProject.getEditingSolution().createNewForm(servoyModel.getNameValidator(), style, newFormWizardPage.getFormName(), dataSource,
				true, null);
			// use superform selected by user
			Form superForm = newFormWizardPage.getSuperForm();

			if (template == null)
			{
				// create default form, most is already set in createNewForm
				if (superForm == null) form.createNewPart(Part.BODY, 480/* height */); // else the form just inherits parts from super; no need to add body
			}
			else
			{
				// create form from template
				ElementFactory.applyTemplate(form, new TemplateElementHolder(template), null, true);

				// set overridden values, selected by the user
				form.setName(newFormWizardPage.getFormName());
				form.setDataSource(dataSource);
				form.setStyleName(style == null ? null : style.getName());
			}

			if (servoyProject.getSolution().getSolutionType() == SolutionMetaData.MOBILE)
			{
				// mobile solution, make the form mobile
				form.putCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName, Boolean.TRUE);
				form.setStyleName("_servoy_mobile"); // set internal style name
			}

			if (superForm != null && template == null)
			{
				form.clearProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
				form.clearProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU.getPropertyName());
			}
			if (superForm != null) form.setExtendsID(superForm.getID());
			// add selected data providers
			DesignerPreferences designerPreferences = new DesignerPreferences();
			if (dataProviderWizardPage != null)
			{
				Object[] dataProviders = dataProviderWizardPage.getDataProviders();
				if (dataProviders != null && dataProviders.length > 0)
				{
					ElementFactory.createFields(form, dataProviders, dataProviderWizardPage.optionsGroup.isPlaceAsLabels(),
						dataProviderWizardPage.optionsGroup.isPlaceWithLabels(), dataProviderWizardPage.optionsGroup.isPlaceHorizontal(),
						dataProviderWizardPage.optionsGroup.isFillText(), dataProviderWizardPage.optionsGroup.isFillName(), designerPreferences.getGridSnapTo()
							? new SnapToGridFieldPositioner(designerPreferences) : null, dataProviderWizardPage.optionsGroup.isPlaceHorizontal() ? new Point(0,
							0) : new Point(60, 70));

					if (dataProviderWizardPage.optionsGroup.isPlaceHorizontal())
					{
						form.setView(FormController.LOCKED_TABLE_VIEW);
					}
				}
			}

			if (form.getNavigatorID() == Form.NAVIGATOR_DEFAULT && !designerPreferences.getShowNavigatorDefault()) form.setNavigatorID(Form.NAVIGATOR_NONE);

			if (form.getExtendsForm() == null)
			{
				form.setEncapsulation(designerPreferences.getEncapsulationType());
			}

			// save
			servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true);

			servoyModel.getDataModelManager().testTableAndCreateDBIFile(form.getTable());

			// open newly created form in the editor (as new editor)
			return EditorUtil.openFormDesignEditor(form, true, true) != null;
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
			return false;
		}
	}

	public class NewFormWizardPage extends WizardPage implements Listener
	{
		private TreeSelectViewer extendsFormViewer;

		private Text formNameField;

		private TreeSelectViewer dataSourceViewer;

		private ComboViewer styleNameCombo;

		private ComboViewer templateNameCombo;

		private ComboViewer projectCombo;

		private String formName;
		private boolean formNameTyped;

		private Style style;

		private Form superForm;

		/**
		 * Creates a new form creation wizard page.
		 * 
		 * @param pageName the name of the page
		 */
		public NewFormWizardPage(String pageName)
		{
			super(pageName);
			setTitle("Create a new form");
			setDescription(getTitle());
			setDialogSettings(Activator.getDefault().getDialogSettings());
		}

		/**
		 * Creates a new form creation wizard page.
		 * 
		 * @param pageName the name of the page
		 * @param superForm the preselected super form
		 */
		public NewFormWizardPage(String pageName, Form superForm)
		{
			this(pageName);
			this.superForm = superForm;
		}

		/**
		 * Creates a new form creation wizard page.
		 * 
		 * @param pageName the name of the page
		 * @param superForm the pre selected super form
		 */
		public NewFormWizardPage(String pageName, TableWrapper tableWrapper)
		{
			this(pageName);
			IDialogSettings settings = NewFormWizard.this.getDialogSettings();
			settings.put("servername", tableWrapper.getServerName()); //$NON-NLS-1$
			settings.put("tablename", tableWrapper.getTableName()); //$NON-NLS-1$
		}

		private TableWrapper getTableWrapper()
		{
			IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
			if (selection.isEmpty())
			{
				return new TableWrapper(null, null);
			}
			Object sel = selection.getFirstElement();
			if (TableContentProvider.TABLE_NONE.equals(sel))
			{
				return null;
			}
			return (TableWrapper)sel;
		}

		public String getTableName()
		{
			TableWrapper tw = getTableWrapper();
			return tw == null ? null : tw.getTableName();
		}

		public String getServerName()
		{
			TableWrapper tw = getTableWrapper();
			return tw == null ? null : tw.getServerName();
		}

		public String getFormName()
		{
			return formName;
		}

		public Style getStyle()
		{
			return style;
		}

		public Template getTemplate()
		{
			Object firstElement = ((IStructuredSelection)templateNameCombo.getSelection()).getFirstElement();
			if (firstElement instanceof Template)
			{
				return (Template)firstElement;
			}
			return null;
		}

		public Form getSuperForm()
		{
			return superForm;
		}

		/**
		 * (non-Javadoc) Method declared on IDialogPage.
		 */
		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);
			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

			Label formNameLabel;
			formNameLabel = new Label(topLevel, SWT.NONE);
			formNameLabel.setText("&Form name");
			formNameField = new Text(topLevel, SWT.BORDER);
			formNameField.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
			formNameField.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					handleFormNameChanged();
				}
			});

			setControl(topLevel);

			Label datasourceLabel = new Label(topLevel, SWT.NONE);
			datasourceLabel.setText("&Datasource");

			dataSourceViewer = new TreeSelectViewer(topLevel, SWT.NONE)
			{
				@Override
				protected IStructuredSelection openDialogBox(Control control)
				{
					if (superForm == null || superForm.getDataSource() == null)
					{
						return super.openDialogBox(control);
					}
					MessageDialog.openInformation(control.getShell(), "Cannot change data source",
						"The new form and the extends form must be based on the same data source");
					return null;
				}
			};

			dataSourceViewer.setContentProvider(new TableContentProvider());
			dataSourceViewer.setLabelProvider(DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY);
			dataSourceViewer.setTextLabelProvider(new DatasourceLabelProvider(Messages.LabelSelect, false, true));
			dataSourceViewer.addStatusChangedListener(new IStatusChangedListener()
			{
				public void statusChanged(boolean valid)
				{
					setPageComplete(validatePage());
				}
			});
			dataSourceViewer.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleDataSourceSelected();
				}
			});
			dataSourceViewer.setInput(new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, true));
			dataSourceViewer.setEditable(!SolutionMetaData.isServoyMobileSolution(getActiveSolution()));
			Control dataSOurceControl = dataSourceViewer.getControl();

			Label extendsLabel = new Label(topLevel, SWT.NONE);
			extendsLabel.setText("E&xtends");
			extendsLabel.setEnabled(!SolutionMetaData.isServoyMobileSolution(getActiveSolution()));

			extendsFormViewer = new TreeSelectViewer(topLevel, SWT.NONE);
			extendsFormViewer.setTitleText("Select super form");

			updateExtendsFormViewer(servoyProject.getEditingFlattenedSolution());
			extendsFormViewer.addStatusChangedListener(new IStatusChangedListener()
			{
				public void statusChanged(boolean valid)
				{
					setPageComplete(validatePage());
				}
			});
			extendsFormViewer.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleExtendsFormChanged();
				}
			});

			extendsFormViewer.setEnabled(!SolutionMetaData.isServoyMobileSolution(getActiveSolution()));
			extendsFormViewer.setEditable(!SolutionMetaData.isServoyMobileSolution(getActiveSolution()));
			Control extendsFormControl = extendsFormViewer.getControl();

			Label styleLabel = new Label(topLevel, SWT.NONE);
			styleLabel.setText("St&yle");
			styleLabel.setEnabled(!SolutionMetaData.isServoyMobileSolution(getActiveSolution()));

			styleNameCombo = new ComboViewer(topLevel, SWT.BORDER | SWT.READ_ONLY);
			styleNameCombo.setContentProvider(new ArrayContentProvider());
			styleNameCombo.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleStyleSelected();
				}
			});
			Combo styleNameComboControl = styleNameCombo.getCombo();
			styleNameComboControl.setEnabled(!SolutionMetaData.isServoyMobileSolution(getActiveSolution()));

			Label templateLabel = new Label(topLevel, SWT.NONE);
			templateLabel.setText("T&emplate");

			templateNameCombo = new ComboViewer(topLevel, SWT.BORDER | SWT.READ_ONLY);
			templateNameCombo.setContentProvider(new ArrayContentProvider());
			templateNameCombo.setLabelProvider(new LabelProvider()
			{
				@Override
				public String getText(Object value)
				{
					if (value == SELECTION_NONE) return Messages.LabelNone;
					return super.getText(value);
				}
			});
			templateNameCombo.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					try
					{
						handleTemplateSelected(servoyProject.getEditingFlattenedSolution());
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			});
			Combo templateNameComboControl = templateNameCombo.getCombo();

			Label projectLabel = new Label(topLevel, SWT.NONE);
			projectLabel.setText("S&olution");

			projectCombo = new ComboViewer(topLevel, SWT.BORDER | SWT.READ_ONLY);
			projectCombo.setContentProvider(new ArrayContentProvider());
			Combo projectComboControl = projectCombo.getCombo();
			projectCombo.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleProjectSelected();
				}
			});


			final GroupLayout groupLayout = new GroupLayout(topLevel);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(formNameLabel).add(extendsLabel).add(datasourceLabel).add(projectLabel).add(
						styleLabel).add(templateLabel)).add(15, 15, 15).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(projectComboControl, GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE).add(
						templateNameComboControl, GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE).add(styleNameComboControl, GroupLayout.DEFAULT_SIZE, 159,
						Short.MAX_VALUE).add(extendsFormControl, GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE).add(dataSOurceControl,
						GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE).add(
						groupLayout.createSequentialGroup().add(formNameField, GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE).addPreferredGap(
							LayoutStyle.RELATED))).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(formNameLabel).add(formNameField, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(
						groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(dataSOurceControl)).add(datasourceLabel)).addPreferredGap(
					LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(extendsLabel).add(extendsFormControl, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(styleNameComboControl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(styleLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(templateNameComboControl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(templateLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(projectLabel).add(projectComboControl, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addContainerGap(142, Short.MAX_VALUE)));
			topLevel.setLayout(groupLayout);
			topLevel.setTabList(new Control[] { formNameField, dataSOurceControl, extendsFormControl, styleNameComboControl, templateNameComboControl, projectComboControl });

			if (superForm != null)
			{
				extendsFormViewer.setSelection(new StructuredSelection(Integer.valueOf(superForm.getID())));
			}
			else
			{
				extendsFormViewer.setSelection(new StructuredSelection(Integer.valueOf(Form.NAVIGATOR_NONE)));
			}
		}

		/**
		 * @param flattenedSolution
		 */
		public void updateExtendsFormViewer(final FlattenedSolution flattenedSolution)
		{
			extendsFormViewer.setContentProvider(new FormContentProvider(flattenedSolution, null));
			extendsFormViewer.setInput(new FormContentProvider.FormListOptions(FormListOptions.FormListType.FORMS, null, true, false, false));
			extendsFormViewer.setLabelProvider(new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedSolution, true),
				flattenedSolution.getSolution()));
		}

		/*
		 * @see DialogPage.setVisible(boolean)
		 */
		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			if (visible)
			{
				IDialogSettings settings = getDialogSettings();
				String serverName = settings.get("servername");
				String tableName = settings.get("tablename");
				if (!Utils.stringIsEmpty(serverName) && !Utils.stringIsEmpty(tableName) && dataSourceViewer.getSelection().isEmpty())
				{
					dataSourceViewer.setSelection(new StructuredSelection(new TableWrapper(serverName, tableName, EditorUtil.isViewTypeTable(serverName,
						tableName))));
				}

				String styleName = settings.get("style");
				if (!Utils.stringIsEmpty(styleName))
				{
					try
					{
						List<Style> styles = ServoyModel.getDeveloperRepository().getActiveRootObjects(IRepository.STYLES);
						for (Style s : styles)
						{
							if (s.getName().equals(styleName))
							{
								this.style = s;
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError("Could not get style list", e);
					}
				}
				setFormName(formName);
				fillStyleCombo();
				fillTemplateCombo(superForm == null ? settings.get("templatename") : null);
				fillProjectCombo();

				formNameField.setFocus();
				setPageComplete(validatePage());
			}
		}

		public void fillFormNameText()
		{
			formNameField.setText(formName == null ? "" : formName);
		}

		public void fillStyleCombo()
		{
			List<Object> styles = new ArrayList<Object>();
			try
			{
				styles = ServoyModel.getDeveloperRepository().getActiveRootObjects(IRepository.STYLES);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get style list", e);
			}
			styles.add(0, Messages.LabelDefault);
			styleNameCombo.setInput(styles);
			if (style != null)
			{
				styleNameCombo.setSelection(new StructuredSelection(style));
			}
			else
			{
				styleNameCombo.setSelection(new StructuredSelection(Messages.LabelDefault));
			}
		}

		public void fillProjectCombo()
		{
			ServoyProject[] modules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			projectCombo.setInput(modules);
			if (servoyProject != null)
			{
				projectCombo.setSelection(new StructuredSelection(servoyProject));
			}
		}

		public void fillTemplateCombo(String templateName)
		{
			List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
			List<Object> formTemplates = new ArrayList<Object>(templates.size() + 1);
			Object selected;
			formTemplates.add(selected = SELECTION_NONE);
			for (IRootObject template : templates)
			{
				try
				{
					// only select form templates
					if (template instanceof Template && new ServoyJSONObject(((Template)template).getContent(), false).has(Template.PROP_FORM))
					{
						formTemplates.add(template);
						if (template.getName().equals(templateName))
						{
							selected = template;
						}
					}
				}
				catch (JSONException e)
				{
					ServoyLog.logError("Could not read template '" + template.getName() + "'", e);
				}
			}
			templateNameCombo.setInput(formTemplates.toArray());
			templateNameCombo.setSelection(new StructuredSelection(selected));
		}

		/**
		 * Find all solutions that have a module reference to the project parameter.
		 * 
		 * @param project
		 * @return
		 */
		private ServoyProject[] getReferringProjects(Form form)
		{
			List<ServoyProject> referringProjects = new ArrayList<ServoyProject>();

			// get all modules of the active solution (related solutions)
			for (ServoyProject sp : ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject())
			{
				if (sp.getEditingFlattenedSolution().getForm(form.getID()) != null)
				{
					referringProjects.add(sp);
				}
			}

			return referringProjects.toArray(new ServoyProject[referringProjects.size()]);
		}

		private void handleDataSourceSelected()
		{
			IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
			if (!selection.isEmpty())
			{
				if (!formNameTyped)
				{
					TableWrapper tw = getTableWrapper();
					if (tw != null)
					{
						setFormName(tw.getTableName());
					}
				}
			}
			setPageComplete(validatePage());
		}

		private void handleExtendsFormChanged()
		{
			FlattenedSolution flattenedSolution = servoyProject.getEditingFlattenedSolution();
			IStructuredSelection selection = (IStructuredSelection)extendsFormViewer.getSelection();
			superForm = null;
			if (!selection.isEmpty())
			{
				Object formId = selection.getFirstElement();
				if (formId instanceof Integer)
				{
					superForm = flattenedSolution.getForm(((Integer)formId).intValue());
				}
			}

			if (superForm == null || superForm.getDataSource() == null)
			{
				dataSourceViewer.setEditable(true);
			}
			else
			{
				// if selected prefill table and style
				dataSourceViewer.setSelection(new StructuredSelection(new TableWrapper(superForm.getServerName(), superForm.getTableName(),
					EditorUtil.isViewTypeTable(superForm.getServerName(), superForm.getTableName()))));
				dataSourceViewer.setEditable(false);
				styleNameCombo.setSelection(new StructuredSelection(superForm.getStyleName() == null ? "" : superForm.getStyleName()));
			}
			dataSourceViewer.setButtonText((superForm == null || superForm.getDataSource() == null) ? TreeSelectViewer.DEFAULT_BUTTON_TEXT : "");
		}

		private void handleTemplateSelected(FlattenedSolution flattenedSolution) throws Exception
		{
			// copy a few properties from the template to the page
			Template template = getTemplate();
			if (template != null)
			{
				JSONObject json = new ServoyJSONObject(template.getContent(), false);
				if (json.has(Template.PROP_FORM))
				{
					IDeveloperRepository repository = ServoyModel.getDeveloperRepository();

					JSONObject formObject = json.getJSONObject(Template.PROP_FORM);

					// dataSource
					if (formObject.has(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName()))
					{
						String[] stn = DataSourceUtilsBase.getDBServernameTablename(formObject.getString(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName()));
						if (stn != null)
						{
							dataSourceViewer.setSelection(new StructuredSelection(new TableWrapper(stn[0], stn[1], EditorUtil.isViewTypeTable(stn[0], stn[1]))));
						}
					}

					// extendsFormID
					int extendsFormID = Form.NAVIGATOR_NONE;
					String formExtendsName = null;
					if (formObject.has(StaticContentSpecLoader.PROPERTY_EXTENDSFORMID.getPropertyName()))
					{
						formExtendsName = formObject.getString(StaticContentSpecLoader.PROPERTY_EXTENDSFORMID.getPropertyName());
					}
					else if (formObject.has(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()))
					{
						formExtendsName = formObject.getString(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
					}
					if (formExtendsName != null)
					{
						Form form = flattenedSolution.getForm(formExtendsName);
						if (form != null)
						{
							extendsFormID = form.getID();
						}
					}
					extendsFormViewer.setSelection(new StructuredSelection(Integer.valueOf(extendsFormID)));

					// styleName
					Style templateStyle = null;
					if (formObject.has(StaticContentSpecLoader.PROPERTY_STYLENAME.getPropertyName()))
					{
						templateStyle = (Style)repository.getActiveRootObject(
							formObject.getString(StaticContentSpecLoader.PROPERTY_STYLENAME.getPropertyName()), IRepository.STYLES);
					}
					styleNameCombo.setSelection(new StructuredSelection(templateStyle == null ? Messages.LabelDefault : templateStyle));
				}
			}
		}

		private void handleProjectSelected()
		{
			servoyProject = null;
			IStructuredSelection selection = (IStructuredSelection)projectCombo.getSelection();
			if (selection.size() == 1)
			{
				servoyProject = ((ServoyProject)selection.getFirstElement());
			}
			if (servoyProject != null)
			{
				ISelection sel = extendsFormViewer.getSelection();
				updateExtendsFormViewer(servoyProject.getEditingFlattenedSolution());
				extendsFormViewer.setSelection(sel);
			}
			setPageComplete(validatePage());
		}

		private void handleStyleSelected()
		{
			style = null;
			IStructuredSelection selection = (IStructuredSelection)styleNameCombo.getSelection();
			if (selection.size() == 1)
			{
				style = selection.getFirstElement() == Messages.LabelDefault ? null : ((Style)selection.getFirstElement());
			}
			setPageComplete(validatePage());
		}

		private void handleFormNameChanged()
		{
			formName = formNameField.getText();
			formNameTyped = formName != null && formName.trim().length() > 0;
			setPageComplete(validatePage());
		}

		private void setFormName(String text)
		{
			formName = "".equals(text) ? null : text;
			formNameField.setText(text == null ? "" : text);
			formNameTyped = false;
			setPageComplete(validatePage());
		}

		protected boolean validatePage()
		{
			String error = null;
			if (formNameField.getText().trim().length() == 0)
			{
				error = "Enter a new form name";
			}
			else if (!IdentDocumentValidator.isJavaIdentifier(formNameField.getText()))
			{
				error = "Invalid form name";
			}
			else if (!dataSourceViewer.isValid())
			{
				error = "Invalid data source";
			}
			else if (!extendsFormViewer.isValid())
			{
				error = "Invalid extends form";
			}
			else if (servoyProject == null)
			{
				error = "You must select a solution (the list contains the active solution and it's modules)";
			}
			else
			{
				try
				{
					ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(formName, -1,
						new ValidatorSearchContext(null, IRepository.FORMS), false);
				}
				catch (RepositoryException e)
				{
					error = e.getMessage();
				}
			}
			setErrorMessage(error);
			return error == null;
		}

		/**
		 * The <code>NewFormWizardPage</code> implementation of this <code>Listener</code> method handles all events and enablements for controls on this page.
		 * Subclasses may extend.
		 */
		public void handleEvent(Event event)
		{
			setPageComplete(validatePage());
		}

	}

	public class DataProviderWizardPage extends WizardPage implements Listener
	{
		private DataProviderTreeViewer treeViewer;

		private PlaceFieldOptionGroup optionsGroup;

		/**
		 * Creates a new form creation wizard page.
		 * 
		 * @param pageName the name of the page
		 * @param selection the current resource selection
		 */
		public DataProviderWizardPage(String pageName)
		{
			super(pageName);
			setTitle("Add data providers");
			setDescription(getTitle());
		}

		/**
		 * (non-Javadoc) Method declared on IDialogPage.
		 */
		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);
			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

			setControl(topLevel);
			treeViewer = new DataProviderTreeViewer(topLevel, DataProviderLabelProvider.INSTANCE_HIDEPREFIX,// label provider will be overwritten when superform is known
				new DataProviderContentProvider(null, servoyProject.getEditingFlattenedSolution(), null), new DataProviderTreeViewer.DataProviderOptions(false,
					true, true, true, true, true, true, true, INCLUDE_RELATIONS.NESTED, true, true, null), true, true, TreePatternFilter.getSavedFilterMode(
					getDialogSettings(), TreePatternFilter.FILTER_LEAFS), SWT.MULTI);

			IDialogSettings settings = NewFormWizard.this.getDialogSettings();
			final boolean isPlaceHorizontal = settings.getBoolean("placeHorizontal");
			final String isPlaceAsLabels = settings.get("placeAsLabels");
			final String isPlaceWithLabels = settings.get("placeLabels");
			final boolean isFillText = settings.getBoolean("fillText");
			final boolean isFillName = settings.getBoolean("fillName");

			optionsGroup = new PlaceFieldOptionGroup(topLevel, SWT.NONE);
			optionsGroup.setText("Options");
			optionsGroup.setPlaceAsLabels(String.valueOf(true).equals(isPlaceAsLabels));
			optionsGroup.setPlaceWithLabels(isPlaceWithLabels == null || String.valueOf(true).equals(isPlaceWithLabels));
			optionsGroup.setPlaceHorizontal(isPlaceHorizontal);
			optionsGroup.setFillText(isFillText);
			optionsGroup.setFillName(isFillName);

			final GroupLayout groupLayout = new GroupLayout(topLevel);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, treeViewer,
				GroupLayout.PREFERRED_SIZE, 274, Short.MAX_VALUE).add(GroupLayout.LEADING, optionsGroup, GroupLayout.PREFERRED_SIZE, 274, Short.MAX_VALUE));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
				groupLayout.createSequentialGroup().add(treeViewer, GroupLayout.PREFERRED_SIZE, 133, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(
					optionsGroup, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)));
			topLevel.setLayout(groupLayout);
		}

		/*
		 * @see DialogPage.setVisible(boolean)
		 */
		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			if (visible)
			{
				fillDataproviderTree();
				setPageComplete(validatePage());
			}
		}

		@Override
		public void dispose()
		{
			((TreePatternFilter)treeViewer.getPatternFilter()).saveSettings(getDialogSettings());
			super.dispose();
		}

		public void fillDataproviderTree()
		{
			try
			{
				IServer server = ServoyModel.getDeveloperRepository().getServer(newFormWizardPage.getServerName());
				Table table = null;
				if (server != null)
				{
					table = (Table)server.getTable(newFormWizardPage.getTableName());
				}
				Form superForm = newFormWizardPage.getSuperForm();
				((DataProviderContentProvider)treeViewer.getContentProvider()).setTable(table, PersistContext.create(superForm));
				treeViewer.setLabelProvider(new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(
					DataProviderLabelProvider.INSTANCE_HIDEPREFIX, superForm)));
				treeViewer.refreshTree();
			}
			catch (RemoteException e)
			{
				ServoyLog.logError("Could not get data source for new form", e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get data source for new form", e);
			}
		}

		private boolean validatePage()
		{
			return true;
		}

		/**
		 * The <code>NewFormWizardPage</code> implementation of this <code>Listener</code> method handles all events and enablements for controls on this page.
		 * Subclasses may extend.
		 */
		public void handleEvent(Event event)
		{
			setPageComplete(validatePage());
		}

		public Object[] getDataProviders()
		{
			return ((IStructuredSelection)treeViewer.getSelection()).toArray();
		}

	}

	private Solution getActiveSolution()
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		return (activeProject != null ? activeProject.getSolution() : null);
	}
}
