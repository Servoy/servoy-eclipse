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

import java.io.IOException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.editors.FormatDialog;
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
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;

public class ColumnDetailsComposite extends Composite
{

	public static final String[] DATE_FORMAT_VALUES = new String[] { "#%", "\u00A4#.00",//first char is currency symbol //$NON-NLS-1$
	"dd-MM-yyyy", "dd/MM/yyyy", "MM-dd-yyyy", "MM/dd/yyyy", "dd-MM-yyyy HH:mm:ss", "MM-dd-yyyy hh:mm:ss", "dd-MM-yyyy HH:mm", "MM-dd-yyyy hh:mm", "yyyy-MM-dd HH:mm:ss.S" };

	private DataBindingContext bindingContext;
	private final TreeSelectViewer foreignTypeTreeSelect;
	private final Composite formatComposite;
	private final Text defaultFormat;
	private final Composite titleComposite;
	private final Text titleText;
	private final Text descriptionText;
	private final Button suggestForeignTypeButton;
	private final Button excludedCheckBox;
	private final Button uuidCheckBox;
	private SuggestForeignTypesWizard suggestForeignTypesWizard;


	private String serverName;

	private Column column;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public ColumnDetailsComposite(Composite parent, int style)
	{
		super(parent, style);

		Label titleLabel;
		titleLabel = new Label(this, SWT.NONE);
		titleLabel.setText("Title");

		Label defaultFormatLabel;
		defaultFormatLabel = new Label(this, SWT.NONE);
		defaultFormatLabel.setText("Default format");

		Label foreignTypeLabel;
		foreignTypeLabel = new Label(this, SWT.NONE);
		foreignTypeLabel.setText("Foreign type");

		Label descriptionLabel;
		descriptionLabel = new Label(this, SWT.NONE);
		descriptionLabel.setText("Description");

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
		titleButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
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
		formatButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
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
			}
		});

		foreignTypeTreeSelect = new TreeSelectViewer(this, SWT.NONE, TableValueEditor.INSTANCE);
		foreignTypeTreeSelect.setTitleText("Select foreign table");
		foreignTypeTreeSelect.setName("foreignDialog");
		foreignTypeTreeSelect.setContentProvider(new TableContentProvider());
		foreignTypeTreeSelect.setLabelProvider(DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY);
		foreignTypeTreeSelect.setEditable(true);

		Control foreignTypeControl = foreignTypeTreeSelect.getControl();

		suggestForeignTypeButton = new Button(this, SWT.PUSH);
		suggestForeignTypeButton.setText("Suggest...");
		suggestForeignTypeButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				IStructuredSelection selection = StructuredSelection.EMPTY;
				suggestForeignTypesWizard.init(PlatformUI.getWorkbench(), selection);
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), suggestForeignTypesWizard);
				dialog.create();
				dialog.open();
			}
		});

		Label flagsLabel;
		flagsLabel = new Label(this, SWT.NONE);
		flagsLabel.setText("Flags");

		excludedCheckBox = new Button(this, SWT.CHECK);
		uuidCheckBox = new Button(this, SWT.CHECK);

		uuidCheckBox.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
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
							int convType = columnConverter.getToObjectType(ComponentFactory.<String> parseJSonProperties(column.getColumnInfo().getConverterProperties()));
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
			}
		});

		excludedCheckBox.setText("excluded");
		uuidCheckBox.setText("UUID");

		final GroupLayout groupLayout = new GroupLayout(this);

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
		add(groupLayout.createParallelGroup(GroupLayout.LEADING).
		//add default format label;
		add(titleLabel).
		//add title label;
		add(defaultFormatLabel).
		//add foreign type label;
		add(foreignTypeLabel).
		//add flags label
		add(flagsLabel)).add(10, 10, 10).
		//add the group
		add(
		//start parallel group
		groupLayout.createParallelGroup(GroupLayout.LEADING).
		//add the title text-box;
		add(titleComposite, GroupLayout.PREFERRED_SIZE, 450, Short.MAX_VALUE).
		//add the default format combo-box;
		add(formatComposite, GroupLayout.PREFERRED_SIZE, 450, Short.MAX_VALUE).
		//add the foreign-type combo-box
		add(groupLayout.createSequentialGroup().add(foreignTypeControl, GroupLayout.PREFERRED_SIZE, 450, Short.MAX_VALUE).
		//
		addPreferredGap(LayoutStyle.RELATED).
		//add the suggest button for foreign type
		add(suggestForeignTypeButton)).
		//add other flags combo-box
		add(
		//add the flags
		groupLayout.createSequentialGroup().
		//
		add(excludedCheckBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
		//
		addPreferredGap(LayoutStyle.RELATED).
		//
		add(uuidCheckBox, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)

		//end adding the flags
		)
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

		groupLayout.setVerticalGroup(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.CENTER, false).add(titleLabel).add(titleComposite)).addPreferredGap(LayoutStyle.RELATED).add(
			groupLayout.createParallelGroup(GroupLayout.CENTER, false).add(defaultFormatLabel).add(formatComposite)).addPreferredGap(LayoutStyle.RELATED).add(
			groupLayout.createParallelGroup(GroupLayout.CENTER, false).
			// foreign type label;
			add(foreignTypeLabel).
			//foreign type treeselect;
			add(foreignTypeControl, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE).
			//suggest button for foreign type
			add(suggestForeignTypeButton, 0, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)).addPreferredGap(LayoutStyle.RELATED).add(
			groupLayout.createParallelGroup(GroupLayout.CENTER, false).add(flagsLabel).
			//other flags combo
			add(excludedCheckBox).add(uuidCheckBox)).addPreferredGap(LayoutStyle.UNRELATED).add(descriptionLabel).addPreferredGap(LayoutStyle.RELATED).add(
			descriptionText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE).addContainerGap());

		setLayout(groupLayout);
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
				IColumnConverter converter = ApplicationServerSingleton.get().getPluginManager().getColumnConverterManager().getConverter(converterName);
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

	void initDataBindings(final Column c)
	{
		this.column = c;
		serverName = c.getTable().getServerName();

		bindingContext = BindingHelper.dispose(bindingContext);

		try
		{
			// Fill foreign type treeselect
			foreignTypeTreeSelect.setInput(new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, true, c.getTable().getServerName()));

			// fill flags combo
			String[] flagStrings = new String[Column.allDefinedOtherFlags.length + 1];
			int j = 0;
			flagStrings[j++] = "";
			for (int element : Column.allDefinedOtherFlags)
			{
				if (Column.getFlagsString(element).equals("excluded"))
				{
					excludedCheckBox.setSelection(c.getColumnInfo().hasFlag(element));
				}
				if (Column.getFlagsString(element).equals("uuid"))
				{
					uuidCheckBox.setSelection(c.getColumnInfo().hasFlag(element));
				}
			}

			if (c.getColumnInfo() != null && (c.getColumnInfo().getDefaultFormat() == null || c.getColumnInfo().getDefaultFormat().equals("")))
			{
				this.defaultFormat.setText("");
			}
			else if (c.getColumnInfo() == null)
			{
				this.defaultFormat.setText("");
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		ColumnInfoBean columnInfoBean = new ColumnInfoBean(c.getColumnInfo());

		IObservableValue getCIDescriptionObserveValue = PojoObservables.observeValue(columnInfoBean, "description");
		IObservableValue descriptionTextObserveWidget = SWTObservables.observeText(descriptionText, SWT.Modify);

		IObservableValue getCITitleObserveValue = PojoObservables.observeValue(columnInfoBean, "titleText");
		IObservableValue titleTextObserveWidget = SWTObservables.observeText(titleText, SWT.Modify);

		IObservableValue getCIDefaultFormatObserveValue = PojoObservables.observeValue(columnInfoBean, "defaultFormat");
		IObservableValue defaultFormatTextObserveWidget = SWTObservables.observeText(defaultFormat, SWT.Modify);

		IObservableValue getCIForeignTypeObserveValue = PojoObservables.observeValue(columnInfoBean, "foreignType");
		IObservableValue foreignTypeTextObserveWidget = new TreeSelectObservableValue(foreignTypeTreeSelect, TableWrapper.class);

		//TODO: put the hardcoded strings in a list;
		IObservableValue getCIOtherFlagsObserveValue1 = PojoObservables.observeValue(columnInfoBean, "excludedFlag");
		IObservableValue getCIOtherFlagsObserveValue2 = PojoObservables.observeValue(columnInfoBean, "uuidFlag");

		IObservableValue uuidOtherFlagsTextObserveWidget = SWTObservables.observeSelection(uuidCheckBox);
		IObservableValue excludedOtherFlagsTextObserveWidget = SWTObservables.observeSelection(excludedCheckBox);

		if (listener != null)
		{
			getCIOtherFlagsObserveValue1.addValueChangeListener(listener);
		}
		bindingContext = new DataBindingContext();

		bindingContext.bindValue(descriptionTextObserveWidget, getCIDescriptionObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCIDescriptionObserveValue)), null);
		bindingContext.bindValue(titleTextObserveWidget, getCITitleObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCITitleObserveValue)), null);
		bindingContext.bindValue(defaultFormatTextObserveWidget, getCIDefaultFormatObserveValue,
			new UpdateValueStrategy().setAfterGetValidator(new NotSameValidator(getCIDefaultFormatObserveValue)), null);
		bindingContext.bindValue(foreignTypeTextObserveWidget, getCIForeignTypeObserveValue,
			new UpdateValueStrategy().setConverter(TableWrapper2ForeignTypeConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(new ForeignType2TableWrapperConverter(c.getTable().getServerName(),
				c.getTable().getTableType() == ITable.VIEW)));

		//bind the 'excluded' checkbox;
		bindingContext.bindValue(excludedOtherFlagsTextObserveWidget, getCIOtherFlagsObserveValue1, null, null);

		//bind the 'UUID' checkbox
		bindingContext.bindValue(uuidOtherFlagsTextObserveWidget, getCIOtherFlagsObserveValue2, null, null);

		suggestForeignTypesWizard = new SuggestForeignTypesWizard(serverName);
		IObservableValue foreignTypeInWizard = suggestForeignTypesWizard.setColumnToTrace(c.getTable().getName(), c.getName(), columnInfoBean.getForeignType());
		bindingContext.bindValue(foreignTypeInWizard, getCIForeignTypeObserveValue, null, null);

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
			super(TableWrapper.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject == null || TableContentProvider.TABLE_NONE.equals(fromObject)) return null;
			return ((TableWrapper)fromObject).getTableName();
		}
	}

	public static class ForeignType2TableWrapperConverter extends Converter
	{
		private final String serverName;
		private final boolean isView;

		public ForeignType2TableWrapperConverter(String serverName, boolean isView)
		{
			super(String.class, TableWrapper.class);
			this.serverName = serverName;
			this.isView = isView;
		}

		public Object convert(Object fromObject)
		{
			if (fromObject == null) return null;
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
