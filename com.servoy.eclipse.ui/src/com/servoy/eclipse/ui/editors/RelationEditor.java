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
package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.StringMatcher;
import com.servoy.eclipse.ui.editors.relation.DataProviderEditingSupport;
import com.servoy.eclipse.ui.editors.relation.DatasourceSelectComposite;
import com.servoy.eclipse.ui.editors.relation.OperatorEditingSupport;
import com.servoy.eclipse.ui.editors.relation.OptionsComposite;
import com.servoy.eclipse.ui.editors.relation.RelationItemLabelProvider;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

public class RelationEditor extends PersistEditor
{
	public static int NUMBER_VISIBLE_ITEMS = 10;

	public final static String EMPTY = "";//Leave the spaces!! //$NON-NLS-1$
	private final static String SEPARATOR = "-"; //$NON-NLS-1$

	private TableViewer tableViewer;
	private Text nameField;
	private DataBindingContext m_bindingContext;

	private DatasourceSelectComposite datasourceSelectComposite;
	private OptionsComposite optionsComposite;
	private Composite tableContainer;
	private int tableRows;

	@Override
	protected boolean validatePersist(IPersist persist)
	{
		return persist instanceof Relation;
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		tableRows = 10;

		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite comp = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(comp);

		Label nameLabel;
		nameLabel = new Label(comp, SWT.NONE);
		nameLabel.setText("Relation name");

		nameField = new Text(comp, SWT.BORDER);

		Relation relation = (Relation)getPersist();
		if (relation != null)
		{
			nameField.setText(relation.getName());
			if (relation.getAllObjectsAsList().size() >= tableRows)
			{
				tableRows = relation.getAllObjectsAsList().size() + 1;
			}
		}
		nameField.addVerifyListener(new VerifyListener()
		{
			public void verifyText(VerifyEvent e)
			{
				DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER.verifyText(e);
				// allow only lower case relation names at design time in developer to avoid easy-to-make-mistakes
				// relation names can have mixed case when created with solution model
				if (e.doit && e.text != null) e.text = Utils.toEnglishLocaleLowerCase(e.text);
			}
		});

		datasourceSelectComposite = new DatasourceSelectComposite(comp, SWT.NONE, getRelation());

		tableContainer = new Composite(comp, SWT.NONE);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		final Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableViewerEditor.create(tableViewer, new TableViewerFocusCellManager(tableViewer, new FocusCellOwnerDrawHighlighter(tableViewer)),
			new ColumnViewerEditorActivationStrategy(tableViewer)
			{

				/**
				 * @see org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy#isEditorActivationEvent(org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent)
				 */
				@Override
				protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event)
				{
					if (super.isEditorActivationEvent(event))
					{
						return true;
					}
					return event.keyCode == '\r';
				}
			}, ColumnViewerEditor.KEYBOARD_ACTIVATION | ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR);
		table.setToolTipText("Ctrl+click in a cell to open data provider dialog");

		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent event)
			{
				Point pt = new Point(event.x, event.y);
				TableItem item = table.getItem(pt);
				if (item != null && item.getBounds(CI_DELETE).contains(pt) && (!"".equals(item.getText(CI_FROM)) || !"".equals(item.getText(CI_TO))))
				{
					List<TableItem> items = Arrays.asList(table.getItems());
					int index = items.indexOf(item);
					if (index >= 0)
					{
						WritableList oldInput = (WritableList)tableViewer.getInput();
						oldInput.remove(index);
						oldInput.add(new Integer[] { null, 0, null, null });
						tableViewer.setInput(oldInput);
						flagModified(true);
					}
				}
			}
		});

		optionsComposite = new OptionsComposite(comp, SWT.NONE);

		final GroupLayout groupLayout = new GroupLayout(comp);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 498, Short.MAX_VALUE).add(
					GroupLayout.LEADING, optionsComposite, GroupLayout.PREFERRED_SIZE, 498, Short.MAX_VALUE).add(GroupLayout.LEADING,
					datasourceSelectComposite, GroupLayout.PREFERRED_SIZE, 498, Short.MAX_VALUE).add(
					GroupLayout.LEADING,
					groupLayout.createSequentialGroup().add(nameLabel).addPreferredGap(LayoutStyle.RELATED).add(nameField, GroupLayout.DEFAULT_SIZE, 417,
						Short.MAX_VALUE))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(nameLabel).add(nameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(datasourceSelectComposite).addPreferredGap(LayoutStyle.RELATED).add(
				tableContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(
				optionsComposite).addContainerGap()));
		comp.setLayout(groupLayout);

		createTableColumns();

		if (getPersist() != null) initDataBindings();

		myScrolledComposite.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private List<Object[]> input;

	public Object getRowInput(int index)
	{
		return input.get(index);
	}

	protected void initDataBindings()
	{
		datasourceSelectComposite.initDataBindings(this);
		optionsComposite.initDataBindings(this);

		m_bindingContext = BindingHelper.dispose(m_bindingContext);
		IObservableValue nameFieldTextObserveWidget = SWTObservables.observeText(nameField, SWT.Modify);
		IObservableValue getRelationNameObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				return getRelation().getName();
			}

			@Override
			protected void doSetValue(Object value)
			{
				//setName cannot be invoked, save does update name
				getRelation().flagChanged();
			}
		};
		m_bindingContext = new DataBindingContext();
		m_bindingContext.bindValue(nameFieldTextObserveWidget, getRelationNameObserveValue, null, null);

		ObservableListContentProvider columnViewContentProvider = new ObservableListContentProvider();
		tableViewer.setContentProvider(columnViewContentProvider);
		BindingHelper.addGlobalChangeListener(m_bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				flagModified(false);
			}
		});
		createInput(false, false, false);
	}

	@Override
	protected void doRefresh()
	{
		datasourceSelectComposite.refresh(this);
		refreshOptions();

		m_bindingContext.updateTargets();
	}

	public void refreshOptions()
	{
		optionsComposite.refresh();
	}

	public void createInput(boolean reuseSource, boolean reuseDestination, boolean autoFill)
	{
		input = new ArrayList<Object[]>();
		WritableList oldInput = (WritableList)tableViewer.getInput();
		List<IPersist> items = getRelation().getAllObjectsAsList();
		for (Object element : items)
		{
			RelationItem persist = (RelationItem)element;
			Integer[] row = null;
			if (oldInput != null) row = (Integer[])oldInput.get(items.indexOf(element));
			Integer ci_from;
			if (reuseSource && row != null) ci_from = row[0];
			else ci_from = getDataProvidersIndex(CI_FROM, persist.getPrimaryDataProviderID());
			Integer ci_to;
			if (reuseDestination && row != null) ci_to = row[2];
			else ci_to = getDataProvidersIndex(CI_TO, persist.getForeignColumnName());
			input.add(new Integer[] { ci_from, new Integer(persist.getOperator()), ci_to, null });
		}
		String[] oldColumns = null;
		if (fromCache != null) oldColumns = fromCache.getRight();
		for (int i = input.size(); i < tableRows; i++)
		{
			if ((items == null || items.size() == 0) && tableViewer.getInput() != null)
			{
				try
				{
					if (oldInput.size() > i)
					{
						Integer[] row = (Integer[])oldInput.get(i);
						if (reuseSource)
						{
							input.add(new Integer[] { row[0], 0, null, null });
							continue;
						}
						else if (reuseDestination)
						{
							if (row[0] != null && oldColumns != null && row[0] < oldColumns.length &&
								oldColumns[row[0]].startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
							{
								String name = oldColumns[row[0]];
								Integer fromIndex = null;
								String[] newColumns = getDataProviders(CI_FROM);
								for (int j = 0; j < newColumns.length; j++)
								{
									if (newColumns[j].equals(name)) fromIndex = j;
								}
								input.add(new Integer[] { fromIndex, 0, row[2], null });
							}
							else input.add(new Integer[] { null, 0, row[2], null });
							continue;
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
			input.add(new Integer[] { null, 0, null, null });
		}
		boolean didAutoFill = false;
		if (autoFill && getRelation().getPrimaryServerName() != null && getRelation().getForeignServerName() != null)
		{
			Integer[] firstRow = (Integer[])input.get(0);
			try
			{
				com.servoy.j2db.persistence.Table primaryTable = getRelation().getPrimaryTable();
				com.servoy.j2db.persistence.Table foreignTable = getRelation().getForeignTable();
				if (primaryTable != null && foreignTable != null)
				{
					if (!reuseSource)
					{
						Iterator<Column> it = primaryTable.getColumns().iterator();
						while (it.hasNext())
						{
							Column c = it.next();
							ColumnInfo ci = c.getColumnInfo();
							if (ci != null && foreignTable.getName().equalsIgnoreCase(ci.getForeignType()))
							{
								firstRow[0] = getDataProvidersIndex(CI_FROM, c.getDataProviderID());
								didAutoFill = true;
								break;
							}
						}
					}
					if (!reuseDestination)
					{
						Iterator<Column> it = foreignTable.getColumns().iterator();
						while (it.hasNext())
						{
							Column c = it.next();
							ColumnInfo ci = c.getColumnInfo();
							if (ci != null && primaryTable.getName().equalsIgnoreCase(ci.getForeignType()))
							{
								firstRow[2] = getDataProvidersIndex(CI_TO, c.getDataProviderID());
								didAutoFill = true;
								break;
							}
						}
					}
					input.remove(0);
					input.add(0, firstRow);
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		if (autoFill && !didAutoFill && !reuseSource && getRelation().getPrimaryServerName() != null)
		{
			try
			{
				com.servoy.j2db.persistence.Table primaryTable = getRelation().getPrimaryTable();
				if (primaryTable != null)
				{
					// pk auto fill
					List<Column> rowIdentColumns = primaryTable.getRowIdentColumns();
					if (rowIdentColumns != null)
					{
						String[] columns = getDataProviders(CI_FROM);
						for (Column column : rowIdentColumns)
						{
							if (input.size() > rowIdentColumns.indexOf(column))
							{
								int index = rowIdentColumns.indexOf(column);
								if (input.get(index)[CI_FROM] == null) input.get(index)[CI_FROM] = getDataProvidersIndex(CI_FROM, column.getDataProviderID());
								else
								{
									int valueIndex = Integer.parseInt(input.get(index)[CI_FROM].toString());
									String dataProvider = columns[valueIndex];
									if (!dataProvider.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
									{
										input.get(index)[CI_FROM] = getDataProvidersIndex(CI_FROM, column.getDataProviderID());
									}
								}
							}
						}
					}
					// name auto fill
					String[] columns = getDataProviders(CI_TO);
					for (Object[] row : input)
					{
						if (row[CI_TO] != null)
						{
							int index = Integer.parseInt(row[CI_TO].toString());
							String columnName = columns[index];
							Column column = primaryTable.getColumn(columnName);
							if (column != null) input.get(input.indexOf(row))[CI_FROM] = getDataProvidersIndex(CI_FROM, column.getDataProviderID());
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		if (autoFill && !didAutoFill && !reuseDestination && getRelation().getPrimaryServerName() != null && getRelation().getForeignServerName() != null)
		{
			try
			{
				com.servoy.j2db.persistence.Table foreignTable = (com.servoy.j2db.persistence.Table)getRelation().getForeignServer().getTable(
					getRelation().getForeignTableName());
				if (foreignTable != null)
				{
					String[] columns = getDataProviders(CI_FROM);
					for (Object[] row : input)
					{
						if (row[CI_FROM] != null)
						{
							int index = Integer.parseInt(row[CI_FROM].toString());
							String columnName = columns[index];
							Column column = foreignTable.getColumn(columnName);
							if (column != null) input.get(input.indexOf(row))[CI_TO] = getDataProvidersIndex(CI_TO, column.getDataProviderID());
						}
					}

				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		tableViewer.setInput(new WritableList(input, null));

		toCache = null;
	}

	public static final int CI_FROM = 0;
	public static final int CI_OP = 1;
	public static final int CI_TO = 2;
	public static final int CI_DELETE = 3;

	private void createTableColumns()
	{
		TableColumn fromColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_FROM);
		fromColumn.setText("From");
		TableViewerColumn fromViewerColumn = new TableViewerColumn(tableViewer, fromColumn);
		fromViewerColumn.setEditingSupport(new DataProviderEditingSupport(this, tableViewer, CI_FROM));

		TableColumn opColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_OP);
		opColumn.setText("op");
		TableViewerColumn opViewerColumn = new TableViewerColumn(tableViewer, opColumn);
		opViewerColumn.setEditingSupport(new OperatorEditingSupport(this, tableViewer, CI_OP));

		TableColumn toColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_TO);
		toColumn.setText("To");
		TableViewerColumn toViewerColumn = new TableViewerColumn(tableViewer, toColumn);
		toViewerColumn.setEditingSupport(new DataProviderEditingSupport(this, tableViewer, CI_TO));

		TableColumn delColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);
		TableViewerColumn delViewerColumn = new TableViewerColumn(tableViewer, delColumn);
		delColumn.setToolTipText("Clear row");

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(fromColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(opColumn, new ColumnPixelData(100, true));
		layout.setColumnData(toColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(delColumn, new ColumnPixelData(20, true));

		tableViewer.setLabelProvider(new RelationItemLabelProvider(this));
	}

	public boolean canEdit(Object obj)
	{
		int idx = input.indexOf(obj);
		return canEditIndex(idx);
	}

	public int getIndex(Object obj)
	{
		return input.indexOf(obj);
	}

	public boolean canEditIndex(int idx)
	{
		if (idx >= 0 && getRowCount() >= idx)
		{
			return true;
		}
		return false;
	}

	private int getRowCount()
	{
		int row = 0;
		outer : for (; row < input.size(); row++)
		{
			Object[] array = input.get(row);
			for (int col = 0; col < 3; col++)
			{
				Object obj = array[col];
				if (obj == null)
				{
					break outer;
				}

				if (obj instanceof Integer && col != 1 && ((Integer)obj).intValue() == 0)
				{
					break outer;
				}
			}
		}
		return row;
	}

	public Integer getDataProvidersIndex(int index, String s)
	{
		if (s == null || index < 0) return null;
		String[] dps = getDataProviders(index);
		if (dps != null)
		{
			for (int i = 0; i < dps.length; i++)
			{
				if (dps[i].equals(s))
				{
					return new Integer(i);
				}
			}
		}
		return null;
	}

	public Integer getDataProvidersIndexLoose(String[] dps, String s)
	{
		if (s == null) return null;
		if (dps != null)
		{
			// Try perfect match first.
			for (int i = 0; i < dps.length; i++)
			{
				if (dps[i].equals(s))
				{
					return new Integer(i);
				}
			}

			// If not found, try longest common substring which matches the shortest word.
			int bestLen = 0;
			int bestIndex = -1;
			for (int i = 0; i < dps.length; i++)
			{
				int currentLen = StringMatcher.stringMatchInLength(s, dps[i]);
				// If the common substring matches the entire shortest word, then consider this solution.
				// We'll just pick the longest solution found (heuristic).
				if ((currentLen == Math.min(dps[i].length(), s.length())) && (currentLen > bestLen))
				{
					bestLen = currentLen;
					bestIndex = i;
				}
			}
			if (bestLen > 0) return new Integer(bestIndex);
		}
		return null;
	}

	private Pair<String, String[]> fromCache;
	private Pair<String, String[]> toCache;

	public String[] getDataProviders(int index)
	{
		String[] retval = null;
		if (index == CI_FROM && fromCache != null && fromCache.getLeft().equals(getRelation().getPrimaryTableName()))
		{
			return fromCache.getRight();
		}
		else if (index == CI_TO && toCache != null && toCache.getLeft().equals(getRelation().getForeignTableName()))
		{
			return toCache.getRight();
		}
		if (retval == null)
		{
			Object[] dps = getDataProvidersEx(index);
			retval = new String[dps.length];
			for (int i = 0; i < retval.length; i++)
			{
				retval[i] = (dps[i] instanceof IDataProvider ? ((IDataProvider)dps[i]).getDataProviderID() : dps[i].toString());
			}
			if (index == CI_FROM)
			{
				fromCache = new Pair<String, String[]>(EMPTY + getRelation().getPrimaryTableName(), retval);
			}
			else if (index == CI_TO)
			{
				toCache = new Pair<String, String[]>(EMPTY + getRelation().getForeignTableName(), retval);
			}
		}
		return retval;
	}

	private Object[] getDataProvidersEx(int index)
	{
		List<Object> retval = new ArrayList<Object>();
		retval.add(EMPTY);
		try
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

			com.servoy.j2db.persistence.Table t = null;
			Map<String, ScriptCalculation> calcs = null;
			FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(getPersist());
			if (index == CI_FROM)
			{
				IServerInternal s = (IServerInternal)ServoyModel.getServerManager().getServer(getRelation().getPrimaryServerName());
				if (s != null)
				{
					t = s.getTable(getRelation().getPrimaryTableName());

					calcs = new LinkedHashMap<String, ScriptCalculation>();
					Iterator<ScriptCalculation> calcsIt = fs.getScriptCalculations(t, true);
					while (calcsIt.hasNext())
					{
						ScriptCalculation calc = calcsIt.next();
						calcs.put(calc.getDataProviderID(), calc);
					}
				}
			}
			else if (index == CI_TO)
			{
				IServerInternal s = (IServerInternal)ServoyModel.getServerManager().getServer(getRelation().getForeignServerName());
				if (s != null)
				{
					t = s.getTable(getRelation().getForeignTableName());
				}
			}
			if (t != null)
			{
				Iterator<Column> cols = EditorUtil.getTableColumns(t);
				while (cols.hasNext())
				{
					// stored calcs are shown in calculations section
					Column col = cols.next();
					if ((calcs == null || !calcs.containsKey(col.getDataProviderID())) && ((col.getFlags() & Column.EXCLUDED_COLUMN) != Column.EXCLUDED_COLUMN))
					{
						retval.add(col);
					}
				}
			}
			if (index == CI_FROM)
			{
				if (calcs != null && calcs.size() > 0)
				{
					retval.add(SEPARATOR);
					for (String dp : calcs.keySet())
					{
						retval.add(calcs.get(dp));
					}
				}
				Iterator<ScriptVariable> globs = fs.getScriptVariables(true);
				if (globs.hasNext())
				{
					retval.add(SEPARATOR);
				}
				while (globs.hasNext())
				{
					retval.add(globs.next());
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return retval.toArray();
	}

	public void flagModified(boolean modifyChilderen)
	{
		if (modifyChilderen)
		{
			Iterator<IPersist> relItems = getRelation().getAllObjects();
			boolean hasChildren = false;
			while (relItems.hasNext())
			{
				hasChildren = true;
				IPersist object = relItems.next();
				object.flagChanged();
			}
			if (!hasChildren) getRelation().flagChanged();
		}
		if (getRowCount() == tableRows)
		{
			tableRows++;
			input.add(new Integer[] { null, 0, null, null });
			tableViewer.setInput(new WritableList(input, null));
		}
		this.getSite().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
	}

	@Override
	public boolean isSaveOnCloseNeeded()
	{
		// make sure cell editors loose focus so that value is applied
		getSite().getShell().forceFocus();
		return super.isSaveOnCloseNeeded();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (isDirty())
		{
			IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			try
			{
				getRelation().updateName(validator, nameField.getText());
				updateTitle();
			}
			catch (RepositoryException e)
			{
				MessageDialog.openError(getSite().getShell(), "Error while saving", e.getMessage());
				if (monitor != null) monitor.setCanceled(true);
				return;
			}
			String message = createAndcheck();
			if (message != null)
			{
				MessageDialog.openError(getSite().getShell(), "Error while saving", message);
				if (monitor != null) monitor.setCanceled(true);
				return;
			}
		}
		super.doSave(monitor);
	}

	private boolean isEmptySelection(Object object)
	{
		if (object == null) return true;
		if (object instanceof Integer && ((Integer)object).intValue() == 0) return true;
		return false;
	}

	private void checkInconsistency() throws RepositoryException
	{
		datasourceSelectComposite.checkInconsistency();

		if ((getRelation().getPrimaryServerName() == null || getRelation().getPrimaryTableName() == null) && isEmptySelection(input.get(0)[0]))
		{
			throw new RepositoryException("Source server data not specified.");
		}
		if (getRelation().getForeignServerName() == null || getRelation().getForeignTableName() == null)
		{
			throw new RepositoryException("Destination server data not specified.");
		}
		for (int row = 0; row < input.size(); row++)
		{
			Object[] array = input.get(row);
			if (isEmptySelection(array[0]) && !isEmptySelection(array[2]))
			{
				throw new RepositoryException("Column data not complete.");
			}
			if (!isEmptySelection(array[0]) && isEmptySelection(array[2]))
			{
				throw new RepositoryException("Column data not complete.");
			}
		}
	}

	public String createAndcheck()
	{
		try
		{
			checkInconsistency();
			Relation r = getRelation();
			int count = getRowCount();
			if (count > 0)
			{
				IDataProvider[] dbp = new IDataProvider[count];
				Column[] dbf = new Column[count];
				int[] operators = new int[count];

				for (int i = 0; i < count; i++)
				{
					Integer[] array = (Integer[])input.get(i);

					Object[] primaries = getDataProvidersEx(CI_FROM);
					int primary_index = Utils.getAsInteger(array[CI_FROM]);
					Object primary = primaries[primary_index];
					if (primary instanceof IDataProvider)
					{
						dbp[i] = (IDataProvider)primary;
					}

					operators[i] = Utils.getAsInteger(array[CI_OP]);

					Object[] foreigns = getDataProvidersEx(CI_TO);
					int foreign_index = Utils.getAsInteger(array[CI_TO]);
					Object foreign = foreigns[foreign_index];
					if (foreign instanceof IDataProvider)
					{
						dbf[i] = (Column)foreign;
					}
				}
				r.createNewRelationItems(dbp, operators, dbf);
			}
			else
			{
				IPersist[] items = r.getAllObjectsAsList().toArray(new IPersist[] { });
				for (IPersist element : items)
				{
					r.removeChild(element);
				}
				if (items.length > 0) r.flagChanged();
			}
			String oldServerName = r.getPrimaryServerName();
			String oldTableName = r.getPrimaryTableName();
			if (oldServerName == null)
			{
				r.setPrimaryServerName(r.getForeignServerName());
			}
			if (oldTableName == null)
			{
				r.setPrimaryTableName(r.getForeignTableName());
			}
			String errorMessage = r.checkKeyTypes(null);
			if (errorMessage != null && (oldServerName == null || oldTableName == null))
			{
				r.setPrimaryServerName(oldServerName);
				r.setPrimaryTableName(oldTableName);
			}
			if (errorMessage == null && oldServerName == null)
			{
				IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
				getRelation().updateName(validator, r.getForeignTableName() + "_to_" + r.getForeignTableName());
				updateTitle();
			}
			return errorMessage;
		}
		catch (RepositoryException e)
		{
			return e.getMessage();
		}
	}

	public Text getNameField()
	{
		return nameField;
	}

	public Relation getRelation()
	{
		return (Relation)getPersist();
	}

	@Override
	public void setFocus()
	{
		nameField.forceFocus();
	}

	/**
	 * @param pi
	 * @param index
	 */
	public void autoFill(Integer[] pi, int index)
	{
		try
		{
			com.servoy.j2db.persistence.Table table = null;
			int from = -1;
			int to = -1;

			if (index == CI_FROM)
			{
				table = getRelation().getForeignTable();
				from = CI_FROM;
				to = CI_TO;
			}
			else if (index == CI_TO)
			{
				table = getRelation().getPrimaryTable();
				from = CI_TO;
				to = CI_FROM;
			}

			if (table == null) return;

			if (pi[from] != null && (pi[to] == null || pi[to].intValue() == 0))
			{
				String[] fromColumns = getDataProviders(from);
				int i = pi[from].intValue();
				String columnName = fromColumns[i];
				String[] toColumns = getDataProviders(to);
				pi[to] = getDataProvidersIndexLoose(toColumns, columnName);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}
}
