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

import java.sql.SQLException;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.TableEditorInput;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.table.AggregationsComposite;
import com.servoy.eclipse.ui.editors.table.CalculationsComposite;
import com.servoy.eclipse.ui.editors.table.ColumnComposite;
import com.servoy.eclipse.ui.editors.table.DataComposite;
import com.servoy.eclipse.ui.editors.table.EventsComposite;
import com.servoy.eclipse.ui.editors.table.FoundsetMethodsComposite;
import com.servoy.eclipse.ui.editors.table.SecurityComposite;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.Utils;

public class TableEditor extends MultiPageEditorPart implements IActiveProjectListener
{
	public static final String ID = "com.servoy.eclipse.ui.editors.TableEditor"; //$NON-NLS-1$

	private boolean isModified;

	private IServerInternal server;

	private Table table;

	private ColumnComposite columnComposite;

	private DataComposite dataComposite;

	private CalculationsComposite calculationsComposite;

	private FoundsetMethodsComposite methodsComposite;

	private AggregationsComposite aggregationsComposite;

	private EventsComposite eventsComposite;

	private SecurityComposite securityComposite;

	private ITableListener tableListener;

	private IServerListener serverListener;

	private final Table templateTable = null;

	public static int ColumnPageIndex = 0;

	public static int CalculationsPageIndex = 1;

	public static int MethodsPageIndex = 2;

	public static int AggregationsPageIndex = 3;

	public static int EventsPageIndex = 4;

	public static int SecurityPageIndex = 5;

	public static int DataPageIndex = 6;

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
		createColumnPage();
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			createCalculationsPage();
			createFoundsSetMethodsPage();
			createAggregationsPage();
			createEventsPage();
			createSecurityPage();
			createDataPage();
		}
		updateTitle();
	}

	private void createColumnPage()
	{
		columnComposite = new ColumnComposite(this, getContainer(), ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(),
			SWT.None);
		addPage(TableEditor.ColumnPageIndex, columnComposite);
		setPageText(TableEditor.ColumnPageIndex, "Columns");
	}

	private void createDataPage()
	{
		if (com.servoy.eclipse.core.Activator.getDefault().isSqlExplorerLoaded())
		{
			dataComposite = new DataComposite(getContainer(), table);
			addPage(TableEditor.DataPageIndex, dataComposite);
			setPageText(TableEditor.DataPageIndex, "Data");
		}
	}

	public void revert()
	{
		if (isDirty())
		{
			try
			{
				if (server != null)
				{
					Iterator<Column> it = getTable().getColumns().iterator();
					while (it.hasNext())
					{
						it.next().removeColumnInfo();
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
				ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
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
		// seems that pages added by add(Control) are not disposed by default like
		// the ones aded with add(editor)
		// we must dispose of them - or they remain as listeners that want to
		// update disposed UI
		if (columnComposite != null) columnComposite.dispose();
		if (securityComposite != null) securityComposite.dispose();
		disposePages();

		if (serverListener != null)
		{
			ServoyModel.getServerManager().removeServerListener(serverListener);
			serverListener = null;
		}

		if (tableListener != null)
		{
			server.removeTableListener(tableListener);
			tableListener = null;
		}
		revert();
		super.dispose();
	}

	/**
	 * 
	 */
	private void disposePages()
	{
		if (calculationsComposite != null)
		{
			removePage(calculationsComposite);
			this.calculationsComposite.dispose();
		}
		if (methodsComposite != null)
		{
			removePage(methodsComposite);
			this.methodsComposite.dispose();
		}
		if (aggregationsComposite != null)
		{
			removePage(aggregationsComposite);
			aggregationsComposite.dispose();
		}
		if (eventsComposite != null)
		{
			removePage(eventsComposite);
			eventsComposite.dispose();
		}
		if (dataComposite != null)
		{
			removePage(dataComposite);
			dataComposite.dispose();
		}
	}

	private void removePage(Control page)
	{
		int pageCount = this.getPageCount();
		for (int i = 0; i < pageCount; i++)
		{
			Control currentPage = this.getControl(i);
			if (currentPage == page)
			{
				this.removePage(i);
				break;
			}
		}
	}

	private void createAggregationsPage()
	{
		aggregationsComposite = new AggregationsComposite(this, getContainer(),
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None);
		addPage(TableEditor.AggregationsPageIndex, aggregationsComposite);
		setPageText(TableEditor.AggregationsPageIndex, "Aggregations");
	}

	private void createCalculationsPage()
	{
		calculationsComposite = new CalculationsComposite(this, getContainer(),
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None);
		addPage(TableEditor.CalculationsPageIndex, calculationsComposite);
		setPageText(TableEditor.CalculationsPageIndex, "Calculations");
	}

	private void createFoundsSetMethodsPage()
	{
		methodsComposite = new FoundsetMethodsComposite(this, getContainer(),
			ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution(), SWT.None);
		addPage(TableEditor.MethodsPageIndex, methodsComposite);
		setPageText(TableEditor.MethodsPageIndex, "Methods");
	}

	@Override
	protected void pageChange(int newPageIndex)
	{
		super.pageChange(newPageIndex);
		if (newPageIndex == CalculationsPageIndex && calculationsComposite != null)
		{
			calculationsComposite.refresh();
		}
		if (newPageIndex == MethodsPageIndex && methodsComposite != null)
		{
			methodsComposite.refresh();
		}
		if (newPageIndex == DataPageIndex && dataComposite != null)
		{
			dataComposite.show();
		}
	}

	private void createEventsPage()
	{
		eventsComposite = new EventsComposite(this, getContainer(), SWT.None);
		addPage(TableEditor.EventsPageIndex, eventsComposite);
		setPageText(TableEditor.EventsPageIndex, "Events");
	}

	private void createSecurityPage()
	{
		securityComposite = new SecurityComposite(getContainer(), SWT.None, this);
		addPage(TableEditor.SecurityPageIndex, securityComposite);
		setPageText(TableEditor.SecurityPageIndex, "Security");
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (Table.class.equals(adapter))
		{
			return getTable();
		}
		return super.getAdapter(adapter);
	}

	public Table getTable()
	{
		return table;
	}

	public void selectColumn(IColumn column)
	{
		if (column != null)
		{
			if (columnComposite != null && column instanceof Column)
			{
				setActivePage(ColumnPageIndex);
				columnComposite.selectColumn((Column)column);
			}
			else if (aggregationsComposite != null && column instanceof AggregateVariable)
			{
				setActivePage(AggregationsPageIndex);
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
		TableEditorInput tableInput = (TableEditorInput)input;

		server = (IServerInternal)ServoyModel.getServerManager().getServer(tableInput.getServerName(), true, true);
		if (server == null)
		{
			throw new RuntimeException("Could not initialize table editor server is not enabled or valid");
		}

		try
		{
			table = server.getTable(tableInput.getName());
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			throw new RuntimeException("Could not initialize table editor: " + e.getMessage());
		}

		if (table == null)
		{
			throw new RuntimeException("Could not initialize table editor table could not be found");
		}

		// close this editor if the server or table get deleted
		IServerManagerInternal serverManager = ServoyModel.getServerManager();
		tableListener = new ITableListener()
		{
			public void tablesAdded(IServer s, String tableNames[])
			{
			}

			public void tablesRemoved(IServer s, Table tables[], boolean delete)
			{
				for (Table t : tables)
				{
					if (t.getName().equalsIgnoreCase(table.getName()))
					{
						closeEditor(false);
					}
				}
			}

			public void serverStateChanged(IServer s, int oldState, int newState)
			{
				if ((newState & (VALID | ENABLED)) != (VALID | ENABLED))
				{
					// the server is now either disabled or invalid
					closeEditor(false);
				}
			}

			public void tableInitialized(Table t)
			{
				// not interested
			}
		};
		server.addTableListener(tableListener);
		serverListener = new IServerListener()
		{

			public void serverAdded(IServer s)
			{
			}

			public void serverRemoved(IServer s)
			{
				if (s == server)
				{
					closeEditor(false);
				}
			}
		};
		serverManager.addServerListener(serverListener);

		table.addIColumnListener(new IColumnListener()
		{
			public void iColumnChanged(IColumn column)
			{
				flagModified();
			}

			public void iColumnCreated(IColumn column)
			{
				flagModified();
			}

			public void iColumnRemoved(IColumn column)
			{
				flagModified();
			}
		});

		// servoyProject =
		// ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(persistInput.getSolutionName());
		// persist = servoyProject.getEditingPersist(persistInput.getUuid(),
		// revertToOriginal);
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
				if (columnComposite != null)
				{
					columnComposite.checkValidState();
				}
				if (table.getRowIdentColumnsCount() == 0)
				{
					MessageDialog.openWarning(getSite().getShell(), "Warning", "No primary key specified on table.");
				}

				ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				servoyModel.flushDataProvidersForTable(table);

//				IColumnInfoManager cim = servoyModel.getDeveloperRepository().getColumnInfoManager();
				server.syncTableObjWithDB(table, false, true, null);
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
				if (Activator.getDefault().getDebugClientHandler().isClientStarted())
				{
					MessageDialog.openInformation(getSite().getShell(), "Client Restart Required",
						"Web Client must be restarted (if still open) in order for the changes to be effective.");
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

	public void flagModified()
	{
		isModified = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
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
	}

	/**
	 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectUpdated(com.servoy.eclipse.model.nature.ServoyProject, int)
	 */
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		// avoid invalid thread access exceptions
		getSite().getShell().getDisplay().syncExec(new Runnable()
		{
			public void run()
			{
				int activePage = getActivePage();
				disposePages();
				createCalculationsPage();
				createFoundsSetMethodsPage();
				createAggregationsPage();
				createEventsPage();
				createDataPage();
				if (activePage >= 0)
				{
					setActivePage(activePage);
				}

			}
		});
	}

	/**
	 * @see com.servoy.eclipse.model.nature.IActiveProjectListener#activeProjectWillChange(com.servoy.eclipse.model.nature.ServoyProject, com.servoy.eclipse.model.nature.ServoyProject)
	 */
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
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
