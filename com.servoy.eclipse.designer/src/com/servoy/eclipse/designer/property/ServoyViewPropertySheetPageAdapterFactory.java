/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
package com.servoy.eclipse.designer.property;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.ui.views.ModifiedPropertySheetPage;
import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Factory for adapters for the IPropertySheetPage from Servoy views.
 */
public class ServoyViewPropertySheetPageAdapterFactory implements IAdapterFactory
{
	private static Class[] ADAPTERS = new Class[] { IPropertySheetPage.class };

	public Class[] getAdapterList()
	{
		return ADAPTERS;
	}

	public Object getAdapter(Object obj, Class key)
	{
		if ((obj instanceof SolutionExplorerView || obj instanceof FormHierarchyView) && key == IPropertySheetPage.class)
		{
			PropertySheetPage page = new ModifiedPropertySheetPage(null);
			page.setRootEntry(new OpenEditorUndoablePropertySheetEntry());
			return page;
		}

		return null;
	}
}
