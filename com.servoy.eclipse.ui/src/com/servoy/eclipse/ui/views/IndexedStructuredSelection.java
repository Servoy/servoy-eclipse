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
package com.servoy.eclipse.ui.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Structured selection with selected indices.
 * 
 * @author rgansevles
 * 
 */

public class IndexedStructuredSelection implements IStructuredSelection
{
	private final IStructuredSelection structuredSelection;
	private final int[] indices;

	/**
	 * Constructor
	 * 
	 * @param structuredSelection
	 * @param indices selected indices
	 */
	public IndexedStructuredSelection(IStructuredSelection structuredSelection, int[] indices)
	{
		this.structuredSelection = structuredSelection;
		this.indices = indices;
	}

	public int[] getSelectedIndices()
	{
		return indices;
	}

	public Object getFirstElement()
	{
		return structuredSelection.getFirstElement();
	}

	public boolean isEmpty()
	{
		return structuredSelection.isEmpty();
	}

	public Iterator iterator()
	{
		return structuredSelection.iterator();
	}

	public int size()
	{
		return structuredSelection.size();
	}

	public Object[] toArray()
	{
		return structuredSelection.toArray();
	}

	public List toList()
	{
		return structuredSelection.toList();
	}


}
