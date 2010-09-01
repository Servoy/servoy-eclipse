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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.editors.table.ColumnLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.smart.WebStart;

public class SortDialog extends Dialog
{
	private TableViewer tableViewer;
	private Table columnTable;
	private DataProviderTreeViewer dataProviderViewer;
	private final com.servoy.j2db.persistence.Table table;
	private final String title;
	private final Object value;
	private Composite tableContainer;
	private SortModel model;
	private final FlattenedSolution flattenedEditingSolution;

	public SortDialog(Shell shell, FlattenedSolution flattenedEditingSolution, com.servoy.j2db.persistence.Table table, Object value, String title)
	{
		super(shell);
		this.flattenedEditingSolution = flattenedEditingSolution;
		this.table = table;
		this.value = value;
		this.title = title;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText(title);

		Composite composite = (Composite)super.createDialogArea(parent);

		applyDialogFont(composite);
		dataProviderViewer = new DataProviderTreeViewer(composite, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, new DataProviderContentProvider(null,
			flattenedEditingSolution, table), new DataProviderTreeViewer.DataProviderOptions(false, true, false, false, false, false, false, true,
			INCLUDE_RELATIONS.NESTED, false, true, null), true, true, TreePatternFilter.getSavedFilterMode(getDialogBoundsSettings(),
			TreePatternFilter.FILTER_PARENTS), TreePatternFilter.getSavedFilterSearchDepth(getDialogBoundsSettings(),
			IMaxDepthTreeContentProvider.DEPTH_DEFAULT), SWT.MULTI);

		final Button leftButton = new Button(composite, SWT.NONE);
		leftButton.setText("<<"); //$NON-NLS-1$
		leftButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (tableViewer.getSelection() != null)
				{
					ISelection sel = tableViewer.getSelection();
					if (sel instanceof IStructuredSelection)
					{
						model.removeColumn(((IStructuredSelection)sel).toList());
					}
				}
			}
		});

		final Button rightButton = new Button(composite, SWT.NONE);
		rightButton.setText(">>"); //$NON-NLS-1$
		rightButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (dataProviderViewer.getSelection() != null)
				{
					ISelection sel = dataProviderViewer.getSelection();
					if (sel instanceof IStructuredSelection)
					{
						model.addColumn(((IStructuredSelection)sel).toList());
					}
				}
			}
		});

		Button upButton = new Button(composite, SWT.NONE);
		upButton.setText("up");
		upButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (columnTable.getSelectionCount() == 1)
				{
					ISelection sel = tableViewer.getSelection();
					if (sel instanceof IStructuredSelection)
					{
						Object first = ((IStructuredSelection)sel).getFirstElement();
						model.moveUp((SortColumn)first);
					}
				}
			}
		});

		Button downButton = new Button(composite, SWT.NONE);
		downButton.setText("down");
		downButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (columnTable.getSelectionCount() == 1)
				{
					ISelection sel = tableViewer.getSelection();
					if (sel instanceof IStructuredSelection)
					{
						Object first = ((IStructuredSelection)sel).getFirstElement();
						model.moveDown((SortColumn)first);
					}
				}
			}
		});

		Button copyButton = new Button(composite, SWT.NONE);
		copyButton.setText("copy");
		copyButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				WebStart.setClipboardContent(getValue());
			}
		});

		leftButton.setEnabled(false);
		rightButton.setEnabled(false);
		dataProviderViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{

			public void selectionChanged(SelectionChangedEvent event)
			{
				if (dataProviderViewer.getSelection() != null && !dataProviderViewer.getSelection().isEmpty())
				{
					rightButton.setEnabled(true);
				}
				else
				{
					rightButton.setEnabled(false);
				}
			}
		});

		dataProviderViewer.addOpenListener(new IOpenListener()
		{
			public void open(OpenEvent event)
			{
				if (dataProviderViewer.getSelection() != null && !dataProviderViewer.getSelection().isEmpty())
				{
					ISelection sel = dataProviderViewer.getSelection();
					if (sel instanceof IStructuredSelection)
					{
						model.addColumn(((IStructuredSelection)sel).toList());
					}
				}
			}
		});

		tableContainer = new Composite(composite, SWT.NONE);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		columnTable = tableViewer.getTable();
		columnTable.setLinesVisible(true);
		columnTable.setHeaderVisible(true);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{

			public void selectionChanged(SelectionChangedEvent event)
			{
				if (tableViewer.getSelection() != null && !tableViewer.getSelection().isEmpty())
				{
					leftButton.setEnabled(true);
				}
				else
				{
					leftButton.setEnabled(false);
				}
			}
		});

		tableViewer.addDoubleClickListener(new IDoubleClickListener()
		{

			public void doubleClick(DoubleClickEvent event)
			{
				if (event.getSelection() != null)
				{
					ISelection sel = event.getSelection();
					if (sel instanceof IStructuredSelection)
					{
						model.removeColumn(((IStructuredSelection)sel).toList());
					}
				}
			}
		});

		final GroupLayout groupLayout = new GroupLayout(composite);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).add(dataProviderViewer, GroupLayout.PREFERRED_SIZE, 240, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(leftButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(
					rightButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(upButton, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(downButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(
					copyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.RELATED).add(
				tableContainer, GroupLayout.PREFERRED_SIZE, 240, Short.MAX_VALUE).add(10, 10, 10)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(10, 10, 10).add(tableContainer, GroupLayout.PREFERRED_SIZE, 254, Short.MAX_VALUE)).add(
					groupLayout.createSequentialGroup().add(10, 10, 10).add(dataProviderViewer, GroupLayout.PREFERRED_SIZE, 254, Short.MAX_VALUE)).add(
					groupLayout.createSequentialGroup().add(46, 46, 46).add(leftButton).add(6, 6, 6).add(rightButton).add(6, 6, 6).add(upButton).add(6, 6, 6).add(
						downButton).add(6, 6, 6).add(copyButton))).addContainerGap()));
		composite.setLayout(groupLayout);

		createTableColumns();

		initDataBindings();
		composite.layout();
		return composite;
	}

	private void initDataBindings()
	{
		model = new SortModel(value);

		tableViewer.setLabelProvider(new ITableLabelProvider()
		{
			public Image getColumnImage(Object element, int columnIndex)
			{
				if (columnIndex == CI_ASCENDING)
				{
					if (((SortColumn)element).getSortOrder() == SortColumn.ASCENDING)
					{
						return ColumnLabelProvider.TRUE_RADIO;
					}
					else
					{
						return ColumnLabelProvider.FALSE_RADIO;
					}
				}
				else if (columnIndex == CI_DESCENDING)
				{
					if (((SortColumn)element).getSortOrder() == SortColumn.ASCENDING)
					{
						return ColumnLabelProvider.FALSE_RADIO;
					}
					else
					{
						return ColumnLabelProvider.TRUE_RADIO;
					}
				}
				else
				{
					return null;
				}
			}

			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == CI_NAME) return ((SortColumn)element).getColumn().getName();
				return null;
			}

			public void addListener(ILabelProviderListener listener)
			{
			}

			public void dispose()
			{
			}

			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			public void removeListener(ILabelProviderListener listener)
			{
			}

		});
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(model.getTableData());
	}

	public static final int CI_NAME = 0;
	public static final int CI_ASCENDING = 1;
	public static final int CI_DESCENDING = 2;

	private void createTableColumns()
	{
		TableColumn nameColumn = new TableColumn(columnTable, SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");

		final TableColumn ascendingColumn = new TableColumn(columnTable, SWT.CENTER, CI_ASCENDING);
		ascendingColumn.setText("Asc");
		TableViewerColumn ascendingViewerColumn = new TableViewerColumn(tableViewer, ascendingColumn);
		ascendingViewerColumn.setEditingSupport(new SortColumnEditingSupport(tableViewer, true));

		final TableColumn descendingColumn = new TableColumn(columnTable, SWT.CENTER, CI_DESCENDING);
		descendingColumn.setText("Desc");
		TableViewerColumn descendingViewerColumn = new TableViewerColumn(tableViewer, descendingColumn);
		descendingViewerColumn.setEditingSupport(new SortColumnEditingSupport(tableViewer, false));

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(1, 50, true));
		layout.setColumnData(ascendingColumn, new ColumnPixelData(40, true));
		layout.setColumnData(descendingColumn, new ColumnPixelData(40, true));
	}

	public Object getValue()
	{
		return model.getValue();
	}

	@Override
	public boolean close()
	{
		((TreePatternFilter)dataProviderViewer.getPatternFilter()).saveSettings(getDialogBoundsSettings());
		return super.close();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return EditorUtil.getDialogSettings("sortDialog"); //$NON-NLS-1$
	}

	public class SortModel
	{
		private final List<SortColumn> tableColumns;

		public SortModel(Object initialData)
		{
			tableColumns = new ArrayList<SortColumn>();

			if (initialData != null)
			{
				try
				{
					StringTokenizer tk = new StringTokenizer(initialData.toString(), ","); //$NON-NLS-1$
					while (tk.hasMoreTokens())
					{
						String columnName = null;
						String order = null;
						String def = tk.nextToken().trim();
						int index = def.indexOf(" "); //$NON-NLS-1$
						if (index != -1)
						{
							columnName = def.substring(0, index);
							order = def.substring(index + 1);
						}
						else
						{
							columnName = def;
						}
						if (columnName != null)
						{
							SortColumn sortColumn = getSortColumn(columnName);
							if (sortColumn != null)
							{
								if (order != null && order.trim().startsWith("desc")) //$NON-NLS-1$
								{
									sortColumn.setSortOrder(SortColumn.DESCENDING);
								}
								else
								{
									sortColumn.setSortOrder(SortColumn.ASCENDING);
								}
								tableColumns.add(sortColumn);
							}
						}
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}

		private SortColumn getSortColumn(String dataProviderID) throws RepositoryException
		{
			com.servoy.j2db.persistence.Table lastTable = table;
			List<Relation> relations = new ArrayList<Relation>();
			String[] split = dataProviderID.split("\\."); //$NON-NLS-1$
			for (int i = 0; i < split.length - 1; i++)
			{
				Relation r = flattenedEditingSolution.getRelation(split[i]);
				if (r == null || !lastTable.equals(r.getPrimaryTable()))
				{
					return null;
				}
				relations.add(r);
				lastTable = r.getForeignTable();
			}

			String colName = split[split.length - 1];
			IColumn c = lastTable.getColumn(colName);
			if (c == null)
			{ // aggregates
				c = AbstractBase.selectByName(flattenedEditingSolution.getAggregateVariables(lastTable, false), colName);
			}
			if (c != null)
			{
				return new SortColumn(c, relations.size() == 0 ? null : relations.toArray(new Relation[relations.size()]));
			}
			return null;
		}

		public SortColumn[] getTableData()
		{
			return tableColumns.toArray(new SortColumn[tableColumns.size()]);
		}

		public void addColumn(List list)
		{
			Iterator iterator = list.iterator();
			while (iterator.hasNext())
			{
				SortColumn sortColumn = null;
				Object element = iterator.next();
				if (!containsSortColumn(element))
				{
					if (element instanceof IColumn)
					{
						sortColumn = new SortColumn((IColumn)element);
					}
					else if (element instanceof ColumnWrapper)
					{
						sortColumn = new SortColumn((ColumnWrapper)element);
					}
					if (sortColumn != null)
					{
						tableColumns.add(sortColumn);
					}
				}
			}

			tableViewer.setInput(getTableData());
		}

		private boolean containsSortColumn(Object element)
		{
			IColumn column = null;
			if (element instanceof IColumn)
			{
				column = (IColumn)element;
			}
			else if (element instanceof ColumnWrapper)
			{
				column = ((ColumnWrapper)element).getColumn();
			}
			if (column != null)
			{
				SortColumn[] sortColumns = getTableData();
				if (sortColumns != null)
				{
					for (SortColumn sortColumn : sortColumns)
					{
						if (sortColumn.getColumn().equals(column)) return true;
					}
				}
			}
			return false;
		}

		public void removeColumn(List<SortColumn> list)
		{
			if (list != null)
			{
				for (SortColumn sortColumn : list)
					tableColumns.remove(sortColumn);
			}
			tableViewer.setInput(getTableData());
		}

		public void moveUp(SortColumn sortColumn)
		{
			if (sortColumn != null)
			{
				int index = tableColumns.indexOf(sortColumn);
				if (index > 0)
				{
					index--;
					tableColumns.remove(sortColumn);
					tableColumns.add(index, sortColumn);
				}
				tableViewer.setInput(getTableData());
			}
		}

		public void moveDown(SortColumn sortColumn)
		{
			if (sortColumn != null)
			{
				int index = tableColumns.indexOf(sortColumn);
				if (index < tableColumns.size() - 1)
				{
					index++;
					tableColumns.remove(sortColumn);
					tableColumns.add(index, sortColumn);
				}
				tableViewer.setInput(getTableData());
			}
		}

		public String getValue()
		{
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < tableColumns.size(); i++)
			{
				SortColumn sc = tableColumns.get(i);
				sb.append(sc.toString());
				if (i < tableColumns.size() - 1) sb.append(", "); //$NON-NLS-1$
			}
			return sb.toString();
		}
	}
}
