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
package com.servoy.eclipse.core.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.security.RemoveAccessMaskForMissingElement;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnListener;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;

/**
 * This class manages security information when running from an Eclipse-Servoy developer.
 * Is aware of things that change when developing. 
 * @author acostescu
 */
public class EclipseUserManager extends WorkspaceUserManager
{

	// listeners to model/server/table changes
	private ITableListener tableListener;
	private IServerListener serverListener;
	private IPersistChangeListener persistChangeListener;
	private IColumnListener columnListener;

	/**
	 * If this method is called, the user manager will listen for form/form element/table/table column add/remove/modify events and will update in-memory
	 * security model and/or security files. For example when a table/form is deleted by the user, delete it's sec file, when a table/form is added check to see
	 * if a sec file already exists for those and read it, ...
	 * 
	 * These updates ignore the write mode - so these changes will be applied even if write mode is WRITE_MODE_MANUAL. If you do not want this, then do not call
	 * this method.
	 */
	public void setFormAndTableChangeAware()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		// listen for TABLE / COLUMN changes
		IServerManagerInternal serverManager = ServoyModel.getServerManager();
		columnListener = new IColumnListener()
		{
			public void iColumnChanged(IColumn column)
			{
				try
				{
					refreshLoadedAccessRights(column.getTable());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			public void iColumnCreated(IColumn column)
			{
				try
				{
					refreshLoadedAccessRights(column.getTable());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			public void iColumnRemoved(IColumn column)
			{
				try
				{
					refreshLoadedAccessRights(column.getTable());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		};

		tableListener = new ITableListener()
		{
			public void tablesAdded(IServer server, String tableNames[])
			{
				try
				{
					for (String tableName : tableNames)
					{
						if (((IServerInternal)server).isTableLoaded(tableName))
						{
							(((IServerInternal)server).getTable(tableName)).addIColumnListener(columnListener);
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				if (isOperational() && (!readingAllTableInfo))
				{
					try
					{
						for (String tableName : tableNames)
						{
							removeSecurityInfoFromMemory(((IServerInternal)server).getName(), tableName);
							readSecurityInfo(((IServerInternal)server).getName(), tableName);
						}
					}
					catch (RepositoryException e)
					{
						setReadError(e);
					}
				}
			}

			public void tablesRemoved(IServer server, Table tables[], boolean deleted)
			{
				for (Table table : tables)
				{
					table.removeIColumnListener(columnListener);
				}
				if (deleted && resourcesProject != null)
				{
					// delete .sec files as well if tables were deleted by user
					try
					{
						for (Table table : tables)
						{
							IPath path = new Path(DataModelManager.getRelativeServerPath(((IServerInternal)server).getName()) + IPath.SEPARATOR +
								getFileName(table.getName()));
							IFile file = resourcesProject.getFile(path);
							if (file.exists())
							{
								file.delete(true, null);
							}
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
				else if (isOperational())
				{
					// remove sec. info we have about this table
					for (Table table : tables)
					{
						reloadSecurityInfo(((IServerInternal)server).getName(), table.getName());
					}
				}
			}

			public void serverStateChanged(IServer server, int oldState, int newState)
			{
				// do nothing
			}

			public void tableInitialized(Table t)
			{
				t.addIColumnListener(columnListener);
			}

			public void hiddenTableChanged(IServer server, Table table)
			{
				// nothing to do here				
			}

		};

		// add listeners to initial servers & tables
		String[] array = serverManager.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal server = (IServerInternal)serverManager.getServer(server_name, false, false);
			server.addTableListener(tableListener);
			if (server.getConfig().isEnabled() && server.isValid())
			{
				try
				{
					for (String tableName : server.getTableAndViewNames(false))
					{
						if (server.isTableLoaded(tableName))
						{
							(server.getTable(tableName)).addIColumnListener(columnListener);
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		// monitor changes in server list
		serverListener = new IServerListener()
		{

			public void serverAdded(IServer s)
			{
				((IServerInternal)s).addTableListener(tableListener);
			}

			public void serverRemoved(IServer s)
			{
				((IServerInternal)s).removeTableListener(tableListener);
			}
		};
		serverManager.addServerListener(serverListener);


		// listen for FORM / FORM ELEMENT changes
		persistChangeListener = new IPersistChangeListener()
		{
			public void persistChanges(Collection<IPersist> changes)
			{
				Set<IPersist> reloadedForms = null;
				for (IPersist persist : changes)
				{
					if (persist instanceof Form)
					{
						if (((Solution)persist.getRootObject()).getForm(persist.getID()) == null)
						{
							// this means the form was deleted; delete dbi file as well
							IPath path = new Path(SolutionSerializer.getRelativePath(persist, false) + IPath.SEPARATOR + getFileName((Form)persist));
							// the path is relative to the solution project; so get the solution's project
							IFile file = ServoyModel.getWorkspace().getRoot().getFile(path);
							if (file.exists())
							{
								try
								{
									boolean reload = false;
									if (file.findMarkers(null, true, IResource.DEPTH_ZERO).length > 0 && readError != null && resourcesProject != null)
									{
										// there was an error on this file that prevented user manager to load sec. info; we must reload after delete to see if the error was solved
										reload = true;
									}
									file.delete(true, null); // because writingResources is not set to true this will result in the permission info being unloaded from memory as well
									if (reload)
									{
										reloadAllSecurityInformation();
									}
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
							}
						}
						else
						{
							synchronized (EclipseUserManager.this)
							{
								// the form has changed but has not been deleted - maybe it was added (no way to know currently with this listener);
								// in case it was just added we must load the sec. info for it
								Iterator<GroupSecurityInfo> it = groupInfos.iterator();
								boolean alreadyLoaded = false;
								while (it.hasNext())
								{
									GroupSecurityInfo gsi = it.next();
									List<SecurityInfo> tmp = gsi.formSecurity.get(persist.getUUID());
									if (tmp != null && tmp.size() > 0)
									{
										alreadyLoaded = true;
										break;
									}
								}
								if (!alreadyLoaded)
								{
									reloadSecurityInfo((Form)persist);
								}
							}
						}
					}
					else if (persist instanceof IFormElement)
					{
						IPersist parent = persist.getParent();
						if (parent instanceof Form)
						{
							if (((Form)parent).getChild(persist.getUUID()) == null)
							{
								synchronized (EclipseUserManager.this)
								{
									// element of form was deleted; delete security entries for it as well
									IPath path = new Path(SolutionSerializer.getRelativePath(parent, false) + IPath.SEPARATOR + getFileName((Form)parent));
									IFile file = ServoyModel.getWorkspace().getRoot().getFile(path);

									RemoveAccessMaskForMissingElement quickFix = RemoveAccessMaskForMissingElement.getInstance();
									quickFix.setSilent(true);
									try
									{
										quickFix.run(SecurityReadException.MISSING_ELEMENT, persist.getUUID().toString(), file);
									}
									finally
									{
										quickFix.setSilent(false);
									}
								}
							}
							else
							{
								if (reloadedForms == null)
								{
									reloadedForms = new HashSet<IPersist>();
								}
								if (!reloadedForms.contains(parent))
								{
									// maybe it was added, so reload
									reloadSecurityInfo((Form)parent);
									reloadedForms.add(parent);
								}
							}
						}
					}
				}
			}
		};
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(true, persistChangeListener);
	}

	@Override
	public void dispose()
	{
		if (serverListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyModel.getServerManager().removeServerListener(serverListener);
			serverListener = null;
		}

		if (tableListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel();
			IServerManagerInternal serverManager = ServoyModel.getServerManager();
			// add listeners to initial server list
			String[] array = serverManager.getServerNames(false, false, true, true);
			for (String server_name : array)
			{
				IServerInternal server = (IServerInternal)serverManager.getServer(server_name, false, false);
				server.removeTableListener(tableListener);

				if (server.getConfig().isEnabled() && server.isValid())
				{
					try
					{
						for (String tableName : server.getTableNames(false))
						{
							if (server.isTableLoaded(tableName))
							{
								(server.getTable(tableName)).removeIColumnListener(columnListener);
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			tableListener = null;
			columnListener = null;
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(true, persistChangeListener);
	}

}