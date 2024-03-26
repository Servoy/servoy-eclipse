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
package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AcceptAllFilter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.model.builder.ServoyBuilder.Problem;
import com.servoy.eclipse.model.builder.ServoyValuelistBuilder;
import com.servoy.eclipse.model.util.DataSourceWrapperFactory;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.SortDialog;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.editors.valuelist.ValueListDPSelectionComposite;
import com.servoy.eclipse.ui.labelproviders.AccesCheckingContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.property.MethodPropertyController.MethodValueEditor;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.RelationPropertyController;
import com.servoy.eclipse.ui.property.TableValueEditor;
import com.servoy.eclipse.ui.property.ValuelistPropertyController;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil.Encapsulation2StringConverter;
import com.servoy.eclipse.ui.util.EditorUtil.String2EncapsulationConverter;
import com.servoy.eclipse.ui.util.EditorUtil.String2TypeConverter;
import com.servoy.eclipse.ui.util.EditorUtil.Type2StringConverter;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.util.IStatusChangedListener;
import com.servoy.eclipse.ui.views.TreeSelectObservableValue;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;

/**
 * @author jcompagner
 *
 */
public class ValueListEditor extends PersistEditor
{
	private Composite valueListEditorComposite;
	private final Set<Control> disableInMobileControls = new HashSet<Control>();

	private DataBindingContext m_bindingContext;
	private Text separator_char;
	private ValueListDPSelectionComposite dp_select1;
	private ValueListDPSelectionComposite dp_select2;
	private ValueListDPSelectionComposite dp_select3;

	private TreeSelectViewer relationSelect;
	private TreeSelectViewer tableSelect;

	private TreeSelectViewer globalMethodSelect;
	private Button globalMethodValuesButton;
	private SelectionAdapter globalMethodValuesSelectionListener;
	private ISelectionChangedListener globalMethodSelectionListener;

	private Button customValuesButton;
	private Button tableValuesButton;

	private Text customValues;
	private Text nameField;

	private Button applyValuelistNameButton;
	private Button relatedValuesButton;

	private Group definitionGroup;
	private Label separatorCharacterLabel;

	private Button allowEmptyValueButton;
	private Button lazyLoading;
	private TreeSelectViewer sortingDefinitionSelect;

	private ITable currentTable;
	private TreeSelectViewer fallbackValuelist;

	private Text deprecated;
	private Combo encapsulation;
	private Text commentText;

	private int databaseValuesTypeOverride = -1; // used when the user clicks the related values button but realtionName has not been set yet

	private SelectionAdapter customvalueButtonSelectionListener;
	private SelectionAdapter tableValuesButtonSelectionListener;
	private SelectionAdapter relatedValuesButtoneSelectionListener;
	private ISelectionChangedListener tableSelectionListener;
	private ISelectionChangedListener relationSelectionListener;
	private IStatusChangedListener statusChangeListener;

	// type definition widgets
	private Combo displayType, realType;
	private Label displayTypeLabel, realTypeLabel;
	private Group typeDefinitionGroup;
	private Composite parent;

	@Override
	protected boolean validatePersist(IPersist persist)
	{
		return persist instanceof ValueList;
	}

	public ValueList getValueList()
	{
		return (ValueList)getPersist();
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
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
		setTitleImage(Activator.loadImageDescriptorFromBundle("valuelist.png").createImage());
		FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(getPersist());

		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		valueListEditorComposite = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");
		Label nameLabel = new Label(valueListEditorComposite, SWT.NONE);
		nameLabel.setText("Valuelist Name");

		myScrolledComposite.setContent(valueListEditorComposite);

		nameField = new Text(valueListEditorComposite, SWT.BORDER);
		nameField.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);

		customValues = new Text(valueListEditorComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		customValues.setToolTipText("list with fixed displayValue[|realValue] ('^' for null real value, %%scopes.globals.NAME%% for global real value)");
		customValuesButton = new Button(valueListEditorComposite, SWT.RADIO);
		customValuesButton.setText("Custom Values");
		customvalueButtonSelectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (customValuesButton.getSelection())
				{
					handleCustomValuesButtonSelected();
					flagModified();
					refresh();
				}
			}
		};

		tableValuesButton = new Button(valueListEditorComposite, SWT.RADIO);
		tableValuesButton.setText("Table Values");
		tableValuesButtonSelectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (tableValuesButton.getSelection())
				{
					handleDatabaseValuesButtonSelected();
					flagModified();
					refresh();
				}
			}
		};
		disableInMobileControls.add(tableValuesButton);

		tableSelect = new TreeSelectViewer(valueListEditorComposite, SWT.NONE, TableValueEditor.INSTANCE);
		tableSelect.setContentProvider(new TableContentProvider());
		tableSelect.setLabelProvider(DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY);
		tableSelect.setTextLabelProvider(new DatasourceLabelProvider("", false, true));
		tableSelect.setTitleText("Select table");
		tableSelectionListener = new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)tableSelect.getSelection();
				handleTableSelected(selection.isEmpty() ? null : (IDataSourceWrapper)selection.getFirstElement());
				handleDataProvidersProperties();
				flagModified();
				refresh();
			}
		};
		tableSelect.setInput(new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, false, false));
		tableSelect.setEditable(true);
		Control tableSelectControl = tableSelect.getControl();
		disableInMobileControls.add(tableSelectControl);

		applyValuelistNameButton = new Button(valueListEditorComposite, SWT.CHECK);
		applyValuelistNameButton.setText("Apply valuelist name as filter on column 'valuelist_name'");
		disableInMobileControls.add(applyValuelistNameButton);

		relatedValuesButton = new Button(valueListEditorComposite, SWT.RADIO);
		relatedValuesButton.setText("Related Values");
		relatedValuesButtoneSelectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (relatedValuesButton.getSelection())
				{
					handleRelatedValuesButtonSelected();
					flagModified();
					refresh();
				}
			}
		};
		disableInMobileControls.add(relatedValuesButton);

		relationSelect = new TreeSelectViewer(valueListEditorComposite, SWT.NONE, RelationPropertyController.RelationValueEditor.INSTANCE);
		relationSelect.setContentProvider(new RelationContentProvider(editingFlattenedSolution, getPersist()));
		relationSelect.setLabelProvider(RelationLabelProvider.INSTANCE_LAST_NAME_ONLY);
		relationSelect.setTextLabelProvider(new RelationLabelProvider("", false, false));
		relationSelect.setSelectionFilter(AcceptAllFilter.getInstance()); // by default only leaf nodes can be selected
		relationSelect.setTitleText("Select relation");
		relationSelect.setEditable(true);
		relationSelectionListener = new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)relationSelect.getSelection();
				handleRelationSelected(selection.isEmpty() ? null : (RelationsWrapper)selection.getFirstElement());
				handleDataProvidersProperties();
				flagModified();
				refresh();
			}
		};
		relationSelect.setInput(new RelationContentProvider.RelationListOptions(null, null, false, true));
		Control relationSelectControl = relationSelect.getControl();
		disableInMobileControls.add(relationSelectControl);

		globalMethodValuesButton = new Button(valueListEditorComposite, SWT.RADIO);
		globalMethodValuesButton.setText("Global method");
		globalMethodValuesSelectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (globalMethodValuesButton.getSelection())
				{
					handleGlobalMethodButtonSelected();
					flagModified();
					refresh();
				}
			}
		};
		disableInMobileControls.add(globalMethodValuesButton);

		// use the solution as context, private methods from the same solution are allowed
		final PersistContext context = PersistContext.create(getValueList().getParent());

		globalMethodSelect = new TreeSelectViewer(valueListEditorComposite, SWT.NONE, new MethodValueEditor(context))
		{
			@Override
			public IStructuredSelection openDialogBox(Control cellEditorWindow)
			{
				final MethodDialog dialog = new MethodDialog(cellEditorWindow.getShell(), (ILabelProvider)getLabelProvider(), getContentProvider(),
					getSelection(), getInput(), SWT.NONE, "Select Method", null);
				dialog.setOptionsAreaFactory(new IControlFactory()
				{
					public Control createControl(Composite composite)
					{
						final AddMethodButtonsComposite buttons = new AddMethodButtonsComposite(composite, SWT.NONE);
						buttons.setContext(context, "valueListGlobalMethod");
						buttons.setDialog(dialog);
						buttons.searchSelectedScope((IStructuredSelection)dialog.getTreeViewer().getViewer().getSelection());
						dialog.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener()
						{
							public void selectionChanged(SelectionChangedEvent event)
							{
								buttons.searchSelectedScope((IStructuredSelection)event.getSelection());
							}
						});
						return buttons;
					}
				});
				dialog.open();

				if (dialog.getReturnCode() == Window.CANCEL)
				{
					return null;
				}
				return (IStructuredSelection)dialog.getSelection(); // single select
			}
		};
		globalMethodSelect.setContentProvider(new MethodDialog.MethodTreeContentProvider(context));
		globalMethodSelect.setLabelProvider(new AccesCheckingContextDelegateLabelProvider(
			new SolutionContextDelegateLabelProvider(new MethodLabelProvider(context, false, false), context.getContext())));
		globalMethodSelect.setTextLabelProvider(new AccesCheckingContextDelegateLabelProvider(
			new SolutionContextDelegateLabelProvider(new MethodLabelProvider(context, true, false), getValueList().getParent(), true)));
		globalMethodSelect.setInput(new MethodListOptions(false, false, false, true, false, null));
		globalMethodSelect.setEditable(true);
		Control globalMethodSelectControl = globalMethodSelect.getControl();
		disableInMobileControls.add(globalMethodSelectControl);

		globalMethodSelectionListener = new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)globalMethodSelect.getSelection();
				if (!selection.isEmpty() && selection.getFirstElement() instanceof MethodWithArguments)
				{
					handleGlobalMethodSelected((MethodWithArguments)selection.getFirstElement());
					flagModified();
					refresh();
				}
			}
		};

		definitionGroup = new Group(valueListEditorComposite, SWT.NONE);
		definitionGroup.setText("Definition");
		disableInMobileControls.add(definitionGroup);
		allowEmptyValueButton = new Button(valueListEditorComposite, SWT.CHECK);
		allowEmptyValueButton.setText("Allow empty value");
		disableInMobileControls.add(allowEmptyValueButton);

		lazyLoading = new Button(valueListEditorComposite, SWT.CHECK);
		lazyLoading.setText("Lazy loading (NGClient)");
		lazyLoading.setToolTipText("Global method valuelists usually have the lowest performance among all valuelists.\n" +
			"For NG Client, we added 'lazy loading' so components can only load the valuelist when values need to be displayed.\n" +
			"This flag has to be set both on valuelist and in component spec, on the valuelist property.\n\n" +

			"IMPORTANT: Usage of real & display values is not supported with lazy loading. Don't set lazy load if your method returns both real and display values!");
		disableInMobileControls.add(lazyLoading);

		dp_select1 = new ValueListDPSelectionComposite(definitionGroup, editingFlattenedSolution, SWT.NONE);
		dp_select2 = new ValueListDPSelectionComposite(definitionGroup, editingFlattenedSolution, SWT.NONE);
		dp_select3 = new ValueListDPSelectionComposite(definitionGroup, editingFlattenedSolution, SWT.NONE);

		separatorCharacterLabel = new Label(definitionGroup, SWT.NONE);
		separatorCharacterLabel.setText("Separator character");
		separator_char = new Text(definitionGroup, SWT.BORDER);

		sortingDefinitionSelect = new TreeSelectViewer(valueListEditorComposite, SWT.NONE)
		{
			@Override
			protected IStructuredSelection openDialogBox(Control control)
			{
				IStructuredSelection selection = (IStructuredSelection)getSelection();
				String sortOptions = (String)(selection.isEmpty() ? null : selection.getFirstElement());
				if (currentTable != null)
				{
					FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(getPersist());

					SortDialog dialog = new SortDialog(control.getShell(), flattenedSolution, currentTable, sortOptions, "Sort options");
					dialog.open();

					if (dialog.getReturnCode() != Window.CANCEL)
					{
						sortOptions = dialog.getValue().toString();
					}
				}

				return sortOptions == null || sortOptions.length() == 0 ? StructuredSelection.EMPTY : new StructuredSelection(sortOptions);
			}
		};
		sortingDefinitionSelect.setButtonText("Sorting Definition...");
		Control sortingDefinitionControl = sortingDefinitionSelect.getControl();
		disableInMobileControls.add(sortingDefinitionControl);

		fallbackValuelist = new TreeSelectViewer(valueListEditorComposite, SWT.NONE,
			new ValuelistPropertyController.ValueListValueEditor(editingFlattenedSolution));
		fallbackValuelist.setButtonText("Fallback Valuelist");
		fallbackValuelist.setContentProvider(new FallbackValuelistContentProvider(editingFlattenedSolution, getValueList()));
		fallbackValuelist.setLabelProvider(new ValuelistLabelProvider(editingFlattenedSolution, getPersist()));
		fallbackValuelist.setTitleText("Select fallback valuelist");
		fallbackValuelist.setInput(getValueList());
		fallbackValuelist.setEditable(true);

		Control fallbackValueListControl = fallbackValuelist.getControl();
		disableInMobileControls.add(fallbackValueListControl);

		statusChangeListener = new IStatusChangedListener()
		{
			public void statusChanged(boolean valid)
			{
				flagModified();
			}
		};

		Label deprecatedLabel;
		deprecatedLabel = new Label(valueListEditorComposite, SWT.NONE);
		deprecatedLabel.setText("Deprecated");
		deprecated = new Text(valueListEditorComposite, SWT.BORDER);
		Label encapsulationLabel;
		encapsulationLabel = new Label(valueListEditorComposite, SWT.NONE);
		encapsulationLabel.setText("Encapsulation");
		encapsulation = new Combo(valueListEditorComposite, SWT.READ_ONLY);
		encapsulation.setItems(new String[] { Messages.Public, Messages.ModuleScope });

		typeDefinitionGroup = new Group(valueListEditorComposite, SWT.NONE);
		typeDefinitionGroup.setText("Type definition");

		displayTypeLabel = new Label(typeDefinitionGroup, SWT.NONE);
		displayTypeLabel.setText("Display value type");
		displayType = new Combo(typeDefinitionGroup, SWT.READ_ONLY);
		displayType.setItems(Type2StringConverter.INSTANCE.getAllTypes());

		realTypeLabel = new Label(typeDefinitionGroup, SWT.NONE);
		realTypeLabel.setText("Real value type");
		realType = new Combo(typeDefinitionGroup, SWT.READ_ONLY);
		realType.setItems(Type2StringConverter.INSTANCE.getAllTypes());

		Label commentLabel = new Label(valueListEditorComposite, SWT.NONE);
		commentLabel.setText("Comment");
		commentText = new Text(valueListEditorComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);

		final GroupLayout groupLayout_0 = new GroupLayout(typeDefinitionGroup);
		groupLayout_0.setHorizontalGroup(
			groupLayout_0.createSequentialGroup().addContainerGap().add(displayTypeLabel).addPreferredGap(LayoutStyle.RELATED).add(displayType,
				GroupLayout.PREFERRED_SIZE, 228, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.UNRELATED).add(realTypeLabel).addPreferredGap(
					LayoutStyle.RELATED)
				.add(realType, GroupLayout.PREFERRED_SIZE, 228, GroupLayout.PREFERRED_SIZE));
		groupLayout_0.setVerticalGroup(groupLayout_0.createSequentialGroup().addContainerGap().add(
			groupLayout_0.createParallelGroup().add(displayTypeLabel).add(displayType).add(realTypeLabel).add(realType)));
		typeDefinitionGroup.setLayout(groupLayout_0);

		final GroupLayout groupLayout_1 = new GroupLayout(definitionGroup);
		groupLayout_1.setHorizontalGroup(groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout_1.createSequentialGroup().add(groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout_1.createSequentialGroup().addContainerGap().add(dp_select1, GroupLayout.PREFERRED_SIZE, 190, Short.MAX_VALUE).addPreferredGap(
					LayoutStyle.RELATED).add(dp_select2, GroupLayout.PREFERRED_SIZE, 191, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(dp_select3,
						GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE))
				.add(
					groupLayout_1.createSequentialGroup().add(8, 8, 8).add(separatorCharacterLabel).addPreferredGap(LayoutStyle.RELATED).add(
						separator_char, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)))
				.addContainerGap()));
		groupLayout_1.setVerticalGroup(groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(groupLayout_1.createSequentialGroup().addContainerGap().add(
			groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(dp_select1, GroupLayout.PREFERRED_SIZE, 149, Short.MAX_VALUE).add(dp_select3,
				GroupLayout.PREFERRED_SIZE, 149, Short.MAX_VALUE).add(dp_select2, GroupLayout.PREFERRED_SIZE, 149, Short.MAX_VALUE))
			.addPreferredGap(
				LayoutStyle.RELATED)
			.add(
				groupLayout_1.createParallelGroup(GroupLayout.BASELINE).add(separator_char, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(separatorCharacterLabel))
			.addContainerGap()));
		definitionGroup.setLayout(groupLayout_1);

		final GroupLayout groupLayout = new GroupLayout(valueListEditorComposite);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(
			groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(nameLabel).addPreferredGap(
					LayoutStyle.RELATED).add(nameField, 100, 200, Short.MAX_VALUE)).add(customValuesButton))
				.addPreferredGap(LayoutStyle.RELATED)).add(
					groupLayout.createSequentialGroup().add(19, 19, 19).add(customValues, 100, 200, Short.MAX_VALUE)))
			.add(9, 9, 9)).add(
				groupLayout.createSequentialGroup().addContainerGap().add(fallbackValueListControl, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(40, 40, 40).add(allowEmptyValueButton,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.add(40, 40, 40).addPreferredGap(
						LayoutStyle.RELATED)
					.add(sortingDefinitionControl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE)
					.addContainerGap())
			.add(
				groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(
					GroupLayout.LEADING).add(deprecatedLabel).add(encapsulationLabel)).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(deprecated, GroupLayout.PREFERRED_SIZE, 228,
							GroupLayout.PREFERRED_SIZE).add(encapsulation, GroupLayout.PREFERRED_SIZE, 228,
								GroupLayout.PREFERRED_SIZE))
					.add(10, 10, 10).add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(commentLabel)).addPreferredGap(
						LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(commentText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE))
					.addContainerGap())
			.add(groupLayout.createSequentialGroup().add(36,
				36, 36).add(applyValuelistNameButton).addContainerGap())
			.add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(
						groupLayout.createSequentialGroup().add(relatedValuesButton,
							GroupLayout.PREFERRED_SIZE, 134,
							GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(
								relationSelectControl, GroupLayout.DEFAULT_SIZE, 200,
								Short.MAX_VALUE))
						.add(
							groupLayout.createSequentialGroup().add(globalMethodValuesButton,
								GroupLayout.PREFERRED_SIZE, 134,
								GroupLayout.PREFERRED_SIZE).addPreferredGap(
									LayoutStyle.RELATED)
								.add(globalMethodSelectControl,
									GroupLayout.DEFAULT_SIZE, 200,
									Short.MAX_VALUE)
								.addPreferredGap(
									LayoutStyle.RELATED)
								.add(lazyLoading,
									GroupLayout.PREFERRED_SIZE,
									GroupLayout.PREFERRED_SIZE,
									GroupLayout.PREFERRED_SIZE))
						.add(
							groupLayout.createSequentialGroup().add(
								tableValuesButton,
								GroupLayout.PREFERRED_SIZE, 134,
								GroupLayout.PREFERRED_SIZE).addPreferredGap(
									LayoutStyle.RELATED)
								.add(
									tableSelectControl,
									GroupLayout.DEFAULT_SIZE, 200,
									Short.MAX_VALUE)))
					.add(0, 200,
						318))
			.add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					definitionGroup,
					100, 200,
					Short.MAX_VALUE).addContainerGap())
			.add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					typeDefinitionGroup,
					100, 200,
					Short.MAX_VALUE).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(nameLabel).add(nameField,
				GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
					customValuesButton)
				.addPreferredGap(LayoutStyle.RELATED).add(customValues, 50, 100, 400).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.CENTER).add(globalMethodValuesButton).add(globalMethodSelectControl).add(
						lazyLoading))
				.addPreferredGap(LayoutStyle.RELATED).add(typeDefinitionGroup, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
					Short.MAX_VALUE)
				.add(9, 9, 9).add(
					groupLayout.createParallelGroup(GroupLayout.TRAILING).add(tableValuesButton).add(tableSelectControl))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(applyValuelistNameButton).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(relatedValuesButton).add(
						relationSelectControl))
				.addPreferredGap(LayoutStyle.RELATED).add(definitionGroup, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.add(9, 9, 9).add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(
						deprecatedLabel).add(9, 9, 9).add(
							encapsulationLabel))
					.add(
						groupLayout.createSequentialGroup().add(
							deprecated).add(9, 9, 9).add(
								encapsulation))
					.add(commentLabel).add(commentText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE))
				.add(9, 9,
					9)
				.add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(
						groupLayout.createParallelGroup(GroupLayout.BASELINE).add(
							fallbackValueListControl))
						.add(
							groupLayout.createParallelGroup(GroupLayout.BASELINE).add(
								sortingDefinitionControl).add(allowEmptyValueButton)))
				.add(
					24, 24, 24)));
		valueListEditorComposite.setLayout(groupLayout);

		myScrolledComposite.setMinSize(valueListEditorComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		initDataBindings();

		if ((getValueList().getValueListType() == IValueListConstants.CUSTOM_VALUES ||
			getValueList().getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES) &&
			(getValueList().getDisplayValueType() == 0 || getValueList().getRealValueType() == 0))
		{
			if (getValueList().getDisplayValueType() == 0) getValueList().setDisplayValueType(IColumnTypes.TEXT);
			if (getValueList().getRealValueType() == 0) getValueList().setRealValueType(IColumnTypes.TEXT);
			getValueList().flagChanged();
			flagModified();
		}

		// mark dirty if invalid - so that user is hinted to save it as a valid valuelist
		String message = checkValidState();
		if (message == null)
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			List<Problem> problems = ServoyValuelistBuilder.checkValuelist(getValueList(), servoyModel.getFlattenedSolution(),
				ApplicationServerRegistry.get().getServerManager(), false);
			for (Problem problem : problems)
			{
				if (problem.fix != null)
				{
					message = problem.message;
					break;
				}
			}
		}
		if (message != null)
		{
			MessageDialog.openWarning(getSite().getShell(), "Invalid valuelist", message);
			getValueList().flagChanged();
			flagModified();
		}

		// currently we only support custom vl in mobile
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolutionMetaData().getSolutionType() == SolutionMetaData.MOBILE &&
			getValueList().getValueListType() != IValueListConstants.CUSTOM_VALUES)
		{
			handleCustomValuesButtonSelected();
		}
		parent.requestLayout();
		doRefresh();
	}

	public void updateTypeDefinitionWidgets()
	{
		ValueListDPSelectionComposite[] dpSelectionCompositeA = { dp_select1, dp_select2, dp_select3 };
		boolean isDisplayValueSet = false;
		int displayValueType = IColumnTypes.TEXT;
		int realValueType = IColumnTypes.TEXT;
		boolean isRealValueSet = false;
		for (ValueListDPSelectionComposite dpSelectionComposite : dpSelectionCompositeA)
		{
			if (dpSelectionComposite.isEnabled())
			{
				if (!isDisplayValueSet && dpSelectionComposite.getShowInField())
				{
					displayValueType = dpSelectionComposite.getDataProviderType();
					isDisplayValueSet = true;
				}

				if (dpSelectionComposite.getReturnInDataProvider())
				{
					realValueType = isRealValueSet ? IColumnTypes.TEXT : dpSelectionComposite.getDataProviderType();
					isRealValueSet = true;
				}
			}
		}
		if (isDisplayValueSet && (displayValueType != getValueList().getDisplayValueType()))
		{
			getValueList().setDisplayValueType(displayValueType);
		}
		if (isRealValueSet && (realValueType != getValueList().getRealValueType()))
		{
			getValueList().setRealValueType(realValueType);
		}
		if (m_bindingContext != null) m_bindingContext.updateTargets();
	}

	@Override
	protected void doRefresh()
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		boolean activeSolutionIsMobile = servoyModel.isActiveSolutionMobile();

		removeListeners();

		if (!activeSolutionIsMobile)
		{
			for (Control control : valueListEditorComposite.getChildren())
			{
				if (disableInMobileControls.contains(control))
				{
					control.setEnabled(true);
				}
			}
		}

		ValueList valueList = getValueList();
		try
		{
			int databaseValuesType;
			if (databaseValuesTypeOverride == -1)
			{
				databaseValuesType = valueList.getDatabaseValuesType();
			}
			else
			{
				databaseValuesType = databaseValuesTypeOverride;
				databaseValuesTypeOverride = -1;
			}
			customValuesButton.setSelection(valueList.getValueListType() == IValueListConstants.CUSTOM_VALUES);
			customValues.setEnabled(customValuesButton.getSelection());

			globalMethodValuesButton.setSelection(valueList.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES);

			tableValuesButton.setSelection(
				valueList.getValueListType() == IValueListConstants.DATABASE_VALUES && databaseValuesType == IValueListConstants.TABLE_VALUES);
			applyValuelistNameButton.setEnabled(tableValuesButton.getSelection());
			relatedValuesButton.setSelection(
				valueList.getValueListType() == IValueListConstants.DATABASE_VALUES && databaseValuesType == IValueListConstants.RELATED_VALUES);

			FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(getPersist());
			ITable table = null;
			if (valueList.getValueListType() == IValueListConstants.DATABASE_VALUES && databaseValuesType == IValueListConstants.TABLE_VALUES)
			{
				IDataSourceWrapper wrapper = DataSourceWrapperFactory.getWrapper(valueList.getDataSource());
				if (wrapper == null)
				{
					tableSelect.setSelection(StructuredSelection.EMPTY);
				}
				else
				{
					tableSelect.setSelection(new StructuredSelection(wrapper));
					table = flattenedSolution.getTable(valueList.getDataSource());
				}
			}
			else
			{
				tableSelect.setSelection(StructuredSelection.EMPTY);
			}

			if (valueList.getValueListType() == IValueListConstants.DATABASE_VALUES && databaseValuesType == IValueListConstants.RELATED_VALUES)
			{
				Relation[] relations = flattenedSolution.getRelationSequence(valueList.getRelationName());
				if (relations == null)
				{
					relationSelect.setSelection(StructuredSelection.EMPTY);
				}
				else
				{
					relationSelect.setSelection(new StructuredSelection(new RelationsWrapper(relations)));
					table = flattenedSolution.getTable(relations[relations.length - 1].getForeignDataSource());
				}
			}
			else
			{
				relationSelect.setSelection(StructuredSelection.EMPTY);
			}

			if (valueList.getValueListType() != IValueListConstants.CUSTOM_VALUES && valueList.getValueListType() != IValueListConstants.GLOBAL_METHOD_VALUES)
			{
				customValues.setText("");
			}

			boolean isCustomOrGlobalValuelist = valueList.getValueListType() == IValueListConstants.CUSTOM_VALUES ||
				valueList.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES;
			realType.setEnabled(isCustomOrGlobalValuelist);
			displayType.setEnabled(isCustomOrGlobalValuelist);

			Color usedColor = isCustomOrGlobalValuelist ? globalMethodValuesButton.getForeground() : Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			realTypeLabel.setForeground(usedColor);
			displayTypeLabel.setForeground(usedColor);
			typeDefinitionGroup.setForeground(usedColor);


			currentTable = table;

			dp_select1.initDataBindings(table);
			dp_select2.initDataBindings(table);
			dp_select3.initDataBindings(table);

			boolean enabled = table != null;
			usedColor = enabled ? globalMethodValuesButton.getForeground() : Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			definitionGroup.setForeground(usedColor);

			dp_select1.setEnabled(enabled);
			dp_select2.setEnabled(enabled);
			dp_select3.setEnabled(enabled);

			sortingDefinitionSelect.setEnabled(enabled);
			separator_char.setEnabled(enabled);
			separatorCharacterLabel.setEnabled(enabled);

			if (enabled && valueList.getSortOptions() != null)
			{
				sortingDefinitionSelect.setSelection(new StructuredSelection(valueList.getSortOptions()));
			}
			else
			{
				sortingDefinitionSelect.setSelection(StructuredSelection.EMPTY);
			}

			if (activeSolutionIsMobile)
			{
				for (Control control : valueListEditorComposite.getChildren())
				{
					if (disableInMobileControls.contains(control))
					{
						control.setEnabled(false);
					}
				}
			}

			m_bindingContext.updateTargets();
		}
		finally
		{
			addListeners();
		}
	}

	private void addListeners()
	{
		customValuesButton.addSelectionListener(customvalueButtonSelectionListener);
		tableValuesButton.addSelectionListener(tableValuesButtonSelectionListener);
		relatedValuesButton.addSelectionListener(relatedValuesButtoneSelectionListener);
		tableSelect.addSelectionChangedListener(tableSelectionListener);
		tableSelect.addStatusChangedListener(statusChangeListener);
		relationSelect.addSelectionChangedListener(relationSelectionListener);
		relationSelect.addStatusChangedListener(statusChangeListener);
		globalMethodValuesButton.addSelectionListener(globalMethodValuesSelectionListener);
		globalMethodSelect.addSelectionChangedListener(globalMethodSelectionListener);
		globalMethodSelect.addStatusChangedListener(statusChangeListener);
		fallbackValuelist.addStatusChangedListener(statusChangeListener);
		sortingDefinitionSelect.addStatusChangedListener(statusChangeListener);
	}

	private void removeListeners()
	{
		customValuesButton.removeSelectionListener(customvalueButtonSelectionListener);
		tableValuesButton.removeSelectionListener(tableValuesButtonSelectionListener);
		relatedValuesButton.removeSelectionListener(relatedValuesButtoneSelectionListener);
		tableSelect.removeSelectionChangedListener(tableSelectionListener);
		tableSelect.removeStatusChangedListener(statusChangeListener);
		relationSelect.removeSelectionChangedListener(relationSelectionListener);
		relationSelect.removeStatusChangedListener(statusChangeListener);
		globalMethodValuesButton.removeSelectionListener(globalMethodValuesSelectionListener);
		globalMethodSelect.removeSelectionChangedListener(globalMethodSelectionListener);
		globalMethodSelect.removeStatusChangedListener(statusChangeListener);
		fallbackValuelist.removeStatusChangedListener(statusChangeListener);
		sortingDefinitionSelect.removeStatusChangedListener(statusChangeListener);
	}

	private DataBindingContext defineObservablesAndBindingContext()
	{
		m_bindingContext = BindingHelper.dispose(m_bindingContext);

		IObservableValue getValueListCustomValuesObserveValue = PojoProperties.value("customValues").observe(getValueList());
		IObservableValue fallbackValueListObserveValue = PojoProperties.value("fallbackValueListID").observe(getValueList());

		IObservableValue customValuesTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(customValues);
		IObservableValue globalMethodtObserveWidget = new TreeSelectObservableValue(globalMethodSelect, MethodWithArguments.class);
		IObservableValue fallbackValueListObserveWidget = new TreeSelectObservableValue(fallbackValuelist, int.class);

		IObservableValue getValueListSortOpotionsObserveValue = PojoProperties.value("sortOptions").observe(getValueList());
		IObservableValue sortingDefinitionSelectObserveWidget = new TreeSelectObservableValue(sortingDefinitionSelect, String.class);

		IObservableValue nameFieldTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(nameField);
		IObservableValue getValueListNameObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				return getValueList().getName();
			}

			@Override
			protected void doSetValue(Object value)
			{
				//setName cannot be invoked, save does update name
				getValueList().flagChanged();
				flagModified();
			}
		};
		IObservableValue separatorFieldTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(separator_char);
		IObservableValue getValueListSeparatorObserveValue = PojoProperties.value("separator").observe(getValueList());

		IObservableValue allowEmptyFieldTextObserveWidget = WidgetProperties.widgetSelection().observe(allowEmptyValueButton);
		IObservableValue getValueListAllowEmptyValueObserveValue = PojoProperties.value("addEmptyValue").observe(getValueList());
		IObservableValue lazyLoadingTextObserveWidget = WidgetProperties.widgetSelection().observe(lazyLoading);
		IObservableValue getValueListLazyLoadingObserveValue = PojoProperties.value("lazyLoading").observe(getValueList());
		IObservableValue applyNameFilterSelectionObserveWidget = WidgetProperties.widgetSelection().observe(applyValuelistNameButton);
		IObservableValue getApplyNameFilterSelectionObserveValue = PojoProperties.value("useTableFilter").observe(getValueList());
		IObservableValue deprecatedObserveWidget = WidgetProperties.text(SWT.Modify).observe(deprecated);
		IObservableValue deprecatedObserveValue = PojoProperties.value("deprecated").observe(getValueList());
		IObservableValue encapsulationObserveWidget = WidgetProperties.widgetSelection().observe(encapsulation);
		IObservableValue encapsulationObserveValue = PojoProperties.value("encapsulation").observe(getValueList());
		IObservableValue commentObserveWidget = WidgetProperties.text(SWT.Modify).observe(commentText);
		IObservableValue commentObserveValue = PojoProperties.value("comment").observe(getValueList());

		IObservableValue displayTypeObserveWidget = WidgetProperties.widgetSelection().observe(displayType);
		IObservableValue displayTypeObserveValue = PojoProperties.value("displayValueType").observe(getValueList());
		IObservableValue realTypeObserveWidget = WidgetProperties.widgetSelection().observe(realType);
		IObservableValue realTypeObserveValue = PojoProperties.value("realValueType").observe(getValueList());

		m_bindingContext = new DataBindingContext();
		//
		m_bindingContext.bindValue(fallbackValueListObserveWidget, fallbackValueListObserveValue,
			new UpdateValueStrategy().setConverter(new Converter(Integer.class, Integer.class)
			{
				public Object convert(Object fromObject)
				{
					return fromObject == null ? Integer.valueOf(0) : fromObject;
				}
			}), new UpdateValueStrategy());

		m_bindingContext.bindValue(nameFieldTextObserveWidget, getValueListNameObserveValue, null, null);
		m_bindingContext.bindValue(lazyLoadingTextObserveWidget, getValueListLazyLoadingObserveValue, null, null);
		m_bindingContext.bindValue(customValuesTextObserveWidget, getValueListCustomValuesObserveValue,
			new UpdateValueStrategy().setConverter(new Converter(String.class, String.class)
			{
				public Object convert(Object fromObject)
				{
					if ("".equals(fromObject)) return null;
					return fromObject;
				}
			}), new UpdateValueStrategy().setConverter(new Converter(String.class, String.class)
			{
				/**
				 * @see org.eclipse.core.databinding.conversion.IConverter#convert(java.lang.Object)
				 */
				public Object convert(Object fromObject)
				{
					if (fromObject == null) return "";
					if (getValueList().getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
					{
						return "";
					}
					return fromObject;
				}
			}));
		m_bindingContext.bindValue(globalMethodtObserveWidget, getValueListCustomValuesObserveValue,
			new UpdateValueStrategy().setConverter(new Converter(MethodWithArguments.class, String.class)
			{
				public Object convert(Object fromObject)
				{
					if ("".equals(fromObject)) return null;
					if (fromObject instanceof String) return fromObject;
					if (fromObject instanceof MethodWithArguments)
					{
						FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(getPersist());
						ScriptMethod scriptMethod = fs.getScriptMethod(((MethodWithArguments)fromObject).methodId);
						if (scriptMethod != null)
						{
							return scriptMethod.getPrefixedName();
						}
					}
					return null;
				}
			}), new UpdateValueStrategy().setConverter(new Converter(String.class, MethodWithArguments.class)
			{
				public Object convert(Object fromObject)
				{
					if (fromObject == null || getValueList().getValueListType() != IValueListConstants.GLOBAL_METHOD_VALUES)
					{
						return null;
					}

					FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(getPersist());
					ScriptMethod scriptMethod = fs.getScriptMethod(fromObject.toString());
					if (scriptMethod != null)
					{
						return MethodWithArguments.create(scriptMethod, null);
					}

					Pair<String, String> scope = ScopesUtils.getVariableScope(fromObject.toString());
					return new MethodWithArguments.UnresolvedMethodWithArguments(ScopesUtils.getScopeString(scope));
				}
			}));

		m_bindingContext.bindValue(sortingDefinitionSelectObserveWidget, getValueListSortOpotionsObserveValue, null, null);
		m_bindingContext.bindValue(allowEmptyFieldTextObserveWidget, getValueListAllowEmptyValueObserveValue,
			new UpdateValueStrategy().setConverter(new Converter(boolean.class, int.class)
			{
				public Object convert(Object fromObject)
				{
					return Integer.valueOf(Boolean.TRUE.equals(fromObject) ? IValueListConstants.EMPTY_VALUE_ALWAYS : IValueListConstants.EMPTY_VALUE_NEVER);
				}
			}), new UpdateValueStrategy().setConverter(new Converter(int.class, boolean.class)
			{
				public Object convert(Object fromObject)
				{
					return Boolean.valueOf(fromObject instanceof Integer && ((Integer)fromObject).intValue() == IValueListConstants.EMPTY_VALUE_ALWAYS);
				}
			}));
		m_bindingContext.bindValue(applyNameFilterSelectionObserveWidget, getApplyNameFilterSelectionObserveValue, null, null);
		m_bindingContext.bindValue(separatorFieldTextObserveWidget, getValueListSeparatorObserveValue, null, null);
		m_bindingContext.bindValue(deprecatedObserveWidget, deprecatedObserveValue, new UpdateValueStrategy(), new UpdateValueStrategy());
		m_bindingContext.bindValue(commentObserveWidget, commentObserveValue, new UpdateValueStrategy(), new UpdateValueStrategy());
		m_bindingContext.bindValue(encapsulationObserveWidget, encapsulationObserveValue,
			new UpdateValueStrategy().setConverter(String2EncapsulationConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(Encapsulation2StringConverter.INSTANCE));
		m_bindingContext.bindValue(displayTypeObserveWidget, displayTypeObserveValue, new UpdateValueStrategy().setConverter(String2TypeConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(Type2StringConverter.INSTANCE));
		m_bindingContext.bindValue(realTypeObserveWidget, realTypeObserveValue, new UpdateValueStrategy().setConverter(String2TypeConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(Type2StringConverter.INSTANCE));

		BindingHelper.addGlobalChangeListener(m_bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				flagModified();
			}
		});
		return m_bindingContext;
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
	}

	@Override
	public boolean isDirty()
	{
		return super.isDirty() || (tableSelect != null && !tableSelect.isValid()) || (relationSelect != null && !relationSelect.isValid()) ||
			(globalMethodSelect != null && !globalMethodSelect.isValid()) || (sortingDefinitionSelect != null && !sortingDefinitionSelect.isValid()) ||
			(fallbackValuelist != null && !fallbackValuelist.isValid());
	}

	private void handleGlobalMethodButtonSelected()
	{
		getValueList().setValueListType(IValueListConstants.GLOBAL_METHOD_VALUES);
		getValueList().setDataSource(null);
		getValueList().setRelationName(null);
		getValueList().setCustomValues(null);
	}

	private void handleCustomValuesButtonSelected()
	{
		getValueList().setValueListType(IValueListConstants.CUSTOM_VALUES);
		getValueList().setDataSource(null);
		getValueList().setRelationName(null);
		getValueList().setCustomValues(null);
		getValueList().setLazyLoading(false);
	}

	private void handleDatabaseValuesButtonSelected()
	{
		getValueList().setValueListType(IValueListConstants.DATABASE_VALUES);
		getValueList().setCustomValues(null);
		getValueList().setRelationName(null);
		getValueList().setLazyLoading(false);
		databaseValuesTypeOverride = IValueListConstants.TABLE_VALUES;
	}

	private void handleRelatedValuesButtonSelected()
	{
		getValueList().setValueListType(IValueListConstants.DATABASE_VALUES);
		getValueList().setCustomValues(null);
		getValueList().setDataSource(null);
		getValueList().setLazyLoading(false);
		databaseValuesTypeOverride = IValueListConstants.RELATED_VALUES;
	}

	private void handleGlobalMethodSelected(MethodWithArguments methodWithArguments)
	{
		if (methodWithArguments == null)
		{
			getValueList().setCustomValues(null);
		}
		else
		{
			handleGlobalMethodButtonSelected();
			FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(getPersist());
			ScriptMethod scriptMethod = fs.getScriptMethod(methodWithArguments.methodId);
			if (scriptMethod != null)
			{
				getValueList().setCustomValues(scriptMethod.getUUID().toString());
			}
		}
	}

	private void handleTableSelected(IDataSourceWrapper tableWrapper)
	{
		if (tableWrapper == null)
		{
			getValueList().setDataSource(null);
		}
		else
		{
			handleDatabaseValuesButtonSelected();
			getValueList().setDataSource(tableWrapper.getDataSource());
			try
			{
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				if (servoyModel.getDataModelManager() != null)
				{
					servoyModel.getDataModelManager().testTableAndCreateDBIFile(servoyModel.getDataSourceManager().getDataSource(tableWrapper.getDataSource()));
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void handleRelationSelected(RelationsWrapper relationsWrapper)
	{
		if (relationsWrapper == null)
		{
			getValueList().setRelationName(null);
		}
		else
		{
			handleRelatedValuesButtonSelected();
			getValueList().setRelationName(EclipseDatabaseUtils.getRelationsString(relationsWrapper.relations));
		}
	}

	@Override
	public void setFocus()
	{
		nameField.forceFocus();
	}

	int lastReturnDataProvidersValue = 0;
	int lastShowDataProviders = 0;

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (isDirty())
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			IValidateName validator = servoyModel.getNameValidator();
			try
			{
				ValueList valuelist = getValueList();
				valuelist.updateName(validator, nameField.getText());
				updateTitle();
				if (valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES ||
					valuelist.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
				{
					lastReturnDataProvidersValue = valuelist.getReturnDataProviders();
					lastShowDataProviders = valuelist.getShowDataProviders();
					valuelist.setReturnDataProviders(0);
					valuelist.setShowDataProviders(0);
				}
			}
			catch (RepositoryException e)
			{
				MessageDialog.openError(getSite().getShell(), "Error while saving", e.getMessage());
				if (monitor != null) monitor.setCanceled(true);
				return;
			}

			String message = checkValidState();
			if (message != null)
			{
				MessageDialog.openWarning(getSite().getShell(), "Error while saving", message);
				if (monitor != null) monitor.setCanceled(true);
				return;
			}
			else
			{
				// if valuelist still has invalid data in it, try to correct it automatically and tell the user what was changed
				List<Problem> problems = ServoyValuelistBuilder.checkValuelist(getValueList(), servoyModel.getFlattenedSolution(),
					ApplicationServerRegistry.get().getServerManager(), true);
				StringBuilder paf = new StringBuilder("Some problems with the contents of this valuelist were noticed and corrected:\n");
				boolean autoFixes = false;
				for (Problem problem : problems)
				{
					if (problem.fix != null)
					{
						autoFixes = true;
						paf.append("\n- ");
						paf.append(problem.message);
						paf.append(" Auto changed: ");
						paf.append(problem.fix);
					}
				}
				if (autoFixes) MessageDialog.openWarning(getSite().getShell(), "Auto fixes when saving", paf.toString());
			}
		}
		super.doSave(monitor);
	}

	private String checkValidState()
	{
		if (nameField.getText() == null || nameField.getText().equals(""))
		{
			return "You must specify the name of the valuelist.";
		}

		if (tableValuesButton.getSelection())
		{
			if (!tableSelect.isValid())
			{
				return "The table name field is invalid";
			}

			String warningMessage = validateFlags();
			if (warningMessage != null) return warningMessage;
		}
		else if (relatedValuesButton.getSelection())
		{
			if (!relationSelect.isValid())
			{
				return "The relation name field is invalid";
			}
			String warningMessage = validateFlags();
			if (warningMessage != null) return warningMessage;

		}
		else if (globalMethodValuesButton.getSelection())
		{
			if (!globalMethodSelect.isValid())
			{
				return "The global method field is invalid";
			}
		}

		if (!sortingDefinitionSelect.isValid())
		{
			return "The sorting definition is invalid";
		}

		if (!fallbackValuelist.isValid())
		{
			return "The fallback value list is invalid";
		}

		return null;
	}

	private String validateFlags()
	{
		boolean box1 = false;
		boolean box2 = false;
		boolean box3 = false;
		int boxesUsed = 0;
		int showUsed = 0;
		int returnUsed = 0;
		if (dp_select1.getShowInFieldFlag() || dp_select1.getReturnInDataproviderFlag())
		{
			if (dp_select1.getDataProvider() != null)
			{
				box1 = true;//filled
				boxesUsed++;
				if (dp_select1.getShowInFieldFlag())
				{
					showUsed++;
				}
				if (dp_select1.getReturnInDataproviderFlag())
				{
					returnUsed++;
				}
			}
			else
			{
				box1 = false;
			}
		}
		else
		{
			box1 = true;//still oke
		}

		if (dp_select2.getShowInFieldFlag() || dp_select2.getReturnInDataproviderFlag())
		{
			if (dp_select2.getDataProvider() != null)
			{
				box2 = true;//filled
				boxesUsed++;
				if (dp_select2.getShowInFieldFlag())
				{
					showUsed++;
				}
				if (dp_select2.getReturnInDataproviderFlag())
				{
					returnUsed++;
				}
			}
			else
			{
				box2 = false;
			}
		}
		else
		{
			box2 = true;//still oke
		}

		if (dp_select3.getShowInFieldFlag() || dp_select3.getReturnInDataproviderFlag())
		{
			if (dp_select3.getDataProvider() != null)
			{
				box3 = true;//filled
				boxesUsed++;
				if (dp_select3.getShowInFieldFlag())
				{
					showUsed++;
				}
				if (dp_select3.getReturnInDataproviderFlag())
				{
					returnUsed++;
				}
			}
			else
			{
				box3 = false;
			}
		}
		else
		{
			box3 = true;//still oke
		}

		if (boxesUsed == 0)
		{
			return "You must use at least one definition box.";
		}
		if (showUsed == 0)
		{
			return "You must check at least one Show in field/list from a selected column.";
		}
		if (returnUsed == 0)
		{
			return "You must check at least one Return in dataprovider from a selected column.";
		}
		if (!box1)
		{
			return "There is an error in the first definition box";
		}
		if (!box2)
		{
			return "There is an error in the second definition box";
		}
		if (!box3)
		{
			return "There is an error in the third definition box";
		}
		return null;
	}

	private static class FallbackValuelistContentProvider extends FlatTreeContentProvider
	{
		private final FlattenedSolution flattenedSolution;
		private final ValueList vl;

		FallbackValuelistContentProvider(FlattenedSolution flattenedSolution, ValueList vl)
		{
			this.flattenedSolution = flattenedSolution;
			this.vl = vl;
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			List<Integer> vlIds = new ArrayList<Integer>();
			vlIds.add(Integer.valueOf(ValuelistLabelProvider.VALUELIST_NONE));

			Iterator<ValueList> it = flattenedSolution.getValueLists(true);
			while (it.hasNext())
			{
				Set<ValueList> processed = new HashSet<ValueList>(3);
				ValueList obj = it.next();
				if (isValid(obj, processed))
				{
					vlIds.add(Integer.valueOf(obj.getID()));
				}
			}

			return vlIds.toArray();
		}

		private boolean isValid(ValueList list, Set<ValueList> processed)
		{
			if (list == vl) return false;
			if (!processed.add(list)) return false;

			if (list.getValueListType() == IValueListConstants.DATABASE_VALUES && vl.getValueListType() == IValueListConstants.DATABASE_VALUES)
			{
				String listTable = getDataSource(list);
				String vlTable = getDataSource(vl);
				if (!listTable.equals(vlTable)) return false;
			}
			if (list.getFallbackValueListID() != 0)
			{
				ValueList valueList = flattenedSolution.getValueList(list.getFallbackValueListID());
				if (valueList != null)
				{
					return isValid(valueList, processed);
				}
			}
			return true;
		}

		/**
		 * @param list
		 */
		private String getDataSource(ValueList list)
		{
			if (list.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
			{
				return list.getDataSource();
			}
			else if (list.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES && list.getRelationName() != null)
			{
				Relation[] relations = flattenedSolution.getRelationSequence(list.getRelationName());
				if (relations != null)
				{
					return relations[relations.length - 1].getForeignDataSource();
				}
			}
			return "";
		}

	}

	protected DataBindingContext initDataBindings()
	{
		dp_select1.initValueListBindings(this, 1, 1);
		dp_select2.initValueListBindings(this, 2, 2);
		dp_select3.initValueListBindings(this, 3, 4);

		if (ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution() != null &&
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getValueList(getValueList().getName()) == null)
		{
			// is a new valuelist
			dp_select1.initDefaultValues();
		}

		return defineObservablesAndBindingContext();
	}

	private void handleDataProvidersProperties()
	{
		if (getValueList().getReturnDataProviders() == 0)
		{
			getValueList().setReturnDataProviders(lastReturnDataProvidersValue);
		}
		if (getValueList().getShowDataProviders() == 0)
		{
			getValueList().setShowDataProviders(lastShowDataProviders);
		}
	}

	@Override
	protected String getContextId()
	{
		return "com.servoy.eclipse.ui.create_valuelist";
	}
}
