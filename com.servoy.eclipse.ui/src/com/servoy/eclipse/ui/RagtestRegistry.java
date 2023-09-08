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

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

/**
 * RAGTEST doc
 *
 * @author rgansevles
 *
 */
public class RagtestRegistry
{
	private static Map<EditorRagtestActions, EditorRagtestHandler> editorActionHandlers = new HashMap<>();

	public enum EditorRagtestActions
	{
		CREATE_COMPONENT_RAGTEST
	}

	public interface EditorRagtestHandler
	{
		void createComponent(UUID uuid, String propertyName, String type);

		void deleteComponent(IPersist persist);
	}

	public static void registerRagtest(EditorRagtestActions action, EditorRagtestHandler handler)
	{
		editorActionHandlers.put(action, handler); // RAGTEST todo lijst
	}

	public static void unregisterRagtest(EditorRagtestActions action, EditorRagtestHandler handler)
	{
		editorActionHandlers.remove(action); // RAGTEST todo lijst
	}

	public static EditorRagtestHandler getRagtestHandler(EditorRagtestActions action)
	{
		return editorActionHandlers.get(action); // RAGTEST todo lijst
	}
}
