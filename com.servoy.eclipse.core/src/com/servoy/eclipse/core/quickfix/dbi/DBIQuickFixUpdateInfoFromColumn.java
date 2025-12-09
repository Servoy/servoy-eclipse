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
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.preferences.DbiPreferences;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Quick fix for differences between column info in the dbi file and columns in the DB. It will change the column information to match the DB column.
 *
 * @author acostescu
 */
public class DBIQuickFixUpdateInfoFromColumn extends TableDifferenceQuickFix
{

	private static DBIQuickFixUpdateInfoFromColumn instance;

	private DBIQuickFixUpdateInfoFromColumn()
	{
	}

	public static DBIQuickFixUpdateInfoFromColumn getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixUpdateInfoFromColumn();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Update column information to match the DB column.";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && ((difference.getType() == TableDifference.COLUMN_CONFLICT && (//
		(!difference.getDbiFileDefinition().columnType.equals(difference.getTableDefinition().columnType)) || //
			(difference.getDbiFileDefinition().allowNull != difference.getTableDefinition().allowNull) || //
			((difference.getDbiFileDefinition().flags & IBaseColumn.PK_COLUMN) != (difference.getTableDefinition().flags & IBaseColumn.PK_COLUMN))//
		)) || (difference.getType() == TableDifference.COLUMN_SEQ_TYPE_OVERRIDEN));
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
						TableDef tableInfo = DatabaseUtils.deserializeTableInfo(dbiFileContent, false);

						// prepare to update column creation order to the one of current real table
						ArrayList<String> colNames = new ArrayList<String>();
						Collection<Column> col = difference.getTable().getColumns();
						for (Column column : col)
						{
							colNames.add(column.getName());
						}
						for (int i = tableInfo.columnInfoDefSet.size() - 1; i >= 0; i--)
						{
							ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(i);
							int creationOrderIndex = colNames.indexOf(cid.name);
							if (creationOrderIndex >= 0)
							{
								cid.creationOrderIndex = DbiPreferences.DBI_SORT_BY_INDEX.equals(new DbiPreferences().getDbiSortingKey()) ? creationOrderIndex
									: -1;
							}
							if (cid.name.equals(difference.getColumnName()))
							{
								// if the types are the same or compatible, we will keep the rest of the database information intact;
								// otherwise create default column information for this column
								if (!Column.isColumnInfoCompatible(difference.getTableDefinition().columnType, cid.columnType, false))
								{
									// create defaults
									Column c = difference.getTable().getColumn(difference.getColumnName());
									if (c != null)
									{
										dmm.createNewColumnInfo(c, false);
										cid = DataModelManager.getColumnInfoDef(c, colNames.indexOf(difference.getColumnName()));
										// if table definition has a scale, add it to the compatible list, because the default type won't store scale.
										if (difference.getTableDefinition().columnType.getScale() > 0)
										{
											cid.compatibleColumnTypes = Arrays.asList(difference.getTableDefinition().columnType);
										}
										tableInfo.columnInfoDefSet.set(i, cid);
									}
									else
									{
										ServoyLog.logError("Column does not exist - while trying to apply UpdateInfoFromColumn QuickFix", null);
									}
								}
								else
								{
									// only change what could be conflicting and keep the rest of the column info
									cid.columnType = difference.getTableDefinition().columnType;
									cid.allowNull = difference.getTableDefinition().allowNull;
									// if table definition has a scale, add it to the compatible list if needed
									if (difference.getTableDefinition().columnType.getScale() > 0)
									{
										if (cid.compatibleColumnTypes == null)
										{
											cid.compatibleColumnTypes = Arrays.asList(difference.getTableDefinition().columnType);
										}
										else if (!cid.compatibleColumnTypes.contains(difference.getTableDefinition().columnType))
										{
											cid.compatibleColumnTypes = new ArrayList<ColumnType>(cid.compatibleColumnTypes);
											cid.compatibleColumnTypes.add(difference.getTableDefinition().columnType);
										}
									}
								}
								// if pk info differs... use real one
								cid.flags = (cid.flags & (~IBaseColumn.PK_COLUMN)) | (difference.getTableDefinition().flags & IBaseColumn.PK_COLUMN);
								// make sure that if it is marked as pk, it is not marked as user row id col. too
								if ((cid.flags & IBaseColumn.PK_COLUMN) != 0) cid.flags = cid.flags & (~IBaseColumn.USER_ROWID_COLUMN);

								// if it's an auto increment but the subtype is different in the db, use the one in the db
								if (((cid.flags & IBaseColumn.USER_ROWID_COLUMN) != 0 || (cid.flags & IBaseColumn.PK_COLUMN) != 0) &&
									difference.getTableDefinition().autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER &&
									cid.autoEnterType == difference.getTableDefinition().autoEnterType &&
									cid.autoEnterSubType != difference.getTableDefinition().autoEnterSubType)
								{
									cid.autoEnterSubType = difference.getTableDefinition().autoEnterSubType;
								}
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
			ServoyLog.logError("Null dmm while trying to apply UpdateInfoFromColumn QuickFix", null);
		}
	}
}
