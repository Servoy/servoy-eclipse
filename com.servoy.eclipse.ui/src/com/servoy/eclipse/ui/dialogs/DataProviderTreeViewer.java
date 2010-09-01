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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Utils;

/**
 * JFace-like viewer for selecting a data provider from a tree. A filter is built-in.
 * 
 * @author rgansevles
 */

public class DataProviderTreeViewer extends FilteredTreeViewer
{
	public static final String CALCULATIONS = "calculations"; //$NON-NLS-1$
	public static final String FORM_VARIABLES = "form variables"; //$NON-NLS-1$
	public static final String GLOBALS = "globals"; //$NON-NLS-1$
	public static final String AGGREGATES = "aggregates"; //$NON-NLS-1$
	public static final String RELATIONS = "relations"; //$NON-NLS-1$
	public static final Object[] EMPTY_ARRAY = new Object[0];

	public DataProviderTreeViewer(Composite parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, DataProviderOptions input,
		boolean showFilter, boolean showFilterMenu, int filterMode, int filterSearchDepth, int treeStyle)
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
			new TreePatternFilter(filterMode, filterSearchDepth),
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

	public static class DataProviderContentProvider extends ArrayContentProvider implements IMaxDepthTreeContentProvider, IKeywordChecker
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

		private IPersist persist;
		private final FlattenedSolution flattenedSolution;
		private Table table;

		private DataProviderOptions options;

		private final Map<Table, List<Relation>> relationsCache = new HashMap<Table, List<Relation>>();
		private final Map<Table, List<AggregateVariable>> aggregatesCache = new HashMap<Table, List<AggregateVariable>>();
		private final Map<Table, Map<String, ScriptCalculation>> calculationsCache = new HashMap<Table, Map<String, ScriptCalculation>>();
		private final Map<Table, List<Column>> columnCache = new HashMap<Table, List<Column>>();

		public DataProviderContentProvider(IPersist persist, FlattenedSolution flattenedSolution, Table table)
		{
			this.persist = persist;
			this.flattenedSolution = flattenedSolution;
			this.table = table;
		}

		public void setTable(Table table, IPersist persist)
		{
			this.table = table;
			this.persist = persist;
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof DataProviderOptions)
			{
				options = (DataProviderOptions)inputElement;
				List<Object> input = new ArrayList<Object>();
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
						addTableColumns(input, table, null, options.includeCalculations);
					}

					// calculations
					if (options.includeCalculations && table != null)
					{
						input.add(new DataProviderNodeWrapper(CALCULATIONS, null));
					}

					// form variables
					if (options.includeFormVariables && persist != null && persist.getAncestor(IRepository.FORMS) != null)
					{
						input.add(new DataProviderNodeWrapper(FORM_VARIABLES, null));
					}

					// globals
					if (options.includeGlobals)
					{
						input.add(new DataProviderNodeWrapper(GLOBALS, null));
					}

					// aggregates
					if (options.includeAggregates && table != null)
					{
						input.add(new DataProviderNodeWrapper(AGGREGATES, null));
					}

					// relations
					if (options.includeRelations != INCLUDE_RELATIONS.NO)
					{
						if (options.relations == null)
						{
							Iterator<Relation> relations = flattenedSolution.getRelations(table, true, true);
							Set<String> relationNames = new HashSet<String>();
							while (relations.hasNext())
							{
								Relation relation = relations.next();
								if ((options.includeGlobalRelations || !relation.isGlobal()) && relationNames.add(relation.getName()))
								{
									input.add(new DataProviderNodeWrapper(RELATIONS, new Relation[] { relation }));
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
			List<Object> children = new ArrayList<Object>();
			try
			{
				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == CALCULATIONS)
				{
					DataProviderNodeWrapper nodeWrapper = (DataProviderNodeWrapper)parentElement;
					Table calcsTable;
					if (nodeWrapper.relations == null)
					{
						calcsTable = table;
					}
					else
					{
						calcsTable = nodeWrapper.relations[nodeWrapper.relations.length - 1].getForeignTable();
					}
					if (calcsTable != null)
					{
						Map<String, ScriptCalculation> map = getCalculationMap(calcsTable);
						Iterator<ScriptCalculation> calcs = map.values().iterator();
						while (calcs.hasNext())
						{
							ScriptCalculation calc = calcs.next();
							if (nodeWrapper.relations == null)
							{
								children.add(calc);
							}
							else
							{
								children.add(new ColumnWrapper(calc, nodeWrapper.relations));
							}
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == FORM_VARIABLES)
				{
					Form flattenedForm = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist).getFlattenedForm(
						persist);
					if (flattenedForm != null)
					{
						Iterator<ScriptVariable> formVariables = flattenedForm.getScriptVariables(true);
						while (formVariables.hasNext())
						{
							children.add(formVariables.next());
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == GLOBALS)
				{
					Iterator<ScriptVariable> globals = flattenedSolution.getScriptVariables(true);
					while (globals.hasNext())
					{
						children.add(globals.next());
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == AGGREGATES)
				{
					DataProviderNodeWrapper nodeWrapper = (DataProviderNodeWrapper)parentElement;
					Table aggsTable;
					if (nodeWrapper.relations == null)
					{
						aggsTable = table;
					}
					else
					{
						aggsTable = nodeWrapper.relations[nodeWrapper.relations.length - 1].getForeignTable();
					}
					if (aggsTable != null)
					{
						List<AggregateVariable> list = aggregatesCache.get(aggsTable);
						if (list == null)
						{
							list = new ArrayList<AggregateVariable>();
							Iterator<AggregateVariable> aggs = flattenedSolution.getAggregateVariables(aggsTable, true);
							while (aggs.hasNext())
							{
								list.add(aggs.next());
							}
							aggregatesCache.put(aggsTable, list);
						}
						Iterator<AggregateVariable> aggs = list.iterator();
						while (aggs.hasNext())
						{
							AggregateVariable agg = aggs.next();
							if (nodeWrapper.relations == null)
							{
								children.add(agg);
							}
							else
							{
								children.add(new ColumnWrapper(agg, nodeWrapper.relations));
							}
						}
					}
				}

				if (parentElement instanceof DataProviderNodeWrapper && ((DataProviderNodeWrapper)parentElement).node == RELATIONS &&
					((DataProviderNodeWrapper)parentElement).relations.length > 0)
				{
					Relation relation = ((DataProviderNodeWrapper)parentElement).relations[((DataProviderNodeWrapper)parentElement).relations.length - 1];
					if (relation.getForeignTable() != null)
					{
						addTableColumns(children, relation.getForeignTable(), ((DataProviderNodeWrapper)parentElement).relations,
							options.includeRelatedCalculations);

						// related calculations
						if (options.includeRelatedCalculations)
						{
							children.add(new DataProviderNodeWrapper(CALCULATIONS, ((DataProviderNodeWrapper)parentElement).relations));
						}

						// related aggregates
						if (options.includeRelatedAggregates)
						{
							children.add(new DataProviderNodeWrapper(AGGREGATES, ((DataProviderNodeWrapper)parentElement).relations));
						}

						// nested relations
						if (options.includeRelations == INCLUDE_RELATIONS.NESTED)
						{
							Iterator<Relation> relations = null;
							List<Relation> tableRelations = relationsCache.get(relation.getForeignTable());
							if (tableRelations == null)
							{
								tableRelations = new ArrayList<Relation>();
								Set<String> relationNames = new HashSet<String>();
								relations = flattenedSolution.getRelations(relation.getForeignTable(), true, true);
								while (relations.hasNext())
								{
									Relation rel = relations.next();
									if (!rel.isGlobal() && relationNames.add(rel.getName()))
									{
										tableRelations.add(rel);
									}
								}
								relationsCache.put(relation.getForeignTable(), tableRelations);
							}
							relations = tableRelations.iterator();
							List<Relation> relChain = Arrays.asList(((DataProviderNodeWrapper)parentElement).relations);
							while (relations.hasNext())
							{
								Relation rel = relations.next();
								if (relChain.contains(rel) && (rel.isExactPKRef(flattenedSolution) || rel.isParentRef())) continue;
								children.add(new DataProviderNodeWrapper(RELATIONS, Utils.arrayAdd(((DataProviderNodeWrapper)parentElement).relations, rel,
									true)));
							}
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			return children.size() == 0 ? EMPTY_ARRAY : children.toArray();
		}

		/**
		 * @param calcsTable
		 * @return
		 * @throws RepositoryException
		 */
		private Map<String, ScriptCalculation> getCalculationMap(Table calcsTable) throws RepositoryException
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
					return new DataProviderNodeWrapper(CALCULATIONS, columnWrapper.getRelations());
				}
				if (columnWrapper.getColumn() instanceof AggregateVariable)
				{
					return new DataProviderNodeWrapper(AGGREGATES, columnWrapper.getRelations());
				}
				return new DataProviderNodeWrapper(RELATIONS, columnWrapper.getRelations());
			}
			if (value instanceof ScriptCalculation)
			{
				return new DataProviderNodeWrapper(CALCULATIONS, null);
			}
			if (value instanceof ScriptVariable)
			{
				if (((ScriptVariable)value).getParent() instanceof Form)
				{
					return new DataProviderNodeWrapper(FORM_VARIABLES, null);
				}
				else
				{
					return new DataProviderNodeWrapper(GLOBALS, null);
				}
			}
			if (value instanceof AggregateVariable)
			{
				return new DataProviderNodeWrapper(AGGREGATES, null);
			}
			if (value instanceof DataProviderNodeWrapper)
			{
				DataProviderNodeWrapper wrapper = (DataProviderNodeWrapper)value;
				if (wrapper.relations != null)
				{
					if (wrapper.node == RELATIONS)
					{
						if (wrapper.relations.length > 1)
						{
							return new DataProviderNodeWrapper(RELATIONS, Utils.arraySub(wrapper.relations, 0, wrapper.relations.length - 1));
						}
					}
					else
					{
						return new DataProviderNodeWrapper(RELATIONS, wrapper.relations);
					}
				}
			}
			return null;
		}

		public boolean hasChildren(Object value)
		{
			return value instanceof DataProviderNodeWrapper;
		}

		public boolean searchLimitReached(Object element, int depth)
		{
			if (element instanceof DataProviderNodeWrapper)
			{
				return RelationContentProvider.exceedsRelationsDepth(((DataProviderNodeWrapper)element).relations, depth);
			}
			if (element instanceof ColumnWrapper)
			{
				return RelationContentProvider.exceedsRelationsDepth(((ColumnWrapper)element).getRelations(), depth);
			}
			return false;
		}

		private void addTableColumns(List<Object> input, Table table, Relation[] relations, boolean includeCalculations) throws RepositoryException
		{
			Map<String, ScriptCalculation> calculations = Collections.emptyMap();
			if (includeCalculations)
			{
				calculations = getCalculationMap(table);
			}

			List<Column> lst = columnCache.get(table);
			if (lst == null)
			{
				lst = new ArrayList<Column>();
				Iterator<Column> columns = table.getColumnsSortedByName();
				while (columns.hasNext())
				{
					lst.add(columns.next());
				}
				columnCache.put(table, lst);
			}
			Iterator<Column> columns = lst.iterator();
			while (columns.hasNext())
			{
				Column column = columns.next();

				ColumnInfo ci = column.getColumnInfo();
				if (ci != null && ci.isExcluded())
				{
					continue;
				}

				// do not add the column if there is a stored calc for it, so prevent duplicate select values
				if (!calculations.containsKey(column.getDataProviderID()))
				{
					if (relations == null)
					{
						input.add(column);
					}
					else
					{
						input.add(new ColumnWrapper(column, relations));
					}
				}
			}
		}

		public boolean isKeyword(Object element)
		{
			if (element == CALCULATIONS || element == FORM_VARIABLES || element == GLOBALS || element == AGGREGATES || element == RELATIONS)
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

	public static class DataProviderNodeWrapper
	{
		public final String node;
		public final Relation[] relations;

		public DataProviderNodeWrapper(String node, Relation[] relations)
		{
			this.node = node;
			this.relations = (relations == null || relations.length == 0) ? null : relations;
		}


		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + Arrays.hashCode(relations);
			return result;
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
			if (!Arrays.equals(relations, other.relations)) return false;
			return true;
		}


		@Override
		public String toString()
		{
			return "DataProviderNode(" + String.valueOf(node) + ',' + Utils.stringJoin(relations, '.') + ')';
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
		public final boolean includeGlobals;
		public final boolean includeAggregates;
		public final INCLUDE_RELATIONS includeRelations;
		public final boolean includeGlobalRelations;
		private final Relation[] relations;
		private final boolean includeRelatedCalculations;
		private final boolean includeRelatedAggregates;

		public DataProviderOptions(boolean includeNone, boolean includeColumns, boolean includeCalculations, boolean includeRelatedCalculations,
			boolean includeFormVariables, boolean includeGlobals, boolean includeAggregates, boolean includeRelatedAggregates,
			INCLUDE_RELATIONS includeRelations, boolean includeGlobalRelations, boolean expandSingleParent, Relation[] relations)
		{
			this.includeNone = includeNone;
			this.includeColumns = includeColumns;
			this.includeCalculations = includeCalculations;
			this.includeRelatedCalculations = includeRelatedCalculations;
			this.includeFormVariables = includeFormVariables;
			this.includeGlobals = includeGlobals;
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
			String append = ""; //$NON-NLS-1$
			if (selectedElements != null)
			{
				if (selectedElements.contains(value)) append += getDataProviderTypeByValue(value);
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
				return " - " + Column.getDisplayTypeString(((Column)value).getDataProviderType()); //$NON-NLS-1$
			}
			else if (value instanceof ScriptVariable)
			{
				return " - " + Column.getDisplayTypeString(((ScriptVariable)value).getVariableType()); //$NON-NLS-1$
			}
			else if (value instanceof ScriptCalculation)
			{
				return " - " + ((ScriptCalculation)value).getTypeAsString(); //$NON-NLS-1$
			}
			else if (value instanceof AggregateVariable)
			{
				return " - " + Column.getDisplayTypeString(((AggregateVariable)value).getDataProviderType()); //$NON-NLS-1$
			}
			return "";//$NON-NLS-1$
		}

		protected String getDataProviderDialogText(Object value)
		{
			if (value instanceof DataProviderNodeWrapper)
			{
				DataProviderNodeWrapper wrapper = (DataProviderNodeWrapper)value;
				if (wrapper.node == RELATIONS && wrapper.relations.length > 0)
				{
					return wrapper.relations[wrapper.relations.length - 1].getName();
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
				if (wrapper.node == RELATIONS && wrapper.relations.length > 0)
				{
					return RelationLabelProvider.INSTANCE_LAST_NAME_ONLY.getImage(wrapper.relations[wrapper.relations.length - 1]);
				}
			}
			return null;
		}

		public ILabelProvider getDelegate()
		{
			// special implementation of getDelegate, add DataProviderDialogLabelProvider stuff to delegate label provider.
			Object delegate = labelProvider;
			while (delegate instanceof IDelegate)
			{
				delegate = ((IDelegate)delegate).getDelegate();
			}
			final ILabelProvider delegateLabelProvider = (ILabelProvider)delegate;
			return new LabelProvider()
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
			}

		}
	}
}
