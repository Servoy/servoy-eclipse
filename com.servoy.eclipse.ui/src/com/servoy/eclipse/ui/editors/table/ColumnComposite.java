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
import java.util.Comparator;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.DataModelManager;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;

public class ColumnComposite extends Composite
{
	private static int MIN_COLUMN_WIDTH = 50;
//	private final DataBindingContext bindingContext;
	private final TableViewer tableViewer;
	private final ColumnDetailsComposite columnDetailsComposite;
	private final ColumnAutoEnterComposite columnAutoEnterComposite;
	private final ColumnValidationComposite columnValidationComposite;
	private final ColumnConversionComposite columnConversionComposite;
	private final Composite tableContainer;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public ColumnComposite(TableEditor te, Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style);

		this.setLayout(new FillLayout());
		final ScrolledComposite myScrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		final Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);

		final Table t = te.getTable();
		tableContainer = new Composite(container, SWT.NONE);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);

		final TabFolder tabFolder;
		tabFolder = new TabFolder(container, SWT.NONE);
		tabFolder.setVisible(false);

		final GroupLayout groupLayout = new GroupLayout(container);
		final GroupLayout tableLayout = new GroupLayout(container);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				ISelection sel = tableViewer.getSelection();
				if (sel instanceof IStructuredSelection)
				{
					Object first = ((IStructuredSelection)sel).getFirstElement();
					if (first instanceof Column)
					{
						Column c = (Column)first;
						boolean b = (c.getColumnInfo() != null);
						if (!b)
						{
							// if we are only using the eclipse column info manager we can create empty column
							// info, because it will not be saved to disk unless the column exists in the DB;
							// if we are using old table based column info provider, creating new column info would
							// result in it being written into the database even if the column is not...
							IColumnInfoManager[] cims = ServoyModel.getServerManager().getColumnInfoManagers(c.getTable().getServerName());
							if (cims != null && cims.length == 1 && cims[0] instanceof DataModelManager)
							{
								try
								{
									cims[0].createNewColumnInfo(c, false);
									b = true;
								}
								catch (RepositoryException e)
								{
									ServoyLog.logWarning("Cannot create new column info in table editor", e);
								}
							}
						}
						if (b && !tabFolder.isVisible())
						{
							container.setLayout(groupLayout);
							myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
							container.layout(true);
						}
						if (!b && tabFolder.isVisible())
						{
							container.setLayout(tableLayout);
							myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
							container.layout(true);
						}
						tabFolder.setVisible(b);
						if (b) propagateSelection(c);
						tableViewer.getTable().setToolTipText(c.getNote());

					}
					else
					{
						tabFolder.setVisible(false);
						container.setLayout(tableLayout);
						myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
						container.layout(true);
					}
				}
			}
		});

		tableViewer.getTable().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = tableViewer.getTable().getItem(pt);
				if (item != null && item.getBounds(CI_DELETE).contains(pt))
				{
					if (t.getTableType() != ITable.TABLE)
					{
						MessageDialog.openInformation(getShell(), "Cannot delete column", "Cannot delete column from a view.");
						return;
					}
					Object column = item.getData();
					if (column instanceof Column &&
						MessageDialog.openConfirm(getShell(), "Delete column", "Are you sure you want to delete column '" + ((Column)column).getName() + "'?"))
					{
						t.removeColumn((Column)column);
						WritableList columnsList = new WritableList(new ArrayList<Column>(t.getColumns()), Column.class);
						tableViewer.setInput(columnsList);
						tableViewer.refresh();
						if (columnsList.size() > 0) tableViewer.setSelection(new StructuredSelection(columnsList.get(0)), true);
					}
				}
			}
		});

		final TabItem detailsTabItem = new TabItem(tabFolder, SWT.NONE);
		detailsTabItem.setText("Details");

		final TabItem autoEnterTabItem = new TabItem(tabFolder, SWT.NONE);
		autoEnterTabItem.setText("Auto Enter");

		final TabItem validationTabItem = new TabItem(tabFolder, SWT.NONE);
		validationTabItem.setText("Validation");

		columnValidationComposite = new ColumnValidationComposite(tabFolder, SWT.NONE);
		validationTabItem.setControl(columnValidationComposite);

		columnAutoEnterComposite = new ColumnAutoEnterComposite(tabFolder, flattenedSolution, SWT.NONE);
		autoEnterTabItem.setControl(columnAutoEnterComposite);
		columnAutoEnterComposite.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				tableViewer.refresh();
			}
		});

		columnDetailsComposite = new ColumnDetailsComposite(tabFolder, SWT.NONE);
		detailsTabItem.setControl(columnDetailsComposite);

		final TabItem conversionTabItem = new TabItem(tabFolder, SWT.NONE);
		conversionTabItem.setText("Conversion");

		columnConversionComposite = new ColumnConversionComposite(tabFolder, SWT.NONE);
		conversionTabItem.setControl(columnConversionComposite);

		Button addButton;
		addButton = new Button(container, SWT.NONE);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
				String orgName = "type_here"; //$NON-NLS-1$
				String newName = orgName;
				Column c = t.getColumn(newName);
				boolean showWarning = false;
				int i = 1;
				while (c != null)
				{
					newName = orgName + i;
					i++;
					c = t.getColumn(newName);
					showWarning = true;
				}
				try
				{
					if (showWarning) MessageDialog.openWarning(getShell(), "Warning", "There is another type_here column.");
					c = t.createNewColumn(nameValidator, newName, IColumnTypes.TEXT, 50);
					WritableList columnsList = new WritableList(new ArrayList<Column>(t.getColumns()), Column.class);
					tableViewer.setInput(columnsList);
					tableViewer.refresh();
					tableViewer.editElement(c, 0);
					if (tabFolder.isVisible())
					{
						tableViewer.setSelection(new StructuredSelection(c), true);
					}
				}
				catch (RepositoryException e1)
				{
					ServoyLog.logError(e1);
					MessageDialog.openError(getShell(), "Error", "Add column failed: " + e1.getMessage());
				}
			}
		});

		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582, Short.MAX_VALUE).add(
					GroupLayout.LEADING, tabFolder, GroupLayout.PREFERRED_SIZE, 582, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(addButton).addPreferredGap(LayoutStyle.RELATED))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(tableContainer, GroupLayout.PREFERRED_SIZE, 185, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addButton)).add(tabFolder, GroupLayout.PREFERRED_SIZE,
				GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));

		tableLayout.setHorizontalGroup(tableLayout.createParallelGroup(GroupLayout.TRAILING).add(
			tableLayout.createSequentialGroup().addContainerGap().add(
				tableLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582, Short.MAX_VALUE).add(
					tableLayout.createSequentialGroup().add(addButton).addPreferredGap(LayoutStyle.RELATED))).addContainerGap()));
		tableLayout.setVerticalGroup(tableLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			tableLayout.createSequentialGroup().addContainerGap().add(tableContainer, GroupLayout.PREFERRED_SIZE, 185, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(tableLayout.createParallelGroup(GroupLayout.BASELINE).add(addButton)).addContainerGap()));
		container.setLayout(tableLayout);
		//

		// Cannot add/remove columns for views
		addButton.setEnabled(t.getTableType() == ITable.TABLE);

		createTableColumns(t);

		initDataBindings(t);

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void refreshSelection()
	{
		tableViewer.setSelection(tableViewer.getSelection(), true);
	}

	public void selectColumn(Column column)
	{
		tableViewer.setSelection(new StructuredSelection(column));
	}

	private void propagateSelection(Column c)
	{
		columnDetailsComposite.initDataBindings(c);
		columnAutoEnterComposite.initDataBindings(c);
		columnValidationComposite.initDataBindings(c);
		columnConversionComposite.initDataBindings(c);
	}

	static final int CI_NAME = 0;
	static final int CI_TYPE = 1;
	static final int CI_LENGHT = 2;
	static final int CI_ROW_IDENT = 3;
	static final int CI_ALLOW_NULL = 4;
	static final int CI_SEQUENCE_TYPE = 5;
	static final int CI_DELETE = 6;

	private void createTableColumns(final Table table)
	{
		final TableColumn nameColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");
		TableViewerColumn nameViewerColumn = new TableViewerColumn(tableViewer, nameColumn);
		nameViewerColumn.setEditingSupport(new ColumnNameEditingSupport(tableViewer));

		final TableColumn typeColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_TYPE);
		typeColumn.setText("Type");
		TableViewerColumn typeViewerColumn = new TableViewerColumn(tableViewer, typeColumn);
		typeViewerColumn.setEditingSupport(new ColumnTypeEditingSupport(tableViewer));

		final TableColumn lengthColumn = new TableColumn(tableViewer.getTable(), SWT.TRAIL, CI_LENGHT);
		lengthColumn.setText("Length");
		TableViewerColumn lengthViewerColumn = new TableViewerColumn(tableViewer, lengthColumn);
		lengthViewerColumn.setEditingSupport(new ColumnLengthEditingSupport(tableViewer));

		final TableColumn rowIdentColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_ROW_IDENT);
		rowIdentColumn.setText("Row Ident");
		TableViewerColumn rowIdentViewerColumn = new TableViewerColumn(tableViewer, rowIdentColumn);
		rowIdentViewerColumn.setEditingSupport(new ColumnRowIdentEditingSupport(table, tableViewer));

		final TableColumn allowNullColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_ALLOW_NULL);
		allowNullColumn.setText("Allow Null");
		TableViewerColumn allowNullViewerColumn = new TableViewerColumn(tableViewer, allowNullColumn);
		allowNullViewerColumn.setEditingSupport(new ColumnAllowNullEditingSupport(tableViewer));


		final TableColumn seqType = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_SEQUENCE_TYPE);
		seqType.setText("Sequence Type");
		TableViewerColumn seqTypeViewerColumn = new TableViewerColumn(tableViewer, seqType);
		ColumnSeqTypeEditingSupport editingSupport = new ColumnSeqTypeEditingSupport(tableViewer, table);
		seqTypeViewerColumn.setEditingSupport(editingSupport);

		final TableColumn delColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);
		delColumn.setToolTipText("Delete column");

		editingSupport.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				if (event.getSource() instanceof ColumnSeqTypeEditingSupport)
				{
					Column column = ((ColumnSeqTypeEditingSupport)event.getSource()).getColumn();
					if (column != null && column.getExistInDB() && column.getColumnInfo() != null)
					{
						//here the data binding for a column is performed;
						columnAutoEnterComposite.initDataBindings(column);
						columnDetailsComposite.refresh();
					}
				}
			}
		});

		final TableColumnLayout layout = new TableColumnLayout()
		{
			public boolean attachedListener = false;
			public boolean firstTime = true;

			@Override
			protected void setColumnWidths(Scrollable tableTree, int[] widths)
			{
				super.setColumnWidths(tableTree, widths);
				if (!attachedListener)
				{
					ControlAdapter listener = new ControlAdapter()
					{
						@Override
						public void controlResized(ControlEvent e)
						{
							TableColumn tb = (TableColumn)e.getSource();
							if (tb.getWidth() < MIN_COLUMN_WIDTH)
							{
								tb.setWidth(MIN_COLUMN_WIDTH);
							}
							else
							{
								tableViewer.getTable().layout();
							}
						}
					};
					nameColumn.addControlListener(listener);
					typeColumn.addControlListener(listener);
					lengthColumn.addControlListener(listener);
					rowIdentColumn.addControlListener(listener);
					allowNullColumn.addControlListener(listener);
					seqType.addControlListener(listener);
					attachedListener = true;
				}
			}

			@Override
			protected void updateColumnData(Widget column)
			{
				if (firstTime)
				{
					//setting of column data here is important for first resize of columns
					//after the creation of columns page
					this.setColumnData(nameColumn, new ColumnPixelData(nameColumn.getWidth(), true));
					this.setColumnData(typeColumn, new ColumnPixelData(typeColumn.getWidth(), true));
					this.setColumnData(lengthColumn, new ColumnPixelData(lengthColumn.getWidth(), true));
					this.setColumnData(rowIdentColumn, new ColumnPixelData(rowIdentColumn.getWidth(), true));
					this.setColumnData(seqType, new ColumnPixelData(seqType.getWidth(), true));
					firstTime = false;
					super.updateColumnData(column);
				}
				else
				{
					super.updateColumnData(column);
					this.setColumnData(nameColumn, new ColumnPixelData(nameColumn.getWidth(), true));
					this.setColumnData(typeColumn, new ColumnPixelData(typeColumn.getWidth(), true));
					this.setColumnData(lengthColumn, new ColumnPixelData(lengthColumn.getWidth(), true));
					this.setColumnData(rowIdentColumn, new ColumnPixelData(rowIdentColumn.getWidth(), true));
					this.setColumnData(seqType, new ColumnPixelData(seqType.getWidth(), true));
				}
			}
		};
		tableContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		layout.setColumnData(typeColumn, new ColumnWeightData(10, 25, true));
		layout.setColumnData(lengthColumn, new ColumnWeightData(10, 25, true));
		layout.setColumnData(rowIdentColumn, new ColumnWeightData(8, 20, true));
		layout.setColumnData(allowNullColumn, new ColumnPixelData(70, true));
		layout.setColumnData(seqType, new ColumnWeightData(10, 25, true));
		layout.setColumnData(delColumn, new ColumnPixelData(20, false));


		tableViewer.setLabelProvider(new ColumnLabelProvider(new Color(tableViewer.getTable().getShell().getDisplay(), 255, 127, 0)));
		tableViewer.setSorter(new ColumnsSorter(
			tableViewer,
			new TableColumn[] { nameColumn, typeColumn, lengthColumn, rowIdentColumn, allowNullColumn },
			new Comparator[] { NameComparator.INSTANCE, ColumnTypeComparator.INSTANCE, ColumnLengthComparator.INSTANCE, ColumnRowIdentComparator.INSTANCE, ColumnAllowNullComparator.INSTANCE }));

	}

	protected void initDataBindings(Table t)
	{
//		bindingContext = BindingHelper.dispose(bindingContext);
		// if there are no columns in the table create a pk column
		if (t.getColumns().size() == 0)
		{
			String tname = t.getName();
			if (tname.length() > 1 && tname.endsWith("s")) //$NON-NLS-1$
			{
				tname = tname.substring(0, tname.length() - 1);
			}
			IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			String colname = tname.substring(0, Math.min(tname.length(), Column.MAX_SQL_OBJECT_NAME_LENGTH - 3)) + "_id"; //$NON-NLS-1$
			try
			{
				Column id = t.createNewColumn(nameValidator, colname, IColumnTypes.INTEGER, 0);
				id.setDatabasePK(true);
				id.setSequenceType(ColumnInfo.SERVOY_SEQUENCE);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}

		}

		ObservableListContentProvider columnViewContentProvider = new ObservableListContentProvider();
		tableViewer.setContentProvider(columnViewContentProvider);
		WritableList columnsList = new WritableList(new ArrayList<Column>(t.getColumns()), Column.class);
		tableViewer.setInput(columnsList);
//		bindingContext = new DataBindingContext();


	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public static class ColumnTypeComparator implements Comparator
	{
		public static final ColumnTypeComparator INSTANCE = new ColumnTypeComparator();

		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof Column && o2 instanceof Column)
			{
				Column column1 = (Column)o1;
				Column column2 = (Column)o2;
				return column1.getTypeAsString().compareToIgnoreCase(column2.getTypeAsString());
			}
			return 0;
		}
	}

	public static class ColumnLengthComparator implements Comparator
	{
		public static final ColumnLengthComparator INSTANCE = new ColumnLengthComparator();

		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof Column && o2 instanceof Column)
			{
				Column column1 = (Column)o1;
				Column column2 = (Column)o2;
				int length1 = column1.getLength();
				int length2 = column2.getLength();
				if (length1 > length2) return 1;
				else if (length1 == length2) return 0;
				else return -1;
			}
			return 0;
		}
	}

	public static class ColumnAllowNullComparator implements Comparator
	{
		public static final ColumnAllowNullComparator INSTANCE = new ColumnAllowNullComparator();

		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof Column && o2 instanceof Column)
			{
				Column column1 = (Column)o1;
				Column column2 = (Column)o2;
				boolean allowNull1 = column1.getAllowNull();
				boolean allowNull2 = column2.getAllowNull();
				if (!allowNull1 & allowNull2) return 1;
				else if (allowNull1 & !allowNull2) return -1;
				else return 0;
			}
			return 0;
		}
	}

	public static class ColumnRowIdentComparator implements Comparator
	{
		public static final ColumnRowIdentComparator INSTANCE = new ColumnRowIdentComparator();

		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof Column && o2 instanceof Column)
			{
				Column column1 = (Column)o1;
				Column column2 = (Column)o2;
				return Column.getFlagsString(column1.getRowIdentType()).compareToIgnoreCase(Column.getFlagsString(column2.getRowIdentType()));
			}
			return 0;
		}
	}

	public void checkValidState() throws RepositoryException
	{
		if (columnDetailsComposite != null)
		{
			columnDetailsComposite.checkValidState();
		}
	}
}
