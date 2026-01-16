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
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Quick fix for missing columns in DB (although they are present in the dbi files). It will delete the info from the dbi file.
 *
 * @author acostescu
 */
public class DBIQuickFixDeleteInfoForColumn extends TableDifferenceQuickFix
{

	private static DBIQuickFixDeleteInfoForColumn instance;

	private DBIQuickFixDeleteInfoForColumn()
	{
	}

	public static DBIQuickFixDeleteInfoForColumn getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixDeleteInfoForColumn();
		}
		return instance;
	}

	@Override
	public String getShortLabel()
	{
		return "Delete information from .dbi";
	}

	public String getLabel()
	{
		return "Delete information from .dbi file about column that does not exist in DB.";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && difference.getType() == TableDifference.COLUMN_MISSING_FROM_DB;
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
						TableDef tableInfo = DatabaseUtils.deserializeTableInfo(dbiFileContent, false);

						// delete the column information
						boolean removed = false;
						Iterator<ColumnInfoDef> it = tableInfo.columnInfoDefSet.iterator();
						while (it.hasNext() && removed == false)
						{
							ColumnInfoDef cid = it.next();
							if (cid.name.equals(difference.getDbiFileDefinition().name))
							{
								it.remove();
								removed = true;
							}
						}

						if (removed)
						{
							// write back the contents and reload them to make sure markers are in sync
							String contents = dmm.serializeTableInfo(tableInfo);
							InputStream source = new ByteArrayInputStream(contents.getBytes("UTF8")); // encoding issues?
							file.setContents(source, true, false, null); // this will trigger a reload of the column information and markers will be refreshed
						}
						else
						{
							ServoyLog.logWarning("Cannot find column to delete in column info file - while trying to apply DeleteInfoForColumn QuickFix", null);
						}
					}
				}
				else
				{
					ServoyLog.logError("DBI file does not exist - while trying to apply DeleteInfoForColumn QuickFix", null);
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
		}
		else
		{
			ServoyLog.logError("Null dmm while trying to apply DeleteInfoForColumn QuickFix", null);
		}
	}

}
