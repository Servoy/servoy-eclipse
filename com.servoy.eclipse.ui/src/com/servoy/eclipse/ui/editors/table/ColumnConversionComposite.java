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
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONException;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.ConvertersComposite;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

public class ColumnConversionComposite extends ConvertersComposite<IColumnConverter>
{
	private Column column;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public ColumnConversionComposite(final TableEditor te, Composite parent, int style)
	{
		super(parent, style);

		addConverterChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				te.flushTable();
				loadTable();
				saveTable();
			}
		});

		addPropertyChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flushTable();
				saveTable();
			}
		});
	}

	public void initDataBindings(Column c)
	{
		this.column = c;

		setConverters(c.getConfiguredColumnType().getSqlType(), c.getColumnInfo().getConverterName(),
			ApplicationServerRegistry.get().getPluginManager().getColumnConverterManager().getConverters().values());
		loadTable();
	}

	private void loadTable()
	{
		Map<String, String> props = null;
		if (column != null)
		{
			try
			{
				props = ComponentFactory.parseJSonProperties(column.getColumnInfo().getConverterProperties());
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
		setProperties(props);
	}

	private void saveTable()
	{
		if (column != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();

			IColumnConverter conv = getSelectedConverter();
			String props = null;
			if (conv != null)
			{
				columnInfo.setConverterName(conv.getName());
				Map<String, String> properties = getProperties();
				if (properties != null && properties.size() > 0)
				{
					ServoyJSONObject json = new ServoyJSONObject(false, false);
					Map<String, String> defaults = conv.getDefaultProperties();

					//subtract defaults, makes save smaller
					for (Entry<String, String> pair : properties.entrySet())
					{
						if (defaults == null || !Utils.equalObjects(defaults.get(pair.getKey()), pair.getValue()))
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
						props = Utils.stringReplace(json.toString(false).trim(), System.getProperty("line.separator"), "\n");// to avoid conflicts on different developer OSes with team when it's actually the same content
					}
				}
			}
			else
			{
				columnInfo.setConverterName(null);
			}
			columnInfo.setConverterProperties(props);
			column.flagColumnInfoChanged();
		}
	}

	@Override
	protected int[] getSupportedTypes(IColumnConverter converter)
	{
		return converter.getSupportedColumnTypes();
	}
}
