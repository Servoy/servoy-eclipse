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
package com.servoy.eclipse.ui.editors.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.builder.ScriptingUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider.UnresolvedDataProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderNodeWrapper;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.AddMethodButtonsComposite;
import com.servoy.eclipse.ui.editors.table.ColumnDetailsComposite.NotSameValidator;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.DataProviderConverter;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.tweaks.IconPreferences;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.util.ScopeWithContext;
import com.servoy.eclipse.ui.views.TreeSelectObservableValue;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RelationList;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.SortedList;

public class ColumnAutoEnterComposite extends Composite implements SelectionListener
{
	private DataBindingContext bindingContext;
	private final Combo systemValueCombo;
	private final Text customValueText;
	private final Combo sequenceCombo;
	private final Button systemValueButton;
	private final Button customValueButton;
	private final Button databaseDefaultButton;
	private final Button lookupValueButton;
	private final Button sequenceButton;
	private final TreeSelectViewer lookupValueSelect;
	private final Control lookupValueControl;
	private final CTabFolder tabFolder;
	private final Text databaseDefaultValue;
	private Column column;
	private ColumnInfoBean columnInfoBean;
	private final ChangeSupportObservable observable;

	private final ColumnAutoEnterServoySeqComposite columnAutoEnterServoySeqComposite;
	private final ColumnAutoEnterDBSeqComposite columnAutoEnterDBSeqComposite;
	private final FlattenedSolution flattenedSolution;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public ColumnAutoEnterComposite(Composite parent, final FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style);
		setBackgroundMode(SWT.INHERIT_FORCE);
		this.flattenedSolution = flattenedSolution;
		column = null;
		systemValueButton = new Button(this, SWT.RADIO);
		systemValueButton.setText("System Value");
		systemValueButton.addSelectionListener(this);
		systemValueCombo = new Combo(this, SWT.READ_ONLY | SWT.BORDER);
		systemValueCombo.setVisibleItemCount(UIUtils.COMBO_VISIBLE_ITEM_COUNT);
		systemValueCombo.addSelectionListener(this);

		customValueButton = new Button(this, SWT.RADIO);
		customValueButton.setText("Custom Value");
		customValueButton.addSelectionListener(this);
		customValueText = new Text(this, SWT.BORDER);

		databaseDefaultButton = new Button(this, SWT.RADIO);
		databaseDefaultButton.setText("Database Default");
		databaseDefaultButton.addSelectionListener(this);


		databaseDefaultValue = new Text(this, SWT.BORDER);
		databaseDefaultValue.setEnabled(false);

		lookupValueButton = new Button(this, SWT.RADIO);
		lookupValueButton.setText("Lookup Value");
		lookupValueButton.addSelectionListener(this);

		sequenceButton = new Button(this, SWT.RADIO);
		sequenceButton.setText("Sequence");
		sequenceButton.addSelectionListener(this);

		sequenceCombo = new Combo(this, SWT.READ_ONLY | SWT.BORDER);
		sequenceCombo.setVisibleItemCount(UIUtils.COMBO_VISIBLE_ITEM_COUNT);
		sequenceCombo.addSelectionListener(this);

		tabFolder = new CTabFolder(this, SWT.NONE);
		final CTabItem servoySeqTabItem = new CTabItem(tabFolder, SWT.NONE, 0);
		servoySeqTabItem.setText("Servoy sequence");

		columnAutoEnterServoySeqComposite = new ColumnAutoEnterServoySeqComposite(tabFolder, SWT.NONE);
		servoySeqTabItem.setControl(columnAutoEnterServoySeqComposite);

		final CTabItem databaseSequenceTabItem = new CTabItem(tabFolder, SWT.NONE, 1);
		databaseSequenceTabItem.setText("Database sequence");

		columnAutoEnterDBSeqComposite = new ColumnAutoEnterDBSeqComposite(tabFolder, SWT.NONE);
		databaseSequenceTabItem.setControl(columnAutoEnterDBSeqComposite);

		if (IconPreferences.getInstance().getUseDarkThemeIcons())
		{
			Color backgroundColor = ColumnComposite.getServoyGrayBackground();
			columnAutoEnterServoySeqComposite.setBackground(backgroundColor);
			columnAutoEnterDBSeqComposite.setBackground(backgroundColor);
		}

		lookupValueSelect = new TreeSelectViewer(this, SWT.NONE)
		{
			@Override
			protected TreeSelectDialog createDialog(Control control)
			{
				final DataProviderDialog dialog = new DataProviderDialog(control.getShell(), (ILabelProvider)getLabelProvider(), null,
					ColumnAutoEnterComposite.this.flattenedSolution, column.getTable(), new DataProviderTreeViewer.DataProviderOptions(false, true, false,
						false, false, true, false, false, INCLUDE_RELATIONS.NESTED, true, true, null),
					getSelection(), SWT.NONE, title);
				dialog.setContentProvider(new DataProviderContentProvider(null, ColumnAutoEnterComposite.this.flattenedSolution, column.getTable())
				{
					@Override
					public Object[] getChildren(Object parentElement)
					{
						if (parentElement instanceof DataProviderNodeWrapper &&
							((DataProviderNodeWrapper)parentElement).node == DataProviderTreeViewer.SCOPE_METHODS)
						{
							Collection<Pair<String, IRootObject>> scopes = ColumnAutoEnterComposite.this.flattenedSolution.getScopes();
							SortedList<ScopeWithContext> scopesList = new SortedList<ScopeWithContext>(ScopeWithContext.SCOPE_COMPARATOR);
							for (Pair<String, IRootObject> sc : scopes)
							{
								scopesList.add(new ScopeWithContext(sc.getLeft(), sc.getRight()));
							}
							List<Object> children = new ArrayList<Object>();
							for (ScopeWithContext scope : scopesList)
							{
								children.add(new DataProviderNodeWrapper(scope.getName(), scope, DataProviderTreeViewer.METHODS));
							}
							return children.toArray();
						}
						if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).scope != null &&
							((DataProviderNodeWrapper)parentElement).type == DataProviderTreeViewer.METHODS)
						{
							Iterator<ScriptMethod> scopeMethods = ColumnAutoEnterComposite.this.flattenedSolution.getScriptMethods(
								((DataProviderNodeWrapper)parentElement).scope.getName(), true);
							List<Object> children = new ArrayList<Object>();
							if (scopeMethods.hasNext())
							{
								while (scopeMethods.hasNext())
								{
									ScriptMethod sm = scopeMethods.next();
									if (!sm.isPrivate())
									{
										children.add(sm);
									}
								}
								return children.toArray();

							}
						}
						if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).scope != null &&
							((DataProviderNodeWrapper)parentElement).type == DataProviderTreeViewer.VARIABLES && flattenedSolution != null)
						{
							Iterator<ScriptVariable> scopeVars = flattenedSolution.getScriptVariables(((DataProviderNodeWrapper)parentElement).scope.getName(),
								true);
							List<Object> children = new ArrayList<Object>();
							while (scopeVars.hasNext())
							{
								ScriptVariable sv = scopeVars.next();
								if (!sv.isPrivate())
								{
									if (sv.isEnum())
									{
										children.addAll(ScriptingUtils.getEnumDataProviders(sv));
									}
									else
									{
										children.add(sv);
									}
								}
							}
							return children.toArray();
						}
						return super.getChildren(parentElement);
					}

					@Override
					public Object getParent(Object value)
					{
						if (value instanceof ScriptMethod)
						{
							ScriptMethod scriptMethod = (ScriptMethod)value;
							return new DataProviderNodeWrapper(scriptMethod.getScopeName(),
								new ScopeWithContext(scriptMethod.getScopeName(), (Solution)scriptMethod.getParent()), DataProviderTreeViewer.METHODS);
						}
						if (value instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)value).scope != null &&
							((DataProviderNodeWrapper)value).type == DataProviderTreeViewer.METHODS)
						{
							return new DataProviderNodeWrapper(DataProviderTreeViewer.SCOPE_METHODS, (ScopeWithContext)null);
						}
						if (value instanceof EnumDataProvider)
						{
							Pair<String, String> scopePair = ScopesUtils.getVariableScope(((EnumDataProvider)value).getDataProviderID());
							Collection<Pair<String, IRootObject>> scopes = flattenedSolution.getScopes();
							ScopeWithContext scope = null;
							for (Pair<String, IRootObject> sc : scopes)
							{
								if (sc.getLeft().equals(scopePair.getLeft()))
								{
									scope = new ScopeWithContext(sc.getLeft(), sc.getRight());
									break;
								}
							}
							return new DataProviderNodeWrapper(scopePair.getLeft(), scope, DataProviderTreeViewer.VARIABLES);
						}
						return super.getParent(value);
					}

					@Override
					public Object[] getElements(Object inputElement)
					{
						Object[] input = super.getElements(inputElement);
						List<Object> combinedInputs = new ArrayList<Object>(Arrays.asList(input));
						combinedInputs.add(new DataProviderNodeWrapper(DataProviderTreeViewer.SCOPE_METHODS, (RelationList)null));
						return combinedInputs.toArray();
					}
				});
				dialog.setOptionsAreaFactory(new IControlFactory()
				{
					public Control createControl(Composite composite)
					{
						final AddMethodButtonsComposite buttons = new AddMethodButtonsComposite(composite, SWT.NONE)
						{
							@Override
							protected Object getSelectionObject(ScriptMethod method)
							{
								return method;
							}
						};
						buttons.setContext(PersistContext.create(ColumnAutoEnterComposite.this.flattenedSolution.getSolution()), "getLookupValue");
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
				return dialog;
			}
		};
		lookupValueSelect.setTitleText("Specify DataProvider/Global Method");
		lookupValueSelect.setName("lookupDialog");
		lookupValueSelect.setLabelProvider(new SolutionContextDelegateLabelProvider(new DataProviderLabelProvider(true)
		{
			@Override
			public String getText(Object dataProvider)
			{
				if (dataProvider instanceof ScriptMethod)
				{
					return ((ScriptMethod)dataProvider).getName() + "()";
				}
				return super.getText(dataProvider);
			}
		}, flattenedSolution.getSolution()));
		lookupValueSelect.setTextLabelProvider(new LabelProvider()
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
				if (element instanceof ScriptMethod)
				{
					return ((ScriptMethod)element).getPrefixedName() + "()";
				}
				return "";
			}
		});
		lookupValueControl = lookupValueSelect.getControl();

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tabFolder, GroupLayout.PREFERRED_SIZE, 509, Short.MAX_VALUE).add(
				groupLayout.createSequentialGroup().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(systemValueButton).add(customValueButton).add(databaseDefaultButton).add(
						lookupValueButton).add(sequenceButton))
					.addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(customValueText, GroupLayout.PREFERRED_SIZE, 420, Short.MAX_VALUE).add(
							systemValueCombo, GroupLayout.PREFERRED_SIZE, 420, Short.MAX_VALUE).add(databaseDefaultValue, GroupLayout.PREFERRED_SIZE, 420,
								Short.MAX_VALUE)
							.add(sequenceCombo, GroupLayout.PREFERRED_SIZE, 420, Short.MAX_VALUE).add(lookupValueControl,
								GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE))))
			.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(systemValueButton).add(
				systemValueCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(customValueButton).add(customValueText, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(databaseDefaultButton).add(databaseDefaultValue))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(lookupValueButton).add(lookupValueControl))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(sequenceButton).add(sequenceCombo,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(tabFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.addContainerGap()));
		setLayout(groupLayout);

		observable = new ChangeSupportObservable(new SimpleChangeSupport());
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public void initDataBindings(final Column c)
	{
		bindingContext = BindingHelper.dispose(bindingContext);

		this.column = c;
		columnAutoEnterServoySeqComposite.initDataBindings(c);
		columnAutoEnterDBSeqComposite.initDataBindings(c);
		ColumnInfo columnInfo = c.getColumnInfo();
		List<String> systemTypes = new ArrayList<String>();

		// use dataprovider type as defined by column converter
		ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, c, Activator.getDefault().getDesignClient());
		int type = componentFormat.dpType;
		systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.NO_SYSTEM_VALUE));//no system value
		systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.DATABASE_MANAGED));
		if (type == IColumnTypes.DATETIME)
		{
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_CREATION_DATETIME));
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_CREATION_SERVER_DATETIME));
		}
		if (type == IColumnTypes.TEXT && !column.isUUID())
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_CREATION_USERNAME));
		if (type == IColumnTypes.TEXT || type == IColumnTypes.INTEGER || type == IColumnTypes.NUMBER || column.isUUID())
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_CREATION_USERUID));
		if (type == IColumnTypes.DATETIME)
		{
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_MODIFICATION_DATETIME));
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_MODIFICATION_SERVER_DATETIME));
		}
		if (type == IColumnTypes.TEXT && !column.isUUID())
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERNAME));
		if (type == IColumnTypes.TEXT || type == IColumnTypes.INTEGER || type == IColumnTypes.NUMBER || column.isUUID())
			systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERUID));

		String[] system = new String[systemTypes.size()];
		for (int i = 0; i < systemTypes.size(); i++)
		{
			system[i] = systemTypes.get(i).toString();
		}

		systemValueCombo.removeAll();
		systemValueCombo.setItems(system);
		sequenceCombo.setItems(ColumnComposite.getSeqDisplayTypeStrings(c.getTable()));

		systemValueButton.setSelection(false);
		sequenceButton.setSelection(false);
		customValueButton.setSelection(false);
		databaseDefaultButton.setSelection(false);
		lookupValueButton.setSelection(false);
		switch (columnInfo.getAutoEnterType())
		{
			case ColumnInfo.NO_AUTO_ENTER :
				if (columnInfo.getDatabaseDefaultValue() != null)
				{
					databaseDefaultButton.setSelection(true);
					break;
				}
			case ColumnInfo.SYSTEM_VALUE_AUTO_ENTER :
				systemValueButton.setSelection(true);
				break;
			case ColumnInfo.SEQUENCE_AUTO_ENTER :
				sequenceButton.setSelection(true);
				break;
			case ColumnInfo.CUSTOM_VALUE_AUTO_ENTER :
				customValueButton.setSelection(true);
				break;
			case ColumnInfo.CALCULATION_VALUE_AUTO_ENTER :
				// not implemented
				break;
			case ColumnInfo.LOOKUP_VALUE_AUTO_ENTER :
				lookupValueButton.setSelection(true);
				break;
			default :
		}
		databaseDefaultValue.setText(columnInfo.getDatabaseDefaultValue() == null ? "" : columnInfo.getDatabaseDefaultValue());

		sequenceButton.setEnabled(type != IColumnTypes.DATETIME);
		sequenceCombo.setEnabled(type != IColumnTypes.DATETIME);
		customValueButton.setEnabled(type != IColumnTypes.DATETIME);
		customValueText.setEnabled(type != IColumnTypes.DATETIME);

		enableControls();

		refreshComboValue();

		if (columnInfoBean == null)
		{
			columnInfoBean = new ColumnInfoBean(c.getColumnInfo());
		}
		else
		{
			columnInfoBean.setColumnInfo(c.getColumnInfo());
		}
		IObservableValue getCICustomValueObserveValue = PojoProperties.value("defaultValue").observe(columnInfoBean);
		IObservableValue customValueTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(customValueText);
		IObservableValue getCILookUpValueObserveValue = PojoProperties.value("lookupValue").observe(columnInfoBean);
		IObservableValue lookUpValueSelectObserveWidget = new TreeSelectObservableValue(lookupValueSelect, IDataProvider.class);

		bindingContext = new DataBindingContext();

		bindingContext.bindValue(customValueTextObserveWidget, getCICustomValueObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCICustomValueObserveValue)), null);
		bindingContext.bindValue(lookUpValueSelectObserveWidget, getCILookUpValueObserveValue,
			new UpdateValueStrategy().setConverter(DataProvider2LookupValueConverter.INSTANCE).setAfterGetValidator(new LookupValueValidator(column)),
			new UpdateValueStrategy().setConverter(new LookupValue2DataProviderConverter(flattenedSolution, column.getTable())));

		BindingHelper.addGlobalChangeListener(bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				getShell().getDisplay().asyncExec(new Runnable()
				{
					public void run()
					{
						c.flagColumnInfoChanged();
					}
				});
			}
		});

		refreshComboValue();
	}

	public void widgetDefaultSelected(SelectionEvent e)
	{
		// nothing to do here
	}

	public void widgetSelected(SelectionEvent e)
	{
		if (e.getSource() instanceof Button && ((Button)e.getSource()).getSelection())
		{
			setAutoEnterValue();
			bindingContext.updateTargets();
			enableControls();
		}
		else if (e.getSource().equals(sequenceCombo))
		{
			enableSequenceTab();
		}
		if (e.getSource().equals(sequenceCombo) || e.getSource().equals(systemValueCombo))
		{
			setComboValue();
		}
		if (column != null) column.flagColumnInfoChanged();

	}

	private String oldAutoEnterSubTypeString = null;

	private void setAutoEnterValue()
	{
		// it seems obvious but is this the behavior?
		if (column != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();
			if (customValueButton.getSelection() || lookupValueButton.getSelection() ||
				databaseDefaultButton.getSelection() && columnInfo.getAutoEnterSubType() > 0)
			{
				if (columnInfo.getAutoEnterType() == ColumnInfo.SEQUENCE_AUTO_ENTER)
				{
					oldAutoEnterSubTypeString = ColumnInfo.getSeqDisplayTypeString(columnInfo.getAutoEnterSubType());
				}
				else
				{
					oldAutoEnterSubTypeString = columnInfo.getAutoEnterSubTypeString(columnInfo.getAutoEnterType(), columnInfo.getAutoEnterSubType());
				}
			}
			columnInfo.setAutoEnterSubType(ColumnInfo.NO_SEQUENCE_SELECTED);
			if (systemValueButton.getSelection())
			{
				columnInfoBean.setAutoEnterType(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER);
				if (oldAutoEnterSubTypeString != null) systemValueCombo.setText(oldAutoEnterSubTypeString);
				if (systemValueCombo.getSelectionIndex() > 0)
				{
					String type = systemValueCombo.getText();
					int[] subTypes = ColumnInfo.allDefinedSystemValues;
					for (int element : subTypes)
					{
						if (columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, element).equals(type))
						{
							columnInfo.setAutoEnterSubType(element);
							break;
						}
					}
				}
				else
				{
					columnInfo.setAutoEnterSubType(ColumnInfo.NO_SYSTEM_VALUE);
				}
			}
			else if (customValueButton.getSelection())
			{
				columnInfoBean.setAutoEnterType(ColumnInfo.CUSTOM_VALUE_AUTO_ENTER);
			}
			else if (lookupValueButton.getSelection())
			{
				columnInfoBean.setAutoEnterType(ColumnInfo.LOOKUP_VALUE_AUTO_ENTER);
			}
			else if (sequenceButton.getSelection())
			{
				columnInfoBean.setAutoEnterType(ColumnInfo.SEQUENCE_AUTO_ENTER);
				if (oldAutoEnterSubTypeString != null) sequenceCombo.setText(oldAutoEnterSubTypeString);
				if (sequenceCombo.getSelectionIndex() > 0)
				{
					String type = sequenceCombo.getText();
					int[] subTypes = ColumnInfo.allDefinedSeqTypes;
					for (int element : subTypes)
					{
						if (ColumnInfo.getSeqDisplayTypeString(element).equals(type))
						{
							column.setSequenceType(element);
							columnInfo.setFlag(IBaseColumn.UUID_COLUMN, element == ColumnInfo.UUID_GENERATOR);
							observable.fireChangeEvent();
							break;
						}
					}
				}
				else
				{
					column.setSequenceType(ColumnInfo.NO_SEQUENCE_SELECTED);
				}

			}
			else if (databaseDefaultButton.getSelection())
			{
				columnInfoBean.setAutoEnterType(ColumnInfo.NO_AUTO_ENTER);
			}
			refreshComboValue();
		}
	}

	private void enableControls()
	{
		// first disable all :)
		customValueText.setEnabled(false);
		systemValueCombo.setEnabled(false);
		sequenceCombo.setEnabled(false);
		lookupValueSelect.setEnabled(false);
		tabFolder.setVisible(false);

		if (systemValueButton.getSelection())
		{
			systemValueCombo.setEnabled(true);
		}
		else if (sequenceButton.getSelection())
		{
			sequenceCombo.setEnabled(true);
			enableSequenceTab();
		}
		else if (customValueButton.getSelection())
		{
			customValueText.setEnabled(true);
		}
		else if (lookupValueButton.getSelection())
		{
			lookupValueSelect.setEnabled(true);
		}
	}

	private void enableSequenceTab()
	{
		if (sequenceButton.getSelection())
		{
			int[] types = ColumnInfo.allDefinedSeqTypes;
			for (int element : types)
			{
				if (ColumnInfo.getSeqDisplayTypeString(element).equals(sequenceCombo.getText()) &&
					(element == ColumnInfo.SERVOY_SEQUENCE || element == ColumnInfo.DATABASE_SEQUENCE))
				{
					tabFolder.setVisible(true);
					columnAutoEnterServoySeqComposite.initDataValues();
					if (element == ColumnInfo.DATABASE_SEQUENCE) tabFolder.setSelection(1);
					else tabFolder.setSelection(0);
					return;
				}
			}
		}
		tabFolder.setVisible(false);

	}

	private void refreshComboValue()
	{
		if (systemValueButton.getSelection())
		{
			if (column != null)
			{
				ColumnInfo columnInfo = column.getColumnInfo();
				String type = columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, columnInfo.getAutoEnterSubType());
				systemValueCombo.setText(type);
			}
		}
		else if (sequenceButton.getSelection())
		{
			if (column != null)
			{
				ColumnInfo columnInfo = column.getColumnInfo();
				String type = ColumnInfo.getSeqDisplayTypeString(columnInfo.getAutoEnterSubType());
				sequenceCombo.setText(type);
				enableSequenceTab();
			}
		}
		else
		{
			systemValueCombo.select(0);
			sequenceCombo.select(0);
		}
	}

	private void setComboValue()
	{
		if (systemValueButton.getSelection())
		{
			if (column != null)
			{
				ColumnInfo columnInfo = column.getColumnInfo();
				String type = systemValueCombo.getText();
				int[] subTypes = ColumnInfo.allDefinedSystemValues;
				for (int element : subTypes)
				{
					if (columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, element).equals(type))
					{
						columnInfo.setAutoEnterType(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER);
						columnInfo.setAutoEnterSubType(element);
						break;
					}
				}
			}
		}
		else if (sequenceButton.getSelection())
		{
			if (column != null)
			{
				ColumnInfo columnInfo = column.getColumnInfo();
				String type = sequenceCombo.getText();
				int[] subTypes = ColumnInfo.allDefinedSeqTypes;
				for (int element : subTypes)
				{
					if (ColumnInfo.getSeqDisplayTypeString(element).equals(type))
					{
						columnInfo.setAutoEnterSubType(element);
						columnInfo.setFlag(IBaseColumn.UUID_COLUMN, element == ColumnInfo.UUID_GENERATOR);
						observable.fireChangeEvent();
						if (element == ColumnInfo.SERVOY_SEQUENCE)
						{
							columnInfo.setPreSequenceChars("");
							columnInfo.setPostSequenceChars("");
						}
						break;
					}
				}
			}
		}
	}

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	// Converters to convert between IDataProvider (used in TreeSelectViewer) to String (lookup value in ColumnInfo)
	public static class DataProvider2LookupValueConverter extends Converter
	{
		public final static DataProvider2LookupValueConverter INSTANCE = new DataProvider2LookupValueConverter();

		private DataProvider2LookupValueConverter()
		{
			super(IDataProvider.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject == null || fromObject == DataProviderContentProvider.NONE) return null;
			if (fromObject instanceof ScriptMethod) return ((ScriptMethod)fromObject).getPrefixedName();
			return ((IDataProvider)fromObject).getDataProviderID();
		}
	}

	public static class LookupValue2DataProviderConverter extends Converter
	{
		private final FlattenedSolution flattenedSolution;
		private final ITable table;

		public LookupValue2DataProviderConverter(FlattenedSolution flattenedSolution, ITable table)
		{
			super(String.class, IDataProvider.class);
			this.flattenedSolution = flattenedSolution;
			this.table = table;
		}

		public Object convert(Object fromObject)
		{
			if (fromObject == null) return null;
			try
			{
				Object object = DataProviderConverter.getDataProvider(flattenedSolution, null, table, (String)fromObject);
				if (object == null) object = flattenedSolution.getScriptMethod(null, (String)fromObject);
				if (object == null) return new UnresolvedDataProvider((String)fromObject);
				return object;
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}
	}

	public static class LookupValueValidator implements IValidator
	{
		private final Column column;

		public LookupValueValidator(Column column)
		{
			this.column = column;
		}

		public IStatus validate(Object value)
		{
			if (value == null || value instanceof UnresolvedDataProvider || value instanceof ScriptMethod)
			{
				return Status.OK_STATUS;
			}

			// use dataprovider type as defined by column converter
			ComponentFormat otherComponentFormat = ComponentFormat.getComponentFormat(null, (IDataProvider)value, Activator.getDefault().getDesignClient());
			ComponentFormat columnComponentFormat = ComponentFormat.getComponentFormat(null, column, Activator.getDefault().getDesignClient());

			int otherType = otherComponentFormat.dpType;
			int valueType = columnComponentFormat.dpType;
			boolean typeMismatch = false;
			if (valueType == otherType)
			{
				typeMismatch = false;//accepted
			}
			else if (valueType == IColumnTypes.INTEGER || valueType == IColumnTypes.NUMBER)
			{
				if (otherType != IColumnTypes.INTEGER && otherType != IColumnTypes.NUMBER)
				{
					typeMismatch = true;
				}
			}
			else if (valueType == IColumnTypes.TEXT)
			{
				typeMismatch = false;//accept anything
			}
			else if (valueType != otherType)
			{
				typeMismatch = true;
			}
			if (typeMismatch)
			{
				String msg = "Type mismatch";
				MessageDialog.openError(UIUtils.getActiveShell(), "Wrong type", msg);
				return ValidationStatus.error(msg);
			}

			return Status.OK_STATUS;
		}

	}

	public void addDisposeListener(IDisposeListener listener)
	{

	}

	public void removeDisposeListener(IDisposeListener listener)
	{

	}
}
