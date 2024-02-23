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

import static com.servoy.base.query.IBaseSQLCondition.EQUALS_OPERATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
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

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ScriptingUtils;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.StringMatcher;
import com.servoy.eclipse.ui.editors.relation.DataProviderEditingSupport;
import com.servoy.eclipse.ui.editors.relation.DatasourceSelectComposite;
import com.servoy.eclipse.ui.editors.relation.OperatorEditingSupport;
import com.servoy.eclipse.ui.editors.relation.OperatorMaskEditingSupport;
import com.servoy.eclipse.ui.editors.relation.OptionsComposite;
import com.servoy.eclipse.ui.editors.relation.RelationItemLabelProvider;
import com.servoy.eclipse.ui.editors.relation.RelationRow;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.SWTFakeTableTooltipSupport;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Utils;

public class RelationEditor extends PersistEditor implements IItemChangeListener<IColumn>
{
	public static int NUMBER_VISIBLE_ITEMS = 10;

	public final static String EMPTY = "";//Leave the spaces!!
	private final static String SEPARATOR = "-";

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
		myScrolledComposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");
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

		datasourceSelectComposite = new DatasourceSelectComposite(comp, SWT.NONE);

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
						WritableList<RelationRow> oldInput = (WritableList<RelationRow>)tableViewer.getInput();
						oldInput.remove(index);
						oldInput.add(new RelationRow(null, EQUALS_OPERATOR, null, null));
						tableViewer.setInput(oldInput);
						flagModified(true);
					}
				}
			}
		});

		optionsComposite = new OptionsComposite(comp, SWT.NONE);

		final GroupLayout groupLayout = new GroupLayout(comp);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 498,
				Short.MAX_VALUE).add(GroupLayout.LEADING, optionsComposite, GroupLayout.PREFERRED_SIZE, 498, Short.MAX_VALUE).add(GroupLayout.LEADING,
					datasourceSelectComposite, GroupLayout.PREFERRED_SIZE, 498, Short.MAX_VALUE)
				.add(GroupLayout.LEADING,
					groupLayout.createSequentialGroup().add(nameLabel).addPreferredGap(LayoutStyle.RELATED).add(nameField, GroupLayout.DEFAULT_SIZE, 417,
						Short.MAX_VALUE)))
			.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(nameLabel).add(nameField,
				GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
					datasourceSelectComposite)
				.addPreferredGap(LayoutStyle.RELATED).add(tableContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					Short.MAX_VALUE)
				.addPreferredGap(LayoutStyle.RELATED).add(optionsComposite).addContainerGap()));
		comp.setLayout(groupLayout);

		createTableColumns();

		if (getPersist() != null) initDataBindings();

		myScrolledComposite.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private List<RelationRow> input;

	public RelationRow getRowInput(int index)
	{
		return input.get(index);
	}

	@Override
	protected void init()
	{
		if (getPersist() != null) initDataBindings();
	}

	protected void initDataBindings()
	{
		datasourceSelectComposite.initDataBindings(this,
			datasourceSelectComposite.new NameFiller(nameField, getRelation().getPrimaryTableName(), getRelation().getForeignTableName()));
		optionsComposite.initDataBindings(this);

		m_bindingContext = BindingHelper.dispose(m_bindingContext);
		IObservableValue nameFieldTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(nameField);
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
		registerListeners();
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
		input = new ArrayList<>();
		WritableList<RelationRow> oldInput = (WritableList<RelationRow>)tableViewer.getInput();
		List<IPersist> items = getRelation().getAllObjectsAsList();
		for (Object element : items)
		{
			RelationItem persist = (RelationItem)element;
			RelationRow row = null;
			if (oldInput != null) row = oldInput.get(items.indexOf(element));
			String ci_from;
			if (reuseSource && row != null)
			{
				ci_from = row.getRawCIFrom();
			}
			else
			{
				// parse as scopes string so globals.x gets changes to scopes.globals.x
				ci_from = ScopesUtils.getScopeString(ScopesUtils.getVariableScope(persist.getPrimaryDataProviderID()));
			}
			String ci_to;
			if (reuseDestination && row != null) ci_to = row.getCITo();
			else ci_to = persist.getForeignColumnName();
			input.add(new RelationRow(ci_from, persist.getOperator(), ci_to, null));
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
						RelationRow row = oldInput.get(i);
						if (reuseSource)
						{
							input.add(new RelationRow(row.getRawCIFrom(), EQUALS_OPERATOR, null, null));
							continue;
						}
						else if (reuseDestination)
						{
							if (row.getCIFrom() != null && oldColumns != null &&
								(ScopesUtils.isVariableScope(row.getCIFrom()) || row.getRawCIFrom().startsWith(LiteralDataprovider.LITERAL_PREFIX)))
							{
								input.add(new RelationRow(row.getRawCIFrom(), EQUALS_OPERATOR, row.getCITo(), null));
							}
							else input.add(new RelationRow(null, EQUALS_OPERATOR, row.getCITo(), null));
							continue;
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
			input.add(new RelationRow(null, EQUALS_OPERATOR, null, null));
		}
		boolean didAutoFill = false;
		if (autoFill && getRelation().getPrimaryDataSource() != null && getRelation().getForeignDataSource() != null)
		{
			RelationRow firstRow = input.get(0);
			try
			{
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				ITable primaryTable = dsm.getDataSource(getRelation().getPrimaryDataSource());
				ITable foreignTable = dsm.getDataSource(getRelation().getForeignDataSource());
				if (primaryTable != null && foreignTable != null)
				{
					if (!reuseSource)
					{
						for (Column c : primaryTable.getColumns())
						{
							ColumnInfo ci = c.getColumnInfo();
							if (ci != null && foreignTable.getName().equalsIgnoreCase(ci.getForeignType()))
							{
								firstRow.setCIFrom(c.getDataProviderID());
								didAutoFill = true;
								break;
							}
						}
					}
					if (!reuseDestination)
					{
						for (Column c : foreignTable.getColumns())
						{
							ColumnInfo ci = c.getColumnInfo();
							if (ci != null && primaryTable.getName().equalsIgnoreCase(ci.getForeignType()))
							{
								firstRow.setCITo(getDataProvidersIndex(CI_TO, c.getDataProviderID()));
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
		if (autoFill && !didAutoFill && !reuseSource && getRelation().getPrimaryDataSource() != null)
		{
			try
			{
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				ITable primaryTable = dsm.getDataSource(getRelation().getPrimaryDataSource());
				if (primaryTable != null)
				{
					// pk auto fill
					List<Column> rowIdentColumns = primaryTable.getRowIdentColumns();
					if (rowIdentColumns != null)
					{
						for (Column column : rowIdentColumns)
						{
							if (input.size() > rowIdentColumns.indexOf(column))
							{
								int index = rowIdentColumns.indexOf(column);
								if (input.get(index).getCIFrom() == null || !(ScopesUtils.isVariableScope(input.get(index).getCIFrom()) ||
									input.get(index).getRawCIFrom().startsWith(LiteralDataprovider.LITERAL_PREFIX)))
								{
									input.get(index).setCIFrom(column.getDataProviderID());
								}
							}
						}
					}
					// name auto fill
					for (RelationRow row : input)
					{
						if (row.getCITo() != null)
						{
							String columnName = row.getCITo();
							Column column = primaryTable.getColumn(columnName);
							if (column != null) row.setCIFrom(column.getDataProviderID());
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
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				ITable foreignTable = dsm.getDataSource(getRelation().getForeignDataSource());
				if (foreignTable != null)
				{
					for (RelationRow row : input)
					{
						if (row.getCIFrom() != null)
						{
							String columnName = row.getCIFrom();
							Column column = foreignTable.getColumn(columnName);
							if (column != null) row.setCITo(getDataProvidersIndex(CI_TO, column.getDataProviderID()));
						}
					}

				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		tableViewer.setInput(new WritableList<>(input, null));

		toCache = null;
	}

	public static final int CI_FROM = 0;
	public static final int CI_OP = 1;
	public static final int CI_MASK = 2;
	public static final int CI_TO = 3;
	public static final int CI_DELETE = 4;

	private void createTableColumns()
	{
		List<String> columnTooltips = new ArrayList<>();

		TableColumn fromColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_FROM);
		fromColumn.setText("From");
		TableViewerColumn fromViewerColumn = new TableViewerColumn(tableViewer, fromColumn);
		fromViewerColumn.setEditingSupport(new DataProviderEditingSupport(this, tableViewer, CI_FROM, true));
		columnTooltips.add("Ctrl+click in a cell to open data provider dialog");

		TableColumn opColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_OP);
		opColumn.setText("Op");
		TableViewerColumn opViewerColumn = new TableViewerColumn(tableViewer, opColumn);
		opViewerColumn.setEditingSupport(new OperatorEditingSupport(this, tableViewer));
		columnTooltips.add("Relation operator");

		TableColumn maskColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_MASK);
		maskColumn.setText("Modifier");
		TableViewerColumn maskViewerColumn = new TableViewerColumn(tableViewer, maskColumn);
		maskViewerColumn.setEditingSupport(new OperatorMaskEditingSupport(this, tableViewer));
		columnTooltips.add(OperatorMaskEditingSupport.getTooltip());

		TableColumn toColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_TO);
		toColumn.setText("To");
		TableViewerColumn toViewerColumn = new TableViewerColumn(tableViewer, toColumn);
		toViewerColumn.setEditingSupport(new DataProviderEditingSupport(this, tableViewer, CI_TO, false));
		columnTooltips.add("Ctrl+click in a cell to open data provider dialog");

		TableColumn delColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);
		columnTooltips.add("Clear row");

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(fromColumn, new ColumnWeightData(10, 45, true));
		layout.setColumnData(opColumn, new ColumnPixelData(100, true));
		layout.setColumnData(maskColumn, new ColumnPixelData(100, true));
		layout.setColumnData(toColumn, new ColumnWeightData(10, 45, true));
		layout.setColumnData(delColumn, new ColumnPixelData(20, true));

		SWTFakeTableTooltipSupport.enableTooltipFor(tableViewer.getTable(), null, columnTooltips.toArray(new String[0]));
		tableViewer.setLabelProvider(new RelationItemLabelProvider(this));
	}

	public boolean canEdit(Object obj)
	{
		return canEditIndex(input.indexOf(obj));
	}

	public int getIndex(Object obj)
	{
		return input.indexOf(obj);
	}

	public boolean canEditIndex(int idx)
	{
		return idx >= 0 && getRowCount() >= idx;
	}

	private int getRowCount()
	{
		int row = 0;
		outer : for (; row < input.size(); row++)
		{
			RelationRow rr = input.get(row);
			String from = rr.getCIFrom();
			if (from == null || "".equals(from)) break outer;
			String to = rr.getCITo();
			if (to == null || "".equals(to)) break outer;
		}
		return row;
	}

	public String getDataProvidersIndex(int index, String s)
	{
		if (s == null || index < 0) return null;
		if (s.startsWith(LiteralDataprovider.LITERAL_PREFIX)) return s;
		String[] dps = getDataProviders(index);
		if (dps != null)
		{
			for (String dp : dps)
			{
				if (dp.equals(s))
				{
					return dp;
				}
			}
		}
		return null;
	}

	public String getDataProvidersIndexLoose(String[] dps, String s)
	{
		if (s == null) return null;
		if (dps != null)
		{
			// Try perfect match first.
			for (String dp : dps)
			{
				if (dp.equals(s))
				{
					return dp;
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
			if (bestLen > 0) return dps[bestIndex];
		}
		return null;
	}

	private Pair<String, String[]> fromCache;
	private Pair<String, String[]> toCache;

	public String[] getDataProviders(int index)
	{
		if (index == CI_FROM && fromCache != null && fromCache.getLeft().equals(getRelation().getPrimaryTableName()))
		{
			return fromCache.getRight();
		}
		if (index == CI_TO && toCache != null && toCache.getLeft().equals(getRelation().getForeignTableName()))
		{
			return toCache.getRight();
		}

		Object[] dps = getDataProvidersEx(index);
		String[] retval = new String[dps.length];
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

		return retval;
	}

	private Object[] getDataProvidersEx(int index)
	{
		final List<Object> retval = new ArrayList<Object>();
		retval.add(EMPTY);
		ITable t = null;
		Map<String, ScriptCalculation> calcs = null;
		FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(getPersist());
		if (index == CI_FROM)
		{
			t = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(getRelation().getPrimaryDataSource());
			calcs = new LinkedHashMap<String, ScriptCalculation>();
			Iterator<ScriptCalculation> calcsIt = fs.getScriptCalculations(t, true);
			while (calcsIt.hasNext())
			{
				ScriptCalculation calc = calcsIt.next();
				calcs.put(calc.getDataProviderID(), calc);
			}
		}
		else if (index == CI_TO)
		{
			t = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(getRelation().getForeignDataSource());
		}
		if (t != null)
		{
			Iterator<Column> cols = EditorUtil.getTableColumns(t, false);
			while (cols.hasNext())
			{
				// stored calcs are shown in calculations section
				IColumn col = cols.next();
				if ((calcs == null || !calcs.containsKey(col.getDataProviderID())) &&
					((col.getFlags() & IBaseColumn.EXCLUDED_COLUMN) != IBaseColumn.EXCLUDED_COLUMN))
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
				retval.addAll(calcs.values());
			}
			Iterator<ScriptVariable> globs = fs.getScriptVariables(true);
			if (globs.hasNext())
			{
				retval.add(SEPARATOR);
			}
			while (globs.hasNext())
			{
				final ScriptVariable global = globs.next();
				if (global.isEnum())
				{
					retval.addAll(ScriptingUtils.getEnumDataProviders(global));
				}
				else
				{
					retval.add(global);
				}
			}
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
			input.add(new RelationRow(null, EQUALS_OPERATOR, null, null));
			tableViewer.setInput(new WritableList<>(input, null));
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
		if (getSite().getPage().isPartVisible(this))
		{
			// make sure cell editors loose focus so that value is applied
			getSite().getShell().forceFocus();
		}
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
		if (object == null || "".equals(object)) return true;
		if (object instanceof Integer && ((Integer)object).intValue() == 0) return true;
		return false;
	}

	private void checkInconsistency() throws RepositoryException
	{
		datasourceSelectComposite.checkInconsistency();

		if ((getRelation().getPrimaryDataSource() == null) && isEmptySelection(input.get(0).getCIFrom()))
		{
			throw new RepositoryException("Source server data not specified.");
		}
		if (getRelation().getForeignDataSource() == null)
		{
			throw new RepositoryException("Destination server data not specified.");
		}
		for (RelationRow rr : input)
		{
			if (isEmptySelection(rr.getCIFrom()) && !isEmptySelection(rr.getCITo()))
			{
				throw new RepositoryException("Column data not complete.");
			}
			if (!isEmptySelection(rr.getCIFrom()) && isEmptySelection(rr.getCITo()))
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
			FlattenedSolution relationFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(r,
				ServoyModelFinder.getServoyModel().getFlattenedSolution());
			int count = getRowCount();
			if (count > 0)
			{
				IDataProvider[] dbp = new IDataProvider[count];
				Column[] dbf = new Column[count];
				int[] operators = new int[count];

				for (int i = 0; i < count; i++)
				{
					RelationRow rr = input.get(i);

					Object[] foreigns = getDataProvidersEx(CI_TO);
					int foreign_index = Arrays.asList(getDataProviders(CI_TO)).indexOf(rr.getCITo());
					Object foreign = foreigns[foreign_index];
					if (foreign instanceof IDataProvider)
					{
						dbf[i] = (Column)foreign;
					}

					Object[] primaries = getDataProvidersEx(CI_FROM);
					int primary_index = Arrays.asList(getDataProviders(CI_FROM)).indexOf(rr.getCIFrom());
					if (primary_index == -1)
					{
						if (ScopesUtils.isVariableScope(rr.getCIFrom()))
						{
							// match with foreign key type
							dbp[i] = new EnumDataProvider(rr.getCIFrom(), dbf[i] != null ? dbf[i].getType() : 0);
						}
						else
						{
							dbp[i] = new LiteralDataprovider(rr.getCIFrom());
						}
					}
					else
					{
						Object primary = primaries[primary_index];
						if (primary instanceof IDataProvider)
						{
							dbp[i] = (IDataProvider)primary;
						}
					}

					operators[i] = rr.getOperator();

				}
				r.createNewRelationItems(dbp, operators, dbf);
			}
			else
			{
				Object[] items = r.getAllObjectsAsList().toArray();
				for (Object element : items)
				{
					r.removeChild((IPersist)element);
				}
				if (items.length > 0) r.flagChanged();
			}
			String oldDataSource = r.getPrimaryDataSource();
			if (oldDataSource == null)
			{
				r.setPrimaryDataSource(r.getForeignDataSource());
			}
			String errorMessage = r.checkKeyTypes(relationFlattenedSolution);
			if (errorMessage != null && oldDataSource == null)
			{
				r.setPrimaryDataSource(oldDataSource);
			}
			if (errorMessage == null && oldDataSource == null)
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
	public void autoFill(RelationRow pi, int index)
	{
		try
		{
			ITable table = null;
			String from = null;
			String to = null;

			if (index == CI_FROM)
			{
				table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(getRelation().getForeignDataSource());
				from = pi.getCIFrom();
				to = pi.getCITo();
			}
			else if (index == CI_TO)
			{
				table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(getRelation().getPrimaryDataSource());
				from = pi.getCITo();
				to = pi.getCIFrom();
			}

			if (table == null) return;

			if (from != null && (to == null || "".equals(to)))
			{
				String[] toColumns = getDataProviders(index == CI_FROM ? CI_TO : CI_FROM);
				if (index == CI_FROM) pi.setCITo(getDataProvidersIndexLoose(toColumns, from));
				else pi.setCIFrom(getDataProvidersIndexLoose(toColumns, from));
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void persistChanges(Collection<IPersist> changes)
	{
		super.persistChanges(changes);
		for (IPersist changed : changes)
		{
			if (changed instanceof ScriptVariable && ScopesUtils.isVariableScope(((ScriptVariable)changed).getDataProviderID()))
			{
				fromCache = null;
				break;
			}
		}
	}

	public void itemCreated(IColumn column)
	{
		refreshTable(column);
	}

	public void itemRemoved(IColumn column)
	{
		refreshTable(column);
	}

	public void itemChanged(IColumn column)
	{
		itemChanged(Collections.singletonList(column));
	}

	public void itemChanged(Collection<IColumn> columns)
	{
		if (columns != null && columns.size() > 0)
		{
			refreshTable(columns.iterator().next());
		}
	}

	private void refreshTable(IColumn column)
	{
		try
		{
			if (column.getTable().getName().equals(getRelation().getPrimaryTableName()))
			{
				fromCache = null;
			}
			if (column.getTable().getName().equals(getRelation().getForeignTableName()))
			{
				toCache = null;
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	@Override
	public void dispose()
	{
		super.dispose();
		unregisterListeners();
	}

	public void registerListeners()
	{
		ITable primaryTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(getRelation().getPrimaryDataSource());
		if (primaryTable != null)
		{
			primaryTable.addIColumnListener(this);
		}
		ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(getRelation().getForeignDataSource());
		if (foreignTable != null && !Utils.equalObjects(primaryTable, foreignTable))
		{
			foreignTable.addIColumnListener(this);
		}
	}

	public void unregisterListeners()
	{
		Relation relation = getRelation();
		if (relation != null)
		{
			ITable primaryTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(relation.getPrimaryDataSource());
			if (primaryTable != null)
			{
				primaryTable.removeIColumnListener(this);
			}
			ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(relation.getForeignDataSource());
			if (foreignTable != null)
			{
				foreignTable.removeIColumnListener(this);
			}
		}
	}

	@Override
	protected String getContextId()
	{
		return "com.servoy.eclipse.ui.create_relation";
	}
}
