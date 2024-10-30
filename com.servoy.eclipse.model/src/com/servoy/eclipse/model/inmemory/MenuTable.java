/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.model.inmemory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.DatePropertyType;
import org.sablo.specification.property.types.IntPropertyType;

import com.servoy.j2db.dataprocessing.IndexInfo;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnComparator;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.SortedList;

/**
 * @author lvostinar
 *
 */
public class MenuTable implements ITable
{
	private final String menuName;
	protected final Map<String, Column> columns = new HashMap<String, Column>();

	public MenuTable(String menuName)
	{
		// should we share the columns for all tables?
		this.menuName = menuName;
		columns.put("itemID", new Column(this, "itemID", IColumnTypes.TEXT, 50, 0, true));
		columns.put("menuText", new Column(this, "menuText", IColumnTypes.TEXT, 50, 0, true));
		columns.put("styleClass", new Column(this, "styleClass", IColumnTypes.TEXT, 50, 0, true));
		columns.put("iconStyleClass", new Column(this, "iconStyleClass", IColumnTypes.TEXT, 50, 0, true));
		columns.put("tooltipText", new Column(this, "tooltipText", IColumnTypes.TEXT, 50, 0, true));
		columns.put("enabled", new Column(this, "enabled", IColumnTypes.INTEGER, 10, 0, true));
		columns.put("callbackArguments", new Column(this, "callbackArguments", IColumnTypes.MEDIA, 100, 0, true));
		for (Map<String, PropertyDescription> propertiesMap : MenuPropertyType.INSTANCE.getExtraProperties().values())
		{
			for (PropertyDescription propertyDescription : propertiesMap.values())
			{
				int type = IColumnTypes.TEXT;
				if (propertyDescription.getType() instanceof BooleanPropertyType || propertyDescription.getType() instanceof IntPropertyType)
				{
					type = IColumnTypes.INTEGER;
				}
				if (propertyDescription.getType() instanceof DatePropertyType)
				{
					type = IColumnTypes.DATETIME;
				}
				columns.put(propertyDescription.getName(), new Column(this, propertyDescription.getName(), type, 100, 0, true));
			}
		}

	}

	@Override
	public String getName()
	{
		return menuName;
	}

	@Override
	public String getCatalog()
	{
		return null;
	}

	@Override
	public String getSchema()
	{
		return null;
	}

	@Override
	public int getTableType()
	{
		return 0;
	}

	@Override
	public int getColumnType(String name)
	{
		return 0;
	}

	@Override
	public String getSQLName()
	{
		return menuName;
	}

	@Override
	public String getDataSource()
	{
		return DataSourceUtils.createMenuDataSource(menuName);
	}

	@Override
	public String getServerName()
	{
		return null;
	}

	@Override
	public String[] getColumnNames()
	{
		return columns.keySet().toArray(new String[0]);
	}

	@Override
	public String[] getDataProviderIDs()
	{
		return getColumnNames();
	}

	@Override
	public Iterator<String> getRowIdentColumnNames()
	{
		return null;
	}

	@Override
	public boolean getExistInDB()
	{
		return true;
	}

	@Override
	public void setExistInDB(boolean b)
	{

	}

	@Override
	public void acquireWriteLock()
	{

	}

	@Override
	public void removeIColumnListener(IItemChangeListener<IColumn> columnListener)
	{

	}

	@Override
	public Column getColumn(String columnName)
	{
		return columns.get(columnName);
	}

	@Override
	public void removeColumn(Column column)
	{

	}

	@Override
	public boolean isMarkedAsHiddenInDeveloper()
	{
		return false;
	}

	@Override
	public boolean isMarkedAsMetaData()
	{
		return false;
	}

	@Override
	public Collection<Column> getColumns()
	{
		return columns.values();
	}

	@Override
	public void addIColumnListener(IItemChangeListener<IColumn> columnListener)
	{

	}

	@Override
	public int getRowIdentColumnsCount()
	{
		return 0;
	}

	@Override
	public void setTableInvalidInDeveloperBecauseNoPk(boolean b)
	{

	}

	@Override
	public boolean isTableInvalidInDeveloperBecauseNoPk()
	{
		return false;
	}

	@Override
	public int getColumnCount()
	{
		return columns.size();
	}

	@Override
	public List<Column> getRowIdentColumns()
	{
		return null;
	}

	@Override
	public List<Column> getTenantColumns()
	{
		return null;
	}

	@Override
	public Iterator<Column> getColumnsSortedByName()
	{
		SortedList<Column> newList = new SortedList<Column>(ColumnComparator.getColumnsNameComparator(), getColumns());
		return newList.iterator();
	}

	@Override
	public void setMarkedAsMetaData(boolean selection)
	{

	}

	@Override
	public void setMarkedAsHiddenInDeveloperInternal(boolean selection)
	{

	}

	@Override
	public void releaseWriteLock()
	{

	}

	@Override
	public void updateDataproviderIDsIfNeeded()
	{

	}

	@Override
	public int getPKColumnTypeRowIdentCount()
	{
		return 0;
	}

	@Override
	public void fireIColumnsChanged(Collection<IColumn> changedColumns)
	{

	}

	@Override
	public void acquireReadLock()
	{

	}

	@Override
	public void releaseReadLock()
	{

	}

	@Override
	public void removeColumn(String columnName)
	{

	}

	@Override
	public Column createNewColumn(IValidateName validator, String name, int sqlType, int length, int scale) throws RepositoryException
	{
		return null;
	}

	@Override
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull, boolean pkColumn)
		throws RepositoryException
	{
		return null;
	}

	@Override
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull) throws RepositoryException
	{
		return null;
	}

	@Override
	public Column createNewColumn(IValidateName nameValidator, String colname, int type, int length) throws RepositoryException
	{
		return null;
	}

	@Override
	public void columnDataProviderIDChanged(String oldDataProviderID)
	{

	}

	@Override
	public void fireIColumnChanged(IColumn column)
	{

	}

	@Override
	public void columnNameChange(IValidateName validator, String oldSQLName, String newName) throws RepositoryException
	{

	}

	@Override
	public void addRowIdentColumn(Column column)
	{

	}

	@Override
	public void removeRowIdentColumn(Column column)
	{

	}

	@Override
	public void setInitialized(boolean initialized)
	{

	}

	@Override
	public boolean isInitialized()
	{
		return true;
	}

	@Override
	public IColumn getColumnBySqlname(String columnSqlname)
	{
		return columns.get(columnSqlname);
	}

	@Override
	public Iterator<Column> getColumnsSortedByIndex(List<String> indexedNames)
	{
		return null;
	}

	@Override
	public List<IndexInfo> getIndexes()
	{
		return null;
	}

}
