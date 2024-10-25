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
package com.servoy.eclipse.ui.editors.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.view.ViewFoundsetTable;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.labelproviders.AccesCheckingContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.StringTokenizerConverter;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class EventsComposite extends Composite
{
	private final TreeViewer treeViewer;
	private final Composite treeContainer;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public EventsComposite(final TableEditor te, Composite parent, int style)
	{
		super(parent, style);

		this.setLayout(new FillLayout());
		ScrolledComposite myScrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);

		final ITable t = te.getTable();
		treeContainer = new Composite(container, SWT.NONE);

		treeViewer = new TreeViewer(treeContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);

		Tree tree = treeViewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING,
				treeContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE))));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(treeContainer, GroupLayout.PREFERRED_SIZE, 323, Short.MAX_VALUE)));
		container.setLayout(groupLayout);
		//
		initDataBindings(t, te);
		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		// expand main solution node
		Object[] treeElements = ((ITreeContentProvider)treeViewer.getContentProvider()).getElements(treeViewer.getInput());
		if (treeElements != null && treeElements.length > 0)
		{
			treeViewer.setExpandedElements(new Object[] { treeElements[0] });
		}
	}

	public void refreshViewer(ITable table)
	{
		List<EventNode> rows = getViewerInput(table);

		if (!getViewerInput(table).equals(treeViewer.getInput()))
		{
			setViewerInput(table, false);
		}
		IBaseLabelProvider labelProvider = treeViewer.getLabelProvider();
		if (labelProvider instanceof EventsLabelProvider)
		{
			((EventsLabelProvider)labelProvider).triggerUpdate();
		}
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public static final int CI_NAME = 0;
	public static final int CI_METHOD = 1;

	private static final class TreeColumnsSorter extends SelectionAdapter
	{
		private final TreeViewerNodesComparator comparator;
		private final TreeViewer viewer;

		private TreeColumnsSorter(TreeViewerNodesComparator comparator, TreeViewer viewer)
		{
			this.comparator = comparator;
			this.viewer = viewer;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			comparator.setAscending(!comparator.isAscending());
			viewer.getTree().setSortDirection(comparator.isAscending() ? SWT.UP : SWT.DOWN);

			try
			{
				viewer.getTree().setRedraw(false);
				viewer.refresh(); // trigger the compare (of Comparator)
			}
			catch (Exception ex)
			{
				Debug.log(ex);
			}
			finally
			{
				viewer.getTree().setRedraw(true);
			}
		}
	}

	protected void initDataBindings(ITable t, final TableEditor te)
	{
		Tree tree = treeViewer.getTree();

		TreeColumn nameColumn = new TreeColumn(tree, SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");

		TreeViewerNodesComparator comparator = new TreeViewerNodesComparator();
		treeViewer.setComparator(comparator);

		tree.setSortColumn(nameColumn);
		nameColumn.addSelectionListener(new TreeColumnsSorter(comparator, treeViewer));

		TreeColumn methodColumn = new TreeColumn(tree, SWT.LEFT, CI_METHOD);
		methodColumn.setText("Method");
		TreeViewerColumn methodViewerColumn = new TreeViewerColumn(treeViewer, methodColumn);
		EventsMethodEditingSupport methodEditing = new EventsMethodEditingSupport(treeViewer, t);
		methodViewerColumn.setEditingSupport(methodEditing);
		methodEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(methodColumn, new ColumnWeightData(10, 50, true));

		treeViewer.setLabelProvider(new EventsLabelProvider());

		treeViewer.setContentProvider(EventsContentProvider.INSTANCE);
		setViewerInput(t, true);
	}

	private List<EventNode> getViewerInput(ITable t)
	{
		List<EventNode> rows = new ArrayList<EventNode>();
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		if (servoyProject != null)
		{
			Set<UUID> solutions = new HashSet<UUID>();
			if (t instanceof AbstractMemTable)
			{
				ServoyProject owner = ((AbstractMemTable)t).getServoyProject();
				ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
				for (ServoyProject project : projects)
				{
					if ((Utils.equalObjects(owner.getProject().getName(), project.getProject().getName()) ||
						Arrays.asList(ServoyBuilder.getSolutionModules(project)).contains(owner)) &&
						!solutions.contains(project.getEditingSolution().getUUID()))
					{
						rows.add(new EventNode(project.getEditingSolution(), t));
						solutions.add(project.getEditingSolution().getUUID());
					}
				}
			}
			else
			{
				Solution solution = (Solution)servoyProject.getEditingPersist(servoyProject.getSolution().getUUID());
				rows.add(new EventNode(solution, t));
				solutions.add(solution.getUUID());

				for (int i = 0; i < rows.size(); i++)
				{
					Solution sol = rows.get(i).getSolution();
					String[] modulesNames = new StringTokenizerConverter(",", true).convertProperty("modulesNames", sol.getModulesNames());
					for (String modulename : modulesNames)
					{
						ServoyProject moduleProject = servoyModel.getServoyProject(modulename);
						if (moduleProject != null && moduleProject.getSolution() != null && solutions.add(moduleProject.getSolution().getUUID()))
						{
							rows.add(new EventNode((Solution)moduleProject.getEditingPersist(moduleProject.getSolution().getUUID()), t));
						}
					}
				}
			}
		}
		return rows;
	}

	/**
	 * @param t
	 */
	private void setViewerInput(ITable t, boolean initialExpand)
	{
		List<EventNode> rows = getViewerInput(t);

		Object[] expandedState = treeViewer.getExpandedElements();
		treeViewer.setInput(rows);
		if (initialExpand)
		{
			final HashSet<EventNode> expandedRows = new HashSet<EventNode>();
			for (EventNode node : rows)
			{
				for (EventNode methodNode : node.getChildren())
				{
					if (methodNode.getMethodWithArguments() != null && methodNode.getMethodWithArguments().methodId > 0)
					{
						expandedRows.add(node);
						break;
					}
				}
			}
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (!treeViewer.getControl().isDisposed()) treeViewer.setExpandedElements(expandedRows.toArray());
				}
			});
		}
		else
		{
			treeViewer.setExpandedElements(expandedState);
		}
	}

	private static class TreeViewerNodesComparator extends ViewerComparator
	{
		private boolean ascending = true;

		public boolean isAscending()
		{
			return ascending;
		}

		public void setAscending(boolean ascending)
		{
			this.ascending = ascending;
		}

		@Override
		public final int compare(Viewer viewer, Object a, Object b)
		{
			int result = compareColumn(viewer, a, b, 0); //only for name column (first column).. for now
			return ascending ? result : (-1) * result;
		}

		private int compareColumn(Viewer viewer, Object a, Object b, int columnNumber)
		{
			if (a instanceof EventNode && b instanceof EventNode)
			{
				if (!((EventNode)a).isSolution() && !((EventNode)b).isSolution()) return 0;
			}

			IBaseLabelProvider baseLabel = ((TreeViewer)viewer).getLabelProvider();
			if (baseLabel instanceof ITableLabelProvider)
			{
				ITableLabelProvider tableProvider = (ITableLabelProvider)baseLabel;
				String e1p = tableProvider.getColumnText(a, columnNumber);
				String e2p = tableProvider.getColumnText(b, columnNumber);
				if (e1p != null && e2p != null)
				{
					return getComparator().compare(e1p, e2p);
				}
			}
			return 0;
		}
	}

	public static class EventNode
	{
		public static enum EventNodeType
		{
			onRecordInsert(StaticContentSpecLoader.PROPERTY_ONINSERTMETHODID, true, true, false),
			onRecordUpdate(StaticContentSpecLoader.PROPERTY_ONUPDATEMETHODID, true, true, false),
			onRecordDelete(StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID, true, true, false),
			afterRecordInsert(StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID, true, true, false),
			afterRecordUpdate(StaticContentSpecLoader.PROPERTY_ONAFTERUPDATEMETHODID, true, true, false),
			afterRecordDelete(StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID, true, true, false),
			onFoundSetRecordCreate(StaticContentSpecLoader.PROPERTY_ONCREATEMETHODID, true, true, false),
			onFoundSetFind(StaticContentSpecLoader.PROPERTY_ONFINDMETHODID, true, true, false),
			onFoundSetSearch(StaticContentSpecLoader.PROPERTY_ONSEARCHMETHODID, true, true, false),
			onFoundSetBeforeSelectionChange(StaticContentSpecLoader.PROPERTY_ONFOUNDSETBEFORESELECTIONCHANGEMETHODID, true, true, false),
			afterFoundSetRecordCreate(StaticContentSpecLoader.PROPERTY_ONAFTERCREATEMETHODID, true, true, false),
			afterFoundSetFind(StaticContentSpecLoader.PROPERTY_ONAFTERFINDMETHODID, true, true, false),
			afterFoundSetSearch(StaticContentSpecLoader.PROPERTY_ONAFTERSEARCHMETHODID, true, true, false),
			onLoad(StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID, false, true, true),
			onFoundsetChunk(StaticContentSpecLoader.PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID, false, true, false),
			onValidate(StaticContentSpecLoader.PROPERTY_ONVALIDATEMETHODID, true, true, true);

			private final TypedProperty<Integer> property;
			private final boolean forTable;
			private final boolean forInmem;
			private final boolean forViewfs;

			EventNodeType(TypedProperty<Integer> property, boolean forTable, boolean forInmem, boolean forViewfs)
			{
				this.property = property;
				this.forTable = forTable;
				this.forInmem = forInmem;
				this.forViewfs = forViewfs;
			}

			public boolean isForInmem()
			{
				return forInmem;
			}

			public boolean isForTable()
			{
				return forTable;
			}

			public boolean isForViewfs()
			{
				return forViewfs;
			}

			public TypedProperty<Integer> getProperty()
			{
				return property;
			}
		}

		private final Solution solution;
		private final List<EventNode> children;
		private final EventNodeType type;
		private MethodWithArguments mwa;
		private final ILabelProvider methodLabelProvider;
		private final ITable table;

		public EventNode(EventNodeType type, MethodWithArguments mwa, Solution solution, ITable table)
		{
			if (type == null) throw new NullPointerException("Type can't be null");
			this.type = type;
			this.mwa = mwa;
			this.solution = solution;
			this.table = table;
			this.children = null;

			this.methodLabelProvider = new AccesCheckingContextDelegateLabelProvider(
				new SolutionContextDelegateLabelProvider(new MethodLabelProvider(PersistContext.create(solution), true, true), solution), null)
			{
				@Override
				public com.servoy.j2db.persistence.IPersist getContext()
				{
					// get the table node lazily, it may not exist yet when the label provider is created
					try
					{
						Iterator<TableNode> it = EventNode.this.solution.getTableNodes(EventNode.this.table);
						if (it.hasNext())
						{
							return it.next();
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					return null;
				}
			};
		}

		public EventNode(Solution solution, ITable table)
		{
			this.solution = solution;
			this.table = table;
			this.type = null;
			this.methodLabelProvider = null;
			this.children = new ArrayList<EventNode>();
			TableNode tableNode = null;
			try
			{
				Iterator<TableNode> it = solution.getTableNodes(table);
				if (it.hasNext())
				{
					tableNode = it.next();
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
			for (EventNodeType tp : EventNodeType.values())
			{
				if (table instanceof Table && !tp.isForTable()) continue;
				if (table instanceof ViewFoundsetTable && !tp.isForViewfs()) continue;
				if (table instanceof MemTable && !tp.isForInmem()) continue;

				MethodWithArguments mw = MethodWithArguments.METHOD_DEFAULT;
				if (tableNode != null)
				{
					Integer property = (Integer)tableNode.getProperty(tp.getProperty().getPropertyName());
					if (property == null)
					{
						continue;
					}
					mw = new MethodWithArguments(
						property.intValue(), dsm.getDataSource(tableNode.getDataSource()));
				}
				children.add(new EventNode(tp, mw, solution, table));
			}
		}

		public MethodWithArguments getMethodWithArguments()
		{
			return mwa;
		}

		public void setMethodWithArguments(MethodWithArguments mwa)
		{
			this.mwa = mwa;
		}

		public String getName()
		{
			return type.toString();
		}

		public ITable getTable()
		{
			return table;
		}


		public Solution getSolution()
		{
			return solution;
		}

		public ILabelProvider getMethodLabelProvider()
		{
			return methodLabelProvider;
		}

		public EventNodeType getType()
		{
			return type;
		}

		public boolean isSolution()
		{
			return type == null;
		}

		public List<EventNode> getChildren()
		{
			return children;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			int code = solution.hashCode();
			if (type == null) return code;
			return code + type.hashCode();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof EventNode)
			{
				EventNode node = (EventNode)obj;
				return node.solution.equals(solution) && node.type == type;
			}
			return false;
		}
	}
}
