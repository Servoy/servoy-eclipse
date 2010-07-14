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

import org.eclipse.osgi.util.NLS;

import com.servoy.eclipse.ui.Messages;

/**
 * Container for unresolved values in designer.
 * 
 * @author rgansevles
 *
 */
public class UnresolvedValue
{
	public final static UnresolvedValue NO_STRING_VALUE = new UnresolvedValue(null); // when you do not have a string representation of the unresolved value

	private final String unresolved;

	public UnresolvedValue(String unresolved)
	{
		this.unresolved = unresolved;
	}

	public String getUnresolved()
	{
		return unresolved;
	}

	public String getUnresolvedMessage()
	{
		return getUnresolvedMessage(unresolved);
	}

	public static String getUnresolvedMessage(String unresolved)
	{
		return unresolved == null ? Messages.LabelUnresolved : NLS.bind(Messages.LabelUnresolved_arg, unresolved);
	}

	@Override
	public String toString()
	{
		return "UnresolvedValue [unresolved=" + unresolved + ']'; //$NON-NLS-1$
	}

}
