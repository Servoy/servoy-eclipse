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
package com.servoy.eclipse.core.quickfix.dbi;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.preferences.DbiPreferences;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Quick fix for missing column info in the dbi file (although they are present in the DB). It will create the column info.
 *
 * @author acostescu
 */
public class DBIQuickFixCreateInfoForColumn extends TableDifferenceQuickFix
{

	private static DBIQuickFixCreateInfoForColumn instance;

	private DBIQuickFixCreateInfoForColumn()
	{
	}

	public static DBIQuickFixCreateInfoForColumn getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixCreateInfoForColumn();
		}
		return instance;
	}

	@Override
	public String getShortLabel()
	{
		return "Update .dbi";
	}

	public String getLabel()
	{
		return "Add default information about this column to the database information file.";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && difference.getType() == TableDifference.COLUMN_MISSING_FROM_DBI_FILE;
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
					// as other columns may have error markers in this table and we cannot be certain that
					// the in-memory info matches the .dbi file info for all other table columns, we do not
					// directly serialize the in-memory table contents... we read the file and only modify what is needed
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
						Column c = difference.getTable().getColumn(difference.getColumnName());
						if (c != null)
						{
							TableDef tableInfo = DatabaseUtils.deserializeTableInfo(dbiFileContent, false);
							String dbiSortingKey = new DbiPreferences().getDbiSortingKey();
							// add the column information
							ArrayList<String> colNames = new ArrayList<String>();
							difference.getTable().getColumns().forEach(col -> colNames.add(col.getName()));
							dmm.createNewColumnInfo(c, false);
							ColumnInfoDef cidToBeAdded = DataModelManager.getColumnInfoDef(c,
								(DbiPreferences.DBI_SORT_BY_INDEX.equals(dbiSortingKey) ? colNames.indexOf(difference.getColumnName()) : -1));
							Iterator<Column> it = (DbiPreferences.DBI_SORT_BY_INDEX.equals(dbiSortingKey))
								? difference.getTable().getColumnsSortedByIndex(colNames)
								: difference.getTable().getColumnsSortedByName();
							ArrayList<ColumnInfoDef> columnInfoDefSetCopy = new ArrayList<ColumnInfoDef>(tableInfo.columnInfoDefSet);
							tableInfo.columnInfoDefSet.clear();
							while (it.hasNext())
							{
								Column column = it.next();
								columnInfoDefSetCopy.stream().filter(cid -> cid.name.equals(column.getName())).findFirst()
									.ifPresentOrElse(cid -> tableInfo.columnInfoDefSet.add(cid), () -> {
										if (column.getName().equals(cidToBeAdded.name))
										{
											tableInfo.columnInfoDefSet.add(cidToBeAdded);
										}
									});
							}

							// write back the contents and reload them to make sure markers are in sync
							String contents = dmm.serializeTableInfo(tableInfo);
							InputStream source = new ByteArrayInputStream(contents.getBytes("UTF8")); // encoding issues?
							file.setContents(source, true, false, null); // this will trigger a reload of the column information and markers will be refreshed
						}
						else
						{
							ServoyLog.logWarning("Cannot find column - while trying to apply CreateInfoForColumn QuickFix", null);
						}
					}
				}
				else
				{
					ServoyLog.logError("DBI file does not exist - while trying to apply CreateInfoForColumn QuickFix", null);
				}
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
				storeException(e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
				storeException(e);
			}
			catch (UnsupportedEncodingException e)
			{
				ServoyLog.logError(e);
				storeException(e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				storeException(e);
			}
		}
		else
		{
			ServoyLog.logError("Null dmm while trying to apply CreateInfoForColumn QuickFix", null);
		}
	}
}
