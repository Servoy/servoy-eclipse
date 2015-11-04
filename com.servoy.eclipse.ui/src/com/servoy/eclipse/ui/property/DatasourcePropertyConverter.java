/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.property;

import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.util.DataSourceUtils;

public class DatasourcePropertyConverter implements IPropertyConverter<String, TableWrapper>
{
	public String convertValue(Object id, TableWrapper tw)
	{
		return tw == null ? null : DataSourceUtils.createDBTableDataSource(tw.getServerName(), tw.getTableName());
	}

	public TableWrapper convertProperty(Object id, String dataSource)
	{
		String[] servernameTablename = DataSourceUtils.getDBServernameTablename(dataSource);
		if (servernameTablename == null)
		{
			return null;
		}
		return new TableWrapper(servernameTablename[0], servernameTablename[1], EditorUtil.isViewTypeTable(servernameTablename[0],
			servernameTablename[1]));
	}
}