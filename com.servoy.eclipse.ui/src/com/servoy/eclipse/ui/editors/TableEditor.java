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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.SQLExplorerLoader;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.TableEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.view.ViewFoundsetTable;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.editors.table.AggregationsComposite;
import com.servoy.eclipse.ui.editors.table.CalculationsComposite;
import com.servoy.eclipse.ui.editors.table.ColumnComposite;
import com.servoy.eclipse.ui.editors.table.DataComposite;
import com.servoy.eclipse.ui.editors.table.EventsComposite;
import com.servoy.eclipse.ui.editors.table.FoundsetMethodsComposite;
import com.servoy.eclipse.ui.editors.table.LabelComposite;
import com.servoy.eclipse.ui.editors.table.PropertiesComposite;
import com.servoy.eclipse.ui.editors.table.SecurityComposite;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.IColumnValidator;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class TableEditor extends MultiPageEditorPart implements IActiveProjectListener
{
	public static final String ID = "com.servoy.eclipse.ui.editors.TableEditor";

	public static final String PREFERENCE_KEY_EXTENDED_VIEW = "com.servoy.eclipse.ui.editors.TableEditor.extended_view";

	private boolean isModified;

	private IServerInternal server;

	private ITable table;

	private ColumnComposite columnComposite;

	private DataComposite dataComposite;

	private CalculationsComposite calculationsComposite;

	private FoundsetMethodsComposite methodsComposite;

	private AggregationsComposite aggregationsComposite;

	private EventsComposite eventsComposite;

	private SecurityComposite securityComposite;

	private PropertiesComposite propertiesComposite;

	private ITableListener tableListener;

	private IItemChangeListener<IColumn> columnListener;

	private IServerListener serverListener;

	private IPersistChangeListener persistListener;

	private boolean delayedLoad = false;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, convertInput(input));
		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(this);
	}

	protected IEditorInput convertInput(IEditorInput input)
	{
		if (input instanceof FileEditorInput)
		{
			TableEditorInput tableEditorInput = TableEditorInput.createFromFileEditorInput((FileEditorInput)input);
			if (tableEditorInput != null)
			{
				return tableEditorInput;
			}
		}
		return input;
	}

	@Override
	protected void createPages()
	{
		if (delayedLoad)
		{
			//when there is no solution activated, the table editor cannot be opened
			LabelComposite labelComposite = new LabelComposite(getContainer(), SWT.NONE, "A solution must first be activated, to be able to show the editor!");
			setPageText(addPage(labelComposite), "Loading Solution");
			return;
		}
		createColumnPage();
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null)
		{
			createDynamicPages();

			if (!DataSourceUtils.VIEW_DATASOURCE.equals(server.getName()))
			{
				createPropertiesPage();
			}
		}
		updateTitle();
	}

	@Override
	protected CTabFolder createContainer(Composite parent)
	{
		CTabFolder parentControl = super.createContainer(parent);
		parentControl.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");
		return parentControl;
	}

	protected int getPageIndex(Control control)
	{
		int pages = getPageCount();
		for (int i = 0; i < pages; i++)
		{
			if (getControl(i) == control) return i;
		}
		return -1;
	}

	public void addPage(int order, Control control, String text)
	{
		int pages = getPageCount();
		int idx = pages;
		for (int i = 0; i < pages; i++)
		{
			Control page = getControl(i);
			if (page != null && page.getData("order") instanceof Integer && ((Integer)page.getData("order")).intValue() > order)
			{
				idx = i;
				break;
			}
		}

		control.setData("order", Integer.valueOf(order));
		addPage(idx, control);
		setPageText(getPageIndex(control), text);
	}

	protected boolean activeSolutionIsMobile()
	{
		return ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile();
	}

	private void createColumnPage()
	{
		addPage(100, columnComposite = new ColumnComposite(this, getContainer(),
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None), "Columns");
	}

	private void createCalculationsPage()
	{
		addPage(200, calculationsComposite = new CalculationsComposite(this, getContainer(),
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None), "Calculations");
	}

	private void createFoundsSetMethodsPage()
	{
		if (!activeSolutionIsMobile())
		{
			addPage(300, methodsComposite = new FoundsetMethodsComposite(this, getContainer(),
				ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None), "Methods");
		}
	}

	private void createAggregationsPage()
	{
		if (!activeSolutionIsMobile())
		{
			addPage(400, aggregationsComposite = new AggregationsComposite(this, getContainer(),
				ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None), "Aggregations");
		}
	}

	private void createEventsPage()
	{
		if (!activeSolutionIsMobile())
		{
			addPage(500, eventsComposite = new EventsComposite(this, getContainer(), SWT.None), "Events");
		}
	}

	private void createSecurityPage()
	{
		if (!activeSolutionIsMobile())
		{
			addPage(600, securityComposite = new SecurityComposite(getContainer(), SWT.None, this,
				ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingSolution()), "Security");
		}
	}

	private void createPropertiesPage()
	{
		addPage(700, propertiesComposite = new PropertiesComposite(getContainer(), SWT.None, this), "Properties");
	}

	private void createDataPage()
	{
		if (SQLExplorerLoader.isSqlExplorerLoaded() && DataSourceUtils.getDBServernameTablename(table.getDataSource()) != null)
		{
			addPage(800, dataComposite = new DataComposite(getContainer(), table), "Data");
		}
	}

	public ColumnComposite getColumnComposite()
	{
		return columnComposite;
	}

	public void revert()
	{
		if (isDirty())
		{
			try
			{
				if (server != null && getTable().getExistInDB())
				{
					for (Column element : getTable().getColumns())
					{
						element.removeColumnInfo();
					}
					server.refreshTable(getTable().getName());
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			try
			{
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getActiveProject();
				if (servoyProject != null)
				{
					Solution solution = servoyProject.getEditingSolution();
					if (solution != null)
					{
						Iterator<TableNode> tableNodes = solution.getTableNodes(table);
						while (tableNodes.hasNext())
						{
							servoyModel.revertEditingPersist(servoyProject, tableNodes.next());
						}
						Solution[] modules = ModelUtils.getEditingFlattenedSolution(solution).getModules();
						if (modules != null)
						{
							for (Solution module : modules)
							{
								ServoyProject project = servoyModel.getServoyProject(module.getName());
								if (project != null)
								{
									solution = project.getEditingSolution();
									if (solution != null)
									{
										tableNodes = solution.getTableNodes(table);
										while (tableNodes.hasNext())
										{
											ServoyModelManager.getServoyModelManager().getServoyModel().revertEditingPersist(project, tableNodes.next());
										}
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			if (!table.getExistInDB())
			{
				// editor closes for a table that does not exist in DB - remove the table
				try
				{
					server.removeTable(table);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				catch (SQLException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	@Override
	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
		if (persistListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, persistListener);
		}
		// seems that pages added by add(Control) are not disposed by default like
		// the ones added with add(editor)
		// we must dispose of them - or they remain as listeners that want to
		// update disposed UI
		if (columnComposite != null) columnComposite.dispose();
		if (propertiesComposite != null) propertiesComposite.dispose();
		disposeDynamicPages();

		if (serverListener != null)
		{
			ApplicationServerRegistry.get().getServerManager().removeServerListener(serverListener);
			serverListener = null;
		}

		if (tableListener != null)
		{
			server.removeTableListener(tableListener);
			tableListener = null;
		}

		if (columnListener != null)
		{
			table.removeIColumnListener(columnListener);
			columnListener = null;
		}

		revert();
		super.dispose();
	}

	/**
	 *
	 */
	private void disposeDynamicPages()
	{
		if (calculationsComposite != null)
		{
			removePage(getPageIndex(calculationsComposite));
			this.calculationsComposite.dispose();
			calculationsComposite = null;
		}
		if (methodsComposite != null)
		{
			removePage(getPageIndex(methodsComposite));
			this.methodsComposite.dispose();
			methodsComposite = null;
		}
		if (aggregationsComposite != null)
		{
			removePage(getPageIndex(aggregationsComposite));
			aggregationsComposite.dispose();
			aggregationsComposite = null;
		}
		if (eventsComposite != null)
		{
			removePage(getPageIndex(eventsComposite));
			eventsComposite.dispose();
			eventsComposite = null;
		}
		if (securityComposite != null)
		{
			removePage(getPageIndex(securityComposite));
			securityComposite.dispose();
			securityComposite = null;
		}
		if (dataComposite != null)
		{
			removePage(getPageIndex(dataComposite));
			dataComposite.dispose();
			dataComposite = null;
		}
	}

	@Override
	public void removePage(int pageIndex)
	{
		if (pageIndex == -1) return; // ignore pages that were not found
		super.removePage(pageIndex);
	}

	private void createDynamicPages()
	{
		if (!DataSourceUtils.VIEW_DATASOURCE.equals(server.getName()))
		{
			createCalculationsPage();
			createFoundsSetMethodsPage();
			createAggregationsPage();
		}
		createEventsPage();
		if (!DataSourceUtils.VIEW_DATASOURCE.equals(server.getName()))
		{
			createSecurityPage();
			createDataPage();
		}
	}

	@Override
	protected void pageChange(int newPageIndex)
	{
		super.pageChange(newPageIndex);
		Object newControl = getControl(newPageIndex);
		if (newControl == null) return;

		if (newControl == calculationsComposite)
		{
			calculationsComposite.refresh();
		}
		else if (newControl == methodsComposite)
		{
			methodsComposite.refresh();
		}
		else if (newControl == dataComposite)
		{
			dataComposite.show();
		}
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (ITable.class.equals(adapter))
		{
			return getTable();
		}
		if (adapter.equals(IGotoMarker.class))
		{
			return new IGotoMarker()
			{
				public void gotoMarker(IMarker marker)
				{
					try
					{
						String columnName = (String)marker.getAttribute("columnName");
						if (columnName != null)
						{
							IColumn column = getTable().getColumn(columnName);
							if (column != null && columnComposite != null)
							{
								columnComposite.selectColumn(column);
							}
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			};
		}
		if (adapter == IPropertySourceProvider.class)
		{
			return new IPropertySourceProvider()
			{
				public IPropertySource getPropertySource(Object object)
				{
					if (object instanceof TableNode)
					{
						return PersistPropertySource.createPersistPropertySource((TableNode)object, (TableNode)object, false);
					}
					return null;
				}
			};
		}
		if (adapter.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider("com.servoy.eclipse.ui.table_editor");
		}
		return super.getAdapter(adapter);
	}

	public ITable getTable()
	{
		return table;
	}

	public void selectColumn(IColumn column)
	{
		if (column != null)
		{
			if (columnComposite != null && column instanceof Column)
			{
				setActivePage(getPageIndex(columnComposite));
				columnComposite.selectColumn(column);
			}
			else if (aggregationsComposite != null && column instanceof AggregateVariable)
			{
				setActivePage(getPageIndex(aggregationsComposite));
				aggregationsComposite.selectColumn((AggregateVariable)column);
			}
		}
	}

	private void updateTitle()
	{
		IEditorInput input = getEditorInput();
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		super.setInput(input);
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			TableEditorInput tableInput = (TableEditorInput)input;
			table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(tableInput.getDataSource());

			if (table == null)
			{
				throw new RuntimeException("Could not initialize table editor table could not be found");
			}
			isModified = isModified || !table.getExistInDB() || (table instanceof AbstractMemTable && ((AbstractMemTable)table).isChanged());

			IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();

			server = (IServerInternal)serverManager.getServer(table.getServerName(), true, true);
			if (server == null)
			{
				if (DataSourceUtils.INMEM_DATASOURCE.equals(table.getServerName()))
				{
					MemTable memTable = (MemTable)table;
					server = memTable.getParent();
				}
				else if (DataSourceUtils.VIEW_DATASOURCE.equals(table.getServerName()))
				{
					ViewFoundsetTable viewTable = (ViewFoundsetTable)table;
					server = viewTable.getParent();
				}
				if (server == null)
				{
					throw new RuntimeException("Could not initialize table editor server is not enabled or valid");
				}
			}

			// close this editor if the server or table get deleted
			tableListener = new ITableListener.TableListener()
			{
				public void tablesRemoved(IServerInternal s, ITable tables[], boolean delete)
				{
					for (ITable t : tables)
					{
						if (t.getName().equalsIgnoreCase(table.getName()))
						{
							closeEditor(false);
						}
					}
				}

				@Override
				public void serverStateChanged(IServerInternal s, int oldState, int newState)
				{
					if ((newState & (VALID | ENABLED)) != (VALID | ENABLED))
					{
						// the server is now either disabled or invalid
						closeEditor(false);
					}
				}
			};
			server.addTableListener(tableListener);
			serverListener = new IServerListener()
			{

				public void serverAdded(IServerInternal s)
				{
				}

				public void serverRemoved(IServerInternal s)
				{
					if (s == server)
					{
						closeEditor(false);
					}
				}
			};
			serverManager.addServerListener(serverListener);

			columnListener = new IItemChangeListener<IColumn>()
			{
				public void itemChanged(IColumn column)
				{
					itemChanged(Collections.singletonList(column));
				}

				public void itemChanged(Collection<IColumn> columns)
				{
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							refresh();
						}
					}, false);
				}

				public void itemCreated(IColumn column)
				{
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							refresh();
						}
					}, false);
				}

				public void itemRemoved(IColumn column)
				{
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							refresh();
						}
					}, false);
				}
			};
			table.addIColumnListener(columnListener);

			persistListener = new IPersistChangeListener()
			{

				public void persistChanges(Collection<IPersist> changes)
				{
					IDataSourceManager dsm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataSourceManager();
					for (IPersist persist : changes)
					{
						try
						{
							if (persist instanceof TableNode && table.equals(dsm.getDataSource(((TableNode)persist).getDataSource())))
							{
								final boolean changed = persist.isChanged();
								UIUtils.runInUI(new Runnable()
								{
									public void run()
									{
										refresh();
										flagModified(!changed);
									}
								}, false);
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			};

			ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, persistListener);
		}
		else
		{

			delayedLoad = true;
		}
	}

	@Override
	public void setFocus()
	{
		getContainer().setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (isModified)
		{
			try
			{
				try
				{
					validateColumnSettings();
				}
				catch (Exception e)
				{
					MessageDialog.openError(getSite().getShell(), "Error", "Save failed: " + e.getMessage());
					if (monitor != null) monitor.setCanceled(true);
					return;
				}
				//passed validation
				if (columnComposite != null)
				{
					columnComposite.checkValidState();
				}
				if (columnComposite != null && table instanceof Table && columnComposite.shouldDropTable())
				{
					try
					{
						server.dropTable((Table)table);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				if (propertiesComposite != null)
				{
					propertiesComposite.saveValues();
				}
				if (!(table instanceof AbstractMemTable))
				{
					if (table.getRowIdentColumnsCount() == 0)
					{
						MessageDialog.openWarning(getSite().getShell(), "Warning", "No primary key specified on table.");
						table.setTableInvalidInDeveloperBecauseNoPk(true);
						//server.setTableMarkedAsHiddenInDeveloper(table.getName(), true);
					}
					else if (table.isTableInvalidInDeveloperBecauseNoPk())
					{
						table.setTableInvalidInDeveloperBecauseNoPk(false);
						//server.setTableMarkedAsHiddenInDeveloper(table.getName(), false);
					}
				}
				for (Column column : table.getColumns())
				{
					if (column.getColumnInfo() != null)
					{
						if (column.getColumnInfo().getValidatorName() != null && column.getColumnInfo().getValidatorProperties() == null)
						{
							IColumnValidator validator = ApplicationServerRegistry.get().getPluginManager().getColumnValidatorManager().getValidator(
								column.getColumnInfo().getValidatorName());
							if (validator != null && validator.getDefaultProperties() != null && validator.getDefaultProperties().size() > 0)
							{
								throw new RepositoryException(
									"Column " + column.getName() + " has validator " + validator.getName() + " that doesn't have properties filled in.");
							}
						}
						if (column.getColumnInfo().getConverterName() != null && column.getColumnInfo().getConverterProperties() == null)
						{
							IColumnConverter converter = ApplicationServerRegistry.get().getPluginManager().getColumnConverterManager().getConverter(
								column.getColumnInfo().getConverterName());
							if (converter != null && converter.getDefaultProperties() != null)
							{
								throw new RepositoryException(
									"Column " + column.getName() + " has converter " + converter.getName() + " that doesn't have properties filled in.");
							}
						}
					}
				}

				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				servoyModel.flushDataProvidersForTable(table);

				server.syncTableObjWithDB(table, false, true);
				if (securityComposite != null) securityComposite.saveValues();
				ServoyProject servoyProject = servoyModel.getActiveProject();
				if (servoyProject != null)
				{
					Solution solution = servoyProject.getEditingSolution();
					if (solution != null)
					{
						servoyProject.saveEditingSolutionNodes(Utils.asArray(solution.getTableNodes(table), IPersist.class), true);
						Solution[] modules = ModelUtils.getEditingFlattenedSolution(solution).getModules();
						if (modules != null)
						{
							for (Solution module : modules)
							{
								ServoyProject project = servoyModel.getServoyProject(module.getName());
								if (project != null)
								{
									solution = project.getEditingSolution();
									if (solution != null)
									{
										project.saveEditingSolutionNodes(Utils.asArray(solution.getTableNodes(table), IPersist.class), true);
									}
								}
							}
						}
					}
				}

				isModified = false;
				firePropertyChange(IEditorPart.PROP_DIRTY);
				if (flushTable)
				{
					flushTable = false;
					Activator.getDefault().getDebugClientHandler().refreshDebugClients(table);
				}
				columnComposite.refreshSelection();
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				// ErrorDialog.openError(parent, dialogTitle, message, new
				// IStatus(){});
				MessageDialog.openError(getSite().getShell(), "Error", "Save failed: " + e.getMessage());
				if (monitor != null) monitor.setCanceled(true);
			}
		}
	}

	private void validateColumnSettings() throws Exception
	{
		for (Column col : table.getColumns())
		{
			int colType = Column.mapToDefaultType(col.getConfiguredColumnType().getSqlType());
			// check UUID generator valid types
			if (col.getColumnInfo() != null && col.getSequenceType() == ColumnInfo.UUID_GENERATOR)
			{
				if (!col.getColumnInfo().hasFlag(IBaseColumn.UUID_COLUMN))
				{
					throw new Exception("Column '" + col.getName() +
						"' has sequence type of 'UUID generator' , the column type should be TEXT(36), MEDIA(16) or UUD(DB Native) with an UUID flag set.");
				}
				if (colType != IColumnTypes.TEXT && colType != IColumnTypes.MEDIA)
				{
					throw new Exception(
						"Column '" + col.getName() + "' has sequence type as UUID generator and is only supported for TEXT and MEDIA column types.");
				}
				else if (col.getLength() > 0 &&
					((colType == IColumnTypes.MEDIA && col.getLength() < 16) || (colType == IColumnTypes.TEXT && col.getLength() < 36)))
				{
					throw new Exception(
						"Column '" + col.getName() + "' with sequence type UUID generator has length too small (a minimum of 16 for MEDIA and 36 for TEXT).");
				}
			}
		}
	}

	/**
	 *
	 */
	public void refresh()
	{
		flagModified(true);
		if (propertiesComposite != null && !propertiesComposite.isDisposed()) propertiesComposite.refresh();
		if (columnComposite != null && !columnComposite.isDisposed()) columnComposite.refreshViewer(table);
		if (eventsComposite != null && !eventsComposite.isDisposed()) eventsComposite.refreshViewer(table);
	}

	public void flagModified()
	{
		flagModified(false);
	}

	public void flagModified(boolean checkChanges)
	{
		boolean modified = true;
		if (checkChanges)
		{

			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			DataModelManager dmm = servoyModel.getDataModelManager();
			if (dmm != null)
			{
				if (table instanceof AbstractMemTable)
				{
					modified = ((AbstractMemTable)table).isChanged();
				}
				else
				{
					IFile dbiFile = dmm.getDBIFile(table.getServerName(), table.getName());
					InputStream is = null;
					try
					{
						if (dbiFile != null && dbiFile.exists())
						{
							is = dbiFile.getContents(true);
							String disk = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
							String mem = dmm.serializeTable(table, false);
							modified = !mem.equals(disk);
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
					finally
					{
						try
						{
							is.close();
						}
						catch (Exception e)
						{
						}
					}
				}
			}
		}
		if (modified != isModified)
		{
			isModified = modified;
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					firePropertyChange(IEditorPart.PROP_DIRTY);
				}
			});
		}
	}

	private boolean flushTable = false;

	public void flushTable()
	{
		flushTable = true;
	}

	@Override
	public boolean isDirty()
	{
		return isModified || super.isDirty();
	}

	@Override
	public boolean isSaveOnCloseNeeded()
	{
		// make sure cell editors loose focus so that value is applied
		getSite().getShell().forceFocus();
		return super.isSaveOnCloseNeeded();
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void doSaveAs()
	{
		// not supported, never called
	}

	/**
	 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectChanged(com.servoy.eclipse.model.nature.ServoyProject)
	 */
	public void activeProjectChanged(ServoyProject activeProject)
	{
		if (delayedLoad)
		{
			delayedLoad = false;
			// place hoder, replace it and create the normal pages
			Display.getDefault().asyncExec(() -> {
				setInput(getEditorInput());
				removePage(0);
				createPages();
				setActivePage(0);
			});
		}
	}

	/**
	 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectUpdated(com.servoy.eclipse.model.nature.ServoyProject, int)
	 */
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		if (delayedLoad) return;
		if (updateInfo == IActiveProjectListener.MODULES_UPDATED || updateInfo == IActiveProjectListener.RESOURCES_UPDATED_ON_ACTIVE_PROJECT ||
			updateInfo == IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED || updateInfo == IActiveProjectListener.COLUMN_INFO_CHANGED ||
			updateInfo == IActiveProjectListener.SECURITY_INFO_CHANGED || updateInfo == IActiveProjectListener.SCOPE_NAMES_CHANGED)
		{
			// avoid invalid thread access exceptions
			getSite().getShell().getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					Object activeOrder = null;
					int activePage = getActivePage();
					if (activePage >= 0)
					{
						Control activeControl = getControl(activePage);
						if (activeControl != null)
						{
							activeOrder = activeControl.getData("order");
						}
					}

					disposeDynamicPages();
					createDynamicPages();

					if (activeOrder != null)
					{
						int pages = getPageCount();
						for (int i = 0; i < pages; i++)
						{
							if (activeOrder.equals(getControl(i).getData("order")))
							{
								setActivePage(i);
								break;
							}
						}
					}
				}
			});
		}
	}

	/**
	 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectWillChange(com.servoy.eclipse.model.nature.ServoyProject, com.servoy.eclipse.model.nature.ServoyProject)
	 */
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		if (delayedLoad) return true;
		closeEditor(true);
		return true;
	}

	protected void closeEditor(final boolean save)
	{
		getSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				getSite().getPage().closeEditor(TableEditor.this, save);
			}
		});
	}

}
