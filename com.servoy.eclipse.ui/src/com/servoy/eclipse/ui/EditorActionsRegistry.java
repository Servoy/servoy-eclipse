/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IModificationSubject;
import com.servoy.j2db.util.UUID;

/**
 * Registry for editor actions that are handled by commands in the designer.
 *
 * @author rgansevles
 *
 */
public class EditorActionsRegistry
{
	private static Map<EditorComponentActions, EditorComponentActionHandler> editorActionHandlers = new HashMap<>();

	public enum EditorComponentActions
	{
		CREATE_CUSTOM_COMPONENT
	}

	public interface EditorComponentActionHandler
	{
		void createComponent(IPropertySource persistPropertySource, UUID uuid, String propertyName, String type, Integer index, boolean dropTargetIsSibling);

		void deleteComponent(IPropertySource propertySource, UUID uuid);

		IModificationSubject getModificationSubject();
	}

	public static void registerHandler(EditorComponentActions action, EditorComponentActionHandler handler)
	{
		editorActionHandlers.put(action, handler);
	}

	public static void unregisterHandler(EditorComponentActions action, EditorComponentActionHandler handler)
	{
		editorActionHandlers.remove(action);
	}

	public static EditorComponentActionHandler getHandler(EditorComponentActions action)
	{
		return editorActionHandlers.get(action);
	}

	/**
	 * Add a listener to be called after the handler has executed a command.
	 */
	public static IModificationListener addComponentActionListener(EditorComponentActions action, IModificationListener listener)
	{
		EditorComponentActionHandler handler = getHandler(action);
		if (handler == null)
		{
			ServoyLog.logWarning("No handler registered for " + action, null);
			return null;
		}

		handler.getModificationSubject().addModificationListener(listener);
		return listener;
	}

	/**
	 * Remove a listener to be called after the handler has executed a command.
	 */
	public static void removeComponentActionListener(EditorComponentActions action, IModificationListener listener)
	{
		EditorComponentActionHandler handler = getHandler(action);
		if (handler == null)
		{
			ServoyLog.logWarning("No handler registered for " + action, null);
		}
		else
		{
			handler.getModificationSubject().removeModificationListener(listener);
		}
	}
}
