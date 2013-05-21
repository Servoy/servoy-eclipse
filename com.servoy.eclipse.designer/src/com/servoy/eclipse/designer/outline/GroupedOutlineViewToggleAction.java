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

package com.servoy.eclipse.designer.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author alorincz
 *
 */
public class GroupedOutlineViewToggleAction extends Action implements IPropertyChangeListener
{
	private static GroupedOutlineViewToggleAction INSTANCE = null;

	private static List<TreeViewer> treeViewerList = null;

	protected GroupedOutlineViewToggleAction()
	{
		super("Group Elements", AS_CHECK_BOX); //$NON-NLS-1$
		setChecked(FormOutlineContentProvider.getDisplayType());

		addListenerObject(this);
	}

	public static GroupedOutlineViewToggleAction addListener(TreeViewer treeViewer)
	{
		if (INSTANCE == null) INSTANCE = new GroupedOutlineViewToggleAction();
		if (treeViewerList == null) treeViewerList = new ArrayList<TreeViewer>();

		treeViewerList.add(treeViewer);

		return INSTANCE;
	}

	public void propertyChange(PropertyChangeEvent event)
	{
		FormOutlineContentProvider.setDisplayType(((Boolean)event.getNewValue()).booleanValue());
		for (TreeViewer treeViewer : treeViewerList)
			treeViewer.refresh();

		IEclipsePreferences preferences = new InstanceScope().getNode("com.servoy.eclipse.designer"); //$NON-NLS-1$
		preferences.putBoolean("OutlineViewMode", ((Boolean)event.getNewValue()).booleanValue()); //$NON-NLS-1$
		try
		{
			preferences.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
	}
}
