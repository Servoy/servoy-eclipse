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
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
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
						TableDef tableInfo = dmm.deserializeTableInfo(dbiFileContent);

						// add the column information
						ArrayList<String> colNames = new ArrayList<String>();
						Collection<Column> col = difference.getTable().getColumns();
						for (Column column : col)
						{
							colNames.add(column.getName());
						}
						Column c = difference.getTable().getColumn(difference.getColumnName());
						if (c != null)
						{
							dmm.createNewColumnInfo(c, false);
							ColumnInfoDef cidToBeAdded = DataModelManager.getColumnInfoDef(c, colNames.indexOf(difference.getColumnName()));
							int insertIndex = -1;
							for (int i = tableInfo.columnInfoDefSet.size() - 1; i >= 0; i--)
							{
								ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(i);
								int creationOrderIndex = colNames.indexOf(cid.name);
								if (creationOrderIndex >= 0)
								{
									cid.creationOrderIndex = creationOrderIndex;
								}
								if (insertIndex == -1 && cid.name.compareToIgnoreCase(cidToBeAdded.name) <= 0)
								{
									insertIndex = i + 1;
								}
							}
							if (insertIndex == -1)
							{
								tableInfo.columnInfoDefSet.add(0, cidToBeAdded);
							}
							else
							{
								tableInfo.columnInfoDefSet.add(insertIndex, cidToBeAdded);
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
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			catch (UnsupportedEncodingException e)
			{
				ServoyLog.logError(e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		else
		{
			ServoyLog.logError("Null dmm while trying to apply CreateInfoForColumn QuickFix", null);
		}
	}
}
