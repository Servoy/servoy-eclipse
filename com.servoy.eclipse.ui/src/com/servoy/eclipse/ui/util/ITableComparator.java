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
package com.servoy.eclipse.ui.util;

import java.util.Comparator;

import com.servoy.j2db.persistence.ITable;

public class ITableComparator implements Comparator<ITable>
{
	public int compare(ITable t1, ITable t2)
	{
		if (t1 == null)
		{
			if (t2 == null) return 0;
			else return -1;
		}
		else
		{
			if (t2 == null) return 1;
			else
			{
				if (t1.getName() == null)
				{
					if (t2.getName() == null) return 0;
					else return -1;
				}
				else
				{
					if (t2.getName() == null) return 1;
					else return t1.getName().compareTo(t2.getName());
				}
			}
		}
	}
}
