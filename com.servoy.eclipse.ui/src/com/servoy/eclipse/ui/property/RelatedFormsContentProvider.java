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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.CachingContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;

/**
 * Content provider for forms with relations.
 * 
 * @author rob
 * 
 */
public class RelatedFormsContentProvider extends CachingContentProvider implements IMaxDepthTreeContentProvider, IKeywordChecker
{
	private final Form rootForm;
	private final FlattenedSolution flattenedSolution;

	public RelatedFormsContentProvider(Form rootForm)
	{
		this.rootForm = rootForm;
		this.flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(rootForm);
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
				String ft = lastRelation.getForeignTableName();
				Iterator<Form> forms = flattenedSolution.getForms(true);
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (form == rootForm) continue; // is rootForm accessible via self
					// ref relation
					// compare via server objects, form or relation may refer to name of a duplicate server
					IServer foreignServer = lastRelation.getForeignServer();
					IServer formServer = flattenedSolution.getSolution().getServer(form.getServerName());
					if (formServer != null && foreignServer != null && ft.equals(form.getTableName()) && foreignServer.getName().equals(formServer.getName()))
					{
						children.add(new RelatedForm(rf.relations, form));
					}
				}

				// add relations 1 level deeper
				Iterator<Relation> relations = flattenedSolution.getRelations(lastRelation.getForeignTable(), true, true);
				Set<String> relationNames = new HashSet<String>();
				while (relations.hasNext())
				{
					Relation relation = relations.next();
					if (!relation.isGlobal() && relationNames.add(relation.getName()))
					{
						children.add(new RelatedForm(Utils.arrayAdd(rf.relations, relation, true), null));
					}
				}

				return children.toArray();
			}

			if (Messages.LabelUnrelated == parentElement)
			{
				boolean includeNoTable = false;
				TreeSet<Table> tableSet = new TreeSet<Table>(StringComparator.INSTANCE);
				Iterator<Form> forms = flattenedSolution.getForms(true);
				while (forms.hasNext())
				{
					Form form = forms.next();
					if (form == rootForm) continue; //is rootForm accessible via self ref relation
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
					if (form == rootForm) continue; //is rootForm accessible via self ref relation
					if (form.getTable() == parentElement || (form.getDataSource() == null && Messages.LabelNoTable == parentElement))
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

	public boolean searchLimitReached(Object element, int depth)
	{
		return element instanceof RelatedForm && RelationContentProvider.exceedsRelationsDepth(((RelatedForm)element).relations, depth);
	}

	public boolean isKeyword(Object element)
	{
		return element == Messages.LabelUnrelated;
	}
}