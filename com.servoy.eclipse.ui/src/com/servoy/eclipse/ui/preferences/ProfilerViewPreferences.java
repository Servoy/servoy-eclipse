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

package com.servoy.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;

/**
 * @author alorincz
 *
 */
public class ProfilerViewPreferences
{
	private static final String PROFILER_VIEW_SETTINGS_PREFIX = "profilerView.";

	public static final String METHOD_NAME_COLUMN_WIDTH_SETTING = "methodNameColumnWidth";
	public static final String OWN_TIME_COLUMN_WIDTH_SETTING = "ownTimeColumnWidth";
	public static final String TIME_COLUMN_WIDTH_SETTING = "timeColumnWidth";
	public static final String FILE_COLUMN_WIDTH_SETTING = "fileColumnWidth";
	public static final String ARGS_COLUMN_WIDTH_SETTING = "argsColumnWidth";
	public static final String NAME_TABLE_COLUMN_WIDTH_SETTING = "nameTableColumnWidth";
	public static final String TIME_TABLE_COLUMN_WIDTH_SETTING = "timeTableColumnWidth";
	public static final String QUERY_TABLE_COLUMN_WIDTH_SETTING = "queryTableColumnWidth";
	public static final String ARGUMENTS_TABLE_COLUMN_WIDTH_SETTING = "argumentsTableColumnWidth";
	public static final String DATASOURCE_TABLE_COLUMN_WIDTH_SETTING = "datasourceTableColumnWidth";
	public static final String TRANSACTION_TABLE_COLUMN_WIDTH_SETTING = "transactionTableColumnWidth";
	public static final String TREE_WIDTH_SETTING = "treeWidth";
	public static final String TABLE_WIDTH_SETTING = "tableWidth";

	public static final int METHOD_NAME_COLUMN_WIDTH_DEFAULT = 200;
	public static final int OWN_TIME_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TIME_COLUMN_WIDTH_DEFAULT = 80;
	public static final int FILE_COLUMN_WIDTH_DEFAULT = 400;
	public static final int ARGS_COLUMN_WIDTH_DEFAULT = 120;
	public static final int NAME_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TIME_TABLE_COLUMN_WIDTH_DEFAULT = 70;
	public static final int QUERY_TABLE_COLUMN_WIDTH_DEFAULT = 350;
	public static final int ARGUMENTS_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int DATASOURCE_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TRANSACTION_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TREE_WIDTH_DEFAULT = 50;
	public static final int TABLE_WIDTH_DEFAULT = 50;

	protected final IEclipsePreferences eclipsePreferences;

	public ProfilerViewPreferences()
	{
		eclipsePreferences = Activator.getDefault().getEclipsePreferences();
	}

	public void save()
	{
		try
		{
			eclipsePreferences.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	protected int getProperty(String key, int defaultValue)
	{
		return eclipsePreferences.getInt(PROFILER_VIEW_SETTINGS_PREFIX + key, defaultValue);
	}

	protected void setProperty(String key, int value)
	{
		eclipsePreferences.putInt(PROFILER_VIEW_SETTINGS_PREFIX + key, value);
	}

	public int getMethodNameColumnWidth()
	{
		int methodNameColumnWidth = getProperty(METHOD_NAME_COLUMN_WIDTH_SETTING, METHOD_NAME_COLUMN_WIDTH_DEFAULT);
		if (methodNameColumnWidth <= 0) return METHOD_NAME_COLUMN_WIDTH_DEFAULT;
		return methodNameColumnWidth;
	}

	public int getOwnTimeColumnWidth()
	{
		int ownTimeColumnWidth = getProperty(OWN_TIME_COLUMN_WIDTH_SETTING, OWN_TIME_COLUMN_WIDTH_DEFAULT);
		if (ownTimeColumnWidth <= 0) return OWN_TIME_COLUMN_WIDTH_DEFAULT;
		return ownTimeColumnWidth;
	}

	public int getTimeColumnWidth()
	{
		int timeColumnWidth = getProperty(TIME_COLUMN_WIDTH_SETTING, TIME_COLUMN_WIDTH_DEFAULT);
		if (timeColumnWidth <= 0) return TIME_COLUMN_WIDTH_DEFAULT;
		return timeColumnWidth;
	}

	public int getFileColumnWidth()
	{
		int fileColumnWidth = getProperty(FILE_COLUMN_WIDTH_SETTING, FILE_COLUMN_WIDTH_DEFAULT);
		if (fileColumnWidth <= 0) return FILE_COLUMN_WIDTH_DEFAULT;
		return fileColumnWidth;
	}

	public int getArgsColumnWidth()
	{
		int argsColumnWidth = getProperty(ARGS_COLUMN_WIDTH_SETTING, ARGS_COLUMN_WIDTH_DEFAULT);
		if (argsColumnWidth <= 0) return ARGS_COLUMN_WIDTH_DEFAULT;
		return argsColumnWidth;
	}

	public int getNameTableColumnWidth()
	{
		int nameTableColumnWidth = getProperty(NAME_TABLE_COLUMN_WIDTH_SETTING, NAME_TABLE_COLUMN_WIDTH_DEFAULT);
		if (nameTableColumnWidth <= 0) return NAME_TABLE_COLUMN_WIDTH_DEFAULT;
		return nameTableColumnWidth;
	}

	public int getTimeTableColumnWidth()
	{
		int timeTableColumnWidth = getProperty(TIME_TABLE_COLUMN_WIDTH_SETTING, TIME_TABLE_COLUMN_WIDTH_DEFAULT);
		if (timeTableColumnWidth <= 0) return TIME_TABLE_COLUMN_WIDTH_DEFAULT;
		return timeTableColumnWidth;
	}

	public int getQueryTableColumnWidth()
	{
		int queryTableColumnWidth = getProperty(QUERY_TABLE_COLUMN_WIDTH_SETTING, QUERY_TABLE_COLUMN_WIDTH_DEFAULT);
		if (queryTableColumnWidth <= 0) return QUERY_TABLE_COLUMN_WIDTH_DEFAULT;
		return queryTableColumnWidth;
	}

	public int getArgumentsTableColumnWidth()
	{
		int argumentsTableColumnWidth = getProperty(ARGUMENTS_TABLE_COLUMN_WIDTH_SETTING, ARGUMENTS_TABLE_COLUMN_WIDTH_DEFAULT);
		if (argumentsTableColumnWidth <= 0) return ARGUMENTS_TABLE_COLUMN_WIDTH_DEFAULT;
		return argumentsTableColumnWidth;
	}

	public int getDatasourceTableColumnWidth()
	{
		int datasourceTableColumnWidth = getProperty(DATASOURCE_TABLE_COLUMN_WIDTH_SETTING, DATASOURCE_TABLE_COLUMN_WIDTH_DEFAULT);
		if (datasourceTableColumnWidth <= 0) return DATASOURCE_TABLE_COLUMN_WIDTH_DEFAULT;
		return datasourceTableColumnWidth;
	}

	public int getTransactionTableColumnWidth()
	{
		int transactionTableColumnWidth = getProperty(TRANSACTION_TABLE_COLUMN_WIDTH_SETTING, TRANSACTION_TABLE_COLUMN_WIDTH_DEFAULT);
		if (transactionTableColumnWidth <= 0) return TRANSACTION_TABLE_COLUMN_WIDTH_DEFAULT;
		return transactionTableColumnWidth;
	}

	public int getTreeWidth()
	{
		int treeWidth = getProperty(TREE_WIDTH_SETTING, TREE_WIDTH_DEFAULT);
		if (treeWidth <= 0) return TREE_WIDTH_DEFAULT;
		return treeWidth;
	}

	public int getTableWidth()
	{
		int tableWidth = getProperty(TABLE_WIDTH_SETTING, TABLE_WIDTH_DEFAULT);
		if (tableWidth <= 0) return TABLE_WIDTH_DEFAULT;
		return tableWidth;
	}

	public void setMethodNameColumnWidth(int methodNameColumnWidth)
	{
		setProperty(METHOD_NAME_COLUMN_WIDTH_SETTING, methodNameColumnWidth);
	}

	public void setOwnTimeColumnWidth(int ownTimeColumnWidth)
	{
		setProperty(OWN_TIME_COLUMN_WIDTH_SETTING, ownTimeColumnWidth);
	}

	public void setTimeColumnWidth(int timeColumnWidth)
	{
		setProperty(TIME_COLUMN_WIDTH_SETTING, timeColumnWidth);
	}

	public void setFileColumnWidth(int fileColumnWidth)
	{
		setProperty(FILE_COLUMN_WIDTH_SETTING, fileColumnWidth);
	}

	public void setArgsColumnWidth(int argsColumnWidth)
	{
		setProperty(ARGS_COLUMN_WIDTH_SETTING, argsColumnWidth);
	}

	public void setNameTableColumnWidth(int nameTableColumnWidth)
	{
		setProperty(NAME_TABLE_COLUMN_WIDTH_SETTING, nameTableColumnWidth);
	}

	public void setTimeTableColumnWidth(int timeTableColumnWidth)
	{
		setProperty(TIME_TABLE_COLUMN_WIDTH_SETTING, timeTableColumnWidth);
	}

	public void setQueryTableColumnWidth(int queryTableColumnWidth)
	{
		setProperty(QUERY_TABLE_COLUMN_WIDTH_SETTING, queryTableColumnWidth);
	}

	public void setArgumentsTableColumnWidth(int argumentsTableColumnWidth)
	{
		setProperty(ARGUMENTS_TABLE_COLUMN_WIDTH_SETTING, argumentsTableColumnWidth);
	}

	public void setDatasourceTableColumnWidth(int datasourceTableColumnWidth)
	{
		setProperty(DATASOURCE_TABLE_COLUMN_WIDTH_SETTING, datasourceTableColumnWidth);
	}

	public void setTransactionTableColumnWidth(int transactionTableColumnWidth)
	{
		setProperty(TRANSACTION_TABLE_COLUMN_WIDTH_SETTING, transactionTableColumnWidth);
	}

	public void setTreeWidth(int treeWidth)
	{
		setProperty(TREE_WIDTH_SETTING, treeWidth);
	}

	public void setTableWidth(int tableWidth)
	{
		setProperty(TABLE_WIDTH_SETTING, tableWidth);
	}
}
