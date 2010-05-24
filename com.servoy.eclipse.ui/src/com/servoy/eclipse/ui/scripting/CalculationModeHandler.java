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
package com.servoy.eclipse.ui.scripting;

import java.util.HashSet;
import java.util.Set;

import com.servoy.eclipse.ui.Messages;

/**
 * @author jcompagner
 * 
 */
public class CalculationModeHandler
{
	private static CalculationModeHandler instance = new CalculationModeHandler();

	private final Set<String> names = new HashSet<String>();

	private final Set<String> partialList = new HashSet<String>();

	private boolean calculationMode;

	private CalculationModeHandler()
	{
		names.add(Messages.TreeStrings_DatabaseManager);
		names.add(Messages.TreeStrings_History);
		names.add(Messages.TreeStrings_SolutionModel);

		// partial list should have the tree string and the script string.
		partialList.add(Messages.TreeStrings_Application);
		partialList.add("application");
		partialList.add(Messages.TreeStrings_i18n);
		partialList.add("i18n");
		partialList.add(Messages.TreeStrings_Security);
		partialList.add("security");

	}

	public static CalculationModeHandler getInstance()
	{
		return instance;
	}

	public boolean hide(String parent)
	{
		return hide(parent, null);
	}

	public boolean hide(String parent, String child)
	{
		if (!calculationMode) return false;
		if (names.contains(parent))
		{
			return true;
		}
		if (child != null && partialList.contains(parent))
		{
			// all gets are allowed.
			if (child.startsWith("get")) return false;
			if (child.equals("beep") || child.equals("output")) return false;
			return true;
		}
		return false;
	}

	/**
	 * @param name
	 * @return
	 */
	public boolean hasPartialList(String name)
	{
		if (!calculationMode) return false;
		return partialList.contains(name);
	}

	public void setCalculationMode(boolean calculationMode)
	{
		this.calculationMode = calculationMode;
	}

	/**
	 * @return
	 */
	public boolean isCalculationMode()
	{
		return calculationMode;
	}
}
