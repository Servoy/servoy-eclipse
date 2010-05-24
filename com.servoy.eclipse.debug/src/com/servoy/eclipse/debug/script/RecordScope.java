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
package com.servoy.eclipse.debug.script;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.debug.Activator;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 * 
 */
public class RecordScope extends ScriptObjectClassScope
{
	private final static URL COLUMN_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/column.gif"), null); //$NON-NLS-1$
	private final static URL COLUMN_AGGR_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/columnaggr.gif"), null); //$NON-NLS-1$
	private final static URL COLUMN_CALC_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/columncalc.gif"), null); //$NON-NLS-1$

	private final Table table; // may be null

	public RecordScope()
	{
		super(null, Record.class, "record"); //$NON-NLS-1$
		table = null;
	}

	/**
	 * @param parent
	 * @param doc
	 * @param parameterNames
	 */
	public RecordScope(FoundSetScope parent, String[] params, String doc)
	{
		super(parent, Record.class, "record", params, doc, true); //$NON-NLS-1$
		table = parent.getTable();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return RecordScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		List<Object> al = new ArrayList<Object>();
		// data providers
		try
		{
			if (table != null)
			{
				FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
				Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(table);

				if (allDataProvidersForTable != null) al.addAll(allDataProvidersForTable.keySet());

				// relations
				Iterator<Relation> relations = fs.getRelations(table, true, false); // returns global relations when table is null
				while (relations.hasNext())
				{
					al.add(relations.next().getName());
				}
			}
			al.add("foundset"); //$NON-NLS-1$
			al.add("exception"); //$NON-NLS-1$
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("RecordScope getId()", e); //$NON-NLS-1$
		}
		al.addAll(Arrays.asList(super.getIds()));
		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
		Object o = testForDataProvider(this, fs, table, name);
		if (o != null) return o;
		// relations
		Relation relation = fs.getRelation(name);
		if (relation != null && relation.isValid())
		{
			return new FoundSetScope(this, RelatedFoundSet.class, relation);
		}
		if ("exception".equals(name)) //$NON-NLS-1$
		{
			return new ScriptObjectClassScope(this, DataException.class, "exception"); //$NON-NLS-1$
		}
		else if ("foundset".equals(name)) //$NON-NLS-1$
		{
			return new FoundSetScope(this, FoundSet.class, table);
		}
		return super.get(name, start);
	}

	/**
	 * @param name
	 */
	public static Object testForDataProvider(Scriptable scope, FlattenedSolution fs, Table table, String name)
	{
		try
		{
			if (table == null)
			{
				return null;
			}
			// data providers
			Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(table);
			if (allDataProvidersForTable != null && allDataProvidersForTable.get(name) != null)
			{
				IDataProvider dataProvider = allDataProvidersForTable.get(name);

				Object o = null;
				if ((dataProvider.getFlags() & Column.UUID_COLUMN) != 0)
				{
					// column is marked as UUID, this means it will return UUID type
					o = new ScriptObjectClassScope(scope, UUID.class, name);
				}
				else switch (Column.mapToDefaultType(dataProvider.getDataProviderType()))
				{
					case IColumnTypes.DATETIME :
						o = new Date();
						break;

					case IColumnTypes.TEXT :
						o = ""; //$NON-NLS-1$
						break;

					case IColumnTypes.NUMBER :
						o = new Double(0);
						break;

					case IColumnTypes.INTEGER :
						o = new Integer(0);
						break;

					case IColumnTypes.MEDIA :
						o = new byte[0];
						break;

					default :
						o = null;
				}
				URL url = COLUMN_IMAGE;
				String variableType = "Column"; //$NON-NLS-1$
				IFile sourceFile = null;
				if (dataProvider instanceof AggregateVariable)
				{
					url = COLUMN_AGGR_IMAGE;
					variableType = "Aggregate (" + ((AggregateVariable)dataProvider).getRootObject().getName() + ")"; //$NON-NLS-1$//$NON-NLS-2$
				}
				else if (dataProvider instanceof ScriptCalculation)
				{
					url = COLUMN_CALC_IMAGE;
					variableType = "Calculation (" + ((ScriptCalculation)dataProvider).getRootObject().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					String filename = SolutionSerializer.getScriptPath((ScriptCalculation)dataProvider, false);
					if (filename != null)
					{
						sourceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(filename));
					}
				}
				return new ProposalHolder(o, null, "Datatype: " + Column.getDisplayTypeString(dataProvider.getDataProviderType()) + "<br/>Variabletype: " + //$NON-NLS-1$ //$NON-NLS-2$
					variableType, false, url, sourceFile);
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof RecordScope)
		{
			return (table == null && ((RecordScope)obj).table == null) || (table != null && table.equals(((RecordScope)obj).table));
		}
		return false;
	}
}
