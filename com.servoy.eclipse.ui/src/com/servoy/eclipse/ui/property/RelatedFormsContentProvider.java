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
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.CachingContentProvider;
import com.servoy.eclipse.ui.dialogs.ISearchKeyAdapter;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Utils;

/**
 * Content provider for forms with relations.
 *
 * @author rgansevles
 *
 */
public class RelatedFormsContentProvider extends CachingContentProvider implements ISearchKeyAdapter, IKeywordChecker
{
	private static class TableComparator implements Comparator<ITable>
	{
		public static final TableComparator INSTANCE = new TableComparator();

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(ITable o1, ITable o2)
		{
			if (o1 == null && o2 == null) return 0;
			if (o1 == null || o1.getDataSource() == null) return -1;
			if (o2 == null || o2.getDataSource() == null) return 1;
			return o1.getDataSource().compareToIgnoreCase(o2.getDataSource());
		}
	}

	private final Form rootForm;
	private final FlattenedSolution flattenedSolution;
	private boolean onlyFormComponents = false;

	public RelatedFormsContentProvider(Form rootForm)
	{
		this.rootForm = rootForm;
		this.flattenedSolution = ModelUtils.getEditingFlattenedSolution(rootForm);
	}

	public RelatedFormsContentProvider(Form rootForm, boolean onlyFormComponents)
	{
		this.rootForm = rootForm;
		this.flattenedSolution = ModelUtils.getEditingFlattenedSolution(rootForm);
		this.onlyFormComponents = onlyFormComponents;
	}

	public RelatedFormsContentProvider(Form rootForm, FlattenedSolution fs)
	{
		this.rootForm = rootForm;
		this.flattenedSolution = fs;
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
				Iterator<Relation> relations = flattenedSolution.getRelations(
					ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(form.getDataSource()), true, true);
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

	private final Map<String, List<Form>> formsCache = new HashMap<String, List<Form>>(5);

	private List<Form> getFormsOfTable(String dataSource)
	{
		if (formsCache.isEmpty())
		{
			Iterator<Form> forms = flattenedSolution.getForms(true);
			while (forms.hasNext())
			{
				Form form = forms.next();
				if (form == rootForm || PersistEncapsulation.isModuleScope(form, flattenedSolution.getSolution())) continue; //is rootForm accessible via self ref relation

				if (form.getDataSource() == null) continue;
				List<Form> formsByTable = formsCache.get(form.getDataSource());
				if (formsByTable == null)
				{
					formsByTable = new ArrayList<Form>();
					formsCache.put(form.getDataSource(), formsByTable);
				}
				formsByTable.add(form);
			}

		}
		List<Form> list = formsCache.get(dataSource);
		if (list == null) return Collections.emptyList();
		return list;
	}

	private final Map<ITable, List<Relation>> relationCache = new HashMap<ITable, List<Relation>>();

	private List<Relation> getRelations(ITable table) throws RepositoryException
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

				for (Form form : getFormsOfTable(lastRelation.getForeignDataSource()))
				{
					if (!PersistEncapsulation.isModuleScope(form, flattenedSolution.getSolution()) &&
						((this.onlyFormComponents && form.isFormComponent().booleanValue()) ||
							(!this.onlyFormComponents && !form.isFormComponent().booleanValue())))
						children.add(new RelatedForm(rf.relations, form));
				}

				for (Relation element : getRelations(
					ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(lastRelation.getForeignDataSource())))
				{
					children.add(new RelatedForm(Utils.arrayAdd(rf.relations, element, true), null));
				}

				return children.toArray();
			}

			if (Messages.LabelUnrelated == parentElement)
			{
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				boolean includeNoTable = false;
				TreeSet<ITable> tableSet = new TreeSet<ITable>(TableComparator.INSTANCE);
				Iterator<Form> forms = flattenedSolution.getForms(true);
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (form == rootForm || PersistEncapsulation.isModuleScope(form, flattenedSolution.getSolution()))
						continue; //is rootForm accessible via self ref relation
					if ((this.onlyFormComponents && !form.isFormComponent().booleanValue()) ||
						(!this.onlyFormComponents && form.isFormComponent().booleanValue()))
						continue;
					ITable table = dsm.getDataSource(form.getDataSource());
					if (table == null)
					{
						includeNoTable = true;
					}
					else
					{
						tableSet.add(table);
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

			if (parentElement instanceof ITable || Messages.LabelNoTable == parentElement)
			{
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				List<RelatedForm> children = new ArrayList<RelatedForm>();

				Iterator<Form> forms = flattenedSolution.getForms(true);
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (form == rootForm || PersistEncapsulation.isModuleScope(form, flattenedSolution.getSolution()))
						continue; //is rootForm accessible via self ref relation
					if ((this.onlyFormComponents && !form.isFormComponent().booleanValue()) ||
						(!this.onlyFormComponents && form.isFormComponent().booleanValue()))
						continue;
					if (dsm.getDataSource(form.getDataSource()) == parentElement || (form.getDataSource() == null && Messages.LabelNoTable == parentElement))
					{
						children.add(new RelatedForm(null, form));
					}
				}
				return children.toArray();
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
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				// form leaf node
				ITable table = dsm.getDataSource(rf.form.getDataSource());
				return table == null ? Messages.LabelNoTable : table;
			}

			// form under relation
			return new RelatedForm(rf.relations, null);
		}
		if (element instanceof ITable || Messages.LabelNoTable == element)
		{
			return Messages.LabelUnrelated;
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