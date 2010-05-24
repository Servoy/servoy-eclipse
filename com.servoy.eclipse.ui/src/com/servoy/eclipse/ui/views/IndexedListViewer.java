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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.widgets.Composite;

public class IndexedListViewer extends ListViewer
{
	/**
	 * Creates a list viewer on a newly-created list control under the given parent. The list control is created using the SWT style bits
	 * <code>MULTI, H_SCROLL, V_SCROLL,</code> and <code>BORDER</code>. The viewer has no input, no content provider, a default label provider, no sorter,
	 * and no filters.
	 * 
	 * @param parent the parent control
	 */
	public IndexedListViewer(Composite parent)
	{
		super(parent);
	}

	/**
	 * Creates a list viewer on a newly-created list control under the given parent. The list control is created using the given SWT style bits. The viewer has
	 * no input, no content provider, a default label provider, no sorter, and no filters.
	 * 
	 * @param parent the parent control
	 * @param style the SWT style bits
	 */
	public IndexedListViewer(Composite parent, int style)
	{
		super(parent, style);
	}

	/**
	 * Creates a list viewer on the given list control. The viewer has no input, no content provider, a default label provider, no sorter, and no filters.
	 * 
	 * @param list the list control
	 */
	public IndexedListViewer(org.eclipse.swt.widgets.List list)
	{
		super(list);
	}

	/**
	 * The <code>StructuredViewer</code> implementation of this method returns the result as an <code>IStructuredSelection</code>.
	 * <p>
	 * Subclasses do not typically override this method, but implement <code>getSelectionFromWidget(List)</code> instead.
	 * <p>
	 * 
	 * @return ISelection
	 */
	@Override
	public ISelection getSelection()
	{
		return new IndexedStructuredSelection((IStructuredSelection)super.getSelection(), listGetSelectionIndices());
	}
}
