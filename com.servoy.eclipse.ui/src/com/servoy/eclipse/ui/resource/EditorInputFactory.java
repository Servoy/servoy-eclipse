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
package com.servoy.eclipse.ui.resource;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.core.resource.TableEditorInput;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.util.UUID;

/**
 * Factory for re-creating editor input from a previously saved memento.
 * 
 * @author rgansevles
 */

public class EditorInputFactory implements IElementFactory, IAdapterFactory
{
	/**
	 * Factory id. The workbench plug-in registers a factory by this name with the "org.eclipse.ui.elementFactories" extension point.
	 */
	private static final String ID_FACTORY = "com.servoy.eclipse.core.resource.EditorInputFactory";

	private static final String TAG_TYPE = "type";
	private static final String TYPE_PERSIST = "persist";
	private static final String TYPE_SERVER = "server";
	private static final String TYPE_TABLE = "table";

	private static final String TAG_UUID = "uuid";
	private static final String TAG_NAME = "name";
	private static final String TAG_SOLUTION_NAME = "solutionName";
	private static final String TAG_SERVER_NAME = "serverName";

	/**
	 * Creates a new factory.
	 */
	public EditorInputFactory()
	{
	}

	/*
	 * (non-Javadoc) Method declared on IElementFactory.
	 */
	public IAdaptable createElement(IMemento memento)
	{
		String type = memento.getString(TAG_TYPE);
		if (TYPE_PERSIST.equals(type)) return PersitablePersistEditorInput.createPersistEditorInput(memento);
		if (TYPE_SERVER.equals(type)) return PersistableServerEditorInput.createServerEditorInput(memento);
		if (TYPE_TABLE.equals(type)) return PersitableTableEditorInput.createTableEditorInput(memento);

		return null;
	}

	public static class PersitablePersistEditorInput implements IPersistableElement
	{
		private final PersistEditorInput persistEditorInput;

		public PersitablePersistEditorInput(PersistEditorInput persistEditorInput)
		{
			this.persistEditorInput = persistEditorInput;
		}

		public String getFactoryId()
		{
			return ID_FACTORY;
		}

		/**
		 * Saves the state of the given editor input into the given memento.
		 * 
		 * @param memento the storage area for element state
		 * @param input the editor input
		 */
		public void saveState(IMemento memento)
		{
			memento.putString(TAG_TYPE, TYPE_PERSIST);
			memento.putString(TAG_UUID, persistEditorInput.getUuid().toString());
			memento.putString(TAG_NAME, persistEditorInput.getName());
			memento.putString(TAG_SOLUTION_NAME, persistEditorInput.getSolutionName());
		}

		public static IAdaptable createPersistEditorInput(IMemento memento)
		{
			String uuidString = memento.getString(TAG_UUID);
			String name = memento.getString(TAG_NAME);
			String solutionName = memento.getString(TAG_SOLUTION_NAME);
			if (uuidString == null || name == null || solutionName == null)
			{
				return null;
			}
			return new PersistEditorInput(name, solutionName, UUID.fromString(uuidString));
		}
	}

	public static class PersistableServerEditorInput implements IPersistableElement
	{
		private final ServerEditorInput serverEditorInput;

		public PersistableServerEditorInput(ServerEditorInput serverEditorInput)
		{
			this.serverEditorInput = serverEditorInput;
		}

		public String getFactoryId()
		{
			return ID_FACTORY;
		}

		/**
		 * Saves the state of the given editor input into the given memento.
		 * 
		 * @param memento the storage area for element state
		 */
		public void saveState(IMemento memento)
		{
			memento.putString(TAG_TYPE, TYPE_SERVER);
			memento.putString(TAG_NAME, serverEditorInput.getServerConfig().getServerName());
		}

		public static IAdaptable createServerEditorInput(IMemento memento)
		{
			String name = memento.getString(TAG_NAME);
			if (name == null)
			{
				return null;
			}
			ServoyModelManager.getServoyModelManager().getServoyModel();
			ServerConfig serverConfig = ServoyModel.getServerManager().getServerConfig(name);
			if (serverConfig == null)
			{
				return null;
			}
			return new ServerEditorInput(serverConfig);
		}
	}

	public static class PersitableTableEditorInput implements IPersistableElement
	{
		private final TableEditorInput tableEditorInput;

		public PersitableTableEditorInput(TableEditorInput tableEditorInput)
		{
			this.tableEditorInput = tableEditorInput;
		}

		public String getFactoryId()
		{
			return ID_FACTORY;
		}

		/**
		 * Saves the state of the given editor input into the given memento.
		 * 
		 * @param memento the storage area for element state
		 * @param input the editor input
		 */

		public void saveState(IMemento memento)
		{
			memento.putString(TAG_TYPE, TYPE_TABLE);
			memento.putString(TAG_SERVER_NAME, tableEditorInput.getServerName());
			memento.putString(TAG_NAME, tableEditorInput.getName());
		}

		public static IAdaptable createTableEditorInput(IMemento memento)
		{
			String serverName = memento.getString(TAG_SERVER_NAME);
			String tableName = memento.getString(TAG_NAME);
			if (serverName == null || tableName == null)
			{
				return null;
			}
			return new TableEditorInput(serverName, tableName);
		}
	}

	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		if (!(new DesignerPreferences().getCloseEditorOnExit()))
		{
			if (adaptableObject instanceof PersistEditorInput)
			{
				return new PersitablePersistEditorInput((PersistEditorInput)adaptableObject);
			}
			if (adaptableObject instanceof TableEditorInput)
			{
				return new PersitableTableEditorInput((TableEditorInput)adaptableObject);
			}
			if (adaptableObject instanceof ServerEditorInput)
			{
				return new PersistableServerEditorInput((ServerEditorInput)adaptableObject);
			}
		}
		return null;
	}

	public Class[] getAdapterList()
	{
		return new Class[] { IPersistableElement.class };
	}
}
