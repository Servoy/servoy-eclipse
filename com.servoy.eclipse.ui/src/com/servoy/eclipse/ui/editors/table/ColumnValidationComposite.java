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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.json.JSONException;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.util.MapEntryValueEditor;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IColumnValidator;
import com.servoy.j2db.dataprocessing.IPropertyDescriptorProvider;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class ColumnValidationComposite extends Composite
{
	private final Combo combo;
	private final MapEntryValueEditor tableViewer;

	private Column column;
	private final Table table;
	private final Composite tableContainer;
	private final RelayEditorProvider relayEditorProvider;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public ColumnValidationComposite(final TableEditor te, Composite parent, int style)
	{
		super(parent, style);

		Button check = new Button(this, SWT.CHECK);
		check.setText("Only execute validators on validate/save");
		check.setSelection(Boolean.parseBoolean(Settings.getInstance().getProperty("servoy.execute.column.validators.only.on.validate_and_save", "true")));
		check.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				Settings.getInstance().setProperty("servoy.execute.column.validators.only.on.validate_and_save", Boolean.toString(check.getSelection()));
				try
				{
					Settings.getInstance().save();
				}
				catch (Exception e1)
				{
				}
			}
		});

		combo = new Combo(this, SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(combo);
		combo.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}

			public void widgetSelected(SelectionEvent e)
			{
				te.flushTable();
				if (column != null && column.getColumnInfo() != null)
				{
					column.getColumnInfo().setValidatorProperties(null);
				}
				loadTable();
				saveTable();
			}
		});
		tableContainer = new Composite(this, SWT.NONE);
		relayEditorProvider = new RelayEditorProvider();
		tableViewer = new MapEntryValueEditor(tableContainer, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION, relayEditorProvider);
		tableViewer.getObservable().addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flushTable();
				saveTable();
			}
		});

		table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE)
					.add(
						GroupLayout.LEADING, combo, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE)
					.add(
						GroupLayout.LEADING, check, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE))
				.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(check, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(tableContainer, GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE).addContainerGap()));
		setLayout(groupLayout);
		//
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public void initDataBindings(Column c)
	{
		this.column = c;

		ColumnInfo ci = c.getColumnInfo();
		int selectedIndex = 0;

		Map<String, IColumnValidator> validators = ApplicationServerRegistry.get().getPluginManager().getColumnValidatorManager().getValidators();
		List<String> options = new ArrayList<String>();
		options.add("none");
		for (IColumnValidator conv : validators.values())
		{
			String name = conv.getName();
			int[] conv_types = conv.getSupportedColumnTypes();
			if (conv_types != null)
			{
				for (int element : conv_types)
				{
					if (Column.mapToDefaultType(c.getConfiguredColumnType().getSqlType()) == Column.mapToDefaultType(element))
					{
						options.add(name);
						if (name.equals(ci.getValidatorName()))
						{
							selectedIndex = options.size() - 1;
						}
					}
				}
			}
			else
			{
				//works on all columns
				options.add(name);
				if (name.equals(ci.getValidatorName()))
				{
					selectedIndex = options.size() - 1;
				}
			}
		}
		combo.setItems(options.toArray(new String[options.size()]));
		combo.select(selectedIndex);
		loadTable();

	}

	private IColumnValidator getSelectedValidator()
	{
		Map<String, IColumnValidator> Validators = ApplicationServerRegistry.get().getPluginManager().getColumnValidatorManager().getValidators();
		String selectedConvertor = combo.getText();
		for (IColumnValidator conv : Validators.values())
		{
			String name = conv.getName();
			if (name.equals(selectedConvertor))
			{
				return conv;
			}
		}
		return null;
	}

	private void loadTable()
	{
		if (column != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();
			Map<String, String> props = new HashMap<String, String>();

			IColumnValidator conv = getSelectedValidator();
			if (conv != null)
			{
				//make sure it lists all defaults
				if (conv.getDefaultProperties() != null)
				{
					props.putAll(conv.getDefaultProperties());
				}

				try
				{
					Map<String, String> parsedValidatorProperties = ComponentFactory.parseJSonProperties(columnInfo.getValidatorProperties());
					if (parsedValidatorProperties != null)
					{
						props.putAll(parsedValidatorProperties);
					}
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
			}
			if (conv instanceof IPropertyDescriptorProvider)
			{
				relayEditorProvider.setPropertyDescriptorProvider((IPropertyDescriptorProvider)conv);
			}
			else
			{
				relayEditorProvider.setPropertyDescriptorProvider(null);
			}
			tableViewer.setInput(props.entrySet());
			if (props.size() > 0) table.select(0);
		}
	}

	private void saveTable()
	{
		if (column != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();

			IColumnValidator conv = getSelectedValidator();
			String props = null;
			if (conv != null)
			{
				columnInfo.setValidatorName(conv.getName());
				@SuppressWarnings("unchecked")
				Set<Map.Entry<String, String>> data = (Set<Map.Entry<String, String>>)tableViewer.getInput();
				if (data != null && data.size() > 0)
				{
					ServoyJSONObject json = new ServoyJSONObject();
					Map<String, String> defaults = conv.getDefaultProperties();

					for (Entry<String, String> pair : data)
					{
						if (defaults == null || (defaults != null && !Utils.equalObjects(defaults.get(pair.getKey()), pair.getValue())))
						{
							try
							{
								json.put(pair.getKey(), pair.getValue());
							}
							catch (JSONException e)
							{
								ServoyLog.logError(e);
							}
						}
					}

					if (json.length() > 0)
					{
						props = json.toString(false).trim();
					}
				}
			}
			else
			{
				columnInfo.setValidatorName(null);
			}
			columnInfo.setValidatorProperties(props);
			column.flagColumnInfoChanged();
		}
	}
}
