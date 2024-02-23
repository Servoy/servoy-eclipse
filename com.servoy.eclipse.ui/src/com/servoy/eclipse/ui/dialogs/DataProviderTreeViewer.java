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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.labelproviders.DelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.util.ScopeWithContext;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationList;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * JFace-like viewer for selecting a data provider from a tree. A filter is built-in.
 *
 * @author rgansevles
 */

public class DataProviderTreeViewer extends FilteredTreeViewer
{
	public static final String CALCULATIONS = "calculations";
	public static final String FORM_VARIABLES = "form variables";
	public static final String SCOPE_VARIABLES = "scope variables";
	public static final String SCOPE_METHODS = "scope methods";
	public static final String AGGREGATES = "aggregates";
	public static final String RELATIONS = "relations";
	public static final String VARIABLES = "variables";
	public static final String METHODS = "methods";
	public static final Object[] EMPTY_ARRAY = new Object[0];

	public DataProviderTreeViewer(Composite parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, Object input, boolean showFilter,
		boolean showFilterMenu, int filterMode, int treeStyle)
	{
		super(parent, showFilter, showFilterMenu,
			// contentProvider
			contentProvider,
			//labelProvider
			new DataProviderDialogLabelProvider(labelProvider),
			// comparator
			null,
			// treeStyle
			treeStyle,
			// filter
			new TreePatternFilter(filterMode),
			// selectionFilter
			new LeafnodesSelectionFilter(contentProvider));
		setInput(input);

		if (getLabelProvider() instanceof ISelectionChangedListener)
		{
			addSelectionChangedListener((ISelectionChangedListener)getLabelProvider());
		}
	}

	public void refreshTree()
	{
		setInput(getInput());
	}

	@Override
	public void setLabelProvider(ILabelProvider labelProvider)
	{
		if (getLabelProvider() instanceof ISelectionChangedListener)
		{
			removeSelectionChangedListener((ISelectionChangedListener)getLabelProvider());
		}
		DataProviderDialogLabelProvider labelProvider2 = new DataProviderDialogLabelProvider(labelProvider);
		addSelectionChangedListener(labelProvider2);
		super.setLabelProvider(labelProvider2);
	}

	public static class DataProviderContentProvider extends ArrayContentProvider implements ITreeContentProvider, IKeywordChecker, ISearchKeyAdapter
	{
		public static final IDataProvider NONE = new NoDataProvider();

		public static class NoDataProvider implements IDataProvider
		{
			public ColumnWrapper getColumnWrapper()
			{
				return null;
			}

			public String getDataProviderID()
			{
				return null;
			}

			public int getDataProviderType()
			{
				return 0;
			}

			public int getFlags()
			{
				return 0;
			}

			public int getLength()
			{
				return 0;
			}

			public boolean isEditable()
			{
				return false;
			}
		}

		public static class UnresolvedDataProvider extends NoDataProvider
		{
			private final String dataProvider;

			public UnresolvedDataProvider(String dataProvider)
			{
				this.dataProvider = dataProvider;
			}

			@Override
			public String getDataProviderID()
			{
				return UnresolvedValue.getUnresolvedMessage(dataProvider);
			}
		}

		private PersistContext persistContext;
		private final FlattenedSolution flattenedSolution;
		private ITable table;

		private DataProviderOptions options;

		private final Map<ITable, List<Relation>> relationsCache = new HashMap<ITable, List<Relation>>();
		private final Map<ITable, List<AggregateVariable>> aggregatesCache = new HashMap<ITable, List<AggregateVariable>>();
		private final Map<ITable, Map<String, ScriptCalculation>> calculationsCache = new HashMap<ITable, Map<String, ScriptCalculation>>();
		private final Map<ITable, List<IColumn>> columnCache = new HashMap<ITable, List<IColumn>>();

		public DataProviderContentProvider(PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table)
		{
			this.persistContext = persistContext;
			this.flattenedSolution = flattenedSolution;
			this.table = table;
		}

		public void setTable(ITable table, PersistContext persistContext)
		{
			this.table = table;
			this.persistContext = persistContext;
		}

		public Object getSearchKey(Object element)
		{
			if (element instanceof DataProviderNodeWrapper)
			{
				RelationList rl = ((DataProviderNodeWrapper)element).relations;
				if (rl != null)
				{
					String node = ((DataProviderNodeWrapper)element).node;
					if (node == CALCULATIONS || node == AGGREGATES)
					{
						return null;
					}
					return rl.getRelation();
				}
			}
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof DataProviderOptions)
			{
				options = (DataProviderOptions)inputElement;
				ArrayList<Object> input = new ArrayList<Object>(20);
				try
				{
					// none
					if (options.includeNone)
					{
						input.add(NONE);
					}

					// columns
					if (options.includeColumns && table != null)
					{
						input = addTableColumns(input, table, null, options.includeCalculations);
					}

					// calculations
					if (options.includeCalculations && table != null)
					{
						input.add(new DataProviderNodeWrapper(CALCULATIONS, (RelationList)null));
					}

					// form variables
					if (options.includeFormVariables && persistContext != null && persistContext.getContext().getAncestor(IRepository.FORMS) != null &&
						!Utils.getAsBoolean(((Form)persistContext.getContext().getAncestor(IRepository.FORMS)).isFormComponent()))
					{
						input.add(new DataProviderNodeWrapper(FORM_VARIABLES, (RelationList)null));
					}

					// scope variables
					if (options.includeScopes)
					{
						input.add(new DataProviderNodeWrapper(SCOPE_VARIABLES, (ScopeWithContext)null, (String)null));
					}

					// aggregates
					if (options.includeAggregates && table != null)
					{
						input.add(new DataProviderNodeWrapper(AGGREGATES, (RelationList)null));
					}

					// relations
					if (options.includeRelations != INCLUDE_RELATIONS.NO && flattenedSolution != null)
					{
						if (options.relations == null)
						{
							Iterator<Relation> relations = flattenedSolution.getRelations(table, true, true);
							Set<String> relationNames = new HashSet<String>();
							while (relations.hasNext())
							{
								Relation relation = relations.next();
								if (persistContext != null && persistContext.getContext() != null &&
									PersistEncapsulation.isModuleScope(relation, (Solution)persistContext.getContext().getRootObject()))
								{
									continue;
								}
								if ((options.includeGlobalRelations || !relation.isGlobal()) && relationNames.add(relation.getName()))
								{
									input.add(new DataProviderNodeWrapper(RELATIONS, new RelationList(relation)));
								}
							}
						}
						else if (options.relations.length > 0)
						{
							input.add(new DataProviderNodeWrapper(RELATIONS, options.relations));
						}
					}

					// in some cases we show only one category, in that case remove the single parent.
					if (options.expandSingleParent)
					{
						int parents = options.includeColumns ? 1 : 0;
						int parentIndex = -1;
						for (int i = 0; i < input.size(); i++)
						{
							if (hasChildren(input.get(i)))
							{
								parents++;
								parentIndex = i;
							}
						}
						if (parents == 1 && parentIndex >= 0)
						{
							// replace the single parent with its children
							Object parentElement = input.remove(parentIndex);
							input.addAll(Arrays.asList(getChildren(parentElement)));
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				return input.toArray();
			}
			return super.getElements(inputElement);
		}

		public Object[] getChildren(Object parentElement)
		{
			ArrayList<Object> children = null;
			try
			{
				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == CALCULATIONS)
				{
					DataProviderNodeWrapper nodeWrapper = (DataProviderNodeWrapper)parentElement;
					ITable calcsTable;
					if (nodeWrapper.relations == null)
					{
						calcsTable = table;
					}
					else
					{
						calcsTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
							nodeWrapper.relations.getRelation().getForeignDataSource());
					}
					if (calcsTable != null)
					{
						Map<String, ScriptCalculation> map = getCalculationMap(calcsTable);
						if (map.size() > 0)
						{
							Collection<ScriptCalculation> calcs = map.values();
							if (nodeWrapper.relations == null)
							{
								children = new ArrayList<Object>(calcs);
							}
							else
							{
								children = new ArrayList<Object>(calcs.size() + 10);
								for (ScriptCalculation calc : calcs)
								{
									children.add(new ColumnWrapper(calc, nodeWrapper.relations));
								}
							}
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == FORM_VARIABLES)
				{
					Form flattenedForm = ModelUtils.getEditingFlattenedSolution(persistContext.getContext()).getFlattenedForm(persistContext.getContext());
					if (flattenedForm != null)
					{
						Form currentForm = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
						Iterator<ScriptVariable> formVariables = flattenedForm.getScriptVariables(true);
						if (formVariables.hasNext() && children == null) children = new ArrayList<Object>(10);
						while (formVariables.hasNext())
						{
							ScriptVariable sv = formVariables.next();
							if (sv.getParent() != currentForm && sv.isPrivate()) continue;
							children.add(sv);
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == SCOPE_VARIABLES &&
					flattenedSolution != null)
				{
					Collection<Pair<String, IRootObject>> scopes = flattenedSolution.getScopes();
					SortedList<ScopeWithContext> scopesList = new SortedList<ScopeWithContext>(ScopeWithContext.SCOPE_COMPARATOR);
					for (Pair<String, IRootObject> sc : scopes)
					{
						scopesList.add(new ScopeWithContext(sc.getLeft(), sc.getRight()));
					}
					children = new ArrayList<Object>();
					for (ScopeWithContext scope : scopesList)
					{
						children.add(new DataProviderNodeWrapper(scope.getName(), scope, VARIABLES));
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).scope != null &&
					((DataProviderNodeWrapper)parentElement).type == VARIABLES && flattenedSolution != null)
				{
					Iterator<ScriptVariable> scopeVars = flattenedSolution.getScriptVariables(((DataProviderNodeWrapper)parentElement).scope.getName(), true);
					if (scopeVars.hasNext() && children == null) children = new ArrayList<Object>(10);
					while (scopeVars.hasNext())
					{
						ScriptVariable sv = scopeVars.next();
						if (!sv.isPrivate())
						{
							children.add(sv);
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == AGGREGATES &&
					flattenedSolution != null)
				{
					DataProviderNodeWrapper nodeWrapper = (DataProviderNodeWrapper)parentElement;
					ITable aggsTable;
					if (nodeWrapper.relations == null)
					{
						aggsTable = table;
					}
					else
					{
						aggsTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
							nodeWrapper.relations.getRelation().getForeignDataSource());
					}
					if (aggsTable != null)
					{
						List<AggregateVariable> list = aggregatesCache.get(aggsTable);
						if (list == null)
						{
							list = new ArrayList<AggregateVariable>(4);
							Iterator<AggregateVariable> aggs = flattenedSolution.getAggregateVariables(aggsTable, true);
							while (aggs.hasNext())
							{
								list.add(aggs.next());
							}
							aggregatesCache.put(aggsTable, list);
						}

						if (list.size() > 0)
						{
							if (nodeWrapper.relations == null)
							{
								if (children == null) children = new ArrayList<Object>(list);
								else children.addAll(list);
							}
							else
							{
								if (children == null) children = new ArrayList<Object>(list.size() + 10);
								else children.ensureCapacity(children.size() + list.size() + 10);
								for (AggregateVariable element : list)
								{
									children.add(new ColumnWrapper(element, nodeWrapper.relations));
								}
							}
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == RELATIONS &&
					((DataProviderNodeWrapper)parentElement).relations != null)
				{
					Relation relation = ((DataProviderNodeWrapper)parentElement).relations.getRelation();
					ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(relation.getForeignDataSource());
					if (foreignTable != null)
					{
						children = addTableColumns(children, foreignTable, ((DataProviderNodeWrapper)parentElement).relations,
							options.includeRelatedCalculations);

						// related calculations
						if (options.includeRelatedCalculations)
						{
							if (children == null) children = new ArrayList<Object>(4);
							children.add(new DataProviderNodeWrapper(CALCULATIONS, ((DataProviderNodeWrapper)parentElement).relations));
						}

						// related aggregates
						if (options.includeRelatedAggregates)
						{
							if (children == null) children = new ArrayList<Object>(4);
							children.add(new DataProviderNodeWrapper(AGGREGATES, ((DataProviderNodeWrapper)parentElement).relations));
						}

						// nested relations
						if (options.includeRelations == INCLUDE_RELATIONS.NESTED && flattenedSolution != null)
						{
							List<Relation> tableRelations = relationsCache.get(foreignTable);
							if (tableRelations == null)
							{
								tableRelations = new ArrayList<Relation>();
								Set<String> relationNames = new HashSet<String>();
								Iterator<Relation> relations = flattenedSolution.getRelations(foreignTable, true, true);
								while (relations.hasNext())
								{
									Relation rel = relations.next();
									if (persistContext != null && persistContext.getContext() != null &&
										PersistEncapsulation.isModuleScope(relation, (Solution)persistContext.getContext().getRootObject()))
									{
										continue;
									}
									if (!rel.isGlobal() && relationNames.add(rel.getName()))
									{
										tableRelations.add(rel);
									}
								}
								relationsCache.put(foreignTable, tableRelations);
							}
							if (tableRelations.size() > 0)
							{
								if (children == null) children = new ArrayList<Object>(tableRelations.size());
								else children.ensureCapacity(children.size() + tableRelations.size());
								RelationList relChain = ((DataProviderNodeWrapper)parentElement).relations;
								for (Relation rel : tableRelations)
								{
									if (relChain.contains(rel) && (rel.isExactPKRef(flattenedSolution) || rel.isParentRef())) continue;
									children.add(new DataProviderNodeWrapper(RELATIONS, new RelationList(relChain, rel)));
								}
							}
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			return children == null ? EMPTY_ARRAY : children.toArray();
		}

		public static Relation[] arrayAdd(Relation[] array, Relation element)
		{
			Relation[] res;
			if (array == null)
			{
				res = new Relation[1];
			}
			else
			{
				res = new Relation[array.length + 1];
				System.arraycopy(array, 0, res, 0, array.length);
			}
			res[res.length - 1] = element;
			return res;
		}

		/**
		 * @param calcsTable
		 * @return
		 * @throws RepositoryException
		 */
		private Map<String, ScriptCalculation> getCalculationMap(ITable calcsTable) throws RepositoryException
		{
			Map<String, ScriptCalculation> map = calculationsCache.get(calcsTable);
			if (map == null)
			{
				map = new TreeMap<String, ScriptCalculation>();
				Iterator<ScriptCalculation> calcs = flattenedSolution.getScriptCalculations(calcsTable, false);
				while (calcs.hasNext())
				{
					ScriptCalculation calc = calcs.next();
					map.put(calc.getDataProviderID(), calc);
				}
				calculationsCache.put(calcsTable, map);
			}
			return map;
		}

		public Object getParent(Object value)
		{
			if (value instanceof ColumnWrapper)
			{
				ColumnWrapper columnWrapper = (ColumnWrapper)value;
				if (columnWrapper.getColumn() instanceof ScriptCalculation)
				{
					return new DataProviderNodeWrapper(CALCULATIONS, columnWrapper.getRelationList());
				}
				if (columnWrapper.getColumn() instanceof AggregateVariable)
				{
					return new DataProviderNodeWrapper(AGGREGATES, columnWrapper.getRelationList());
				}
				return new DataProviderNodeWrapper(RELATIONS, columnWrapper.getRelationList());
			}
			if (value instanceof ScriptCalculation)
			{
				return new DataProviderNodeWrapper(CALCULATIONS, (RelationList)null);
			}
			if (value instanceof ScriptVariable)
			{
				ScriptVariable var = (ScriptVariable)value;

				if (var.getParent() instanceof Form)
				{
					return new DataProviderNodeWrapper(FORM_VARIABLES, (RelationList)null);
				}
				if (var.getParent() instanceof Solution)
				{
					return new DataProviderNodeWrapper(var.getScopeName(), new ScopeWithContext(var.getScopeName(), (Solution)var.getParent()), VARIABLES);
				}
			}
			if (value instanceof AggregateVariable)
			{
				return new DataProviderNodeWrapper(AGGREGATES, (RelationList)null);
			}
			if (value instanceof DataProviderNodeWrapper)
			{
				DataProviderNodeWrapper wrapper = (DataProviderNodeWrapper)value;
				if (wrapper.relations != null)
				{
					if (wrapper.node == RELATIONS)
					{
						if (wrapper.relations.getParent() != null)
						{
							return new DataProviderNodeWrapper(RELATIONS, wrapper.relations.getParent());
						}
					}
					else
					{
						return new DataProviderNodeWrapper(RELATIONS, wrapper.relations);
					}
				}
				if (wrapper.scope != null && wrapper.type == VARIABLES)
				{
					return new DataProviderNodeWrapper(SCOPE_VARIABLES, (ScopeWithContext)null);
				}
			}
			return null;
		}

		public boolean hasChildren(Object value)
		{
			return value instanceof DataProviderNodeWrapper;
		}

		private ArrayList<Object> addTableColumns(ArrayList<Object> input, ITable t, RelationList relations, boolean includeCalculations)
			throws RepositoryException
		{
			ArrayList<Object> retValue = input;
			Map<String, ScriptCalculation> calculations = Collections.emptyMap();
			if (includeCalculations)
			{
				calculations = getCalculationMap(t);
			}

			List<IColumn> lst = columnCache.get(t);
			if (lst == null)
			{
				lst = new ArrayList<IColumn>();
				Iterator<Column> columns = EditorUtil.getTableColumns(t, false);
				while (columns.hasNext())
				{
					IColumn column = columns.next();
					// do not add the column if there is a stored calc for it, so prevent duplicate select values
					if (!calculations.containsKey(column.getDataProviderID()))
					{
						lst.add(column);
					}
				}
				columnCache.put(t, lst);
			}
			int size = lst.size();
			if (size > 0)
			{
				if (relations == null)
				{
					if (retValue == null) retValue = new ArrayList<Object>(lst);
					else retValue.addAll(lst);
				}
				else
				{
					if (retValue == null) retValue = new ArrayList<Object>(size + 4);
					else retValue.ensureCapacity(retValue.size() + size);
					for (int i = 0; i < size; i++)
					{
						retValue.add(new ColumnWrapper(lst.get(i), relations));
					}
				}
			}
			return retValue;
		}

		public boolean isKeyword(Object element)
		{
			if (element == CALCULATIONS || element == FORM_VARIABLES || element == SCOPE_VARIABLES || element == AGGREGATES || element == RELATIONS)
			{
				return true;
			}
			if (element instanceof DataProviderNodeWrapper)
			{
				return isKeyword(((DataProviderNodeWrapper)element).node);
			}
			return false;
		}
	}

	public static class DataProviderNodeWrapper implements IAdaptable
	{
		public final String node;

		public final String type;

		public final RelationList relations;

		public ScopeWithContext scope;

		private int hashcode;

		public DataProviderNodeWrapper(String node, RelationList relations)
		{
			this(node, relations, null, null);
		}

		public DataProviderNodeWrapper(String node, Relation[] relations)
		{
			this.node = node;
			if (relations != null && relations.length > 0)
			{
				RelationList list = new RelationList(relations[0]);
				for (int i = 1; i < relations.length; i++)
				{
					list = new RelationList(list, relations[i]);
				}
				this.relations = list;
			}
			else
			{
				this.relations = null;
			}
			this.scope = null;
			this.type = null;
		}

		public DataProviderNodeWrapper(String node, ScopeWithContext scope)
		{
			this(node, null, scope, null);
		}

		public DataProviderNodeWrapper(String node, ScopeWithContext scope, String type)
		{
			this(node, null, scope, type);
		}

		public DataProviderNodeWrapper(String node, RelationList relations, ScopeWithContext scope, String type)
		{
			this.node = node;
			this.relations = relations;
			this.scope = scope;
			this.type = type;
		}


		@Override
		public Object getAdapter(Class adapter)
		{
			if (adapter == ISupportDeprecated.class && node == RELATIONS && relations != null)
			{
				return relations.getRelation();
			}
			return Platform.getAdapterManager().getAdapter(this, adapter);
		}

		@Override
		public int hashCode()
		{
			if (hashcode == 0)
			{
				final int prime = 31;
				int result = 1;
				result = prime * result + ((node == null) ? 0 : node.hashCode());
				result = prime * result + ((relations == null) ? 0 : relations.hashCode());
				result = prime * result + ((scope == null) ? 0 : scope.hashCode());
				result = prime * result + ((type == null) ? 0 : type.hashCode());
				hashcode = result;
			}
			return hashcode;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final DataProviderNodeWrapper other = (DataProviderNodeWrapper)obj;
			if (node == null)
			{
				if (other.node != null) return false;
			}
			else if (!node.equals(other.node)) return false;
			if (relations == null)
			{
				if (other.relations != null) return false;
			}
			else if (!relations.equals(other.relations)) return false;
			if (scope == null)
			{
				if (other.scope != null) return false;
			}
			else if (!scope.equals(other.scope)) return false;
			if (type == null)
			{
				if (other.type != null) return false;
			}
			else if (!type.equals(other.type)) return false;
			return true;
		}


		@Override
		public String toString()
		{
			return "DataProviderNode(" + String.valueOf(node) + ',' + (scope != null ? scope : "") + "," +
				(relations != null ? Utils.stringJoin(relations.getRelations(), '.') : "") + (type != null ? type : "") + "," + ')';
		}
	}

	public static class DataProviderOptions
	{
		public static enum INCLUDE_RELATIONS
		{
			YES, NO, NESTED
		}

		public final boolean expandSingleParent;
		public final boolean includeNone;
		public final boolean includeColumns;
		public final boolean includeCalculations;
		public final boolean includeFormVariables;
		public final boolean includeScopes;
		public final boolean includeAggregates;
		public final INCLUDE_RELATIONS includeRelations;
		public final boolean includeGlobalRelations;
		private final Relation[] relations;
		private final boolean includeRelatedCalculations;
		private final boolean includeRelatedAggregates;

		public DataProviderOptions(boolean includeNone, boolean includeColumns, boolean includeCalculations, boolean includeRelatedCalculations,
			boolean includeFormVariables, boolean includeScopes, boolean includeAggregates, boolean includeRelatedAggregates,
			INCLUDE_RELATIONS includeRelations, boolean includeGlobalRelations, boolean expandSingleParent, Relation[] relations)
		{
			this.includeNone = includeNone;
			this.includeColumns = includeColumns;
			this.includeCalculations = includeCalculations;
			this.includeRelatedCalculations = includeRelatedCalculations;
			this.includeFormVariables = includeFormVariables;
			this.includeScopes = includeScopes;
			this.includeAggregates = includeAggregates;
			this.includeRelatedAggregates = includeRelatedAggregates;
			this.includeRelations = includeRelations;
			this.includeGlobalRelations = includeGlobalRelations;
			this.expandSingleParent = expandSingleParent;
			this.relations = relations;
		}
	}

	public static class DataProviderDialogLabelProvider extends LabelProvider implements IFontProvider, IDelegate<ILabelProvider>, ISelectionChangedListener
	{
		private final ILabelProvider labelProvider;
		private List<Object> selectedElements;

		public DataProviderDialogLabelProvider(ILabelProvider labelProvider)
		{
			this.labelProvider = labelProvider;
		}

		@Override
		public String getText(Object value)
		{
			String append = "";
			if (selectedElements != null && selectedElements.contains(value))
			{
				append += getDataProviderTypeByValue(value);
			}
			String dpDialogText = getDataProviderDialogText(value);
			if (dpDialogText == null)
			{
				return labelProvider.getText(value) + append;
			}
			return dpDialogText + append;
		}

		private String getDataProviderTypeByValue(Object value)
		{
			if (value instanceof Column)
			{
				// use dataprovider type as defined by column converter
				ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, (Column)value, Activator.getDefault().getDesignClient());
				return " - " + Column.getDisplayTypeString(componentFormat.dpType);
			}
			if (value instanceof ScriptVariable)
			{
				return " - " + Column.getDisplayTypeString(((ScriptVariable)value).getVariableType());
			}
			if (value instanceof ScriptCalculation)
			{
				return " - " + ((ScriptCalculation)value).getTypeAsString();
			}
			if (value instanceof AggregateVariable)
			{
				return " - " + Column.getDisplayTypeString(((AggregateVariable)value).getDataProviderType());
			}
			return "";
		}

		protected String getDataProviderDialogText(Object value)
		{
			if (value instanceof DataProviderNodeWrapper)
			{
				DataProviderNodeWrapper wrapper = (DataProviderNodeWrapper)value;
				if (wrapper.node == RELATIONS && wrapper.relations != null)
				{
					return wrapper.relations.getRelation().getName();
				}
				return ((DataProviderNodeWrapper)value).node;
			}
			return null;
		}

		public Font getFont(Object value)
		{
			if (value instanceof Relation || value instanceof DataProviderNodeWrapper)
			{
				return FontResource.getDefaultFont(SWT.ITALIC, 0);
			}
			if (labelProvider instanceof IFontProvider)
			{
				return ((IFontProvider)labelProvider).getFont(value);
			}
			return null;
		}

		@Override
		public Image getImage(Object value)
		{
			Image dpDialogImage = getDataProviderDialogImage(value);
			if (dpDialogImage == null)
			{
				return labelProvider.getImage(value);
			}
			return dpDialogImage;
		}

		public Image getDataProviderDialogImage(Object value)
		{
			if (value instanceof DataProviderNodeWrapper)
			{
				DataProviderNodeWrapper wrapper = (DataProviderNodeWrapper)value;
				if (wrapper.node == RELATIONS && wrapper.relations != null)
				{
					return RelationLabelProvider.INSTANCE_LAST_NAME_ONLY.getImage(wrapper.relations.getRelation());
				}
			}
			return null;
		}

		private ILabelProvider delegatingLabelProvider = null;

		public ILabelProvider getDelegate()
		{
			if (delegatingLabelProvider != null) return delegatingLabelProvider;
			// special implementation of getDelegate, add DataProviderDialogLabelProvider stuff to delegate label provider.
			Object delegate = labelProvider;
			while (delegate instanceof IDelegate)
			{
				delegate = ((IDelegate)delegate).getDelegate();
			}
			final ILabelProvider delegateLabelProvider = (ILabelProvider)delegate;
			delegatingLabelProvider = new DelegateLabelProvider(delegateLabelProvider)
			{
				@Override
				public String getText(Object element)
				{
					String dpDialogText = getDataProviderDialogText(element);
					if (dpDialogText == null)
					{
						return delegateLabelProvider.getText(element);
					}
					return dpDialogText;
				}

				@Override
				public Image getImage(Object element)
				{
					Image dpDialogImage = getDataProviderDialogImage(element);
					if (dpDialogImage == null)
					{
						return delegateLabelProvider.getImage(element);
					}
					return dpDialogImage;
				}
			};

			return delegatingLabelProvider;
		}

		public void selectionChanged(SelectionChangedEvent event)
		{
			List<Object> previousElems = selectedElements;
			selectedElements = Arrays.asList(((IStructuredSelection)event.getSelection()).toArray());

			if (event.getSource() instanceof TreeViewer)
			{
				if (previousElems != null && selectedElements != null)
				{
					List<Object> selected = new ArrayList<Object>();
					selected.addAll(previousElems);
					selected.addAll(selectedElements);
					((TreeViewer)event.getSource()).update(selected.toArray(), null);
				}
				else if (selectedElements != null)
				{
					((TreeViewer)event.getSource()).update(selectedElements.toArray(), null);
				}
				else if (previousElems != null)
				{
					((TreeViewer)event.getSource()).update(previousElems.toArray(), null);
				}

				if (UIUtils.isDarkThemeSelected(true))
				{
					((TreeViewer)event.getSource()).getTree().addListener(SWT.EraseItem, evt -> {
						evt.detail &= ~SWT.HOT;
						if ((evt.detail & SWT.SELECTED) == 0) return; /// item not selected

						TreeItem item = (TreeItem)evt.item;
						int clientWidth = item.getBounds().width;

						GC gc = evt.gc;
						Color oldForeground = gc.getForeground();
						Color oldBackground = gc.getBackground();

						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
						gc.fillRectangle(item.getBounds().x, evt.y, clientWidth, evt.height);

						gc.setForeground(oldForeground);
						gc.setBackground(oldBackground);
						evt.detail &= ~SWT.SELECTED;
					});
				}
			}
		}
	}
}
