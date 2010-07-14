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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.commons.dbcp.DbcpException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.builder.ServoyBuilder;
import com.servoy.eclipse.core.quickfix.security.RemoveAccessMaskForMissingElement;
import com.servoy.eclipse.core.util.ResourcesUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnListener;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * This class manages security information when running from an Eclipse-Servoy workspace.
 * @author acostescu
 */
public class EclipseUserManager implements IUserManager
{
	public interface IUserGroupChangeListener
	{
		public void userGroupChanged();
	}

	private final ArrayList<IUserGroupChangeListener> userGroupChangedChangeListeners = new ArrayList<IUserGroupChangeListener>();

	public void addUserGroupChangeListener(IUserGroupChangeListener listener)
	{
		userGroupChangedChangeListeners.add(listener);
	}

	public void removeUserGroupChangeListener(IUserGroupChangeListener listener)
	{
		userGroupChangedChangeListeners.remove(listener);
	}

	private void fireUserGroupChanged()
	{
		for (IUserGroupChangeListener listener : userGroupChangedChangeListeners)
			listener.userGroupChanged();
	}

	public static class SecurityInfo implements Comparable<SecurityInfo>
	{
		public String element_uid;//element UUID or columnname
		public int access;

		public SecurityInfo(String element_uid, int access)
		{
			this.element_uid = element_uid;
			this.access = access;
		}

		public String getName()
		{
			return element_uid;
		}

		// needed in order for remove(Object) to work correctly in a sorted list
		public int compareTo(SecurityInfo o)
		{
			int result;
			if (element_uid == o.element_uid)
			{
				result = 0;
			}
			else if (element_uid == null) result = -1;
			else if (o.element_uid == null) result = 1;
			else result = element_uid.compareToIgnoreCase(o.element_uid); // case insensitive sort

			if (result == 0) result = element_uid.compareTo(o.element_uid); // do not consider equal ones that match case insensitive
			if (result == 0)
			{
				// names are the same; check pwd
				result = access - o.access;
			}

			return result;
		}

		// needed in order for remove(Object) to work correctly in an ArrayList
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof SecurityInfo) return (compareTo((SecurityInfo)o) == 0);
			return false;
		}

	}

	public static class User implements Comparable<User>
	{
		public static final String NAME = "name"; //$NON-NLS-1$
		public static final String PASSWORD_HASH = "password_hash"; //$NON-NLS-1$
		public static final String USER_UID = "user_uid"; //$NON-NLS-1$

		public String name;
		public String passwordHash;
		public String userUid;//mostly uuid

		public User(String name, String password_hash, String user_uid)
		{
			this.name = name;
			this.passwordHash = password_hash;
			this.userUid = user_uid;
		}

		public ServoyJSONObject toJSON() throws JSONException
		{
			ServoyJSONObject obj = new ServoyJSONObject(true, false);
			obj.put(NAME, name);
			obj.put(PASSWORD_HASH, passwordHash);
			obj.put(USER_UID, userUid);
			return obj;
		}

		public static User fromJSON(JSONObject obj) throws JSONException
		{
			String name = obj.getString(NAME);
			String user_uid = obj.getString(USER_UID);
			String password_hash = obj.getString(PASSWORD_HASH);
			return new User(name, password_hash, user_uid);
		}

		public String getName()
		{
			return name;
		}

		@Override
		public String toString()
		{
			return "Name: " + name + ", PWD Hash: " + passwordHash + ", UID: " + userUid; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}

		// needed in order for remove(Object) to work correctly in a sorted list
		public int compareTo(User o)
		{
			int result;
			if (name == o.name)
			{
				result = 0;
			}
			else if (name == null) result = -1;
			else if (o.name == null) result = 1;
			else result = name.compareToIgnoreCase(o.name); // case insensitive sort

			if (result == 0) result = name.compareTo(o.name); // do not consider equal users ones that match case insensitive
			if (result == 0)
			{
				// names are the same; check pwd
				if (passwordHash == o.passwordHash)
				{
					result = 0;
				}
				else if (passwordHash == null) result = -1;
				else if (o.passwordHash == null) result = 1;
				else result = passwordHash.compareTo(o.passwordHash);
				if (result == 0)
				{
					// same pwd; check UID
					if (userUid == o.userUid)
					{
						result = 0;
					}
					else if (userUid == null) result = -1;
					else if (o.userUid == null) result = 1;
					else result = userUid.compareTo(o.userUid);
				}
			}

			return result;
		}

		// needed in order for remove(Object) to work correctly in an ArrayList
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof User) return (compareTo((User)o) == 0);
			return false;
		}

	}

	protected static class GroupSecurityInfo implements ISupportName
	{
		public String groupName;
		public Map<CharSequence, List<SecurityInfo>> tableSecurity = new HashMap<CharSequence, List<SecurityInfo>>();//servername.tablename -> SecurityInfos 
		public Map<UUID, List<SecurityInfo>> formSecurity = new HashMap<UUID, List<SecurityInfo>>();//form_uuid -> SecurityInfos

		/**
		 * Creates a new instance for the group with the given name.<BR>
		 * tableSecurity and formSecurity will be != null and empty.
		 * 
		 * @param groupName the group name.
		 */
		public GroupSecurityInfo(String groupName)
		{
			this.groupName = groupName;
		}

		public String getName()
		{
			return groupName;
		}
	}

	public static class SecurityReadException extends RepositoryException // only extends RepositoryException so it can be thrown in certain inherited methods
	{
		public static final int UNKNOWN = 0;
		public static final int JSON_DESERIALIZE_ERROR = 1;
		public static final int DUPLICATE_USER_NAME = 2;
		public static final int DUPLICATE_USER_UID = 3;
		public static final int INVALID_USER_NAME_OR_PASSWORD = 4;
		public static final int MISSING_USER_REFERENCED_IN_GROUP = 5;
		public static final int INVALID_GROUP_NAME = 7;
		public static final int INVALID_GROUP_NAME_IN_USER_LIST = 8;
		public static final int GROUP_NOT_DECLARED = 9;
		public static final int MISSING_ELEMENT = 10;
		public static final int DUPLICATE_ELEMENT_PERMISSION = 11;

		private final int type;
		private final Object wrongValue;

		private SecurityReadException(int type, String message)
		{
			this(type, message, null);
		}

		private SecurityReadException(int type, String message, Object wrongValue)
		{
			super(message);
			this.type = type;
			this.wrongValue = wrongValue;
		}

		public int getType()
		{
			return type;
		}

		public Object getWrongValue()
		{
			return wrongValue;
		}

	}

	public static final int WRITE_MODE_MANUAL = 0;
	public static final int WRITE_MODE_AUTOMATIC = 1;

	public static final String SECURITY_DIR = "security"; //$NON-NLS-1$
	public static final String SECURITY_FILE_EXTENSION_WITHOUT_DOT = "sec"; //$NON-NLS-1$
	public static final String SECURITY_FILE_EXTENSION = '.' + SECURITY_FILE_EXTENSION_WITHOUT_DOT;
	public static final String SECURITY_FILENAME_WITHOUT_EXTENSION = "security"; //$NON-NLS-1$
	public static final String SECURITY_FILENAME = SECURITY_FILENAME_WITHOUT_EXTENSION + SECURITY_FILE_EXTENSION;
	public static final String SECURITY_FILE_RELATIVE_TO_PROJECT = SECURITY_DIR + IPath.SEPARATOR + SECURITY_FILENAME;

	public static final String MARKER_ATTRIBUTE_TYPE = "SPM_type"; // security problem marker type //$NON-NLS-1$
	public static final String MARKER_ATTRIBUTE_WRONG_VALUE = "SPM_wrong_value"; // security problem marker wrong value (for example the name of an invalid group...) - if not an array //$NON-NLS-1$
	public static final String MARKER_ATTRIBUTE_WRONG_VALUE_ARRAY_LENGTH = "SPM_wrong_value_array_length"; // security problem marker wrong value array length //$NON-NLS-1$
	public static final String MARKER_ATTRIBUTE_WRONG_VALUE_ARRAY = "SPM_wrong_value_array_index_"; // security problem marker wrong value array element //$NON-NLS-1$

	private static final String TABLE_PERMISSION_KEY = "permission"; //$NON-NLS-1$

	public static final String JSON_GROUPS = "groups"; //$NON-NLS-1$
	public static final String JSON_USERS = "users"; //$NON-NLS-1$
	public static final String JSON_USERGROUPS = "usergroups"; //$NON-NLS-1$

	private boolean writingResources = false;

	private final HashMap<Integer, String> idToUUID = new HashMap<Integer, String>();
	private final HashMap<String, Integer> UUIDToId = new HashMap<String, Integer>();
	private int idCounter = 1;

	private final List<GroupSecurityInfo> groupInfos = new SortedList<GroupSecurityInfo>(NameComparator.INSTANCE); //group names
	private final List<User> allDefinedUsers = new SortedList<User>();
	private final Map<String, List<String>> userGroups = new HashMap<String, List<String>>(); //groupname -> list of user_uids

	private int writeMode = WRITE_MODE_AUTOMATIC;
	protected IProject resourcesProject;
	private SecurityReadException readError;
	private IResource lastReadResource;
	private boolean isOperational = false;

	// listeners to model/server/table changes
	private ITableListener tableListener;
	private IServerListener serverListener;
	private IPersistChangeListener persistChangeListener;
	private IColumnListener columnListener;
	private boolean readingAllTableInfo;

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
							((IServerInternal)server).getTable(tableName).addIColumnListener(columnListener);
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
					// delete dbi files as well if tables were deleted by user
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
					for (String tableName : server.getTableNames())
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

	protected void refreshLoadedAccessRights(Table table)
	{
		if (isOperational())
		{
			boolean changed = false;
			Iterator<GroupSecurityInfo> it = groupInfos.iterator();
			while (it.hasNext())
			{
				GroupSecurityInfo groupSecurityInfo = it.next();
				List<SecurityInfo> infos = groupSecurityInfo.tableSecurity.get(Utils.getDotQualitfied(table.getServerName(), table.getName()));
				if (infos != null && infos.size() > 0)
				{
					int access = infos.get(0).access;
					changed = changed || isTableSecurityChanged(table, infos);
					if (changed)
					{
						infos.clear();
						for (String columnName : table.getColumnNames())
						{
							setSecurityInfo(infos, columnName, access, false);
						}
					}
					if (table.getColumnNames().length == 0) setSecurityInfo(infos, TABLE_PERMISSION_KEY, access, false); // if a new table is created and a sec file already exists for it, remember permission even if no columns are available for it yet...
				}
			}
			if (changed)
			{
				try
				{
					writeSecurityInfo(table.getServerName(), table.getName(), true);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	private boolean isTableSecurityChanged(Table table, List<SecurityInfo> infos)
	{
		if (table.getColumnCount() != infos.size()) return true;
		for (String columnName : table.getColumnNames())
		{
			boolean columnFound = false;
			Iterator<SecurityInfo> it = infos.iterator();
			while (it.hasNext())
			{
				if (it.next().element_uid.equals(columnName))
				{
					columnFound = true;
					break;
				}
			}
			if (!columnFound) return true;
		}
		return false;
	}

	public static String getFileName(String name)
	{
		if (name == null) return null;

		return name + SECURITY_FILE_EXTENSION;
	}

	public static String getFileName(ISupportName t)
	{
		if (t == null) return null;

		return t.getName() + SECURITY_FILE_EXTENSION;
	}

	private int getNextId()
	{
		return idCounter++;
	}

	/**
	 * Get (create if necessary) the id for given UUID. Note that as groups do not have UUIDs but have ids, group names are passed to this methos as UUIDs.
	 * 
	 * @param UUID the UUID for which an id must be created.
	 * @return the id.
	 */
	private int getId(String UUID)
	{
		if (UUIDToId.containsKey(UUID))
		{
			return UUIDToId.get(UUID).intValue();
		}
		else
		{
			if (UUID == null) return -1;
			Integer id = new Integer(getNextId());
			UUIDToId.put(UUID, id);
			idToUUID.put(id, UUID);
			return id.intValue();
		}
	}

	/**
	 * Returns the UUID associated to the given id. Note that groups names can be returned for group id's, as groups do not have associated UUIDs.
	 * 
	 * @param id the id.
	 * @return the UUID.
	 */
	private String getUUID(int id)
	{
		return idToUUID.get(new Integer(id));
	}

	/**
	 * Add the accessMask & identifier or replace them if the identifier already exists.
	 * 
	 * @return true if the identifier existed and was replaced; false if the info was just added.
	 */
	protected boolean setSecurityInfo(List<SecurityInfo> securityInfo, String element_uid, int accessMask, boolean formElement)
	{
		boolean replaced = false;
		Iterator<SecurityInfo> it = securityInfo.iterator();
		while (it.hasNext())
		{
			if (it.next().element_uid.equals(element_uid))
			{
				// the element is already there
				replaced = true;
				it.remove();
			}
		}

		// see if the accessMask is implicit; if it is, then no use in keeping track of it...
		boolean implicit;
		if (formElement)
		{
			implicit = (accessMask == IRepository.IMPLICIT_FORM_ACCESS);
		}
		else
		{
			implicit = (accessMask == IRepository.IMPLICIT_TABLE_ACCESS);
		}
		if (!implicit)
		{
			securityInfo.add(new SecurityInfo(element_uid, accessMask));
		}
		return replaced;
	}

	/**
	 * Writes the user and group information to the "security.sec" file in the resources project.<BR>
	 * This method should be used when the write mode is {@link #WRITE_MODE_MANUAL}. When the write mode is {@link #WRITE_MODE_AUTOMATIC}, it will get called
	 * automatically when security information is changed.
	 * 
	 * @param later if the file write operation should be performed later using a job.
	 * @throws RepositoryException if the security contents cannot be written because of some reason...
	 */
	private void writeUserAndGroupInfo(boolean later) throws RepositoryException
	{
		//write groups/users/usergroups
		IPath path = new Path(SECURITY_FILE_RELATIVE_TO_PROJECT);
		final IFile file = resourcesProject.getFile(path);

		try
		{
			final String out = serializeUserAndGroupInfo();
			if (out.length() == 0)
			{
				// no content to write
				if (later)
				{
					runLaterInUserJob(file.getProject(), new Runnable()
					{
						public void run()
						{
							if (file.exists())
							{
								writingResources = true;
								try
								{
									file.delete(true, null);
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
								finally
								{
									writingResources = false;
								}
							}
						}
					});
				}
				else if (file.exists())
				{
					writingResources = true;
					file.delete(true, null);
				}
			}
			else
			{
				if (later)
				{
					runLaterInUserJob(file.getProject(), new Runnable()
					{
						public void run()
						{
							writingResources = true;
							try
							{
								InputStream source = Utils.getUTF8EncodedStream(out);
								if (file.exists())
								{
									file.setContents(source, true, false, null);
								}
								else
								{
									ResourcesUtils.createFileAndParentContainers(file, source, true);
								}
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
							finally
							{
								writingResources = false;
							}
						}
					});
				}
				else
				{
					writingResources = true;
					InputStream source = Utils.getUTF8EncodedStream(out);
					if (file.exists())
					{
						file.setContents(source, true, false, null);
					}
					else
					{
						ResourcesUtils.createFileAndParentContainers(file, source, true);
					}
				}
			}

			fireUserGroupChanged();
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
		finally
		{
			writingResources = false;
		}
	}

	protected String serializeUserAndGroupInfo() throws JSONException
	{
		List<String> groupList = new ArrayList<String>();
		for (GroupSecurityInfo gi : groupInfos)
		{
			groupList.add(gi.getName());
		}
		return serializeUserAndGroupInfo(groupList, allDefinedUsers, userGroups);
	}

	public static final String serializeUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups) throws JSONException
	{
		if (groups.size() == 0 && users.size() == 0 && usersForGroups.size() == 0) return ""; //$NON-NLS-1$
		ServoyJSONObject obj = new ServoyJSONObject();

		ServoyJSONArray jsonGroups = new ServoyJSONArray();
		for (String groupName : groups)
		{
			jsonGroups.put(groupName);
		}
		obj.put(JSON_GROUPS, jsonGroups);

		ServoyJSONArray jsonUsers = new ServoyJSONArray();
		for (User user : users)
		{
			jsonUsers.put(user.toJSON());
		}
		obj.put(JSON_USERS, jsonUsers);

		ServoyJSONObject jsonUsersForGroups = new ServoyJSONObject();
		String[] ugs = usersForGroups.keySet().toArray(new String[usersForGroups.size()]);
		Arrays.sort(ugs, StringComparator.INSTANCE);
		for (String gname : ugs)
		{
			List<String> ulist = usersForGroups.get(gname);
			if (ulist != null && ulist.size() > 0)
			{
				jsonUsersForGroups.put(gname, new ServoyJSONArray(ulist));
			}
		}
		obj.put(JSON_USERGROUPS, jsonUsersForGroups);
		return obj.toString(true);
	}

	/**
	 * Writes the file "tablename.sec" into the folder in the resources project where column info is also kept. This file contains the security access
	 * information for that table.<BR>
	 * This method should be used when the write mode is {@link #WRITE_MODE_MANUAL}. When the write mode is {@link #WRITE_MODE_AUTOMATIC}, it will get called
	 * automatically when security information is changed.
	 * 
	 * @param later if the file write operation should be performed later using a job.
	 * @param t the table who's information is to be written.
	 * @throws RepositoryException if the info cannot be created because of some reason.
	 */
	protected void writeSecurityInfo(String serverName, String tableName, boolean later) throws RepositoryException
	{
		IPath path = new Path(DataModelManager.getRelativeServerPath(serverName) + IPath.SEPARATOR + getFileName(tableName));
		final IFile file = resourcesProject.getFile(path);
		try
		{
			final String out = serializeSecurityInfo(serverName, tableName);
			if (out.trim().length() == 0)
			{
				// no content to write
				if (later)
				{
					runLaterInUserJob(file.getProject(), new Runnable()
					{
						public void run()
						{
							if (file.exists())
							{
								writingResources = true;
								try
								{
									file.delete(true, null);
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
								finally
								{
									writingResources = false;
								}
							}
						}
					});
				}
				else if (file.exists())
				{
					writingResources = true;
					file.delete(true, null);
				}
			}
			else
			{
				if (later)
				{
					runLaterInUserJob(file.getProject(), new Runnable()
					{
						public void run()
						{
							writingResources = true;
							try
							{
								InputStream source = Utils.getUTF8EncodedStream(out);
								if (file.exists())
								{
									file.setContents(source, true, false, null);
								}
								else
								{
									ResourcesUtils.createFileAndParentContainers(file, source, true);
								}
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
							finally
							{
								writingResources = false;
							}
						}
					});
				}
				else
				{
					writingResources = true;
					InputStream source = Utils.getUTF8EncodedStream(out);
					if (file.exists())
					{
						file.setContents(source, true, false, null);
					}
					else
					{
						ResourcesUtils.createFileAndParentContainers(file, source, true);
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
		finally
		{
			writingResources = false;
		}
	}

	protected String serializeSecurityInfo(String serverName, String tableName) throws JSONException
	{
		if (serverName == null || tableName == null) return null;

		Map<String, List<SecurityInfo>> tableAccess = new HashMap<String, List<SecurityInfo>>();
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		while (it.hasNext())
		{
			GroupSecurityInfo groupSecurityInfo = it.next();
			List<SecurityInfo> infos = groupSecurityInfo.tableSecurity.get(Utils.getDotQualitfied(serverName, tableName));
			if (infos != null && infos.size() > 0)
			{
				List<SecurityInfo> info = new ArrayList<SecurityInfo>();
				info.add(new SecurityInfo(TABLE_PERMISSION_KEY, infos.get(0).access));
				tableAccess.put(groupSecurityInfo.getName(), info);
			}
		}
		return serializeSecurityPermissionInfo(tableAccess);
	}

	/**
	 * Writes the "formName.sec" file in the form's directory. This file contains the user/group security access rights for that form.<BR>
	 * This method should be used when the write mode is {@link #WRITE_MODE_MANUAL}. When the write mode is {@link #WRITE_MODE_AUTOMATIC}, it will get called
	 * automatically when security information is changed.
	 * 
	 * @param later if the file write operation should be performed later using a job.
	 * @param f the form who's security info is written.
	 * @throws RepositoryException if the file cannot be written because of some reason.
	 */
	private synchronized void writeSecurityInfo(Form f, boolean later) throws RepositoryException
	{
		if (!isOperational()) return;
		IPath path = new Path(SolutionSerializer.getRelativePath(f, false) + IPath.SEPARATOR + getFileName(f));
		final IFile file = ServoyModel.getWorkspace().getRoot().getFile(path);
		try
		{
			final String out = serializeSecurityInfo(f);
			if (out.trim().length() == 0)
			{
				// no content to write
				if (later)
				{
					runLaterInUserJob(file.getProject(), new Runnable()
					{
						public void run()
						{
							writingResources = true;
							try
							{
								file.delete(true, null);
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
							finally
							{
								writingResources = false;
							}
						}
					});
				}
				else if (file.exists())
				{
					writingResources = true;
					file.delete(true, null);
				}
			}
			else
			{
				if (later)
				{
					runLaterInUserJob(file.getProject(), new Runnable()
					{
						public void run()
						{
							writingResources = true;
							try
							{
								InputStream source = Utils.getUTF8EncodedStream(out);
								if (file.exists())
								{
									file.setContents(source, true, false, null);
								}
								else
								{
									ResourcesUtils.createFileAndParentContainers(file, source, true);
								}
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
							finally
							{
								writingResources = false;
							}
						}
					});
				}
				else
				{
					writingResources = true;
					InputStream source = Utils.getUTF8EncodedStream(out);
					if (file.exists())
					{
						file.setContents(source, true, false, null);
					}
					else
					{
						ResourcesUtils.createFileAndParentContainers(file, source, true);
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
		finally
		{
			writingResources = false;
		}
	}

	public static String serializeSecurityPermissionInfo(Map<String, List<SecurityInfo>> access) throws JSONException
	{
		ServoyJSONObject obj = new ServoyJSONObject();
		Iterator<String> it = access.keySet().iterator();
		while (it.hasNext())
		{
			String groupName = it.next();
			List<SecurityInfo> infos = access.get(groupName);
			if (infos != null && infos.size() > 0)
			{
				ServoyJSONObject jinfos = new ServoyJSONObject();
				Iterator<SecurityInfo> it2 = infos.iterator();
				while (it2.hasNext())
				{
					SecurityInfo securityInfo = it2.next();
					jinfos.put(securityInfo.element_uid, securityInfo.access);
				}
				obj.put(groupName, jinfos);
			}
		}
		return obj.toString(true);
	}

	protected String serializeSecurityInfo(Form f) throws JSONException
	{
		if (f == null) return null;

		Map<String, List<SecurityInfo>> formAccess = new HashMap<String, List<SecurityInfo>>();
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		while (it.hasNext())
		{
			GroupSecurityInfo groupSecurityInfo = it.next();
			List<SecurityInfo> infos = groupSecurityInfo.formSecurity.get(f.getUUID());
			if (infos != null && infos.size() > 0)
			{
				formAccess.put(groupSecurityInfo.getName(), infos);
			}
		}
		return serializeSecurityPermissionInfo(formAccess);
	}

	private void runLaterInUserJob(IResource resource, final Runnable runnable)
	{
		Job job = new Job("Correcting security info") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				runnable.run();
				return Status.OK_STATUS;
			}
		};

		job.setRule(resource);
		job.setUser(true);
		job.schedule();

	}

	/**
	 * Reads the group users and groups.<BR>
	 * Groups that are not in the list of groups but are supposed to contain users are created. Users that are supposed to belong to a group, but they are not
	 * in the user list will be deleted. If any of these two happens the security file will be written back with the corrections applied.
	 * 
	 * @throws RepositoryException if something wrong happens.
	 */
	private void readUserAndGroupInfo(String clientId) throws RepositoryException
	{
		//read groups/users/usergroups
		IPath path = new Path(SECURITY_FILE_RELATIVE_TO_PROJECT);
		IFile file = resourcesProject.getFile(path);
		lastReadResource = file;

		try
		{
			if (file.exists())
			{
				String fileContent = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8")); //$NON-NLS-1$
				deserializeUserAndGroupInfo(clientId, fileContent);
			}
		}
		catch (RepositoryException e)
		{
			throw e;
		}
		catch (JSONException e)
		{
			throw new SecurityReadException(SecurityReadException.JSON_DESERIALIZE_ERROR, (e.getMessage() == null) ? e.getClass().getName() : e.getMessage());
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	public static void deserializeUserAndGroupInfo(String jsonContent, List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
		throws JSONException
	{
		// read JSON contents into the objects above (that can be easily altered)
		ServoyJSONObject oldContent = new ServoyJSONObject(jsonContent, true);
		if (oldContent.length() == 0) return; // allow empty content

		JSONArray oldGroups = oldContent.getJSONArray(EclipseUserManager.JSON_GROUPS);
		JSONArray oldUsers = oldContent.getJSONArray(EclipseUserManager.JSON_USERS);
		JSONObject oldUsersForGroups = oldContent.getJSONObject(EclipseUserManager.JSON_USERGROUPS);

		for (int i = 0; i < oldGroups.length(); i++)
		{
			String groupName = oldGroups.getString(i);
			groups.add(groupName);
		}
		for (int i = 0; i < oldUsers.length(); i++)
		{
			User u = User.fromJSON(oldUsers.getJSONObject(i));
			users.add(u);
		}
		Iterator<String> keys /* group names */= oldUsersForGroups.keys();
		while (keys.hasNext())
		{
			String groupName = keys.next();
			JSONArray userListForGroup = oldUsersForGroups.getJSONArray(groupName);
			List<String> newUserListForGroup = new SortedList<String>(StringComparator.INSTANCE);
			for (int i = 0; i < userListForGroup.length(); i++)
			{
				String userUUID = userListForGroup.getString(i);
				newUserListForGroup.add(userUUID);
			}
			usersForGroups.put(groupName, newUserListForGroup);
		}
	}

	private void deserializeUserAndGroupInfo(String clientId, String info) throws JSONException, RepositoryException
	{
		List<String> groups = new SortedList<String>(StringComparator.INSTANCE);
		List<EclipseUserManager.User> users = new SortedList<User>();
		Map<String, List<String>> usersForGroups = new HashMap<String, List<String>>();

		// read the file into memory
		EclipseUserManager.deserializeUserAndGroupInfo(info, groups, users, usersForGroups);

		// some of the inconsistencies in the file will be automatically corrected
		boolean mustWriteBack = false;

		for (String groupName : groups)
		{
			if (!createGroupInternal(clientId, groupName))
			{
				if (userGroups.containsKey(groupName))
				{
					// duplicate
					mustWriteBack = true;
				}
				else
				{
					// invalid group name
					throw new SecurityReadException(SecurityReadException.INVALID_GROUP_NAME, "Invalid group name '" + groupName + "' in security file.", //$NON-NLS-1$ //$NON-NLS-2$
						groupName);
				}
			}
		}
		for (User u : users)
		{
			if (u.name != null && u.passwordHash != null && u.name.trim().length() > 0 && u.passwordHash.length() > 0)
			{
				if (u.userUid == null || u.userUid.length() == 0)
				{
					mustWriteBack = true;
					u.userUid = UUID.randomUUID().toString();
				}

				if (getUserName(clientId, u.userUid) != null)
				{
					// duplicate UUID for users
					throw new SecurityReadException(SecurityReadException.DUPLICATE_USER_UID, "Duplicate UUID for users security file: " + u.userUid, u.userUid); //$NON-NLS-1$
				}
				else if (getUserUID(clientId, u.name) != null)
				{
					// do not choose one or the other automatically - user should know about this
					throw new SecurityReadException(SecurityReadException.DUPLICATE_USER_NAME, "Duplicate user name in security file: " + u.name, u.name); //$NON-NLS-1$
				}
				else
				{
					allDefinedUsers.add(u);
				}
			}
			else
			{
				// invalid user
				throw new SecurityReadException(SecurityReadException.INVALID_USER_NAME_OR_PASSWORD, "Invalid user in security file: " + u, //$NON-NLS-1$
					u.toJSON().toString(true));
			}
		}
		Iterator<String> keys /* group names */= usersForGroups.keySet().iterator();
		while (keys.hasNext())
		{
			String groupName = keys.next();
			List<String> userListForGroup = usersForGroups.get(groupName);
			if (!userGroups.containsKey(groupName))
			{
				// some group name in the group-user table is not listed in the groups section; add it
				mustWriteBack = true;
				if (!createGroupInternal(clientId, groupName))
				{
					throw new SecurityReadException(SecurityReadException.INVALID_GROUP_NAME_IN_USER_LIST, "Invalid group name '" + groupName + //$NON-NLS-1$
						"' is listing users in security file.", groupName); //$NON-NLS-1$
				}
			}

			for (String userUID : userListForGroup)
			{
				if (getUserName(clientId, userUID) == null)
				{
					if (userUID != null && userUID.trim().length() != 0)
					{
						// some user mentioned here does not exist...
						throw new SecurityReadException(SecurityReadException.MISSING_USER_REFERENCED_IN_GROUP, "User with uid '" + userUID + //$NON-NLS-1$
							"' referenced in group '" + groupName + "' does not exist.", userUID); //$NON-NLS-1$ //$NON-NLS-2$
					}
					else
					{
						// simply remove this entry from the list...
						mustWriteBack = true;
					}
				}
				else
				{
					List<String> usersInGroup = userGroups.get(groupName);
					if (usersInGroup.contains(userUID))
					{
						// user is mentioned twice in this group
						mustWriteBack = true;
					}
					else
					{
						usersInGroup.add(userUID);
					}
				}
			}
		}

		if (mustWriteBack && writeMode == WRITE_MODE_AUTOMATIC)
		{
			try
			{
				writeUserAndGroupInfo(true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	/**
	 * Reads the security info for the given table from the resources project.<BR>
	 * Missing groups that are in the table security info file will be added to the group list. Groups with invalid names will be removed from this table's
	 * security info. If any of these two happens the security file will be written back with the corrections applied.
	 * 
	 * @param table the table.
	 * @throws RepositoryException if something wrong happens.
	 */
	private void readSecurityInfo(String serverName, String tableName) throws RepositoryException
	{
		IPath path = new Path(DataModelManager.getRelativeServerPath(serverName) + IPath.SEPARATOR + getFileName(tableName));
		IFile file = resourcesProject.getFile(path);
		lastReadResource = file;

		try
		{
			if (file.exists())
			{
				String fileContent = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8")); //$NON-NLS-1$
				deserializeSecurityInfo(serverName, tableName, fileContent);
			}
		}
		catch (RepositoryException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	private void deserializeSecurityInfo(String serverName, String tableName, String info) throws JSONException, RepositoryException
	{
		Map<String, List<SecurityInfo>> tableAccess = new HashMap<String, List<SecurityInfo>>();
		deserializeSecurityPermissionInfo(info, tableAccess);

		boolean mustWriteBackTableInfo = false;

		Iterator<String> keys = tableAccess.keySet().iterator();
		while (keys.hasNext())
		{
			String groupName = keys.next();
			List<SecurityInfo> groupPermissions = tableAccess.get(groupName);

			if (userGroups.containsKey(groupName))
			{
				GroupSecurityInfo gsi = null;
				for (GroupSecurityInfo gsiIt : groupInfos)
				{
					if (groupName.equals(gsiIt.groupName))
					{
						gsi = gsiIt;
						break;
					}
				}
				List<SecurityInfo> tableInfoForGroup = gsi.tableSecurity.get(Utils.getDotQualitfied(serverName, tableName));
				if (tableInfoForGroup == null)
				{
					tableInfoForGroup = new SortedList<SecurityInfo>();
					gsi.tableSecurity.put(Utils.getDotQualitfied(serverName, tableName), tableInfoForGroup);
				}

				SecurityInfo element = groupPermissions.get(0);
				if (element.access == IRepository.IMPLICIT_TABLE_ACCESS) mustWriteBackTableInfo = true;
				else
				{
					ServoyModelManager.getServoyModelManager().getServoyModel();
					IServer server = ServoyModel.getServerManager().getServer(serverName);
					ITable table = null;
					if (server != null)
					{
						try
						{
							table = server.getTable(tableName);
						}
						catch (RemoteException e)
						{
						}
					}
					if (table != null)
					{
						for (String columnName : table.getColumnNames())
						{
							setSecurityInfo(tableInfoForGroup, columnName, element.access, false);
						}
						if (table.getColumnNames().length == 0) setSecurityInfo(tableInfoForGroup, TABLE_PERMISSION_KEY, element.access, false); // if a new table is created and a sec file already exists for it, remember permission even if no columns are available for it yet...
					} // else it is ok not to load it in memory - because it will not be written...
					if (groupPermissions.size() > 1 || (!TABLE_PERMISSION_KEY.equals(element.element_uid)))
					{
						mustWriteBackTableInfo = true;
					}
				}
			}
			else
			{
				throw new SecurityReadException(SecurityReadException.GROUP_NOT_DECLARED, "Group '" + groupName + "' not defined, but referenced in table '" + //$NON-NLS-1$ //$NON-NLS-2$
					serverName + "->" + tableName + "' security file.", groupName); //$NON-NLS-1$//$NON-NLS-2$
			}
		}

		if (writeMode == WRITE_MODE_AUTOMATIC && mustWriteBackTableInfo)
		{
			try
			{
				writeSecurityInfo(serverName, tableName, true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void readSecurityInfo(Form form) throws RepositoryException
	{
		if (!isOperational()) return;
		IPath path = new Path(SolutionSerializer.getRelativePath(form, false) + IPath.SEPARATOR + getFileName(form));
		// the path is relative to the solution project; so get the solution's project
		IFile file = ServoyModel.getWorkspace().getRoot().getFile(path);
		lastReadResource = file;

		try
		{
			if (file.exists())
			{
				String fileContent = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8")); //$NON-NLS-1$
				deserializeSecurityInfo(form, fileContent);
			}
		}
		catch (RepositoryException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	public static void deserializeSecurityPermissionInfo(String jsonContent, Map<String, List<SecurityInfo>> access) throws JSONException
	{
		ServoyJSONObject obj = new ServoyJSONObject(jsonContent, true);

		Iterator<String> keys = obj.keys();
		while (keys.hasNext())
		{
			String groupName = keys.next();
			JSONObject groupPermissions = obj.getJSONObject(groupName);

			Iterator<String> elementUIDs = groupPermissions.keys();
			List<SecurityInfo> groupAccessList = null;
			if (elementUIDs.hasNext())
			{
				groupAccessList = new SortedList<SecurityInfo>();
				access.put(groupName, groupAccessList);
			}
			while (elementUIDs.hasNext())
			{
				String elementUUID = elementUIDs.next();
				groupAccessList.add(new SecurityInfo(elementUUID, groupPermissions.getInt(elementUUID)));
			}
		}
	}

	private void deserializeSecurityInfo(Form form, String info) throws RepositoryException, JSONException
	{
		Map<String, List<SecurityInfo>> formAccess = new HashMap<String, List<SecurityInfo>>();
		deserializeSecurityPermissionInfo(info, formAccess);

		boolean mustWriteBackFormInfo = false;

		Iterator<String> keys = formAccess.keySet().iterator();
		while (keys.hasNext())
		{
			String groupName = keys.next();
			List<SecurityInfo> groupPermissions = formAccess.get(groupName);

			if (userGroups.containsKey(groupName))
			{
				GroupSecurityInfo gsi = null;
				for (GroupSecurityInfo gsiIt : groupInfos)
				{
					if (groupName.equals(gsiIt.groupName))
					{
						gsi = gsiIt;
						break;
					}
				}
				List<SecurityInfo> formInfoForGroup = gsi.formSecurity.get(form.getUUID());
				if (formInfoForGroup == null)
				{
					formInfoForGroup = new SortedList<SecurityInfo>();
					gsi.formSecurity.put(form.getUUID(), formInfoForGroup);
				}
				for (SecurityInfo element : groupPermissions)
				{
					if (isElementChildOfForm(element.element_uid, form))
					{
						if (element.access == IRepository.IMPLICIT_FORM_ACCESS) mustWriteBackFormInfo = true;
						boolean replaced = setSecurityInfo(formInfoForGroup, element.element_uid, element.access, true);
						if (replaced)
						{
							// this cannot happen with current structure of JSON file and the in-mem structures that are read (having more entries with same key in a JSON obj)
							throw new SecurityReadException(SecurityReadException.DUPLICATE_ELEMENT_PERMISSION, "Element with UUID " + element.element_uid + //$NON-NLS-1$
								" on form " + form.getName() + " is mentioned multiple times within the same group " + groupName + //$NON-NLS-1$//$NON-NLS-2$
								"; it can only have 1 access mask specified...", new String[] { element.element_uid, groupName }); //$NON-NLS-1$
						}
					}
					else
					{
						throw new SecurityReadException(SecurityReadException.MISSING_ELEMENT, "Element with UUID " + element.element_uid + " on form " + //$NON-NLS-1$ //$NON-NLS-2$
							form.getName() + " does not exist, but has an access mask specified...", element.element_uid); //$NON-NLS-1$
					}
				}
			}
			else
			{
				throw new SecurityReadException(SecurityReadException.GROUP_NOT_DECLARED, "Group '" + groupName + "' not defined, but referenced in form '" + //$NON-NLS-1$ //$NON-NLS-2$
					form.getName() + "' security file.", groupName); //$NON-NLS-1$
			}
		}

		if (writeMode == WRITE_MODE_AUTOMATIC && mustWriteBackFormInfo)
		{
			try
			{
				writeSecurityInfo(form, true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	protected boolean isElementChildOfForm(final String elementUUID, Form form)
	{
		Object retVal = form.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (elementUUID.equals(o.getUUID().toString()))
				{
					return Boolean.TRUE;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		return retVal == Boolean.TRUE;
	}

	public List<SecurityInfo> getSecurityInfos(String group, Form f)
	{
		GroupSecurityInfo groupSecurityInfo = getGroupSecurityInfo(group);
		if (groupSecurityInfo != null)
		{
			return groupSecurityInfo.formSecurity.get(f.getUUID());
		}
		return null;
	}

	public List<SecurityInfo> getSecurityInfos(String group, Table t)
	{
		GroupSecurityInfo groupSecurityInfo = getGroupSecurityInfo(group);
		if (groupSecurityInfo != null)
		{
			return groupSecurityInfo.tableSecurity.get(Utils.getDotQualitfied(t.getServerName(), t.getName()));
		}
		return null;
	}

	protected GroupSecurityInfo getGroupSecurityInfo(String group)
	{
		GroupSecurityInfo groupSecurityInfo = null;
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		while (it.hasNext())
		{
			groupSecurityInfo = it.next();
			if (groupSecurityInfo.getName().equals(group)) break;
		}
		return groupSecurityInfo;
	}

	public String checkPasswordForUserName(String clientId, String userName, String password, boolean passwordAlreadyHashed)
	{
		if (userName == null || userName.length() == 0 || password == null || password.length() == 0) return null;
		String hashedPassword = password;
		if (!passwordAlreadyHashed) hashedPassword = Utils.calculateMD5HashBase64(password);
		String userUUID = null;

		for (User user : allDefinedUsers)
		{
			if (userName.equals(user.name) && (hashedPassword.equals(user.passwordHash)))
			{
				userUUID = user.userUid;
				break;
			}
		}
		return userUUID;
	}

	public boolean checkPasswordForUserUID(String clientId, String userUID, String password, boolean passwordAlreadyHashed)
	{
		if (userUID == null || userUID.length() == 0 || password == null || password.length() == 0) return false;
		String hashedPassword = password;
		if (!passwordAlreadyHashed) hashedPassword = Utils.calculateMD5HashBase64(password);

		for (User user : allDefinedUsers)
		{
			if (userUID.equals(user.userUid) && (hashedPassword.equals(user.passwordHash)))
			{
				return true;
			}
		}
		return false;
	}

	public Map<Object, Integer> getSecurityAccess(String clientId, int[] solution_ids, int[] releaseNumbers, String[] groups)
	{
		Map<Object, Integer> retval = new HashMap<Object, Integer>();
		Map<Object, Pair<Integer, Boolean>> groupsWithNonDefaultAccess = new HashMap<Object, Pair<Integer, Boolean>>(); // true stands for form elements, false for table columns
		for (int i = 0; i < solution_ids.length; i++)
		{
			int solution_id = solution_ids[i];
			int releaseNumber = releaseNumbers[i];

			IRootObject solution = null;
			if (solution_id >= 0)
			{
				try
				{
					ServoyModelManager.getServoyModelManager().getServoyModel();
					solution = ServoyModel.getDeveloperRepository().getRootObject(solution_id, releaseNumber);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cannot get security access for solution with id, release = " + solution_id + ", " + releaseNumber, e); //$NON-NLS-1$ //$NON-NLS-2$
					return retval;
				}
				if (solution == null)
				{
					ServoyLog.logError("Cannot get security access because of missing solution with id, release = " + solution_id + ", " + releaseNumber, null); //$NON-NLS-1$ //$NON-NLS-2$
					return retval;
				}
			}

			if (groups != null)
			{
				for (String group : groups)
				{
					GroupSecurityInfo gsi = getGroupSecurityInfo(group);
					if (solution != null)
					{
						Iterator<Entry<UUID, List<SecurityInfo>>> it = gsi.formSecurity.entrySet().iterator();
						while (it.hasNext())
						{
							Entry<UUID, List<SecurityInfo>> formSecurityEntry = it.next();
							UUID formUUID = formSecurityEntry.getKey();
							if (formIsChildOfPersist(solution, formUUID))
							{
								List<SecurityInfo> lsi = formSecurityEntry.getValue();
								Iterator<SecurityInfo> it2 = lsi.iterator();
								while (it2.hasNext())
								{
									SecurityInfo si = it2.next();
									UUID uuid = UUID.fromString(si.element_uid);
									Object value = retval.get(uuid);
									if (value instanceof Integer)
									{
										value = new Integer(((Integer)value).intValue() | si.access);
									}
									else
									{
										value = new Integer(si.access);
									}
									Pair<Integer, Boolean> old = groupsWithNonDefaultAccess.get(uuid);
									if (old == null)
									{
										groupsWithNonDefaultAccess.put(uuid, new Pair<Integer, Boolean>(Integer.valueOf(1), Boolean.TRUE));
									}
									else
									{
										old.setLeft(Integer.valueOf(old.getLeft().intValue() + 1));
									}
									retval.put(uuid, (Integer)value);
								}
							}
						}
					}
				}
			}
		}
		for (String group : groups)
		{
			GroupSecurityInfo gsi = getGroupSecurityInfo(group);
			Iterator<Entry<CharSequence, List<SecurityInfo>>> it3 = gsi.tableSecurity.entrySet().iterator();
			while (it3.hasNext())
			{
				Entry<CharSequence, List<SecurityInfo>> entry = it3.next();
				CharSequence s_t = entry.getKey();
				List<SecurityInfo> lsi = entry.getValue();
				Iterator<SecurityInfo> it2 = lsi.iterator();
				while (it2.hasNext())
				{
					SecurityInfo si = it2.next();
					CharSequence cid = Utils.getDotQualitfied(s_t, si.element_uid);
					Object value = retval.get(cid);
					if (value instanceof Integer)
					{
						value = new Integer(((Integer)value).intValue() | si.access);
					}
					else
					{
						value = new Integer(si.access);
					}
					Pair<Integer, Boolean> old = groupsWithNonDefaultAccess.get(cid);
					if (old == null)
					{
						groupsWithNonDefaultAccess.put(cid, new Pair<Integer, Boolean>(Integer.valueOf(1), Boolean.FALSE));
					}
					else
					{
						old.setLeft(Integer.valueOf(old.getLeft().intValue() + 1));
					}
					retval.put(cid, (Integer)value); //server.table.column -> int
				}
			}
		}
		Iterator<Entry<Object, Integer>> entries = retval.entrySet().iterator();
		while (entries.hasNext())
		{
			Entry<Object, Integer> entry = entries.next();
			Pair<Integer, Boolean> gwnda = groupsWithNonDefaultAccess.get(entry.getKey());
			if (gwnda.getLeft().intValue() < groups.length)
			{
				// this means the user is part of more groups, some of which have default access values
				// for this form element or table column; merge these defaults with other group access value
				if (gwnda.getRight().equals(Boolean.TRUE))
				{
					// form element
					entry.setValue(Integer.valueOf(entry.getValue().intValue() | (IRepository.VIEWABLE + IRepository.ACCESSIBLE)));
				}
				else
				{
					// table column
					entry.setValue(Integer.valueOf(entry.getValue().intValue() |
						(IRepository.READ + IRepository.INSERT + IRepository.UPDATE + IRepository.DELETE))); // default no TRACKING!
				}
			}
		}
		return retval;
	}

	private boolean formIsChildOfPersist(IPersist persist, final UUID formUUID)
	{
		Object result = null;
		result = persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (formUUID.equals(o.getUUID()))
				{
					return o.getTypeID() == IRepository.FORMS ? Boolean.TRUE : Boolean.FALSE;
				}
				return CONTINUE_TRAVERSAL;
			}
		});
		if (result instanceof Boolean)
		{
			return ((Boolean)result).booleanValue();
		}
		return false;
	}

	public String[] getUserGroups(String clientId, String userUID)
	{
		if (userUID == null || userUID.length() == 0) return null;
		List<String> groups = new ArrayList<String>();

		// find groups for this user
		for (Entry<String, List<String>> entry : userGroups.entrySet())
		{
			String groupName = entry.getKey();
			List<String> userUUIDsForGroup = entry.getValue();

			if (userUUIDsForGroup.contains(userUID))
			{
				groups.add(groupName);
			}
		}
		String[] result = groups.toArray(new String[groups.size()]);
		Arrays.sort(result, NameComparator.INSTANCE);

		return result;
	}

	public boolean checkIfAdministratorsAreAvailable(String clientId)
	{
		List<String> adminGroup = userGroups.get(IRepository.ADMIN_GROUP);
		if (adminGroup != null)
		{
			return adminGroup.size() > 0;
		}
		return false;
	}

	public boolean checkIfUserIsAdministrator(String clientId, String userUid)
	{
		List<String> adminGroup = userGroups.get(IRepository.ADMIN_GROUP);
		if (adminGroup != null)
		{
			return adminGroup.contains(userUid);
		}
		return false;
	}

	public int getGroupId(String clientId, String groupName)
	{
		// as groups do not have UUID, ids will be kept per group name
		return userGroups.containsKey(groupName) ? getId(groupName) : -1;
	}

	public IDataSet getGroups(String clientId)
	{
		List<Object[]> groups = new ArrayList<Object[]>();
		for (GroupSecurityInfo gi : groupInfos)
		{
			groups.add(new Object[] { new Integer(getId(gi.groupName)), gi.groupName });
		}

		BufferedDataSet dataSet = new BufferedDataSet(new String[] { "group_id", "group_name" }, groups); //$NON-NLS-1$ //$NON-NLS-2$
		return dataSet;
	}

	public IDataSet getUserGroups(String clientId, int userId)
	{
		String userUUID = getUUID(userId);
		if (userUUID == null) return null;
		List<Object[]> groups = new SortedList<Object[]>(new Comparator<Object[]>()
		{
			public int compare(Object[] o1, Object[] o2)
			{
				return ((String)o1[1]).compareToIgnoreCase(((String)o2[1]));
			}
		});
		for (Entry<String, List<String>> entry : userGroups.entrySet())
		{
			String groupName = entry.getKey();
			List<String> userUUIDsForGroup = entry.getValue();

			if (userUUIDsForGroup.contains(userUUID))
			{
				groups.add(new Object[] { new Integer(getId(groupName)), groupName });
			}
		}
		BufferedDataSet dataSet = new BufferedDataSet(new String[] { "group_id", "group_name" }, groups); //$NON-NLS-1$ //$NON-NLS-2$
		return dataSet;
	}

	public String getUserName(String clientId, String userUID)
	{
		if (userUID == null || (!isOperational())) return null;

		for (User u : allDefinedUsers)
		{
			if (userUID.equals(u.userUid))
			{
				return u.name;
			}
		}

		return null;
	}

	public String getUserName(String clientId, int userId) throws ServoyException
	{
		return getUserName(clientId, getUUID(userId));
	}

	public String getUserPasswordHash(String clientId, String userUID) throws ServoyException
	{
		if (userUID == null || (!isOperational())) return null;

		for (User u : allDefinedUsers)
		{
			if (userUID.equals(u.userUid))
			{
				return u.passwordHash;
			}
		}

		return null;

	}

	public String getUserUID(String clientId, String userName)
	{
		if (userName == null) return null;
		for (User u : allDefinedUsers)
		{
			if (userName.equals(u.name))
			{
				return u.userUid;
			}
		}

		return null;
	}

	public String getUserUID(String clientId, int userId)
	{
		return getUUID(userId);
	}

	public int getUserIdByUID(String clientId, String userUID)
	{
		if (userUID == null || userUID.length() == 0) return -1;
		// see that the user exists
		boolean exists = false;
		for (User u : allDefinedUsers)
		{
			if (userUID.equals(u.userUid))
			{
				exists = true;
				break;
			}
		}
		return exists ? getId(userUID) : -1;
	}

	public int getUserIdByUserName(String clientId, String userName)
	{
		return getId(getUserUID(clientId, userName));
	}

	public IDataSet getUsers(String clientId)
	{
		List<Object[]> users = new ArrayList<Object[]>();
		for (User u : allDefinedUsers)
		{
			users.add(new Object[] { u.userUid, u.name, new Integer(getId(u.userUid)) });
		}
		BufferedDataSet dataSet = new BufferedDataSet(new String[] { "user_uid", "user_name", "user_id" }, users); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return dataSet;
	}

	public int createGroup(String clientId, String groupName) throws ServoyException
	{
		if (!createGroupInternal(clientId, groupName)) return -1;

		if (writeMode == WRITE_MODE_AUTOMATIC)
		{
			writeUserAndGroupInfo(false);
		}

		return getId(groupName);
	}

	private boolean createGroupInternal(String clientId, String groupName)
	{
		if (groupName == null || groupName.trim().length() == 0 || (!isOperational())) return false;
		// Check to see if the group already exists.
		int id = getGroupId(clientId, groupName);
		if (id != -1) return false;

		// create the group
		userGroups.put(groupName, new SortedList<String>(StringComparator.INSTANCE));
		groupInfos.add(new GroupSecurityInfo(groupName));

		return true;
	}


	public int createUser(String clientId, String userName, String password, String userUID, boolean alreadyHashed) throws ServoyException
	{
		if (!isOperational())
		{
			return ERR_RESOURCE_PROJECT_MISSING;
		}
		if (userName == null || userName.trim().length() == 0 || password == null || password.length() == 0) return ERR_EMPTY_USERNAME_OR_PASSWORD;
		String hashedPassword = password;
		if (!alreadyHashed)
		{
			hashedPassword = Utils.calculateMD5HashBase64(password);
		}

		// Check to see if the user already exists.
		int id = getUserIdByUserName(clientId, userName);
		if (id != -1) return ERR_USERNAME_EXISTS;

		String uuid = userUID;
		if (uuid == null || uuid.length() == 0)
		{
			uuid = UUID.randomUUID().toString();
		}
		// create the user
		allDefinedUsers.add(new User(userName, hashedPassword, uuid));

		if (writeMode == WRITE_MODE_AUTOMATIC)
		{
			writeUserAndGroupInfo(false);
		}

		return getId(uuid);
	}

	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID elementUUID, String solutionName) throws ServoyException
	{
		if (groupName == null || groupName.length() == 0 || accessMask == null || elementUUID == null || (!isOperational()))
		{
			ServoyLog.logError("Invalid parameters received, or manager is not operational - setFormSecurityAccess(...)", null); //$NON-NLS-1$
			return;
		}
		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);

		if (gsi != null)
		{
			// now we must find the Form's UUID from the elementUUID
			Form form = null;
			Solution solution = null;
			ServoyProject solutionProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (solutionProject != null)
			{
				solution = solutionProject.getSolution();
			}
			if (solution != null)
			{
				Iterator<Form> it = solution.getForms(null, false);
				while (it.hasNext())
				{
					Form f = it.next();
					if (isElementChildOfForm(elementUUID.toString(), f))
					{
						form = f;
					}
				}
			}

			if (form != null)
			{
				addFormSecurityAccess(groupName, accessMask, elementUUID, form.getUUID());
				if (writeMode == WRITE_MODE_AUTOMATIC)
				{
					writeSecurityInfo(form, false);
				}
			}
			else
			{
				ServoyLog.logWarning("setFormSecurityAccess(...) cannot find element with given UUID or not form element!", null); //$NON-NLS-1$
			}
		}
		else
		{
			ServoyLog.logWarning("setFormSecurityAccess(...) cannot find the group with the given name!", null); //$NON-NLS-1$
		}
	}

	public void addFormSecurityAccess(String groupName, Integer accessMask, UUID elementUUID, UUID formUuid)
	{
		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);

		if (gsi != null)
		{
			List<SecurityInfo> securityInfo = gsi.formSecurity.get(formUuid);
			if (securityInfo == null)
			{
				securityInfo = new SortedList<SecurityInfo>();
				gsi.formSecurity.put(formUuid, securityInfo);
			}
			setSecurityInfo(securityInfo, elementUUID.toString(), accessMask.intValue(), true);
		}
		else
		{
			ServoyLog.logWarning("setFormSecurityAccess(...) cannot find the group with the given name!", null); //$NON-NLS-1$
		}
	}

	public void setTableSecurityAccess(String clientId, String groupName, Integer accessMask, String connectionName, String tableName, String columnName)
		throws ServoyException
	{
		if (groupName == null || groupName.length() == 0 || accessMask == null || (!isOperational()))
		{
			ServoyLog.logError("Invalid parameters received, or manager is not operational - setTableSecurityAccess(...)", null); //$NON-NLS-1$
			return;
		}

		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);
		if (gsi != null)
		{
			List<SecurityInfo> securityInfo = gsi.tableSecurity.get(Utils.getDotQualitfied(connectionName, tableName));
			if (securityInfo == null)
			{
				securityInfo = new SortedList<SecurityInfo>();
				gsi.tableSecurity.put(Utils.getDotQualitfied(connectionName, tableName), securityInfo);
			}
			setSecurityInfo(securityInfo, columnName, accessMask.intValue(), false);
			if (writeMode == WRITE_MODE_AUTOMATIC)
			{
				writeSecurityInfo(connectionName, tableName, false);
			}
		}
		else
		{
			ServoyLog.logWarning("setTableSecurityAccess(...) cannot find the group with the given name!", null); //$NON-NLS-1$
		}
	}

	public boolean addUserToGroup(String clientId, int userId, int groupId) throws ServoyException
	{
		if (userId < 0 || groupId < 0 || (!isOperational())) return false;
		String groupName = getUUID(groupId);
		String userUUID = getUUID(userId);
		if (groupName == null || userUUID == null) return false;

		// now we know the user and group are there - so now we have to make a couple out of them :)
		List<String> usersInGroup = userGroups.get(groupName);
		if (!usersInGroup.contains(userUUID))
		{
			usersInGroup.add(userUUID);
			if (writeMode == WRITE_MODE_AUTOMATIC)
			{
				writeUserAndGroupInfo(false);
			}
		}

		return true;
	}

	public boolean setPassword(String clientId, String userUID, String password) throws ServoyException
	{
		return setPassword(clientId, userUID, password, true);
	}

	public boolean setPassword(String clientId, String userUID, String password, boolean hashPassword) throws ServoyException
	{
		if (userUID == null || userUID.length() == 0 || password == null || password.length() == 0 || (!isOperational())) return false;
		String passwordHash = hashPassword ? Utils.calculateMD5HashBase64(password) : password;

		for (User u : allDefinedUsers)
		{
			if (userUID.equals(u.userUid))
			{
				u.passwordHash = passwordHash;
				if (writeMode == WRITE_MODE_AUTOMATIC)
				{
					writeUserAndGroupInfo(false);
				}
				return true;
			}
		}

		return false;
	}

	public boolean setUserUID(String clientId, String oldUserUID, String newUserUID) throws ServoyException
	{
		if (oldUserUID == null || oldUserUID.length() == 0 || newUserUID == null || newUserUID.length() == 0 || (!isOperational())) return false;

		for (User u : allDefinedUsers)
		{
			if (oldUserUID.equals(u.userUid))
			{
				// update the id <-> UUID mappings
				if (UUIDToId.containsKey(oldUserUID))
				{
					// keep the id unaltered - modify only the UUID
					Integer id = new Integer(getId(oldUserUID));
					idToUUID.put(id, newUserUID);
					UUIDToId.remove(oldUserUID);
					UUIDToId.put(newUserUID, id);
				} // else - no ID was used for this user before - so nothing in the mappings to change

				//change the UUID
				u.userUid = newUserUID;
				if (writeMode == WRITE_MODE_AUTOMATIC)
				{
					writeUserAndGroupInfo(false);
				}
				return true;
			}
		}

		return false;
	}

	public boolean changeUserName(String clientId, String userUID, String newUserName) throws ServoyException
	{
		if (userUID == null || newUserName == null || newUserName.length() == 0 || (!isOperational())) return false;

		for (User u : allDefinedUsers)
		{
			if (userUID.equals(u.userUid))
			{
				if (getUserIdByUserName(clientId, newUserName) != -1) return false;

				u.name = newUserName;
				if (writeMode == WRITE_MODE_AUTOMATIC)
				{
					writeUserAndGroupInfo(false);
				}
				return true;
			}
		}
		return false;
	}

	public boolean removeUserFromGroup(String clientId, int userId, int groupId) throws ServoyException
	{
		if (userId < 0 || groupId < 0 || (!isOperational())) return false;
		String groupName = getUUID(groupId);
		String userUUID = getUUID(userId);
		if (groupName == null || userUUID == null) return false;

		// now we know the user and group are there
		List<String> usersInGroup = userGroups.get(groupName);
		if (usersInGroup.contains(userUUID))
		{
			usersInGroup.remove(userUUID);
			if (writeMode == WRITE_MODE_AUTOMATIC)
			{
				writeUserAndGroupInfo(false);
			}
		}

		return true;
	}

	public boolean deleteUser(String clientId, String userUUID) throws ServoyException
	{
		if (userUUID == null || (!isOperational())) return false;
		User user = null;
		for (User u : allDefinedUsers)
		{
			if (userUUID.equals(u.userUid))
			{
				user = u;
			}
		}

		if (user != null)
		{
			if (UUIDToId.containsKey(userUUID))
			{
				Integer id = new Integer(getId(userUUID));
				UUIDToId.remove(userUUID);
				idToUUID.remove(id);
			}
			allDefinedUsers.remove(user);

			// also remove the user from the groups he used to belong to
			for (List<String> userListInGroup : userGroups.values())
			{
				userListInGroup.remove(userUUID);
			}

			if (writeMode == WRITE_MODE_AUTOMATIC)
			{
				writeUserAndGroupInfo(false);
			}
			return true;
		}

		return false;
	}

	public boolean deleteUser(String clientId, int userId) throws ServoyException
	{
		return deleteUser(clientId, getUUID(userId));
	}

	public boolean changeGroupName(String clientId, int groupId, String newName) throws ServoyException
	{
		if (groupId < 0 || newName == null || newName.length() == 0 || (!isOperational()) || userGroups.containsKey(newName)) return false;

		String oldName = getUUID(groupId);
		if (oldName != null)
		{
			userGroups.put(newName, userGroups.get(oldName));
			for (GroupSecurityInfo gsi : groupInfos)
			{
				if (oldName.equals(gsi.groupName))
				{
					gsi.groupName = newName;
				}
			}
			userGroups.remove(oldName);
			if (writeMode == WRITE_MODE_AUTOMATIC)
			{
				writeUserAndGroupInfo(false);
			}
			return true;
		}

		return false;
	}

	public boolean deleteGroup(String clientId, int groupId) throws ServoyException
	{
		if (groupId < 0 || (!isOperational())) return false;
		String groupName = getUUID(groupId);
		if (groupName != null)
		{
			userGroups.remove(groupName);
			refreshGroupSecurity(groupName, true);

			// do not take into account writeMode
			deleteGroupReferences(groupName);
			return true;
		}
		return false;
	}

	private void refreshGroupSecurity(String groupName, boolean deleteGroup)
	{
		List<Form> affectedForms = new ArrayList<Form>();
		List<TableWrapper> affectedTables = new ArrayList<TableWrapper>();
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		GroupSecurityInfo gsi = null;
		while (it.hasNext())
		{
			gsi = it.next();
			if (groupName.equals(gsi.groupName))
			{
				// if tables/forms used this group, we must write those files as well (to remove the group)
				Set<UUID> formsUsingThisGroup = gsi.formSecurity.keySet();
				for (UUID formUUID : formsUsingThisGroup)
				{
					Form f = getForm(formUUID);
					if (f != null)
					{
						affectedForms.add(f);
					}
					else
					{
						ServoyLog.logError("Cannot find form with UUID " + formUUID + " for delete group", null); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				Set<CharSequence> tablesUsingThisGroup = gsi.tableSecurity.keySet();
				for (CharSequence tableString : tablesUsingThisGroup)
				{
					TableWrapper t = getTable(tableString.toString());
					if (t != null)
					{
						affectedTables.add(t);
					}
					else
					{
						ServoyLog.logError("Cannot find table " + tableString + " for delete group", null); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				if (deleteGroup) it.remove();
			}
		}
		if (writeMode == WRITE_MODE_AUTOMATIC)
		{
			try
			{
				writeUserAndGroupInfo(false);
				for (Form f : affectedForms)
				{
					writeSecurityInfo(f, false);
				}
				for (TableWrapper tw : affectedTables)
				{
					writeSecurityInfo(tw.getServerName(), tw.getTableName(), false);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void deleteGroupReferences(String groupName)
	{
		try
		{
			IProject[] projects = resourcesProject.getReferencingProjects();
			if (projects != null && projects.length > 0)
			{
				for (IProject project : projects)
				{
					ServoyModelManager.getServoyModelManager().getServoyModel();
					Solution solution = (Solution)ServoyModel.getDeveloperRepository().getActiveRootObject(project.getName(), IRepository.SOLUTIONS);
					if (solution != null)
					{
						Iterator<Form> iterator = solution.getForms(null, false);
						while (iterator.hasNext())
						{
							Form form = iterator.next();
							IPath path = new Path(SolutionSerializer.getRelativePath(form, false) + IPath.SEPARATOR + getFileName(form));
							// the path is relative to the solution project; so get the solution's project
							IFile file = ServoyModel.getWorkspace().getRoot().getFile(path);
							if (file.exists())
							{
								String fileContent = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8")); //$NON-NLS-1$
								ServoyJSONObject obj = new ServoyJSONObject(fileContent, true);
								if (obj.has(groupName))
								{
									obj.remove(groupName);
									String out = obj.toString(true);
									if (obj.length() > 0)
									{
										InputStream source = Utils.getUTF8EncodedStream(out);
										file.setContents(source, true, false, null);
									}
									else
									{
										file.delete(true, null);
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

	}

//	public List<String> getBrokenReferences(String name)
//	{
//		return brokenReferenceGroups.get(name);
//	}
//
//	public void deleteBrokenReference(String group)
//	{
//		try
//		{
//			Iterator<List<String>> it = brokenReferenceGroups.values().iterator();
//			while (it.hasNext())
//			{
//				List<String> list = it.next();
//				list.remove(group);
//			}
//			deleteGroup(getGroupId(group));
//		}
//		catch (Exception ex)
//		{
//			ServoyLog.logError(ex);
//		}
//	}
//
//	public void revertBrokenReference(String group)
//	{
//		Iterator<List<String>> it = brokenReferenceGroups.values().iterator();
//		while (it.hasNext())
//		{
//			List<String> list = it.next();
//			list.remove(group);
//		}
//		refreshGroupSecurity(group, false);
//	}
//
//	private boolean containsBrokenReference(String group)
//	{
//		Iterator<List<String>> it = brokenReferenceGroups.values().iterator();
//		while (it.hasNext())
//		{
//			List<String> list = it.next();
//			if (list.contains(group)) return true;
//		}
//		return false;
//	}

	private TableWrapper getTable(String tableString)
	{
		StringTokenizer st = new StringTokenizer(tableString, "."); //$NON-NLS-1$
		try
		{
			String serverName = st.nextToken();
			String tableName = st.nextToken();
			return new TableWrapper(serverName, tableName);
		}
		catch (NoSuchElementException e)
		{
			ServoyLog.logError("Table identifier string is bad: " + tableString, e); //$NON-NLS-1$
		}
		return null;
	}

	private Form getForm(UUID formUUID)
	{
		FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		Iterator<Form> allFormsInActiveModules = flatSolution.getForms(false);
		while (allFormsInActiveModules.hasNext())
		{
			Form f = allFormsInActiveModules.next();
			if (formUUID.equals(f.getUUID()))
			{
				return f;
			}
		}
		return null;
	}

	/**
	 * Reloads the security information related to table t.
	 * 
	 * @param t the table.
	 */
	public void reloadSecurityInfo(String serverName, String tableName)
	{
		if (serverName == null || tableName == null) return;
		if (readError != null && resourcesProject != null)
		{
			// this means the user manager is in a read error state; maybe the change that triggered this reload
			// fixed the problem - so try to load all security info again...
			reloadAllSecurityInformation();
		}
		else if (isOperational())
		{
			try
			{
				removeSecurityInfoFromMemory(serverName, tableName);
				readSecurityInfo(serverName, tableName);
			}
			catch (RepositoryException e)
			{
				setReadError(e);
			}
		}
	}

	private void setReadError(RepositoryException e)
	{
//		ServoyLog.logError(e);
		clearAllSecurityInfo();
		// mark as not operational
		if (e instanceof SecurityReadException)
		{
			readError = (SecurityReadException)e;
		}
		else
		{
			readError = new SecurityReadException(SecurityReadException.UNKNOWN, (e.getMessage() == null) ? e.getClass().getName() : e.getMessage());
		}
		setSecurityProblemMarker();
	}

	/**
	 * Reloads (removes & re-reads) all form security info.
	 */
	public void reloadAllFormInfo()
	{
		if (readError != null && resourcesProject != null)
		{
			// this means the user manager is in a read error state; maybe the change that triggered this reload
			// fixed the problem - so try to load all security info again...
			reloadAllSecurityInformation();
		}
		else if (isOperational())
		{
			try
			{
				removeAllFormSecurityInfoFromMemory();
				readAllFormInfo();
			}
			catch (RepositoryException e)
			{
				setReadError(e);
			}
		}
	}

	/**
	 * Reloads the security information related to form f.
	 * 
	 * @param f the form.
	 */
	public synchronized void reloadSecurityInfo(Form f)
	{
		if (f == null) return;
		if (readError != null && resourcesProject != null)
		{
			// this means the user manager is in a read error state; maybe the change that triggered this reload
			// fixed the problem - so try to load all security info again...
			reloadAllSecurityInformation();
		}
		else if (isOperational())
		{
			try
			{
				removeSecurityInfoFromMemory(f);
				readSecurityInfo(f);
			}
			catch (RepositoryException e)
			{
				setReadError(e);
			}
		}
	}

	private void removeAllFormSecurityInfoFromMemory()
	{
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		while (it.hasNext())
		{
			GroupSecurityInfo gsi = it.next();
			gsi.formSecurity.clear();
		}
	}

	/**
	 * Deletes all security info for the given table, but does not write the changes to disk.
	 * 
	 * @param t the table.
	 */
	private void removeSecurityInfoFromMemory(String serverName, String tableName)
	{
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		while (it.hasNext())
		{
			GroupSecurityInfo gsi = it.next();
			gsi.tableSecurity.remove(Utils.getDotQualitfied(serverName, tableName));
		}
	}

	/**
	 * Deletes all security info for the given form, but does not write the changes to disk.
	 * 
	 * @param f the form.
	 */
	private void removeSecurityInfoFromMemory(Form f)
	{
		Iterator<GroupSecurityInfo> it = groupInfos.iterator();
		while (it.hasNext())
		{
			GroupSecurityInfo gsi = it.next();
			gsi.formSecurity.remove(f.getUUID());
		}
	}

	/**
	 * Specifies the resources project to be used by this security manager. Also reloads all sec. info.
	 * 
	 * @param project the resources project.
	 */
	public void setResourcesProject(IProject project)
	{
		if (project != resourcesProject)
		{
			resourcesProject = project;
			reloadAllSecurityInformation();
		}
	}

	/**
	 * Writes to disk all the security information from memory.
	 * 
	 * @param discardInvalidOldInfo if this is true, previous read errors are ignored, and (probably empty) memory content is written to disk. If this is false,
	 *            the method will do nothing if the security manager detected that the security content on disk is invalid.
	 * @throws RepositoryException if the security information cannot be written to disk.
	 */
	public void writeAllSecurityInformation(boolean discardInvalidOldInfo) throws RepositoryException
	{
		if (discardInvalidOldInfo)
		{
			removeSecurityProblemMarker();
		}
		if (isOperational())
		{
			writeUserAndGroupInfo(false);
			writeAllTableInfo();
			writeAllFormInfo();
		}
	}

	protected void writeAllTableInfo() throws RepositoryException
	{
		boolean invalidSecurityContent = false;
		IServerManagerInternal sm = ServoyModel.getServerManager();
		String[] servers = sm.getServerNames(true, true, false, false);
		for (int i = servers.length - 1; i >= 0; i--)
		{
			IServerInternal s = (IServerInternal)sm.getServer(servers[i]);
			if (s != null)
			{
				try
				{
					Iterator<String> tables = s.getTableAndViewNames().iterator();
					while (tables.hasNext())
					{
						String tableName = tables.next();
						try
						{
							writeSecurityInfo(s.getName(), tableName, false);
						}
						catch (RepositoryException e)
						{
							invalidSecurityContent = true;
							throw e;
						}
					}
				}
				catch (DbcpException ex)
				{
					// the initialize of servers might not be completed at this point; so we may have an invalid server
					// don't do anything, this error should not exist 
				}
				catch (RepositoryException e)
				{
					if (invalidSecurityContent) throw e;
					ServoyLog.logError(e);
				}
			}
		}
	}

	private void writeAllFormInfo() throws RepositoryException
	{
		FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		Iterator<Form> allFormsInActiveModules = flatSolution.getForms(false);
		while (allFormsInActiveModules.hasNext())
		{
			writeSecurityInfo(allFormsInActiveModules.next(), false);
		}
	}

	/**
	 * Reads (loads) all security information (users&groups + security access rights for forms/tables).
	 */
	public void reloadAllSecurityInformation()
	{
		clearAllSecurityInfo();
		if (isOperational())
		{
			try
			{
				readUserAndGroupInfo(ApplicationServerSingleton.get().getClientId());
				readAllTableInfo();
				readAllFormInfo();
			}
			catch (RepositoryException e)
			{
				setReadError(e);
			}
		}
	}

	private void readAllTableInfo() throws RepositoryException
	{
		readingAllTableInfo = true;
		try
		{
			boolean invalidSecurityContent = false;
			IServerManagerInternal sm = ServoyModel.getServerManager();
			String[] servers = sm.getServerNames(true, true, false, false);
			for (int i = servers.length - 1; i >= 0; i--)
			{
				IServerInternal s = (IServerInternal)sm.getServer(servers[i]);
				if (s != null)
				{
					try
					{
						Iterator<String> tables = s.getTableAndViewNames().iterator();
						while (tables.hasNext())
						{
							String tableName = tables.next();
							try
							{
								readSecurityInfo(s.getName(), tableName);
							}
							catch (RepositoryException e)
							{
								invalidSecurityContent = true;
								throw e;
							}
						}
					}
					catch (DbcpException ex)
					{
						// the initialize of servers might not be completed at this point; so we may have an invalid server
						// don't do anything, this error should not exist 
					}
					catch (RepositoryException e)
					{
						if (invalidSecurityContent) throw e;
						ServoyLog.logError(e);
					}
				}
			}
		}
		finally
		{
			readingAllTableInfo = false;
		}
	}

	private void readAllFormInfo() throws RepositoryException
	{
		FlattenedSolution flatSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		Iterator<Form> allFormsInActiveModules = flatSolution.getForms(false);
		while (allFormsInActiveModules.hasNext())
		{
			readSecurityInfo(allFormsInActiveModules.next());
		}
	}

	private void removeSecurityProblemMarker()
	{
		if (readError != null)
		{
			readError = null;
			final IResource er = lastReadResource;
			Job job = new Job("Remove old security problem marker - active project changed") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					if (er.exists())
					{
						// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
						// the project might have disappeared before this job was started... (delete)
						ServoyBuilder.deleteMarkers(er, ServoyBuilder.USER_SECURITY_MARKER_TYPE);
					}
					return Status.OK_STATUS;
				}
			};
			job.setRule(er);
			job.setSystem(true);
			job.schedule();

			lastReadResource = null;
		}
	}

	private void setSecurityProblemMarker()
	{
		if (readError != null)
		{
			final IResource er = lastReadResource;
			final SecurityReadException error = readError;
			Job job = new Job("Update security problem marker") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
					// the project might have disappeared before this job was started... (delete)
					if (er.exists())
					{
						int charNo = -1;
						if (error.getType() == SecurityReadException.JSON_DESERIALIZE_ERROR)
						{
							// find out where the error occurred if possible...
							int idx = error.getMessage().indexOf("character"); //$NON-NLS-1$
							if (idx >= 0)
							{
								StringTokenizer st = new StringTokenizer(error.getMessage().substring(idx + 9), " "); //$NON-NLS-1$
								if (st.hasMoreTokens())
								{
									String charNoString = st.nextToken();
									try
									{
										charNo = Integer.parseInt(charNoString);
									}
									catch (NumberFormatException e)
									{
										// cannot fine character number... this is not a tragedy
									}
								}
							}
						}

						// we have an active solution with a resources project but with invalid security info; add problem marker
						IMarker marker = ServoyBuilder.addMarker(er, ServoyBuilder.USER_SECURITY_MARKER_TYPE, "Bad User/Security information: " + //$NON-NLS-1$
							error.getMessage(), charNo, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, "JSON file"); //$NON-NLS-1$
						try
						{
							marker.setAttribute(MARKER_ATTRIBUTE_TYPE, error.getType());
							if (error.getWrongValue() instanceof String[])
							{
								marker.setAttribute(MARKER_ATTRIBUTE_WRONG_VALUE_ARRAY_LENGTH, error.getWrongValue());
								String[] wv = (String[])error.getWrongValue();
								for (int i = 0; i < wv.length; i++)
								{
									marker.setAttribute(MARKER_ATTRIBUTE_WRONG_VALUE_ARRAY + i, wv[i]);
								}
							}
							else
							{
								marker.setAttribute(MARKER_ATTRIBUTE_WRONG_VALUE, error.getWrongValue());
							}
						}
						catch (CoreException e)
						{
							ServoyLog.logError("Cannot set security problem marker attributes.", e); //$NON-NLS-1$
						}
					}
					return Status.OK_STATUS;
				}
			};

			job.setRule(er);
			job.setSystem(true);
			job.schedule();
		}
	}

	private void clearAllSecurityInfo()
	{
		removeSecurityProblemMarker();
		userGroups.clear();
		allDefinedUsers.clear();
		groupInfos.clear();
		idCounter = 1;
		idToUUID.clear();
		UUIDToId.clear();
	}

	/**
	 * Tells whether or not this security manager is able to manipulate user/group/security information.
	 * 
	 * @return true if security info is available, false otherwise.
	 */
	public boolean isOperational()
	{
		// this security manager should not be able to store user/group data until a resourcesProject
		// (and therefore a solution project) is set
		return (isOperational || (resourcesProject != null && readError == null));
	}

	public void setOperational(boolean value)
	{
		isOperational = value;
	}

	/**
	 * Sets the write mode for this security manager.<BR>
	 * The write mode determines how the security information it written to a persistent state.
	 * 
	 * @param mode {@link #WRITE_MODE_AUTOMATIC} or {@link #WRITE_MODE_MANUAL}
	 */
	public void setWriteMode(int mode)
	{
		writeMode = mode;
	}

	public int getWriteMode()
	{
		return writeMode;
	}

	/**
	 * Returns true if security files are in the process of being written/deleted; false otherwise.
	 * 
	 * @return true if security files are in the process of being written/deleted; false otherwise.
	 */
	public boolean isWritingResources()
	{
		return writingResources;
	}

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
						for (String tableName : server.getTableNames())
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