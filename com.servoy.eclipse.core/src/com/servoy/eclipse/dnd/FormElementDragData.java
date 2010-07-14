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
package com.servoy.eclipse.dnd;

import java.io.Serializable;

import com.servoy.j2db.util.UUID;

/**
 * Holders for dragging elements or dataproviders.
 * 
 * @author rgansevles
 */


public abstract class FormElementDragData implements IDragData, Serializable
{
	public static final String DND_PREFIX = "%SERVOY%"; //$NON-NLS-1$

	public static class PersistDragData extends FormElementDragData
	{
		public final String solutionName;
		public final UUID uuid;
		public final int type;

		public PersistDragData(String solutionName, UUID uuid, int type)
		{
			this.solutionName = solutionName;
			this.uuid = uuid;
			this.type = type;
		}
	}

	public static class DataProviderDragData extends FormElementDragData
	{
		public final String columnTableName; // table that the column exists in
		public final String serverName;
		public final String dataProviderId;
		public final String baseTableName; // table that the data provider may be placed on
		public final String relationName; // may be null

		public DataProviderDragData(String columnTableName, String serverName, String dataProviderId, String baseTableName, String relationName)
		{
			this.columnTableName = columnTableName;
			this.serverName = serverName;
			this.dataProviderId = dataProviderId;
			this.baseTableName = baseTableName;
			this.relationName = relationName;
		}
	}
}
