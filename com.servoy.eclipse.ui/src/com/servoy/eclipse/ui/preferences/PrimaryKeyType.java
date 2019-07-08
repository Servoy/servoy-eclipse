/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ui.preferences;

import com.servoy.j2db.persistence.IColumnTypes;

/**
 * Values for primary key type in Servoy Preferences.
 *
 * @author rgansevles
 *
 */
public enum PrimaryKeyType
{
	INTEGER(0, IColumnTypes.INTEGER, false, false), //
	UUD_BYTE_ARRAY(16, IColumnTypes.MEDIA, true, false), //
	UUD_STRING_ARRAY(32, IColumnTypes.TEXT, true, false), //
	UUD_NATIVE(16, IColumnTypes.MEDIA, true, true);

	private final int length;
	private final int columnType;
	private final boolean isUUID;
	private final boolean isNative;

	private PrimaryKeyType(int length, int columnType, boolean isUUID, boolean isNative)
	{
		this.length = length;
		this.columnType = columnType;
		this.isUUID = isUUID;
		this.isNative = isNative;
	}

	public int getLength()
	{
		return length;
	}

	public int getColumnType()
	{
		return columnType;
	}

	public boolean isUUID()
	{
		return isUUID;
	}

	public boolean isNative()
	{
		return isNative;
	}
}
