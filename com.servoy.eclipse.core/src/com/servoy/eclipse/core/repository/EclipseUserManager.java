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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.security.RemoveAccessMaskForMissingElement;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.PersistFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.GroupSecurityInfo;
import com.servoy.j2db.server.shared.IClientInternal;
import com.servoy.j2db.server.shared.IClientManagerInternal;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;

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
	private IItemChangeListener<IColumn> columnListener;

	/** Check if user is administrator.
	 * <p> Some operations can only be done by admin user or own user (like change own password)
	 * @param clientId
	 * @param ownerUserId allowed when non-null
	 * @throws RepositoryException
	 */
	@Override
	public void checkForAdminUser(String clientId, String ownerUserId) throws RepositoryException
	{
		if (ApplicationServerRegistry.get().getClientId().equals(clientId))
		{
			// internal: ok
			return;
		}

		if (!ServoyModel.isClientRepositoryAccessAllowed())
		{
			// check if user is in admin group
			IClientManagerInternal clientManager = ((IDataServerInternal)ApplicationServerRegistry.get().getDataServer()).getClientManager();
			IClientInternal client = clientManager.getClient(clientId);
			if (client == null || client.getClientInfo().getUserGroups() == null ||
				!Arrays.asList(client.getClientInfo().getUserGroups()).contains(IRepository.ADMIN_GROUP))
			{
				// non-admin user, check for own user
				if (ownerUserId == null || !ownerUserId.equals(client.getClientInfo().getUserUid()))
				{
					Debug.error("Access to repository server denied to client code, see admin property " + Settings.ALLOW_CLIENT_REPOSITORY_ACCESS_SETTING,
						new IllegalAccessException());
					throw new RepositoryException(ServoyException.NO_ACCESS, new Object[] { IRepository.ADMIN_GROUP });
				}
			}
		}

		// ok
	}

	public boolean hasSecurityInfo(IPersist persist)
	{
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		for (GroupSecurityInfo groupSecurityInfo : groupInfos)
		{
			List<SecurityInfo> elementAccess = groupSecurityInfo.formSecurity.get(form.getUUID());
			for (SecurityInfo element : elementAccess)
			{
				if (PersistFinder.INSTANCE.fromPersist(persist).toJSONString().equals(element.element_uid) ||
					persist.getUUID().toString().equals(element.element_uid))
				{
					return true;
				}
			}
		}
		return false;
	}

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
		// listen for TABLE / COLUMN changes
		IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
		columnListener = new IItemChangeListener<IColumn>()
		{
			public void itemChanged(IColumn column)
			{
				itemChanged(Collections.singletonList(column));
			}

			public void itemChanged(Collection<IColumn> columns)
			{
				try
				{
					Set<ITable> set = new HashSet<ITable>();
					for (IColumn col : columns)
					{
						if (set.add(col.getTable())) refreshLoadedAccessRights(col.getTable());
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			public void itemCreated(IColumn column)
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

			public void itemRemoved(IColumn column)
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

		tableListener = new ITableListener.TableListener()
		{
			@Override
			public void tablesAdded(IServerInternal server, String tableNames[])
			{
				try
				{
					for (String tableName : tableNames)
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
				if (isOperational() && (!readingAllTableInfo))
				{
					try
					{
						for (String tableName : tableNames)
						{
							removeSecurityInfoFromMemory(server.getName(), tableName);
							readSecurityInfo(server.getName(), tableName);
						}
					}
					catch (RepositoryException e)
					{
						setReadError(e);
					}
				}
			}

			public void tablesRemoved(IServerInternal server, ITable tables[], boolean deleted)
			{
				for (ITable table : tables)
				{
					table.removeIColumnListener(columnListener);
				}
				if (deleted && resourcesProject != null)
				{
					// delete .sec files as well if tables were deleted by user
					try
					{
						for (ITable table : tables)
						{
							IFile file = dataModelManager.getSecurityFile(server.getName(), table.getName());
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
					for (ITable table : tables)
					{
						reloadSecurityInfo(server.getName(), table.getName());
					}
				}
			}

			@Override
			public void tableInitialized(Table t)
			{
				t.addIColumnListener(columnListener);
			}
		};

		// add listeners to initial servers & tables
		String[] array = serverManager.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal server = (IServerInternal)serverManager.getServer(server_name, false, false);
			server.addTableListener(tableListener);
			if (server.getConfig().isEnabled() && server.isValid() && server.isTableListLoaded())
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

			public void serverAdded(IServerInternal s)
			{
				s.addTableListener(tableListener);
			}

			public void serverRemoved(IServerInternal s)
			{
				s.removeTableListener(tableListener);
			}
		};
		serverManager.addServerListener(serverListener);


		// listen for FORM / FORM ELEMENT changes
		persistChangeListener = new IPersistChangeListener()
		{
			public void persistChanges(Collection<IPersist> changes)
			{
				Set<Form> formsToReload = null;
				for (IPersist persist : changes)
				{
					if (persist instanceof Form)
					{
						if (((Solution)persist.getRootObject()).getForm(persist.getUUID().toString()) == null)
						{
							// this means the form was deleted; delete security file as well
							IPath path = new Path(SolutionSerializer.getRelativePath(persist, false) + IPath.SEPARATOR +
								DataModelManager.getSecurityFileName(((Form)persist).getName()));
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
								boolean alreadyLoaded = false;
								for (GroupSecurityInfo gsi : groupInfos)
								{
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
									IPath path = new Path(SolutionSerializer.getRelativePath(parent, false) + IPath.SEPARATOR +
										DataModelManager.getSecurityFileName(((Form)parent).getName()));
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
								if (formsToReload == null)
								{
									formsToReload = new HashSet<Form>();
								}
								if (!formsToReload.contains(parent))
								{
									// maybe it was added, so reload
									formsToReload.add((Form)parent);
								}
							}
						}
					}
				}
				// first make sure the security file is updated with all deletes, reload form at the end
				if (formsToReload != null)
				{
					for (Form form : formsToReload)
					{
						reloadSecurityInfo(form);
					}
				}
			}
		};
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(true, persistChangeListener);
	}

	@Override
	public void dispose()
	{
		if (ApplicationServerRegistry.get() == null)
		{
			// don't start app server again
			serverListener = null;
			tableListener = null;
			columnListener = null;
		}
		else
		{
			if (serverListener != null)
			{
				ApplicationServerRegistry.get().getServerManager().removeServerListener(serverListener);
				serverListener = null;
			}

			if (tableListener != null)
			{
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				// add listeners to initial server list
				String[] array = serverManager.getServerNames(false, false, true, true);
				for (String server_name : array)
				{
					IServerInternal server = (IServerInternal)serverManager.getServer(server_name, false, false);
					server.removeTableListener(tableListener);

					if (server.getConfig().isEnabled() && server.isValid() && server.isTableListLoaded())
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
		}
		if (persistChangeListener != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(true, persistChangeListener);
			persistChangeListener = null;
		}
	}

}