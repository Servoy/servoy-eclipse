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
package com.servoy.eclipse.ui.property;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;

/**
 * Convert between {@link IDataProvider} and String representation
 * 
 * @author rgansevles
 * 
 */
public class DataProviderConverter implements IPropertyConverter<String, IDataProvider>
{
	private final Table table;
	private final FlattenedSolution flattenedSolution;
	private final IPersist persist;

	public DataProviderConverter(FlattenedSolution flattenedSolution, IPersist persist, Table table)
	{
		this.flattenedSolution = flattenedSolution;
		this.persist = persist;
		this.table = table;
	}

	public IDataProvider convertProperty(Object id, String value)
	{
		try
		{
			if (value == null)
			{
				return DataProviderContentProvider.NONE;
			}
			return getDataProvider(flattenedSolution, persist, table, value);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	public static IDataProvider getDataProvider(FlattenedSolution flattenedSolution, IPersist persist, Table table, String value) throws RepositoryException
	{
		IDataProvider dataProvider = null;
		if (table != null)
		{
			dataProvider = flattenedSolution.getDataProviderForTable(table, value);
		}
		if (dataProvider == null)
		{
			Form flattenedForm = flattenedSolution.getFlattenedForm(persist);
			if (flattenedForm != null)
			{
				dataProvider = flattenedForm.getScriptVariable(value);
			}
		}
		if (dataProvider == null)
		{
			return flattenedSolution.getGlobalDataProvider(value);
		}
		return dataProvider;
	}

	public String convertValue(Object id, IDataProvider value)
	{
		if (value == null || value == DataProviderContentProvider.NONE)
		{
			return null;
		}
		return value.getDataProviderID();
	}

}
