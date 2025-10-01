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

import static com.servoy.j2db.persistence.SortingNullprecedence.ascNullsFirst;
import static com.servoy.j2db.persistence.SortingNullprecedence.ascNullsLast;
import static com.servoy.j2db.persistence.SortingNullprecedence.databaseDefault;
import static java.util.Arrays.stream;

import java.io.IOException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.InMemServerWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.model.util.ViewFoundsetServerWrapper;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.editors.FormatDialog;
import com.servoy.eclipse.ui.editors.table.ColumnInfoBean.BooleanTristate;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.property.TableValueEditor;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.views.TreeSelectObservableValue;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.eclipse.ui.wizards.SuggestForeignTypesWizard;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.ITypedColumnConverter;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;

public class ColumnDetailsComposite extends Composite
{

	public static final String[] DATE_FORMAT_VALUES = new String[] { "#%", "\u00A4#.00", //first char is currency symbol
		"dd-MM-yyyy", "dd/MM/yyyy", "MM-dd-yyyy", "MM/dd/yyyy", "dd-MM-yyyy HH:mm:ss", "MM-dd-yyyy hh:mm:ss", "dd-MM-yyyy HH:mm", "MM-dd-yyyy hh:mm", "yyyy-MM-dd HH:mm:ss.S" };

	private DataBindingContext bindingContext;
	private final TreeSelectViewer foreignTypeTreeSelect;
	private final Composite formatComposite;
	private final Text defaultFormat;
	private final Composite titleComposite;
	private final Text titleText;
	private final Text descriptionText;
	private final Button suggestForeignTypeButton;
	private final Combo sortIgnoringcaseCombo;
	private final Combo sortNullprecedenceCombo;
	private final Button excludedCheckBox;
	private final Button noDataLogCheckBox;
	private final Button uuidCheckBox;
	private final Button tenantCheckBox;
	private SuggestForeignTypesWizard suggestForeignTypesWizard;


	private String dataSource;

	private Column column;

	private final boolean isViewFoundsetTable;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 * @param isViewFoundsetTable
	 */
	public ColumnDetailsComposite(Composite parent, int style, boolean isViewFoundsetTable)
	{
		super(parent, style);
		this.isViewFoundsetTable = isViewFoundsetTable;

		Label titleLabel = label(this, "Title");
		Label defaultFormatLabel = label(this, "Default format");
		Label foreignTypeLabel = label(this, "Foreign type");
		Label sortingLabel = label(this, "Sorting");
		Label flagsLabel = label(this, "Flags");
		Label descriptionLabel = label(this, "Description");

		descriptionText = new Text(this, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);

		titleComposite = new Composite(this, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;

		titleComposite.setLayout(layout);
		titleText = new Text(titleComposite, SWT.BORDER);
		titleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button titleButton = new Button(titleComposite, SWT.PUSH);
		titleButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		titleButton.setText("...");
		titleButton.addListener(SWT.Selection, event -> {
			if (column != null)
			{
				TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(getShell(), null, null, column.getTable(), titleText.getText(), "Edit title",
					Activator.getDefault().getDesignClient(), false);
				dialog.open();
				if (dialog.getReturnCode() != Window.CANCEL)
				{
					String value = (String)dialog.getValue();
					titleText.setText(value == null ? "" : value); // TODO: use label provider to hide json format
				}
			}
		});

		formatComposite = new Composite(this, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;

		formatComposite.setLayout(layout);
		defaultFormat = new Text(formatComposite, SWT.BORDER);
		defaultFormat.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button formatButton = new Button(formatComposite, SWT.PUSH);
		formatButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		formatButton.setText("...");
		formatButton.addListener(SWT.Selection, event -> {
			if (column != null)
			{
				// base the format dialog on the type of the default converter
				ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, column, Activator.getDefault().getDesignClient());
				FormatDialog fd = new FormatDialog(getShell(), defaultFormat.getText(), componentFormat.dpType);
				fd.open();
				if (fd.getReturnCode() != Window.CANCEL)
				{
					String property = fd.getFormatString();
					defaultFormat.setText(property == null ? "" : property); // TODO: use label provider to hide json format
				}
			}
		});

		foreignTypeTreeSelect = new TreeSelectViewer(this, SWT.NONE, TableValueEditor.INSTANCE);
		Control foreignTypeControl = foreignTypeTreeSelect.getControl();
		suggestForeignTypeButton = new Button(this, SWT.PUSH);
		if (!isViewFoundsetTable)
		{
			foreignTypeTreeSelect.setTitleText("Select foreign table");
			foreignTypeTreeSelect.setName("foreignDialog");
			foreignTypeTreeSelect.setContentProvider(new TableContentProvider());
			foreignTypeTreeSelect.setLabelProvider(DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY);
			foreignTypeTreeSelect.setEditable(true);


			suggestForeignTypeButton.setText("Suggest...");
			suggestForeignTypeButton.addListener(SWT.Selection, event -> {
				IStructuredSelection selection = StructuredSelection.EMPTY;
				suggestForeignTypesWizard.init(PlatformUI.getWorkbench(), selection);
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), suggestForeignTypesWizard);
				dialog.create();
				dialog.open();
			});
		}
		else
		{
			foreignTypeControl.setVisible(false);
			foreignTypeLabel.setVisible(false);
			suggestForeignTypeButton.setVisible(false);
		}

		Label sortIgnoringcaseComboLabel = label(this, "Ignoring case");
		sortIgnoringcaseCombo = combo(this, (Object[])BooleanTristate.values());
		sortIgnoringcaseCombo.setEnabled(false);
		Label sortNullprecedenceLabel = label(this, "Null precedence");
		sortNullprecedenceCombo = combo(this, databaseDefault.display(), ascNullsFirst.display(), ascNullsLast.display());

		excludedCheckBox = checkbox(this, "Excluded");
		uuidCheckBox = checkbox(this, "UUID");
		tenantCheckBox = checkbox(this, "Tenant");
		noDataLogCheckBox = checkbox(this, "No data log");

		if (!isViewFoundsetTable)
		{
			tenantCheckBox.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					if (tenantCheckBox.getSelection())
					{
						for (Column col : column.getTable().getColumns())
						{
							if (col.getColumnInfo().hasFlag(IBaseColumn.TENANT_COLUMN) && col != column)
							{
								UIUtils.reportWarning("Warning", "There is already another column marked as tenant: " + col.getName());
								break;
							}
						}
					}
				}
			});
		}
		else
		{
			excludedCheckBox.setVisible(false);
			tenantCheckBox.setVisible(false);
			noDataLogCheckBox.setVisible(false);
		}

		uuidCheckBox.addListener(SWT.Selection, event -> {
			if (uuidCheckBox.getSelection())
			{
				int length = column.getConfiguredColumnType().getLength();
				int datatype = column.getConfiguredColumnType().getSqlType();
				// use converted type if available
				ITypedColumnConverter columnConverter = getColumnConverter(column);
				if (columnConverter != null)
				{
					try
					{
						int convType = columnConverter.getToObjectType(
							ComponentFactory.parseJSonProperties(column.getColumnInfo().getConverterProperties()));
						if (convType != Integer.MAX_VALUE)
						{
							length = 0;
							datatype = convType;
						}
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
				}

				boolean compatibleForUUID = false;
				switch (Column.mapToDefaultType(datatype))
				{
					case IColumnTypes.MEDIA :
						compatibleForUUID = length == 0 || length >= 16;
						break;
					case IColumnTypes.TEXT :
						compatibleForUUID = length == 0 || length >= 36;
						break;
				}
				if (!compatibleForUUID)
				{
					UIUtils.reportWarning("Warning", "The column type and/or length are not compatible with UUID (MEDIA:16 or TEXT:36).");
					uuidCheckBox.setSelection(false);
				}
			}
		});

		GroupLayout groupLayout = new GroupLayout(this);

		SequentialGroup foreignTypeHorizontalGroup = isViewFoundsetTable ? groupLayout.createSequentialGroup().addContainerGap()
			: groupLayout.createSequentialGroup().add(foreignTypeControl, GroupLayout.PREFERRED_SIZE, 450, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(suggestForeignTypeButton);

		SequentialGroup sortingHorizontalGroup = groupLayout.createSequentialGroup()
			.add(sortIgnoringcaseCombo, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED)
			.add(sortIgnoringcaseComboLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.UNRELATED)
			.add(sortNullprecedenceCombo, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED)
			.add(sortNullprecedenceLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED);

		SequentialGroup flagsHorizontalGroup = groupLayout.createSequentialGroup();
		if (!isViewFoundsetTable)
		{
			flagsHorizontalGroup.add(excludedCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED);
		}
		flagsHorizontalGroup.add(uuidCheckBox, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
			LayoutStyle.RELATED);
		if (!isViewFoundsetTable)
		{
			flagsHorizontalGroup.add(tenantCheckBox, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED);
			flagsHorizontalGroup.add(noDataLogCheckBox, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED);
		}


		ParallelGroup labelsHorizontalGroup = groupLayout.createParallelGroup(GroupLayout.LEADING).
		// Title
			add(titleLabel).
			// Default format
			add(defaultFormatLabel);
		if (!isViewFoundsetTable)
		{
			// Foreign type
			labelsHorizontalGroup.add(foreignTypeLabel);
		}
		// Sorting
		labelsHorizontalGroup.add(sortingLabel);
		// Flags
		labelsHorizontalGroup.add(flagsLabel);

		groupLayout.setHorizontalGroup(
			//here we create a parallel group
			groupLayout.createSequentialGroup().
			//add a container gap;
				addContainerGap().
				//add group
				add(groupLayout.createParallelGroup(GroupLayout.LEADING).
				//add the description text
					add(GroupLayout.TRAILING, descriptionText, GroupLayout.PREFERRED_SIZE, 522, Short.MAX_VALUE).
					//add group
					add(
						//start group
						GroupLayout.TRAILING, groupLayout.createSequentialGroup().
						//add group with default format label, title label, foreign type label, flags label
							add(labelsHorizontalGroup).add(10, 10, 10).
							//add the group
							add(
								//start parallel group
								groupLayout.createParallelGroup(GroupLayout.LEADING).
								// Title
									add(titleComposite, GroupLayout.PREFERRED_SIZE, 450, Short.MAX_VALUE).
									// Default format
									add(formatComposite, GroupLayout.PREFERRED_SIZE, 450, Short.MAX_VALUE).
									// Foreign type
									add(foreignTypeHorizontalGroup).
									// Sorting
									add(sortingHorizontalGroup).
									// Flags
									add(flagsHorizontalGroup)
							//-- end group layout
							)
					//-- end group
					).add(
						//create a sequential group
						groupLayout.createSequentialGroup().
						//add the items;
							add(descriptionLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).
							// add
							add(450, 450, 450)
					//end sequential step
					)).
				//add a container gap
				addContainerGap());

		//other flags combo
		ParallelGroup flagsVerticalGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false).add(flagsLabel);
		if (!isViewFoundsetTable) flagsVerticalGroup.add(excludedCheckBox);
		flagsVerticalGroup.add(uuidCheckBox);
		if (!isViewFoundsetTable)
		{
			flagsVerticalGroup.add(tenantCheckBox);
			flagsVerticalGroup.add(noDataLogCheckBox);
		}

		ParallelGroup foreignTypeVerticalGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false);
		if (!isViewFoundsetTable)
		{
			foreignTypeVerticalGroup.add(foreignTypeLabel).
			//foreign type treeselect;
				add(foreignTypeControl, 0, isViewFoundsetTable ? 0 : GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE).
				//suggest button for foreign type
				add(suggestForeignTypeButton, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE);
		}

		groupLayout.setVerticalGroup(groupLayout.createSequentialGroup().addContainerGap()
			// Title
			.add(groupLayout.createParallelGroup(GroupLayout.CENTER, false)
				.add(titleLabel).add(titleComposite))

			.addPreferredGap(LayoutStyle.RELATED)

			// Default format
			.add(groupLayout.createParallelGroup(GroupLayout.CENTER, false)
				.add(defaultFormatLabel).add(formatComposite))

			.addPreferredGap(LayoutStyle.RELATED)

			// Foreign type
			.add(foreignTypeVerticalGroup)

			.addPreferredGap(LayoutStyle.RELATED)

			// Sorting
			.add(groupLayout.createParallelGroup(GroupLayout.CENTER, false)
				.add(sortingLabel)
				.add(sortIgnoringcaseCombo, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)
				.add(sortIgnoringcaseComboLabel, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)
				.add(sortNullprecedenceCombo, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)
				.add(sortNullprecedenceLabel, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE))

			.addPreferredGap(LayoutStyle.RELATED)

			// Flags
			.add(flagsVerticalGroup)

			.addPreferredGap(LayoutStyle.UNRELATED)

			// Description
			.add(descriptionLabel).addPreferredGap(LayoutStyle.RELATED)
			.add(descriptionText, 100, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)

			.addContainerGap());

		setLayout(groupLayout);
	}

	private static Combo combo(Composite parent, Object... values)
	{
		Combo combo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		combo.setVisibleItemCount(UIUtils.COMBO_VISIBLE_ITEM_COUNT);
		stream(values).map(String::valueOf).forEach(combo::add);
		return combo;
	}

	private static Label label(Composite parent, String text)
	{
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		return label;
	}

	private static Button checkbox(Composite parent, String text)
	{
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);
		return button;
	}

	private static ITypedColumnConverter getColumnConverter(Column column)
	{
		ColumnInfo columnInfo = column.getColumnInfo();
		if (columnInfo != null)
		{
			String converterName = columnInfo.getConverterName();
			if (converterName != null)
			{
				// check type defined by column converter
				IColumnConverter converter = ApplicationServerRegistry.get().getPluginManager().getColumnConverterManager().getConverter(converterName);
				if (converter instanceof ITypedColumnConverter)
				{
					return (ITypedColumnConverter)converter;
				}
			}
		}
		return null;
	}


	public void refresh()
	{
		bindingContext.updateTargets();
	}

	void initDataBindings(Column col)
	{
		this.column = col;
		dataSource = column.getTable().getDataSource();

		bindingContext = BindingHelper.dispose(bindingContext);

		try
		{
			// Fill foreign type treeselect
			if (!isViewFoundsetTable)
			{
				foreignTypeTreeSelect.setInput(
					new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, true, column.getTable().getServerName()));
			}

			if (column.getColumnInfo() != null && (column.getColumnInfo().getDefaultFormat() == null || column.getColumnInfo().getDefaultFormat().equals("")))
			{
				this.defaultFormat.setText("");
			}
			else if (column.getColumnInfo() == null)
			{
				this.defaultFormat.setText("");
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		sortIgnoringcaseCombo.setEnabled(column.getDataProviderType() == IColumnTypes.TEXT && !column.isUUID());

		ColumnInfoBean columnInfoBean = new ColumnInfoBean(column.getColumnInfo());

		IObservableValue getCIDescriptionObserveValue = PojoProperties.value(ColumnInfoBean.class, "description").observe(columnInfoBean);
		IObservableValue descriptionTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(descriptionText);

		IObservableValue getCITitleObserveValue = PojoProperties.value("titleText").observe(columnInfoBean);
		IObservableValue titleTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(titleText);

		IObservableValue getCIDefaultFormatObserveValue = PojoProperties.value("defaultFormat").observe(columnInfoBean);
		IObservableValue defaultFormatTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(defaultFormat);

		IObservableValue getCIForeignTypeObserveValue = PojoProperties.value("foreignType").observe(columnInfoBean);
		IObservableValue foreignTypeTextObserveWidget = new TreeSelectObservableValue(foreignTypeTreeSelect, IDataSourceWrapper.class);

		//TODO: put the hardcoded strings in a list;
		IObservableValue getCISortIgnorecaseObservableValue = PojoProperties.value("sortIgnorecase").observe(columnInfoBean);
		IObservableValue getCISortingNullprecedenceObservableValue = PojoProperties.value("sortingNullprecedence").observe(columnInfoBean);
		IObservableValue getCIFlagsExcludedObserveValue = PojoProperties.value(ColumnInfoBean.class, "excludedFlag").observe(columnInfoBean);
		IObservableValue getCIFlagsUuidObserveValue = PojoProperties.value("uuidFlag").observe(columnInfoBean);
		IObservableValue getCIFlagsTenantObserveValue = PojoProperties.value("tenantFlag").observe(columnInfoBean);
		IObservableValue getCIFlagsNoDataLogObserveValue = PojoProperties.value(ColumnInfoBean.class, "noDataLogFlag").observe(columnInfoBean);

		IObservableValue sortIgnoringcaseComboObserveWidget = WidgetProperties.widgetSelection().observe(sortIgnoringcaseCombo);
		IObservableValue sortingNullprecedenceComboObserveWidget = WidgetProperties.widgetSelection().observe(sortNullprecedenceCombo);
		IObservableValue uuidOtherFlagsTextObserveWidget = WidgetProperties.widgetSelection().observe(uuidCheckBox);
		IObservableValue excludedOtherFlagsTextObserveWidget = WidgetProperties.widgetSelection().observe(excludedCheckBox);
		IObservableValue tenantOtherFlagsTextObserveWidget = WidgetProperties.widgetSelection().observe(tenantCheckBox);
		IObservableValue noDataLogFlagsTextObserveWidget = WidgetProperties.widgetSelection().observe(noDataLogCheckBox);

		if (listener != null)
		{
			getCIFlagsExcludedObserveValue.addValueChangeListener(listener);
		}
		bindingContext = new DataBindingContext();

		bindingContext.bindValue(descriptionTextObserveWidget, getCIDescriptionObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCIDescriptionObserveValue)), null);
		bindingContext.bindValue(titleTextObserveWidget, getCITitleObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCITitleObserveValue)), null);
		bindingContext.bindValue(defaultFormatTextObserveWidget, getCIDefaultFormatObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCIDefaultFormatObserveValue)), null);
		bindingContext.bindValue(foreignTypeTextObserveWidget, getCIForeignTypeObserveValue,
			new UpdateValueStrategy().setConverter(TableWrapper2ForeignTypeConverter.INSTANCE), new UpdateValueStrategy().setConverter(
				new ForeignType2TableWrapperConverter(column.getTable().getServerName(), column.getTable().getTableType() == ITable.VIEW)));

		// bind the 'sortIgnorecase' combo;
		bindingContext.bindValue(sortIgnoringcaseComboObserveWidget, getCISortIgnorecaseObservableValue, null, null);

		// bind the 'sortingNullprecedence' combo;
		bindingContext.bindValue(sortingNullprecedenceComboObserveWidget, getCISortingNullprecedenceObservableValue, null, null);

		// bind the 'excluded' checkbox;
		bindingContext.bindValue(excludedOtherFlagsTextObserveWidget, getCIFlagsExcludedObserveValue, null, null);

		// bind the 'UUID' checkbox
		bindingContext.bindValue(uuidOtherFlagsTextObserveWidget, getCIFlagsUuidObserveValue, null, null);

		// bind the 'TENANT' checkbox
		bindingContext.bindValue(tenantOtherFlagsTextObserveWidget, getCIFlagsTenantObserveValue, null, null);

		// bind the 'noDataLog' checkbox;
		bindingContext.bindValue(noDataLogFlagsTextObserveWidget, getCIFlagsNoDataLogObserveValue, null, null);

		suggestForeignTypesWizard = new SuggestForeignTypesWizard(dataSource);
		IObservableValue foreignTypeInWizard = suggestForeignTypesWizard.setColumnToTrace(column.getTable().getName(), column.getName(),
			columnInfoBean.getForeignType());
		bindingContext.bindValue(foreignTypeInWizard, getCIForeignTypeObserveValue, null, null);

		BindingHelper.addGlobalChangeListener(bindingContext, event -> getShell().getDisplay().asyncExec(column::flagColumnInfoChanged));
	}

	private IValueChangeListener listener;

	public void addValueChangeListener(IValueChangeListener listener)
	{
		this.listener = listener;
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	/**
	 * This will be called whenever the selection in the column-table will occur.
	 *
	 * @param sequenceName
	 */
	public void refreshUuidCheckBoxState(String sequenceName)
	{
		//TODO: place the hardcoded strings in a list;
		this.uuidCheckBox.setSelection("uuid generator".equals(sequenceName));
	}

	// Converters to convert between TableWrapper (used in TreeSelectViewer) to String (Foreign type in ColumnInfo)
	public static class TableWrapper2ForeignTypeConverter extends Converter
	{
		public final static TableWrapper2ForeignTypeConverter INSTANCE = new TableWrapper2ForeignTypeConverter();

		private TableWrapper2ForeignTypeConverter()
		{
			super(IDataSourceWrapper.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject == null || TableContentProvider.TABLE_NONE.equals(fromObject)) return null;
			return ((IDataSourceWrapper)fromObject).getTableName();
		}
	}

	public static class ForeignType2TableWrapperConverter extends Converter
	{
		private final String serverName;
		private final boolean isView;

		public ForeignType2TableWrapperConverter(String serverName, boolean isView)
		{
			super(String.class, IDataSourceWrapper.class);
			this.serverName = serverName;
			this.isView = isView;
		}

		public Object convert(Object fromObject)
		{
			if (fromObject == null) return null;
			// TODO this should be more generic
			if (serverName.equals(DataSourceUtils.INMEM_DATASOURCE))
			{
				return new InMemServerWrapper((String)fromObject);
			}
			else if (serverName.equals(DataSourceUtils.VIEW_DATASOURCE))
			{
				return new ViewFoundsetServerWrapper((String)fromObject);
			}
			return new TableWrapper(serverName, (String)fromObject, isView);
		}
	}
	public static class NotSameValidator implements IValidator
	{
		IObservableValue oldValue;

		public NotSameValidator(IObservableValue oldValue)
		{
			this.oldValue = oldValue;
		}

		public IStatus validate(Object value)
		{
			String oldString = (String)oldValue.getValue();
			String newString = (String)value;
			if (newString == null && "".equals(oldString)) return Status.CANCEL_STATUS;
			if (oldString == null && "".equals(newString)) return Status.CANCEL_STATUS;
			return Status.OK_STATUS;
		}
	}

	public void checkValidState() throws RepositoryException
	{
		if (!foreignTypeTreeSelect.isValid())
		{
			throw new RepositoryException("Foreign type is invalid.");
		}
	}
}
