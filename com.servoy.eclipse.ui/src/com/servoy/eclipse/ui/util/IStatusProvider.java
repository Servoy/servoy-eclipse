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


/**
 * Interface common to all objects that provide a status.
 *
 * @see IStatusChangedListener
 * 
 * @author rgansevles
 */
public interface IStatusProvider
{
	/**
	 * Adds a listener for selection changes in this status provider.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener a status changed listener
	 */
	public void addStatusChangedListener(IStatusChangedListener listener);

	/**
	 * Returns the current status for this provider.
	 * 
	 * @return the current status
	 */
	public boolean isValid();

	/**
	 * Removes the given status change listener from this status provider.
	 * Has no affect if an identical listener is not registered.
	 *
	 * @param listener a status changed listener
	 */
	public void removeStatusChangedListener(IStatusChangedListener listener);

	/**
	* Sets the current status for this status provider.
	*
	* @param valid the new status
	*/
	public void setValid(boolean valid);
}
