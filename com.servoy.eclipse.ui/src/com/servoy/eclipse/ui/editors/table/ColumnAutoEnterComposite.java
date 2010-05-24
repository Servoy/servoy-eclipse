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
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.dialogs.DataProviderDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider.UnresolvedDataProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.editors.table.ColumnDetailsComposite.NotSameValidator;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.DataProviderConverter;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.views.TreeSelectObservableValue;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;

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
	private final TabFolder tabFolder;
	private final Label databaseDefaultValue;
	private Column column;
	private final IObservable observable;

	private final ColumnAutoEnterServoySeqComposite columnAutoEnterServoySeqComposite;
	private final ColumnAutoEnterDBSeqComposite columnAutoEnterDBSeqComposite;
	private final FlattenedSolution flattenedSolution;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public ColumnAutoEnterComposite(Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style);
		this.flattenedSolution = flattenedSolution;
		column = null;
		systemValueButton = new Button(this, SWT.RADIO);
		systemValueButton.setText("System Value");
		systemValueButton.addSelectionListener(this);
		systemValueCombo = new Combo(this, SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(systemValueCombo);
		systemValueCombo.addSelectionListener(this);

		customValueButton = new Button(this, SWT.RADIO);
		customValueButton.setText("Custom Value");
		customValueButton.addSelectionListener(this);
		customValueText = new Text(this, SWT.BORDER);

		databaseDefaultButton = new Button(this, SWT.RADIO);
		databaseDefaultButton.setText("Database Default");
		databaseDefaultButton.addSelectionListener(this);


		databaseDefaultValue = new Label(this, SWT.BORDER);

		lookupValueButton = new Button(this, SWT.RADIO);
		lookupValueButton.setText("Lookup Value");
		lookupValueButton.addSelectionListener(this);

		sequenceButton = new Button(this, SWT.RADIO);
		sequenceButton.setText("Sequence");
		sequenceButton.addSelectionListener(this);

		sequenceCombo = new Combo(this, SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(sequenceCombo);
		sequenceCombo.addSelectionListener(this);

		tabFolder = new TabFolder(this, SWT.NONE);

		final TabItem servoySeqTabItem = new TabItem(tabFolder, SWT.NONE);
		servoySeqTabItem.setText("Servoy sequence");

		columnAutoEnterServoySeqComposite = new ColumnAutoEnterServoySeqComposite(tabFolder, SWT.NONE);
		servoySeqTabItem.setControl(columnAutoEnterServoySeqComposite);

		final TabItem databaseSequenceTabItem = new TabItem(tabFolder, SWT.NONE);
		databaseSequenceTabItem.setText("Database sequence");

		columnAutoEnterDBSeqComposite = new ColumnAutoEnterDBSeqComposite(tabFolder, SWT.NONE);
		databaseSequenceTabItem.setControl(columnAutoEnterDBSeqComposite);

		lookupValueSelect = new TreeSelectViewer(this, SWT.NONE)
		{
			@Override
			protected TreeSelectDialog createDialog(Control control)
			{
				return new DataProviderDialog(control.getShell(), (ILabelProvider)getLabelProvider(), null, ColumnAutoEnterComposite.this.flattenedSolution,
					column.getTable(), new DataProviderTreeViewer.DataProviderOptions(false, true, false, false, false, true, false, false,
						INCLUDE_RELATIONS.NESTED, true, null), getSelection(), SWT.NONE, title);
			}
		};
		lookupValueSelect.setTitleText("Specify Field");
		lookupValueSelect.setName("lookupDialog");
		lookupValueSelect.setLabelProvider(new SolutionContextDelegateLabelProvider(DataProviderLabelProvider.INSTANCE_HIDEPREFIX,
			flattenedSolution.getSolution()));
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
				return "";
			}
		});
		lookupValueControl = lookupValueSelect.getControl();

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tabFolder, GroupLayout.PREFERRED_SIZE, 509, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(systemValueButton).add(customValueButton).add(databaseDefaultButton).add(
							lookupValueButton).add(sequenceButton)).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(customValueText, GroupLayout.PREFERRED_SIZE, 420, Short.MAX_VALUE).add(
							systemValueCombo, GroupLayout.PREFERRED_SIZE, 420, Short.MAX_VALUE).add(databaseDefaultValue, GroupLayout.PREFERRED_SIZE, 420,
							Short.MAX_VALUE).add(sequenceCombo, GroupLayout.PREFERRED_SIZE, 420, Short.MAX_VALUE).add(lookupValueControl,
							GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(systemValueButton).add(systemValueCombo, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(customValueButton).add(customValueText, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(databaseDefaultButton).add(databaseDefaultValue)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(lookupValueButton).add(lookupValueControl)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(sequenceButton).add(sequenceCombo, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(tabFolder, GroupLayout.PREFERRED_SIZE,
				GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));
		setLayout(groupLayout);

		changeSupport = new ChangeSupport(Realm.getDefault())
		{
			@Override
			protected void lastListenerRemoved()
			{
			}

			@Override
			protected void firstListenerAdded()
			{
			}
		};

		observable = new AbstractObservable(Realm.getDefault())
		{
			@Override
			public void addChangeListener(IChangeListener listener)
			{
				changeSupport.addChangeListener(listener);
			}

			@Override
			public void removeChangeListener(IChangeListener listener)
			{
				changeSupport.removeChangeListener(listener);
			}

			public boolean isStale()
			{
				return false;
			}
		};
		//
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
		int type = c.getType();
		systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.NO_SYSTEM_VALUE));//no system value
		systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.DATABASE_MANAGED));
		if (type == IColumnTypes.DATETIME) systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER,
			ColumnInfo.SYSTEM_VALUE_CREATION_DATETIME));
		if (type == IColumnTypes.DATETIME) systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER,
			ColumnInfo.SYSTEM_VALUE_CREATION_SERVER_DATETIME));
		if (type == IColumnTypes.TEXT) systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER,
			ColumnInfo.SYSTEM_VALUE_CREATION_USERNAME));
		if (type == IColumnTypes.TEXT || type == IColumnTypes.INTEGER || type == IColumnTypes.NUMBER) systemTypes.add(columnInfo.getAutoEnterSubTypeString(
			ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_CREATION_USERUID));
		if (type == IColumnTypes.DATETIME) systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER,
			ColumnInfo.SYSTEM_VALUE_MODIFICATION_DATETIME));
		if (type == IColumnTypes.DATETIME) systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER,
			ColumnInfo.SYSTEM_VALUE_MODIFICATION_SERVER_DATETIME));
		if (type == IColumnTypes.TEXT) systemTypes.add(columnInfo.getAutoEnterSubTypeString(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER,
			ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERNAME));
		if (type == IColumnTypes.TEXT || type == IColumnTypes.INTEGER || type == IColumnTypes.NUMBER) systemTypes.add(columnInfo.getAutoEnterSubTypeString(
			ColumnInfo.SYSTEM_VALUE_AUTO_ENTER, ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERUID));
		Object[] oSystem = systemTypes.toArray();
		String[] system = new String[systemTypes.size()];
		for (int i = 0; i < oSystem.length; i++)
		{
			system[i] = oSystem[i].toString();
		}
		systemValueCombo.removeAll();
		systemValueCombo.setItems(system);


		int[] types = ColumnInfo.allDefinedSeqTypes;
		try
		{
			List<String> seqType = new ArrayList<String>();
			IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(c.getTable().getServerName());
			for (int element : types)
			{
				if (element == ColumnInfo.SERVOY_SEQUENCE || element == ColumnInfo.NO_SEQUENCE_SELECTED || server.supportsSequenceType(element, null/*
																																					 * TODO: add
																																					 * current
																																					 * selected
																																					 * column
																																					 */))
				{
					seqType.add(ColumnInfo.getSeqDisplayTypeString(element));
				}
			}
			String[] comboSeqTypes = new String[seqType.size()];
			Object[] oType = seqType.toArray();
			for (int i = 0; i < oType.length; i++)
			{
				comboSeqTypes[i] = oType[i].toString();
			}
			sequenceCombo.setItems(comboSeqTypes);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
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

		int columnType = Column.mapToDefaultType(c.getType());
		sequenceButton.setEnabled(columnType != IColumnTypes.DATETIME);
		sequenceCombo.setEnabled(columnType != IColumnTypes.DATETIME);
		customValueButton.setEnabled(columnType != IColumnTypes.DATETIME);
		customValueText.setEnabled(columnType != IColumnTypes.DATETIME);

		enableControls();

		refreshComboValue();

		ColumnInfoBean columnInfoBean = new ColumnInfoBean(c.getColumnInfo());
		IObservableValue getCICustomValueObserveValue = PojoObservables.observeValue(columnInfoBean, "defaultValue");
		IObservableValue customValueTextObserveWidget = SWTObservables.observeText(customValueText, SWT.Modify);
		IObservableValue getCILookUpValueObserveValue = PojoObservables.observeValue(columnInfoBean, "lookupValue");
		IObservableValue lookUpValueSelectObserveWidget = new TreeSelectObservableValue(lookupValueSelect, IDataProvider.class);


		bindingContext = new DataBindingContext();

		bindingContext.bindValue(customValueTextObserveWidget, getCICustomValueObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCICustomValueObserveValue)), null);
		bindingContext.bindValue(lookUpValueSelectObserveWidget, getCILookUpValueObserveValue, new UpdateValueStrategy().setConverter(
			DataProvider2LookupValueConverter.INSTANCE).setAfterGetValidator(new LookupValueValidator(column)),
			new UpdateValueStrategy().setConverter(new LookupValue2DataProviderConverter(flattenedSolution, column.getTable())));

		BindingHelper.addGlobalChangeListener(bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				c.flagColumnInfoChanged();
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

	private void setAutoEnterValue()
	{
		// it seems obvious but is this the behavior?
		if (column != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();
			if (systemValueButton.getSelection())
			{
				columnInfo.setAutoEnterType(ColumnInfo.SYSTEM_VALUE_AUTO_ENTER);
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
				refreshComboValue();
			}
			else if (customValueButton.getSelection())
			{
				columnInfo.setAutoEnterType(ColumnInfo.CUSTOM_VALUE_AUTO_ENTER);
			}
			else if (lookupValueButton.getSelection())
			{
				columnInfo.setAutoEnterType(ColumnInfo.LOOKUP_VALUE_AUTO_ENTER);
			}
			else if (sequenceButton.getSelection())
			{
				if (sequenceCombo.getSelectionIndex() > 0)
				{
					String type = sequenceCombo.getText();
					int[] subTypes = ColumnInfo.allDefinedSeqTypes;
					for (int element : subTypes)
					{
						if (ColumnInfo.getSeqDisplayTypeString(element).equals(type))
						{
							column.setSequenceType(element);
							columnInfo.setFlag(Column.UUID_COLUMN, element == ColumnInfo.UUID_GENERATOR);
							changeSupport.fireEvent(new ChangeEvent(observable));
							break;
						}
					}
				}
				else
				{
					column.setSequenceType(ColumnInfo.NO_SEQUENCE_SELECTED);
				}

				refreshComboValue();
			}
			else if (databaseDefaultButton.getSelection())
			{
				columnInfo.setAutoEnterType(ColumnInfo.NO_AUTO_ENTER);
			}
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
						columnInfo.setFlag(Column.UUID_COLUMN, element == ColumnInfo.UUID_GENERATOR);
						changeSupport.fireEvent(new ChangeEvent(observable));
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

	private final ChangeSupport changeSupport;

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
			return ((IDataProvider)fromObject).getDataProviderID();
		}
	}

	public static class LookupValue2DataProviderConverter extends Converter
	{
		private final FlattenedSolution flattenedSolution;
		private final Table table;

		public LookupValue2DataProviderConverter(FlattenedSolution flattenedSolution, Table table)
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
			if (value == null || value instanceof UnresolvedDataProvider)
			{
				return Status.OK_STATUS;
			}
			IDataProvider dataProvider = (IDataProvider)value;
			int otherType = Column.mapToDefaultType(dataProvider.getDataProviderType());
			int valueType = Column.mapToDefaultType(column.getType());
			boolean typeMismatch = false;
			if (valueType == otherType)
			{
				typeMismatch = false;//accepted
			}
			else if (valueType == IColumnTypes.INTEGER && otherType != IColumnTypes.INTEGER)
			{
				typeMismatch = true;
			}
			else if (valueType == IColumnTypes.NUMBER && (otherType != IColumnTypes.NUMBER || otherType != IColumnTypes.INTEGER))
			{
				typeMismatch = true;
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
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Wrong type", msg);
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
