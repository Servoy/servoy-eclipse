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
package com.servoy.eclipse.ui.wizards;

import static com.servoy.eclipse.core.ServoyModelManager.getServoyModelManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.StringMatcher;
import com.servoy.eclipse.ui.editors.table.ColumnLabelProvider;
import com.servoy.eclipse.ui.util.IColumnComparator;
import com.servoy.eclipse.ui.util.ITableComparator;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Wizard to suggest foreign types for a column.
 *
 * @author gersze
 *
 */

public class SuggestForeignTypesWizard extends Wizard
{
	// Only scores above this threshold are displayed when matching FKs.
	private static final double MATCH_THRESHOLD = 7;

	public static final String TABLE_OR_COLUMN_NAME = "Name";
	public static final String DATATYPE = "Datatype";
	public static final String CURRENT_FOREIGN_TYPE = "Current Foreign Type";
	public static final String SUGGESTED_FOREIGN_TYPE = "Suggested Foreign Type";
	public static final String SAVE = "Save";

	public static final int TABLE_OR_COLUMN_NAME_INDEX = 0;
	public static final int DATATYPE_INDEX = 1;
	public static final int CURRENT_FOREIGN_TYPE_INDEX = 2;
	public static final int SUGGESTED_FOREIGN_TYPE_INDEX = 3;
	public static final int SAVE_INDEX = 4;

	public static final String ENTRY_NONE = "-none-";

	private final String[] columnNames = new String[] { TABLE_OR_COLUMN_NAME, DATATYPE, CURRENT_FOREIGN_TYPE, SUGGESTED_FOREIGN_TYPE, SAVE };

	private ServerSelectionPage serverSelectionPage;
	private SuggestionPage suggestionPage;

	private String[] allTableNames;
	private String[] arrayNamesWithEmptyEntry;

	private IServerInternal server = null;
	private ForeignTypeSuggestionData data;

	boolean listOnlyChangedColumns = true;
	boolean listOnlyColumnsWithoutCurrentType = true;
	boolean skipDatetimeAndMedia = true;
	boolean skipText = true;
	boolean groupByColumns = false;
	boolean hasServer;

	private String tableName = null;
	private String columnName = null;
	private IObservableValue foreignTypeForChosenColumn = null;

	public SuggestForeignTypesWizard(String serverName)
	{
		setWindowTitle("Suggest Foreign Types");
		getServoyModelManager().getServoyModel();
		server = ServoyModelFinder.getServoyModel().getDataSourceManager().getServer(serverName);
		hasServer = true;
	}

	public IObservableValue setColumnToTrace(String tableName, String columnName, String initialValue)
	{
		this.tableName = tableName;
		this.columnName = columnName;
		this.foreignTypeForChosenColumn = new WritableValue(initialValue, String.class);
		return this.foreignTypeForChosenColumn;
	}

	@Override
	public boolean performFinish()
	{
		WorkspaceJob exportJob = new WorkspaceJob("Saving foreign types")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				monitor.beginTask("Saving foreign types", allTableNames.length);

				final String answer = data.commitData(monitor);

				if (answer.length() == 0)
				{
					monitor.done();
					return Status.OK_STATUS;
				}
				else
				{
					monitor.done();
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(UIUtils.getActiveShell(), "Errors occured while saving the foreign type changes", answer);
						}
					});
					return Status.CANCEL_STATUS;
				}
			}
		};

		exportJob.setUser(true); // we want the progress to be visible in a dialog, not to stay in the status bar
		exportJob.schedule();

		return true;
	}

	public void init(@SuppressWarnings("unused") IWorkbench workbench, @SuppressWarnings("unused") IStructuredSelection selection)
	{
		if (!hasServer) serverSelectionPage = new ServerSelectionPage("serverSelection");
		suggestionPage = new SuggestionPage("suggestionPage");
	}

	@Override
	public void addPages()
	{
		if (!hasServer) this.addPage(serverSelectionPage);
		this.addPage(suggestionPage);
	}

	@Override
	public boolean canFinish()
	{
		return this.getContainer().getCurrentPage().equals(suggestionPage);
	}


	private class ServerSelectionPage extends WizardPage
	{
		private Combo serversCombo;

		protected ServerSelectionPage(String pageName)
		{
			super(pageName);
			setTitle("Select server");
		}

		public void createControl(Composite parent)
		{
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayout(new GridLayout(2, false));

			Label sourceServerLabel = new Label(topLevel, SWT.NONE);
			sourceServerLabel.setText("Server");

			serversCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(serversCombo);
			serversCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			getServoyModelManager().getServoyModel();
			String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, true, false);
			if (serverNames != null && serverNames.length > 0)
			{
				serversCombo.setItems(serverNames);
				serversCombo.select(0);
				getServoyModelManager().getServoyModel();
				server = ServoyModelFinder.getServoyModel().getDataSourceManager().getServer(serverNames[0]);

				serversCombo.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent event)
					{
						String serverName = serversCombo.getItem(serversCombo.getSelectionIndex());
						server = ServoyModelFinder.getServoyModel().getDataSourceManager().getServer(serverName);
					}
				});
			}
			else
			{
				setMessage("No servers found.", IMessageProvider.WARNING);
			}

			setControl(topLevel);
		}

		@Override
		public void setVisible(boolean visible)
		{
			if (!visible) if (suggestionPage.needsUpdate()) suggestionPage.updateContent(true);
			super.setVisible(visible);
		}
	}

	private class SuggestionPage extends WizardPage
	{
		private TreeViewer suggestionTree;
		private ComboBoxCellEditor suggestionCellEditor;

		private boolean needsUpdate;

		protected SuggestionPage(String pageName)
		{
			super(pageName);
			setTitle("Foreign Type Suggestions");
			needsUpdate = false;
		}

		public void createControl(Composite parent)
		{
			// Spacing between components. Not sure what is the 'standard' in GUI design for this.
			int space = 7;

			FormLayout flayout = new FormLayout();
			parent.setLayout(flayout);

			// First row, the label with some hints.
			Label hintsLabel = new Label(parent, SWT.WRAP);
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			hintsLabel.setLayoutData(fd);
			hintsLabel.setText(
				"Below you can see the suggestions generated for foreign types. Please review them, make any adjustments that you consider necessary, and click the \"Finish\" button when you are ready to perform the changes.");

			// Second row, buttons for grouping, expanding and collapsing.
			final Button collapseAllButton = new Button(parent, SWT.PUSH);
			collapseAllButton.setText("Collapse all");
			fd = new FormData();
			fd.top = new FormAttachment(hintsLabel, space);
			fd.right = new FormAttachment(100);
			collapseAllButton.setLayoutData(fd);
			collapseAllButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					suggestionTree.collapseAll();
					suggestionTree.getTree().setFocus();
				}
			});

			final Button expandAllButton = new Button(parent, SWT.PUSH);
			expandAllButton.setText("Expand all");
			fd = new FormData();
			fd.top = new FormAttachment(collapseAllButton, 0, SWT.TOP);
			fd.right = new FormAttachment(collapseAllButton, -space);
			expandAllButton.setLayoutData(fd);
			expandAllButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					suggestionTree.expandAll();
					suggestionTree.getTree().setFocus();
				}
			});

			Label groupingLabel = new Label(parent, SWT.WRAP);
			fd = new FormData();
			int delta = collapseAllButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y - groupingLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
			fd.top = new FormAttachment(collapseAllButton, delta / 2, SWT.TOP);
			fd.left = new FormAttachment(0);
			groupingLabel.setLayoutData(fd);
			groupingLabel.setText("Group by");

			final Button groupByTablesButton = new Button(parent, SWT.RADIO);
			groupByTablesButton.setText("Tables");
			fd = new FormData();
			fd.top = new FormAttachment(groupingLabel, 0, SWT.TOP);
			fd.bottom = new FormAttachment(collapseAllButton, 0, SWT.BOTTOM);
			fd.left = new FormAttachment(groupingLabel, space);
			groupByTablesButton.setLayoutData(fd);
			groupByTablesButton.setSelection(!groupByColumns);
			groupByTablesButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					groupByColumns = !groupByTablesButton.getSelection();
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			final Button groupByColumnsButton = new Button(parent, SWT.RADIO);
			groupByColumnsButton.setText("Columns");
			fd = new FormData();
			fd.top = new FormAttachment(groupingLabel, 0, SWT.TOP);
			fd.bottom = new FormAttachment(collapseAllButton, 0, SWT.BOTTOM);
			fd.left = new FormAttachment(groupByTablesButton, space);
			groupByColumnsButton.setLayoutData(fd);
			groupByColumnsButton.setSelection(groupByColumns);
			groupByColumnsButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					groupByColumns = !groupByTablesButton.getSelection();
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			// Row four, further buttons and checks. We put it before row three,
			// because row three will occupy all space in the middle of the form,
			// so it depends on this.
			// We bind the checks from bottom to top and then bind the buttons to the checks
			final Button skipTextButton = new Button(parent, SWT.CHECK);
			skipTextButton.setText("Skip Text");
			fd = new FormData();
			fd.bottom = new FormAttachment(100);
			fd.left = new FormAttachment(0);
			skipTextButton.setLayoutData(fd);
			skipTextButton.setSelection(skipText);
			skipTextButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					skipText = skipTextButton.getSelection();
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			final Button skipDatetimeAndMediaButton = new Button(parent, SWT.CHECK);
			skipDatetimeAndMediaButton.setText("Skip DATETIME and MEDIA");
			fd = new FormData();
			fd.bottom = new FormAttachment(skipTextButton, -space);
			fd.left = new FormAttachment(skipTextButton, 0, SWT.LEFT);
			skipDatetimeAndMediaButton.setLayoutData(fd);
			skipDatetimeAndMediaButton.setSelection(skipDatetimeAndMedia);
			skipDatetimeAndMediaButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					skipDatetimeAndMedia = skipDatetimeAndMediaButton.getSelection();
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			final Button listOnlyColumnsWithoutCurrentTypeButton = new Button(parent, SWT.CHECK);
			listOnlyColumnsWithoutCurrentTypeButton.setText("List only columns without current type");
			fd = new FormData();

			fd.bottom = new FormAttachment(skipDatetimeAndMediaButton, -space);
			fd.left = new FormAttachment(skipDatetimeAndMediaButton, 0, SWT.LEFT);
			listOnlyColumnsWithoutCurrentTypeButton.setLayoutData(fd);
			listOnlyColumnsWithoutCurrentTypeButton.setSelection(listOnlyColumnsWithoutCurrentType);
			listOnlyColumnsWithoutCurrentTypeButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					listOnlyColumnsWithoutCurrentType = listOnlyColumnsWithoutCurrentTypeButton.getSelection();
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			final Button listOnlyChangesButton = new Button(parent, SWT.CHECK);
			listOnlyChangesButton.setText("List only columns that change");
			fd = new FormData();

			fd.bottom = new FormAttachment(listOnlyColumnsWithoutCurrentTypeButton, -space);
			fd.left = new FormAttachment(listOnlyColumnsWithoutCurrentTypeButton, 0, SWT.LEFT);
			listOnlyChangesButton.setLayoutData(fd);
			listOnlyChangesButton.setSelection(listOnlyChangedColumns);
			listOnlyChangesButton.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					listOnlyChangedColumns = listOnlyChangesButton.getSelection();
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			Button selectNoneForSaving = new Button(parent, SWT.PUSH);
			selectNoneForSaving.setText("Select none for saving");
			fd = new FormData();
			fd.top = new FormAttachment(listOnlyChangesButton, 0, SWT.TOP);
			fd.right = new FormAttachment(100);
			selectNoneForSaving.setLayoutData(fd);
			selectNoneForSaving.setSelection(true);
			selectNoneForSaving.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					data.changeSaveStatusForAllModified(false);
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			Button selectAllForSaving = new Button(parent, SWT.PUSH);
			selectAllForSaving.setText("Select all for saving");
			fd = new FormData();
			fd.top = new FormAttachment(selectNoneForSaving, 0, SWT.TOP);
			fd.right = new FormAttachment(selectNoneForSaving, -space);
			selectAllForSaving.setLayoutData(fd);
			selectAllForSaving.setSelection(true);
			selectAllForSaving.addListener(SWT.Selection, new Listener()
			{
				public void handleEvent(Event event)
				{
					data.changeSaveStatusForAllModified(true);
					updateContent(false);
					suggestionTree.getTree().setFocus();
				}
			});

			// Row three, the tree editor.
			Composite suggestionTreeHolder = new Composite(parent, SWT.NONE);
			fd = new FormData();
			fd.top = new FormAttachment(collapseAllButton, space);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			fd.bottom = new FormAttachment(listOnlyChangesButton, -space);
			fd.height = 400;
			suggestionTreeHolder.setLayoutData(fd);

			suggestionTree = new TreeViewer(suggestionTreeHolder, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			suggestionTree.getTree().setHeaderVisible(true);
			suggestionTree.getTree().setLinesVisible(true);
			suggestionTree.setColumnProperties(columnNames);

			TreeColumn sourceTableNameColumn = new TreeColumn(suggestionTree.getTree(), SWT.LEFT);
			sourceTableNameColumn.setText(TABLE_OR_COLUMN_NAME);
			TreeColumn datatypeColumn = new TreeColumn(suggestionTree.getTree(), SWT.LEFT);
			datatypeColumn.setText(DATATYPE);
			TreeColumn currentForeignTypeColumn = new TreeColumn(suggestionTree.getTree(), SWT.LEFT);
			currentForeignTypeColumn.setText(CURRENT_FOREIGN_TYPE);
			TreeColumn suggestedForeignTypeColumn = new TreeColumn(suggestionTree.getTree(), SWT.LEFT);
			suggestedForeignTypeColumn.setText(SUGGESTED_FOREIGN_TYPE);
			TreeColumn saveColumn = new TreeColumn(suggestionTree.getTree(), SWT.CHECK);
			saveColumn.setText(SAVE);

			TreeColumnLayout layout = new TreeColumnLayout();
			layout.setColumnData(sourceTableNameColumn, new ColumnWeightData(10, 150, true));
			layout.setColumnData(datatypeColumn, new ColumnWeightData(7, 100, true));
			layout.setColumnData(currentForeignTypeColumn, new ColumnWeightData(10, 150, true));
			layout.setColumnData(suggestedForeignTypeColumn, new ColumnWeightData(10, 150, true));
			layout.setColumnData(saveColumn, new ColumnWeightData(3, 50, true));
			suggestionTreeHolder.setLayout(layout);

			TreeViewerColumn suggestedForeignTypeViewerCol = new TreeViewerColumn(suggestionTree, suggestedForeignTypeColumn);
			suggestionCellEditor = new ComboBoxCellEditor(suggestionTree.getTree(), new String[] { }, SWT.READ_ONLY);
			SuggestionTreeEditingSupport editingSupport = new SuggestionTreeEditingSupport(suggestionTree, suggestionCellEditor);
			suggestedForeignTypeViewerCol.setEditingSupport(editingSupport);

			TreeViewerColumn saveViewerCol = new TreeViewerColumn(suggestionTree, saveColumn);
			CheckboxCellEditor saveCellEditor = new CheckboxCellEditor(suggestionTree.getTree(), SWT.CHECK);
			CanSaveEditingSupport canSaveEditSupport = new CanSaveEditingSupport(suggestionTree, saveCellEditor);
			saveViewerCol.setEditingSupport(canSaveEditSupport);

			suggestionTree.setLabelProvider(new SuggestionTreeLabelProvider());
			suggestionTree.setContentProvider(new SuggestionTreeContentProvider());

			setControl(parent);

			needsUpdate = true;

			updateContent(true);
		}

		public boolean needsUpdate()
		{
			return needsUpdate;
		}

		public void updateContent(boolean reloadData)
		{
			if (reloadData)
			{
				try
				{
					List<String> tableNames = server.getTableAndViewNames(true, true);
					allTableNames = new String[tableNames.size()];
					allTableNames = tableNames.toArray(allTableNames);
					Arrays.sort(allTableNames);

					arrayNamesWithEmptyEntry = new String[allTableNames.length + 1];
					arrayNamesWithEmptyEntry[0] = ENTRY_NONE;
					for (int i = 0; i < allTableNames.length; i++)
						arrayNamesWithEmptyEntry[i + 1] = allTableNames[i];

					suggestionCellEditor.setItems(arrayNamesWithEmptyEntry);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logWarning("Failed to get table names list.", e);
				}

				data = new ForeignTypeSuggestionData();

				WorkspaceJob foreignTypesComputationJob = new WorkspaceJob("Computing foreign type suggestions")
				{
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
					{
						monitor.beginTask("Computing foreign types", allTableNames.length);

						try
						{
							matchForeignTypesToColumns(allTableNames, monitor);
							Display.getDefault().syncExec(new Runnable()
							{
								public void run()
								{
									suggestionTree.refresh();
									suggestionTree.expandAll();
								}
							});
							monitor.done();
							return Status.OK_STATUS;
						}
						catch (RepositoryException e)
						{
							ServoyLog.logWarning("Failed to compute foreign types.", e);
							monitor.done();
							return Status.CANCEL_STATUS;
						}
					}
				};

				foreignTypesComputationJob.setRule(ServoyModel.getWorkspace().getRoot());
				foreignTypesComputationJob.setUser(false);
				foreignTypesComputationJob.schedule();

				suggestionTree.setInput(data);
				suggestionTree.expandAll();
			}
			else
			{
				suggestionTree.refresh();
			}
		}

		private void matchForeignTypesToColumns(String[] tableNames, IProgressMonitor monitor) throws RepositoryException
		{
			FlattenedSolution solution = getServoyModelManager().getServoyModel().getFlattenedSolution();

			for (String fkTableName : tableNames)
			{
				monitor.setTaskName("Computing foreign types for table '" + fkTableName + "'");

				ITable fkTable = server.getTable(fkTableName);

				Map<String, Column> matchedFromRelations = new HashMap<String, Column>();

				Iterator<Relation> relationsIterator = solution.getRelations(fkTable, false, true);
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				while (relationsIterator.hasNext())
				{
					Relation relation = relationsIterator.next();
					IServerInternal foreignServer = dsm.getServer(relation.getForeignDataSource());
					IServerInternal primaryServer = dsm.getServer(relation.getPrimaryDataSource());
					if (primaryServer != null && server.getName().equals(primaryServer.getName()) && foreignServer != null &&
						server.getName().equals(foreignServer.getName()))
					{
						List<IPersist> allRelationItems = relation.getAllObjectsAsList();
						for (IPersist persist : allRelationItems)
						{
							RelationItem relItem = (RelationItem)persist;
							if (relItem.getOperator() == IBaseSQLCondition.EQUALS_OPERATOR && !ScopesUtils.isVariableScope(relItem.getPrimaryDataProviderID()))
							{
								// Don't use self-referencing columns.
								if (!(relation.getPrimaryTableName().equals(relation.getForeignTableName()) &&
									relItem.getPrimaryDataProviderID().equals(relItem.getForeignColumnName())))
								{
									Table pkTable = (Table)server.getTable(relation.getPrimaryTableName());
									Column pkCol = pkTable.getColumn(relItem.getPrimaryDataProviderID());
									Column fkCol = fkTable.getColumn(relItem.getForeignColumnName());
									if (fkCol != null && !fkCol.isDatabasePK() && pkTable != null && !pkTable.isMarkedAsHiddenInDeveloper() && pkCol != null &&
										pkCol.isDatabasePK())
									{
										if (matchedFromRelations.containsKey(fkCol.getName()))
										{
											Column oldMatch = matchedFromRelations.get(fkCol.getName());
											if (!oldMatch.getTable().getName().equals(pkTable.getName()))
											{
												double oldScore = matchNames(fkTable.getName(), fkCol, oldMatch);
												double newScore = matchNames(fkTable.getName(), fkCol, pkCol);
												if (newScore > oldScore)
												{
													data.addSuggestion(fkTable, fkCol, pkTable);
													matchedFromRelations.put(fkCol.getName(), pkCol);
												}
											}
										}
										else
										{
											data.addSuggestion(fkTable, fkCol, pkTable);
											matchedFromRelations.put(fkCol.getName(), pkCol);
										}
									}
								}
							}
						}
					}
				}


				Collection<Column> fkColumns = fkTable.getColumns();
				for (Column fkCol : fkColumns)
				{
					monitor.subTask("column '" + fkCol.getName() + "'");

					// PKs don't need foreign type
					if (!fkCol.hasFlag(IBaseColumn.EXCLUDED_COLUMN) && !fkCol.isDatabasePK() && !matchedFromRelations.containsKey(fkCol.getName()))
					{
						boolean matched = false;

						// Match against all tables and all relevant columns, and just pick the best score.
						double bestScore = 0;
						ITable bestTable = null;
						for (String tabName : tableNames)
						{
							if (!fkTableName.equals(tabName))
							{
								ITable table = server.getTable(tabName);
								List< ? > pkColumns = table.getRowIdentColumns();
								for (Object oo : pkColumns)
								{
									Column pkCol = (Column)oo;
									double score = matchNames(fkTable.getName(), fkCol, pkCol);
									if (score > bestScore)
									{
										bestScore = score;
										bestTable = table;
									}
								}
							}
						}

						if (bestScore > MATCH_THRESHOLD)
						{
							data.addSuggestion(fkTable, fkCol, bestTable);
							matched = true;
						}

						// Set no matching, unless was solved through relations.
						if (!matched && !matchedFromRelations.containsKey(fkCol.getName())) data.addSuggestion(fkTable, fkCol, null);
					}
				}

				monitor.worked(1);
			}
		}
	}

	private double matchNames(String fkTable, Column fkColumn, Column pkColumn)
	{
		// First check the types. If not the same, don't check further.

		// use dataprovider type as defined by converter
		ComponentFormat fkComponentFormat = ComponentFormat.getComponentFormat(null, fkColumn, Activator.getDefault().getDesignClient());
		ComponentFormat pkComponentFormat = ComponentFormat.getComponentFormat(null, pkColumn, Activator.getDefault().getDesignClient());

		// We try to match only INTEGER, NUMBER and STRING columns.
		if (fkComponentFormat.dpType != IColumnTypes.INTEGER && fkComponentFormat.dpType != IColumnTypes.NUMBER &&
			fkComponentFormat.dpType != IColumnTypes.TEXT) return 0;
		if (pkComponentFormat.dpType != IColumnTypes.INTEGER && pkComponentFormat.dpType != IColumnTypes.NUMBER &&
			pkComponentFormat.dpType != IColumnTypes.TEXT) return 0;

		if (!(pkComponentFormat.dpType == fkComponentFormat.dpType ||
			(pkComponentFormat.dpType == IColumnTypes.NUMBER && fkComponentFormat.dpType == IColumnTypes.INTEGER) ||
			(pkComponentFormat.dpType == IColumnTypes.INTEGER && fkComponentFormat.dpType == IColumnTypes.NUMBER))) return 0;

		// Compute two parameters:
		// - how much the PK and the FK columns resemble
		// - how much the FK column resembles the name of the FK table
		// Return a formula based on both these values.
		double mainScore = StringMatcher.stringMatchProportion(fkColumn.getName(), pkColumn.getName());
		double result = mainScore * 10;
		// If the score is too low, don't bother matching with the table name.
		if (result > MATCH_THRESHOLD - 1)
		{
			double secondaryScore = StringMatcher.stringMatchProportion(fkTable, pkColumn.getName());
			result += secondaryScore;
		}
		return result;
	}

	private class ForeignTypeSuggestionData
	{
		private final SortedMap<String, ForeignTypeSuggestionEntry> suggestions;
		private final SortedMap<ITable, SortedMap<IColumn, ForeignTypeSuggestionEntry>> suggestionsByTable;
		private final SortedMap<IColumn, SortedMap<ITable, ForeignTypeSuggestionEntry>> suggestionsByColumn;
		private final SortedMap<ITable, ForeignTypeSuggestionEntry> groupingEntriesByTable;
		private final SortedMap<IColumn, ForeignTypeSuggestionEntry> groupingEntriesByColumn;

		public ForeignTypeSuggestionData()
		{
			suggestions = new TreeMap<String, ForeignTypeSuggestionEntry>();
			suggestionsByTable = new TreeMap<ITable, SortedMap<IColumn, ForeignTypeSuggestionEntry>>(new ITableComparator());
			suggestionsByColumn = new TreeMap<IColumn, SortedMap<ITable, ForeignTypeSuggestionEntry>>(new IColumnComparator());
			groupingEntriesByTable = new TreeMap<ITable, ForeignTypeSuggestionEntry>(new ITableComparator());
			groupingEntriesByColumn = new TreeMap<IColumn, ForeignTypeSuggestionEntry>(new IColumnComparator());
		}

		public synchronized void addSuggestion(ITable parentTable, IColumn parentColumn, ITable suggestedTable)
		{
			// First add (if not already added) the suggestion to the list of all suggestions.
			String key = buildKey(parentTable.getName(), parentColumn.getName());
			ForeignTypeSuggestionEntry suggestion = suggestions.get(key);
			if (suggestion != null)
			{
				suggestion.setSuggestedForeignType(suggestedTable);
			}
			else
			{
				suggestion = new ForeignTypeSuggestionEntry(parentTable, parentColumn, suggestedTable);

				// Store the suggestion in the list of all suggestions.
				suggestions.put(key, suggestion);

				// Place the suggestion in the map organized by tables.
				SortedMap<IColumn, ForeignTypeSuggestionEntry> suggestionsForTable;
				if (suggestionsByTable.containsKey(parentTable)) suggestionsForTable = suggestionsByTable.get(parentTable);
				else
				{
					suggestionsForTable = new TreeMap<IColumn, ForeignTypeSuggestionEntry>(new IColumnComparator());
					suggestionsByTable.put(parentTable, suggestionsForTable);
				}
				suggestionsForTable.put(parentColumn, suggestion);

				ForeignTypeSuggestionEntry groupingEntryForTable = groupingEntriesByTable.get(parentTable);
				if (groupingEntryForTable == null)
				{
					groupingEntryForTable = new ForeignTypeSuggestionEntry(parentTable);
					groupingEntriesByTable.put(parentTable, groupingEntryForTable);
				}
				groupingEntryForTable.addChild(suggestion);

				// And now do the same with the map organized by columns.
				SortedMap<ITable, ForeignTypeSuggestionEntry> suggestionsForColumn;
				if (suggestionsByColumn.containsKey(parentColumn))
				{
					suggestionsForColumn = suggestionsByColumn.get(parentColumn);
				}
				else
				{
					suggestionsForColumn = new TreeMap<ITable, ForeignTypeSuggestionEntry>(new ITableComparator());
					suggestionsByColumn.put(parentColumn, suggestionsForColumn);
				}
				suggestionsForColumn.put(parentTable, suggestion);

				ForeignTypeSuggestionEntry groupingEntryForColumn = groupingEntriesByColumn.get(parentColumn);
				if (groupingEntryForColumn == null)
				{
					groupingEntryForColumn = new ForeignTypeSuggestionEntry(parentColumn);
					groupingEntriesByColumn.put(parentColumn, groupingEntryForColumn);
				}
				groupingEntryForColumn.addChild(suggestion);
			}
		}

		public synchronized List<ForeignTypeSuggestionEntry> getData()
		{
			List<ForeignTypeSuggestionEntry> result = new ArrayList<ForeignTypeSuggestionEntry>();
			if (groupByColumns)
			{
				for (IColumn parentColumn : groupingEntriesByColumn.keySet())
				{
					ForeignTypeSuggestionEntry parentEntry = groupingEntriesByColumn.get(parentColumn);
					if (parentEntry.shouldShow()) result.add(parentEntry);
				}
			}
			else
			{
				for (ITable parentTable : groupingEntriesByTable.keySet())
				{
					ForeignTypeSuggestionEntry parentEntry = groupingEntriesByTable.get(parentTable);
					if (parentEntry.shouldShow()) result.add(parentEntry);
				}
			}
			return result;
		}

		public synchronized String commitData(IProgressMonitor monitor)
		{
			StringBuilder result = new StringBuilder();
			for (ITable parentTable : suggestionsByTable.keySet())
			{
				monitor.setTaskName("Saving foreign types for table '" + parentTable.getName() + "'.");

				SortedMap<IColumn, ForeignTypeSuggestionEntry> suggestionsForTable = suggestionsByTable.get(parentTable);
				for (IColumn parentColumn : suggestionsForTable.keySet())
				{
					ForeignTypeSuggestionEntry suggestedTable = suggestionsForTable.get(parentColumn);
					if (suggestedTable.isDoSave())
					{
						String foreignTypeToSet = null;
						if ((suggestedTable != null) && (suggestedTable.getSuggestedForeignType() != null))
							foreignTypeToSet = suggestedTable.getSuggestedForeignType().getName();

						ColumnInfo ci = ((Column)parentColumn).getColumnInfo();
						if (ci != null)
						{
							// If we need to trace changes for a certain column, do an update here (if needed).
							// Someone else is probably listening to these changes.
							if (foreignTypeForChosenColumn != null && (parentTable.getName().equals(tableName)) && (parentColumn.getName().equals(columnName)))
							{
								final String foreignTypeForThread = foreignTypeToSet;
								foreignTypeForChosenColumn.getRealm().exec(new Runnable()
								{
									public void run()
									{
										foreignTypeForChosenColumn.setValue(foreignTypeForThread);
									}
								});
							}
							ci.setForeignType(foreignTypeToSet);
							ci.flagChanged();
						}
					}
				}
				try
				{
					server.updateAllColumnInfo(parentTable);
				}
				catch (RepositoryException e)
				{
					result.append("Failed to save foreign types for table '" + parentTable.getName() + "'.");
					ServoyLog.logError("Failed to save foreign types for table '" + parentTable.getName() + "'.", e);
				}
				monitor.worked(1);
			}
			return result.toString();
		}

		public synchronized void changeSaveStatusForAllModified(boolean newSaveStatus)
		{
			for (ForeignTypeSuggestionEntry entry : suggestions.values())
			{
				if (entry.isChanged()) entry.setDoSave(newSaveStatus);
			}
		}

		private String buildKey(String tName, String cName)
		{
			return tName + "###" + cName;
		}
	}

	private class ForeignTypeSuggestionEntry
	{
		private final ITable parentTable;
		private final IColumn parentColumn;
		private ITable suggestedForeignType;
		private final boolean isGrouping;
		private final boolean isGroupingByColumns;
		private final List<ForeignTypeSuggestionEntry> children = new ArrayList<ForeignTypeSuggestionEntry>();
		private ForeignTypeSuggestionEntry parent = null;
		private boolean doSave;

		public ForeignTypeSuggestionEntry(ITable parentTable)
		{
			this.parentTable = parentTable;
			this.parentColumn = null;
			this.suggestedForeignType = null;
			this.isGrouping = true;
			this.isGroupingByColumns = false;
			this.doSave = false;
		}

		public ForeignTypeSuggestionEntry(IColumn parentColumn)
		{
			this.parentTable = null;
			this.parentColumn = parentColumn;
			this.suggestedForeignType = null;
			this.isGrouping = true;
			this.isGroupingByColumns = true;
			this.doSave = false;
		}

		public ForeignTypeSuggestionEntry(ITable parentTable, IColumn parentColumn, ITable suggestedForeignType)
		{
			this.parentTable = parentTable;
			this.parentColumn = parentColumn;
			this.suggestedForeignType = suggestedForeignType;
			this.isGrouping = false;
			this.isGroupingByColumns = false;
			this.doSave = isChanged();
		}

		public boolean isWithoutCurrentType()
		{
			if (isGroupingEntry())
			{
				for (ForeignTypeSuggestionEntry child : children)
				{
					if (child.isWithoutCurrentType())
					{
						return true;
					}
				}
				return true;
			}

			ColumnInfo ci = ((Column)parentColumn).getColumnInfo();
			return ci == null || ci.getForeignType() == null;
		}

		public boolean isNotDatatimeAndMedia()
		{
			if (isGroupingEntry())
			{
				for (ForeignTypeSuggestionEntry child : children)
				{
					if (child.isNotDatatimeAndMedia())
					{
						return true;
					}
				}
				return false;
			}

			// use dataprovider type as defined by converter
			ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, parentColumn, Activator.getDefault().getDesignClient());
			return componentFormat.dpType == IColumnTypes.INTEGER || componentFormat.dpType == IColumnTypes.NUMBER ||
				componentFormat.dpType == IColumnTypes.TEXT;
		}

		public boolean isNotText()
		{
			if (isGroupingEntry())
			{
				for (ForeignTypeSuggestionEntry child : children)
				{
					if (child.isNotText())
					{
						return true;
					}
				}
				return false;
			}

			// use dataprovider type as defined by converter
			ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, parentColumn, Activator.getDefault().getDesignClient());
			return componentFormat.dpType == IColumnTypes.INTEGER || componentFormat.dpType == IColumnTypes.NUMBER ||
				componentFormat.dpType == IColumnTypes.DATETIME || componentFormat.dpType == IColumnTypes.MEDIA;
		}

		public boolean isChanged()
		{
			if (isGroupingEntry())
			{
				for (ForeignTypeSuggestionEntry child : children)
				{
					if (child.isChanged())
					{
						return true;
					}
				}
				return false;
			}

			ColumnInfo ci = ((Column)parentColumn).getColumnInfo();
			boolean suggestedForeignTypeEmpty = suggestedForeignType == null || suggestedForeignType.getName() == null ||
				suggestedForeignType.getName().trim().length() == 0;
			boolean currentForeignTypeEmpty = ci == null || ci.getForeignType() == null || ci.getForeignType().trim().length() == 0;
			if (currentForeignTypeEmpty)
			{
				return !suggestedForeignTypeEmpty;
			}
			return suggestedForeignTypeEmpty || !ci.getForeignType().equals(suggestedForeignType.getName());
		}

		public boolean isGroupingEntry()
		{
			return this.isGrouping;
		}

		public IColumn getParentColumn()
		{
			return parentColumn;
		}

		public ITable getSuggestedForeignType()
		{
			return suggestedForeignType;
		}

		public void setSuggestedForeignType(ITable suggestedForeignType)
		{
			this.suggestedForeignType = suggestedForeignType;
			if (!doSave && isChanged()) doSave = true;
		}

		public boolean isDoSave()
		{
			return doSave;
		}

		public void setDoSave(boolean doSave)
		{
			this.doSave = doSave;
		}

		public void addChild(ForeignTypeSuggestionEntry child)
		{
			this.children.add(child);
			child.parent = this;
		}

		public ForeignTypeSuggestionEntry getParent()
		{
			return this.parent;
		}

		public List<ForeignTypeSuggestionEntry> getChildrenFiltered()
		{
			List<ForeignTypeSuggestionEntry> result = new ArrayList<ForeignTypeSuggestionEntry>();
			for (ForeignTypeSuggestionEntry entry : children)
			{
				if (entry.shouldShow()) result.add(entry);
			}
			return result;
		}

		public boolean shouldShow()
		{
			return (!listOnlyChangedColumns || (listOnlyChangedColumns && isChanged())) &&
				(!listOnlyColumnsWithoutCurrentType || (listOnlyColumnsWithoutCurrentType && isWithoutCurrentType())) &&
				(!skipDatetimeAndMedia || (skipDatetimeAndMedia && isNotDatatimeAndMedia())) && (!skipText || (skipText && isNotText()));
		}

		public String getRelevantName()
		{
			if (isGrouping)
			{
				if (isGroupingByColumns) return parentColumn.getName();
				return parentTable.getName();
			}

			if (groupByColumns) return parentTable.getName();
			return parentColumn.getName();
		}
	}

	private class SuggestionTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider
	{
		public Object[] getChildren(Object parentElement)
		{
			ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)parentElement;
			if (entry.isGroupingEntry()) return entry.getChildrenFiltered().toArray();
			return null;
		}

		public Object getParent(Object element)
		{
			ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)element;
			if (entry.isGroupingEntry()) return null;
			return entry.getParent();
		}

		public boolean hasChildren(Object element)
		{
			ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)element;
			return entry.isGroupingEntry();
		}

		@Override
		public Object[] getElements(Object inputElement)
		{
			return ((ForeignTypeSuggestionData)inputElement).getData().toArray();
		}
	}

	private class SuggestionTreeLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public Image getColumnImage(Object element, int columnIndex)
		{
			ForeignTypeSuggestionEntry row = (ForeignTypeSuggestionEntry)element;
			if (columnIndex == TABLE_OR_COLUMN_NAME_INDEX)
			{
				if (row.isGroupingEntry()) return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
			}
			else if (columnIndex == SAVE_INDEX)
			{
				if (!row.isGrouping && row.isChanged()) return row.isDoSave() ? ColumnLabelProvider.TRUE_IMAGE : ColumnLabelProvider.FALSE_IMAGE;
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex)
		{
			ForeignTypeSuggestionEntry row = (ForeignTypeSuggestionEntry)element;
			IColumn parentColumn = row.getParentColumn();
			String currentForeignType = null;
			String suggestedForeignType = null;
			if (!row.isGroupingEntry())
			{
				ColumnInfo ci = ((Column)parentColumn).getColumnInfo();
				currentForeignType = ci == null ? ENTRY_NONE : ci.getForeignType();
				if ((currentForeignType == null) || (currentForeignType.trim().length() == 0)) currentForeignType = ENTRY_NONE;

				ITable suggestedTable = row.getSuggestedForeignType();
				suggestedForeignType = suggestedTable == null ? ENTRY_NONE : suggestedTable.getName();
			}

			switch (columnIndex)
			{
				case TABLE_OR_COLUMN_NAME_INDEX :
					return row.getRelevantName();

				case DATATYPE_INDEX :
					if (row.isGroupingEntry())
					{
						return "";
					}

					// use dataprovider type as defined by converter
					ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, parentColumn, Activator.getDefault().getDesignClient());
					return Column.getDisplayTypeString(componentFormat.dpType);

				case CURRENT_FOREIGN_TYPE_INDEX :
					return currentForeignType;

				case SUGGESTED_FOREIGN_TYPE_INDEX :
					return suggestedForeignType;

				default :
					return null;
			}
		}
	}

	private class CanSaveEditingSupport extends EditingSupport
	{
		private final CheckboxCellEditor checkboxEditor;

		public CanSaveEditingSupport(ColumnViewer viewer, CheckboxCellEditor checkboxEditor)
		{
			super(viewer);
			this.checkboxEditor = checkboxEditor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)element;
			return !entry.isGroupingEntry();
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return checkboxEditor;
		}

		@Override
		protected Object getValue(Object element)
		{
			ForeignTypeSuggestionEntry row = (ForeignTypeSuggestionEntry)element;
			return new Boolean(row.isDoSave());
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			Boolean b = (Boolean)value;
			ForeignTypeSuggestionEntry row = (ForeignTypeSuggestionEntry)element;
			row.setDoSave(b.booleanValue());
			getViewer().update(element, null);
		}

	}

	private class SuggestionTreeEditingSupport extends EditingSupport
	{
		ComboBoxCellEditor suggestionCellEditor;

		public SuggestionTreeEditingSupport(ColumnViewer viewer, ComboBoxCellEditor suggestionCellEditor)
		{
			super(viewer);
			this.suggestionCellEditor = suggestionCellEditor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)element;
			return !entry.isGroupingEntry();
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return suggestionCellEditor;
		}

		@Override
		protected Object getValue(Object element)
		{
			ForeignTypeSuggestionEntry row = (ForeignTypeSuggestionEntry)element;
			ITable suggestedTable = row.getSuggestedForeignType();
			int suggestedTableIndex = 0;
			if (suggestedTable != null)
			{
				for (int i = 1; i < arrayNamesWithEmptyEntry.length; i++)
					if (arrayNamesWithEmptyEntry[i].equals(suggestedTable.getName()))
					{
						suggestedTableIndex = i;
						break;
					}
			}
			return new Integer(suggestedTableIndex);
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			Integer index = (Integer)value;
			if (index.intValue() > 0)
			{
				String newTableName = arrayNamesWithEmptyEntry[index.intValue()];
				try
				{
					ITable newTable = server.getTable(newTableName);
					ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)element;
					entry.setSuggestedForeignType(newTable);
					getViewer().refresh();
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cannot find this table for foreign type: " + newTableName + ".", e);
				}
			}
			else
			{
				ForeignTypeSuggestionEntry entry = (ForeignTypeSuggestionEntry)element;
				entry.setSuggestedForeignType(null);
				getViewer().refresh();
			}
		}
	}
}
