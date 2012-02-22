/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
package com.servoy.eclipse.model.preferences;

import java.util.Arrays;

import org.jabsorb.JSONSerializer;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.query.ColumnType;

/**
 * Preferences for dbi files.
 * Are stored at project level.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 * 
 */
public class DbiFilePreferences extends ProjectPreferences
{
	public static final String ACCEPTED_COLUMN_DIFFERENCES_SETTING = "acceptedColumnDifferences";

	public DbiFilePreferences(ServoyProject project)
	{
		super(project, "dbifiles");
	}

	protected Integer[][] getAcceptedColumnDifferenceList()
	{
		String json = getProperty(ACCEPTED_COLUMN_DIFFERENCES_SETTING, null);
		if (json != null)
		{
			try
			{
				JSONSerializer serializer = new JSONSerializer();
				serializer.registerDefaultSerializers();
				return (Integer[][])serializer.fromJSON(json);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}

	protected void setAcceptedColumnDifferenceList(Integer[][] accepted)
	{
		if (accepted == null || accepted.length == 0)
		{
			removeProperty(ACCEPTED_COLUMN_DIFFERENCES_SETTING);
		}
		else
		{
			try
			{
				JSONSerializer serializer = new JSONSerializer();
				serializer.registerDefaultSerializers();
				setProperty(ACCEPTED_COLUMN_DIFFERENCES_SETTING, serializer.toJSON(accepted));
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		save();
	}

	/**
	 * @param dbiColumnType
	 * @param tableColumnType
	 */
	public void addAcceptedColumnDifference(ColumnType dbiColumnType, ColumnType tableColumnType)
	{
		if (dbiColumnType == null || tableColumnType == null) return;

		Integer[][] accepted = getAcceptedColumnDifferenceList();
		if (checkAcceptedColumnDifference(accepted, dbiColumnType, tableColumnType))
		{
			// already accepted
			return;
		}

		if (accepted == null)
		{
			accepted = new Integer[1][];
		}
		else
		{
			accepted = Arrays.asList(accepted).toArray(new Integer[accepted.length + 1][]);
		}
		accepted[accepted.length - 1] = new Integer[] {//
		Integer.valueOf(dbiColumnType.getSqlType()), Integer.valueOf(dbiColumnType.getLength()), Integer.valueOf(dbiColumnType.getScale()),//
		Integer.valueOf(tableColumnType.getSqlType()), Integer.valueOf(tableColumnType.getLength()), Integer.valueOf(tableColumnType.getScale()) };

		setAcceptedColumnDifferenceList(accepted);
	}

	/**
	 * @param dbiColumnType
	 * @param tableColumnType
	 */
	public boolean isAcceptedColumnDifference(ColumnType dbiColumnType, ColumnType tableColumnType)
	{
		if (dbiColumnType == null || tableColumnType == null) return false;

		return checkAcceptedColumnDifference(getAcceptedColumnDifferenceList(), dbiColumnType, tableColumnType);
	}

	protected boolean checkAcceptedColumnDifference(Integer[][] accepted, ColumnType dbiColumnType, ColumnType tableColumnType)
	{
		if (accepted != null)
		{
			for (Integer[] info : accepted)
			{
				if (info != null && info.length == 6 //
					&& dbiColumnType.getSqlType() == info[0].intValue()//
					&& dbiColumnType.getLength() == info[1].intValue()//
					&& dbiColumnType.getScale() == info[2].intValue()//
					&& tableColumnType.getSqlType() == info[3].intValue()//
					&& tableColumnType.getLength() == info[4].intValue()//
					&& tableColumnType.getScale() == info[5].intValue()//
				)
				{
					return true;
				}
			}
		}

		return false;
	}

	public void clearAcceptedColumnDifferences()
	{
		setAcceptedColumnDifferenceList(null);
	}
}