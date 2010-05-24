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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.debug.Activator;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;

/**
 * @author jcompagner
 * 
 */
public class FoundSetScope extends ScriptObjectClassScope
{
	private final static URL FOUNDSET_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/foundset.gif"), null); //$NON-NLS-1$
	private final static URL RELATION_IMAGE = FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/relation.gif"), null); //$NON-NLS-1$


	private Table table; // may be null

	private Relation relation;
	private boolean functionRef = false;

	/**
	 * @param parent
	 */
	public FoundSetScope(Scriptable parent, Class< ? > scriptObjectClass, ITable table)
	{
		super(parent, scriptObjectClass, "foundset"); //$NON-NLS-1$
		this.table = (Table)table;
	}

	public FoundSetScope(Scriptable parent, Class< ? > scriptObjectClass, Relation relation)
	{
		super(parent, scriptObjectClass, "foundset"); //$NON-NLS-1$
		this.relation = relation;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return FoundSetScope.class.getName();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		ArrayList<Object> al = new ArrayList<Object>();
		al.add("alldataproviders"); //$NON-NLS-1$

		// these should not be used for scripting, but only for tags
		//al.add("recordIndex"); //$NON-NLS-1$
		//al.add("selectedIndex"); //$NON-NLS-1$
		//al.add("maxRecordIndex"); //$NON-NLS-1$

		al.addAll(Arrays.asList(super.getIds()));

		// data providers
		Table tbl = getTable();
		if (tbl != null)
		{
			try
			{
				FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
				Map<String, IDataProvider> allDataProvidersForTable = fs.getAllDataProvidersForTable(tbl);
				if (allDataProvidersForTable != null) al.addAll(allDataProvidersForTable.keySet());

				// relations
				Iterator<Relation> relations = fs.getRelations(tbl, true, false);
				while (relations.hasNext())
				{
					al.add(relations.next().getName());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}


		return al.toArray();
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (name.equals("alldataproviders")) //$NON-NLS-1$
		{
			return new NativeArray();
		}
		if (name.equals("recordIndex")) //$NON-NLS-1$
		{
			return name;
		}
		if (name.equals("selectedIndex")) //$NON-NLS-1$
		{
			return name;
		}
		if (name.equals("maxRecordIndex")) //$NON-NLS-1$
		{
			return name;
		}
//		// special case for get records code completion.
		if ((name.equals("getRecord") || name.equals("getSelectedRecord"))) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return new RecordScope(this, FormDomProvider.getParameterNames(name, getScriptObjectClass()), FormDomProvider.getDoc(name, getScriptObjectClass(),
				getName()));
		}
		if (name.equals("duplicateFoundSet")) //$NON-NLS-1$
		{
			FoundSetScope dupFs = null;
			if (relation != null)
			{
				dupFs = new FoundSetScope(getParentScope(), getScriptObjectClass(), relation);
			}
			else
			{
				dupFs = new FoundSetScope(getParentScope(), getScriptObjectClass(), getTable());
			}
			dupFs.functionRef = true;
			return dupFs;
		}

		Table tbl = getTable();
		if (tbl != null)
		{
			FlattenedSolution fs = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
			Object o = RecordScope.testForDataProvider(this, fs, tbl, name);
			if (o != null) return o;
			Relation r = fs.getRelation(name);
			if (r != null && r.isValid())
			{
				return new FoundSetScope(this, RelatedFoundSet.class, r);
			}
		}
		return super.get(name, start);
	}

	/**
	 * @return
	 * 
	 */
	public Table getTable()
	{
		if (table == null && relation != null && relation.isValid())
		{
			try
			{
				table = relation.getForeignTable();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return table;
	}


	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#getProposalInfo()
	 */
	@Override
	public String getProposalInfo()
	{
		if (relation != null)
		{
			try
			{
				FlattenedSolution editingFlattenedSolution = FormDomProvider.CURRENT_PROJECT.get().getEditingFlattenedSolution();
				IDataProvider[] primaryDataProviders = relation.getPrimaryDataProviders(editingFlattenedSolution);
				Column[] foreignColumns = relation.getForeignColumns();
				StringBuilder sb = new StringBuilder(150);
				if (relation.isGlobal())
				{
					sb.append("Global relation defined in solution: "); //$NON-NLS-1$
				}
				else if (primaryDataProviders.length == 0)
				{
					sb.append("Self referencing relation defined in solution:"); //$NON-NLS-1$
				}
				else
				{
					sb.append("Relation defined in solution: "); //$NON-NLS-1$
				}
				sb.append(relation.getRootObject().getName());
				if (relation.isGlobal() || primaryDataProviders.length == 0)
				{
					sb.append("<br/>On table: "); //$NON-NLS-1$
					sb.append(relation.getForeignServerName() + "->" + relation.getForeignTableName()); //$NON-NLS-1$
				}
				else
				{
					sb.append("<br/>From: "); //$NON-NLS-1$
//					sb.append(relation.getPrimaryDataSource());
					sb.append(relation.getPrimaryServerName() + "->" + relation.getPrimaryTableName()); //$NON-NLS-1$
					sb.append("<br/>To: "); //$NON-NLS-1$
					sb.append(relation.getForeignServerName() + "->" + relation.getForeignTableName()); //$NON-NLS-1$
				}
				sb.append("<br/>"); //$NON-NLS-1$
				if (primaryDataProviders.length != 0)
				{
					for (int i = 0; i < foreignColumns.length; i++)
					{
						sb.append("&nbsp;&nbsp;"); //$NON-NLS-1$
						sb.append(primaryDataProviders[i].getDataProviderID());
						sb.append("->"); //$NON-NLS-1$
						sb.append(foreignColumns[i].getDataProviderID());
						sb.append("<br/>"); //$NON-NLS-1$
					}
				}
				return sb.toString();
			}
			catch (Exception e)
			{
				return "Relation defined in solution: " + relation.getRootObject().getName() + "<br/>Primairy table: " + relation.getPrimaryServerName() + //$NON-NLS-1$ //$NON-NLS-2$
					"->" + relation.getPrimaryTableName() + "<br/>Foreign table: " + relation.getForeignServerName() + "->" + relation.getForeignTableName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		Table tbl = getTable();
		if (tbl == null)
		{
			return "No table for foundset"; //$NON-NLS-1$
		}
		return "Foundset from table: " + tbl.getName() + " of server: " + tbl.getServerName(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see com.servoy.eclipse.debug.script.ScriptObjectClassScope#getImageURL()
	 */
	@Override
	public URL getImageURL()
	{
		if (isFunctionRef())
		{
			return METHOD;
		}
		if (getScriptObjectClass() == RelatedFoundSet.class)
		{
			return RELATION_IMAGE;
		}
		return FOUNDSET_IMAGE;
	}

	/**
	 * @see org.eclipse.dlkt.javascript.dom.support.IProposalHolder#isFunctionRef()
	 */
	@Override
	public boolean isFunctionRef()
	{
		return functionRef;
	}

	/**
	 * @see com.servoy.eclipse.debug.script.ScriptObjectClassScope#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof FoundSetScope && super.equals(obj))
		{
			Table tbl = getTable();
			return (tbl == null && ((FoundSetScope)obj).getTable() == null) || (tbl != null && tbl.equals(((FoundSetScope)obj).getTable()));
		}
		return false;
	}

	/**
	 * @see com.servoy.eclipse.debug.script.ScriptObjectClassScope#getSourceFile()
	 */
	@Override
	public final IFile getSourceFile()
	{
		if (relation != null)
		{
			String path = SolutionSerializer.getRelativePath(relation, false);
			String persistBaseFilename = SolutionSerializer.getFileName(relation, false);
			return ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(path + persistBaseFilename));
		}
		return null;
	}
}
