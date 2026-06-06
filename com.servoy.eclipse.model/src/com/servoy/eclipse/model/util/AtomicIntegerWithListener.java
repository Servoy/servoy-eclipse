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

package com.servoy.eclipse.model.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atomic Integer With Listener.
 * Listener is notified when value reaches zero.
 * 
 * @author rgansevles
 *
 */
public class AtomicIntegerWithListener
{
	private final List<IValueListener> listeners = Collections.synchronizedList(new ArrayList<IValueListener>());

	private final AtomicInteger atomicInteger = new AtomicInteger();

	/**
	 * Atomically increment by one the current value.
	 * @return the new value
	 */
	public int increment()
	{
		return atomicInteger.getAndIncrement() + 1;
	}

	/**
	 * Atomically decrement by one the current value.
	 * @return the new value
	 */
	public int decrement()
	{
		int newValue = atomicInteger.getAndDecrement() - 1;
		if (newValue < 0)
		{
			throw new IllegalStateException("AtomicIntegerWithListener increment/decrement out of sync");
		}
		if (newValue == 0) notifyListeners();
		return newValue;
	}

	/**
	* Get the current value.
	*
	* @return the current value
	*/
	public int getValue()
	{
		return atomicInteger.get();
	}

	public void addValueListener(IValueListener listener)
	{
		if (!listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}

	public void removeValueListener(IValueListener listener)
	{
		listeners.remove(listener);
	}

	private void notifyListeners()
	{
		if (listeners.size() > 0)
		{
			for (Object listener : listeners.toArray())
			{
				if (listeners.contains(listener))
				{
					((IValueListener)listener).valueSetToZero();
				}
			}
		}
	}

	public interface IValueListener
	{
		void valueSetToZero();
	}

}
