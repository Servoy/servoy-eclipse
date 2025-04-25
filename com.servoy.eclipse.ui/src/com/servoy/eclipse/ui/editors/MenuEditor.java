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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.AcceptAllFilter;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.json.JSONException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.InputAndComboDialog;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderNodeWrapper;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.dialogs.PermissionsDialog;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.dialogs.ValuelistContentProvider;
import com.servoy.eclipse.ui.dialogs.ValuelistContentProvider.ValuelistListOptions;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.property.FormValueEditor;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.RelationPropertyController;
import com.servoy.eclipse.ui.property.ValuelistPropertyController;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMenuItemAction;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormAndTableDataProviderLookup;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.server.ngclient.property.types.RelationPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author lvostinar
 *
 */
public class MenuEditor extends PersistEditor
{
	public static final Object ROOT = new Object();

	static final int CI_NAME = 0;
	static final int CI_TYPE = 1;
	static final int CI_DELETE = 2;

	static final int CI_ADD = 1;

	private Composite parent;
	private MenuItem selectedMenuItem;
	private final List<Consumer<MenuItem>> selectedMenuItemCallbacks = new ArrayList<Consumer<MenuItem>>();
	private TreeViewer menuViewer;
	private boolean initializingData = false;
	private Group customPropertiesGroup;

	private TableViewer customPropertiesViewer;
	private MenuEditorDragAndDropListener menuEditorDragAndDropListener;

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
//		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

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

		Button addCustomProperty = new Button(container, SWT.NONE);
		addCustomProperty.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false, 2, 1));
		addCustomProperty.setText("Add Custom Property");
		addCustomProperty.setToolTipText("Adds a new custom property definition that can be defined on each menu item");
		addCustomProperty.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				InputAndComboDialog dialog = new InputAndComboDialog(getSite().getShell(), "Add Menu Custom Property Definition", "Custom Property Name", "",
					new IInputValidator()
					{
						public String isValid(String newText)
						{
							boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
							return valid ? null : (newText.length() == 0 ? "" : "Invalid property name");
						}
					}, new String[] { "boolean", "dataprovider", "form", "int", "relation", "string", "valuelist" }, "Custom Property Type", 5);
				if (dialog.open() == Window.OK)
				{
					getPersist().putCustomPropertyDefinition(dialog.getValue(), dialog.getExtendedValue());
					customPropertiesViewer.refresh();
					Map<String, PropertyDescription> newPropertyMap = new HashMap<String, PropertyDescription>();
					newPropertyMap.put(dialog.getValue(),
						new PropertyDescriptionBuilder().withName(dialog.getValue()).withType(TypesRegistry.getType(dialog.getExtendedValue(), false))
							.build());
					createGroupedComponents(customPropertiesGroup, null,
						newPropertyMap,
						false);
					myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					parent.requestLayout();
					parent.layout(true, true);
					flagModified();
				}
			}
		});

		Composite tableContainer = new Composite(container, SWT.NONE);
		tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		customPropertiesViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		customPropertiesViewer.getTable().setHeaderVisible(true);
		customPropertiesViewer.getTable().setLinesVisible(true);

		TableColumn nameColumn = new TableColumn(customPropertiesViewer.getTable(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Custom Property Name");

		final TableColumn typeColumn = new TableColumn(customPropertiesViewer.getTable(), SWT.LEFT, CI_TYPE);
		typeColumn.setText("Custom Property Type");

		final TableColumn deleteColumn = new TableColumn(customPropertiesViewer.getTable(), SWT.CENTER, CI_DELETE);

		customPropertiesViewer.getTable().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = customPropertiesViewer.getTable().getItem(pt);
				if (item != null && item.getBounds(CI_DELETE).contains(pt))
				{
					Object propertyDefinition = item.getData();
					if (propertyDefinition instanceof Pair pair &&
						MessageDialog.openConfirm(getSite().getShell(), "Delete custom property definition",
							"Are you sure you want to delete property '" + pair.getLeft() + "'?"))
					{
						getPersist().clearCustomPropertyDefinition(pair.getLeft().toString());
						customPropertiesViewer.refresh();
						parent.layout(true, true);
						flagModified();
					}

				}
			}
		});

		customPropertiesViewer.setLabelProvider(new ITableLabelProvider()
		{

			@Override
			public void removeListener(ILabelProviderListener listener)
			{

			}

			@Override
			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			@Override
			public void dispose()
			{

			}

			@Override
			public void addListener(ILabelProviderListener listener)
			{

			}

			@Override
			public String getColumnText(Object element, int columnIndex)
			{
				Pair<String, String> definition = (Pair<String, String>)element;
				if (columnIndex == CI_NAME)
				{
					return definition.getLeft();
				}
				if (columnIndex == CI_TYPE)
				{
					return definition.getRight();
				}
				return null;
			}

			@Override
			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == CI_DELETE)
				{
					return Activator.getDefault().loadImageFromBundle("delete.png");
				}
				return null;
			}
		});
		customPropertiesViewer.setContentProvider(new IStructuredContentProvider()
		{
			@Override
			public Object[] getElements(Object inputElement)
			{
				Map<String, Object> definitions = getPersist().getCustomPropertiesDefinition();
				if (definitions != null)
				{
					return definitions.keySet().stream().map(key -> new Pair<String, Object>(key, definitions.get(key))).collect(Collectors.toList()).toArray();
				}
				return new Object[0];
			}

		});
		customPropertiesViewer.setInput(getPersist());


		TableColumnLayout tableLayout = new TableColumnLayout();
		tableLayout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		tableLayout.setColumnData(typeColumn, new ColumnWeightData(10, 25, true));
		tableLayout.setColumnData(deleteColumn, new ColumnPixelData(20, false));
		tableContainer.setLayout(tableLayout);

		SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Composite leftComposite = new Composite(sashForm, SWT.NONE);
		leftComposite.setLayout(new GridLayout(1, false));
		leftComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label menuItemsLabel = new Label(leftComposite, SWT.NONE);
		menuItemsLabel.setText("Menu Items");
		menuItemsLabel.setToolTipText("Select a menu item to edit its properties");

		Composite treeContainer = new Composite(leftComposite, SWT.NONE);
		treeContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		menuViewer = new TreeViewer(treeContainer, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		menuViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		menuViewer.getTree().setToolTipText("Select a menu item to edit its properties");

		menuViewer.getTree().setLinesVisible(false);
		menuViewer.getTree().setHeaderVisible(false);

		TreeColumn menuItemNameColumn = new TreeColumn(menuViewer.getTree(), SWT.LEFT, CI_NAME);
		menuItemNameColumn.setText("Menu Item Name");

		TreeColumn menuItemAddColumn = new TreeColumn(menuViewer.getTree(), SWT.LEFT, CI_ADD);

		TreeColumn menuItemDeleteColumn = new TreeColumn(menuViewer.getTree(), SWT.LEFT, CI_DELETE);

		menuViewer.setContentProvider(new ITreeContentProvider()
		{

			@Override
			public Object[] getElements(Object inputElement)
			{
				return new Object[] { getPersist() };
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
				if (element instanceof MenuItem && columnIndex == CI_DELETE)
				{
					return Activator.getDefault().loadImageFromBundle("delete.png");
				}
				if (columnIndex == CI_ADD)
				{
					return Activator.getDefault().loadImageFromBundle("new.png");
				}
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == CI_NAME && element instanceof ISupportName nameElement)
				{
					return nameElement.getName();
				}
				return null;
			}

		});
		menuViewer.setInput(ROOT);
		menuViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				StructuredSelection newSelection = (StructuredSelection)event.getSelection();
				selectedMenuItem = null;
				if (newSelection != null && !newSelection.isEmpty() && newSelection.getFirstElement() instanceof MenuItem menuItem)
				{
					selectedMenuItem = menuItem;
				}
				else if (newSelection.getFirstElement() instanceof Menu menu && menu.getAllObjectsAsList().size() > 0)
				{
					menuViewer.setSelection(new StructuredSelection(menu.getAllObjectsAsList().get(0)), true);
					return;
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

		menuViewer.getTree().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				// why doesn't this work
				//TreeItem item = menuViewer.getTree().getItem(pt);
				TreeItem item = findTreeItem(menuViewer.getTree().getItems(), pt);
				if (item != null)
				{
					Object menuItemData = item.getData();
					if (item.getBounds(CI_DELETE).contains(pt) && menuItemData instanceof MenuItem menuItem)
					{
						if (UIUtils.askConfirmation(getSite().getShell(), "Delete Menu Item '" + menuItem.getName() + "'",
							"Are you sure you want to delete menu item '" + menuItem.getName() + "'"))
							try
						{
							((EclipseRepository)getPersist().getRootObject().getRepository()).deleteObject(menuItem);
							menuViewer.refresh();
							getPersist().flagChanged();
							flagModified();
							ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, menuItem, false);
						}
						catch (RepositoryException e1)
						{
							ServoyLog.logError(e1);
							UIUtils.reportError("Delete failed", e1.getMessage());
						}
					}
					else if (item.getBounds(CI_ADD).contains(pt))
					{
						Set<String> existingNames = new HashSet<>();
						if (menuItemData instanceof Menu menu)
						{
							setExistingNames(existingNames, menu, null);
						}
						else if (menuItemData instanceof MenuItem menuItem)
						{
							setExistingNames(existingNames, null, menuItem);
						}
						String name = NewMenuItemAction.askMenuItemName(getSite().getShell(), existingNames);
						if (name != null)
						{
							IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
							try
							{
								MenuItem childMenuItem = menuItemData instanceof MenuItem menuItem ? menuItem.createNewMenuItem(validator, name)
									: getPersist().createNewMenuItem(validator, name);
								menuViewer.refresh();
								menuViewer.setSelection(new StructuredSelection(childMenuItem), true);
								flagModified();
							}
							catch (Exception ex)
							{
								ServoyLog.logError(ex);
								MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Save failed: " + ex.getMessage());
							}
						}
					}

				}
			}

			/**
			 * @param existingNames
			 * @param menu
			 */
			private void setExistingNames(Set<String> existingNames, Menu menu, MenuItem menuItem)
			{
				try
				{
					Iterator<IPersist> children = (menu != null ? menu : menuItem).getAllObjects();
					while (children.hasNext())
					{
						IPersist child = children.next();
						if (child instanceof MenuItem)
						{
							existingNames.add(((MenuItem)child).getName());
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
					MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Failed to retrieve existing menu item names.");
					return;
				}
			}
		});

		menuEditorDragAndDropListener = new MenuEditorDragAndDropListener(menuViewer, this);
		menuViewer.addDragSupport(DND.DROP_MOVE,
			new Transfer[] { FormElementTransfer.getInstance() }, menuEditorDragAndDropListener);
		menuViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_DEFAULT,
			new Transfer[] { FormElementTransfer.getInstance() }, menuEditorDragAndDropListener);

		TreeColumnLayout treeLayout = new TreeColumnLayout();
		treeLayout.setColumnData(menuItemNameColumn, new ColumnWeightData(20, 50, true));
		treeLayout.setColumnData(menuItemAddColumn, new ColumnPixelData(20, false));
		treeLayout.setColumnData(menuItemDeleteColumn, new ColumnPixelData(20, false));
		treeContainer.setLayout(treeLayout);
		menuViewer.expandAll();

		Composite propertiesComposite = new Composite(sashForm, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		propertiesComposite.setLayout(layout);
		propertiesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Group generalPropertiesGroup = new Group(propertiesComposite, SWT.NONE);
		generalPropertiesGroup.setText("General Properties");
		generalPropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		generalPropertiesGroup.setLayout(new GridLayout(3, false));

		Label label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Name");
		Text txtName = new Text(generalPropertiesGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		txtName.setLayoutData(data);
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
		Button btnText = new Button(generalPropertiesGroup, SWT.NONE);
		btnText.setText("...");
		btnText.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (selectedMenuItem != null)
				{
					TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(getSite().getShell(), PersistContext.create(selectedMenuItem),
						ModelUtils.getEditingFlattenedSolution(getPersist()), null, selectedMenuItem.getText(), "Edit Text",
						com.servoy.eclipse.core.Activator.getDefault().getDesignClient(), false);
					dialog.open();

					if (dialog.getReturnCode() != Window.CANCEL)
					{
						selectedMenuItem.setText(dialog.getValue().toString());
						flagModified();
						txtText.setText(Objects.requireNonNullElse(selectedMenuItem.getText(), ""));
					}
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
		chkEnabled.setLayoutData(data);

		label = new Label(generalPropertiesGroup, SWT.NONE);
		label.setText("Icon StyleClass");
		Text txtIconStyleClass = new Text(generalPropertiesGroup, SWT.BORDER);
		txtIconStyleClass.setLayoutData(data);
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
		txtStyleClass.setLayoutData(data);
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
		Button btnTootipText = new Button(generalPropertiesGroup, SWT.NONE);
		btnTootipText.setText("...");
		btnTootipText.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (selectedMenuItem != null)
				{
					TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(getSite().getShell(), PersistContext.create(selectedMenuItem),
						ModelUtils.getEditingFlattenedSolution(getPersist()), null, selectedMenuItem.getToolTipText(), "Edit Tooltip Text",
						com.servoy.eclipse.core.Activator.getDefault().getDesignClient(), false);
					dialog.open();

					if (dialog.getReturnCode() != Window.CANCEL)
					{
						selectedMenuItem.setToolTipText(dialog.getValue().toString());
						flagModified();
						txtTooltipText.setText(Objects.requireNonNullElse(selectedMenuItem.getToolTipText(), ""));
					}
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
		Button btnPermissions = new Button(generalPropertiesGroup, SWT.NONE);
		btnPermissions.setText("...");
		btnPermissions.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (selectedMenuItem != null)
				{
					PermissionsDialog dialog = new PermissionsDialog(getSite().getShell(), selectedMenuItem.getPermissions());
					dialog.open();

					if (dialog.getReturnCode() != Window.CANCEL)
					{
						selectedMenuItem.setPermissions(dialog.getValue());
						flagModified();
						txtPermissions
							.setText(Objects.requireNonNullElse(
								selectedMenuItem != null && selectedMenuItem.getPermissions() != null
									? ServoyJSONObject.toString(selectedMenuItem.getPermissions(), false, false, false) : "",
								""));
					}
				}
			}
		});
		Map<String, Map<String, PropertyDescription>> extraProperties = MenuPropertyType.INSTANCE.getExtraProperties();
		if (extraProperties != null)
		{
			for (String categoryName : extraProperties.keySet())
			{
				Group categoryPropertiesGroup = new Group(propertiesComposite, SWT.NONE);
				categoryPropertiesGroup.setText(categoryName + " Properties");
				categoryPropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				categoryPropertiesGroup.setLayout(new GridLayout(2, false));
				createGroupedComponents(categoryPropertiesGroup, categoryName, extraProperties.get(categoryName), true);
			}
		}

		customPropertiesGroup = new Group(propertiesComposite, SWT.NONE);
		customPropertiesGroup.setText("Custom Properties");
		customPropertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		customPropertiesGroup.setLayout(new GridLayout(2, false));

		Map<String, Object> definitions = getPersist().getCustomPropertiesDefinition();
		if (definitions != null)
		{
			createGroupedComponents(customPropertiesGroup, null,
				definitions.keySet().stream()
					.map(key -> new PropertyDescriptionBuilder().withName(key).withType(TypesRegistry.getType(definitions.get(key).toString(), false)).build())
					.collect(Collectors.toMap(pd -> pd.getName(), pd -> pd)),
				false);
		}

		sashForm.setWeights(new int[] { 20, 80 });
		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		parent.requestLayout();
	}

	private void createGroupedComponents(Group categoryPropertiesGroup, String categoryName, Map<String, PropertyDescription> extraProperties,
		boolean isExtraProperties)
	{
		FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(getPersist());
		List<String> propertyNames = new ArrayList<String>(extraProperties.keySet());
		propertyNames.sort(StringComparator.INSTANCE);
		for (String propertyName : propertyNames)
		{

			PropertyDescription propertyDescription = extraProperties.get(propertyName);
			Label label = new Label(categoryPropertiesGroup, SWT.NONE);
			label.setText(propertyName);
			if (propertyDescription.getType() instanceof BooleanPropertyType)
			{
				Button propertyCheckbox = new Button(categoryPropertiesGroup, SWT.CHECK);
				selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
					Object value = null;
					if (menuItem != null)
					{
						value = isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName);
					}
					propertyCheckbox.setSelection(menuItem != null ? Utils.getAsBoolean(value) : false);
				});
				propertyCheckbox.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						if (selectedMenuItem != null && !initializingData)
						{
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, propertyCheckbox.getSelection());
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, propertyCheckbox.getSelection());
							}
							flagModified();
						}
					}
				});
			}
			else if (propertyDescription.getType() instanceof ValueListPropertyType)
			{
				TreeSelectViewer valuelistComponent = new TreeSelectViewer(categoryPropertiesGroup, SWT.NONE,
					new ValuelistPropertyController.ValueListValueEditor(editingFlattenedSolution));
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
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, valuelist != null ? valuelist.getUUID() : null);
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, valuelist != null ? valuelist.getUUID() : null);
							}
							flagModified();
						}
					}
				});
				selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
					Object value = "";
					if (menuItem != null)
					{
						value = isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName);
					}
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
							Object value = selection.isEmpty() ? null
								: EclipseDatabaseUtils.getRelationsString(((RelationsWrapper)selection.getFirstElement()).relations);
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, value);
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, value);
							}
							flagModified();
						}
					}
				});
				relationSelect.setInput(new RelationContentProvider.RelationListOptions(null, null, true, true));
				selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
					Object value = null;
					if (menuItem != null)
					{
						value = isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName);
					}
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
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, form != null ? form.getUUID() : null);
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, form != null ? form.getUUID() : null);
							}
							flagModified();
						}
					}
				});
				selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
					Object value = menuItem != null
						? (isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName)) : null;
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
			else if (propertyDescription.getType() instanceof DataproviderPropertyType)
			{
				TreeSelectViewer dataSourceViewer = new TreeSelectViewer(categoryPropertiesGroup, SWT.NONE);
				dataSourceViewer.setTitleText("Select dataprovider");
				dataSourceViewer.setContentProvider(new DataProviderContentProvider(null, editingFlattenedSolution, null));
				dataSourceViewer.setLabelProvider(new LabelProvider()
				{
					@Override
					public Image getImage(Object element)
					{
						return null;
					}

					@Override
					public String getText(Object element)
					{
						if (element == null || element == DataProviderContentProvider.NONE)
						{
							return Messages.LabelNone;
						}
						if (element instanceof IDataProvider dataProvider) return dataProvider.getDataProviderID();
						if (element instanceof DataProviderNodeWrapper wrapper) return wrapper.node;
						return Messages.LabelUnresolved;
					}
				});
				dataSourceViewer
					.setInput(new DataProviderTreeViewer.DataProviderOptions(true, true, true, true, true, true, true, true, INCLUDE_RELATIONS.NO, true, true,
						null));
				dataSourceViewer.setEditable(false);
				dataSourceViewer.setTextLabelProvider(new LabelProvider()
				{
					@Override
					public Image getImage(Object element)
					{
						return null;
					}

					@Override
					public String getText(Object element)
					{
						if (element instanceof IDataProvider) return ((IDataProvider)element).getDataProviderID();
						return "";
					}
				});

				dataSourceViewer.addSelectionChangedListener(new ISelectionChangedListener()
				{
					public void selectionChanged(SelectionChangedEvent event)
					{
						if (selectedMenuItem != null && !initializingData)
						{
							IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
							String dpID = null;
							if (!selection.isEmpty())
							{
								dpID = selection.getFirstElement().toString();
							}
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, dpID);
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, dpID);
							}
							flagModified();
						}
					}
				});
				selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
					Object value = menuItem != null
						? (isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName)) : null;
					FormAndTableDataProviderLookup dpLookup = new FormAndTableDataProviderLookup(editingFlattenedSolution, null, null);
					IDataProvider dp = null;
					if (value != null)
					{
						try
						{
							dp = dpLookup.getDataProvider(value.toString());
						}
						catch (RepositoryException e)
						{
							Debug.error(e);
						}
					}
					if (dp == null)
					{
						dataSourceViewer.setSelection(StructuredSelection.EMPTY);
					}
					else
					{
						dataSourceViewer.setSelection(new StructuredSelection(dp));
					}
				});
			}
			else if (propertyDescription.getConfig() instanceof ValuesConfig valuesConfig && valuesConfig.getDisplay() != null)
			{
				Combo propertyCombobox = new Combo(categoryPropertiesGroup, SWT.READ_ONLY);
				propertyCombobox.setItems(valuesConfig.getDisplay());
				selectedMenuItemCallbacks.add((MenuItem menuItem) -> {
					Object value = menuItem != null
						? (isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName)) : null;
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
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, propertyCombobox.getText());
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, propertyCombobox.getText());
							}
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
					Object value = menuItem != null
						? (isExtraProperties ? menuItem.getExtraProperty(categoryName, propertyName) : menuItem.getCustomPropertyValue(propertyName)) : "";
					propertyTextbox.setText(value != null ? value.toString() : "");
				});
				propertyTextbox.addModifyListener(new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent e)
					{
						if (selectedMenuItem != null && !initializingData)
						{
							if (isExtraProperties)
							{
								selectedMenuItem.putExtraProperty(categoryName, propertyName, propertyTextbox.getText());
							}
							else
							{
								selectedMenuItem.putCustomPropertyValue(propertyName, propertyTextbox.getText());
							}
							flagModified();
						}
					}
				});
			}
		}
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

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		super.doSave(monitor);
		if (!isDirty())
		{
			this.menuEditorDragAndDropListener.saveDragAndDrops();
		}
	}

	@Override
	public void revert(boolean force)
	{
		this.menuEditorDragAndDropListener.revertDragAndDrops();
		super.revert(force);
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

	private TreeItem findTreeItem(TreeItem[] items, Point pt)
	{
		if (items != null)
		{
			for (TreeItem item : items)
			{
				if (item.getBounds(CI_ADD).contains(pt) || item.getBounds(CI_DELETE).contains(pt))
				{
					return item;
				}
				TreeItem found = findTreeItem(item.getItems(), pt);
				if (found != null)
				{
					return found;
				}
			}
		}

		return null;

	}
}
