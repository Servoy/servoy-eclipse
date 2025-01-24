/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AcceptAllFilter;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.json.JSONException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.types.BooleanPropertyType;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.ValuelistContentProvider;
import com.servoy.eclipse.ui.dialogs.ValuelistContentProvider.ValuelistListOptions;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.property.FormValueEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.RelationPropertyController;
import com.servoy.eclipse.ui.property.ValuelistPropertyController;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMenuItemAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.server.ngclient.property.types.RelationPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class MenuEditor extends PersistEditor
{
	private Composite parent;
	private MenuItem selectedMenuItem;
	private final List<Consumer<MenuItem>> selectedMenuItemCallbacks = new ArrayList<Consumer<MenuItem>>();
	private TreeViewer menuViewer;
	private boolean initializingData = false;

	@Override
	protected void doRefresh()
	{
		if (selectedMenuItem != null)
		{
			try
			{
				initializingData = true;
				selectedMenuItemCallbacks.forEach(consumer -> consumer.accept(selectedMenuItem));
			}
			finally
			{
				initializingData = false;
			}
		}
	}

	@Override
	protected boolean validatePersist(IPersist p)
	{
		return p instanceof Menu;
	}

	@Override
	public void createPartControl(Composite parent)
	{
		this.parent = parent;
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			init();
		}
	}

	@Override
	protected void init()
	{
		setTitleImage(Activator.loadImageDescriptorFromBundle("column.png").createImage());

		parent.setLayout(new FillLayout());

		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		myScrolledComposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");

		Composite container = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(container);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label menuNameLabel = new Label(container, SWT.NONE);
		menuNameLabel.setText("Menu Name");
		Text txtMenuName = new Text(container, SWT.BORDER);
		txtMenuName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtMenuName.setText(Objects.requireNonNullElse(getPersist().getName(), ""));
		txtMenuName.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				try
				{
					getPersist().updateName(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), txtMenuName.getText());
					MenuEditor.this.updateTitle();
					flagModified();
				}
				catch (RepositoryException e1)
				{
					UIUtils.reportError("Rename failed", e1.getMessage());
				}
			}
		});

		Label menuStyleClassLabel = new Label(container, SWT.NONE);
		menuStyleClassLabel.setText("Menu StyleClass");
		Text txtMenuStyleClass = new Text(container, SWT.BORDER);
		txtMenuStyleClass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtMenuStyleClass.setText(Objects.requireNonNullElse(getPersist().getStyleClass(), ""));
		txtMenuStyleClass.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				getPersist().setStyleClass(txtMenuStyleClass.getText());
				flagModified();
			}
		});

		SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Composite leftComposite = new Composite(sashForm, SWT.NONE);
		leftComposite.setLayout(new GridLayout(1, false));
		leftComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Button addMenuItem = new Button(leftComposite, SWT.NONE);
		addMenuItem.setText("Add Menu Item");
		addMenuItem.setToolTipText("Adds a new Menu Item to the Menu or to selected Menu Item");
		addMenuItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				String name = NewMenuItemAction.askMenuItemName(getSite().getShell());
				if (name != null)
				{
					IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
					try
					{
						MenuItem menuItem = selectedMenuItem != null ? selectedMenuItem.createNewMenuItem(validator, name)
							: getPersist().createNewMenuItem(validator, name);
						menuViewer.refresh();
						menuViewer.setSelection(new StructuredSelection(menuItem), true);
						flagModified();
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
						MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Save failed: " + ex.getMessage());
					}
				}
			}
		});

		Button deleteMenuItem = new Button(leftComposite, SWT.NONE);
		deleteMenuItem.setText("Delete Menu Item");
		deleteMenuItem.setToolTipText("Deletes selected Menu Item");
		deleteMenuItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				if (selectedMenuItem != null)
				{
					if (UIUtils.askConfirmation(getSite().getShell(), "Delete Menu Item '" + selectedMenuItem.getName() + "'",
						"Are you sure you want to delete menu item '" + selectedMenuItem.getName() + "'"))
						try
					{
						((EclipseRepository)getPersist().getRootObject().getRepository()).deleteObject(selectedMenuItem);
						menuViewer.refresh();
						getPersist().flagChanged();
						flagModified();
						ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, selectedMenuItem, false);
						selectedMenuItem = null;
					}
					catch (RepositoryException e1)
					{
						ServoyLog.logError(e1);
						UIUtils.reportError("Delete failed", e1.getMessage());
					}
				}
				else
				{
					UIUtils.reportWarning("No Menu Item Selected", "Should select a menu item in the tree before trying to delete");
				}
			}
		});

		Label menuItemsLabel = new Label(leftComposite, SWT.NONE);
		menuItemsLabel.setText("Menu Items");
		menuItemsLabel.setToolTipText("Select a menu item to edit its properties");

		menuViewer = new TreeViewer(leftComposite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		menuViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		menuViewer.getTree().setToolTipText("Select a menu item to edit its properties");
		menuViewer.setContentProvider(new ITreeContentProvider()
		{

			@Override
			public Object[] getElements(Object inputElement)
			{
				if (inputElement instanceof Menu menu)
				{
					return menu.getAllObjectsAsList().toArray();
				}
				return null;
			}

			@Override
			public Object[] getChildren(Object parentElement)
			{
				if (parentElement instanceof AbstractBase parent)
				{
					return parent.getAllObjectsAsList().toArray();
				}
				return null;
			}

			@Override
			public Object getParent(Object element)
			{
				return ((AbstractBase)element).getParent();
			}

			@Override
			public boolean hasChildren(Object element)
			{
				return ((AbstractBase)element).getAllObjects().hasNext();
			}
		});
		menuViewer.setLabelProvider(new ITableLabelProvider()
		{

			@Override
			public void addListener(ILabelProviderListener listener)
			{

			}

			@Override
			public void dispose()
			{

			}

			@Override
			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener)
			{

			}

			@Override
			public Image getColumnImage(Object element, int columnIndex)
			{
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex)
			{
				if (element instanceof MenuItem menuItem)
				{
					return menuItem.getName();
				}
				if (element instanceof Menu menu)
				{
					return menu.getName();
				}
				return null;
			}

		});
		menuViewer.setInput(getPersist());
		menuViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				StructuredSelection newSelection = (StructuredSelection)event.getSelection();
				selectedMenuItem = null;
				if (newSelection != null && !newSelection.isEmpty())
				{
					selectedMenuItem = (MenuItem)newSelection.getFirstElement();
				}
				getSite().getSelectionProvider().setSelection(selectedMenuItem != null ? new StructuredSelection(selectedMenuItem) : StructuredSelection.EMPTY);
				try
				{
					initializingData = true;
					selectedMenuItemCallbacks.forEach(consumer -> consumer.accept(selectedMenuItem));
				}
				finally
				{
					initializingData = false;
				}
			}
		});

		Composite propertiesComposite = new Composite(sashForm, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		propertiesComposite.setLayout(layout);
		propertiesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Group generalPropertiesGroup = new Group(propertiesComposite, SWT.NONE);
		generalPropertiesGroup.setText("Menu Item - General Properties");
		generalPropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		generalPropertiesGroup.setLayout(new GridLayout(2, false));

		Label label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Name");
		Text txtName = new Text(generalPropertiesGroup, SWT.BORDER);
		txtName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			txtName.setText(Objects.requireNonNullElse(menuItem != null ? menuItem.getName() : "", ""));
		});
		txtName.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				try
				{
					if (selectedMenuItem != null && !initializingData)
					{
						selectedMenuItem.updateName(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), txtName.getText());
						flagModified();
					}
				}
				catch (RepositoryException e1)
				{
					UIUtils.reportError("Rename failed", e1.getMessage());
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Text");
		Text txtText = new Text(generalPropertiesGroup, SWT.BORDER);
		txtText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			txtText.setText(Objects.requireNonNullElse(menuItem != null ? menuItem.getText() : "", ""));
		});
		txtText.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				if (selectedMenuItem != null && !initializingData)
				{
					selectedMenuItem.setText(txtText.getText());
					flagModified();
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Enabled");
		Button chkEnabled = new Button(generalPropertiesGroup, SWT.CHECK);
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			chkEnabled.setSelection(menuItem != null ? menuItem.getEnabled() : false);
		});
		chkEnabled.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (selectedMenuItem != null && !initializingData)
				{
					selectedMenuItem.setEnabled(chkEnabled.getSelection());
					flagModified();
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Icon StyleClass");
		Text txtIconStyleClass = new Text(generalPropertiesGroup, SWT.BORDER);
		txtIconStyleClass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			txtIconStyleClass.setText(Objects.requireNonNullElse(menuItem != null ? menuItem.getIconStyleClass() : "", ""));
		});
		txtIconStyleClass.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				if (selectedMenuItem != null && !initializingData)
				{
					selectedMenuItem.setIconStyleClass(txtIconStyleClass.getText());
					flagModified();
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("StyleClass");
		Text txtStyleClass = new Text(generalPropertiesGroup, SWT.BORDER);
		txtStyleClass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			txtStyleClass.setText(Objects.requireNonNullElse(menuItem != null ? menuItem.getStyleClass() : "", ""));
		});
		txtStyleClass.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				if (selectedMenuItem != null && !initializingData)
				{
					selectedMenuItem.setStyleClass(txtStyleClass.getText());
					flagModified();
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Tooltip Text");
		Text txtTooltipText = new Text(generalPropertiesGroup, SWT.BORDER);
		txtTooltipText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			txtTooltipText.setText(Objects.requireNonNullElse(menuItem != null ? menuItem.getToolTipText() : "", ""));
		});
		txtTooltipText.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				if (selectedMenuItem != null && !initializingData)
				{
					selectedMenuItem.setToolTipText(txtTooltipText.getText());
					flagModified();
				}
			}
		});

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Permissions");
		Text txtPermissions = new Text(generalPropertiesGroup, SWT.BORDER);
		txtPermissions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
			txtPermissions
				.setText(Objects.requireNonNullElse(
					menuItem != null && menuItem.getPermissions() != null ? ServoyJSONObject.toString(menuItem.getPermissions(), false, false, false) : "",
					""));
		});
		txtPermissions.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				if (selectedMenuItem != null && !initializingData)
				{
					try
					{
						selectedMenuItem.setPermissions(new ServoyJSONObject(txtPermissions.getText(), false, false, false));
						flagModified();
					}
					catch (JSONException ex)
					{
						//ignore ?
					}
				}
			}
		});

		Map<String, Map<String, PropertyDescription>> extraProperties = MenuPropertyType.INSTANCE.getExtraProperties();
		if (extraProperties != null)
		{
			FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(getPersist());
			for (String categoryName : extraProperties.keySet())
			{
				Group categoryPropertiesGroup = new Group(propertiesComposite, SWT.NONE);
				categoryPropertiesGroup.setText("Menu Item - " + categoryName + " Properties");
				categoryPropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				categoryPropertiesGroup.setLayout(new GridLayout(2, false));

				Map<String, PropertyDescription> extraCategoryProperties = extraProperties.get(categoryName);
				List<String> propertyNames = new ArrayList<String>(extraCategoryProperties.keySet());
				propertyNames.sort(StringComparator.INSTANCE);
				for (String propertyName : propertyNames)
				{
					PropertyDescription propertyDescription = extraCategoryProperties.get(propertyName);
					label = new Label(categoryPropertiesGroup, SWT.NONE);
					label.setText(propertyDescription.getName());
					Object config = propertyDescription.getConfig();
					if (propertyDescription.getType() instanceof BooleanPropertyType)
					{
						Button propertyCheckbox = new Button(categoryPropertiesGroup, SWT.CHECK);
						selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
							propertyCheckbox.setSelection(menuItem != null ? Utils.getAsBoolean(menuItem.getExtraProperty(categoryName, propertyName)) : false);
						});
						propertyCheckbox.addSelectionListener(new SelectionAdapter()
						{
							@Override
							public void widgetSelected(SelectionEvent e)
							{
								if (selectedMenuItem != null && !initializingData)
								{
									selectedMenuItem.putExtraProperty(categoryName, propertyName, propertyCheckbox.getSelection());
									flagModified();
								}
							}
						});
					}
					else if (propertyDescription.getType() instanceof ValueListPropertyType)
					{
						TreeSelectViewer valuelistComponent = new TreeSelectViewer(categoryPropertiesGroup, SWT.NONE,
							new ValuelistPropertyController.ValueListValueEditor(editingFlattenedSolution));
						valuelistComponent.setButtonText("Select Valuelist");
						valuelistComponent.setContentProvider(new ValuelistContentProvider(editingFlattenedSolution, getPersist()));
						valuelistComponent.setLabelProvider(new ValuelistLabelProvider(editingFlattenedSolution, getPersist()));
						valuelistComponent.setTitleText("Select valuelist");
						valuelistComponent.setInput(new ValuelistListOptions(true));
						valuelistComponent.setEditable(true);
						valuelistComponent.addSelectionChangedListener(new ISelectionChangedListener()
						{
							public void selectionChanged(SelectionChangedEvent event)
							{
								if (selectedMenuItem != null && !initializingData)
								{
									IStructuredSelection selection = (IStructuredSelection)valuelistComponent.getSelection();
									ValueList valuelist = null;
									if (!selection.isEmpty())
									{
										valuelist = editingFlattenedSolution.getValueList(Utils.getAsInteger(selection.getFirstElement()));
									}
									selectedMenuItem.putExtraProperty(categoryName, propertyName, valuelist != null ? valuelist.getUUID() : null);
									flagModified();
								}
							}
						});
						selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
							Object value = menuItem != null ? menuItem.getExtraProperty(categoryName, propertyName) : "";
							ValueList valuelist = null;
							if (value != null)
							{
								valuelist = editingFlattenedSolution.getValueList(value.toString());
							}
							if (valuelist == null)
							{
								valuelistComponent.setSelection(StructuredSelection.EMPTY);
							}
							else
							{
								valuelistComponent.setSelection(new StructuredSelection(valuelist.getID()));
							}
						});
					}
					else if (propertyDescription.getType() instanceof RelationPropertyType)
					{
						TreeSelectViewer relationSelect = new TreeSelectViewer(categoryPropertiesGroup, SWT.NONE,
							RelationPropertyController.RelationValueEditor.INSTANCE);
						relationSelect.setContentProvider(new RelationContentProvider(editingFlattenedSolution, getPersist()));
						relationSelect.setLabelProvider(RelationLabelProvider.INSTANCE_ALL);
						relationSelect.setTextLabelProvider(new RelationLabelProvider("", false, false));
						relationSelect.setSelectionFilter(AcceptAllFilter.getInstance());
						relationSelect.setTitleText("Select relation");
						relationSelect.setEditable(true);
						relationSelect.addSelectionChangedListener(new ISelectionChangedListener()
						{
							public void selectionChanged(SelectionChangedEvent event)
							{
								if (selectedMenuItem != null && !initializingData)
								{
									IStructuredSelection selection = (IStructuredSelection)relationSelect.getSelection();
									selectedMenuItem.putExtraProperty(categoryName, propertyName, selection.isEmpty() ? null
										: EclipseDatabaseUtils.getRelationsString(((RelationsWrapper)selection.getFirstElement()).relations));
									flagModified();
								}
							}
						});
						relationSelect.setInput(new RelationContentProvider.RelationListOptions(null, null, true, true));
						selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
							Object value = menuItem != null ? menuItem.getExtraProperty(categoryName, propertyName) : "";
							Relation[] relations = null;
							if (value != null)
							{
								relations = editingFlattenedSolution.getRelationSequence(value.toString());
							}
							if (relations == null)
							{
								relationSelect.setSelection(StructuredSelection.EMPTY);
							}
							else
							{
								relationSelect.setSelection(new StructuredSelection(new RelationsWrapper(relations)));
							}
						});
					}
					else if (propertyDescription.getType() instanceof FormPropertyType)
					{
						TreeSelectViewer formViewer = new TreeSelectViewer(categoryPropertiesGroup, SWT.NONE, new FormValueEditor(editingFlattenedSolution));
						formViewer.setTitleText("Select form");
						formViewer.setContentProvider(new FormContentProvider(editingFlattenedSolution, null));
						formViewer.setLabelProvider(
							new SolutionContextDelegateLabelProvider(new FormLabelProvider(editingFlattenedSolution, true),
								editingFlattenedSolution.getSolution()));
						formViewer.setInput(new FormContentProvider.FormListOptions(FormListOptions.FormListType.FORMS, null, true, false, false,
							false, null));
						formViewer.setEditable(true);
						formViewer.addSelectionChangedListener(new ISelectionChangedListener()
						{
							public void selectionChanged(SelectionChangedEvent event)
							{
								if (selectedMenuItem != null && !initializingData)
								{
									IStructuredSelection selection = (IStructuredSelection)formViewer.getSelection();
									Form form = null;
									if (!selection.isEmpty())
									{
										form = editingFlattenedSolution.getForm(Utils.getAsInteger(selection.getFirstElement()));
									}
									selectedMenuItem.putExtraProperty(categoryName, propertyName, form != null ? form.getUUID() : null);
									flagModified();
								}
							}
						});
						selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
							Object value = menuItem != null ? menuItem.getExtraProperty(categoryName, propertyName) : "";
							Form form = null;
							if (value != null)
							{
								form = editingFlattenedSolution.getForm(value.toString());
							}
							if (form == null)
							{
								formViewer.setSelection(StructuredSelection.EMPTY);
							}
							else
							{
								formViewer.setSelection(new StructuredSelection(form.getID()));
							}
						});
					}
					else if (config instanceof ValuesConfig valuesConfig && valuesConfig.getDisplay() != null)
					{
						Combo propertyCombobox = new Combo(categoryPropertiesGroup, SWT.READ_ONLY);
						propertyCombobox.setItems(valuesConfig.getDisplay());
						selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
							Object value = menuItem != null ? menuItem.getExtraProperty(categoryName, propertyName) : null;
							if (value != null)
							{
								propertyCombobox.setText(value.toString());
							}
							else if (valuesConfig.hasDefault())
							{
								propertyCombobox.setText(valuesConfig.getDisplayDefault());
							}
						});
						propertyCombobox.addModifyListener(new ModifyListener()
						{
							@Override
							public void modifyText(ModifyEvent e)
							{
								if (selectedMenuItem != null && !initializingData)
								{
									selectedMenuItem.putExtraProperty(categoryName, propertyName, propertyCombobox.getText());
									flagModified();
								}
							}
						});
					}
					else
					{
						Text propertyTextbox = new Text(categoryPropertiesGroup, SWT.BORDER);
						propertyTextbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
						selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
							Object value = menuItem != null ? menuItem.getExtraProperty(categoryName, propertyName) : "";
							propertyTextbox.setText(value != null ? value.toString() : "");
						});
						propertyTextbox.addModifyListener(new ModifyListener()
						{
							@Override
							public void modifyText(ModifyEvent e)
							{
								if (selectedMenuItem != null && !initializingData)
								{
									selectedMenuItem.putExtraProperty(categoryName, propertyName, propertyTextbox.getText());
									flagModified();
								}
							}
						});
					}
				}

			}
		}

		sashForm.setWeights(new int[] { 20, 80 });
		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	@Override
	public void setFocus()
	{

	}

	@Override
	public Menu getPersist()
	{
		return (Menu)super.getPersist();
	}

	@Override
	public void dispose()
	{
		super.dispose();
		selectedMenuItemCallbacks.clear();
	}

	public void flagModified()
	{
		this.getSite().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
		PersistPropertySource.refreshPropertiesView();
	}
}
