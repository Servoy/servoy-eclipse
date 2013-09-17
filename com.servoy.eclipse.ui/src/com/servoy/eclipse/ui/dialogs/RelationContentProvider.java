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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Utils;

/**
 * Content provider class for relations.
 * 
 * @author rgansevles
 * 
 */

public class RelationContentProvider extends CachingContentProvider implements ITreeContentProvider, ISearchKeyAdapter
{
	public static final Object NONE = new Object();

	private final FlattenedSolution flattenedSolution;
	private RelationListOptions options;
	private final IPersist context;
	private final Map<Table, List<Relation>> relationCache = new HashMap<Table, List<Relation>>();

	public RelationContentProvider(FlattenedSolution flattenedSolution, IPersist context)
	{
		this.flattenedSolution = flattenedSolution;
		this.context = context;
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof RelationListOptions)
		{
			try
			{
				options = (RelationListOptions)inputElement;

				relationCache.clear();

				List<Object> elements = new ArrayList<Object>();
				if (options.includeNone)
				{
					elements.add(NONE);
				}

				for (Relation relation : getRelations(options.primaryTable, options.foreignTable, options.includeNested, false))
				{
					elements.add(new RelationsWrapper(new Relation[] { relation }));
				}

				return elements.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				return new Object[0];
			}
		}

		return null;
	}

	/**
	 * Get all relations that are the first part of a possible path from primaryTable to foreignTable.
	 * 
	 * @param primaryTable
	 * @param foreignTable
	 * @return
	 * @throws RepositoryException
	 */
	protected List<Relation> getRelations(Table primaryTable, Table foreignTable, boolean recursive, boolean excludeGlobalRelations) throws RepositoryException
	{
		List<Relation> relations = new ArrayList<Relation>();
		Iterator<Relation> primaryrelations;
		if (primaryTable == null)
		{
			primaryrelations = flattenedSolution.getRelations(true);
		}
		else
		{
			primaryrelations = flattenedSolution.getRelations(primaryTable, true, true);
		}
		Set<String> relationNames = new HashSet<String>();
		while (primaryrelations.hasNext())
		{
			Relation relation = primaryrelations.next();
			if (context != null && PersistEncapsulation.isModulePrivate(relation, (Solution)context.getRootObject()))
			{
				continue;
			}
			if (!relationNames.contains(relation.getName()) //
				&&
				(!excludeGlobalRelations || !relation.isGlobal()) //
				&& //
				(foreignTable == null || //
					foreignTable.equals(relation.getForeignTable()) || //
				(recursive && canReachTable(relation, relation.getForeignTable()))))
			{
				relations.add(relation);
				relationNames.add(relation.getName());
			}
		}
		return relations;
	}

	protected boolean canReachTable(Relation relation, Table table) throws RepositoryException
	{
		Set<Relation> visited = new HashSet<Relation>();
		List<Relation> searchrelations = new ArrayList<Relation>();
		searchrelations.add(relation);
		while (searchrelations.size() > 0)
		{
			Relation r = searchrelations.remove(0);
			if (visited.add(r))
			{
				Table foreignTable = r.getForeignTable();
				if (foreignTable != null)
				{
					if (foreignTable.equals(table))
					{
						return true;
					}
					Iterator<Relation> it = flattenedSolution.getRelations(foreignTable, true, false);
					while (it.hasNext())
					{
						searchrelations.add(it.next());
					}
				}
			}
		}
		return false;
	}

	@Override
	public Object[] getChildrenUncached(Object parentElement)
	{
		try
		{
			if (parentElement instanceof RelationsWrapper)
			{
				if (!options.includeNested)
				{
					return new Object[0];
				}
				RelationsWrapper wrapper = (RelationsWrapper)parentElement;
				List<Relation> relations = relationCache.get(wrapper.relations[wrapper.relations.length - 1].getForeignTable());
				if (relations == null)
				{
					relations = getRelations(wrapper.relations[wrapper.relations.length - 1].getForeignTable(), options.foreignTable, true, true);
					relationCache.put(wrapper.relations[wrapper.relations.length - 1].getForeignTable(), relations);
				}
				Object[] children = new Object[relations.size()];

				for (int i = 0; i < relations.size(); i++)
				{
					children[i] = new RelationsWrapper(Utils.arrayAdd(wrapper.relations, relations.get(i), true));
				}
				return children;
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return new Object[0];
	}

	public Object getParent(Object element)
	{
		if (element instanceof RelationsWrapper && ((RelationsWrapper)element).relations.length > 1)
		{
			return new RelationsWrapper(Utils.arraySub(((RelationsWrapper)element).relations, 0, ((RelationsWrapper)element).relations.length - 1));
		}
		return null;
	}

	public boolean hasChildren(Object element)
	{
		return options.includeNested && element instanceof RelationsWrapper;
	}

	public Object getSearchKey(Object element)
	{
		if (element instanceof RelationsWrapper)
		{
			Relation[] relations = ((RelationsWrapper)element).relations;
			if (relations != null && relations.length > 0) return relations[relations.length - 1];
		}
		return null;
	}

	public static class RelationsWrapper
	{
		public final Relation[] relations;

		public RelationsWrapper(Relation[] relations)
		{
			this.relations = relations;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof RelationsWrapper && Arrays.equals(relations, ((RelationsWrapper)obj).relations);
		}
	}

	public static class RelationListOptions
	{
		public final boolean includeNone;
		public final Table foreignTable;
		public final Table primaryTable;
		public final boolean includeNested;

		public RelationListOptions(Table primaryTable, Table foreignTable, boolean includeNone, boolean includeNested)
		{
			this.primaryTable = primaryTable;
			this.foreignTable = foreignTable;
			this.includeNone = includeNone;
			this.includeNested = includeNested;
		}
	}
}
