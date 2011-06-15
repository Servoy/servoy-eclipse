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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.CachingContentProvider;
import com.servoy.eclipse.ui.dialogs.ISearchKeyAdapter;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormEncapsulation;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Utils;

/**
 * Content provider for forms with relations.
 * 
 * @author rgansevles
 * 
 */
public class RelatedFormsContentProvider extends CachingContentProvider implements ISearchKeyAdapter, IKeywordChecker
{
	private static class TableComparator implements Comparator<Table>
	{
		public static final TableComparator INSTANCE = new TableComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Table o1, Table o2)
		{
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			String o1Name = o1.getServerName() + "." + o1.getName(); //$NON-NLS-1$
			String o2Name = o2.getServerName() + "." + o2.getName(); //$NON-NLS-1$
			return o1Name.compareToIgnoreCase(o2Name);
		}
	}

	private final Form rootForm;
	private final FlattenedSolution flattenedSolution;

	public RelatedFormsContentProvider(Form rootForm)
	{
		this.rootForm = rootForm;
		this.flattenedSolution = ModelUtils.getEditingFlattenedSolution(rootForm);
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof Form)
		{
			Form form = (Form)inputElement;
			List<Object> nodes = new ArrayList<Object>();
			try
			{
				Iterator<Relation> relations = flattenedSolution.getRelations(form.getTable(), true, true);
				Set<String> relationNames = new HashSet<String>();
				while (relations.hasNext())
				{
					Relation relation = relations.next();
					if (relationNames.add(relation.getName()))
					{
						nodes.add(new RelatedForm(new Relation[] { relation }, null));
					}
				}
				nodes.add(Messages.LabelUnrelated);
				return nodes.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				return new Object[0];
			}
		}

		return super.getElements(inputElement);
	}

	private final Map<String, Map<String, List<Form>>> formsCache = new HashMap<String, Map<String, List<Form>>>(5);

	private List<Form> getFormsOfTable(String serverName, String tableName) throws RemoteException, RepositoryException
	{
		if (formsCache.isEmpty())
		{
			Iterator<Form> forms = flattenedSolution.getForms(true);
			while (forms.hasNext())
			{
				Form form = forms.next();
				if (form == rootForm || FormEncapsulation.isModulePrivate(form, flattenedSolution)) continue; //is rootForm accessible via self ref relation

				IServer formServer = flattenedSolution.getSolution().getServer(form.getServerName());
				if (formServer == null) continue;
				Map<String, List<Form>> map = formsCache.get(formServer.getName());
				if (map == null)
				{
					map = new HashMap<String, List<Form>>(100);
					formsCache.put(formServer.getName(), map);
				}

				List<Form> formsByTable = map.get(form.getTableName());
				if (formsByTable == null)
				{
					formsByTable = new ArrayList<Form>();
					map.put(form.getTableName(), formsByTable);
				}
				formsByTable.add(form);
			}

		}
		Map<String, List<Form>> map = formsCache.get(serverName);
		if (map == null) return Collections.emptyList();
		List<Form> list = map.get(tableName);
		if (list == null) return Collections.emptyList();
		return list;
	}

	private final Map<Table, List<Relation>> relationCache = new HashMap<Table, List<Relation>>();

	private List<Relation> getRelations(Table table) throws RepositoryException
	{
		List<Relation> list = relationCache.get(table);
		if (list != null) return list;

		Iterator<Relation> relations = flattenedSolution.getRelations(table, true, true);
		if (relations.hasNext())
		{
			list = new ArrayList<Relation>();
			Set<String> relationNames = new HashSet<String>();
			while (relations.hasNext())
			{
				Relation relation = relations.next();
				if (!relation.isGlobal() && relationNames.add(relation.getName()))
				{
					list.add(relation);
				}
			}
		}

		if (list == null) list = Collections.emptyList();
		relationCache.put(table, list);
		return list;

	}


	@Override
	public Object[] getChildrenUncached(Object parentElement)
	{
		try
		{
			if (parentElement instanceof RelatedForm && ((RelatedForm)parentElement).form == null && ((RelatedForm)parentElement).relations != null &&
				((RelatedForm)parentElement).relations.length > 0)
			{
				RelatedForm rf = (RelatedForm)parentElement;
				Relation lastRelation = rf.relations[rf.relations.length - 1];
				List<RelatedForm> children = new ArrayList<RelatedForm>();

				// add forms for this relation

				Iterator<Form> forms = getFormsOfTable(lastRelation.getForeignServer().getName(), lastRelation.getForeignTableName()).iterator();
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (!FormEncapsulation.isModulePrivate(form, flattenedSolution)) children.add(new RelatedForm(rf.relations, form));
				}

				// add relations 1 level deeper
				Iterator<Relation> relations = getRelations(lastRelation.getForeignTable()).iterator();
				while (relations.hasNext())
				{
					children.add(new RelatedForm(Utils.arrayAdd(rf.relations, relations.next(), true), null));
				}

				return children.toArray();
			}

			if (Messages.LabelUnrelated == parentElement)
			{
				boolean includeNoTable = false;
				TreeSet<Table> tableSet = new TreeSet<Table>(TableComparator.INSTANCE);
				Iterator<Form> forms = flattenedSolution.getForms(true);
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (form == rootForm || FormEncapsulation.isModulePrivate(form, flattenedSolution)) continue; //is rootForm accessible via self ref relation
					try
					{
						Table table = form.getTable();
						if (table == null)
						{
							includeNoTable = true;
						}
						else
						{
							tableSet.add(table);
						}
					}
					catch (RepositoryException e)
					{
						// cannot get table of one form, log and continue to the next
						ServoyLog.logError(e);
					}
				}
				if (includeNoTable)
				{
					Object[] array = tableSet.toArray(new Object[tableSet.size() + 1]);
					array[array.length - 1] = Messages.LabelNoTable;
					return array;
				}
				return tableSet.toArray();
			}

			if (parentElement instanceof Table || Messages.LabelNoTable == parentElement)
			{
				List<RelatedForm> children = new ArrayList<RelatedForm>();

				Iterator<Form> forms = flattenedSolution.getForms(true);
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (form == rootForm || FormEncapsulation.isModulePrivate(form, flattenedSolution)) continue; //is rootForm accessible via self ref relation
					try
					{
						if (form.getTable() == parentElement || (form.getDataSource() == null && Messages.LabelNoTable == parentElement))
						{
							children.add(new RelatedForm(null, form));
						}
					}
					catch (RepositoryException e)
					{
						// cannot get table of one form, log and continue to the next
						ServoyLog.logError(e);
					}
				}
				return children.toArray();
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		catch (RemoteException e)
		{
			ServoyLog.logError(e);
		}

		return new Object[0];
	}

	public Object getParent(Object element)
	{
		try
		{
			if (element instanceof RelatedForm)
			{
				RelatedForm rf = (RelatedForm)element;
				if (rf.form == null)
				{
					if (rf.relations == null || rf.relations.length == 1)
					{
						return null;
					}
					// intermediate relations node
					return new RelatedForm(Utils.arraySub(rf.relations, 0, rf.relations.length - 1), null);
				}

				if (rf.relations == null)
				{
					// form leaf node
					Table table = rf.form.getTable();
					return table == null ? Messages.LabelNoTable : table;
				}

				// form under relation
				return new RelatedForm(rf.relations, null);
			}
			if (element instanceof Table)
			{
				return Messages.LabelUnrelated;
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	public boolean hasChildren(Object element)
	{
		return !(element instanceof RelatedForm) || ((RelatedForm)element).form == null;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.dialogs.ISearchKeyAdapter#getSearchKey(java.lang.Object)
	 */
	public Object getSearchKey(Object element)
	{
		if (element instanceof RelatedForm)
		{
			// only return a real relation node, not a form node for that relation.
			if (((RelatedForm)element).form == null)
			{
				Relation[] relations = ((RelatedForm)element).relations;
				if (relations != null && relations.length > 0) return relations[relations.length - 1];
			}
		}
		return null;
	}

	public boolean isKeyword(Object element)
	{
		return element == Messages.LabelUnrelated;
	}
}