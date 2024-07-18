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
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.view.ViewFoundsetTable;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.editors.table.ColumnSeqTypeEditingSupport.ColumnSeqTypeEditingObservable;
import com.servoy.eclipse.ui.editors.table.actions.CopyColumnNameAction;
import com.servoy.eclipse.ui.editors.table.actions.SearchForDataProvidersReferencesAction;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.PrimaryKeyType;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.eclipse.ui.tweaks.IconPreferences;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

public class ColumnComposite extends Composite
{
	private static int MIN_COLUMN_WIDTH = 50;
//	private final DataBindingContext bindingContext;
	private final TableViewer tableViewer;
	private final ColumnDetailsComposite columnDetailsComposite;
	private ColumnAutoEnterComposite columnAutoEnterComposite;
	private ColumnValidationComposite columnValidationComposite;
	private ColumnConversionComposite columnConversionComposite;
	private final Composite tableContainer;
	private final Button displayDataProviderID;

	private CopyColumnNameAction copyColumnNameAction;
	private SearchForDataProvidersReferencesAction searchForReferences;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public ColumnComposite(TableEditor te, Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style);
		parent.getShell().setBackgroundMode(SWT.INHERIT_FORCE);

		this.setLayout(new FillLayout());
		final ScrolledComposite myScrolledComposite = new ScrolledComposite(this, SWT.TRANSPARENT | SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		final Composite container = new Composite(myScrolledComposite, SWT.TRANSPARENT);
		myScrolledComposite.setContent(container);

		final ITable t = te.getTable();
		boolean isViewFoundsetTable = t instanceof ViewFoundsetTable;
		tableContainer = new Composite(container, SWT.INHERIT_DEFAULT);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);

		final CTabFolder tabFolder;
		tabFolder = new CTabFolder(container, SWT.NONE);
		tabFolder.setVisible(false);

		final GroupLayout groupLayout = new GroupLayout(container);
		final GroupLayout tableLayout = new GroupLayout(container);

		tableViewer.addSelectionChangedListener(event -> {
			ISelection sel = tableViewer.getSelection();
			if (sel instanceof IStructuredSelection)
			{
				final Object first = ((IStructuredSelection)sel).getFirstElement();
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
						DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();
						if (dmm != null)
						{
							try
							{
								dmm.createNewColumnInfo(c, false);
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
					ColumnComposite.this.layout(true, true);

					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							tableViewer.reveal(first);
						}
					});
				}
				else
				{
					tabFolder.setVisible(false);
					container.setLayout(tableLayout);
					myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					container.layout(true);
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
				if (item != null && (event.stateMask & SWT.MOD1) > 0 && tableViewer.getTable().getSelection() != null)
				{
					tableViewer.getTable().deselectAll();
				}
				else if (item != null && item.getBounds(displayDataProviderID.getSelection() ? CI_DELETE + 1 : CI_DELETE).contains(pt))
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
						ArrayList<Column> columns = new ArrayList<Column>(t.getColumns());
						int index = columns.indexOf(column);
						t.removeColumn((Column)column);
						WritableList columnsList = new WritableList(new ArrayList<Column>(t.getColumns()), Column.class);
						if (columnsList.size() > 0)
							tableViewer.setSelection(new StructuredSelection(columnsList.get(index >= columnsList.size() ? index - 1 : index)), true);
					}
				}
			}
		});

		final CTabItem detailsTabItem = new CTabItem(tabFolder, SWT.NONE, 0);
		detailsTabItem.setText("Details");
		tabFolder.setSelection(0);

		if (!isViewFoundsetTable)
		{
			final CTabItem autoEnterTabItem = new CTabItem(tabFolder, SWT.NONE, 1);
			autoEnterTabItem.setText("Auto Enter");
			columnAutoEnterComposite = new ColumnAutoEnterComposite(tabFolder, flattenedSolution, SWT.NONE);
			autoEnterTabItem.setControl(columnAutoEnterComposite);
			columnAutoEnterComposite.addChangeListener(event -> tableViewer.refresh());
		}

		columnDetailsComposite = new ColumnDetailsComposite(tabFolder, SWT.NONE, isViewFoundsetTable);
		detailsTabItem.setControl(columnDetailsComposite);

		columnDetailsComposite.addValueChangeListener(event -> tableViewer.refresh());

		//TODO conversion and validation support will be added later for view foundset tables SVY-13547
		if (!isViewFoundsetTable)
		{
			final CTabItem validationTabItem = new CTabItem(tabFolder, SWT.NONE, 2);
			validationTabItem.setText("Validation");
			columnValidationComposite = new ColumnValidationComposite(te, tabFolder, SWT.NONE);
			validationTabItem.setControl(columnValidationComposite);

			final CTabItem conversionTabItem = new CTabItem(tabFolder, SWT.NONE, 3);
			conversionTabItem.setText("Conversion");
			columnConversionComposite = new ColumnConversionComposite(te, tabFolder, SWT.NONE);
			conversionTabItem.setControl(columnConversionComposite);
		}

		if (IconPreferences.getInstance().getUseDarkThemeIcons())
		{
			Color backgroundColor = getServoyGrayBackground();
			//TODO validation and conversion support will be added in the future for view foundset tables SVY-13547
			if (!isViewFoundsetTable)
			{
				columnValidationComposite.setBackground(backgroundColor);
				columnConversionComposite.setBackground(backgroundColor);
			}
			columnDetailsComposite.setBackground(backgroundColor);

			if (!isViewFoundsetTable)
			{
				columnAutoEnterComposite.setBackground(backgroundColor);
			}
		}

		Button addButton;
		addButton = new Button(container, SWT.NONE);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				String orgName = "type_here";
				String newName = orgName;
				IColumn c = t.getColumn(newName);
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
					c = addColumn(t, newName, IColumnTypes.TEXT, 50);
					if (tabFolder.isVisible())
					{
						tableViewer.setSelection(new StructuredSelection(c), true);
					}
					tableViewer.editElement(c, 0);
				}
				catch (RepositoryException e1)
				{
					ServoyLog.logError(e1);
					MessageDialog.openError(getShell(), "Error", "Add column failed: " + e1.getMessage());
				}
			}
		});

		displayDataProviderID = new Button(container, SWT.CHECK);
		displayDataProviderID.setText("Display DataProviderID");
		displayDataProviderID.setVisible(!isViewFoundsetTable);
		displayDataProviderID.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				showDataProviderColumn();
			}
		});
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582,
				Short.MAX_VALUE).add(GroupLayout.LEADING, tabFolder, GroupLayout.PREFERRED_SIZE, 582, Short.MAX_VALUE).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582,
						Short.MAX_VALUE).add(addButton).add(
							groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582,
								Short.MAX_VALUE).add(displayDataProviderID))))));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(tableContainer, GroupLayout.PREFERRED_SIZE, 185, Short.MAX_VALUE).addPreferredGap(
				LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addButton).add(displayDataProviderID)).addPreferredGap(
					LayoutStyle.RELATED)
				.add(tabFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));

		tableLayout.setHorizontalGroup(tableLayout.createParallelGroup(GroupLayout.TRAILING).add(tableLayout.createSequentialGroup().addContainerGap().add(
			tableLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582, Short.MAX_VALUE).add(
				addButton).add(
					tableLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 582,
						Short.MAX_VALUE).add(displayDataProviderID)))
			.addContainerGap()));
		tableLayout.setVerticalGroup(
			tableLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
				tableLayout.createSequentialGroup().addContainerGap().add(tableContainer, GroupLayout.PREFERRED_SIZE, 185, Short.MAX_VALUE).addPreferredGap(
					LayoutStyle.RELATED).add(
						tableLayout.createParallelGroup(GroupLayout.BASELINE).add(addButton).add(displayDataProviderID))
					.addContainerGap()));
		container.setLayout(tableLayout);
		//

		// Cannot add/remove columns for views
		addButton.setEnabled(t.getTableType() == ITable.TABLE);

		createTableColumns(t);

		initDataBindings(t);

		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (hasDataProviderSet(t))
		{
			displayDataProviderID.setSelection(true);
			showDataProviderColumn();
		}
	}

	public static Color getServoyGrayBackground()
	{
		//JFaceResources.getColorRegistry().get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"); is the
		//closest match, but the label background is slightly visible
		Color backgroundColor = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
			.get("org.eclipse.ui.workbench.HOVER_BACKGROUND");
		if (backgroundColor == null)
		{
			JFaceResources.getColorRegistry().put("servoy_gray_background", new RGB(38, 38, 38));
			backgroundColor = JFaceResources.getColorRegistry().get("servoy_gray_background");
		}
		return backgroundColor;
	}

	public IColumn addColumn(ITable t, String newName, int type, int length) throws RepositoryException
	{
		return t.createNewColumn(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), newName, type, length);
	}

	private boolean hasDataProviderSet(ITable table)
	{
		for (Column c : table.getColumns())
		{
			if (!Utils.equalObjects(c.getName(), c.getDataProviderID()))
			{
				return true;
			}
		}
		return false;
	}

	private void showDataProviderColumn()
	{
		TableColumnLayout layout = getTableLayout();
		if (displayDataProviderID.getSelection())
		{
			final TableColumn dataProviderIDColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_DATAPROVIDER_ID);
			dataProviderIDColumn.setText("DataProviderID");
			TableViewerColumn dataProviderIDViewerColumn = new TableViewerColumn(tableViewer, dataProviderIDColumn);
			dataProviderIDViewerColumn.setEditingSupport(new ColumnNameEditingSupport(tableViewer, false));
			layout.setColumnData(dataProviderIDColumn, new ColumnWeightData(20, 50, true));
			tableViewer.getTable().getColumn(CI_NAME).setText("SQL Name");
		}
		else
		{
			tableViewer.getTable().getColumn(CI_DATAPROVIDER_ID).dispose();
			tableViewer.getTable().getColumn(CI_NAME).setText("Name");
		}
		tableContainer.setLayout(layout);
		tableViewer.setLabelProvider(tableViewer.getLabelProvider());
		tableViewer.refresh();
		tableContainer.layout(true);
	}

	public void refreshSelection()
	{
		tableViewer.setSelection(tableViewer.getSelection(), true);
	}

	public void refreshViewer(ITable table)
	{
		WritableList columnsList = new WritableList(new ArrayList<Column>(table.getColumns()), Column.class);
		tableViewer.setInput(columnsList);
		tableViewer.refresh();
	}

	public void selectColumn(IColumn column)
	{
		tableViewer.setSelection(new StructuredSelection(column));
	}

	private void propagateSelection(Column c)
	{
		columnDetailsComposite.initDataBindings(c);
		//TODO validation and conversion support will be added for view foundset tables SVY-13547
		//auto-enter will not be supported!
		if (!(c.getTable() instanceof ViewFoundsetTable))
		{
			columnAutoEnterComposite.initDataBindings(c);
			columnValidationComposite.initDataBindings(c);
			columnConversionComposite.initDataBindings(c);
		}
	}

	static final int CI_NAME = 0;
	static final int CI_TYPE = 1;
	static final int CI_LENGTH = 2;
	static final int CI_ROW_IDENT = 3;
	static final int CI_ALLOW_NULL = 4;
	static final int CI_SEQUENCE_TYPE = 5;
	static final int CI_DELETE = 6;
	static final int CI_DATAPROVIDER_ID = 1;

	private ColumnNameEditingSupport nameEditor = null;
	private TableColumn nameColumn;
	private TableColumn typeColumn;
	private TableColumn lengthColumn;
	private TableColumn rowIdentColumn;
	private TableColumn allowNullColumn;
	private TableColumn seqType;
	private TableColumn delColumn;
	private ColumnAllowNullEditingSupport colomnAllowNullEditingSupport;

	private void createTableColumns(final ITable table)
	{
		nameColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");
		TableViewerColumn nameViewerColumn = new TableViewerColumn(tableViewer, nameColumn);
		nameEditor = new ColumnNameEditingSupport(tableViewer, true);
		nameViewerColumn.setEditingSupport(nameEditor);

		copyColumnNameAction = new CopyColumnNameAction(tableViewer.getTable().getDisplay());//getSite().getShell().getDisplay());
		searchForReferences = new SearchForDataProvidersReferencesAction();
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.add(copyColumnNameAction);
		menuMgr.add(searchForReferences);
		Menu menu = menuMgr.createContextMenu(nameViewerColumn.getViewer().getControl());
		nameViewerColumn.getViewer().getControl().setMenu(menu);
		nameViewerColumn.getViewer().addSelectionChangedListener(copyColumnNameAction);
		nameViewerColumn.getViewer().addSelectionChangedListener(searchForReferences);

		typeColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_TYPE);
		typeColumn.setText("Type");
		TableViewerColumn typeViewerColumn = new TableViewerColumn(tableViewer, typeColumn);
		typeViewerColumn.setEditingSupport(new ColumnTypeEditingSupport(tableViewer));

		lengthColumn = new TableColumn(tableViewer.getTable(), SWT.TRAIL, CI_LENGTH);
		lengthColumn.setText("Length");
		TableViewerColumn lengthViewerColumn = new TableViewerColumn(tableViewer, lengthColumn);
		lengthViewerColumn.setEditingSupport(new ColumnLengthEditingSupport(tableViewer));

		rowIdentColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_ROW_IDENT);
		rowIdentColumn.setText("Row Ident");
		TableViewerColumn rowIdentViewerColumn = new TableViewerColumn(tableViewer, rowIdentColumn);
		rowIdentViewerColumn.setEditingSupport(new ColumnRowIdentEditingSupport(table, tableViewer));

		allowNullColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_ALLOW_NULL);
		allowNullColumn.setText("Allow Null");
		TableViewerColumn allowNullViewerColumn = new TableViewerColumn(tableViewer, allowNullColumn);
		colomnAllowNullEditingSupport = new ColumnAllowNullEditingSupport(tableViewer);
		allowNullViewerColumn.setEditingSupport(colomnAllowNullEditingSupport);
		allowNullColumn.setToolTipText("Modifying allow null after table is created in database may break existing tables at deployment.");

		seqType = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_SEQUENCE_TYPE);
		seqType.setText("Sequence Type");
		TableViewerColumn seqTypeViewerColumn = new TableViewerColumn(tableViewer, seqType);
		ColumnSeqTypeEditingSupport editingSupport = new ColumnSeqTypeEditingSupport(tableViewer, table);
		seqTypeViewerColumn.setEditingSupport(editingSupport);

		delColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);
		delColumn.setToolTipText("Delete column");

		editingSupport.addChangeListener(event -> {
			if (event.getSource() instanceof ColumnSeqTypeEditingObservable)
			{
				columnDetailsComposite.refresh();
			}
		});

		tableContainer.setLayout(getTableLayout());

		tableViewer.setLabelProvider(new ColumnLabelProvider(ColorResource.INSTANCE.getColor(new RGB(255, 127, 0)), this));
		tableViewer.setSorter(new ColumnsSorter(tableViewer, new TableColumn[] { nameColumn, typeColumn, lengthColumn, rowIdentColumn, allowNullColumn },
			new Comparator[] { NameComparator.INSTANCE, ColumnTypeComparator.INSTANCE, ColumnLengthComparator.INSTANCE, ColumnRowIdentComparator.INSTANCE, ColumnAllowNullComparator.INSTANCE }));

	}

	private TableColumnLayout getTableLayout()
	{
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
				if (!firstTime)
				{
					super.updateColumnData(column);
				}

				//setting of column data here is important for first resize of columns
				//after the creation of columns page
				this.setColumnData(nameColumn, new ColumnPixelData(nameColumn.getWidth(), true));
				this.setColumnData(typeColumn, new ColumnPixelData(typeColumn.getWidth(), true));
				this.setColumnData(lengthColumn, new ColumnPixelData(lengthColumn.getWidth(), true));
				this.setColumnData(rowIdentColumn, new ColumnPixelData(rowIdentColumn.getWidth(), true));
				this.setColumnData(seqType, new ColumnPixelData(seqType.getWidth(), true));

				if (firstTime)
				{
					firstTime = false;
					super.updateColumnData(column);
				}
			}
		};

		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));
		layout.setColumnData(typeColumn, new ColumnWeightData(10, 25, true));
		layout.setColumnData(lengthColumn, new ColumnWeightData(10, 25, true));
		layout.setColumnData(rowIdentColumn, new ColumnWeightData(8, 20, true));
		layout.setColumnData(allowNullColumn, new ColumnPixelData(70, true));
		layout.setColumnData(seqType, new ColumnWeightData(10, 25, true));
		layout.setColumnData(delColumn, new ColumnPixelData(20, false));
		return layout;
	}

	protected void initDataBindings(ITable t)
	{
		// if there are no columns in the table create a pk column
		if (t.getColumns().size() == 0 && !(t instanceof AbstractMemTable))
		{
			String tname = t.getName();
			if (tname.length() > 1 && tname.endsWith("s"))
			{
				tname = tname.substring(0, tname.length() - 1);
			}
			IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			String colname = tname.substring(0, Math.min(tname.length(), Column.MAX_SQL_OBJECT_NAME_LENGTH - 5));
			try
			{
				int defaultFirstColumnSequenceType = getDefaultFirstColumnSequenceType(t);
				PrimaryKeyType keyType;
				if (defaultFirstColumnSequenceType == ColumnInfo.UUID_GENERATOR)
				{
					keyType = new DesignerPreferences().getPrimaryKeyUuidType();
					colname += "_uuid";
				}
				else
				{
					keyType = PrimaryKeyType.INTEGER;
					colname += "_id";
				}
				Column id = t.createNewColumn(nameValidator, colname, keyType.getColumnType(), keyType.getLength());
				id.setDatabasePK(true);
				id.setSequenceType(defaultFirstColumnSequenceType);
				id.setFlag(IBaseColumn.UUID_COLUMN, keyType.isUUID());
				id.setFlag(IBaseColumn.NATIVE_COLUMN, keyType.isNative());
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
	}

	private int getDefaultFirstColumnSequenceType(ITable table)
	{
		if (table instanceof AbstractMemTable)
		{
			return ColumnInfo.NO_SEQUENCE_SELECTED;
		}
		return new DesignerPreferences().getPrimaryKeySequenceType();
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public static class ColumnTypeComparator implements Comparator<Column>
	{
		public static final ColumnTypeComparator INSTANCE = new ColumnTypeComparator();

		public int compare(Column column1, Column column2)
		{
			return Column.getDisplayTypeString(column1.getConfiguredColumnType().getSqlType()).compareToIgnoreCase(
				Column.getDisplayTypeString(column2.getConfiguredColumnType().getSqlType()));
		}
	}

	public static class ColumnLengthComparator implements Comparator<Column>
	{
		public static final ColumnLengthComparator INSTANCE = new ColumnLengthComparator();

		public int compare(Column column1, Column column2)
		{
			int length1 = column1.getLength();
			int length2 = column2.getLength();
			if (length1 > length2) return 1;
			if (length1 == length2) return 0;
			return -1;
		}
	}

	public static class ColumnAllowNullComparator implements Comparator<Column>
	{
		public static final ColumnAllowNullComparator INSTANCE = new ColumnAllowNullComparator();

		public int compare(Column column1, Column column2)
		{
			boolean allowNull1 = column1.getAllowNull();
			boolean allowNull2 = column2.getAllowNull();
			if (!allowNull1 & allowNull2) return 1;
			if (allowNull1 & !allowNull2) return -1;
			return 0;
		}
	}

	public static class ColumnRowIdentComparator implements Comparator<Column>
	{
		public static final ColumnRowIdentComparator INSTANCE = new ColumnRowIdentComparator();

		public int compare(Column column1, Column column2)
		{
			return Column.getFlagsString(column1.getRowIdentType()).compareToIgnoreCase(Column.getFlagsString(column2.getRowIdentType()));
		}
	}

	public void checkValidState() throws RepositoryException
	{
		if (columnDetailsComposite != null)
		{
			columnDetailsComposite.checkValidState();
		}
		if (nameEditor != null)
		{
			nameEditor.checkValidState();
		}
	}

	public boolean shouldDropTable()
	{
		return colomnAllowNullEditingSupport.getAndResetDropTable();
	}

	public boolean isDataProviderIdDisplayed()
	{
		return displayDataProviderID.getSelection();
	}

	public static String[] getSeqDisplayTypeStrings(ITable table)
	{
		String[] comboSeqTypes = new String[0];

		try
		{
			List<String> seqType = new ArrayList<String>();

			IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(table.getServerName());
			for (int element : ColumnInfo.allDefinedSeqTypes)
			{
				boolean validType = true;
				if (table instanceof AbstractMemTable)
				{
					if (element == ColumnInfo.SERVOY_SEQUENCE || element == ColumnInfo.DATABASE_SEQUENCE)
					{
						validType = false;
					}
				}
				else if (element != ColumnInfo.NO_SEQUENCE_SELECTED && element != ColumnInfo.SERVOY_SEQUENCE &&
					!server.supportsSequenceType(element, null/*
																 * TODO: add current selected column
																 */))
				{
					validType = false;
				}

				if (element == ColumnInfo.SERVOY_SEQUENCE)
				{
					validType = false;
					List<Column> pkColumns = table.getRowIdentColumns();
					if (pkColumns != null && pkColumns.size() > 0)
					{
						for (Column column : pkColumns)
						{
							if (column.getColumnInfo() != null && column.getColumnInfo().getAutoEnterType() == ColumnInfo.SEQUENCE_AUTO_ENTER &&
								column.getColumnInfo().getAutoEnterSubType() == ColumnInfo.SERVOY_SEQUENCE)
							{
								// only show if already set
								validType = true;
								break;
							}
						}
					}
				}
				if (validType)
				{
					seqType.add(ColumnInfo.getSeqDisplayTypeString(element));
				}
			}

			comboSeqTypes = new String[seqType.size()];
			Object[] oType = seqType.toArray();
			for (int i = 0; i < oType.length; i++)
			{
				comboSeqTypes[i] = oType[i].toString();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		return comboSeqTypes;
	}
}
