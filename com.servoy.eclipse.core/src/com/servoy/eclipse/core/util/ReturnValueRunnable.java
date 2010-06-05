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
package com.servoy.eclipse.core.util;

/**
 * A Runnable that is able to remember a return value for later use (useful for async/sync calls that execute in other threads).
 * 
 * @author Andrei Costescu
 */
public abstract class ReturnValueRunnable implements Runnable
{
	/**
	 * The return value of this runnable. It's value must be set by the execution of run().
	 */
	protected Object returnValue = null;

	/**
	 * Gives the return value of the code executed in run().
	 * 
	 * @return the return value of the code executed in run().
	 */
	public Object getReturnValue()
	{
		return returnValue;
	}

}