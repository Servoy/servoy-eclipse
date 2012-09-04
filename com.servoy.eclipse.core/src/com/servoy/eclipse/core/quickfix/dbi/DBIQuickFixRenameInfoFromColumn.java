/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.core.quickfix.dbi;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * @author hhardut
 *
 */
public class DBIQuickFixRenameInfoFromColumn extends TableDifferenceQuickFix
{
	private static DBIQuickFixRenameInfoFromColumn instance;

	private DBIQuickFixRenameInfoFromColumn()
	{
	}

	public static DBIQuickFixRenameInfoFromColumn getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixRenameInfoFromColumn();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Rename column from database information file to match the DB table column name.";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		if (difference == null) return false;
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm != null)
		{
			try
			{
				IFile file = dmm.getDBIFile(difference.getServerName(), difference.getTableName());
				if (file.exists())
				{
					InputStream is = file.getContents(true);
					String dbiFileContent = null;
					try
					{
						dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
					}
					finally
					{
						Utils.closeInputStream(is);
					}
					if (dbiFileContent != null)
					{
						TableDef tableInfo = dmm.deserializeTableInfo(dbiFileContent);

						ArrayList<String> colNames = new ArrayList<String>();
						Collection<Column> col = difference.getTable().getColumns();
						for (Column column : col)
						{
							colNames.add(column.getName());
						}
						for (int i = tableInfo.columnInfoDefSet.size() - 1; i >= 0; i--)
						{
							ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(i);
							String dbiColumnName = Ident.RESERVED_NAME_PREFIX + cid.name;
							if (dbiColumnName.equals(difference.getColumnName()))
							{
								return difference.getType() == TableDifference.COLUMN_MISSING_FROM_DB ||
									difference.getType() == TableDifference.COLUMN_MISSING_FROM_DBI_FILE;
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}

		return false;
	}

	@Override
	public void run(TableDifference difference)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm != null)
		{
			try
			{
				IFile file = dmm.getDBIFile(difference.getServerName(), difference.getTableName());
				if (file.exists())
				{
					InputStream is = file.getContents(true);
					String dbiFileContent = null;
					try
					{
						dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
					}
					finally
					{
						Utils.closeInputStream(is);
					}
					if (dbiFileContent != null)
					{
						TableDef tableInfo = dmm.deserializeTableInfo(dbiFileContent);

						ArrayList<String> colNames = new ArrayList<String>();
						Collection<Column> col = difference.getTable().getColumns();
						for (Column column : col)
						{
							colNames.add(column.getName());
						}
						for (int i = tableInfo.columnInfoDefSet.size() - 1; i >= 0; i--)
						{
							ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(i);
							String dbiColumnName = Ident.RESERVED_NAME_PREFIX + cid.name;
							if (dbiColumnName.equals(difference.getColumnName()))
							{
								cid.name = dbiColumnName;
								tableInfo.columnInfoDefSet.set(i, cid);
							}
						}

						// write back the contents and reload them to make sure markers are in sync
						String contents = dmm.serializeTableInfo(tableInfo);
						InputStream source = new ByteArrayInputStream(contents.getBytes("UTF8")); // encoding issues?
						file.setContents(source, true, false, null); // this will trigger a reload of the column information and markers will be refreshed
					}
				}
				else
				{
					ServoyLog.logError("DBI file does not exist - while trying to apply UpdateInfoFromColumn QuickFix", null);
				}
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			catch (UnsupportedEncodingException e)
			{
				ServoyLog.logError(e);
			}
		}
		else
		{
			ServoyLog.logError("Null dmm while trying to apply UpdateInfoFromColumn QuickFix", null);
		}
	}
}
